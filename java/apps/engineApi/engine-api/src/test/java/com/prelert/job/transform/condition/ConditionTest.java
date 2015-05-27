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

import java.util.Arrays;

import org.junit.Test;

import com.prelert.job.transform.exceptions.TransformConfigurationException;
import com.prelert.rs.data.ErrorCode;

public class ConditionTest
{
    @Test
    public void testVerifyArgsReversedOrder()
    throws TransformConfigurationException
    {
        assertTrue(Condition.verifyArguments(Arrays.asList("100", "lte")));
        assertTrue(Condition.verifyArguments(Arrays.asList("gt", "10.0")));
    }

    @Test
    public void testVerifyArgs()
    {
        tryVerifyArgs(ErrorCode.TRANSFORM_INVALID_ARGUMENT_COUNT, "");
        tryVerifyArgs(ErrorCode.TRANSFORM_INVALID_ARGUMENT, "bad-op", "bad-num");
        tryVerifyArgs(ErrorCode.TRANSFORM_INVALID_ARGUMENT, "gt", "bad-num");
        tryVerifyArgs(ErrorCode.TRANSFORM_INVALID_ARGUMENT, "bad-num", "gt");
        tryVerifyArgs(ErrorCode.TRANSFORM_INVALID_ARGUMENT, "1.0", "bad-op");
    }

    @Test
    public void testParseArgsReversedOrder()
    {
        Condition cond = new Condition(Arrays.asList("100", "lte"));
        assertEquals(Operation.LTE, cond.getOp());
        assertEquals(100.0, cond.getFilterValue(), 0.00001);
    }


    @Test
    public void testParseBadArgs()
    {
        // When the args can't be parsed the
        // default is the < operator and 0.
        Condition cond = new Condition(Arrays.asList("lte"));
        assertEquals(Operation.LT, cond.getOp());
        assertEquals(0.0, cond.getFilterValue(), 0.00001);

        cond = new Condition(Arrays.asList("bad-op", "1.0"));
        assertEquals(Operation.LT, cond.getOp());
        assertEquals(0.0, cond.getFilterValue(), 0.00001);

        cond = new Condition(Arrays.asList("bad-op", "bad-number"));
        assertEquals(Operation.LT, cond.getOp());
        assertEquals(0.0, cond.getFilterValue(), 0.00001);

        cond = new Condition(Arrays.asList("1.0", "bad-op"));
        assertEquals(Operation.LT, cond.getOp());
        assertEquals(1.0, cond.getFilterValue(), 0.00001);

        cond = new Condition(Arrays.asList("gte", "NAN"));
        assertEquals(Operation.GTE, cond.getOp());
        assertEquals(0.0, cond.getFilterValue(), 0.00001);
    }

    private void tryVerifyArgs(ErrorCode expected, String ... args)
    {
        try
        {
            Condition.verifyArguments(Arrays.asList(args));
            fail(); // should throw
        }
        catch (TransformConfigurationException e)
        {
            assertEquals(expected, e.getErrorCode());
        }
    }
}
