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
import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.apache.zookeeper.data.Stat;
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
import com.prelert.job.manager.actions.ScheduledAction;

public class ZooKeeperActionGuardianIT
{
    private static TestingServer s_Server;

    private static final int PORT = 2182;
//    private static final String CONNECTION_STRING = "localhost:" + PORT;

    // running against an existing ZooKeeper is much faster than
    // using the testing server but some tests will fail if it's
    // not a clean setup
    private static final String CONNECTION_STRING = "qa3:2181";


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
    public void testConnectionString_withMultipleServers()
    {
        try (ZooKeeperActionGuardian<Action> actionGuardian =
                new ZooKeeperActionGuardian<>(Action.CLOSED, "qa3:2181," + CONNECTION_STRING))
        {
        }
    }

    @Test
    public void testRegistersSelfAsEphemeralNode() throws Exception
    {
        try (ZooKeeperActionGuardian<Action> actionGuardian =
                new ZooKeeperActionGuardian<>(Action.CLOSED, CONNECTION_STRING))
        {
            // while the action guardian is connected the hostname node should exist
            try (CuratorFramework client = CuratorFrameworkFactory.newClient(CONNECTION_STRING,
                    new ExponentialBackoffRetry(1000, 3)))
            {
                client.start();

                List<String> children = client.getChildren().forPath(
                        ZooKeeperActionGuardian.NODES_PATH);

                //
                String hostname = Inet4Address.getLocalHost().getHostName();
                assertTrue(children.contains(hostname));
            }
        }

        // now the ephemeral hostname node is gone
        try (CuratorFramework client = CuratorFrameworkFactory.newClient(CONNECTION_STRING,
                new ExponentialBackoffRetry(1000, 3)))
        {
            client.start();

            List<String> children = client.getChildren().forPath(
                    ZooKeeperActionGuardian.NODES_PATH);

            //
            String hostname = Inet4Address.getLocalHost().getHostName();
            assertFalse(children.contains(hostname));
        }
    }

    @Test
    public void testEngineApiHosts() throws UnknownHostException
    {
        try (ZooKeeperActionGuardian<Action> actionGuardian =
                new ZooKeeperActionGuardian<>(Action.CLOSED, CONNECTION_STRING))
        {
            List<String> hosts = actionGuardian.engineApiHosts();
            assertTrue(hosts.contains(Inet4Address.getLocalHost().getHostName()));
        }
    }

    @Test
    public void testCurrentAction_isCLOSEDForNewJob()
    {
        try (ZooKeeperActionGuardian<Action> actionGuardian =
                    new ZooKeeperActionGuardian<>(Action.CLOSED, CONNECTION_STRING))
        {
            assertEquals(Action.CLOSED, actionGuardian.currentAction("some-new-job"));
        }
    }

    @Test
    public void testTryAcquiringAction()
    throws JobInUseException
    {
        try (ZooKeeperActionGuardian<Action> actionGuardian =
                new ZooKeeperActionGuardian<>(Action.CLOSED, CONNECTION_STRING))
        {
            ZooKeeperActionGuardian<Action>.ActionTicket ticket =
                        actionGuardian.tryAcquiringAction("foo", Action.UPDATING);
            ticket.close();
        }
    }


