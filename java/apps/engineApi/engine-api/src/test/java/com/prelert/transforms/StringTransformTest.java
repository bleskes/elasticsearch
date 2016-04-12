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

package com.prelert.transforms;

import static com.prelert.transforms.TransformTestUtils.createIndexArray;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.prelert.transforms.Transform.TransformIndex;
import com.prelert.transforms.Transform.TransformResult;

public class StringTransformTest
{
    @Test(expected = IllegalArgumentException.class)
    public void testUpperCaseTransform_GivenZeroInputs() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray();
        List<TransformIndex> writeIndicies = createIndexArray(new TransformIndex(2, 1));

        StringTransform.createUpperCase(readIndicies, writeIndicies, mock(Logger.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpperCaseTransform_GivenTwoInputs() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray(
                new TransformIndex(0, 0), new TransformIndex(0, 1));
        List<TransformIndex> writeIndicies = createIndexArray(new TransformIndex(2, 1));

        StringTransform.createUpperCase(readIndicies, writeIndicies, mock(Logger.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpperCaseTransform_GivenZeroOutputs() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray(new TransformIndex(0, 1));
        List<TransformIndex> writeIndicies = createIndexArray();

        StringTransform.createUpperCase(readIndicies, writeIndicies, mock(Logger.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpperCaseTransform_GivenTwoOutputs() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray(new TransformIndex(0, 1));
        List<TransformIndex> writeIndicies = createIndexArray(
                new TransformIndex(1, 1), new TransformIndex(1, 2));

        StringTransform.createUpperCase(readIndicies, writeIndicies, mock(Logger.class));
    }

    @Test
    public void testUpperCaseTransform_GivenSingleInputAndSingleOutput() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray(new TransformIndex(0, 1));
        List<TransformIndex> writeIndicies = createIndexArray(new TransformIndex(2, 0));

        StringTransform upperCase = StringTransform.createUpperCase(readIndicies, writeIndicies,
                mock(Logger.class));

        String [] input = {"aa", "aBcD", "cc", "dd", "ee"};
        String [] scratch = {};
        String [] output = new String [1];
        String [][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.OK, upperCase.transform(readWriteArea));
        assertEquals("ABCD", output[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLowerCaseTransform_GivenZeroInputs() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray();
        List<TransformIndex> writeIndicies = createIndexArray(new TransformIndex(2, 1));

        StringTransform.createLowerCase(readIndicies, writeIndicies, mock(Logger.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLowerCaseTransform_GivenTwoInputs() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray(
                new TransformIndex(0, 0), new TransformIndex(0, 1));
        List<TransformIndex> writeIndicies = createIndexArray(new TransformIndex(2, 1));

        StringTransform.createLowerCase(readIndicies, writeIndicies, mock(Logger.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLowerCaseTransform_GivenZeroOutputs() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray(new TransformIndex(0, 1));
        List<TransformIndex> writeIndicies = createIndexArray();

        StringTransform.createLowerCase(readIndicies, writeIndicies, mock(Logger.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLowerCaseTransform_GivenTwoOutputs() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray(new TransformIndex(0, 1));
        List<TransformIndex> writeIndicies = createIndexArray(
                new TransformIndex(1, 1), new TransformIndex(1, 2));

        StringTransform.createLowerCase(readIndicies, writeIndicies, mock(Logger.class));
    }

    @Test
    public void testLowerCaseTransform_GivenSingleInputAndSingleOutput() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray(new TransformIndex(0, 1));
        List<TransformIndex> writeIndicies = createIndexArray(new TransformIndex(2, 0));

        StringTransform upperCase = StringTransform.createLowerCase(readIndicies, writeIndicies,
                mock(Logger.class));

        String [] input = {"aa", "AbCde", "cc", "dd", "ee"};
        String [] scratch = {};
        String [] output = new String [1];
        String [][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.OK, upperCase.transform(readWriteArea));
        assertEquals("abcde", output[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTrimTransform_GivenZeroInputs() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray();
        List<TransformIndex> writeIndicies = createIndexArray(new TransformIndex(2, 1));

        StringTransform.createTrim(readIndicies, writeIndicies, mock(Logger.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTrimTransform_GivenTwoInputs() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray(
                new TransformIndex(0, 0), new TransformIndex(0, 1));
        List<TransformIndex> writeIndicies = createIndexArray(new TransformIndex(2, 1));

        StringTransform.createTrim(readIndicies, writeIndicies, mock(Logger.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTrimTransform_GivenZeroOutputs() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray(new TransformIndex(0, 1));
        List<TransformIndex> writeIndicies = createIndexArray();

        StringTransform.createTrim(readIndicies, writeIndicies, mock(Logger.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTrimTransform_GivenTwoOutputs() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray(new TransformIndex(0, 1));
        List<TransformIndex> writeIndicies = createIndexArray(
                new TransformIndex(1, 1), new TransformIndex(1, 2));

        StringTransform.createTrim(readIndicies, writeIndicies, mock(Logger.class));
    }

    @Test
    public void testTrimTransform_GivenSingleInputAndSingleOutput() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray(new TransformIndex(0, 1));
        List<TransformIndex> writeIndicies = createIndexArray(new TransformIndex(2, 0));

        StringTransform upperCase = StringTransform.createTrim(readIndicies, writeIndicies,
                mock(Logger.class));

        String [] input = {"  a ", "\t b ", " c", "d", "e"};
        String [] scratch = {};
        String [] output = new String [1];
        String [][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.OK, upperCase.transform(readWriteArea));
        assertEquals("b", output[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGeoUnhashTransform_GivenZeroInputs() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray();
        List<TransformIndex> writeIndicies = createIndexArray(new TransformIndex(2, 1));

        StringTransform.createGeoUnhash(readIndicies, writeIndicies, mock(Logger.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGeoUnhashTransform_GivenTwoInputs() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray(
                new TransformIndex(0, 0), new TransformIndex(0, 1));
        List<TransformIndex> writeIndicies = createIndexArray(new TransformIndex(2, 1));

        StringTransform.createGeoUnhash(readIndicies, writeIndicies, mock(Logger.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGeoUnhashTransform_GivenZeroOutputs() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray(new TransformIndex(0, 1));
        List<TransformIndex> writeIndicies = createIndexArray();

        StringTransform.createGeoUnhash(readIndicies, writeIndicies, mock(Logger.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGeoUnhashTransform_GivenTwoOutputs() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray(new TransformIndex(0, 1));
        List<TransformIndex> writeIndicies = createIndexArray(
                new TransformIndex(1, 1), new TransformIndex(1, 2));

        StringTransform.createGeoUnhash(readIndicies, writeIndicies, mock(Logger.class));
    }

    @Test
    public void testGeoUnhashTransform_GivenSingleInputAndSingleOutput() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray(new TransformIndex(0, 1));
        List<TransformIndex> writeIndicies = createIndexArray(new TransformIndex(2, 0));

        StringTransform upperCase = StringTransform.createGeoUnhash(readIndicies, writeIndicies,
                mock(Logger.class));

        String [] input = {"  a ", "drm3btev3e86", " c", "d", "e"};
        String [] scratch = {};
        String [] output = new String [1];
        String [][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.OK, upperCase.transform(readWriteArea));
        String[] splitLatLong = output[0].split(",");
        assertEquals(2, splitLatLong.length);
        assertEquals(41.12, Double.parseDouble(splitLatLong[0]), 0.001);
        assertEquals(-71.34, Double.parseDouble(splitLatLong[1]), 0.001);
    }
}
