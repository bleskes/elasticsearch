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

import org.junit.Test;

public class ConcatTest
{
	@Test
	public void testMultipleInputs() throws TransformException
	{
		Concat concat = new Concat(new int [] {1,  2, 4}, new int [] {1});

		String [] input = {"a", "b", "c", "d", "e"};
		String [] output = new String [2];

		concat.transform(input, output);
		assertNull(output[0]);
		assertEquals("bce", output[1]);
	}

	@Test
	public void testZeroInputs() throws TransformException
	{
		Concat concat = new Concat(new int [] {}, new int [] {0});

		String [] input = {"a", "b", "c", "d", "e"};
		String [] output = new String [1];

		concat.transform(input, output);
		assertEquals("", output[0]);
	}

	@Test
	public void testNoOutput() throws TransformException
	{
		Concat concat = new Concat(new int [] {}, new int [0]);

		String [] input = {"a", "b", "c", "d", "e"};
		String [] output = new String [1];

		concat.transform(input, output);
		assertNull(output[0]);
	}
}
