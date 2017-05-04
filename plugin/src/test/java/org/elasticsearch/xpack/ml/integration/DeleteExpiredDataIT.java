/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2017 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.ml.integration;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateAction;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.xpack.ml.action.DeleteExpiredDataAction;
import org.elasticsearch.xpack.ml.action.UpdateModelSnapshotAction;
import org.elasticsearch.xpack.ml.datafeed.DatafeedConfig;
import org.elasticsearch.xpack.ml.job.config.AnalysisConfig;
import org.elasticsearch.xpack.ml.job.config.DataDescription;
import org.elasticsearch.xpack.ml.job.config.Detector;
import org.elasticsearch.xpack.ml.job.config.Job;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.ModelSnapshot;
import org.elasticsearch.xpack.ml.job.results.Bucket;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

public class DeleteExpiredDataIT extends MlNativeAutodetectIntegTestCase {

    private static final String DATA_INDEX = "delete-expired-data-test-data";
    private static final String DATA_TYPE = "my_type";

    @Before
    public void setUpData() throws IOException {
        client().admin().indices().prepareCreate(DATA_INDEX)
                .addMapping(DATA_TYPE, "time", "type=date,format=epoch_millis")
                .get();

        // We are going to create data for last 2 days
        long nowMillis = System.currentTimeMillis();
        int totalBuckets = 3 * 24;
        int normalRate = 10;
        int anomalousRate = 100;
        int anomalousBucket = 30;
        BulkRequestBuilder bulkRequestBuilder = client().prepareBulk();
        for (int bucket = 0; bucket < totalBuckets; bucket++) {
            long timestamp = nowMillis - TimeValue.timeValueHours(totalBuckets - bucket).getMillis();
            int bucketRate = bucket == anomalousBucket ? anomalousRate : normalRate;
            for (int point = 0; point < bucketRate; point++) {
                IndexRequest indexRequest = new IndexRequest(DATA_INDEX, DATA_TYPE);
                indexRequest.source("time", timestamp);
                bulkRequestBuilder.add(indexRequest);
            }
        }

        BulkResponse bulkResponse = bulkRequestBuilder
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
                .get();
        assertThat(bulkResponse.hasFailures(), is(false));

        // Ensure all data is searchable
        client().admin().indices().prepareRefresh(DATA_INDEX).get();
    }

    @After
    public void tearDownData() throws Exception {
        client().admin().indices().prepareDelete(DATA_INDEX).get();
        cleanUp();
    }

