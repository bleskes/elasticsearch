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
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.JobDetails;
import com.prelert.job.UnknownJobException;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.persistence.JobRenormaliser;
import com.prelert.job.persistence.QueryPage;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.Influencer;

/**
 * Thread safe class that updated the scores of all existing results
 * with the normalised scores
 */
class ScoresUpdater
{
    /**
     * Maximum number of buckets to renormalise at a time
     */
    private static final int MAX_BUCKETS_PER_PAGE = 10000;

    /**
     * Target number of records to renormalise at a time
     */
    private static final int TARGET_RECORDS_TO_RENORMALISE = 100000;

    /**
     * Maximum number of influncers to renormalise at a time
     *
     * An influencer object is about 100 bytes, thus we can get
     * a million of them and still only use 100MB.
     */
    private static final int MAX_INFLUENCERS_PER_PAGE = 1000000;

    private final String m_JobId;
    private final JobProvider m_JobProvider;
    private final JobRenormaliser m_JobRenormaliser;
    private final NormaliserFactory m_NormaliserFactory;

    /**
     * This read from the data store on first access (lazy initialization)
     */
    private volatile int m_BucketSpan;

    public ScoresUpdater(String jobId, JobProvider jobProvider, JobRenormaliser jobRenormaliser,
            NormaliserFactory normaliserFactory)
    {
        m_JobId = jobId;
        m_JobProvider = Objects.requireNonNull(jobProvider);
        m_JobRenormaliser = Objects.requireNonNull(jobRenormaliser);
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
        Normaliser normaliser = m_NormaliserFactory.create(m_JobId, logger);
        int[] counts = { 0, 0 };
        try
        {
            updateBuckets(normaliser, quantilesState, endBucketEpochMs, counts, logger);
            updateInfluencers(normaliser, quantilesState, endBucketEpochMs, counts, logger);
        }
        catch (UnknownJobException uje)
        {
            logger.error("Inconsistency - job " + m_JobId + " unknown during renormalisation", uje);
        }
        catch (NativeProcessRunException npe)
        {
            logger.error("Failed to renormalise", npe);
        }

        logger.info("Normalisation resulted in: " +
                counts[0] + " updates, " +
                counts[1] + " no-ops");
    }

    private void updateBuckets(Normaliser normaliser, String quantilesState, long endBucketEpochMs,
            int[] counts, Logger logger) throws UnknownJobException,
            NativeProcessRunException
    {
        int skip = 0;
        // Get some buckets without their records to calculate how many
        // buckets can be sensibly retrieved with their records
        QueryPage<Bucket> page = m_JobProvider.buckets(m_JobId, false, false, skip,
                MAX_BUCKETS_PER_PAGE, 0, endBucketEpochMs, 0.0, 0.0);

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

            // Make a list of buckets with their records to be renormalised.
            // This may be shorter than the original list of buckets for two
            // reasons:
            // 1) We don't bother with buckets that have raw score 0 and no
            //    records
            // 2) We limit the total number of records to be not much more
            //    than 100000
            List<Bucket> bucketsToRenormalise = new ArrayList<>();
            int taken = 0;
            int totalRecordCount = 0;
            for (Bucket bucket : buckets)
            {
                ++taken;
                if (bucket.getRawAnomalyScore() > 0.0 || bucket.getRecordCount() > 0)
                {
                    bucketsToRenormalise.add(bucket);
                    m_JobProvider.expandBucket(m_JobId, false, bucket);
                    totalRecordCount += m_JobProvider.expandBucket(m_JobId, false, bucket);
                    if (totalRecordCount >= TARGET_RECORDS_TO_RENORMALISE)
                    {
                        break;
                    }
                }
            }

            if (!bucketsToRenormalise.isEmpty())
            {
                logger.debug("Will renormalize a batch of " + bucketsToRenormalise.size()
                        + " buckets with " + totalRecordCount + " records ("
                        + (taken - bucketsToRenormalise.size()) + " empty buckets skipped)");

                List<Normalisable> asNormalisables = bucketsToRenormalise.stream()
                        .map(bucket -> new BucketNormalisable(bucket)).collect(Collectors.toList());
                normaliser.normalise(getJobBucketSpan(logger), asNormalisables, quantilesState);

                for (Bucket bucket : bucketsToRenormalise)
                {
                    updateSingleBucket(bucket, counts, logger);
                }
            }

            skip += taken;
            if (page.hitCount() > skip)
            {
                // Get more buckets without their records
                page = m_JobProvider.buckets(m_JobId, false, false,
                        skip, MAX_BUCKETS_PER_PAGE, 0, endBucketEpochMs, 0.0, 0.0);
            }
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
        updateBucketIfItHasBigChange(bucket, counts, logger);
        updateRecordsThatHaveBigChange(bucket, counts, logger);
    }

    private void updateBucketIfItHasBigChange(Bucket bucket, int[] counts, Logger logger)
    {
        String bucketId = bucket.getId();
        if (bucketId != null)
        {
            if (bucket.hadBigNormalisedUpdate())
            {
                logger.trace("ES API CALL: update ID " + bucketId + " type " + Bucket.TYPE +
                        " in index " + m_JobId + " using map of new values");

                m_JobRenormaliser.updateBucket(m_JobId, bucket);

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
    }

    private void updateRecordsThatHaveBigChange(Bucket bucket, int[] counts, Logger logger)
    {
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
            m_JobRenormaliser.updateRecords(m_JobId, bucket.getId(), toUpdate);
        }
    }

    private void updateInfluencers(Normaliser normaliser, String quantilesState, long endBucketEpochMs,
            int[] counts, Logger logger) throws UnknownJobException, NativeProcessRunException
    {
        int skip = 0;
        QueryPage<Influencer> page = m_JobProvider.influencers(m_JobId, skip,
                MAX_INFLUENCERS_PER_PAGE, 0, endBucketEpochMs, null, false, 0.0);

        while (page.hitCount() > skip)
        {
            List<Influencer> influencers = page.queryResults();

            List<Normalisable> asNormalisables = influencers.stream()
                    .map(bucket -> new InfluencerNormalisable(bucket)).collect(Collectors.toList());
            normaliser.normalise(getJobBucketSpan(logger), asNormalisables, quantilesState);

            for (Influencer influencer : influencers)
            {
                if (influencer.hadBigNormalisedUpdate())
                {
                    m_JobRenormaliser.updateInfluencer(m_JobId, influencer);
                    ++counts[0];
                }
                else
                {
                    ++counts[1];
                }
            }

            skip += MAX_INFLUENCERS_PER_PAGE;
            if (page.hitCount() > skip)
            {
                page = m_JobProvider.influencers(m_JobId, skip, MAX_INFLUENCERS_PER_PAGE, 0,
                        endBucketEpochMs, null, false, 0.0);
            }
        }
    }
}