    @Test
    public void testCurrentAction_whenLockHasBeenAcquired()
    throws JobInUseException
    {
        try (ZooKeeperActionGuardian<Action> actionGuardian =
                new ZooKeeperActionGuardian<>(Action.CLOSED, CONNECTION_STRING))
        {
            try (ZooKeeperActionGuardian<Action>.ActionTicket ticket =
                            actionGuardian.tryAcquiringAction("foo", Action.UPDATING))
            {
                ZooKeeperActionGuardian<Action> actionGuardian2 =
                        new ZooKeeperActionGuardian<>(Action.CLOSED, CONNECTION_STRING);
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
                new ZooKeeperActionGuardian<>(Action.CLOSED, CONNECTION_STRING))
        {
            try (ZooKeeperActionGuardian<Action>.ActionTicket ticket =
                            actionGuardian.tryAcquiringAction("foo", Action.UPDATING))
            {
                try (ZooKeeperActionGuardian<Action> actionGuardian2 =
                        new ZooKeeperActionGuardian<>(Action.CLOSED, CONNECTION_STRING))
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
                new ZooKeeperActionGuardian<>(Action.CLOSED, CONNECTION_STRING))
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
                new ZooKeeperActionGuardian<>(Action.CLOSED, CONNECTION_STRING))
        {
            try (ZooKeeperActionGuardian<Action>.ActionTicket ticket =
                    actionGuardian.tryAcquiringAction("jeff", Action.DELETING))
            {
                actionGuardian.tryAcquiringAction("jeff", Action.DELETING);
            }
        }
    }


    @Test
    @SuppressWarnings("unchecked")
    public void testTryAcquireThrowsReleasesLease_WhenNextClientThrowsJobInUse()
    throws JobInUseException, UnknownHostException
    {
        ActionGuardian<Action> next = Mockito.mock(ActionGuardian.class);
        Mockito.when(next.tryAcquiringAction("foo4", Action.UPDATING))
                                .thenThrow(JobInUseException.class);


        try (ZooKeeperActionGuardian<Action> actionGuardian =
                new ZooKeeperActionGuardian<>(Action.CLOSED, CONNECTION_STRING, next))
        {
            try
            {
                actionGuardian.tryAcquiringAction("foo4", Action.UPDATING);
                fail("Expected next guardian to throw");
            }
            catch (JobInUseException e)
            {
            }

            // if the lease wasn't released we can't acquire here
            try (ZooKeeperActionGuardian<Action>.ActionTicket ticket =
                        actionGuardian.tryAcquiringAction("foo4", Action.CLOSING))
            {
            }
        }
    }


    @Test
    public void testTryAcquiringAction_acquiresNextLockInChain() throws JobInUseException
    {
        @SuppressWarnings("unchecked")
        ActionGuardian<Action> nextGuardian = Mockito.mock(ActionGuardian.class);

        try (ZooKeeperActionGuardian<Action> actionGuardian = new ZooKeeperActionGuardian<>(Action.CLOSED, CONNECTION_STRING, nextGuardian))
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

        try (ZooKeeperActionGuardian<Action> actionGuardian = new ZooKeeperActionGuardian<>(Action.CLOSED, CONNECTION_STRING, nextGuardian))
        {
            actionGuardian.tryAcquiringAction("foo", Action.DELETING);
            actionGuardian.releaseAction("foo", Action.DELETING);
            Mockito.verify(nextGuardian).releaseAction("foo", Action.DELETING);
        }

    }

    @Test
    public void testLockDataToHostnameAction()
    {
        try (ZooKeeperActionGuardian<Action> guardian = new ZooKeeperActionGuardian<>(Action.CLOSED, CONNECTION_STRING))
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
        try (ZooKeeperActionGuardian<Action> guardian = new ZooKeeperActionGuardian<>(Action.CLOSED, CONNECTION_STRING))
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
        try (ZooKeeperActionGuardian<Action> guardian = new ZooKeeperActionGuardian<>(Action.CLOSED, CONNECTION_STRING))
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
                new ZooKeeperActionGuardian<>(Action.CLOSED, CONNECTION_STRING))
        {
            try (ZooKeeperActionGuardian<Action>.ActionTicket ticket =
                    actionGuardian.tryAcquiringAction("foo", Action.UPDATING))
            {
                ZooKeeperActionGuardian<Action>.ActionTicket ticket2 = actionGuardian.tryAcquiringAction("foo", Action.UPDATING);
                ticket2.close();
            }
        }
    }

    @Test
    public void testReleasingLockTransitionsToNextState() throws JobInUseException
    {
        try (ZooKeeperActionGuardian<Action> actionGuardian =
                new ZooKeeperActionGuardian<>(Action.CLOSED, CONNECTION_STRING))
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
                new ZooKeeperActionGuardian<>(Action.CLOSED, CONNECTION_STRING))
        {
            try (ActionGuardian<Action>.ActionTicket ticket =
                    actionGuardian.tryAcquiringAction("foo", Action.WRITING))
            {
            }
            assertEquals(Action.SLEEPING, actionGuardian.currentAction("foo"));

            m_ExpectedException.expect(JobInUseException.class);

            try (ZooKeeperActionGuardian<Action> actionGuardian2 =
                    new ZooKeeperActionGuardian<>(Action.CLOSED, CONNECTION_STRING))
            {
                actionGuardian2.tryAcquiringAction("foo", Action.WRITING);
            }
        }
    }


