
package org.elasticsearch.xpack.prelert.job.config.verification;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.AnalysisConfig;
import org.elasticsearch.xpack.prelert.job.AnalysisLimits;
import org.elasticsearch.xpack.prelert.job.DataDescription;
import org.elasticsearch.xpack.prelert.job.DataDescription.DataFormat;
import org.elasticsearch.xpack.prelert.job.Detector;
import org.elasticsearch.xpack.prelert.job.JobConfiguration;
import org.elasticsearch.xpack.prelert.job.SchedulerConfig;
import org.elasticsearch.xpack.prelert.job.SchedulerConfig.DataSource;
import org.elasticsearch.xpack.prelert.job.condition.Condition;
import org.elasticsearch.xpack.prelert.job.condition.Operator;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.job.transform.TransformConfig;
import org.elasticsearch.xpack.prelert.job.transform.TransformType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class JobConfigurationVerifierTest extends ESTestCase {

    public void testCheckValidId_IdTooLong()  {
        JobConfiguration conf = buildJobConfigurationNoTransforms();
        conf.setId("averyveryveryaveryveryveryaveryveryveryaveryveryveryaveryveryveryaveryveryverylongid");

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> JobConfigurationVerifier.verify(conf));

        assertEquals(ErrorCodes.JOB_ID_TOO_LONG.getValueString(), e.getHeader("errorCode").get(0));
    }


    public void testCheckValidId_GivenAllValidChars() {
        JobConfiguration conf = buildJobConfigurationNoTransforms();
        conf.setId("abcdefghijklmnopqrstuvwxyz-0123456789_.");

        assertTrue(JobConfigurationVerifier.verify(conf));
    }


    public void testCheckValidId_GivenEmpty() {
        JobConfiguration conf = buildJobConfigurationNoTransforms();
        conf.setId("");

        assertTrue(JobConfigurationVerifier.verify(conf));
    }


    public void testCheckValidId_ProhibitedChars() {
        String invalidChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ!@#$%^&*()+?\"'~±/\\[]{},<>=";

        JobConfiguration conf = buildJobConfigurationNoTransforms();

        String errorMessage = Messages.getMessage(Messages.JOB_CONFIG_INVALID_JOBID_CHARS);

        for (char c : invalidChars.toCharArray()) {
            conf.setId(Character.toString(c));

            try {
                JobConfigurationVerifier.verify(conf);
                fail("Character '" + c + "' should not be valid");
            } catch (ElasticsearchStatusException e) {
                assertEquals(ErrorCodes.PROHIBITIED_CHARACTER_IN_JOB_ID.getValueString(), e.getHeader("errorCode").get(0));
                assertEquals(errorMessage, e.getMessage());
            }
        }
    }


    public void testCheckValidId_ControlChars() {
        JobConfiguration conf = buildJobConfigurationNoTransforms();

        conf.setId("two\nlines");

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> JobConfigurationVerifier.verify(conf));

        assertEquals(ErrorCodes.PROHIBITIED_CHARACTER_IN_JOB_ID.getValueString(), e.getHeader("errorCode").get(0));
    }


    public void jobConfigurationTest() {
        JobConfiguration jc = new JobConfiguration();
        try {
            JobConfigurationVerifier.verify(jc);
            assertTrue(false); // shouldn't get here
        } catch (ElasticsearchStatusException e) {
            assertEquals(ErrorCodes.INCOMPLETE_CONFIGURATION.getValueString(), e.getHeader("errorCode").get(0));
        }

        jc.setId("bad id with spaces");
        try {
            JobConfigurationVerifier.verify(jc);
            assertTrue(false); // shouldn't get here
        } catch (ElasticsearchStatusException e) {
            assertEquals(ErrorCodes.PROHIBITIED_CHARACTER_IN_JOB_ID.getValueString(), e.getHeader("errorCode").get(0));
        }


        jc.setId("bad_id_with_UPPERCASE_chars");
        try {
            JobConfigurationVerifier.verify(jc);
            assertTrue(false); // shouldn't get here
        } catch (ElasticsearchStatusException e) {
            assertEquals(ErrorCodes.PROHIBITIED_CHARACTER_IN_JOB_ID.getValueString(), e.getHeader("errorCode").get(0));
        }

        jc.setId("a very  very very very very very very very very very very very very very very very very very very very long id");
        try {
            JobConfigurationVerifier.verify(jc);
            assertTrue(false); // shouldn't get here
        } catch (ElasticsearchStatusException e) {
            assertEquals(ErrorCodes.JOB_ID_TOO_LONG.getValueString(), e.getHeader("errorCode").get(0));
        }


        jc.setId(null);
        jc.setAnalysisConfig(new AnalysisConfig());
        try {
            JobConfigurationVerifier.verify(jc);
            assertTrue(false); // shouldn't get here
        } catch (ElasticsearchStatusException e) {
            assertEquals(ErrorCodes.INCOMPLETE_CONFIGURATION.getValueString(), e.getHeader("errorCode").equals(0));
        }


        AnalysisConfig ac = new AnalysisConfig();
        Detector d = new Detector("foo", "max", "a");
        d.setByFieldName("b");
        ac.setDetectors(Arrays.asList(new Detector[]{d}));

        jc.setAnalysisConfig(ac);
        JobConfigurationVerifier.verify(jc); // ok

        jc.setAnalysisLimits(new AnalysisLimits(-1, null));
        try {
            JobConfigurationVerifier.verify(jc);
        } catch (ElasticsearchStatusException e) {
            assertTrue(false); // shouldn't get here
        }

        jc.setAnalysisLimits(new AnalysisLimits(1000, 4L));
        JobConfigurationVerifier.verify(jc);

        DataDescription dc = new DataDescription();
        dc.setTimeFormat("YYY_KKKKajsatp*");

        jc.setDataDescription(dc);
        try {
            JobConfigurationVerifier.verify(jc);
            assertTrue(false); // shouldn't get here
        } catch (ElasticsearchStatusException e) {
            assertEquals(ErrorCodes.INVALID_DATE_FORMAT.getValueString(), e.getHeader("errorCode").equals(0));
        }


        dc = new DataDescription();
        jc.setDataDescription(dc);

        jc.setTimeout(-1L);
        try {
            JobConfigurationVerifier.verify(jc);
            assertTrue(false); // shouldn't get here
        } catch (ElasticsearchStatusException e) {
            assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").equals(0));
        }

        jc.setTimeout(300L);
        JobConfigurationVerifier.verify(jc);

    }



    public void testCheckTransformOutputIsUsed_throws() {
        JobConfiguration jc = buildJobConfigurationNoTransforms();

        TransformConfig tc = new TransformConfig();
        tc.setTransform(TransformType.Names.DOMAIN_SPLIT_NAME);
        tc.setInputs(Arrays.asList("dns"));

        jc.setTransforms(Arrays.asList(tc));

        try {
            JobConfigurationVerifier.verify(jc);
            fail("verify should throw"); // shouldn't get here
        } catch (ElasticsearchStatusException e) {
            assertEquals(ErrorCodes.TRANSFORM_OUTPUTS_UNUSED.getValueString(), e.getHeader("errorCode").get(0));
        }


        Detector existingDetector = jc.getAnalysisConfig().getDetectors().get(0);
        Detector newDetector = new Detector(existingDetector.getDetectorDescription(), existingDetector.getFunction(),
                TransformType.DOMAIN_SPLIT.defaultOutputNames().get(0));
        newDetector.setByFieldName(existingDetector.getByFieldName());
        newDetector.setOverFieldName(existingDetector.getOverFieldName());
        newDetector.setPartitionFieldName(existingDetector.getPartitionFieldName());
        newDetector.setDetectorRules(existingDetector.getDetectorRules());
        newDetector.setExcludeFrequent(existingDetector.getExcludeFrequent());
        newDetector.setUseNull(existingDetector.isUseNull());
        jc.getAnalysisConfig().getDetectors().set(0, newDetector);
        assertTrue(JobConfigurationVerifier.verify(jc));
    }


    public void testCheckTransformOutputIsUsed_outputIsSummaryCountField() {
        JobConfiguration jc = buildJobConfigurationNoTransforms();
        jc.getAnalysisConfig().setSummaryCountFieldName("summaryCountField");

        TransformConfig tc = new TransformConfig();
        tc.setTransform(TransformType.Names.DOMAIN_SPLIT_NAME);
        tc.setInputs(Arrays.asList("dns"));
        tc.setOutputs(Arrays.asList("summaryCountField"));

        jc.setTransforms(Arrays.asList(tc));

        try {
            JobConfigurationVerifier.verify(jc);
            fail("verify should throw"); // shouldn't get here
        } catch (ElasticsearchStatusException e) {
            assertEquals(ErrorCodes.DUPLICATED_TRANSFORM_OUTPUT_NAME.getValueString(), e.getHeader("errorCode").get(0));
        }

        Detector existingDetector = jc.getAnalysisConfig().getDetectors().get(0);
        Detector newDetector = new Detector(existingDetector.getDetectorDescription(), existingDetector.getFunction(),
                TransformType.DOMAIN_SPLIT.defaultOutputNames().get(0));
        newDetector.setByFieldName(existingDetector.getByFieldName());
        newDetector.setOverFieldName(existingDetector.getOverFieldName());
        newDetector.setPartitionFieldName(existingDetector.getPartitionFieldName());
        newDetector.setDetectorRules(existingDetector.getDetectorRules());
        newDetector.setExcludeFrequent(existingDetector.getExcludeFrequent());
        newDetector.setUseNull(existingDetector.isUseNull());
        jc.getAnalysisConfig().getDetectors().set(0, newDetector);
        tc.setOutputs(TransformType.DOMAIN_SPLIT.defaultOutputNames());
        assertTrue(JobConfigurationVerifier.verify(jc));
    }


    public void testCheckTransformOutputIsUsed_transformHasNoOutput() {
        JobConfiguration jc = buildJobConfigurationNoTransforms();

        // The exclude filter has no output
        TransformConfig tc = new TransformConfig();
        tc.setTransform(TransformType.Names.EXCLUDE_NAME);
        tc.setCondition(new Condition(Operator.MATCH, "whitelisted_host"));
        tc.setInputs(Arrays.asList("dns"));

        jc.setTransforms(Arrays.asList(tc));

        JobConfigurationVerifier.verify(jc);
    }


    public void testVerify_GivenDataFormatIsSingleLineAndNullTransforms() {
        String errorMessage = Messages.getMessage(
                Messages.JOB_CONFIG_DATAFORMAT_REQUIRES_TRANSFORM,
                DataFormat.SINGLE_LINE);

        JobConfiguration config = buildJobConfigurationNoTransforms();
        config.getDataDescription().setFormat(DataFormat.SINGLE_LINE);

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> JobConfigurationVerifier.verify(config));

        assertEquals(ErrorCodes.DATA_FORMAT_IS_SINGLE_LINE_BUT_NO_TRANSFORMS.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(errorMessage, e.getMessage());
    }


    public void testVerify_GivenDataFormatIsSingleLineAndEmptyTransforms() {
        String errorMessage = Messages.getMessage(
                Messages.JOB_CONFIG_DATAFORMAT_REQUIRES_TRANSFORM,
                DataFormat.SINGLE_LINE);

        JobConfiguration config = buildJobConfigurationNoTransforms();
        config.setTransforms(new ArrayList<>());
        config.getDataDescription().setFormat(DataFormat.SINGLE_LINE);

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> JobConfigurationVerifier.verify(config));

        assertEquals(ErrorCodes.DATA_FORMAT_IS_SINGLE_LINE_BUT_NO_TRANSFORMS.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(errorMessage, e.getMessage());
    }


    public void testVerify_GivenDataFormatIsSingleLineAndNonEmptyTransforms() {
        ArrayList<TransformConfig> transforms = new ArrayList<>();
        TransformConfig transform = new TransformConfig();
        transform.setTransform("trim");
        transform.setInputs(Arrays.asList("raw"));
        transform.setOutputs(Arrays.asList("time"));
        transforms.add(transform);
        JobConfiguration config = buildJobConfigurationNoTransforms();
        config.setTransforms(transforms);
        config.getDataDescription().setFormat(DataFormat.SINGLE_LINE);

        JobConfigurationVerifier.verify(config);
    }


    public void testVerify_GivenNegativeRenormalizationWindowDays() {
        String errorMessage = Messages.getMessage(Messages.JOB_CONFIG_FIELD_VALUE_TOO_LOW,
                "renormalizationWindowDays", 0, -1);

        JobConfiguration jobConfig = buildJobConfigurationNoTransforms();
        jobConfig.setRenormalizationWindowDays(-1L);

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> JobConfigurationVerifier.verify(jobConfig));

        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(errorMessage, e.getMessage());
    }


    public void testVerify_GivenNegativeModelSnapshotRetentionDays() {
        String errorMessage = Messages.getMessage(Messages.JOB_CONFIG_FIELD_VALUE_TOO_LOW, "modelSnapshotRetentionDays", 0, -1);

        JobConfiguration jobConfig = buildJobConfigurationNoTransforms();
        jobConfig.setModelSnapshotRetentionDays(-1L);

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> JobConfigurationVerifier.verify(jobConfig));

        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(errorMessage, e.getMessage());
    }


    public void testVerify_GivenLowBackgroundPersistInterval() {
        String errorMessage = Messages.getMessage(Messages.JOB_CONFIG_FIELD_VALUE_TOO_LOW,
                "backgroundPersistInterval", 3600, 3599);

        JobConfiguration jobConfig = buildJobConfigurationNoTransforms();
        jobConfig.setBackgroundPersistInterval(3599L);

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> JobConfigurationVerifier.verify(jobConfig));

        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(errorMessage, e.getMessage());
    }


    public void testVerify_GivenNegativeResultsRetentionDays() {
        String errorMessage = Messages.getMessage(Messages.JOB_CONFIG_FIELD_VALUE_TOO_LOW,
                "resultsRetentionDays", 0, -1);

        JobConfiguration jobConfig = buildJobConfigurationNoTransforms();
        jobConfig.setResultsRetentionDays(-1L);

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> JobConfigurationVerifier.verify(jobConfig));

        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(errorMessage, e.getMessage());
    }


    public void testVerify_GivenSchedulerButNoBucketSpan() {

        String errorMessage = Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_REQUIRES_BUCKET_SPAN);

        SchedulerConfig schedulerConfig = createValidElasticsearchSchedulerConfig();
        JobConfiguration jobConfig = buildJobConfigurationNoTransforms();
        jobConfig.setSchedulerConfig(schedulerConfig);
        jobConfig.getAnalysisConfig().setBucketSpan(null);

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> JobConfigurationVerifier.verify(jobConfig));

        assertEquals(ErrorCodes.SCHEDULER_REQUIRES_BUCKET_SPAN.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(errorMessage, e.getMessage());
    }


    public void testVerify_GivenElasticsearchSchedulerAndNonZeroLatency() {
        String errorMessage = Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_ELASTICSEARCH_DOES_NOT_SUPPORT_LATENCY);

        SchedulerConfig schedulerConfig = createValidElasticsearchSchedulerConfig();
        JobConfiguration jobConfig = buildJobConfigurationNoTransforms();
        jobConfig.setSchedulerConfig(schedulerConfig);
        jobConfig.getAnalysisConfig().setBucketSpan(1800L);
        jobConfig.getAnalysisConfig().setLatency(3600L);

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> JobConfigurationVerifier.verify(jobConfig));

        assertEquals(ErrorCodes.SCHEDULER_ELASTICSEARCH_DOES_NOT_SUPPORT_LATENCY.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(errorMessage, e.getMessage());
    }


    public void testVerify_GivenElasticsearchSchedulerAndZeroLatency() {
        SchedulerConfig schedulerConfig = createValidElasticsearchSchedulerConfig();
        JobConfiguration jobConfig = buildJobConfigurationNoTransforms();
        jobConfig.setSchedulerConfig(schedulerConfig);
        jobConfig.getDataDescription().setFormat(DataFormat.ELASTICSEARCH);
        jobConfig.getAnalysisConfig().setBucketSpan(1800L);
        jobConfig.getAnalysisConfig().setLatency(0L);

        assertTrue(JobConfigurationVerifier.verify(jobConfig));
    }


    public void testVerify_GivenElasticsearchSchedulerAndNullLatency() {
        SchedulerConfig schedulerConfig = createValidElasticsearchSchedulerConfig();
        JobConfiguration jobConfig = buildJobConfigurationNoTransforms();
        jobConfig.setSchedulerConfig(schedulerConfig);
        jobConfig.getDataDescription().setFormat(DataFormat.ELASTICSEARCH);
        jobConfig.getAnalysisConfig().setBucketSpan(1800L);
        jobConfig.getAnalysisConfig().setLatency(null);

        assertTrue(JobConfigurationVerifier.verify(jobConfig));
    }


    public void testVerify_GivenElasticsearchSchedulerWithAggsAndCorrectSummaryCountField() throws IOException {
        SchedulerConfig schedulerConfig = createValidElasticsearchSchedulerConfigWithAggs();
        JobConfiguration jobConfig = buildJobConfigurationNoTransforms();
        jobConfig.setSchedulerConfig(schedulerConfig);
        jobConfig.getDataDescription().setFormat(DataFormat.ELASTICSEARCH);
        jobConfig.getAnalysisConfig().setBucketSpan(1800L);
        jobConfig.getAnalysisConfig().setSummaryCountFieldName("doc_count");

        assertTrue(JobConfigurationVerifier.verify(jobConfig));
    }


    public void testVerify_GivenElasticsearchSchedulerWithAggsAndNoSummaryCountField()
            throws IOException {
        String errorMessage = Messages.getMessage(
                Messages.JOB_CONFIG_SCHEDULER_AGGREGATIONS_REQUIRES_SUMMARY_COUNT_FIELD,
                DataSource.ELASTICSEARCH.toString(), SchedulerConfig.DOC_COUNT);

        SchedulerConfig schedulerConfig = createValidElasticsearchSchedulerConfigWithAggs();
        JobConfiguration jobConfig = buildJobConfigurationNoTransforms();
        jobConfig.setSchedulerConfig(schedulerConfig);
        jobConfig.getDataDescription().setFormat(DataFormat.ELASTICSEARCH);
        jobConfig.getAnalysisConfig().setBucketSpan(1800L);

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> JobConfigurationVerifier.verify(jobConfig));

        assertEquals(ErrorCodes.SCHEDULER_AGGREGATIONS_REQUIRES_SUMMARY_COUNT_FIELD.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(errorMessage, e.getMessage());
    }


    public void testVerify_GivenElasticsearchSchedulerWithAggsAndWrongSummaryCountField() throws IOException {
        String errorMessage = Messages.getMessage(
                Messages.JOB_CONFIG_SCHEDULER_AGGREGATIONS_REQUIRES_SUMMARY_COUNT_FIELD,
                DataSource.ELASTICSEARCH.toString(), SchedulerConfig.DOC_COUNT);

        SchedulerConfig schedulerConfig = createValidElasticsearchSchedulerConfigWithAggs();
        JobConfiguration jobConfig = buildJobConfigurationNoTransforms();
        jobConfig.setSchedulerConfig(schedulerConfig);
        jobConfig.getDataDescription().setFormat(DataFormat.ELASTICSEARCH);
        jobConfig.getAnalysisConfig().setBucketSpan(1800L);
        jobConfig.getAnalysisConfig().setSummaryCountFieldName("wrong");

        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> JobConfigurationVerifier.verify(jobConfig));

        assertEquals(ErrorCodes.SCHEDULER_AGGREGATIONS_REQUIRES_SUMMARY_COUNT_FIELD.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(errorMessage, e.getMessage());
    }

    private JobConfiguration buildJobConfigurationNoTransforms() {
        JobConfiguration jc = new JobConfiguration();

        Detector d1 = new Detector("foo", "info_content", "domain");
        d1.setOverFieldName("client");

        Detector d2 = new Detector("foo", "count");

        AnalysisConfig ac = new AnalysisConfig();
        ac.setDetectors(Arrays.asList(new Detector[]{d1, d2}));

        DataDescription dc = new DataDescription();

        jc.setAnalysisConfig(ac);
        jc.setDataDescription(dc);

        return jc;
    }

    private static SchedulerConfig createValidElasticsearchSchedulerConfig() {
        SchedulerConfig schedulerConfig = new SchedulerConfig();
        schedulerConfig.setDataSource(DataSource.ELASTICSEARCH);
        schedulerConfig.setBaseUrl("http://localhost:9200");
        schedulerConfig.setIndexes(Arrays.asList("myIndex"));
        schedulerConfig.setTypes(Arrays.asList("myType"));
        return schedulerConfig;
    }

    private static SchedulerConfig createValidElasticsearchSchedulerConfigWithAggs()
            throws IOException {
        SchedulerConfig schedulerConfig = createValidElasticsearchSchedulerConfig();
        String aggStr =
                "{" +
                        "\"buckets\" : {" +
                        "\"histogram\" : {" +
                        "\"field\" : \"time\"," +
                        "\"interval\" : 3600000" +
                        "}," +
                        "\"aggs\" : {" +
                        "\"byField\" : {" +
                        "\"terms\" : {" +
                        "\"field\" : \"airline\"," +
                        "\"size\" : 0" +
                        "}," +
                        "\"aggs\" : {" +
                        "\"stats\" : {" +
                        "\"stats\" : {" +
                        "\"field\" : \"responsetime\"" +
                        "}" +
                        "}" +
                        "}" +
                        "}" +
                        "}" +
                        "}   " +
                        "}";
        ObjectMapper mapper = new ObjectMapper();
        schedulerConfig.setAggs(mapper.readValue(aggStr, new TypeReference<Map<String, Object>>() {
        }));
        return schedulerConfig;
    }
}
