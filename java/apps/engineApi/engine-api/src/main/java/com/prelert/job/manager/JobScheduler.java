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

import java.io.InputStream;
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
import com.prelert.job.JobDetails;
import com.prelert.job.JobSchedulerStatus;
import com.prelert.job.SchedulerConfig;
import com.prelert.job.UnknownJobException;
import com.prelert.job.data.extraction.DataExtractor;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.exceptions.TooManyJobsException;
import com.prelert.job.persistence.JobDetailsProvider;
import com.prelert.job.process.autodetect.JobLogger;
import com.prelert.job.process.exceptions.MalformedJsonException;
import com.prelert.job.process.exceptions.MissingFieldException;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.process.params.DataLoadParams;
import com.prelert.job.process.params.TimeRange;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;
import com.prelert.utils.scheduler.TaskScheduler;

public class JobScheduler
{
    private static final int MILLIS_IN_SECOND = 1000;
    private static final DataLoadParams DATA_LOAD_PARAMS =
            new DataLoadParams(false, new TimeRange(null, null));
    private static final int DEFAULT_TIME_OFFSET_MS = 100;
    private static final int STOP_TIMEOUT_MINUTES = 60;

    private final String m_JobId;
    private final long m_BucketSpanMs;
    private final DataExtractor m_DataExtractor;
    private final DataProcessor m_DataProcessor;
    private final JobDetailsProvider m_JobProvider;
    private final JobLoggerFactory m_JobLoggerFactory;
    private volatile ExecutorService m_LookbackExecutor;
    private volatile TaskScheduler m_RealTimeScheduler;
    private volatile long m_LastBucketEndMs;
    private volatile Logger m_Logger;
    private volatile boolean m_IsStopping;
    private volatile boolean m_IsLookbackOnly;

    public JobScheduler(String jobId, long bucketSpan, DataExtractor dataExtractor,
            DataProcessor dataProcessor, JobDetailsProvider jobProvider,
            JobLoggerFactory jobLoggerFactory)
    {
        m_JobId = jobId;
        m_BucketSpanMs = bucketSpan * MILLIS_IN_SECOND;
        m_DataExtractor = Objects.requireNonNull(dataExtractor);
        m_DataProcessor = Objects.requireNonNull(dataProcessor);
        m_JobProvider = Objects.requireNonNull(jobProvider);
        m_JobLoggerFactory = Objects.requireNonNull(jobLoggerFactory);
        m_IsStopping = false;
    }

    private Runnable createNextTask()
    {
        return () -> {
            long nextBucketEnd = toBucketStartEpochMs(new Date());
            String start = String.valueOf(m_LastBucketEndMs);
            String end = String.valueOf(nextBucketEnd);
            extractAndProcessData(start, end);
        };
    }

    private long toBucketStartEpochMs(Date date)
    {
        return toBucketStartEpochMs(date.getTime());
    }

    private long toBucketStartEpochMs(long epochMs)
    {
        return (epochMs / m_BucketSpanMs) * m_BucketSpanMs;
    }

    private void extractAndProcessData(String start, String end)
    {
        if (start.equals(end))
        {
            return;
        }

        m_DataExtractor.newSearch(start, end, m_Logger);
        while(m_DataExtractor.hasNext() && m_IsStopping == false)
        {
            Optional<InputStream> nextDataStream = m_DataExtractor.next();
            if (nextDataStream.isPresent())
            {
                boolean success = submitData(nextDataStream.get());
                if (!success)
                {
                    return;
                }
            }
        }
        m_LastBucketEndMs = Long.valueOf(end);
    }

    private boolean submitData(InputStream stream)
    {
        try
        {
            m_DataProcessor.submitDataLoadJob(m_JobId, stream, DATA_LOAD_PARAMS);
            return true;
        }
        catch (JsonParseException | UnknownJobException | NativeProcessRunException
                | MissingFieldException | JobInUseException
                | HighProportionOfBadTimestampsException | OutOfOrderRecordsException
                | TooManyJobsException | MalformedJsonException e)
        {
            m_Logger.error("An error has occurred while submitting data to job '" + m_JobId + "'", e);
            return false;
        }
    }

    private Supplier<LocalDateTime> calculateNextTime()
    {
        return () -> {
            long nowMs = new Date().getTime();
            long bucketSurplus = nowMs - toBucketStartEpochMs(nowMs);
            Date nextTime = new Date(nowMs - bucketSurplus + m_BucketSpanMs + DEFAULT_TIME_OFFSET_MS);
            return LocalDateTime.ofInstant(nextTime.toInstant(), ZoneId.systemDefault());
        };
    }

