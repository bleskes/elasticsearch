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

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
import com.prelert.job.ModelSizeStats;
import com.prelert.job.ModelSnapshot;
import com.prelert.job.UnknownJobException;
import com.prelert.job.audit.Auditor;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.Influencer;
import com.prelert.job.results.ModelDebugOutput;

public class OldDataRemoverTest
{
    private static final String JOB_WITH_RETENTION_ID = "foo";
    private static final String JOB_NO_RETENTION_ID = "bar";

    @Mock private JobProvider m_JobProvider;
    @Mock private JobDataDeleterFactory m_DeleterFactory;
    @Mock private Auditor m_Auditor;

    private OldDataRemover m_OldDataRemover;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        when(m_JobProvider.audit(anyString())).thenReturn(m_Auditor);
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
        MockBatchedDocumentsIterator<ModelSnapshot> modelSnapshotIterator = new MockBatchedDocumentsIterator<>(0, cutoffEpochMs, modelSnapshotBatches);

        when(m_JobProvider.newBatchedModelSnapshotIterator(JOB_WITH_RETENTION_ID)).thenReturn(modelSnapshotIterator);
        when(m_JobProvider.modelSnapshots(JOB_WITH_RETENTION_ID, 0, 1)).thenReturn(new QueryPage<ModelSnapshot>(Arrays.asList(modelSnapshot3), 1));

        JobDataDeleter deleter = mock(JobDataDeleter.class);
        when(m_DeleterFactory.newDeleter(JOB_WITH_RETENTION_ID)).thenReturn(deleter);

        m_OldDataRemover.removeOldData();

        verify(m_DeleterFactory).newDeleter(JOB_WITH_RETENTION_ID);
        ArgumentCaptor<ModelSnapshot> modelSnapshotCaptor = ArgumentCaptor.forClass(ModelSnapshot.class);
        verify(deleter, times(2)).deleteModelSnapshot(modelSnapshotCaptor.capture());
        assertEquals(Arrays.asList(modelSnapshot1, modelSnapshot2), modelSnapshotCaptor.getAllValues());
        verify(deleter).commitAndFreeDiskSpace();

