/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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

package com.prelert.job.persistence.elasticsearch;


import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.JobDetails;
import com.prelert.job.exceptions.UnknownJobException;
import com.prelert.job.normalisation.Normaliser;
import com.prelert.job.normalisation.NormaliserProcessFactory;
import com.prelert.job.persistence.JobRenormaliser;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.quantiles.Quantiles;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.rs.data.Pagination;


/**
 * Updates {@linkplain Bucket Buckets} and their contained
 * {@linkplain AnomalyRecord AnomalyRecords} with new normalised
 * anomaly scores and unusual scores.
 *
 * This is done in a separate thread to avoid blocking the main
 * data processing during renormalisations.
 */
public class ElasticsearchJobRenormaliser implements JobRenormaliser
{
    private String m_JobId;
    private ElasticsearchJobProvider m_JobProvider;

    /**
     * This read from the data store on first access
     */
    private int m_BucketSpan;

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
     * Maximum number of buckets to renormalise at a time
     */
    private static final int MAX_BUCKETS_PER_PAGE = 100;

    /**
     * Create with the Elasticsearch client. Data will be written to
     * the index <code>jobId</code>
     *
     * @param jobId The job Id/Elasticsearch index
     * @param jobProvider The Elasticsearch job provider
     */
    public ElasticsearchJobRenormaliser(String jobId, ElasticsearchJobProvider jobProvider)
    {
        m_JobId = jobId;
        m_JobProvider = jobProvider;
        m_BucketSpan = 0;

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
     * Update the anomaly score field on all previously persisted buckets
     * and all contained records
     * @param sysChangeState
     * @param endTime
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
                    quantiles.getState(), endBucketEpochMS, logger));
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
     * Update the anomaly score field on all previously persisted buckets
     * and all contained records
     * @param quantilesState
     * @param endBucketEpochMs
     * @param logger
     */
    private void updateScores(String quantilesState, long endBucketEpochMs, Logger logger)
    {
        try
        {
            Normaliser normaliser = new Normaliser(m_JobId, new NormaliserProcessFactory(), logger);
            int[] counts = { 0, 0 };
            int skip = 0;
            Pagination<Bucket> page = m_JobProvider.buckets(m_JobId, true, false,
                        skip, MAX_BUCKETS_PER_PAGE, 0, endBucketEpochMs, 0.0, 0.0);

            while (page.getHitCount() > skip)
            {
                List<Bucket> buckets = page.getDocuments();
                if (buckets == null)
                {
                    logger.warn("No buckets to renormalise for job " +
                                m_JobId + " with skip " + skip + " and hit count " +
                                page.getHitCount());
                    break;
                }

                List<Bucket> normalisedBuckets =
                        normaliser.normalise(getJobBucketSpan(logger), buckets, quantilesState);

                for (Bucket bucket : normalisedBuckets)
                {
                    updateSingleBucket(bucket, logger, counts);
                }

                skip += MAX_BUCKETS_PER_PAGE;
                if (page.getHitCount() > skip)
                {
                    page = m_JobProvider.buckets(m_JobId, true, false,
                            skip, MAX_BUCKETS_PER_PAGE, 0, endBucketEpochMs, 0.0, 0.0);
                }
            }

            logger.info("System changes normalisation resulted in: " +
                        counts[0] + " updates, " +
                        counts[1] + " no-ops");
        }
        catch (UnknownJobException uje)
        {
            logger.error("Inconsistency - job " + m_JobId +
                            " unknown during system change renormalisation", uje);
        }
        catch (NativeProcessRunException npe)
        {
            logger.error("Failed to renormalise for system changes", npe);
        }
    }

    /**
     * Update the anomaly score and unsual score fields on the bucket provided
     * and all contained records
     * @param bucket
     * @param logger
     * @param counts Element 0 will be incremented if we update a document and
     * element 1 if we don't
     */
    private void updateSingleBucket(Bucket bucket, Logger logger, int[] counts)
    {
        try
        {
            // First update the bucket if worthwhile
            String bucketId = bucket.getId();
            if (bucketId != null)
            {
                if (bucket.hadBigNormalisedUpdate())
                {
                    Map<String, Object> map = new TreeMap<>();
                    map.put(Bucket.ANOMALY_SCORE, bucket.getAnomalyScore());
                    map.put(Bucket.MAX_NORMALIZED_PROBABILITY, bucket.getMaxNormalizedProbability());

                    m_JobProvider.getClient().prepareUpdate(m_JobId, Bucket.TYPE, bucketId)
                            .setDoc(map)
                            .execute().actionGet();

                    ++counts[0];
                }
                else
                {
                    ++counts[1];
                }
            }
            else
            {
                logger.warn("Failed to renormalise bucket - no ID");
                ++counts[1];
            }

            // Now bulk update the records within the bucket
            BulkRequestBuilder bulkRequest = m_JobProvider.getClient().prepareBulk();
            boolean addedAny = false;
            for (AnomalyRecord record : bucket.getRecords())
            {
                String recordId = record.getId();
                if (recordId != null)
                {
                    if (record.hadBigNormalisedUpdate())
                    {
                        Map<String, Object> map = new TreeMap<>();
                            map.put(AnomalyRecord.ANOMALY_SCORE, record.getAnomalyScore());
                            map.put(AnomalyRecord.NORMALIZED_PROBABILITY, record.getNormalizedProbability());

                        bulkRequest.add(m_JobProvider.getClient()
                                .prepareUpdate(m_JobId, AnomalyRecord.TYPE, recordId)
                                .setDoc(map)
                                // Need to specify the parent ID when updating a child
                                .setParent(bucketId));

                        addedAny = true;
                        ++counts[0];
                    }
                    else
                    {
                        ++counts[1];
                    }
                }
                else
                {
                    logger.warn("Failed to renormalise record - no ID");
                    ++counts[1];
                }
            }

            if (addedAny)
            {
                BulkResponse bulkResponse = bulkRequest.execute().actionGet();
                if (bulkResponse.hasFailures())
                {
                    logger.error("BulkResponse has errors");
                    for (BulkItemResponse item : bulkResponse.getItems())
                    {
                        logger.error(item.getFailureMessage());
                    }
                }
            }
        }
        catch (ElasticsearchException e)
        {
            logger.error("Error updating bucket state", e);
        }
    }


    private int getJobBucketSpan(Logger logger)
    {
        if (m_BucketSpan == 0)
        {
            // use dot notation to get fields from nested docs.
            Number num = m_JobProvider.<Number>getField(m_JobId,
                    JobDetails.ANALYSIS_CONFIG + "." + AnalysisConfig.BUCKET_SPAN);
            if (num != null)
            {
                m_BucketSpan = num.intValue();
                logger.info("Caching bucket span " + m_BucketSpan +
                            " for job " + m_JobId);
            }
        }

        return m_BucketSpan;
    }

    /**
     * Blocks until the queue is empty and no normalisation is in progress.
     */
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
                    QuantileInfo latestInfo = null;

                    // take() will block if the queue is empty
                    QuantileInfo info = ElasticsearchJobRenormaliser.this.m_UpdatedQuantileQueue.take();
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
                        info = ElasticsearchJobRenormaliser.this.m_UpdatedQuantileQueue.poll();
                    }

                    if (latestInfo != null)
                    {
                        ElasticsearchJobRenormaliser.this.updateScores(latestInfo.m_State,
                                latestInfo.m_EndBucketEpochMs, latestInfo.m_Logger);
                    }

                    // Refresh the indexes so that normalised results are
                    // available to search before either:
                    // a) The next normalisation starting
                    // b) A request to close the job returning
                    lastLogger.info("Renormaliser thread about to refresh indexes");
                    ElasticsearchJobRenormaliser.this.m_JobProvider.getClient().admin().indices().refresh(
                            new RefreshRequest(ElasticsearchJobRenormaliser.this.m_JobId)).actionGet();

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

