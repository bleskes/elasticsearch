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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.prelert.job.errorcodes.ErrorCodes;

public class ScheduledActionTest
{
    @Test
    public void testTypename()
    {
        assertEquals("ScheduledAction", ScheduledAction.STOP.typename());
    }

    @Test
    public void testGetBusyActionError_GivenVariousActionsInUse()
    {
        assertEquals("Cannot start scheduler for job 'foo' while its status is started",
                ScheduledAction.START.getBusyActionError("foo", ScheduledAction.START));

        String g = ScheduledAction.STOP.getBusyActionError("foo", ScheduledAction.START);
        assertEquals("Cannot stop scheduler for job 'foo' while its status is started", g);
    }

    @Test
    public void testGetBusyActionError_GivenHostAndVariousActionsInUse()
    {
        assertEquals("Cannot start scheduler for job 'foo' while its status is started on host marple",
                ScheduledAction.START.getBusyActionError("foo", ScheduledAction.START, "marple"));

        String msg = ScheduledAction.STOP.getBusyActionError("foo", ScheduledAction.START, "marple");
        assertEquals("Cannot stop scheduler for job 'foo' while its status is started on host marple", msg);

        msg = ScheduledAction.UPDATE.getBusyActionError("foo", ScheduledAction.START, "marple");
        assertEquals("Cannot update scheduler for job 'foo' while its status is started on host marple", msg);

        msg = ScheduledAction.DELETE.getBusyActionError("foo", ScheduledAction.START, "marple");
        assertEquals("Cannot delete scheduler for job 'foo' while its status is started on host marple", msg);
    }

    @Test
    public void testIsValidTransition()
    {
        assertTrue(ScheduledAction.START.isValidTransition(ScheduledAction.STOP));
        assertTrue(ScheduledAction.STOP.isValidTransition(ScheduledAction.START));
        assertTrue(ScheduledAction.STOP.isValidTransition(ScheduledAction.UPDATE));
        assertTrue(ScheduledAction.STOP.isValidTransition(ScheduledAction.STOP));
        assertTrue(ScheduledAction.STOP.isValidTransition(ScheduledAction.DELETE));

        assertFalse(ScheduledAction.START.isValidTransition(ScheduledAction.START));
        assertFalse(ScheduledAction.START.isValidTransition(ScheduledAction.UPDATE));
        assertFalse(ScheduledAction.DELETE.isValidTransition(ScheduledAction.START));
        assertFalse(ScheduledAction.UPDATE.isValidTransition(ScheduledAction.START));
        assertFalse(ScheduledAction.UPDATE.isValidTransition(ScheduledAction.UPDATE));
    }

    @Test
    public void testNextState()
    {
        assertEquals(ScheduledAction.STOP, ScheduledAction.START.nextState(ScheduledAction.STOP));
        assertEquals(ScheduledAction.STOP, ScheduledAction.START.nextState(ScheduledAction.START));
        assertEquals(ScheduledAction.STOP, ScheduledAction.STOP.nextState(ScheduledAction.START));
        assertEquals(ScheduledAction.STOP, ScheduledAction.STOP.nextState(ScheduledAction.STOP));
        assertEquals(ScheduledAction.STOP, ScheduledAction.UPDATE.nextState(ScheduledAction.STOP));
        assertEquals(ScheduledAction.STOP, ScheduledAction.UPDATE.nextState(ScheduledAction.DELETE));
        assertEquals(ScheduledAction.STOP, ScheduledAction.DELETE.nextState(ScheduledAction.STOP));
        assertEquals(ScheduledAction.STOP, ScheduledAction.DELETE.nextState(ScheduledAction.START));
    }

    @Test
    public void testHoldDistributedLock()
    {
        assertTrue(ScheduledAction.START.holdDistributedLock());
        assertFalse(ScheduledAction.STOP.holdDistributedLock());
        assertFalse(ScheduledAction.UPDATE.holdDistributedLock());
        assertFalse(ScheduledAction.DELETE.holdDistributedLock());
    }

    @Test
    public void testStartingState()
    {
        assertEquals(ScheduledAction.STOP, ScheduledAction.startingState());
    }

    @Test
    public void testErrorCode()
    {
        assertEquals(ErrorCodes.CANNOT_START_JOB_SCHEDULER, ScheduledAction.START.getErrorCode());
        assertEquals(ErrorCodes.CANNOT_STOP_JOB_SCHEDULER, ScheduledAction.STOP.getErrorCode());
        assertEquals(ErrorCodes.CANNOT_UPDATE_JOB_SCHEDULER, ScheduledAction.UPDATE.getErrorCode());
        assertEquals(ErrorCodes.CANNOT_DELETE_JOB_SCHEDULER, ScheduledAction.DELETE.getErrorCode());
    }
}
