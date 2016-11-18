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
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.xpack.prelert.job.AnalysisConfig;
import org.elasticsearch.xpack.prelert.job.DataDescription;
import org.elasticsearch.xpack.prelert.job.Detector;
import org.elasticsearch.xpack.prelert.job.Job;
import org.elasticsearch.xpack.prelert.job.persistence.JobResultsPersister;
import org.elasticsearch.xpack.prelert.job.process.autodetect.output.parsing.AutoDetectResultProcessor;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.DataLoadParams;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.InterimResultsParams;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.TimeRange;
import org.elasticsearch.xpack.prelert.job.status.StatusReporter;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.concurrent.ExecutorService;

import static org.elasticsearch.mock.orig.Mockito.doAnswer;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AutodetectCommunicatorTests extends ESTestCase {

    public void testWriteResetBucketsControlMessage() throws IOException {
        DataLoadParams params = new DataLoadParams(TimeRange.builder().startTime("1").endTime("2").build());
        AutodetectProcess process = mockAutodetectProcessWithOutputStream();
        try (AutodetectCommunicator communicator = createAutodetectCommunicator(process, mock(AutoDetectResultProcessor.class))) {
            communicator.writeResetBucketsControlMessage(params);
            Mockito.verify(process).writeResetBucketsControlMessage(params);
        }
    }

    public void tesWriteUpdateConfigMessage() throws IOException {
        AutodetectProcess process = mockAutodetectProcessWithOutputStream();
        try (AutodetectCommunicator communicator = createAutodetectCommunicator(process, mock(AutoDetectResultProcessor.class))) {
            String config = "";
            communicator.writeUpdateConfigMessage(config);
            Mockito.verify(process).writeUpdateConfigMessage(config);
        }
    }

    public void testFlushJob() throws IOException {
        AutodetectProcess process = mockAutodetectProcessWithOutputStream();
        when(process.isProcessAlive()).thenReturn(true);
        AutoDetectResultProcessor processor = mock(AutoDetectResultProcessor.class);
        when(processor.waitForFlushAcknowledgement(anyString(), any())).thenReturn(true);
        try (AutodetectCommunicator communicator = createAutodetectCommunicator(process, processor)) {
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
        AutodetectCommunicator communicator = createAutodetectCommunicator(process, mock(AutoDetectResultProcessor.class));
        InterimResultsParams params = InterimResultsParams.builder().build();
        ElasticsearchException e = ESTestCase.expectThrows(ElasticsearchException.class, () -> communicator.flushJob(params));
        assertEquals("Flush failed: Unexpected death of the Autodetect process flushing job. Mock process is dead", e.getMessage());
    }

    public void testFlushJob_throwsOnTimeout() throws IOException {
        AutodetectProcess process = mockAutodetectProcessWithOutputStream();
        when(process.isProcessAlive()).thenReturn(true);
        when(process.readError()).thenReturn("Mock process has stalled");
        AutoDetectResultProcessor autoDetectResultProcessor = Mockito.mock(AutoDetectResultProcessor.class);
        when(autoDetectResultProcessor.waitForFlushAcknowledgement(anyString(), any())).thenReturn(false);
        try (AutodetectCommunicator communicator = createAutodetectCommunicator(process, mock(AutoDetectResultProcessor.class))) {
            InterimResultsParams params = InterimResultsParams.builder().build();
            ElasticsearchException e = ESTestCase.expectThrows(ElasticsearchException.class, () -> communicator.flushJob(params, 1, 1));
            assertEquals("Timed out flushing job. Mock process has stalled", e.getMessage());
        }
    }

    public void testClose() throws IOException {
        AutodetectProcess process = mockAutodetectProcessWithOutputStream();
        AutodetectCommunicator communicator = createAutodetectCommunicator(process, mock(AutoDetectResultProcessor.class));
        communicator.close();
        Mockito.verify(process).close();
    }

    private Job createJobDetails() {
        Job.Builder builder = new Job.Builder("foo");

        DataDescription.Builder dd = new DataDescription.Builder();
        dd.setTimeField("timeField");

        Detector.Builder detector = new Detector.Builder("metric", "value");
        detector.setByFieldName("host-metric");
        AnalysisConfig.Builder ac = new AnalysisConfig.Builder(Collections.singletonList(detector.build()));

        builder.setDataDescription(dd);
        builder.setAnalysisConfig(ac);
        return builder.build();
    }

    private AutodetectProcess mockAutodetectProcessWithOutputStream() throws IOException {
        InputStream io = Mockito.mock(InputStream.class);
        when(io.read(any(byte [].class))).thenReturn(-1);
        AutodetectProcess process = Mockito.mock(AutodetectProcess.class);
        when(process.getProcessOutStream()).thenReturn(io);
        when(process.getPersistStream()).thenReturn(io);
        when(process.isProcessAlive()).thenReturn(true);
        return process;
    }

    private AutodetectCommunicator createAutodetectCommunicator(AutodetectProcess autodetectProcess,
                                                                AutoDetectResultProcessor autoDetectResultProcessor) throws IOException {
        ThreadPool threadPool = mock(ThreadPool.class);
        ExecutorService executorService = Mockito.mock(ExecutorService.class);
        doAnswer(invocation -> {
            ((Runnable) invocation.getArguments()[0]).run();
            return null;
        }).when(executorService).submit(any(Runnable.class));
        Mockito.when(threadPool.executor(ThreadPool.Names.GENERIC)).thenReturn(executorService);
        Logger jobLogger = Mockito.mock(Logger.class);
        JobResultsPersister resultsPersister = mock(JobResultsPersister.class);
        StatusReporter statusReporter = mock(StatusReporter.class);
        return new AutodetectCommunicator(threadPool, createJobDetails(), autodetectProcess, jobLogger, resultsPersister,
                statusReporter, autoDetectResultProcessor);
    }

}
