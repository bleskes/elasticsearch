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

import static org.junit.Assert.assertEquals;
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
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.JobDetails;
import com.prelert.job.UnknownJobException;
import com.prelert.job.persistence.BatchedResultsIterator;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.persistence.JobRenormaliser;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.BucketInfluencer;
import com.prelert.job.results.Influencer;

public class ScoresUpdaterTest
{
    private static final String JOB_ID = "foo";
    private static final String QUANTILES_STATE = "someState";
    private static final long BUCKET_SPAN = 3600;
    private static final long END_TIME = 3600;

    @Mock private JobProvider m_JobProvider;
    @Mock private JobRenormaliser m_JobRenormaliser;
    @Mock private Normaliser m_Normaliser;
    @Mock private Logger m_Logger;

    private ScoresUpdater m_ScoresUpdater;

    @Before
    public void setUp() throws UnknownJobException
    {
        MockitoAnnotations.initMocks(this);

        JobDetails job = new JobDetails();
        job.setId(JOB_ID);
        AnalysisConfig config = new AnalysisConfig();
        config.setBucketSpan(BUCKET_SPAN);
        job.setAnalysisConfig(config);

        m_ScoresUpdater = new ScoresUpdater(job, m_JobProvider, m_JobRenormaliser,
                (jobId, logger) -> m_Normaliser);
        givenProviderReturnsNoBuckets(END_TIME);
        givenProviderReturnsNoInfluencers(END_TIME);
    }

    @Test
    public void testUpdate_GivenBucketWithZeroScoreAndNoRecords()
            throws UnknownJobException, NativeProcessRunException
    {
        Bucket bucket = new Bucket();
        bucket.setId("0");
        bucket.setAnomalyScore(0.0);
        bucket.addBucketInfluencer(createTimeBucketInfluencer(0.7, 0.0));
        Deque<Bucket> buckets = new ArrayDeque<>();
        buckets.add(bucket);
        givenProviderReturnsBuckets(END_TIME, buckets);

        m_ScoresUpdater.update(QUANTILES_STATE, 3600, m_Logger);

        verifyNormaliserWasInvoked(0);
        verifyBucketWasNotUpdated(bucket);
        verifyBucketRecordsWereNotUpdated("0");
    }

    @Test
    public void testUpdate_GivenBucketWithNonZeroScoreButNoBucketInfluencers()
            throws UnknownJobException, NativeProcessRunException
    {
        Bucket bucket = new Bucket();
        bucket.setId("0");
        bucket.setAnomalyScore(0.0);
        bucket.setBucketInfluencers(null);
        Deque<Bucket> buckets = new ArrayDeque<>();
        buckets.add(bucket);
        givenProviderReturnsBuckets(END_TIME, buckets);

        m_ScoresUpdater.update(QUANTILES_STATE, 3600, m_Logger);

        verifyNormaliserWasInvoked(0);
        verifyBucketWasNotUpdated(bucket);
        verifyBucketRecordsWereNotUpdated("0");
    }

    @Test
    public void testUpdate_GivenSingleBucketWithoutBigChangeAndNoRecords()
            throws UnknownJobException, NativeProcessRunException
    {
        Bucket bucket = new Bucket();
        bucket.setId("0");
        bucket.setAnomalyScore(30.0);
        bucket.addBucketInfluencer(createTimeBucketInfluencer(0.04, 30.0));
        Deque<Bucket> buckets = new ArrayDeque<>();
        buckets.add(bucket);
        givenProviderReturnsBuckets(END_TIME, buckets);

        m_ScoresUpdater.update(QUANTILES_STATE, 3600, m_Logger);

        verifyNormaliserWasInvoked(1);
        verifyBucketWasNotUpdated(bucket);
        verifyBucketRecordsWereNotUpdated("0");
    }

