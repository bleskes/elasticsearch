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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.core.JsonParseException;
import com.prelert.job.JobConfiguration;
import com.prelert.job.JobDetails;
import com.prelert.job.JobStatus;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.exceptions.TooManyJobsException;
import com.prelert.job.exceptions.UnknownJobException;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.process.ProcessManager;
import com.prelert.job.process.exceptions.MalformedJsonException;
import com.prelert.job.process.exceptions.MissingFieldException;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.process.params.DataLoadParams;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;
import com.prelert.rs.data.DataCounts;

public class JobManagerTest
{
    @Rule
    public ExpectedException m_ExpectedException = ExpectedException.none();

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
                new JobDetails("foo", new JobConfiguration()));
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
                new JobDetails("foo", new JobConfiguration()));
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
                new JobDetails("foo", new JobConfiguration()));
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
                new JobDetails("foo", new JobConfiguration()));
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
                new JobDetails("foo", new JobConfiguration()));
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

    private void givenProcessInfo(int maxLicenseJobs)
    {
        String info = String.format("{\"jobs\":\"%d\"}", maxLicenseJobs);
        when(m_ProcessManager.getInfo()).thenReturn(info);
    }
}
