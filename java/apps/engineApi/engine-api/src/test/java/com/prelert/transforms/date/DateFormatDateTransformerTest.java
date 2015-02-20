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

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.prelert.transforms.TransformException;

public class DateFormatDateTransformerTest
{

    @Rule
    public ExpectedException m_ExpectedException = ExpectedException.none();

    @Test
    public void testTransform_GivenValidTimestamp() throws TransformException
    {
    	DateFormatTransform transformer = new DateFormatTransform("y-M-d", new int [] {0},
    														new int [] {0});

    	String [] input = {"2014-01-01"};
    	String [] output = new String[1];

    	transformer.transform(input, output);

        assertEquals(1388534400, transformer.epoch());
        assertEquals("1388534400", output[0]);
    }

    @Test
    public void testTransform_GivenInvalidTimestamp() throws TransformException
    {
        m_ExpectedException.expect(ParseTimestampException.class);
        m_ExpectedException.expectMessage("Cannot parse date 'invalid' with format string 'y-M-d'");

    	DateFormatTransform transformer = new DateFormatTransform("y-M-d",
    							new int [] {0}, new int [] {0});

    	String [] input = {"invalid"};
    	String [] output = new String[1];

    	transformer.transform(input, output);
    }

    @Test
    public void testTransform_GivenNull() throws TransformException
    {
        m_ExpectedException.expect(ParseTimestampException.class);

    	DateFormatTransform transformer = new DateFormatTransform("y-M-d",
    							new int [] {0}, new int [] {0});

    	String [] input = {null};
    	String [] output = new String[1];

    	transformer.transform(input, output);
    }

    @Test
    public void testTransform_GivenBadFormat() throws TransformException
    {
        m_ExpectedException.expect(IllegalArgumentException.class);

    	DateFormatTransform transformer = new DateFormatTransform("e",
    							new int [] {0}, new int [] {0});

    	String [] input = {"2015-02-01"};
    	String [] output = new String[1];

    	transformer.transform(input, output);
    }
}
