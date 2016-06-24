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

package com.prelert.job.process.normaliser;


import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

import com.prelert.job.persistence.JobProvider;
import com.prelert.job.persistence.JobRenormaliser;
import com.prelert.job.process.output.parsing.Renormaliser;
import com.prelert.job.quantiles.Quantiles;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;


/**
 * Updates {@linkplain Bucket Buckets} and their contained
 * {@linkplain AnomalyRecord AnomalyRecords} with new normalised
 * anomaly scores and unusual scores.
 *
 * This is done in a separate thread to avoid blocking the main
 * data processing during renormalisations.
 *
 * Access to the job results and updates to the results (buckets,
 * records) is via the {@linkplain JobProvider} interface.
 */
public class BlockingQueueRenormaliser implements Renormaliser
{
    private final String m_JobId;
    private final JobProvider m_JobProvider;
    private final ScoresUpdater m_ScoresUpdater;

    /**
     * Queue of updated quantiles to be used for renormalisation
     */
    private final BlockingQueue<QuantileInfo> m_UpdatedQuantileQueue;

    /**
     * Thread to use so that quantile updates can run in parallel to the main
     * data processing of the job
     */
    private final Thread m_QuantileUpdateThread;

    private final Object m_WaitForIdleLock = new Object();

    /** Guarded by {@link #m_WaitForIdleLock} */
    private volatile boolean m_IsNormalisationInProgress;

    /**
     * Create with the job provider client. Data will be written to
     * the index <code>jobId</code>
     *
     * @param jobId The job Id/datastore index
     * @param jobProvider The job provider for accessing and updating job results
     */
    public BlockingQueueRenormaliser(String jobId, JobProvider jobProvider,
            JobRenormaliser jobRenormaliser)
    {
        m_JobId = jobId;
        m_JobProvider = jobProvider;
        m_ScoresUpdater = new ScoresUpdater(jobId, jobProvider, jobRenormaliser,
                (someJobId, logger) -> new Normaliser(jobId, new NormaliserProcessFactory(), logger));

        // Queue limit of 50 means that renormalisation will eventually block
        // the main data processing of the job if it gets too far ahead
        m_UpdatedQuantileQueue = new ArrayBlockingQueue<>(50);
        m_QuantileUpdateThread = new Thread(this.new QueueDrainer(),
            m_JobId + "-Renormalizer");
        m_QuantileUpdateThread.start();
    }

    /**
     * Shut down the worker thread
     */
    @Override
    public synchronized boolean shutdown(Logger logger)
    {
        try
        {
            m_UpdatedQuantileQueue.put(new QuantileInfo(QuantileInfo.InfoType.END,
                    "", 0, logger));
            m_QuantileUpdateThread.join();
            logger.info("After shutting down renormaliser thread " +
                        m_UpdatedQuantileQueue.size() +
                        " renormalisation requests remain unprocessed");
            m_UpdatedQuantileQueue.clear();
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            logger.info("Interrupted whilst shutting down renormaliser thread");
            return false;
        }

        return true;
    }

    /**
     * Is the worker thread running
     * @return
     */
    boolean isWorkerThreadRunning()
    {
        return m_QuantileUpdateThread.isAlive();
    }

    /**
     * Update the anomaly score field on all previously persisted buckets
     * and all contained records
     * @param quantiles
     * @param logger
     */
    @Override
    public synchronized void renormalise(Quantiles quantiles, Logger logger)
    {
        if (m_QuantileUpdateThread.isAlive())
        {
            Date timestamp = quantiles.getTimestamp();
            long endBucketEpochMS = (timestamp == null ? new Date() : timestamp).getTime();
            try
            {
                m_UpdatedQuantileQueue.put(new QuantileInfo(QuantileInfo.InfoType.RENORMALISATION,
                    quantiles.getQuantileState(), endBucketEpochMS, logger));
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                logger.error("Can't add item to queue: " + e);
            }
        }
        else
        {
            logger.error("Cannot renormalise for system changes " +
                        "- update thread no longer running");
        }
    }

