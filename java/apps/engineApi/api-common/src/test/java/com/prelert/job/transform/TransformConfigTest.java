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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.prelert.job.condition.Condition;
import com.prelert.job.condition.Operator;

public class TransformConfigTest
{
    @Rule
    public ExpectedException m_ExpectedException = ExpectedException.none();

    @Test
    public void testGetOutputs_GivenEmptyTransformConfig()
    {
        assertTrue(new TransformConfig().getOutputs().isEmpty());
    }

    @Test
    public void testGetOutputs_GivenNoExplicitOutputsSpecified()
    {
        TransformConfig config = new TransformConfig();
        config.setTransform("concat");

        assertEquals(Arrays.asList("concat"), config.getOutputs());
    }

    @Test
    public void testGetOutputs_GivenEmptyOutputsSpecified()
    {
        TransformConfig config = new TransformConfig();
        config.setTransform("concat");
        config.setOutputs(new ArrayList<>());

        assertEquals(Arrays.asList("concat"), config.getOutputs());
    }

    @Test
    public void testGetOutputs_GivenOutputsSpecified()
    {
        TransformConfig config = new TransformConfig();
        config.setTransform("concat");
        config.setOutputs(Arrays.asList("o1", "o2"));

        assertEquals(Arrays.asList("o1", "o2"), config.getOutputs());
    }

    @Test
    public void testVerify_GivenUnknownTransform()
    {
        m_ExpectedException.expect(IllegalArgumentException.class);

        TransformConfig tr = new TransformConfig();
        tr.setInputs(Arrays.asList("f1", "f2"));
        tr.setTransform("unknown+transform");

        tr.type();
    }

    @Test
    public void testEquals_GivenSameReference()
    {
        TransformConfig config = new TransformConfig();
        assertTrue(config.equals(config));
    }

    @Test
    public void testEquals_GivenDifferentClass()
    {
        TransformConfig config = new TransformConfig();
        assertFalse(config.equals("a string"));
    }

    @Test
    public void testEquals_GivenNull()
    {
        TransformConfig config = new TransformConfig();
        assertFalse(config.equals(null));
    }

    @Test
    public void testEquals_GivenEqualTransform()
    {
        TransformConfig config1 = new TransformConfig();
        config1.setTransform("concat");
        config1.setInputs(Arrays.asList("input1", "input2"));
        config1.setOutputs(Arrays.asList("output"));
        config1.setArguments(Arrays.asList("-"));
        config1.setCondition(new Condition());

        TransformConfig config2 = new TransformConfig();
        config2.setTransform("concat");
        config2.setInputs(Arrays.asList("input1", "input2"));
        config2.setOutputs(Arrays.asList("output"));
        config2.setArguments(Arrays.asList("-"));
        config2.setCondition(new Condition());

        assertTrue(config1.equals(config2));
        assertTrue(config2.equals(config1));
    }

    @Test
    public void testEquals_GivenDifferentType()
    {
        TransformConfig config1 = new TransformConfig();
        config1.setTransform("concat");

        TransformConfig config2 = new TransformConfig();
        config2.setTransform("lowercase");

        assertFalse(config1.equals(config2));
        assertFalse(config2.equals(config1));
    }

    @Test
    public void testEquals_GivenDifferentInputs()
    {
        TransformConfig config1 = new TransformConfig();
        config1.setTransform("concat");
        config1.setInputs(Arrays.asList("input1", "input2"));

        TransformConfig config2 = new TransformConfig();
        config2.setTransform("concat");
        config2.setInputs(Arrays.asList("input1", "input3"));

        assertFalse(config1.equals(config2));
        assertFalse(config2.equals(config1));
    }

    @Test
    public void testEquals_GivenDifferentOutputs()
    {
        TransformConfig config1 = new TransformConfig();
        config1.setTransform("concat");
        config1.setInputs(Arrays.asList("input1", "input2"));
        config1.setOutputs(Arrays.asList("output1"));

        TransformConfig config2 = new TransformConfig();
        config2.setTransform("concat");
        config2.setInputs(Arrays.asList("input1", "input2"));
        config2.setOutputs(Arrays.asList("output2"));

        assertFalse(config1.equals(config2));
        assertFalse(config2.equals(config1));
    }

    @Test
    public void testEquals_GivenDifferentArguments()
    {
        TransformConfig config1 = new TransformConfig();
        config1.setTransform("concat");
        config1.setInputs(Arrays.asList("input1", "input2"));
        config1.setOutputs(Arrays.asList("output"));
        config1.setArguments(Arrays.asList("-"));

        TransformConfig config2 = new TransformConfig();
        config2.setTransform("concat");
        config2.setInputs(Arrays.asList("input1", "input2"));
        config2.setOutputs(Arrays.asList("output"));
        config2.setArguments(Arrays.asList("--"));

        assertFalse(config1.equals(config2));
        assertFalse(config2.equals(config1));
    }

    @Test
    public void testEquals_GivenDifferentConditions()
    {
        TransformConfig config1 = new TransformConfig();
        config1.setTransform("concat");
        config1.setInputs(Arrays.asList("input1", "input2"));
        config1.setOutputs(Arrays.asList("output"));
        config1.setArguments(Arrays.asList("-"));
        config1.setCondition(new Condition(Operator.EQ, "foo"));

        TransformConfig config2 = new TransformConfig();
        config2.setTransform("concat");
        config2.setInputs(Arrays.asList("input1", "input2"));
        config2.setOutputs(Arrays.asList("output"));
        config2.setArguments(Arrays.asList("-"));
        config2.setCondition(new Condition());

        assertFalse(config1.equals(config2));
        assertFalse(config2.equals(config1));
    }
}
