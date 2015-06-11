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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.Range;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobConfigurationException;
import com.prelert.job.transform.condition.Condition;
import com.prelert.job.transform.condition.Operator;
import com.prelert.job.transform.exceptions.TransformConfigurationException;
import com.prelert.transforms.TransformTestUtils;


public class TransformConfigTest
{

    @Test
    public void testVerify_GivenUnknownTransform()
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
            assertEquals(ErrorCodes.UNKNOWN_TRANSFORM, e.getErrorCode());
        }
    }

    @Test
    public void testVerify_GivenValidTransform() throws JobConfigurationException
    {
        Set<TransformType> types = EnumSet.allOf(TransformType.class);

        for (TransformType type : types)
        {
            TransformConfig tr = TransformTestUtils.createValidTransform(type);
            tr.verify();
            assertEquals(type, tr.type());
        }
    }

    @Test
    public void testVerify_GivenConcat() throws JobConfigurationException
    {
        List<String> inputs = new ArrayList<>();

        TransformConfig tr = new TransformConfig();
        tr.setTransform(TransformType.Names.CONCAT_NAME);
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

    @Test
    public void testVerify_GivenInvalidInputCount()
    {
        Set<TransformType> types = EnumSet.allOf(TransformType.class);

        for (TransformType type : types)
        {
            TransformConfig tr = TransformTestUtils.createValidTransform(type);

            Range<Integer> arityRange = type.arityRange();
            int invalidArity = arityRange.hasLowerBound() ? arityRange.lowerEndpoint() - 1 : 0;
            List<String> inputs = new ArrayList<>();
            for (int arg = 0; arg < invalidArity; ++arg)
            {
                inputs.add(Integer.toString(arg));
            }
            tr.setInputs(inputs);

            try
            {
                tr.verify();
                fail("Transform with the wrong input count should throw an TransformConfigurationException");
            }
            catch (TransformConfigurationException e)
            {
                assertEquals(ErrorCodes.TRANSFORM_INVALID_INPUT_COUNT, e.getErrorCode());
            }
        }
    }

    @Test
    public void testVerify_GivenInvalidArgumentCount()
    {
        Set<TransformType> types = EnumSet.allOf(TransformType.class);

        for (TransformType type : types)
        {
            TransformConfig tr = TransformTestUtils.createValidTransform(type);

            Range<Integer> argsRange = type.argumentsRange();
            int invalidArgsCount = argsRange.hasUpperBound() ? argsRange.upperEndpoint() + 1
                    : argsRange.lowerEndpoint() - 1;
            List<String> args = new ArrayList<>();
            for (int arg = 0; arg < invalidArgsCount; ++arg)
            {
                args.add(Integer.toString(arg));
            }
            tr.setArguments(args);

            try
            {
                tr.verify();
                fail("Transform with the wrong args count should throw an TransformConfigurationException");
            }
            catch (TransformConfigurationException e)
            {
                assertEquals(ErrorCodes.TRANSFORM_INVALID_ARGUMENT_COUNT, e.getErrorCode());
            }
        }
    }

    @Test
    public void testVerify_GivenInvalidOutputCount()
    {
        Set<TransformType> types = EnumSet.allOf(TransformType.class);

        for (TransformType type : types)
        {
            TransformConfig tr = TransformTestUtils.createValidTransform(type);

            Range<Integer> outputsRange = type.outputsRange();

            // If there is no upper bound and the lower bound is 0 or 1 then we cannot invalidate
            // outputs due to the default output names.
            if (!outputsRange.hasUpperBound()
                    && (outputsRange.hasLowerBound() && outputsRange.lowerEndpoint() <= 1))
            {
                continue;
            }

            int invalidOutputCount = outputsRange.hasUpperBound() ? outputsRange.upperEndpoint() + 1
                    : outputsRange.lowerEndpoint() - 1;
            List<String> outputs = new ArrayList<>();
            for (int output = 0; output < invalidOutputCount; ++output)
            {
                outputs.add(Integer.toString(output));
            }
            tr.setOutputs(outputs);

            try
            {
                tr.verify();
                fail("Transform with the wrong output count should throw an TransformConfigurationException");
            }
            catch (TransformConfigurationException e)
            {
                assertEquals(ErrorCodes.TRANSFORM_INVALID_OUTPUT_COUNT, e.getErrorCode());
            }
        }
    }

    @Test
    public void testVerify_NoInputsThrows()
    {
        Set<TransformType> types = EnumSet.allOf(TransformType.class);

        for (TransformType type : types)
        {
            TransformConfig tr = TransformTestUtils.createValidTransform(type);
            tr.setInputs(Collections.emptyList());

            try
            {
                tr.verify();
                fail("Transform with zero inputs should throw an TransformConfigurationException");
            }
            catch (TransformConfigurationException e)
            {
                assertEquals(ErrorCodes.TRANSFORM_INVALID_INPUT_COUNT, e.getErrorCode());
            }

        }
    }

    @Test
    public void testVerify_TypeHasCondition() throws TransformConfigurationException
    {
        TransformConfig tr = new TransformConfig();
        tr.setTransform(TransformType.EXCLUDE.prettyName());
        tr.setInputs(Arrays.asList("in"));

        try
        {
            tr.verify();
            fail("exclude filter without condition " +
                    "should throw an TransformConfigurationException");
        }
        catch (TransformConfigurationException e)
        {
            assertEquals(ErrorCodes.TRANSFORM_REQUIRES_CONDITION, e.getErrorCode());
        }

        // too many args
        tr.setArguments(Arrays.asList("100.0", "lte", "bad-extra-arg"));
        try
        {
            tr.verify();
            fail("exclude with bad arguments " +
                    "should throw an TransformConfigurationException");
        }
        catch (TransformConfigurationException e)
        {
            assertEquals(ErrorCodes.TRANSFORM_INVALID_ARGUMENT_COUNT, e.getErrorCode());
        }

        // this works
        tr.setCondition(new Condition(Operator.LTE, "20.00001"));

        Condition cond = tr.getCondition();
        assertNotNull(cond);
        assertEquals(Operator.LTE, cond.getOperator());
        assertEquals("20.00001", cond.getValue());
    }

}
