
package org.elasticsearch.xpack.prelert.job.config.verification;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.xpack.prelert.integration.hack.ESTestCase;
import org.elasticsearch.xpack.prelert.job.SchedulerConfig;
import org.elasticsearch.xpack.prelert.job.SchedulerConfig.DataSource;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobConfigurationException;
import org.elasticsearch.xpack.prelert.job.messages.Messages;

import java.io.IOException;
import java.util.*;


public class SchedulerConfigVerifierTest extends ESTestCase {

    public void testVerify_GivenAllDataSources_DoesNotThrowIllegalStateException() throws JobConfigurationException {
        for (DataSource dataSource : DataSource.values()) {
            SchedulerConfig conf = new SchedulerConfig();
            conf.setDataSource(dataSource);

            try {
                SchedulerConfigVerifier.verify(conf);
            } catch (JobConfigurationException e) {
                // Expected
            }
        }
    }


    public void testCheckValidFile_AllOk() throws JobConfigurationException {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.FILE);
        conf.setFilePath("myfile.csv");

        SchedulerConfigVerifier.verify(conf);
    }


    public void testCheckValidFile_NoPath() {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.FILE);

        JobConfigurationException e =
                ESTestCase.expectThrows(JobConfigurationException.class,
                        () -> SchedulerConfigVerifier.verify(conf));

        assertEquals(ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE, e.getErrorCode());
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_INVALID_OPTION_VALUE, "filePath", "null"), e.getMessage());
    }


    public void testCheckValidFile_EmptyPath() {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.FILE);
        conf.setFilePath("");

        JobConfigurationException e =
                ESTestCase.expectThrows(JobConfigurationException.class,
                        () -> SchedulerConfigVerifier.verify(conf));

        assertEquals(ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE, e.getErrorCode());
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_INVALID_OPTION_VALUE, "filePath", ""), e.getMessage());
    }


    public void testCheckValidFile_InappropriateField() {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.FILE);
        conf.setFilePath("myfile.csv");
        conf.setBaseUrl("http://localhost:9200/");

        JobConfigurationException e =
                ESTestCase.expectThrows(JobConfigurationException.class,
                        () -> SchedulerConfigVerifier.verify(conf));

        assertEquals(ErrorCodes.SCHEDULER_FIELD_NOT_SUPPORTED_FOR_DATASOURCE, e.getErrorCode());
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_FIELD_NOT_SUPPORTED, "baseUrl", DataSource.FILE),
                e.getMessage());
    }


    public void testCheckValidElasticsearch_AllOk() throws JobConfigurationException, IOException {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
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


    public void testCheckValidElasticsearch_WithUsernameAndPassword() throws JobConfigurationException, IOException {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
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


    public void testCheckValidElasticsearch_WithUsernameAndEncryptedPassword() throws JobConfigurationException, IOException {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
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
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myindex"));
        conf.setTypes(Arrays.asList("mytype"));
        conf.setPassword("secret");

        JobConfigurationException e =
                ESTestCase.expectThrows(JobConfigurationException.class,
                        () -> SchedulerConfigVerifier.verify(conf));

        assertEquals(ErrorCodes.SCHEDULER_INCOMPLETE_CREDENTIALS, e.getErrorCode());
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_INCOMPLETE_CREDENTIALS), e.getMessage());
    }


    public void testCheckValidElasticsearch_BothPasswordAndEncryptedPassword() {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myindex"));
        conf.setTypes(Arrays.asList("mytype"));
        conf.setUsername("dave");
        conf.setPassword("secret");
        conf.setEncryptedPassword("already_encrypted");

        JobConfigurationException e =
                ESTestCase.expectThrows(JobConfigurationException.class,
                        () -> SchedulerConfigVerifier.verify(conf));

        assertEquals(ErrorCodes.SCHEDULER_MULTIPLE_PASSWORDS, e.getErrorCode());
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_MULTIPLE_PASSWORDS), e.getMessage());
    }


    public void testCheckValidElasticsearch_NoQuery() throws JobConfigurationException {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myindex"));
        conf.setTypes(Arrays.asList("mytype"));

        assertTrue(SchedulerConfigVerifier.verify(conf));
    }


    public void testCheckValidElasticsearch_InappropriateField() throws IOException {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myindex"));
        conf.setTypes(Arrays.asList("mytype"));
        ObjectMapper mapper = new ObjectMapper();
        conf.setQuery(mapper.readValue("{ \"match_all\" : {} }", new TypeReference<Map<String, Object>>() {}));
        conf.setTailFile(true);

        JobConfigurationException e =
                ESTestCase.expectThrows(JobConfigurationException.class,
                        () -> SchedulerConfigVerifier.verify(conf));

        assertEquals(ErrorCodes.SCHEDULER_FIELD_NOT_SUPPORTED_FOR_DATASOURCE, e.getErrorCode());
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_FIELD_NOT_SUPPORTED, "tailFile", DataSource.ELASTICSEARCH), e.getMessage());
    }


    public void testCheckValidElasticsearch_GivenScriptFieldsNotWholeSource() throws JobConfigurationException, IOException {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
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
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myindex"));
        conf.setTypes(Arrays.asList("mytype"));
        ObjectMapper mapper = new ObjectMapper();
        conf.setScriptFields(mapper.readValue("{ \"twiceresponsetime\" : { \"script\" : { \"lang\" : \"expression\", \"inline\" : \"doc['responsetime'].value * 2\" } } }", new TypeReference<Map<String, Object>>() {
        }));
        conf.setRetrieveWholeSource(true);

        JobConfigurationException e =
                ESTestCase.expectThrows(JobConfigurationException.class,
                        () -> SchedulerConfigVerifier.verify(conf));

        assertEquals(ErrorCodes.SCHEDULER_FIELD_NOT_SUPPORTED_FOR_DATASOURCE, e.getErrorCode());
    }


    public void testCheckValidElasticsearch_GivenNullIndexes() throws IOException {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(null);
        conf.setTypes(new ArrayList<String>(Arrays.asList("mytype")));

        JobConfigurationException e =
                ESTestCase.expectThrows(JobConfigurationException.class,
                        () -> SchedulerConfigVerifier.verify(conf));

        assertEquals(ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE, e.getErrorCode());
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_INVALID_OPTION_VALUE, "indexes", "null"), e.getMessage());
    }

    public void testCheckValidElasticsearch_GivenEmptyIndexes() throws IOException {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Collections.emptyList());
        conf.setTypes(Arrays.asList("mytype"));

        JobConfigurationException e =
                ESTestCase.expectThrows(JobConfigurationException.class,
                        () -> SchedulerConfigVerifier.verify(conf));

        assertEquals(ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE, e.getErrorCode());
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_INVALID_OPTION_VALUE, "indexes", "[]"), e.getMessage());
    }


    public void testCheckValidElasticsearch_GivenIndexesContainsOnlyNulls()
            throws JobConfigurationException, IOException {
        List<String> indexes = new ArrayList<>();
        indexes.add(null);
        indexes.add(null);

        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(indexes);
        conf.setTypes(Arrays.asList("mytype"));

        JobConfigurationException e =
                ESTestCase.expectThrows(JobConfigurationException.class,
                        () -> SchedulerConfigVerifier.verify(conf));

        assertEquals(ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE, e.getErrorCode());
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_INVALID_OPTION_VALUE, "indexes", "[null, null]"), e.getMessage());
    }


    public void testCheckValidElasticsearch_GivenIndexesContainsOnlyEmptyStrings()
            throws JobConfigurationException, IOException {
        List<String> indexes = new ArrayList<>();
        indexes.add("");
        indexes.add("");

        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(indexes);
        conf.setTypes(Arrays.asList("mytype"));

        JobConfigurationException e =
                ESTestCase.expectThrows(JobConfigurationException.class,
                        () -> SchedulerConfigVerifier.verify(conf));

        assertEquals(ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE, e.getErrorCode());
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_INVALID_OPTION_VALUE, "indexes", "[, ]"), e.getMessage());
    }


    public void testCheckValidElasticsearch_GivenNegativeQueryDelay()
            throws JobConfigurationException, IOException {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setQueryDelay(-10L);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myIndex"));
        conf.setTypes(Arrays.asList("mytype"));

        JobConfigurationException e =
                ESTestCase.expectThrows(JobConfigurationException.class,
                        () -> SchedulerConfigVerifier.verify(conf));

        assertEquals(ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE, e.getErrorCode());
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_INVALID_OPTION_VALUE, "queryDelay", -10L), e.getMessage());
    }


    public void testCheckValidElasticsearch_GivenNegativeFrequency()
            throws JobConfigurationException, IOException {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setFrequency(-600L);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myIndex"));
        conf.setTypes(Arrays.asList("mytype"));

        JobConfigurationException e =
                ESTestCase.expectThrows(JobConfigurationException.class,
                        () -> SchedulerConfigVerifier.verify(conf));

        assertEquals(ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE, e.getErrorCode());
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_INVALID_OPTION_VALUE, "frequency", -600L), e.getMessage());
    }


    public void testCheckValidElasticsearch_GivenNegativeScrollSize()
            throws JobConfigurationException, IOException {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setScrollSize(-1000);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myIndex"));
        conf.setTypes(Arrays.asList("mytype"));

        JobConfigurationException e =
                ESTestCase.expectThrows(JobConfigurationException.class,
                        () -> SchedulerConfigVerifier.verify(conf));

        assertEquals(ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE, e.getErrorCode());
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_INVALID_OPTION_VALUE, "scrollSize", -1000L), e.getMessage());
    }


    public void testCheckValidElasticsearch_GivenBothAggregationsAndAggsAreSet()
            throws JobConfigurationException {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setScrollSize(1000);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myIndex"));
        conf.setTypes(Arrays.asList("mytype"));

        Map<String, Object> aggs = new HashMap<>();
        conf.setAggregations(aggs);
        conf.setAggs(aggs);

        JobConfigurationException e =
                ESTestCase.expectThrows(JobConfigurationException.class,
                        () -> SchedulerConfigVerifier.verify(conf));

        assertEquals(ErrorCodes.SCHEDULER_MULTIPLE_AGGREGATIONS, e.getErrorCode());
        assertEquals(Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_MULTIPLE_AGGREGATIONS), e.getMessage());
    }
}
