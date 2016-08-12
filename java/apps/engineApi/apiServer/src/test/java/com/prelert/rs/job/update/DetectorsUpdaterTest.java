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

import static org.junit.Assert.assertEquals;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prelert.job.AnalysisConfig;
import com.prelert.job.Detector;
import com.prelert.job.JobDetails;
import com.prelert.job.JobException;
import com.prelert.job.UnknownJobException;
import com.prelert.job.condition.Condition;
import com.prelert.job.condition.Operator;
import com.prelert.job.detectionrules.DetectionRule;
import com.prelert.job.detectionrules.RuleCondition;
import com.prelert.job.detectionrules.RuleConditionType;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobConfigurationException;
import com.prelert.job.manager.JobManager;
import com.prelert.rs.provider.JobConfigurationParseException;

public class DetectorsUpdaterTest
{
    private static final String JOB_ID = "foo";

    @Rule public ExpectedException m_ExpectedException = ExpectedException.none();

    @Mock private JobManager m_JobManager;
    private StringWriter m_ConfigWriter;
    private JobDetails m_Job;

    @Before
    public void setUp() throws UnknownJobException
    {
        MockitoAnnotations.initMocks(this);
        m_Job = new JobDetails();
        m_Job.setId(JOB_ID);
        when(m_JobManager.getJobOrThrowIfUnknown(JOB_ID)).thenReturn(m_Job);
        m_ConfigWriter = new StringWriter();
    }

    @Test
    public void testPrepareUpdate_GivenParamIsNotArray() throws JobException, IOException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid update value for detectors: value must be an array");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        JsonNode node = new ObjectMapper().readTree("{\"index\":0,\"description\":\"haha\"}");

        createUpdater().prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenParamIsNotJsonObject() throws JobException, IOException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid update value for detectors: requires [index] and at least one of [description, detectorRules]");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        JsonNode node = new ObjectMapper().readTree("[1,2,3]");

