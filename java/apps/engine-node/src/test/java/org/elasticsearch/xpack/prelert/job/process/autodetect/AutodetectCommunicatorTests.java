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
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class AutodetectCommunicatorTests extends ESTestCase {

    public void testAddRemoveAlertObserver() throws IOException {
        AutodetectCommunicator communicator =
                new AutodetectCommunicator(createJobDetails(), mockAutodetectProcessWithOutputStream(), Mockito.mock(Logger.class),
                        Mockito.mock(JobResultsPersister.class), Mockito.mock(StatusReporter.class));

        AlertObserver ao = Mockito.mock(AlertObserver.class);
        communicator.addAlertObserver(ao);
        assertTrue(communicator.removeAlertObserver(ao));

        communicator.close();
    }

    public void testWriteResetBucketsControlMessage() throws IOException {
        DataLoadParams params = new DataLoadParams(TimeRange.builder().startTime("1").endTime("2").build());
        AutodetectProcess process = mockAutodetectProcessWithOutputStream();

        try (AutodetectCommunicator communicator =
                new AutodetectCommunicator(createJobDetails(), process, Mockito.mock(Logger.class), Mockito.mock(JobResultsPersister.class),
                        Mockito.mock(StatusReporter.class))) {

            communicator.writeResetBucketsControlMessage(params);
            Mockito.verify(process).writeResetBucketsControlMessage(params);
        }
    }

    public void tesWriteUpdateConfigMessage() throws IOException {
        AutodetectProcess process = mockAutodetectProcessWithOutputStream();

        try (AutodetectCommunicator communicator =
                new AutodetectCommunicator(createJobDetails(), process, Mockito.mock(Logger.class), Mockito.mock(JobResultsPersister.class),
                        Mockito.mock(StatusReporter.class))) {

            String config = "";
            communicator.writeUpdateConfigMessage(config);
            Mockito.verify(process).writeUpdateConfigMessage(config);
        }
    }

    public void testFlushJob() throws IOException {
        AutodetectProcess process = mockAutodetectProcessWithOutputStream();
        when(process.isProcessAlive()).thenReturn(true);

        try (AutodetectCommunicator communicator =
                new AutodetectCommunicator(createJobDetails(), process, Mockito.mock(Logger.class), Mockito.mock(JobResultsPersister.class),
                        Mockito.mock(StatusReporter.class))) {

            InterimResultsParams params = InterimResultsParams.builder().build();
            communicator.flushJob(params);
            Mockito.verify(process).flushJob(params);
        }
    }

    public void testFlushJob_throwsIfProcessIsDead() throws IOException {
        AutodetectProcess process = mockAutodetectProcessWithOutputStream();
        when(process.isProcessAlive()).thenReturn(false);
        when(process.readError()).thenReturn("Mock process is dead");

        @SuppressWarnings("resource")
        AutodetectCommunicator communicator = new AutodetectCommunicator(createJobDetails(), process, Mockito.mock(Logger.class),
                Mockito.mock(JobResultsPersister.class), Mockito.mock(StatusReporter.class));

        InterimResultsParams params = InterimResultsParams.builder().build();
        ElasticsearchException e = ESTestCase.expectThrows(ElasticsearchException.class, () -> communicator.flushJob(params));
        assertEquals("Flush failed: Unexpected death of the Autodetect process flushing job. Mock process is dead", e.getMessage());
        assertEquals(ErrorCodes.NATIVE_PROCESS_ERROR.getValueString(), e.getHeader("errorCode").get(0));
    }

    public void testFlushJob_throwsOnTimeout() throws IOException {
        AutodetectProcess process = mockAutodetectProcessWithOutputStream();
        when(process.isProcessAlive()).thenReturn(true);
        when(process.readError()).thenReturn("Mock process has stalled");

        ResultsReader resultsReader = Mockito.mock(ResultsReader.class);
        when(resultsReader.waitForFlushAcknowledgement(anyString(), any())).thenReturn(false);

        try (AutodetectCommunicator communicator = new AutodetectCommunicator(createJobDetails(), process, Mockito.mock(Logger.class),
                Mockito.mock(StatusReporter.class), resultsReader)) {

            InterimResultsParams params = InterimResultsParams.builder().build();
            ElasticsearchException e = ESTestCase.expectThrows(ElasticsearchException.class, () -> communicator.flushJob(params, 1, 1));
            assertEquals("Timed out flushing job. Mock process has stalled", e.getMessage());
            assertEquals(ErrorCodes.NATIVE_PROCESS_ERROR.getValueString(), e.getHeader("errorCode").get(0));
        }
    }

    public void testClose() throws IOException {
        AutodetectProcess process = mockAutodetectProcessWithOutputStream();

        AutodetectCommunicator communicator =
                new AutodetectCommunicator(createJobDetails(), process, Mockito.mock(Logger.class), Mockito.mock(JobResultsPersister.class),
                        Mockito.mock(StatusReporter.class));

        communicator.close();
        Mockito.verify(process).close();
    }

    private JobDetails createJobDetails() {
        JobDetails jobDetails = new JobConfiguration().build();

        DataDescription dd = new DataDescription();
        dd.setTimeField("timeField");

        AnalysisConfig ac = new AnalysisConfig();
        Detector.Builder detector = new Detector.Builder("metric", "value");
        detector.setByFieldName("host-metric");
        ac.setDetectors(Collections.singletonList(detector.build()));

        jobDetails.setDataDescription(dd);
        jobDetails.setAnalysisConfig(ac);
        return jobDetails;
    }

    private AutodetectProcess mockAutodetectProcessWithOutputStream() throws IOException {
        InputStream io = Mockito.mock(InputStream.class);
        when(io.read(any(byte [].class))).thenReturn(-1);
        AutodetectProcess process = Mockito.mock(AutodetectProcess.class);
        when(process.out()).thenReturn(io);
        when(process.isProcessAlive()).thenReturn(true);
        return process;
    }

}
