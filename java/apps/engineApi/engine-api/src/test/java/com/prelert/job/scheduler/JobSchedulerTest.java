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

package com.prelert.job.scheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.fasterxml.jackson.core.JsonParseException;
import com.prelert.job.DataCounts;
import com.prelert.job.JobConfiguration;
import com.prelert.job.JobDetails;
import com.prelert.job.JobException;
import com.prelert.job.JobSchedulerStatus;
import com.prelert.job.UnknownJobException;
import com.prelert.job.audit.Auditor;
import com.prelert.job.data.extraction.DataExtractor;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.exceptions.TooManyJobsException;
import com.prelert.job.logging.JobLoggerFactory;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.process.exceptions.MalformedJsonException;
import com.prelert.job.process.exceptions.MissingFieldException;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.process.params.DataLoadParams;
import com.prelert.job.process.params.InterimResultsParams;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;

public class JobSchedulerTest
{
    private static final String JOB_ID = "foo";
    private static final Duration BUCKET_SPAN = Duration.ofSeconds(2);
    private static final Duration FREQUENCY = Duration.ofSeconds(1);
    private static final Duration QUERY_DELAY = Duration.ofSeconds(0);

    /**
     * Query delay milliseconds when query delay is configured to 0.
     */
    private static final long EFFECTIVE_QUERY_DELAY_MS = QUERY_DELAY.toMillis() + 100;

    @Rule public ExpectedException m_ExpectedException = ExpectedException.none();

    @Mock private JobProvider m_JobProvider;
    @Mock private JobLoggerFactory m_JobLoggerFactory;
    @Mock private Logger m_JobLogger;
    @Mock private Auditor m_Auditor;

    private volatile JobSchedulerStatus m_CurrentStatus;

    private JobScheduler m_JobScheduler;
    private volatile CountDownLatch m_SchedulerStoppedAuditedLatch;

    @Before
    public void setUp() throws UnknownJobException
    {
        MockitoAnnotations.initMocks(this);
        m_CurrentStatus = null;
        when(m_JobLoggerFactory.newLogger(JOB_ID)).thenReturn(m_JobLogger);
        m_SchedulerStoppedAuditedLatch = new CountDownLatch(1);
        when(m_JobProvider.audit(anyString())).thenReturn(m_Auditor);
        recordSchedulerStatus();
        recordSchedulerStoppedAudited();
    }

    @Test
    public void testStart_GivenEndIsEarlierThanStart() throws JobException
    {
        DataExtractor dataExtractor = mock(DataExtractor.class);
        DataProcessor dataProcessor = mock(DataProcessor.class);
        m_JobScheduler = createJobScheduler(dataExtractor, dataProcessor);

        m_JobScheduler.start(new JobDetails("foo", new JobConfiguration()), 1000L,
                OptionalLong.of(500));

        waitUntilSchedulerStoppedIsAudited();
        verify(dataProcessor).closeJob("foo");
        verify(m_Auditor).info("Scheduler stopped");
        Mockito.verifyNoMoreInteractions(dataExtractor, dataProcessor, m_Auditor);
    }

    @Test
    public void testStart_GivenSameStartAndEnd() throws JobException
    {
        DataExtractor dataExtractor = mock(DataExtractor.class);
        DataProcessor dataProcessor = mock(DataProcessor.class);
        m_JobScheduler = createJobScheduler(dataExtractor, dataProcessor);

        m_JobScheduler.start(new JobDetails("foo", new JobConfiguration()), 1000L,
                OptionalLong.of(1000));

        waitUntilSchedulerStoppedIsAudited();
        verify(dataProcessor).closeJob("foo");
        verify(m_Auditor).info("Scheduler stopped");
        Mockito.verifyNoMoreInteractions(dataExtractor, dataProcessor, m_Auditor);
    }

