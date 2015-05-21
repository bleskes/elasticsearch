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

package com.prelert.transforms;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.prelert.job.transform.TransformConfigurationException;
import com.prelert.rs.data.ErrorCode;
import com.prelert.transforms.Transform.TransformIndex;
import com.prelert.transforms.Transform.TransformResult;

public class ExcludeFilterNumericTest
{
    @Test
    public void testEq()
    throws TransformException
    {
        ExcludeFilterNumeric transform = createTransform("EQ", "5");

        String [] input = {"5"};
        String [] scratch = {};
        String [] output = {};
        String [][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.FATAL_FAIL, transform.transform(readWriteArea));

        input[0] = "5.10000";
        assertEquals(TransformResult.OK, transform.transform(readWriteArea));
    }

    @Test
    public void testGT()
    throws TransformException
    {
        ExcludeFilterNumeric transform = createTransform("gt", "10.000");

        String [] input = {"100"};
        String [] scratch = {};
        String [] output = {};
        String [][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.FATAL_FAIL, transform.transform(readWriteArea));

        input[0] = "1.0";
        assertEquals(TransformResult.OK, transform.transform(readWriteArea));
    }

    @Test
    public void testGTE()
    throws TransformException
    {
        ExcludeFilterNumeric transform = createTransform("gte", "10.000");

        String [] input = {"100"};
        String [] scratch = {};
        String [] output = {};
        String [][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.FATAL_FAIL, transform.transform(readWriteArea));

        input[0] = "10";
        assertEquals(TransformResult.FATAL_FAIL, transform.transform(readWriteArea));

        input[0] = "9.5";
        assertEquals(TransformResult.OK, transform.transform(readWriteArea));
    }

    @Test
    public void testLT()
    throws TransformException
    {
        ExcludeFilterNumeric transform = createTransform("LT", "2000");

        String [] input = {"100.2"};
        String [] scratch = {};
        String [] output = {};
        String [][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.FATAL_FAIL, transform.transform(readWriteArea));

        input[0] = "2005.0000";
        assertEquals(TransformResult.OK, transform.transform(readWriteArea));
    }

    @Test
    public void testLTE()
    throws TransformException
    {
        ExcludeFilterNumeric transform = createTransform("lte", "2000");

        String [] input = {"100.2"};
        String [] scratch = {};
        String [] output = {};
        String [][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.FATAL_FAIL, transform.transform(readWriteArea));

        input[0] = "2000.0000";
        assertEquals(TransformResult.FATAL_FAIL, transform.transform(readWriteArea));

        input[0] = "9000.5";
        assertEquals(TransformResult.OK, transform.transform(readWriteArea));
    }

    @Test
    public void testParseArgsReversedOrder()
    {
        ExcludeFilterNumeric transform = createTransform("100", "lte");
        assertEquals(Operation.LTE, transform.getOp());
        assertEquals(100.0, transform.getFilterValue(), 0.00001);
    }

    @Test
    public void testParseBadArgs()
    {
        // When the args can't be parsed the
        // default is the < operator and 0.
        ExcludeFilterNumeric transform = createTransform("lte");
        assertEquals(Operation.LT, transform.getOp());
        assertEquals(0.0, transform.getFilterValue(), 0.00001);

        transform = createTransform("bad-op", "1.0");
        assertEquals(Operation.LT, transform.getOp());
        assertEquals(0.0, transform.getFilterValue(), 0.00001);

        transform = createTransform("bad-op", "bad-number");
        assertEquals(Operation.LT, transform.getOp());
        assertEquals(0.0, transform.getFilterValue(), 0.00001);

        transform = createTransform("1.0", "bad-op");
        assertEquals(Operation.LT, transform.getOp());
        assertEquals(1.0, transform.getFilterValue(), 0.00001);

        transform = createTransform("gte", "NAN");
        assertEquals(Operation.GTE, transform.getOp());
        assertEquals(0.0, transform.getFilterValue(), 0.00001);
    }

    @Test
    public void testVerifyArgsReversedOrder()
    throws TransformConfigurationException
    {
        assertTrue(ExcludeFilterNumeric.verifyArguments(Arrays.asList("100", "lte")));
        assertTrue(ExcludeFilterNumeric.verifyArguments(Arrays.asList("gt", "10.0")));
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


    private void tryVerifyArgs(ErrorCode expected, String ... args)
    {
        try
        {
            ExcludeFilterNumeric.verifyArguments(Arrays.asList(args));
            fail(); // should throw
        }
        catch (TransformConfigurationException e)
        {
            assertEquals(expected, e.getErrorCode());
        }
    }

    private ExcludeFilterNumeric createTransform(String... args)
    {
        List<String> argsList = Arrays.asList(args);
        List<TransformIndex> readIndicies = createIndexArray(new TransformIndex(0, 0));
        List<TransformIndex> writeIndicies = createIndexArray();

        return new ExcludeFilterNumeric(argsList, readIndicies, writeIndicies, mock(Logger.class));
    }

    private List<TransformIndex> createIndexArray(TransformIndex...indexs)
    {
        List<TransformIndex> result = new ArrayList<Transform.TransformIndex>();
        for (TransformIndex i : indexs)
        {
            result.add(i);
        }

        return result;
    }

}
