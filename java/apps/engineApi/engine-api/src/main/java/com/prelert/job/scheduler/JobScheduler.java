/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/

package com.prelert.job.scheduler;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.google.common.base.Preconditions;
import com.prelert.job.DataCounts;
import com.prelert.job.JobDetails;
import com.prelert.job.JobSchedulerStatus;
import com.prelert.job.UnknownJobException;
import com.prelert.job.audit.Auditor;
import com.prelert.job.data.extraction.DataExtractor;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.exceptions.LicenseViolationException;
import com.prelert.job.exceptions.TooManyJobsException;
import com.prelert.job.logging.JobLoggerFactory;
import com.prelert.job.manager.actions.ActionGuardian;
import com.prelert.job.manager.actions.ScheduledAction;
import com.prelert.job.messages.Messages;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.process.exceptions.DataUploadException;
import com.prelert.job.process.exceptions.MalformedJsonException;
import com.prelert.job.process.exceptions.MissingFieldException;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.process.params.DataLoadParams;
import com.prelert.job.process.params.InterimResultsParams;
import com.prelert.job.process.params.InterimResultsParams.Builder;
import com.prelert.job.process.params.TimeRange;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;
import com.prelert.utils.scheduler.TaskScheduler;
import com.prelert.utils.time.TimeUtils;

public class JobScheduler
{
    private static final int MILLIS_IN_SECOND = 1000;
    private static final DataLoadParams DATA_LOAD_PARAMS =
            new DataLoadParams(false, new TimeRange(null, null));
    private static final int STOP_TIMEOUT_MINUTES = 60;

    /**
     * A small delay to ensure task is run after the specified time
     */
    private static final int NEXT_TASK_DELAY_MS = 100;

    private final String m_JobId;
    private final long m_BucketSpanMs;
    private final long m_FrequencyMs;
    private final long m_QueryDelayMs;
    private final DataExtractor m_DataExtractor;
    private final DataProcessor m_DataProcessor;
    private final JobProvider m_JobProvider;
    private final JobLoggerFactory m_JobLoggerFactory;
    private volatile ExecutorService m_LookbackExecutor;
    private volatile TaskScheduler m_RealTimeScheduler;
    private volatile long m_LookbackStartTimeMs;
    private volatile Long m_LastEndTimeMs;
    private volatile Logger m_Logger;
    private volatile JobSchedulerStatus m_Status;
    private volatile boolean m_IsLookbackOnly;
    private final ProblemTracker m_ProblemTracker;
    private volatile Optional<ActionGuardian<ScheduledAction>.ActionTicket> m_ActionTicket;

    /**
     * Constructor
     *
     * @param jobId the job Id
     * @param bucketSpan the bucket span
     * @param frequency the frequency of queries - it is also the results update refresh interval
     * @param queryDelay the query delay
     * @param dataExtractor the data extractor
     * @param dataProcessor the data processor
     * @param jobProvider the job provider
     * @param jobLoggerFactory the factory to create a job logger
     */
    public JobScheduler(String jobId, Duration bucketSpan, Duration frequency, Duration queryDelay,
            DataExtractor dataExtractor, DataProcessor dataProcessor,
            JobProvider jobProvider, JobLoggerFactory jobLoggerFactory)
    {
        m_JobId = jobId;
        m_BucketSpanMs = bucketSpan.toMillis();
        m_FrequencyMs = frequency.toMillis();
        m_QueryDelayMs = queryDelay.toMillis();
        m_DataExtractor = Objects.requireNonNull(dataExtractor);
        m_DataProcessor = Objects.requireNonNull(dataProcessor);
        m_JobProvider = Objects.requireNonNull(jobProvider);
        m_JobLoggerFactory = Objects.requireNonNull(jobLoggerFactory);
        m_Status = JobSchedulerStatus.STOPPED;
        m_ProblemTracker = new ProblemTracker(() -> m_JobProvider.audit(m_JobId));
        m_ActionTicket = Optional.empty();
    }