    @Test
    public void testStart_GivenLookbackOnlyAndSingleStream()
            throws CannotStartSchedulerException, CannotStopSchedulerException
    {
        MockDataExtractor dataExtractor = new MockDataExtractor(Arrays.asList(1));
        MockDataProcessor dataProcessor = new MockDataProcessor(Arrays.asList(
                newCounts(42, 1400000001000L)));
        m_JobScheduler = createJobScheduler(dataExtractor, dataProcessor);

        m_JobScheduler.start(new JobDetails(), 1400000000000L, OptionalLong.of(1400000001000L));
        waitUntilSchedulerStoppedIsAudited();
        assertEquals(JobSchedulerStatus.STOPPED, m_CurrentStatus);
        assertTrue(dataProcessor.isJobClosed());

        assertEquals(1, dataProcessor.getNumberOfStreams());
        assertEquals("0-0", dataProcessor.getStream(0));
        assertEquals("1400000000000", dataExtractor.getStart(0));
        assertEquals("1400000001000", dataExtractor.getEnd(0));

        List<InterimResultsParams> flushParams = dataProcessor.getFlushParams();
        assertEquals(1, flushParams.size());
        assertTrue(flushParams.get(0).shouldCalculateInterim());
        assertFalse(flushParams.get(0).shouldAdvanceTime());

        verify(m_JobProvider, times(3)).audit(JOB_ID);
        verify(m_Auditor).info(startsWith("Scheduler started (from:"));
        verify(m_Auditor).info(startsWith("Scheduler lookback completed"));
        verify(m_Auditor).info(startsWith("Scheduler stopped"));
    }

    @Test
    public void testStart_GivenLookbackOnlyAndMultipleStreams()
            throws CannotStartSchedulerException, CannotStopSchedulerException
    {
        MockDataExtractor dataExtractor = new MockDataExtractor(Arrays.asList(3));
        MockDataProcessor dataProcessor = new MockDataProcessor(Arrays.asList(
                newCounts(67, 1400000000300L),
                newCounts(23, 1400000000600L),
                newCounts(55, 1400000000900L)));
        m_JobScheduler = createJobScheduler(dataExtractor, dataProcessor);

        m_JobScheduler.start(new JobDetails(), 1400000000000L, OptionalLong.of(1400000001000L));
        waitUntilSchedulerStoppedIsAudited();
        assertEquals(JobSchedulerStatus.STOPPED, m_CurrentStatus);
        assertTrue(dataProcessor.isJobClosed());

        assertEquals(3, dataProcessor.getNumberOfStreams());
        assertEquals("0-0", dataProcessor.getStream(0));
        assertEquals("0-1", dataProcessor.getStream(1));
        assertEquals("0-2", dataProcessor.getStream(2));
        assertEquals("1400000000000", dataExtractor.getStart(0));
        assertEquals("1400000001000", dataExtractor.getEnd(0));

        List<InterimResultsParams> flushParams = dataProcessor.getFlushParams();
        assertEquals(1, flushParams.size());
        assertTrue(flushParams.get(0).shouldCalculateInterim());
        assertFalse(flushParams.get(0).shouldAdvanceTime());
    }

    @Test
    public void testStart_GivenLookbackOnlyWithSameStartEndTimes()
            throws CannotStartSchedulerException, CannotStopSchedulerException
    {
        MockDataExtractor dataExtractor = new MockDataExtractor(Arrays.asList(1));
        MockDataProcessor dataProcessor = new MockDataProcessor(Arrays.asList(
                newCounts(42, 1400000001000L)));
        m_JobScheduler = createJobScheduler(dataExtractor, dataProcessor);

        m_JobScheduler.start(new JobDetails(), 1400000000000L, OptionalLong.of(1400000000000L));
        waitUntilSchedulerStoppedIsAudited();
        assertEquals(JobSchedulerStatus.STOPPED, m_CurrentStatus);
        assertTrue(dataProcessor.isJobClosed());

        assertEquals(0, dataProcessor.getNumberOfStreams());
        List<InterimResultsParams> flushParams = dataProcessor.getFlushParams();
        assertEquals(0, flushParams.size());
    }

