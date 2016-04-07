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

package com.prelert.job.persistence;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.apache.log4j.Logger;

import com.prelert.job.JobDetails;
import com.prelert.job.ModelSnapshot;
import com.prelert.job.UnknownJobException;

/**
 * A class that removes results from all the jobs that
 * have expired their respected retention time.
 */
public class OldDataRemover
{
    private static final Logger LOGGER = Logger.getLogger(OldDataRemover.class);

    private static final int MAX_TAKE = 10000;

    private static final int SECONDS_IN_DAY = 86400;
    private static final int MILLISECONDS_IN_SECOND = 1000;

    private static final long DEFAULT_MODEL_SNAPSHOT_RETENTION_DAYS = 1L;

    private final JobProvider m_JobProvider;
    private final JobDataDeleterFactory m_DataDeleterFactory;

    public OldDataRemover(JobProvider jobProvider, JobDataDeleterFactory dataDeleterFactory)
    {
        m_JobProvider = Objects.requireNonNull(jobProvider);
        m_DataDeleterFactory = Objects.requireNonNull(dataDeleterFactory);
    }

    /**
     * Removes old results and model snapshots.  Does this by calling
     * {@link #removeOldModelSnapshots() removeOldModelSnapshots},
     * {@link #removeOldResults() removeOldResults},
     * {@link #removeOldModelDebugOutput() removeOldModelDebugOutput} &
     * {@link #removeOldModelSizeStats() removeOldModelSizeStats} methods.
     */
    public void removeOldData()
    {
        removeOldModelSnapshots();
        removeOldResults();
        removeOldModelDebugOutput();
        removeOldModelSizeStats();
    }

    /**
     * Removes all ModelDebugOutputs that have expired the configured retention time
     * of their respective job. A record is deleted if its timestamp is earlier
     * than the start of the current day (local time-zone) minus the retention
     * period.
     */
    public void removeOldModelDebugOutput()
    {
        LOGGER.info("Initialising removal of expired ModelDebugOutput records");
        List<JobDetails> jobs = m_JobProvider.getJobs(0, MAX_TAKE).queryResults();
        long startOfDayEpoch = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
        for (JobDetails job : jobs)
        {
            Long retentionDays = job.getResultsRetentionDays();
            if (retentionDays == null)
            {
                continue;
            }
            long cutoffEpochSeconds = startOfDayEpoch - (retentionDays * SECONDS_IN_DAY);
            LOGGER.info("Removing ModelDebugOutput for job with ID '" + job.getId()
                    + "' that have a timestamp before: " + cutoffEpochSeconds);
            deleteModelDebugOutputBefore(job.getId(), cutoffEpochSeconds * MILLISECONDS_IN_SECOND);
        }
        LOGGER.info("Removal of expired ModelDebugOutput records is complete");
    }

    /**
     * Removes all ModelSizeStats that have expired the configured retention time
     * of their respective job. A record is deleted if its timestamp is earlier
     * than the start of the current day (local time-zone) minus the retention
     * period.
     */
    public void removeOldModelSizeStats()
    {
        LOGGER.info("Initialising removal of expired ModelSizeStats records");
        List<JobDetails> jobs = m_JobProvider.getJobs(0, MAX_TAKE).queryResults();
        long startOfDayEpoch = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
        for (JobDetails job : jobs)
        {
            Long retentionDays = job.getResultsRetentionDays();
            if (retentionDays == null)
            {
                continue;
            }
            long cutoffEpochSeconds = startOfDayEpoch - (retentionDays * SECONDS_IN_DAY);
            LOGGER.info("Removing ModelSizeStats for job with ID '" + job.getId()
                    + "' that have a timestamp before: " + cutoffEpochSeconds);
            deleteModelSizeStatsBefore(job.getId(), cutoffEpochSeconds * MILLISECONDS_IN_SECOND);
        }
        LOGGER.info("Removal of expired ModelSizeStats records is complete");
    }

    /**
     * Removes all model snapshots that have expired the configured retention time
     * of their respective job. A snapshot is deleted if its timestamp is earlier
     * than the start of the current day (local time-zone) minus the retention
     * period.
     */
    public void removeOldModelSnapshots()
    {
        LOGGER.info("Initialising removal of expired model snapshots");
        List<JobDetails> jobs = m_JobProvider.getJobs(0, MAX_TAKE).queryResults();
        long startOfDayEpoch = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
        for (JobDetails job : jobs)
        {
            Long retentionDays = job.getModelSnapshotRetentionDays();
            if (retentionDays == null)
            {
                retentionDays = DEFAULT_MODEL_SNAPSHOT_RETENTION_DAYS;
            }
            long cutoffEpochSeconds = startOfDayEpoch - (retentionDays * SECONDS_IN_DAY);
            LOGGER.info("Removing model snapshots for job with ID '" + job.getId()
                    + "' that have a timestamp before: " + cutoffEpochSeconds);
            deleteModelStateBefore(job.getId(), cutoffEpochSeconds * MILLISECONDS_IN_SECOND);
        }
        LOGGER.info("Removal of expired model snapshots is complete");
    }

