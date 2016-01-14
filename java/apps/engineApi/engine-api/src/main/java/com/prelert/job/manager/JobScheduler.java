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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.prelert.job.JobDetails;
import com.prelert.job.SchedulerConfig;
import com.prelert.job.UnknownJobException;
import com.prelert.job.data.extraction.DataExtractor;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.exceptions.TooManyJobsException;
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

    private final String m_JobId;
    private final long m_BucketSpanMs;
    private final DataExtractor m_DataExtractor;
    private final DataProcessor m_DataProcessor;
    private final JobLoggerFactory m_JobLoggerFactory;
    private final TaskScheduler m_RealTimeScheduler;
    private volatile long m_LastBucketEndMs;
    private volatile Logger m_Logger;

    public JobScheduler(String jobId, long bucketSpan, DataExtractor dataExtractor,
            DataProcessor dataProcessor, JobLoggerFactory jobLoggerFactory)
    {
        m_JobId = jobId;
        m_BucketSpanMs = bucketSpan * MILLIS_IN_SECOND;
        m_DataExtractor = Objects.requireNonNull(dataExtractor);
        m_DataProcessor = Objects.requireNonNull(dataProcessor);
        m_JobLoggerFactory = Objects.requireNonNull(jobLoggerFactory);
        m_RealTimeScheduler = new TaskScheduler(createNextTask(), calculateNextTime());
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
        while(m_DataExtractor.hasNext())
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
        m_Logger.info("Finished extracting and processing data. Last bucket end is: "
                + m_LastBucketEndMs + " ms");
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
            Date nextTime = new Date(nowMs - bucketSurplus + m_BucketSpanMs + 100);
            return LocalDateTime.ofInstant(nextTime.toInstant(), ZoneId.systemDefault());
        };
    }

    public Future<?> start(JobDetails job)
    {
        m_Logger = m_JobLoggerFactory.newLogger(m_JobId);
        updateLastBucketEndFromLatestRecordTimestamp(job);
        SchedulerConfig schedulerConfig = job.getSchedulerConfig();
        String lookbackStart = calcLookbackStart(schedulerConfig);
        String lookbackEnd = calcLookbackEnd(schedulerConfig);
        boolean startRealTime = schedulerConfig.getEndTime() == null;
        return Executors.newSingleThreadExecutor().submit(
                createLookbackAndStartRealTimeTask(lookbackStart, lookbackEnd, startRealTime));
    }

    private void updateLastBucketEndFromLatestRecordTimestamp(JobDetails job)
    {
        if (job.getCounts() == null || job.getCounts().getLatestRecordTimeStamp() == null)
        {
            return;
        }
        Date latestRecordTimestamp = job.getCounts().getLatestRecordTimeStamp();
        m_LastBucketEndMs = toBucketStartEpochMs(latestRecordTimestamp) + m_BucketSpanMs;
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

    private Runnable createLookbackAndStartRealTimeTask(String start, String end,
            boolean startRealTime)
    {
        return () -> {
            m_Logger.info("Starting lookback");
            extractAndProcessData(start, end);
            m_Logger.info("Lookback has finished");
            if (startRealTime)
            {
                m_Logger.info("Entering real-time mode");
                m_RealTimeScheduler.start();
            }
        };
    }

    public void stop()
    {
        if (m_RealTimeScheduler != null)
        {
            m_RealTimeScheduler.stop();
        }
        if (m_Logger != null)
        {
            m_Logger.info("Scheduler has stopped");
            JobLogger.close(m_Logger);
            m_Logger = null;
        }
    }
}
