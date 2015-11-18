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
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.prelert.job.UnknownJobException;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.persistence.QueryPage;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.Influencer;

public class ScoresUpdaterTest
{
    private static final String JOB_ID = "foo";
    private static final int MAX_BUCKETS_PER_PAGE = 10000;
    private static final String QUANTILES_STATE = "someState";

    @Mock private JobProvider m_JobProvider;
    @Mock private Normaliser m_Normaliser;
    @Mock private Logger m_Logger;

    private ScoresUpdater m_ScoresUpdater;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        m_ScoresUpdater = new ScoresUpdater(JOB_ID, m_JobProvider, (jobId, logger) -> m_Normaliser);
        givenProviderReturnsNoInfluencers();
    }

    @Test
    public void testUpdate_GivenSingleBucketWithoutBigChangeAndNoRecords()
            throws UnknownJobException, NativeProcessRunException
    {
        Bucket bucket = new Bucket();
        bucket.setId("0");
        int totalBuckets = 1;
        long endTime = 3600;
        int bucketSpan = 3600;
        givenBucketSpan(bucketSpan);
        List<Bucket> originalBatch = Arrays.asList(bucket);
        List<Bucket> normalisedBatch = Arrays.asList(bucket);
        givenProviderReturnsBuckets(0, endTime, totalBuckets, originalBatch);

        givenNormalisedBuckets(bucketSpan, originalBatch, normalisedBatch);

        m_ScoresUpdater.update(QUANTILES_STATE, 3600, m_Logger);

        verifyBucketWasNotUpdated("0");
        verifyBucketRecordsWereNotUpdated("0");
        verifyBucketSpanWasNotRequested();
    }

    @Test
    public void testUpdate_GivenSingleBucketWithoutBigChangeAndRecordsWithoutBigChange()
            throws UnknownJobException, NativeProcessRunException
    {
        Bucket bucket = new Bucket();
        List<AnomalyRecord> records = new ArrayList<>();
        AnomalyRecord record1 = createRecordWithoutBigChange();
        AnomalyRecord record2 = createRecordWithoutBigChange();
        records.add(record1);
        records.add(record2);
        bucket.setRecords(records);

        bucket.setId("0");
        int totalBuckets = 1;
        long endTime = 3600;
        int bucketSpan = 3600;
        givenBucketSpan(bucketSpan);
        List<Bucket> originalBatch = Arrays.asList(bucket);
        List<Bucket> normalisedBatch = Arrays.asList(bucket);
        givenProviderReturnsBuckets(0, endTime, totalBuckets, originalBatch);

        givenNormalisedBuckets(bucketSpan, originalBatch, normalisedBatch);

        m_ScoresUpdater.update(QUANTILES_STATE, 3600, m_Logger);

        verifyBucketWasNotUpdated("0");
        verifyBucketRecordsWereNotUpdated("0");
        verifyBucketSpanWasNotRequested();
    }

    @Test
    public void testUpdate_GivenSingleBucketWithBigChangeAndNoRecords()
            throws UnknownJobException, NativeProcessRunException
    {
        Bucket bucket = new Bucket();
        bucket.setId("0");
        bucket.setRawAnomalyScore(42.0);
        bucket.setAnomalyScore(42.0);
        bucket.setMaxNormalizedProbability(50.0);
        Bucket normalisedBucket = new Bucket();
        normalisedBucket.setId("0");
        normalisedBucket.setAnomalyScore(60.0);
        normalisedBucket.setMaxNormalizedProbability(99.0);
        normalisedBucket.raiseBigNormalisedUpdateFlag();

        int totalBuckets = 1;
        long endTime = 3600;
        int bucketSpan = 3600;
        givenBucketSpan(bucketSpan);
        List<Bucket> originalBatch = Arrays.asList(bucket);
        List<Bucket> normalisedBatch = Arrays.asList(normalisedBucket);
        givenProviderReturnsBuckets(0, endTime, totalBuckets, originalBatch);

        givenNormalisedBuckets(bucketSpan, originalBatch, normalisedBatch);

        m_ScoresUpdater.update(QUANTILES_STATE, 3600, m_Logger);

        verifyBucketWasUpdated(normalisedBucket);
        verifyBucketRecordsWereNotUpdated("0");
        verifyBucketSpanWasRequestedOnlyOnce();
    }

    @Test
    public void testUpdate_GivenSingleBucketWithoutBigChangeAndSomeRecordsWithBigChange()
            throws UnknownJobException, NativeProcessRunException
    {
        Bucket bucket = new Bucket();
        bucket.setId("0");
        bucket.setRawAnomalyScore(42.0);
        bucket.setAnomalyScore(42.0);
        bucket.setMaxNormalizedProbability(50.0);
        Bucket normalisedBucket = new Bucket();
        normalisedBucket.setId("0");
        normalisedBucket.setAnomalyScore(60.0);
        normalisedBucket.setMaxNormalizedProbability(99.0);
        List<AnomalyRecord> records = new ArrayList<>();
        AnomalyRecord record1 = createRecordWithBigChange();
        AnomalyRecord record2 = createRecordWithoutBigChange();
        AnomalyRecord record3 = createRecordWithBigChange();
        records.add(record1);
        records.add(record2);
        records.add(record3);
        normalisedBucket.setRecords(records);

        int totalBuckets = 1;
        long endTime = 3600;
        int bucketSpan = 3600;
        givenBucketSpan(bucketSpan);
        List<Bucket> originalBatch = Arrays.asList(bucket);
        List<Bucket> normalisedBatch = Arrays.asList(normalisedBucket);
        givenProviderReturnsBuckets(0, endTime, totalBuckets, originalBatch);

        givenNormalisedBuckets(bucketSpan, originalBatch, normalisedBatch);

        m_ScoresUpdater.update(QUANTILES_STATE, 3600, m_Logger);

        verifyBucketWasNotUpdated("0");
        verifyRecordsWereUpdated("0", Arrays.asList(record1, record3));
        verifyBucketSpanWasRequestedOnlyOnce();
    }

    @Test
    public void testUpdate_GivenSingleBucketWithBigChangeAndSomeRecordsWithBigChange()
            throws UnknownJobException, NativeProcessRunException
    {
        Bucket bucket = new Bucket();
        bucket.setId("0");
        bucket.setRawAnomalyScore(42.0);
        bucket.setAnomalyScore(42.0);
        bucket.setMaxNormalizedProbability(50.0);
        Bucket normalisedBucket = new Bucket();
        normalisedBucket.setId("0");
        normalisedBucket.setRawAnomalyScore(42.0);
        normalisedBucket.setAnomalyScore(60.0);
        normalisedBucket.setMaxNormalizedProbability(99.0);
        normalisedBucket.raiseBigNormalisedUpdateFlag();
        List<AnomalyRecord> records = new ArrayList<>();
        AnomalyRecord record1 = createRecordWithBigChange();
        AnomalyRecord record2 = createRecordWithoutBigChange();
        AnomalyRecord record3 = createRecordWithBigChange();
        records.add(record1);
        records.add(record2);
        records.add(record3);
        normalisedBucket.setRecords(records);

        int totalBuckets = 1;
        long endTime = 3600;
        int bucketSpan = 3600;
        givenBucketSpan(bucketSpan);
        List<Bucket> originalBatch = Arrays.asList(bucket);
        List<Bucket> normalisedBatch = Arrays.asList(normalisedBucket);
        givenProviderReturnsBuckets(0, endTime, totalBuckets, originalBatch);

        givenNormalisedBuckets(bucketSpan, originalBatch, normalisedBatch);

        m_ScoresUpdater.update(QUANTILES_STATE, 3600, m_Logger);

        verifyBucketWasUpdated(normalisedBucket);
        verifyRecordsWereUpdated("0", Arrays.asList(record1, record3));
        verifyBucketSpanWasRequestedOnlyOnce();
    }

    @Test
    public void testUpdate_GivenEnoughBucketsToRequirePaging() throws UnknownJobException,
            NativeProcessRunException
    {
        List<Bucket> originalBatch1 = new ArrayList<>();
        List<Bucket> normalisedBatch1 = new ArrayList<>();
        for (int i = 0; i < 10000; ++i)
        {
            Bucket bucket = new Bucket();
            bucket.setId(String.valueOf(i));
            bucket.setRawAnomalyScore(42.0);
            bucket.setAnomalyScore(42.0);
            bucket.setMaxNormalizedProbability(50.0);
            originalBatch1.add(bucket);
            Bucket normalisedBucket = new Bucket();
            normalisedBucket.setId(String.valueOf(i));
            normalisedBucket.setRawAnomalyScore(42.0);
            normalisedBucket.setAnomalyScore(60.0);
            normalisedBucket.setMaxNormalizedProbability(99.0);
            normalisedBucket.raiseBigNormalisedUpdateFlag();
            normalisedBatch1.add(normalisedBucket);
        }

        Bucket bucket = new Bucket();
        bucket.setId("10000");
        bucket.setRawAnomalyScore(42.0);
        bucket.setAnomalyScore(42.0);
        bucket.setMaxNormalizedProbability(50.0);
        Bucket normalisedBucket = new Bucket();
        normalisedBucket.setId("10000");
        normalisedBucket.setRawAnomalyScore(42.0);
        normalisedBucket.setAnomalyScore(60.0);
        normalisedBucket.setMaxNormalizedProbability(99.0);
        normalisedBucket.raiseBigNormalisedUpdateFlag();
        List<Bucket> originalBatch2 = Arrays.asList(bucket);
        List<Bucket> normalisedBatch2 = Arrays.asList(normalisedBucket);

        int totalBuckets = 10001;
        long endTime = 3600;
        int bucketSpan = 3600;
        givenBucketSpan(bucketSpan);
        givenProviderReturnsBuckets(0, endTime, totalBuckets, originalBatch1);
        givenProviderReturnsBuckets(10000, endTime, totalBuckets, originalBatch2);

        givenNormalisedBuckets(bucketSpan, originalBatch1, originalBatch2, normalisedBatch1,
                normalisedBatch2);

        m_ScoresUpdater.update(QUANTILES_STATE, 3600, m_Logger);

        for (Bucket b : normalisedBatch1)
        {
            verifyBucketWasUpdated(b);
            verifyBucketRecordsWereNotUpdated(b.getId());
            // Just verify the first bucket for the time being as the Mockito is
            // horrifically slow to verify all 10000
            break;
        }
        verifyBucketWasUpdated(normalisedBucket);
        verifyBucketRecordsWereNotUpdated(normalisedBucket.getId());
        verifyBucketSpanWasRequestedOnlyOnce();
    }

    private void givenBucketSpan(int bucketSpan)
    {
        when(m_JobProvider.getField(JOB_ID, "analysisConfig.bucketSpan")).thenReturn(
                new Integer(bucketSpan));
    }

    private void givenProviderReturnsBuckets(int skip, long endTime, int hitCount, List<Bucket> buckets) throws UnknownJobException
    {
        QueryPage<Bucket> page = new QueryPage<>(buckets, hitCount);
        when(m_JobProvider.buckets(JOB_ID, false, false, skip, MAX_BUCKETS_PER_PAGE, 0, endTime, 0.0, 0.0))
                .thenReturn(page);
    }

    private void givenProviderReturnsNoInfluencers()
    {
        QueryPage<Influencer> page = new QueryPage<>(Collections.emptyList(), 0);
        when(m_JobProvider.influencers(eq(JOB_ID), anyInt(), anyInt(), anyLong(), anyLong(),
                eq(null), eq(false), eq(0.0))).thenReturn(page);
    }

    private void givenNormalisedBuckets(int bucketSpan, List<Bucket> originalBuckets,
            List<Bucket> normalisedBuckets) throws NativeProcessRunException, UnknownJobException
    {
        givenNormalisedBuckets(bucketSpan, originalBuckets, new ArrayList<>(), normalisedBuckets,
                new ArrayList<>());
    }
    private void givenNormalisedBuckets(int bucketSpan, List<Bucket> originalBuckets1,
            List<Bucket> originalBuckets2, List<Bucket> normalisedBuckets1, List<Bucket> normalisedBuckets2)
            throws NativeProcessRunException, UnknownJobException
    {
        doAnswer(new Answer<Void>()
        {
            private int m_Count = 0;

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                ++m_Count;

                if (m_Count == 1)
                {
                    applyBuckets(originalBuckets1, normalisedBuckets1);
                }
                if (m_Count == 2)
                {
                    applyBuckets(originalBuckets2, normalisedBuckets2);
                }
                return null;
            }
        }).when(m_Normaliser).normalise(eq(bucketSpan), anyListOf(Normalisable.class), eq(QUANTILES_STATE));
    }

    private void applyBuckets(List<Bucket> oldBuckets, List<Bucket> newBuckets)
    {
        assertEquals(oldBuckets.size(), newBuckets.size());
        for (int i = 0; i < oldBuckets.size(); ++i)
        {
            Bucket newBucket = newBuckets.get(i);
            Bucket oldBucket = oldBuckets.get(i);
            oldBucket.setRawAnomalyScore(newBucket.getRawAnomalyScore());
            oldBucket.setAnomalyScore(newBucket.getAnomalyScore());
            oldBucket.setMaxNormalizedProbability(newBucket.getMaxNormalizedProbability());
            if (newBucket.hadBigNormalisedUpdate())
            {
                oldBucket.raiseBigNormalisedUpdateFlag();
            }
            else
            {
                oldBucket.resetBigNormalisedUpdateFlag();
            }
            oldBucket.setRecords(newBucket.getRecords());
        }
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
        verify(m_JobProvider).updateBucket(JOB_ID, bucket.getId(), bucket.getAnomalyScore(),
                bucket.getMaxNormalizedProbability());
    }

    private void verifyRecordsWereUpdated(String bucketId, List<AnomalyRecord> records)
    {
        verify(m_JobProvider).updateRecords(JOB_ID, bucketId, records);
    }

    private void verifyBucketWasNotUpdated(String bucketId)
    {
        verify(m_JobProvider, never()).updateBucket(eq(JOB_ID), eq(bucketId), anyDouble(),
                anyDouble());
    }

    private void verifyBucketRecordsWereNotUpdated(String bucketId)
    {
        verify(m_JobProvider, never()).updateRecords(eq(JOB_ID), eq(bucketId),
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
}
