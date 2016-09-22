
package org.elasticsearch.xpack.prelert.job.config.verification;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.xpack.prelert.job.SchedulerConfig;
import org.elasticsearch.xpack.prelert.job.SchedulerConfig.DataSource;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodeMatcher;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobConfigurationException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertTrue;


public class SchedulerConfigVerifierTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
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

    @Test
    public void testCheckValidFile_AllOk() throws JobConfigurationException {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.FILE);
        conf.setFilePath("myfile.csv");

        SchedulerConfigVerifier.verify(conf);
    }

    @Test
    public void testCheckValidFile_NoPath() throws JobConfigurationException {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.FILE);

        expectedException.expect(JobConfigurationException.class);
        expectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE));

        SchedulerConfigVerifier.verify(conf);
    }

    @Test
    public void testCheckValidFile_EmptyPath() throws JobConfigurationException {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.FILE);
        conf.setFilePath("");

        expectedException.expect(JobConfigurationException.class);
        expectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE));

        SchedulerConfigVerifier.verify(conf);
    }

    @Test
    public void testCheckValidFile_InappropriateField() throws JobConfigurationException {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.FILE);
        conf.setFilePath("myfile.csv");
        conf.setBaseUrl("http://localhost:9200/");

        expectedException.expect(JobConfigurationException.class);
        expectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.SCHEDULER_FIELD_NOT_SUPPORTED_FOR_DATASOURCE));

        SchedulerConfigVerifier.verify(conf);
    }

    @Test
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

    @Test
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

    @Test
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

    @Test
    public void testCheckValidElasticsearch_WithPasswordNoUsername() throws JobConfigurationException {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myindex"));
        conf.setTypes(Arrays.asList("mytype"));
        conf.setPassword("secret");

        expectedException.expect(JobConfigurationException.class);
        expectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.SCHEDULER_INCOMPLETE_CREDENTIALS));
        SchedulerConfigVerifier.verify(conf);
    }

    @Test
    public void testCheckValidElasticsearch_BothPasswordAndEncryptedPassword() throws JobConfigurationException {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myindex"));
        conf.setTypes(Arrays.asList("mytype"));
        conf.setUsername("dave");
        conf.setPassword("secret");
        conf.setEncryptedPassword("already_encrypted");

        expectedException.expect(JobConfigurationException.class);
        expectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.SCHEDULER_MULTIPLE_PASSWORDS));
        SchedulerConfigVerifier.verify(conf);
    }

    @Test
    public void testCheckValidElasticsearch_NoQuery() throws JobConfigurationException {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myindex"));
        conf.setTypes(Arrays.asList("mytype"));

        assertTrue(SchedulerConfigVerifier.verify(conf));
    }

    @Test
    public void testCheckValidElasticsearch_InappropriateField() throws JobConfigurationException, IOException {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myindex"));
        conf.setTypes(Arrays.asList("mytype"));
        ObjectMapper mapper = new ObjectMapper();
        conf.setQuery(mapper.readValue("{ \"match_all\" : {} }", new TypeReference<Map<String, Object>>() {
        }));
        conf.setTailFile(true);

        expectedException.expect(JobConfigurationException.class);
        expectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.SCHEDULER_FIELD_NOT_SUPPORTED_FOR_DATASOURCE));
        SchedulerConfigVerifier.verify(conf);
    }

    @Test
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

    @Test
    public void testCheckValidElasticsearch_GivenScriptFieldsAndWholeSource() throws JobConfigurationException, IOException {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myindex"));
        conf.setTypes(Arrays.asList("mytype"));
        ObjectMapper mapper = new ObjectMapper();
        conf.setScriptFields(mapper.readValue("{ \"twiceresponsetime\" : { \"script\" : { \"lang\" : \"expression\", \"inline\" : \"doc['responsetime'].value * 2\" } } }", new TypeReference<Map<String, Object>>() {
        }));
        conf.setRetrieveWholeSource(true);

        expectedException.expect(JobConfigurationException.class);
        expectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.SCHEDULER_FIELD_NOT_SUPPORTED_FOR_DATASOURCE));
        SchedulerConfigVerifier.verify(conf);
    }

    @Test
    public void testCheckValidElasticsearch_GivenNullIndexes() throws JobConfigurationException,
            IOException {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(null);
        conf.setTypes(new ArrayList<String>(Arrays.asList("mytype")));

        expectedException.expect(JobConfigurationException.class);
        expectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE));
        expectedException.expectMessage("Invalid indexes value 'null' in scheduler configuration");

        SchedulerConfigVerifier.verify(conf);
    }

    @Test
    public void testCheckValidElasticsearch_GivenEmptyIndexes() throws JobConfigurationException,
            IOException {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Collections.emptyList());
        conf.setTypes(Arrays.asList("mytype"));

        expectedException.expect(JobConfigurationException.class);
        expectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE));
        expectedException.expectMessage("Invalid indexes value '[]' in scheduler configuration");

        SchedulerConfigVerifier.verify(conf);
    }

    @Test
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

        expectedException.expect(JobConfigurationException.class);
        expectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE));
        expectedException.expectMessage("Invalid indexes value '[null, null]' in scheduler configuration");

        SchedulerConfigVerifier.verify(conf);
    }

    @Test
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

        expectedException.expect(JobConfigurationException.class);
        expectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE));
        expectedException.expectMessage("Invalid indexes value '[, ]' in scheduler configuration");

        SchedulerConfigVerifier.verify(conf);
    }

    @Test
    public void testCheckValidElasticsearch_GivenNegativeQueryDelay()
            throws JobConfigurationException, IOException {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setQueryDelay(-10L);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myIndex"));
        conf.setTypes(Arrays.asList("mytype"));

        expectedException.expect(JobConfigurationException.class);
        expectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE));
        expectedException.expectMessage("Invalid queryDelay value");

        SchedulerConfigVerifier.verify(conf);
    }

    @Test
    public void testCheckValidElasticsearch_GivenNegativeFrequency()
            throws JobConfigurationException, IOException {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setFrequency(-600L);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myIndex"));
        conf.setTypes(Arrays.asList("mytype"));

        expectedException.expect(JobConfigurationException.class);
        expectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE));
        expectedException.expectMessage("Invalid frequency value");

        SchedulerConfigVerifier.verify(conf);
    }

    @Test
    public void testCheckValidElasticsearch_GivenNegativeScrollSize()
            throws JobConfigurationException, IOException {
        SchedulerConfig conf = new SchedulerConfig();
        conf.setDataSource(DataSource.ELASTICSEARCH);
        conf.setScrollSize(-1000);
        conf.setBaseUrl("http://localhost:9200/");
        conf.setIndexes(Arrays.asList("myIndex"));
        conf.setTypes(Arrays.asList("mytype"));

        expectedException.expect(JobConfigurationException.class);
        expectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE));
        expectedException.expectMessage("Invalid scrollSize value");

        SchedulerConfigVerifier.verify(conf);
    }

    @Test
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

        expectedException.expect(JobConfigurationException.class);
        expectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.SCHEDULER_MULTIPLE_AGGREGATIONS));
        expectedException.expectMessage(
                "Both aggregations and aggs were specified - please just specify one");

        SchedulerConfigVerifier.verify(conf);
    }
}
