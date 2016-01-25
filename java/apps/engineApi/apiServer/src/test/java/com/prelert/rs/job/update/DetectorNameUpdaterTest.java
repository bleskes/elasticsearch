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
 * are owned by Prelert Ltd. No part of this source code    *
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.prelert.job.AnalysisConfig;
import com.prelert.job.Detector;
import com.prelert.job.JobDetails;
import com.prelert.job.UnknownJobException;
import com.prelert.job.config.verification.JobConfigurationException;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.manager.JobManager;

public class DetectorNameUpdaterTest
{
    private static final String JOB_ID = "foo";

    @Rule public ExpectedException m_ExpectedException = ExpectedException.none();

    @Mock private JobManager m_JobManager;
    private JobDetails m_Job;

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        m_Job = new JobDetails();
        m_Job.setId(JOB_ID);
        when(m_JobManager.getJob(JOB_ID)).thenReturn(Optional.of(m_Job));
    }

    @Test
    public void testUpdate_GivenUnknownJob() throws UnknownJobException,
            JobConfigurationException, IOException
    {
        m_ExpectedException.expect(UnknownJobException.class);
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.MISSING_JOB_ERROR));

        JsonNode node = new ObjectMapper().readTree("{\"index\":1}");
        when(m_JobManager.getJob("unknown")).thenReturn(Optional.empty());

        new DetectorNameUpdater(m_JobManager, "unknown").update(node);
    }

    @Test
    public void testUpdate_GivenParamIsNotJsonObject() throws UnknownJobException,
            JobConfigurationException, IOException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid parameters: expected [index, name]");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        JsonNode node = TextNode.valueOf("foo");

        new DetectorNameUpdater(m_JobManager, JOB_ID).update(node);
    }

    @Test
    public void testUpdate_GivenMissingNameParam() throws UnknownJobException,
            JobConfigurationException, IOException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid parameters: expected [index, name]");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        JsonNode node = new ObjectMapper().readTree("{\"index\":1}");

        new DetectorNameUpdater(m_JobManager, JOB_ID).update(node);
    }

    @Test
    public void testUpdate_GivenMissingIndexParam() throws UnknownJobException,
            JobConfigurationException, IOException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid parameters: expected [index, name]");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        JsonNode node = new ObjectMapper().readTree("{\"name\":\"bar\"}");

        new DetectorNameUpdater(m_JobManager, JOB_ID).update(node);
    }

    @Test
    public void testUpdate_GivenEmptyObject() throws UnknownJobException,
            JobConfigurationException, IOException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid parameters: expected [index, name]");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        JsonNode node = new ObjectMapper().readTree("{}");

        new DetectorNameUpdater(m_JobManager, JOB_ID).update(node);
    }

    @Test
    public void testUpdate_GivenIndexIsNotInteger() throws UnknownJobException,
            JobConfigurationException, IOException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid index: integer expected; actual was: a string");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        JsonNode node = new ObjectMapper().readTree("{\"index\":\"a string\", \"name\":\"bar\"}");

        new DetectorNameUpdater(m_JobManager, JOB_ID).update(node);
    }

    @Test
    public void testUpdate_GivenNameIsNotString() throws UnknownJobException,
            JobConfigurationException, IOException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid name: string expected; actual was: 1");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        JsonNode node = new ObjectMapper().readTree("{\"index\":0, \"name\":1}");

        new DetectorNameUpdater(m_JobManager, JOB_ID).update(node);
    }

    @Test
    public void testUpdate_GivenIndexIsNegative() throws UnknownJobException,
            JobConfigurationException, IOException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid index: valid range is [0, 1]; actual was: -1");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));
        givenJobHasNDetectors(2);

        JsonNode node = new ObjectMapper().readTree("{\"index\":-1, \"name\":\"bar\"}");

        new DetectorNameUpdater(m_JobManager, JOB_ID).update(node);
    }

    @Test
    public void testUpdate_GivenIndexIsEqualToDetectorsCount() throws UnknownJobException,
            JobConfigurationException, IOException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid index: valid range is [0, 2]; actual was: 3");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));
        givenJobHasNDetectors(3);

        JsonNode node = new ObjectMapper().readTree("{\"index\":3, \"name\":\"bar\"}");

        new DetectorNameUpdater(m_JobManager, JOB_ID).update(node);
    }

    @Test
    public void testUpdate_GivenIndexIsGreaterThanDetectorsCount() throws UnknownJobException,
            JobConfigurationException, IOException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid index: valid range is [0, 2]; actual was: 4");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));
        givenJobHasNDetectors(3);

        JsonNode node = new ObjectMapper().readTree("{\"index\":4, \"name\":\"bar\"}");

        new DetectorNameUpdater(m_JobManager, JOB_ID).update(node);
    }

    @Test
    public void testUpdate_GivenValidParamsButUpdateFails() throws UnknownJobException,
            JobConfigurationException, IOException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage(
                "Update failed. Please see the logs to trace the cause of the failure.");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.UNKNOWN_ERROR));
        givenJobHasNDetectors(3);

        JsonNode node = new ObjectMapper().readTree("{\"index\":0, \"name\":\"bar\"}");

        new DetectorNameUpdater(m_JobManager, JOB_ID).update(node);
    }

    @Test
    public void testUpdate_GivenValidParams() throws UnknownJobException,
            JobConfigurationException, IOException
    {
        JsonNode node = new ObjectMapper().readTree("{\"index\":1, \"name\":\"Ipanema\"}");
        givenJobHasNDetectors(3);
        givenUpdateSucceeds(1, "Ipanema");

        new DetectorNameUpdater(m_JobManager, JOB_ID).update(node);

        verify(m_JobManager).updateDetectorName(JOB_ID, 1, "Ipanema");
    }

    private void givenJobHasNDetectors(int n)
    {
        AnalysisConfig analysisConfig = new AnalysisConfig();
        List<Detector> detectors = new ArrayList<>();
        for (int i = 0; i< n; i++)
        {
            detectors.add(new Detector());
        }
        analysisConfig.setDetectors(detectors);
        m_Job.setAnalysisConfig(analysisConfig);
    }

    private void givenUpdateSucceeds(int detectorIndex, String newName) throws UnknownJobException
    {
        when(m_JobManager.updateDetectorName(JOB_ID, detectorIndex, newName)).thenReturn(true);
    }
}