        createUpdater().prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenMissingDescriptionParam() throws JobException, IOException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid update value for detectors: requires [index] and at least one of [description, detectorRules]");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        JsonNode node = new ObjectMapper().readTree("[{\"index\":1}]");

        createUpdater().prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenIndexOnly() throws JobException, IOException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid update value for detectors: requires [index] and at least one of [description, detectorRules]");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        JsonNode node = new ObjectMapper().readTree("[{\"index\":0}]");

        createUpdater().prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenMissingIndexParam() throws JobException, IOException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid update value for detectors: requires [index] and at least one of [description, detectorRules]");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        JsonNode node = new ObjectMapper().readTree("[{\"description\":\"bar\",\"detectorRules\":[]}]");

        createUpdater().prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenUnknownParam() throws JobException, IOException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid update value for detectors: requires [index] and at least one of [description, detectorRules]");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        JsonNode node = new ObjectMapper().readTree("[{\"index\":\"1\",\"unknown\":[]}]");

        createUpdater().prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenEmptyObject() throws JobException, IOException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid update value for detectors: requires [index] and at least one of [description, detectorRules]");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        JsonNode node = new ObjectMapper().readTree("[{}]");

        createUpdater().prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenIndexIsNotInteger() throws JobException, IOException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid index: integer expected; actual was: a string");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        JsonNode node = new ObjectMapper().readTree("[{\"index\":\"a string\", \"description\":\"bar\"}]");

        createUpdater().prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenDescriptionIsNotString() throws JobException, IOException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid description: string expected; actual was: 1");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        JsonNode node = new ObjectMapper().readTree("[{\"index\":0, \"description\":1}]");

        createUpdater().prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenIndexIsNegative() throws JobException, IOException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid index: valid range is [0, 1]; actual was: -1");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));
        givenJobHasNDetectors(2);

        JsonNode node = new ObjectMapper().readTree("[{\"index\":-1, \"description\":\"bar\"}]");

        createUpdater().prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenIndexIsEqualToDetectorsCount() throws JobException, IOException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid index: valid range is [0, 2]; actual was: 3");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));
        givenJobHasNDetectors(3);

        JsonNode node = new ObjectMapper().readTree("[{\"index\":3, \"description\":\"bar\"}]");

        createUpdater().prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenIndexIsGreaterThanDetectorsCount() throws JobException, IOException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid index: valid range is [0, 2]; actual was: 4");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));
        givenJobHasNDetectors(3);

        JsonNode node = new ObjectMapper().readTree("[{\"index\":4, \"description\":\"bar\"}]");

        createUpdater().prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenMultipleParamsSecondInvalid() throws JobException, IOException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid index: valid range is [0, 2]; actual was: 4");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        JsonNode node = new ObjectMapper().readTree(
                "[{\"index\":1, \"description\":\"Ipanema\"}, {\"index\":4, \"description\":\"A Train\"}]");
        givenJobHasNDetectors(3);

        createUpdater().prepareUpdate(node);
    }

    @Test
    public void testPrepareUpdate_GivenValidDescription() throws JobException, IOException
    {
        JsonNode node = new ObjectMapper().readTree("[{\"index\":1, \"description\":\"Ipanema\"}]");
        givenJobHasNDetectors(3);
        givenDescriptionUpdateSucceeds(1, "Ipanema");

        DetectorsUpdater updater = createUpdater();
        updater.prepareUpdate(node);

        verify(m_JobManager, never()).updateDetectorDescription(JOB_ID, 1, "Ipanema");
    }

    @Test
    public void testPrepareUpdate_GivenRulesCannotBeParsed() throws JobException, IOException
    {
        m_ExpectedException.expect(JobConfigurationParseException.class);
        m_ExpectedException.expectMessage("JSON parse error reading the update value for detectorRules");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        JsonNode node = new ObjectMapper().readTree(
                "[{\"index\":1, \"detectorRules\":[{\"actionRule\":\"invalid\"}]}]");
        givenJobHasNDetectors(3);

        createUpdater().prepareUpdate(node);
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

        JsonNode node = new ObjectMapper().readTree("[{\"index\":0, \"description\":\"bar\"}]");

        DetectorsUpdater updater = createUpdater();
        updater.prepareUpdate(node);
        updater.commit();
    }

    @Test
    public void testCommit_GivenValidDescriptionUpdate() throws JobException, IOException
    {
        JsonNode node = new ObjectMapper().readTree("[{\"index\":1, \"description\":\"Ipanema\"}]");
        givenJobHasNDetectors(3);
        givenDescriptionUpdateSucceeds(1, "Ipanema");

        DetectorsUpdater updater = createUpdater();
        updater.prepareUpdate(node);
        updater.commit();

        verify(m_JobManager).updateDetectorDescription(JOB_ID, 1, "Ipanema");
    }

    @Test
    public void testCommit_GivenEmptyDescription() throws JobException, IOException
    {
        JsonNode node = new ObjectMapper().readTree("[{\"index\":1, \"description\":\"\"}]");
        givenJobHasNDetectors(3);
        m_Job.getAnalysisConfig().getDetectors().get(1).setFunction("mean");
        m_Job.getAnalysisConfig().getDetectors().get(1).setFieldName("responsetime");
        m_Job.getAnalysisConfig().getDetectors().get(1).setByFieldName("airline");
        givenDescriptionUpdateSucceeds(1, "mean(responsetime) by airline");

        DetectorsUpdater updater = createUpdater();
        updater.prepareUpdate(node);
        updater.commit();

        verify(m_JobManager).updateDetectorDescription(JOB_ID, 1, "mean(responsetime) by airline");
    }

    @Test
    public void testCommit_GivenMultipleValidParams() throws JobException, IOException
    {
        JsonNode node = new ObjectMapper().readTree(
                "[{\"index\":1, \"description\":\"Ipanema\"}, {\"index\":0, \"description\":\"A Train\"}]");
        givenJobHasNDetectors(3);
        givenDescriptionUpdateSucceeds(1, "Ipanema");
        givenDescriptionUpdateSucceeds(0, "A Train");

        DetectorsUpdater updater = createUpdater();
        updater.prepareUpdate(node);
        updater.commit();

        verify(m_JobManager).updateDetectorDescription(JOB_ID, 1, "Ipanema");
        verify(m_JobManager).updateDetectorDescription(JOB_ID, 0, "A Train");
    }

    @Test
    public void testCommit_GivenValidRules() throws JobException, IOException
    {
        JsonNode node = new ObjectMapper().readTree(
                "[{\"index\":0, \"detectorRules\":[{\"ruleConditions\":["
                + "{\"conditionType\":\"numerical_actual\","
                + "\"condition\":{\"operator\":\"LT\",\"value\":\"3\"}}]}]}]");
        givenJobHasNDetectors(1);
        m_Job.getAnalysisConfig().getDetectors().get(0).setFunction("count");

        List<DetectionRule> rules = new ArrayList<>();
        DetectionRule rule = new DetectionRule();
        RuleCondition condition = new RuleCondition();
        condition.setConditionType(RuleConditionType.NUMERICAL_ACTUAL);
        condition.setCondition(new Condition(Operator.LT, "3"));
        rule.setRuleConditions(Arrays.asList(condition));
        rules.add(rule);
        givenRulesUpdateSucceeds(0, rules);

        DetectorsUpdater updater = createUpdater();
        updater.prepareUpdate(node);
        updater.commit();

        verify(m_JobManager).updateDetectorRules(JOB_ID, 0, rules);
        String expectedRulesJson = new ObjectMapper().writeValueAsString(rules);
        String expectedConfig = "[detectorRules]\ndetectorIndex = 0\nrulesJson = "
                + expectedRulesJson + "\n";
        assertEquals(expectedConfig, m_ConfigWriter.toString());
    }

    @Test
    public void testCommit_GivenValidDescriptionAndRules() throws JobException, IOException
    {
        JsonNode node = new ObjectMapper().readTree(
                "[{\"index\":0, \"description\":\"Ipanema\","
                + "\"detectorRules\":[{\"ruleConditions\":["
                + "{\"conditionType\":\"numerical_actual\","
                + "\"condition\":{\"operator\":\"LT\",\"value\":\"3\"}}]}]}]");
        givenJobHasNDetectors(1);
        m_Job.getAnalysisConfig().getDetectors().get(0).setFunction("count");

        List<DetectionRule> rules = new ArrayList<>();
        DetectionRule rule = new DetectionRule();
        RuleCondition condition = new RuleCondition();
        condition.setConditionType(RuleConditionType.NUMERICAL_ACTUAL);
        condition.setCondition(new Condition(Operator.LT, "3"));
        rule.setRuleConditions(Arrays.asList(condition));
        rules.add(rule);

        givenDescriptionUpdateSucceeds(0, "Ipanema");
        givenRulesUpdateSucceeds(0, rules);

        DetectorsUpdater updater = createUpdater();
        updater.prepareUpdate(node);
        updater.commit();

        verify(m_JobManager).updateDetectorDescription(JOB_ID, 0, "Ipanema");
        verify(m_JobManager).updateDetectorRules(JOB_ID, 0, rules);
        String expectedRulesJson = new ObjectMapper().writeValueAsString(rules);
        String expectedConfig = "[detectorRules]\ndetectorIndex = 0\nrulesJson = "
                + expectedRulesJson + "\n";
        assertEquals(expectedConfig, m_ConfigWriter.toString());
    }

    private DetectorsUpdater createUpdater()
    {
        return new DetectorsUpdater(m_JobManager, m_Job, "detectors", m_ConfigWriter);
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

    private void givenDescriptionUpdateSucceeds(int detectorIndex, String newName)
            throws UnknownJobException, JobException
    {
        when(m_JobManager.updateDetectorDescription(JOB_ID, detectorIndex, newName)).thenReturn(true);
    }

    private void givenRulesUpdateSucceeds(int detectorIndex, List<DetectionRule> rules)
            throws UnknownJobException, JobException
    {
        when(m_JobManager.updateDetectorRules(JOB_ID, detectorIndex, rules)).thenReturn(true);
    }
}
