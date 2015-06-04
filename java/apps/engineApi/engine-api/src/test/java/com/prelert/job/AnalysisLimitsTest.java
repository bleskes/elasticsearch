/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.prelert.job.exceptions.JobConfigurationException;
import com.prelert.rs.data.ErrorCode;
import com.prelert.rs.data.ErrorCodeMatcher;

public class AnalysisLimitsTest
{
    @Rule
    public ExpectedException m_ExpectedException = ExpectedException.none();

    @Test
    public void testVerify_GivenNegativeModeLMemoryLimit() throws JobConfigurationException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("modelMemoryLimit cannot be < 0. Value = -1");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCode.INVALID_VALUE));

        AnalysisLimits limits = new AnalysisLimits();
        limits.setModelMemoryLimit(-1L);

        limits.verify();
    }

    @Test
    public void testVerify_GivenNegativeCategorizationExamplesLimit()
            throws JobConfigurationException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage(
                "categorizationExamplesLimit cannot be < 0. Value = -1");
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCode.INVALID_VALUE));

        AnalysisLimits limits = new AnalysisLimits(1L, -1L);

        limits.verify();
    }

    @Test
    public void testVerify_GivenValid() throws JobConfigurationException
    {
        AnalysisLimits limits = new AnalysisLimits(0L, 0L);
        assertTrue(limits.verify());
        limits = new AnalysisLimits(1L, null);
        assertTrue(limits.verify());
        limits = new AnalysisLimits(1L, 1L);
        assertTrue(limits.verify());
    }

    @Test
    public void testEquals_GivenEqual()
    {
        AnalysisLimits analysisLimits1 = new AnalysisLimits(10, 20L);
        AnalysisLimits analysisLimits2 = new AnalysisLimits(10, 20L);

        assertTrue(analysisLimits1.equals(analysisLimits1));
        assertTrue(analysisLimits1.equals(analysisLimits2));
        assertTrue(analysisLimits2.equals(analysisLimits1));
    }

    @Test
    public void testEquals_GivenDifferentModelMemoryLimit()
    {
        AnalysisLimits analysisLimits1 = new AnalysisLimits(10, 20L);
        AnalysisLimits analysisLimits2 = new AnalysisLimits(11, 20L);

        assertFalse(analysisLimits1.equals(analysisLimits2));
        assertFalse(analysisLimits2.equals(analysisLimits1));
    }

    @Test
    public void testEquals_GivenDifferentCategorizationExamplesLimit()
    {
        AnalysisLimits analysisLimits1 = new AnalysisLimits(10, 20L);
        AnalysisLimits analysisLimits2 = new AnalysisLimits(10, 21L);

        assertFalse(analysisLimits1.equals(analysisLimits2));
        assertFalse(analysisLimits2.equals(analysisLimits1));
    }

    @Test
    public void testHashCode_GivenEqual()
    {
        AnalysisLimits analysisLimits1 = new AnalysisLimits(5555, 3L);
        AnalysisLimits analysisLimits2 = new AnalysisLimits(5555, 3L);

        assertEquals(analysisLimits1.hashCode(), analysisLimits2.hashCode());
    }
}
