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

import org.junit.Test;

public class SchedulerStateTest
{
    @Test
    public void testEquals_GivenDifferentClass()
    {
        assertFalse(new SchedulerState().equals("a string"));
    }

    @Test
    public void testEquals_GivenSameReference()
    {
        SchedulerState schedulerState = new SchedulerState();
        assertTrue(schedulerState.equals(schedulerState));
    }

    @Test
    public void testEquals_GivenEqualObjects()
    {
        SchedulerState schedulerState1 = new SchedulerState();
        schedulerState1.setStartTimeMillis(18L);
        schedulerState1.setEndTimeMillis(42L);

        SchedulerState schedulerState2 = new SchedulerState(18L, 42L);

        assertTrue(schedulerState1.equals(schedulerState2));
        assertTrue(schedulerState2.equals(schedulerState1));
        assertEquals(schedulerState1.hashCode(), schedulerState2.hashCode());
    }
}
