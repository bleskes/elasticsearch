/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.prelert.job.scheduler;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.DataCounts;
import org.elasticsearch.xpack.prelert.job.Job;
import org.elasticsearch.xpack.prelert.job.JobSchedulerStatus;
import org.elasticsearch.xpack.prelert.job.JobStatus;
import org.elasticsearch.xpack.prelert.job.SchedulerState;
import org.elasticsearch.xpack.prelert.job.audit.Auditor;
import org.elasticsearch.xpack.prelert.job.data.DataProcessor;
import org.elasticsearch.xpack.prelert.job.extraction.DataExtractor;
import org.elasticsearch.xpack.prelert.job.logging.JobLoggerFactory;
import org.elasticsearch.xpack.prelert.job.metadata.Allocation;
import org.elasticsearch.xpack.prelert.job.persistence.BucketsQueryBuilder;
import org.elasticsearch.xpack.prelert.job.persistence.JobProvider;
import org.elasticsearch.xpack.prelert.job.persistence.QueryPage;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.DataLoadParams;
import org.elasticsearch.xpack.prelert.job.process.autodetect.params.InterimResultsParams;
import org.elasticsearch.xpack.prelert.job.results.Bucket;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;
import org.junit.Before;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.elasticsearch.xpack.prelert.job.JobTests.buildJobBuilder;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JobSchedulerTests extends ESTestCase {

    private static final String JOB_ID = "foo";
    private static final Duration BUCKET_SPAN = Duration.ofSeconds(2);

    private JobProvider jobProvider;
    private JobLoggerFactory jobLoggerFactory;
    private Logger jobLogger;
    private Auditor auditor;
    private JobScheduler.Listener statusListener;

    private volatile JobSchedulerStatus currentStatus;

    private Duration frequency;
    private Duration queryDelay;
    private JobScheduler jobScheduler;
    private volatile CountDownLatch schedulerStoppedAuditedLatch;

    @Before
    public void setUpTests()
    {
        jobProvider = mock(JobProvider.class);
        jobLoggerFactory = mock(JobLoggerFactory.class);
        jobLogger = mock(Logger.class);
        auditor = mock(Auditor.class);

        currentStatus = JobSchedulerStatus.STARTED;
        frequency = Duration.ofSeconds(1);
        queryDelay = Duration.ofSeconds(0);
        when(jobLoggerFactory.newLogger(JOB_ID)).thenReturn(jobLogger);
        schedulerStoppedAuditedLatch = new CountDownLatch(1);
        when(jobProvider.audit(anyString())).thenReturn(auditor);
        recordSchedulerStatus();
        recordSchedulerStoppedAudited();
        givenNoExistingBuckets();
    }

    public void testStart_GivenEndIsEarlierThanStart() {
        Job job = createScheduledJob();
        Allocation allocation = createAllocation(JobSchedulerStatus.STARTED, 1000L, 500L);
        DataExtractor dataExtractor = mock(DataExtractor.class);
        DataProcessor dataProcessor = mock(DataProcessor.class);
        jobScheduler = createJobScheduler(dataExtractor, dataProcessor);

        jobScheduler.start(job, allocation);

        waitUntilSchedulerStoppedIsAudited();

        assertEquals(JobSchedulerStatus.STOPPED, currentStatus);
        verify(dataExtractor).clear();
        verify(auditor).info("Scheduler stopped");
        Mockito.verifyNoMoreInteractions(dataExtractor, dataProcessor, auditor);
    }

    public void testStart_GivenSameStartAndEnd() {
        Job job = createScheduledJob();
        Allocation allocation = createAllocation(JobSchedulerStatus.STARTED, 1000L, 1000L);
        DataExtractor dataExtractor = mock(DataExtractor.class);
        DataProcessor dataProcessor = mock(DataProcessor.class);
        jobScheduler = createJobScheduler(dataExtractor, dataProcessor);

        jobScheduler.start(job, allocation);

        waitUntilSchedulerStoppedIsAudited();

        verify(dataExtractor).clear();
        assertEquals(JobSchedulerStatus.STOPPED, currentStatus);
        verify(auditor).info("Scheduler stopped");
        Mockito.verifyNoMoreInteractions(dataExtractor, dataProcessor, auditor);
    }

    public void testStart_GivenLookbackOnlyAndSingleStream() {
        Job job = createScheduledJob();
        Allocation allocation = createAllocation(JobSchedulerStatus.STARTED, 1400000000000L, 1400000001000L);
        MockDataExtractor dataExtractor = new MockDataExtractor(Arrays.asList(1));
        MockDataProcessor dataProcessor = new MockDataProcessor(Arrays.asList(newCounts(42, 1400000001000L)));
        jobScheduler = createJobScheduler(dataExtractor, dataProcessor);

        jobScheduler.start(job, allocation);
        waitUntilSchedulerStoppedIsAudited();
        assertEquals(JobSchedulerStatus.STOPPED, currentStatus);

        assertEquals(1, dataProcessor.getNumberOfStreams());
        assertEquals("0-0", dataProcessor.getStream(0));
        assertEquals(1400000000000L, dataExtractor.getStart(0));
        assertEquals(1400000001000L, dataExtractor.getEnd(0));
        assertFalse(dataExtractor.isCancelled);
        assertEquals(1, dataExtractor.nCleared);

        List<InterimResultsParams> flushParams = dataProcessor.getFlushParams();
        assertEquals(1, flushParams.size());
        assertTrue(flushParams.get(0).shouldCalculateInterim());
        assertFalse(flushParams.get(0).shouldAdvanceTime());

        verify(jobProvider, times(3)).audit(JOB_ID);
        verify(auditor).info(startsWith("Scheduler started (from:"));
        verify(auditor).info(startsWith("Scheduler lookback completed"));
        verify(auditor).info(startsWith("Scheduler stopped"));
    }

    public void testStart_GivenLookbackOnlyAndMultipleStreams() {
        Job job = createScheduledJob();
        Allocation allocation = createAllocation(JobSchedulerStatus.STARTED, 1400000000000L, 1400000001000L);

        MockDataExtractor dataExtractor = new MockDataExtractor(Arrays.asList(3));
        MockDataProcessor dataProcessor = new MockDataProcessor(Arrays.asList(
                newCounts(67, 1400000000300L),
                newCounts(23, 1400000000600L),
                newCounts(55, 1400000000900L)));
        jobScheduler = createJobScheduler(dataExtractor, dataProcessor);

        jobScheduler.start(job, allocation);
        waitUntilSchedulerStoppedIsAudited();
        assertEquals(JobSchedulerStatus.STOPPED, currentStatus);
        assertFalse(dataExtractor.isCancelled);
        assertEquals(1, dataExtractor.nCleared);

        assertEquals(3, dataProcessor.getNumberOfStreams());
        assertEquals("0-0", dataProcessor.getStream(0));
        assertEquals("0-1", dataProcessor.getStream(1));
        assertEquals("0-2", dataProcessor.getStream(2));
        assertEquals(1400000000000L, dataExtractor.getStart(0));
        assertEquals(1400000001000L, dataExtractor.getEnd(0));

        List<InterimResultsParams> flushParams = dataProcessor.getFlushParams();
        assertEquals(1, flushParams.size());
        assertTrue(flushParams.get(0).shouldCalculateInterim());
        assertFalse(flushParams.get(0).shouldAdvanceTime());
    }

    public void testStart_GivenLookbackOnlyWithSameStartEndTimes() {
        Job job = createScheduledJob();
        Allocation allocation = createAllocation(JobSchedulerStatus.STARTED, 1400000000000L, 1400000000000L);
        MockDataExtractor dataExtractor = new MockDataExtractor(Arrays.asList(1));
        MockDataProcessor dataProcessor = new MockDataProcessor(Arrays.asList(newCounts(42, 1400000001000L)));
        jobScheduler = createJobScheduler(dataExtractor, dataProcessor);

        jobScheduler.start(job, allocation);
        waitUntilSchedulerStoppedIsAudited();
        assertEquals(JobSchedulerStatus.STOPPED, currentStatus);
        assertFalse(dataExtractor.isCancelled);
        assertEquals(1, dataExtractor.nCleared);

        assertEquals(0, dataProcessor.getNumberOfStreams());
        List<InterimResultsParams> flushParams = dataProcessor.getFlushParams();
        assertEquals(0, flushParams.size());
    }

    public void testStart_GivenLookbackAndRealtimeWithSingleStreams() {
        Job job = createScheduledJob();
        Allocation allocation = createAllocation(JobSchedulerStatus.STARTED, 1400000000000L, null);
        long lookbackLatestRecordTime = System.currentTimeMillis() - 100;
        long[] latestRecordTimes = { lookbackLatestRecordTime, lookbackLatestRecordTime + 1000, lookbackLatestRecordTime + 2000 };

        int numberOfSearches = 3;
        MockDataExtractor dataExtractor = new MockDataExtractor(Arrays.asList(1, 1, 1));
        MockDataProcessor dataProcessor = new MockDataProcessor(Arrays.asList(
                newCounts(67, latestRecordTimes[0]),
                newCounts(23, latestRecordTimes[1]),
                newCounts(55, latestRecordTimes[2])),
                new CountDownLatch(numberOfSearches));
        jobScheduler = createJobScheduler(dataExtractor, dataProcessor);

        long schedulerStartedTimeMs = System.currentTimeMillis();

        jobScheduler.start(job, allocation);
        assertEquals(JobSchedulerStatus.STARTED, currentStatus);
        assertTrue(dataProcessor.awaitForCountDownLatch());
        jobScheduler.stopManual();
        assertEquals(JobSchedulerStatus.STOPPED, currentStatus);
        assertTrue(dataExtractor.isCancelled);
        assertEquals(1, dataExtractor.nCleared);

        assertEquals(numberOfSearches, dataProcessor.getNumberOfStreams());
        List<InterimResultsParams> flushParams = dataProcessor.getFlushParams();
        assertEquals(numberOfSearches, flushParams.size());

        long lookbackEnd = dataExtractor.getEnd(0);
        for (int i = 0; i < numberOfSearches; i++)
        {
            assertEquals(i + "-0", dataProcessor.getStream(i));
            assertTrue(flushParams.get(i).shouldCalculateInterim());
            long searchStart = dataExtractor.getStart(i);
            long searchEnd = dataExtractor.getEnd(i);

            // Assert lookback
            if (i == 0)
            {
                assertEquals(1400000000000L, dataExtractor.getStart(i));
                assertTrue(lookbackEnd >= schedulerStartedTimeMs);
                assertTrue(lookbackEnd <= schedulerStartedTimeMs + 1000);
                assertFalse(flushParams.get(i).shouldAdvanceTime());
                continue;
            }

            assertTrue(searchStart > dataExtractor.getStart(i - 1));
            assertTrue(searchStart < System.currentTimeMillis());
            assertTrue(searchEnd > dataExtractor.getEnd(i - 1));
            assertTrue(searchEnd < System.currentTimeMillis());

            assertTrue(flushParams.get(i).shouldAdvanceTime());
        }

        verify(jobProvider, times(4)).audit(JOB_ID);
        verify(auditor).info(startsWith("Scheduler started (from:"));
        verify(auditor).info(startsWith("Scheduler lookback completed"));
        verify(auditor).info(startsWith("Scheduler continued in real-time"));
        verify(auditor).info(startsWith("Scheduler stopped"));
    }

    public void testStart_GivenLookbackAndRealtimeWithEmptyData() {
        Job job = createScheduledJob();
        Allocation allocation = createAllocation(JobSchedulerStatus.STARTED, 1400000000000L, null);
        long lookbackLatestRecordTime = new Date().getTime() - 100;
        int numberOfSearches = 2;
        MockDataExtractor dataExtractor = new MockDataExtractor(Arrays.asList(1, 1));
        MockDataProcessor dataProcessor = new MockDataProcessor(Arrays.asList(
                newCounts(67, lookbackLatestRecordTime),
                newCounts(0, null)),
                new CountDownLatch(numberOfSearches));
        jobScheduler = createJobScheduler(dataExtractor, dataProcessor);

        jobScheduler.start(job, allocation);
        assertEquals(JobSchedulerStatus.STARTED, currentStatus);
        assertTrue(dataProcessor.awaitForCountDownLatch());
        jobScheduler.stopManual();
        assertEquals(JobSchedulerStatus.STOPPED, currentStatus);
        assertTrue(dataExtractor.isCancelled);
        assertEquals(1, dataExtractor.nCleared);

        assertEquals(numberOfSearches, dataProcessor.getNumberOfStreams());
        List<InterimResultsParams> flushParams = dataProcessor.getFlushParams();
        assertEquals(2, flushParams.size());

        assertTrue(flushParams.get(0).shouldCalculateInterim());
        assertFalse(flushParams.get(0).shouldAdvanceTime());
        assertTrue(flushParams.get(1).shouldCalculateInterim());
        assertTrue(flushParams.get(1).shouldAdvanceTime());
    }

    public void testStart_GivenLookbackWithEmptyDataAndRealtimeWithEmptyData() {
        Job job = createScheduledJob();
        Allocation allocation = createAllocation(JobSchedulerStatus.STARTED, 1400000000000L, null);

        // Minimise the time the test takes by setting frequency to 1.
        // In addition to the minimum query delay of 100ms this means an effective
        // frequency of 101ms.
        frequency = Duration.ofMillis(1);

        // 1 for lookback and 9 empty real-time searches in order to get the no data warning
        int numberOfSearches = 10;
        List<Integer> batchesPerSearch = new ArrayList<>();
        List<DataCounts> responseDataCounts = new ArrayList<>();
        for (int i = 0; i < numberOfSearches; i++)
        {
            batchesPerSearch.add(1);
            responseDataCounts.add(newCounts(0, null));
        }
        MockDataExtractor dataExtractor = new MockDataExtractor(batchesPerSearch);
        MockDataProcessor dataProcessor = new MockDataProcessor(responseDataCounts,
                new CountDownLatch(numberOfSearches));
        jobScheduler = createJobScheduler(dataExtractor, dataProcessor);

        jobScheduler.start(job, allocation);
        assertEquals(JobSchedulerStatus.STARTED, currentStatus);
        assertTrue(dataProcessor.awaitForCountDownLatch());
        jobScheduler.stopManual();
        assertEquals(JobSchedulerStatus.STOPPED, currentStatus);
        assertTrue(dataExtractor.isCancelled);
        assertEquals(1, dataExtractor.nCleared);

        assertEquals(numberOfSearches, dataProcessor.getNumberOfStreams());
        assertEquals(1400000000000L, dataExtractor.getStart(0));
        assertTrue(dataExtractor.getStart(1) < System.currentTimeMillis());
        assertEquals(10, dataProcessor.getFlushParams().size());
        assertFalse(dataProcessor.getFlushParams().get(0).shouldAdvanceTime());
        for (int i = 1; i < dataProcessor.getFlushParams().size(); i++)
        {
            assertTrue(dataProcessor.getFlushParams().get(i).shouldAdvanceTime());
        }

        assertTrue(dataProcessor.isJobClosed());
        verify(auditor).warning("Scheduler has been retrieving no data for a while");
    }

    public void testStart_GivenDataProcessorThrows() {
        Job job = createScheduledJob();
        Allocation allocation = createAllocation(JobSchedulerStatus.STARTED, 1400000000000L, 1400000001000L);

        MockDataExtractor dataExtractor = new MockDataExtractor(Arrays.asList(1, 1, 1, 1, 1, 1, 1, 1));
        MockDataProcessor dataProcessor = new MockDataProcessor(Arrays.asList(
                newCounts(67, 1400000000300L),
                newCounts(67, 1400000000300L),
                newCounts(67, 1400000000300L),
                newCounts(67, 1400000000300L),
                newCounts(67, 1400000000300L),
                newCounts(67, 1400000000300L),
                newCounts(67, 1400000000300L),
                newCounts(23, 1400000000600L)));
        dataProcessor.setShouldThrow(true);
        jobScheduler = createJobScheduler(dataExtractor, dataProcessor);

        jobScheduler.start(job, allocation);
        waitUntilSchedulerStoppedIsAudited();
        assertEquals(JobSchedulerStatus.STOPPED, currentStatus);
        assertEquals(1, dataExtractor.nCleared);

        assertEquals(0, dataProcessor.getNumberOfStreams());
        assertEquals(1400000000000L, dataExtractor.getStart(0));
        assertEquals(1400000001000L, dataExtractor.getEnd(0));
        assertEquals(1, dataProcessor.getFlushParams().size());

        // Repeat to test that scheduler did not advance time
        allocation = createAllocation(JobSchedulerStatus.STARTED, 1400000000000L, 1400000001000L);
        jobScheduler.start(job, allocation);
        waitUntilSchedulerStoppedIsAudited();

        assertEquals(0, dataProcessor.getNumberOfStreams());
        assertEquals(1400000000000L, dataExtractor.getStart(0));
        assertEquals(1400000001000L, dataExtractor.getEnd(0));
    }

    public void testStart_GivenDataExtractorThrows() throws IOException {
        Job job = createScheduledJob();
        Allocation allocation = createAllocation(JobSchedulerStatus.STARTED, 1400000000000L, 1400000001000L);
        DataExtractor dataExtractor = mock(DataExtractor.class);
        when(dataExtractor.hasNext()).thenReturn(true);
        when(dataExtractor.next()).thenThrow(new IOException());

        MockDataProcessor dataProcessor = new MockDataProcessor(Collections.emptyList());
        jobScheduler = createJobScheduler(dataExtractor, dataProcessor);

        jobScheduler.start(job, allocation);
        waitUntilSchedulerStoppedIsAudited();
        assertEquals(JobSchedulerStatus.STOPPED, currentStatus);
        verify(dataExtractor).clear();

        assertEquals(0, dataProcessor.getNumberOfStreams());
        assertEquals(1, dataProcessor.getFlushParams().size());
    }

    public void testStart_GivenLatestRecordTimestampIsAfterSchedulerStartTime() {
        Job.Builder job = buildJobBuilder(JOB_ID);
        Allocation allocation = createAllocation(JobSchedulerStatus.STARTED, 1450000000000L, 1460000000000L);
        DataCounts dataCounts = new DataCounts();
        dataCounts.setLatestRecordTimeStamp(new Date(1455000000000L));
        job.setCounts(dataCounts);

        MockDataExtractor dataExtractor = new MockDataExtractor(Arrays.asList(1));
        MockDataProcessor dataProcessor = new MockDataProcessor(Arrays.asList(newCounts(67, 1450000000000L)));
        jobScheduler = createJobScheduler(dataExtractor, dataProcessor);

        jobScheduler.start(job.build(), allocation);
        waitUntilSchedulerStoppedIsAudited();
        assertEquals(JobSchedulerStatus.STOPPED, currentStatus);
        assertEquals(1, dataExtractor.nCleared);

        assertEquals(1, dataProcessor.getNumberOfStreams());
        assertEquals("0-0", dataProcessor.getStream(0));
        assertEquals(1455000000001L, dataExtractor.getStart(0));
        assertEquals(1460000000000L, dataExtractor.getEnd(0));
        List<InterimResultsParams> flushParams = dataProcessor.getFlushParams();
        assertEquals(1, flushParams.size());
        assertTrue(flushParams.get(0).shouldCalculateInterim());
        assertFalse(flushParams.get(0).shouldAdvanceTime());
    }

    public void testStart_GivenLastFinalBucketEndAfterLatestRecordTimestampAndAfterSchedulerStartTime() {
        Job.Builder job = buildJobBuilder(JOB_ID);
        Allocation allocation = createAllocation(JobSchedulerStatus.STARTED, 1450000000000L, 1460000000000L);
        DataCounts dataCounts = new DataCounts();
        dataCounts.setLatestRecordTimeStamp(new Date(1455000000000L));
        job.setCounts(dataCounts);
        givenLatestFinalBucketTime(1455000000000L);

        MockDataExtractor dataExtractor = new MockDataExtractor(Arrays.asList(1));
        MockDataProcessor dataProcessor = new MockDataProcessor(Arrays.asList(newCounts(67, 1455000000000L)));
        jobScheduler = createJobScheduler(dataExtractor, dataProcessor);

        jobScheduler.start(job.build(), allocation);
        waitUntilSchedulerStoppedIsAudited();
        assertEquals(JobSchedulerStatus.STOPPED, currentStatus);
        assertEquals(1, dataExtractor.nCleared);

        assertEquals(1, dataProcessor.getNumberOfStreams());
        assertEquals("0-0", dataProcessor.getStream(0));
        assertEquals(1455000002000L, dataExtractor.getStart(0));
        assertEquals(1460000000000L, dataExtractor.getEnd(0));
        List<InterimResultsParams> flushParams = dataProcessor.getFlushParams();
        assertEquals(1, flushParams.size());
        assertTrue(flushParams.get(0).shouldCalculateInterim());
        assertFalse(flushParams.get(0).shouldAdvanceTime());
    }

    public void testStart_GivenLatestRecordTimestampIsBeforeSchedulerStartTime() {
        Job.Builder job = buildJobBuilder(JOB_ID);
        Allocation allocation = createAllocation(JobSchedulerStatus.STARTED, 1455000000000L, 1460000000000L);
        DataCounts dataCounts = new DataCounts();
        dataCounts.setLatestRecordTimeStamp(new Date(1450000000000L));
        job.setCounts(dataCounts);

        MockDataExtractor dataExtractor = new MockDataExtractor(Arrays.asList(1));
        MockDataProcessor dataProcessor = new MockDataProcessor(Arrays.asList(newCounts(67, 1455000000000L)));
        jobScheduler = createJobScheduler(dataExtractor, dataProcessor);

        jobScheduler.start(job.build(), allocation);
        waitUntilSchedulerStoppedIsAudited();
        assertEquals(JobSchedulerStatus.STOPPED, currentStatus);
        assertEquals(1, dataExtractor.nCleared);

        assertEquals(1, dataProcessor.getNumberOfStreams());
        assertEquals("0-0", dataProcessor.getStream(0));
        assertEquals(1455000000000L, dataExtractor.getStart(0));
        assertEquals(1460000000000L, dataExtractor.getEnd(0));
        List<InterimResultsParams> flushParams = dataProcessor.getFlushParams();
        assertEquals(1, flushParams.size());
        assertTrue(flushParams.get(0).shouldCalculateInterim());
        assertFalse(flushParams.get(0).shouldAdvanceTime());
    }

    public void testStart_GivenAlreadyStarted() {
        Job job = createScheduledJob();
        Allocation allocation = createAllocation(JobSchedulerStatus.STARTED, 1400000000000L, null);
        DataExtractor dataExtractor = mock(DataExtractor.class);
        DataProcessor dataProcessor = mock(DataProcessor.class);
        jobScheduler = createJobScheduler(dataExtractor, dataProcessor);
        jobScheduler.start(job, allocation);

        expectThrows(IllegalStateException.class, () -> jobScheduler.start(job, allocation));

        jobScheduler.stopManual();
    }

    public void testStopAuto_GivenLookbackOnlyJob() throws InterruptedException {
        CountDownLatch lookbackStartedLatch = new CountDownLatch(1);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                lookbackStartedLatch.countDown();
                return null;
            }
        }).when(jobLogger).info("Scheduler started");

        Job job = createScheduledJob();
        Allocation allocation = createAllocation(JobSchedulerStatus.STARTED, 1400000000000L, 1500000000000L);
        MockDataExtractor dataExtractor = new MockDataExtractor(Arrays.asList(1));
        MockDataProcessor dataProcessor = new MockDataProcessor(Arrays.asList(newCounts(67, 1500000000000L)));
        jobScheduler = createJobScheduler(dataExtractor, dataProcessor);
        jobScheduler.start(job, allocation);

        lookbackStartedLatch.await();

        currentStatus = JobSchedulerStatus.STOPPING;

        jobScheduler.stopAuto();

        assertEquals(JobSchedulerStatus.STARTED, currentStatus);
    }

    public void testStopAuto_GivenRealTimeJob() {
        Job job = createScheduledJob();
        Allocation allocation = createAllocation(JobSchedulerStatus.STARTED, 1400000000000L, null);
        MockDataExtractor dataExtractor = new MockDataExtractor(Arrays.asList(1, 1, 1));
        MockDataProcessor dataProcessor = new MockDataProcessor(Arrays.asList(
                newCounts(67, 1400000000300L),
                newCounts(23, 1400000000600L),
                newCounts(55, 1400000000900L)),
                new CountDownLatch(2));
        jobScheduler = createJobScheduler(dataExtractor, dataProcessor);

        jobScheduler.start(job, allocation);
        assertEquals(JobSchedulerStatus.STARTED, currentStatus);

        // Wait enough time for real-time tasks to begin in case stop did not work
        assertTrue(dataProcessor.awaitForCountDownLatch());

        jobScheduler.stopAuto();

        assertEquals(JobSchedulerStatus.STARTED, currentStatus);
    }

    private Job createScheduledJob() {
        return buildJobBuilder(JOB_ID).build();
    }

    private Allocation createAllocation(JobSchedulerStatus status, long startTimeMillis, Long endTimeMillis) {
        currentStatus = status;
        return new Allocation(JOB_ID, "nodeId", JobStatus.RUNNING, new SchedulerState(status, startTimeMillis, endTimeMillis));
    }

    private void recordSchedulerStatus() {
        statusListener = new JobScheduler.Listener() {
            @Override
            public void statusChanged(JobSchedulerStatus newStatus) {
                currentStatus = newStatus;
            }
        };
    }

    private void recordSchedulerStoppedAudited() {
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                schedulerStoppedAuditedLatch.countDown();
                return null;
            }
        }).when(auditor).info("Scheduler stopped");
    }

    private void givenNoExistingBuckets() {
        givenLatestFinalBucketTime(null);
    }

    private void givenLatestFinalBucketTime(Long latestBucketTimeMs) {
        BucketsQueryBuilder.BucketsQuery latestBucketQuery = new BucketsQueryBuilder().sortField(Bucket.TIMESTAMP.getPreferredName())
                .sortDescending(true).take(1)
                .includeInterim(false).build();
        QueryPage<Bucket> buckets = null;
        if (latestBucketTimeMs == null) {
            buckets = new QueryPage<>(Collections.emptyList(), 0);
        } else {
            Bucket bucket = new Bucket();
            bucket.setTimestamp(new Date(latestBucketTimeMs));
            buckets = new QueryPage<>(Arrays.asList(bucket), 1);
        }
        when(jobProvider.buckets(JOB_ID, latestBucketQuery)).thenReturn(buckets);
    }

    private void waitUntilSchedulerStoppedIsAudited() {
        try {
            schedulerStoppedAuditedLatch.await();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        schedulerStoppedAuditedLatch = new CountDownLatch(1);
    }

    private JobScheduler createJobScheduler(DataExtractor dataExtractor, DataProcessor dataProcessor) {
        return new JobScheduler(JOB_ID, BUCKET_SPAN, frequency, queryDelay, dataExtractor, dataProcessor, jobProvider,
                jobLoggerFactory, () -> currentStatus, statusListener);
    }

    private static DataCounts newCounts(int recordCount, Long latestRecordTime) {
        DataCounts counts = new DataCounts();
        counts.setProcessedRecordCount(recordCount);
        counts.setLatestRecordTimeStamp(latestRecordTime == null ? null : new Date(latestRecordTime));
        return counts;
    }

    private static class MockDataExtractor implements DataExtractor {
        private final List<Integer> batchesPerSearch;
        private final List<Long> starts = new ArrayList<>();
        private final List<Long> ends = new ArrayList<>();
        private int searchCount = -1;
        private int streamCount = -1;
        private int nCleared;
        private boolean isCancelled;

        public MockDataExtractor(List<Integer> batchesPerSearch) {
            this.batchesPerSearch = batchesPerSearch;
            nCleared = 0;
        }

        @Override
        public synchronized boolean hasNext() {
            return streamCount < batchesPerSearch.get(searchCount) - 1;
        }

        @Override
        public synchronized Optional<InputStream> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            streamCount++;
            String stream = "" + searchCount + "-" + streamCount;
            return Optional.of(new ByteArrayInputStream(stream.getBytes(StandardCharsets.UTF_8)));
        }

        @Override
        public synchronized void newSearch(long start, long end, Logger logger) {
            if (searchCount == batchesPerSearch.size() - 1) {
                throw new IllegalStateException();
            }
            searchCount++;
            streamCount = -1;
            starts.add(start);
            ends.add(end);
            isCancelled = false;
        }

        public synchronized long getStart(int searchCount) {
            return starts.get(searchCount);
        }

        public synchronized long getEnd(int searchCount) {
            return ends.get(searchCount);
        }

        @Override
        public synchronized void clear() {
            nCleared++;
        }

        @Override
        public synchronized void cancel() {
            isCancelled = true;
        }
    }

    private static class MockDataProcessor implements DataProcessor {

        private final List<DataCounts> countsPerStream;
        private final List<String> streams = new ArrayList<>();
        private final List<InterimResultsParams> flushParams = new ArrayList<>();
        private boolean shouldThrow = false;
        private boolean isJobClosed = false;
        private int streamCount = 0;
        private final CountDownLatch countDownLatch;

        MockDataProcessor(List<DataCounts> countsPerStream) {
            this(countsPerStream, new CountDownLatch(0));
        }

        MockDataProcessor(List<DataCounts> countsPerStream, CountDownLatch countDownLatch) {
            this.countsPerStream = countsPerStream;
            this.countDownLatch = countDownLatch;
        }

        /**
         * Wait until there are as many streams processed as the count down
         * latch provided upon construction. This method has a timeout of 10 seconds.
         * No test should need more than 10 seconds.
         *
         * @return {@code true} if latch was counted down to 0 or {@code false} if it timed out
         */
        private boolean awaitForCountDownLatch() {
            try {
                countDownLatch.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                return false;
            }
            return true;
        }

        private synchronized void setShouldThrow(boolean value) {
            shouldThrow = value;
        }

        private synchronized String getStream(int index) {
            return streams.get(index);
        }

        private synchronized int getNumberOfStreams() {
            return streams.size();
        }

        private synchronized List<InterimResultsParams> getFlushParams() {
            return flushParams;
        }

        private synchronized boolean isJobClosed() {
            return isJobClosed;
        }

        @Override
        public synchronized DataCounts processData(String jobId, InputStream input, DataLoadParams params) {
            assertEquals(JOB_ID, jobId);
            assertFalse(params.isResettingBuckets());
            countDownLatch.countDown();
            if (shouldThrow) {
                throw ExceptionsHelper.missingJobException(jobId);
            }
            try {
                streams.add(streamToString(input));
            } catch (IOException e) {
                throw new IllegalStateException();
            }
            return countsPerStream.get(streamCount++);
        }

        @Override
        public synchronized void flushJob(String jobId, InterimResultsParams params) {
            if (jobId.equals(JOB_ID)) {
                flushParams.add(params);
            }
        }

        @Override
        public synchronized void closeJob(String jobId)  {
            isJobClosed = jobId.equals(JOB_ID);
        }
    }

    private static String streamToString(InputStream stream) throws IOException {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return buffer.lines().collect(Collectors.joining("\n")).trim();
        }
    }
}
