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
import com.prelert.job.manager.actions.ActionGuardian.ActionTicket;

public class LocalActionGuardianTest
{
    @Test
    public void testTryAcquiringAction_GivenAvailable() throws JobInUseException
    {
        ActionGuardian actionGuardian = new LocalActionGuardian();
        try (ActionTicket actionTicket = actionGuardian.tryAcquiringAction("foo", Action.WRITING))
        {
            assertEquals(Action.WRITING, actionGuardian.currentAction("foo"));
            assertEquals(Action.NONE, actionGuardian.currentAction("unknown"));
        }
        assertEquals(Action.NONE, actionGuardian.currentAction("foo"));
    }

    @Test
    public void testTryAcquiringAction_GivenJobIsInUse() throws JobInUseException
    {
        ActionGuardian actionGuardian = new LocalActionGuardian();
        try (ActionTicket deleting = actionGuardian.tryAcquiringAction("foo", Action.DELETING))
        {
            try (ActionTicket writing = actionGuardian.tryAcquiringAction("foo", Action.WRITING))
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
        assertEquals(Action.NONE, actionGuardian.currentAction("foo"));
    }

    @Test
    public void testTryAcquiringAction_acquiresNextLock() throws JobInUseException
    {
        ActionGuardian nextGuardian = Mockito.mock(ActionGuardian.class);

        ActionGuardian actionGuardian = new LocalActionGuardian(nextGuardian);
        actionGuardian.tryAcquiringAction("foo", Action.CLOSING);

        Mockito.verify(nextGuardian).tryAcquiringAction("foo", Action.CLOSING);
    }

    @Test
    public void testTryAcquiringAction_releasesNextLock() throws JobInUseException
    {
        ActionGuardian nextGuardian = Mockito.mock(ActionGuardian.class);

        ActionGuardian actionGuardian = new LocalActionGuardian(nextGuardian);
        actionGuardian.releaseAction("foo");

        Mockito.verify(nextGuardian).releaseAction("foo");
    }

}
