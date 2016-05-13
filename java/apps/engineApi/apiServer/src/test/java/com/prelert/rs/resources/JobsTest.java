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

package com.prelert.rs.resources;

import static com.prelert.job.errorcodes.ErrorCodeMatcher.hasErrorCode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.Detector;
import com.prelert.job.JobConfiguration;
import com.prelert.job.JobDetails;
import com.prelert.job.JobException;
import com.prelert.job.JobIdAlreadyExistsException;
import com.prelert.job.ModelDebugConfig;
import com.prelert.job.ModelDebugConfig.DebugDestination;
import com.prelert.job.UnknownJobException;
import com.prelert.job.config.verification.JobConfigurationException;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.exceptions.LicenseViolationException;
import com.prelert.job.persistence.DataStoreException;
import com.prelert.job.persistence.QueryPage;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.job.scheduler.CannotStartSchedulerException;
import com.prelert.job.scheduler.CannotStopSchedulerException;
import com.prelert.rs.data.Acknowledgement;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;
import com.prelert.rs.exception.ActionNotAllowedForScheduledJobException;
import com.prelert.rs.exception.InvalidParametersException;

public class JobsTest extends ServiceTest
{
    @Rule public ExpectedException m_ExpectedException = ExpectedException.none();

    private Jobs m_Jobs;

    @Before
    public void setUp() throws UnknownJobException
    {
        m_Jobs = new Jobs();
        configureService(m_Jobs);
    }

