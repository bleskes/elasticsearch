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
package org.elasticsearch.xpack.prelert.job.process;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.AnalysisConfig;
import org.elasticsearch.xpack.prelert.job.DataDescription;
import org.elasticsearch.xpack.prelert.job.Detector;
import org.elasticsearch.xpack.prelert.job.IgnoreDowntime;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.Mockito;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.elasticsearch.xpack.prelert.job.JobDetailsTests.buildJobBuilder;

public class ProcessCtrlTests extends ESTestCase {
    @Mock
    private Logger logger;

    @Before
    public void setupMock() {
        logger = Mockito.mock(Logger.class);
    }

    public void testBuildEnvironment() {
        ProcessBuilder pb = new ProcessBuilder();
        ProcessCtrl.buildEnvironment(pb);

        assertEquals(0, pb.environment().size());
    }

    public void testBuildAutodetectCommand() {
        Settings settings = Settings.builder().put(Environment.PATH_HOME_SETTING.getKey(), createTempDir().toString()).build();
        Environment env = new Environment(settings);
        JobDetails.Builder job = buildJobBuilder("unit-test-job");

        Detector.Builder d1 = new Detector.Builder("info_content", "domain");
        d1.setOverFieldName("client");
        d1.setPartitionFieldName("field");
        AnalysisConfig ac = new AnalysisConfig();
        ac.setDetectors(Collections.singletonList(d1.build()));
        ac.setBatchSpan(100L);
        ac.setBucketSpan(120L);
        ac.setLatency(360L);
        ac.setPeriod(20L);
        ac.setSummaryCountFieldName("summaryField");
        ac.setOverlappingBuckets(true);
        ac.setMultivariateByFields(true);
        ac.setUsePerPartitionNormalization(true);
        job.setAnalysisConfig(ac);

        DataDescription dd = new DataDescription();
        dd.setFieldDelimiter('|');
        dd.setTimeField("tf");
        job.setDataDescription(dd);

        job.setIgnoreDowntime(IgnoreDowntime.ONCE);

        List<String> command = ProcessCtrl.buildAutodetectCommand(env, settings, job.build(), logger, null, false);
        assertEquals(17, command.size());
        assertTrue(command.contains(ProcessCtrl.getAutodetectPath(env).toString()));
        assertTrue(command.contains(ProcessCtrl.BATCH_SPAN_ARG + "100"));
        assertTrue(command.contains(ProcessCtrl.BUCKET_SPAN_ARG + "120"));
        assertTrue(command.contains(ProcessCtrl.LATENCY_ARG + "360"));
        assertTrue(command.contains(ProcessCtrl.PERIOD_ARG + "20"));
        assertTrue(command.contains(ProcessCtrl.SUMMARY_COUNT_FIELD_ARG + "summaryField"));
        assertTrue(command.contains(ProcessCtrl.RESULT_FINALIZATION_WINDOW_ARG + "2"));
        assertTrue(command.contains(ProcessCtrl.MULTIVARIATE_BY_FIELDS_ARG));

        assertTrue(command.contains(ProcessCtrl.LENGTH_ENCODED_INPUT_ARG));
        assertTrue(command.contains(ProcessCtrl.maxAnomalyRecordsArg(settings)));

        assertTrue(command.contains(ProcessCtrl.TIME_FIELD_ARG + "tf"));
        assertTrue(command.contains(ProcessCtrl.LOG_ID_ARG + "unit-test-job"));

        assertTrue(command.contains(ProcessCtrl.PER_PARTITION_NORMALIZATION));

        int expectedPersistInterval = 10800 + ProcessCtrl.calculateStaggeringInterval(job.getId());
        assertTrue(command.contains(ProcessCtrl.PERSIST_INTERVAL_ARG + expectedPersistInterval));
        int expectedMaxQuantileInterval = 21600 + ProcessCtrl.calculateStaggeringInterval(job.getId());
        assertTrue(command.contains(ProcessCtrl.MAX_QUANTILE_INTERVAL_ARG + expectedMaxQuantileInterval));
        assertTrue(String.join(", ", command), command.contains(
                ProcessCtrl.PERSIST_URL_BASE_ARG + "http://localhost:" + ProcessCtrl.ES_HTTP_PORT + "/prelertresults-unit-test-job"));
        assertTrue(command.contains(ProcessCtrl.IGNORE_DOWNTIME_ARG));
    }

