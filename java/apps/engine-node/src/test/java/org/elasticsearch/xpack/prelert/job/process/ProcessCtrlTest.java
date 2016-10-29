
package org.elasticsearch.xpack.prelert.job.process;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.AnalysisConfig;
import org.elasticsearch.xpack.prelert.job.DataDescription;
import org.elasticsearch.xpack.prelert.job.IgnoreDowntime;
import org.elasticsearch.xpack.prelert.job.JobConfiguration;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.List;

public class ProcessCtrlTest extends ESTestCase {
    @Mock
    private Logger logger;

    @Before
    public void setupMock() {
        MockitoAnnotations.initMocks(this);
    }

    public void testBuildEnvironment() {
        Environment env = new Environment(Settings.EMPTY);
        ProcessBuilder pb = new ProcessBuilder();
        ProcessCtrl.buildEnvironment(env, pb);

        assertEquals(1, pb.environment().size());

        // NORELEASE assertEquals(ProcessCtrl.PRELERT_HOME,
        // pb.environment().get(PrelertSettings.PRELERT_HOME_ENV));
    }

    public void testBuildAutodetectCommand() {
        Environment env = new Environment(Settings.EMPTY);
        JobDetails job = new JobConfiguration().build();
        job.setId("unit-test-job");

        AnalysisConfig ac = new AnalysisConfig();
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

        List<String> command = ProcessCtrl.buildAutodetectCommand(env, job, logger, null, false);

        assertEquals(17, command.size());
        assertTrue(command.contains(ProcessCtrl.getAutodetectPath(env)));
        assertTrue(command.contains(ProcessCtrl.BATCH_SPAN_ARG + "100"));
        assertTrue(command.contains(ProcessCtrl.BUCKET_SPAN_ARG + "120"));
        assertTrue(command.contains(ProcessCtrl.LATENCY_ARG + "360"));
        assertTrue(command.contains(ProcessCtrl.PERIOD_ARG + "20"));
        assertTrue(command.contains(ProcessCtrl.SUMMARY_COUNT_FIELD_ARG + "summaryField"));
        assertTrue(command.contains(ProcessCtrl.RESULT_FINALIZATION_WINDOW_ARG + "2"));
        assertTrue(command.contains(ProcessCtrl.MULTIVARIATE_BY_FIELDS_ARG));

        assertTrue(command.contains(ProcessCtrl.LENGTH_ENCODED_INPUT_ARG));
        assertTrue(command.contains(ProcessCtrl.maxAnomalyRecordsArg(env)));

        assertTrue(command.contains(ProcessCtrl.TIME_FIELD_ARG + "tf"));
        assertTrue(command.contains(ProcessCtrl.LOG_ID_ARG + "unit-test-job"));

        assertTrue(command.contains(ProcessCtrl.PER_PARTITION_NORMALIZATION));

        int expectedPersistInterval = 10800 + ProcessCtrl.calculateStaggeringInterval(job.getId());
        assertTrue(command.contains(ProcessCtrl.PERSIST_INTERVAL_ARG + expectedPersistInterval));
        int expectedMaxQuantileInterval = 21600 + ProcessCtrl.calculateStaggeringInterval(job.getId());
        assertTrue(command.contains(ProcessCtrl.MAX_QUANTILE_INTERVAL_ARG + expectedMaxQuantileInterval));
        assertTrue(String.join(", ", command), command.contains(ProcessCtrl.PERSIST_URL_BASE_ARG +
                "http://localhost:" + ProcessCtrl.ES_HTTP_PORT + "/prelertresults-unit-test-job"));
        assertTrue(command.contains(ProcessCtrl.IGNORE_DOWNTIME_ARG));
    }

    public void testBuildAutodetectCommand_defaultTimeField() {
        Environment env = new Environment(Settings.EMPTY);
        JobDetails job = new JobConfiguration().build();
        job.setId("unit-test-job");

        List<String> command = ProcessCtrl.buildAutodetectCommand(env, job, logger, null, false);

        assertTrue(command.contains(ProcessCtrl.TIME_FIELD_ARG + "time"));
    }

    // NORELEASE work out if we can remove this
    // public void testBuildAutodetectCommand_givenPersistModelState() {
    // Environment env = new Environment(Settings.EMPTY);
    // JobDetails job = new JobConfiguration().build();
    // job.setId("unit-test-job");
    //
    // System.setProperty(ProcessCtrl.DONT_PERSIST_MODEL_STATE, "true");
    //
    // int expectedPersistInterval = 10800 +
    // ProcessCtrl.calculateStaggeringInterval(job.getId());
    //
    // List<String> command = ProcessCtrl.buildAutodetectCommand(env, job,
    // logger, null, false);
    // assertFalse(command.contains(ProcessCtrl.PERSIST_INTERVAL_ARG +
    // expectedPersistInterval));
    //
    // System.getProperties().remove(ProcessCtrl.DONT_PERSIST_MODEL_STATE);
    //
    // command = ProcessCtrl.buildAutodetectCommand(env, job, logger, null,
    // false);
    // assertTrue(command.contains(ProcessCtrl.PERSIST_INTERVAL_ARG +
    // expectedPersistInterval));
    // }

    public void testBuildAutodetectCommand_GivenNoIgnoreDowntime() {
        Environment env = new Environment(Settings.EMPTY);
        JobDetails job = new JobConfiguration().build();
        job.setId("foo");

        List<String> command = ProcessCtrl.buildAutodetectCommand(env, job, logger, null, false);

        assertFalse(command.contains("--ignoreDowntime"));
    }

    public void testBuildAutodetectCommand_GivenIgnoreDowntimeParam() {
        Environment env = new Environment(Settings.EMPTY);
        JobDetails job = new JobConfiguration().build();
        job.setId("foo");

        List<String> command = ProcessCtrl.buildAutodetectCommand(env, job, logger, null, true);

        assertTrue(command.contains("--ignoreDowntime"));
    }

    public void testBuildNormaliserCommand() throws IOException {
        Environment env = new Environment(Settings.EMPTY);
        String jobId = "unit-test-job";

        List<String> command = ProcessCtrl.buildNormaliserCommand(env, jobId, 300, true);

        assertEquals(5, command.size());
        assertTrue(command.contains(ProcessCtrl.getNormalizePath(env)));
        assertTrue(command.contains(ProcessCtrl.BUCKET_SPAN_ARG + "300"));
        assertTrue(command.contains(ProcessCtrl.LOG_ID_ARG + jobId));
        assertTrue(command.contains(ProcessCtrl.LENGTH_ENCODED_INPUT_ARG));
        assertTrue(command.contains(ProcessCtrl.PER_PARTITION_NORMALIZATION));
    }
}
