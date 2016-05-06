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
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.JobDetails;
import com.prelert.job.UnknownJobException;
import com.prelert.job.persistence.BatchedDocumentsIterator;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.persistence.JobRenormaliser;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.Influencer;

/**
 * Thread safe class that updates the scores of all existing results
 * with the normalised scores
 */
class ScoresUpdater
{
    /**
     * Target number of records to renormalise at a time
     */
    private static final int TARGET_RECORDS_TO_RENORMALISE = 100000;

    // 30 days
    private static final long DEFAULT_RENORMALISATION_WINDOW_MS = 2592000000L;

    private static final int DEFAULT_BUCKETS_IN_RENORMALISATION_WINDOW = 100;

    private static final long SECONDS_IN_DAY = 86400;
    private static final long MILLISECONDS_IN_SECOND = 1000;

    private final String m_JobId;
    private final JobProvider m_JobProvider;
    private final JobRenormaliser m_JobRenormaliser;
    private final NormaliserFactory m_NormaliserFactory;
    private int m_BucketSpan;
    private long m_NormalisationWindow;

    public ScoresUpdater(String jobId, JobProvider jobProvider, JobRenormaliser jobRenormaliser,
            NormaliserFactory normaliserFactory)
    {
        m_JobId = jobId;
        m_JobProvider = Objects.requireNonNull(jobProvider);
        m_JobRenormaliser = Objects.requireNonNull(jobRenormaliser);
        m_NormaliserFactory = Objects.requireNonNull(normaliserFactory);
    }

    private void updateJobDetails()
    {
        JobDetails jobDetails = m_JobProvider.getJobDetails(m_JobId).get();
        m_BucketSpan = getBucketSpanOrDefault(jobDetails.getAnalysisConfig());
        m_NormalisationWindow = getNormalisationWindowOrDefault(jobDetails);
    }

    private static int getBucketSpanOrDefault(AnalysisConfig analysisConfig)
    {
        if (analysisConfig != null && analysisConfig.getBucketSpan() != null)
        {
            return analysisConfig.getBucketSpan().intValue();
        }
        // A bucketSpan value of 0 will result to the default
        // bucketSpan value being used in the back-end.
        return 0;
    }

    private long getNormalisationWindowOrDefault(JobDetails job)
    {
        if (job.getRenormalizationWindowDays() != null)
        {
            return job.getRenormalizationWindowDays() * SECONDS_IN_DAY * MILLISECONDS_IN_SECOND;
        }
        return Math.max(DEFAULT_RENORMALISATION_WINDOW_MS,
                DEFAULT_BUCKETS_IN_RENORMALISATION_WINDOW * m_BucketSpan * MILLISECONDS_IN_SECOND);
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
        updateJobDetails();
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
            int[] counts, Logger logger) throws UnknownJobException, NativeProcessRunException
    {
        BatchedDocumentsIterator<Bucket> bucketsIterator =
                m_JobProvider.newBatchedBucketsIterator(m_JobId)
                .timeRange(calcNormalisationWindowStart(endBucketEpochMs), endBucketEpochMs);

        // Make a list of buckets with their records to be renormalised.
        // This may be shorter than the original list of buckets for two
        // reasons:
        // 1) We don't bother with buckets that have raw score 0 and no
        //    records
        // 2) We limit the total number of records to be not much more
        //    than 100000
        List<Bucket> bucketsToRenormalise = new ArrayList<>();
        int batchRecordCount = 0;
        int skipped = 0;

        while (bucketsIterator.hasNext())
        {
            // Get a batch of buckets without their records to calculate
            // how many buckets can be sensibly retrieved
            Deque<Bucket> buckets = bucketsIterator.next();
            if (buckets.isEmpty())
            {
                logger.debug("No buckets to renormalise for job " + m_JobId);
                break;
            }

            while (!buckets.isEmpty())
            {
                Bucket currentBucket = buckets.removeFirst();
                if (currentBucket.isNormalisable())
                {
                    bucketsToRenormalise.add(currentBucket);
                    batchRecordCount += m_JobProvider.expandBucket(m_JobId, false, currentBucket);
                }
                else
                {
                    ++skipped;
                }

                if (batchRecordCount >= TARGET_RECORDS_TO_RENORMALISE)
                {
                    normaliseBuckets(normaliser, bucketsToRenormalise, quantilesState,
                            batchRecordCount, skipped, counts, logger);

                    bucketsToRenormalise = new ArrayList<>();
                    batchRecordCount = 0;
                    skipped = 0;
                }
            }
        }
        if (!bucketsToRenormalise.isEmpty())
        {
            normaliseBuckets(normaliser, bucketsToRenormalise, quantilesState,
                    batchRecordCount, skipped, counts, logger);
        }
    }

    private long calcNormalisationWindowStart(long endEpochMs)
    {
        return Math.max(0, endEpochMs - m_NormalisationWindow);
    }

    private void normaliseBuckets(Normaliser normaliser, List<Bucket> buckets,
            String quantilesState, int recordCount, int skipped, int[] counts, Logger logger)
            throws NativeProcessRunException, UnknownJobException
    {
        logger.debug("Will renormalize a batch of " + buckets.size()
                + " buckets with " + recordCount + " records ("
                + skipped + " empty buckets skipped)");

        List<Normalisable> asNormalisables = buckets.stream()
                .map(bucket -> new BucketNormalisable(bucket)).collect(Collectors.toList());
        normaliser.normalise(m_BucketSpan, asNormalisables, quantilesState);

        for (Bucket bucket : buckets)
        {
            updateSingleBucket(bucket, counts, logger);
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
                        " for job " + m_JobId + " using map of new values");

                m_JobRenormaliser.updateBucket(bucket);

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
            m_JobRenormaliser.updateRecords(bucket.getId(), toUpdate);
        }
    }

    private void updateInfluencers(Normaliser normaliser, String quantilesState, long endBucketEpochMs,
            int[] counts, Logger logger) throws UnknownJobException, NativeProcessRunException
    {
        BatchedDocumentsIterator<Influencer> influencersIterator = m_JobProvider
                .newBatchedInfluencersIterator(m_JobId)
                .timeRange(calcNormalisationWindowStart(endBucketEpochMs), endBucketEpochMs);
        while (influencersIterator.hasNext())
        {
            Deque<Influencer> influencers = influencersIterator.next();
            if (influencers.isEmpty())
            {
                logger.debug("No influencers to renormalise for job " + m_JobId);
                break;
            }

            logger.debug("Will renormalize a batch of " + influencers.size() + " influencers");
            List<Normalisable> asNormalisables = influencers.stream()
                    .map(bucket -> new InfluencerNormalisable(bucket)).collect(Collectors.toList());
            normaliser.normalise(m_BucketSpan, asNormalisables, quantilesState);

            for (Influencer influencer : influencers)
            {
                if (influencer.hadBigNormalisedUpdate())
                {
                    m_JobRenormaliser.updateInfluencer(influencer);
                    ++counts[0];
                }
                else
                {
                    ++counts[1];
                }
            }
        }
    }
}
