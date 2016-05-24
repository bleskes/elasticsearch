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
import static org.junit.Assert.fail;

import org.junit.Test;
import org.mockito.Mockito;

import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobInUseException;

public class LocalActionGuardianTest
{
    @Test
    public void testTryAcquiringAction_GivenAvailable() throws JobInUseException
    {
        ActionGuardian<Action> actionGuardian = new LocalActionGuardian<>(Action.CLOSED);
        try (ActionGuardian<Action>.ActionTicket actionTicket = actionGuardian.tryAcquiringAction("foo", Action.WRITING))
        {
            assertEquals(Action.WRITING, actionGuardian.currentAction("foo"));
            assertEquals(Action.CLOSED, actionGuardian.currentAction("unknown"));
        }
        assertEquals(Action.SLEEPING, actionGuardian.currentAction("foo"));
    }

    @Test
    public void testTryAcquiringAction_GivenJobIsInUse() throws JobInUseException
    {
        ActionGuardian<Action> actionGuardian = new LocalActionGuardian<>(Action.CLOSED);
        try (ActionGuardian<Action>.ActionTicket deleting = actionGuardian.tryAcquiringAction("foo", Action.DELETING))
        {
            try (ActionGuardian<Action>.ActionTicket writing = actionGuardian.tryAcquiringAction("foo", Action.WRITING))
            {
                fail();
            }
            catch (JobInUseException e)
            {
                assertEquals("Cannot write to job foo while another connection is deleting the job", e.getMessage());
                assertEquals(ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR, e.getErrorCode());
            }
            assertEquals(Action.DELETING, actionGuardian.currentAction("foo"));
        }
        assertEquals(Action.CLOSED, actionGuardian.currentAction("foo"));
    }

    @Test
    public void testTryAcquiringAction_acquiresNextLock() throws JobInUseException
    {
        @SuppressWarnings("unchecked")
        ActionGuardian<Action> nextGuardian = Mockito.mock(ActionGuardian.class);

        ActionGuardian<Action> actionGuardian = new LocalActionGuardian<>(Action.CLOSED, nextGuardian);
        actionGuardian.tryAcquiringAction("foo", Action.CLOSING);

        Mockito.verify(nextGuardian).tryAcquiringAction("foo", Action.CLOSING);
    }

    @Test
    public void testTryAcquiringAction_releasesNextLock() throws JobInUseException
    {
        @SuppressWarnings("unchecked")
        ActionGuardian<Action> nextGuardian = Mockito.mock(ActionGuardian.class);

        ActionGuardian<Action> actionGuardian = new LocalActionGuardian<>(Action.CLOSED, nextGuardian);
        actionGuardian.releaseAction("foo", Action.CLOSED);

        Mockito.verify(nextGuardian).releaseAction("foo", Action.CLOSED);
    }

    @Test
    public void testReleasingLockTransitionsToNextState() throws JobInUseException
    {
        ActionGuardian<Action> actionGuardian = new LocalActionGuardian<>(Action.CLOSED);

        try (ActionGuardian<Action>.ActionTicket ticket =
                    actionGuardian.tryAcquiringAction("foo", Action.CLOSING))
        {
        }
        assertEquals(Action.CLOSED, actionGuardian.currentAction("foo"));

        try (ActionGuardian<Action>.ActionTicket ticket =
                actionGuardian.tryAcquiringAction("foo", Action.WRITING))
        {
        }
        assertEquals(Action.SLEEPING, actionGuardian.currentAction("foo"));
    }
}
