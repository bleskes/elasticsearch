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

package com.prelert.transforms.date;

import static com.prelert.transforms.TransformTestUtils.createIndexArray;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.prelert.transforms.Transform.TransformIndex;
import com.prelert.transforms.TransformException;

public class DateFormatDateTransformerTest
{
    @Rule
    public ExpectedException m_ExpectedException = ExpectedException.none();

    @Test
    public void testTransform_GivenValidTimestamp() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray(new TransformIndex(0, 0));
        List<TransformIndex> writeIndicies = createIndexArray(new TransformIndex(2, 0));

        DateFormatTransform transformer = new DateFormatTransform("y-M-d",
                                        readIndicies, writeIndicies, mock(Logger.class));

    	String [] input = {"2014-01-01"};
    	String [] scratch = {};
    	String [] output = new String[1];
        String [][] readWriteArea = {input, scratch, output};

        transformer.transform(readWriteArea);

        assertEquals(1388534400, transformer.epoch());
        assertEquals("1388534400", output[0]);
    }

    @Test
    public void testTransform_GivenInvalidTimestamp() throws TransformException
    {
        m_ExpectedException.expect(ParseTimestampException.class);
        m_ExpectedException.expectMessage("Cannot parse date 'invalid' with format string 'y-M-d'");

        List<TransformIndex> readIndicies = createIndexArray(new TransformIndex(0, 0));
        List<TransformIndex> writeIndicies = createIndexArray(new TransformIndex(2, 0));

        DateFormatTransform transformer = new DateFormatTransform("y-M-d",
                                        readIndicies, writeIndicies, mock(Logger.class));

    	String [] input = {"invalid"};
    	String [] scratch = {};
    	String [] output = new String[1];
        String [][] readWriteArea = {input, scratch, output};

        transformer.transform(readWriteArea);
    }

    @Test
    public void testTransform_GivenNull() throws TransformException
    {
        m_ExpectedException.expect(ParseTimestampException.class);

        List<TransformIndex> readIndicies = createIndexArray(new TransformIndex(1, 0));
        List<TransformIndex> writeIndicies = createIndexArray(new TransformIndex(2, 0));

        DateFormatTransform transformer = new DateFormatTransform("y-M-d",
                                        readIndicies, writeIndicies, mock(Logger.class));

    	String [] input = {};
    	String [] scratch = {null};
    	String [] output = new String[1];
        String [][] readWriteArea = {input, scratch, output};

        transformer.transform(readWriteArea);
    }

    @Test
    public void testTransform_GivenBadFormat() throws TransformException
    {
        m_ExpectedException.expect(IllegalArgumentException.class);

        List<TransformIndex> readIndicies = createIndexArray(new TransformIndex(0, 0));
        List<TransformIndex> writeIndicies = createIndexArray(new TransformIndex(2, 0));

        DateFormatTransform transformer = new DateFormatTransform("e",
                                        readIndicies, writeIndicies, mock(Logger.class));

    	String [] input = {"2015-02-01"};
    	String [] scratch = {};
    	String [] output = new String[1];
        String [][] readWriteArea = {input, scratch, output};

        transformer.transform(readWriteArea);
    }

    @Test
    public void testTransform_FromScratchArea() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray(new TransformIndex(1, 0));
        List<TransformIndex> writeIndicies = createIndexArray(new TransformIndex(2, 0));

        DateFormatTransform transformer = new DateFormatTransform("y-M-d",
                                        readIndicies, writeIndicies, mock(Logger.class));

        String [] input = {};
        String [] scratch = {"2014-01-01"};
        String [] output = new String[1];
        String [][] readWriteArea = {input, scratch, output};

        transformer.transform(readWriteArea);

        assertEquals(1388534400, transformer.epoch());
        assertEquals("1388534400", output[0]);
    }


    @Test
    public void testTransform_WithBrackets() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray(new TransformIndex(0, 0));
        List<TransformIndex> writeIndicies = createIndexArray(new TransformIndex(2, 0));

        DateFormatTransform transformer = new DateFormatTransform("[yyyy-MM-dd HH:mm:ssX]",
                                        readIndicies, writeIndicies, mock(Logger.class));

        String [] input = {"[2014-06-23 00:00:00Z]"};
        String [] scratch = {};
        String [] output = new String[1];
        String [][] readWriteArea = {input, scratch, output};

        transformer.transform(readWriteArea);
    }

    @Test
    public void testTransform_Example() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray(new TransformIndex(0, 0));
        List<TransformIndex> writeIndicies = createIndexArray(new TransformIndex(2, 0));

        DateFormatTransform transformer = new DateFormatTransform("[dd/MMM/yyyy:HH:mm:ssX",
                                        readIndicies, writeIndicies, mock(Logger.class));

        String [] input = {"[02/Jul/2013:13:36:07+0000"};
        String [] scratch = {};
        String [] output = new String[1];
        String [][] readWriteArea = {input, scratch, output};

        transformer.transform(readWriteArea);
    }
}
