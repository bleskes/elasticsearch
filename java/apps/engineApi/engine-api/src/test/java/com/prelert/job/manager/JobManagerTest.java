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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Stubber;

import com.fasterxml.jackson.core.JsonParseException;
import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataCounts;
import com.prelert.job.Detector;
import com.prelert.job.JobConfiguration;
import com.prelert.job.JobDetails;
import com.prelert.job.JobException;
import com.prelert.job.JobIdAlreadyExistsException;
import com.prelert.job.JobSchedulerStatus;
import com.prelert.job.ModelDebugConfig;
import com.prelert.job.ModelSnapshot;
import com.prelert.job.SchedulerConfig;
import com.prelert.job.SchedulerConfig.DataSource;
import com.prelert.job.SchedulerState;
import com.prelert.job.UnknownJobException;
import com.prelert.job.audit.Auditor;
import com.prelert.job.config.verification.JobConfigurationException;
import com.prelert.job.data.extraction.DataExtractor;
import com.prelert.job.data.extraction.DataExtractorFactory;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.exceptions.TooManyJobsException;
import com.prelert.job.logging.JobLoggerFactory;
import com.prelert.job.messages.Messages;
import com.prelert.job.persistence.DataStoreException;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.persistence.QueryPage;
import com.prelert.job.process.autodetect.ProcessManager;
import com.prelert.job.process.exceptions.MalformedJsonException;
import com.prelert.job.process.exceptions.MissingFieldException;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.process.params.DataLoadParams;
import com.prelert.job.process.params.InterimResultsParams;
import com.prelert.job.process.writer.CsvRecordWriter;
import com.prelert.job.scheduler.CannotStartSchedulerException;
import com.prelert.job.scheduler.CannotStopSchedulerException;
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
    @Mock private DataExtractorFactory m_DataExtractorFactory;
    @Mock private JobLoggerFactory m_JobLoggerFactory;
    @Mock private Auditor m_Auditor;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        when(m_JobProvider.jobIdIsUnique(anyString())).thenReturn(true);
        when(m_JobProvider.audit(anyString())).thenReturn(m_Auditor);
    }

    @After
    public void tearDown()
    {
        System.clearProperty("max.jobs.factor");
    }

    @Test
    public void testCloseJob_GivenExistingJob() throws UnknownJobException, DataStoreException,
            NativeProcessRunException, JobInUseException
    {
        givenProcessInfo(5);
        JobManager jobManager = createJobManager();

        jobManager.closeJob("foo");

        verify(m_ProcessManager).closeJob("foo");
    }

    @Test
    public void testCloseJob_GivenJobActionIsNotAvailable() throws NativeProcessRunException,
            InterruptedException, ExecutionException
    {
        givenProcessInfo(5);
        JobManager jobManager = createJobManager();
        doAnswerSleep(200).when(m_ProcessManager).closeJob("foo");

        ExecutorService executor = Executors.newFixedThreadPool(3);
        Future<Throwable> task_1_result = executor.submit(
                new ExceptionCallable(() -> jobManager.closeJob("foo")));
        Future<Throwable> task_2_result = executor.submit(
                new ExceptionCallable(() -> jobManager.closeJob("foo")));
        Future<Throwable> task_3_result = executor.submit(
                new ExceptionCallable(() -> jobManager.closeJob("bar")));
        executor.shutdown();

        Throwable result1 = task_1_result.get();
        Throwable result2 = task_2_result.get();
        Throwable result3 = task_3_result.get();
        assertTrue(result1 == null || result2 == null);
        assertNull(result3);
        if (result1 == null)
        {
            assertTrue(result2 instanceof JobInUseException);
        }
        else
        {
            assertTrue(result1 instanceof JobInUseException);
        }

        verify(m_ProcessManager).closeJob("foo");
        verify(m_ProcessManager).closeJob("bar");
    }

    @Test
    public void testDeleteJob_GivenNonRunningJob() throws UnknownJobException, DataStoreException,
            NativeProcessRunException, JobInUseException, CannotStopSchedulerException
    {
        givenProcessInfo(5);
        JobManager jobManager = createJobManager();
        when(m_ProcessManager.jobIsRunning("foo")).thenReturn(false);
        when(m_JobProvider.deleteJob("foo")).thenReturn(true);

        jobManager.deleteJob("foo");

        verify(m_ProcessManager, never()).closeJob("foo");
        verify(m_ProcessManager).deletePersistedData("foo");
        verify(m_JobProvider).deleteJob("foo");
        verify(m_JobProvider).audit("foo");
        verify(m_Auditor).info("Job deleted");
    }

    @Test
    public void testDeleteJob_GivenRunningJob() throws UnknownJobException, DataStoreException,
            NativeProcessRunException, JobInUseException, CannotStopSchedulerException
    {
        givenProcessInfo(5);
        JobManager jobManager = createJobManager();
        when(m_ProcessManager.jobIsRunning("foo")).thenReturn(true);

        jobManager.deleteJob("foo");

        verify(m_ProcessManager).closeJob("foo");
        verify(m_ProcessManager).deletePersistedData("foo");
        verify(m_JobProvider).deleteJob("foo");
    }

    @Test
    public void testDeleteJob_GivenScheduledJob()
            throws UnknownJobException, DataStoreException, NativeProcessRunException,
            JobInUseException, TooManyJobsException, JobConfigurationException,
            JobIdAlreadyExistsException, CannotStopSchedulerException
    {
        givenProcessInfo(5);
        JobManager jobManager = createJobManager();

        JobConfiguration jobConfig = createScheduledJobConfig();
        Logger jobLogger = mock(Logger.class);
        when(m_JobLoggerFactory.newLogger("foo")).thenReturn(jobLogger);
        DataExtractor dataExtractor = mock(DataExtractor.class);
        when(m_DataExtractorFactory.newExtractor(any(JobDetails.class))).thenReturn(dataExtractor);
        when(m_ProcessManager.jobIsRunning("foo")).thenReturn(true);

        jobManager.createJob(jobConfig);

        jobManager.deleteJob("foo");

        verify(m_ProcessManager).closeJob("foo");
        verify(m_ProcessManager).deletePersistedData("foo");
        verify(m_JobProvider).deleteJob("foo");
    }

    @Test
    public void testDeleteJob_GivenJobActionIsNotAvailable() throws UnknownJobException,
            DataStoreException, InterruptedException, ExecutionException
    {
        givenProcessInfo(5);
        JobManager jobManager = createJobManager();
        doAnswerSleep(200).when(m_JobProvider).deleteJob("foo");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<Throwable> task_1_result = executor.submit(
                new ExceptionCallable(() -> jobManager.deleteJob("foo")));
        Future<Throwable> task_2_result = executor.submit(
                new ExceptionCallable(() -> jobManager.deleteJob("foo")));
        executor.shutdown();

        Throwable result1 = task_1_result.get();
        Throwable result2 = task_2_result.get();
        assertTrue(result1 == null || result2 == null);
        if (result1 == null)
        {
            assertTrue(result2 instanceof JobInUseException);
        }
        else
        {
            assertTrue(result1 instanceof JobInUseException);
        }

        verify(m_JobProvider).deleteJob("foo");
    }

    @Test
    public void testFlushJob_GivenJobExists() throws UnknownJobException, NativeProcessRunException,
            JobInUseException
    {
        givenProcessInfo(5);
        JobManager jobManager = createJobManager();
        InterimResultsParams params = InterimResultsParams.newBuilder().build();

        jobManager.flushJob("foo", params);

        verify(m_JobProvider).checkJobExists("foo");
        verify(m_ProcessManager).flushJob("foo", params);
    }

    @Test
    public void testFlushJob_GivenJobActionIsNotAvailable() throws UnknownJobException,
            NativeProcessRunException, JobInUseException, InterruptedException, ExecutionException
    {
        givenProcessInfo(5);
        JobManager jobManager = createJobManager();
        InterimResultsParams params = InterimResultsParams.newBuilder().build();
        doAnswerSleep(200).when(m_ProcessManager).flushJob("foo", params);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<Throwable> task_1_result = executor.submit(
                new ExceptionCallable(() -> jobManager.flushJob("foo", params)));
        Future<Throwable> task_2_result = executor.submit(
                new ExceptionCallable(() -> jobManager.flushJob("foo", params)));
        executor.shutdown();

        Throwable result1 = task_1_result.get();
        Throwable result2 = task_2_result.get();
        assertTrue(result1 == null || result2 == null);
        if (result1 == null)
        {
            assertTrue(result2 instanceof JobInUseException);
        }
        else
        {
            assertTrue(result1 instanceof JobInUseException);
        }

        verify(m_ProcessManager).flushJob("foo", params);
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
        JobManager jobManager = createJobManager();

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
        JobManager jobManager = createJobManager();

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
        System.setProperty("max.jobs.factor", "5.0");
        when(m_JobProvider.getJobDetails("foo")).thenReturn(
                Optional.of(new JobDetails("foo", new JobConfiguration())));
        givenProcessInfo(5);
        when(m_ProcessManager.jobIsRunning("foo")).thenReturn(false);
        when(m_ProcessManager.numberOfRunningJobs()).thenReturn(10000);
        JobManager jobManager = createJobManager();

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
        System.setProperty("max.jobs.factor", "invalid");
        when(m_JobProvider.getJobDetails("foo")).thenReturn(
                Optional.of(new JobDetails("foo", new JobConfiguration())));
        givenProcessInfo(5);
        when(m_ProcessManager.jobIsRunning("foo")).thenReturn(false);
        when(m_ProcessManager.numberOfRunningJobs()).thenReturn(10000);
        JobManager jobManager = createJobManager();

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
    public void testSubmitDataLoadJob_GivenProcessIsRunSuccessfully()
            throws JsonParseException, UnknownJobException, NativeProcessRunException,
            MissingFieldException, JobInUseException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, TooManyJobsException, MalformedJsonException
    {
        InputStream inputStream = mock(InputStream.class);
        DataLoadParams params = mock(DataLoadParams.class);
        JobDetails job = new JobDetails("foo", new JobConfiguration());
        when(m_JobProvider.getJobDetails("foo")).thenReturn(Optional.of(job));
        givenProcessInfo(5);
        when(m_ProcessManager.jobIsRunning("foo")).thenReturn(false);
        when(m_ProcessManager.numberOfRunningJobs()).thenReturn(0);
        when(m_ProcessManager.processDataLoadJob(job, inputStream, params)).thenReturn(new DataCounts());
        JobManager jobManager = createJobManager();

        DataCounts stats = jobManager.submitDataLoadJob("foo", inputStream, params);
        assertNotNull(stats);

        verify(m_JobProvider).updateJob(eq("foo"), m_JobUpdateCaptor.capture());
        Map<String, Object> updates = m_JobUpdateCaptor.getValue();
        assertNotNull(updates.get(JobDetails.LAST_DATA_TIME));
    }

    @Test
    public void testSubmitDataLoadJob_GivenProcessIsRunSuccessfullyAndJobShouldIgnoreDowntimeAndPositiveProcessedRecordCount()
            throws JsonParseException, UnknownJobException, NativeProcessRunException,
            MissingFieldException, JobInUseException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, TooManyJobsException, MalformedJsonException
    {
        InputStream inputStream = mock(InputStream.class);
        DataLoadParams params = mock(DataLoadParams.class);
        JobDetails job = new JobDetails("foo", new JobConfiguration());
        job.setIgnoreDowntime(true);
        when(m_JobProvider.getJobDetails("foo")).thenReturn(Optional.of(job));
        givenProcessInfo(5);
        when(m_ProcessManager.jobIsRunning("foo")).thenReturn(false);
        when(m_ProcessManager.numberOfRunningJobs()).thenReturn(0);
        DataCounts counts = new DataCounts();
        counts.setProcessedRecordCount(1L);
        when(m_ProcessManager.processDataLoadJob(job, inputStream, params)).thenReturn(counts);
        JobManager jobManager = createJobManager();

        DataCounts actualCounts = jobManager.submitDataLoadJob("foo", inputStream, params);
        assertEquals(actualCounts, counts);

        verify(m_JobProvider, times(2)).updateJob(eq("foo"), m_JobUpdateCaptor.capture());
        assertNotNull(m_JobUpdateCaptor.getAllValues().get(0).get(JobDetails.LAST_DATA_TIME));
        assertTrue(m_JobUpdateCaptor.getAllValues().get(1).containsKey(JobDetails.IGNORE_DOWNTIME));
        assertNull(m_JobUpdateCaptor.getAllValues().get(1).get(JobDetails.IGNORE_DOWNTIME));
    }

    @Test
    public void testSubmitDataLoadJob_GivenProcessIsRunSuccessfullyAndJobShouldIgnoreDowntimeAndZeroProcessedRecordCount()
            throws JsonParseException, UnknownJobException, NativeProcessRunException,
            MissingFieldException, JobInUseException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, TooManyJobsException, MalformedJsonException
    {
        InputStream inputStream = mock(InputStream.class);
        DataLoadParams params = mock(DataLoadParams.class);
        JobDetails job = new JobDetails("foo", new JobConfiguration());
        job.setIgnoreDowntime(true);
        when(m_JobProvider.getJobDetails("foo")).thenReturn(Optional.of(job));
        givenProcessInfo(5);
        when(m_ProcessManager.jobIsRunning("foo")).thenReturn(false);
        when(m_ProcessManager.numberOfRunningJobs()).thenReturn(0);
        DataCounts counts = new DataCounts();
        counts.setProcessedRecordCount(0L);
        when(m_ProcessManager.processDataLoadJob(job, inputStream, params)).thenReturn(counts);
        JobManager jobManager = createJobManager();

        DataCounts actualCounts = jobManager.submitDataLoadJob("foo", inputStream, params);
        assertEquals(actualCounts, counts);

        verify(m_JobProvider).updateJob(eq("foo"), m_JobUpdateCaptor.capture());
        assertNotNull(m_JobUpdateCaptor.getValue().get(JobDetails.LAST_DATA_TIME));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSetModelDebugConfig_GivenConfig() throws UnknownJobException
    {
        givenProcessInfo(5);
        ModelDebugConfig config = new ModelDebugConfig(85.0, "bar");
        JobManager jobManager = createJobManager();

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
        JobManager jobManager = createJobManager();

        jobManager.setModelDebugConfig("foo", null);

        verify(m_JobProvider).updateJob(eq("foo"), m_JobUpdateCaptor.capture());
        Map<String, Object> jobUpdate = m_JobUpdateCaptor.getValue();
        assertNull(jobUpdate.get("modelDebugConfig"));
    }

    @Test
    public void testSetDesciption() throws UnknownJobException
    {
        givenProcessInfo(5);
        JobManager jobManager = createJobManager();

        jobManager.setDescription("foo", "foo job");

        verify(m_JobProvider).updateJob(eq("foo"), m_JobUpdateCaptor.capture());
        Map<String, Object> jobUpdate = m_JobUpdateCaptor.getValue();
        assertEquals("foo job", jobUpdate.get(JobDetails.DESCRIPTION));
    }

    @Test
    public void testSetRenormalizationWindow() throws UnknownJobException
    {
        givenProcessInfo(5);
        JobManager jobManager = createJobManager();

        jobManager.setRenormalizationWindow("foo", 7L);

        verify(m_JobProvider).updateJob(eq("foo"), m_JobUpdateCaptor.capture());
        Map<String, Object> jobUpdate = m_JobUpdateCaptor.getValue();
        assertEquals(new Long(7), jobUpdate.get(JobDetails.RENORMALIZATION_WINDOW));
    }

    @Test
    public void testSetModelSnapshotRetentionDays() throws UnknownJobException
    {
        givenProcessInfo(5);
        JobManager jobManager = createJobManager();

        jobManager.setModelSnapshotRetentionDays("foo", 20L);

        verify(m_JobProvider).updateJob(eq("foo"), m_JobUpdateCaptor.capture());
        Map<String, Object> jobUpdate = m_JobUpdateCaptor.getValue();
        assertEquals(new Long(20), jobUpdate.get(JobDetails.MODEL_SNAPSHOT_RETENTION_DAYS));
    }

    @Test
    public void testSetResultsRetentionDays() throws UnknownJobException
    {
        givenProcessInfo(5);
        JobManager jobManager = createJobManager();

        jobManager.setResultsRetentionDays("foo", 90L);

        verify(m_JobProvider).updateJob(eq("foo"), m_JobUpdateCaptor.capture());
        Map<String, Object> jobUpdate = m_JobUpdateCaptor.getValue();
        assertEquals(new Long(90), jobUpdate.get(JobDetails.RESULTS_RETENTION_DAYS));
    }

    @Test
    public void testGetJob() throws UnknownJobException
    {
        givenLicenseConstraints(2, 2, 0);
        JobManager jobManager = createJobManager();
        when(m_JobProvider.getJobDetails("foo")).thenReturn(Optional.of(new JobDetails()));

        Optional<JobDetails> doc = jobManager.getJob("foo");
        assertTrue(doc.isPresent());
    }

    @Test
    public void createJob_licensingConstraintMaxJobs() throws UnknownJobException,
            JobConfigurationException, JobIdAlreadyExistsException, IOException,
            CannotStartSchedulerException
    {
        givenLicenseConstraints(2, 2, 0);
        when(m_ProcessManager.numberOfRunningJobs()).thenReturn(3);

        JobManager jobManager = createJobManager();

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
            IOException, TooManyJobsException, CannotStartSchedulerException
    {
        givenLicenseConstraints(5, 1, 0);
        when(m_ProcessManager.numberOfRunningJobs()).thenReturn(3);

        JobManager jobManager = createJobManager();

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
    public void createJob_licensingConstraintMaxPartitions() throws UnknownJobException,
            JobIdAlreadyExistsException, IOException, TooManyJobsException,
            CannotStartSchedulerException
    {
        givenLicenseConstraints(5, -1, 0);
        when(m_ProcessManager.numberOfRunningJobs()).thenReturn(3);

        JobManager jobManager = createJobManager();

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
    public void testCreateJob_FillsDefaults()
            throws NoSuchScheduledJobException, UnknownJobException,
            CannotStartSchedulerException, TooManyJobsException,
            JobConfigurationException, JobIdAlreadyExistsException, IOException,
            NativeProcessRunException, JobInUseException
    {
        givenProcessInfo(2);
        JobManager jobManager = createJobManager();

        AnalysisConfig analysisConfig = new AnalysisConfig();
        analysisConfig.setBucketSpan(3600L);
        Detector detectorNullDescription = new Detector();
        detectorNullDescription.setFunction("sum");
        detectorNullDescription.setFieldName("revenue");
        detectorNullDescription.setByFieldName("vendor");
        Detector detectorWithDescription = new Detector();
        detectorWithDescription.setDetectorDescription("Named");
        detectorWithDescription.setFunction("sum");
        detectorWithDescription.setFieldName("revenue");
        detectorWithDescription.setByFieldName("vendor");

        analysisConfig.setDetectors(Arrays.asList(detectorNullDescription, detectorWithDescription));

        JobConfiguration jobConfig = new JobConfiguration();
        jobConfig.setId("revenue-by-vendor");
        jobConfig.setAnalysisConfig(analysisConfig);

        JobDetails job = jobManager.createJob(jobConfig);

        assertEquals("sum(revenue) by vendor", job.getAnalysisConfig().getDetectors().get(0).getDetectorDescription());
        assertEquals("Named", job.getAnalysisConfig().getDetectors().get(1).getDetectorDescription());
        verify(m_JobProvider).audit("revenue-by-vendor");
        verify(m_Auditor).info("Job created");
    }

    @Test
    public void testWriteUpdateConfigMessage() throws JobInUseException, NativeProcessRunException
    {
        givenProcessInfo(5);
        JobManager jobManager = createJobManager();

        jobManager.writeUpdateConfigMessage("foo", "bar");

        verify(m_ProcessManager).writeUpdateConfigMessage("foo", "bar");
    }

    @Test
    public void testWriteUpdateConfigMessage_GivenJobActionIsNotAvailable()
            throws NativeProcessRunException, InterruptedException, ExecutionException
    {
        givenProcessInfo(5);
        JobManager jobManager = createJobManager();
        doAnswerSleep(200).when(m_ProcessManager).writeUpdateConfigMessage("foo", "bar");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<Throwable> task_1_result = executor.submit(
                new ExceptionCallable(() -> jobManager.writeUpdateConfigMessage("foo", "bar")));
        Future<Throwable> task_2_result = executor.submit(
                new ExceptionCallable(() -> jobManager.writeUpdateConfigMessage("foo", "bar")));
        executor.shutdown();

        Throwable result1 = task_1_result.get();
        Throwable result2 = task_2_result.get();
        assertTrue(result1 == null || result2 == null);
        if (result1 == null)
        {
            assertTrue(result2 instanceof JobInUseException);
        }
        else
        {
            assertTrue(result1 instanceof JobInUseException);
        }

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
        when(m_ProcessManager.writeToJob(any(CsvRecordWriter.class), any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenAnswer(writeToWriter());

        JobManager jobManager = createJobManager();
        String answer = jobManager.previewTransforms("foo", mock(InputStream.class));

        assertEquals("csv,header,one\n", answer);
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testUpdateLastDataTime() throws UnknownJobException
    {
        givenProcessInfo(2);
        JobManager jobManager = createJobManager();

        jobManager.updateLastDataTime("foo", new Date(1450790609000L));

        ArgumentCaptor<Map> updateCaptor = ArgumentCaptor.forClass(Map.class);
        verify(m_JobProvider, times(1)).updateJob(eq("foo"), updateCaptor.capture());
        Map updates = updateCaptor.getValue();
        Date update = (Date) updates.get(JobDetails.LAST_DATA_TIME);
        assertNotNull(update);
        assertEquals(1450790609000L, update.getTime());

        // Check immediate call doesn't do anything
        jobManager.updateLastDataTime("foo", new Date(1450790609000L));
    }

    @Test
    public void testStartJobScheduler_GivenNoScheduledJob() throws JobException
    {
        givenProcessInfo(2);
        JobManager jobManager = createJobManager();

        m_ExpectedException.expect(NoSuchScheduledJobException.class);
        m_ExpectedException.expectMessage("There is no job 'foo' with a scheduler configured");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.NO_SUCH_SCHEDULED_JOB));

        jobManager.startJobScheduler("foo", 0, OptionalLong.empty());
    }

    @Test
    public void testStartJobScheduler_GivenNewlyCreatedJob() throws UnknownJobException,
            TooManyJobsException, JobConfigurationException, JobIdAlreadyExistsException,
            CannotStartSchedulerException, IOException, NoSuchScheduledJobException,
            CannotStopSchedulerException, NativeProcessRunException, JobInUseException
    {
        givenProcessInfo(2);
        JobManager jobManager = createJobManager();
        JobConfiguration jobConfig = createScheduledJobConfig();

        Logger jobLogger = mock(Logger.class);
        when(m_JobLoggerFactory.newLogger("foo")).thenReturn(jobLogger);
        DataExtractor dataExtractor = mock(DataExtractor.class);
        when(m_DataExtractorFactory.newExtractor(any(JobDetails.class))).thenReturn(dataExtractor);

        JobDetails job = jobManager.createJob(jobConfig);
        DataCounts dataCounts = new DataCounts();
        dataCounts.setLatestRecordTimeStamp(new Date(0));
        job.setCounts(dataCounts);
        when(m_JobProvider.getJobDetails("foo")).thenReturn(Optional.of(job));

        jobManager.startJobScheduler("foo", 0, OptionalLong.empty());

        jobManager.stopJobScheduler("foo");

        verify(dataExtractor).newSearch(anyString(), anyString(), eq(jobLogger));
        verify(m_ProcessManager).closeJob("foo");
        verify(m_JobProvider).updateSchedulerState("foo", new SchedulerState(0L, null));
    }

    @Test
    public void testStopScheduledJob_GivenNoScheduledJob()
            throws NoSuchScheduledJobException, CannotStopSchedulerException, UnknownJobException,
            NativeProcessRunException, JobInUseException
    {
        givenProcessInfo(2);
        JobManager jobManager = createJobManager();

        m_ExpectedException.expect(NoSuchScheduledJobException.class);
        m_ExpectedException.expectMessage("There is no job 'foo' with a scheduler configured");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.NO_SUCH_SCHEDULED_JOB));

        jobManager.stopJobScheduler("foo");
    }

    @Test
    public void testRestartScheduledJobs_GivenNonScheduledJobAndJobWithStartedScheduler()
            throws NoSuchScheduledJobException, UnknownJobException,
            CannotStartSchedulerException, TooManyJobsException,
            JobConfigurationException, JobIdAlreadyExistsException, IOException, InterruptedException
    {
        JobDetails nonScheduledJob = new JobDetails("non-scheduled", new JobConfiguration());

        JobConfiguration jobConfig = createScheduledJobConfig();
        JobDetails scheduledJob = new JobDetails("scheduled", jobConfig);
        DataCounts dataCounts = new DataCounts();
        dataCounts.setLatestRecordTimeStamp(new Date(0));
        scheduledJob.setCounts(dataCounts);
        scheduledJob.setSchedulerStatus(JobSchedulerStatus.STARTED);
        SchedulerState schedulerState = new SchedulerState();
        schedulerState.setStartTimeMillis(0L);
        schedulerState.setEndTimeMillis(null);

        QueryPage<JobDetails> jobsPage = new QueryPage<>(
                Arrays.asList(nonScheduledJob, scheduledJob), 2);
        when(m_JobProvider.getJobs(0, 10000)).thenReturn(jobsPage);
        when(m_JobProvider.getSchedulerState("scheduled")).thenReturn(Optional.of(schedulerState));

        Logger jobLogger = mock(Logger.class);
        when(m_JobLoggerFactory.newLogger("scheduled")).thenReturn(jobLogger);
        DataExtractor dataExtractor = mock(DataExtractor.class);
        when(m_DataExtractorFactory.newExtractor(any(JobDetails.class))).thenReturn(dataExtractor);

        givenProcessInfo(2);
        JobManager jobManager = createJobManager();

        jobManager.restartScheduledJobs();

        Thread.sleep(200);

        verify(m_JobLoggerFactory).newLogger("scheduled");
        verify(m_DataExtractorFactory).newExtractor(scheduledJob);
        verify(dataExtractor).newSearch(anyString(), anyString(), eq(jobLogger));
        jobManager.shutdown();

        verify(m_JobLoggerFactory).close("scheduled", jobLogger);
        // Verify no other calls to factories - means no other job was scheduled
        Mockito.verifyNoMoreInteractions(m_JobLoggerFactory, m_DataExtractorFactory);
    }

    @Test
    public void testRestartScheduledJobs_GivenJobWithStoppedScheduler() throws NoSuchScheduledJobException, UnknownJobException,
            CannotStartSchedulerException, TooManyJobsException,
            JobConfigurationException, JobIdAlreadyExistsException, IOException, InterruptedException
    {
        JobConfiguration jobConfig = createScheduledJobConfig();
        JobDetails scheduledJob = new JobDetails("scheduled", jobConfig);
        DataCounts dataCounts = new DataCounts();
        dataCounts.setLatestRecordTimeStamp(new Date(0));
        scheduledJob.setCounts(dataCounts);
        scheduledJob.setSchedulerStatus(JobSchedulerStatus.STOPPED);

        QueryPage<JobDetails> jobsPage = new QueryPage<>(Arrays.asList(scheduledJob), 1);
        when(m_JobProvider.getJobs(0, 10000)).thenReturn(jobsPage);

        DataExtractor dataExtractor = mock(DataExtractor.class);
        when(m_DataExtractorFactory.newExtractor(any(JobDetails.class))).thenReturn(dataExtractor);

        givenProcessInfo(2);
        JobManager jobManager = createJobManager();

        jobManager.restartScheduledJobs();

        verify(m_DataExtractorFactory).newExtractor(scheduledJob);
        jobManager.checkJobHasScheduler("scheduled");

        jobManager.shutdown();

        // Verify no other calls to factories - means no other job was scheduled
        Mockito.verifyNoMoreInteractions(m_JobLoggerFactory, m_DataExtractorFactory);
    }

    @Test
    public void testUpdateCustomSettings() throws UnknownJobException
    {
        givenProcessInfo(2);
        JobManager jobManager = createJobManager();
        Map<String, Object> customSettings = new HashMap<>();
        customSettings.put("answer", 42);

        jobManager.updateCustomSettings("foo", customSettings);

        verify(m_JobProvider).updateJob(eq("foo"), m_JobUpdateCaptor.capture());
        Map<String, Object> jobUpdate = m_JobUpdateCaptor.getValue();
        assertEquals(customSettings, jobUpdate.get(JobDetails.CUSTOM_SETTINGS));
    }

    @Test
    public void testRevertModelSnapshot_GivenSnapshotExists()
            throws JobInUseException, UnknownJobException, NoSuchModelSnapshotException
    {
        givenProcessInfo(2);
        JobManager jobManager = createJobManager();

        ModelSnapshot modelSnapshot = new ModelSnapshot();
        modelSnapshot.setDescription("my description");
        modelSnapshot.setRestorePriority(1);

        QueryPage<ModelSnapshot> modelSnapshotPage = new QueryPage<>(Arrays.asList(modelSnapshot), 1);
        when(m_JobProvider.modelSnapshots("foo", 0, 1, 0, 0, ModelSnapshot.TIMESTAMP, "my description")).thenReturn(modelSnapshotPage);
        when(m_JobProvider.updateModelSnapshot("foo", modelSnapshot)).thenReturn(true);

        assertTrue(jobManager.revertToSnapshot("foo", 0, "my description"));
        assertTrue(modelSnapshot.getRestorePriority() > 1);

        verify(m_JobProvider).updateJob(eq("foo"), m_JobUpdateCaptor.capture());
        assertTrue(m_JobUpdateCaptor.getValue().containsKey(JobDetails.IGNORE_DOWNTIME));
        assertEquals(Boolean.TRUE, m_JobUpdateCaptor.getValue().get(JobDetails.IGNORE_DOWNTIME));
    }

    @Test
    public void testRevertModelSnapshot_GivenSnapshotDoesNotExist()
            throws JobInUseException, UnknownJobException, NoSuchModelSnapshotException
    {
        givenProcessInfo(2);
        JobManager jobManager = createJobManager();

        ModelSnapshot modelSnapshot = new ModelSnapshot();
        modelSnapshot.setDescription("my description");
        modelSnapshot.setRestorePriority(1);

        QueryPage<ModelSnapshot> modelSnapshotPage = new QueryPage<>(null, 0);
        when(m_JobProvider.modelSnapshots("foo", 0, 1, 0, 0, ModelSnapshot.TIMESTAMP, "my description")).thenReturn(modelSnapshotPage);

        m_ExpectedException.expect(NoSuchModelSnapshotException.class);
        m_ExpectedException.expectMessage("No matching model snapshot exists for job 'foo'");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.NO_SUCH_MODEL_SNAPSHOT));

        jobManager.revertToSnapshot("foo", 0, "my description");

        verify(m_JobProvider, never()).updateJob(eq("foo"), anyMapOf(String.class, Object.class));
    }

    @Test
    public void updateModelSnapshotDescription_GivenValidOldAndNew()
            throws JobInUseException, UnknownJobException, NoSuchModelSnapshotException, DescriptionAlreadyUsedException
    {
        givenProcessInfo(2);
        JobManager jobManager = createJobManager();

        ModelSnapshot oldModelSnapshot = new ModelSnapshot();
        oldModelSnapshot.setDescription("old description");

        QueryPage<ModelSnapshot> oldModelSnapshotPage = new QueryPage<>(Arrays.asList(oldModelSnapshot), 1);
        QueryPage<ModelSnapshot> clashingModelSnapshotPage = new QueryPage<>(null, 0);
        when(m_JobProvider.modelSnapshots("foo", 0, 1, 0, 0, null, "old description")).thenReturn(oldModelSnapshotPage);
        when(m_JobProvider.modelSnapshots("foo", 0, 1, 0, 0, null, "new description")).thenReturn(clashingModelSnapshotPage);
        when(m_JobProvider.updateModelSnapshot("foo", oldModelSnapshot)).thenReturn(true);

        assertTrue(jobManager.updateModelSnapshotDescription("foo", "old description", "new description"));
        assertEquals("new description", oldModelSnapshot.getDescription());
    }

    @Test
    public void updateModelSnapshotDescription_GivenOldDescriptionNotFound()
            throws JobInUseException, UnknownJobException, NoSuchModelSnapshotException, DescriptionAlreadyUsedException
    {
        givenProcessInfo(2);
        JobManager jobManager = createJobManager();

        QueryPage<ModelSnapshot> oldModelSnapshotPage = new QueryPage<>(null, 0);
        when(m_JobProvider.modelSnapshots("foo", 0, 1, 0, 0, null, "old description")).thenReturn(oldModelSnapshotPage);

        m_ExpectedException.expect(NoSuchModelSnapshotException.class);
        m_ExpectedException.expectMessage("No matching model snapshot exists for job 'foo'");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.NO_SUCH_MODEL_SNAPSHOT));

        jobManager.updateModelSnapshotDescription("foo", "old description", "new description");
    }

    @Test
    public void updateModelSnapshotDescription_GivenClashingOldAndNew()
            throws JobInUseException, UnknownJobException, NoSuchModelSnapshotException, DescriptionAlreadyUsedException
    {
        givenProcessInfo(2);
        JobManager jobManager = createJobManager();

        ModelSnapshot oldModelSnapshot = new ModelSnapshot();
        oldModelSnapshot.setDescription("old description");
        ModelSnapshot clashingModelSnapshot = new ModelSnapshot();
        clashingModelSnapshot.setDescription("new description");

        QueryPage<ModelSnapshot> oldModelSnapshotPage = new QueryPage<>(Arrays.asList(oldModelSnapshot), 1);
        QueryPage<ModelSnapshot> clashingModelSnapshotPage = new QueryPage<>(Arrays.asList(clashingModelSnapshot), 1);
        when(m_JobProvider.modelSnapshots("foo", 0, 1, 0, 0, null, "old description")).thenReturn(oldModelSnapshotPage);
        when(m_JobProvider.modelSnapshots("foo", 0, 1, 0, 0, null, "new description")).thenReturn(clashingModelSnapshotPage);

        m_ExpectedException.expect(DescriptionAlreadyUsedException.class);
        m_ExpectedException.expectMessage("Model snapshot description 'new description' has already been used for job 'foo'");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.DESCRIPTION_ALREADY_USED));

        jobManager.updateModelSnapshotDescription("foo", "old description", "new description");
    }

    @Test
    public void testUpdateDetectorDescription() throws UnknownJobException
    {
        givenProcessInfo(2);
        JobManager jobManager = createJobManager();

        jobManager.updateDetectorDescription("foo", 1, "bar");

        verify(m_JobProvider).updateDetectorDescription("foo", 1, "bar");
    }

    @Test
    public void testShutdown()
    {
        givenProcessInfo(2);
        JobManager jobManager = createJobManager();

        jobManager.shutdown();

        verify(m_JobProvider).audit("");
        verify(m_Auditor).info("System shut down");
    }

    @Test
    public void testSetIgnoreDowntimeToAllJobs() throws UnknownJobException
    {
        DataCounts countsWithPositiveProcessedRecordCount = new DataCounts();
        countsWithPositiveProcessedRecordCount.setProcessedRecordCount(1L);

        JobDetails jobThatHasSeenData = new JobDetails();
        jobThatHasSeenData.setId("jobThatHasSeenData");
        jobThatHasSeenData.setCounts(countsWithPositiveProcessedRecordCount);

        JobDetails jobThatHasNotSeenData = new JobDetails();
        jobThatHasNotSeenData.setId("jobThatHasNotSeenData");
        jobThatHasNotSeenData.setCounts(new DataCounts());

        JobDetails jobWithNullCounts = new JobDetails();
        jobWithNullCounts.setId("jobWithNullCounts");
        jobWithNullCounts.setCounts(null);

        JobDetails jobThatIsUnknown = new JobDetails();
        jobThatIsUnknown.setId("jobThatIsUnknown");
        jobThatIsUnknown.setCounts(countsWithPositiveProcessedRecordCount);
        jobThatIsUnknown.setIgnoreDowntime(true);
        when(m_JobProvider.updateJob(eq("jobThatIsUnknown"), anyMapOf(String.class, Object.class)))
                .thenThrow(new UnknownJobException("jobThatIsUnknown"));

        QueryPage<JobDetails> jobsPage = new QueryPage<>(Arrays.asList(jobThatHasSeenData,
                jobThatHasNotSeenData, jobWithNullCounts, jobThatIsUnknown), 4);
        when(m_JobProvider.getJobs(0, 10000)).thenReturn(jobsPage);

        givenProcessInfo(2);
        JobManager jobManager = createJobManager();

        jobManager.setIgnoreDowntimeToAllJobs();

        verify(m_JobProvider).updateJob(eq("jobThatHasSeenData"), m_JobUpdateCaptor.capture());
        assertTrue(m_JobUpdateCaptor.getValue().containsKey(JobDetails.IGNORE_DOWNTIME));
        assertEquals(Boolean.TRUE, m_JobUpdateCaptor.getValue().get(JobDetails.IGNORE_DOWNTIME));

        verify(m_JobProvider, never()).updateJob(eq("jobThatHasNotSeenData"), anyMapOf(String.class, Object.class));
        verify(m_JobProvider, never()).updateJob(eq("jobWithNullCounts"), anyMapOf(String.class, Object.class));

        verify(m_JobProvider).updateJob(eq("jobThatIsUnknown"), m_JobUpdateCaptor.capture());
        assertTrue(m_JobUpdateCaptor.getValue().containsKey(JobDetails.IGNORE_DOWNTIME));
        assertEquals(Boolean.TRUE, m_JobUpdateCaptor.getValue().get(JobDetails.IGNORE_DOWNTIME));
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

    private JobManager createJobManager()
    {
        return new JobManager(m_JobProvider, m_ProcessManager, m_DataExtractorFactory,
                m_JobLoggerFactory);
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

    private static JobConfiguration createScheduledJobConfig()
    {
        AnalysisConfig analysisConfig = new AnalysisConfig();
        analysisConfig.setBucketSpan(3600L);

        SchedulerConfig schedulerConfig = new SchedulerConfig();
        schedulerConfig.setDataSource(DataSource.ELASTICSEARCH);
        schedulerConfig.setBaseUrl("http://localhost");

        JobConfiguration jobConfig = new JobConfiguration();
        jobConfig.setId("foo");
        jobConfig.setAnalysisConfig(analysisConfig);
        jobConfig.setSchedulerConfig(schedulerConfig);
        return jobConfig;
    }

    private static Stubber doAnswerSleep(long millis)
    {
        return doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                Thread.sleep(millis);
                return null;
            }
        });
    }

    private static class ExceptionCallable implements Callable<Throwable>
    {
        private interface ExceptionTask
        {
            void run() throws Exception;
        }

        private final ExceptionTask m_Task;

        private ExceptionCallable(ExceptionTask task)
        {
            m_Task = task;
        }

        @Override
        public Throwable call()
        {
            try
            {
                m_Task.run();
            } catch (Exception e)
            {
                return e;
            }
            return null;
        }
    }
}
