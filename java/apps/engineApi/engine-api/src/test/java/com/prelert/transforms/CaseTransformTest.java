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

import com.prelert.transforms.Transform.TransformIndex;
import com.prelert.transforms.Transform.TransformResult;

public class CaseTransformTest
{
    @Test(expected = IllegalArgumentException.class)
    public void testUpperCaseTransform_GivenZeroInputs() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray();
        List<TransformIndex> writeIndicies = createIndexArray(new TransformIndex(2, 1));

        CaseTransform.createUpperCase(readIndicies, writeIndicies, mock(Logger.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpperCaseTransform_GivenTwoInputs() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray(
                new TransformIndex(0, 0), new TransformIndex(0, 1));
        List<TransformIndex> writeIndicies = createIndexArray(new TransformIndex(2, 1));

        CaseTransform.createUpperCase(readIndicies, writeIndicies, mock(Logger.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLowerCaseTransform_GivenZeroInputs() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray();
        List<TransformIndex> writeIndicies = createIndexArray(new TransformIndex(2, 1));

        CaseTransform.createLowerCase(readIndicies, writeIndicies, mock(Logger.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLowerCaseTransform_GivenTwoInputs() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray(
                new TransformIndex(0, 0), new TransformIndex(0, 1));
        List<TransformIndex> writeIndicies = createIndexArray(new TransformIndex(2, 1));

        CaseTransform.createLowerCase(readIndicies, writeIndicies, mock(Logger.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpperTransform_GivenZeroOutputs() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray(new TransformIndex(0, 1));
        List<TransformIndex> writeIndicies = createIndexArray();

        CaseTransform.createUpperCase(readIndicies, writeIndicies, mock(Logger.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpperTransform_GivenTwoOutputs() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray(new TransformIndex(0, 1));
        List<TransformIndex> writeIndicies = createIndexArray(
                new TransformIndex(1, 1), new TransformIndex(1, 2));

        CaseTransform.createUpperCase(readIndicies, writeIndicies, mock(Logger.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLowerTransform_GivenZeroOutputs() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray(new TransformIndex(0, 1));
        List<TransformIndex> writeIndicies = createIndexArray();

        CaseTransform.createLowerCase(readIndicies, writeIndicies, mock(Logger.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLowerTransform_GivenTwoOutputs() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray(new TransformIndex(0, 1));
        List<TransformIndex> writeIndicies = createIndexArray(
                new TransformIndex(1, 1), new TransformIndex(1, 2));

        CaseTransform.createLowerCase(readIndicies, writeIndicies, mock(Logger.class));
    }

    @Test
    public void testUpperCaseTransform_GivenSingleInputAndSingleOutput() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray(new TransformIndex(0, 1));
        List<TransformIndex> writeIndicies = createIndexArray(new TransformIndex(2, 0));

        CaseTransform upperCase = CaseTransform.createUpperCase(readIndicies, writeIndicies,
                mock(Logger.class));

        String [] input = {"aa", "aBcD", "cc", "dd", "ee"};
        String [] scratch = {};
        String [] output = new String [1];
        String [][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.OK, upperCase.transform(readWriteArea));
        assertEquals("ABCD", output[0]);
    }


    @Test
    public void testLowerCaseTransform_GivenSingleInputAndSingleOutput() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray(new TransformIndex(0, 1));
        List<TransformIndex> writeIndicies = createIndexArray(new TransformIndex(2, 0));

        CaseTransform upperCase = CaseTransform.createLowerCase(readIndicies, writeIndicies,
                mock(Logger.class));

        String [] input = {"aa", "AbCde", "cc", "dd", "ee"};
        String [] scratch = {};
        String [] output = new String [1];
        String [][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.OK, upperCase.transform(readWriteArea));
        assertEquals("abcde", output[0]);
    }

}
