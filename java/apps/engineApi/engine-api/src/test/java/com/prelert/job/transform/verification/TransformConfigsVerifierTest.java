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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.prelert.job.config.verification.JobConfigurationException;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.transform.TransformConfig;
import com.prelert.job.transform.TransformType;

public class TransformConfigsVerifierTest
{
    @Rule
    public ExpectedException m_ExpectedException = ExpectedException.none();

    @Test
    public void testVerify_HasCircularDependency() throws JobConfigurationException
    {
        List<TransformConfig> transforms = new ArrayList<>();
        transforms.add(createHrdTransform(Arrays.asList("dns"), Arrays.asList("subdomain", "hrd")));
        transforms.add(createHrdTransform(Arrays.asList("hrd"), Arrays.asList("dns")));

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.TRANSFORM_HAS_CIRCULAR_DEPENDENCY));

        TransformConfigsVerifier.verify(transforms);
    }

    @Test
    public void testVerify_DuplicateOutputs() throws JobConfigurationException
    {
        List<TransformConfig> transforms = new ArrayList<>();
        transforms.add(createConcatTransform(Arrays.asList("a", "c"), Arrays.asList()));
        transforms.add(createConcatTransform(Arrays.asList("b", "c"), Arrays.asList()));

        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.DUPLICATED_TRANSFORM_OUTPUT_NAME));

        TransformConfigsVerifier.verify(transforms);
    }

    @Test
    public void testVerify_GivenNullInputs() throws JobConfigurationException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.TRANSFORM_INVALID_INPUT_COUNT));

        List<TransformConfig> transforms = new ArrayList<>();
        transforms.add(createConcatTransform(null, Arrays.asList()));

        TransformConfigsVerifier.verify(transforms);
    }

    @Test
    public void testVerify_Ok() throws JobConfigurationException
    {
        List<TransformConfig> transforms = new ArrayList<>();
        transforms.add(createHrdTransform(Arrays.asList("dns"), Arrays.asList("subdomain", "hrd")));
        transforms.add(createConcatTransform(Arrays.asList("a", "c"), Arrays.asList()));

        assertTrue(TransformConfigsVerifier.verify(transforms));
    }

    @Test
    public void testFindDependencies_CatchCircularDependency()
    {
        List<TransformConfig> transforms = new ArrayList<>();

        List<String> inputs = Arrays.asList("ina", "ab");
        List<String> outputs = Arrays.asList("ab");
        TransformConfig concat = createConcatTransform(inputs, outputs);
        transforms.add(concat);

        int chainIndex = TransformConfigsVerifier.checkForCircularDependencies(transforms);
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

        int chainIndex = TransformConfigsVerifier.checkForCircularDependencies(transforms);
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

        int chainIndex = TransformConfigsVerifier.checkForCircularDependencies(transforms);
        assertEquals(chainIndex, 1);
    }

    @Test
    public void testCheckForCircularDependencies_NoCircles()
    {
        List<TransformConfig> transforms = new ArrayList<>();

        TransformConfig concat = createConcatTransform(Arrays.asList("ina", "inb"),
                                                            Arrays.asList("ab"));
        transforms.add(concat);

        int chainIndex = TransformConfigsVerifier.checkForCircularDependencies(transforms);
        assertEquals(chainIndex, -1);

        // add more transforms
        TransformConfig concat1 = createConcatTransform(Arrays.asList("inc", "ind"),
                                                        Arrays.asList("cd"));
        transforms.add(concat1);

        chainIndex = TransformConfigsVerifier.checkForCircularDependencies(transforms);
        assertEquals(chainIndex, -1);


        TransformConfig hrd1 = createHrdTransform(Arrays.asList("ab"),
                Arrays.asList());
        transforms.add(hrd1);

        TransformConfig concat3 = createConcatTransform(Arrays.asList("hrd", "cd"),
                Arrays.asList());
        transforms.add(concat3);

        chainIndex = TransformConfigsVerifier.checkForCircularDependencies(transforms);
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