/***
    @Test
    public void testReleaseDeletesNode() throws Exception
    {
        final String JOB_ID = "foo";

        try (ZooKeeperActionGuardian<Action> actionGuardian =
                new ZooKeeperActionGuardian<>(Action.CLOSED, CONNECTION_STRING))
        {
            try (ActionGuardian<Action>.ActionTicket ticket =
                    actionGuardian.tryAcquiringAction(JOB_ID, Action.CLOSING))
            {
            }
            assertEquals(Action.CLOSED, actionGuardian.currentAction(JOB_ID));

            try (CuratorFramework client = CuratorFrameworkFactory.newClient(HOST + ":" + Integer.toString(PORT),
                    new ExponentialBackoffRetry(1000, 3)))
            {
                client.start();

                List<String> children = client.getChildren().forPath(
                        ZooKeeperActionGuardian.LOCK_PATH_PREFIX.substring(0, ZooKeeperActionGuardian.LOCK_PATH_PREFIX.length() -1));

                // assert no child nodes with the name JOB_ID
                assertEquals(0, children.stream().filter(s -> s.equals(JOB_ID)).count());
            }
        }
    }

    @Test
    public void testReleaseDoesnotDeleteNodeIfAnotherLockIsHeldForJob() throws Exception
    {
        final String JOB_ID = "foo";

        try (CuratorFramework client = CuratorFrameworkFactory.newClient(HOST + ":" + Integer.toString(PORT),
                new ExponentialBackoffRetry(1000, 3)))
        {
            client.start();

            // set a lock for a scheduled action
            try (ZooKeeperActionGuardian<ScheduledAction> scheduledJobActionGuardian =
                    new ZooKeeperActionGuardian<>(ScheduledAction.STOP, CONNECTION_STRING))
            {

                try (ActionGuardian<ScheduledAction>.ActionTicket schedulerTicket =
                        scheduledJobActionGuardian.tryAcquiringAction(JOB_ID, ScheduledAction.START))
                {
                    // set a lock for the job action
                    try (ZooKeeperActionGuardian<Action> actionGuardian =
                            new ZooKeeperActionGuardian<>(Action.CLOSED, CONNECTION_STRING))
                    {
                        try (ActionGuardian<Action>.ActionTicket ticket =
                                actionGuardian.tryAcquiringAction(JOB_ID, Action.CLOSING))
                        {
                        }
                        assertEquals(Action.CLOSED, actionGuardian.currentAction(JOB_ID));


                        List<String> children = client.getChildren().forPath(
                                ZooKeeperActionGuardian.LOCK_PATH_PREFIX.substring(0, ZooKeeperActionGuardian.LOCK_PATH_PREFIX.length() -1));

                        // assert the job node hasn't been deleted while the scheduler still
                        // has a lock
                        assertTrue(children.contains(JOB_ID));
                    }
                }

                // stopping should release the scheduler lock
                try (ActionGuardian<ScheduledAction>.ActionTicket schedulerTicket =
                        scheduledJobActionGuardian.tryAcquiringAction(JOB_ID, ScheduledAction.STOP))
                {
                }


                // now the scheduler lock is released the job node should have been deleted
                List<String> children = client.getChildren().forPath(
                        ZooKeeperActionGuardian.LOCK_PATH_PREFIX.substring(0, ZooKeeperActionGuardian.LOCK_PATH_PREFIX.length() -1));

                // assert no child nodes with the name JOB_ID
                assertEquals(0, children.stream().filter(s -> s.equals(JOB_ID)).count());
            }
        }
    }
**/
    @Test
    @SuppressWarnings("unchecked")
    public void testActionIsnotSetIfNextGuardianFails() throws JobInUseException
    {
        ActionGuardian<ScheduledAction> next = Mockito.mock(ActionGuardian.class);

        try (ZooKeeperActionGuardian<ScheduledAction> actionGuardian =
                new ZooKeeperActionGuardian<>(ScheduledAction.STOP, CONNECTION_STRING, next))
        {

            Mockito.when(next.tryAcquiringAction("foo5", ScheduledAction.START))
                                .thenThrow(JobInUseException.class);

            try
            {
                actionGuardian.tryAcquiringAction("foo5", ScheduledAction.START);
                fail("Expected JobInUseException to be thrown");
            }
            catch (JobInUseException e)
            {
            }

            assertEquals(ScheduledAction.STOP, actionGuardian.currentAction("foo"));
        }
    }

    @Test
    public void testCanReleaseLockFromDifferentThread() throws JobInUseException, InterruptedException
    {
        try (ZooKeeperActionGuardian<Action> actionGuardian =
                new ZooKeeperActionGuardian<>(Action.CLOSED, CONNECTION_STRING))
        {
            ActionGuardian<Action>.ActionTicket ticket =
                    actionGuardian.tryAcquiringAction("foo", Action.CLOSING);


            Thread th = new Thread(() -> ticket.close());
            th.start();
            th.join();

            assertEquals(Action.CLOSED, actionGuardian.currentAction("foo"));
        }
    }

    @Test
    public void testCurrentAction() throws JobInUseException, InterruptedException
    {
        try (ZooKeeperActionGuardian<Action> actionGuardian =
                new ZooKeeperActionGuardian<>(Action.CLOSED, CONNECTION_STRING))
        {
            try (ActionGuardian<Action>.ActionTicket ticket =
                    actionGuardian.tryAcquiringAction("foo", Action.CLOSING))
            {
                assertEquals(Action.CLOSING, actionGuardian.currentAction("foo"));
            }
        }
    }

    @Test
    public void testEphemeralDescriptionNodeIsRemovedAfterClosingClient() throws Exception
    {
        String path = ZooKeeperActionGuardian.LOCK_PATH_PREFIX + "foo/Action/description";

        // now the ephemeral description node should be gone
        try (CuratorFramework client = CuratorFrameworkFactory.newClient(CONNECTION_STRING,
                new ExponentialBackoffRetry(1000, 3)))
        {
            client.start();

            try (ZooKeeperActionGuardian<Action> actionGuardian =
                    new ZooKeeperActionGuardian<>(Action.CLOSED, CONNECTION_STRING))
            {
                try (ActionGuardian<Action>.ActionTicket ticket =
                        actionGuardian.tryAcquiringAction("foo", Action.UPDATING))
                {
                    Stat stat = client.checkExists().forPath(path);
                    assertNotNull(stat);
                }
            }

            Stat stat = client.checkExists().forPath(path);
            assertNull(stat);
        }
    }

}
