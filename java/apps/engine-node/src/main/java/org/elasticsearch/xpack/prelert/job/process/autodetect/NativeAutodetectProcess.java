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

import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.logging.CppLogMessageHandler;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.DataLoadParams;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.InterimResultsParams;
import org.elasticsearch.xpack.prelert.job.process.autodetect.writer.ControlMsgToProcessWriter;
import org.elasticsearch.xpack.prelert.job.process.autodetect.writer.LengthEncodedWriter;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Autodetect process using native code.
 */
public class NativeAutodetectProcess implements AutodetectProcess {
    private static final Logger LOGGER = Loggers.getLogger(NativeAutodetectProcess.class);

    private final CppLogMessageHandler cppLogHandler;
    private final OutputStream processInStream;
    private final InputStream processOutStream;
    private final LengthEncodedWriter recordWriter;
    private final ZonedDateTime startTime;
    private final int numberOfAnalysisFields;
    private final List<Path> filesToDelete;
    private Thread logTailThread;

    public NativeAutodetectProcess(String jobId, InputStream logStream, OutputStream processInStream,
                                   InputStream processOutStream, int numberOfAnalysisFields, List<Path> filesToDelete) {
        cppLogHandler = new CppLogMessageHandler(jobId, logStream);
        this.processInStream = new BufferedOutputStream(processInStream);
        this.processOutStream = processOutStream;
        this.recordWriter = new LengthEncodedWriter(this.processInStream);
        startTime = ZonedDateTime.now();
        this.numberOfAnalysisFields = numberOfAnalysisFields;
        this.filesToDelete = filesToDelete;
    }

    public void tailLogsInThread() {
        logTailThread = new Thread(() -> {
            try {
                cppLogHandler.tailStream();
                cppLogHandler.close();
            } catch (IOException e) {
                LOGGER.error("Error tailing C++ process logs", e);
            }
        });
        logTailThread.start();
    }

    @Override
    public void writeRecord(String[] record) throws IOException {
        recordWriter.writeRecord(record);
    }

    @Override
    public void writeResetBucketsControlMessage(DataLoadParams params) throws IOException {
        ControlMsgToProcessWriter writer = new ControlMsgToProcessWriter(recordWriter, numberOfAnalysisFields);
        writer.writeResetBucketsMessage(params);
    }

    @Override
    public void writeUpdateConfigMessage(String config) throws IOException {
        ControlMsgToProcessWriter writer = new ControlMsgToProcessWriter(recordWriter, numberOfAnalysisFields);
        writer.writeUpdateConfigMessage(config);
    }

    @Override
    public String flushJob(InterimResultsParams params) throws IOException {
        ControlMsgToProcessWriter writer = new ControlMsgToProcessWriter(recordWriter, numberOfAnalysisFields);
        writer.writeCalcInterimMessage(params);
        return writer.writeFlushMessage();
    }

    @Override
    public void flushStream() throws IOException {
        recordWriter.flush();
    }

    @Override
    public void close() throws IOException {
        try {
            // closing its input causes the process to exit
            processInStream.close();

            // wait for the process to exit by waiting for end-of-file on the named pipe connected to its logger
            if (logTailThread != null) {
                logTailThread.join();
            }

            if (cppLogHandler.seenFatalError()) {
                throw ExceptionsHelper.serverError(cppLogHandler.getErrors(), ErrorCodes.NATIVE_PROCESS_ERROR);
            }
            LOGGER.info("Process exited");
        } catch (InterruptedException e) {
            LOGGER.warn("Exception closing the running native process");
            Thread.currentThread().interrupt();
        } finally {
            deleteAssociatedFiles();
        }
    }

    public void deleteAssociatedFiles() throws IOException {
        if (filesToDelete == null) {
            return;
        }

        for (Path fileToDelete : filesToDelete) {
            if (Files.deleteIfExists(fileToDelete)) {
                LOGGER.debug("Deleted file {}", fileToDelete::toString);
            } else {
                LOGGER.warn("Failed to delete file {}", fileToDelete::toString);
            }
        }
    }

    @Override
    public InputStream error() {
        return new ByteArrayInputStream(cppLogHandler.getErrors().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public InputStream out() {
        return processOutStream;
    }

    @Override
    public ZonedDateTime getProcessStartTime() {
        return startTime;
    }

    @Override
    public boolean isProcessAlive() {
        // Sanity check make sure the process hasn't terminated already
        return !cppLogHandler.hasLogStreamEnded();
    }

    @Override
    public String readError() {
        return cppLogHandler.getErrors();
    }
}
