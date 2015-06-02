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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

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
import com.prelert.job.exceptions.JobConfigurationException;
import com.prelert.job.exceptions.JobIdAlreadyExistsException;
import com.prelert.job.exceptions.TooManyJobsException;
import com.prelert.job.exceptions.UnknownJobException;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;

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
    public void testJobs() throws UnknownJobException
    {
        Pagination<JobDetails> results = new Pagination<>();
        results.setHitCount(3);
        JobDetails job1 = new JobDetails();
        job1.setId("job_1");
        JobDetails job2 = new JobDetails();
        job2.setId("job_2");
        JobDetails job3 = new JobDetails();
        job3.setId("job_3");
        results.setDocuments(Arrays.asList(job1, job2, job3));

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
        SingleDocument<JobDetails> result = new SingleDocument<>();
        JobDetails job = new JobDetails();
        job.setId("foo");
        result.setDocument(job);
        result.setExists(true);

        when(jobManager().getJob("foo")).thenReturn(result);

        Response response = m_Jobs.job("foo");

        assertEquals(result, response.getEntity());
    }

    @Test
    public void testJob_GivenUnknownJob() throws UnknownJobException
    {
        doThrow(new UnknownJobException("foo")).when(jobManager()).getJob("foo");

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
            IOException
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
            IOException
    {
        m_ExpectedException.expect(JobConfigurationException.class);

        m_Jobs.createJob(new JobConfiguration());
    }

    @Test
    public void testCreateJob_GivenServerError() throws UnknownJobException,
            JobConfigurationException, TooManyJobsException, JobIdAlreadyExistsException,
            IOException
    {
        JobConfiguration config = createValidJobConfig();
        when(jobManager().createJob(config)).thenReturn(null);

        Response response = m_Jobs.createJob(config);

        assertEquals(500, response.getStatus());
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
