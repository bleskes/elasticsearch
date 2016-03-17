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

import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.prelert.job.AnalysisLimits;
import com.prelert.job.config.verification.AnalysisLimitsVerifier;
import com.prelert.job.config.verification.JobConfigurationException;
import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;

public class AnalysisLimitsVerifierTest
{

    @Rule
    public ExpectedException m_ExpectedException = ExpectedException.none();

    @Test
    public void testVerify_GivenNegativeCategorizationExamplesLimit()
            throws JobConfigurationException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage(
                "categorizationExamplesLimit cannot be less than 0. Value = -1");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.INVALID_VALUE));

        AnalysisLimits limits = new AnalysisLimits(1L, -1L);

        System.out.print(limits.hashCode());

        AnalysisLimitsVerifier.verify(limits);
    }

    @Test
    public void testVerify_GivenValid() throws JobConfigurationException
    {
        AnalysisLimits limits = new AnalysisLimits(0L, 0L);
        assertTrue(AnalysisLimitsVerifier.verify(limits));
        limits = new AnalysisLimits(1L, null);
        assertTrue(AnalysisLimitsVerifier.verify(limits));
        limits = new AnalysisLimits(1L, 1L);
        assertTrue(AnalysisLimitsVerifier.verify(limits));
    }
}
