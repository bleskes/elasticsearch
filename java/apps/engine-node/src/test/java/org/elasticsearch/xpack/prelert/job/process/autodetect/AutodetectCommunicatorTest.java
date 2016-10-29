package org.elasticsearch.xpack.prelert.job.process.autodetect;

import org.apache.logging.log4j.core.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.AnalysisConfig;
import org.elasticsearch.xpack.prelert.job.DataDescription;
import org.elasticsearch.xpack.prelert.job.Detector;
import org.elasticsearch.xpack.prelert.job.JobConfiguration;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.alert.AlertObserver;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.persistence.JobResultsPersister;
import org.elasticsearch.xpack.prelert.job.process.autodetect.output.parsing.ResultsReader;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.DataLoadParams;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.InterimResultsParams;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.TimeRange;
import org.elasticsearch.xpack.prelert.job.status.StatusReporter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import static org.elasticsearch.mock.orig.Mockito.mock;
import static org.elasticsearch.mock.orig.Mockito.verify;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class AutodetectCommunicatorTest extends ESTestCase {

    public void testAddRemoveAlertObserver() throws IOException {
        AutodetectCommunicator communicator =
                new AutodetectCommunicator(createJobDetails(), mockAutodetectProcessWithOutputStream(), mock(Logger.class),
                        mock(JobResultsPersister.class), mock(StatusReporter.class));

        AlertObserver ao = mock(AlertObserver.class);
        communicator.addAlertObserver(ao);
        assertTrue(communicator.removeAlertObserver(ao));

        communicator.close();
    }

    public void testWriteResetBucketsControlMessage() throws IOException {
        DataLoadParams params = new DataLoadParams(TimeRange.builder().startTime("1").endTime("2").build());
        AutodetectProcess process = mockAutodetectProcessWithOutputStream();

        AutodetectCommunicator communicator =
                new AutodetectCommunicator(createJobDetails(), process, mock(Logger.class),
                        mock(JobResultsPersister.class), mock(StatusReporter.class));

        communicator.writeResetBucketsControlMessage(params);
        verify(process).writeResetBucketsControlMessage(params);
    }

    public void tesWriteUpdateConfigMessage() throws IOException {
        AutodetectProcess process = mockAutodetectProcessWithOutputStream();

        AutodetectCommunicator communicator =
                new AutodetectCommunicator(createJobDetails(), process, mock(Logger.class),
                        mock(JobResultsPersister.class), mock(StatusReporter.class));

        String config = "";
        communicator.writeUpdateConfigMessage(config);
        verify(process).writeUpdateConfigMessage(config);
    }

    public void testFlushJob() throws IOException {
        AutodetectProcess process = mockAutodetectProcessWithOutputStream();
        when(process.isProcessAlive()).thenReturn(true);

        AutodetectCommunicator communicator =
                new AutodetectCommunicator(createJobDetails(), process, mock(Logger.class),
                        mock(JobResultsPersister.class), mock(StatusReporter.class));

        InterimResultsParams params = InterimResultsParams.builder().build();
        communicator.flushJob(params);
        verify(process).flushJob(params);
    }

    public void testFlushJob_throwsIfProcessIsDead() throws IOException {
        AutodetectProcess process = mockAutodetectProcessWithOutputStream();
        when(process.isProcessAlive()).thenReturn(false);
        when(process.readError()).thenReturn("Mock process is dead");

        AutodetectCommunicator communicator =
                new AutodetectCommunicator(createJobDetails(), process, mock(Logger.class),
                        mock(JobResultsPersister.class), mock(StatusReporter.class));

        InterimResultsParams params = InterimResultsParams.builder().build();
        ElasticsearchException e = ESTestCase.expectThrows(ElasticsearchException.class, () -> communicator.flushJob(params));
        assertEquals("Flush failed: Unexpected death of the Autodetect process flushing job. Mock process is dead", e.getMessage());
        assertEquals(ErrorCodes.NATIVE_PROCESS_ERROR.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testFlushJob_throwsOnTimeout() throws IOException {
        AutodetectProcess process = mockAutodetectProcessWithOutputStream();
        when(process.isProcessAlive()).thenReturn(true);
        when(process.readError()).thenReturn("Mock process has stalled");

        ResultsReader resultsReader = mock(ResultsReader.class);
        when(resultsReader.waitForFlushAcknowledgement(anyString(), any())).thenReturn(false);

        AutodetectCommunicator communicator =
                new AutodetectCommunicator(createJobDetails(), process, mock(Logger.class), mock(StatusReporter.class), resultsReader);

        InterimResultsParams params = InterimResultsParams.builder().build();
        ElasticsearchException e = ESTestCase.expectThrows(ElasticsearchException.class, () -> communicator.flushJob(params, 1, 1));
        assertEquals("Timed out flushing job. Mock process has stalled", e.getMessage());
        assertEquals(ErrorCodes.NATIVE_PROCESS_ERROR.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testClose() throws IOException {
        AutodetectProcess process = mockAutodetectProcessWithOutputStream();

        AutodetectCommunicator communicator =
                new AutodetectCommunicator(createJobDetails(), process, mock(Logger.class),
                        mock(JobResultsPersister.class), mock(StatusReporter.class));

        communicator.close();
        verify(process).close();
    }

    private JobDetails createJobDetails() {
        JobDetails jobDetails = new JobConfiguration().build();

        DataDescription dd = new DataDescription();
        dd.setTimeField("timeField");

        AnalysisConfig ac = new AnalysisConfig();
        Detector detector = new Detector("metric", "value");
        detector.setByFieldName("host-metric");
        ac.setDetectors(Collections.singletonList(detector));

        jobDetails.setDataDescription(dd);
        jobDetails.setAnalysisConfig(ac);
        return jobDetails;
    }

    private AutodetectProcess mockAutodetectProcessWithOutputStream() throws IOException {
        InputStream io = mock(InputStream.class);
        when(io.read(any(byte [].class))).thenReturn(-1);
        AutodetectProcess process = mock(AutodetectProcess.class);
        when(process.out()).thenReturn(io);
        when(process.isProcessAlive()).thenReturn(true);
        return process;
    }

}
