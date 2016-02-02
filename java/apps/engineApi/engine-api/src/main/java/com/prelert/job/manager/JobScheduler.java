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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.google.common.annotations.VisibleForTesting;
import com.prelert.job.DataCounts;
import com.prelert.job.JobDetails;
import com.prelert.job.JobSchedulerStatus;
import com.prelert.job.SchedulerConfig;
import com.prelert.job.UnknownJobException;
import com.prelert.job.data.extraction.DataExtractor;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.exceptions.TooManyJobsException;
import com.prelert.job.logging.JobLoggerFactory;
import com.prelert.job.persistence.JobDetailsProvider;
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

public class JobScheduler
{
    private static final int MILLIS_IN_SECOND = 1000;
    private static final DataLoadParams DATA_LOAD_PARAMS =
            new DataLoadParams(false, new TimeRange(null, null));
    private static final int STOP_TIMEOUT_MINUTES = 60;
    private static final int ADDITIONAL_QUERY_DELAY_MS = 100;

    private final String m_JobId;
    private final long m_BucketSpanMs;
    private final long m_FrequencyMs;
    private final long m_QueryDelayMs;
    private final DataExtractor m_DataExtractor;
    private final DataProcessor m_DataProcessor;
    private final JobDetailsProvider m_JobProvider;
    private final JobLoggerFactory m_JobLoggerFactory;
    private volatile ExecutorService m_LookbackExecutor;
    private volatile TaskScheduler m_RealTimeScheduler;
    private volatile Long m_LastEndTimeMs;
    private volatile Logger m_Logger;
    private volatile JobSchedulerStatus m_Status;
    private volatile boolean m_IsLookbackOnly;
    private volatile boolean m_HasDataExtractionProblems;

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
            JobDetailsProvider jobProvider, JobLoggerFactory jobLoggerFactory)
    {
        m_JobId = jobId;
        m_BucketSpanMs = bucketSpan.toMillis();
        m_FrequencyMs = frequency.toMillis();
        m_QueryDelayMs = queryDelay.toMillis() + ADDITIONAL_QUERY_DELAY_MS;
        m_DataExtractor = Objects.requireNonNull(dataExtractor);
        m_DataProcessor = Objects.requireNonNull(dataProcessor);
        m_JobProvider = Objects.requireNonNull(jobProvider);
        m_JobLoggerFactory = Objects.requireNonNull(jobLoggerFactory);
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

        Date latestRecordTimestamp = null;
        m_HasDataExtractionProblems = false;
        m_DataExtractor.newSearch(String.valueOf(start), String.valueOf(end), m_Logger);
        while (m_DataExtractor.hasNext() && m_Status == JobSchedulerStatus.STARTED
                && !m_HasDataExtractionProblems)
        {
            m_HasDataExtractionProblems = false;
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
            return;
        }

        updateLastEndTime(latestRecordTimestamp, end);
        makeResultsAvailable();
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
            m_Logger.error("An error occurred while extracting data");
            return Optional.empty();
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

    public void start(JobDetails job) throws CannotStartSchedulerWhileItIsStoppingException
    {
        if (m_Status == JobSchedulerStatus.STOPPING)
        {
            throw new CannotStartSchedulerWhileItIsStoppingException(m_JobId);
        }
        if (m_Status == JobSchedulerStatus.STARTED)
        {
            m_Logger.info("Cannot start scheduler as it is already started.");
            return;
        }

        m_Logger = m_JobLoggerFactory.newLogger(m_JobId);
        updateStatus(JobSchedulerStatus.STARTED);
        m_LookbackExecutor = Executors.newSingleThreadExecutor();
        initLastEndTime(job);
        SchedulerConfig schedulerConfig = job.getSchedulerConfig();
        long lookbackStart = calcLookbackStart(schedulerConfig);
        long lookbackEnd = calcLookbackEnd(schedulerConfig);
        m_IsLookbackOnly = schedulerConfig.getEndTime() != null;
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
            m_LastEndTimeMs = job.getCounts().getLatestRecordTimeStamp().getTime()
                    + MILLIS_IN_SECOND;
        }
    }

    private long calcLookbackStart(SchedulerConfig schedulerConfig)
    {
        long startEpochMs = 0;
        if (m_LastEndTimeMs != null)
        {
            startEpochMs = m_LastEndTimeMs;
        }
        else
        {
            Date startTime = schedulerConfig.getStartTime();
            startEpochMs = startTime == null ? 0 : startTime.getTime();
        }
        return startEpochMs;
    }

    private long calcLookbackEnd(SchedulerConfig schedulerConfig)
    {
        Date endTime = schedulerConfig.getEndTime();
        if (endTime == null)
        {
            long nowMs = new Date().getTime();
            endTime = new Date(nowMs - m_QueryDelayMs);
        }
        return endTime.getTime();
    }

    private Runnable createLookbackAndStartRealTimeTask(long start, long end)
    {
        return () -> {
            m_Logger.info("Starting lookback");
            extractAndProcessData(start, end);
            m_Logger.info("Lookback has finished");
            if (m_IsLookbackOnly)
            {
                finishLookback();
            }
            else if (m_Status == JobSchedulerStatus.STARTED)
            {
                startRealTime();
            }
            m_LookbackExecutor.shutdown();
        };
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
        updateStatus(JobSchedulerStatus.STOPPED);
        closeLogger();
    }

    private void startRealTime()
    {
        synchronized (this)
        {
            m_Logger.info("Entering real-time mode");
            m_RealTimeScheduler = new TaskScheduler(createNextTask(), calculateNextTime());
            m_RealTimeScheduler.start();
        }
    }

    /**
     * Stops the scheduler and blocks the current thread until
     * the scheduler is stopped. At the end of the stopping process
     * the status is set to STOPPED.
     * @throws JobInUseException
     * @throws NativeProcessRunException
     * @throws UnknownJobException
     */
    public void stopManual() throws UnknownJobException, NativeProcessRunException, JobInUseException
    {
        stop(JobSchedulerStatus.STOPPED);
        m_DataProcessor.closeJob(m_JobId);
    }


    /**
     * Stops the scheduler and blocks the current thread until
     * the scheduler is stopped. At the end of the stopping process
     * the status is set to STARTED.
     */
    public void stopAuto()
    {
        stop(JobSchedulerStatus.STARTED);
    }

    /**
     * Stops the scheduler and blocks the current thread until
     * the scheduler is stopped.
     * @param finalStatus the status of the scheduler after the stop operation is finished
     */
    private void stop(JobSchedulerStatus finalStatus)
    {
        if (m_Status != JobSchedulerStatus.STARTED)
        {
            return;
        }

        updateStatus(JobSchedulerStatus.STOPPING);

        if (awaitLookbackTermination() == false || stopRealtimeScheduler() == false)
        {
            m_Logger.error("Unable to stop the scheduler.");
        }

        if (m_IsLookbackOnly == false)
        {
            updateStatus(finalStatus);
            closeLogger();
        }
    }

    @VisibleForTesting
    boolean awaitLookbackTermination()
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

    private void closeLogger()
    {
        m_JobLoggerFactory.close(m_Logger);
        m_Logger = null;
    }
}