    @Test
    public void testStart_GivenLookbackAndRealtimeWithSingleStreams()
            throws CannotStartSchedulerException, CannotStopSchedulerException
    {
        long lookbackLatestRecordTime = new Date().getTime() - EFFECTIVE_QUERY_DELAY_MS;
        long realtimeLatestRecord_1 = lookbackLatestRecordTime + 100;
        long realtimeLatestRecord_2 = lookbackLatestRecordTime + 1000;

        MockDataExtractor dataExtractor = new MockDataExtractor(Arrays.asList(1, 1, 1));
        MockDataProcessor dataProcessor = new MockDataProcessor(Arrays.asList(
                newCounts(67, lookbackLatestRecordTime),
                newCounts(23, realtimeLatestRecord_1),
                newCounts(55, realtimeLatestRecord_2)));
        m_JobScheduler = createJobScheduler(dataExtractor, dataProcessor);

        long nowMs = new Date().getTime() - EFFECTIVE_QUERY_DELAY_MS;
        long intervalMs = FREQUENCY.toMillis();
        long intervalEnd = (nowMs / intervalMs) * intervalMs;

        m_JobScheduler.start(new JobDetails(), 1400000000000L, OptionalLong.empty());
        assertEquals(JobSchedulerStatus.STARTED, m_CurrentStatus);

        // Give time to scheduler to perform at least one real-time search
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        m_JobScheduler.stopManual();
        assertEquals(JobSchedulerStatus.STOPPED, m_CurrentStatus);

        assertTrue(dataProcessor.getNumberOfStreams() > 1);
        assertTrue(dataProcessor.getNumberOfStreams() <= 3);
        List<InterimResultsParams> flushParams = dataProcessor.getFlushParams();
        assertTrue(flushParams.size() > 1);

        // To check the lookback end time we should be lenient as
        // it is possible that between the moment we recorded now
        // and the moment the lookback actually got executed,
        // the current interval end could have changed.
        assertEquals("0-0", dataProcessor.getStream(0));
        assertEquals("1400000000000", dataExtractor.getStart(0));
        long lookbackEnd = Long.parseLong(dataExtractor.getEnd(0));
        assertTrue(lookbackEnd >= intervalEnd);
        assertTrue(lookbackEnd <= intervalEnd + intervalMs);
        intervalEnd = ((lookbackEnd + intervalMs) / intervalMs) * intervalMs;
        assertTrue(flushParams.get(0).shouldCalculateInterim());
        assertFalse(flushParams.get(0).shouldAdvanceTime());

        // The same is true for the first real-time search.
        // It is possible that by the time the first real-time
        // gets executed, we have skipped intervals and therefore
        // we are processing two intervals together.
        assertEquals("1-0", dataProcessor.getStream(1));
        assertEquals(String.valueOf(lookbackLatestRecordTime + 1), dataExtractor.getStart(1));
        long firstRtEnd = Long.parseLong(dataExtractor.getEnd(1));
        assertTrue(firstRtEnd >= intervalEnd);
        assertTrue(firstRtEnd <= intervalEnd + intervalMs);
        assertTrue(flushParams.get(1).shouldCalculateInterim());
        assertTrue(flushParams.get(1).shouldAdvanceTime());
        assertEquals(Math.min(calcAlignedBucketEnd(realtimeLatestRecord_1), firstRtEnd),
                flushParams.get(1).getAdvanceTime() * 1000);
        intervalEnd = firstRtEnd + intervalMs;

        // The rest of real-time searches (if any) should span over exactly one interval
        if (dataProcessor.getNumberOfStreams() > 2)
        {
            assertEquals("2-0", dataProcessor.getStream(2));
            assertEquals(String.valueOf(intervalEnd - intervalMs), dataExtractor.getStart(2));
            assertEquals(String.valueOf(intervalEnd), dataExtractor.getEnd(2));
            assertTrue(flushParams.get(2).shouldCalculateInterim());
            assertTrue(flushParams.get(2).shouldAdvanceTime());
            assertEquals(Math.min(calcAlignedBucketEnd(realtimeLatestRecord_2),
                    Long.parseLong(dataExtractor.getEnd(2))),
                    flushParams.get(2).getAdvanceTime() * 1000);
            intervalEnd += intervalMs;
        }

        verify(m_JobProvider, times(4)).audit(JOB_ID);
        verify(m_Auditor).info(startsWith("Scheduler started (from:"));
        verify(m_Auditor).info(startsWith("Scheduler lookback completed"));
        verify(m_Auditor).info(startsWith("Scheduler continued in real-time"));
        verify(m_Auditor).info(startsWith("Scheduler stopped"));
    }

