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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.prelert.job.condition.Condition;
import com.prelert.job.condition.Operator;
import com.prelert.job.detectionrules.RuleCondition;
import com.prelert.job.detectionrules.RuleConditionType;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobConfigurationException;

public class RuleConditionVerifierTest
{
    @Rule public ExpectedException m_ExpectedException = ExpectedException.none();

    @Test
    public void testVerify_GivenCategoricalWithCondition() throws JobConfigurationException
    {
        RuleCondition ruleCondition = new RuleCondition();
        ruleCondition.setConditionType(RuleConditionType.CATEGORICAL);
        ruleCondition.setCondition(new Condition());

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.DETECTOR_RULE_CONDITION_INVALID_OPTION));
        m_ExpectedException.expectMessage(
                "Invalid detector rule: a categorical ruleCondition does not support condition");

        RuleConditionVerifier.verify(ruleCondition);
    }

    @Test
    public void testVerify_GivenCategoricalWithoutValueList() throws JobConfigurationException
    {
        RuleCondition ruleCondition = new RuleCondition();
        ruleCondition.setConditionType(RuleConditionType.CATEGORICAL);
        ruleCondition.setValueList(null);

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.DETECTOR_RULE_CONDITION_MISSING_FIELD));
        m_ExpectedException.expectMessage(
                "Invalid detector rule: a categorical ruleCondition requires that valueList is specified");

        RuleConditionVerifier.verify(ruleCondition);
    }

    @Test
    public void testVerify_GivenNumericalActualWithValueList() throws JobConfigurationException
    {
        RuleCondition ruleCondition = new RuleCondition();
        ruleCondition.setConditionType(RuleConditionType.NUMERICAL_ACTUAL);
        ruleCondition.setValueList("myList");

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.DETECTOR_RULE_CONDITION_INVALID_OPTION));
        m_ExpectedException.expectMessage(
                "Invalid detector rule: a numerical ruleCondition does not support valueList");

        RuleConditionVerifier.verify(ruleCondition);
    }

    @Test
    public void testVerify_GivenNumericalActualWithoutCondition() throws JobConfigurationException
    {
        RuleCondition ruleCondition = new RuleCondition();
        ruleCondition.setConditionType(RuleConditionType.NUMERICAL_ACTUAL);
        ruleCondition.setCondition(null);

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.DETECTOR_RULE_CONDITION_MISSING_FIELD));
        m_ExpectedException.expectMessage(
                "Invalid detector rule: a numerical ruleCondition requires that condition is specified");

        RuleConditionVerifier.verify(ruleCondition);
    }

    @Test
    public void testVerify_GivenNumericalTypicalWithValueList() throws JobConfigurationException
    {
        RuleCondition ruleCondition = new RuleCondition();
        ruleCondition.setConditionType(RuleConditionType.NUMERICAL_TYPICAL);
        ruleCondition.setValueList("myList");

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.DETECTOR_RULE_CONDITION_INVALID_OPTION));
        m_ExpectedException.expectMessage(
                "Invalid detector rule: a numerical ruleCondition does not support valueList");

        RuleConditionVerifier.verify(ruleCondition);
    }

    @Test
    public void testVerify_GivenNumericalTypicalWithoutCondition() throws JobConfigurationException
    {
        RuleCondition ruleCondition = new RuleCondition();
        ruleCondition.setConditionType(RuleConditionType.NUMERICAL_TYPICAL);
        ruleCondition.setCondition(null);

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.DETECTOR_RULE_CONDITION_MISSING_FIELD));
        m_ExpectedException.expectMessage(
                "Invalid detector rule: a numerical ruleCondition requires that condition is specified");

        RuleConditionVerifier.verify(ruleCondition);
    }

    @Test
    public void testVerify_GivenNumericalDiffAbsWithValueList() throws JobConfigurationException
    {
        RuleCondition ruleCondition = new RuleCondition();
        ruleCondition.setConditionType(RuleConditionType.NUMERICAL_DIFF_ABS);
        ruleCondition.setValueList("myList");

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.DETECTOR_RULE_CONDITION_INVALID_OPTION));
        m_ExpectedException.expectMessage(
                "Invalid detector rule: a numerical ruleCondition does not support valueList");

        RuleConditionVerifier.verify(ruleCondition);
    }

    @Test
    public void testVerify_GivenNumericalDiffAbsWithoutCondition() throws JobConfigurationException
    {
        RuleCondition ruleCondition = new RuleCondition();
        ruleCondition.setConditionType(RuleConditionType.NUMERICAL_DIFF_ABS);
        ruleCondition.setCondition(null);

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.DETECTOR_RULE_CONDITION_MISSING_FIELD));
        m_ExpectedException.expectMessage(
                "Invalid detector rule: a numerical ruleCondition requires that condition is specified");

        RuleConditionVerifier.verify(ruleCondition);
    }

    @Test
    public void testVerify_GivenNumericalWithInvalidCondition() throws JobConfigurationException
    {
        RuleCondition ruleCondition = new RuleCondition();
        ruleCondition.setConditionType(RuleConditionType.NUMERICAL_DIFF_ABS);
        Condition condition = new Condition();
        condition.setOperator(Operator.LT);
        condition.setValue("a string");
        ruleCondition.setCondition(condition);

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.CONDITION_INVALID_ARGUMENT));
        m_ExpectedException.expectMessage(
                "Invalid condition value: cannot parse a double from string 'a string'");

        RuleConditionVerifier.verify(ruleCondition);
    }

    @Test
    public void testVerify_GivenFieldValueWithoutFieldName() throws JobConfigurationException
    {
        RuleCondition ruleCondition = new RuleCondition();
        ruleCondition.setConditionType(RuleConditionType.CATEGORICAL);
        ruleCondition.setValueList("myList");
        ruleCondition.setFieldValue("foo");

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.DETECTOR_RULE_CONDITION_MISSING_FIELD));
        m_ExpectedException.expectMessage(
                "Invalid detector rule: missing fieldName in ruleCondition where fieldValue 'foo' is specified");

        RuleConditionVerifier.verify(ruleCondition);
    }

    @Test
    public void testVerify_GivenValidCategorical() throws JobConfigurationException
    {
        RuleCondition ruleCondition = new RuleCondition();
        ruleCondition.setConditionType(RuleConditionType.CATEGORICAL);
        ruleCondition.setValueList("myList");
        ruleCondition.setFieldName("metric");
        ruleCondition.setFieldValue("cpu");

        RuleConditionVerifier.verify(ruleCondition);
    }

    @Test
    public void testVerify_GivenValidNumericalActual() throws JobConfigurationException
    {
        RuleCondition ruleCondition = new RuleCondition();
        ruleCondition.setConditionType(RuleConditionType.NUMERICAL_ACTUAL);
        ruleCondition.setFieldName("metric");
        ruleCondition.setFieldValue("cpu");
        ruleCondition.setCondition(new Condition(Operator.LT, "5"));

        RuleConditionVerifier.verify(ruleCondition);
    }

    @Test
    public void testVerify_GivenValidNumericalTypical() throws JobConfigurationException
    {
        RuleCondition ruleCondition = new RuleCondition();
        ruleCondition.setConditionType(RuleConditionType.NUMERICAL_TYPICAL);
        ruleCondition.setFieldName("metric");
        ruleCondition.setFieldValue("cpu");
        ruleCondition.setCondition(new Condition(Operator.LT, "5"));

        RuleConditionVerifier.verify(ruleCondition);
    }

    @Test
    public void testVerify_GivenValidNumericalDiffAbs() throws JobConfigurationException
    {
        RuleCondition ruleCondition = new RuleCondition();
        ruleCondition.setConditionType(RuleConditionType.NUMERICAL_DIFF_ABS);
        ruleCondition.setFieldName("metric");
        ruleCondition.setFieldValue("cpu");
        ruleCondition.setCondition(new Condition(Operator.LT, "5"));

        RuleConditionVerifier.verify(ruleCondition);
    }
}
