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
package com.prelert.job.condition.verification;

import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.prelert.job.condition.Condition;
import com.prelert.job.condition.Operator;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobConfigurationException;

public class ConditionVerifierTest
{
    @Rule public ExpectedException m_ExpectedException = ExpectedException.none();

    @Test
    public void testVerifyArgsNumericArgs() throws JobConfigurationException
    {
        Condition c = new Condition(Operator.LTE, "100");
        assertTrue(ConditionVerifier.verify(c));
        c = new Condition(Operator.GT, "10.0");
        assertTrue(ConditionVerifier.verify(c));
    }

    @Test
    public void testVerify_GivenUnsetOperator() throws JobConfigurationException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid operator for condition");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.CONDITION_INVALID_ARGUMENT));

        ConditionVerifier.verify(new Condition());
    }

    @Test
    public void testVerify_GivenOperatorIsNone() throws JobConfigurationException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid operator for condition");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.CONDITION_INVALID_ARGUMENT));

        Condition condition = new Condition();
        condition.setOperator(Operator.NONE);

        ConditionVerifier.verify(condition);
    }

    @Test
    public void testVerify_GivenEmptyValue() throws JobConfigurationException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage(
                "Invalid condition value: cannot parse a double from string ''");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.CONDITION_INVALID_ARGUMENT));

        Condition condition = new Condition();
        condition.setOperator(Operator.LT);
        condition.setValue("");

        ConditionVerifier.verify(condition);
    }

    @Test
    public void testVerify_GivenInvalidRegex() throws JobConfigurationException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.CONDITION_INVALID_ARGUMENT));

        Condition condition = new Condition();
        condition.setOperator(Operator.MATCH);
        condition.setValue("[*");

        ConditionVerifier.verify(condition);
    }

    @Test
    public void testVerify_GivenNullRegex() throws JobConfigurationException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.CONDITION_INVALID_ARGUMENT));

        Condition condition = new Condition();
        condition.setOperator(Operator.MATCH);

        ConditionVerifier.verify(condition);
    }
}
