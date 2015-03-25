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

package com.prelert.job.transform;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.prelert.job.exceptions.JobConfigurationException;
import com.prelert.job.transform.TransformConfig;
import com.prelert.job.transform.TransformConfigurationException;
import com.prelert.job.transform.TransformType;
import com.prelert.rs.data.ErrorCode;


public class TransformConfigTest
{

	@Test
	public void testUnknownTransfrom()
	{
		TransformConfig tr = new TransformConfig();
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

			int argCount = type.arity();
			if (type.arity() < 0)
			{
				// variadic
				argCount = 2;
			}

			for (int arg = 0; arg < argCount; ++arg)
			{
				inputs.add(Integer.toString(arg));
			}

			TransformConfig tr = new TransformConfig();
			tr.setTransform(type.toString());
			tr.setInputs(inputs);

			tr.verify();
			assertEquals(type, tr.type());
		}
	}

	@Test
	public void testVariadicTransformVerifies() throws JobConfigurationException
	{

		List<String> inputs = new ArrayList<>();

		TransformConfig tr = new TransformConfig();
		tr.setTransform(TransformType.Names.CONCAT);
		assertEquals(TransformType.CONCAT, tr.type());

		tr.setInputs(inputs);
		for (int arg = 0; arg < 1; ++arg)
		{
			inputs.add(Integer.toString(arg));
		}
		tr.verify();

		inputs.clear();
		for (int arg = 0; arg < 2; ++arg)
		{
			inputs.add(Integer.toString(arg));
		}
		tr.verify();

		inputs.clear();
		for (int arg = 0; arg < 3; ++arg)
		{
			inputs.add(Integer.toString(arg));
		}
		tr.verify();
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


			int argCount = type.arity() -1;
			if (type.arity() < 0)
			{
				// variadic
				argCount = 0;
			}

			for (int arg = 0; arg < argCount; ++arg)
			{
				inputs.add(Integer.toString(arg));
			}

			TransformConfig tr = new TransformConfig();
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
