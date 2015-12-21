/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.fasterxml.jackson.core.JsonParseException;
import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataCounts;
import com.prelert.job.Detector;
import com.prelert.job.JobConfiguration;
import com.prelert.job.JobDetails;
import com.prelert.job.JobIdAlreadyExistsException;
import com.prelert.job.JobStatus;
import com.prelert.job.ModelDebugConfig;
import com.prelert.job.UnknownJobException;
import com.prelert.job.config.verification.JobConfigurationException;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.exceptions.TooManyJobsException;
import com.prelert.job.messages.Messages;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.process.autodetect.ProcessManager;
import com.prelert.job.process.exceptions.MalformedJsonException;
import com.prelert.job.process.exceptions.MissingFieldException;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.process.params.DataLoadParams;
import com.prelert.job.process.writer.CsvRecordWriter;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;

public class JobManagerTest
{
    @Rule
    public ExpectedException m_ExpectedException = ExpectedException.none();

    @Captor
    private ArgumentCaptor<Map<String, Object>> m_JobUpdateCaptor;

    @Mock private JobProvider m_JobProvider;
    @Mock private ProcessManager m_ProcessManager;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown()
    {
        System.clearProperty("prelert.max.jobs.factor");
    }

    @Test
    public void testSubmitDataLoadJob_GivenProcessIsRunningAsManyJobsAsLicenseAllows()
            throws JsonParseException, UnknownJobException, NativeProcessRunException,
            MissingFieldException, JobInUseException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, TooManyJobsException, MalformedJsonException
    {
        when(m_JobProvider.getJobDetails("foo")).thenReturn(
                Optional.of(new JobDetails("foo", new JobConfiguration())));
        givenProcessInfo(2);
        when(m_ProcessManager.jobIsRunning("foo")).thenReturn(false);
        when(m_ProcessManager.numberOfRunningJobs()).thenReturn(2);
        JobManager jobManager = new JobManager(m_JobProvider, m_ProcessManager);

        m_ExpectedException.expect(TooManyJobsException.class);
        m_ExpectedException.expectMessage("Cannot reactivate job with id 'foo' - your license "
                + "limits you to 2 concurrently running jobs. You must close a job before you "
                + "can reactivate another.");


        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.LICENSE_VIOLATION));

        jobManager.submitDataLoadJob("foo", mock(InputStream.class), mock(DataLoadParams.class));
    }

    @Test
    public void testSubmitDataLoadJob_GivenDefaultFactorAndProcessIsRunningMoreJobsThanMaxAllowed()
            throws JsonParseException, UnknownJobException, NativeProcessRunException,
            MissingFieldException, JobInUseException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, TooManyJobsException, MalformedJsonException
    {
        when(m_JobProvider.getJobDetails("foo")).thenReturn(
                Optional.of(new JobDetails("foo", new JobConfiguration())));
        givenProcessInfo(5);
        when(m_ProcessManager.jobIsRunning("foo")).thenReturn(false);
        when(m_ProcessManager.numberOfRunningJobs()).thenReturn(10000);
        JobManager jobManager = new JobManager(m_JobProvider, m_ProcessManager);

        m_ExpectedException.expect(TooManyJobsException.class);
        m_ExpectedException.expectMessage("Cannot start job with id 'foo'. " +
                "The maximum number of concurrently running jobs is limited as a function " +
                "of the number of CPU cores see this error code's help documentation " +
                "for details of how to elevate the setting");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.TOO_MANY_JOBS_RUNNING_CONCURRENTLY));

        jobManager.submitDataLoadJob("foo", mock(InputStream.class), mock(DataLoadParams.class));
    }

    @Test
    public void testSubmitDataLoadJob_GivenSpecifiedFactorAndProcessIsRunningMoreJobsThanMaxAllowed()
            throws JsonParseException, UnknownJobException, NativeProcessRunException,
            MissingFieldException, JobInUseException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, TooManyJobsException, MalformedJsonException
    {
        System.setProperty("prelert.max.jobs.factor", "5.0");
        when(m_JobProvider.getJobDetails("foo")).thenReturn(
                Optional.of(new JobDetails("foo", new JobConfiguration())));
        givenProcessInfo(5);
        when(m_ProcessManager.jobIsRunning("foo")).thenReturn(false);
        when(m_ProcessManager.numberOfRunningJobs()).thenReturn(10000);
        JobManager jobManager = new JobManager(m_JobProvider, m_ProcessManager);

        m_ExpectedException.expect(TooManyJobsException.class);
        m_ExpectedException.expectMessage("Cannot start job with id 'foo'. " +
                "The maximum number of concurrently running jobs is limited as a function " +
                "of the number of CPU cores see this error code's help documentation " +
                "for details of how to elevate the setting");
        m_ExpectedException.expect(ErrorCodeMatcher
                .hasErrorCode(ErrorCodes.TOO_MANY_JOBS_RUNNING_CONCURRENTLY));

        jobManager.submitDataLoadJob("foo", mock(InputStream.class), mock(DataLoadParams.class));
    }

    @Test
    public void testSubmitDataLoadJob_GivenInvalidFactorAndProcessIsRunningMoreJobsThanMaxAllowed()
            throws JsonParseException, UnknownJobException, NativeProcessRunException,
            MissingFieldException, JobInUseException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, TooManyJobsException, MalformedJsonException
    {
        System.setProperty("prelert.max.jobs.factor", "invalid");
        when(m_JobProvider.getJobDetails("foo")).thenReturn(
                Optional.of(new JobDetails("foo", new JobConfiguration())));
        givenProcessInfo(5);
        when(m_ProcessManager.jobIsRunning("foo")).thenReturn(false);
        when(m_ProcessManager.numberOfRunningJobs()).thenReturn(10000);
        JobManager jobManager = new JobManager(m_JobProvider, m_ProcessManager);

        m_ExpectedException.expect(TooManyJobsException.class);
        m_ExpectedException.expectMessage("Cannot start job with id 'foo'. " +
                "The maximum number of concurrently running jobs is limited as a function " +
                "of the number of CPU cores see this error code's help documentation " +
                "for details of how to elevate the setting");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.TOO_MANY_JOBS_RUNNING_CONCURRENTLY));

        jobManager.submitDataLoadJob("foo", mock(InputStream.class), mock(DataLoadParams.class));
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testSubmitDataLoadJob_GivenProcessIsRunSuccessfully()
            throws JsonParseException, UnknownJobException, NativeProcessRunException,
            MissingFieldException, JobInUseException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, TooManyJobsException, MalformedJsonException
    {
        InputStream inputStream = mock(InputStream.class);
        DataLoadParams params = mock(DataLoadParams.class);
        when(m_JobProvider.getJobDetails("foo")).thenReturn(
                Optional.of(new JobDetails("foo", new JobConfiguration())));
        givenProcessInfo(5);
        when(m_ProcessManager.jobIsRunning("foo")).thenReturn(false);
        when(m_ProcessManager.numberOfRunningJobs()).thenReturn(0);
        when(m_ProcessManager.processDataLoadJob("foo", inputStream, params)).thenReturn(new DataCounts());
        JobManager jobManager = new JobManager(m_JobProvider, m_ProcessManager);

        DataCounts stats = jobManager.submitDataLoadJob("foo", inputStream, params);
        assertNotNull(stats);

        ArgumentCaptor<Map> updateCaptor = ArgumentCaptor.forClass(Map.class);
        verify(m_JobProvider).updateJob(eq("foo"), updateCaptor.capture());
        Map updates = updateCaptor.getValue();
        assertEquals(JobStatus.RUNNING, updates.get(JobDetails.STATUS));
        assertNotNull(updates.get(JobDetails.LAST_DATA_TIME));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSetModelDebugConfig_GivenConfig() throws UnknownJobException
    {
        givenProcessInfo(5);
        ModelDebugConfig config = new ModelDebugConfig(85.0, "bar");
        JobManager jobManager = new JobManager(m_JobProvider, m_ProcessManager);

        jobManager.setModelDebugConfig("foo", config);

        verify(m_JobProvider).updateJob(eq("foo"), m_JobUpdateCaptor.capture());
        Map<String, Object> jobUpdate = m_JobUpdateCaptor.getValue();
        Map<String, Object> configUpdate = (Map<String, Object>) jobUpdate.get("modelDebugConfig");
        assertEquals(85.0, configUpdate.get("boundsPercentile"));
        assertEquals("bar", configUpdate.get("terms"));
    }

    @Test
    public void testSetModelDebugConfig_GivenNull() throws UnknownJobException
    {
        givenProcessInfo(5);
        JobManager jobManager = new JobManager(m_JobProvider, m_ProcessManager);

        jobManager.setModelDebugConfig("foo", null);

        verify(m_JobProvider).updateJob(eq("foo"), m_JobUpdateCaptor.capture());
        Map<String, Object> jobUpdate = m_JobUpdateCaptor.getValue();
        assertNull(jobUpdate.get("modelDebugConfig"));
    }

    @Test
    public void testSetDesciption() throws UnknownJobException
    {
        givenProcessInfo(5);
        JobManager jobManager = new JobManager(m_JobProvider, m_ProcessManager);

        jobManager.setDescription("foo", "foo job");

        verify(m_JobProvider).updateJob(eq("foo"), m_JobUpdateCaptor.capture());
        Map<String, Object> jobUpdate = m_JobUpdateCaptor.getValue();
        assertEquals("foo job", jobUpdate.get(JobDetails.DESCRIPTION));
    }

    private void givenProcessInfo(int maxLicenseJobs)
    {
        String info = String.format("{\"jobs\":\"%d\"}", maxLicenseJobs);
        when(m_ProcessManager.getInfo()).thenReturn(info);
    }

    private void givenLicenseConstraints(int maxJobs, int maxDetectors, int maxPartitions)
    {
        String info = String.format("{\"%s\":%d, \"%s\":%d, \"%s\":%d}",
                                 JobManager.JOBS_LICENSE_CONSTRAINT, maxJobs,
                                 JobManager.DETECTORS_LICENSE_CONSTRAINT, maxDetectors,
                                 JobManager.PARTITIONS_LICENSE_CONSTRAINT, maxPartitions);

        when(m_ProcessManager.getInfo()).thenReturn(info);
    }

    @Test
    public void testGetJob() throws UnknownJobException
    {
        givenLicenseConstraints(2, 2, 0);
        JobManager jobManager = new JobManager(m_JobProvider, m_ProcessManager);
        when(m_JobProvider.getJobDetails("foo")).thenReturn(Optional.of(new JobDetails()));

        Optional<JobDetails> doc = jobManager.getJob("foo");
        assertTrue(doc.isPresent());
    }

    @Test
    public void createJob_licensingConstraintMaxJobs()
    throws UnknownJobException, JobConfigurationException, JobIdAlreadyExistsException, IOException
    {
        givenLicenseConstraints(2, 2, 0);
        when(m_ProcessManager.numberOfRunningJobs()).thenReturn(3);

        JobManager jobManager = new JobManager(m_JobProvider, m_ProcessManager);

        try
        {
            jobManager.createJob(new JobConfiguration());
            fail();
        }
        catch (TooManyJobsException e)
        {
            assertEquals(ErrorCodes.LICENSE_VIOLATION, e.getErrorCode());

            String message = Messages.getMessage(Messages.LICENSE_LIMIT_JOBS, 2);
            assertEquals(message, e.getMessage());
        }
    }

    @Test
    public void createJob_licensingConstraintMaxDetectors()
    throws UnknownJobException, JobIdAlreadyExistsException,
            IOException, TooManyJobsException
    {
        givenLicenseConstraints(5, 1, 0);
        when(m_ProcessManager.numberOfRunningJobs()).thenReturn(3);

        JobManager jobManager = new JobManager(m_JobProvider, m_ProcessManager);

        try
        {
            AnalysisConfig ac = new AnalysisConfig();
            // create 2 detectors
            ac.getDetectors().add(new Detector());
            ac.getDetectors().add(new Detector());
            jobManager.createJob(new JobConfiguration(ac));
            fail();
        }
        catch (JobConfigurationException e)
        {
            assertEquals(ErrorCodes.LICENSE_VIOLATION, e.getErrorCode());

            String message = Messages.getMessage(Messages.LICENSE_LIMIT_DETECTORS, 1, 2);
            assertEquals(message, e.getMessage());
        }
    }

    @Test
    public void createJob_licensingConstraintMaxPartitions()
    throws UnknownJobException, JobIdAlreadyExistsException,
            IOException, TooManyJobsException
    {
        givenLicenseConstraints(5, -1, 0);
        when(m_ProcessManager.numberOfRunningJobs()).thenReturn(3);

        JobManager jobManager = new JobManager(m_JobProvider, m_ProcessManager);

        try
        {
            AnalysisConfig ac = new AnalysisConfig();
            // create 2 detectors
            Detector d = new Detector();
            d.setPartitionFieldName("pfield");
            ac.getDetectors().add(d);
            jobManager.createJob(new JobConfiguration(ac));
            fail();
        }
        catch (JobConfigurationException e)
        {
            assertEquals(ErrorCodes.LICENSE_VIOLATION, e.getErrorCode());

            String message = Messages.getMessage(Messages.LICENSE_LIMIT_PARTITIONS);
            assertEquals(message, e.getMessage());
        }
    }

    @Test
    public void testWriteUpdateConfigMessage() throws JobInUseException, NativeProcessRunException
    {
        givenProcessInfo(5);
        JobManager jobManager = new JobManager(m_JobProvider, m_ProcessManager);

        jobManager.writeUpdateConfigMessage("foo", "bar");

        verify(m_ProcessManager).writeUpdateConfigMessage("foo", "bar");
    }

    @Test
    public void testPreviewTransforms()
    throws JsonParseException, MissingFieldException, HighProportionOfBadTimestampsException,
    OutOfOrderRecordsException, MalformedJsonException, IOException, UnknownJobException
    {
        givenLicenseConstraints(5, -1, 0);
        when(m_JobProvider.getJobDetails("foo")).thenReturn(
                Optional.of(new JobDetails("foo", new JobConfiguration(new AnalysisConfig()))));

        when(m_ProcessManager.numberOfRunningJobs()).thenReturn(3);
        when(m_ProcessManager.writeToJob(any(CsvRecordWriter.class), any(), any(), any(), any(), any(), any(), any()))
                    .thenAnswer(writeToWriter());

        JobManager jobManager = new JobManager(m_JobProvider, m_ProcessManager);
        String answer = jobManager.previewTransforms("foo", mock(InputStream.class));

        assertEquals("csv,header,one\n", answer);
    }

    private static Answer<Object> writeToWriter()
    {
        return new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws IOException
            {
                CsvRecordWriter writer = (CsvRecordWriter) invocation.getArguments()[0];
                writer.writeRecord(new String [] {"csv","header","one"});
                return null;
            }
        };
    }
}
