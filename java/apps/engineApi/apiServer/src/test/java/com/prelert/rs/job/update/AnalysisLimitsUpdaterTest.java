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

package com.prelert.rs.job.update;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prelert.job.AnalysisLimits;
import com.prelert.job.JobDetails;
import com.prelert.job.JobException;
import com.prelert.job.JobStatus;
import com.prelert.job.UnknownJobException;
import com.prelert.job.config.verification.JobConfigurationException;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.manager.JobManager;
import com.prelert.rs.provider.JobConfigurationParseException;

public class AnalysisLimitsUpdaterTest
{
    @Rule public ExpectedException m_ExpectedException = ExpectedException.none();

    @Mock private JobManager m_JobManager;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testPrepareUpdate_GivenJobIsNotClosed() throws JobException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage(
                "Cannot update key 'analysisLimits' while job is not closed; current status is RUNNING");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.JOB_NOT_CLOSED));

        givenJob("foo", JobStatus.RUNNING, 42L);
        createUpdater("foo").prepareUpdate(null);
    }

    @Test
    public void testPrepareUpdate_GivenNull() throws JobException, IOException
    {
        givenJob("foo", JobStatus.CLOSED, 42L);
        String update = "null";
        JsonNode node = new ObjectMapper().readTree(update);

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid update value for analysisLimits: null");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        createUpdater("foo").prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenInvalidJson() throws JobException, IOException
    {
        givenJob("foo", JobStatus.CLOSED, 42L);
        String update = "{\"modelMemory!!Limit\":50}";
        JsonNode node = new ObjectMapper().readTree(update);

        m_ExpectedException.expect(JobConfigurationParseException.class);
        m_ExpectedException.expectMessage("JSON parse error reading the update value for analysisLimits");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        createUpdater("foo").prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenCategorizationExamplesLimitLessThanZero()
            throws JobException, IOException
    {
        givenJob("foo", JobStatus.CLOSED, 0L);
        String update = "{\"categorizationExamplesLimit\":-1}";
        JsonNode node = new ObjectMapper().readTree(update);

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("categorizationExamplesLimit cannot be less than 0. Value = -1");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        createUpdater("foo").prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenModelMemoryLimitLessIsDecreased()
            throws JobException, IOException
    {
        givenJob("foo", JobStatus.CLOSED, 42L);
        String update = "{\"modelMemoryLimit\":41}";
        JsonNode node = new ObjectMapper().readTree(update);

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid update value for analysisLimits: modelMemoryLimit cannot be decreased; existing is 42, update had 41");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        createUpdater("foo").prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdateAndCommit_GivenValid() throws JobException, IOException
    {
        givenJob("foo", JobStatus.CLOSED, 42L);
        String update = "{\"modelMemoryLimit\":43, \"categorizationExamplesLimit\": 5}";
        JsonNode node = new ObjectMapper().readTree(update);

        AnalysisLimitsUpdater updater = createUpdater("foo");
        updater.prepareUpdate(node);
        updater.commit();

        AnalysisLimits newLimits = new AnalysisLimits();
        newLimits.setModelMemoryLimit(43);
        newLimits.setCategorizationExamplesLimit(5L);
        verify(m_JobManager).setAnalysisLimits("foo", newLimits);
    }

    @Test
    public void testPrepareUpdateAndCommit_GivenValidAndExistingLimitsIsNull()
            throws JobException, IOException
    {
        givenJob("foo", JobStatus.CLOSED, null);
        String update = "{\"modelMemoryLimit\":43, \"categorizationExamplesLimit\": 5}";
        JsonNode node = new ObjectMapper().readTree(update);

        AnalysisLimitsUpdater updater = createUpdater("foo");
        updater.prepareUpdate(node);
        updater.commit();

        AnalysisLimits newLimits = new AnalysisLimits();
        newLimits.setModelMemoryLimit(43);
        newLimits.setCategorizationExamplesLimit(5L);
        verify(m_JobManager).setAnalysisLimits("foo", newLimits);
    }

    private void givenJob(String jobId, JobStatus jobStatus, Long memoryLimit) throws UnknownJobException
    {
        JobDetails job = new JobDetails();
        job.setId(jobId);
        job.setStatus(jobStatus);
        if (memoryLimit != null)
        {
            AnalysisLimits analysisLimits = new AnalysisLimits();
            analysisLimits.setModelMemoryLimit(memoryLimit);
            job.setAnalysisLimits(analysisLimits);
        }
        when(m_JobManager.getJobOrThrowIfUnknown(jobId)).thenReturn(job);
    }

    private AnalysisLimitsUpdater createUpdater(String jobId)
    {
        return new AnalysisLimitsUpdater(m_JobManager, jobId, "analysisLimits");
    }
}