    @Test
    public void testStart_GivenLookbackAndRealtimeWithEmptyData()
            throws CannotStartSchedulerException, CannotStopSchedulerException
    {
        long lookbackLatestRecordTime = new Date().getTime() - EFFECTIVE_QUERY_DELAY_MS;

        MockDataExtractor dataExtractor = new MockDataExtractor(Arrays.asList(1, 1));
        MockDataProcessor dataProcessor = new MockDataProcessor(Arrays.asList(
                newCounts(67, lookbackLatestRecordTime),
                newCounts(0, null)));
        m_JobScheduler = createJobScheduler(dataExtractor, dataProcessor);

        m_JobScheduler.start(new JobDetails(), 1400000000000L, OptionalLong.empty());
        assertEquals(JobSchedulerStatus.STARTED, m_CurrentStatus);

        // Give time to scheduler to perform at least one real-time search
        try
        {
            Thread.sleep(1200);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        m_JobScheduler.stopManual();
        assertEquals(JobSchedulerStatus.STOPPED, m_CurrentStatus);

        assertEquals(2, dataProcessor.getNumberOfStreams());
        List<InterimResultsParams> flushParams = dataProcessor.getFlushParams();
        assertEquals(2, flushParams.size());

        assertTrue(flushParams.get(0).shouldCalculateInterim());
        assertFalse(flushParams.get(0).shouldAdvanceTime());
        assertTrue(flushParams.get(1).shouldCalculateInterim());
        assertTrue(flushParams.get(1).shouldAdvanceTime());
        long expectedAdvanceTime = (lookbackLatestRecordTime + 1) / 1000;
        assertEquals(expectedAdvanceTime, flushParams.get(1).getAdvanceTime());
    }

    @Test
    public void testStart_GivenLookbackWithEmptyDataAndRealtimeWithEmptyData()
            throws CannotStartSchedulerException, CannotStopSchedulerException
    {
        MockDataExtractor dataExtractor = new MockDataExtractor(Arrays.asList(1, 1));
        MockDataProcessor dataProcessor = new MockDataProcessor(Arrays.asList(
                newCounts(0, null),
                newCounts(0, null)));
        m_JobScheduler = createJobScheduler(dataExtractor, dataProcessor);

        m_JobScheduler.start(new JobDetails(), 1400000000000L, OptionalLong.empty());
        assertEquals(JobSchedulerStatus.STARTED, m_CurrentStatus);

        // Give time to scheduler to perform at least one real-time search
        try
        {
            Thread.sleep(1200);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        m_JobScheduler.stopManual();
        assertEquals(JobSchedulerStatus.STOPPED, m_CurrentStatus);

        assertEquals(2, dataProcessor.getNumberOfStreams());
        assertEquals("1400000000000", dataExtractor.getStart(0));
        assertEquals("1400000000000", dataExtractor.getStart(1));
        assertTrue(dataProcessor.getFlushParams().isEmpty());
    }

    @Test
    public void testStart_GivenDataProcessorThrows()
            throws CannotStartSchedulerException, CannotStopSchedulerException
    {
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
        m_JobScheduler = createJobScheduler(dataExtractor, dataProcessor);

        m_JobScheduler.start(new JobDetails(), 1400000000000L, OptionalLong.of(1400000001000L));
        waitUntilSchedulerStoppedIsAudited();
        assertEquals(JobSchedulerStatus.STOPPED, m_CurrentStatus);
        assertTrue(dataProcessor.isJobClosed());

        assertEquals(0, dataProcessor.getNumberOfStreams());
        assertEquals("1400000000000", dataExtractor.getStart(0));
        assertEquals("1400000001000", dataExtractor.getEnd(0));
        assertEquals(0, dataProcessor.getFlushParams().size());

        // Repeat to test that scheduler did not advance time
        m_JobScheduler.start(new JobDetails(), 1400000000000L, OptionalLong.of(1400000001000L));
        waitUntilSchedulerStoppedIsAudited();

        assertEquals(0, dataProcessor.getNumberOfStreams());
        assertEquals("1400000000000", dataExtractor.getStart(0));
        assertEquals("1400000001000", dataExtractor.getEnd(0));
    }

    @Test
    public void testStart_GivenDataExtractorThrows() throws CannotStartSchedulerException,
            CannotStopSchedulerException, IOException, InterruptedException
    {
        DataExtractor dataExtractor = mock(DataExtractor.class);
        when(dataExtractor.hasNext()).thenReturn(true);
        when(dataExtractor.next()).thenThrow(new IOException());

        MockDataProcessor dataProcessor = new MockDataProcessor(Collections.emptyList());
        m_JobScheduler = createJobScheduler(dataExtractor, dataProcessor);

        m_JobScheduler.start(new JobDetails(), 1400000000000L, OptionalLong.of(1400000001000L));
        waitUntilSchedulerStoppedIsAudited();
        assertEquals(JobSchedulerStatus.STOPPED, m_CurrentStatus);
        assertTrue(dataProcessor.isJobClosed());

        assertEquals(0, dataProcessor.getNumberOfStreams());
        assertEquals(0, dataProcessor.getFlushParams().size());
    }

    @Test
    public void testStart_GivenLatestRecordTimestampIsBeforeSchedulerStartTime()
            throws CannotStartSchedulerException, CannotStopSchedulerException
    {
        JobDetails job = new JobDetails();
        DataCounts dataCounts = new DataCounts();
        dataCounts.setLatestRecordTimeStamp(new Date(1455000000000L));
        job.setCounts(dataCounts);

        MockDataExtractor dataExtractor = new MockDataExtractor(Arrays.asList(1));
        MockDataProcessor dataProcessor = new MockDataProcessor(Arrays.asList(
                newCounts(67, 1450000000000L)));
        m_JobScheduler = createJobScheduler(dataExtractor, dataProcessor);

        m_JobScheduler.start(job, 1450000000000L, OptionalLong.of(1460000000000L));
        waitUntilSchedulerStoppedIsAudited();
        assertEquals(JobSchedulerStatus.STOPPED, m_CurrentStatus);
        assertTrue(dataProcessor.isJobClosed());

        assertEquals(1, dataProcessor.getNumberOfStreams());
        assertEquals("0-0", dataProcessor.getStream(0));
        assertEquals("1455000001000", dataExtractor.getStart(0));
        assertEquals("1460000000000", dataExtractor.getEnd(0));
        List<InterimResultsParams> flushParams = dataProcessor.getFlushParams();
        assertEquals(1, flushParams.size());
        assertTrue(flushParams.get(0).shouldCalculateInterim());
        assertFalse(flushParams.get(0).shouldAdvanceTime());
    }

    @Test
    public void testStart_GivenLatestRecordTimestampIsAfterSchedulerStartTime()
            throws CannotStartSchedulerException, CannotStopSchedulerException
    {
        JobDetails job = new JobDetails();
        DataCounts dataCounts = new DataCounts();
        dataCounts.setLatestRecordTimeStamp(new Date(1450000000000L));
        job.setCounts(dataCounts);

        MockDataExtractor dataExtractor = new MockDataExtractor(Arrays.asList(1));
        MockDataProcessor dataProcessor = new MockDataProcessor(Arrays.asList(
                newCounts(67, 1455000000000L)));
        m_JobScheduler = createJobScheduler(dataExtractor, dataProcessor);

        m_JobScheduler.start(job, 1455000000000L, OptionalLong.of(1460000000000L));
        waitUntilSchedulerStoppedIsAudited();
        assertEquals(JobSchedulerStatus.STOPPED, m_CurrentStatus);
        assertTrue(dataProcessor.isJobClosed());

        assertEquals(1, dataProcessor.getNumberOfStreams());
        assertEquals("0-0", dataProcessor.getStream(0));
        assertEquals("1455000000000", dataExtractor.getStart(0));
        assertEquals("1460000000000", dataExtractor.getEnd(0));
        List<InterimResultsParams> flushParams = dataProcessor.getFlushParams();
        assertEquals(1, flushParams.size());
        assertTrue(flushParams.get(0).shouldCalculateInterim());
        assertFalse(flushParams.get(0).shouldAdvanceTime());
    }

    @Test
    public void testStart_GivenAlreadyStarted()
            throws CannotStartSchedulerException, CannotStopSchedulerException
    {
        m_ExpectedException.expect(CannotStartSchedulerException.class);
        m_ExpectedException.expectMessage(
                "Cannot start scheduler for job 'foo' while its status is STARTED");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.CANNOT_START_JOB_SCHEDULER));

        MockDataExtractor dataExtractor = new MockDataExtractor(Arrays.asList(1, 1, 1));
        MockDataProcessor dataProcessor = new MockDataProcessor(Arrays.asList(
                newCounts(67, 1400000000300L),
                newCounts(23, 1400000000600L),
                newCounts(55, 1400000000900L)));
        JobDetails job = new JobDetails();
        m_JobScheduler = createJobScheduler(dataExtractor, dataProcessor);

        m_JobScheduler.start(job, 1400000000000L, OptionalLong.empty());
        assertEquals(JobSchedulerStatus.STARTED, m_CurrentStatus);
        m_JobScheduler.start(job, 1400000000000L, OptionalLong.empty());
    }