    public void start(JobDetails job) throws CannotStartSchedulerWhileItIsStoppingException
    {
        if (job.getSchedulerStatus() == JobSchedulerStatus.STOPPING)
        {
            throw new CannotStartSchedulerWhileItIsStoppingException(m_JobId);
        }

        updateStatus(JobSchedulerStatus.STARTED);
        m_Logger = m_JobLoggerFactory.newLogger(m_JobId);
        m_LookbackExecutor = Executors.newSingleThreadExecutor();
        updateLastBucketEndFromLatestRecordTimestamp(job);
        SchedulerConfig schedulerConfig = job.getSchedulerConfig();
        String lookbackStart = calcLookbackStart(schedulerConfig);
        String lookbackEnd = calcLookbackEnd(schedulerConfig);
        m_IsLookbackOnly = schedulerConfig.getEndTime() != null;
        m_LookbackExecutor.execute(createLookbackAndStartRealTimeTask(lookbackStart, lookbackEnd));
    }

    private void updateStatus(JobSchedulerStatus status)
    {
        Map<String, Object> updates = new HashMap<>();
        updates.put(JobDetails.SCHEDULER_STATUS, status);
        try
        {
            m_JobProvider.updateJob(m_JobId, updates);
        } catch (UnknownJobException e)
        {
            throw new IllegalStateException();
        }
    }

    private void updateLastBucketEndFromLatestRecordTimestamp(JobDetails job)
    {
        if (job.getCounts() == null || job.getCounts().getLatestRecordTimeStamp() == null)
        {
            return;
        }
        Date latestRecordTimestamp = job.getCounts().getLatestRecordTimeStamp();
        m_LastBucketEndMs = latestRecordTimestamp.getTime() + MILLIS_IN_SECOND;
    }

    private String calcLookbackStart(SchedulerConfig schedulerConfig)
    {
        long startEpochMs = 0;
        if (m_LastBucketEndMs > 0)
        {
            startEpochMs = m_LastBucketEndMs;
        }
        else
        {
            Date startTime = schedulerConfig.getStartTime();
            startEpochMs = startTime == null ? 0 : toBucketStartEpochMs(startTime);
        }
        return String.valueOf(startEpochMs);
    }

    private String calcLookbackEnd(SchedulerConfig schedulerConfig)
    {
        Date endTime = schedulerConfig.getEndTime() == null ?
                new Date() : schedulerConfig.getEndTime();
        long endEpochMs = toBucketStartEpochMs(endTime);
        return String.valueOf(endEpochMs);
    }

    private Runnable createLookbackAndStartRealTimeTask(String start, String end)
    {
        return () -> {
            m_Logger.info("Starting lookback");
            extractAndProcessData(start, end);
            m_Logger.info("Lookback has finished");
            if (m_IsLookbackOnly)
            {
                closeLogger();
                updateStatus(JobSchedulerStatus.STOPPED);
            }
            else
            {
                m_Logger.info("Entering real-time mode");
                m_RealTimeScheduler = new TaskScheduler(createNextTask(), calculateNextTime());
                m_RealTimeScheduler.start();
            }
            m_LookbackExecutor.shutdown();
        };
    }

    /**
     * Stops the scheduler and blocks the current thread until
     * the scheduler is stopped. At the end of the stopping process
     * the status is set to STOPPED.
     */
    public void stopManual()
    {
        stop(true);
    }


    /**
     * Stops the scheduler and blocks the current thread until
     * the scheduler is stopped. At the end of the stopping process
     * the status is set to STARTED.
     */
    public void stopAuto()
    {
        stop(false);
    }

    /**
     * Stops the scheduler and blocks the current thread until
     * the scheduler is stopped.
     * @param shouldSetStoppedStatus if {@code true} the status is set to STOPPED
     */
    private void stop(boolean shouldSetStoppedStatus)
    {
        if (m_Logger == null)
        {
            // It means it was never started
            return;
        }

        m_IsStopping = true;
        updateStatus(JobSchedulerStatus.STOPPING);

        if (awaitLookbackTermination() == false || stopRealtimeScheduler() == false)
        {
            m_Logger.error("Unable to stop the scheduler.");
        }

        if (m_IsLookbackOnly == false)
        {
            closeLogger();
            updateStatus(shouldSetStoppedStatus ? JobSchedulerStatus.STOPPED
                    : JobSchedulerStatus.STARTED);
        }
        m_IsStopping = false;
    }

    @VisibleForTesting
    boolean awaitLookbackTermination()
    {
        try
        {
            return m_LookbackExecutor.awaitTermination(STOP_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        } catch (InterruptedException e)
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
        m_Logger.info("Scheduler has stopped");
        JobLogger.close(m_Logger);
        m_Logger = null;
    }
}
