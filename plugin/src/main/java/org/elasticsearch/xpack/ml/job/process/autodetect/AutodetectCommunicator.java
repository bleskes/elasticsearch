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
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.common.CheckedSupplier;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.util.concurrent.AbstractRunnable;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.xpack.ml.job.config.DataDescription;
import org.elasticsearch.xpack.ml.job.config.Job;
import org.elasticsearch.xpack.ml.job.config.JobUpdate;
import org.elasticsearch.xpack.ml.job.config.ModelPlotConfig;
import org.elasticsearch.xpack.ml.job.process.CountingInputStream;
import org.elasticsearch.xpack.ml.job.process.DataCountsReporter;
import org.elasticsearch.xpack.ml.job.process.autodetect.output.AutoDetectResultProcessor;
import org.elasticsearch.xpack.ml.job.process.autodetect.params.DataLoadParams;
import org.elasticsearch.xpack.ml.job.process.autodetect.params.InterimResultsParams;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.DataCounts;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.ModelSizeStats;
import org.elasticsearch.xpack.ml.job.process.autodetect.writer.DataToProcessWriter;
import org.elasticsearch.xpack.ml.job.process.autodetect.writer.DataToProcessWriterFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class AutodetectCommunicator implements Closeable {

    private static final Logger LOGGER = Loggers.getLogger(AutodetectCommunicator.class);
    private static final Duration FLUSH_PROCESS_CHECK_FREQUENCY = Duration.ofSeconds(1);

    private final Job job;
    private final DataCountsReporter dataCountsReporter;
    private final AutodetectProcess autodetectProcess;
    private final AutoDetectResultProcessor autoDetectResultProcessor;
    private final Consumer<Exception> handler;
    private final ExecutorService autodetectWorkerExecutor;
    private final NamedXContentRegistry xContentRegistry;

    AutodetectCommunicator(Job job, AutodetectProcess process,
            DataCountsReporter dataCountsReporter,
            AutoDetectResultProcessor autoDetectResultProcessor, Consumer<Exception> handler,
            NamedXContentRegistry xContentRegistry, ExecutorService autodetectWorkerExecutor) {
        this.job = job;
        this.autodetectProcess = process;
        this.dataCountsReporter = dataCountsReporter;
        this.autoDetectResultProcessor = autoDetectResultProcessor;
        this.handler = handler;
        this.xContentRegistry = xContentRegistry;
        this.autodetectWorkerExecutor = autodetectWorkerExecutor;
    }

    public void writeJobInputHeader() throws IOException {
        createProcessWriter(Optional.empty()).writeHeader();
    }

    private DataToProcessWriter createProcessWriter(Optional<DataDescription> dataDescription) {
        return DataToProcessWriterFactory.create(true, autodetectProcess,
                dataDescription.orElse(job.getDataDescription()), job.getAnalysisConfig(),
                dataCountsReporter, xContentRegistry);
    }

    public void writeToJob(InputStream inputStream, XContentType xContentType,
            DataLoadParams params, BiConsumer<DataCounts, Exception> handler) throws IOException {
        submitOperation(() -> {
            if (params.isResettingBuckets()) {
                autodetectProcess.writeResetBucketsControlMessage(params);
            }
            CountingInputStream countingStream = new CountingInputStream(inputStream, dataCountsReporter);

            DataToProcessWriter autoDetectWriter = createProcessWriter(params.getDataDescription());
            DataCounts results = autoDetectWriter.write(countingStream, xContentType);
            autoDetectWriter.flush();
            return results;
        }, handler);
    }

    @Override
    public void close() throws IOException {
        close(false, null);
    }

    /**
     * Closes job this communicator is encapsulating.
     *
     * @param restart   Whether the job should be restarted by persistent tasks
     * @param reason    The reason for closing the job
     */
    public void close(boolean restart, String reason) throws IOException {
        Future<?> future = autodetectWorkerExecutor.submit(() -> {
            checkProcessIsAlive();
            dataCountsReporter.close();
            autodetectProcess.close();
            autoDetectResultProcessor.awaitCompletion();
            handler.accept(restart ? new ElasticsearchException(reason) : null);
            LOGGER.info("[{}] job closed", job.getId());
            return null;
        });
        try {
            future.get();
            autodetectWorkerExecutor.shutdown();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            throw ExceptionsHelper.convertToElastic(e);
        }
    }


    public void writeUpdateProcessMessage(ModelPlotConfig config, List<JobUpdate.DetectorUpdate> updates,
                                          BiConsumer<Void, Exception> handler) throws IOException {
        submitOperation(() -> {
            if (config != null) {
                autodetectProcess.writeUpdateModelPlotMessage(config);
            }
            if (updates != null) {
                for (JobUpdate.DetectorUpdate update : updates) {
                    if (update.getRules() != null) {
                        autodetectProcess.writeUpdateDetectorRulesMessage(update.getIndex(), update.getRules());
                    }
                }
            }
            return null;
        }, handler);
    }

    public void flushJob(InterimResultsParams params, BiConsumer<Void, Exception> handler) throws IOException {
        submitOperation(() -> {
            String flushId = autodetectProcess.flushJob(params);
            waitFlushToCompletion(flushId);
            return null;
        }, handler);
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
            throw new ElasticsearchException(message.getFormattedMessage());
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

    private <T> void submitOperation(CheckedSupplier<T, IOException> operation, BiConsumer<T, Exception> handler) throws IOException {
        autodetectWorkerExecutor.execute(new AbstractRunnable() {
            @Override
            public void onFailure(Exception e) {
                handler.accept(null, e);
            }

            @Override
            protected void doRun() throws Exception {
                checkProcessIsAlive();
                handler.accept(operation.get(), null);
            }
        });
    }

}
