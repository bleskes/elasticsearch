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
package org.elasticsearch.xpack.prelert.integration;

import org.apache.http.entity.StringEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.test.rest.ESRestTestCase;
import org.junit.After;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;

public class PrelertJobIT extends ESRestTestCase {

    public void testPutJob_GivenFarequoteConfig() throws Exception {
        Response response = createFarequoteJob();

        assertThat(response.getStatusLine().getStatusCode(), equalTo(200));
        String responseAsString = responseEntityToString(response);
        assertThat(responseAsString, containsString("\"jobId\":\"farequote\""));
    }

    public void testGetJob_GivenNoSuchJob() throws Exception {
        ResponseException e = expectThrows(ResponseException.class,
                () -> client().performRequest("get", "engine/v2/jobs/non-existing-job"));

        assertThat(e.getResponse().getStatusLine().getStatusCode(), equalTo(404));
        assertThat(e.getMessage(), containsString("\"exists\":false"));
        assertThat(e.getMessage(), containsString("\"type\":\"job\""));
    }

    public void testGetJob_GivenJobExists() throws Exception {
        createFarequoteJob();

        Response response = client().performRequest("get", "engine/v2/jobs/farequote");

        assertThat(response.getStatusLine().getStatusCode(), equalTo(200));
        String responseAsString = responseEntityToString(response);
        assertThat(responseAsString, containsString("\"exists\":true"));
        assertThat(responseAsString, containsString("\"type\":\"job\""));
        assertThat(responseAsString, containsString("\"jobId\":\"farequote\""));
    }

    public void testGetJobs_GivenNegativeSkip() throws Exception {
        ResponseException e = expectThrows(ResponseException.class, () -> client().performRequest("get", "engine/v2/jobs?skip=-1"));

        assertThat(e.getResponse().getStatusLine().getStatusCode(), equalTo(400));
        assertThat(e.getMessage(), containsString("\"reason\":\"Parameter [skip] cannot be < 0\""));
        assertThat(e.getMessage(), containsString("\"errorCode\":\"60110"));
    }

    public void testGetJobs_GivenNegativeTake() throws Exception {
        ResponseException e = expectThrows(ResponseException.class, () -> client().performRequest("get", "engine/v2/jobs?take=-1"));

        assertThat(e.getResponse().getStatusLine().getStatusCode(), equalTo(400));
        assertThat(e.getMessage(), containsString("\"reason\":\"Parameter [take] cannot be < 0\""));
        assertThat(e.getMessage(), containsString("\"errorCode\":\"60111"));
    }

    public void testGetJobs_GivenSkipAndTakeSumTo10001() throws Exception {
        ResponseException e = expectThrows(ResponseException.class,
                () -> client().performRequest("get", "engine/v2/jobs?skip1000&take=11001"));

        assertThat(e.getResponse().getStatusLine().getStatusCode(), equalTo(400));
        assertThat(e.getMessage(), containsString("\"reason\":\"The sum of parameters [skip] and [take] cannot be higher than 10000."));
        assertThat(e.getMessage(), containsString("\"errorCode\":\"60111"));
    }

    public void testGetJobs_GivenSingleJob() throws Exception {
        createFarequoteJob();

        Response response = client().performRequest("get", "engine/v2/jobs");

        assertThat(response.getStatusLine().getStatusCode(), equalTo(200));
        String responseAsString = responseEntityToString(response);
        assertThat(responseAsString, containsString("\"hitCount\":1"));
        assertThat(responseAsString, containsString("\"jobId\":\"farequote\""));
    }

    public void testGetJobs_GivenMultipleJobs() throws Exception {
        createFarequoteJob("farequote_1");
        createFarequoteJob("farequote_2");
        createFarequoteJob("farequote_3");

        Response response = client().performRequest("get", "engine/v2/jobs");

        assertThat(response.getStatusLine().getStatusCode(), equalTo(200));
        String responseAsString = responseEntityToString(response);
        assertThat(responseAsString, containsString("\"hitCount\":3"));
        assertThat(responseAsString, containsString("\"jobId\":\"farequote_1\""));
        assertThat(responseAsString, containsString("\"jobId\":\"farequote_2\""));
        assertThat(responseAsString, containsString("\"jobId\":\"farequote_3\""));
    }

