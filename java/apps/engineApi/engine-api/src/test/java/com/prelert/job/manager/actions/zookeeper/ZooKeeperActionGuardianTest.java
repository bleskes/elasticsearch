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

package com.prelert.job.manager.actions.zookeeper;

import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.curator.test.TestingServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.manager.actions.Action;
import com.prelert.job.manager.actions.ActionGuardian;
import com.prelert.job.manager.actions.ActionGuardian.ActionTicket;

public class ZooKeeperActionGuardianTest
{
    static final int PORT = 2182;

    private static TestingServer s_Server;

    @BeforeClass
    public static void zooKeeperSetup() throws Exception
    {
        s_Server = new TestingServer(PORT);
    }

    @AfterClass
    public static void zooKeeperTakeDown() throws IOException
    {
        s_Server.close();
    }

    @Test
    public void testCurrentAction_isNoneForNewJob()
    {
        ZooKeeperActionGuardian actionGuardian = new ZooKeeperActionGuardian("localhost", PORT);

        assertEquals(Action.NONE, actionGuardian.currentAction("some-new-job"));
    }

    @Test
    public void testTryAcquiringAction()
    throws JobInUseException
    {
        ZooKeeperActionGuardian actionGuardian = new ZooKeeperActionGuardian("localhost", PORT);
        ActionTicket ticket = actionGuardian.tryAcquiringAction("foo", Action.UPDATING);
        ticket.close();
    }

    @Test
    public void testCurrentAction_whenLockHasBeenAcquired()
    throws JobInUseException
    {
        ZooKeeperActionGuardian actionGuardian = new ZooKeeperActionGuardian("localhost", PORT);
        try (ActionTicket ticket = actionGuardian.tryAcquiringAction("foo", Action.UPDATING))
        {
            ZooKeeperActionGuardian actionGuardian2 = new ZooKeeperActionGuardian("localhost", PORT);
            Action currentAction = actionGuardian2.currentAction("foo");

            assertEquals(Action.UPDATING, currentAction);
        }
    }

    @Test(expected=JobInUseException.class)
    public void testTryAcquireThrows_whenJobIsLocked() throws JobInUseException
    {
        ZooKeeperActionGuardian actionGuardian = new ZooKeeperActionGuardian("localhost", PORT);
        try (ActionTicket ticket = actionGuardian.tryAcquiringAction("foo", Action.UPDATING))
        {
            ZooKeeperActionGuardian actionGuardian2 = new ZooKeeperActionGuardian("localhost", PORT);
            actionGuardian2.tryAcquiringAction("foo", Action.CLOSING);
        }
    }

    @Test(expected=JobInUseException.class)
    public void testTryAcquireThrows_whenJobIsLockedByTheSameGuardian() throws JobInUseException
    {
        ZooKeeperActionGuardian actionGuardian = new ZooKeeperActionGuardian("localhost", PORT);
        try (ActionTicket ticket = actionGuardian.tryAcquiringAction("jeff", Action.UPDATING))
        {
            actionGuardian.tryAcquiringAction("jeff", Action.CLOSING);
        }
    }

    @Test()
    public void testTryAcquireReturnsTicketWhenRequestedActionIsSameAsCurrent()
    throws JobInUseException
    {
        ZooKeeperActionGuardian actionGuardian = new ZooKeeperActionGuardian("localhost", PORT);
        try (ActionTicket ticket = actionGuardian.tryAcquiringAction("jeff", Action.DELETING))
        {
            actionGuardian.tryAcquiringAction("jeff", Action.DELETING);
        }
    }


    @Test
    public void testTryAcquiringAction_acquiresNextLockInChain() throws JobInUseException
    {
        ActionGuardian nextGuardian = Mockito.mock(ActionGuardian.class);

        ActionGuardian actionGuardian = new ZooKeeperActionGuardian("localhost", PORT, nextGuardian);

        try (ActionTicket ticket = actionGuardian.tryAcquiringAction("foo", Action.CLOSING))
        {

        }

        Mockito.verify(nextGuardian).tryAcquiringAction("foo", Action.CLOSING);
    }

    @Test
    public void testTryAcquiringAction_releasesNextLockInChain() throws JobInUseException
    {
        ActionGuardian nextGuardian = Mockito.mock(ActionGuardian.class);

        ActionGuardian actionGuardian = new ZooKeeperActionGuardian("localhost", PORT, nextGuardian);
        actionGuardian.tryAcquiringAction("foo", Action.DELETING);
        actionGuardian.releaseAction("foo");

        Mockito.verify(nextGuardian).releaseAction("foo");
    }
}
