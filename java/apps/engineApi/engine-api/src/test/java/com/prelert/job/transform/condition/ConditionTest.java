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

import static org.junit.Assert.*;

import org.junit.Test;

import com.prelert.job.transform.exceptions.TransformConfigurationException;
import com.prelert.rs.data.ErrorCode;

public class ConditionTest
{
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
    public void testVerifyArgsThrows()
    {
        Condition c = new Condition();
        tryVerifyArgsCheckErrorCode(c, ErrorCode.CONDITION_INVALID_ARGUMENT);
        c.setOperator(Operator.NONE);
        tryVerifyArgsCheckErrorCode(c, ErrorCode.CONDITION_INVALID_ARGUMENT);

        c.setOperator(Operator.LT);
        tryVerifyArgsCheckErrorCode(c, ErrorCode.CONDITION_INVALID_ARGUMENT);
        c.setValue("");
        tryVerifyArgsCheckErrorCode(c, ErrorCode.CONDITION_INVALID_ARGUMENT);
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

    private void tryVerifyArgsCheckErrorCode(Condition condition, ErrorCode expected)
    {
        try
        {
            condition.verify();
            fail(); // should throw
        }
        catch (TransformConfigurationException e)
        {
            assertEquals(expected, e.getErrorCode());
        }
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
