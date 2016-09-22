
package org.elasticsearch.xpack.prelert.job.config.verification;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.xpack.prelert.integration.hack.ESTestCase;
import org.elasticsearch.xpack.prelert.job.*;
import org.elasticsearch.xpack.prelert.job.DataDescription.DataFormat;
import org.elasticsearch.xpack.prelert.job.SchedulerConfig.DataSource;
import org.elasticsearch.xpack.prelert.job.condition.Condition;
import org.elasticsearch.xpack.prelert.job.condition.Operator;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodeMatcher;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobConfigurationException;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.job.transform.TransformConfig;
import org.elasticsearch.xpack.prelert.job.transform.TransformType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JobConfigurationVerifierTest extends ESTestCase {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();


    public void testCheckValidId_IdTooLong() throws JobConfigurationException {
        JobConfiguration conf = buildJobConfigurationNoTransforms();
        conf.setId("averyveryveryaveryveryveryaveryveryveryaveryveryveryaveryveryveryaveryveryverylongid");

        expectedException.expect(JobConfigurationException.class);
        expectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.JOB_ID_TOO_LONG));

        JobConfigurationVerifier.verify(conf);
    }


    public void testCheckValidId_GivenAllValidChars() throws JobConfigurationException {
        JobConfiguration conf = buildJobConfigurationNoTransforms();
        conf.setId("abcdefghijklmnopqrstuvwxyz-0123456789_.");

        assertTrue(JobConfigurationVerifier.verify(conf));
    }


    public void testCheckValidId_GivenEmpty() throws JobConfigurationException {
        JobConfiguration conf = buildJobConfigurationNoTransforms();
        conf.setId("");

        assertTrue(JobConfigurationVerifier.verify(conf));
    }


    public void testCheckValidId_ProhibitedChars() throws JobConfigurationException {
        String invalidChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ!@#$%^&*()+?\"'~±/\\[]{},<>=";

        JobConfiguration conf = buildJobConfigurationNoTransforms();

        String errorMessage = Messages.getMessage(Messages.JOB_CONFIG_INVALID_JOBID_CHARS);

        for (char c : invalidChars.toCharArray()) {
            conf.setId(Character.toString(c));

            try {
                JobConfigurationVerifier.verify(conf);
                fail("Character '" + c + "' should not be valid");
            } catch (JobConfigurationException e) {
                assertEquals(ErrorCodes.PROHIBITIED_CHARACTER_IN_JOB_ID, e.getErrorCode());
                assertEquals(errorMessage, e.getMessage());
            }
        }
    }


    public void testCheckValidId_ControlChars() throws JobConfigurationException {
        JobConfiguration conf = buildJobConfigurationNoTransforms();

        expectedException.expect(JobConfigurationException.class);
        expectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.PROHIBITIED_CHARACTER_IN_JOB_ID));

        conf.setId("two\nlines");
        JobConfigurationVerifier.verify(conf);
    }


    public void jobConfigurationTest()
            throws JobConfigurationException {
        JobConfiguration jc = new JobConfiguration();
        try {
            JobConfigurationVerifier.verify(jc);
            assertTrue(false); // shouldn't get here
        } catch (JobConfigurationException e) {
            assertTrue(e.getErrorCode() == ErrorCodes.INCOMPLETE_CONFIGURATION);
        }

        jc.setId("bad id with spaces");
        try {
            JobConfigurationVerifier.verify(jc);
            assertTrue(false); // shouldn't get here
        } catch (JobConfigurationException e) {
            assertTrue(e.getErrorCode() == ErrorCodes.PROHIBITIED_CHARACTER_IN_JOB_ID);
        }


        jc.setId("bad_id_with_UPPERCASE_chars");
        try {
            JobConfigurationVerifier.verify(jc);
            assertTrue(false); // shouldn't get here
        } catch (JobConfigurationException e) {
            assertTrue(e.getErrorCode() == ErrorCodes.PROHIBITIED_CHARACTER_IN_JOB_ID);
        }

        jc.setId("a very  very very very very very very very very very very very very very very very very very very very long id");
        try {
            JobConfigurationVerifier.verify(jc);
            assertTrue(false); // shouldn't get here
        } catch (JobConfigurationException e) {
            assertTrue(e.getErrorCode() == ErrorCodes.JOB_ID_TOO_LONG);
        }


        jc.setId(null);
        jc.setAnalysisConfig(new AnalysisConfig());
        try {
            JobConfigurationVerifier.verify(jc);
            assertTrue(false); // shouldn't get here
        } catch (JobConfigurationException e) {
            assertTrue(e.getErrorCode() == ErrorCodes.INCOMPLETE_CONFIGURATION);
        }


        AnalysisConfig ac = new AnalysisConfig();
        Detector d = new Detector();
        d.setFieldName("a");
        d.setByFieldName("b");
        d.setFunction("max");
        ac.setDetectors(Arrays.asList(new Detector[]{d}));

        jc.setAnalysisConfig(ac);
        JobConfigurationVerifier.verify(jc); // ok

        jc.setAnalysisLimits(new AnalysisLimits(-1, null));
        try {
            JobConfigurationVerifier.verify(jc);
        } catch (JobConfigurationException e) {
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
        } catch (JobConfigurationException e) {
            assertEquals(ErrorCodes.INVALID_DATE_FORMAT, e.getErrorCode());
        }


        dc = new DataDescription();
        jc.setDataDescription(dc);

        jc.setTimeout(-1L);
        try {
            JobConfigurationVerifier.verify(jc);
            assertTrue(false); // shouldn't get here
        } catch (JobConfigurationException e) {
            assertEquals(ErrorCodes.INVALID_VALUE, e.getErrorCode());
        }

        jc.setTimeout(300L);
        JobConfigurationVerifier.verify(jc);

    }



    public void testCheckTransformOutputIsUsed_throws() throws JobConfigurationException {
        JobConfiguration jc = buildJobConfigurationNoTransforms();

        TransformConfig tc = new TransformConfig();
        tc.setTransform(TransformType.Names.DOMAIN_SPLIT_NAME);
        tc.setInputs(Arrays.asList("dns"));

        jc.setTransforms(Arrays.asList(tc));

        try {
            JobConfigurationVerifier.verify(jc);
            fail("verify should throw"); // shouldn't get here
        } catch (JobConfigurationException e) {
            assertTrue(e instanceof JobConfigurationException);
            assertEquals(ErrorCodes.TRANSFORM_OUTPUTS_UNUSED, e.getErrorCode());
        }


        jc.getAnalysisConfig().getDetectors().get(0).setFieldName(TransformType.DOMAIN_SPLIT.defaultOutputNames().get(0));
        assertTrue(JobConfigurationVerifier.verify(jc));
    }


    public void testCheckTransformOutputIsUsed_outputIsSummaryCountField() throws JobConfigurationException {
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
        } catch (JobConfigurationException e) {
            assertTrue(e instanceof JobConfigurationException);
            assertEquals(ErrorCodes.DUPLICATED_TRANSFORM_OUTPUT_NAME, e.getErrorCode());
        }

        jc.getAnalysisConfig().getDetectors().get(0).setFieldName(TransformType.DOMAIN_SPLIT.defaultOutputNames().get(0));
        tc.setOutputs(TransformType.DOMAIN_SPLIT.defaultOutputNames());
        assertTrue(JobConfigurationVerifier.verify(jc));
    }


    public void testCheckTransformOutputIsUsed_transformHasNoOutput()
            throws JobConfigurationException {
        JobConfiguration jc = buildJobConfigurationNoTransforms();

        // The exclude filter has no output
        TransformConfig tc = new TransformConfig();
        tc.setTransform(TransformType.Names.EXCLUDE_NAME);
        tc.setCondition(new Condition(Operator.MATCH, "whitelisted_host"));
        tc.setInputs(Arrays.asList("dns"));

        jc.setTransforms(Arrays.asList(tc));

        JobConfigurationVerifier.verify(jc);
    }


    public void testVerify_GivenDataFormatIsSingleLineAndNullTransforms()
            throws JobConfigurationException {
        String errorMessage = Messages.getMessage(
                Messages.JOB_CONFIG_DATAFORMAT_REQUIRES_TRANSFORM,
                DataFormat.SINGLE_LINE);

        expectedException.expect(JobConfigurationException.class);
        expectedException.expectMessage(errorMessage);
        expectedException.expect(ErrorCodeMatcher.hasErrorCode(
                ErrorCodes.DATA_FORMAT_IS_SINGLE_LINE_BUT_NO_TRANSFORMS));

        JobConfiguration config = buildJobConfigurationNoTransforms();
        config.getDataDescription().setFormat(DataFormat.SINGLE_LINE);

        JobConfigurationVerifier.verify(config);
    }


    public void testVerify_GivenDataFormatIsSingleLineAndEmptyTransforms()
            throws JobConfigurationException {
        String errorMessage = Messages.getMessage(
                Messages.JOB_CONFIG_DATAFORMAT_REQUIRES_TRANSFORM,
                DataFormat.SINGLE_LINE);

        expectedException.expect(JobConfigurationException.class);
        expectedException.expectMessage(errorMessage);
        expectedException.expect(ErrorCodeMatcher.hasErrorCode(
                ErrorCodes.DATA_FORMAT_IS_SINGLE_LINE_BUT_NO_TRANSFORMS));

        JobConfiguration config = buildJobConfigurationNoTransforms();
        config.setTransforms(new ArrayList<>());
        config.getDataDescription().setFormat(DataFormat.SINGLE_LINE);

        JobConfigurationVerifier.verify(config);
    }


    public void testVerify_GivenDataFormatIsSingleLineAndNonEmptyTransforms()
            throws JobConfigurationException {
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


    public void testVerify_GivenNegativeRenormalizationWindowDays() throws JobConfigurationException {
        String errorMessage = Messages.getMessage(Messages.JOB_CONFIG_FIELD_VALUE_TOO_LOW,
                "renormalizationWindowDays", 0, -1);

        expectedException.expect(JobConfigurationException.class);
        expectedException.expectMessage(errorMessage);
        expectedException.expect(ErrorCodeMatcher.hasErrorCode(
                ErrorCodes.INVALID_VALUE));

        JobConfiguration jobConfig = buildJobConfigurationNoTransforms();
        jobConfig.setRenormalizationWindowDays(-1L);

        JobConfigurationVerifier.verify(jobConfig);
    }


    public void testVerify_GivenNegativeModelSnapshotRetentionDays() throws JobConfigurationException {
        String errorMessage = Messages.getMessage(Messages.JOB_CONFIG_FIELD_VALUE_TOO_LOW,
                "modelSnapshotRetentionDays", 0, -1);

        expectedException.expect(JobConfigurationException.class);
        expectedException.expectMessage(errorMessage);
        expectedException.expect(ErrorCodeMatcher.hasErrorCode(
                ErrorCodes.INVALID_VALUE));

        JobConfiguration jobConfig = buildJobConfigurationNoTransforms();
        jobConfig.setModelSnapshotRetentionDays(-1L);

        JobConfigurationVerifier.verify(jobConfig);
    }


    public void testVerify_GivenLowBackgroundPersistInterval() throws JobConfigurationException {
        String errorMessage = Messages.getMessage(Messages.JOB_CONFIG_FIELD_VALUE_TOO_LOW,
                "backgroundPersistInterval", 3600, 3599);

        expectedException.expect(JobConfigurationException.class);
        expectedException.expectMessage(errorMessage);
        expectedException.expect(ErrorCodeMatcher.hasErrorCode(
                ErrorCodes.INVALID_VALUE));

        JobConfiguration jobConfig = buildJobConfigurationNoTransforms();
        jobConfig.setBackgroundPersistInterval(3599L);

        JobConfigurationVerifier.verify(jobConfig);
    }


    public void testVerify_GivenNegativeResultsRetentionDays() throws JobConfigurationException {
        String errorMessage = Messages.getMessage(Messages.JOB_CONFIG_FIELD_VALUE_TOO_LOW,
                "resultsRetentionDays", 0, -1);

        expectedException.expect(JobConfigurationException.class);
        expectedException.expectMessage(errorMessage);
        expectedException.expect(ErrorCodeMatcher.hasErrorCode(
                ErrorCodes.INVALID_VALUE));

        JobConfiguration jobConfig = buildJobConfigurationNoTransforms();
        jobConfig.setResultsRetentionDays(-1L);

        JobConfigurationVerifier.verify(jobConfig);
    }


    public void testVerify_GivenSchedulerButNoBucketSpan() throws JobConfigurationException {
        expectedException.expect(JobConfigurationException.class);
        String errorMessage = Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_REQUIRES_BUCKET_SPAN);
        expectedException.expectMessage(errorMessage);
        expectedException.expect(ErrorCodeMatcher.hasErrorCode(
                ErrorCodes.SCHEDULER_REQUIRES_BUCKET_SPAN));

        SchedulerConfig schedulerConfig = createValidElasticsearchSchedulerConfig();
        JobConfiguration jobConfig = buildJobConfigurationNoTransforms();
        jobConfig.setSchedulerConfig(schedulerConfig);
        jobConfig.getAnalysisConfig().setBucketSpan(null);

        JobConfigurationVerifier.verify(jobConfig);
    }


    public void testVerify_GivenElasticsearchSchedulerAndNonZeroLatency() throws JobConfigurationException {
        expectedException.expect(JobConfigurationException.class);
        String errorMessage = Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_ELASTICSEARCH_DOES_NOT_SUPPORT_LATENCY);
        expectedException.expectMessage(errorMessage);
        expectedException.expect(ErrorCodeMatcher.hasErrorCode(
                ErrorCodes.SCHEDULER_ELASTICSEARCH_DOES_NOT_SUPPORT_LATENCY));

        SchedulerConfig schedulerConfig = createValidElasticsearchSchedulerConfig();
        JobConfiguration jobConfig = buildJobConfigurationNoTransforms();
        jobConfig.setSchedulerConfig(schedulerConfig);
        jobConfig.getAnalysisConfig().setBucketSpan(1800L);
        jobConfig.getAnalysisConfig().setLatency(3600L);

        JobConfigurationVerifier.verify(jobConfig);
    }


    public void testVerify_GivenElasticsearchSchedulerAndZeroLatency() throws JobConfigurationException {
        SchedulerConfig schedulerConfig = createValidElasticsearchSchedulerConfig();
        JobConfiguration jobConfig = buildJobConfigurationNoTransforms();
        jobConfig.setSchedulerConfig(schedulerConfig);
        jobConfig.getDataDescription().setFormat(DataFormat.ELASTICSEARCH);
        jobConfig.getAnalysisConfig().setBucketSpan(1800L);
        jobConfig.getAnalysisConfig().setLatency(0L);

        assertTrue(JobConfigurationVerifier.verify(jobConfig));
    }


    public void testVerify_GivenElasticsearchSchedulerAndNullLatency() throws JobConfigurationException {
        SchedulerConfig schedulerConfig = createValidElasticsearchSchedulerConfig();
        JobConfiguration jobConfig = buildJobConfigurationNoTransforms();
        jobConfig.setSchedulerConfig(schedulerConfig);
        jobConfig.getDataDescription().setFormat(DataFormat.ELASTICSEARCH);
        jobConfig.getAnalysisConfig().setBucketSpan(1800L);
        jobConfig.getAnalysisConfig().setLatency(null);

        assertTrue(JobConfigurationVerifier.verify(jobConfig));
    }


    public void testVerify_GivenElasticsearchSchedulerWithAggsAndCorrectSummaryCountField()
            throws JobConfigurationException, IOException {
        SchedulerConfig schedulerConfig = createValidElasticsearchSchedulerConfigWithAggs();
        JobConfiguration jobConfig = buildJobConfigurationNoTransforms();
        jobConfig.setSchedulerConfig(schedulerConfig);
        jobConfig.getDataDescription().setFormat(DataFormat.ELASTICSEARCH);
        jobConfig.getAnalysisConfig().setBucketSpan(1800L);
        jobConfig.getAnalysisConfig().setSummaryCountFieldName("doc_count");

        assertTrue(JobConfigurationVerifier.verify(jobConfig));
    }


    public void testVerify_GivenElasticsearchSchedulerWithAggsAndNoSummaryCountField()
            throws JobConfigurationException, IOException {
        String errorMessage = Messages.getMessage(
                Messages.JOB_CONFIG_SCHEDULER_AGGREGATIONS_REQUIRES_SUMMARY_COUNT_FIELD,
                DataSource.ELASTICSEARCH.toString(), SchedulerConfig.DOC_COUNT);

        expectedException.expect(JobConfigurationException.class);
        expectedException.expectMessage(errorMessage);
        expectedException.expect(ErrorCodeMatcher.hasErrorCode(
                ErrorCodes.SCHEDULER_AGGREGATIONS_REQUIRES_SUMMARY_COUNT_FIELD));

        SchedulerConfig schedulerConfig = createValidElasticsearchSchedulerConfigWithAggs();
        JobConfiguration jobConfig = buildJobConfigurationNoTransforms();
        jobConfig.setSchedulerConfig(schedulerConfig);
        jobConfig.getDataDescription().setFormat(DataFormat.ELASTICSEARCH);
        jobConfig.getAnalysisConfig().setBucketSpan(1800L);

        assertTrue(JobConfigurationVerifier.verify(jobConfig));
    }


    public void testVerify_GivenElasticsearchSchedulerWithAggsAndWrongSummaryCountField()
            throws JobConfigurationException, IOException {
        String errorMessage = Messages.getMessage(
                Messages.JOB_CONFIG_SCHEDULER_AGGREGATIONS_REQUIRES_SUMMARY_COUNT_FIELD,
                DataSource.ELASTICSEARCH.toString(), SchedulerConfig.DOC_COUNT);

        expectedException.expect(JobConfigurationException.class);
        expectedException.expectMessage(errorMessage);
        expectedException.expect(ErrorCodeMatcher.hasErrorCode(
                ErrorCodes.SCHEDULER_AGGREGATIONS_REQUIRES_SUMMARY_COUNT_FIELD));

        SchedulerConfig schedulerConfig = createValidElasticsearchSchedulerConfigWithAggs();
        JobConfiguration jobConfig = buildJobConfigurationNoTransforms();
        jobConfig.setSchedulerConfig(schedulerConfig);
        jobConfig.getDataDescription().setFormat(DataFormat.ELASTICSEARCH);
        jobConfig.getAnalysisConfig().setBucketSpan(1800L);
        jobConfig.getAnalysisConfig().setSummaryCountFieldName("wrong");

        assertTrue(JobConfigurationVerifier.verify(jobConfig));
    }

    private JobConfiguration buildJobConfigurationNoTransforms() {
        JobConfiguration jc = new JobConfiguration();

        Detector d1 = new Detector();
        d1.setFieldName("domain");
        d1.setOverFieldName("client");
        d1.setFunction("info_content");

        Detector d2 = new Detector();
        d2.setFunction("count");

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
