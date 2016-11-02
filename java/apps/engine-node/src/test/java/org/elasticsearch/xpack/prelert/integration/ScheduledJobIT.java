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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

public class ScheduledJobIT extends ESRestTestCase {

    public void testStartJobScheduler_GivenMissingJob() {
        ResponseException e = expectThrows(ResponseException.class,
                () -> client().performRequest("post", "engine/v2/schedulers/invalid-job/start"));
        assertThat(e.getResponse().getStatusLine().getStatusCode(), equalTo(404));
    }

    public void testStartJobScheduler_GivenNonScheduledJob() throws Exception {
        createNonScheduledJob();

        ResponseException e = expectThrows(ResponseException.class,
                () -> client().performRequest("post", "engine/v2/schedulers/non-scheduled/start"));
        assertThat(e.getResponse().getStatusLine().getStatusCode(), equalTo(400));
        String responseAsString = responseEntityToString(e.getResponse());
        assertThat(responseAsString, containsString("\"reason\":\"There is no job 'non-scheduled' with a scheduler configured\""));
        assertThat(responseAsString, containsString("\"errorCode\":\"60116"));
    }

    public void testStartJobScheduler_GivenInvalidStartParam() throws Exception {
        createScheduledJob();

        ResponseException e = expectThrows(ResponseException.class,
                () -> client().performRequest("post", "engine/v2/schedulers/scheduled/start?start=not-a-date"));
        assertThat(e.getResponse().getStatusLine().getStatusCode(), equalTo(400));
        String responseAsString = responseEntityToString(e.getResponse());
        assertThat(responseAsString, containsString(
                "\"reason\":\"Query param 'start' with value 'not-a-date' cannot be parsed as a date or converted to a number (epoch).\""));
        assertThat(responseAsString, containsString("\"errorCode\":\"60101"));
    }

    public void testStartJobScheduler_GivenInvalidEndParam() throws Exception {
        createScheduledJob();

        ResponseException e = expectThrows(ResponseException.class,
                () -> client().performRequest("post", "engine/v2/schedulers/scheduled/start?end=not-a-date"));
        assertThat(e.getResponse().getStatusLine().getStatusCode(), equalTo(400));
        String responseAsString = responseEntityToString(e.getResponse());
        assertThat(responseAsString, containsString(
                "\"reason\":\"Query param 'end' with value 'not-a-date' cannot be parsed as a date or converted to a number (epoch).\""));
        assertThat(responseAsString, containsString("\"errorCode\":\"60101"));
    }

