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
import java.util.concurrent.TimeUnit;
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
import com.prelert.job.exceptions.LicenseViolationException;
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

    @Rule public ExpectedException m_ExpectedException = ExpectedException.none();

    @Mock private JobProvider m_JobProvider;
    @Mock private JobLoggerFactory m_JobLoggerFactory;
    @Mock private Logger m_JobLogger;
    @Mock private Auditor m_Auditor;

    private volatile JobSchedulerStatus m_CurrentStatus;

    private Duration m_Frequency;
    private Duration m_QueryDelay;
    private JobScheduler m_JobScheduler;
    private volatile CountDownLatch m_SchedulerStoppedAuditedLatch;

    @Before
    public void setUp() throws UnknownJobException
    {
        MockitoAnnotations.initMocks(this);
        m_CurrentStatus = null;
        m_Frequency = Duration.ofSeconds(1);
        m_QueryDelay = Duration.ofSeconds(0);
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
        assertEquals(1400000000000L, dataExtractor.getStart(0));
        assertEquals(1400000001000L, dataExtractor.getEnd(0));

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
        assertEquals(1400000000000L, dataExtractor.getStart(0));
        assertEquals(1400000001000L, dataExtractor.getEnd(0));

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
        long lookbackLatestRecordTime = System.currentTimeMillis() - 100;
        long[] latestRecordTimes = { lookbackLatestRecordTime, lookbackLatestRecordTime + 1000,
                lookbackLatestRecordTime + 2000 };

        int numberOfSearches = 3;
        MockDataExtractor dataExtractor = new MockDataExtractor(Arrays.asList(1, 1, 1));
        MockDataProcessor dataProcessor = new MockDataProcessor(Arrays.asList(
                newCounts(67, latestRecordTimes[0]),
                newCounts(23, latestRecordTimes[1]),
                newCounts(55, latestRecordTimes[2])),
                new CountDownLatch(numberOfSearches));
        m_JobScheduler = createJobScheduler(dataExtractor, dataProcessor);

        long schedulerStartedTimeMs = System.currentTimeMillis();

        m_JobScheduler.start(new JobDetails(), 1400000000000L, OptionalLong.empty());
        assertEquals(JobSchedulerStatus.STARTED, m_CurrentStatus);
        assertTrue(dataProcessor.awaitForCountDownLatch());
        m_JobScheduler.stopManual();
        assertEquals(JobSchedulerStatus.STOPPED, m_CurrentStatus);

        assertEquals(numberOfSearches, dataProcessor.getNumberOfStreams());
        List<InterimResultsParams> flushParams = dataProcessor.getFlushParams();
        assertEquals(numberOfSearches, flushParams.size());

        long lookbackEnd = dataExtractor.getEnd(0);
        long firstRealTimeEnd = dataExtractor.getEnd(1);
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

            if (i == 1)
            {
                // Assert first real-time search
                assertEquals(lookbackLatestRecordTime + 1, searchStart);
                assertTrue(firstRealTimeEnd > lookbackLatestRecordTime + 1);
            }
            else
            {
                // Assert rest of real-time searches
                assertEquals(dataExtractor.getEnd(i - 1), dataExtractor.getStart(i));
                assertEquals(searchStart + m_Frequency.toMillis(), searchEnd);
            }

            assertTrue(flushParams.get(i).shouldAdvanceTime());
            assertEquals(Math.min(calcAlignedBucketEnd(latestRecordTimes[i]), searchEnd),
                                    flushParams.get(i).getAdvanceTime() * 1000);
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
        long lookbackLatestRecordTime = new Date().getTime() - 100;

        int numberOfSearches = 2;
        MockDataExtractor dataExtractor = new MockDataExtractor(Arrays.asList(1, 1));
        MockDataProcessor dataProcessor = new MockDataProcessor(Arrays.asList(
                newCounts(67, lookbackLatestRecordTime),
                newCounts(0, null)),
                new CountDownLatch(numberOfSearches));
        m_JobScheduler = createJobScheduler(dataExtractor, dataProcessor);

        m_JobScheduler.start(new JobDetails(), 1400000000000L, OptionalLong.empty());
        assertEquals(JobSchedulerStatus.STARTED, m_CurrentStatus);
        assertTrue(dataProcessor.awaitForCountDownLatch());
        m_JobScheduler.stopManual();
        assertEquals(JobSchedulerStatus.STOPPED, m_CurrentStatus);

        assertEquals(numberOfSearches, dataProcessor.getNumberOfStreams());
        List<InterimResultsParams> flushParams = dataProcessor.getFlushParams();
        assertEquals(1, flushParams.size());

        assertTrue(flushParams.get(0).shouldCalculateInterim());
        assertFalse(flushParams.get(0).shouldAdvanceTime());
    }

    @Test
    public void testStart_GivenLookbackWithEmptyDataAndRealtimeWithEmptyData()
            throws CannotStartSchedulerException, CannotStopSchedulerException
    {
        // Minimise the time the test takes by setting frequency to 1.
        // In addition to the minimum query delay of 100ms this means an effective
        // frequency of 101ms.
        m_Frequency = Duration.ofMillis(1);

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
        m_JobScheduler = createJobScheduler(dataExtractor, dataProcessor);

        m_JobScheduler.start(new JobDetails(), 1400000000000L, OptionalLong.empty());
        assertEquals(JobSchedulerStatus.STARTED, m_CurrentStatus);
        assertTrue(dataProcessor.awaitForCountDownLatch());
        m_JobScheduler.stopManual();
        assertEquals(JobSchedulerStatus.STOPPED, m_CurrentStatus);

        assertEquals(numberOfSearches, dataProcessor.getNumberOfStreams());
        assertEquals(1400000000000L, dataExtractor.getStart(0));
        assertEquals(1400000000000L, dataExtractor.getStart(1));
        assertTrue(dataProcessor.getFlushParams().isEmpty());

        assertTrue(dataProcessor.isJobClosed());
        verify(m_Auditor).warning("Scheduler has been retrieving no data for a while");
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
        assertEquals(1400000000000L, dataExtractor.getStart(0));
        assertEquals(1400000001000L, dataExtractor.getEnd(0));
        assertEquals(0, dataProcessor.getFlushParams().size());

        // Repeat to test that scheduler did not advance time
        m_JobScheduler.start(new JobDetails(), 1400000000000L, OptionalLong.of(1400000001000L));
        waitUntilSchedulerStoppedIsAudited();

        assertEquals(0, dataProcessor.getNumberOfStreams());
        assertEquals(1400000000000L, dataExtractor.getStart(0));
        assertEquals(1400000001000L, dataExtractor.getEnd(0));
    }

    @Test
    public void testStart_GivenDataExtractorThrows() throws CannotStartSchedulerException,
            CannotStopSchedulerException, IOException
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
        assertEquals(1455000000001L, dataExtractor.getStart(0));
        assertEquals(1460000000000L, dataExtractor.getEnd(0));
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
        assertEquals(1455000000000L, dataExtractor.getStart(0));
        assertEquals(1460000000000L, dataExtractor.getEnd(0));
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
    public void testStopAuto_GivenRealTimeJob() throws CannotStartSchedulerException
    {
        MockDataExtractor dataExtractor = new MockDataExtractor(Arrays.asList(1, 1, 1));
        MockDataProcessor dataProcessor = new MockDataProcessor(Arrays.asList(
                newCounts(67, 1400000000300L),
                newCounts(23, 1400000000600L),
                newCounts(55, 1400000000900L)),
                new CountDownLatch(2));
        m_JobScheduler = createJobScheduler(dataExtractor, dataProcessor);

        m_JobScheduler.start(new JobDetails(), 1400000000000L, OptionalLong.empty());
        assertEquals(JobSchedulerStatus.STARTED, m_CurrentStatus);

        // Wait enough time for real-time tasks to begin in case stop did not work
        assertTrue(dataProcessor.awaitForCountDownLatch());

        m_JobScheduler.stopAuto();
        assertFalse(dataProcessor.isJobClosed());
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
        return new JobScheduler(JOB_ID, BUCKET_SPAN, m_Frequency, m_QueryDelay, dataExtractor,
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
        private final List<Long> m_Starts = new ArrayList<>();
        private final List<Long> m_Ends = new ArrayList<>();

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
        public void newSearch(long start, long end, Logger logger)
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

        public long getStart(int searchCount)
        {
            return m_Starts.get(searchCount);
        }

        public long getEnd(int searchCount)
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
        private final CountDownLatch m_CountDownLatch;

        MockDataProcessor(List<DataCounts> countsPerStream)
        {
            this(countsPerStream, new CountDownLatch(0));
        }

        MockDataProcessor(List<DataCounts> countsPerStream, CountDownLatch countDownLatch)
        {
            m_CountsPerStream = countsPerStream;
            m_CountDownLatch = countDownLatch;
        }

        /**
         * Wait until there are as many streams processed as the count down
         * latch provided upon construction. This method has a timeout of 10 seconds.
         * No test should need more than 10 seconds.
         *
         * @return {@code true} if latch was counted down to 0 or {@code false} if it timed out
         */
        public boolean awaitForCountDownLatch()
        {
            try
            {
                m_CountDownLatch.await(10, TimeUnit.SECONDS);
            }
            catch (InterruptedException e)
            {
                return false;
            }
            return true;
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
                OutOfOrderRecordsException, LicenseViolationException, MalformedJsonException
        {
            assertEquals(JOB_ID, jobId);
            assertFalse(params.isPersisting());
            assertFalse(params.isResettingBuckets());
            m_CountDownLatch.countDown();
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
