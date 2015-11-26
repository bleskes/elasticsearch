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

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.prelert.job.UnknownJobException;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.persistence.JobRenormaliser;
import com.prelert.job.persistence.QueryPage;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.BucketInfluencer;
import com.prelert.job.results.Influencer;

public class ScoresUpdaterTest
{
    private static final String JOB_ID = "foo";
    private static final int MAX_BUCKETS_PER_PAGE = 10000;
    private static final int MAX_INFLUENCERS_PER_PAGE = 1000000;
    private static final String QUANTILES_STATE = "someState";

    @Mock private JobProvider m_JobProvider;
    @Mock private JobRenormaliser m_JobRenormaliser;
    @Mock private Normaliser m_Normaliser;
    @Mock private Logger m_Logger;

    private ScoresUpdater m_ScoresUpdater;

    @Before
    public void setUp() throws UnknownJobException
    {
        MockitoAnnotations.initMocks(this);
        m_ScoresUpdater = new ScoresUpdater(JOB_ID, m_JobProvider, m_JobRenormaliser,
                (jobId, logger) -> m_Normaliser);
        givenProviderReturnsNoBuckets();
        givenProviderReturnsNoInfluencers();
    }

    @Test
    public void testUpdate_GivenBucketWithZeroScoreAndNoRecords()
            throws UnknownJobException, NativeProcessRunException
    {
        Bucket bucket = new Bucket();
        bucket.setId("0");
        bucket.setAnomalyScore(0.0);
        bucket.addBucketInfluencer(createTimeBucketInfluencer(0.7, 0.0));
        int totalBuckets = 1;
        long endTime = 3600;
        int bucketSpan = 3600;
        givenBucketSpan(bucketSpan);
        List<Bucket> buckets = Arrays.asList(bucket);
        givenProviderReturnsBuckets(0, endTime, totalBuckets, buckets);

        m_ScoresUpdater.update(QUANTILES_STATE, 3600, m_Logger);

        verifyNormaliserWasInvoked(bucketSpan, 0);
        verifyBucketWasNotUpdated(bucket);
        verifyBucketRecordsWereNotUpdated("0");
        verifyBucketSpanWasNotRequested();
    }

    @Test
    public void testUpdate_GivenBucketWithNonZeroScoreButNoBucketInfluencers()
            throws UnknownJobException, NativeProcessRunException
    {
        Bucket bucket = new Bucket();
        bucket.setId("0");
        bucket.setAnomalyScore(0.0);
        bucket.setBucketInfluencers(null);
        int totalBuckets = 1;
        long endTime = 3600;
        int bucketSpan = 3600;
        givenBucketSpan(bucketSpan);
        List<Bucket> buckets = Arrays.asList(bucket);
        givenProviderReturnsBuckets(0, endTime, totalBuckets, buckets);

        m_ScoresUpdater.update(QUANTILES_STATE, 3600, m_Logger);

        verifyNormaliserWasInvoked(bucketSpan, 0);
        verifyBucketWasNotUpdated(bucket);
        verifyBucketRecordsWereNotUpdated("0");
        verifyBucketSpanWasNotRequested();
    }