    public void testStartJobScheduler_GivenLookbackOnly() throws Exception {
        createAirlineDataIndex();
        createScheduledJob();

        Response response = client().performRequest("post",
                "engine/v2/schedulers/scheduled/start?start=2016-06-01T00:00:00Z&end=2016-06-02T00:00:00Z");
        assertThat(response.getStatusLine().getStatusCode(), equalTo(200));
        assertThat(responseEntityToString(response), equalTo("{\"acknowledged\":true}"));

        assertBusy(() -> {
            try {
                Response response2 = client().performRequest("get", "/_cluster/state",
                        Collections.singletonMap("filter_path", "metadata.prelert.allocations.scheduler_state"));
                assertThat(responseEntityToString(response2), containsString("\"status\":\"STARTED\""));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        waitForSchedulerToBeStopped();
    }

    public void testStartJobScheduler_GivenRealtime() throws Exception {
        createAirlineDataIndex();
        createScheduledJob();

        Response response = client().performRequest("post", "engine/v2/schedulers/scheduled/start?start=2016-06-01T00:00:00Z");
        assertThat(response.getStatusLine().getStatusCode(), equalTo(200));
        assertThat(responseEntityToString(response), equalTo("{\"acknowledged\":true}"));

        assertBusy(() -> {
            try {
                Response response2 = client().performRequest("get", "/_cluster/state",
                        Collections.singletonMap("filter_path", "metadata.prelert.allocations.scheduler_state"));
                assertThat(responseEntityToString(response2), containsString("\"status\":\"STARTED\""));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        response = client().performRequest("post", "engine/v2/schedulers/scheduled/stop");
        assertThat(response.getStatusLine().getStatusCode(), equalTo(200));
        assertThat(responseEntityToString(response), equalTo("{\"acknowledged\":true}"));

        waitForSchedulerToBeStopped();

    }

    private void createAirlineDataIndex() throws Exception {
        String airlineDataMappings = "{" + "  \"mappings\": {" + "    \"response\": {" + "      \"properties\": {"
                + "        \"time\": { \"type\":\"date\"}," + "        \"airline\": { \"type\":\"keyword\"},"
                + "        \"responsetime\": { \"type\":\"float\"}" + "      }" + "    }" + "  }" + "}";
        client().performRequest("put", "airline-data", Collections.emptyMap(), new StringEntity(airlineDataMappings));

        client().performRequest("put", "airline-data/response/1", Collections.emptyMap(),
                new StringEntity("{\"time\":\"2016-10-01T00:00:00Z\",\"airline\":\"AAA\",\"responsetime\":135.22}"));
        client().performRequest("put", "airline-data/response/2", Collections.emptyMap(),
                new StringEntity("{\"time\":\"2016-10-01T01:59:00Z\",\"airline\":\"AAA\",\"responsetime\":541.76}"));

        client().performRequest("post", "airline-data/_refresh");
    }

    private Response createNonScheduledJob() throws Exception {
        String job = "{\n" + "    \"id\":\"non-scheduled\",\n" + "    \"description\":\"Analysis of response time by airline\",\n"
                + "    \"analysisConfig\" : {\n" + "        \"bucketSpan\":3600,\n"
                + "        \"detectors\" :[{\"function\":\"mean\",\"fieldName\":\"responsetime\",\"byFieldName\":\"airline\"}]\n"
                + "    },\n" + "    \"dataDescription\" : {\n" + "        \"fieldDelimiter\":\",\",\n" + "        \"timeField\":\"time\",\n"
                + "        \"timeFormat\":\"yyyy-MM-dd'T'HH:mm:ssX\"\n" + "    }\n" + "}";

        return client().performRequest("post", "engine/v2/jobs", Collections.emptyMap(), new StringEntity(job));
    }

    private Response createScheduledJob() throws Exception {
        String job = "{\n" + "    \"id\":\"scheduled\",\n" + "    \"description\":\"Analysis of response time by airline\",\n"
                + "    \"analysisConfig\" : {\n" + "        \"bucketSpan\":3600,\n"
                + "        \"detectors\" :[{\"function\":\"mean\",\"fieldName\":\"responsetime\",\"byFieldName\":\"airline\"}]\n"
                + "    },\n" + "    \"dataDescription\" : {\n" + "        \"format\":\"ELASTICSEARCH\",\n"
                + "        \"timeField\":\"time\",\n" + "        \"timeFormat\":\"yyyy-MM-dd'T'HH:mm:ssX\"\n" + "    },\n"
                + "    \"schedulerConfig\" : {\n" + "        \"dataSource\":\"ELASTICSEARCH\",\n"
                + "        \"baseUrl\":\"http://localhost:8080\",\n" + "        \"indexes\":[\"airline-data\"],\n"
                + "        \"types\":[\"response\"],\n" + "        \"retrieveWholeSource\":true\n" + "    }\n" + "}";

        return client().performRequest("post", "engine/v2/jobs", Collections.emptyMap(), new StringEntity(job));
    }

    private static String responseEntityToString(Response response) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private void waitForSchedulerToBeStopped() throws Exception {
        assertBusy(() -> {
            try {
                Response response = client().performRequest("get", "/_cluster/state",
                        Collections.singletonMap("filter_path", "metadata.prelert.allocations.scheduler_state"));
                assertThat(responseEntityToString(response), containsString("\"status\":\"STOPPED\""));
            } catch (Exception e) {
                fail();
            }
        }, 1500, TimeUnit.MILLISECONDS);
    }

    @After
    public void clearPrelertState() throws IOException {
        adminClient().performRequest("DELETE", "/engine/v2/clear");
    }
}
