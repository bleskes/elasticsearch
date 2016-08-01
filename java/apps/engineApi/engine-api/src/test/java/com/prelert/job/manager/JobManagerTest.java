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

import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
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
import java.security.GeneralSecurityException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Stubber;

import com.fasterxml.jackson.core.JsonParseException;
import com.prelert.job.AnalysisConfig;
import com.prelert.job.AnalysisLimits;
import com.prelert.job.DataCounts;
import com.prelert.job.Detector;
import com.prelert.job.IgnoreDowntime;
import com.prelert.job.JobConfiguration;
import com.prelert.job.JobDetails;
import com.prelert.job.JobException;
import com.prelert.job.JobIdAlreadyExistsException;
import com.prelert.job.JobSchedulerStatus;
import com.prelert.job.JobStatus;
import com.prelert.job.ModelDebugConfig;
import com.prelert.job.ModelSnapshot;
import com.prelert.job.NoSuchModelSnapshotException;
import com.prelert.job.SchedulerConfig;
import com.prelert.job.SchedulerConfig.DataSource;
import com.prelert.job.SchedulerState;
import com.prelert.job.UnknownJobException;
import com.prelert.job.audit.Auditor;
import com.prelert.job.data.extraction.DataExtractor;
import com.prelert.job.data.extraction.DataExtractorFactory;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobConfigurationException;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.exceptions.LicenseViolationException;
import com.prelert.job.exceptions.TooManyJobsException;
import com.prelert.job.logging.JobLoggerFactory;
import com.prelert.job.manager.actions.Action;
import com.prelert.job.manager.actions.LocalActionGuardian;
import com.prelert.job.manager.actions.ScheduledAction;
import com.prelert.job.messages.Messages;
import com.prelert.job.password.PasswordManager;
import com.prelert.job.persistence.BatchedDocumentsIterator;
import com.prelert.job.persistence.DataStoreException;
import com.prelert.job.persistence.JobDataDeleterFactory;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.persistence.MockBatchedDocumentsIterator;
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
    @Mock private PasswordManager m_PasswordManager;
    @Mock private Auditor m_Auditor;
    @Mock private JobDataDeleterFactory m_JobDataDeleter;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        when(m_JobProvider.jobIdIsUnique("not-unique")).thenReturn(false);
        when(m_JobProvider.jobIdIsUnique(not(eq("not-unique")))).thenReturn(true);
        when(m_JobProvider.audit(anyString())).thenReturn(m_Auditor);
    }

    @After
    public void tearDown()
    {
        System.clearProperty("max.jobs.factor");
    }

    @Test
    public void testFilter()

    {
        Set<String> running = new HashSet<String>(Arrays.asList("henry", "dim", "dave"));
        Set<String> diff = new HashSet<String>(Arrays.asList("dave", "tom")).stream()
                                    .filter((s) -> !running.contains(s))
                                    .collect(Collectors.toCollection(HashSet::new));

        assertTrue(diff.size() == 1);
        assertTrue(diff.contains("tom"));
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
        when(m_JobProvider.getJobDetails("foo")).thenReturn(Optional.empty());

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
        when(m_JobProvider.getJobDetails("foo")).thenReturn(Optional.empty());

        jobManager.deleteJob("foo");

        verify(m_ProcessManager).closeJob("foo");
        verify(m_ProcessManager).deletePersistedData("foo");
        verify(m_JobProvider).deleteJob("foo");
    }

    @Test
    public void testDeleteJob_GivenScheduledJob()
            throws UnknownJobException, DataStoreException, NativeProcessRunException,
            JobInUseException, LicenseViolationException, JobConfigurationException,
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
        when(m_JobProvider.getJobDetails("foo")).thenReturn(Optional.of(new JobDetails()));

        jobManager.createJob(jobConfig, false);

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

        when(m_JobProvider.getJobDetails("foo")).thenReturn(Optional.of(new JobDetails()));

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
        Throwable exception = result1 != null ? result1 : result2;
        assertTrue(exception instanceof JobInUseException);
        assertEquals("Cannot delete job foo while another connection is deleting the job",
                exception.getMessage());

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
            OutOfOrderRecordsException, LicenseViolationException, MalformedJsonException,
            TooManyJobsException
    {
        AnalysisConfig ac = new AnalysisConfig();
        ac.setDetectors(Arrays.asList(new Detector()));

        when(m_JobProvider.getJobDetails("foo")).thenReturn(
                Optional.of(new JobDetails("foo", new JobConfiguration(ac))));
        givenProcessInfo(2);
        when(m_ProcessManager.jobIsRunning("foo")).thenReturn(false);
        when(m_ProcessManager.runningJobs()).thenReturn(Arrays.asList("bar", "bah"));
        JobManager jobManager = createJobManager();

        String expectedError = "Cannot reactivate job with id 'foo' - your license "
                + "limits you to 2 concurrently running jobs. You must close a job before you "
                + "can reactivate another.";
        try
        {
            jobManager.submitDataLoadJob("foo", mock(InputStream.class), mock(DataLoadParams.class));
            fail();
        }
        catch (LicenseViolationException e)
        {
            assertEquals(expectedError, e.getMessage());
            assertEquals(ErrorCodes.LICENSE_VIOLATION, e.getErrorCode());
            verify(m_JobProvider).audit("foo");
            verify(m_Auditor).error(expectedError);
        }
    }

    @Test
    public void testSubmitDataLoadJob_GivenDefaultFactorAndProcessIsRunningMoreJobsThanMaxAllowed()
            throws JsonParseException, UnknownJobException, NativeProcessRunException,
            MissingFieldException, JobInUseException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, LicenseViolationException, MalformedJsonException,
            TooManyJobsException
    {
        AnalysisConfig ac = new AnalysisConfig();
        ac.setDetectors(Arrays.asList(new Detector()));

        when(m_JobProvider.getJobDetails("foo")).thenReturn(
                Optional.of(new JobDetails("foo", new JobConfiguration(ac))));
        givenProcessInfo(10001);
        when(m_ProcessManager.jobIsRunning("foo")).thenReturn(false);
        when(m_ProcessManager.runningJobs()).thenReturn(createJobIds(10000));
        JobManager jobManager = createJobManager();

        String expectedError = "Cannot start job with id 'foo'. " +
                "The maximum number of concurrently running jobs is limited as a function " +
                "of the number of CPU cores see this error code's help documentation " +
                "for details of how to elevate the setting";
        try
        {
            jobManager.submitDataLoadJob("foo", mock(InputStream.class), mock(DataLoadParams.class));
            fail();
        }
        catch (TooManyJobsException e)
        {
            assertEquals(expectedError, e.getMessage());
            assertEquals(ErrorCodes.TOO_MANY_JOBS_RUNNING_CONCURRENTLY, e.getErrorCode());
            verify(m_JobProvider).audit("foo");
            verify(m_Auditor).error(expectedError);
        }
    }

    @Test
    public void testSubmitDataLoadJob_GivenSpecifiedFactorAndProcessIsRunningMoreJobsThanMaxAllowed()
            throws JsonParseException, UnknownJobException, NativeProcessRunException,
            MissingFieldException, JobInUseException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, LicenseViolationException, MalformedJsonException,
            TooManyJobsException
    {
        System.setProperty("max.jobs.factor", "5.0");

        AnalysisConfig ac = new AnalysisConfig();
        ac.setDetectors(Arrays.asList(new Detector()));
        when(m_JobProvider.getJobDetails("foo")).thenReturn(
                Optional.of(new JobDetails("foo", new JobConfiguration(ac))));
        givenProcessInfo(50000);
        when(m_ProcessManager.jobIsRunning("foo")).thenReturn(false);
        when(m_ProcessManager.runningJobs()).thenReturn(createJobIds(10000));
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
            OutOfOrderRecordsException, LicenseViolationException, MalformedJsonException,
            TooManyJobsException
    {
        System.setProperty("max.jobs.factor", "invalid");

        AnalysisConfig ac = new AnalysisConfig();
        ac.setDetectors(Arrays.asList(new Detector()));
        when(m_JobProvider.getJobDetails("foo")).thenReturn(
                Optional.of(new JobDetails("foo", new JobConfiguration(ac))));
        givenProcessInfo(50000);
        when(m_ProcessManager.jobIsRunning("foo")).thenReturn(false);
        when(m_ProcessManager.runningJobs()).thenReturn(createJobIds(10000));
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
    public void testSubmitDataLoadJob_GivenMoreDetectorsThanAllowed()
            throws JsonParseException, UnknownJobException, NativeProcessRunException,
            MissingFieldException, JobInUseException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, LicenseViolationException, MalformedJsonException,
            TooManyJobsException
    {
        AnalysisConfig ac = new AnalysisConfig();
        ac.setDetectors(Arrays.asList(new Detector()));
        when(m_JobProvider.getJobDetails("foo")).thenReturn(
                Optional.of(new JobDetails("foo", new JobConfiguration(ac))));

        int maxDetectors = 40;
        givenProcessInfo(5, maxDetectors);
        when(m_ProcessManager.jobIsRunning("foo")).thenReturn(false);
        when(m_ProcessManager.runningJobs()).thenReturn(createJobIds(4));
        when(m_ProcessManager.numberOfRunningDetectors()).thenReturn(40);
        JobManager jobManager = createJobManager();

        m_ExpectedException.expect(LicenseViolationException.class);
        m_ExpectedException.expectMessage(
                Messages.getMessage(Messages.LICENSE_LIMIT_DETECTORS_REACTIVATE, "foo", maxDetectors));
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.LICENSE_VIOLATION));

        jobManager.submitDataLoadJob("foo", mock(InputStream.class), mock(DataLoadParams.class));
    }

    @Test
    public void testSubmitDataLoadJob_GivenPausedJob() throws JobException, JsonParseException
    {
        AnalysisConfig ac = new AnalysisConfig();
        ac.setDetectors(Arrays.asList(new Detector()));
        JobDetails job = new JobDetails("foo", new JobConfiguration(ac));
        job.setStatus(JobStatus.PAUSED);

        InputStream inputStream = mock(InputStream.class);
        DataLoadParams params = mock(DataLoadParams.class);
        when(m_JobProvider.getJobDetails("foo")).thenReturn(Optional.of(job));
        givenProcessInfo(5);
        JobManager jobManager = createJobManager();

        DataCounts stats = jobManager.submitDataLoadJob("foo", inputStream, params);
        assertNotNull(stats);
        assertEquals(0, stats.getProcessedRecordCount());

        verify(m_ProcessManager, never()).processDataLoadJob(job, inputStream, params);
        verify(m_JobProvider, never()).updateJob(eq("foo"), anyMapOf(String.class, Object.class));
    }

    @Test
    public void testSubmitDataLoadJob_GivenProcessIsRunSuccessfully()
            throws JsonParseException, UnknownJobException, NativeProcessRunException,
            MissingFieldException, JobInUseException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, LicenseViolationException, MalformedJsonException,
            TooManyJobsException
    {
        InputStream inputStream = mock(InputStream.class);
        DataLoadParams params = mock(DataLoadParams.class);

        AnalysisConfig ac = new AnalysisConfig();
        ac.setDetectors(Arrays.asList(new Detector()));
        JobDetails job = new JobDetails("foo", new JobConfiguration(ac));
        when(m_JobProvider.getJobDetails("foo")).thenReturn(Optional.of(job));
        givenProcessInfo(5);
        when(m_ProcessManager.jobIsRunning("foo")).thenReturn(false);
        when(m_ProcessManager.runningJobs()).thenReturn(createJobIds(0));
        when(m_ProcessManager.processDataLoadJob(job, inputStream, params)).thenReturn(new DataCounts());
        JobManager jobManager = createJobManager();

        DataCounts stats = jobManager.submitDataLoadJob("foo", inputStream, params);
        assertNotNull(stats);

        verify(m_JobProvider).updateJob(eq("foo"), m_JobUpdateCaptor.capture());
        Map<String, Object> updates = m_JobUpdateCaptor.getValue();
        assertNotNull(updates.get(JobDetails.LAST_DATA_TIME));
    }

    @Test
    public void testSubmitDataLoadJob_GivenProcessIsRunSuccessfullyAndJobShouldIgnoreDowntimeOnceAndPositiveProcessedRecordCount()
            throws JsonParseException, UnknownJobException, NativeProcessRunException,
            MissingFieldException, JobInUseException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, LicenseViolationException, MalformedJsonException,
            TooManyJobsException
    {
        InputStream inputStream = mock(InputStream.class);
        DataLoadParams params = mock(DataLoadParams.class);
        AnalysisConfig ac = new AnalysisConfig();
        ac.setDetectors(Arrays.asList(new Detector()));
        JobDetails job = new JobDetails("foo", new JobConfiguration(ac));
        job.setIgnoreDowntime(IgnoreDowntime.ONCE);
        when(m_JobProvider.getJobDetails("foo")).thenReturn(Optional.of(job));
        givenProcessInfo(5);
        when(m_ProcessManager.jobIsRunning("foo")).thenReturn(false);
        when(m_ProcessManager.runningJobs()).thenReturn(createJobIds(0));
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
    public void testSubmitDataLoadJob_GivenProcessIsRunSuccessfullyAndJobShouldIgnoreDowntimeAlwaysAndPositiveProcessedRecordCount()
            throws JsonParseException, UnknownJobException, NativeProcessRunException,
            MissingFieldException, JobInUseException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, LicenseViolationException, MalformedJsonException,
            TooManyJobsException
    {
        InputStream inputStream = mock(InputStream.class);
        DataLoadParams params = mock(DataLoadParams.class);
        AnalysisConfig ac = new AnalysisConfig();
        ac.setDetectors(Arrays.asList(new Detector()));
        JobDetails job = new JobDetails("foo", new JobConfiguration(ac));
        job.setIgnoreDowntime(IgnoreDowntime.ALWAYS);
        when(m_JobProvider.getJobDetails("foo")).thenReturn(Optional.of(job));
        givenProcessInfo(5);
        when(m_ProcessManager.jobIsRunning("foo")).thenReturn(false);
        when(m_ProcessManager.runningJobs()).thenReturn(createJobIds(0));
        DataCounts counts = new DataCounts();
        counts.setProcessedRecordCount(1L);
        when(m_ProcessManager.processDataLoadJob(job, inputStream, params)).thenReturn(counts);
        JobManager jobManager = createJobManager();

        DataCounts actualCounts = jobManager.submitDataLoadJob("foo", inputStream, params);
        assertEquals(actualCounts, counts);

        verify(m_JobProvider).updateJob(eq("foo"), m_JobUpdateCaptor.capture());
        assertNotNull(m_JobUpdateCaptor.getValue().get(JobDetails.LAST_DATA_TIME));
    }

    @Test
    public void testSubmitDataLoadJob_GivenProcessIsRunSuccessfullyAndJobShouldIgnoreDowntimeOnceAndZeroProcessedRecordCount()
            throws JsonParseException, UnknownJobException, NativeProcessRunException,
            MissingFieldException, JobInUseException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, LicenseViolationException, MalformedJsonException,
            TooManyJobsException
    {
        InputStream inputStream = mock(InputStream.class);
        DataLoadParams params = mock(DataLoadParams.class);
        AnalysisConfig ac = new AnalysisConfig();
        ac.setDetectors(Arrays.asList(new Detector()));
        JobDetails job = new JobDetails("foo", new JobConfiguration(ac));
        job.setIgnoreDowntime(IgnoreDowntime.ONCE);
        when(m_JobProvider.getJobDetails("foo")).thenReturn(Optional.of(job));
        givenProcessInfo(5);
        when(m_ProcessManager.jobIsRunning("foo")).thenReturn(false);
        when(m_ProcessManager.runningJobs()).thenReturn(createJobIds(0));
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
    public void testSetBackgroundPersistInterval() throws UnknownJobException
    {
        givenProcessInfo(5);
        JobManager jobManager = createJobManager();

        jobManager.setBackgroundPersistInterval("foo", 36000L);

        verify(m_JobProvider).updateJob(eq("foo"), m_JobUpdateCaptor.capture());
        Map<String, Object> jobUpdate = m_JobUpdateCaptor.getValue();
        assertEquals(36000L, jobUpdate.get(JobDetails.BACKGROUND_PERSIST_INTERVAL));
    }

    @Test
    public void testSetRenormalizationWindowDays() throws UnknownJobException
    {
        givenProcessInfo(5);
        JobManager jobManager = createJobManager();

        jobManager.setRenormalizationWindowDays("foo", 7L);

        verify(m_JobProvider).updateJob(eq("foo"), m_JobUpdateCaptor.capture());
        Map<String, Object> jobUpdate = m_JobUpdateCaptor.getValue();
        assertEquals(new Long(7), jobUpdate.get(JobDetails.RENORMALIZATION_WINDOW_DAYS));
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
    public void testGetJobOrThrowIfUnknown_GivenUnknownJob() throws UnknownJobException
    {
        givenLicenseConstraints(2, 2, 0);
        JobManager jobManager = createJobManager();
        when(m_JobProvider.getJobDetails("foo")).thenReturn(Optional.empty());

        m_ExpectedException.expect(UnknownJobException.class);

        jobManager.getJobOrThrowIfUnknown("foo");
    }

    @Test
    public void testGetJobOrThrowIfUnknown_GivenKnownJob() throws UnknownJobException
    {
        givenLicenseConstraints(2, 2, 0);
        JobManager jobManager = createJobManager();
        JobDetails job = new JobDetails();
        when(m_JobProvider.getJobDetails("foo")).thenReturn(Optional.of(job));

        assertEquals(job, jobManager.getJobOrThrowIfUnknown("foo"));
    }

    @Test
    public void testCreateJob_licensingConstraintMaxJobs() throws UnknownJobException,
            JobConfigurationException, JobIdAlreadyExistsException, IOException,
            CannotStartSchedulerException, DataStoreException, NativeProcessRunException,
            JobInUseException, CannotStopSchedulerException
    {
        givenLicenseConstraints(2, 2, 0);
        when(m_ProcessManager.jobIsRunning(any())).thenReturn(false);
        when(m_ProcessManager.runningJobs()).thenReturn(createJobIds(2));

        JobManager jobManager = createJobManager();

        try
        {
            AnalysisConfig ac = new AnalysisConfig();
            List<Detector> detectors = Arrays.asList(new Detector());
            ac.setDetectors(detectors);
            jobManager.createJob(new JobConfiguration(ac), false);
            fail();
        }
        catch (LicenseViolationException e)
        {
            assertEquals(ErrorCodes.LICENSE_VIOLATION, e.getErrorCode());

            String message = Messages.getMessage(Messages.LICENSE_LIMIT_JOBS, 2);
            assertEquals(message, e.getMessage());
        }
    }

    @Test
    public void testCreateJob_licensingConstraintOnLimitAndOverwriting() throws UnknownJobException,
            JobConfigurationException, JobIdAlreadyExistsException, IOException,
            CannotStartSchedulerException, DataStoreException, NativeProcessRunException,
            JobInUseException, CannotStopSchedulerException
    {
        givenLicenseConstraints(2, 2, 0);
        when(m_ProcessManager.jobIsRunning("not-unique")).thenReturn(true);
        when(m_ProcessManager.runningJobs()).thenReturn(createJobIds(2));
        Mockito.doThrow(new NativeProcessRunException("mock", ErrorCodes.NATIVE_PROCESS_ERROR)).when(m_ProcessManager).closeJob("not-unique");

        AnalysisConfig analysisConfig = new AnalysisConfig();
        analysisConfig.setBucketSpan(3600L);
        Detector detector = new Detector();
        detector.setFunction("count");

        analysisConfig.setDetectors(Arrays.asList(detector));

        JobConfiguration jobConfig = new JobConfiguration();
        jobConfig.setId("not-unique");
        jobConfig.setAnalysisConfig(analysisConfig);

        JobManager jobManager = createJobManager();

        when(m_JobProvider.getJobDetails("not-unique")).thenReturn(Optional.of(new JobDetails()));

        try
        {
            jobManager.createJob(jobConfig, true);
            fail();
        }
        catch (LicenseViolationException tmje)
        {
            // Usually creating a third job when 2 are allowed and 2 are running
            // would throw a license exception, but in the case of overwriting
            // one of the running jobs we shouldn't get this
            fail();
        }
        catch (NativeProcessRunException npre)
        {
            assertEquals(ErrorCodes.NATIVE_PROCESS_ERROR, npre.getErrorCode());

            assertEquals("mock", npre.getMessage());
        }
    }

    @Test
    public void testCreateJob_licensingConstraintMaxDetectors()
    throws UnknownJobException, JobIdAlreadyExistsException,
            IOException, LicenseViolationException, CannotStartSchedulerException, DataStoreException, NativeProcessRunException,
            JobInUseException, CannotStopSchedulerException, JobConfigurationException
    {
        givenLicenseConstraints(5, 1, 0);
        when(m_ProcessManager.jobIsRunning(any())).thenReturn(false);
        when(m_ProcessManager.runningJobs()).thenReturn(createJobIds(3));

        JobManager jobManager = createJobManager();

        try
        {
            AnalysisConfig ac = new AnalysisConfig();
            // create 2 detectors
            ac.getDetectors().add(new Detector());
            ac.getDetectors().add(new Detector());
            jobManager.createJob(new JobConfiguration(ac), false);
            fail();
        }
        catch (LicenseViolationException e)
        {
            assertEquals(ErrorCodes.LICENSE_VIOLATION, e.getErrorCode());

            String message = Messages.getMessage(Messages.LICENSE_LIMIT_DETECTORS, 1, 2);
            assertEquals(message, e.getMessage());
        }
    }

    @Test
    public void testCreateJob_licensingConstraintMaxPartitions() throws UnknownJobException,
            JobIdAlreadyExistsException, IOException, LicenseViolationException,
            CannotStartSchedulerException, DataStoreException, NativeProcessRunException,
            JobInUseException, CannotStopSchedulerException, JobConfigurationException
    {
        givenLicenseConstraints(5, -1, 0);
        when(m_ProcessManager.jobIsRunning(any())).thenReturn(false);
        when(m_ProcessManager.runningJobs()).thenReturn(createJobIds(3));

        JobManager jobManager = createJobManager();

        try
        {
            AnalysisConfig ac = new AnalysisConfig();
            // create 2 detectors
            Detector d = new Detector();
            d.setPartitionFieldName("pfield");
            ac.getDetectors().add(d);
            jobManager.createJob(new JobConfiguration(ac), false);
            fail();
        }
        catch (LicenseViolationException e)
        {
            assertEquals(ErrorCodes.LICENSE_VIOLATION, e.getErrorCode());

            String message = Messages.getMessage(Messages.LICENSE_LIMIT_PARTITIONS);
            assertEquals(message, e.getMessage());
        }
    }

    @Test
    public void testCreateJob_OverwriteExisting()
            throws NoSuchScheduledJobException, UnknownJobException,
            CannotStartSchedulerException, LicenseViolationException,
            JobConfigurationException, JobIdAlreadyExistsException, IOException,
            NativeProcessRunException, JobInUseException, DataStoreException,
            CannotStopSchedulerException
    {
        givenProcessInfo(2);
        JobManager jobManager = createJobManager();
        when(m_JobProvider.deleteJob("not-unique")).thenReturn(true);

        AnalysisConfig analysisConfig = new AnalysisConfig();
        analysisConfig.setBucketSpan(3600L);
        Detector detector = new Detector();
        detector.setFunction("count");

        analysisConfig.setDetectors(Arrays.asList(detector));

        JobConfiguration jobConfig = new JobConfiguration();
        jobConfig.setId("not-unique");
        jobConfig.setAnalysisConfig(analysisConfig);

        when(m_JobProvider.getJobDetails("not-unique")).thenReturn(Optional.of(new JobDetails()));

        jobManager.createJob(jobConfig, true);

        verify(m_JobProvider, times(2)).audit("not-unique");
        InOrder inOrder = Mockito.inOrder(m_Auditor, m_Auditor);
        inOrder.verify(m_Auditor).info(eq("Job deleted"));
        inOrder.verify(m_Auditor).info(eq("Job created"));
    }

    @Test
    public void testCreateJob_FillsDefaultDetectorDescriptions()
            throws NoSuchScheduledJobException, UnknownJobException,
            CannotStartSchedulerException, LicenseViolationException,
            JobConfigurationException, JobIdAlreadyExistsException, IOException,
            NativeProcessRunException, JobInUseException, DataStoreException,
            CannotStopSchedulerException
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

        JobDetails job = jobManager.createJob(jobConfig, false);

        assertEquals("sum(revenue) by vendor", job.getAnalysisConfig().getDetectors().get(0).getDetectorDescription());
        assertEquals("Named", job.getAnalysisConfig().getDetectors().get(1).getDetectorDescription());
        verify(m_JobProvider).audit("revenue-by-vendor");
        verify(m_Auditor).info("Job created");
    }

    @Test
    public void testCreateJob_GivenScheduledJobSetsTimeoutToZero() throws JobException
    {
        givenProcessInfo(2);
        JobManager jobManager = createJobManager();

        JobConfiguration jobConfig = createScheduledJobConfig();
        jobConfig.setTimeout(900L);

        Logger jobLogger = mock(Logger.class);
        when(m_JobLoggerFactory.newLogger("foo")).thenReturn(jobLogger);
        when(m_ProcessManager.jobIsRunning("foo")).thenReturn(true);

        JobDetails scheduledJob = jobManager.createJob(jobConfig, false);

        jobConfig.setSchedulerConfig(null);
        JobDetails normalJob = jobManager.createJob(jobConfig, false);

        assertEquals(0, scheduledJob.getTimeout());
        assertEquals(900, normalJob.getTimeout());
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

        when(m_ProcessManager.runningJobs()).thenReturn(createJobIds(3));
        when(m_ProcessManager.writeToJob(eq(false), any(CsvRecordWriter.class), any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenAnswer(writeToWriter());

        JobManager jobManager = createJobManager();
        String answer = jobManager.previewTransforms("foo", mock(InputStream.class));

        assertEquals("csv,header,one\r\n", answer);
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

        JobDetails jd = new JobDetails();
        when(m_JobProvider.getJobDetails("foo")).thenReturn(Optional.of(jd));

        jobManager.startJobScheduler("foo", 0, OptionalLong.empty());
    }

    @Test
    public void testStartJobScheduler_GivenNewlyCreatedJob() throws UnknownJobException,
            LicenseViolationException, JobConfigurationException, JobIdAlreadyExistsException,
            CannotStartSchedulerException, IOException, NoSuchScheduledJobException,
            CannotStopSchedulerException, NativeProcessRunException, JobInUseException,
            DataStoreException, TooManyJobsException
    {
        givenProcessInfo(2);
        JobManager jobManager = createJobManager();
        JobConfiguration jobConfig = createScheduledJobConfig();

        Logger jobLogger = mock(Logger.class);
        when(m_JobLoggerFactory.newLogger("foo")).thenReturn(jobLogger);
        DataExtractor dataExtractor = mock(DataExtractor.class);
        when(m_DataExtractorFactory.newExtractor(any(JobDetails.class))).thenReturn(dataExtractor);

        JobDetails job = jobManager.createJob(jobConfig, false);
        DataCounts dataCounts = new DataCounts();
        dataCounts.setLatestRecordTimeStamp(new Date(0));
        job.setCounts(dataCounts);
        when(m_JobProvider.getJobDetails("foo")).thenReturn(Optional.of(job));

        jobManager.startJobScheduler("foo", 0, OptionalLong.empty());

        jobManager.stopJobScheduler("foo");

        verify(dataExtractor).newSearch(anyLong(), anyLong(), eq(jobLogger));
        verify(m_ProcessManager).closeJob("foo");
        verify(m_JobProvider).updateSchedulerState("foo", new SchedulerState(0L, null));
    }

    @Test
    public void testStartJobScheduler_MaxDetectorsLicenceViolation_GivenAnotherNonScheduledJob()
            throws JobException
    {
        int maxDetectors = 10;
        givenProcessInfo(5, maxDetectors);

        when(m_ProcessManager.jobIsRunning("foo")).thenReturn(false);
        when(m_ProcessManager.runningJobs()).thenReturn(createJobIds(2));
        when(m_ProcessManager.numberOfRunningDetectors()).thenReturn(10);

        JobManager jobManager = createJobManager();
        JobConfiguration jobConfig = createScheduledJobConfig();

        Logger jobLogger = mock(Logger.class);
        when(m_JobLoggerFactory.newLogger("foo")).thenReturn(jobLogger);

        JobDetails job = jobManager.createJob(jobConfig, false);
        when(m_JobProvider.getJobDetails("foo")).thenReturn(Optional.of(job));

        m_ExpectedException.expect(LicenseViolationException.class);
        m_ExpectedException.expectMessage(
                Messages.getMessage(Messages.LICENSE_LIMIT_DETECTORS_REACTIVATE, "foo", maxDetectors));
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.LICENSE_VIOLATION));

        jobManager.startJobScheduler("foo", 0, OptionalLong.empty());
    }

    @Test
    public void testStartJobScheduler_MaxDetectorsLicenceViolation_GivenStartedScheduledJob()
            throws JobException
    {
        int maxDetectors = 1;
        givenProcessInfo(2, maxDetectors);

        when(m_ProcessManager.jobIsRunning("foo1")).thenReturn(false);
        Logger jobLogger = mock(Logger.class);
        when(m_JobLoggerFactory.newLogger(anyString())).thenReturn(jobLogger);
        DataExtractor dataExtractor = mock(DataExtractor.class);
        when(m_DataExtractorFactory.newExtractor(any(JobDetails.class))).thenReturn(dataExtractor);

        JobManager jobManager = createJobManager();

        JobConfiguration jobConfig1 = createScheduledJobConfig();
        jobConfig1.setId("foo1");
        JobDetails job1 = jobManager.createJob(jobConfig1, false);
        when(m_JobProvider.getJobDetails("foo1")).thenReturn(Optional.of(job1));
        jobManager.startJobScheduler("foo1", 0, OptionalLong.empty());

        JobConfiguration jobConfig2 = createScheduledJobConfig();
        jobConfig2.setId("foo2");
        JobDetails job2 = jobManager.createJob(jobConfig2, false);
        when(m_JobProvider.getJobDetails("foo2")).thenReturn(Optional.of(job2));

        m_ExpectedException.expect(LicenseViolationException.class);
        m_ExpectedException.expectMessage(
                Messages.getMessage(Messages.LICENSE_LIMIT_DETECTORS_REACTIVATE, "foo2", maxDetectors));
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.LICENSE_VIOLATION));

        jobManager.startJobScheduler("foo2", 0, OptionalLong.empty());
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

        JobDetails jd = new JobDetails();
        when(m_JobProvider.getJobDetails("foo")).thenReturn(Optional.of(jd));

        jobManager.stopJobScheduler("foo");
    }

    @Test
    public void testSetupScheduledJobs_GivenNonScheduledJobAndJobWithStartedScheduler()
            throws NoSuchScheduledJobException, UnknownJobException,
            CannotStartSchedulerException, LicenseViolationException,
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

        BatchedDocumentsIterator<JobDetails> jobIterator = newBatchedJobsIterator(
                Arrays.asList(nonScheduledJob, scheduledJob));
        when(m_JobProvider.newBatchedJobsIterator()).thenReturn(jobIterator);
        when(m_JobProvider.getSchedulerState("scheduled")).thenReturn(Optional.of(schedulerState));

        Logger jobLogger = mock(Logger.class);
        when(m_JobLoggerFactory.newLogger("scheduled")).thenReturn(jobLogger);
        DataExtractor dataExtractor = mock(DataExtractor.class);
        when(m_DataExtractorFactory.newExtractor(any(JobDetails.class))).thenReturn(dataExtractor);

        givenProcessInfo(2);
        JobManager jobManager = createJobManager();

        jobManager.setupScheduledJobs();

        Thread.sleep(200);

        verify(m_JobLoggerFactory).newLogger("scheduled");
        verify(m_DataExtractorFactory).newExtractor(scheduledJob);
        verify(dataExtractor).newSearch(anyLong(), anyLong(), eq(jobLogger));
        jobManager.shutdown();

        verify(m_JobLoggerFactory).close("scheduled", jobLogger);
        // Verify no other calls to factories - means no other job was scheduled
        Mockito.verifyNoMoreInteractions(m_JobLoggerFactory, m_DataExtractorFactory);
    }

    @Test
    public void testSetupScheduledJobs_GivenJobWithStartedSchedulerAndAutorestartIsDisabled()
            throws NoSuchScheduledJobException, UnknownJobException,
            CannotStartSchedulerException, LicenseViolationException,
            JobConfigurationException, JobIdAlreadyExistsException, IOException, InterruptedException
    {
        System.setProperty("scheduler.autorestart", "false");

        JobConfiguration jobConfig = createScheduledJobConfig();
        JobDetails scheduledJob = new JobDetails("scheduled", jobConfig);
        DataCounts dataCounts = new DataCounts();
        dataCounts.setLatestRecordTimeStamp(new Date(0));
        scheduledJob.setCounts(dataCounts);
        scheduledJob.setSchedulerStatus(JobSchedulerStatus.STARTED);
        SchedulerState schedulerState = new SchedulerState();
        schedulerState.setStartTimeMillis(0L);
        schedulerState.setEndTimeMillis(null);

        BatchedDocumentsIterator<JobDetails> jobIterator = newBatchedJobsIterator(
                Arrays.asList(scheduledJob));
        when(m_JobProvider.newBatchedJobsIterator()).thenReturn(jobIterator);
        when(m_JobProvider.getSchedulerState("scheduled")).thenReturn(Optional.of(schedulerState));

        Logger jobLogger = mock(Logger.class);
        when(m_JobLoggerFactory.newLogger("scheduled")).thenReturn(jobLogger);

        givenProcessInfo(2);
        JobManager jobManager = createJobManager();

        jobManager.setupScheduledJobs();

        // Give time for schedulers to start
        Thread.sleep(200);

        jobManager.shutdown();

        // Verify scheduler status was updated to STOPPED
        verify(m_JobProvider).updateJob(eq("scheduled"), m_JobUpdateCaptor.capture());
        Map<String, Object> jobUpdate = m_JobUpdateCaptor.getValue();
        assertEquals(JobSchedulerStatus.STOPPED, jobUpdate.get(JobDetails.SCHEDULER_STATUS));

        // Verify no other calls to factories - means no job was scheduled
        Mockito.verifyNoMoreInteractions(m_JobLoggerFactory, m_DataExtractorFactory);

        System.clearProperty("scheduler.autorestart");
    }

    @Test
    public void testSetupScheduledJobs_GivenJobWithStoppedScheduler() throws NoSuchScheduledJobException, UnknownJobException,
            CannotStartSchedulerException, LicenseViolationException,
            JobConfigurationException, JobIdAlreadyExistsException, IOException, InterruptedException
    {
        JobConfiguration jobConfig = createScheduledJobConfig();
        JobDetails scheduledJob = new JobDetails("scheduled", jobConfig);
        DataCounts dataCounts = new DataCounts();
        dataCounts.setLatestRecordTimeStamp(new Date(0));
        scheduledJob.setCounts(dataCounts);
        scheduledJob.setSchedulerStatus(JobSchedulerStatus.STOPPED);

        BatchedDocumentsIterator<JobDetails> jobIterator = newBatchedJobsIterator(
                Arrays.asList(scheduledJob));
        when(m_JobProvider.newBatchedJobsIterator()).thenReturn(jobIterator);

        JobDetails jd = new JobDetails();
        jd.setSchedulerConfig(new SchedulerConfig());
        when(m_JobProvider.getJobDetails("scheduled")).thenReturn(Optional.of(jd));

        givenProcessInfo(2);
        JobManager jobManager = createJobManager();

        jobManager.setupScheduledJobs();

        jobManager.checkJobHasScheduler("scheduled");

        jobManager.shutdown();

        // Verify no other calls to factories - means no other job was scheduled
        Mockito.verifyNoMoreInteractions(m_JobLoggerFactory, m_DataExtractorFactory);
    }

    @Test
    public void testSetupScheduledJobs_GivenJobWithStoppingScheduler() throws JobException
    {
        JobConfiguration jobConfig = createScheduledJobConfig();
        JobDetails scheduledJob = new JobDetails("scheduled", jobConfig);
        DataCounts dataCounts = new DataCounts();
        dataCounts.setLatestRecordTimeStamp(new Date(0));
        scheduledJob.setCounts(dataCounts);
        scheduledJob.setSchedulerStatus(JobSchedulerStatus.STOPPING);

        BatchedDocumentsIterator<JobDetails> jobIterator = newBatchedJobsIterator(
                Arrays.asList(scheduledJob));
        when(m_JobProvider.newBatchedJobsIterator()).thenReturn(jobIterator);

        JobDetails jd = new JobDetails();
        jd.setSchedulerConfig(new SchedulerConfig());
        when(m_JobProvider.getJobDetails("scheduled")).thenReturn(Optional.of(jd));

        givenProcessInfo(2);
        JobManager jobManager = createJobManager();

        jobManager.setupScheduledJobs();

        jobManager.checkJobHasScheduler("scheduled");

        jobManager.shutdown();

        // Verify scheduler status was updated to STOPPED
        verify(m_JobProvider).updateJob(eq("scheduled"), m_JobUpdateCaptor.capture());
        Map<String, Object> jobUpdate = m_JobUpdateCaptor.getValue();
        assertEquals(JobSchedulerStatus.STOPPED, jobUpdate.get(JobDetails.SCHEDULER_STATUS));

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
        when(m_JobProvider.modelSnapshots("foo", 0, 1, 0, 0, ModelSnapshot.TIMESTAMP, true, "", "my description")).thenReturn(modelSnapshotPage);
        Logger jobLogger = mock(Logger.class);
        when(m_JobLoggerFactory.newLogger("foo")).thenReturn(jobLogger);
        ModelSnapshot revertedModelSnapshot = jobManager.revertToSnapshot("foo", 0, "", "my description", false);
        assertNotNull(revertedModelSnapshot);
        assertTrue(revertedModelSnapshot.getRestorePriority() > 1);

        verify(m_JobProvider).updateJob(eq("foo"), m_JobUpdateCaptor.capture());
        assertTrue(m_JobUpdateCaptor.getValue().containsKey(JobDetails.IGNORE_DOWNTIME));
        assertEquals(IgnoreDowntime.ONCE, m_JobUpdateCaptor.getValue().get(JobDetails.IGNORE_DOWNTIME));
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
        when(m_JobProvider.modelSnapshots("foo", 0, 1, 0, 0, ModelSnapshot.TIMESTAMP, true, "", "my description")).thenReturn(modelSnapshotPage);
        Logger jobLogger = mock(Logger.class);
        when(m_JobLoggerFactory.newLogger("foo")).thenReturn(jobLogger);
        try
        {
            jobManager.revertToSnapshot("foo", 0, "", "my description", false);
            fail();
        }
        catch (NoSuchModelSnapshotException e)
        {
            assertEquals("No matching model snapshot exists for job 'foo'", e.getMessage());
            assertEquals(ErrorCodes.NO_SUCH_MODEL_SNAPSHOT, e.getErrorCode());
        }

        verify(m_JobProvider, never()).updateJob(eq("foo"), anyMapOf(String.class, Object.class));
    }

    @Test
    public void testDeleteModelSnapshot_GivenSnapshotExistsAndNotHighestPriority()
            throws JobInUseException, UnknownJobException, NoSuchModelSnapshotException,
            CannotDeleteSnapshotException
    {
        givenProcessInfo(2);
        JobManager jobManager = createJobManager();

        ModelSnapshot modelSnapshot1 = new ModelSnapshot();
        modelSnapshot1.setSnapshotId("1");
        modelSnapshot1.setRestorePriority(1);
        ModelSnapshot modelSnapshot2 = new ModelSnapshot();
        modelSnapshot2.setSnapshotId("2");
        modelSnapshot2.setRestorePriority(2);

        QueryPage<ModelSnapshot> modelSnapshotPage = new QueryPage<>(Arrays.asList(modelSnapshot2), 1);
        when(m_JobProvider.modelSnapshots("foo", 0, 1)).thenReturn(modelSnapshotPage);
        when(m_JobProvider.deleteModelSnapshot("foo", "1")).thenReturn(modelSnapshot1);

        ModelSnapshot deletedSnapshot = jobManager.deleteModelSnapshot("foo", "1");
        assertNotNull(deletedSnapshot);

        verify(m_JobProvider).modelSnapshots("foo", 0, 1);
        assertEquals("1", deletedSnapshot.getSnapshotId());
    }

    @Test
    public void testDeleteModelSnapshot_GivenSnapshotExistsButHighestPriority()
            throws JobInUseException, UnknownJobException, NoSuchModelSnapshotException,
            CannotDeleteSnapshotException
    {
        givenProcessInfo(2);
        JobManager jobManager = createJobManager();

        ModelSnapshot modelSnapshot1 = new ModelSnapshot();
        modelSnapshot1.setSnapshotId("1");
        modelSnapshot1.setRestorePriority(1);
        ModelSnapshot modelSnapshot2 = new ModelSnapshot();
        modelSnapshot2.setSnapshotId("2");
        modelSnapshot2.setRestorePriority(2);

        QueryPage<ModelSnapshot> modelSnapshotPage = new QueryPage<>(Arrays.asList(modelSnapshot2), 1);
        when(m_JobProvider.modelSnapshots("foo", 0, 1)).thenReturn(modelSnapshotPage);
        when(m_JobProvider.deleteModelSnapshot("foo", "2")).thenReturn(modelSnapshot2);

        m_ExpectedException.expect(CannotDeleteSnapshotException.class);
        m_ExpectedException.expectMessage("Model snapshot '2' is the active snapshot for job 'foo', so cannot be deleted");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.CANNOT_DELETE_HIGHEST_PRIORITY));

        jobManager.deleteModelSnapshot("foo", "2");
    }

    @Test
    public void testDeleteModelSnapshot_GivenSnapshotDoesNotExist()
            throws JobInUseException, UnknownJobException, NoSuchModelSnapshotException,
            CannotDeleteSnapshotException
    {
        givenProcessInfo(2);
        JobManager jobManager = createJobManager();

        ModelSnapshot modelSnapshot1 = new ModelSnapshot();
        modelSnapshot1.setSnapshotId("1");
        modelSnapshot1.setRestorePriority(1);
        ModelSnapshot modelSnapshot2 = new ModelSnapshot();
        modelSnapshot2.setSnapshotId("2");
        modelSnapshot2.setRestorePriority(2);

        QueryPage<ModelSnapshot> modelSnapshotPage = new QueryPage<>(Arrays.asList(modelSnapshot2), 1);
        when(m_JobProvider.modelSnapshots("foo", 0, 1)).thenReturn(modelSnapshotPage);
        when(m_JobProvider.deleteModelSnapshot("foo", "3")).thenThrow(new NoSuchModelSnapshotException("foo"));

        m_ExpectedException.expect(NoSuchModelSnapshotException.class);
        m_ExpectedException.expectMessage("No matching model snapshot exists for job 'foo'");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.NO_SUCH_MODEL_SNAPSHOT));

        jobManager.deleteModelSnapshot("foo", "3");
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
        when(m_JobProvider.modelSnapshots("foo", 0, 1, 0, 0, null, true, "123", null)).thenReturn(oldModelSnapshotPage);
        when(m_JobProvider.modelSnapshots("foo", 0, 1, 0, 0, null, true, null, "new description")).thenReturn(clashingModelSnapshotPage);

        ModelSnapshot updatedModelSnapshot = jobManager.updateModelSnapshotDescription("foo", "123", "new description");
        assertNotNull(updatedModelSnapshot);
        assertEquals("new description", updatedModelSnapshot.getDescription());
    }

    @Test
    public void updateModelSnapshotDescription_GivenSnapshotNotFound()
            throws JobInUseException, UnknownJobException, NoSuchModelSnapshotException, DescriptionAlreadyUsedException
    {
        givenProcessInfo(2);
        JobManager jobManager = createJobManager();

        QueryPage<ModelSnapshot> oldModelSnapshotPage = new QueryPage<>(null, 0);
        when(m_JobProvider.modelSnapshots("foo", 0, 1, 0, 0, null, true, "123", null)).thenReturn(oldModelSnapshotPage);

        m_ExpectedException.expect(NoSuchModelSnapshotException.class);
        m_ExpectedException.expectMessage("No matching model snapshot exists for job 'foo'");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.NO_SUCH_MODEL_SNAPSHOT));

        jobManager.updateModelSnapshotDescription("foo", "123", "new description");
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
        when(m_JobProvider.modelSnapshots("foo", 0, 1, 0, 0, null, true, "123", null)).thenReturn(oldModelSnapshotPage);
        when(m_JobProvider.modelSnapshots("foo", 0, 1, 0, 0, null, true, null, "new description")).thenReturn(clashingModelSnapshotPage);

        m_ExpectedException.expect(DescriptionAlreadyUsedException.class);
        m_ExpectedException.expectMessage("Model snapshot description 'new description' has already been used for job 'foo'");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.DESCRIPTION_ALREADY_USED));

        jobManager.updateModelSnapshotDescription("foo", "123", "new description");
    }

    @Test
    public void testUpdateCategorizationFilters() throws UnknownJobException, JobException
    {
        givenProcessInfo(2);
        JobManager jobManager = createJobManager();

        jobManager.updateCategorizationFilters("foo", Arrays.asList("a", "b"));

        verify(m_JobProvider).updateCategorizationFilters("foo", Arrays.asList("a", "b"));
    }

    @Test
    public void testUpdateDetectorDescription() throws UnknownJobException, JobException
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

        JobDetails jobWithIgnoreDowntimeSet = new JobDetails();
        jobWithIgnoreDowntimeSet.setId("jobWithIgnoreDowntimeSet");
        jobWithIgnoreDowntimeSet.setCounts(countsWithPositiveProcessedRecordCount);
        jobWithIgnoreDowntimeSet.setIgnoreDowntime(IgnoreDowntime.ALWAYS);

        JobDetails jobThatHasNotSeenData = new JobDetails();
        jobThatHasNotSeenData.setId("jobThatHasNotSeenData");
        jobThatHasNotSeenData.setCounts(new DataCounts());

        JobDetails jobWithNullCounts = new JobDetails();
        jobWithNullCounts.setId("jobWithNullCounts");
        jobWithNullCounts.setCounts(null);

        JobDetails jobThatIsUnknown = new JobDetails();
        jobThatIsUnknown.setId("jobThatIsUnknown");
        jobThatIsUnknown.setCounts(countsWithPositiveProcessedRecordCount);
        when(m_JobProvider.updateJob(eq("jobThatIsUnknown"), anyMapOf(String.class, Object.class)))
                .thenThrow(new UnknownJobException("jobThatIsUnknown"));

        BatchedDocumentsIterator<JobDetails> jobIterator = newBatchedJobsIterator(
                Arrays.asList(jobThatHasSeenData, jobWithIgnoreDowntimeSet, jobThatHasNotSeenData,
                        jobWithNullCounts, jobThatIsUnknown));
        when(m_JobProvider.newBatchedJobsIterator()).thenReturn(jobIterator);

        givenProcessInfo(2);
        JobManager jobManager = createJobManager();

        jobManager.setIgnoreDowntimeToAllJobs();

        verify(m_JobProvider).updateJob(eq("jobThatHasSeenData"), m_JobUpdateCaptor.capture());
        assertTrue(m_JobUpdateCaptor.getValue().containsKey(JobDetails.IGNORE_DOWNTIME));
        assertEquals(IgnoreDowntime.ONCE, m_JobUpdateCaptor.getValue().get(JobDetails.IGNORE_DOWNTIME));

        verify(m_JobProvider, never()).updateJob(eq("jobWithIgnoreDowntimeSet"), anyMapOf(String.class, Object.class));
        verify(m_JobProvider, never()).updateJob(eq("jobThatHasNotSeenData"), anyMapOf(String.class, Object.class));
        verify(m_JobProvider, never()).updateJob(eq("jobWithNullCounts"), anyMapOf(String.class, Object.class));

        verify(m_JobProvider).updateJob(eq("jobThatIsUnknown"), m_JobUpdateCaptor.capture());
        assertTrue(m_JobUpdateCaptor.getValue().containsKey(JobDetails.IGNORE_DOWNTIME));
        assertEquals(IgnoreDowntime.ONCE, m_JobUpdateCaptor.getValue().get(JobDetails.IGNORE_DOWNTIME));
    }

    @Test (expected =IllegalStateException.class)
    public void testPauseJob_GivenScheduledJob() throws JobException
    {
        givenProcessInfo(2);
        JobManager jobManager = createJobManager();

        JobConfiguration jobConfig = createScheduledJobConfig();
        Logger jobLogger = mock(Logger.class);
        when(m_JobLoggerFactory.newLogger("foo")).thenReturn(jobLogger);

        JobDetails jd = new JobDetails();
        jd.setSchedulerConfig(new SchedulerConfig());
        when(m_JobProvider.getJobDetails("foo")).thenReturn(Optional.of(jd));

        jobManager.createJob(jobConfig, false);

        jobManager.pauseJob("foo");
    }

    @Test
    public void testPauseJob_GivenPausedJob() throws JobException
    {
        givenProcessInfo(2);
        JobManager jobManager = createJobManager();

        JobDetails job = new JobDetails();
        job.setId("foo");
        job.setStatus(JobStatus.PAUSED);
        when(m_JobProvider.getJobDetails("foo")).thenReturn(Optional.of(job));

        m_ExpectedException.expect(CannotPauseJobException.class);
        m_ExpectedException.expectMessage("Cannot pause job 'foo' while its status is PAUSED");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.CANNOT_PAUSE_JOB));

        jobManager.pauseJob("foo");
    }

    @Test
    public void testPauseJob_GivenClosedJob() throws JobException
    {
        givenProcessInfo(2);
        JobManager jobManager = createJobManager();
        JobDetails job = new JobDetails();
        job.setId("foo");
        job.setStatus(JobStatus.CLOSED);

        when(m_JobProvider.getJobDetails("foo")).thenReturn(Optional.of(job));
        when(m_ProcessManager.jobIsRunning("foo")).thenReturn(false);

        jobManager.pauseJob("foo");

        verify(m_ProcessManager, never()).closeJob("foo");

        ArgumentCaptor<JobStatus> statusCaptor = ArgumentCaptor.forClass(JobStatus.class);
        verify(m_JobProvider, times(2)).setJobStatus(eq("foo"), statusCaptor.capture());
        assertEquals(JobStatus.PAUSING, statusCaptor.getAllValues().get(0));
        assertEquals(JobStatus.PAUSED, statusCaptor.getAllValues().get(1));

        verify(m_JobProvider).updateJob(eq("foo"), m_JobUpdateCaptor.capture());
        assertTrue(m_JobUpdateCaptor.getValue().containsKey(JobDetails.IGNORE_DOWNTIME));
        assertEquals(IgnoreDowntime.ONCE, m_JobUpdateCaptor.getValue().get(JobDetails.IGNORE_DOWNTIME));

        verify(m_JobProvider).audit("foo");
        verify(m_Auditor).info("Job paused");
    }

    @Test
    public void testPauseJob_GivenRunningJob() throws JobException
    {
        givenProcessInfo(2);
        JobManager jobManager = createJobManager();
        JobDetails job = new JobDetails();
        job.setId("foo");
        job.setStatus(JobStatus.RUNNING);

        when(m_JobProvider.getJobDetails("foo")).thenReturn(Optional.of(job));
        when(m_ProcessManager.jobIsRunning("foo")).thenReturn(true);

        jobManager.pauseJob("foo");

        verify(m_ProcessManager).closeJob("foo");

        ArgumentCaptor<JobStatus> statusCaptor = ArgumentCaptor.forClass(JobStatus.class);
        verify(m_JobProvider, times(2)).setJobStatus(eq("foo"), statusCaptor.capture());
        assertEquals(JobStatus.PAUSING, statusCaptor.getAllValues().get(0));
        assertEquals(JobStatus.PAUSED, statusCaptor.getAllValues().get(1));

        verify(m_JobProvider).updateJob(eq("foo"), m_JobUpdateCaptor.capture());
        assertTrue(m_JobUpdateCaptor.getValue().containsKey(JobDetails.IGNORE_DOWNTIME));
        assertEquals(IgnoreDowntime.ONCE, m_JobUpdateCaptor.getValue().get(JobDetails.IGNORE_DOWNTIME));

        verify(m_JobProvider).audit("foo");
        verify(m_Auditor).info("Job paused");
    }

    @Test
    public void testPauseJob_GivenJobActionIsNotAvailable()
            throws JobException, InterruptedException, ExecutionException
    {
        givenProcessInfo(2);
        JobManager jobManager = createJobManager();
        JobDetails job = new JobDetails();
        job.setId("foo");

        when(m_JobProvider.getJobDetails("foo")).thenReturn(Optional.of(job));
        when(m_ProcessManager.jobIsRunning("foo")).thenReturn(true);
        doAnswerSleep(200).when(m_ProcessManager).closeJob("foo");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<Throwable> task_1_result = executor.submit(
                new ExceptionCallable(() -> jobManager.pauseJob("foo")));
        Future<Throwable> task_2_result = executor.submit(
                new ExceptionCallable(() -> jobManager.pauseJob("foo")));
        executor.shutdown();

        Throwable result1 = task_1_result.get();
        Throwable result2 = task_2_result.get();
        assertTrue(result1 == null || result2 == null);
        Throwable exception = result1 != null ? result1 : result2;
        assertTrue(exception instanceof JobInUseException);
        assertEquals("Cannot pause job foo while another connection is pausing the job",
                exception.getMessage());

        verify(m_ProcessManager).closeJob("foo");

        ArgumentCaptor<JobStatus> statusCaptor = ArgumentCaptor.forClass(JobStatus.class);
        verify(m_JobProvider, times(2)).setJobStatus(eq("foo"), statusCaptor.capture());
        assertEquals(JobStatus.PAUSING, statusCaptor.getAllValues().get(0));
        assertEquals(JobStatus.PAUSED, statusCaptor.getAllValues().get(1));

        verify(m_JobProvider).updateJob(eq("foo"), m_JobUpdateCaptor.capture());
        assertTrue(m_JobUpdateCaptor.getValue().containsKey(JobDetails.IGNORE_DOWNTIME));
        assertEquals(IgnoreDowntime.ONCE, m_JobUpdateCaptor.getValue().get(JobDetails.IGNORE_DOWNTIME));
    }

    @Test (expected = IllegalStateException.class)
    public void testResumeJob_GivenScheduledJob() throws JobException
    {
        givenProcessInfo(2);
        JobManager jobManager = createJobManager();

        JobConfiguration jobConfig = createScheduledJobConfig();
        Logger jobLogger = mock(Logger.class);
        when(m_JobLoggerFactory.newLogger("foo")).thenReturn(jobLogger);
        DataExtractor dataExtractor = mock(DataExtractor.class);
        when(m_DataExtractorFactory.newExtractor(any(JobDetails.class))).thenReturn(dataExtractor);

        JobDetails jd = new JobDetails();
        jd.setSchedulerConfig(new SchedulerConfig());
        when(m_JobProvider.getJobDetails("foo")).thenReturn(Optional.of(jd));

        jobManager.createJob(jobConfig, false);

        jobManager.resumeJob("foo");
    }

    @Test
    public void testResumeJob_GivenClosedJob() throws JobException
    {
        givenProcessInfo(2);
        JobManager jobManager = createJobManager();

        JobDetails job = new JobDetails();
        job.setId("foo");
        job.setStatus(JobStatus.CLOSED);
        when(m_JobProvider.getJobDetails("foo")).thenReturn(Optional.of(job));

        m_ExpectedException.expect(CannotResumeJobException.class);
        m_ExpectedException.expectMessage("Cannot resume job 'foo' while its status is CLOSED");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.CANNOT_RESUME_JOB));

        jobManager.resumeJob("foo");
    }

    @Test
    public void testResumeJob_GivenPausedJob() throws JobException
    {
        givenProcessInfo(2);
        JobManager jobManager = createJobManager();

        JobDetails job = new JobDetails();
        job.setId("foo");
        job.setStatus(JobStatus.PAUSED);
        when(m_JobProvider.getJobDetails("foo")).thenReturn(Optional.of(job));

        jobManager.resumeJob("foo");

        verify(m_JobProvider).setJobStatus("foo", JobStatus.CLOSED);
        verify(m_JobProvider).audit("foo");
        verify(m_Auditor).info("Job resumed");
    }

    @Test
    public void testResumeJob_GivenJobActionIsNotAvailable()
            throws JobException, InterruptedException, ExecutionException
    {
        givenProcessInfo(2);
        JobManager jobManager = createJobManager();
        JobDetails job = new JobDetails();
        job.setId("foo");
        job.setStatus(JobStatus.PAUSED);
        when(m_JobProvider.getJobDetails("foo")).thenReturn(Optional.of(job));

        doAnswerSleep(200).when(m_JobProvider).setJobStatus("foo", JobStatus.CLOSED);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<Throwable> task_1_result = executor.submit(
                new ExceptionCallable(() -> jobManager.resumeJob("foo")));
        Future<Throwable> task_2_result = executor.submit(
                new ExceptionCallable(() -> jobManager.resumeJob("foo")));
        executor.shutdown();

        Throwable result1 = task_1_result.get();
        Throwable result2 = task_2_result.get();
        assertTrue(result1 == null || result2 == null);
        Throwable exception = result1 != null ? result1 : result2;
        assertTrue(exception instanceof JobInUseException);
        assertEquals("Cannot resume job foo while another connection is resuming the job",
                exception.getMessage());

        verify(m_JobProvider).setJobStatus("foo", JobStatus.CLOSED);
    }

    @Test
    public void testUpdateSchedulerConfig_GivenSchedulerIsRunning() throws JobException
    {
        givenProcessInfo(2);
        JobManager jobManager = createJobManager();

        JobConfiguration jobConfig = createScheduledJobConfig();
        Logger jobLogger = mock(Logger.class);
        when(m_JobLoggerFactory.newLogger("foo")).thenReturn(jobLogger);
        DataExtractor dataExtractor = mock(DataExtractor.class);
        when(m_DataExtractorFactory.newExtractor(any(JobDetails.class))).thenReturn(dataExtractor);

        JobDetails job = jobManager.createJob(jobConfig, false);
        DataCounts dataCounts = new DataCounts();
        dataCounts.setLatestRecordTimeStamp(new Date(0));
        job.setCounts(dataCounts);
        when(m_JobProvider.getJobDetails("foo")).thenReturn(Optional.of(job));
        jobManager.startJobScheduler("foo", 0L, OptionalLong.empty());

        SchedulerConfig newSchedulerConfig = new SchedulerConfig();

        m_ExpectedException.expect(JobInUseException.class);
        m_ExpectedException.expectMessage("Cannot update scheduler for job 'foo' while its status is started");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.CANNOT_UPDATE_JOB_SCHEDULER));
        jobManager.updateSchedulerConfig("foo", newSchedulerConfig);

        verify(m_JobProvider).updateSchedulerConfig("foo", newSchedulerConfig);
        jobManager.stopJobScheduler("foo");
    }

    @Test
    public void testUpdateSchedulerConfig_GivenValid() throws JobException, GeneralSecurityException
    {
        givenProcessInfo(2);
        JobManager jobManager = createJobManager();

        JobConfiguration jobConfig = createScheduledJobConfig();
        Logger jobLogger = mock(Logger.class);
        when(m_JobLoggerFactory.newLogger("foo")).thenReturn(jobLogger);

        JobDetails job = jobManager.createJob(jobConfig, false);
        DataCounts dataCounts = new DataCounts();
        dataCounts.setLatestRecordTimeStamp(new Date(0));
        job.setCounts(dataCounts);
        when(m_JobProvider.getJobDetails("foo")).thenReturn(Optional.of(job));

        SchedulerConfig newSchedulerConfig = createScheduledJobConfig().getSchedulerConfig();
        newSchedulerConfig.setUsername("bar");
        newSchedulerConfig.setPassword("1234");
        newSchedulerConfig.fillDefaults();
        when(m_JobProvider.updateSchedulerConfig("foo", newSchedulerConfig)).thenReturn(true);

        jobManager.updateSchedulerConfig("foo", newSchedulerConfig);

        verify(m_PasswordManager).secureStorage(newSchedulerConfig);
        verify(m_JobProvider).updateSchedulerConfig("foo", newSchedulerConfig);
    }

    @Test
    public void testUpdateSchedulerConfig_GivenValidButUpdateFails() throws JobException, GeneralSecurityException
    {
        givenProcessInfo(2);
        JobManager jobManager = createJobManager();

        JobConfiguration jobConfig = createScheduledJobConfig();
        Logger jobLogger = mock(Logger.class);
        when(m_JobLoggerFactory.newLogger("foo")).thenReturn(jobLogger);

        JobDetails job = jobManager.createJob(jobConfig, false);
        DataCounts dataCounts = new DataCounts();
        dataCounts.setLatestRecordTimeStamp(new Date(0));
        job.setCounts(dataCounts);
        when(m_JobProvider.getJobDetails("foo")).thenReturn(Optional.of(job));

        SchedulerConfig newSchedulerConfig = createScheduledJobConfig().getSchedulerConfig();
        newSchedulerConfig.setUsername("bar");
        newSchedulerConfig.setPassword("1234");
        newSchedulerConfig.fillDefaults();
        when(m_JobProvider.updateSchedulerConfig("foo", newSchedulerConfig)).thenReturn(false);

        jobManager.updateSchedulerConfig("foo", newSchedulerConfig);

        verify(m_PasswordManager).secureStorage(newSchedulerConfig);
        verify(m_JobProvider).updateSchedulerConfig("foo", newSchedulerConfig);
    }

    @Test
    public void testSetAnalysisLimits() throws UnknownJobException
    {
        givenProcessInfo(2);
        JobManager jobManager = createJobManager();

        AnalysisLimits newLimits = new AnalysisLimits();
        newLimits.setModelMemoryLimit(1L);
        newLimits.setCategorizationExamplesLimit(2L);

        jobManager.setAnalysisLimits("foo", newLimits);

        verify(m_JobProvider).updateJob(eq("foo"), m_JobUpdateCaptor.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> capturedLimits = (Map<String, Object>) m_JobUpdateCaptor.getValue()
                .get(JobDetails.ANALYSIS_LIMITS);
        assertNotNull(capturedLimits);
        assertEquals(1L, capturedLimits.get(AnalysisLimits.MODEL_MEMORY_LIMIT));
        assertEquals(2L, capturedLimits.get(AnalysisLimits.CATEGORIZATION_EXAMPLES_LIMIT));
    }

    private void givenProcessInfo(int maxLicenseJobs)
    {
        String info = String.format("{\"jobs\":\"%d\"}", maxLicenseJobs);
        when(m_ProcessManager.getInfo()).thenReturn(info);
    }

    private void givenProcessInfo(int maxLicenseJobs, int maxLicenseDetectors)
    {
        String info = String.format("{\"jobs\":\"%d\", \"detectors\":\"%d\"}",
                    maxLicenseJobs, maxLicenseDetectors);
        when(m_ProcessManager.getInfo()).thenReturn(info);
    }

    private void givenLicenseConstraints(int maxJobs, int maxDetectors, int maxPartitions)
    {
        String info = String.format("{\"%s\":%d, \"%s\":%d, \"%s\":%d}",
                                 BackendInfo.JOBS_LICENSE_CONSTRAINT, maxJobs,
                                 BackendInfo.DETECTORS_LICENSE_CONSTRAINT, maxDetectors,
                                 BackendInfo.PARTITIONS_LICENSE_CONSTRAINT, maxPartitions);
        when(m_ProcessManager.getInfo()).thenReturn(info);
    }

    private List<String> createJobIds(int jobCount)
    {
        List<String> jobIds = new ArrayList<>();
        for (int i=0; i<jobCount; i++)
        {
            jobIds.add(Integer.toString(i));
        }
        return jobIds;
    }

    private JobManager createJobManager()
    {
        return new JobManager(m_JobProvider, m_ProcessManager, m_DataExtractorFactory,
                m_JobLoggerFactory, m_PasswordManager, m_JobDataDeleter,
                new LocalActionGuardian<Action>(Action.CLOSED),
                new LocalActionGuardian<ScheduledAction>(ScheduledAction.STOPPED), false);
    }

    private static Answer<Object> writeToWriter()
    {
        return new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws IOException
            {
                CsvRecordWriter writer = (CsvRecordWriter) invocation.getArguments()[1];
                writer.writeRecord(new String [] {"csv","header","one"});
                writer.flush();
                return null;
            }
        };
    }

    private static JobConfiguration createScheduledJobConfig()
    {
        AnalysisConfig analysisConfig = new AnalysisConfig();
        analysisConfig.setBucketSpan(3600L);
        analysisConfig.setDetectors(Arrays.asList(new Detector()));

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

    private static MockBatchedDocumentsIterator<JobDetails> newBatchedJobsIterator(List<JobDetails> jobs)
    {
        Deque<JobDetails> batch1 = new ArrayDeque<>();
        Deque<JobDetails> batch2 = new ArrayDeque<>();
        for (int i = 0; i < jobs.size(); i++)
        {
            if (i == 0)
            {
                batch1.add(jobs.get(i));
            }
            else
            {
                batch2.add(jobs.get(i));
            }
        }
        List<Deque<JobDetails>> batches = new ArrayList<>();
        batches.add(batch1);
        batches.add(batch2);
        return new MockBatchedDocumentsIterator<>(batches);
    }
}
