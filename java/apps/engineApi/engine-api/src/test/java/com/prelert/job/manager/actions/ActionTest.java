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

import com.prelert.job.manager.actions.Action;

public class ActionTest
{
    @Test
    public void testgetBusyActionError_GivenVariousActionsInUse()
    {
        assertEquals("Cannot close job foo while another connection is closing the job",
                Action.CLOSING.getBusyActionError("foo", Action.CLOSING));
        assertEquals("Cannot close job foo while another connection is deleting the job",
                Action.CLOSING.getBusyActionError("foo", Action.DELETING));
        assertEquals("Cannot close job bar while another connection is flushing the job",
                Action.CLOSING.getBusyActionError("bar", Action.FLUSHING));
        assertEquals("Cannot close job bar while another connection is pausing the job",
                Action.CLOSING.getBusyActionError("bar", Action.PAUSING));
        assertEquals("Cannot close job bar while another connection is resuming the job",
                Action.CLOSING.getBusyActionError("bar", Action.RESUMING));
        assertEquals("Cannot close job bar while another connection is reverting the model snapshot for the job",
                Action.CLOSING.getBusyActionError("bar", Action.REVERTING));
        assertEquals("Cannot close job foo while another connection is updating the job",
                Action.CLOSING.getBusyActionError("foo", Action.UPDATING));
        assertEquals("Cannot close job foo while another connection is writing to the job",
                Action.CLOSING.getBusyActionError("foo", Action.WRITING));
    }

    @Test
    public void testgetBusyActionError_GivenVariousActions()
    {
        assertEquals("Cannot close job foo while another connection is flushing the job",
                Action.CLOSING.getBusyActionError("foo", Action.FLUSHING));
        assertEquals("Cannot delete job foo while another connection is flushing the job",
                Action.DELETING.getBusyActionError("foo", Action.FLUSHING));
        assertEquals("Cannot flush job bar while another connection is flushing the job",
                Action.FLUSHING.getBusyActionError("bar", Action.FLUSHING));
        assertEquals("Cannot pause job foo while another connection is flushing the job",
                Action.PAUSING.getBusyActionError("foo", Action.FLUSHING));
        assertEquals("Cannot resume job foo while another connection is flushing the job",
                Action.RESUMING.getBusyActionError("foo", Action.FLUSHING));
        assertEquals("Cannot revert model snapshot for job foo while another connection is flushing the job",
                Action.REVERTING.getBusyActionError("foo", Action.FLUSHING));
        assertEquals("Cannot update job foo while another connection is flushing the job",
                Action.UPDATING.getBusyActionError("foo", Action.FLUSHING));
        assertEquals("Cannot write to job foo while another connection is flushing the job",
                Action.WRITING.getBusyActionError("foo", Action.FLUSHING));
    }

    @Test
    public void testgetBusyActionErrorWithHost_GivenVariousActions()
    {
        assertEquals("Cannot close job foo while another connection on host marple is flushing the job",
                Action.CLOSING.getBusyActionError("foo", Action.FLUSHING, "marple"));
        assertEquals("Cannot delete job foo while another connection on host marple is flushing the job",
                Action.DELETING.getBusyActionError("foo", Action.FLUSHING, "marple"));
        assertEquals("Cannot flush job bar while another connection on host marple is flushing the job",
                Action.FLUSHING.getBusyActionError("bar", Action.FLUSHING, "marple"));
        assertEquals("Cannot pause job foo while another connection on host marple is flushing the job",
                Action.PAUSING.getBusyActionError("foo", Action.FLUSHING, "marple"));
        assertEquals("Cannot resume job foo while another connection on host marple is flushing the job",
                Action.RESUMING.getBusyActionError("foo", Action.FLUSHING, "marple"));
        assertEquals("Cannot revert model snapshot for job foo while another connection on host marple is flushing the job",
                Action.REVERTING.getBusyActionError("foo", Action.FLUSHING, "marple"));
        assertEquals("Cannot update job foo while another connection on host marple is flushing the job",
                Action.UPDATING.getBusyActionError("foo", Action.FLUSHING, "marple"));
        assertEquals("Cannot write to job foo while another connection on host marple is flushing the job",
                Action.WRITING.getBusyActionError("foo", Action.FLUSHING, "marple"));
    }

    @Test
    public void testIsValidTransition_WhenClosed()
    {
        Action currentAction = Action.CLOSED;
        for (Action nextAction : Action.values())
        {
            assertTrue(currentAction.isValidTransition(nextAction));
        }
    }

    @Test
    public void testIsValidTransition_FalseWhenNotClosedOrSleeping()
    {
        for (Action currentAction : Action.values())
        {
            if (currentAction == Action.CLOSED || currentAction == Action.SLEEPING)
            {
                continue;
            }

            for (Action nextAction : Action.values())
            {
                assertFalse(currentAction.isValidTransition(nextAction));
            }
        }
    }

    @Test
    public void testIsValidTransition_ValidStatesFromSleeping()
    {
        assertTrue(Action.SLEEPING.isValidTransition(Action.UPDATING));
        assertTrue(Action.SLEEPING.isValidTransition(Action.FLUSHING));
        assertTrue(Action.SLEEPING.isValidTransition(Action.CLOSING));
        assertTrue(Action.SLEEPING.isValidTransition(Action.DELETING));
        assertTrue(Action.SLEEPING.isValidTransition(Action.WRITING));
        assertTrue(Action.SLEEPING.isValidTransition(Action.PAUSING));

        assertFalse(Action.SLEEPING.isValidTransition(Action.RESUMING));
        assertFalse(Action.SLEEPING.isValidTransition(Action.REVERTING));
        assertFalse(Action.SLEEPING.isValidTransition(Action.CLOSED));
    }

    @Test
    public void testNextState()
    {
        assertEquals(Action.CLOSED, Action.CLOSED.nextState(Action.CLOSED));
        assertEquals(Action.CLOSED, Action.CLOSING.nextState(Action.SLEEPING));
        assertEquals(Action.CLOSED, Action.DELETING.nextState(Action.SLEEPING));
        assertEquals(Action.CLOSED, Action.PAUSING.nextState(Action.SLEEPING));
        assertEquals(Action.CLOSED, Action.RESUMING.nextState(Action.CLOSED));
        assertEquals(Action.CLOSED, Action.REVERTING.nextState(Action.CLOSED));
        assertEquals(Action.SLEEPING, Action.SLEEPING.nextState(Action.WRITING));
        assertEquals(Action.SLEEPING, Action.FLUSHING.nextState(Action.SLEEPING));
        assertEquals(Action.SLEEPING, Action.UPDATING.nextState(Action.SLEEPING));
        assertEquals(Action.CLOSED, Action.UPDATING.nextState(Action.CLOSED));
        assertEquals(Action.SLEEPING, Action.WRITING.nextState(Action.SLEEPING));
    }
}