    @Test
    public void testJobs_GivenNegativeSkip()
    {
        m_ExpectedException.expect(InvalidParametersException.class);
        m_ExpectedException.expectMessage("Parameter 'skip' cannot be < 0");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_SKIP_PARAM));

        m_Jobs.jobs(-1, 100);
    }

    @Test
    public void testJobs_GivenNegativeTake()
    {
        m_ExpectedException.expect(InvalidParametersException.class);
        m_ExpectedException.expectMessage("Parameter 'take' cannot be < 0");
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_TAKE_PARAM));

        m_Jobs.jobs(0, -1);
    }

    @Test
    public void testJobs() throws UnknownJobException
    {
        JobDetails job1 = new JobDetails();
        job1.setId("job_1");
        JobDetails job2 = new JobDetails();
        job2.setId("job_2");
        JobDetails job3 = new JobDetails();
        job3.setId("job_3");
        QueryPage<JobDetails> results = new QueryPage<>(Arrays.asList(job1, job2, job3), 3);

        when(jobReader().getJobs(0, 10)).thenReturn(results);

        Pagination<JobDetails> jobs = m_Jobs.jobs(0, 10);

        assertEquals(3, jobs.getHitCount());
        assertTrue(jobs.isAllResults());
        verifyJobHasIdAndEndpoints("job_1", jobs.getDocuments().get(0));
        verifyJobHasIdAndEndpoints("job_2", jobs.getDocuments().get(1));
        verifyJobHasIdAndEndpoints("job_3", jobs.getDocuments().get(2));
    }

    @Test
    public void testJob_GivenExistingJob() throws UnknownJobException
    {
        JobDetails job = new JobDetails();
        job.setId("foo");
        Optional<JobDetails> result = Optional.of(job);

        when(jobReader().getJob("foo")).thenReturn(result);

        Response response = m_Jobs.job("foo");

        @SuppressWarnings("unchecked")
        SingleDocument<JobDetails> entity = (SingleDocument<JobDetails>) response.getEntity();
        assertTrue(entity.isExists());
        assertEquals("foo", entity.getDocument().getId());
        assertEquals(JobDetails.TYPE, entity.getType());
        assertEquals(job, entity.getDocument());
    }

    @Test
    public void testJob_GivenUnknownJob() throws UnknownJobException
    {
        when(jobReader().getJob("foo")).thenReturn(Optional.empty());

        Response response = m_Jobs.job("foo");
        assertEquals(404, response.getStatus());

        @SuppressWarnings("unchecked")
        SingleDocument<JobDetails> result = (SingleDocument<JobDetails>) response.getEntity();

        assertEquals(JobDetails.TYPE, result.getType());
        assertNull(result.getDocument());
        assertFalse(result.isExists());
    }

    @Test
    public void testCreateJob_GivenValidConfig() throws UnknownJobException,
            JobConfigurationException, LicenseViolationException, JobIdAlreadyExistsException,
            IOException, CannotStartSchedulerException, DataStoreException,
            NativeProcessRunException, JobInUseException, CannotStopSchedulerException
    {
        JobConfiguration config = createValidJobConfig();

        JobDetails job = new JobDetails();
        job.setId("foo");
        when(jobManager().createJob(config, false)).thenReturn(job);

        Response response = m_Jobs.createJob(false, config);

        assertEquals("{\"id\":\"foo\"}\n", response.getEntity());
    }

    @Test
    public void testCreateJob_GivenInvalidConfig() throws UnknownJobException,
            JobConfigurationException, LicenseViolationException, JobIdAlreadyExistsException,
            IOException, CannotStartSchedulerException, DataStoreException,
            NativeProcessRunException, JobInUseException, CannotStopSchedulerException
    {
        m_ExpectedException.expect(JobConfigurationException.class);

        m_Jobs.createJob(false, new JobConfiguration());
    }

    @Test
    public void testCreateJob_GivenServerError() throws UnknownJobException,
            JobConfigurationException, LicenseViolationException, JobIdAlreadyExistsException,
            IOException, CannotStartSchedulerException, DataStoreException,
            NativeProcessRunException, JobInUseException, CannotStopSchedulerException
    {
        JobConfiguration config = createValidJobConfig();
        when(jobManager().createJob(config, false)).thenReturn(null);

        Response response = m_Jobs.createJob(false, config);

        assertEquals(500, response.getStatus());
    }

    @Test
    public void testUpdate_GivenValidModelDebugConfig() throws JobException
    {
        Response response = m_Jobs.updateJob("foo",
                "{\"modelDebugConfig\":{\"boundsPercentile\":90.0, \"terms\":\"someTerm\"}}");
        assertEquals(200, response.getStatus());
        assertTrue(((Acknowledgement) response.getEntity()).getAcknowledgement());

        verify(jobManager()).setModelDebugConfig("foo", new ModelDebugConfig(null, 90.0, "someTerm"));
        String expectedConfig = "[modelDebugConfig]\nboundspercentile = 90.0\nterms = someTerm\n";
        verify(jobManager()).writeUpdateConfigMessage("foo", expectedConfig);
    }

    @Test
    public void testUpdate_GivenValidModelDebugConfigChangeToFile() throws JobException
    {
        Response response = m_Jobs.updateJob("foo",
                "{\"modelDebugConfig\":{\"writeTo\":\"File\",\"boundsPercentile\":90.0, \"terms\":\"someTerm\"}}");
        assertEquals(200, response.getStatus());
        assertTrue(((Acknowledgement) response.getEntity()).getAcknowledgement());

        verify(jobManager()).setModelDebugConfig("foo", new ModelDebugConfig(DebugDestination.FILE, 90.0, "someTerm"));
        String expectedConfig = "[modelDebugConfig]\nwriteto = FILE\nboundspercentile = 90.0\nterms = someTerm\n";
        verify(jobManager()).writeUpdateConfigMessage("foo", expectedConfig);
    }

    @Test
    public void testUpdate_GivenValidModelDebugConfigChangeToDataStore() throws JobException
    {
        Response response = m_Jobs.updateJob("foo",
                "{\"modelDebugConfig\":{\"writeTo\":\"data_store\",\"boundsPercentile\":90.0, \"terms\":\"someTerm\"}}");
        assertEquals(200, response.getStatus());
        assertTrue(((Acknowledgement) response.getEntity()).getAcknowledgement());

        verify(jobManager()).setModelDebugConfig("foo", new ModelDebugConfig(DebugDestination.DATA_STORE, 90.0, "someTerm"));
        String expectedConfig = "[modelDebugConfig]\nwriteto = DATA_STORE\nboundspercentile = 90.0\nterms = someTerm\n";
        verify(jobManager()).writeUpdateConfigMessage("foo", expectedConfig);
    }

    @Test
    public void testPauseJob_GivenScheduledJob() throws JobException
    {
        when(jobManager().isScheduledJob("foo")).thenReturn(true);

        m_ExpectedException.expect(ActionNotAllowedForScheduledJobException.class);
        m_ExpectedException.expectMessage("This action is not allowed for a scheduled job");
        m_ExpectedException.expect(hasErrorCode(ErrorCodes.ACTION_NOT_ALLOWED_FOR_SCHEDULED_JOB));

        m_Jobs.pauseJob("foo");
    }

    @Test
    public void testPauseJob() throws JobException
    {
        when(jobManager().isScheduledJob("bar")).thenReturn(false);

        m_Jobs.pauseJob("bar");

        verify(jobManager()).pauseJob("bar");
    }

    @Test
    public void testResumeJob_GivenScheduledJob() throws JobException
    {
        when(jobManager().isScheduledJob("foo")).thenReturn(true);

        m_ExpectedException.expect(ActionNotAllowedForScheduledJobException.class);
        m_ExpectedException.expectMessage("This action is not allowed for a scheduled job");
        m_ExpectedException.expect(hasErrorCode(ErrorCodes.ACTION_NOT_ALLOWED_FOR_SCHEDULED_JOB));

        m_Jobs.resumeJob("foo");
    }

    @Test
    public void testResumeJob() throws JobException
    {
        when(jobManager().isScheduledJob("bar")).thenReturn(false);

        m_Jobs.resumeJob("bar");

        verify(jobManager()).resumeJob("bar");
    }

    private static void verifyJobHasIdAndEndpoints(String jobId, JobDetails job)
    {
        assertEquals(jobId, job.getId());
        assertEquals(createUri("http://localhost/test/jobs/" + jobId), job.getLocation());
        Map<String, URI> endpoints = job.getEndpoints();
        assertEquals(createUri("http://localhost/test/data/" + jobId), endpoints.get("data"));
        assertEquals(createUri("http://localhost/test/results/" + jobId + "/buckets"),
                endpoints.get("buckets"));
        assertEquals(createUri("http://localhost/test/results/" + jobId + "/categorydefinitions"),
                endpoints.get("categoryDefinitions"));
        assertEquals(createUri("http://localhost/test/results/" + jobId + "/records"),
                endpoints.get("records"));
        assertEquals(createUri("http://localhost/test/logs/" + jobId), endpoints.get("logs"));
        assertEquals(createUri("http://localhost/test/alerts_longpoll/" + jobId),
                endpoints.get("alertsLongPoll"));
        assertEquals(createUri("http://localhost/test/modelsnapshots/" + jobId),
                endpoints.get("modelSnapshots"));
    }

    private static URI createUri(String url)
    {
        try
        {
            return new URI(url);
        } catch (URISyntaxException e)
        {
            fail();
            return null;
        }
    }

    private static JobConfiguration createValidJobConfig()
    {
        Detector detector = new Detector();
        detector.setFunction("count");
        AnalysisConfig analysisConfig = new AnalysisConfig();
        analysisConfig.setDetectors(Arrays.asList(detector));
        DataDescription dataDescription = new DataDescription();
        JobConfiguration config = new JobConfiguration();
        config.setAnalysisConfig(analysisConfig);
        config.setDataDescription(dataDescription);
        return config;
    }
}