    @Test
    public void testUpdate_GivenSingleBucketWithoutBigChangeAndRecordsWithoutBigChange()
            throws UnknownJobException, NativeProcessRunException
    {
        Bucket bucket = new Bucket();
        bucket.setAnomalyScore(30.0);
        bucket.addBucketInfluencer(createTimeBucketInfluencer(0.04, 30.0));
        List<AnomalyRecord> records = new ArrayList<>();
        AnomalyRecord record1 = createRecordWithoutBigChange();
        AnomalyRecord record2 = createRecordWithoutBigChange();
        records.add(record1);
        records.add(record2);
        bucket.setRecords(records);
        bucket.setRecordCount(2);

        bucket.setId("0");
        Deque<Bucket> buckets = new ArrayDeque<>();
        buckets.add(bucket);
        givenProviderReturnsBuckets(END_TIME, buckets);

        m_ScoresUpdater.update(QUANTILES_STATE, 3600, m_Logger);

        verifyNormaliserWasInvoked(1);
        verifyBucketWasNotUpdated(bucket);
        verifyBucketRecordsWereNotUpdated("0");
    }

    @Test
    public void testUpdate_GivenSingleBucketWithBigChangeAndNoRecords()
            throws UnknownJobException, NativeProcessRunException
    {
        Bucket bucket = new Bucket();
        bucket.setId("0");
        bucket.setAnomalyScore(42.0);
        bucket.addBucketInfluencer(createTimeBucketInfluencer(0.04, 42.0));
        bucket.setMaxNormalizedProbability(50.0);
        bucket.raiseBigNormalisedUpdateFlag();

        Deque<Bucket> buckets = new ArrayDeque<>();
        buckets.add(bucket);
        givenProviderReturnsBuckets(END_TIME, buckets);

        m_ScoresUpdater.update(QUANTILES_STATE, 3600, m_Logger);

        verifyNormaliserWasInvoked(1);
        verifyBucketWasUpdated(bucket);
        verifyBucketRecordsWereNotUpdated("0");
    }

    @Test
    public void testUpdate_GivenSingleBucketWithoutBigChangeAndSomeRecordsWithBigChange()
            throws UnknownJobException, NativeProcessRunException
    {
        Bucket bucket = new Bucket();
        bucket.setId("0");
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
        givenProviderReturnsBuckets(END_TIME, buckets);

        m_ScoresUpdater.update(QUANTILES_STATE, 3600, m_Logger);

        verifyNormaliserWasInvoked(1);
        verifyBucketWasNotUpdated(bucket);
        verifyRecordsWereUpdated("0", Arrays.asList(record1, record3));
    }

    @Test
    public void testUpdate_GivenSingleBucketWithBigChangeAndSomeRecordsWithBigChange()
            throws UnknownJobException, NativeProcessRunException
    {
        Bucket bucket = new Bucket();
        bucket.setId("0");
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
        givenProviderReturnsBuckets(END_TIME, buckets);

        m_ScoresUpdater.update(QUANTILES_STATE, 3600, m_Logger);

        verifyNormaliserWasInvoked(1);
        verifyBucketWasUpdated(bucket);
        verifyRecordsWereUpdated("0", Arrays.asList(record1, record3));
    }

    @Test
    public void testUpdate_GivenEnoughBucketsForTwoBatchesButOneNormalisation()
            throws UnknownJobException, NativeProcessRunException
    {
        Deque<Bucket> batch1 = new ArrayDeque<>();
        for (int i = 0; i < 10000; ++i)
        {
            Bucket bucket = new Bucket();
            bucket.setId(String.valueOf(i));
            bucket.setAnomalyScore(42.0);
            bucket.addBucketInfluencer(createTimeBucketInfluencer(0.04, 42.0));
            bucket.setMaxNormalizedProbability(50.0);
            bucket.raiseBigNormalisedUpdateFlag();
            batch1.add(bucket);
        }

        Bucket secondBatchBucket = new Bucket();
        secondBatchBucket.setId("10000");
        secondBatchBucket.addBucketInfluencer(createTimeBucketInfluencer(0.04, 42.0));
        secondBatchBucket.setAnomalyScore(42.0);
        secondBatchBucket.setMaxNormalizedProbability(50.0);
        secondBatchBucket.raiseBigNormalisedUpdateFlag();
        Deque<Bucket> batch2 = new ArrayDeque<>();
        batch2.add(secondBatchBucket);

        givenProviderReturnsBuckets(END_TIME, batch1, batch2);

        m_ScoresUpdater.update(QUANTILES_STATE, 3600, m_Logger);

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
        Bucket bucket1 = new Bucket();
        bucket1.setId("0");
        bucket1.setAnomalyScore(42.0);
        bucket1.addBucketInfluencer(createTimeBucketInfluencer(0.04, 42.0));
        bucket1.setMaxNormalizedProbability(50.0);
        bucket1.raiseBigNormalisedUpdateFlag();
        when(m_JobProvider.expandBucket(JOB_ID, false, bucket1)).thenReturn(100000);

        Bucket bucket2 = new Bucket();
        bucket2.setId("10000");
        bucket2.addBucketInfluencer(createTimeBucketInfluencer(0.04, 42.0));
        bucket2.setAnomalyScore(42.0);
        bucket2.setMaxNormalizedProbability(50.0);
        bucket2.raiseBigNormalisedUpdateFlag();

        Deque<Bucket> batch = new ArrayDeque<>();
        batch.add(bucket1);
        batch.add(bucket2);
        givenProviderReturnsBuckets(END_TIME, batch);

        m_ScoresUpdater.update(QUANTILES_STATE, 3600, m_Logger);

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
        givenProviderReturnsInfluencers(END_TIME, influencers);

        m_ScoresUpdater.update(QUANTILES_STATE, 3600, m_Logger);

        verifyNormaliserWasInvoked(1);
        verifyInfluencerWasUpdated(influencer);
    }

