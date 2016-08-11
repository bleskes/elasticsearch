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
package com.prelert.job.detectionrules.verification;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.prelert.job.Detector;
import com.prelert.job.condition.Condition;
import com.prelert.job.condition.Operator;
import com.prelert.job.detectionrules.DetectionRule;
import com.prelert.job.detectionrules.RuleCondition;
import com.prelert.job.detectionrules.RuleConditionType;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobConfigurationException;

public class DetectionRuleVerifierTest
{
    @Rule
    public ExpectedException m_ExpectedException = ExpectedException.none();

    @Test
    public void testVerify_GivenDetectorFunctionIsNull() throws JobConfigurationException
    {
        DetectionRule rule = new DetectionRule();
        rule.setTargetFieldValue("foo");

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.DETECTOR_RULES_NOT_SUPPORTED_BY_FUNCTION));
        m_ExpectedException.expectMessage(
                "Invalid detector rule: function metric does not support rules");

        DetectionRuleVerifier.verify(rule, new Detector());
    }

    @Test
    public void testVerify_GivenDetectorFunctionIsMetric() throws JobConfigurationException
    {
        Detector detector = new Detector();
        detector.setFunction(Detector.METRIC);
        DetectionRule rule = new DetectionRule();
        rule.setTargetFieldValue("foo");

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.DETECTOR_RULES_NOT_SUPPORTED_BY_FUNCTION));
        m_ExpectedException.expectMessage(
                "Invalid detector rule: function metric does not support rules");

        DetectionRuleVerifier.verify(rule, detector);
    }

    @Test
    public void testVerify_GivenDetectorFunctionIsLatLong() throws JobConfigurationException
    {
        Detector detector = new Detector();
        detector.setFunction(Detector.LAT_LONG);
        DetectionRule rule = new DetectionRule();
        rule.setTargetFieldValue("foo");

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.DETECTOR_RULES_NOT_SUPPORTED_BY_FUNCTION));
        m_ExpectedException.expectMessage(
                "Invalid detector rule: function lat_long does not support rules");

        DetectionRuleVerifier.verify(rule, detector);
    }

    @Test
    public void testVerify_GivenTargetFieldValueWithoutTargetField() throws JobConfigurationException
    {
        Detector detector = new Detector();
        detector.setFunction(Detector.HIGH_DC);
        DetectionRule rule = new DetectionRule();
        rule.setTargetFieldValue("foo");

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.DETECTOR_RULE_MISSING_FIELD));
        m_ExpectedException.expectMessage(
                "Invalid detector rule: missing targetFieldName where targetFieldValue 'foo' is set");

        DetectionRuleVerifier.verify(rule, detector);
    }

    @Test
    public void testVerify_GivenNullConditions() throws JobConfigurationException
    {
        Detector detector = new Detector();
        detector.setFunction(Detector.COUNT);
        DetectionRule rule = new DetectionRule();

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.DETECTOR_RULE_REQUIRES_ONE_OR_MORE_CONDITIONS));
        m_ExpectedException.expectMessage(
                "Invalid detector rule: at least one ruleCondition is required");

        DetectionRuleVerifier.verify(rule, detector);
    }

    @Test
    public void testVerify_GivenEmptyConditions() throws JobConfigurationException
    {
        Detector detector = new Detector();
        detector.setFunction(Detector.COUNT);
        DetectionRule rule = new DetectionRule();
        rule.setRuleConditions(Collections.emptyList());

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.DETECTOR_RULE_REQUIRES_ONE_OR_MORE_CONDITIONS));
        m_ExpectedException.expectMessage(
                "Invalid detector rule: at least one ruleCondition is required");

        DetectionRuleVerifier.verify(rule, detector);
    }

    @Test
    public void testVerify_GivenInvalidCondition() throws JobConfigurationException
    {
        RuleCondition ruleCondition = new RuleCondition();
        ruleCondition.setConditionType(RuleConditionType.NUMERICAL_ACTUAL);
        ruleCondition.setValueList("myList");
        DetectionRule rule = new DetectionRule();
        rule.setRuleConditions(Arrays.asList(ruleCondition));

        m_ExpectedException.expect(JobConfigurationException.class);

        DetectionRuleVerifier.verify(rule, new Detector());
    }

    @Test
    public void testVerify_GivenInvalidDetectionRuleConditionFieldName() throws JobConfigurationException
    {
        Detector detector = new Detector();
        detector.setFunction("mean");
        detector.setFieldName("metricVale");
        detector.setByFieldName("metricName");
        RuleCondition ruleCondition = new RuleCondition();
        ruleCondition.setConditionType(RuleConditionType.NUMERICAL_ACTUAL);
        ruleCondition.setFieldName("metricValue");
        ruleCondition.setCondition(new Condition(Operator.LT, "5"));
        DetectionRule rule = new DetectionRule();
        rule.setRuleConditions(Arrays.asList(ruleCondition));
        detector.setDetectorRules(Arrays.asList(rule));

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.DETECTOR_RULE_CONDITION_INVALID_FIELD_NAME));
        m_ExpectedException.expectMessage(
                "Invalid detector rule: fieldName has to be one of [metricName]; actual was 'metricValue'");

        DetectionRuleVerifier.verify(rule, detector);
    }

    @Test
    public void testVerify_GivenInvalidDetectionRuleTargetFieldName() throws JobConfigurationException
    {
        Detector detector = new Detector();
        detector.setFunction("mean");
        detector.setFieldName("metricVale");
        detector.setByFieldName("metricName");
        detector.setPartitionFieldName("instance");
        RuleCondition ruleCondition = new RuleCondition();
        ruleCondition.setConditionType(RuleConditionType.NUMERICAL_ACTUAL);
        ruleCondition.setFieldName("metricName");
        ruleCondition.setCondition(new Condition(Operator.LT, "5"));
        DetectionRule rule = new DetectionRule();
        rule.setTargetFieldName("instancE");
        rule.setRuleConditions(Arrays.asList(ruleCondition));
        detector.setDetectorRules(Arrays.asList(rule));

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.DETECTOR_RULE_INVALID_TARGET_FIELD));
        m_ExpectedException.expectMessage(
                "Invalid detector rule: targetFieldName has to be one of [metricName, instance]; actual was 'instancE'");

        DetectionRuleVerifier.verify(rule, detector);
    }

    @Test
    public void testVerify_GivenNoConditionFieldsInRuleWithTargetFieldAndOneAnalysisField() throws JobConfigurationException
    {
        Detector detector = new Detector();
        detector.setFunction("mean");
        detector.setFieldName("metricValue");
        detector.setByFieldName("metricName");
        RuleCondition ruleCondition = new RuleCondition();
        ruleCondition.setConditionType(RuleConditionType.NUMERICAL_ACTUAL);
        ruleCondition.setCondition(new Condition(Operator.LT, "5"));
        DetectionRule rule = new DetectionRule();
        rule.setTargetFieldName("metricName");
        rule.setRuleConditions(Arrays.asList(ruleCondition));
        detector.setDetectorRules(Arrays.asList(rule));

        DetectionRuleVerifier.verify(rule, detector);
    }

    @Test
    public void testVerify_GivenConditionFieldsInRuleWithTargetFieldAndOneAnalysisField() throws JobConfigurationException
    {
        Detector detector = new Detector();
        detector.setFunction("mean");
        detector.setFieldName("metricValue");
        detector.setByFieldName("metricName");
        RuleCondition ruleCondition = new RuleCondition();
        ruleCondition.setFieldName("metricName");
        ruleCondition.setFieldValue("cpu");
        ruleCondition.setConditionType(RuleConditionType.NUMERICAL_ACTUAL);
        ruleCondition.setCondition(new Condition(Operator.LT, "5"));
        DetectionRule rule = new DetectionRule();
        rule.setTargetFieldName("metricName");
        rule.setRuleConditions(Arrays.asList(ruleCondition));
        detector.setDetectorRules(Arrays.asList(rule));

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.DETECTOR_RULE_CONDITION_INVALID_FIELD_NAME));
        m_ExpectedException.expectMessage(
                "Invalid detector rule: fieldName has to be one of [null]; actual was 'metricName'");

        DetectionRuleVerifier.verify(rule, detector);
    }

    @Test
    public void testVerify_GivenNoConditionFieldsInRuleWithTargetFieldAndTwoAnalysisFields() throws JobConfigurationException
    {
        Detector detector = new Detector();
        detector.setFunction("mean");
        detector.setFieldName("metricValue");
        detector.setByFieldName("metricName");
        detector.setPartitionFieldName("instance");
        RuleCondition ruleCondition = new RuleCondition();
        ruleCondition.setConditionType(RuleConditionType.NUMERICAL_ACTUAL);
        ruleCondition.setCondition(new Condition(Operator.LT, "5"));
        DetectionRule rule = new DetectionRule();
        rule.setTargetFieldName("instance");
        rule.setRuleConditions(Arrays.asList(ruleCondition));
        detector.setDetectorRules(Arrays.asList(rule));

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.DETECTOR_RULE_CONDITION_INVALID_FIELD_NAME));
        m_ExpectedException.expectMessage(
                "Invalid detector rule: fieldName has to be one of [metricName]; actual was 'null'");

        DetectionRuleVerifier.verify(rule, detector);
    }

    @Test
    public void testVerify_GivenFieldNameIsPartitionFieldInDetectorWithPartitionAndBy()
            throws JobConfigurationException
    {
        Detector detector = new Detector();
        detector.setFunction("mean");
        detector.setFieldName("metricValue");
        detector.setByFieldName("metricName");
        detector.setPartitionFieldName("instance");
        RuleCondition ruleCondition = new RuleCondition();
        ruleCondition.setFieldName("instance");
        ruleCondition.setFieldValue("instance_1");
        ruleCondition.setConditionType(RuleConditionType.NUMERICAL_ACTUAL);
        ruleCondition.setCondition(new Condition(Operator.LT, "5"));
        DetectionRule rule = new DetectionRule();
        rule.setRuleConditions(Arrays.asList(ruleCondition));
        detector.setDetectorRules(Arrays.asList(rule));

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.DETECTOR_RULE_CONDITION_INVALID_FIELD_NAME));
        m_ExpectedException.expectMessage(
                "Invalid detector rule: fieldName has to be one of [metricName]; actual was 'instance'");

        DetectionRuleVerifier.verify(rule, detector);
    }

    @Test
    public void testVerify_GivenFieldNameIsOverFieldInDetectorWithOverAndBy()
            throws JobConfigurationException
    {
        Detector detector = new Detector();
        detector.setFunction("mean");
        detector.setFieldName("metricValue");
        detector.setByFieldName("metricName");
        detector.setOverFieldName("instance");
        RuleCondition ruleCondition = new RuleCondition();
        ruleCondition.setFieldName("instance");
        ruleCondition.setFieldValue("instance_1");
        ruleCondition.setConditionType(RuleConditionType.NUMERICAL_ACTUAL);
        ruleCondition.setCondition(new Condition(Operator.LT, "5"));
        DetectionRule rule = new DetectionRule();
        rule.setRuleConditions(Arrays.asList(ruleCondition));
        detector.setDetectorRules(Arrays.asList(rule));

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.DETECTOR_RULE_CONDITION_INVALID_FIELD_NAME));
        m_ExpectedException.expectMessage(
                "Invalid detector rule: fieldName has to be one of [metricName]; actual was 'instance'");

        DetectionRuleVerifier.verify(rule, detector);
    }

    @Test
    public void testVerify_GivenRuleWithTargetFieldAndCategoricalCondition()
            throws JobConfigurationException
    {
        Detector detector = new Detector();
        detector.setFunction("mean");
        detector.setFieldName("metricValue");
        detector.setByFieldName("metricName");
        detector.setPartitionFieldName("instance");
        RuleCondition ruleCondition = new RuleCondition();
        ruleCondition.setFieldName("metricName");
        ruleCondition.setConditionType(RuleConditionType.CATEGORICAL);
        ruleCondition.setValueList("myList");
        DetectionRule rule = new DetectionRule();
        rule.setTargetFieldName("instance");
        rule.setRuleConditions(Arrays.asList(ruleCondition));
        detector.setDetectorRules(Arrays.asList(rule));

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.DETECTOR_RULE_CONDITION_INVALID_OPTION));
        m_ExpectedException.expectMessage(
                "Invalid detector rule: a categorical ruleCondition does not support targetField");

        assertTrue(DetectionRuleVerifier.verify(rule, detector));
    }

    @Test
    public void testVerify_GivenValidRuleWithNoTargetFieldAndCategoricalCondition()
            throws JobConfigurationException
    {
        Detector detector = new Detector();
        detector.setFunction(Detector.COUNT);
        RuleCondition ruleCondition = new RuleCondition();
        ruleCondition.setConditionType(RuleConditionType.CATEGORICAL);
        ruleCondition.setValueList("myList");
        DetectionRule rule = new DetectionRule();
        rule.setRuleConditions(Arrays.asList(ruleCondition));

        assertTrue(DetectionRuleVerifier.verify(rule, detector));
    }

    @Test
    public void testVerify_GivenValidRuleWithTargetFieldAndTwoAnalysisFieldsAndNumericalCondition()
            throws JobConfigurationException
    {
        Detector detector = new Detector();
        detector.setFunction("mean");
        detector.setFieldName("metricValue");
        detector.setByFieldName("metricName");
        detector.setPartitionFieldName("instance");
        RuleCondition ruleCondition = new RuleCondition();
        ruleCondition.setFieldName("metricName");
        ruleCondition.setFieldValue("CPU");
        ruleCondition.setConditionType(RuleConditionType.NUMERICAL_ACTUAL);
        ruleCondition.setCondition(new Condition(Operator.LT, "5"));
        DetectionRule rule = new DetectionRule();
        rule.setTargetFieldName("instance");
        rule.setRuleConditions(Arrays.asList(ruleCondition));
        detector.setDetectorRules(Arrays.asList(rule));

        assertTrue(DetectionRuleVerifier.verify(rule, detector));
    }

    @Test
    public void testVerify_GivenValidRuleWhereFieldNameFieldIsOverAndDetectorWithOver()
            throws JobConfigurationException
    {
        Detector detector = new Detector();
        detector.setFunction("mean");
        detector.setFieldName("metricValue");
        detector.setOverFieldName("metricName");
        RuleCondition ruleCondition = new RuleCondition();
        ruleCondition.setFieldName("metricName");
        ruleCondition.setFieldValue("CPU");
        ruleCondition.setConditionType(RuleConditionType.NUMERICAL_ACTUAL);
        ruleCondition.setCondition(new Condition(Operator.LT, "5"));
        DetectionRule rule = new DetectionRule();
        rule.setRuleConditions(Arrays.asList(ruleCondition));
        detector.setDetectorRules(Arrays.asList(rule));

        assertTrue(DetectionRuleVerifier.verify(rule, detector));
    }
}
