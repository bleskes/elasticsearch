/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;

public class PrelertJobIT extends ESRestTestCase {

    public void testPutJob_GivenFarequoteConfig() throws Exception {
        Response response = createFarequoteJob();

        assertThat(response.getStatusLine().getStatusCode(), equalTo(200));
        String responseAsString = responseEntityToString(response);
        assertThat(responseAsString, containsString("\"jobId\":\"farequote\""));
        assertThat(responseAsString, containsString("\"status\":\"CLOSED\""));
    }

    public void testGetJob_GivenNoSuchJob() throws Exception {
        ResponseException e = expectThrows(ResponseException.class, () -> client().performRequest("get", "engine/v2/jobs/non-existing-job"));

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
        assertThat(responseAsString, containsString("\"status\":\"CLOSED\""));
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

    public void testUpdateJob_GivenFullUpdateSuite() throws Exception {
        String job = "{\n" +
                "    \"id\":\"job-update-test\",\n" +
                "    \"description\":\"Old description\",\n" +
                "    \"analysisConfig\" : {\n" +
                "        \"bucketSpan\":3600,\n" +
                "        \"detectors\" :[{\"function\":\"metric\",\"fieldName\":\"responsetime\",\"byFieldName\":\"airline\"}]\n" +
                "    },\n" +
                "    \"dataDescription\" : {\n" +
                "        \"fieldDelimiter\":\",\",\n" +
                "        \"timeField\":\"time\",\n" +
                "        \"timeFormat\":\"yyyy-MM-dd HH:mm:ssX\",\n" +
                "        \"format\":\"ELASTICSEARCH\"\n" +
                "    },\n" +
                "    \"analysisLimits\":{\"modelMemoryLimit\":1024},\n" +
                "    \"backgroundPersistInterval\":4000,\n" +
                "    \"modelSnapshotRetentionDays\":10,\n" +
                "    \"renormalizationWindowDays\":30,\n" +
                "    \"resultsRetentionDays\":100,\n" +
                "    \"schedulerConfig\":{\"dataSource\":\"ELASTICSEARCH\",\"baseUrl\":\"http://localhost:8080\",\"indexes\":[\"myIndex\"],\"types\":[\"myType\"],\"scrollSize\":2000}\n" +
                "}";

        client().performRequest("post", "engine/v2/jobs", Collections.emptyMap(), new StringEntity(job));

        Response response = client().performRequest("get", "engine/v2/jobs/job-update-test");
        String responseAsString = responseEntityToString(response);
        assertThat(responseAsString, containsString("\"jobId\":\"job-update-test\""));
        assertThat(responseAsString, containsString("\"analysisLimits\":{\"modelMemoryLimit\":1024}"));
        assertThat(responseAsString, containsString("\"backgroundPersistInterval\":4000"));
        assertThat(responseAsString, containsString("\"description\":\"Old description\""));
        assertThat(responseAsString, containsString("\"detectorDescription\":\"metric(responsetime) by airline\""));
        assertThat(responseAsString, containsString("\"modelSnapshotRetentionDays\":10"));
        assertThat(responseAsString, containsString("\"renormalizationWindowDays\":30"));
        assertThat(responseAsString, containsString("\"resultsRetentionDays\":100"));
        assertThat(responseAsString, containsString("\"scrollSize\":2000"));
        assertThat(responseAsString, not(containsString("customSettings")));
        assertThat(responseAsString, not(containsString("ignoreDowntime")));
        assertThat(responseAsString, not(containsString("modelDebugConfig")));

        String update = "{\n" +
                "    \"analysisLimits\":{\"modelMemoryLimit\":2048},\n" +
                "    \"backgroundPersistInterval\":7200,\n" +
                "    \"customSettings\":{\"a\":1},\n" +
                "    \"description\":\"New description\",\n" +
                "    \"detectors\":[{\"index\":0,\"description\":\"Ipanema\"}],\n" +
                "    \"ignoreDowntime\":\"ONCE\",\n" +
                "    \"modelDebugConfig\":{\"boundsPercentile\":95.0},\n" +
                "    \"modelSnapshotRetentionDays\":20,\n" +
                "    \"renormalizationWindowDays\":60,\n" +
                "    \"resultsRetentionDays\":50,\n" +
                "    \"schedulerConfig\":{\"dataSource\":\"ELASTICSEARCH\",\"baseUrl\":\"http://localhost:8080\",\"indexes\":[\"myIndex\"],\"types\":[\"myType\"],\"scrollSize\":10000}\n" +
                "}";
        response = client().performRequest("put", "engine/v2/jobs/job-update-test/update", Collections.emptyMap(), new StringEntity(update));
        assertThat(responseEntityToString(response), equalTo("{\"acknowledged\":true}"));

        response = client().performRequest("get", "engine/v2/jobs/job-update-test");
        responseAsString = responseEntityToString(response);
        assertThat(responseAsString, containsString("\"jobId\":\"job-update-test\""));
        assertThat(responseAsString, containsString("\"analysisLimits\":{\"modelMemoryLimit\":2048}"));
        assertThat(responseAsString, containsString("\"backgroundPersistInterval\":7200"));
        assertThat(responseAsString, containsString("\"customSettings\":{\"a\":1}"));
        assertThat(responseAsString, containsString("\"description\":\"New description\""));
        assertThat(responseAsString, containsString("\"detectorDescription\":\"Ipanema\""));
        assertThat(responseAsString, containsString("\"ignoreDowntime\":\"ONCE\""));
        assertThat(responseAsString, containsString("\"modelDebugConfig\":{\"boundsPercentile\":95.0}"));
        assertThat(responseAsString, containsString("\"modelSnapshotRetentionDays\":20"));
        assertThat(responseAsString, containsString("\"renormalizationWindowDays\":60"));
        assertThat(responseAsString, containsString("\"resultsRetentionDays\":50"));
        assertThat(responseAsString, containsString("\"scrollSize\":10000"));
    }

    public void testUpdateJob_GivenUnknownJob() throws Exception {
        String update = "{\n" +
                "    \"description\":\"New description\"\n" +
                "}";
        ResponseException e = expectThrows(ResponseException.class,
                () -> client().performRequest("put", "engine/v2/jobs/unknown-job/update", Collections.emptyMap(), new StringEntity(update)));

        assertThat(e.getResponse().getStatusLine().getStatusCode(), equalTo(404));
        assertThat(e.getMessage(), containsString("\"reason\":\"No known job with id 'unknown-job'"));
        assertThat(e.getMessage(), containsString("\"errorCode\":\"20101"));
    }

    public void testUpdateJob_GivenInvalidKey() throws Exception {
        createFarequoteJob();

        String update = "{\n" +
                "    \"invalid\":\"New description\"\n" +
                "}";
        ResponseException e = expectThrows(ResponseException.class,
                () -> client().performRequest("put", "engine/v2/jobs/farequote/update", Collections.emptyMap(), new StringEntity(update)));

        assertThat(e.getResponse().getStatusLine().getStatusCode(), equalTo(400));
        assertThat(e.getMessage(), containsString("\"reason\":\"Invalid key 'invalid'"));
        assertThat(e.getMessage(), containsString("\"errorCode\":\"10115"));
    }

    public void testUpdateJob_GivenInvalidUpdate_ShouldFailWithoutApplyingAnyUpdate() throws Exception {
        createFarequoteJob();
        String update = "{\n" +
                "    \"description\":\"New description\"\n," +
                "    \"detectors\":[{\"index\":42,\"description\":\"blah\"}]\n" +
                "}";
        ResponseException e = expectThrows(ResponseException.class,
                () -> client().performRequest("put", "engine/v2/jobs/farequote/update", Collections.emptyMap(), new StringEntity(update)));

        assertThat(e.getResponse().getStatusLine().getStatusCode(), equalTo(400));
        assertThat(e.getMessage(), containsString("\"reason\":\"Invalid index: valid range is [0, 0]; actual was: 42"));

        Response response = client().performRequest("get", "engine/v2/jobs/farequote");
        String responseAsString = responseEntityToString(response);
        assertThat(responseAsString, containsString("\"jobId\":\"farequote\""));

        // NORELEASE the below assertion won't work until we fix the job immutability issue.
        // assertThat(responseAsString, containsString("\"description\":\"Analysis of response time by airline\""));
    }

    private Response createFarequoteJob() throws Exception {
        return createFarequoteJob("farequote");
    }

    private Response createFarequoteJob(String jobId) throws Exception {
        String job = "{\n" +
                "    \"id\":\"" + jobId + "\",\n" +
                "    \"description\":\"Analysis of response time by airline\",\n" +
                "    \"analysisConfig\" : {\n" +
                "        \"bucketSpan\":3600,\n" +
                "        \"detectors\" :[{\"function\":\"metric\",\"fieldName\":\"responsetime\",\"byFieldName\":\"airline\"}]\n" +
                "    },\n" +
                "    \"dataDescription\" : {\n" +
                "        \"fieldDelimiter\":\",\",\n" +
                "        \"timeField\":\"time\",\n" +
                "        \"timeFormat\":\"yyyy-MM-dd HH:mm:ssX\"\n" +
                "    }\n" +
                "}";

        return client().performRequest("post", "engine/v2/jobs", Collections.emptyMap(), new StringEntity(job));
    }

    public void testGetBucketResults() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("start", "2016-06-01T00:00:00Z"); // inclusive
        params.put("end", "2016-06-04T00:00:00Z"); // exclusive

        ResponseException e = expectThrows(ResponseException.class, () ->
        client().performRequest("get", "/engine/v2/results/1/buckets", params));
        assertThat(e.getResponse().getStatusLine().getStatusCode(), equalTo(404));
        assertThat(e.getMessage(), containsString("No known job with id '1'"));
        assertThat(e.getMessage(), containsString("\"errorCode\":\"20101"));

        addBucketResult("1", "2016-06-01T00:00:00Z");
        addBucketResult("1", "2016-06-02T00:00:00Z");
        addBucketResult("1", "2016-06-03T00:00:00Z");
        Response response = client().performRequest("get", "/engine/v2/results/1/buckets", params);
        assertThat(response.getStatusLine().getStatusCode(), equalTo(200));
        String responseAsString = responseEntityToString(response);
        assertThat(responseAsString, containsString("\"hitCount\":3"));

        params.put("end", "2016-06-02T00:00:00Z");
        response = client().performRequest("get", "/engine/v2/results/1/buckets", params);
        assertThat(response.getStatusLine().getStatusCode(), equalTo(200));
        responseAsString = responseEntityToString(response);
        assertThat(responseAsString, containsString("\"hitCount\":1"));

        e = expectThrows(ResponseException.class, () ->
        client().performRequest("get", "/engine/v2/results/2/bucket/2016-06-01T00:00:00Z"));
        assertThat(e.getResponse().getStatusLine().getStatusCode(), equalTo(404));
        assertThat(e.getMessage(), containsString("No known job with id '2'"));
        assertThat(e.getMessage(), containsString("\"errorCode\":\"20101"));

        e = expectThrows(ResponseException.class,
                () -> client().performRequest("get", "/engine/v2/results/1/bucket/2015-06-01T00:00:00Z"));
        assertThat(e.getResponse().getStatusLine().getStatusCode(), equalTo(404));
        responseAsString = responseEntityToString(e.getResponse());
        assertThat(responseAsString, equalTo("{\"exists\":false,\"type\":\"bucket\"}"));

        response = client().performRequest("get", "/engine/v2/results/1/bucket/2016-06-01T00:00:00Z");
        assertThat(response.getStatusLine().getStatusCode(), equalTo(200));
        responseAsString = responseEntityToString(response);
        assertThat(responseAsString, not(isEmptyString()));
    }

    private Response addBucketResult(String jobId, String timestamp) throws Exception {
        String createIndexBody = "{ \"mappings\": {\"bucket\": { \"properties\": { \"@timestamp\": { \"type\" : \"date\" } } } } }";
        try {
            client().performRequest("put", "prelertresults-" + jobId, Collections.emptyMap(),
                    new StringEntity(createIndexBody));
        } catch (ResponseException e) {
            // it is ok: the index already exists
            assertThat(e.getMessage(), containsString("index_already_exists_exception"));
            assertThat(e.getResponse().getStatusLine().getStatusCode(), equalTo(400));
        }


        String bucketResult = "{\"@timestamp\": \"" + timestamp + "\"}";
        return client().performRequest("put", "prelertresults-" + jobId + "/bucket/" + timestamp,
                Collections.singletonMap("refresh", "true"), new StringEntity(bucketResult));
    }

    private static String responseEntityToString(Response response) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    @After
    public void clearPrelertState() throws IOException {
        adminClient().performRequest("DELETE", "/engine/v2/clear");
    }
}
