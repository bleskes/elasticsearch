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

package com.prelert.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

public class AnalysisLimitsTest
{
    @Test
    public void testSetModelMemoryLimit_GivenNegative()
    {
        AnalysisLimits limits = new AnalysisLimits();

        limits.setModelMemoryLimit(-42);

        assertEquals(-1, limits.getModelMemoryLimit());
    }

    @Test
    public void testSetModelMemoryLimit_GivenZero()
    {
        AnalysisLimits limits = new AnalysisLimits();

        limits.setModelMemoryLimit(0);

        assertEquals(0, limits.getModelMemoryLimit());
    }

    @Test
    public void testSetModelMemoryLimit_GivenPositive()
    {
        AnalysisLimits limits = new AnalysisLimits();

        limits.setModelMemoryLimit(52);

        assertEquals(52, limits.getModelMemoryLimit());
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

    @Test
    public void testToMap_GivenDefault()
    {
        AnalysisLimits defaultLimits = new AnalysisLimits();

        Map<String, Object> map = defaultLimits.toMap();

        assertEquals(1, map.size());
        assertEquals(0L, map.get(AnalysisLimits.MODEL_MEMORY_LIMIT));
    }

    @Test
    public void testToMap_GivenFullyPopulated()
    {
        AnalysisLimits limits = new AnalysisLimits();
        limits.setCategorizationExamplesLimit(5L);
        limits.setModelMemoryLimit(1000L);

        Map<String, Object> map = limits.toMap();

        assertEquals(2, map.size());
        assertEquals(1000L, map.get(AnalysisLimits.MODEL_MEMORY_LIMIT));
        assertEquals(5L, map.get(AnalysisLimits.CATEGORIZATION_EXAMPLES_LIMIT));
    }
}
