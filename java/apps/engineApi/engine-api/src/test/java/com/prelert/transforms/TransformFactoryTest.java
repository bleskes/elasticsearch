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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.prelert.job.TransformConfig;
import com.prelert.job.TransformConfigurationException;
import com.prelert.job.TransformType;

public class TransformFactoryTest {

	@Test
	public void testIndiciesMapping() throws TransformConfigurationException
	{
		TransformConfig conf = new TransformConfig();
		conf.setInputs(Arrays.asList("field1", "field2"));
		conf.setOutputs(Arrays.asList("concatted"));
		conf.setTransform(TransformType.CONCAT.prettyName());

		Map<String, Integer> inputMap = new HashMap<>();
		inputMap.put("field1", 5);
		inputMap.put("field2", 3);


		Map<String, Integer> outputMap = new HashMap<>();
		outputMap.put("concatted", 2);

		Transform tr = new TransformFactory().create(conf, inputMap, outputMap, mock(Logger.class));
		assertTrue(tr instanceof Concat);

		int [] inputIndicies = tr.inputIndicies();
		assertEquals(inputIndicies[0], 5);
		assertEquals(inputIndicies[1], 3);

		int [] outputIndicies = tr.outputIndicies();
		assertEquals(outputIndicies[0], 2);
	}

	@Test
	public void testAllTypesCreated() throws TransformConfigurationException
	{
		EnumSet<TransformType> all = EnumSet.allOf(TransformType.class);

		Map<String, Integer> inputIndicies = new HashMap<>();
		Map<String, Integer> outputIndicies = new HashMap<>();

		for (TransformType type : all)
		{
			TransformConfig conf = new TransformConfig();
			conf.setInputs(new ArrayList<String>());
			conf.setOutputs(new ArrayList<String>());
			conf.setTransform(type.prettyName());

			// throws IllegalArgumentException if it doesn't handle the type
			new TransformFactory().create(conf, inputIndicies, outputIndicies, mock(Logger.class));
		}
	}

}
