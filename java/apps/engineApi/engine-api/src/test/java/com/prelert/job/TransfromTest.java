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

package com.prelert.job;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.prelert.rs.data.ErrorCode;


public class TransfromTest {

	@Test
	public void testUnknownTransfrom()
	{
		Transform tr = new Transform();
		tr.setInputs(Arrays.asList("f1", "f2"));
		tr.setTransform("unknown+transform");

		try
		{
			TransformType type = tr.type();
			fail("Unknown transform type should throw an TransformConfigurationException " +
					"Type = " + type);
		}
		catch (TransformConfigurationException e)
		{
			assertEquals(ErrorCode.UNKNOWN_TRANSFORM, e.getErrorCode());
		}
	}

	@Test
	public void testValidTransformVerifies() throws JobConfigurationException
	{
		Set<TransformType> types = EnumSet.allOf(TransformType.class);

		for (TransformType type : types)
		{
			List<String> inputs = new ArrayList<>();
			for (int arg = 0; arg < type.arity(); ++arg)
			{
				inputs.add(Integer.toString(arg));
			}

			Transform tr = new Transform();
			tr.setTransform(type.toString());
			tr.setInputs(inputs);

			tr.verify();
			assertEquals(type, tr.type());
		}
	}


	/**
	 * creating a transform with the wrong number of inputs should throw
	 */
	@Test
	public void testValidTransformVerifyThrows()
	{
		Set<TransformType> types = EnumSet.allOf(TransformType.class);

		for (TransformType type : types)
		{
			List<String> inputs = new ArrayList<>();

			for (int arg = 0; arg < type.arity() -1; ++arg)
			{
				inputs.add(Integer.toString(arg));
			}

			Transform tr = new Transform();
			tr.setTransform(type.toString());
			tr.setInputs(inputs);

			try
			{
				tr.verify();
				fail("Transform with the wrong argument count should throw an TransformConfigurationException");
			}
			catch (TransformConfigurationException e)
			{
				assertEquals(ErrorCode.INCORRECT_TRANSFORM_ARGUMENT_COUNT, e.getErrorCode());
			}
		}
	}

}
