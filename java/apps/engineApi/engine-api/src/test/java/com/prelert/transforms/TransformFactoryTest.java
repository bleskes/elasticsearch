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
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.prelert.job.transform.TransformConfig;
import com.prelert.job.transform.TransformConfigurationException;
import com.prelert.job.transform.TransformType;
import com.prelert.job.transform.condition.Operation;
import com.prelert.transforms.Transform.TransformIndex;

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

		Map<String, Integer> scratchMap = new HashMap<>();

		Map<String, Integer> outputMap = new HashMap<>();
		outputMap.put("concatted", 2);

		Transform tr = new TransformFactory().create(conf, inputMap, scratchMap,
		                                    outputMap, mock(Logger.class));
		assertTrue(tr instanceof Concat);

		List<TransformIndex> inputIndicies = tr.getReadIndicies();
		assertEquals(inputIndicies.get(0), new TransformIndex(0, 5));
		assertEquals(inputIndicies.get(1), new TransformIndex(0, 3));

		List<TransformIndex> outputIndicies = tr.getWriteIndicies();
		assertEquals(outputIndicies.get(0), new TransformIndex(2, 2));
	}

	@Test
	public void testConcatWithOptionalArgs() throws TransformConfigurationException
	{
	    TransformConfig conf = new TransformConfig();
	    conf.setTransform(TransformType.CONCAT.prettyName());
	    conf.setInputs(Arrays.asList("field1", "field2"));
	    conf.setOutputs(Arrays.asList("concatted"));

	    Map<String, Integer> inputMap = new HashMap<>();
	    inputMap.put("field1", 5);
	    inputMap.put("field2", 3);

	    Map<String, Integer> scratchMap = new HashMap<>();

	    Map<String, Integer> outputMap = new HashMap<>();
	    outputMap.put("concatted", 2);

	    Transform tr = new TransformFactory().create(conf, inputMap, scratchMap,
	            outputMap, mock(Logger.class));
	    assertTrue(tr instanceof Concat);
	    assertEquals(null, ((Concat)tr).getDelimiter());

	    conf.setArguments(Arrays.asList("delimiter"));
	    tr = new TransformFactory().create(conf, inputMap, scratchMap,
	            outputMap, mock(Logger.class));
	    assertTrue(tr instanceof Concat);
	    assertEquals("delimiter", ((Concat)tr).getDelimiter());
	}

	@Test
	public void testAllTypesCreated() throws TransformConfigurationException
	{
		EnumSet<TransformType> all = EnumSet.allOf(TransformType.class);

		Map<String, Integer> inputIndicies = new HashMap<>();
		Map<String, Integer> scratchMap = new HashMap<>();
		Map<String, Integer> outputIndicies = new HashMap<>();

		for (TransformType type : all)
		{
			TransformConfig conf = new TransformConfig();
			conf.setInputs(new ArrayList<String>());
			conf.setOutputs(new ArrayList<String>());
			conf.setTransform(type.prettyName());

			if (type.hasCondition())
			{
			    conf.setArguments(Arrays.asList("gt", "100"));
			}
			else
			{
			    List<String> args = new ArrayList<>();
			    for (int i=0; i<type.argumentCount(); i++)
			    {
			        args.add(Integer.toString(i));
			    }
			    conf.setArguments(args);
			}



			// throws IllegalArgumentException if it doesn't handle the type
			new TransformFactory().create(conf, inputIndicies, scratchMap,
			                    outputIndicies, mock(Logger.class));
		}
	}

	@Test
	public void testExcludeFilterNumericWithoutCondition() throws TransformConfigurationException
	{
	    Map<String, Integer> inputIndicies = new HashMap<>();
	    Map<String, Integer> scratchMap = new HashMap<>();
	    Map<String, Integer> outputIndicies = new HashMap<>();


        TransformConfig conf = new TransformConfig();
        conf.setInputs(new ArrayList<String>());
        conf.setOutputs(new ArrayList<String>());
        conf.setTransform(TransformType.EXCLUDE_FILTER_NUMERIC.prettyName());


        ExcludeFilterNumeric transform =
                (ExcludeFilterNumeric) new TransformFactory().create(conf, inputIndicies,
                                scratchMap, outputIndicies, mock(Logger.class));

        assertEquals(Operation.LT, transform.getCondition().getOp());
        assertEquals(0.0, transform.getCondition().getFilterValue(), 0.00001);

        conf.setArguments(Arrays.asList("eq", "100"));

        transform =
                (ExcludeFilterNumeric) new TransformFactory().create(conf, inputIndicies,
                                scratchMap, outputIndicies, mock(Logger.class));

        assertEquals(Operation.EQ, transform.getCondition().getOp());
        assertEquals(100.0, transform.getCondition().getFilterValue(), 0.00001);
	}

}