        Mockito.verifyNoMoreInteractions(m_DeleterFactory, deleter);
    }

    @Test
    public void testRemoveOldResults() throws UnknownJobException
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
        MockBatchedDocumentsIterator<Bucket> bucketsIterator = new MockBatchedDocumentsIterator<>(0, cutoffEpochMs, bucketBatches);

        Influencer influencer = new Influencer();
        Deque<Influencer> influencerBatch = new ArrayDeque<>();
        influencerBatch.add(influencer);
        List<Deque<Influencer>> influencerBatches = new ArrayList<>();
        influencerBatches.add(influencerBatch);
        MockBatchedDocumentsIterator<Influencer> influencersIterator = new MockBatchedDocumentsIterator<>(0, cutoffEpochMs, influencerBatches);

        when(m_JobProvider.newBatchedBucketsIterator(JOB_WITH_RETENTION_ID)).thenReturn(bucketsIterator);
        when(m_JobProvider.newBatchedInfluencersIterator(JOB_WITH_RETENTION_ID)).thenReturn(influencersIterator);

        List<ModelSnapshot> snapshots = new ArrayList<>();
        QueryPage<ModelSnapshot> queryPage = new QueryPage<ModelSnapshot>(snapshots, 0);
        when(m_JobProvider.modelSnapshots(JOB_WITH_RETENTION_ID, 0, 1)).thenReturn(queryPage);
        when(m_JobProvider.modelSnapshots(JOB_NO_RETENTION_ID, 0, 1)).thenReturn(queryPage);

        List<Deque<ModelDebugOutput>> debugBatches = Arrays.asList();
        MockBatchedDocumentsIterator<ModelDebugOutput> debugIterator = new MockBatchedDocumentsIterator<>(0, cutoffEpochMs, debugBatches);
        when(m_JobProvider.newBatchedModelDebugOutputIterator(JOB_WITH_RETENTION_ID)).thenReturn(debugIterator);
        when(m_JobProvider.newBatchedModelDebugOutputIterator(JOB_NO_RETENTION_ID)).thenReturn(debugIterator);

        List<Deque<ModelSizeStats>> statsBatches = Arrays.asList();
        MockBatchedDocumentsIterator<ModelSizeStats> statsIterator = new MockBatchedDocumentsIterator<>(0, cutoffEpochMs, statsBatches);
        when(m_JobProvider.newBatchedModelSizeStatsIterator(JOB_WITH_RETENTION_ID)).thenReturn(statsIterator);
        when(m_JobProvider.newBatchedModelSizeStatsIterator(JOB_NO_RETENTION_ID)).thenReturn(statsIterator);

        JobDataDeleter deleter = mock(JobDataDeleter.class);
        when(m_DeleterFactory.newDeleter(JOB_WITH_RETENTION_ID)).thenReturn(deleter);
        when(m_DeleterFactory.newDeleter(JOB_NO_RETENTION_ID)).thenReturn(deleter);

        m_OldDataRemover.removeOldData();

        verify(m_DeleterFactory).newDeleter(JOB_WITH_RETENTION_ID);
        verify(m_DeleterFactory).newDeleter(JOB_NO_RETENTION_ID);
        ArgumentCaptor<Bucket> bucketCaptor = ArgumentCaptor.forClass(Bucket.class);
        verify(deleter, times(2)).deleteBucket(bucketCaptor.capture());
        assertEquals(Arrays.asList(bucket1, bucket2), bucketCaptor.getAllValues());
        verify(deleter).deleteInfluencer(influencer);
        verify(deleter, times(2)).commitAndFreeDiskSpace();
        verifyDeletedResultsWereAuditted(JOB_WITH_RETENTION_ID, cutoffEpochMs);

        Mockito.verifyNoMoreInteractions(m_DeleterFactory, m_Auditor, deleter);
    }

    @Test
    public void testRemoveOldModelDebugOutput() throws UnknownJobException
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

        ModelDebugOutput debug1 = new ModelDebugOutput();
        ModelDebugOutput debug2 = new ModelDebugOutput();

        Deque<ModelDebugOutput> debugBatch1 = new ArrayDeque<>();
        Deque<ModelDebugOutput> debugBatch2 = new ArrayDeque<>();
        debugBatch1.add(debug1);
        debugBatch2.addLast(debug2);
        List<Deque<ModelDebugOutput>> debugBatches = Arrays.asList(debugBatch1, debugBatch2);

        MockBatchedDocumentsIterator<ModelDebugOutput> debugIterator = new MockBatchedDocumentsIterator<>(0, cutoffEpochMs, debugBatches);

        when(m_JobProvider.newBatchedModelDebugOutputIterator(JOB_WITH_RETENTION_ID)).thenReturn(debugIterator);

        List<ModelSnapshot> snapshots = new ArrayList<>();
        QueryPage<ModelSnapshot> queryPage = new QueryPage<ModelSnapshot>(snapshots, 0);
        when(m_JobProvider.modelSnapshots(JOB_WITH_RETENTION_ID, 0, 1)).thenReturn(queryPage);
        when(m_JobProvider.modelSnapshots(JOB_NO_RETENTION_ID, 0, 1)).thenReturn(queryPage);

        List<Deque<ModelSizeStats>> statsBatches = Arrays.asList();
        MockBatchedDocumentsIterator<ModelSizeStats> statsIterator = new MockBatchedDocumentsIterator<>(0, cutoffEpochMs, statsBatches);
        when(m_JobProvider.newBatchedModelSizeStatsIterator(JOB_WITH_RETENTION_ID)).thenReturn(statsIterator);
        when(m_JobProvider.newBatchedModelSizeStatsIterator(JOB_NO_RETENTION_ID)).thenReturn(statsIterator);

        List<Deque<Bucket>> bucketBatches = Arrays.asList();
        MockBatchedDocumentsIterator<Bucket> bucketsIterator = new MockBatchedDocumentsIterator<>(0, cutoffEpochMs, bucketBatches);
        when(m_JobProvider.newBatchedBucketsIterator(JOB_WITH_RETENTION_ID)).thenReturn(bucketsIterator);
        when(m_JobProvider.newBatchedBucketsIterator(JOB_NO_RETENTION_ID)).thenReturn(bucketsIterator);

        List<Deque<Influencer>> influencerBatches = new ArrayList<>();
        MockBatchedDocumentsIterator<Influencer> influencersIterator = new MockBatchedDocumentsIterator<>(0, cutoffEpochMs, influencerBatches);
        when(m_JobProvider.newBatchedInfluencersIterator(JOB_WITH_RETENTION_ID)).thenReturn(influencersIterator);
        when(m_JobProvider.newBatchedInfluencersIterator(JOB_NO_RETENTION_ID)).thenReturn(influencersIterator);

        JobDataDeleter deleter = mock(JobDataDeleter.class);
        when(m_DeleterFactory.newDeleter(JOB_WITH_RETENTION_ID)).thenReturn(deleter);
        when(m_DeleterFactory.newDeleter(JOB_NO_RETENTION_ID)).thenReturn(deleter);

        m_OldDataRemover.removeOldData();

        verify(m_DeleterFactory).newDeleter(JOB_WITH_RETENTION_ID);
        verify(m_DeleterFactory).newDeleter(JOB_NO_RETENTION_ID);

        ArgumentCaptor<ModelDebugOutput>debugCaptor = ArgumentCaptor.forClass(ModelDebugOutput.class);
        verify(deleter, times(2)).deleteModelDebugOutput(debugCaptor.capture());
        assertEquals(Arrays.asList(debug1, debug2), debugCaptor.getAllValues());
        verify(deleter, times(2)).commitAndFreeDiskSpace();
        verifyDeletedResultsWereAuditted(JOB_WITH_RETENTION_ID, cutoffEpochMs);

        Mockito.verifyNoMoreInteractions(m_DeleterFactory, m_Auditor, deleter);
    }

    @Test
    public void testRemoveOldModelSizeStats() throws UnknownJobException
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

        ModelSizeStats stats1 = new ModelSizeStats();
        ModelSizeStats stats2 = new ModelSizeStats();

        Deque<ModelSizeStats> statsBatch1 = new ArrayDeque<>();
        Deque<ModelSizeStats> statsBatch2 = new ArrayDeque<>();
        statsBatch1.add(stats1);
        statsBatch2.add(stats2);
        List<Deque<ModelSizeStats>> statsBatches = Arrays.asList(statsBatch1, statsBatch2);

        MockBatchedDocumentsIterator<ModelSizeStats> statsIterator = new MockBatchedDocumentsIterator<>(0, cutoffEpochMs, statsBatches);
        when(m_JobProvider.newBatchedModelSizeStatsIterator(JOB_WITH_RETENTION_ID)).thenReturn(statsIterator);

        List<ModelSnapshot> snapshots = new ArrayList<>();
        QueryPage<ModelSnapshot> queryPage = new QueryPage<ModelSnapshot>(snapshots, 0);
        when(m_JobProvider.modelSnapshots(JOB_WITH_RETENTION_ID, 0, 1)).thenReturn(queryPage);
        when(m_JobProvider.modelSnapshots(JOB_NO_RETENTION_ID, 0, 1)).thenReturn(queryPage);

        List<Deque<Bucket>> bucketBatches = Arrays.asList();
        MockBatchedDocumentsIterator<Bucket> bucketsIterator = new MockBatchedDocumentsIterator<>(0, cutoffEpochMs, bucketBatches);
        when(m_JobProvider.newBatchedBucketsIterator(JOB_WITH_RETENTION_ID)).thenReturn(bucketsIterator);
        when(m_JobProvider.newBatchedBucketsIterator(JOB_NO_RETENTION_ID)).thenReturn(bucketsIterator);

        List<Deque<Influencer>> influencerBatches = new ArrayList<>();
        MockBatchedDocumentsIterator<Influencer> influencersIterator = new MockBatchedDocumentsIterator<>(0, cutoffEpochMs, influencerBatches);
        when(m_JobProvider.newBatchedInfluencersIterator(JOB_WITH_RETENTION_ID)).thenReturn(influencersIterator);
        when(m_JobProvider.newBatchedInfluencersIterator(JOB_NO_RETENTION_ID)).thenReturn(influencersIterator);

        List<Deque<ModelDebugOutput>> debugBatches = Arrays.asList();
        MockBatchedDocumentsIterator<ModelDebugOutput> debugIterator = new MockBatchedDocumentsIterator<>(0, cutoffEpochMs, debugBatches);
        when(m_JobProvider.newBatchedModelDebugOutputIterator(JOB_WITH_RETENTION_ID)).thenReturn(debugIterator);
        when(m_JobProvider.newBatchedModelDebugOutputIterator(JOB_NO_RETENTION_ID)).thenReturn(debugIterator);

        JobDataDeleter deleter = mock(JobDataDeleter.class);
        when(m_DeleterFactory.newDeleter(JOB_WITH_RETENTION_ID)).thenReturn(deleter);
        when(m_DeleterFactory.newDeleter(JOB_NO_RETENTION_ID)).thenReturn(deleter);

        m_OldDataRemover.removeOldData();

        verify(m_DeleterFactory).newDeleter(JOB_WITH_RETENTION_ID);
        verify(m_DeleterFactory).newDeleter(JOB_NO_RETENTION_ID);
        ArgumentCaptor<ModelSizeStats>statsCaptor = ArgumentCaptor.forClass(ModelSizeStats.class);
        verify(deleter, times(2)).deleteModelSizeStats(statsCaptor.capture());
        assertEquals(Arrays.asList(stats1, stats2), statsCaptor.getAllValues());
        verify(deleter, times(2)).commitAndFreeDiskSpace();
        verifyDeletedResultsWereAuditted(JOB_WITH_RETENTION_ID, cutoffEpochMs);

        Mockito.verifyNoMoreInteractions(m_DeleterFactory, m_Auditor, deleter);
    }

    private void verifyDeletedResultsWereAuditted(String jobId, long epochMs)
    {
        Instant instant = Instant.ofEpochSecond(epochMs / 1000);
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneOffset.systemDefault());
        String formatted = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(zonedDateTime);
        verify(m_JobProvider).audit(jobId);
        verify(m_Auditor).info("Deleted results prior to " + formatted);
    }
}
