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

package com.prelert.job.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.prelert.job.DataCounts;
import com.prelert.job.JobDetails;
import com.prelert.job.SchedulerConfig;
import com.prelert.job.UnknownJobException;
import com.prelert.job.data.extraction.DataExtractor;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.exceptions.TooManyJobsException;
import com.prelert.job.process.exceptions.MalformedJsonException;
import com.prelert.job.process.exceptions.MissingFieldException;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.process.params.DataLoadParams;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;

public class JobSchedulerTest
{
    private static final String JOB_ID = "foo";
    private static final long BUCKET_SPAN = 1L;

    private JobScheduler m_JobScheduler;

    @Test
    public void testStart_GivenLookbackOnlyAndSingleStream()
    {
        JobDetails job = newJobWithElasticScheduler(1400000000000L, 1400000001000L);
        MockDataExtractor dataExtractor = new MockDataExtractor(Arrays.asList(1));
        MockDataProcessor dataProcessor = new MockDataProcessor();
        m_JobScheduler = new JobScheduler(JOB_ID, BUCKET_SPAN, dataExtractor, dataProcessor);

        m_JobScheduler.start(job);
        m_JobScheduler.stop();

        assertEquals(1, dataProcessor.getNumberOfStreams());
        assertEquals("0-0", dataProcessor.getStream(0));
        assertEquals("1400000000000", dataExtractor.getStart(0));
        assertEquals("1400000001000", dataExtractor.getEnd(0));
    }

    @Test
    public void testStart_GivenLookbackOnlyAndMultipleStreams()
    {
        JobDetails job = newJobWithElasticScheduler(1400000000000L, 1400000001000L);
        MockDataExtractor dataExtractor = new MockDataExtractor(Arrays.asList(3));
        MockDataProcessor dataProcessor = new MockDataProcessor();
        m_JobScheduler = new JobScheduler(JOB_ID, BUCKET_SPAN, dataExtractor, dataProcessor);

        m_JobScheduler.start(job);
        m_JobScheduler.stop();

        assertEquals(3, dataProcessor.getNumberOfStreams());
        assertEquals("0-0", dataProcessor.getStream(0));
        assertEquals("0-1", dataProcessor.getStream(1));
        assertEquals("0-2", dataProcessor.getStream(2));
        assertEquals("1400000000000", dataExtractor.getStart(0));
        assertEquals("1400000001000", dataExtractor.getEnd(0));
    }

    @Test
    public void testStart_GivenLookbackAndRealtimeWithSingleStreams()
    {
        JobDetails job = newJobWithElasticScheduler(1400000000000L, null);
        MockDataExtractor dataExtractor = new MockDataExtractor(Arrays.asList(1, 1, 1));
        MockDataProcessor dataProcessor = new MockDataProcessor();
        m_JobScheduler = new JobScheduler(JOB_ID, BUCKET_SPAN, dataExtractor, dataProcessor);

        long nowMs = new Date().getTime();
        long bucketSpanMs = BUCKET_SPAN * 1000;
        long bucketEnd = (nowMs / bucketSpanMs) * bucketSpanMs;

        m_JobScheduler.start(job);

        // Give time to scheduler to perform at least one real-time search
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        m_JobScheduler.stop();

        assertTrue(dataProcessor.getNumberOfStreams() > 1);
        for (int i = 0; i < dataProcessor.getNumberOfStreams(); i++)
        {
            assertEquals("" + i + "-0", dataProcessor.getStream(i));
            if (i == 0)
            {
                assertEquals("1400000000000", dataExtractor.getStart(i));
            }
            else
            {
                assertEquals(String.valueOf(bucketEnd - bucketSpanMs), dataExtractor.getStart(i));
            }
            assertEquals(String.valueOf(bucketEnd), dataExtractor.getEnd(i));
            bucketEnd += bucketSpanMs;
        }
    }

