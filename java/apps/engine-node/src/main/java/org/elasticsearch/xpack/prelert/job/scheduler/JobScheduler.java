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
package org.elasticsearch.xpack.prelert.job.scheduler;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.common.util.concurrent.EsExecutors;
import org.elasticsearch.index.mapper.DateFieldMapper;
import org.elasticsearch.xpack.prelert.job.DataCounts;
import org.elasticsearch.xpack.prelert.job.Job;
import org.elasticsearch.xpack.prelert.job.JobSchedulerStatus;
import org.elasticsearch.xpack.prelert.job.audit.Auditor;
import org.elasticsearch.xpack.prelert.job.data.DataProcessor;
import org.elasticsearch.xpack.prelert.job.extraction.DataExtractor;
import org.elasticsearch.xpack.prelert.job.logging.JobLoggerFactory;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.job.metadata.Allocation;
import org.elasticsearch.xpack.prelert.job.persistence.BucketsQueryBuilder;
import org.elasticsearch.xpack.prelert.job.persistence.JobProvider;
import org.elasticsearch.xpack.prelert.job.persistence.QueryPage;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.DataLoadParams;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.InterimResultsParams;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.TimeRange;
import org.elasticsearch.xpack.prelert.job.results.Bucket;
import org.elasticsearch.xpack.prelert.utils.scheduler.TaskScheduler;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * A scheduler that manages a job configured to pull data from a source.
 */
public class JobScheduler {
    private static final DataLoadParams DATA_LOAD_PARAMS = new DataLoadParams(TimeRange.builder().build());

    /**
     * A small delay to ensure task is run after the specified time
     */
    private static final int NEXT_TASK_DELAY_MS = 100;

    private final String jobId;
    private final long bucketSpanMs;
    private final long frequencyMs;
    private final long queryDelayMs;
    private final DataExtractor dataExtractor;
    private final DataProcessor dataProcessor;
    private final JobProvider jobProvider;
    private final JobLoggerFactory jobLoggerFactory;
    private volatile ExecutorService lookbackExecutor;
    private volatile TaskScheduler realTimeScheduler;
    private volatile long lookbackStartTimeMs;

    /** The end time time of the last search (inclusive) */
    private volatile Long lastEndTimeMs;

    private volatile Logger logger;
    private volatile boolean isLookbackOnly;
    private final ProblemTracker problemTracker;

    private final Supplier<JobSchedulerStatus> statusSupplier;
    private final Listener listener;

    /**
     * Constructor
     *
     * @param jobId
     *            the job Id
     * @param bucketSpan
     *            the bucket span
     * @param frequency
     *            the frequency of queries - it is also the results update
     *            refresh interval
     * @param queryDelay
     *            the query delay
     * @param dataExtractor
     *            the data extractor
     * @param dataProcessor
     *            the data processor
     * @param jobProvider
     *            the job provider
     * @param jobLoggerFactory
     *            the factory to create a job logger
     */
    public JobScheduler(String jobId, Duration bucketSpan, Duration frequency, Duration queryDelay, DataExtractor dataExtractor,
            DataProcessor dataProcessor, JobProvider jobProvider, JobLoggerFactory jobLoggerFactory,
            Supplier<JobSchedulerStatus> statusSupplier, Listener listener) {
        this.jobId = jobId;
        bucketSpanMs = bucketSpan.toMillis();
        frequencyMs = frequency.toMillis();
        queryDelayMs = queryDelay.toMillis();
        this.dataExtractor = Objects.requireNonNull(dataExtractor);
        this.dataProcessor = Objects.requireNonNull(dataProcessor);
        this.jobProvider = Objects.requireNonNull(jobProvider);
        this.jobLoggerFactory = Objects.requireNonNull(jobLoggerFactory);
        problemTracker = new ProblemTracker(() -> this.jobProvider.audit(this.jobId));
        this.statusSupplier = Objects.requireNonNull(statusSupplier);
        this.listener = Objects.requireNonNull(listener);
    }

    private Runnable createNextTask() {
        return () -> {
            long start = lastEndTimeMs == null ? lookbackStartTimeMs : lastEndTimeMs + 1;
            long nowMinusQueryDelay = System.currentTimeMillis() - queryDelayMs;
            long end = toIntervalStartEpochMs(nowMinusQueryDelay);
            extractAndProcessData(start, end);
        };
    }

    private long toIntervalStartEpochMs(long epochMs) {
        return (epochMs / frequencyMs) * frequencyMs;
    }

