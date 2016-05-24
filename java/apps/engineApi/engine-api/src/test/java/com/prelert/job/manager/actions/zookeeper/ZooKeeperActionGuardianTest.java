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
import java.net.Inet4Address;
import java.net.UnknownHostException;

import org.apache.curator.test.TestingServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.prelert.job.errorcodes.ErrorCodeMatcher;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.manager.actions.Action;
import com.prelert.job.manager.actions.ActionGuardian;

public class ZooKeeperActionGuardianTest
{
    static final int PORT = 2182;

    private static TestingServer s_Server;

    @Rule
    public ExpectedException m_ExpectedException = ExpectedException.none();

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
    public void testCurrentAction_isCLOSEDForNewJob()
    {
        try (ZooKeeperActionGuardian<Action> actionGuardian =
                    new ZooKeeperActionGuardian<>(Action.CLOSED, "localhost", PORT))
        {

            assertEquals(Action.CLOSED, actionGuardian.currentAction("some-new-job"));
        }
    }

    @Test
    public void testTryAcquiringAction()
    throws JobInUseException
    {
        try (ZooKeeperActionGuardian<Action> actionGuardian =
                new ZooKeeperActionGuardian<>(Action.CLOSED, "localhost", PORT))
        {
            ZooKeeperActionGuardian<Action>.ActionTicket ticket = actionGuardian.tryAcquiringAction("foo", Action.UPDATING);
            ticket.close();
        }
    }

    @Test
    public void testCurrentAction_whenLockHasBeenAcquired()
    throws JobInUseException
    {
        try (ZooKeeperActionGuardian<Action> actionGuardian =
                new ZooKeeperActionGuardian<>(Action.CLOSED, "localhost", PORT))
        {
            try (ZooKeeperActionGuardian<Action>.ActionTicket ticket = actionGuardian.tryAcquiringAction("foo", Action.UPDATING))
            {
                ZooKeeperActionGuardian<Action> actionGuardian2 =
                        new ZooKeeperActionGuardian<>(Action.CLOSED, "localhost", PORT);
                Action currentAction = actionGuardian2.currentAction("foo");
                actionGuardian2.close();

                assertEquals(Action.UPDATING, currentAction);
            }
        }
    }