    /**
     * Removes all results that have expired the configured retention time
     * of their respective job. A result is deleted if its timestamp is earlier
     * than the start of the current day (local time-zone) minus the retention
     * period.
     */
    public void removeOldResults()
    {
        LOGGER.info("Initialising removal of expired results");
        List<JobDetails> jobs = m_JobProvider.getJobs(0, MAX_TAKE).queryResults();
        long startOfDayEpoch = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
        for (JobDetails job : jobs)
        {
            Long retentionDays = job.getResultsRetentionDays();
            if (retentionDays == null)
            {
                continue;
            }
            long cutoffEpochSeconds = startOfDayEpoch - (retentionDays * SECONDS_IN_DAY);
            LOGGER.info("Removing results of job with ID '" + job.getId()
                    + "' that have a timestamp before: " + cutoffEpochSeconds);
            deleteResultsBefore(job.getId(), cutoffEpochSeconds * MILLISECONDS_IN_SECOND);
        }
        LOGGER.info("Removal of expired results is complete");
    }

    private void deleteModelDebugOutputBefore(String jobId, long cutoffEpochMs)
    {
        JobDataDeleter deleter = m_DataDeleterFactory.newDeleter(jobId);
        deleteDataBefore(
                m_JobProvider.newBatchedModelDebugOutputIterator(jobId).timeRange(0, cutoffEpochMs),
                modelDebugOutput -> deleter.deleteModelDebugOutput(modelDebugOutput));
        deleter.commitAndFreeDiskSpace();
    }

    private void deleteModelSizeStatsBefore(String jobId, long cutoffEpochMs)
    {
        JobDataDeleter deleter = m_DataDeleterFactory.newDeleter(jobId);
        deleteDataBefore(
                m_JobProvider.newBatchedModelSizeStatsIterator(jobId).timeRange(0, cutoffEpochMs),
                modelSizeStats -> deleter.deleteModelSizeStats(modelSizeStats));
        deleter.commitAndFreeDiskSpace();
    }

    private void deleteModelStateBefore(String jobId, long cutoffEpochMs)
    {
        // Don't delete the highest priority model snapshot
        List<ModelSnapshot> highestPriority = null;
        try
        {
            highestPriority = m_JobProvider.modelSnapshots(jobId, 0, 1).queryResults();
        }
        catch (UnknownJobException e)
        {
            LOGGER.warn("Failed to retrieve highest priority model snapshot for job '"
                    + e.getJobId() + "'. " + "The job appears to have been deleted.");
        }
        if (highestPriority == null || highestPriority.isEmpty())
        {
            // There are no snapshots at all, so nothing to delete
            return;
        }

        String highestPriorityId = highestPriority.get(0).getSnapshotId();

        JobDataDeleter deleter = m_DataDeleterFactory.newDeleter(jobId);
        deleteDataBefore(
                m_JobProvider.newBatchedModelSnapshotIterator(jobId).timeRange(0, cutoffEpochMs),
                modelSnapshot -> {
                    if (!highestPriorityId.equals(modelSnapshot.getSnapshotId()))
                    {
                        deleter.deleteModelSnapshot(modelSnapshot);
                    }
                }
        );
        deleter.commitAndFreeDiskSpace();
    }

    private void deleteResultsBefore(String jobId, long cutoffEpochMs)
    {
        JobDataDeleter deleter = m_DataDeleterFactory.newDeleter(jobId);
        deleteDataBefore(
                m_JobProvider.newBatchedInfluencersIterator(jobId).timeRange(0, cutoffEpochMs),
                influencer -> deleter.deleteInfluencer(influencer)
        );
        deleteDataBefore(
                m_JobProvider.newBatchedBucketsIterator(jobId).timeRange(0, cutoffEpochMs),
                bucket -> deleter.deleteBucket(bucket)
        );
        deleter.commitAndFreeDiskSpace();
    }

    private <T> void deleteDataBefore(BatchedResultsIterator<T> resultsIterator,
            Consumer<T> deleteFunction)
    {
        while (resultsIterator.hasNext())
        {
            Deque<T> batch = nextBatch(resultsIterator);
            if (batch.isEmpty())
            {
                return;
            }
            for (T result : batch)
            {
                deleteFunction.accept(result);
            }
        }
    }

    private <T> Deque<T> nextBatch(BatchedResultsIterator<T> resultsIterator)
    {
        try
        {
            return resultsIterator.next();
        }
        catch (UnknownJobException e)
        {
            LOGGER.warn("Failed to retrieve results for job '" + e.getJobId() + "'. "
                    + "The job appears to have been deleted.");
            return new ArrayDeque<T>();
        }
    }
}
