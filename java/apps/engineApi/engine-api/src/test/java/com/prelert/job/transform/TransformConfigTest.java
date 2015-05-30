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
import java.util.Optional;
import java.util.Set;

import org.junit.Test;

import com.prelert.job.exceptions.JobConfigurationException;
import com.prelert.job.transform.TransformConfig;
import com.prelert.job.transform.TransformType;
import com.prelert.job.transform.condition.Condition;
import com.prelert.job.transform.condition.Operation;
import com.prelert.job.transform.exceptions.TransformConfigurationException;
import com.prelert.rs.data.ErrorCode;


public class TransformConfigTest
{

    @Test
    public void testUnknownTransform()
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

            int arity = type.arity();
            if (type.arity() < 0)
            {
                // variadic
                arity = 2;
            }

            for (int arg = 0; arg < arity; ++arg)
            {
                inputs.add(Integer.toString(arg));
            }

            List<String> initArgs = new ArrayList<>();
            if (type == TransformType.EXCLUDE_FILTER_NUMERIC)
            {
                // this one needs specific arguments that are verified
                initArgs.add(Operation.EQ.toString());
                initArgs.add("100");
            }
            else
            {
                for (int arg = 0; arg < type.argumentCount(); ++arg)
                {
                    initArgs.add(Integer.toString(arg));
                }
            }

            TransformConfig tr = new TransformConfig();
            tr.setTransform(type.toString());
            tr.setInputs(inputs);
            tr.setArguments(initArgs);

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
    public void testInvalidTransformVerifyThrows()
    {
        Set<TransformType> types = EnumSet.allOf(TransformType.class);

        for (TransformType type : types)
        {
            TransformConfig tr = new TransformConfig();
            tr.setTransform(type.toString());

            List<String> inputs = new ArrayList<>();
            tr.setInputs(inputs);

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

            // set the wrong number of arguments
            int initArgCount = 0;
            if (type.argumentCount() == 0 && type.optionalArgumentCount() == 0)
            {
                initArgCount = 1;
            }
            else if (type.optionalArgumentCount() > 0)
            {
                initArgCount = type.argumentCount() + type.optionalArgumentCount() + 1;
            }

            List<String> initArgs = new ArrayList<>();
            for (int i=0; i<initArgCount; i++)
            {
                initArgs.add(Integer.toString(i));
            }
            tr.setArguments(initArgs);

            try
            {
                tr.verify();
                fail("Transform with the wrong init argument count should throw an TransformConfigurationException");
            }
            catch (TransformConfigurationException e)
            {
                assertEquals(ErrorCode.TRANSFORM_INVALID_ARGUMENT_COUNT, e.getErrorCode());
            }

            initArgs = new ArrayList<>();
            for (int i=0; i<type.argumentCount(); i++)
            {
                initArgs.add(Integer.toString(i));
            }
            tr.setArguments(initArgs);


            try
            {
                tr.verify();
                fail("Transform with the wrong argument count should throw an TransformConfigurationException");
            }
            catch (TransformConfigurationException e)
            {
                assertEquals(ErrorCode.INCORRECT_TRANSFORM_INPUT_COUNT, e.getErrorCode());
            }

        }
    }

    @Test
    public void testVerify_NoInputsThrows()
    {
        Set<TransformType> types = EnumSet.allOf(TransformType.class);

        for (TransformType type : types)
        {
            TransformConfig tr = new TransformConfig();
            tr.setTransform(type.toString());

            List<String> initArgs = new ArrayList<>();
            for (int i=0; i<type.argumentCount(); i++)
            {
                initArgs.add(Integer.toString(i));
            }
            tr.setArguments(initArgs);

            try
            {
                tr.verify();
                fail("Transform with zero inputs should throw an TransformConfigurationException");
            }
            catch (TransformConfigurationException e)
            {
                assertEquals(ErrorCode.INCORRECT_TRANSFORM_INPUT_COUNT, e.getErrorCode());
            }

        }
    }

    @Test
    public void testVerify_typeHasCondition() throws TransformConfigurationException
    {
        TransformConfig tr = new TransformConfig();
        tr.setTransform(TransformType.EXCLUDE_FILTER_NUMERIC.prettyName());
        tr.setInputs(Arrays.asList("in"));
        tr.setOutputs(Arrays.asList("out"));

        try
        {
            tr.verify();
            fail("exclude filter numeric transform without arguments " +
                    "should throw an TransformConfigurationException");
        }
        catch (TransformConfigurationException e)
        {
            assertEquals(ErrorCode.TRANSFORM_INVALID_ARGUMENT_COUNT, e.getErrorCode());
        }

        // can't parse args
        tr.setArguments(Arrays.asList("bad-arg1", "bad-arg2"));
        try
        {
            tr.verify();
            fail("exclude filter numeric transform with bad arguments " +
                    "should throw an TransformConfigurationException");
        }
        catch (TransformConfigurationException e)
        {
            assertEquals(ErrorCode.TRANSFORM_INVALID_ARGUMENT, e.getErrorCode());
        }

        // too many args
        tr.setArguments(Arrays.asList("100.0", "lte", "bad-extra-arg"));
        try
        {
            tr.verify();
            fail("exclude filter numeric transform with bad arguments " +
                    "should throw an TransformConfigurationException");
        }
        catch (TransformConfigurationException e)
        {
            assertEquals(ErrorCode.TRANSFORM_INVALID_ARGUMENT_COUNT, e.getErrorCode());
        }

        // this works
        tr.setArguments(Arrays.asList("100.0", "lte"));
        tr.verify();

        Optional<Condition> cond = tr.getCondition();
        assertTrue(cond.isPresent());
        assertEquals(Operation.LTE, cond.get().getOp());
        assertEquals(100.0, cond.get().getFilterValue(), 0.0000001);
    }

}