    @Test
    public void testUpdate_GivenSingleBucketWithoutBigChangeAndNoRecords()
            throws UnknownJobException, NativeProcessRunException
    {
        Bucket bucket = new Bucket();
        bucket.setId("0");
        bucket.setAnomalyScore(30.0);
        bucket.addBucketInfluencer(createTimeBucketInfluencer(0.04, 30.0));
        int totalBuckets = 1;
        long endTime = 3600;
        int bucketSpan = 3600;
        givenBucketSpan(bucketSpan);
        List<Bucket> buckets = Arrays.asList(bucket);
        givenProviderReturnsBuckets(0, endTime, totalBuckets, buckets);

        m_ScoresUpdater.update(QUANTILES_STATE, 3600, m_Logger);

        verifyNormaliserWasInvoked(bucketSpan, 1);
        verifyBucketWasNotUpdated(bucket);
        verifyBucketRecordsWereNotUpdated("0");
        verifyBucketSpanWasRequestedOnlyOnce();
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
        int totalBuckets = 1;
        long endTime = 3600;
        int bucketSpan = 3600;
        givenBucketSpan(bucketSpan);
        List<Bucket> buckets = Arrays.asList(bucket);
        givenProviderReturnsBuckets(0, endTime, totalBuckets, buckets);

        m_ScoresUpdater.update(QUANTILES_STATE, 3600, m_Logger);

        verifyNormaliserWasInvoked(bucketSpan, 1);
        verifyBucketWasNotUpdated(bucket);
        verifyBucketRecordsWereNotUpdated("0");
        verifyBucketSpanWasRequestedOnlyOnce();
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

        int totalBuckets = 1;
        long endTime = 3600;
        int bucketSpan = 3600;
        givenBucketSpan(bucketSpan);
        List<Bucket> buckets = Arrays.asList(bucket);
        givenProviderReturnsBuckets(0, endTime, totalBuckets, buckets);

        m_ScoresUpdater.update(QUANTILES_STATE, 3600, m_Logger);

        verifyNormaliserWasInvoked(bucketSpan, 1);
        verifyBucketWasUpdated(bucket);
        verifyBucketRecordsWereNotUpdated("0");
        verifyBucketSpanWasRequestedOnlyOnce();
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

        int totalBuckets = 1;
        long endTime = 3600;
        int bucketSpan = 3600;
        givenBucketSpan(bucketSpan);
        List<Bucket> buckets = Arrays.asList(bucket);
        givenProviderReturnsBuckets(0, endTime, totalBuckets, buckets);

        m_ScoresUpdater.update(QUANTILES_STATE, 3600, m_Logger);

        verifyNormaliserWasInvoked(bucketSpan, 1);
        verifyBucketWasNotUpdated(bucket);
        verifyRecordsWereUpdated("0", Arrays.asList(record1, record3));
        verifyBucketSpanWasRequestedOnlyOnce();
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

        int totalBuckets = 1;
        long endTime = 3600;
        int bucketSpan = 3600;
        givenBucketSpan(bucketSpan);
        List<Bucket> buckets = Arrays.asList(bucket);
        givenProviderReturnsBuckets(0, endTime, totalBuckets, buckets);

        m_ScoresUpdater.update(QUANTILES_STATE, 3600, m_Logger);

        verifyNormaliserWasInvoked(bucketSpan, 1);
        verifyBucketWasUpdated(bucket);
        verifyRecordsWereUpdated("0", Arrays.asList(record1, record3));
        verifyBucketSpanWasRequestedOnlyOnce();
    }

    @Test
    public void testUpdate_GivenEnoughBucketsToRequirePaging() throws UnknownJobException,
            NativeProcessRunException
    {
        List<Bucket> batch1 = new ArrayList<>();
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
        List<Bucket> batch2 = Arrays.asList(secondBatchBucket);

        int totalBuckets = 10001;
        long endTime = 3600;
        int bucketSpan = 3600;
        givenBucketSpan(bucketSpan);
        givenProviderReturnsBuckets(0, endTime, totalBuckets, batch1);
        givenProviderReturnsBuckets(10000, endTime, totalBuckets, batch2);

        m_ScoresUpdater.update(QUANTILES_STATE, 3600, m_Logger);

        verifyNormaliserWasInvoked(bucketSpan, 2);

        // Batch 1 - Just verify first and last were updated as Mockito
        // is forbiddingly slow when tring to verify all 10000
        verifyBucketWasUpdated(batch1.get(0));
        verifyBucketRecordsWereNotUpdated(batch1.get(0).getId());
        verifyBucketWasUpdated(batch1.get(9999));
        verifyBucketRecordsWereNotUpdated(batch1.get(9999).getId());

        verifyBucketWasUpdated(secondBatchBucket);
        verifyBucketRecordsWereNotUpdated(secondBatchBucket.getId());
        verifyBucketSpanWasRequestedOnlyOnce();
    }