    public void testGetJobs_GivenMultipleJobsAndSkipIsOne() throws Exception {
        createFarequoteJob("farequote_1");
        createFarequoteJob("farequote_2");
        createFarequoteJob("farequote_3");

        Response response = client().performRequest("get", "engine/v2/jobs?skip=1");

        assertThat(response.getStatusLine().getStatusCode(), equalTo(200));
        String responseAsString = responseEntityToString(response);
        assertThat(responseAsString, containsString("\"hitCount\":3"));
        assertThat(responseAsString, not(containsString("\"jobId\":\"farequote_1\"")));
        assertThat(responseAsString, containsString("\"jobId\":\"farequote_2\""));
        assertThat(responseAsString, containsString("\"jobId\":\"farequote_3\""));
    }

    public void testGetJobs_GivenMultipleJobsAndTakeIsOne() throws Exception {
        createFarequoteJob("farequote_1");
        createFarequoteJob("farequote_2");
        createFarequoteJob("farequote_3");

        Response response = client().performRequest("get", "engine/v2/jobs?take=1");

        assertThat(response.getStatusLine().getStatusCode(), equalTo(200));
        String responseAsString = responseEntityToString(response);
        assertThat(responseAsString, containsString("\"hitCount\":3"));
        assertThat(responseAsString, containsString("\"jobId\":\"farequote_1\""));
        assertThat(responseAsString, not(containsString("\"jobId\":\"farequote_2\"")));
        assertThat(responseAsString, not(containsString("\"jobId\":\"farequote_3\"")));
    }

    public void testGetJobs_GivenMultipleJobsAndSkipIsOneAndTakeIsOne() throws Exception {
        createFarequoteJob("farequote_1");
        createFarequoteJob("farequote_2");
        createFarequoteJob("farequote_3");

        Response response = client().performRequest("get", "engine/v2/jobs?skip=1&take=1");

        assertThat(response.getStatusLine().getStatusCode(), equalTo(200));
        String responseAsString = responseEntityToString(response);
        assertThat(responseAsString, containsString("\"hitCount\":3"));
        assertThat(responseAsString, not(containsString("\"jobId\":\"farequote_1\"")));
        assertThat(responseAsString, containsString("\"jobId\":\"farequote_2\""));
        assertThat(responseAsString, not(containsString("\"jobId\":\"farequote_3\"")));
    }

    private Response createFarequoteJob() throws Exception {
        return createFarequoteJob("farequote");
    }

    private Response createFarequoteJob(String jobId) throws Exception {
        String job = "{\n" + "    \"jobId\":\"" + jobId + "\",\n" + "    \"description\":\"Analysis of response time by airline\",\n"
                + "    \"analysisConfig\" : {\n" + "        \"bucketSpan\":3600,\n"
                + "        \"detectors\" :[{\"function\":\"metric\",\"fieldName\":\"responsetime\",\"byFieldName\":\"airline\"}]\n"
                + "    },\n" + "    \"dataDescription\" : {\n" + "        \"fieldDelimiter\":\",\",\n" + "        \"timeField\":\"time\",\n"
                + "        \"timeFormat\":\"yyyy-MM-dd HH:mm:ssX\"\n" + "    }\n" + "}";

        return client().performRequest("post", "engine/v2/jobs", Collections.emptyMap(), new StringEntity(job));
    }

    public void testGetBucketResults() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("start", "1200"); // inclusive
        params.put("end", "1400"); // exclusive

        ResponseException e = expectThrows(ResponseException.class,
                () -> client().performRequest("get", "/engine/v2/results/1/buckets", params));
        assertThat(e.getResponse().getStatusLine().getStatusCode(), equalTo(404));
        assertThat(e.getMessage(), containsString("No known job with id '1'"));
        assertThat(e.getMessage(), containsString("\"errorCode\":\"20101"));

        addBucketResult("1", "1234");
        addBucketResult("1", "1235");
        addBucketResult("1", "1236");
        Response response = client().performRequest("get", "/engine/v2/results/1/buckets", params);
        assertThat(response.getStatusLine().getStatusCode(), equalTo(200));
        String responseAsString = responseEntityToString(response);
        assertThat(responseAsString, containsString("\"hitCount\":3"));

        params.put("end", "1235");
        response = client().performRequest("get", "/engine/v2/results/1/buckets", params);
        assertThat(response.getStatusLine().getStatusCode(), equalTo(200));
        responseAsString = responseEntityToString(response);
        assertThat(responseAsString, containsString("\"hitCount\":1"));

