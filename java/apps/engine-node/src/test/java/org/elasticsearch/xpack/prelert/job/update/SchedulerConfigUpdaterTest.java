package org.elasticsearch.xpack.prelert.job.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.JobConfiguration;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.SchedulerConfig;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.IsEqual.equalTo;

public class SchedulerConfigUpdaterTest extends ESTestCase {

    private JobDetails job;

    public void testUpdate_GivenJobIsNotScheduled() throws IOException {
        givenJob("foo");
        String update = "{}";
        JsonNode node = new ObjectMapper().readTree(update);

        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class, () -> createUpdater().update(node));
        assertEquals("There is no job 'foo' with a scheduler configured", e.getMessage());
        assertEquals(ErrorCodes.NO_SUCH_SCHEDULED_JOB.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testUpdate_GivenNull() throws IOException {
        givenJobWithSchedulerConfig("foo", createSchedulerBuilder());
        String update = "null";
        JsonNode node = new ObjectMapper().readTree(update);

        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class, () -> createUpdater().update(node));
        assertEquals("Invalid update value for schedulerConfig: null", e.getMessage());
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testUpdate_GivenInvalidJson() throws IOException {
        givenJobWithSchedulerConfig("foo", createSchedulerBuilder());
        String update = "{\"dataSour!!ce\":\"whatever\"}";
        JsonNode node = new ObjectMapper().readTree(update);

        ElasticsearchParseException e = expectThrows(ElasticsearchParseException.class, () -> createUpdater().update(node));
        assertEquals("JSON parse error reading the update value for schedulerConfig", e.getMessage());
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testUpdate_GivenDifferentDataSource() throws IOException {
        givenJobWithSchedulerConfig("foo", createSchedulerBuilder());

        String update = "{\"dataSource\":\"FILE\"}";
        JsonNode node = new ObjectMapper().readTree(update);

        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class, () -> createUpdater().update(node));
        assertEquals("Invalid update value for schedulerConfig: dataSource cannot be changed; existing is ELASTICSEARCH, update had FILE", e.getMessage());
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testUpdate_GivenValidationError() throws IOException {
        givenJobWithSchedulerConfig("foo", createSchedulerBuilder());
        String update = "{\"dataSource\":\"ELASTICSEARCH\", \"queryDelay\":\"-10\"}";
        JsonNode node = new ObjectMapper().readTree(update);

        // Original message gets swallowed by jackson-databind when it tries to invoke setQueryDelay(...) via reflection
        // and a valid exception is thrown because the provided value is negative. It is ok, we remove jackson-databind soon...
        ElasticsearchParseException e = expectThrows(ElasticsearchParseException.class, () -> createUpdater().update(node));
        assertEquals("JSON parse error reading the update value for schedulerConfig", e.getMessage());
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testUpdate_GivenValid() throws JobException, IOException {
        givenJobWithSchedulerConfig("foo", createSchedulerBuilder());
        String update = "{"
                + "\"dataSource\":\"ELASTICSEARCH\","
                + "\"baseUrl\":\"http://localhost:9200\","
                + "\"indexes\":[\"index1\", \"index2\"],"
                + "\"types\":[\"type1\", \"type2\"],"
                + "\"query\":{\"term\":{\"airline\":\"AAL\"}},"
                + "\"scrollSize\": 10000"
                + "}";
        JsonNode node = new ObjectMapper().readTree(update);

        createUpdater().update(node);

        SchedulerConfig.Builder expected = createSchedulerBuilder();
        expected.setBaseUrl("http://localhost:9200");
        expected.setIndexes(Arrays.asList("index1", "index2"));
        expected.setTypes(Arrays.asList("type1", "type2"));
        Map<String, Object> query = new HashMap<>();
        Map<String, Object> termQuery = new HashMap<>();
        termQuery.put("airline", "AAL");
        query.put("term", termQuery);
        expected.setQuery(query);
        expected.setQueryDelay(60L);
        expected.setRetrieveWholeSource(false);
        expected.setScrollSize(10000);

        assertThat(job.getSchedulerConfig(), equalTo(expected.build()));
    }

    private SchedulerConfigUpdater createUpdater() {
        return new SchedulerConfigUpdater(job, "schedulerConfig");
    }

    private void givenJob(String jobId) {
        givenJobWithSchedulerConfig(jobId, null);
    }

    private void givenJobWithSchedulerConfig(String jobId, SchedulerConfig.Builder schedulerConfig) {
        JobConfiguration jobConfiguration = new JobConfiguration();
        jobConfiguration.setId(jobId);
        if (schedulerConfig != null) {
            jobConfiguration.setSchedulerConfig(schedulerConfig);
        }
        job = jobConfiguration.build();
    }

    private static SchedulerConfig.Builder createSchedulerBuilder() {
        SchedulerConfig.Builder builder = new SchedulerConfig.Builder(SchedulerConfig.DataSource.ELASTICSEARCH);
        builder.setBaseUrl("http://localhost");
        builder.setIndexes(Collections.singletonList("index"));
        builder.setTypes(Collections.singletonList("type"));
        return builder;
    }
}
