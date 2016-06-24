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

import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.JobDetails;
import com.prelert.job.UnknownJobException;
import com.prelert.job.persistence.BatchedDocumentsIterator;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.persistence.JobRenormaliser;
import com.prelert.job.persistence.MockBatchedDocumentsIterator;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.BucketInfluencer;
import com.prelert.job.results.Influencer;

public class ScoresUpdaterTest
{
    private static final String JOB_ID = "foo";
    private static final String QUANTILES_STATE = "someState";
    private static final long DEFAULT_BUCKET_SPAN = 3600;
    private static final long DEFAULT_START_TIME = 0;
    private static final long DEFAULT_END_TIME = 3600;
    private static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static Random ms_Rnd = new Random();

    private JobDetails m_Job;
    @Mock private JobProvider m_JobProvider;
    @Mock private JobRenormaliser m_JobRenormaliser;
    @Mock private Normaliser m_Normaliser;
    @Mock private Logger m_Logger;

    private ScoresUpdater m_ScoresUpdater;

    private String randomString()
    {
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++)
        {
            sb.append(AB.charAt(ms_Rnd.nextInt(AB.length())));
        }
        return sb.toString();
    }

    private Bucket generateBucket()
    {
        Bucket bucket = new Bucket();
        bucket.setId(randomString());
        return bucket;
    }

    @Before
    public void setUp() throws UnknownJobException
    {
        MockitoAnnotations.initMocks(this);

        m_Job = new JobDetails();
        m_Job.setId(JOB_ID);
        AnalysisConfig config = new AnalysisConfig();
        config.setBucketSpan(DEFAULT_BUCKET_SPAN);
        m_Job.setAnalysisConfig(config);

        m_ScoresUpdater = new ScoresUpdater(JOB_ID, m_JobProvider, m_JobRenormaliser,
                (jobId, logger) -> m_Normaliser);

        givenProviderReturnsNoBuckets(DEFAULT_START_TIME, DEFAULT_END_TIME);
        givenProviderReturnsNoInfluencers(DEFAULT_START_TIME, DEFAULT_END_TIME);
        when(m_JobProvider.getJobDetails(JOB_ID)).thenReturn(Optional.of(m_Job));
    }

    @Test
    public void testUpdate_GivenBucketWithZeroScoreAndNoRecords()
            throws UnknownJobException, NativeProcessRunException
    {
        Bucket bucket = generateBucket();
        bucket.setTimestamp(new Date(0));
        bucket.setAnomalyScore(0.0);
        bucket.addBucketInfluencer(createTimeBucketInfluencer(0.7, 0.0));
        Deque<Bucket> buckets = new ArrayDeque<>();
        buckets.add(bucket);
        givenProviderReturnsBuckets(DEFAULT_START_TIME, DEFAULT_END_TIME, buckets);

        m_ScoresUpdater.update(QUANTILES_STATE, 3600, 0, m_Logger);

        verifyNormaliserWasInvoked(0);
        verifyBucketWasNotUpdated(bucket);
        verifyBucketRecordsWereNotUpdated(bucket.getId());
    }

    @Test
    public void testUpdate_GivenBucketWithNonZeroScoreButNoBucketInfluencers()
            throws UnknownJobException, NativeProcessRunException
    {
        Bucket bucket = generateBucket();
        bucket.setTimestamp(new Date(0));
        bucket.setAnomalyScore(0.0);
        bucket.setBucketInfluencers(null);
        Deque<Bucket> buckets = new ArrayDeque<>();
        buckets.add(bucket);
        givenProviderReturnsBuckets(DEFAULT_START_TIME, DEFAULT_END_TIME, buckets);

        m_ScoresUpdater.update(QUANTILES_STATE, 3600, 0, m_Logger);

        verifyNormaliserWasInvoked(0);
        verifyBucketWasNotUpdated(bucket);
        verifyBucketRecordsWereNotUpdated(bucket.getId());
    }

    @Test
    public void testUpdate_GivenSingleBucketWithoutBigChangeAndNoRecords()
            throws UnknownJobException, NativeProcessRunException
    {
        Bucket bucket = generateBucket();
        bucket.setTimestamp(new Date(0));;
        bucket.setAnomalyScore(30.0);
        bucket.addBucketInfluencer(createTimeBucketInfluencer(0.04, 30.0));
        Deque<Bucket> buckets = new ArrayDeque<>();
        buckets.add(bucket);
        givenProviderReturnsBuckets(DEFAULT_START_TIME, DEFAULT_END_TIME, buckets);

        m_ScoresUpdater.update(QUANTILES_STATE, 3600, 0, m_Logger);

        verifyNormaliserWasInvoked(1);
        verifyBucketWasNotUpdated(bucket);
        verifyBucketRecordsWereNotUpdated(bucket.getId());
    }

    @Test
    public void testUpdate_GivenSingleBucketWithoutBigChangeAndRecordsWithoutBigChange()
            throws UnknownJobException, NativeProcessRunException
    {
        Bucket bucket = generateBucket();
        bucket.setAnomalyScore(30.0);
        bucket.addBucketInfluencer(createTimeBucketInfluencer(0.04, 30.0));
        List<AnomalyRecord> records = new ArrayList<>();
        AnomalyRecord record1 = createRecordWithoutBigChange();
        AnomalyRecord record2 = createRecordWithoutBigChange();
        records.add(record1);
        records.add(record2);
        bucket.setRecords(records);
        bucket.setRecordCount(2);

        bucket.setTimestamp(new Date(0));;
        Deque<Bucket> buckets = new ArrayDeque<>();
        buckets.add(bucket);
        givenProviderReturnsBuckets(DEFAULT_START_TIME, DEFAULT_END_TIME, buckets);

        m_ScoresUpdater.update(QUANTILES_STATE, 3600, 0, m_Logger);

        verifyNormaliserWasInvoked(1);
        verifyBucketWasNotUpdated(bucket);
        verifyBucketRecordsWereNotUpdated(bucket.getId());
    }

    @Test
    public void testUpdate_GivenSingleBucketWithBigChangeAndNoRecords()
            throws UnknownJobException, NativeProcessRunException
    {
        Bucket bucket = generateBucket();
        bucket.setTimestamp(new Date(0));
        bucket.setAnomalyScore(42.0);
        bucket.addBucketInfluencer(createTimeBucketInfluencer(0.04, 42.0));
        bucket.setMaxNormalizedProbability(50.0);
        bucket.raiseBigNormalisedUpdateFlag();

        Deque<Bucket> buckets = new ArrayDeque<>();
        buckets.add(bucket);
        givenProviderReturnsBuckets(DEFAULT_START_TIME, DEFAULT_END_TIME, buckets);

        m_ScoresUpdater.update(QUANTILES_STATE, 3600, 0, m_Logger);

        verifyNormaliserWasInvoked(1);
        verifyBucketWasUpdated(bucket);
        verifyBucketRecordsWereNotUpdated(bucket.getId());
    }

    @Test
    public void testUpdate_GivenSingleBucketWithoutBigChangeAndSomeRecordsWithBigChange()
            throws UnknownJobException, NativeProcessRunException
    {
        Bucket bucket = generateBucket();
        bucket.setTimestamp(new Date(0));
        bucket.setAnomalyScore(42.0);
        bucket.addBucketInfluencer(createTimeBucketInfluencer(0.04, 42.0));
        bucket.setMaxNormalizedProbability(50.0);

        List<AnomalyRecord> records = new ArrayList<>();
        AnomalyRecord record1 = createRecordWithBigChange();
        AnomalyRecord record2 = createRecordWithoutBigChange();
        AnomalyRecord record3 = createRecordWithBigChange();
        records.add(record1);
        records.add(record2);
        records.add(record3);
        bucket.setRecords(records);

        Deque<Bucket> buckets = new ArrayDeque<>();
        buckets.add(bucket);
        givenProviderReturnsBuckets(DEFAULT_START_TIME, DEFAULT_END_TIME, buckets);

        m_ScoresUpdater.update(QUANTILES_STATE, 3600, 0, m_Logger);

        verifyNormaliserWasInvoked(1);
        verifyBucketWasNotUpdated(bucket);
        verifyRecordsWereUpdated(bucket.getId(), Arrays.asList(record1, record3));
    }

    @Test
    public void testUpdate_GivenSingleBucketWithBigChangeAndSomeRecordsWithBigChange()
            throws UnknownJobException, NativeProcessRunException
    {
        Bucket bucket = generateBucket();
        bucket.setTimestamp(new Date(0));
        bucket.setAnomalyScore(42.0);
        bucket.addBucketInfluencer(createTimeBucketInfluencer(0.04, 42.0));
        bucket.setMaxNormalizedProbability(50.0);
        bucket.raiseBigNormalisedUpdateFlag();
        List<AnomalyRecord> records = new ArrayList<>();
        AnomalyRecord record1 = createRecordWithBigChange();
        AnomalyRecord record2 = createRecordWithoutBigChange();
        AnomalyRecord record3 = createRecordWithBigChange();
        records.add(record1);
        records.add(record2);
        records.add(record3);
        bucket.setRecords(records);

        Deque<Bucket> buckets = new ArrayDeque<>();
        buckets.add(bucket);
        givenProviderReturnsBuckets(DEFAULT_START_TIME, DEFAULT_END_TIME, buckets);

        m_ScoresUpdater.update(QUANTILES_STATE, 3600, 0, m_Logger);

        verifyNormaliserWasInvoked(1);
        verifyBucketWasUpdated(bucket);
        verifyRecordsWereUpdated(bucket.getId(), Arrays.asList(record1, record3));
    }

    @Test
    public void testUpdate_GivenEnoughBucketsForTwoBatchesButOneNormalisation()
            throws UnknownJobException, NativeProcessRunException
    {
        Deque<Bucket> batch1 = new ArrayDeque<>();
        for (int i = 0; i < 10000; ++i)
        {
            Bucket bucket = generateBucket();
            bucket.setTimestamp(new Date(i * 1000));
            bucket.setAnomalyScore(42.0);
            bucket.addBucketInfluencer(createTimeBucketInfluencer(0.04, 42.0));
            bucket.setMaxNormalizedProbability(50.0);
            bucket.raiseBigNormalisedUpdateFlag();
            batch1.add(bucket);
        }

        Bucket secondBatchBucket = generateBucket();
        secondBatchBucket.setTimestamp(new Date(10000 * 1000));
        secondBatchBucket.addBucketInfluencer(createTimeBucketInfluencer(0.04, 42.0));
        secondBatchBucket.setAnomalyScore(42.0);
        secondBatchBucket.setMaxNormalizedProbability(50.0);
        secondBatchBucket.raiseBigNormalisedUpdateFlag();
        Deque<Bucket> batch2 = new ArrayDeque<>();
        batch2.add(secondBatchBucket);

        givenProviderReturnsBuckets(DEFAULT_START_TIME, DEFAULT_END_TIME, batch1, batch2);

        m_ScoresUpdater.update(QUANTILES_STATE, 3600, 0, m_Logger);

        verifyNormaliserWasInvoked(1);

        // Batch 1 - Just verify first and last were updated as Mockito
        // is forbiddingly slow when tring to verify all 10000
        verifyBucketWasUpdated(batch1.getFirst());
        verifyBucketRecordsWereNotUpdated(batch1.getFirst().getId());
        verifyBucketWasUpdated(batch1.getLast());
        verifyBucketRecordsWereNotUpdated(batch1.getLast().getId());

        verifyBucketWasUpdated(secondBatchBucket);
        verifyBucketRecordsWereNotUpdated(secondBatchBucket.getId());
    }

    @Test
    public void testUpdate_GivenTwoBucketsWithFirstHavingEnoughRecordsToForceSecondNormalisation()
            throws UnknownJobException, NativeProcessRunException
    {
        Bucket bucket1 = generateBucket();
        bucket1.setTimestamp(new Date(0));
        bucket1.setAnomalyScore(42.0);
        bucket1.addBucketInfluencer(createTimeBucketInfluencer(0.04, 42.0));
        bucket1.setMaxNormalizedProbability(50.0);
        bucket1.raiseBigNormalisedUpdateFlag();
        when(m_JobProvider.expandBucket(JOB_ID, false, bucket1)).thenReturn(100000);

        Bucket bucket2 = generateBucket();
        bucket2.setTimestamp(new Date(10000 * 1000));
        bucket2.addBucketInfluencer(createTimeBucketInfluencer(0.04, 42.0));
        bucket2.setAnomalyScore(42.0);
        bucket2.setMaxNormalizedProbability(50.0);
        bucket2.raiseBigNormalisedUpdateFlag();

        Deque<Bucket> batch = new ArrayDeque<>();
        batch.add(bucket1);
        batch.add(bucket2);
        givenProviderReturnsBuckets(DEFAULT_START_TIME, DEFAULT_END_TIME, batch);

        m_ScoresUpdater.update(QUANTILES_STATE, 3600, 0, m_Logger);

        verifyNormaliserWasInvoked(2);

        verifyBucketWasUpdated(bucket1);
        verifyBucketRecordsWereNotUpdated(bucket1.getId());
        verifyBucketWasUpdated(bucket2);
        verifyBucketRecordsWereNotUpdated(bucket2.getId());
    }

    @Test
    public void testUpdate_GivenInfluencerWithBigChange()
            throws UnknownJobException, NativeProcessRunException
    {
        Influencer influencer = new Influencer();
        influencer.raiseBigNormalisedUpdateFlag();

        Deque<Influencer> influencers = new ArrayDeque<>();
        influencers.add(influencer);
        givenProviderReturnsInfluencers(DEFAULT_START_TIME, DEFAULT_END_TIME, influencers);

        m_ScoresUpdater.update(QUANTILES_STATE, 3600, 0, m_Logger);

        verifyNormaliserWasInvoked(1);
        verifyInfluencerWasUpdated(influencer);
    }

    @Test
    public void testDefaultRenormalizationWindowBasedOnTime()
            throws UnknownJobException, NativeProcessRunException
    {
        Bucket bucket = generateBucket();
        bucket.setTimestamp(new Date(0));
        bucket.setAnomalyScore(42.0);
        bucket.addBucketInfluencer(createTimeBucketInfluencer(0.04, 42.0));
        bucket.setMaxNormalizedProbability(50.0);
        bucket.raiseBigNormalisedUpdateFlag();

        Deque<Bucket> buckets = new ArrayDeque<>();
        buckets.add(bucket);
        givenProviderReturnsBuckets(3600000, 2595600000L, buckets);
        givenProviderReturnsNoInfluencers(3600000, 2595600000L);

        m_ScoresUpdater.update(QUANTILES_STATE, 2595600000L, 0, m_Logger);

        verifyNormaliserWasInvoked(1);
        verifyBucketWasUpdated(bucket);
        verifyBucketRecordsWereNotUpdated(bucket.getId());
    }

    @Test
    public void testDefaultRenormalizationWindowBasedOnBuckets()
            throws UnknownJobException, NativeProcessRunException
    {
        m_Job.getAnalysisConfig().setBucketSpan(86400L);
        // Bucket span is a day, so default window should be 8640000000 ms

        Bucket bucket = generateBucket();
        bucket.setTimestamp(new Date(0));
        bucket.setAnomalyScore(42.0);
        bucket.addBucketInfluencer(createTimeBucketInfluencer(0.04, 42.0));
        bucket.setMaxNormalizedProbability(50.0);
        bucket.raiseBigNormalisedUpdateFlag();

        Deque<Bucket> buckets = new ArrayDeque<>();
        buckets.add(bucket);
        givenProviderReturnsBuckets(3600000, 8643600000L, buckets);
        givenProviderReturnsNoInfluencers(3600000, 8643600000L);

        m_ScoresUpdater.update(QUANTILES_STATE, 8643600000L, 0, m_Logger);

        verifyNormaliserWasInvoked(1);
        verifyBucketWasUpdated(bucket);
        verifyBucketRecordsWereNotUpdated(bucket.getId());
    }

    @Test
    public void testManualRenormalizationWindow()
            throws UnknownJobException, NativeProcessRunException
    {
        // 1 day
        m_Job.setRenormalizationWindowDays(1L);

        Bucket bucket = generateBucket();
        bucket.setTimestamp(new Date(0));
        bucket.setAnomalyScore(42.0);
        bucket.addBucketInfluencer(createTimeBucketInfluencer(0.04, 42.0));
        bucket.setMaxNormalizedProbability(50.0);
        bucket.raiseBigNormalisedUpdateFlag();

        Deque<Bucket> buckets = new ArrayDeque<>();
        buckets.add(bucket);
        givenProviderReturnsBuckets(3600000, 90000000L, buckets);
        givenProviderReturnsNoInfluencers(3600000, 90000000L);

        m_ScoresUpdater.update(QUANTILES_STATE, 90000000L, 0, m_Logger);

        verifyNormaliserWasInvoked(1);
        verifyBucketWasUpdated(bucket);
        verifyBucketRecordsWereNotUpdated(bucket.getId());
    }

    @Test
    public void testManualRenormalizationWindow_GivenExtension()
            throws UnknownJobException, NativeProcessRunException
    {
        // 1 day
        m_Job.setRenormalizationWindowDays(1L);

        Bucket bucket = generateBucket();
        bucket.setTimestamp(new Date(0));
        bucket.setAnomalyScore(42.0);
        bucket.addBucketInfluencer(createTimeBucketInfluencer(0.04, 42.0));
        bucket.setMaxNormalizedProbability(50.0);
        bucket.raiseBigNormalisedUpdateFlag();

        Deque<Bucket> buckets = new ArrayDeque<>();
        buckets.add(bucket);
        givenProviderReturnsBuckets(2700000, 90000000L, buckets);
        givenProviderReturnsNoInfluencers(2700000, 90000000L);

        m_ScoresUpdater.update(QUANTILES_STATE, 90000000L, 900000, m_Logger);

        verifyNormaliserWasInvoked(1);
        verifyBucketWasUpdated(bucket);
        verifyBucketRecordsWereNotUpdated(bucket.getId());
    }

    private void verifyNormaliserWasInvoked(int times) throws NativeProcessRunException,
            UnknownJobException
    {
        int bucketSpan = m_Job.getAnalysisConfig() == null ? 0
                : m_Job.getAnalysisConfig().getBucketSpan().intValue();
        verify(m_Normaliser, times(times)).normalise(
                eq(bucketSpan), anyListOf(Normalisable.class),
                eq(QUANTILES_STATE));
    }

    private BucketInfluencer createTimeBucketInfluencer(double probability, double anomalyScore)
    {
        BucketInfluencer influencer = new BucketInfluencer();
        influencer.setProbability(probability);
        influencer.setAnomalyScore(anomalyScore);
        influencer.setInfluencerFieldName(BucketInfluencer.BUCKET_TIME);
        return influencer;
    }

    private void givenProviderReturnsNoBuckets(long startTime, long endTime) throws UnknownJobException
    {
        givenBuckets(startTime, endTime, Collections.emptyList());
    }

    private void givenProviderReturnsBuckets(long startTime, long endTime, Deque<Bucket> batch1,
            Deque<Bucket> batch2) throws UnknownJobException
    {
        List<Deque<Bucket>> batches = new ArrayList<>();
        batches.add(new ArrayDeque<>(batch1));
        batches.add(new ArrayDeque<>(batch2));
        givenBuckets(startTime, endTime, batches);
    }

    private void givenProviderReturnsBuckets(long startTime, long endTime, Deque<Bucket> buckets)
    {
        List<Deque<Bucket>> batches = new ArrayList<>();
        batches.add(new ArrayDeque<>(buckets));
        givenBuckets(startTime, endTime, batches);
    }

    private void givenBuckets(long startTime, long endTime, List<Deque<Bucket>> batches)
    {
        BatchedDocumentsIterator<Bucket> iterator = new MockBatchedDocumentsIterator<Bucket>(startTime,
                endTime, batches);
        when(m_JobProvider.newBatchedBucketsIterator(JOB_ID)).thenReturn(iterator);
    }

    private void givenProviderReturnsNoInfluencers(long startTime, long endTime) throws UnknownJobException
    {
        givenProviderReturnsInfluencers(startTime, endTime, new ArrayDeque<>());
    }

    private void givenProviderReturnsInfluencers(long startTime, long endTime,
            Deque<Influencer> influencers)
    {
        List<Deque<Influencer>> batches = new ArrayList<>();
        batches.add(new ArrayDeque<>(influencers));
        BatchedDocumentsIterator<Influencer> iterator = new MockBatchedDocumentsIterator<Influencer>(
                startTime, endTime, batches);
        when(m_JobProvider.newBatchedInfluencersIterator(JOB_ID)).thenReturn(iterator);
    }

    private void verifyBucketWasUpdated(Bucket bucket)
    {
        verify(m_JobRenormaliser).updateBucket(bucket);
    }

    private void verifyRecordsWereUpdated(String bucketId, List<AnomalyRecord> records)
    {
        verify(m_JobRenormaliser).updateRecords(bucketId, records);
    }

    private void verifyBucketWasNotUpdated(Bucket bucket)
    {
        verify(m_JobRenormaliser, never()).updateBucket(bucket);
    }

    private void verifyBucketRecordsWereNotUpdated(String bucketId)
    {
        verify(m_JobRenormaliser, never()).updateRecords(eq(bucketId),
                anyListOf(AnomalyRecord.class));
    }

    private static AnomalyRecord createRecordWithoutBigChange()
    {
        return createRecord(false);
    }

    private static AnomalyRecord createRecordWithBigChange()
    {
        return createRecord(true);
    }

    private static AnomalyRecord createRecord(boolean hadBigChange)
    {
        AnomalyRecord anomalyRecord = mock(AnomalyRecord.class);
        when(anomalyRecord.hadBigNormalisedUpdate()).thenReturn(hadBigChange);
        when(anomalyRecord.getId()).thenReturn("someId");
        return anomalyRecord;
    }

    private void verifyInfluencerWasUpdated(Influencer influencer)
    {
        verify(m_JobRenormaliser).updateInfluencer(influencer);
    }
}