    private Runnable createNextTask()
    {
        return () -> {
            long start = m_LastEndTimeMs == null ? m_LookbackStartTimeMs : m_LastEndTimeMs;
            long nowMinusQueryDelay = System.currentTimeMillis() - m_QueryDelayMs;
            long end = toIntervalStartEpochMs(nowMinusQueryDelay);
            extractAndProcessData(start, end);
        };
    }

    private long toIntervalStartEpochMs(long epochMs)
    {
        return (epochMs / m_FrequencyMs) * m_FrequencyMs;
    }

    private void extractAndProcessData(long start, long end)
    {
        if (end <= start)
        {
            return;
        }

        Long previousLastEndTimeMs = m_LastEndTimeMs;
        Date latestRecordTimestamp = null;
        newSearch(start, end);
        while (m_DataExtractor.hasNext() && !m_ProblemTracker.hasProblems())
        {
            Optional<InputStream> extractedData = tryExtractingNextAvailableData();
            if (extractedData.isPresent())
            {
                DataCounts counts = submitData(extractedData.get());
                if (counts.getLatestRecordTimeStamp() != null)
                {
                    latestRecordTimestamp = counts.getLatestRecordTimeStamp();
                    m_LastEndTimeMs = latestRecordTimestamp.getTime() + 1;
                }
            }
        }

        updateLastEndTime(latestRecordTimestamp, end);
        if (m_ProblemTracker.updateEmptyDataCount(latestRecordTimestamp == null))
        {
            closeJob();
        }
        m_ProblemTracker.finishReport();

        if (m_LastEndTimeMs != null && !m_LastEndTimeMs.equals(previousLastEndTimeMs))
        {
            makeResultsAvailable();
        }
    }

    private void newSearch(long start, long end)
    {
        try
        {
            m_DataExtractor.newSearch(start, end, m_Logger);
        }
        catch (IOException e)
        {
            m_ProblemTracker.reportExtractionProblem(e.getMessage());
            m_Logger.error("An error has occurred while starting a new search for ["
                    + start + ", " + end + ")", e);
        }
    }

    private Auditor audit()
    {
        return m_JobProvider.audit(m_JobId);
    }

    private Optional<InputStream> tryExtractingNextAvailableData()
    {
        try
        {
            return m_DataExtractor.next();
        }
        catch (IOException e)
        {
            m_ProblemTracker.reportExtractionProblem(e.getMessage());
            m_Logger.error("An error occurred while extracting data", e);
            return Optional.empty();
        }
    }

    private void updateLastEndTime(Date latestRecordTimestamp, long searchEndTimeMs)
    {
        // Only update last end time when:
        //   1. We have seen data, i.e. there is a non null latestRecordTimestamp
        //   2. The scheduler is still running
        //   3. There are no problems
        //   4. We are in real-time mode
        // in order to retry from the last time when data were successfully processed
        if (latestRecordTimestamp != null
                && m_Status == JobSchedulerStatus.STARTED
                && !m_ProblemTracker.hasProblems()
                && isInRealTimeMode())
        {
            m_LastEndTimeMs = Math.min(searchEndTimeMs, alignToBucketEnd(latestRecordTimestamp));
        }
    }

    private long alignToBucketEnd(Date time)
    {
        long timeMs = time.getTime();
        long result = (timeMs / m_BucketSpanMs) * m_BucketSpanMs;
        return result == timeMs ? result : result + m_BucketSpanMs;
    }

    private boolean isInRealTimeMode()
    {
        return m_RealTimeScheduler != null;
    }

    private void makeResultsAvailable()
    {
        Preconditions.checkState(m_LastEndTimeMs != null);

        Builder flushParamsBuilder = InterimResultsParams.newBuilder().calcInterim(true);
        if (isInRealTimeMode())
        {
            flushParamsBuilder.advanceTime(m_LastEndTimeMs / MILLIS_IN_SECOND);
        }
        try
        {
            // This ensures the results are available as soon as possible in the
            // case where we're searching specific time periods once and only once
            m_DataProcessor.flushJob(m_JobId, flushParamsBuilder.build());
        }
        catch (UnknownJobException | NativeProcessRunException | JobInUseException e)
        {
            m_Logger.error("An error has occurred while flushing job '" + m_JobId + "'", e);
        }
    }

