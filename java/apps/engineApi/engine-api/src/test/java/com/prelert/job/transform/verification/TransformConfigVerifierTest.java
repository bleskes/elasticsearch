/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
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
package com.prelert.job.transform.verification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.Range;
import com.prelert.job.condition.Condition;
import com.prelert.job.condition.Operator;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobConfigurationException;
import com.prelert.job.transform.TransformConfig;
import com.prelert.job.transform.TransformType;
import com.prelert.transforms.TransformTestUtils;

public class TransformConfigVerifierTest
{
    @Rule
    public ExpectedException m_ExpectedException = ExpectedException.none();

    @Test
    public void testVerify_GivenValidTransform() throws JobConfigurationException
    {
        Set<TransformType> types = EnumSet.allOf(TransformType.class);

        for (TransformType type : types)
        {
            TransformConfig tr = TransformTestUtils.createValidTransform(type);
            TransformConfigVerifier.verify(tr);
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
        for (int arg = 0; arg < 2; ++arg)
        {
            inputs.add(Integer.toString(arg));
        }
        TransformConfigVerifier.verify(tr);

        inputs.clear();
        for (int arg = 0; arg < 3; ++arg)
        {
            inputs.add(Integer.toString(arg));
        }
        TransformConfigVerifier.verify(tr);
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
                TransformConfigVerifier.verify(tr);
                fail("Transform with the wrong input count should throw a JobConfigurationException");
            }
            catch (JobConfigurationException e)
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
                TransformConfigVerifier.verify(tr);
                fail("Transform with the wrong args count should throw a JobConfigurationException");
            }
            catch (JobConfigurationException e)
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
                TransformConfigVerifier.verify(tr);
                fail("Transform with the wrong output count should throw a JobConfigurationException");
            }
            catch (JobConfigurationException e)
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
                TransformConfigVerifier.verify(tr);
                fail("Transform with zero inputs should throw a JobConfigurationException");
            }
            catch (JobConfigurationException e)
            {
                assertEquals(ErrorCodes.TRANSFORM_INVALID_INPUT_COUNT, e.getErrorCode());
            }
        }
    }

    @Test
    public void testVerify_TypeHasCondition() throws JobConfigurationException
    {
        TransformConfig tr = new TransformConfig();
        tr.setTransform(TransformType.EXCLUDE.prettyName());
        tr.setInputs(Arrays.asList("in"));

        try
        {
            TransformConfigVerifier.verify(tr);
            fail("exclude filter without condition should throw a JobConfigurationException");
        }
        catch (JobConfigurationException e)
        {
            assertEquals(ErrorCodes.TRANSFORM_REQUIRES_CONDITION, e.getErrorCode());
        }

        // this works
        tr.setCondition(new Condition(Operator.LTE, "20.00001"));

        // too many args
        tr.setArguments(Arrays.asList("100.0", "lte", "bad-extra-arg"));
        try
        {
            TransformConfigVerifier.verify(tr);
            fail("exclude with bad arguments should throw a JobConfigurationException");
        }
        catch (JobConfigurationException e)
        {
            assertEquals(ErrorCodes.TRANSFORM_INVALID_ARGUMENT_COUNT, e.getErrorCode());
        }

        Condition cond = tr.getCondition();
        assertNotNull(cond);
        assertEquals(Operator.LTE, cond.getOperator());
        assertEquals("20.00001", cond.getValue());
    }

    @Test
    public void testVerify() throws JobConfigurationException
    {
        TransformConfig conf = new TransformConfig();
        conf.setTransform(TransformType.CONCAT.prettyName());
        conf.setInputs(Arrays.asList("a", "b", "c"));
        TransformConfigVerifier.verify(conf);

        conf = new TransformConfig();
        conf.setTransform(TransformType.DOMAIN_SPLIT.prettyName());
        conf.setInputs(Arrays.asList("dns"));
        assertTrue(TransformConfigVerifier.verify(conf));

        conf = new TransformConfig();
        conf.setTransform(TransformType.EXCLUDE.prettyName());
        conf.setInputs(Arrays.asList("f1"));
        conf.setCondition(new Condition(Operator.GTE, "100"));
        assertTrue(TransformConfigVerifier.verify(conf));
    }

    @Test
    public void testVerify_GivenNullInputs() throws JobConfigurationException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Transform type concat expected [2‥+∞) input(s), got 0");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.TRANSFORM_INVALID_INPUT_COUNT));

        TransformConfig conf = new TransformConfig();
        conf.setTransform(TransformType.CONCAT.prettyName());
        conf.setInputs(null);

        TransformConfigVerifier.verify(conf);
    }

    @Test
    public void testVerify_GivenEmptyInputs() throws JobConfigurationException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Transform type concat expected [2‥+∞) input(s), got 0");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.TRANSFORM_INVALID_INPUT_COUNT));

        TransformConfig conf = new TransformConfig();
        conf.setTransform(TransformType.CONCAT.prettyName());
        conf.setInputs(Arrays.asList());

        TransformConfigVerifier.verify(conf);
    }

    @Test
    public void testVerify_GivenInputsContainEmptyStrings() throws JobConfigurationException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Transform type concat contains empty input");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.TRANSFORM_INPUTS_CANNOT_BE_EMPTY_STRINGS));

        TransformConfig conf = new TransformConfig();
        conf.setTransform(TransformType.CONCAT.prettyName());
        conf.setInputs(Arrays.asList("", "foo"));

        TransformConfigVerifier.verify(conf);
    }

    @Test
    public void testVerify_GivenInputsContainWhitespaceOnlyStrings() throws JobConfigurationException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Transform type concat contains empty input");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.TRANSFORM_INPUTS_CANNOT_BE_EMPTY_STRINGS));

        TransformConfig conf = new TransformConfig();
        conf.setTransform(TransformType.CONCAT.prettyName());
        conf.setInputs(Arrays.asList("   ", "foo"));

        TransformConfigVerifier.verify(conf);
    }

    @Test
    public void testVerify_GivenInputsDoesNotMatchArrity() throws JobConfigurationException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Transform type domain_split expected 1 input(s), got 2");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.TRANSFORM_INVALID_INPUT_COUNT));

        TransformConfig conf = new TransformConfig();
        conf.setTransform(TransformType.DOMAIN_SPLIT.prettyName());
        conf.setInputs(Arrays.asList("foo", "bar"));
        TransformConfigVerifier.verify(conf);
    }

    @Test
    public void testVerify_GivenNoOptionalArguments() throws JobConfigurationException
    {
        TransformConfig conf = new TransformConfig();
        conf.setTransform(TransformType.CONCAT.prettyName());
        conf.setInputs(Arrays.asList("a", "b", "c"));

        assertTrue(TransformConfigVerifier.verify(conf));
    }

    @Test
    public void testVerify_GivenOneOptionalArgumentWhenOneIsSupported()
            throws JobConfigurationException
    {
        TransformConfig conf = new TransformConfig();
        conf.setTransform(TransformType.CONCAT.prettyName());
        conf.setInputs(Arrays.asList("a", "b", "c"));
        conf.setArguments(Arrays.asList("delimiter"));

        assertTrue(TransformConfigVerifier.verify(conf));
    }

    @Test
    public void testVerify_GivenTwoOptionalArgumentsWhenOneIsSupported()
            throws JobConfigurationException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage(
                "Transform type concat expected [0‥1] argument(s), got 2");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.TRANSFORM_INVALID_ARGUMENT_COUNT));

        TransformConfig conf = new TransformConfig();
        conf.setTransform(TransformType.CONCAT.prettyName());
        conf.setInputs(Arrays.asList("a", "b", "c"));
        conf.setArguments(Arrays.asList("delimiter", "invalidarg"));

        TransformConfigVerifier.verify(conf);
    }

    @Test
    public void testVerify_GivenNullOutputs() throws JobConfigurationException
    {
        TransformConfig conf = new TransformConfig();
        conf.setTransform(TransformType.LOWERCASE.prettyName());
        conf.setInputs(Arrays.asList("a"));
        conf.setOutputs(null);

        assertEquals(Arrays.asList("lowercase"), conf.getOutputs());
        TransformConfigVerifier.verify(conf);
    }

    @Test
    public void testVerify_GivenEmptyOutputs() throws JobConfigurationException
    {
        TransformConfig conf = new TransformConfig();
        conf.setTransform(TransformType.LOWERCASE.prettyName());
        conf.setInputs(Arrays.asList("a"));
        conf.setOutputs(Collections.emptyList());

        assertEquals(Arrays.asList("lowercase"), conf.getOutputs());
        assertTrue(TransformConfigVerifier.verify(conf));
    }

    @Test
    public void testVerify_GivenOutputsContainEmptyStrings() throws JobConfigurationException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Transform type concat contains empty output");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.TRANSFORM_OUTPUTS_CANNOT_BE_EMPTY_STRINGS));

        TransformConfig conf = new TransformConfig();
        conf.setTransform(TransformType.CONCAT.prettyName());
        conf.setInputs(Arrays.asList("input1", "input2"));
        conf.setOutputs(Arrays.asList(""));

        TransformConfigVerifier.verify(conf);
    }

    @Test
    public void testVerify_GivenOutputsContainWhitespaceOnlyStrings() throws JobConfigurationException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Transform type concat contains empty output");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.TRANSFORM_OUTPUTS_CANNOT_BE_EMPTY_STRINGS));

        TransformConfig conf = new TransformConfig();
        conf.setTransform(TransformType.CONCAT.prettyName());
        conf.setInputs(Arrays.asList("input1", "input2"));
        conf.setOutputs(Arrays.asList("   "));

        TransformConfigVerifier.verify(conf);
    }

    @Test
    public void testVerify_GivenTwoOutputsWhenOneIsExpected() throws JobConfigurationException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Transform type lowercase expected 1 output(s), got 2");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.TRANSFORM_INVALID_OUTPUT_COUNT));

        TransformConfig conf = new TransformConfig();
        conf.setTransform(TransformType.LOWERCASE.prettyName());
        conf.setInputs(Arrays.asList("a"));
        conf.setOutputs(Arrays.asList("one", "two"));

        assertTrue(TransformConfigVerifier.verify(conf));
    }
}
