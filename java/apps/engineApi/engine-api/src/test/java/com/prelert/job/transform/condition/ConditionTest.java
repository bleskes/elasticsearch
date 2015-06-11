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
package com.prelert.job.transform.condition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.transform.exceptions.TransformConfigurationException;
import com.prelert.rs.data.ErrorCodeMatcher;

public class ConditionTest
{
    @Rule public ExpectedException m_ExpectedException = ExpectedException.none();

    @Test
    public void testVerifyArgsNumericArgs()
    throws TransformConfigurationException
    {
        Condition c = new Condition(Operator.LTE, "100");
        assertTrue(c.verify());
        c = new Condition(Operator.GT, "10.0");
        assertTrue(c.verify());
    }

    @Test
    public void testVerify_GivenUnsetOperator() throws TransformConfigurationException
    {
        m_ExpectedException.expect(TransformConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid operator for condition");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.CONDITION_INVALID_ARGUMENT));

        new Condition().verify();
    }

    @Test
    public void testVerify_GivenOperatorIsNone() throws TransformConfigurationException
    {
        m_ExpectedException.expect(TransformConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid operator for condition");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.CONDITION_INVALID_ARGUMENT));

        Condition condition = new Condition();
        condition.setOperator(Operator.NONE);
        condition.verify();
    }

    @Test
    public void testVerify_GivenEmptyValue() throws TransformConfigurationException
    {
        m_ExpectedException.expect(TransformConfigurationException.class);
        m_ExpectedException.expectMessage(
                "Invalid condition value: cannot parse a double from string ''");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.CONDITION_INVALID_ARGUMENT));

        Condition condition = new Condition();
        condition.setOperator(Operator.LT);
        condition.setValue("");
        condition.verify();
    }

    @Test
    public void testSetValues()
    {
        // When the args can't be parsed the
        // default is the < operator and 0.
        Condition cond = new Condition();
        assertEquals(Operator.NONE, cond.getOperator());
        assertEquals("", cond.getValue());

        cond = new Condition(Operator.EQ, "astring");
        assertEquals(Operator.EQ, cond.getOperator());
        assertEquals("astring", cond.getValue());
    }

    @Test
    public void testHashCodeAndEquals()
    {
        Condition cond1 = new Condition(Operator.MATCH, "regex");
        Condition cond2 = new Condition(Operator.MATCH, "regex");

        assertEquals(cond1, cond2);
        assertEquals(cond1.hashCode(), cond2.hashCode());

        cond2.setOperator(Operator.EQ);
        assertFalse(cond1.equals(cond2));
        assertFalse(cond1.hashCode() == cond2.hashCode());
    }
}