    private void verifyNormaliserWasInvoked(int times) throws NativeProcessRunException,
            UnknownJobException
    {
        verify(m_Normaliser, times(times)).normalise(eq((int) BUCKET_SPAN), anyListOf(Normalisable.class),
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

    private void givenProviderReturnsNoBuckets(long endTime) throws UnknownJobException
    {
        givenBuckets(endTime, Collections.emptyList());
    }

    private void givenProviderReturnsBuckets(long endTime, Deque<Bucket> batch1, Deque<Bucket> batch2)
            throws UnknownJobException
    {
        List<Deque<Bucket>> batches = new ArrayList<>();
        batches.add(new ArrayDeque<>(batch1));
        batches.add(new ArrayDeque<>(batch2));
        givenBuckets(endTime, batches);
    }

    private void givenProviderReturnsBuckets(long endTime, Deque<Bucket> buckets)
    {
        List<Deque<Bucket>> batches = new ArrayList<>();
        batches.add(new ArrayDeque<>(buckets));
        givenBuckets(endTime, batches);
    }

    private void givenBuckets(long endTime, List<Deque<Bucket>> batches)
    {
        BatchedResultsIterator<Bucket> iterator = new MockBatchedResultsIterator<Bucket>(0,
                endTime, batches);
        when(m_JobProvider.newBatchedBucketsIterator(JOB_ID)).thenReturn(iterator);
    }

    private void givenProviderReturnsNoInfluencers(long endTime) throws UnknownJobException
    {
        givenProviderReturnsInfluencers(endTime, new ArrayDeque<>());
    }

    private void givenProviderReturnsInfluencers(long endTime, Deque<Influencer> influencers)
    {
        List<Deque<Influencer>> batches = new ArrayList<>();
        batches.add(new ArrayDeque<>(influencers));
        BatchedResultsIterator<Influencer> iterator = new MockBatchedResultsIterator<Influencer>(0,
                endTime, batches);
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

    private static class MockBatchedResultsIterator<T> implements BatchedResultsIterator<T>
    {
        private final long m_StartEpochMs;
        private final long m_EndEpochMs;
        private final List<Deque<T>> m_Batches;
        private int m_Index;
        private boolean m_WasTimeRangeCalled;

        public MockBatchedResultsIterator(long startEpochMs, long endEpochMs, List<Deque<T>> batches)
        {
            m_StartEpochMs = startEpochMs;
            m_EndEpochMs = endEpochMs;
            m_Batches = batches;
            m_Index = 0;
            m_WasTimeRangeCalled = false;
        }

        @Override
        public BatchedResultsIterator<T> timeRange(long startEpochMs, long endEpochMs)
        {
            assertEquals(m_StartEpochMs, startEpochMs);
            assertEquals(m_EndEpochMs, endEpochMs);
            m_WasTimeRangeCalled = true;
            return this;
        }

        @Override
        public Deque<T> next() throws UnknownJobException
        {
            if (!m_WasTimeRangeCalled || !hasNext())
            {
                throw new NoSuchElementException();
            }
            return m_Batches.get(m_Index++);
        }

        @Override
        public boolean hasNext()
        {
            return m_Index != m_Batches.size();
        }
    }
}
