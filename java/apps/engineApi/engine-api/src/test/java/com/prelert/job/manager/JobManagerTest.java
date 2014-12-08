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
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.Map;

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
import com.prelert.job.JobInUseException;
import com.prelert.job.JobStatus;
import com.prelert.job.TooManyJobsException;
import com.prelert.job.UnknownJobException;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.process.ProcessManager;
import com.prelert.job.process.exceptions.MissingFieldException;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.status.HighProportionOfBadTimestampsException;
import com.prelert.job.status.OutOfOrderRecordsException;

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

    @Test
    public void testSubmitDataLoadJob_GivenProcessIsRunningMoreJobsThanLicenseAllows()
            throws JsonParseException, UnknownJobException, NativeProcessRunException,
            MissingFieldException, JobInUseException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, TooManyJobsException
    {
        when(m_JobProvider.getJobDetails("foo")).thenReturn(
                new JobDetails("foo", new JobConfiguration()));
        givenProcessInfo(5);
        when(m_ProcessManager.jobIsRunning("foo")).thenReturn(false);
        when(m_ProcessManager.numberOfRunningJobs()).thenReturn(5);
        JobManager jobManager = new JobManager(m_JobProvider, m_ProcessManager);

        m_ExpectedException.expect(TooManyJobsException.class);
        m_ExpectedException.expectMessage("Cannot reactivate job with id 'foo' - your license "
                + "limits you to 5 concurrently running jobs.  You must close a job before you "
                + "can reactivate a closed one.");

        jobManager.submitDataLoadJob("foo", mock(InputStream.class));
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testSubmitDataLoadJob_GivenProcessIsRunSuccessfully()
            throws JsonParseException, UnknownJobException, NativeProcessRunException,
            MissingFieldException, JobInUseException, HighProportionOfBadTimestampsException,
            OutOfOrderRecordsException, TooManyJobsException
    {
        InputStream inputStream = mock(InputStream.class);
        when(m_JobProvider.getJobDetails("foo")).thenReturn(
                new JobDetails("foo", new JobConfiguration()));
        givenProcessInfo(5);
        when(m_ProcessManager.jobIsRunning("foo")).thenReturn(false);
        when(m_ProcessManager.numberOfRunningJobs()).thenReturn(3);
        when(m_ProcessManager.processDataLoadJob("foo", inputStream)).thenReturn(true);
        JobManager jobManager = new JobManager(m_JobProvider, m_ProcessManager);

        assertTrue(jobManager.submitDataLoadJob("foo", inputStream));
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
