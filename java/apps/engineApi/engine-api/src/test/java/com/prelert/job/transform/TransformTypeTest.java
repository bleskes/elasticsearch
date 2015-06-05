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
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.prelert.job.transform.condition.Condition;
import com.prelert.job.transform.condition.Operator;
import com.prelert.job.transform.exceptions.TransformConfigurationException;
import com.prelert.rs.data.ErrorCode;
import com.prelert.rs.data.ErrorCodeMatcher;

public class TransformTypeTest
{
    @Rule public ExpectedException m_ExpectedException = ExpectedException.none();

    @Test
    public void testFromString() throws TransformConfigurationException
    {
        Set<TransformType> all = EnumSet.allOf(TransformType.class);

        for (TransformType type : all)
        {
            assertEquals(type.prettyName(), type.toString());

            TransformType created = TransformType.fromString(type.prettyName());
            assertEquals(type, created);
        }
    }

    @Test(expected=TransformConfigurationException.class)
    public void testFromString_UnknownType()
    throws TransformConfigurationException
    {
        @SuppressWarnings("unused")
        TransformType created = TransformType.fromString("random_type");
    }

    @Test
    public void testVerify() throws TransformConfigurationException
    {
        TransformConfig conf = new TransformConfig();
        conf.setTransform(TransformType.CONCAT.prettyName());
        conf.setInputs(Arrays.asList("a", "b", "c"));
        assertTrue(TransformType.CONCAT.verify(conf));

        conf = new TransformConfig();
        conf.setTransform(TransformType.DOMAIN_SPLIT.prettyName());
        conf.setInputs(Arrays.asList("dns"));
        assertTrue(TransformType.DOMAIN_SPLIT.verify(conf));

        conf = new TransformConfig();
        conf.setTransform(TransformType.EXCLUDE.prettyName());
        conf.setInputs(Arrays.asList("f1"));
        conf.setCondition(new Condition(Operator.GTE, "100"));
        assertTrue(TransformType.EXCLUDE.verify(conf));
    }

    @Test
    public void testVerify_GivenNullInputs() throws TransformConfigurationException
    {
        m_ExpectedException.expect(TransformConfigurationException.class);
        m_ExpectedException.expectMessage("Function arity error: no inputs defined");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCode.INCORRECT_TRANSFORM_INPUT_COUNT));

        TransformConfig conf = new TransformConfig();
        conf.setTransform(TransformType.CONCAT.prettyName());
        conf.setInputs(null);

        TransformType.CONCAT.verify(conf);
    }

    @Test
    public void testVerify_GivenEmptyInputs() throws TransformConfigurationException
    {
        m_ExpectedException.expect(TransformConfigurationException.class);
        m_ExpectedException.expectMessage(
                "Function arity error: expected at least one argument, got 0");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCode.INCORRECT_TRANSFORM_INPUT_COUNT));

        TransformConfig conf = new TransformConfig();
        conf.setTransform(TransformType.CONCAT.prettyName());
        conf.setInputs(Arrays.asList());

        TransformType.CONCAT.verify(conf);

    }

    @Test
    public void testVerify_GivenInputsDoesNotMatchArrity() throws TransformConfigurationException
    {
        m_ExpectedException.expect(TransformConfigurationException.class);
        m_ExpectedException.expectMessage("Function arity error: expected 1 arguments, got 2");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCode.INCORRECT_TRANSFORM_INPUT_COUNT));

        TransformConfig conf = new TransformConfig();
        conf.setTransform(TransformType.DOMAIN_SPLIT.prettyName());
        conf.setInputs(Arrays.asList("foo", "bar"));
        TransformType.DOMAIN_SPLIT.verify(conf);
    }

    @Test
    public void testVerify_GivenNoOptionalArguments() throws TransformConfigurationException
    {
        TransformConfig conf = new TransformConfig();
        conf.setTransform(TransformType.CONCAT.prettyName());
        conf.setInputs(Arrays.asList("a", "b", "c"));

        assertTrue(TransformType.CONCAT.verify(conf));
    }

    @Test
    public void testVerify_GivenOneOptionalArgumentWhenOneIsSupported()
            throws TransformConfigurationException
    {
        TransformConfig conf = new TransformConfig();
        conf.setTransform(TransformType.CONCAT.prettyName());
        conf.setInputs(Arrays.asList("a", "b", "c"));
        conf.setArguments(Arrays.asList("delimiter"));

        assertTrue(TransformType.CONCAT.verify(conf));
    }

    @Test
    public void testVerify_GivenTwoOptionalArgumentsWhenOneIsSupported()
            throws TransformConfigurationException
    {
        m_ExpectedException.expect(TransformConfigurationException.class);
        m_ExpectedException.expectMessage(
                "Transform type concat must be defined with at most 1 arguments");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCode.TRANSFORM_INVALID_ARGUMENT_COUNT));

        TransformConfig conf = new TransformConfig();
        conf.setTransform(TransformType.CONCAT.prettyName());
        conf.setInputs(Arrays.asList("a", "b", "c"));
        conf.setArguments(Arrays.asList("delimiter", "invalidarg"));

        TransformType.CONCAT.verify(conf);
    }
}