    @Test
    public void testStart_GivenDataProcessorThrows()
    {
        JobDetails job = newJobWithElasticScheduler(1400000000000L, 1400000001000L);
        MockDataExtractor dataExtractor = new MockDataExtractor(Arrays.asList(1, 1));
        MockDataProcessor dataProcessor = new MockDataProcessor();
        dataProcessor.setShouldThrow(true);
        m_JobScheduler = new JobScheduler(JOB_ID, BUCKET_SPAN, dataExtractor, dataProcessor);

        m_JobScheduler.start(job);
        m_JobScheduler.stop();

        assertEquals(0, dataProcessor.getNumberOfStreams());
        assertEquals("1400000000000", dataExtractor.getStart(0));
        assertEquals("1400000001000", dataExtractor.getEnd(0));

        // Repeat to test that scheduler did not advance time
        m_JobScheduler.start(job);
        m_JobScheduler.stop();

        assertEquals(0, dataProcessor.getNumberOfStreams());
        assertEquals("1400000000000", dataExtractor.getStart(0));
        assertEquals("1400000001000", dataExtractor.getEnd(0));
    }

    @Test
    public void testStart_GivenJobHasLatestRecordTimestamp()
    {
        JobDetails job = newJobWithElasticScheduler(1450000000000L, 1460000000000L);
        DataCounts dataCounts = new DataCounts();
        dataCounts.setLatestRecordTimeStamp(new Date(1455000000000L));
        job.setCounts(dataCounts);

        MockDataExtractor dataExtractor = new MockDataExtractor(Arrays.asList(1));
        MockDataProcessor dataProcessor = new MockDataProcessor();
        m_JobScheduler = new JobScheduler(JOB_ID, BUCKET_SPAN, dataExtractor, dataProcessor);

        m_JobScheduler.start(job);
        m_JobScheduler.stop();

        assertEquals(1, dataProcessor.getNumberOfStreams());
        assertEquals("0-0", dataProcessor.getStream(0));
        assertEquals("1455000001000", dataExtractor.getStart(0));
        assertEquals("1460000000000", dataExtractor.getEnd(0));
    }

    @Test
    public void testStart_GivenLookbackStartTimeIsNull()
    {
        JobDetails job = newJobWithElasticScheduler(null, 1400000000000L);
        MockDataExtractor dataExtractor = new MockDataExtractor(Arrays.asList(1));
        MockDataProcessor dataProcessor = new MockDataProcessor();
        m_JobScheduler = new JobScheduler(JOB_ID, BUCKET_SPAN, dataExtractor, dataProcessor);

        m_JobScheduler.start(job);
        m_JobScheduler.stop();

        assertEquals(1, dataProcessor.getNumberOfStreams());
        assertEquals("0-0", dataProcessor.getStream(0));
        assertEquals("0", dataExtractor.getStart(0));
        assertEquals("1400000000000", dataExtractor.getEnd(0));
    }

    private static JobDetails newJobWithElasticScheduler(Long startTimeEpochMs, Long endTimeEpochMs)
    {
        JobDetails job = new JobDetails();
        job.setId(JOB_ID);
        SchedulerConfig schedulerConfig = new SchedulerConfig();
        if (startTimeEpochMs != null)
        {
            schedulerConfig.setStartTime(new Date(startTimeEpochMs));
        }
        if (endTimeEpochMs != null)
        {
            schedulerConfig.setEndTime(new Date(endTimeEpochMs));
        }
        job.setSchedulerConfig(schedulerConfig);
        return job;
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
        public void newSearch(String start, String end)
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
        private List<String> m_Streams = new ArrayList<>();
        private boolean m_ShouldThrow = false;

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
            } catch (IOException e)
            {
                throw new IllegalStateException();
            }
            return new DataCounts();
        }
    }

    private static String streamToString(InputStream stream) throws IOException
    {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return buffer.lines().collect(Collectors.joining("\n")).trim();
        }
    }
}
