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

import static org.mockito.Mockito.never;
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
import com.prelert.job.JobException;
import com.prelert.job.UnknownJobException;
import com.prelert.job.config.verification.JobConfigurationException;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.manager.JobManager;

public class DetectorDescriptionUpdaterTest
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
    public void testPrepareUpdate_GivenUnknownJob() throws JobException, IOException
    {
        m_ExpectedException.expect(UnknownJobException.class);
        m_ExpectedException.expect(ErrorCodeMatcher.hasErrorCode(ErrorCodes.MISSING_JOB_ERROR));

        JsonNode node = new ObjectMapper().readTree("{\"index\":1}");
        when(m_JobManager.getJob("unknown")).thenReturn(Optional.empty());

        new DetectorDescriptionUpdater(m_JobManager, "unknown").prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenParamIsNotJsonObject() throws JobException, IOException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid parameters: expected [index, description]");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        JsonNode node = TextNode.valueOf("foo");

        new DetectorDescriptionUpdater(m_JobManager, JOB_ID).prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenMissingDescriptionParam() throws JobException, IOException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid parameters: expected [index, description]");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        JsonNode node = new ObjectMapper().readTree("{\"index\":1}");

        new DetectorDescriptionUpdater(m_JobManager, JOB_ID).prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenMissingIndexParam() throws JobException, IOException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid parameters: expected [index, description]");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        JsonNode node = new ObjectMapper().readTree("{\"name\":\"bar\"}");

        new DetectorDescriptionUpdater(m_JobManager, JOB_ID).prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenEmptyObject() throws JobException, IOException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid parameters: expected [index, description]");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        JsonNode node = new ObjectMapper().readTree("{}");

        new DetectorDescriptionUpdater(m_JobManager, JOB_ID).prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenIndexIsNotInteger() throws JobException, IOException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid index: integer expected; actual was: a string");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        JsonNode node = new ObjectMapper().readTree("{\"index\":\"a string\", \"description\":\"bar\"}");

        new DetectorDescriptionUpdater(m_JobManager, JOB_ID).prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenDescriptionIsNotString() throws JobException, IOException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid description: string expected; actual was: 1");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        JsonNode node = new ObjectMapper().readTree("{\"index\":0, \"description\":1}");

        new DetectorDescriptionUpdater(m_JobManager, JOB_ID).prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenIndexIsNegative() throws JobException, IOException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid index: valid range is [0, 1]; actual was: -1");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));
        givenJobHasNDetectors(2);

        JsonNode node = new ObjectMapper().readTree("{\"index\":-1, \"description\":\"bar\"}");

        new DetectorDescriptionUpdater(m_JobManager, JOB_ID).prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenIndexIsEqualToDetectorsCount() throws JobException, IOException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid index: valid range is [0, 2]; actual was: 3");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));
        givenJobHasNDetectors(3);

        JsonNode node = new ObjectMapper().readTree("{\"index\":3, \"description\":\"bar\"}");

        new DetectorDescriptionUpdater(m_JobManager, JOB_ID).prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenIndexIsGreaterThanDetectorsCount() throws JobException, IOException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid index: valid range is [0, 2]; actual was: 4");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));
        givenJobHasNDetectors(3);

        JsonNode node = new ObjectMapper().readTree("{\"index\":4, \"description\":\"bar\"}");

        new DetectorDescriptionUpdater(m_JobManager, JOB_ID).prepareUpdate(node);
    }

    @Test
    public void testCommit_GivenValidParamsButUpdateFails() throws JobException, IOException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage(
                "Update failed. Please see the logs to trace the cause of the failure.");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.UNKNOWN_ERROR));
        givenJobHasNDetectors(3);

        JsonNode node = new ObjectMapper().readTree("{\"index\":0, \"description\":\"bar\"}");

        DetectorDescriptionUpdater updater = new DetectorDescriptionUpdater(m_JobManager, JOB_ID);
        updater.prepareUpdate(node);
        updater.commit();
    }

    @Test
    public void testPrepareUpdate_GivenValidParams() throws JobException, IOException
    {
        JsonNode node = new ObjectMapper().readTree("{\"index\":1, \"description\":\"Ipanema\"}");
        givenJobHasNDetectors(3);
        givenUpdateSucceeds(1, "Ipanema");

        DetectorDescriptionUpdater updater = new DetectorDescriptionUpdater(m_JobManager, JOB_ID);
        updater.prepareUpdate(node);

        verify(m_JobManager, never()).updateDetectorDescription(JOB_ID, 1, "Ipanema");
    }

    @Test
    public void testCommit_GivenValidParams() throws JobException, IOException
    {
        JsonNode node = new ObjectMapper().readTree("{\"index\":1, \"description\":\"Ipanema\"}");
        givenJobHasNDetectors(3);
        givenUpdateSucceeds(1, "Ipanema");

        DetectorDescriptionUpdater updater = new DetectorDescriptionUpdater(m_JobManager, JOB_ID);
        updater.prepareUpdate(node);
        updater.commit();

        verify(m_JobManager).updateDetectorDescription(JOB_ID, 1, "Ipanema");
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
        when(m_JobManager.updateDetectorDescription(JOB_ID, detectorIndex, newName)).thenReturn(true);
    }
}
