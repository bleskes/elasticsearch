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

package com.prelert.rs.resources;

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
import com.prelert.job.JobIdAlreadyExistsException;
import com.prelert.job.ModelDebugConfig;
import com.prelert.job.UnknownJobException;
import com.prelert.job.config.verification.JobConfigurationException;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.exceptions.TooManyJobsException;
import com.prelert.job.manager.CannotStartSchedulerWhileItIsStoppingException;
import com.prelert.job.persistence.QueryPage;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.rs.data.Acknowledgement;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;
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

        when(jobManager().getJobs(0, 10)).thenReturn(results);

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

        when(jobManager().getJob("foo")).thenReturn(result);

        Response response = m_Jobs.job("foo");

        @SuppressWarnings("unchecked")
        SingleDocument<JobDetails> entity = (SingleDocument<JobDetails>) response.getEntity();
        assertTrue(entity.isExists());
        assertEquals("foo", entity.getDocumentId());
        assertEquals(JobDetails.TYPE, entity.getType());
        assertEquals(job, entity.getDocument());
    }

    @Test
    public void testJob_GivenUnknownJob() throws UnknownJobException
    {
        when(jobManager().getJob("foo")).thenReturn(Optional.empty());

        Response response = m_Jobs.job("foo");
        assertEquals(404, response.getStatus());

        @SuppressWarnings("unchecked")
        SingleDocument<JobDetails> result = (SingleDocument<JobDetails>) response.getEntity();

        assertEquals(JobDetails.TYPE, result.getType());
        assertEquals("foo", result.getDocumentId());
        assertNull(result.getDocument());
        assertFalse(result.isExists());
    }

    @Test
    public void testCreateJob_GivenValidConfig() throws UnknownJobException,
            JobConfigurationException, TooManyJobsException, JobIdAlreadyExistsException,
            IOException, CannotStartSchedulerWhileItIsStoppingException
    {
        JobConfiguration config = createValidJobConfig();

        JobDetails job = new JobDetails();
        job.setId("foo");
        when(jobManager().createJob(config)).thenReturn(job);

        Response response = m_Jobs.createJob(config);

        assertEquals("{\"id\":\"foo\"}\n", response.getEntity());
    }

    @Test
    public void testCreateJob_GivenInvalidConfig() throws UnknownJobException,
            JobConfigurationException, TooManyJobsException, JobIdAlreadyExistsException,
            IOException, CannotStartSchedulerWhileItIsStoppingException
    {
        m_ExpectedException.expect(JobConfigurationException.class);

        m_Jobs.createJob(new JobConfiguration());
    }

    @Test
    public void testCreateJob_GivenServerError() throws UnknownJobException,
            JobConfigurationException, TooManyJobsException, JobIdAlreadyExistsException,
            IOException, CannotStartSchedulerWhileItIsStoppingException
    {
        JobConfiguration config = createValidJobConfig();
        when(jobManager().createJob(config)).thenReturn(null);

        Response response = m_Jobs.createJob(config);

        assertEquals(500, response.getStatus());
    }

    @Test
    public void testUpdate_GivenValidModelDebugConfig() throws UnknownJobException,
            JobConfigurationException, JobInUseException, NativeProcessRunException
    {
        Response response = m_Jobs.updateJob("foo",
                "{\"modelDebugConfig\":{\"boundsPercentile\":90.0, \"terms\":\"someTerm\"}}");
        assertEquals(200, response.getStatus());
        assertTrue(((Acknowledgement) response.getEntity()).getAcknowledgement());

        verify(jobManager()).setModelDebugConfig("foo", new ModelDebugConfig(90.0, "someTerm"));
        String expectedConfig = "[modelDebugConfig]\nboundspercentile = 90.0\nterms = someTerm\n";
        verify(jobManager()).writeUpdateConfigMessage("foo", expectedConfig);
    }

    private static void verifyJobHasIdAndEndpoints(String jobId, JobDetails job)
    {
        assertEquals(jobId, job.getId());
        assertEquals(createUri("http://localhost/test/jobs/" + jobId), job.getLocation());
        assertEquals(createUri("http://localhost/test/data/" + jobId), job.getDataEndpoint());
        assertEquals(createUri("http://localhost/test/results/" + jobId + "/buckets"),
                job.getBucketsEndpoint());
        assertEquals(createUri("http://localhost/test/results/" + jobId + "/categorydefinitions"),
                job.getCategoryDefinitionsEndpoint());
        assertEquals(createUri("http://localhost/test/results/" + jobId + "/records"),
                job.getRecordsEndpoint());
        assertEquals(createUri("http://localhost/test/logs/" + jobId), job.getLogsEndpoint());
        assertEquals(createUri("http://localhost/test/alerts_longpoll/" + jobId),
                            job.getAlertsLongPollEndpoint());
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
