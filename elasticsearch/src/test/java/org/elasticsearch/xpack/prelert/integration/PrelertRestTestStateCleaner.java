/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
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
package org.elasticsearch.xpack.prelert.integration;

import org.elasticsearch.client.RestClient;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.test.rest.ESRestTestCase;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PrelertRestTestStateCleaner {

    private final RestClient client;
    private final ESRestTestCase testCase;

    public PrelertRestTestStateCleaner(RestClient client, ESRestTestCase testCase) {
        this.client = client;
        this.testCase = testCase;
    }

    public void clearPrelertMetadata() throws IOException {
        deleteAllSchedulers();
        deleteAllJobs();
    }

    @SuppressWarnings("unchecked")
    private void deleteAllSchedulers() throws IOException {
        Map<String, Object> clusterStateAsMap = testCase.entityAsMap(client.performRequest("GET", "/_cluster/state",
                Collections.singletonMap("filter_path", "metadata.prelert.schedulers")));
        List<Map<String, Object>> schedulers =
                (List<Map<String, Object>>) XContentMapValues.extractValue("metadata.prelert.schedulers", clusterStateAsMap);
        if (schedulers == null) {
            return;
        }

        for (Map<String, Object> scheduler : schedulers) {
            Map<String, Object> schedulerMap = (Map<String, Object>) scheduler.get("config");
            String schedulerId = (String) schedulerMap.get("scheduler_id");
            try {
                client.performRequest("POST", "/_xpack/prelert/schedulers/" + schedulerId + "/_stop");
            } catch (Exception e) {
                // ignore
            }
            client.performRequest("DELETE", "/_xpack/prelert/schedulers/" + schedulerId);
        }
    }

    private void deleteAllJobs() throws IOException {
        Map<String, Object> clusterStateAsMap = testCase.entityAsMap(client.performRequest("GET", "/_cluster/state",
                Collections.singletonMap("filter_path", "metadata.prelert.jobs")));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> jobConfigs =
                (List<Map<String, Object>>) XContentMapValues.extractValue("metadata.prelert.jobs", clusterStateAsMap);
        if (jobConfigs == null) {
            return;
        }

        for (Map<String, Object> jobConfig : jobConfigs) {
            String jobId = (String) jobConfig.get("job_id");
            try {
                client.performRequest("POST", "/_xpack/prelert/anomaly_detectors/" + jobId + "/_close");
            } catch (Exception e) {
                // ignore
            }
            client.performRequest("DELETE", "/_xpack/prelert/anomaly_detectors/" + jobId);
        }
    }
}
