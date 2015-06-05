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
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.prelert.transforms.Transform.TransformIndex;
import com.prelert.transforms.Transform.TransformResult;

public class ConcatTest
{
	@Test
	public void testMultipleInputs() throws TransformException
	{
	    List<TransformIndex> readIndicies = createIndexArray(new TransformIndex(0, 1), new TransformIndex(0, 2), new TransformIndex(0, 4));
	    List<TransformIndex> writeIndicies = createIndexArray(new TransformIndex(2, 1));

		Concat concat = new Concat(readIndicies, writeIndicies, mock(Logger.class));

		String [] input = {"a", "b", "c", "d", "e"};
		String [] scratch = {};
		String [] output = new String [2];
		String [][] readWriteArea = {input, scratch, output};

		assertEquals(TransformResult.OK, concat.transform(readWriteArea));
		assertNull(output[0]);
		assertEquals("bce", output[1]);
	}

	@Test
    public void testWithDelimiter() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray(new TransformIndex(0, 1), new TransformIndex(0, 2), new TransformIndex(0, 4));
        List<TransformIndex> writeIndicies = createIndexArray(new TransformIndex(2, 1));

        Concat concat = new Concat("--", readIndicies, writeIndicies, mock(Logger.class));

        String [] input = {"a", "b", "c", "d", "e"};
        String [] scratch = {};
        String [] output = new String [2];
        String [][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.OK, concat.transform(readWriteArea));
        assertNull(output[0]);
        assertEquals("b--c--e", output[1]);
    }

	@Test
	public void testZeroInputs() throws TransformException
	{
        List<TransformIndex> readIndicies = createIndexArray();
        List<TransformIndex> writeIndicies = createIndexArray(new TransformIndex(2, 0));

        Concat concat = new Concat(readIndicies, writeIndicies, mock(Logger.class));

		String [] input = {"a", "b", "c", "d", "e"};
		String [] scratch = {};
		String [] output = new String [1];
        String [][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.OK, concat.transform(readWriteArea));
		assertEquals("", output[0]);
	}

	@Test
	public void testNoOutput() throws TransformException
	{
	    List<TransformIndex> readIndicies = createIndexArray(new TransformIndex(0, 1), new TransformIndex(0, 2), new TransformIndex(0, 3));
	    List<TransformIndex> writeIndicies = createIndexArray();

	    Concat concat = new Concat(readIndicies, writeIndicies, mock(Logger.class));

		String [] input = {"a", "b", "c", "d", "e"};
		String [] scratch = {};
		String [] output = new String [1];
        String [][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.FAIL, concat.transform(readWriteArea));
		assertNull(output[0]);
	}

    @Test
    public void testScratchAreaInputs() throws TransformException
    {
        List<TransformIndex> readIndicies = createIndexArray(new TransformIndex(0, 1), new TransformIndex(0, 2),
                                        new TransformIndex(1, 0), new TransformIndex(1, 2));
        List<TransformIndex> writeIndicies = createIndexArray(new TransformIndex(1, 4));

        Concat concat = new Concat(readIndicies, writeIndicies, mock(Logger.class));

        String [] input = {"a", "b", "c", "d", "e"};
        String [] scratch = {"a", "b", "c", "d", null};
        String [] output = new String [1];
        String [][] readWriteArea = {input, scratch, output};

        assertEquals(TransformResult.OK, concat.transform(readWriteArea));
        assertEquals("bcac", scratch[4]);
    }
}
