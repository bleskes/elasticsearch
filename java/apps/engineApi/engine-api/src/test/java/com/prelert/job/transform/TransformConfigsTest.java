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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;

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
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.TRANSFORM_HAS_CIRCULAR_DEPENDENCY));

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
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.DUPLICATED_TRANSFORM_OUTPUT_NAME));

        new TransformConfigs(transforms).verify();
    }

    @Test
    public void testVerify_GivenNullInputs() throws TransformConfigurationException
    {
        m_ExpectedException.expect(TransformConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.TRANSFORM_INVALID_INPUT_COUNT));

        List<TransformConfig> transforms = new ArrayList<>();
        transforms.add(createConcatTransform(null, Arrays.asList()));

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

    @Test
    public void testFindDependencies_CatchCircularDependency()
    {
        List<TransformConfig> transforms = new ArrayList<>();

        List<String> inputs = Arrays.asList("ina", "ab");
        List<String> outputs = Arrays.asList("ab");
        TransformConfig concat = createConcatTransform(inputs, outputs);
        transforms.add(concat);

        int chainIndex = TransformConfigs.checkForCircularDependencies(transforms);
        assertEquals(chainIndex, 0);
    }

    @Test
    public void testCheckForCircularDependencies_CatchCircularDependency()
    {
        List<TransformConfig> transforms = new ArrayList<>();

        TransformConfig concat = createConcatTransform(Arrays.asList("ina", "abc"),
                                                        Arrays.asList("ab"));
        transforms.add(concat);

        TransformConfig concat2 = createConcatTransform(Arrays.asList("ab", "ac"),
                                                        Arrays.asList("abc"));
        transforms.add(concat2);

        TransformConfig concat3 = createConcatTransform(Arrays.asList("ind", "ine"),
                                                        Arrays.asList("de"));
        transforms.add(concat3);

        int chainIndex = TransformConfigs.checkForCircularDependencies(transforms);
        assertEquals(chainIndex, 0);
    }

    @Test
    public void testCheckForCircularDependencies_CatchCircularDependencyTransitive()
    {
        List<TransformConfig> transforms = new ArrayList<>();

        TransformConfig concatNoChain = createConcatTransform(Arrays.asList("ind", "ine"),
                                                        Arrays.asList("de"));
        transforms.add(concatNoChain);

        TransformConfig concat = createConcatTransform(Arrays.asList("ina", "abcd"),
                                                        Arrays.asList("ab"));
        transforms.add(concat);

        TransformConfig concat1 = createConcatTransform(Arrays.asList("ab", "inc"),
                                                        Arrays.asList("abc"));
        transforms.add(concat1);

        TransformConfig concat2 = createConcatTransform(Arrays.asList("abc", "ind"),
                                                        Arrays.asList("abcd"));
        transforms.add(concat2);

        int chainIndex = TransformConfigs.checkForCircularDependencies(transforms);
        assertEquals(chainIndex, 1);
    }

    @Test
    public void testCheckForCircularDependencies_NoCircles()
    {
        List<TransformConfig> transforms = new ArrayList<>();

        TransformConfig concat = createConcatTransform(Arrays.asList("ina", "inb"),
                                                            Arrays.asList("ab"));
        transforms.add(concat);

        int chainIndex = TransformConfigs.checkForCircularDependencies(transforms);
        assertEquals(chainIndex, -1);

        // add more transforms
        TransformConfig concat1 = createConcatTransform(Arrays.asList("inc", "ind"),
                                                        Arrays.asList("cd"));
        transforms.add(concat1);

        chainIndex = TransformConfigs.checkForCircularDependencies(transforms);
        assertEquals(chainIndex, -1);


        TransformConfig hrd1 = createHrdTransform(Arrays.asList("ab"),
                Arrays.asList());
        transforms.add(hrd1);

        TransformConfig concat3 = createConcatTransform(Arrays.asList("hrd", "cd"),
                Arrays.asList());
        transforms.add(concat3);

        chainIndex = TransformConfigs.checkForCircularDependencies(transforms);
        assertEquals(chainIndex, -1);
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
        concat.setTransform(TransformType.DOMAIN_SPLIT.prettyName());
        concat.setInputs(inputs);
        concat.setOutputs(outputs);
        return concat;
    }

}
