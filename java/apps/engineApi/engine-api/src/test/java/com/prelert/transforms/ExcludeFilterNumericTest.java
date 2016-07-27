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

import static com.prelert.transforms.TransformTestUtils.createIndexArray;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.prelert.job.condition.Condition;
import com.prelert.job.condition.Operator;
import com.prelert.transforms.Transform.TransformIndex;
import com.prelert.transforms.Transform.TransformResult;

public class ExcludeFilterNumericTest
{
    @Test
    public void testEq()
    throws TransformException
    {
        ExcludeFilterNumeric transform = createTransform(Operator.EQ, "5.0");

        String [] input = {"5"};
        String [] scratch = {};
        String [] output = {};
        String [][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.EXCLUDE, transform.transform(readWriteArea));

        input[0] = "5.10000";
        assertEquals(TransformResult.OK, transform.transform(readWriteArea));
    }

    @Test
    public void testGT()
    throws TransformException
    {
        ExcludeFilterNumeric transform = createTransform(Operator.GT, "10.000");

        String [] input = {"100"};
        String [] scratch = {};
        String [] output = {};
        String [][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.EXCLUDE, transform.transform(readWriteArea));

        input[0] = "1.0";
        assertEquals(TransformResult.OK, transform.transform(readWriteArea));
    }

    @Test
    public void testGTE()
    throws TransformException
    {
        ExcludeFilterNumeric transform = createTransform(Operator.GTE, "10.000");

        String [] input = {"100"};
        String [] scratch = {};
        String [] output = {};
        String [][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.EXCLUDE, transform.transform(readWriteArea));

        input[0] = "10";
        assertEquals(TransformResult.EXCLUDE, transform.transform(readWriteArea));

        input[0] = "9.5";
        assertEquals(TransformResult.OK, transform.transform(readWriteArea));
    }

    @Test
    public void testLT()
    throws TransformException
    {
        ExcludeFilterNumeric transform = createTransform(Operator.LT, "2000");

        String [] input = {"100.2"};
        String [] scratch = {};
        String [] output = {};
        String [][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.EXCLUDE, transform.transform(readWriteArea));

        input[0] = "2005.0000";
        assertEquals(TransformResult.OK, transform.transform(readWriteArea));
    }

    @Test
    public void testLTE()
    throws TransformException
    {
        ExcludeFilterNumeric transform = createTransform(Operator.LTE, "2000");

        String [] input = {"100.2"};
        String [] scratch = {};
        String [] output = {};
        String [][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.EXCLUDE, transform.transform(readWriteArea));

        input[0] = "2000.0000";
        assertEquals(TransformResult.EXCLUDE, transform.transform(readWriteArea));

        input[0] = "9000.5";
        assertEquals(TransformResult.OK, transform.transform(readWriteArea));
    }

    private ExcludeFilterNumeric createTransform(Operator op, String filterValue)
    {
        Condition condition = new Condition(op, filterValue);
        List<TransformIndex> readIndicies = createIndexArray(new TransformIndex(0, 0));
        List<TransformIndex> writeIndicies = createIndexArray();

        return new ExcludeFilterNumeric(condition, readIndicies, writeIndicies, mock(Logger.class));
    }
}
