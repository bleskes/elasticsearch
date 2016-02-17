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

package com.prelert.job.manager;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
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
import com.prelert.job.DataCounts;
import com.prelert.job.JobDetails;
import com.prelert.job.JobSchedulerStatus;
import com.prelert.job.UnknownJobException;
import com.prelert.job.audit.Auditor;
import com.prelert.job.data.extraction.DataExtractor;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.exceptions.TooManyJobsException;
import com.prelert.job.logging.JobLoggerFactory;
import com.prelert.job.messages.Messages;
import com.prelert.job.persistence.JobProvider;
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
    private static final int ADDITIONAL_QUERY_DELAY_MS = 100;
    private static final int EMPTY_DATA_WARN_COUNT = 10;

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
    private volatile Long m_LastEndTimeMs;
    private volatile Logger m_Logger;
    private volatile JobSchedulerStatus m_Status;
    private volatile boolean m_IsLookbackOnly;
    private volatile boolean m_HasDataExtractionProblems;
    private volatile String m_LastDataExtractionProblem;
    private volatile int m_EmptyDataCount;

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
        m_QueryDelayMs = queryDelay.toMillis() + ADDITIONAL_QUERY_DELAY_MS;
        m_DataExtractor = Objects.requireNonNull(dataExtractor);
        m_DataProcessor = Objects.requireNonNull(dataProcessor);
        m_JobProvider = Objects.requireNonNull(jobProvider);
        m_JobLoggerFactory = Objects.requireNonNull(jobLoggerFactory);
        m_Status = JobSchedulerStatus.STOPPED;
        m_EmptyDataCount = 0;
    }

    private Runnable createNextTask()
    {
        return () -> {
            long start = m_LastEndTimeMs;
            long end = toIntervalStartEpochMs(new Date());
            extractAndProcessData(start, end);
        };
    }

    private long toIntervalStartEpochMs(Date date)
    {
        return toIntervalStartEpochMs(date.getTime());
    }

    private long toIntervalStartEpochMs(long epochMs)
    {
        return (epochMs / m_FrequencyMs) * m_FrequencyMs;
    }

    private void extractAndProcessData(long start, long end)
    {
        if (start == end)
        {
            return;
        }

        boolean hadErrors = m_HasDataExtractionProblems;
        String previousProblem = m_LastDataExtractionProblem;

        Date latestRecordTimestamp = null;
        m_HasDataExtractionProblems = false;
        m_LastDataExtractionProblem = null;
        m_DataExtractor.newSearch(String.valueOf(start), String.valueOf(end), m_Logger);
        while (m_DataExtractor.hasNext() && m_Status == JobSchedulerStatus.STARTED
                && !m_HasDataExtractionProblems)
        {
            Optional<InputStream> extractedData = tryExtractingNextAvailableData();
            if (extractedData.isPresent())
            {
                DataCounts counts = submitData(extractedData.get());
                if (counts.getLatestRecordTimeStamp() != null)
                {
                    latestRecordTimestamp = counts.getLatestRecordTimeStamp();
                }
            }
        }

        // If there was a failure return without advancing time in order to retry
        if (m_HasDataExtractionProblems)
        {
            if (!Objects.equals(previousProblem, m_LastDataExtractionProblem))
            {
                audit().error(Messages.getMessage(Messages.JOB_AUDIT_SCHEDULE_DATA_EXTRACTION_ERROR,
                        m_LastDataExtractionProblem));
            }
            return;
        }

        if (hadErrors)
        {
            auditDataExtractionRecovered();
        }

        updateEmptyDataCount(latestRecordTimestamp == null);
        updateLastEndTime(latestRecordTimestamp, end);
        makeResultsAvailable();
    }

    private Auditor audit()
    {
        return m_JobProvider.audit(m_JobId);
    }

    private void auditDataExtractionRecovered()
    {
        audit().info(Messages.getMessage(Messages.JOB_AUDIT_SCHEDULE_DATA_EXTRACTION_RECOVERED));
    }

    private Optional<InputStream> tryExtractingNextAvailableData()
    {
        try
        {
            return m_DataExtractor.next();
        }
        catch (IOException e)
        {
            m_HasDataExtractionProblems = true;
            m_LastDataExtractionProblem = e.getMessage();
            m_Logger.error("An error occurred while extracting data", e);
            return Optional.empty();
        }
    }

    private void updateEmptyDataCount(boolean empty)
    {
        if (empty && m_EmptyDataCount < EMPTY_DATA_WARN_COUNT)
        {
            m_EmptyDataCount++;
            if (m_EmptyDataCount == EMPTY_DATA_WARN_COUNT)
            {
                audit().warning(Messages.getMessage(Messages.JOB_AUDIT_SCHEDULE_NO_DATA));
            }
        }
        else if (!empty)
        {
            if (m_EmptyDataCount >= EMPTY_DATA_WARN_COUNT)
            {
                auditDataExtractionRecovered();
            }
            m_EmptyDataCount = 0;
        }
    }

    private void updateLastEndTime(Date latestRecordTimestamp, long searchEndTimeMs)
    {
        if (latestRecordTimestamp == null)
        {
            return;
        }

        if (isInRealTimeMode())
        {
            m_LastEndTimeMs = Math.min(searchEndTimeMs, alignToBucketEnd(latestRecordTimestamp));
        }
        else
        {
            m_LastEndTimeMs = latestRecordTimestamp.getTime() + 1;
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
                | MissingFieldException | JobInUseException
                | HighProportionOfBadTimestampsException | OutOfOrderRecordsException
                | TooManyJobsException | MalformedJsonException e)
        {
            m_Logger.error("An error has occurred while submitting data to job '" + m_JobId + "'", e);
            m_HasDataExtractionProblems = true;
            m_LastDataExtractionProblem = e.getMessage();
        }
        return new DataCounts();
    }

    private Supplier<LocalDateTime> calculateNextTime()
    {
        return () -> {
            long nowMs = new Date().getTime();
            long intervalSurplus = nowMs - toIntervalStartEpochMs(nowMs);
            Date nextTime = new Date(nowMs - intervalSurplus + m_FrequencyMs + m_QueryDelayMs);
            return LocalDateTime.ofInstant(nextTime.toInstant(), ZoneId.systemDefault());
        };
    }

    public synchronized void start(JobDetails job, long startMs, OptionalLong endMs)
            throws CannotStartSchedulerException
    {
        if (m_Status != JobSchedulerStatus.STOPPED)
        {
            throw new CannotStartSchedulerException(m_JobId, m_Status);
        }

        m_Logger = m_JobLoggerFactory.newLogger(m_JobId);
        updateStatus(JobSchedulerStatus.STARTED);
        m_LookbackExecutor = Executors.newSingleThreadExecutor();
        initLastEndTime(job);
        long lookbackStart = (m_LastEndTimeMs != null && m_LastEndTimeMs > startMs) ?
                m_LastEndTimeMs : startMs;
        long lookbackEnd =  endMs.orElse(System.currentTimeMillis() - m_QueryDelayMs);
        m_IsLookbackOnly = endMs.isPresent();
        m_LookbackExecutor.execute(createLookbackAndStartRealTimeTask(lookbackStart, lookbackEnd));
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
            m_LastEndTimeMs = job.getCounts().getLatestRecordTimeStamp().getTime() + MILLIS_IN_SECOND;
        }
    }

    private Runnable createLookbackAndStartRealTimeTask(long start, long end)
    {
        return () -> {
            m_Logger.info("Starting lookback");
            auditLookbackStarted(start, end);
            extractAndProcessData(start, end);
            m_Logger.info("Lookback has finished");
            audit().info(Messages.getMessage(Messages.JOB_AUDIT_SCHEDULE_LOOKBACK_COMPLETED));
            if (m_Status == JobSchedulerStatus.STARTED)
            {
                if (m_IsLookbackOnly)
                {
                    finishLookback();
                }
                else
                {
                    startRealTime();
                }
            }
            m_LookbackExecutor.shutdown();
        };
    }

    private void auditLookbackStarted(long start, long end)
    {
        String msg = Messages.getMessage(Messages.JOB_AUDIT_SCHEDULE_STARTED_FROM_TO,
                TimeUtils.formatEpochMillisAsIso(start), TimeUtils.formatEpochMillisAsIso(end));
        audit().info(msg);
    }

    private void finishLookback()
    {
        try
        {
            m_DataProcessor.closeJob(m_JobId);
        } catch (UnknownJobException | NativeProcessRunException | JobInUseException e)
        {
            m_Logger.error("An error has occurred while closing the job", e);
        }
        updateFinalStatusAndCloseLogger(JobSchedulerStatus.STOPPED);
    }

    private void updateFinalStatusAndCloseLogger(JobSchedulerStatus finalStatus)
    {
        synchronized (this)
        {
            updateStatus(finalStatus);
            m_JobLoggerFactory.close(m_JobId, m_Logger);
            m_Logger = null;
            if (finalStatus == JobSchedulerStatus.STOPPED)
            {
                audit().info(Messages.getMessage(Messages.JOB_AUDIT_SCHEDULE_STOPPED));
            }
        }
    }

    private void startRealTime()
    {
        m_Logger.info("Entering real-time mode");
        audit().info(Messages.getMessage(Messages.JOB_AUDIT_SCHEDULE_STARTED_REALTIME));
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
        awaitTermination();
        updateFinalStatusAndCloseLogger(JobSchedulerStatus.STOPPED);
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
        awaitTermination();
        updateFinalStatusAndCloseLogger(JobSchedulerStatus.STARTED);
    }

    private void awaitTermination()
    {
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

    public boolean isStarted()
    {
        return m_Status == JobSchedulerStatus.STARTED;
    }
}
