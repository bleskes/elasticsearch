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

public class DoubleDateTransformerTest
{

    @Rule
    public ExpectedException m_ExpectedException = ExpectedException.none();

    @Test
    public void testTransform_GivenTimestampIsNotMilliseconds() throws TransformException
    {
    	DoubleDateTransform transformer = new DoubleDateTransform(false, new int [] {0},
												new int [] {0});

    	String [] input = {"1000"};
    	String [] output = new String[1];

		transformer.transform(input, output);

		assertEquals(1000, transformer.epoch());
		assertEquals("1000", output[0]);
    }

    @Test
    public void testTransform_GivenTimestampIsMilliseconds() throws TransformException
    {
    	DoubleDateTransform transformer = new DoubleDateTransform(true, new int [] {0},
				new int [] {0});

		String [] input = {"1000"};
		String [] output = new String[1];

		transformer.transform(input, output);

		assertEquals(1, transformer.epoch());
		assertEquals("1", output[0]);
    }

    @Test
    public void testTransform_GivenTimestampIsNotValidDouble() throws TransformException
    {
        m_ExpectedException.expect(ParseTimestampException.class);
        m_ExpectedException.expectMessage("Cannot parse timestamp 'invalid' as epoch value");

    	DoubleDateTransform transformer = new DoubleDateTransform(false, new int [] {0},
				new int [] {0});

		String [] input = {"invalid"};
		String [] output = new String[1];

		transformer.transform(input, output);
    }
}
