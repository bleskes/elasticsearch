/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
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
import com.prelert.job.UnknownJobException;

/**
 * A class that removes results from all the jobs that
 * have expired their respected retention time.
 */
public class OldResultsRemover
{
    private static final Logger LOGGER = Logger.getLogger(OldResultsRemover.class);

    private static final int MAX_TAKE = 10000;

    private static final int SECONDS_IN_DAY = 86400;
    private static final int MILLISECONDS_IN_SECOND = 1000;

    private final JobProvider m_JobProvider;
    private final JobResultsDeleterFactory m_ResultsDeleterFactory;

    public OldResultsRemover(JobProvider jobProvider, JobResultsDeleterFactory resultsDeleterFactory)
    {
        m_JobProvider = Objects.requireNonNull(jobProvider);
        m_ResultsDeleterFactory = Objects.requireNonNull(resultsDeleterFactory);
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

    private void deleteResultsBefore(String jobId, long cutoffEpochMs)
    {
        JobResultsDeleter deleter = m_ResultsDeleterFactory.newDeleter(jobId);
        deleteResultsBefore(
                m_JobProvider.newBatchedInfluencersIterator(jobId).timeRange(0, cutoffEpochMs),
                influencer -> deleter.deleteInfluencer(influencer)
        );
        deleteResultsBefore(
                m_JobProvider.newBatchedBucketsIterator(jobId).timeRange(0, cutoffEpochMs),
                influencer -> deleter.deleteBucket(influencer)
        );
        deleter.commit();
    }

    private <T> void deleteResultsBefore(BatchedResultsIterator<T> influencersIterator,
            Consumer<T> deleteFunction)
    {
        while(influencersIterator.hasNext())
        {
            Deque<T> batch = nextBatch(influencersIterator);
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
