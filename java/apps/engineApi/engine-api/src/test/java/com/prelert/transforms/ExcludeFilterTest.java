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

public class ExcludeFilterTest
{
    @Test
    public void testTransform_matches() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray(new TransformIndex(0, 0));
        List<TransformIndex> writeIndicies = createIndexArray();

        Condition cond = new Condition(Operator.MATCH, "cat");

        ExcludeFilterRegex transform = new ExcludeFilterRegex(cond, readIndicies, writeIndicies, mock(Logger.class));

        String [] input = {"cat"};
        String [] scratch = {};
        String [] output = {};
        String [][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.EXCLUDE, transform.transform(readWriteArea));
    }

    @Test
    public void testTransform_noMatches() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray(new TransformIndex(0, 0));
        List<TransformIndex> writeIndicies = createIndexArray();

        Condition cond = new Condition(Operator.MATCH, "boat");

        ExcludeFilterRegex transform = new ExcludeFilterRegex(cond, readIndicies, writeIndicies, mock(Logger.class));

        String [] input = {"cat"};
        String [] scratch = {};
        String [] output = {};
        String [][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.OK, transform.transform(readWriteArea));
    }

    @Test
    public void testTransform_matchesRegex() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray(new TransformIndex(0, 0));
        List<TransformIndex> writeIndicies = createIndexArray();

        Condition cond = new Condition(Operator.MATCH, "metric[0-9]+");

        ExcludeFilterRegex transform = new ExcludeFilterRegex(cond, readIndicies, writeIndicies, mock(Logger.class));
        String [] input = {"metric01"};
        String [] scratch = {};
        String [] output = new String [3];
        String [][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.EXCLUDE, transform.transform(readWriteArea));

        readWriteArea[0] = new String [] {"metric02-A"};
        assertEquals(TransformResult.OK, transform.transform(readWriteArea));
    }


    @Test
    public void testTransform_matchesMultipleInputs() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray(new TransformIndex(0, 0),
                                                            new TransformIndex(0, 1),
                                                            new TransformIndex(0, 2));
        List<TransformIndex> writeIndicies = createIndexArray();

        Condition cond = new Condition(Operator.MATCH, "boat");

        ExcludeFilterRegex transform = new ExcludeFilterRegex(cond, readIndicies, writeIndicies, mock(Logger.class));

        String [] input = {"cat", "hat", "boat"};
        String [] scratch = {};
        String [] output = {};
        String [][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.EXCLUDE, transform.transform(readWriteArea));
    }


    @Test
    public void testTransform() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray(new TransformIndex(0, 0));
        List<TransformIndex> writeIndicies = createIndexArray();

        Condition cond = new Condition(Operator.MATCH, "^(?!latency\\.total).*$");

        ExcludeFilterRegex transform = new ExcludeFilterRegex(cond, readIndicies, writeIndicies, mock(Logger.class));
        String [] input = {"utilization.total"};
        String [] scratch = {};
        String [] output = new String [3];
        String [][] readWriteArea = {input, scratch, output};

        TransformResult tr = transform.transform(readWriteArea);
        assertEquals(TransformResult.EXCLUDE, tr);

        readWriteArea[0] = new String [] {"latency.total"};
        tr = transform.transform(readWriteArea);
        assertEquals(TransformResult.OK, tr);
    }
}