    /**
     * Blocks until the queue is empty and no normalisation is in progress.
     */
    @Override
    public void waitUntilIdle()
    {
        synchronized (m_WaitForIdleLock)
        {
            while (!m_UpdatedQuantileQueue.isEmpty() || m_IsNormalisationInProgress)
            {
                try
                {
                    m_WaitForIdleLock.wait();
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private void setIdleAndNotify()
    {
        synchronized (m_WaitForIdleLock)
        {
            m_IsNormalisationInProgress = false;
            m_WaitForIdleLock.notifyAll();
        }
    }

    private void setIsNormalisationInProgress()
    {
        synchronized (m_WaitForIdleLock)
        {
            m_IsNormalisationInProgress = true;
        }
    }

    /**
     * Simple class to group information in the blocking queue
     */
    private static class QuantileInfo
    {
        public enum InfoType { END, RENORMALISATION }

        public final InfoType m_Type;
        public final String m_State;
        public final long m_EndBucketEpochMs;
        public final Logger m_Logger;

        private QuantileInfo(InfoType type, String state, long endBucketEpochMs, Logger logger)
        {
            m_Type = type;
            m_State = state;
            m_EndBucketEpochMs = endBucketEpochMs;
            m_Logger = logger;
        }
    }


    /**
     * Thread handler to drain updated quantiles from the blocking queue and
     * trigger renormalisations in response
     *
     * The logic is to drain as many info objects from the queue as possible
     * until an end marker is reached.  Any info objects after the first end
     * marker are discarded.  The benefit in draining as many objects as
     * possible each time the loop runs is that if normalisation is taking
     * much longer than the main processing and multiple sets of quantiles
     * of the same type are in the queue we can ignore all but the most
     * recent (i.e. those nearest the back of the queue).
     */
    private class QueueDrainer implements Runnable
    {
        @Override
        public void run()
        {
            Logger lastLogger = null;
            try
            {
                boolean keepGoing = true;
                while (keepGoing)
                {
                    QuantileInfo earliestInfo = null;
                    QuantileInfo latestInfo = null;

                    // take() will block if the queue is empty
                    QuantileInfo info = BlockingQueueRenormaliser.this.m_UpdatedQuantileQueue.take();
                    setIsNormalisationInProgress();
                    while (keepGoing && info != null)
                    {
                        lastLogger = info.m_Logger;
                        switch (info.m_Type)
                        {
                            case END:
                                keepGoing = false;
                                lastLogger.info("Normaliser thread received end instruction");
                                break;
                            case RENORMALISATION:
                                if (earliestInfo == null)
                                {
                                    earliestInfo = info;
                                }
                                if (latestInfo != null)
                                {
                                    lastLogger.info("Quantiles update superseded before processing");
                                }
                                latestInfo = info;
                                break;
                            default:
                                throw new IllegalStateException();
                        }
                        // poll() will return null if the queue is empty
                        info = BlockingQueueRenormaliser.this.m_UpdatedQuantileQueue.poll();
                    }

                    if (latestInfo != null)
                    {
                        m_ScoresUpdater.update(latestInfo.m_State, latestInfo.m_EndBucketEpochMs,
                                // The Math.abs() shouldn't be necessary - just being defensive
                                Math.abs(latestInfo.m_EndBucketEpochMs - earliestInfo.m_EndBucketEpochMs),
                                latestInfo.m_Logger);
                    }

                    // Refresh the indexes so that normalised results are
                    // available to search before either:
                    // a) The next normalisation starting
                    // b) A request to close the job returning
                    lastLogger.info("Renormaliser thread about to refresh indexes");
                    m_JobProvider.refreshIndex(BlockingQueueRenormaliser.this.m_JobId);

                    if (m_UpdatedQuantileQueue.isEmpty())
                    {
                        setIdleAndNotify();
                    }
                }
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                // Thread will exit now
                if (lastLogger != null)
                {
                    lastLogger.info("Renormaliser thread interrupted - will not refresh indexes");
                }
            }
            finally
            {
                setIdleAndNotify();
            }
        }
    }
}

