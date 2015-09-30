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

package com.prelert.job.process.normaliser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.log4j.Logger;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.JobDetails;
import com.prelert.job.UnknownJobException;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.persistence.QueryPage;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;

/**
 * Thread safe class that updated the scores of all existing results
 * with the normalised scores
 */
class ScoresUpdater
{
    /**
     * Maximum number of buckets to renormalise at a time
     */
    private static final int MAX_BUCKETS_PER_PAGE = 100;

    private final String m_JobId;
    private final JobProvider m_JobProvider;
    private final NormaliserFactory m_NormaliserFactory;

    /**
     * This read from the data store on first access (lazy initialization)
     */
    private volatile int m_BucketSpan;

    public ScoresUpdater(String jobId, JobProvider jobProvider, NormaliserFactory normaliserFactory)
    {
        m_JobId = jobId;
        m_JobProvider = Objects.requireNonNull(jobProvider);
        m_NormaliserFactory = Objects.requireNonNull(normaliserFactory);
        m_BucketSpan = 0;
    }

    /**
     * Update the anomaly score field on all previously persisted buckets
     * and all contained records
     * @param quantilesState
     * @param endBucketEpochMs
     * @param logger
     */
    public void update(String quantilesState, long endBucketEpochMs, Logger logger)
    {
        try
        {
            Normaliser normaliser = m_NormaliserFactory.create(m_JobId, logger);
            int[] counts = { 0, 0 };
            int skip = 0;
            QueryPage<Bucket> page = m_JobProvider.buckets(m_JobId, true, false,
                        skip, MAX_BUCKETS_PER_PAGE, 0, endBucketEpochMs, 0.0, 0.0);

            while (page.hitCount() > skip)
            {
                List<Bucket> buckets = page.queryResults();
                if (buckets == null)
                {
                    logger.warn("No buckets to renormalise for job " +
                                m_JobId + " with skip " + skip + " and hit count " +
                                page.hitCount());
                    break;
                }

                List<Bucket> normalisedBuckets =
                        normaliser.normalise(getJobBucketSpan(logger), buckets, quantilesState);

                for (Bucket bucket : normalisedBuckets)
                {
                    updateSingleBucket(bucket, counts, logger);
                }

                skip += MAX_BUCKETS_PER_PAGE;
                if (page.hitCount() > skip)
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
     * @param counts Element 0 will be incremented if we update a document and
     * element 1 if we don't
     * @param logger
     */
    private void updateSingleBucket(Bucket bucket, int[] counts, Logger logger)
    {
        // First update the bucket if worthwhile
        String bucketId = bucket.getId();
        if (bucketId != null)
        {
            if (bucket.hadBigNormalisedUpdate())
            {
                logger.trace("ES API CALL: update ID " + bucketId + " type " + Bucket.TYPE +
                        " in index " + m_JobId + " using map of new values");

                m_JobProvider.updateBucket(m_JobId, bucketId, bucket.getAnomalyScore(),
                        bucket.getMaxNormalizedProbability());

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
        List<AnomalyRecord> toUpdate = new ArrayList<AnomalyRecord>();

        for (AnomalyRecord record : bucket.getRecords())
        {
            String recordId = record.getId();
            if (recordId != null)
            {
                if (record.hadBigNormalisedUpdate())
                {
                    toUpdate.add(record);

                    logger.trace("ES BULK ACTION: update ID " + recordId + " type " + AnomalyRecord.TYPE +
                            " in index " + m_JobId + " using map of new values");
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

        if (!toUpdate.isEmpty())
        {
            m_JobProvider.updateRecords(m_JobId, bucketId, toUpdate);
        }
    }

    private int getJobBucketSpan(Logger logger)
    {
        // lazy initialization idiom of instance field (as in Effective Java)

        int bucketSpan = m_BucketSpan;
        if (bucketSpan == 0)
        {
            synchronized (this)
            {
                bucketSpan = m_BucketSpan;
                if (bucketSpan == 0)
                {
                    // use dot notation to get fields from nested docs.
                    Number num = m_JobProvider.<Number>getField(m_JobId,
                            JobDetails.ANALYSIS_CONFIG + "." + AnalysisConfig.BUCKET_SPAN);
                    if (num != null)
                    {
                        m_BucketSpan = bucketSpan = num.intValue();
                        logger.info("Caching bucket span " + m_BucketSpan +
                                " for job " + m_JobId);
                    }
                }
            }
        }
        return bucketSpan;
    }
}
