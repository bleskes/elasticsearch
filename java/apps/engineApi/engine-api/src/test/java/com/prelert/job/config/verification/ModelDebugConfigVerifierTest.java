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
package com.prelert.job.config.verification;

import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.prelert.job.ModelDebugConfig;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobConfigurationException;

public class ModelDebugConfigVerifierTest
{
    @Rule public ExpectedException m_ExpectedException = ExpectedException.none();

    @Test
    public void testVerify_GivenBoundPercentileLessThanZero() throws JobConfigurationException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage(
                "Invalid modelDebugConfig: boundsPercentile must be in the range [0, 100]");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        ModelDebugConfigVerifier.verify(new ModelDebugConfig(-1.0, ""));
    }

    @Test
    public void testVerify_GivenBoundPercentileGreaterThan100() throws JobConfigurationException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage(
                "Invalid modelDebugConfig: boundsPercentile must be in the range [0, 100]");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        ModelDebugConfigVerifier.verify(new ModelDebugConfig(100.1, ""));
    }

    @Test
    public void testVerify_GivenValid() throws JobConfigurationException
    {
        assertTrue(ModelDebugConfigVerifier.verify(new ModelDebugConfig()));
        assertTrue(ModelDebugConfigVerifier.verify(new ModelDebugConfig(93.0, "")));
        assertTrue(ModelDebugConfigVerifier.verify(new ModelDebugConfig(93.0, "foo,bar")));
    }
}
