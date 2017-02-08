/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
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
package org.elasticsearch.xpack.ml.job.process.autodetect;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.xpack.ml.job.config.DataDescription;
import org.elasticsearch.xpack.ml.job.config.DetectionRule;
import org.elasticsearch.xpack.ml.job.config.Job;
import org.elasticsearch.xpack.ml.job.config.ModelDebugConfig;
import org.elasticsearch.xpack.ml.job.messages.Messages;
import org.elasticsearch.xpack.ml.job.process.CountingInputStream;
import org.elasticsearch.xpack.ml.job.process.DataCountsReporter;
import org.elasticsearch.xpack.ml.job.process.autodetect.output.AutoDetectResultProcessor;
import org.elasticsearch.xpack.ml.job.process.autodetect.params.DataLoadParams;
import org.elasticsearch.xpack.ml.job.process.autodetect.params.InterimResultsParams;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.DataCounts;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.ModelSizeStats;
import org.elasticsearch.xpack.ml.job.process.autodetect.writer.DataToProcessWriter;
import org.elasticsearch.xpack.ml.job.process.autodetect.writer.DataToProcessWriterFactory;
import org.elasticsearch.xpack.ml.utils.ExceptionsHelper;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AutodetectCommunicator implements Closeable {

    private static final Logger LOGGER = Loggers.getLogger(AutodetectCommunicator.class);
    private static final Duration FLUSH_PROCESS_CHECK_FREQUENCY = Duration.ofSeconds(1);

    private final Job job;
    private final DataCountsReporter dataCountsReporter;
    private final AutodetectProcess autodetectProcess;
    private final AutoDetectResultProcessor autoDetectResultProcessor;
    private final Consumer<Exception> handler;

    final AtomicReference<CountDownLatch> inUse = new AtomicReference<>();

    public AutodetectCommunicator(Job job, AutodetectProcess process, DataCountsReporter dataCountsReporter,
                                  AutoDetectResultProcessor autoDetectResultProcessor, Consumer<Exception> handler) {
        this.job = job;
        this.autodetectProcess = process;
        this.dataCountsReporter = dataCountsReporter;
        this.autoDetectResultProcessor = autoDetectResultProcessor;
        this.handler = handler;
    }

    public void writeJobInputHeader() throws IOException {
        createProcessWriter(Optional.empty()).writeHeader();
    }

    private DataToProcessWriter createProcessWriter(Optional<DataDescription> dataDescription) {
        return DataToProcessWriterFactory.create(true, autodetectProcess, dataDescription.orElse(job.getDataDescription()),
                job.getAnalysisConfig(), dataCountsReporter);
    }

    public DataCounts writeToJob(InputStream inputStream, DataLoadParams params) throws IOException {
        return checkAndRun(() -> Messages.getMessage(Messages.JOB_DATA_CONCURRENT_USE_UPLOAD, job.getId()), () -> {
            if (params.isResettingBuckets()) {
                autodetectProcess.writeResetBucketsControlMessage(params);
            }
            CountingInputStream countingStream = new CountingInputStream(inputStream, dataCountsReporter);

            DataToProcessWriter autoDetectWriter = createProcessWriter(params.getDataDescription());
            DataCounts results = autoDetectWriter.write(countingStream);
            autoDetectWriter.flush();
            return results;
        }, false);
    }

    @Override
    public void close() throws IOException {
        checkAndRun(() -> Messages.getMessage(Messages.JOB_DATA_CONCURRENT_USE_CLOSE, job.getId()), () -> {
            dataCountsReporter.close();
            autodetectProcess.close();
            autoDetectResultProcessor.awaitCompletion();
            handler.accept(null);
            return null;
        }, true);
    }


    public void writeUpdateModelDebugMessage(ModelDebugConfig config) throws IOException {
        checkAndRun(() -> Messages.getMessage(Messages.JOB_DATA_CONCURRENT_USE_UPDATE, job.getId()), () -> {
            autodetectProcess.writeUpdateModelDebugMessage(config);
            return null;
        }, false);
    }

    public void writeUpdateDetectorRulesMessage(int detectorIndex, List<DetectionRule> rules) throws IOException {
        checkAndRun(() -> Messages.getMessage(Messages.JOB_DATA_CONCURRENT_USE_UPDATE, job.getId()), () -> {
            autodetectProcess.writeUpdateDetectorRulesMessage(detectorIndex, rules);
            return null;
        }, false);
    }

    public void flushJob(InterimResultsParams params) throws IOException {
        checkAndRun(() -> Messages.getMessage(Messages.JOB_DATA_CONCURRENT_USE_FLUSH, job.getId()), () -> {
            String flushId = autodetectProcess.flushJob(params);
            waitFlushToCompletion(flushId);
            return null;
        }, false);
    }

    private void waitFlushToCompletion(String flushId) throws IOException {
        LOGGER.info("[{}] waiting for flush", job.getId());

        try {
            boolean isFlushComplete = autoDetectResultProcessor.waitForFlushAcknowledgement(flushId, FLUSH_PROCESS_CHECK_FREQUENCY);
            while (isFlushComplete == false) {
                checkProcessIsAlive();
                isFlushComplete = autoDetectResultProcessor.waitForFlushAcknowledgement(flushId, FLUSH_PROCESS_CHECK_FREQUENCY);
            }
        } finally {
            autoDetectResultProcessor.clearAwaitingFlush(flushId);
        }

        // We also have to wait for the normalizer to become idle so that we block
        // clients from querying results in the middle of normalization.
        autoDetectResultProcessor.waitUntilRenormalizerIsIdle();

        LOGGER.info("[{}] Flush completed", job.getId());
    }

    /**
     * Throws an exception if the process has exited
     */
    private void checkProcessIsAlive() {
        if (!autodetectProcess.isProcessAlive()) {
            ParameterizedMessage message =
                    new ParameterizedMessage("[{}] Unexpected death of autodetect: {}", job.getId(), autodetectProcess.readError());
            LOGGER.error(message);
            throw ExceptionsHelper.serverError(message.getFormattedMessage());
        }
    }

    public ZonedDateTime getProcessStartTime() {
        return autodetectProcess.getProcessStartTime();
    }

    public ModelSizeStats getModelSizeStats() {
        return autoDetectResultProcessor.modelSizeStats();
    }

    public DataCounts getDataCounts() {
        return dataCountsReporter.runningTotalStats();
    }

    private <T> T checkAndRun(Supplier<String> errorMessage, Callback<T> callback, boolean wait) throws IOException {
        CountDownLatch latch = new CountDownLatch(1);
        if (inUse.compareAndSet(null, latch)) {
            try {
                checkProcessIsAlive();
                return callback.run();
            } finally {
                latch.countDown();
                inUse.set(null);
            }
        } else {
            if (wait) {
                latch = inUse.get();
                if (latch != null) {
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new ElasticsearchStatusException(errorMessage.get(), RestStatus.TOO_MANY_REQUESTS);
                    }
                }
                checkProcessIsAlive();
                return callback.run();
            } else {
                throw new ElasticsearchStatusException(errorMessage.get(), RestStatus.TOO_MANY_REQUESTS);
            }
        }
    }

    private interface Callback<T> {

        T run() throws IOException;

    }

}
