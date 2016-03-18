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

package com.prelert.job.persistence;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.prelert.job.JobDetails;
import com.prelert.job.ModelSnapshot;
import com.prelert.job.UnknownJobException;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.Influencer;

public class OldDataRemoverTest
{
    private static final String JOB_WITH_RETENTION_ID = "foo";
    private static final String JOB_NO_RETENTION_ID = "bar";

    @Mock private JobProvider m_JobProvider;
    @Mock private JobDataDeleterFactory m_DeleterFactory;

    private OldDataRemover m_OldDataRemover;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        m_OldDataRemover = new OldDataRemover(m_JobProvider, m_DeleterFactory);
    }

    @Test
    public void testRemoveOldModelSnapshots() throws UnknownJobException
    {
        JobDetails jobWithRetention = new JobDetails();
        jobWithRetention.setId(JOB_WITH_RETENTION_ID);
        jobWithRetention.setModelSnapshotRetentionDays(10L);
        List<JobDetails> jobs = Arrays.asList(jobWithRetention);
        when(m_JobProvider.getJobs(0, 10000)).thenReturn(new QueryPage<>(jobs, 1));

        long startOfDayEpochMs = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000;
        long cutoffEpochMs = startOfDayEpochMs - 10 * 86400000L;

        ModelSnapshot modelSnapshot1 = new ModelSnapshot();
        ModelSnapshot modelSnapshot2 = new ModelSnapshot();
        ModelSnapshot modelSnapshot3 = new ModelSnapshot();
        modelSnapshot3.setSnapshotId("highest priority");
        Deque<ModelSnapshot> modelSnapshotBatch = new ArrayDeque<>();
        modelSnapshotBatch.add(modelSnapshot1);
        modelSnapshotBatch.add(modelSnapshot2);
        List<Deque<ModelSnapshot>> modelSnapshotBatches = Arrays.asList(modelSnapshotBatch);
        MockBatchedResultsIterator<ModelSnapshot> modelSnapshotIterator = new MockBatchedResultsIterator<>(0, cutoffEpochMs, modelSnapshotBatches);

        when(m_JobProvider.newBatchedModelSnapshotIterator(JOB_WITH_RETENTION_ID)).thenReturn(modelSnapshotIterator);
        when(m_JobProvider.modelSnapshots(JOB_WITH_RETENTION_ID, 0, 1)).thenReturn(new QueryPage<ModelSnapshot>(Arrays.asList(modelSnapshot3), 1));

        JobDataDeleter deleter = mock(JobDataDeleter.class);
        when(m_DeleterFactory.newDeleter(JOB_WITH_RETENTION_ID)).thenReturn(deleter);

        m_OldDataRemover.removeOldModelSnapshots();

        verify(m_DeleterFactory).newDeleter(JOB_WITH_RETENTION_ID);
        ArgumentCaptor<ModelSnapshot> modelSnapshotCaptor = ArgumentCaptor.forClass(ModelSnapshot.class);
        verify(deleter, times(2)).deleteModelSnapshot(modelSnapshotCaptor.capture());
        assertEquals(Arrays.asList(modelSnapshot1, modelSnapshot2), modelSnapshotCaptor.getAllValues());
        verify(deleter).commit();

        Mockito.verifyNoMoreInteractions(m_DeleterFactory, deleter);
    }

    @Test
    public void testRemoveOldResults()
    {
        JobDetails jobNoRetention = new JobDetails();
        jobNoRetention.setId(JOB_NO_RETENTION_ID);
        jobNoRetention.setResultsRetentionDays(null);

        JobDetails jobWithRetention = new JobDetails();
        jobWithRetention.setId(JOB_WITH_RETENTION_ID);
        jobWithRetention.setResultsRetentionDays(10L);
        List<JobDetails> jobs = Arrays.asList(jobNoRetention, jobWithRetention);
        when(m_JobProvider.getJobs(0, 10000)).thenReturn(new QueryPage<>(jobs, 1));

        long startOfDayEpochMs = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000;
        long cutoffEpochMs = startOfDayEpochMs - 10 * 86400000L;

        Bucket bucket1 = new Bucket();
        Bucket bucket2 = new Bucket();
        Deque<Bucket> bucketBatch1 = new ArrayDeque<>();
        Deque<Bucket> bucketBatch2 = new ArrayDeque<>();
        bucketBatch1.add(bucket1);
        bucketBatch2.add(bucket2);
        List<Deque<Bucket>> bucketBatches = Arrays.asList(bucketBatch1, bucketBatch2);
        MockBatchedResultsIterator<Bucket> bucketsIterator = new MockBatchedResultsIterator<>(0, cutoffEpochMs, bucketBatches);

        Influencer influencer = new Influencer();
        Deque<Influencer> influencerBatch = new ArrayDeque<>();
        influencerBatch.add(influencer);
        List<Deque<Influencer>> influencerBatches = new ArrayList<>();
        influencerBatches.add(influencerBatch);
        MockBatchedResultsIterator<Influencer> influencersIterator = new MockBatchedResultsIterator<>(0, cutoffEpochMs, influencerBatches);

        when(m_JobProvider.newBatchedBucketsIterator(JOB_WITH_RETENTION_ID)).thenReturn(bucketsIterator);
        when(m_JobProvider.newBatchedInfluencersIterator(JOB_WITH_RETENTION_ID)).thenReturn(influencersIterator);

        JobDataDeleter deleter = mock(JobDataDeleter.class);
        when(m_DeleterFactory.newDeleter(JOB_WITH_RETENTION_ID)).thenReturn(deleter);

        m_OldDataRemover.removeOldResults();

        verify(m_DeleterFactory).newDeleter(JOB_WITH_RETENTION_ID);
        ArgumentCaptor<Bucket> bucketCaptor = ArgumentCaptor.forClass(Bucket.class);
        verify(deleter, times(2)).deleteBucket(bucketCaptor.capture());
        assertEquals(Arrays.asList(bucket1, bucket2), bucketCaptor.getAllValues());
        verify(deleter).deleteInfluencer(influencer);
        verify(deleter).commit();

        Mockito.verifyNoMoreInteractions(m_DeleterFactory, deleter);
    }
}