    @Test
    public void testTryAcquireThrows_whenJobIsLocked() throws JobInUseException, UnknownHostException
    {
        m_ExpectedException.expect(JobInUseException.class);
        m_ExpectedException.expectMessage(
                                    Action.CLOSING.getBusyActionError("foo", Action.UPDATING,
                                    Inet4Address.getLocalHost().getHostName()));
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR));

        try (ZooKeeperActionGuardian<Action> actionGuardian =
                new ZooKeeperActionGuardian<>(Action.CLOSED, "localhost", PORT))
        {
            try (ZooKeeperActionGuardian<Action>.ActionTicket ticket = actionGuardian.tryAcquiringAction("foo", Action.UPDATING))
            {
                try (ZooKeeperActionGuardian<Action> actionGuardian2 =
                        new ZooKeeperActionGuardian<>(Action.CLOSED, "localhost", PORT))
                {
                    actionGuardian2.tryAcquiringAction("foo", Action.CLOSING);
                }
            }
        }
    }

    @Test
    public void testTryAcquireThrows_whenRequestingADifferentActionFromTheSameGuardian()
    throws JobInUseException, UnknownHostException
    {
        m_ExpectedException.expect(JobInUseException.class);
        m_ExpectedException.expectMessage(
                                    Action.CLOSING.getBusyActionError("jeff", Action.UPDATING,
                                    Inet4Address.getLocalHost().getHostName()));
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR));

        try (ZooKeeperActionGuardian<Action> actionGuardian =
                new ZooKeeperActionGuardian<>(Action.CLOSED, "localhost", PORT))
        {
            try (ZooKeeperActionGuardian<Action>.ActionTicket ticket = actionGuardian.tryAcquiringAction("jeff", Action.UPDATING))
            {
                actionGuardian.tryAcquiringAction("jeff", Action.CLOSING);
            }
        }
    }

    @Test
    public void testTryAcquireThrowsWhenRequestedActionIsSameAsCurrent()
    throws JobInUseException, UnknownHostException
    {
        m_ExpectedException.expect(JobInUseException.class);
        m_ExpectedException.expectMessage(
                                    Action.DELETING.getBusyActionError("jeff", Action.DELETING,
                                    Inet4Address.getLocalHost().getHostName()));
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR));

        try (ZooKeeperActionGuardian<Action> actionGuardian =
                new ZooKeeperActionGuardian<>(Action.CLOSED, "localhost", PORT))
        {
            try (ZooKeeperActionGuardian<Action>.ActionTicket ticket =
                    actionGuardian.tryAcquiringAction("jeff", Action.DELETING))
            {
                actionGuardian.tryAcquiringAction("jeff", Action.DELETING);
            }
        }
    }


    @Test
    public void testTryAcquiringAction_acquiresNextLockInChain() throws JobInUseException
    {
        @SuppressWarnings("unchecked")
        ActionGuardian<Action> nextGuardian = Mockito.mock(ActionGuardian.class);

        try (ZooKeeperActionGuardian<Action> actionGuardian = new ZooKeeperActionGuardian<>(Action.CLOSED, "localhost", PORT, nextGuardian))
        {
            try (ZooKeeperActionGuardian<Action>.ActionTicket ticket = actionGuardian.tryAcquiringAction("foo", Action.CLOSING))
            {
            }

            Mockito.verify(nextGuardian).tryAcquiringAction("foo", Action.CLOSING);
        }
    }

    @Test
    public void testTryAcquiringAction_releasesNextLockInChain() throws JobInUseException
    {
        @SuppressWarnings("unchecked")
        ActionGuardian<Action> nextGuardian = Mockito.mock(ActionGuardian.class);

        try (ZooKeeperActionGuardian<Action> actionGuardian = new ZooKeeperActionGuardian<>(Action.CLOSED, "localhost", PORT, nextGuardian))
        {
            actionGuardian.tryAcquiringAction("foo", Action.DELETING);
            actionGuardian.releaseAction("foo", Action.DELETING);
            Mockito.verify(nextGuardian).releaseAction("foo", Action.DELETING);
        }

    }

    @Test
    public void testLockDataToHostnameAction()
    {
        try (ZooKeeperActionGuardian<Action> guardian = new ZooKeeperActionGuardian<>(Action.CLOSED, "localhost", PORT))
        {
            String data = "macbook-CLOSING";
            ZooKeeperActionGuardian<Action>.HostnameAction ha = guardian.lockDataToHostAction(data);
            assertEquals("macbook", ha.m_Hostname);
            assertEquals(Action.CLOSING, ha.m_Action);

            data = "funny-host.name-FLUSHING";
            ha = guardian.lockDataToHostAction(data);
            assertEquals("funny-host.name", ha.m_Hostname);
            assertEquals(Action.FLUSHING, ha.m_Action);
        }
    }

    @Test
    public void testLockDataToHostnameAction_returnsActionCLOSEDIfBadData()
    {
        try (ZooKeeperActionGuardian<Action> guardian = new ZooKeeperActionGuardian<>(Action.CLOSED, "localhost", PORT))
        {
            String data = "funny-host.name";
            ZooKeeperActionGuardian<Action>.HostnameAction ha = guardian.lockDataToHostAction(data);
            assertEquals("funny-host.name", ha.m_Hostname);
            assertEquals(Action.CLOSED, ha.m_Action);

            data = "funny-host.name-";
            ha = guardian.lockDataToHostAction(data);
            assertEquals("funny-host.name-", ha.m_Hostname);
            assertEquals(Action.CLOSED, ha.m_Action);
        }
    }

    @Test
    public void testHostnameActionToLockData()
    {
        try (ZooKeeperActionGuardian<Action> guardian = new ZooKeeperActionGuardian<>(Action.CLOSED, "localhost", PORT))
        {
            String data = guardian.hostActionToData("macbook", Action.DELETING);
            assertEquals("macbook-DELETING", data);

            data = guardian.hostActionToData("funny-host.name", Action.DELETING);
            assertEquals("funny-host.name-DELETING", data);
        }
    }

    @Test(expected=JobInUseException.class)
    public void reentrantLockTest() throws JobInUseException
    {
        try (ZooKeeperActionGuardian<Action> actionGuardian =
                new ZooKeeperActionGuardian<>(Action.CLOSED, "localhost", PORT))
        {
            ZooKeeperActionGuardian<Action>.ActionTicket ticket = actionGuardian.tryAcquiringAction("foo", Action.UPDATING);
            ZooKeeperActionGuardian<Action>.ActionTicket ticket2 = actionGuardian.tryAcquiringAction("foo", Action.UPDATING);
            ticket.close();
            ticket2.close();
        }
    }

    @Test
    public void testReleasingLockTransitionsToNextState() throws JobInUseException
    {
        try (ZooKeeperActionGuardian<Action> actionGuardian =
                new ZooKeeperActionGuardian<>(Action.CLOSED, "localhost", PORT))
        {
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

            try (ActionGuardian<Action>.ActionTicket ticket =
                    actionGuardian.tryAcquiringAction("foo", Action.UPDATING))
            {
            }
            assertEquals(Action.SLEEPING, actionGuardian.currentAction("foo"));
        }
    }

    @Test
    public void testReleaseAcquiresNewLockWhenNextStateIsSleeping() throws JobInUseException
    {
        try (ZooKeeperActionGuardian<Action> actionGuardian =
                new ZooKeeperActionGuardian<>(Action.CLOSED, "localhost", PORT))
        {
            try (ActionGuardian<Action>.ActionTicket ticket =
                    actionGuardian.tryAcquiringAction("foo", Action.WRITING))
            {
            }
            assertEquals(Action.SLEEPING, actionGuardian.currentAction("foo"));

            m_ExpectedException.expect(JobInUseException.class);

            try (ZooKeeperActionGuardian<Action> actionGuardian2 =
                    new ZooKeeperActionGuardian<>(Action.CLOSED, "localhost", PORT))
            {
                actionGuardian2.tryAcquiringAction("foo", Action.WRITING);
            }
        }
    }
}
