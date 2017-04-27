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

import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.xpack.ml.action.GetBucketsAction;
import org.elasticsearch.xpack.ml.action.util.PageParams;
import org.elasticsearch.xpack.ml.job.config.AnalysisConfig;
import org.elasticsearch.xpack.ml.job.config.DataDescription;
import org.elasticsearch.xpack.ml.job.config.Detector;
import org.elasticsearch.xpack.ml.job.config.Job;
import org.elasticsearch.xpack.ml.job.results.Bucket;
import org.junit.After;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

/**
 * Tests that interim results get updated correctly
 */
public class UpdateInterimResultsIT extends MlNativeAutodetectIntegTestCase {

    private static final String JOB_ID = "update-interim-test";
    private static final long BUCKET_SPAN_SECONDS = 1000;

    private long time;

    @After
    public void cleanUpTest() throws Exception {
        cleanUp();
    }

    public void test() throws Exception {
        AnalysisConfig.Builder analysisConfig = new AnalysisConfig.Builder(
                Arrays.asList(new Detector.Builder("max", "value").build()));
        analysisConfig.setBucketSpan(TimeValue.timeValueSeconds(BUCKET_SPAN_SECONDS));
        analysisConfig.setOverlappingBuckets(true);
        DataDescription.Builder dataDescription = new DataDescription.Builder();
        dataDescription.setTimeFormat("epoch");
        Job.Builder job = new Job.Builder(JOB_ID);
        job.setAnalysisConfig(analysisConfig);
        job.setDataDescription(dataDescription);

        registerJob(job);
        putJob(job);
        openJob(job.getId());

        time = 1400000000;
        Map<Long, Integer> anomalies = new HashMap<>();
        anomalies.put(1400021500L, 14);

        // push some data, flush job, verify no interim results
        assertThat(postData(job.getId(), createData(50, anomalies)).getProcessedRecordCount(), equalTo(50L));
        flushJob(job.getId(), false);
        assertThat(getInterimResults(job.getId()).isEmpty(), is(true));

        // push some more data, flush job, verify no interim results
        assertThat(postData(job.getId(), createData(30, anomalies)).getProcessedRecordCount(), equalTo(30L));
        flushJob(job.getId(), false);
        assertThat(getInterimResults(job.getId()).isEmpty(), is(true));
        assertThat(time, equalTo(1400040000L));

        // push some data up to a 1/4 bucket boundary, flush (with interim), check interim results
        String data = "{\"time\":1400040000,\"value\":14}\n"
                + "{\"time\":1400040500,\"value\":12}\n"
                + "{\"time\":1400040510,\"value\":16}\n";
        assertThat(postData(job.getId(), data).getProcessedRecordCount(), equalTo(3L));
        flushJob(job.getId(), true);

        // We might need to retry this while waiting for a refresh
        assertBusy(() -> {
            List<Bucket> firstInterimBuckets = getInterimResults(job.getId());
            assertThat(firstInterimBuckets.size(), equalTo(2));
            assertThat(firstInterimBuckets.get(0).getTimestamp().getTime(), equalTo(1400039000000L));
            assertThat(firstInterimBuckets.get(0).getRecordCount(), equalTo(0));
            assertThat(firstInterimBuckets.get(1).getTimestamp().getTime(), equalTo(1400040000000L));
            assertThat(firstInterimBuckets.get(1).getRecordCount(), equalTo(1));
            assertThat(firstInterimBuckets.get(1).getRecords().get(0).getActual().get(0), equalTo(16.0));
        });

        // push 1 more record, flush (with interim), check same interim result
        data = "{\"time\":1400040520,\"value\":15}\n";
        assertThat(postData(job.getId(), data).getProcessedRecordCount(), equalTo(1L));
        flushJob(job.getId(), true);

        assertBusy(() -> {
            List<Bucket> secondInterimBuckets = getInterimResults(job.getId());
            assertThat(secondInterimBuckets.get(0).getTimestamp().getTime(), equalTo(1400039000000L));
            assertThat(secondInterimBuckets.get(0).getRecordCount(), equalTo(0));
            assertThat(secondInterimBuckets.get(1).getTimestamp().getTime(), equalTo(1400040000000L));
            assertThat(secondInterimBuckets.get(1).getRecordCount(), equalTo(1));
            assertThat(secondInterimBuckets.get(1).getRecords().get(0).getActual().get(0), equalTo(16.0));
        });

        // push rest of data, close, verify no interim results
        time += BUCKET_SPAN_SECONDS;
        assertThat(postData(job.getId(), createData(30, anomalies)).getProcessedRecordCount(), equalTo(30L));
        closeJob(job.getId());
        assertThat(getInterimResults(job.getId()).isEmpty(), is(true));

        // Verify interim results have been replaced with finalized results
        GetBucketsAction.Request bucketRequest = new GetBucketsAction.Request(job.getId());
        bucketRequest.setTimestamp("1400039500000");
        bucketRequest.setExpand(true);
        List<Bucket> bucket = client().execute(GetBucketsAction.INSTANCE, bucketRequest).get().getBuckets().results();
        assertThat(bucket.size(), equalTo(1));
        assertThat(bucket.get(0).getRecords().get(0).getActual().get(0), equalTo(14.0));
    }

    private String createData(int halfBuckets, Map<Long, Integer> timeToValueMap) {
        StringBuilder data = new StringBuilder();
        for (int i = 0; i < halfBuckets; i++) {
            int value = timeToValueMap.getOrDefault(time, randomIntBetween(1, 3));
            data.append("{\"time\":" + time + ", \"value\":" + value + "}\n");
            time += BUCKET_SPAN_SECONDS / 2;
        }
        return data.toString();
    }

    private List<Bucket> getInterimResults(String jobId) {
        GetBucketsAction.Request request = new GetBucketsAction.Request(jobId);
        request.setExpand(true);
        request.setPageParams(new PageParams(0, 1500));
        GetBucketsAction.Response response = client().execute(GetBucketsAction.INSTANCE, request).actionGet();
        assertThat(response.getBuckets().count(), lessThan(1500L));
        List<Bucket> buckets = response.getBuckets().results();
        assertThat(buckets.size(), greaterThan(0));
        return buckets.stream().filter(b -> b.isInterim()).collect(Collectors.toList());
    }
}