    @Test
    public void testStopManual_GivenAlreadyStopped()
            throws CannotStartSchedulerException, CannotStopSchedulerException
    {
        m_ExpectedException.expect(CannotStopSchedulerException.class);
        m_ExpectedException.expectMessage(
                "Cannot stop scheduler for job 'foo' while its status is STOPPED");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.CANNOT_STOP_JOB_SCHEDULER));

        MockDataExtractor dataExtractor = new MockDataExtractor(Arrays.asList(1, 1, 1));
        MockDataProcessor dataProcessor = new MockDataProcessor(Arrays.asList(
                newCounts(67, 1400000000300L),
                newCounts(23, 1400000000600L),
                newCounts(55, 1400000000900L)));
        JobDetails job = new JobDetails();
        m_JobScheduler = createJobScheduler(dataExtractor, dataProcessor);

        m_JobScheduler.start(job, 1400000000000L, OptionalLong.empty());
        m_JobScheduler.stopManual();
        assertEquals(JobSchedulerStatus.STOPPED, m_CurrentStatus);
        m_JobScheduler.stopManual();
    }

    @Test
    public void testStopAuto_GivenLookbackOnlyJob() throws CannotStartSchedulerException,
            CannotStopSchedulerException
    {
        MockDataExtractor dataExtractor = new MockDataExtractor(Arrays.asList(1));
        MockDataProcessor dataProcessor = new MockDataProcessor(Arrays.asList(
                newCounts(67, 1500000000000L)));
        m_JobScheduler = createJobScheduler(dataExtractor, dataProcessor);
        m_JobScheduler.start(new JobDetails(), 1400000000000L, OptionalLong.of(1500000000000L));

        while (m_CurrentStatus == null)
        {
        }

        boolean lookbackFinished = m_CurrentStatus != JobSchedulerStatus.STARTED;
        m_JobScheduler.stopAuto();

        if (lookbackFinished)
        {
            assertEquals(JobSchedulerStatus.STOPPED, m_CurrentStatus);
            assertTrue(dataProcessor.isJobClosed());
        }
        else
        {
            assertEquals(JobSchedulerStatus.STARTED, m_CurrentStatus);
            assertFalse(dataProcessor.isJobClosed());
        }
    }

    @Test
    public void testStopAuto_GivenRealTimeJob() throws InterruptedException,
            CannotStartSchedulerException
    {
        MockDataExtractor dataExtractor = new MockDataExtractor(Arrays.asList(1, 1, 1));
        MockDataProcessor dataProcessor = new MockDataProcessor(Arrays.asList(
                newCounts(67, 1400000000300L),
                newCounts(23, 1400000000600L),
                newCounts(55, 1400000000900L)));
        m_JobScheduler = createJobScheduler(dataExtractor, dataProcessor);

        m_JobScheduler.start(new JobDetails(), 1400000000000L, OptionalLong.empty());
        assertEquals(JobSchedulerStatus.STARTED, m_CurrentStatus);
        m_JobScheduler.stopAuto();
        assertFalse(dataProcessor.isJobClosed());

        // Wait enough time for real-time tasks to begin in case stop did not work
        Thread.sleep(1200);

        assertEquals(JobSchedulerStatus.STARTED, m_CurrentStatus);
    }

    private void recordSchedulerStatus() throws UnknownJobException
    {
        when(m_JobProvider.updateJob(eq(JOB_ID), anyMapOf(String.class, Object.class))).thenAnswer(
                new Answer<Boolean>()
        {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable
            {
                @SuppressWarnings("unchecked")
                Map<String, Object> updates = (Map<String, Object>) invocation.getArguments()[1];
                m_CurrentStatus = (JobSchedulerStatus) updates.get("schedulerStatus");
                return true;
            }
        });
    }

    private void recordSchedulerStoppedAudited()
    {
        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                m_SchedulerStoppedAuditedLatch.countDown();
                return null;
            }
        }).when(m_Auditor).info("Scheduler stopped");
    }

    private void waitUntilSchedulerStoppedIsAudited()
    {
        try
        {
            m_SchedulerStoppedAuditedLatch.await();
        }
        catch (InterruptedException e)
        {
            throw new IllegalStateException(e);
        }
        m_SchedulerStoppedAuditedLatch = new CountDownLatch(1);
    }

    private JobScheduler createJobScheduler(DataExtractor dataExtractor, DataProcessor dataProcessor)
    {
        return new JobScheduler(JOB_ID, BUCKET_SPAN, FREQUENCY, QUERY_DELAY, dataExtractor,
                dataProcessor, m_JobProvider, m_JobLoggerFactory);
    }

    private static DataCounts newCounts(int recordCount, Long latestRecordTime)
    {
        DataCounts counts = new DataCounts();
        counts.setProcessedRecordCount(recordCount);
        counts.setLatestRecordTimeStamp(
                latestRecordTime == null ? null : new Date(latestRecordTime));
        return counts;
    }

    private long calcAlignedBucketEnd(long timeMs)
    {
        long result = (timeMs / BUCKET_SPAN.toMillis()) * BUCKET_SPAN.toMillis();
        if (result != timeMs)
        {
            result += BUCKET_SPAN.toMillis();
        }
        return result;
    }

    private static class MockDataExtractor implements DataExtractor
    {
        private final List<Integer> m_BatchesPerSearch;
        private int m_SearchCount = -1;
        private int m_StreamCount = -1;
        private final List<String> m_Starts = new ArrayList<>();
        private final List<String> m_Ends = new ArrayList<>();

        public MockDataExtractor(List<Integer> batchesPerSearch)
        {
            m_BatchesPerSearch = batchesPerSearch;
        }

        @Override
        public boolean hasNext()
        {
            return m_StreamCount < m_BatchesPerSearch.get(m_SearchCount) - 1;
        }

        @Override
        public Optional<InputStream> next()
        {
            if (!hasNext())
            {
                throw new NoSuchElementException();
            }
            m_StreamCount++;
            String stream = "" + m_SearchCount + "-" + m_StreamCount;
            return Optional.of(new ByteArrayInputStream(stream.getBytes()));
        }

        @Override
        public void newSearch(String start, String end, Logger logger)
        {
            if (m_SearchCount == m_BatchesPerSearch.size() - 1)
            {
                throw new IllegalStateException();
            }
            m_SearchCount++;
            m_StreamCount = -1;
            m_Starts.add(start);
            m_Ends.add(end);
        }

        public String getStart(int searchCount)
        {
            return m_Starts.get(searchCount);
        }

        public String getEnd(int searchCount)
        {
            return m_Ends.get(searchCount);
        }
    }

    private static class MockDataProcessor implements DataProcessor
    {
        private final List<DataCounts> m_CountsPerStream;
        private final List<String> m_Streams = new ArrayList<>();
        private final List<InterimResultsParams> m_FlushParams = new ArrayList<>();
        private boolean m_ShouldThrow = false;
        private boolean m_IsJobClosed = false;
        private int m_StreamCount = 0;

        MockDataProcessor(List<DataCounts> countsPerStream)
        {
            m_CountsPerStream = countsPerStream;
        }

        public void setShouldThrow(boolean value)
        {
            m_ShouldThrow = value;
        }

        public String getStream(int index)
        {
            return m_Streams.get(index);
        }

        public int getNumberOfStreams()
        {
            return m_Streams.size();
        }

        public List<InterimResultsParams> getFlushParams()
        {
            return m_FlushParams;
        }

        public boolean isJobClosed()
        {
            return m_IsJobClosed;
        }

        @Override
        public DataCounts submitDataLoadJob(String jobId, InputStream input, DataLoadParams params)
                throws UnknownJobException, NativeProcessRunException, MissingFieldException,
                JsonParseException, JobInUseException, HighProportionOfBadTimestampsException,
                OutOfOrderRecordsException, TooManyJobsException, MalformedJsonException
        {
            assertEquals(JOB_ID, jobId);
            assertFalse(params.isPersisting());
            assertFalse(params.isResettingBuckets());
            if (m_ShouldThrow)
            {
                throw new UnknownJobException(JOB_ID);
            }
            try
            {
                m_Streams.add(streamToString(input));
            }
            catch (IOException e)
            {
                throw new IllegalStateException();
            }
            return m_CountsPerStream.get(m_StreamCount++);
        }

        @Override
        public void flushJob(String jobId, InterimResultsParams params)
                throws UnknownJobException, NativeProcessRunException, JobInUseException
        {
            if (jobId.equals(JOB_ID))
            {
                m_FlushParams.add(params);
            }
        }

        @Override
        public void closeJob(String jobId) throws UnknownJobException, NativeProcessRunException,
                JobInUseException
        {
            m_IsJobClosed = jobId.equals(JOB_ID);
        }
    }

    private static String streamToString(InputStream stream) throws IOException
    {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return buffer.lines().collect(Collectors.joining("\n")).trim();
        }
    }
}
