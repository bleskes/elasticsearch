
package org.elasticsearch.xpack.prelert.job.process;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.AnalysisConfig;
import org.elasticsearch.xpack.prelert.job.DataDescription;
import org.elasticsearch.xpack.prelert.job.IgnoreDowntime;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.settings.PrelertSettings;
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
        ProcessBuilder pb = new ProcessBuilder();
        ProcessCtrl.buildEnvironment(pb);

        assertEquals(2, pb.environment().size());

        assertEquals(ProcessCtrl.PRELERT_HOME, pb.environment().get(PrelertSettings.PRELERT_HOME_ENV));
        assertEquals(ProcessCtrl.LIB_PATH, pb.environment().get(ProcessCtrl.LIB_PATH_ENV));
    }

    public void testBuildAutodetectCommand() {
        JobDetails job = new JobDetails();
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

        List<String> command = ProcessCtrl.buildAutodetectCommand(job, logger, null, false);

        assertEquals(16, command.size());
        assertTrue(command.contains(ProcessCtrl.AUTODETECT_PATH));
        assertTrue(command.contains(ProcessCtrl.BATCH_SPAN_ARG + "100"));
        assertTrue(command.contains(ProcessCtrl.BUCKET_SPAN_ARG + "120"));
        assertTrue(command.contains(ProcessCtrl.LATENCY_ARG + "360"));
        assertTrue(command.contains(ProcessCtrl.PERIOD_ARG + "20"));
        assertTrue(command.contains(ProcessCtrl.SUMMARY_COUNT_FIELD_ARG + "summaryField"));
        assertTrue(command.contains(ProcessCtrl.RESULT_FINALIZATION_WINDOW_ARG + "2"));
        assertTrue(command.contains(ProcessCtrl.MULTIVARIATE_BY_FIELDS_ARG));

        assertTrue(command.contains(ProcessCtrl.LENGTH_ENCODED_INPUT_ARG));
        assertTrue(command.contains(ProcessCtrl.MAX_ANOMALY_RECORDS_ARG));

        assertTrue(command.contains(ProcessCtrl.TIME_FIELD_ARG + "tf"));
        assertTrue(command.contains(ProcessCtrl.LOG_ID_ARG + "unit-test-job"));

        assertTrue(command.contains(ProcessCtrl.PER_PARTITION_NORMALIZATION));

        int expectedPersistInterval = 10800 + ProcessCtrl.calculateStaggeringInterval(job.getId());
        assertTrue(command.contains(ProcessCtrl.PERSIST_INTERVAL_ARG + expectedPersistInterval));
        int expectedMaxQuantileInterval = 21600 + ProcessCtrl.calculateStaggeringInterval(job.getId());
        assertTrue(command.contains(ProcessCtrl.MAX_QUANTILE_INTERVAL_ARG + expectedMaxQuantileInterval));
        assertTrue(command.contains(ProcessCtrl.IGNORE_DOWNTIME_ARG));
    }

    public void testBuildAutodetectCommand_defaultTimeField() {
        JobDetails job = new JobDetails();
        job.setId("unit-test-job");

        List<String> command = ProcessCtrl.buildAutodetectCommand(job, logger, null, false);

        assertTrue(command.contains(ProcessCtrl.TIME_FIELD_ARG + "time"));
    }

    public void testBuildAutodetectCommand_givenPersistModelState() {
        JobDetails job = new JobDetails();
        job.setId("unit-test-job");

        System.setProperty(ProcessCtrl.DONT_PERSIST_MODEL_STATE, "true");

        int expectedPersistInterval = 10800 + ProcessCtrl.calculateStaggeringInterval(job.getId());

        List<String> command = ProcessCtrl.buildAutodetectCommand(job, logger, null, false);
        assertFalse(command.contains(ProcessCtrl.PERSIST_INTERVAL_ARG + expectedPersistInterval));

        System.getProperties().remove(ProcessCtrl.DONT_PERSIST_MODEL_STATE);

        command = ProcessCtrl.buildAutodetectCommand(job, logger, null, false);
        assertTrue(command.contains(ProcessCtrl.PERSIST_INTERVAL_ARG + expectedPersistInterval));
    }

    public void testBuildAutodetectCommand_GivenNoIgnoreDowntime() {
        JobDetails job = new JobDetails();
        job.setId("foo");

        List<String> command = ProcessCtrl.buildAutodetectCommand(job, logger, null, false);

        assertFalse(command.contains("--ignoreDowntime"));
    }

    public void testBuildAutodetectCommand_GivenIgnoreDowntimeParam() {
        JobDetails job = new JobDetails();
        job.setId("foo");

        List<String> command = ProcessCtrl.buildAutodetectCommand(job, logger, null, true);

        assertTrue(command.contains("--ignoreDowntime"));
    }

    public void testBuildNormaliserCommand() throws IOException {
        String jobId = "unit-test-job";

        List<String> command = ProcessCtrl.buildNormaliserCommand(jobId, 300, true);

        assertEquals(5, command.size());
        assertTrue(command.contains(ProcessCtrl.NORMALIZE_PATH));
        assertTrue(command.contains(ProcessCtrl.BUCKET_SPAN_ARG + "300"));
        assertTrue(command.contains(ProcessCtrl.LOG_ID_ARG + jobId));
        assertTrue(command.contains(ProcessCtrl.LENGTH_ENCODED_INPUT_ARG));
        assertTrue(command.contains(ProcessCtrl.PER_PARTITION_NORMALIZATION));
    }
}