    public void testBuildAutodetectCommand_defaultTimeField() {
        Settings settings = Settings.builder().put(Environment.PATH_HOME_SETTING.getKey(), createTempDir().toString()).build();
        Environment env = new Environment(settings);
        JobDetails.Builder job = buildJobBuilder("unit-test-job");

        List<String> command = ProcessCtrl.buildAutodetectCommand(env, settings, job.build(), logger, null, false);

        assertTrue(command.contains(ProcessCtrl.TIME_FIELD_ARG + "time"));
    }

    public void testBuildAutodetectCommand_givenPersistModelState() {
        Settings settings = Settings.builder().put(Environment.PATH_HOME_SETTING.getKey(), createTempDir().toString())
                .put(ProcessCtrl.DONT_PERSIST_MODEL_STATE_SETTING.getKey(), true).build();
        Environment env = new Environment(settings);
        JobDetails.Builder job = buildJobBuilder("unit-test-job");

        int expectedPersistInterval = 10800 + ProcessCtrl.calculateStaggeringInterval(job.getId());

        List<String> command = ProcessCtrl.buildAutodetectCommand(env, settings, job.build(), logger, null, false);
        assertFalse(command.contains(ProcessCtrl.PERSIST_INTERVAL_ARG + expectedPersistInterval));

        settings = Settings.builder().put(Environment.PATH_HOME_SETTING.getKey(), createTempDir().toString()).build();
        env = new Environment(settings);

        command = ProcessCtrl.buildAutodetectCommand(env, settings, job.build(), logger, null, false);
        assertTrue(command.contains(ProcessCtrl.PERSIST_INTERVAL_ARG + expectedPersistInterval));
    }

    public void testBuildAutodetectCommand_GivenNoIgnoreDowntime() {
        Settings settings = Settings.builder().put(Environment.PATH_HOME_SETTING.getKey(), createTempDir().toString()).build();
        Environment env = new Environment(
                settings);
        JobDetails.Builder job = buildJobBuilder("foo");

        List<String> command = ProcessCtrl.buildAutodetectCommand(env, settings, job.build(), logger, null, false);

        assertFalse(command.contains("--ignoreDowntime"));
    }

    public void testBuildAutodetectCommand_GivenIgnoreDowntimeParam() {
        Settings settings = Settings.builder().put(Environment.PATH_HOME_SETTING.getKey(), createTempDir().toString()).build();
        Environment env = new Environment(
                settings);
        JobDetails.Builder job = buildJobBuilder("foo");

        List<String> command = ProcessCtrl.buildAutodetectCommand(env, settings, job.build(), logger, null, true);

        assertTrue(command.contains("--ignoreDowntime"));
    }

    public void testBuildNormaliserCommand() throws IOException {
        Environment env = new Environment(
                Settings.builder().put(Environment.PATH_HOME_SETTING.getKey(), createTempDir().toString()).build());
        String jobId = "unit-test-job";

        List<String> command = ProcessCtrl.buildNormaliserCommand(env, jobId, 300, true);
        assertEquals(5, command.size());
        assertTrue(command.contains(ProcessCtrl.getNormalizePath(env).toString()));
        assertTrue(command.contains(ProcessCtrl.BUCKET_SPAN_ARG + "300"));
        assertTrue(command.contains(ProcessCtrl.LOG_ID_ARG + jobId));
        assertTrue(command.contains(ProcessCtrl.LENGTH_ENCODED_INPUT_ARG));
        assertTrue(command.contains(ProcessCtrl.PER_PARTITION_NORMALIZATION));
    }
}
