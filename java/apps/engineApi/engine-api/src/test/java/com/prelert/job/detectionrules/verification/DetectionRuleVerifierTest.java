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

import java.util.Arrays;
import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.prelert.job.Detector;
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
    public void testVerify_GivenTargetValueWithoutTargetField() throws JobConfigurationException
    {
        DetectionRule rule = new DetectionRule();
        rule.setTargetValue("foo");

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.DETECTOR_RULE_MISSING_FIELD));
        m_ExpectedException.expectMessage(
                "Invalid detector rule: missing targetField where targetValue 'foo' is specified");

        DetectionRuleVerifier.verify(rule, new Detector());
    }

    @Test
    public void testVerify_GivenNullConditions() throws JobConfigurationException
    {
        DetectionRule rule = new DetectionRule();

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.DETECTOR_RULE_REQUIRES_ONE_OR_MORE_CONDITIONS));
        m_ExpectedException.expectMessage(
                "Invalid detector rule: at least one ruleCondition is required");

        DetectionRuleVerifier.verify(rule, new Detector());
    }

    @Test
    public void testVerify_GivenEmptyConditions() throws JobConfigurationException
    {
        DetectionRule rule = new DetectionRule();
        rule.setRuleConditions(Collections.emptyList());

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.DETECTOR_RULE_REQUIRES_ONE_OR_MORE_CONDITIONS));
        m_ExpectedException.expectMessage(
                "Invalid detector rule: at least one ruleCondition is required");

        DetectionRuleVerifier.verify(rule, new Detector());
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
    public void testVerify_GivenValidRule() throws JobConfigurationException
    {
        RuleCondition ruleCondition = new RuleCondition();
        ruleCondition.setConditionType(RuleConditionType.CATEGORICAL);
        ruleCondition.setValueList("myList");
        DetectionRule rule = new DetectionRule();
        rule.setRuleConditions(Arrays.asList(ruleCondition));

        DetectionRuleVerifier.verify(rule, new Detector());
    }
}