    public void testDeleteExpiredData() throws Exception {
        registerJob(newJobBuilder("no-retention").setResultsRetentionDays(null).setModelSnapshotRetentionDays(null));
        registerJob(newJobBuilder("results-retention").setResultsRetentionDays(1L).setModelSnapshotRetentionDays(null));
        registerJob(newJobBuilder("snapshots-retention").setResultsRetentionDays(null).setModelSnapshotRetentionDays(2L));
        registerJob(newJobBuilder("snapshots-retention-with-retain").setResultsRetentionDays(null).setModelSnapshotRetentionDays(2L));
        registerJob(newJobBuilder("results-and-snapshots-retention").setResultsRetentionDays(1L).setModelSnapshotRetentionDays(2L));

        long now = System.currentTimeMillis();
        long oneDayAgo = now - TimeValue.timeValueHours(48).getMillis() - 1;
        for (Job.Builder job : getJobs()) {
            putJob(job);

            String datafeedId = job.getId() + "-feed";
            DatafeedConfig.Builder datafeedConfig = new DatafeedConfig.Builder(datafeedId, job.getId());
            datafeedConfig.setIndices(Arrays.asList(DATA_INDEX));
            datafeedConfig.setTypes(Arrays.asList(DATA_TYPE));
            DatafeedConfig datafeed = datafeedConfig.build();
            registerDatafeed(datafeed);
            putDatafeed(datafeed);

            // Run up to a day ago
            openJob(job.getId());
            startDatafeed(datafeedId, 0, now - TimeValue.timeValueHours(24).getMillis());
            waitUntilJobIsClosed(job.getId());
            assertThat(getBuckets(job.getId()).size(), is(greaterThanOrEqualTo(47)));
            assertThat(getRecords(job.getId()).size(), equalTo(1));
            List<ModelSnapshot> modelSnapshots = getModelSnapshots(job.getId());
            assertThat(modelSnapshots.size(), equalTo(1));
            String snapshotDocId = job.getId() + "-" + modelSnapshots.get(0).getSnapshotId();

            // Update snapshot timestamp to force it out of snapshot retention window
            String snapshotUpdate = "{ \"timestamp\": " + oneDayAgo + "}";
            UpdateRequest updateSnapshotRequest = new UpdateRequest(".ml-anomalies-" + job.getId(), "model_snapshot", snapshotDocId);
            updateSnapshotRequest.doc(snapshotUpdate.getBytes(StandardCharsets.UTF_8), XContentType.JSON);
            client().execute(UpdateAction.INSTANCE, updateSnapshotRequest).get();
        }
        // Refresh to ensure the snapshot timestamp updates are visible
        client().admin().indices().prepareRefresh("*").get();

        // We need to wait a second to ensure the second time around model snapshots will have a different ID (it depends on epoch seconds)
        awaitBusy(() -> false, 1, TimeUnit.SECONDS);

        for (Job.Builder job : getJobs()) {
            // Run up to now
            openJob(job.getId());
            startDatafeed(job.getId() + "-feed", 0, now);
            waitUntilJobIsClosed(job.getId());
            assertThat(getBuckets(job.getId()).size(), is(greaterThanOrEqualTo(70)));
            assertThat(getRecords(job.getId()).size(), equalTo(1));
            List<ModelSnapshot> modelSnapshots = getModelSnapshots(job.getId());
            assertThat(modelSnapshots.size(), equalTo(2));
        }

        retainAllSnapshots("snapshots-retention-with-retain");

        long totalModelSizeStatsBeforeDelete = client().prepareSearch("*").setTypes("result")
                .setQuery(QueryBuilders.termQuery("result_type", "model_size_stats"))
                .get().getHits().totalHits;
        long totalNotificationsCountBeforeDelete = client().prepareSearch(".ml-notifications").get().getHits().totalHits;
        assertThat(totalModelSizeStatsBeforeDelete, greaterThan(0L));
        assertThat(totalNotificationsCountBeforeDelete, greaterThan(0L));

        client().execute(DeleteExpiredDataAction.INSTANCE, new DeleteExpiredDataAction.Request()).get();

        // We need to refresh to ensure the deletion is visible
        client().admin().indices().prepareRefresh("*").get();

        // no-retention job should have kept all data
        assertThat(getBuckets("no-retention").size(), is(greaterThanOrEqualTo(70)));
        assertThat(getRecords("no-retention").size(), equalTo(1));
        assertThat(getModelSnapshots("no-retention").size(), equalTo(2));

        List<Bucket> buckets = getBuckets("results-retention");
        assertThat(buckets.size(), is(lessThanOrEqualTo(24)));
        assertThat(buckets.size(), is(greaterThanOrEqualTo(22)));
        assertThat(buckets.get(0).getTimestamp().getTime(), greaterThanOrEqualTo(oneDayAgo));
        assertThat(getRecords("results-retention").size(), equalTo(0));
        assertThat(getModelSnapshots("results-retention").size(), equalTo(2));

        assertThat(getBuckets("snapshots-retention").size(), is(greaterThanOrEqualTo(70)));
        assertThat(getRecords("snapshots-retention").size(), equalTo(1));
        assertThat(getModelSnapshots("snapshots-retention").size(), equalTo(1));

        assertThat(getBuckets("snapshots-retention-with-retain").size(), is(greaterThanOrEqualTo(70)));
        assertThat(getRecords("snapshots-retention-with-retain").size(), equalTo(1));
        assertThat(getModelSnapshots("snapshots-retention-with-retain").size(), equalTo(2));

        buckets = getBuckets("results-and-snapshots-retention");
        assertThat(buckets.size(), is(lessThanOrEqualTo(24)));
        assertThat(buckets.size(), is(greaterThanOrEqualTo(22)));
        assertThat(buckets.get(0).getTimestamp().getTime(), greaterThanOrEqualTo(oneDayAgo));
        assertThat(getRecords("results-and-snapshots-retention").size(), equalTo(0));
        assertThat(getModelSnapshots("results-and-snapshots-retention").size(), equalTo(1));

        long totalModelSizeStatsAfterDelete = client().prepareSearch("*").setTypes("result")
                .setQuery(QueryBuilders.termQuery("result_type", "model_size_stats"))
                .get().getHits().totalHits;
        long totalNotificationsCountAfterDelete = client().prepareSearch(".ml-notifications").get().getHits().totalHits;
        assertThat(totalModelSizeStatsAfterDelete, equalTo(totalModelSizeStatsBeforeDelete));
        assertThat(totalNotificationsCountAfterDelete, greaterThanOrEqualTo(totalNotificationsCountBeforeDelete));
    }

    private static Job.Builder newJobBuilder(String id) {
        Detector.Builder detector = new Detector.Builder();
        detector.setFunction("count");
        AnalysisConfig.Builder analysisConfig = new AnalysisConfig.Builder(Arrays.asList(detector.build()));
        analysisConfig.setBucketSpan(TimeValue.timeValueHours(1));
        DataDescription.Builder dataDescription = new DataDescription.Builder();
        dataDescription.setTimeField("time");
        Job.Builder jobBuilder = new Job.Builder(id);
        jobBuilder.setAnalysisConfig(analysisConfig);
        jobBuilder.setDataDescription(dataDescription);
        return jobBuilder;
    }

    private void retainAllSnapshots(String jobId) throws Exception {
        List<ModelSnapshot> modelSnapshots = getModelSnapshots(jobId);
        for (ModelSnapshot modelSnapshot : modelSnapshots) {
            UpdateModelSnapshotAction.Request request = new UpdateModelSnapshotAction.Request(
                    jobId, modelSnapshot.getSnapshotId());
            request.setRetain(true);
            client().execute(UpdateModelSnapshotAction.INSTANCE, request).get();
        }
        // We need to refresh to ensure the updates are visible
        client().admin().indices().prepareRefresh("*").get();
    }
}
