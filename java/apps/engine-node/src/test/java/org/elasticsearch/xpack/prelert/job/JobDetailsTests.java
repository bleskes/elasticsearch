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
package org.elasticsearch.xpack.prelert.job;

import com.carrotsearch.randomizedtesting.generators.CodepointSetGenerator;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.common.ParseFieldMatcher;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.SchedulerConfig.DataSource;
import org.elasticsearch.xpack.prelert.job.condition.Condition;
import org.elasticsearch.xpack.prelert.job.condition.Operator;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.job.transform.TransformConfig;
import org.elasticsearch.xpack.prelert.job.transform.TransformType;
import org.elasticsearch.xpack.prelert.support.AbstractSerializingTestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class JobDetailsTests extends AbstractSerializingTestCase<JobDetails> {

    @Override
    protected JobDetails createTestInstance() {
        JobDetails.Builder builder = new JobDetails.Builder(randomValidJobId());
        if (randomBoolean()) {
            builder.setDescription(randomAsciiOfLength(10));
        }
        builder.setCreateTime(new Date(randomPositiveLong()));
        if (randomBoolean()) {
            builder.setFinishedTime(new Date(randomPositiveLong()));
        }
        if (randomBoolean()) {
            builder.setLastDataTime(new Date(randomPositiveLong()));
        }
        if (randomBoolean()) {
            builder.setTimeout(randomPositiveLong());
        }
        AnalysisConfig analysisConfig = createAnalysisConfig();
        analysisConfig.setBucketSpan(100);
        builder.setAnalysisConfig(analysisConfig);
        builder.setAnalysisLimits(new AnalysisLimits(randomPositiveLong(), randomPositiveLong()));
        SchedulerConfig.Builder schedulerConfig = new SchedulerConfig.Builder(SchedulerConfig.DataSource.FILE);
        schedulerConfig.setFilePath("/file/path");
        builder.setSchedulerConfig(schedulerConfig);
        if (randomBoolean()) {
            builder.setDataDescription(new DataDescription());
        }
        if (randomBoolean()) {
            builder.setModelSizeStats(new ModelSizeStats());
        }
        String[] outputs;
        TransformType[] transformTypes ;
        if (randomBoolean()) {
            transformTypes = new TransformType[] {TransformType.TRIM, TransformType.LOWERCASE};
            outputs = new String[] {analysisConfig.getDetectors().get(0).getFieldName(),
                    analysisConfig.getDetectors().get(0).getOverFieldName()};
        } else {
            transformTypes = new TransformType[] {TransformType.TRIM};
            outputs = new String[] {analysisConfig.getDetectors().get(0).getFieldName()};
        }
        List<TransformConfig> transformConfigList = new ArrayList<>(transformTypes.length);
        for (int i = 0; i < transformTypes.length; i++) {
            TransformConfig tc = new TransformConfig(transformTypes[i].prettyName());
            tc.setInputs(Collections.singletonList("input" + i));
            tc.setOutputs(Collections.singletonList(outputs[i]));
            transformConfigList.add(tc);
        }
        builder.setTransforms(transformConfigList);
        if (randomBoolean()) {
            builder.setModelDebugConfig(new ModelDebugConfig(randomDouble(), randomAsciiOfLength(10)));
        }
        builder.setCounts(new DataCounts());
        builder.setIgnoreDowntime(randomFrom(IgnoreDowntime.values()));
        if (randomBoolean()) {
            builder.setRenormalizationWindowDays(randomPositiveLong());
        }
        if (randomBoolean()) {
            builder.setBackgroundPersistInterval(randomPositiveLong());
        }
        if (randomBoolean()) {
            builder.setModelSnapshotRetentionDays(randomPositiveLong());
        }
        if (randomBoolean()) {
            builder.setResultsRetentionDays(randomPositiveLong());
        }
        if (randomBoolean()) {
            builder.setCustomSettings(Collections.singletonMap(randomAsciiOfLength(10), randomAsciiOfLength(10)));
        }
        if (randomBoolean()) {
            builder.setAverageBucketProcessingTimeMs(randomDouble());
        }
        if (randomBoolean()) {
            builder.setModelSnapshotId(randomAsciiOfLength(10));
        }
        return builder.build();
    }

    @Override
    protected Writeable.Reader<JobDetails> instanceReader() {
        return JobDetails::new;
    }

    @Override
    protected JobDetails parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return JobDetails.PARSER.apply(parser, () -> matcher).build();
    }

    public void testConstructor_GivenEmptyJobConfiguration() {
        JobDetails jobDetails = buildJobBuilder("foo").build(true);

        assertEquals("foo", jobDetails.getId());
        assertNotNull(jobDetails.getCreateTime());
        assertEquals(600L, jobDetails.getTimeout());
        assertNotNull(jobDetails.getAnalysisConfig());
        assertNull(jobDetails.getAnalysisLimits());
        assertNull(jobDetails.getCustomSettings());
        assertNotNull(jobDetails.getDataDescription());
        assertNull(jobDetails.getDescription());
        assertNull(jobDetails.getFinishedTime());
        assertNull(jobDetails.getIgnoreDowntime());
        assertNull(jobDetails.getLastDataTime());
        assertNull(jobDetails.getModelDebugConfig());
        assertNull(jobDetails.getModelSizeStats());
        assertNull(jobDetails.getRenormalizationWindowDays());
        assertNull(jobDetails.getBackgroundPersistInterval());
        assertNull(jobDetails.getModelSnapshotRetentionDays());
        assertNull(jobDetails.getResultsRetentionDays());
        assertNull(jobDetails.getSchedulerConfig());
        assertEquals(Collections.emptyList(), jobDetails.getTransforms());
        assertNotNull(jobDetails.allFields());
        assertFalse(jobDetails.allFields().isEmpty());
    }

    public void testConstructor_GivenJobConfigurationWithIgnoreDowntime() {
        JobDetails.Builder builder = new JobDetails.Builder("foo");
        builder.setIgnoreDowntime(IgnoreDowntime.ONCE);
        builder.setAnalysisConfig(createAnalysisConfig());
        JobDetails jobDetails = builder.build();

        assertEquals("foo", jobDetails.getId());
        assertEquals(IgnoreDowntime.ONCE, jobDetails.getIgnoreDowntime());
    }

    public void testConstructor_GivenJobConfigurationWithElasticsearchScheduler_ShouldFillDefaults() {
        SchedulerConfig.Builder schedulerConfig = new SchedulerConfig.Builder(DataSource.ELASTICSEARCH);
        expectThrows(NullPointerException.class, () -> schedulerConfig.setQuery(null));
    }

    public void testEquals_GivenDifferentClass() {
        JobDetails jobDetails = buildJobBuilder("foo").build();
        assertFalse(jobDetails.equals("a string"));
    }

    public void testEquals_GivenDifferentIds() {
        Date createTime = new Date();
        JobDetails.Builder builder = buildJobBuilder("foo");
        builder.setCreateTime(createTime);
        JobDetails jobDetails1 = builder.build();
        builder.setId("bar");
        JobDetails jobDetails2 = builder.build();
        assertFalse(jobDetails1.equals(jobDetails2));
    }

    public void testEquals_GivenDifferentRenormalizationWindowDays() {
        JobDetails.Builder jobDetails1 = new JobDetails.Builder("foo");
        jobDetails1.setAnalysisConfig(createAnalysisConfig());
        jobDetails1.setRenormalizationWindowDays(3L);
        JobDetails.Builder jobDetails2 = new JobDetails.Builder("foo");
        jobDetails2.setRenormalizationWindowDays(4L);
        jobDetails2.setAnalysisConfig(createAnalysisConfig());
        assertFalse(jobDetails1.build().equals(jobDetails2.build()));
    }

    public void testEquals_GivenDifferentBackgroundPersistInterval() {
        JobDetails.Builder jobDetails1 = new JobDetails.Builder("foo");
        jobDetails1.setAnalysisConfig(createAnalysisConfig());
        jobDetails1.setBackgroundPersistInterval(10000L);
        JobDetails.Builder jobDetails2 = new JobDetails.Builder("foo");
        jobDetails2.setBackgroundPersistInterval(8000L);
        jobDetails2.setAnalysisConfig(createAnalysisConfig());
        assertFalse(jobDetails1.build().equals(jobDetails2.build()));
    }

    public void testEquals_GivenDifferentModelSnapshotRetentionDays() {
        JobDetails.Builder jobDetails1 = new JobDetails.Builder("foo");
        jobDetails1.setAnalysisConfig(createAnalysisConfig());
        jobDetails1.setModelSnapshotRetentionDays(10L);
        JobDetails.Builder jobDetails2 = new JobDetails.Builder("foo");
        jobDetails2.setModelSnapshotRetentionDays(8L);
        jobDetails2.setAnalysisConfig(createAnalysisConfig());
        assertFalse(jobDetails1.build().equals(jobDetails2.build()));
    }

    public void testEquals_GivenDifferentResultsRetentionDays() {
        JobDetails.Builder jobDetails1 = new JobDetails.Builder("foo");
        jobDetails1.setAnalysisConfig(createAnalysisConfig());
        jobDetails1.setResultsRetentionDays(30L);
        JobDetails.Builder jobDetails2 = new JobDetails.Builder("foo");
        jobDetails2.setResultsRetentionDays(4L);
        jobDetails2.setAnalysisConfig(createAnalysisConfig());
        assertFalse(jobDetails1.build().equals(jobDetails2.build()));
    }

    public void testEquals_GivenDifferentCustomSettings() {
        JobDetails.Builder jobDetails1 = buildJobBuilder("foo");
        Map<String, Object> customSettings1 = new HashMap<>();
        customSettings1.put("key1", "value1");
        jobDetails1.setCustomSettings(customSettings1);
        JobDetails.Builder jobDetails2 = buildJobBuilder("foo");
        Map<String, Object> customSettings2 = new HashMap<>();
        customSettings2.put("key2", "value2");
        jobDetails2.setCustomSettings(customSettings2);
        assertFalse(jobDetails1.build().equals(jobDetails2.build()));
    }

    public void testEquals_GivenDifferentIgnoreDowntime() {
        JobDetails.Builder job1 = new JobDetails.Builder();
        job1.setIgnoreDowntime(IgnoreDowntime.NEVER);
        JobDetails.Builder job2 = new JobDetails.Builder();
        job2.setIgnoreDowntime(IgnoreDowntime.ONCE);

        assertFalse(job1.equals(job2));
        assertFalse(job2.equals(job1));
    }

    public void testSetAnalysisLimits() {
        JobDetails.Builder builder = new JobDetails.Builder();
        builder.setAnalysisLimits(new AnalysisLimits(42L, null));
        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class,
                () -> builder.setAnalysisLimits(new AnalysisLimits(41L, null)));
        assertEquals("Invalid update value for analysisLimits: modelMemoryLimit cannot be decreased; existing is 42, update had 41",
                e.getMessage());
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
    }

    // JobConfigurationTests:

    /**
     * Test the {@link AnalysisConfig#analysisFields()} method which produces a
     * list of analysis fields from the detectors
     */
    public void testAnalysisConfigRequiredFields() {
        Detector.Builder d1 = new Detector.Builder("max", "field");
        d1.setByFieldName("by");

        Detector.Builder d2 = new Detector.Builder("metric", "field2");
        d2.setOverFieldName("over");

        AnalysisConfig ac = new AnalysisConfig();
        ac.setSummaryCountFieldName("agg");
        ac.setDetectors(Arrays.asList(d1.build(), d2.build()));

        List<String> analysisFields = ac.analysisFields();
        assertTrue(analysisFields.size() == 5);

        assertTrue(analysisFields.contains("agg"));
        assertTrue(analysisFields.contains("field"));
        assertTrue(analysisFields.contains("by"));
        assertTrue(analysisFields.contains("field2"));
        assertTrue(analysisFields.contains("over"));

        assertFalse(analysisFields.contains("max"));
        assertFalse(analysisFields.contains(""));
        assertFalse(analysisFields.contains(null));

        Detector.Builder d3 = new Detector.Builder("count", null);
        d3.setByFieldName("by2");
        d3.setPartitionFieldName("partition");

        ac = new AnalysisConfig();
        ac.setDetectors(Arrays.asList(d1.build(), d2.build(), d3.build()));

        analysisFields = ac.analysisFields();
        assertTrue(analysisFields.size() == 6);

        assertTrue(analysisFields.contains("partition"));
        assertTrue(analysisFields.contains("field"));
        assertTrue(analysisFields.contains("by"));
        assertTrue(analysisFields.contains("by2"));
        assertTrue(analysisFields.contains("field2"));
        assertTrue(analysisFields.contains("over"));

        assertFalse(analysisFields.contains("count"));
        assertFalse(analysisFields.contains("max"));
        assertFalse(analysisFields.contains(""));
        assertFalse(analysisFields.contains(null));
    }

    public void testGenerateJobId_doesnotIncludeHost() {
        Pattern pattern = Pattern.compile("[0-9]{14}-[0-9]{5,64}");
        String jobId = JobDetails.Builder.generateJobId(null);
        assertTrue(jobId, pattern.matcher(jobId).matches());
    }

    public void testGenerateJobId_IncludesHost() {
        Pattern pattern = Pattern.compile("[0-9]{14}-server-1-[0-9]{5,64}");
        String jobId = JobDetails.Builder.generateJobId("server-1");
        assertTrue(jobId, pattern.matcher(jobId).matches());
    }

    public void testGenerateJobId_isShorterThanMaxHJobLength() {
        assertTrue(JobDetails.Builder.generateJobId(null).length() < JobDetails.Builder.MAX_JOB_ID_LENGTH);
    }

    public void testGenerateJobId_isShorterThanMaxHJobLength_withLongHostname() {
        String id = JobDetails.Builder.generateJobId("averyverylongstringthatcouldbeahostnameorfullyqualifieddomainname");
        assertEquals("Unexpected id length: " + id, JobDetails.Builder.MAX_JOB_ID_LENGTH, id.length());
        assertTrue(
                "Unexpected id ending: " + id + ", expected ending: "
                        + String.format(Locale.ROOT, "%05d", JobDetails.Builder.ID_SEQUENCE.get()),
                id.endsWith(String.format(Locale.ROOT, "%05d", JobDetails.Builder.ID_SEQUENCE.get())));
    }

    public void testGenerateJobId_isShorterThanMaxHJobLength_withLongHostname_andSixDigitSequence() {
        String id = null;
        for (int i = 0; i < 100000; i++) {
            id = JobDetails.Builder.generateJobId("averyverylongstringthatcouldbeahostnameorfullyqualifieddomainname");
        }
        assertTrue(id.endsWith("-" + JobDetails.Builder.ID_SEQUENCE.get()));
        assertEquals(JobDetails.Builder.MAX_JOB_ID_LENGTH, id.length());
    }

    // JobConfigurationVerifierTests:

    public void testCheckValidId_IdTooLong()  {
        JobDetails.Builder builder = buildJobBuilder("foo");
        builder.setId("averyveryveryaveryveryveryaveryveryveryaveryveryveryaveryveryveryaveryveryverylongid");
        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, () -> builder.build());
        assertEquals(ErrorCodes.JOB_ID_TOO_LONG.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testCheckValidId_GivenAllValidChars() {
        JobDetails.Builder builder = buildJobBuilder("foo");
        builder.setId("abcdefghijklmnopqrstuvwxyz-0123456789_.");
        builder.build();
    }

    public void testCheckValidId_ProhibitedChars() {
        String invalidChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ!@#$%^&*()+?\"'~±/\\[]{},<>=";
        JobDetails.Builder builder = buildJobBuilder("foo");
        String errorMessage = Messages.getMessage(Messages.JOB_CONFIG_INVALID_JOBID_CHARS);
        for (char c : invalidChars.toCharArray()) {
            builder.setId(Character.toString(c));
            ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class, builder::build);
            assertEquals(ErrorCodes.PROHIBITIED_CHARACTER_IN_JOB_ID.getValueString(), e.getHeader("errorCode").get(0));
            assertEquals(errorMessage, e.getMessage());
        }
    }

    public void testCheckValidId_ControlChars() {
        JobDetails.Builder builder = buildJobBuilder("foo");
        builder.setId("two\nlines");
        ElasticsearchStatusException e =
                ESTestCase.expectThrows(ElasticsearchStatusException.class, builder::build);

        assertEquals(ErrorCodes.PROHIBITIED_CHARACTER_IN_JOB_ID.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void jobConfigurationTest() {
        JobDetails.Builder builder = new JobDetails.Builder();
        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class, builder::build);
        assertEquals(ErrorCodes.INCOMPLETE_CONFIGURATION.getValueString(), e.getHeader("errorCode").get(0));
        builder.setId("bad id with spaces");
        e = expectThrows(ElasticsearchStatusException.class, builder::build);
        assertEquals(ErrorCodes.PROHIBITIED_CHARACTER_IN_JOB_ID.getValueString(), e.getHeader("errorCode").get(0));
        builder.setId("bad_id_with_UPPERCASE_chars");
        e = expectThrows(ElasticsearchStatusException.class, builder::build);
        assertEquals(ErrorCodes.PROHIBITIED_CHARACTER_IN_JOB_ID.getValueString(), e.getHeader("errorCode").get(0));
        builder.setId("a very  very very very very very very very very very very very very very very very very very very very long id");
        e = expectThrows(ElasticsearchStatusException.class, builder::build);
        assertEquals(ErrorCodes.JOB_ID_TOO_LONG.getValueString(), e.getHeader("errorCode").get(0));
        builder.setId(null);
        builder.setAnalysisConfig(new AnalysisConfig());
        e = expectThrows(ElasticsearchStatusException.class, builder::build);
        assertEquals(ErrorCodes.INCOMPLETE_CONFIGURATION.getValueString(), e.getHeader("errorCode").equals(0));

        AnalysisConfig ac = new AnalysisConfig();
        Detector.Builder d = new Detector.Builder("max", "a");
        d.setByFieldName("b");
        ac.setDetectors(Arrays.asList(new Detector[]{d.build()}));
        builder.setAnalysisConfig(ac);
        builder.build();
        builder.setAnalysisLimits(new AnalysisLimits(-1, null));
        expectThrows(ElasticsearchStatusException.class, builder::build);
        AnalysisLimits limits = new AnalysisLimits(1000L, 4L);
        builder.setAnalysisLimits(limits);
        builder.build();
        DataDescription dc = new DataDescription();
        dc.setTimeFormat("YYY_KKKKajsatp*");
        builder.setDataDescription(dc);
        e = expectThrows(ElasticsearchStatusException.class, builder::build);
        assertEquals(ErrorCodes.INVALID_DATE_FORMAT.getValueString(), e.getHeader("errorCode").equals(0));
        dc = new DataDescription();
        builder.setDataDescription(dc);
        builder.setTimeout(-1L);
        e = expectThrows(ElasticsearchStatusException.class, builder::build);
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").equals(0));
        builder.setTimeout(300L);
        builder.build();
    }

    public void testCheckTransformOutputIsUsed_throws() {
        JobDetails.Builder builder = buildJobBuilder("foo");
        TransformConfig tc = new TransformConfig(TransformType.Names.DOMAIN_SPLIT_NAME);
        tc.setInputs(Arrays.asList("dns"));
        builder.setTransforms(Arrays.asList(tc));
        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class, builder::build);
        assertEquals(ErrorCodes.TRANSFORM_OUTPUTS_UNUSED.getValueString(), e.getHeader("errorCode").get(0));
        Detector.Builder newDetector = new Detector.Builder();
        newDetector.setFunction(Detector.MIN);
        newDetector.setFieldName(TransformType.DOMAIN_SPLIT.defaultOutputNames().get(0));
        AnalysisConfig config = new AnalysisConfig();
        config.setDetectors(Collections.singletonList(newDetector.build()));
        builder.setAnalysisConfig(config);
        builder.build();
    }

    public void testCheckTransformOutputIsUsed_outputIsSummaryCountField() {
        JobDetails.Builder builder = buildJobBuilder("foo");
        AnalysisConfig config = createAnalysisConfig();
        config.setSummaryCountFieldName("summaryCountField");
        builder.setAnalysisConfig(config);
        TransformConfig tc = new TransformConfig(TransformType.Names.DOMAIN_SPLIT_NAME);
        tc.setInputs(Arrays.asList("dns"));
        tc.setOutputs(Arrays.asList("summaryCountField"));
        builder.setTransforms(Arrays.asList(tc));
        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class, builder::build);
        assertEquals(ErrorCodes.DUPLICATED_TRANSFORM_OUTPUT_NAME.getValueString(), e.getHeader("errorCode").get(0));
        AnalysisConfig ac = createAnalysisConfig();
        Detector existingDetector = ac.getDetectors().get(0);
        Detector.Builder newDetector = new Detector.Builder(existingDetector);
        newDetector.setFieldName(TransformType.DOMAIN_SPLIT.defaultOutputNames().get(0));
        ac.setDetectors(Collections.singletonList(newDetector.build()));
        builder.setAnalysisConfig(config);
        tc.setOutputs(TransformType.DOMAIN_SPLIT.defaultOutputNames());
        e = expectThrows(ElasticsearchStatusException.class, builder::build);
        assertEquals(ErrorCodes.TRANSFORM_OUTPUTS_UNUSED.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testCheckTransformOutputIsUsed_transformHasNoOutput() {
        JobDetails.Builder builder = buildJobBuilder("foo");
        // The exclude filter has no output
        TransformConfig tc = new TransformConfig(TransformType.Names.EXCLUDE_NAME);
        tc.setCondition(new Condition(Operator.MATCH, "whitelisted_host"));
        tc.setInputs(Arrays.asList("dns"));
        builder.setTransforms(Arrays.asList(tc));
        builder.build();
    }

    public void testVerify_GivenDataFormatIsSingleLineAndNullTransforms() {
        String errorMessage = Messages.getMessage(
                Messages.JOB_CONFIG_DATAFORMAT_REQUIRES_TRANSFORM,
                DataDescription.DataFormat.SINGLE_LINE);
        JobDetails.Builder builder = buildJobBuilder("foo");
        DataDescription dataDescription = new DataDescription();
        dataDescription.setFormat(DataDescription.DataFormat.SINGLE_LINE);
        builder.setDataDescription(dataDescription);
        ElasticsearchStatusException e = ESTestCase.expectThrows(ElasticsearchStatusException.class, builder::build);
        assertEquals(ErrorCodes.DATA_FORMAT_IS_SINGLE_LINE_BUT_NO_TRANSFORMS.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(errorMessage, e.getMessage());
    }

    public void testVerify_GivenDataFormatIsSingleLineAndEmptyTransforms() {
        String errorMessage = Messages.getMessage(
                Messages.JOB_CONFIG_DATAFORMAT_REQUIRES_TRANSFORM,
                DataDescription.DataFormat.SINGLE_LINE);
        JobDetails.Builder builder = buildJobBuilder("foo");
        builder.setTransforms(new ArrayList<>());
        DataDescription dataDescription = new DataDescription();
        dataDescription.setFormat(DataDescription.DataFormat.SINGLE_LINE);
        builder.setDataDescription(dataDescription);
        ElasticsearchStatusException e = ESTestCase.expectThrows(ElasticsearchStatusException.class, builder::build);

        assertEquals(ErrorCodes.DATA_FORMAT_IS_SINGLE_LINE_BUT_NO_TRANSFORMS.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(errorMessage, e.getMessage());
    }

    public void testVerify_GivenDataFormatIsSingleLineAndNonEmptyTransforms() {
        ArrayList<TransformConfig> transforms = new ArrayList<>();
        TransformConfig transform = new TransformConfig("trim");
        transform.setInputs(Arrays.asList("raw"));
        transform.setOutputs(Arrays.asList("time"));
        transforms.add(transform);
        JobDetails.Builder builder = buildJobBuilder("foo");
        builder.setTransforms(transforms);
        DataDescription dataDescription = new DataDescription();
        dataDescription.setFormat(DataDescription.DataFormat.SINGLE_LINE);
        builder.setDataDescription(dataDescription);
        builder.build();
    }

    public void testVerify_GivenNegativeRenormalizationWindowDays() {
        String errorMessage = Messages.getMessage(Messages.JOB_CONFIG_FIELD_VALUE_TOO_LOW,
                "renormalizationWindowDays", 0, -1);
        JobDetails.Builder builder = buildJobBuilder("foo");
        builder.setRenormalizationWindowDays(-1L);
        ElasticsearchStatusException e = ESTestCase.expectThrows(ElasticsearchStatusException.class, builder::build);
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(errorMessage, e.getMessage());
    }

    public void testVerify_GivenNegativeModelSnapshotRetentionDays() {
        String errorMessage = Messages.getMessage(Messages.JOB_CONFIG_FIELD_VALUE_TOO_LOW, "modelSnapshotRetentionDays", 0, -1);
        JobDetails.Builder builder = buildJobBuilder("foo");
        builder.setModelSnapshotRetentionDays(-1L);
        ElasticsearchStatusException e = ESTestCase.expectThrows(ElasticsearchStatusException.class, builder::build);

        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(errorMessage, e.getMessage());
    }

    public void testVerify_GivenLowBackgroundPersistInterval() {
        String errorMessage = Messages.getMessage(Messages.JOB_CONFIG_FIELD_VALUE_TOO_LOW,
                "backgroundPersistInterval", 3600, 3599);
        JobDetails.Builder builder = buildJobBuilder("foo");
        builder.setBackgroundPersistInterval(3599L);
        ElasticsearchStatusException e = ESTestCase.expectThrows(ElasticsearchStatusException.class, builder::build);
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(errorMessage, e.getMessage());
    }

    public void testVerify_GivenNegativeResultsRetentionDays() {
        String errorMessage = Messages.getMessage(Messages.JOB_CONFIG_FIELD_VALUE_TOO_LOW,
                "resultsRetentionDays", 0, -1);
        JobDetails.Builder builder = buildJobBuilder("foo");
        builder.setResultsRetentionDays(-1L);
        ElasticsearchStatusException e = ESTestCase.expectThrows(ElasticsearchStatusException.class, builder::build);
        assertEquals(ErrorCodes.INVALID_VALUE.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(errorMessage, e.getMessage());
    }

    public void testVerify_GivenSchedulerButNoBucketSpan() {
        String errorMessage = Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_REQUIRES_BUCKET_SPAN);
        SchedulerConfig.Builder schedulerConfig = createValidElasticsearchSchedulerConfig();
        JobDetails.Builder builder = buildJobBuilder("foo");
        builder.setSchedulerConfig(schedulerConfig);
        ElasticsearchStatusException e = ESTestCase.expectThrows(ElasticsearchStatusException.class, builder::build);
        assertEquals(ErrorCodes.SCHEDULER_REQUIRES_BUCKET_SPAN.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(errorMessage, e.getMessage());
    }

    public void testVerify_GivenElasticsearchSchedulerAndNonZeroLatency() {
        String errorMessage = Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_ELASTICSEARCH_DOES_NOT_SUPPORT_LATENCY);
        SchedulerConfig.Builder schedulerConfig = createValidElasticsearchSchedulerConfig();
        JobDetails.Builder builder = buildJobBuilder("foo");
        builder.setSchedulerConfig(schedulerConfig);
        AnalysisConfig ac = createAnalysisConfig();
        ac.setBucketSpan(1800L);
        ac.setLatency(3600L);
        builder.setAnalysisConfig(ac);
        ElasticsearchStatusException e = ESTestCase.expectThrows(ElasticsearchStatusException.class, builder::build);

        assertEquals(ErrorCodes.SCHEDULER_ELASTICSEARCH_DOES_NOT_SUPPORT_LATENCY.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(errorMessage, e.getMessage());
    }

    public void testVerify_GivenElasticsearchSchedulerAndZeroLatency() {
        SchedulerConfig.Builder schedulerConfig = createValidElasticsearchSchedulerConfig();
        JobDetails.Builder builder = buildJobBuilder("foo");
        builder.setSchedulerConfig(schedulerConfig);
        DataDescription dataDescription = new DataDescription();
        dataDescription.setFormat(DataDescription.DataFormat.ELASTICSEARCH);
        builder.setDataDescription(dataDescription);
        AnalysisConfig ac = createAnalysisConfig();
        ac.setBucketSpan(1800L);
        ac.setLatency(0L);
        builder.setAnalysisConfig(ac);
        builder.build();
    }

    public void testVerify_GivenElasticsearchSchedulerAndNoLatency() {
        SchedulerConfig.Builder schedulerConfig = createValidElasticsearchSchedulerConfig();
        JobDetails.Builder builder = buildJobBuilder("foo");
        builder.setSchedulerConfig(schedulerConfig);
        DataDescription dataDescription = new DataDescription();
        dataDescription.setFormat(DataDescription.DataFormat.ELASTICSEARCH);
        builder.setDataDescription(dataDescription);
        AnalysisConfig ac = createAnalysisConfig();
        ac.setBatchSpan(1800L);
        ac.setBucketSpan(100);
        builder.setAnalysisConfig(ac);
        builder.build();
    }

    public void testVerify_GivenElasticsearchSchedulerWithAggsAndCorrectSummaryCountField() throws IOException {
        SchedulerConfig.Builder schedulerConfig = createValidElasticsearchSchedulerConfigWithAggs();
        JobDetails.Builder builder = buildJobBuilder("foo");
        builder.setSchedulerConfig(schedulerConfig);
        DataDescription dataDescription = new DataDescription();
        dataDescription.setFormat(DataDescription.DataFormat.ELASTICSEARCH);
        builder.setDataDescription(dataDescription);
        AnalysisConfig ac = createAnalysisConfig();
        ac.setBucketSpan(1800L);
        ac.setSummaryCountFieldName("doc_count");
        builder.setAnalysisConfig(ac);
        builder.build();
    }

    public void testVerify_GivenElasticsearchSchedulerWithAggsAndNoSummaryCountField()
            throws IOException {
        String errorMessage = Messages.getMessage(
                Messages.JOB_CONFIG_SCHEDULER_AGGREGATIONS_REQUIRES_SUMMARY_COUNT_FIELD,
                DataSource.ELASTICSEARCH.toString(), SchedulerConfig.DOC_COUNT);
        SchedulerConfig.Builder schedulerConfig = createValidElasticsearchSchedulerConfigWithAggs();
        JobDetails.Builder builder = buildJobBuilder("foo");
        builder.setSchedulerConfig(schedulerConfig);
        DataDescription dataDescription = new DataDescription();
        dataDescription.setFormat(DataDescription.DataFormat.ELASTICSEARCH);
        builder.setDataDescription(dataDescription);
        AnalysisConfig ac = createAnalysisConfig();
        ac.setBucketSpan(1800L);
        builder.setAnalysisConfig(ac);
        ElasticsearchStatusException e = ESTestCase.expectThrows(ElasticsearchStatusException.class, builder::build);

        assertEquals(ErrorCodes.SCHEDULER_AGGREGATIONS_REQUIRES_SUMMARY_COUNT_FIELD.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(errorMessage, e.getMessage());
    }

    public void testVerify_GivenElasticsearchSchedulerWithAggsAndWrongSummaryCountField() throws IOException {
        String errorMessage = Messages.getMessage(
                Messages.JOB_CONFIG_SCHEDULER_AGGREGATIONS_REQUIRES_SUMMARY_COUNT_FIELD,
                DataSource.ELASTICSEARCH.toString(), SchedulerConfig.DOC_COUNT);
        SchedulerConfig.Builder schedulerConfig = createValidElasticsearchSchedulerConfigWithAggs();
        JobDetails.Builder builder = buildJobBuilder("foo");
        builder.setSchedulerConfig(schedulerConfig);
        DataDescription dataDescription = new DataDescription();
        dataDescription.setFormat(DataDescription.DataFormat.ELASTICSEARCH);
        builder.setDataDescription(dataDescription);
        AnalysisConfig ac = createAnalysisConfig();
        ac.setBucketSpan(1800L);
        ac.setSummaryCountFieldName("wrong");
        builder.setAnalysisConfig(ac);
        ElasticsearchStatusException e = ESTestCase.expectThrows(ElasticsearchStatusException.class, builder::build);
        assertEquals(ErrorCodes.SCHEDULER_AGGREGATIONS_REQUIRES_SUMMARY_COUNT_FIELD.getValueString(), e.getHeader("errorCode").get(0));
        assertEquals(errorMessage, e.getMessage());
    }

    public static JobDetails.Builder buildJobBuilder(String id) {
        JobDetails.Builder builder = new JobDetails.Builder(id);
        builder.setCreateTime(new Date());
        AnalysisConfig ac = createAnalysisConfig();
        DataDescription dc = new DataDescription();
        builder.setAnalysisConfig(ac);
        builder.setDataDescription(dc);
        return builder;
    }

    private static SchedulerConfig.Builder createValidElasticsearchSchedulerConfig() {
        SchedulerConfig.Builder schedulerConfig = new SchedulerConfig.Builder(DataSource.ELASTICSEARCH);
        schedulerConfig.setBaseUrl("http://localhost:9200");
        schedulerConfig.setIndexes(Arrays.asList("myIndex"));
        schedulerConfig.setTypes(Arrays.asList("myType"));
        return schedulerConfig;
    }

    private static SchedulerConfig.Builder createValidElasticsearchSchedulerConfigWithAggs()
            throws IOException {
        SchedulerConfig.Builder schedulerConfig = createValidElasticsearchSchedulerConfig();
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
        XContentParser parser = XContentFactory.xContent(aggStr).createParser(aggStr);
        schedulerConfig.setAggs(parser.map());
        return schedulerConfig;
    }

    public static String randomValidJobId() {
        CodepointSetGenerator generator =  new CodepointSetGenerator("abcdefghijklmnopqrstuvwxyz".toCharArray());
        return generator.ofCodePointsLength(random(), 10, 10);
    }

    public static AnalysisConfig createAnalysisConfig() {
        Detector.Builder d1 = new Detector.Builder("info_content", "domain");
        d1.setOverFieldName("client");
        Detector.Builder d2 = new Detector.Builder("min", "field");
        AnalysisConfig ac = new AnalysisConfig();
        ac.setDetectors(Arrays.asList(new Detector[]{d1.build(), d2.build()}));
        return ac;
    }
}