    private void extractAndProcessData(long start, long end) {
        if (end <= start) {
            return;
        }

        long recordCount = 0;
        newSearch(start, end);
        while (dataExtractor.hasNext() && !problemTracker.hasProblems()) {
            Optional<InputStream> extractedData = tryExtractingNextAvailableData();
            if (extractedData.isPresent()) {
                DataCounts counts = submitData(extractedData.get());
                recordCount += counts.getProcessedRecordCount();
                if (counts.getLatestRecordTimeStamp() != null) {
                    lastEndTimeMs = counts.getLatestRecordTimeStamp().getTime();
                }
            }
        }

        lastEndTimeMs = Math.max(lastEndTimeMs == null ? 0 : lastEndTimeMs, end - 1);

        if (problemTracker.updateEmptyDataCount(recordCount == 0)) {
            closeJob();
        }
        problemTracker.finishReport();

        makeResultsAvailable();
    }

    private void newSearch(long start, long end) {
        try {
            dataExtractor.newSearch(start, end, logger);
        } catch (IOException e) {
            problemTracker.reportExtractionProblem(e.getMessage());
            logger.error("An error has occurred while starting a new search for [" + start + ", " + end + ")", e);
        }
    }

    private Auditor audit() {
        return jobProvider.audit(jobId);
    }

    private Optional<InputStream> tryExtractingNextAvailableData() {
        try {
            return dataExtractor.next();
        } catch (IOException e) {
            problemTracker.reportExtractionProblem(e.getMessage());
            logger.error("An error occurred while extracting data", e);
            return Optional.empty();
        }
    }

    private boolean isInRealTimeMode() {
        return realTimeScheduler != null;
    }

    private void makeResultsAvailable() {
        InterimResultsParams.Builder flushParamsBuilder = InterimResultsParams.builder().calcInterim(true);
        if (isInRealTimeMode() && lastEndTimeMs != null) {
            flushParamsBuilder.advanceTime(String.valueOf(lastEndTimeMs));
        }
        try {
            // This ensures the results are available as soon as possible in the
            // case where we're searching specific time periods once and only
            // once
            dataProcessor.flushJob(jobId, flushParamsBuilder.build());
        } catch (ElasticsearchException e) {
            logger.error("An error has occurred while flushing job '" + jobId + "'", e);
        }
    }

    private DataCounts submitData(InputStream stream) {
        try {
            return dataProcessor.processData(jobId, stream, DATA_LOAD_PARAMS);
        } catch (ElasticsearchException e) {
            logger.error("An error has occurred while submitting data to job '" + jobId + "'", e);
            problemTracker.reportAnalysisProblem(e.getMessage());
        }
        return new DataCounts();
    }

