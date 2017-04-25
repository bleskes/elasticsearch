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

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.xpack.ml.job.config.AnalysisConfig;
import org.elasticsearch.xpack.ml.job.config.DataDescription;
import org.elasticsearch.xpack.ml.job.config.Detector;
import org.elasticsearch.xpack.ml.job.config.Job;
import org.elasticsearch.xpack.ml.job.persistence.AnomalyDetectorsIndex;
import org.elasticsearch.xpack.ml.job.results.AnomalyRecord;
import org.junit.After;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * This tests that interim results created before a job was reopened get
 * deleted after new buckets are created.
 */
public class InterimResultsDeletedAfterReopeningJobIT extends MlNativeAutodetectIntegTestCase {

    @After
    public void cleanUpTest() throws Exception {
        cleanUp();
    }

    public void test() throws Exception {
        Detector.Builder detector = new Detector.Builder("mean", "value");
        detector.setByFieldName("by_field");

        AnalysisConfig.Builder analysisConfig = new AnalysisConfig.Builder(
                Arrays.asList(detector.build()));
        analysisConfig.setBucketSpan(TimeValue.timeValueHours(1));
        DataDescription.Builder dataDescription = new DataDescription.Builder();
        Job.Builder job = new Job.Builder("interim-results-deleted-after-reopening-job-test");
        job.setAnalysisConfig(analysisConfig);
        job.setDataDescription(dataDescription);

        registerJob(job);
        putJob(job);
        openJob(job.getId());

        long timestamp = 1491004800000L;
        int totalBuckets = 2 * 24;
        List<String> byFieldValues = Arrays.asList("foo", "bar");
        int normalValue = 1000;
        List<String> data = new ArrayList<>();
        for (int bucket = 0; bucket < totalBuckets; bucket++) {
            for (String byFieldValue : byFieldValues) {
                data.add(createJsonRecord(createRecord(timestamp, byFieldValue, normalValue)));
            }
            timestamp += TimeValue.timeValueHours(1).getMillis();
        }

        data.add(createJsonRecord(createRecord(timestamp, "foo", 1)));
        data.add(createJsonRecord(createRecord(timestamp, "bar", 1)));
        postData(job.getId(), data.stream().collect(Collectors.joining()));
        flushJob(job.getId(), true);
        closeJob(job.getId());

        // We should have 2 interim records
        List<AnomalyRecord> records = getRecords(job.getId());
        assertThat(records.size(), equalTo(2));
        assertThat(records.stream().allMatch(AnomalyRecord::isInterim), is(true));

        // Second batch
        data = new ArrayList<>();

        // This should fix the mean for 'foo'
        data.add(createJsonRecord(createRecord(timestamp, "foo", 2000)));

        // Then advance time and send normal data to force creating final results for previous bucket
        timestamp += TimeValue.timeValueHours(1).getMillis();
        data.add(createJsonRecord(createRecord(timestamp, "foo", normalValue)));
        data.add(createJsonRecord(createRecord(timestamp, "bar", normalValue)));

        openJob(job.getId());
        postData(job.getId(), data.stream().collect(Collectors.joining()));
        closeJob(job.getId());

        records = getRecords(job.getId());
        assertThat(records.size(), equalTo(1));
        assertThat(records.stream().allMatch(AnomalyRecord::isInterim), is(false));

        // No other interim results either
        assertNoInterimResults(job.getId());
    }

    private static Map<String, Object> createRecord(long timestamp, String byFieldValue, int value) {
        Map<String, Object> record = new HashMap<>();
        record.put("time", timestamp);
        record.put("by_field", byFieldValue);
        record.put("value", value);
        return record;
    }

    private static String createJsonRecord(Map<String, Object> keyValueMap) throws IOException {
        return JsonXContent.contentBuilder().map(keyValueMap).string() + "\n";
    }

    private void assertNoInterimResults(String jobId) {
        String indexName = AnomalyDetectorsIndex.jobResultsAliasedName(jobId);
        SearchResponse search = client().prepareSearch(indexName).setTypes("result").setSize(1000)
                .setQuery(QueryBuilders.termQuery("is_interim", true)).get();
        assertThat(search.getHits().getTotalHits(), equalTo(0L));
    }
}