    private DataCounts submitData(InputStream stream)
    {
        try
        {
            return m_DataProcessor.submitDataLoadJob(m_JobId, stream, DATA_LOAD_PARAMS);
        }
        catch (JsonParseException | UnknownJobException | NativeProcessRunException
                | MissingFieldException | JobInUseException | TooManyJobsException
                | HighProportionOfBadTimestampsException | OutOfOrderRecordsException
                | LicenseViolationException | MalformedJsonException | DataUploadException e)
        {
            m_Logger.error("An error has occurred while submitting data to job '" + m_JobId + "'", e);
            m_ProblemTracker.reportAnalysisProblem(e.getMessage());
        }
        return new DataCounts();
    }

    private Supplier<LocalDateTime> calculateNextTime()
    {
        return () -> {
            long nextTimeMs = toIntervalStartEpochMs(System.currentTimeMillis() + m_FrequencyMs)
                    + NEXT_TASK_DELAY_MS;
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(nextTimeMs), ZoneId.systemDefault());
        };
    }

    public synchronized void start(JobDetails job, long startMs, OptionalLong endMs,
                                ActionGuardian<ScheduledAction>.ActionTicket actionTicket)
            throws CannotStartSchedulerException
    {
        if (m_Status != JobSchedulerStatus.STOPPED)
        {
            throw new CannotStartSchedulerException(m_JobId, m_Status);
        }

        m_ActionTicket = Optional.of(actionTicket);

        m_Logger = m_JobLoggerFactory.newLogger(m_JobId);
        updateStatus(JobSchedulerStatus.STARTED);
        m_LookbackExecutor = Executors.newSingleThreadExecutor();
        initLastEndTime(job);
        m_LookbackStartTimeMs = (m_LastEndTimeMs != null && m_LastEndTimeMs > startMs) ?
                m_LastEndTimeMs : startMs;
        long lookbackEnd =  endMs.orElse(System.currentTimeMillis() - m_QueryDelayMs);
        m_IsLookbackOnly = endMs.isPresent();
        m_LookbackExecutor.execute(createLookbackAndStartRealTimeTask(m_LookbackStartTimeMs, lookbackEnd));
        m_LookbackExecutor.shutdown();
    }

    private void updateStatus(JobSchedulerStatus status)
    {
        m_Logger.info("Scheduler status changed to " + status);

        m_Status = status;
        Map<String, Object> updates = new HashMap<>();
        updates.put(JobDetails.SCHEDULER_STATUS, status);
        try
        {
            m_JobProvider.updateJob(m_JobId, updates);
        }
        catch (UnknownJobException e)
        {
            throw new IllegalStateException(e);
        }
    }

    private void initLastEndTime(JobDetails job)
    {
        if (job.getCounts() != null && job.getCounts().getLatestRecordTimeStamp() != null)
        {
            m_LastEndTimeMs = job.getCounts().getLatestRecordTimeStamp().getTime() + 1;
        }
    }

    private Runnable createLookbackAndStartRealTimeTask(long start, long end)
    {
        return () -> {
            boolean runLookback = end > start;
            if (runLookback)
            {
                runLookback(start, end);
            }
            if (m_Status == JobSchedulerStatus.STARTED)
            {
                if (m_IsLookbackOnly)
                {
                    finishLookback();
                }
                else
                {
                    startRealTime(runLookback);
                }
            }
        };
    }

    private void runLookback(long start, long end)
    {
        m_Logger.info("Starting lookback");
        auditLookbackStarted(start, end);
        extractAndProcessData(start, end);
        m_Logger.info("Lookback has finished");
        audit().info(Messages.getMessage(Messages.JOB_AUDIT_SCHEDULER_LOOKBACK_COMPLETED));
    }

    private void auditLookbackStarted(long start, long end)
    {
        String msg = Messages.getMessage(Messages.JOB_AUDIT_SCHEDULER_STARTED_FROM_TO,
                TimeUtils.formatEpochMillisAsIso(start), TimeUtils.formatEpochMillisAsIso(end));
        audit().info(msg);
    }

    private void finishLookback()
    {
        synchronized (this)
        {
            releaseActionTicket();

            if (m_Status != JobSchedulerStatus.STARTED)
            {
                // Stop has already been called
                return;
            }
            updateStatus(JobSchedulerStatus.STOPPING);
        }
        closeJob();
        updateFinalStatusAndCleanUp(JobSchedulerStatus.STOPPED);
    }

    private void closeJob()
    {
        try
        {
            m_DataProcessor.closeJob(m_JobId);
        }
        catch (UnknownJobException | NativeProcessRunException | JobInUseException e)
        {
            m_Logger.error("An error has occurred while closing the job", e);
        }
    }

    private void updateFinalStatusAndCleanUp(JobSchedulerStatus finalStatus)
    {
        synchronized (this)
        {
            updateStatus(finalStatus);
            m_DataExtractor.clear();
            m_JobLoggerFactory.close(m_JobId, m_Logger);
            m_Logger = null;
            if (finalStatus == JobSchedulerStatus.STOPPED)
            {
                audit().info(Messages.getMessage(Messages.JOB_AUDIT_SCHEDULER_STOPPED));
            }
        }
    }

    private void startRealTime(boolean wasLookbackRun)
    {
        if (wasLookbackRun)
        {
            audit().info(Messages.getMessage(Messages.JOB_AUDIT_SCHEDULER_CONTINUED_REALTIME));
        }
        else
        {
            audit().info(Messages.getMessage(Messages.JOB_AUDIT_SCHEDULER_STARTED_REALTIME));
        }
        m_Logger.info("Entering real-time mode");
        m_RealTimeScheduler = new TaskScheduler(createNextTask(), calculateNextTime());
        m_RealTimeScheduler.start();
    }

    /**
     * Stops the scheduler and blocks the current thread until
     * the scheduler is stopped. At the end of the stopping process
     * the status is set to STOPPED.
     * @throws CannotStopSchedulerException if the scheduler cannot be stopped or fails to stop
     */
    public void stopManual() throws CannotStopSchedulerException
    {
        synchronized (this)
        {
            if (m_Status != JobSchedulerStatus.STARTED)
            {
                String msg = Messages.getMessage(Messages.JOB_SCHEDULER_CANNOT_STOP_IN_CURRENT_STATE,
                        m_JobId, m_Status);
                throw new CannotStopSchedulerException(msg);
            }
            updateStatus(JobSchedulerStatus.STOPPING);
        }
        cancelAndAwaitTermination();
        updateFinalStatusAndCleanUp(JobSchedulerStatus.STOPPED);
        releaseActionTicket();
    }

    /**
     * Stops the scheduler and blocks the current thread until
     * the scheduler is stopped. At the end of the stopping process
     * the status is set to STARTED.
     */
    public void stopAuto()
    {
        synchronized (this)
        {
            if (m_Status != JobSchedulerStatus.STARTED)
            {
                return;
            }
            updateStatus(JobSchedulerStatus.STOPPING);
        }
        cancelAndAwaitTermination();
        updateFinalStatusAndCleanUp(JobSchedulerStatus.STARTED);
        releaseActionTicket();
    }

    private void cancelAndAwaitTermination()
    {
        m_DataExtractor.cancel();
        if (awaitLookbackTermination() == false || stopRealtimeScheduler() == false)
        {
            m_Logger.error("Unable to stop the scheduler.");
        }
    }

    private boolean awaitLookbackTermination()
    {
        try
        {
            return m_LookbackExecutor.awaitTermination(STOP_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean stopRealtimeScheduler()
    {
        return m_RealTimeScheduler == null
                || m_RealTimeScheduler.stop(STOP_TIMEOUT_MINUTES, TimeUnit.MINUTES);
    }

    private void releaseActionTicket()
    {
        if (m_ActionTicket.isPresent())
        {
            m_ActionTicket.get().close();
            m_ActionTicket = Optional.empty();
        }
    }

    public boolean isStopped()
    {
        return m_Status == JobSchedulerStatus.STOPPED;
    }

    public boolean isStarted()
    {
        return m_Status == JobSchedulerStatus.STARTED;
    }

    public JobSchedulerStatus getStatus()
    {
        return m_Status;
    }

    public String getJobId()
    {
        return m_JobId;
    }
}
