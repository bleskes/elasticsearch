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

package com.prelert.job.process.autodetect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.core.JsonParseException;
import com.google.common.base.Charsets;
import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataCounts;
import com.prelert.job.DataDescription;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.UnknownJobException;
import com.prelert.job.alert.AlertObserver;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.persistence.DataPersisterFactory;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.process.exceptions.ClosedJobException;
import com.prelert.job.process.exceptions.MalformedJsonException;
import com.prelert.job.process.exceptions.MissingFieldException;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.process.output.parsing.ResultsReader;
import com.prelert.job.process.params.DataLoadParams;
import com.prelert.job.process.params.InterimResultsParams;
import com.prelert.job.process.params.TimeRange;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;
import com.prelert.job.status.StatusReporter;
import com.prelert.job.transform.TransformConfigs;

public class ProcessManagerTest
{
    @Rule public ExpectedException m_ExpectedException = ExpectedException.none();

    @Mock private ProcessFactory m_ProcessFactory;
    @Mock private JobProvider m_JobProvider;
    @Mock private DataPersisterFactory m_DataPersisterFactory;

    private ProcessManager m_ProcessManager;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        m_ProcessManager = new ProcessManager(m_JobProvider, m_ProcessFactory,
                m_DataPersisterFactory);
    }

    @Test
    public void testProcessDataLoadJob_GivenRunningProcess() throws UnknownJobException,
            NativeProcessRunException, JsonParseException, MissingFieldException,
            JobInUseException, HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            MalformedJsonException
    {
        MockProcessBuilder mockProcessBuilder = new MockProcessBuilder();
        ProcessAndDataDescription process = mockProcessBuilder
                .withAvailableWriting().stillRunning(true).build();
        when(m_ProcessFactory.createProcess("foo")).thenReturn(process);

        InputStream inputStream = createInputStream("time");
        m_ProcessManager.processDataLoadJob("foo", inputStream, createNoPersistNoResetDataLoadParams());

        assertTrue(m_ProcessManager.jobIsRunning("foo"));
        String output = mockProcessBuilder.getOutput().trim();
        assertTrue(output.startsWith("time"));
        verify(process).releaseGuard();
    }

    @Test
    public void testFlush_GivenJobWithoutRunningProcess() throws NativeProcessRunException,
            JobInUseException, UnknownJobException
    {
        MockProcessBuilder mockProcessBuilder = new MockProcessBuilder();
        ProcessAndDataDescription process = mockProcessBuilder
                .withAvailableWriting()
                .withAvailableFlushing()
                .stillRunning(false)
                .build();
        when(m_ProcessFactory.createProcess("foo")).thenReturn(process);

        m_ProcessManager.flushJob("foo", createNoInterimResults());

        String output = mockProcessBuilder.getOutput().trim();
        assertTrue(output.isEmpty());
    }

    @Test
    public void testFlush_GivenJobWithRunningProcess() throws NativeProcessRunException,
            JobInUseException, JsonParseException, UnknownJobException, MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            MalformedJsonException
    {
        MockProcessBuilder mockProcessBuilder = new MockProcessBuilder();
        ProcessAndDataDescription process = mockProcessBuilder
                .withAvailableWriting()
                .withAvailableFlushing()
                .stillRunning(true)
                .build();
        when(m_ProcessFactory.createProcess("foo")).thenReturn(process);

        InputStream inputStream = createInputStream("");
        m_ProcessManager.processDataLoadJob("foo", inputStream, createNoPersistNoResetDataLoadParams());

        m_ProcessManager.flushJob("foo", createNoInterimResults());

        String output = mockProcessBuilder.getOutput().trim();
        assertTrue(output.startsWith("f"));
        verify(process, times(2)).releaseGuard();
    }

    @Test
    public void testFlush_GivenJobWithRunningProcessThatIsStillInUse() throws NativeProcessRunException,
            JobInUseException, JsonParseException, UnknownJobException, MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            MalformedJsonException
    {
        m_ExpectedException.expect(JobInUseException.class);

        MockProcessBuilder mockProcessBuilder = new MockProcessBuilder();
        ProcessAndDataDescription process = mockProcessBuilder
                .withAvailableWriting()
                .withAction(Action.WRITING)
                .stillRunning(true)
                .build();
        when(m_ProcessFactory.createProcess("foo")).thenReturn(process);

        InputStream inputStream = createInputStream("");
        m_ProcessManager.processDataLoadJob("foo", inputStream, createNoPersistNoResetDataLoadParams());

        m_ProcessManager.flushJob("foo", createNoInterimResults());
    }

    @Test
    public void testClose_GivenJobWithRunningProcess() throws NativeProcessRunException,
            JobInUseException, JsonParseException, UnknownJobException, MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            MalformedJsonException
    {
        MockProcessBuilder mockProcessBuilder = new MockProcessBuilder();
        ProcessAndDataDescription process = mockProcessBuilder
                .withAvailableWriting()
                .withAvailableClosing()
                .stillRunning(true)
                .build();
        when(m_ProcessFactory.createProcess("foo")).thenReturn(process);

        InputStream inputStream = createInputStream("");
        m_ProcessManager.processDataLoadJob("foo", inputStream, createNoPersistNoResetDataLoadParams());
        assertTrue(m_ProcessManager.jobIsRunning("foo"));
        assertEquals(1, m_ProcessManager.numberOfRunningJobs());

        m_ProcessManager.closeJob("foo");

        assertFalse(m_ProcessManager.jobIsRunning("foo"));
        assertEquals(0, m_ProcessManager.numberOfRunningJobs());
        verify(process, times(2)).releaseGuard();
    }

    @Test
    public void testClose_GivenJobWithRunningProcessThatIsStillInUse() throws NativeProcessRunException,
            JobInUseException, JsonParseException, UnknownJobException, MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            MalformedJsonException
    {
        m_ExpectedException.expect(JobInUseException.class);

        MockProcessBuilder mockProcessBuilder = new MockProcessBuilder();
        ProcessAndDataDescription process = mockProcessBuilder
                .withAvailableWriting()
                .withAction(Action.WRITING)
                .stillRunning(true)
                .build();
        when(m_ProcessFactory.createProcess("foo")).thenReturn(process);

        InputStream inputStream = createInputStream("");
        m_ProcessManager.processDataLoadJob("foo", inputStream, createNoPersistNoResetDataLoadParams());
        assertEquals(1, m_ProcessManager.numberOfRunningJobs());

        m_ProcessManager.closeJob("foo");
    }

    @Test
    public void testClose_GivenJobWithoutRunningProcess() throws NativeProcessRunException,
            JobInUseException, JsonParseException, UnknownJobException, MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            MalformedJsonException
    {
        MockProcessBuilder mockProcessBuilder = new MockProcessBuilder();
        ProcessAndDataDescription process = mockProcessBuilder
                .withAvailableWriting()
                .withAvailableClosing()
                .stillRunning(false)
                .build();
        when(m_ProcessFactory.createProcess("foo")).thenReturn(process);

        m_ProcessManager.closeJob("foo");

        assertEquals(0, m_ProcessManager.numberOfRunningJobs());
    }

    @Test
    public void testStop_ClosesAllRunningJobs() throws NativeProcessRunException,
            JobInUseException, JsonParseException, UnknownJobException, MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            MalformedJsonException
    {
        MockProcessBuilder mockProcessBuilder = new MockProcessBuilder();
        ProcessAndDataDescription process1 = mockProcessBuilder
                .withAvailableWriting()
                .withAvailableClosing()
                .stillRunning(true)
                .build();
        ProcessAndDataDescription process2 = mockProcessBuilder
                .withAvailableWriting()
                .withAvailableClosing()
                .stillRunning(true)
                .build();
        when(m_ProcessFactory.createProcess("job_1")).thenReturn(process1);
        when(m_ProcessFactory.createProcess("job_2")).thenReturn(process2);

        InputStream inputStream = createInputStream("");
        m_ProcessManager.processDataLoadJob("job_1", inputStream, createNoPersistNoResetDataLoadParams());
        m_ProcessManager.processDataLoadJob("job_2", inputStream, createNoPersistNoResetDataLoadParams());
        assertTrue(m_ProcessManager.jobIsRunning("job_1"));
        assertTrue(m_ProcessManager.jobIsRunning("job_1"));
        assertEquals(2, m_ProcessManager.numberOfRunningJobs());

        m_ProcessManager.stop();

        assertFalse(m_ProcessManager.jobIsRunning("job_1"));
        assertFalse(m_ProcessManager.jobIsRunning("job_2"));
        assertEquals(0, m_ProcessManager.numberOfRunningJobs());
    }

    @Test
    public void testWriteUpdateConfigMessage_GivenJobWithoutProcess()
            throws NativeProcessRunException, JobInUseException
    {
        m_ProcessManager.writeUpdateConfigMessage("foo", "bar");
    }

    @Test
    public void testWriteUpdateConfigMessage_GivenJobRunningProcess()
            throws UnknownJobException, NativeProcessRunException, JobInUseException,
            JsonParseException, MissingFieldException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, MalformedJsonException
    {
        MockProcessBuilder mockProcessBuilder = new MockProcessBuilder();
        ProcessAndDataDescription process = mockProcessBuilder
                .withAvailableWriting()
                .withAvailableUpdating()
                .stillRunning(true)
                .build();
        when(m_ProcessFactory.createProcess("foo")).thenReturn(process);

        InputStream inputStream = createInputStream("");
        m_ProcessManager.processDataLoadJob("foo", inputStream, createNoPersistNoResetDataLoadParams());

        m_ProcessManager.writeUpdateConfigMessage("foo", "bar");

        String output = mockProcessBuilder.getOutput().trim();
        assertTrue(output.startsWith("ubar"));
        verify(process, times(2)).releaseGuard();
    }

    @Test
    public void testWriteUpdateConfigMessage_GivenJobRunningProcessThatIsStillInUse()
            throws UnknownJobException, NativeProcessRunException, JobInUseException,
            JsonParseException, MissingFieldException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, MalformedJsonException
    {
        m_ExpectedException.expect(JobInUseException.class);
        m_ExpectedException.expectMessage(
                "Cannot update job foo while another connection is writing to the job");

        MockProcessBuilder mockProcessBuilder = new MockProcessBuilder();
        ProcessAndDataDescription process = mockProcessBuilder
                .withAvailableWriting()
                .withAction(Action.WRITING)
                .stillRunning(true)
                .build();
        when(m_ProcessFactory.createProcess("foo")).thenReturn(process);

        InputStream inputStream = createInputStream("");
        m_ProcessManager.processDataLoadJob("foo", inputStream, createNoPersistNoResetDataLoadParams());

        m_ProcessManager.writeUpdateConfigMessage("foo", "bar");
    }

    @Test
    public void testAddAlertObserver_GivenRunningJob() throws UnknownJobException,
            NativeProcessRunException, JsonParseException, MissingFieldException,
            JobInUseException, HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            MalformedJsonException, ClosedJobException
    {
        MockProcessBuilder mockProcessBuilder = new MockProcessBuilder();
        ProcessAndDataDescription process = mockProcessBuilder
                .withAvailableWriting()
                .withAvailableClosing()
                .stillRunning(true)
                .build();
        when(m_ProcessFactory.createProcess("foo")).thenReturn(process);
        InputStream inputStream = createInputStream("");
        m_ProcessManager.processDataLoadJob("foo", inputStream, createNoPersistNoResetDataLoadParams());

        AlertObserver alertObserver = mock(AlertObserver.class);
        m_ProcessManager.addAlertObserver("foo", alertObserver);

        verify(process.getResultsReader()).addAlertObserver(alertObserver);
    }

    @Test
    public void testAddAlertObserver_GivenNonRunningJob() throws ClosedJobException
    {
        m_ExpectedException.expect(ClosedJobException.class);
        m_ProcessManager.addAlertObserver("nonRunning", null);
    }

    @Test
    public void testRemoveAlertObserver_GivenRunningJob() throws UnknownJobException,
            NativeProcessRunException, JsonParseException, MissingFieldException,
            JobInUseException, HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            MalformedJsonException, ClosedJobException
    {
        MockProcessBuilder mockProcessBuilder = new MockProcessBuilder();
        ProcessAndDataDescription process = mockProcessBuilder.withAvailableWriting()
                .withAvailableClosing().stillRunning(true).build();
        when(m_ProcessFactory.createProcess("foo")).thenReturn(process);
        InputStream inputStream = createInputStream("");
        m_ProcessManager.processDataLoadJob("foo", inputStream,
                createNoPersistNoResetDataLoadParams());

        AlertObserver alertObserver = mock(AlertObserver.class);
        when(process.getResultsReader().removeAlertObserver(alertObserver)).thenReturn(true);

        assertTrue(m_ProcessManager.removeAlertObserver("foo", alertObserver));

        verify(process.getResultsReader()).removeAlertObserver(alertObserver);
    }

    @Test
    public void testRemoveAlertObserver_GivenNonRunningJob()
    {
        assertFalse(m_ProcessManager.removeAlertObserver("nonRunning", null));
    }

    private static InputStream createInputStream(String input)
    {
        return new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    }

    private static InterimResultsParams createNoInterimResults()
    {
        return new InterimResultsParams(false, new TimeRange(null, null));
    }

    private static DataLoadParams createNoPersistNoResetDataLoadParams()
    {
        return new DataLoadParams(false, new TimeRange(null, null));
    }

    private static class MockProcessBuilder
    {
        private static final DataFormat DEFAULT_DATA_FORMAT = DataFormat.DELIMITED;
        private static final long DEFAULT_CLOSE_TIMEOUT = 60L;

        private final ProcessAndDataDescription m_Process;

        private final Logger m_Logger;
        private final Process m_JavaProcess;
        private final ByteArrayOutputStream m_Output;
        private final StatusReporter m_StatusReporter;
        private final AnalysisConfig m_AnalysisConfig;
        private final DataDescription m_DataDescription;
        private final TransformConfigs m_TransformConfigs;
        private final ResultsReader m_ResultsReader;

        MockProcessBuilder()
        {
            m_Process = mock(ProcessAndDataDescription.class);

            when(m_Process.getTimeout()).thenReturn(DEFAULT_CLOSE_TIMEOUT);

            m_JavaProcess = mock(Process.class);

            m_Output = new ByteArrayOutputStream();
            when(m_JavaProcess.getOutputStream()).thenReturn(m_Output);
            when(m_Process.getProcess()).thenReturn(m_JavaProcess);

            m_Logger = mock(Logger.class);
            when(m_Process.getLogger()).thenReturn(m_Logger);

            m_StatusReporter = mock(StatusReporter.class);
            when(m_StatusReporter.incrementalStats()).thenReturn(new DataCounts());
            when(m_Process.getStatusReporter()).thenReturn(m_StatusReporter);

            m_AnalysisConfig = new AnalysisConfig();
            when(m_Process.getAnalysisConfig()).thenReturn(m_AnalysisConfig);

            m_DataDescription = new DataDescription();
            m_DataDescription.setFormat(DEFAULT_DATA_FORMAT);
            when(m_Process.getDataDescription()).thenReturn(m_DataDescription);

            m_TransformConfigs = new TransformConfigs(Collections.emptyList());
            when(m_Process.getTransforms()).thenReturn(m_TransformConfigs);

            m_ResultsReader = mock(ResultsReader.class);
            when(m_Process.getResultsReader()).thenReturn(m_ResultsReader);
        }

        MockProcessBuilder withAvailableWriting()
        {
            when(m_Process.tryAcquireGuard(Action.WRITING)).thenReturn(true);
            return this;
        }

        MockProcessBuilder withAvailableFlushing()
        {
            when(m_Process.tryAcquireGuard(Action.FLUSHING)).thenReturn(true);
            return this;
        }

        MockProcessBuilder withAvailableUpdating()
        {
            when(m_Process.tryAcquireGuard(Action.UPDATING)).thenReturn(true);
            return this;
        }

        MockProcessBuilder withAvailableClosing()
        {
            when(m_Process.tryAcquireGuard(Action.CLOSING)).thenReturn(true);
            return this;
        }

        MockProcessBuilder withAction(Action action)
        {
            when(m_Process.getAction()).thenReturn(action);
            return this;
        }

        MockProcessBuilder stillRunning(Boolean isRunning)
        {
            if (isRunning)
            {
                doThrow(new IllegalThreadStateException()).when(m_JavaProcess).exitValue();
            }
            else
            {
                when(m_JavaProcess.exitValue()).thenReturn(0);
            }
            return this;
        }

        ProcessAndDataDescription build()
        {
            return m_Process;
        }

        String getOutput()
        {
            try
            {
                return m_Output.toString(Charsets.UTF_8.name());
            } catch (UnsupportedEncodingException e)
            {
                return "Could not convert to UTF-8";
            }
        }
    }
}
