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

package com.prelert.job.process.autodetect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

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
import com.prelert.job.Detector;
import com.prelert.job.JobDetails;
import com.prelert.job.UnknownJobException;
import com.prelert.job.alert.AlertObserver;
import com.prelert.job.logging.JobLoggerFactory;
import com.prelert.job.persistence.DataPersisterFactory;
import com.prelert.job.persistence.JobDataCountsPersister;
import com.prelert.job.persistence.JobDataPersister;
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
import com.prelert.job.usage.UsageReporter;

public class ProcessManagerTest
{
    private static final String JOB_ID = "foo";

    @Rule public ExpectedException m_ExpectedException = ExpectedException.none();

    @Mock private ProcessFactory m_ProcessFactory;
    @Mock private JobProvider m_JobProvider;
    @Mock private DataPersisterFactory m_DataPersisterFactory;
    @Mock private JobLoggerFactory m_JobLoggerFactory;
    private JobDetails m_Job;

    private ProcessManager m_ProcessManager;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        m_ProcessManager = new ProcessManager(m_JobProvider, m_ProcessFactory,
                m_DataPersisterFactory, m_JobLoggerFactory);
        m_Job = new JobDetails();
        m_Job.setId(JOB_ID);
        when(m_JobProvider.getJobDetails(JOB_ID)).thenReturn(Optional.of(m_Job));
    }

    @Test
    public void testProcessDataLoadJob_GivenRunningProcess() throws UnknownJobException,
            NativeProcessRunException, JsonParseException, MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            MalformedJsonException
    {
        MockProcessBuilder mockProcessBuilder = new MockProcessBuilder();
        ProcessAndDataDescription process = mockProcessBuilder.stillRunning(true).build();
        when(m_ProcessFactory.createProcess(m_Job, false)).thenReturn(process);

        InputStream inputStream = createInputStream("time");
        m_ProcessManager.processDataLoadJob(m_Job, inputStream, createNoPersistNoResetDataLoadParams());

        assertTrue(m_ProcessManager.jobIsRunning(JOB_ID));
        String output = mockProcessBuilder.getOutput().trim();
        assertTrue(output.startsWith("time"));
    }

    @Test
    public void testFlush_GivenJobWithoutRunningProcess() throws NativeProcessRunException,
            UnknownJobException
    {
        m_ProcessManager.flushJob(JOB_ID, createNoInterimResults());
    }

    @Test
    public void testFlush_GivenJobWithRunningProcess() throws NativeProcessRunException,
            JsonParseException, UnknownJobException, MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            MalformedJsonException
    {
        MockProcessBuilder mockProcessBuilder = new MockProcessBuilder();
        ProcessAndDataDescription process = mockProcessBuilder.stillRunning(true).build();
        when(m_ProcessFactory.createProcess(m_Job, false)).thenReturn(process);

        InputStream inputStream = createInputStream("");
        m_ProcessManager.processDataLoadJob(m_Job, inputStream, createNoPersistNoResetDataLoadParams());

        m_ProcessManager.flushJob(JOB_ID, createNoInterimResults());

        String output = mockProcessBuilder.getOutput().trim();
        assertTrue(output.startsWith("f"));
    }

    @Test
    public void testClose_GivenJobWithRunningProcess() throws NativeProcessRunException,
            JsonParseException, UnknownJobException, MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            MalformedJsonException
    {
        MockProcessBuilder mockProcessBuilder = new MockProcessBuilder();
        ProcessAndDataDescription process = mockProcessBuilder.stillRunning(true).build();
        when(m_ProcessFactory.createProcess(m_Job, false)).thenReturn(process);

        InputStream inputStream = createInputStream("");
        m_ProcessManager.processDataLoadJob(m_Job, inputStream, createNoPersistNoResetDataLoadParams());
        assertTrue(m_ProcessManager.jobIsRunning(JOB_ID));
        assertEquals(1, m_ProcessManager.numberOfRunningJobs());
        assertEquals(1, m_ProcessManager.runningJobs().size());
        assertTrue(m_ProcessManager.runningJobs().contains(m_Job.getId()));

        m_ProcessManager.closeJob(JOB_ID);

        assertFalse(m_ProcessManager.jobIsRunning(JOB_ID));
        assertEquals(0, m_ProcessManager.numberOfRunningJobs());
        assertEquals(0, m_ProcessManager.runningJobs().size());
    }

    @Test
    public void testClose_GivenJobWithoutRunningProcess() throws NativeProcessRunException,
            JsonParseException, UnknownJobException, MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            MalformedJsonException
    {
        MockProcessBuilder mockProcessBuilder = new MockProcessBuilder();
        ProcessAndDataDescription process = mockProcessBuilder.stillRunning(false).build();
        when(m_ProcessFactory.createProcess(m_Job, false)).thenReturn(process);

        m_ProcessManager.closeJob(JOB_ID);

        assertEquals(0, m_ProcessManager.numberOfRunningJobs());
    }

    @Test
    public void testWriteUpdateConfigMessage_GivenJobWithoutProcess()
            throws NativeProcessRunException
    {
        m_ProcessManager.writeUpdateConfigMessage(JOB_ID, "bar");
    }

    @Test
    public void testWriteUpdateConfigMessage_GivenJobRunningProcess()
            throws UnknownJobException, NativeProcessRunException, JsonParseException,
            MissingFieldException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, MalformedJsonException
    {
        MockProcessBuilder mockProcessBuilder = new MockProcessBuilder();
        ProcessAndDataDescription process = mockProcessBuilder.stillRunning(true).build();
        when(m_ProcessFactory.createProcess(m_Job, false)).thenReturn(process);

        InputStream inputStream = createInputStream("");
        m_ProcessManager.processDataLoadJob(m_Job, inputStream, createNoPersistNoResetDataLoadParams());

        m_ProcessManager.writeUpdateConfigMessage(JOB_ID, "bar");

        String output = mockProcessBuilder.getOutput().trim();
        assertTrue(output.startsWith("ubar"));
    }

    @Test
    public void testAddAlertObserver_GivenRunningJob() throws UnknownJobException,
            NativeProcessRunException, JsonParseException, MissingFieldException,
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            MalformedJsonException, ClosedJobException
    {
        MockProcessBuilder mockProcessBuilder = new MockProcessBuilder();
        ProcessAndDataDescription process = mockProcessBuilder.stillRunning(true).build();
        when(m_ProcessFactory.createProcess(m_Job, false)).thenReturn(process);
        InputStream inputStream = createInputStream("");
        m_ProcessManager.processDataLoadJob(m_Job, inputStream, createNoPersistNoResetDataLoadParams());

        AlertObserver alertObserver = mock(AlertObserver.class);
        m_ProcessManager.addAlertObserver(JOB_ID, alertObserver);

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
            HighProportionOfBadTimestampsException, OutOfOrderRecordsException,
            MalformedJsonException, ClosedJobException
    {
        MockProcessBuilder mockProcessBuilder = new MockProcessBuilder();
        ProcessAndDataDescription process = mockProcessBuilder.stillRunning(true).build();
        when(m_ProcessFactory.createProcess(m_Job, false)).thenReturn(process);
        InputStream inputStream = createInputStream("");
        m_ProcessManager.processDataLoadJob(m_Job, inputStream,
                createNoPersistNoResetDataLoadParams());

        AlertObserver alertObserver = mock(AlertObserver.class);
        when(process.getResultsReader().removeAlertObserver(alertObserver)).thenReturn(true);

        assertTrue(m_ProcessManager.removeAlertObserver(JOB_ID, alertObserver));

        verify(process.getResultsReader()).removeAlertObserver(alertObserver);
    }

    @Test
    public void testRemoveAlertObserver_GivenNonRunningJob()
    {
        assertFalse(m_ProcessManager.removeAlertObserver("nonRunning", null));
    }

    @Test
    public void testWriteToJob()
    throws JsonParseException, MissingFieldException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, MalformedJsonException, IOException
    {
        DataDescription dd = new DataDescription();
        dd.setFormat(DataFormat.DELIMITED);
        dd.setFieldDelimiter(',');

        Detector d = new Detector();
        d.setFieldName("value");
        d.setFunction("metric");

        AnalysisConfig ac = new AnalysisConfig();
        ac.setDetectors(Arrays.asList(d));

        TransformConfigs tc = new TransformConfigs(Collections.emptyList());

        StatusReporter statusReporter = new StatusReporter(JOB_ID, mock(UsageReporter.class),
                                            mock(JobDataCountsPersister.class), mock(Logger.class));

        String data = "time,value\n1452095662,1\n1452098662,2\n1452098662,3";
        ByteArrayInputStream input = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream output = new ByteArrayOutputStream();


        DataCounts count = m_ProcessManager.writeToJob(dd, ac, null, tc, input, output, statusReporter,
                                    mock(JobDataPersister.class), mock(Logger.class));

        // the actual input is longer than 47 bytes but the counting input steam
        // knocks a couple off
        assertEquals(47, count.getInputBytes());
        assertEquals(3, count.getInputRecordCount());
        assertEquals(3, count.getInputFieldCount());
        assertEquals(3, count.getProcessedRecordCount());
        assertEquals(3, count.getProcessedFieldCount());
    }


    private static InputStream createInputStream(String input)
    {
        return new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    }

    private static InterimResultsParams createNoInterimResults()
    {
        return InterimResultsParams.newBuilder().build();
    }

    private static DataLoadParams createNoPersistNoResetDataLoadParams()
    {
        return new DataLoadParams(false, new TimeRange(null, null));
    }

    private static class MockProcessBuilder
    {
        private static final DataFormat DEFAULT_DATA_FORMAT = DataFormat.DELIMITED;

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
