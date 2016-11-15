/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
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
package org.elasticsearch.xpack.prelert.action;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.network.NetworkAddress;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.xpack.prelert.PrelertPlugin;
import org.elasticsearch.xpack.prelert.job.AnalysisConfig;
import org.elasticsearch.xpack.prelert.job.DataCounts;
import org.elasticsearch.xpack.prelert.job.DataDescription;
import org.elasticsearch.xpack.prelert.job.Detector;
import org.elasticsearch.xpack.prelert.job.Job;
import org.elasticsearch.xpack.prelert.job.JobSchedulerStatus;
import org.elasticsearch.xpack.prelert.job.SchedulerConfig;
import org.elasticsearch.xpack.prelert.job.SchedulerState;
import org.elasticsearch.xpack.prelert.job.metadata.PrelertMetadata;
import org.elasticsearch.xpack.prelert.job.persistence.ElasticsearchPersister;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@ESIntegTestCase.ClusterScope(numDataNodes = 1)
public class ScheduledJobsIT extends ESIntegTestCase {

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Collections.singleton(PrelertPlugin.class);
    }

    @Override
    protected Collection<Class<? extends Plugin>> transportClientPlugins() {
        return nodePlugins();
    }

    @After
    public void clearPrelertMetadata() throws Exception {
        client().execute(ClearPrelertAction.INSTANCE, new ClearPrelertAction.Request()).get();
    }

    public void testLookbackOnly() throws Exception {
        client().admin().indices().prepareCreate("data")
                .addMapping("type", "time", "type=date")
                .get();
        long numDocs = randomIntBetween(32, 2048);
        long now = System.currentTimeMillis();
        long lastWeek = now - 604800000;
        indexDocs(numDocs, lastWeek, now);

        Job.Builder job = createJob();
        PutJobAction.Request putJobRequest = new PutJobAction.Request(job.build(true));
        client().execute(PutJobAction.INSTANCE, putJobRequest).get();

        SchedulerState schedulerState = new SchedulerState(JobSchedulerStatus.STARTING, 0, now);
        StartJobSchedulerAction.Request startSchedulerRequest = new StartJobSchedulerAction.Request("_job_id", schedulerState);
        client().execute(StartJobSchedulerAction.INSTANCE, startSchedulerRequest).get();
        assertBusy(() -> {
            DataCounts dataCounts = getDataCounts("_job_id");
            assertThat(dataCounts.getInputRecordCount(), equalTo(numDocs));

            PrelertMetadata prelertMetadata = client().admin().cluster().prepareState().all().get()
                    .getState().metaData().custom(PrelertMetadata.TYPE);
            assertThat(prelertMetadata.getAllocations().get("_job_id").getSchedulerState().getStatus(),
                    equalTo(JobSchedulerStatus.STOPPED));
        });
    }

    public void testRealtime() throws Exception {
        client().admin().indices().prepareCreate("data")
                .addMapping("type", "time", "type=date")
                .get();
        long numDocs1 = randomIntBetween(32, 2048);
        long now = System.currentTimeMillis();
        long lastWeek = System.currentTimeMillis() - 604800000;
        indexDocs(numDocs1, lastWeek, now);

        Job.Builder job = createJob();
        PutJobAction.Request putJobRequest = new PutJobAction.Request(job.build(true));
        client().execute(PutJobAction.INSTANCE, putJobRequest).get();

        SchedulerState schedulerState = new SchedulerState(JobSchedulerStatus.STARTING, 0, null);
        StartJobSchedulerAction.Request startSchedulerRequest = new StartJobSchedulerAction.Request("_job_id", schedulerState);
        client().execute(StartJobSchedulerAction.INSTANCE, startSchedulerRequest).get();
        assertBusy(() -> {
            DataCounts dataCounts = getDataCounts("_job_id");
            assertThat(dataCounts.getInputRecordCount(), equalTo(numDocs1));
        });

        long numDocs2 = randomIntBetween(2, 64);
        now = System.currentTimeMillis();
        indexDocs(numDocs2, now + 5000, now + 6000);
        assertBusy(() -> {
            DataCounts dataCounts = getDataCounts("_job_id");
            assertThat(dataCounts.getInputRecordCount(), equalTo(numDocs1 + numDocs2));
        }, 30, TimeUnit.SECONDS);

        StopJobSchedulerAction.Request stopSchedulerRequest = new StopJobSchedulerAction.Request("_job_id");
        client().execute(StopJobSchedulerAction.INSTANCE, stopSchedulerRequest).get();
        assertBusy(() -> {
            PrelertMetadata prelertMetadata = client().admin().cluster().prepareState().all().get()
                    .getState().metaData().custom(PrelertMetadata.TYPE);
            assertThat(prelertMetadata.getAllocations().get("_job_id").getSchedulerState().getStatus(),
                    equalTo(JobSchedulerStatus.STOPPED));
        });
    }

    private void indexDocs(long numDocs, long start, long end) {
        int maxIncrement = (int) ((end - start) / numDocs);
        BulkRequestBuilder bulkRequestBuilder = client().prepareBulk();
        long timestamp = start;
        for (int i = 0; i < numDocs; i++) {
            IndexRequest indexRequest = new IndexRequest("data", "type", Integer.toString(i));
            indexRequest.source("time", timestamp);
            bulkRequestBuilder.add(indexRequest);
            timestamp += randomIntBetween(1, maxIncrement);
        }
        BulkResponse bulkResponse = bulkRequestBuilder
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
                .get();
        assertThat(bulkResponse.hasFailures(), is(false));
        logger.info("Indexed [{}] documents", numDocs);
    }

    private Job.Builder createJob() {
        SchedulerConfig.Builder scheduler = new SchedulerConfig.Builder(SchedulerConfig.DataSource.ELASTICSEARCH);
        scheduler.setQueryDelay(1);
        scheduler.setFrequency(2);
        InetSocketAddress address = cluster().httpAddresses()[0];
        scheduler.setBaseUrl("http://" + NetworkAddress.format(address.getAddress()) + ":" + address.getPort());
        scheduler.setIndexes(Collections.singletonList("data"));
        scheduler.setTypes(Collections.singletonList("type"));

        DataDescription.Builder dataDescription = new DataDescription.Builder();
        dataDescription.setFormat(DataDescription.DataFormat.ELASTICSEARCH);
        dataDescription.setTimeFormat(DataDescription.EPOCH_MS);

        Detector.Builder d = new Detector.Builder("count", null);
        AnalysisConfig.Builder analysisConfig = new AnalysisConfig.Builder(Collections.singletonList(d.build()));

        Job.Builder builder = new Job.Builder();
        builder.setSchedulerConfig(scheduler);
        builder.setId("_job_id");

        builder.setAnalysisConfig(analysisConfig);
        builder.setDataDescription(dataDescription);
        return builder;
    }

    private DataCounts getDataCounts(String jobId) {
        GetResponse getResponse = client().prepareGet(ElasticsearchPersister.getJobIndexName(jobId),
                DataCounts.TYPE.getPreferredName(), jobId + "-data-counts").get();
        if (getResponse.isExists() == false) {
            return new DataCounts("_job_id");
        }

        try (XContentParser parser = XContentHelper.createParser(getResponse.getSourceAsBytesRef())) {
            return DataCounts.PARSER.apply(parser, () -> ParseFieldMatcher.EMPTY);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
