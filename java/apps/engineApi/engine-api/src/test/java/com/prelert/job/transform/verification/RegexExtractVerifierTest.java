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

import java.util.Arrays;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.prelert.job.config.verification.JobConfigurationException;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.transform.TransformConfig;

public class RegexExtractVerifierTest
{
    @Rule
    public ExpectedException m_ExpectedException = ExpectedException.none();

    private final TransformConfig m_TransformConfig = new TransformConfig();

    @Before
    public void setUp()
    {
        m_TransformConfig.setTransform("extract");
        m_TransformConfig.setInputs(Arrays.asList("foo"));
        m_TransformConfig.setArguments(Arrays.asList("Foo ([0-9]+)"));
        m_TransformConfig.setOutputs(Arrays.asList("o1"));
    }

    @Test
    public void testVerify_GivenValidRegex() throws JobConfigurationException
    {
        new RegexExtractVerifier().verify("Foo ([0-9]+)", m_TransformConfig);
    }

    @Test
    public void testVerify_GivenInvalidRegex() throws JobConfigurationException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Transform 'extract' has invalid argument '[+'");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.TRANSFORM_INVALID_ARGUMENT));

        new RegexExtractVerifier().verify("[+", m_TransformConfig);
    }

    @Test
    public void testVerify_GivenTwoOutputsButOneGroup() throws JobConfigurationException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Transform 'extract' expects 2 output(s) but regex 'Foo ([0-9]+)' captures 1 group(s)");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.TRANSFORM_INVALID_ARGUMENT));

        m_TransformConfig.setOutputs(Arrays.asList("o1", "o2"));

        new RegexExtractVerifier().verify("Foo ([0-9]+)", m_TransformConfig);
    }

    @Test
    public void testVerify_GivenOneOutputButTwoGroups() throws JobConfigurationException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage(
                "Transform 'extract' expects 1 output(s) but regex 'Foo ([0-9]+) ([0-9]+)' captures 2 group(s)");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.TRANSFORM_INVALID_ARGUMENT));

        m_TransformConfig.setOutputs(Arrays.asList("o1"));

        new RegexExtractVerifier().verify("Foo ([0-9]+) ([0-9]+)", m_TransformConfig);
    }

    @Test
    public void testVerify_GivenTwoOutputsAndTwoGroups() throws JobConfigurationException
    {
        m_TransformConfig.setOutputs(Arrays.asList("o1", "o2"));

        new RegexExtractVerifier().verify("Foo ([0-9]+) ([0-9]+)", m_TransformConfig);
    }
}