        e = expectThrows(ResponseException.class, () -> client().performRequest("get", "/engine/v2/results/2/bucket/1234"));
        assertThat(e.getResponse().getStatusLine().getStatusCode(), equalTo(404));
        assertThat(e.getMessage(), containsString("No known job with id '2'"));
        assertThat(e.getMessage(), containsString("\"errorCode\":\"20101"));

        e = expectThrows(ResponseException.class, () -> client().performRequest("get", "/engine/v2/results/1/bucket/1"));
        assertThat(e.getResponse().getStatusLine().getStatusCode(), equalTo(404));
        responseAsString = responseEntityToString(e.getResponse());
        assertThat(responseAsString, equalTo("{\"exists\":false,\"type\":\"bucket\"}"));

        response = client().performRequest("get", "/engine/v2/results/1/bucket/1234");
        assertThat(response.getStatusLine().getStatusCode(), equalTo(200));
        responseAsString = responseEntityToString(response);
        assertThat(responseAsString, not(isEmptyString()));
    }

    public void testPauseAndResumeJob() throws Exception {
        createFarequoteJob();

        client().performRequest("post", "engine/v2/jobs/farequote/pause");

        assertBusy(() -> {
            try {
                Response response = client().performRequest("get", "engine/v2/jobs/farequote");
                String responseEntityToString = responseEntityToString(response);
                assertThat(responseEntityToString, containsString("\"ignoreDowntime\":\"ONCE\""));
            } catch (Exception e) {
                fail();
            }
        }, 2, TimeUnit.SECONDS);

        client().performRequest("post", "engine/v2/jobs/farequote/resume");

        ResponseException e = expectThrows(ResponseException.class,
                () -> client().performRequest("post", "engine/v2/jobs/farequote/resume"));

        assertThat(e.getResponse().getStatusLine().getStatusCode(), equalTo(400));
        assertThat(e.getMessage(), containsString("Cannot resume job 'farequote' while its status is CLOSED"));
        assertThat(e.getMessage(), containsString("\"errorCode\":\"60124"));
    }

    public void testPauseJob_GivenJobIsPaused() throws Exception {
        createFarequoteJob();

        client().performRequest("post", "engine/v2/jobs/farequote/pause");

        ResponseException e = expectThrows(ResponseException.class,
                () -> client().performRequest("post", "engine/v2/jobs/farequote/pause"));

        assertThat(e.getResponse().getStatusLine().getStatusCode(), equalTo(400));
        assertThat(e.getMessage(), containsString("Cannot pause job 'farequote' while its status is PAUSED"));
        assertThat(e.getMessage(), containsString("\"errorCode\":\"60123"));
    }

    public void testResumeJob_GivenJobIsClosed() throws Exception {
        createFarequoteJob();

        ResponseException e = expectThrows(ResponseException.class,
                () -> client().performRequest("post", "engine/v2/jobs/farequote/resume"));

        assertThat(e.getResponse().getStatusLine().getStatusCode(), equalTo(400));
        assertThat(e.getMessage(), containsString("Cannot resume job 'farequote' while its status is CLOSED"));
        assertThat(e.getMessage(), containsString("\"errorCode\":\"60124"));
    }

    private Response addBucketResult(String jobId, String timestamp) throws Exception {
        String createIndexBody = "{ \"mappings\": {\"bucket\": { \"properties\": { \"timestamp\": { \"type\" : \"date\" } } } } }";
        try {
            client().performRequest("put", "prelertresults-" + jobId, Collections.emptyMap(), new StringEntity(createIndexBody));
        } catch (ResponseException e) {
            // it is ok: the index already exists
            assertThat(e.getMessage(), containsString("index_already_exists_exception"));
            assertThat(e.getResponse().getStatusLine().getStatusCode(), equalTo(400));
        }

        String bucketResult = "{\"timestamp\": \"" + timestamp + "\"}";
        return client().performRequest("put", "prelertresults-" + jobId + "/bucket/" + timestamp,
                Collections.singletonMap("refresh", "true"), new StringEntity(bucketResult));
    }

    private static String responseEntityToString(Response response) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    @After
    public void clearPrelertState() throws IOException {
        adminClient().performRequest("DELETE", "/engine/v2/clear");
    }
}
