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
import org.elasticsearch.common.ParseFieldMatcherSupplier;
import org.elasticsearch.xpack.prelert.job.DataCounts;
import org.elasticsearch.xpack.prelert.job.Job;
import org.elasticsearch.xpack.prelert.job.alert.AlertObserver;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.job.persistence.JobResultsPersister;
import org.elasticsearch.xpack.prelert.job.process.autodetect.output.parsing.ResultsReader;
import org.elasticsearch.xpack.prelert.job.process.autodetect.output.parsing.StateReader;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.DataLoadParams;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.InterimResultsParams;
import org.elasticsearch.xpack.prelert.job.process.autodetect.writer.DataToProcessWriter;
import org.elasticsearch.xpack.prelert.job.process.autodetect.writer.DataToProcessWriterFactory;
import org.elasticsearch.xpack.prelert.job.process.normalizer.noop.NoOpRenormaliser;
import org.elasticsearch.xpack.prelert.job.status.CountingInputStream;
import org.elasticsearch.xpack.prelert.job.status.StatusReporter;
import org.elasticsearch.xpack.prelert.job.transform.TransformConfigs;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.ZonedDateTime;

public class AutodetectCommunicator implements Closeable {

    private static final int DEFAULT_TRY_COUNT = 5;
    private static final int DEFAULT_TRY_TIMEOUT_SECS = 6;

    private final AutodetectProcess autodetectProcess;
    private final Logger jobLogger;
    private final DataToProcessWriter autoDetectWriter;
    private final StateReader stateReader;
    private final ResultsReader resultsReader;
    private final Thread outputParserThread;
    private final Thread stateParserThread;
    private final StatusReporter statusReporter;


    public AutodetectCommunicator(Job job, AutodetectProcess process, Logger jobLogger,
            JobResultsPersister resultsPersister, StatusReporter statusReporter, ParseFieldMatcherSupplier parseFieldMatcherSupplier) {
        this.autodetectProcess = process;
        this.jobLogger = jobLogger;
        this.statusReporter = statusReporter;

        // TODO Port the normalizer from the old project
        resultsReader = new ResultsReader(new NoOpRenormaliser(), resultsPersister, process.getProcessOutStream(), this.jobLogger,
                job.getAnalysisConfig().getUsePerPartitionNormalization(), parseFieldMatcherSupplier);

        stateReader = new StateReader(resultsPersister, process.getPersistStream(), this.jobLogger);

        // NORELEASE - use ES ThreadPool
        outputParserThread = new Thread(resultsReader, job.getId() + "-Bucket-Parser");
        outputParserThread.start();
        // NORELEASE - use ES ThreadPool
        stateParserThread = new Thread(stateReader, job.getId() + "-State-Parser");
        stateParserThread.start();

        this.autoDetectWriter = createProcessWriter(job, process, statusReporter);
    }

    AutodetectCommunicator(Job job, AutodetectProcess process, Logger jobLogger,
            StatusReporter statusReporter, ResultsReader resultsReader, StateReader stateReader) {
        this.autodetectProcess = process;
        this.jobLogger = jobLogger;
        this.statusReporter = statusReporter;
        this.resultsReader = resultsReader;
        this.stateReader = stateReader;
        // NORELEASE - use ES ThreadPool
        outputParserThread = new Thread(resultsReader, job.getId() + "-Bucket-Parser");
        outputParserThread.start();
        // NORELEASE - use ES ThreadPool
        stateParserThread = new Thread(stateReader, job.getId() + "-State-Parser");
        stateParserThread.start();

        this.autoDetectWriter = createProcessWriter(job, process, statusReporter);
    }

    private DataToProcessWriter createProcessWriter(Job job, AutodetectProcess process, StatusReporter statusReporter) {
        return DataToProcessWriterFactory.create(true, process, job.getDataDescription(), job.getAnalysisConfig(),
                job.getSchedulerConfig(), new TransformConfigs(job.getTransforms()) , statusReporter, jobLogger);
    }

    public DataCounts writeToJob(InputStream inputStream)
            throws IOException {

        checkProcessIsAlive();

        CountingInputStream countingStream = new CountingInputStream(inputStream, statusReporter);
        DataCounts results = autoDetectWriter.write(countingStream);
        autoDetectWriter.flush();
        return results;
    }

    @Override
    public void close() throws IOException {
        checkProcessIsAlive();
        autodetectProcess.close();
        waitForResultsParser();
    }

    private void waitForResultsParser() {
        try {
            outputParserThread.join();
        } catch (InterruptedException e) {
            jobLogger.error("Error joining parser thread", e);
        }
    }

    public void writeResetBucketsControlMessage(DataLoadParams params) throws IOException {
        checkProcessIsAlive();
        autodetectProcess.writeResetBucketsControlMessage(params);
    }

    public void writeUpdateConfigMessage(String config) throws IOException {
        checkProcessIsAlive();
        autodetectProcess.writeUpdateConfigMessage(config);
    }

    public void flushJob(InterimResultsParams params) throws IOException {
        flushJob(params, DEFAULT_TRY_COUNT, DEFAULT_TRY_TIMEOUT_SECS);
    }

    void flushJob(InterimResultsParams params, int tryCount, int tryTimeoutSecs) throws IOException {
        String flushId = autodetectProcess.flushJob(params);

        Duration intermittentTimeout = Duration.ofSeconds(tryTimeoutSecs);
        boolean isFlushComplete = false;
        while (isFlushComplete == false && --tryCount >= 0) {
            // Check there wasn't an error in the flush
            if (!autodetectProcess.isProcessAlive()) {

                String msg = Messages.getMessage(Messages.AUTODETECT_FLUSH_UNEXPTECTED_DEATH) + " " + autodetectProcess.readError();
                jobLogger.error(msg);
                throw ExceptionsHelper.serverError(msg);
            }
            isFlushComplete = resultsReader.waitForFlushAcknowledgement(flushId, intermittentTimeout);
        }

        if (!isFlushComplete) {
            String msg = Messages.getMessage(Messages.AUTODETECT_FLUSH_TIMEOUT) + " " + autodetectProcess.readError();
            jobLogger.error(msg);
            throw ExceptionsHelper.serverError(msg);
        }

        // We also have to wait for the normaliser to become idle so that we block
        // clients from querying results in the middle of normalisation.
        resultsReader.waitUntilRenormaliserIsIdle();
    }

    /**
     * Throws an exception if the process has exited
     */
    private void checkProcessIsAlive() {
        if (!autodetectProcess.isProcessAlive()) {
            String errorMsg = "Unexpected death of autodetect: " + autodetectProcess.readError();
            jobLogger.error(errorMsg);
            throw ExceptionsHelper.serverError(errorMsg);
        }
    }

    public void addAlertObserver(AlertObserver ao) {
        resultsReader.addAlertObserver(ao);
    }

    public boolean removeAlertObserver(AlertObserver ao) {
        return resultsReader.removeAlertObserver(ao);
    }

    public ZonedDateTime getProcessStartTime() {
        return autodetectProcess.getProcessStartTime();
    }

}
