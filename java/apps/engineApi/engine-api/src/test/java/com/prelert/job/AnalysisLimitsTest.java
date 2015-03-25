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
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.prelert.job.exceptions.JobConfigurationException;

public class AnalysisLimitsTest
{
    @Rule
    public ExpectedException m_ExpectedException = ExpectedException.none();

    @Test
    public void testVerify_GivenNegativeModeLMemoryLimit() throws JobConfigurationException
    {
        m_ExpectedException.expect(JobConfigurationException.class);
        m_ExpectedException.expectMessage("Invalid Analysis limit modelMemoryLimit must be >= 0");

        AnalysisLimits limits = new AnalysisLimits();
        limits.setModelMemoryLimit(-1L);

        limits.verify();
    }

    @Test
    public void testVerify_GivenValidModeLMemoryLimit() throws JobConfigurationException
    {
        AnalysisLimits limits = new AnalysisLimits();
        limits.setModelMemoryLimit(0L);
        assertTrue(limits.verify());
        limits.setModelMemoryLimit(1L);
        assertTrue(limits.verify());
    }

    @Test
    public void testEquals_GivenEqual()
    {
        AnalysisLimits analysisLimits1 = new AnalysisLimits(10);
        AnalysisLimits analysisLimits2 = new AnalysisLimits(10);

        assertTrue(analysisLimits1.equals(analysisLimits1));
        assertTrue(analysisLimits1.equals(analysisLimits2));
        assertTrue(analysisLimits2.equals(analysisLimits1));
    }

    @Test
    public void testHashCode_GivenEqual()
    {
        AnalysisLimits analysisLimits1 = new AnalysisLimits(5555);
        AnalysisLimits analysisLimits2 = new AnalysisLimits(5555);

        assertEquals(analysisLimits1.hashCode(), analysisLimits2.hashCode());
    }
}