    @Test
    public void testUpdate_GivenInfluencerWithBigChange()
            throws UnknownJobException, NativeProcessRunException
    {
        Influencer influencer = new Influencer();
        influencer.raiseBigNormalisedUpdateFlag();

        int totalInfluencers = 1;
        long endTime = 3600;
        int bucketSpan = 3600;
        givenBucketSpan(bucketSpan);
        List<Influencer> influencers = Arrays.asList(influencer);
        givenProviderReturnsInfluencers(0, endTime, totalInfluencers, influencers);

        m_ScoresUpdater.update(QUANTILES_STATE, 3600, m_Logger);

        verifyNormaliserWasInvoked(bucketSpan, 1);
        verifyInfluencerWasUpdated(influencer);
        verifyBucketSpanWasRequestedOnlyOnce();
    }

    private void verifyNormaliserWasInvoked(int bucketSpan, int times) throws NativeProcessRunException,
            UnknownJobException
    {
        verify(m_Normaliser, times(times)).normalise(eq(bucketSpan), anyListOf(Normalisable.class),
                eq(QUANTILES_STATE));
    }

    private void givenBucketSpan(int bucketSpan)
    {
        when(m_JobProvider.getField(JOB_ID, "analysisConfig.bucketSpan")).thenReturn(
                new Integer(bucketSpan));
    }

    private BucketInfluencer createTimeBucketInfluencer(double probability, double anomalyScore)
    {
        BucketInfluencer influencer = new BucketInfluencer();
        influencer.setProbability(probability);
        influencer.setAnomalyScore(anomalyScore);
        influencer.setInfluencerFieldName(BucketInfluencer.BUCKET_TIME);
        return influencer;
    }

    private void givenProviderReturnsNoBuckets() throws UnknownJobException
    {
        QueryPage<Bucket> page = new QueryPage<>(Collections.emptyList(), 0);
        when(m_JobProvider.buckets(eq(JOB_ID), anyBoolean(), anyBoolean(), anyInt(), anyInt(),
                anyLong(), anyLong(), eq(0.0), eq(0.0))).thenReturn(page);
    }

    private void givenProviderReturnsBuckets(int skip, long endTime, int hitCount, List<Bucket> buckets) throws UnknownJobException
    {
        QueryPage<Bucket> page = new QueryPage<>(buckets, hitCount);
        when(m_JobProvider.buckets(JOB_ID, false, false, skip, MAX_BUCKETS_PER_PAGE, 0, endTime, 0.0, 0.0))
                .thenReturn(page);
    }

    private void givenProviderReturnsNoInfluencers() throws UnknownJobException
    {
        QueryPage<Influencer> page = new QueryPage<>(Collections.emptyList(), 0);
        when(m_JobProvider.influencers(eq(JOB_ID), anyInt(), anyInt(), anyLong(), anyLong(),
                eq(null), eq(false), eq(0.0))).thenReturn(page);
    }

    private void givenProviderReturnsInfluencers(int skip, long endTime, int hitCount,
            List<Influencer> influencers) throws UnknownJobException
    {
        QueryPage<Influencer> page = new QueryPage<>(influencers, hitCount);
        when(m_JobProvider.influencers(JOB_ID, skip, MAX_INFLUENCERS_PER_PAGE, 0, endTime, null, false, 0.0))
                .thenReturn(page);
    }

    private void verifyBucketSpanWasNotRequested()
    {
        verify(m_JobProvider, times(0)).getField(JOB_ID, "analysisConfig.bucketSpan");
    }

    private void verifyBucketSpanWasRequestedOnlyOnce()
    {
        verify(m_JobProvider, times(1)).getField(JOB_ID, "analysisConfig.bucketSpan");
    }

    private void verifyBucketWasUpdated(Bucket bucket)
    {
        verify(m_JobRenormaliser).updateBucket(JOB_ID, bucket);
    }

    private void verifyRecordsWereUpdated(String bucketId, List<AnomalyRecord> records)
    {
        verify(m_JobRenormaliser).updateRecords(JOB_ID, bucketId, records);
    }

    private void verifyBucketWasNotUpdated(Bucket bucket)
    {
        verify(m_JobRenormaliser, never()).updateBucket(JOB_ID, bucket);
    }

    private void verifyBucketRecordsWereNotUpdated(String bucketId)
    {
        verify(m_JobRenormaliser, never()).updateRecords(eq(JOB_ID), eq(bucketId),
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
        verify(m_JobRenormaliser).updateInfluencer(JOB_ID, influencer);
    }
}
