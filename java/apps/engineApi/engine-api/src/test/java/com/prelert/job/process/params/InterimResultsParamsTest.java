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

package com.prelert.job.process.params;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class InterimResultsParamsTest
{
    @Test
    public void testBuilder_GivenDefault()
    {
        InterimResultsParams params = InterimResultsParams.newBuilder().build();
        assertFalse(params.shouldCalculateInterim());
        assertFalse(params.shouldAdvanceTime());
        assertEquals("", params.getStart());
        assertEquals("", params.getEnd());
    }

    @Test
    public void testBuilder_GivenCalcInterim()
    {
        InterimResultsParams params = InterimResultsParams.newBuilder().calcInterim(true).build();
        assertTrue(params.shouldCalculateInterim());
        assertFalse(params.shouldAdvanceTime());
        assertEquals("", params.getStart());
        assertEquals("", params.getEnd());
    }

    @Test
    public void testBuilder_GivenCalcInterimAndStart()
    {
        InterimResultsParams params = InterimResultsParams.newBuilder().calcInterim(true)
                .forTimeRange(42L, null).build();
        assertTrue(params.shouldCalculateInterim());
        assertFalse(params.shouldAdvanceTime());
        assertEquals("42", params.getStart());
        assertEquals("", params.getEnd());
    }

    @Test
    public void testBuilder_GivenCalcInterimAndEnd()
    {
        InterimResultsParams params = InterimResultsParams.newBuilder().calcInterim(true)
                .forTimeRange(null, 100L).build();
        assertTrue(params.shouldCalculateInterim());
        assertFalse(params.shouldAdvanceTime());
        assertEquals("", params.getStart());
        assertEquals("100", params.getEnd());
    }

    @Test
    public void testBuilder_GivenCalcInterimAndStartAndEnd()
    {
        InterimResultsParams params = InterimResultsParams.newBuilder().calcInterim(true)
                .forTimeRange(3600L, 7200L).build();
        assertTrue(params.shouldCalculateInterim());
        assertFalse(params.shouldAdvanceTime());
        assertEquals("3600", params.getStart());
        assertEquals("7200", params.getEnd());
    }

    @Test
    public void testBuilder_GivenAdvanceTime()
    {
        InterimResultsParams params = InterimResultsParams.newBuilder().advanceTime(true).build();
        assertFalse(params.shouldCalculateInterim());
        assertTrue(params.shouldAdvanceTime());
        assertEquals("", params.getStart());
        assertEquals("", params.getEnd());
    }

    @Test
    public void testBuilder_GivenAdvanceTimeWithTargetTime()
    {
        InterimResultsParams params = InterimResultsParams.newBuilder().advanceTime(1821L).build();
        assertFalse(params.shouldCalculateInterim());
        assertTrue(params.shouldAdvanceTime());
        assertEquals("", params.getStart());
        assertEquals("1821", params.getEnd());
    }

    @Test
    public void testBuilder_GivenCalcInterimAndAdvanceTimeWithTargetTime()
    {
        InterimResultsParams params = InterimResultsParams.newBuilder().calcInterim(true)
                .advanceTime(1940L).build();
        assertTrue(params.shouldCalculateInterim());
        assertTrue(params.shouldAdvanceTime());
        assertEquals("", params.getStart());
        assertEquals("1940", params.getEnd());
    }
}