    private Supplier<LocalDateTime> calculateNextTime() {
        return () -> {
            long nextTimeMs = toIntervalStartEpochMs(System.currentTimeMillis() + frequencyMs) + NEXT_TASK_DELAY_MS;
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(nextTimeMs), ZoneId.systemDefault());
        };
    }

    public void start(Job job, Allocation allocation) {
        if (logger != null) {
            throw new IllegalStateException("Cannot start while scheduler is already started");
        }

        logger = jobLoggerFactory.newLogger(jobId);
        logger.info("Scheduler started");
        lookbackExecutor = Executors.newSingleThreadExecutor(EsExecutors.daemonThreadFactory("job_scheduler"));
        initLastEndTime(job);
        long startMs = allocation.getSchedulerState().getStartTimeMillis();
        OptionalLong endMs = allocation.getSchedulerState().getEndTimeMillis() == null ? OptionalLong.empty()
                : OptionalLong.of(allocation.getSchedulerState().getEndTimeMillis());
        lookbackStartTimeMs = (lastEndTimeMs != null && lastEndTimeMs + 1 > startMs) ? lastEndTimeMs + 1 : startMs;
        long lookbackEnd = endMs.orElse(System.currentTimeMillis() - queryDelayMs);
        isLookbackOnly = endMs.isPresent();
        lookbackExecutor.execute(createLookbackAndStartRealTimeTask(lookbackStartTimeMs, lookbackEnd));
        lookbackExecutor.shutdown();
    }

    private void initLastEndTime(Job job) {
        long lastEndTime = Math.max(getLatestFinalBucketEndTimeMs(), getLatestRecordTimestamp(job));
        if (lastEndTime > 0) {
            lastEndTimeMs = lastEndTime;
        }
    }

    private long getLatestFinalBucketEndTimeMs() {
        long latestFinalBucketEndMs = -1L;
        BucketsQueryBuilder.BucketsQuery latestBucketQuery = new BucketsQueryBuilder()
                .sortField(Bucket.TIMESTAMP.getPreferredName())
                .sortDescending(true).take(1)
                .includeInterim(false)
                .build();
        QueryPage<Bucket> buckets;
        try {
            buckets = jobProvider.buckets(jobId, latestBucketQuery);
            if (buckets.hits().size() == 1) {
                latestFinalBucketEndMs = buckets.hits().get(0).getTimestamp().getTime() + bucketSpanMs - 1;
            }
        } catch (ResourceNotFoundException e) {
            logger.error("Could not retrieve latest bucket timestamp", e);
        }
        return latestFinalBucketEndMs;
    }

    private long getLatestRecordTimestamp(Job job) {
        long latestRecordTimeMs = -1L;
        if (job.getCounts() != null && job.getCounts().getLatestRecordTimeStamp() != null) {
            latestRecordTimeMs = job.getCounts().getLatestRecordTimeStamp().getTime();
        }
        return latestRecordTimeMs;
    }

    private Runnable createLookbackAndStartRealTimeTask(long start, long end) {
        return () -> {
            boolean runLookback = end > start;
            if (runLookback) {
                runLookback(start, end);
            }
            if (statusSupplier.get() == JobSchedulerStatus.STARTED) {
                if (isLookbackOnly) {
                    finishLookback();
                } else {
                    startRealTime(runLookback);
                }
            }
        };
    }

    private void runLookback(long start, long end) {
        logger.info("Starting lookback");
        auditLookbackStarted(start, end);
        extractAndProcessData(start, end);
        logger.info("Lookback has finished");
        audit().info(Messages.getMessage(Messages.JOB_AUDIT_SCHEDULER_LOOKBACK_COMPLETED));
    }

    private void auditLookbackStarted(long start, long end) {
        String msg = Messages.getMessage(Messages.JOB_AUDIT_SCHEDULER_STARTED_FROM_TO,
                DateFieldMapper.DEFAULT_DATE_TIME_FORMATTER.printer().print(start),
                DateFieldMapper.DEFAULT_DATE_TIME_FORMATTER.printer().print(end));
        audit().info(msg);
    }

    private void finishLookback() {
        listener.statusChanged(JobSchedulerStatus.STOPPING);
        notifyFinalStatusAndCleanUp(JobSchedulerStatus.STOPPED);
    }

    private void notifyFinalStatusAndCleanUp(JobSchedulerStatus finalStatus) {
        synchronized (this) {
            if (logger == null) {
                return;
            }
            listener.statusChanged(finalStatus);
            logger.info("Scheduler is stopped");
            dataExtractor.clear();
            jobLoggerFactory.close(jobId, logger);
            logger = null;
            audit().info(Messages.getMessage(Messages.JOB_AUDIT_SCHEDULER_STOPPED));
        }
    }

    private void closeJob() {
        try {
            dataProcessor.closeJob(jobId);
        } catch (ElasticsearchException e) {
            logger.error("An error has occurred while closing the job", e);
        }
    }

    private void startRealTime(boolean wasLookbackRun) {
        if (wasLookbackRun) {
            audit().info(Messages.getMessage(Messages.JOB_AUDIT_SCHEDULER_CONTINUED_REALTIME));
        } else {
            audit().info(Messages.getMessage(Messages.JOB_AUDIT_SCHEDULER_STARTED_REALTIME));
        }
        logger.info("Entering real-time mode");
        realTimeScheduler = new TaskScheduler(createNextTask(), calculateNextTime());
        realTimeScheduler.start();
    }

    /**
     * Stops the scheduler and blocks the current thread until the scheduler is
     * stopped. At the end of the stopping process the status is set to STOPPED.
     */
    public void stopManual() {
        stop(JobSchedulerStatus.STOPPED);
    }

    /**
     * Stops the scheduler and blocks the current thread until the scheduler is
     * stopped. At the end of the stopping process the status is set to STARTED.
     */
    public void stopAuto() {
        // NORELEASE This should be setting the state to RECOVERY
        stop(JobSchedulerStatus.STARTED);
    }

    private void stop(JobSchedulerStatus finalStatus) {
        logger.info("Scheduler is stopping");
        cancelAndAwaitTermination();
        notifyFinalStatusAndCleanUp(finalStatus);
    }

    private void cancelAndAwaitTermination() {
        dataExtractor.cancel();
        if (awaitLookbackTermination() == false || stopRealtimeScheduler() == false) {
            logger.error("Unable to stop the scheduler.");
        }
    }

    private boolean awaitLookbackTermination() {
        try {
            return lookbackExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean stopRealtimeScheduler() {
        return realTimeScheduler == null || realTimeScheduler.stop(Long.MAX_VALUE, TimeUnit.DAYS);
    }

    public interface Listener {
        void statusChanged(JobSchedulerStatus newStatus);
    }
}
