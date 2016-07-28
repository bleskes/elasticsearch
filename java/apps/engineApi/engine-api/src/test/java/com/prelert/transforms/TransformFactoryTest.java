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

import com.prelert.job.condition.Condition;
import com.prelert.job.condition.Operator;
import com.prelert.job.transform.TransformConfig;
import com.prelert.job.transform.TransformType;
import com.prelert.transforms.Transform.TransformIndex;

public class TransformFactoryTest {

    @Test
    public void testIndiciesMapping()
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
    public void testConcatWithOptionalArgs()
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
        assertEquals("", ((Concat)tr).getDelimiter());

        conf.setArguments(Arrays.asList("delimiter"));
        tr = new TransformFactory().create(conf, inputMap, scratchMap,
                outputMap, mock(Logger.class));
        assertTrue(tr instanceof Concat);
        assertEquals("delimiter", ((Concat)tr).getDelimiter());
    }

    @Test
    public void testAllTypesCreated()
    {
        EnumSet<TransformType> all = EnumSet.allOf(TransformType.class);

        Map<String, Integer> inputIndicies = new HashMap<>();
        Map<String, Integer> scratchMap = new HashMap<>();
        Map<String, Integer> outputIndicies = new HashMap<>();

        for (TransformType type : all)
        {
            TransformConfig conf = TransformTestUtils.createValidTransform(type);
            conf.getInputs().stream().forEach(input -> inputIndicies.put(input, 0));
            conf.getOutputs().stream().forEach(output -> outputIndicies.put(output, 0));

            // throws IllegalArgumentException if it doesn't handle the type
            new TransformFactory().create(conf, inputIndicies, scratchMap,
                                outputIndicies, mock(Logger.class));
        }
    }

    @Test
    public void testExcludeTransformsCreated()
    {
        Map<String, Integer> inputIndicies = new HashMap<>();
        Map<String, Integer> scratchMap = new HashMap<>();
        Map<String, Integer> outputIndicies = new HashMap<>();


        TransformConfig conf = new TransformConfig();
        conf.setInputs(new ArrayList<String>());
        conf.setOutputs(new ArrayList<String>());
        conf.setTransform(TransformType.EXCLUDE.prettyName());
        conf.setCondition(new Condition(Operator.LT, "2000"));


        ExcludeFilterNumeric numericTransform =
                (ExcludeFilterNumeric) new TransformFactory().create(conf, inputIndicies,
                                scratchMap, outputIndicies, mock(Logger.class));

        assertEquals(Operator.LT, numericTransform.getCondition().getOperator());
        assertEquals(2000, numericTransform.filterValue(), 0.0000001);

        conf.setCondition(new Condition(Operator.MATCH, "aaaaa"));

        ExcludeFilterRegex regexTransform =
                (ExcludeFilterRegex) new TransformFactory().create(conf, inputIndicies,
                                scratchMap, outputIndicies, mock(Logger.class));

        assertEquals(Operator.MATCH, regexTransform.getCondition().getOperator());
        assertEquals("aaaaa", regexTransform.getCondition().getValue());
    }

}
