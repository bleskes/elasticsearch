package org.elasticsearch.xpack.prelert.job.process.autodetect;

import org.apache.logging.log4j.core.Logger;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.*;
import org.elasticsearch.xpack.prelert.job.alert.AlertObserver;
import org.elasticsearch.xpack.prelert.job.persistence.JobResultsPersister;
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
import static org.mockito.Mockito.when;

public class AutodetectCommunicatorTest extends ESTestCase {

    public void testAddRemoveAlertObserver() throws IOException {
        AutodetectCommunicator communicator =
                new AutodetectCommunicator(createJobDetails(), mockAutodetectProcess(), mock(Logger.class),
                        mock(JobResultsPersister.class), mock(StatusReporter.class));

        AlertObserver ao = mock(AlertObserver.class);
        communicator.addAlertObserver(ao);
        assertTrue(communicator.removeAlertObserver(ao));

        communicator.close();
    }

    public void testWriteResetBucketsControlMessage() throws IOException {
        DataLoadParams params = new DataLoadParams(new TimeRange(0l, 1l));
        AutodetectProcess process = mockAutodetectProcess();

        AutodetectCommunicator communicator =
                new AutodetectCommunicator(createJobDetails(), process, mock(Logger.class),
                        mock(JobResultsPersister.class), mock(StatusReporter.class));

        communicator.writeResetBucketsControlMessage(params);
        verify(process).writeResetBucketsControlMessage(params);
    }

    public void tesWriteUpdateConfigMessage() throws IOException {
        AutodetectProcess process = mockAutodetectProcess();

        AutodetectCommunicator communicator =
                new AutodetectCommunicator(createJobDetails(), process, mock(Logger.class),
                        mock(JobResultsPersister.class), mock(StatusReporter.class));

        String config = "";
        communicator.writeUpdateConfigMessage(config);
        verify(process).writeUpdateConfigMessage(config);
    }

    public void testFlushJob() throws IOException {
        AutodetectProcess process = mockAutodetectProcess();

        AutodetectCommunicator communicator =
                new AutodetectCommunicator(createJobDetails(), process, mock(Logger.class),
                        mock(JobResultsPersister.class), mock(StatusReporter.class));

        InterimResultsParams params = InterimResultsParams.newBuilder().build();
        communicator.flushJob(params);
        verify(process).flushJob(params);
    }

    public void testClose() throws IOException {
        AutodetectProcess process = mockAutodetectProcess();

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

    private AutodetectProcess mockAutodetectProcess() throws IOException {
        InputStream io = mock(InputStream.class);
        when(io.read(any(byte [].class))).thenReturn(-1);
        AutodetectProcess process = mock(AutodetectProcess.class);
        when(process.out()).thenReturn(io);
        return process;
    }

}
