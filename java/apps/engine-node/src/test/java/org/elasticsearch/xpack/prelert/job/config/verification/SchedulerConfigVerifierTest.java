
package org.elasticsearch.xpack.prelert.job.config.verification;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.SchedulerConfig;
import org.elasticsearch.xpack.prelert.job.SchedulerConfig.DataSource;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SchedulerConfigVerifierTest extends ESTestCase {

    public void testCheckValidFile_AllOk() {
        SchedulerConfig.Builder conf = new SchedulerConfig.Builder(DataSource.FILE);
        conf.setFilePath("myfile.csv");

        SchedulerConfigVerifier.verify(conf);
    }


    public void testCheckValidFile_NoPath() {
        SchedulerConfig.Builder conf = new SchedulerConfig.Builder(DataSource.FILE);

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> SchedulerConfigVerifier.verify(conf));

        assertEquals(ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_INVALID_OPTION_VALUE, "filePath", "null"), e.getMessage());
    }


    public void testCheckValidFile_EmptyPath() {
        SchedulerConfig.Builder conf = new SchedulerConfig.Builder(DataSource.FILE);
        conf.setFilePath("");

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> SchedulerConfigVerifier.verify(conf));

        assertEquals(ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_INVALID_OPTION_VALUE, "filePath", ""), e.getMessage());
    }


    public void testCheckValidFile_InappropriateField() {
        SchedulerConfig.Builder conf = new SchedulerConfig.Builder(DataSource.FILE);
        conf.setFilePath("myfile.csv");
        conf.setBaseUrl("http://localhost:9200/");

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> SchedulerConfigVerifier.verify(conf));

        assertEquals(ErrorCodes.SCHEDULER_FIELD_NOT_SUPPORTED_FOR_DATASOURCE.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_FIELD_NOT_SUPPORTED, "baseUrl", DataSource.FILE),
                e.getMessage());
    }


    public void testCheckValidElasticsearch_AllOk() throws IOException {
        SchedulerConfig.Builder conf = new SchedulerConfig.Builder(DataSource.ELASTICSEARCH);
        conf.setQueryDelay(90L);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myindex"));
        conf.setTypes(Arrays.asList("mytype"));
        ObjectMapper mapper = new ObjectMapper();
        conf.setQuery(mapper.readValue("{ \"match_all\" : {} }", new TypeReference<Map<String, Object>>() {
        }));
        conf.setScrollSize(2000);

        assertTrue(SchedulerConfigVerifier.verify(conf));
    }


    public void testCheckValidElasticsearch_WithUsernameAndPassword() throws IOException {
        SchedulerConfig.Builder conf = new SchedulerConfig.Builder(DataSource.ELASTICSEARCH);
        conf.setQueryDelay(90L);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myindex"));
        conf.setTypes(Arrays.asList("mytype"));
        conf.setUsername("dave");
        conf.setPassword("secret");
        ObjectMapper mapper = new ObjectMapper();
        conf.setQuery(mapper.readValue("{ \"match_all\" : {} }", new TypeReference<Map<String, Object>>() {
        }));

        assertTrue(SchedulerConfigVerifier.verify(conf));
    }


    public void testCheckValidElasticsearch_WithUsernameAndEncryptedPassword() throws IOException {
        SchedulerConfig.Builder conf = new SchedulerConfig.Builder(DataSource.ELASTICSEARCH);
        conf.setQueryDelay(90L);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myindex"));
        conf.setTypes(Arrays.asList("mytype"));
        conf.setUsername("dave");
        conf.setEncryptedPassword("already_encrypted");
        ObjectMapper mapper = new ObjectMapper();
        conf.setQuery(mapper.readValue("{ \"match_all\" : {} }", new TypeReference<Map<String, Object>>() {
        }));

        assertTrue(SchedulerConfigVerifier.verify(conf));
    }


    public void testCheckValidElasticsearch_WithPasswordNoUsername() {
        SchedulerConfig.Builder conf = new SchedulerConfig.Builder(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myindex"));
        conf.setTypes(Arrays.asList("mytype"));
        conf.setPassword("secret");

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> SchedulerConfigVerifier.verify(conf));

        assertEquals(ErrorCodes.SCHEDULER_INCOMPLETE_CREDENTIALS.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_INCOMPLETE_CREDENTIALS), e.getMessage());
    }


    public void testCheckValidElasticsearch_BothPasswordAndEncryptedPassword() {
        SchedulerConfig.Builder conf = new SchedulerConfig.Builder(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myindex"));
        conf.setTypes(Arrays.asList("mytype"));
        conf.setUsername("dave");
        conf.setPassword("secret");
        conf.setEncryptedPassword("already_encrypted");

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> SchedulerConfigVerifier.verify(conf));

        assertEquals(ErrorCodes.SCHEDULER_MULTIPLE_PASSWORDS.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_MULTIPLE_PASSWORDS), e.getMessage());
    }


    public void testCheckValidElasticsearch_NoQuery() {
        SchedulerConfig.Builder conf = new SchedulerConfig.Builder(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myindex"));
        conf.setTypes(Arrays.asList("mytype"));

        assertTrue(SchedulerConfigVerifier.verify(conf));
    }


    public void testCheckValidElasticsearch_InappropriateField() throws IOException {
        SchedulerConfig.Builder conf = new SchedulerConfig.Builder(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myindex"));
        conf.setTypes(Arrays.asList("mytype"));
        ObjectMapper mapper = new ObjectMapper();
        conf.setQuery(mapper.readValue("{ \"match_all\" : {} }", new TypeReference<Map<String, Object>>() {}));
        conf.setTailFile(true);

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> SchedulerConfigVerifier.verify(conf));

        assertEquals(ErrorCodes.SCHEDULER_FIELD_NOT_SUPPORTED_FOR_DATASOURCE.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_FIELD_NOT_SUPPORTED, "tailFile", DataSource.ELASTICSEARCH), e.getMessage());
    }


    public void testCheckValidElasticsearch_GivenScriptFieldsNotWholeSource() throws IOException {
        SchedulerConfig.Builder conf = new SchedulerConfig.Builder(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myindex"));
        conf.setTypes(Arrays.asList("mytype"));
        ObjectMapper mapper = new ObjectMapper();
        conf.setScriptFields(mapper.readValue("{ \"twiceresponsetime\" : { \"script\" : { \"lang\" : \"expression\", \"inline\" : \"doc['responsetime'].value * 2\" } } }", new TypeReference<Map<String, Object>>() {
        }));
        conf.setRetrieveWholeSource(false);

        assertTrue(SchedulerConfigVerifier.verify(conf));
    }


    public void testCheckValidElasticsearch_GivenScriptFieldsAndWholeSource() throws IOException {
        SchedulerConfig.Builder conf = new SchedulerConfig.Builder(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myindex"));
        conf.setTypes(Arrays.asList("mytype"));
        ObjectMapper mapper = new ObjectMapper();
        conf.setScriptFields(mapper.readValue("{ \"twiceresponsetime\" : { \"script\" : { \"lang\" : \"expression\", \"inline\" : \"doc['responsetime'].value * 2\" } } }", new TypeReference<Map<String, Object>>() {
        }));
        conf.setRetrieveWholeSource(true);

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> SchedulerConfigVerifier.verify(conf));

        assertEquals(ErrorCodes.SCHEDULER_FIELD_NOT_SUPPORTED_FOR_DATASOURCE.getValueString(), e.getHeader("errorCode").get(0));
    }


    public void testCheckValidElasticsearch_GivenNullIndexes() throws IOException {
        SchedulerConfig.Builder conf = new SchedulerConfig.Builder(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        expectThrows(NullPointerException.class, () -> conf.setIndexes(null));
    }

    public void testCheckValidElasticsearch_GivenEmptyIndexes() throws IOException {
        SchedulerConfig.Builder conf = new SchedulerConfig.Builder(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Collections.emptyList());
        conf.setTypes(Arrays.asList("mytype"));

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> SchedulerConfigVerifier.verify(conf));

        assertEquals(ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_INVALID_OPTION_VALUE, "indexes", "[]"), e.getMessage());
    }


    public void testCheckValidElasticsearch_GivenIndexesContainsOnlyNulls() throws IOException {
        List<String> indexes = new ArrayList<>();
        indexes.add(null);
        indexes.add(null);

        SchedulerConfig.Builder conf = new SchedulerConfig.Builder(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(indexes);
        conf.setTypes(Arrays.asList("mytype"));

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> SchedulerConfigVerifier.verify(conf));

        assertEquals(ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_INVALID_OPTION_VALUE, "indexes", "[null, null]"), e.getMessage());
    }


    public void testCheckValidElasticsearch_GivenIndexesContainsOnlyEmptyStrings() throws IOException {
        List<String> indexes = new ArrayList<>();
        indexes.add("");
        indexes.add("");

        SchedulerConfig.Builder conf = new SchedulerConfig.Builder(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(indexes);
        conf.setTypes(Arrays.asList("mytype"));

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> SchedulerConfigVerifier.verify(conf));

        assertEquals(ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_INVALID_OPTION_VALUE, "indexes", "[, ]"), e.getMessage());
    }


    public void testCheckValidElasticsearch_GivenNegativeQueryDelay() throws IOException {
        SchedulerConfig.Builder conf = new SchedulerConfig.Builder(DataSource.ELASTICSEARCH);
        conf.setQueryDelay(-10L);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myIndex"));
        conf.setTypes(Arrays.asList("mytype"));

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> SchedulerConfigVerifier.verify(conf));

        assertEquals(ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_INVALID_OPTION_VALUE, "queryDelay", -10L), e.getMessage());
    }


    public void testCheckValidElasticsearch_GivenNegativeFrequency() throws IOException {
        SchedulerConfig.Builder conf = new SchedulerConfig.Builder(DataSource.ELASTICSEARCH);
        conf.setFrequency(-600L);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myIndex"));
        conf.setTypes(Arrays.asList("mytype"));

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> SchedulerConfigVerifier.verify(conf));

        assertEquals(ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_INVALID_OPTION_VALUE, "frequency", -600L), e.getMessage());
    }


    public void testCheckValidElasticsearch_GivenNegativeScrollSize() throws IOException {
        SchedulerConfig.Builder conf = new SchedulerConfig.Builder(DataSource.ELASTICSEARCH);
        conf.setScrollSize(-1000);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myIndex"));
        conf.setTypes(Arrays.asList("mytype"));

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> SchedulerConfigVerifier.verify(conf));

        assertEquals(ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_INVALID_OPTION_VALUE, "scrollSize", -1000L), e.getMessage());
    }


    public void testCheckValidElasticsearch_GivenBothAggregationsAndAggsAreSet() {
        SchedulerConfig.Builder conf = new SchedulerConfig.Builder(DataSource.ELASTICSEARCH);
        conf.setScrollSize(1000);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myIndex"));
        conf.setTypes(Arrays.asList("mytype"));

        Map<String, Object> aggs = new HashMap<>();
        conf.setAggregations(aggs);
        conf.setAggs(aggs);

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> SchedulerConfigVerifier.verify(conf));

        assertEquals(ErrorCodes.SCHEDULER_MULTIPLE_AGGREGATIONS.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_MULTIPLE_AGGREGATIONS), e.getMessage());
    }
}
