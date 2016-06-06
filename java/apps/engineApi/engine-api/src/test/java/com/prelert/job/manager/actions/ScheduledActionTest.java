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

package com.prelert.job.manager.actions;

import static org.junit.Assert.*;

import org.junit.Test;

public class ScheduledActionTest
{
    @Test
    public void testTypename()
    {
        assertEquals("ScheduledAction", ScheduledAction.STOP.typename());
    }

    @Test
    public void testgetBusyActionError_GivenVariousActionsInUse()
    {
        assertEquals("Cannot start scheduler for job 'foo' while its status is started on host marple",
                ScheduledAction.START.getBusyActionError("foo", ScheduledAction.START, "marple"));

        String g = ScheduledAction.STOP.getBusyActionError("foo", ScheduledAction.START, "marple");
        assertEquals("Cannot stop scheduler for job 'foo' while its status is started on host marple", g);
    }

    @Test
    public void testIsValidTransition()
    {
        assertTrue(ScheduledAction.START.isValidTransition(ScheduledAction.STOP));
        assertTrue(ScheduledAction.START.isValidTransition(ScheduledAction.START));
        assertTrue(ScheduledAction.STOP.isValidTransition(ScheduledAction.START));
        assertTrue(ScheduledAction.STOP.isValidTransition(ScheduledAction.STOP));
    }

    @Test
    public void testNextState()
    {
        assertEquals(ScheduledAction.STOP, ScheduledAction.START.nextState(ScheduledAction.STOP));
        assertEquals(ScheduledAction.STOP, ScheduledAction.START.nextState(ScheduledAction.START));
        assertEquals(ScheduledAction.STOP, ScheduledAction.STOP.nextState(ScheduledAction.START));
        assertEquals(ScheduledAction.STOP, ScheduledAction.STOP.nextState(ScheduledAction.STOP));
    }

}
