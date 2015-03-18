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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.prelert.rs.data.ErrorCode;

public class TransformConfigsTest
{
    @Rule
    public ExpectedException m_ExpectedException = ExpectedException.none();

    @Test
    public void test_Input_Output_FieldNames()
    {
        List<TransformConfig> transforms = new ArrayList<>();
        transforms.add(createConcatTransform(Arrays.asList("a", "b", "c"), Arrays.asList("c1")));
        transforms.add(createConcatTransform(Arrays.asList("d", "e", "c"), Arrays.asList("c2")));
        transforms.add(createConcatTransform(Arrays.asList("f", "a", "c"), Arrays.asList("c3")));

        TransformConfigs tcs = new TransformConfigs(transforms);

        List<String> inputNames = Arrays.asList("a", "b", "c", "d", "e", "f");
        Set<String> inputSet = new HashSet<>(inputNames);
        assertEquals(inputSet, tcs.inputFieldNames());

        List<String> outputNames = Arrays.asList("c1", "c2", "c3");
        Set<String> outputSet = new HashSet<>(outputNames);
        assertEquals(outputSet, tcs.outputFieldNames());
    }

    @Test
    public void testVerify_HasCircularDependency()
    throws TransformConfigurationException
    {
        List<TransformConfig> transforms = new ArrayList<>();
        transforms.add(createHrdTransform(Arrays.asList("dns"), Arrays.asList("subdomain", "hrd")));
        transforms.add(createHrdTransform(Arrays.asList("hrd"), Arrays.asList("dns")));

        m_ExpectedException.expect(TransformConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCode.TRANSFORM_HAS_CIRCULAR_DEPENDENCY));

        new TransformConfigs(transforms).verify();
    }

    @Test
    public void testVerify_DuplicateOutputs()
    throws TransformConfigurationException
    {
        List<TransformConfig> transforms = new ArrayList<>();
        transforms.add(createConcatTransform(Arrays.asList("a", "c"), Arrays.asList()));
        transforms.add(createConcatTransform(Arrays.asList("b", "c"), Arrays.asList()));

        m_ExpectedException.expect(TransformConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCode.DUPLICATED_TRANSFORM_OUTPUT_NAME));

        new TransformConfigs(transforms).verify();
    }


    @Test
    public void testVerify_Ok()
    throws TransformConfigurationException
    {
        List<TransformConfig> transforms = new ArrayList<>();
        transforms.add(createHrdTransform(Arrays.asList("dns"), Arrays.asList("subdomain", "hrd")));
        transforms.add(createConcatTransform(Arrays.asList("a", "c"), Arrays.asList()));

        assertTrue(new TransformConfigs(transforms).verify());
    }


    private TransformConfig createConcatTransform(List<String> inputs, List<String> outputs)
    {
        TransformConfig concat = new TransformConfig();
        concat.setTransform(TransformType.CONCAT.prettyName());
        concat.setInputs(inputs);
        concat.setOutputs(outputs);
        return concat;
    }

    private TransformConfig createHrdTransform(List<String> inputs, List<String> outputs)
    {
        TransformConfig concat = new TransformConfig();
        concat.setTransform(TransformType.DOMAIN_LOOKUP.prettyName());
        concat.setInputs(inputs);
        concat.setOutputs(outputs);
        return concat;
    }

}
