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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;
import org.apache.zookeeper.data.Stat;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
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
    private static final String CONNECTION_STRING = "localhost:" + PORT;

    // running against an existing ZooKeeper is much faster than
    // using the testing server but some tests will fail if it's
    // not a clean setup
//    private static final String CONNECTION_STRING = "demo1:2181";


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
    public void testConnectionString_withMultipleServers() throws IOException, Exception
    {
        try (TestingServer server2 = new TestingServer(2183))
        {
            try (ZooKeeperActionGuardian<Action> actionGuardian =
                    new ZooKeeperActionGuardian<>(Action.startingState(),
                                "localhost:2183," + CONNECTION_STRING))
            {
            }
        }
    }

    @Test(expected=ConnectException.class)
    public void testConnectionThrows_whenCantConnect() throws IOException, Exception
    {
        try (ZooKeeperActionGuardian<Action> actionGuardian =
                new ZooKeeperActionGuardian<>(Action.startingState(), "localhost:2184"))
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
    public void testEngineApiHosts() throws UnknownHostException, ConnectException
    {
        try (ZooKeeperActionGuardian<Action> actionGuardian =
                new ZooKeeperActionGuardian<>(Action.startingState(), CONNECTION_STRING))
        {
            List<String> hosts = actionGuardian.engineApiHosts();
            assertTrue(hosts.contains(Inet4Address.getLocalHost().getHostName()));
        }
    }

    @Test
    public void testCurrentAction_isCLOSEDForNewJob() throws ConnectException
    {
        try (ZooKeeperActionGuardian<Action> actionGuardian =
                    new ZooKeeperActionGuardian<>(Action.CLOSED, CONNECTION_STRING))
        {
            assertEquals(Action.CLOSED, actionGuardian.currentAction("some-new-job"));
        }
    }

    @Test
    public void testTryAcquiringAction()
    throws JobInUseException, ConnectException
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
    throws JobInUseException, ConnectException
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
    public void testTryAcquireThrows_whenJobIsLocked()
            throws JobInUseException, UnknownHostException, ConnectException
    {
        m_ExpectedException.expect(JobInUseException.class);
        m_ExpectedException.expectMessage(
                                    Action.CLOSING.getBusyActionError("foo", Action.UPDATING,
                                    Inet4Address.getLocalHost().getHostName()));
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR));
        m_ExpectedException.expect(new HostnameMatcher(Inet4Address.getLocalHost().getHostName()));

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
    public void testTryAcquireThrows_resumingWhenJobIsUpdating()
            throws JobInUseException, UnknownHostException, ConnectException
    {
        m_ExpectedException.expect(JobInUseException.class);
        m_ExpectedException.expectMessage(
                                    Action.RESUMING.getBusyActionError("foo", Action.UPDATING,
                                    Inet4Address.getLocalHost().getHostName()));
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.CANNOT_RESUME_JOB));
        m_ExpectedException.expect(new HostnameMatcher(Inet4Address.getLocalHost().getHostName()));

        try (ZooKeeperActionGuardian<Action> actionGuardian =
                new ZooKeeperActionGuardian<>(Action.CLOSED, CONNECTION_STRING))
        {
            try (ZooKeeperActionGuardian<Action>.ActionTicket ticket =
                            actionGuardian.tryAcquiringAction("foo", Action.UPDATING))
            {
                try (ZooKeeperActionGuardian<Action> actionGuardian2 =
                        new ZooKeeperActionGuardian<>(Action.CLOSED, CONNECTION_STRING))
                {
                    actionGuardian2.tryAcquiringAction("foo", Action.RESUMING);
                }
            }
        }
    }


    @Test
    public void testTryAcquireThrows_whenRequestingADifferentActionFromTheSameGuardian()
    throws JobInUseException, UnknownHostException, ConnectException
    {
        m_ExpectedException.expect(JobInUseException.class);
        m_ExpectedException.expectMessage(
                                    Action.CLOSING.getBusyActionError("jeff", Action.UPDATING,
                                    Inet4Address.getLocalHost().getHostName()));
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR));
        m_ExpectedException.expect(new HostnameMatcher(Inet4Address.getLocalHost().getHostName()));

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
    throws JobInUseException, UnknownHostException, ConnectException
    {
        m_ExpectedException.expect(JobInUseException.class);
        m_ExpectedException.expectMessage(
                                    Action.DELETING.getBusyActionError("jeff", Action.DELETING,
                                    Inet4Address.getLocalHost().getHostName()));
        m_ExpectedException.expect(
                ErrorCodeMatcher.hasErrorCode(ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR));
        m_ExpectedException.expect(new HostnameMatcher(Inet4Address.getLocalHost().getHostName()));

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
    throws JobInUseException, UnknownHostException, ConnectException
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
    public void testTryAcquiringAction_acquiresNextLockInChain()
            throws JobInUseException, ConnectException
    {
        @SuppressWarnings("unchecked")
        ActionGuardian<Action> nextGuardian = Mockito.mock(ActionGuardian.class);

        try (ZooKeeperActionGuardian<Action> actionGuardian =
                new ZooKeeperActionGuardian<>(Action.CLOSED, CONNECTION_STRING, nextGuardian))
        {
            try (ZooKeeperActionGuardian<Action>.ActionTicket ticket =
                    actionGuardian.tryAcquiringAction("foo", Action.CLOSING))
            {
            }

            Mockito.verify(nextGuardian).tryAcquiringAction("foo", Action.CLOSING);
        }
    }

    @Test
    public void testTryAcquiringAction_releasesNextLockInChain()
            throws JobInUseException, ConnectException
    {
        @SuppressWarnings("unchecked")
        ActionGuardian<Action> nextGuardian = Mockito.mock(ActionGuardian.class);

        try (ZooKeeperActionGuardian<Action> actionGuardian =
                new ZooKeeperActionGuardian<>(Action.CLOSED, CONNECTION_STRING, nextGuardian))
        {
            actionGuardian.tryAcquiringAction("foo", Action.DELETING);
            actionGuardian.releaseAction("foo", Action.DELETING);
            Mockito.verify(nextGuardian).releaseAction("foo", Action.DELETING);
        }

    }

    @Test
    public void testLockDataToHostnameAction() throws ConnectException
    {
        try (ZooKeeperActionGuardian<Action> guardian =
                new ZooKeeperActionGuardian<>(Action.CLOSED, CONNECTION_STRING))
        {
            String data = "macbook-CLOSING";
            ZooKeeperActionGuardian<Action>.HostnameAction ha = guardian.lockDataToHostAction(data);
            assertEquals("macbook", ha.m_Host);
            assertEquals(Action.CLOSING, ha.m_Action);

            data = "funny-host.name-FLUSHING";
            ha = guardian.lockDataToHostAction(data);
            assertEquals("funny-host.name", ha.m_Host);
            assertEquals(Action.FLUSHING, ha.m_Action);
        }
    }

    @Test
    public void testLockDataToHostnameAction_returnsActionCLOSEDIfBadData()
    throws ConnectException
    {
        try (ZooKeeperActionGuardian<Action> guardian =
                new ZooKeeperActionGuardian<>(Action.CLOSED, CONNECTION_STRING))
        {
            String data = "funny-host.name";
            ZooKeeperActionGuardian<Action>.HostnameAction ha = guardian.lockDataToHostAction(data);
            assertEquals("funny-host.name", ha.m_Host);
            assertEquals(Action.CLOSED, ha.m_Action);

            data = "funny-host.name-";
            ha = guardian.lockDataToHostAction(data);
            assertEquals("funny-host.name-", ha.m_Host);
            assertEquals(Action.CLOSED, ha.m_Action);
        }
    }

    @Test
    public void testHostnameActionToLockData() throws ConnectException
    {
        try (ZooKeeperActionGuardian<Action> guardian =
                new ZooKeeperActionGuardian<>(Action.CLOSED, CONNECTION_STRING))
        {
            String data = guardian.hostActionToData("macbook", Action.DELETING);
            assertEquals("macbook-DELETING", data);

            data = guardian.hostActionToData("funny-host.name", Action.DELETING);
            assertEquals("funny-host.name-DELETING", data);
        }
    }

    @Test(expected=JobInUseException.class)
    public void reentrantLockTest() throws JobInUseException, ConnectException
    {
        try (ZooKeeperActionGuardian<Action> actionGuardian =
                new ZooKeeperActionGuardian<>(Action.CLOSED, CONNECTION_STRING))
        {
            try (ZooKeeperActionGuardian<Action>.ActionTicket ticket =
                    actionGuardian.tryAcquiringAction("foo", Action.UPDATING))
            {
                ZooKeeperActionGuardian<Action>.ActionTicket ticket2 =
                        actionGuardian.tryAcquiringAction("foo", Action.UPDATING);
                ticket2.close();
            }
        }
    }

    @Test
    public void testReleasingLockTransitionsToNextState() throws JobInUseException, ConnectException
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
    public void testReleaseAcquiresNewLockWhenNextStateIsSleeping()
            throws JobInUseException, ConnectException
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

    @Test
    public void testReleaseDeletesActionData() throws Exception
    {
        final String JOB_ID = "foo4";

        try (ZooKeeperActionGuardian<Action> actionGuardian =
                new ZooKeeperActionGuardian<>(Action.CLOSED, CONNECTION_STRING))
        {
            try (ActionGuardian<Action>.ActionTicket ticket =
                    actionGuardian.tryAcquiringAction(JOB_ID, Action.CLOSING))
            {
            }
            assertEquals(Action.CLOSED, actionGuardian.currentAction(JOB_ID));

            try (CuratorFramework client = CuratorFrameworkFactory.newClient(CONNECTION_STRING,
                    new ExponentialBackoffRetry(1000, 3)))
            {
                client.start();

                byte [] data= client.getData().
                            forPath(actionGuardian.descriptionPath(JOB_ID));

                assertEquals(0, data.length);
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testActionIsnotSetIfNextGuardianFails() throws JobInUseException, ConnectException
    {
        ActionGuardian<ScheduledAction> next = Mockito.mock(ActionGuardian.class);

        try (ZooKeeperActionGuardian<ScheduledAction> actionGuardian =
                new ZooKeeperActionGuardian<>(ScheduledAction.startingState(), CONNECTION_STRING, next))
        {

            Mockito.when(next.tryAcquiringAction("foo5", ScheduledAction.STARTED))
                                .thenThrow(JobInUseException.class);

            try
            {
                actionGuardian.tryAcquiringAction("foo5", ScheduledAction.STARTED);
                fail("Expected JobInUseException to be thrown");
            }
            catch (JobInUseException e)
            {
            }

            assertEquals(ScheduledAction.STOPPED, actionGuardian.currentAction("foo"));
        }
    }

    @Test
    public void testCanReleaseLockFromDifferentThread()
            throws JobInUseException, InterruptedException, ConnectException
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
    public void testCurrentAction() throws JobInUseException, InterruptedException, ConnectException
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
        String path = ZooKeeperActionGuardian.JOBS_PATH + "/foo/Action/description";

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

    @Test
    public void testHostByJob() throws Exception
    {
        List<AutoCloseable> toClose = new ArrayList<>();
        try
        {

            ZooKeeperActionGuardian<Action> fooActionGuardian =
                    new ZooKeeperActionGuardian<>(Action.CLOSED, CONNECTION_STRING);
            toClose.add(fooActionGuardian);

            ZooKeeperActionGuardian<Action>.ActionTicket fooTicket =
                    fooActionGuardian.tryAcquiringAction("foo", Action.WRITING);
            toClose.add(0, fooTicket);


            ZooKeeperActionGuardian<Action> barActionGuardian =
                    new ZooKeeperActionGuardian<>(Action.CLOSED, CONNECTION_STRING);
            toClose.add(barActionGuardian);


            ZooKeeperActionGuardian<Action>.ActionTicket barTicket =
                    barActionGuardian.tryAcquiringAction("bar", Action.WRITING);
            toClose.add(1, barTicket);

            Map<String, String> hostByJob = barActionGuardian.hostByJob();
            assertEquals(2, hostByJob.size());
            assertTrue(hostByJob.containsKey("foo"));
            assertTrue(hostByJob.containsKey("bar"));


            String hostname = Inet4Address.getLocalHost().getHostName();
            assertEquals(hostname, hostByJob.get("foo"));
            assertEquals(hostname, hostByJob.get("bar"));
        }
        finally
        {
            for (AutoCloseable closable : toClose)
            {
                closable.close();
            }
        }
    }


    @Test
    public void testSuspendedState() throws Exception
    {
        class TestListener implements ZooKeeperActionGuardian.ConnectionChangeListener
        {
            CountDownLatch reconnectMonitor = new CountDownLatch(1);

            ZooKeeperActionGuardian<Action> actionGuardian;

            public TestListener(ZooKeeperActionGuardian<Action> actionGuardian)
            {
                this.actionGuardian = actionGuardian;
            }

            @Override
            public void stateChange(ConnectionState state)
            {
                switch (state) {
                case RECONNECTED:
                    assertEquals(Action.UPDATING, actionGuardian.currentAction("foo"));
                    reconnectMonitor.countDown();
                break;
                case LOST:
                    assertEquals(Action.CLOSED, actionGuardian.currentAction("foo"));
                break;
                case SUSPENDED:
                break;
                default:
                    break;
                }
            }
        }



        int testPort = 2188;
        try (TestingServer server2 = new TestingServer(testPort))
        {
            try (ZooKeeperActionGuardian<Action> actionGuardian =
                    new ZooKeeperActionGuardian<>(Action.CLOSED, "localhost:" + testPort))
            {
                TestListener listener = new TestListener(actionGuardian);

                try (ActionGuardian<Action>.ActionTicket ticket =
                        actionGuardian.tryAcquiringAction("foo", Action.UPDATING))
                {
                    actionGuardian.addListener(listener);
                    server2.restart();

                    listener.reconnectMonitor.await();

                    // should have reacquired
                    assertEquals(Action.UPDATING, actionGuardian.currentAction("foo"));
                }

            }
        }
    }


    public class HostnameMatcher extends TypeSafeMatcher<JobInUseException>
    {
        private String m_ExpectedHostname;
        private String m_ActualHostname;

        private HostnameMatcher(String expectedHostname)
        {
            m_ExpectedHostname = expectedHostname;
        }

        @Override
        public void describeTo(Description description)
        {
            description.appendValue(m_ActualHostname)
                    .appendText(" was found instead of ")
                    .appendValue(m_ExpectedHostname);
        }

        @Override
        public boolean matchesSafely(JobInUseException e)
        {
            return m_ExpectedHostname.equals(e.getHost());
        }
    }

}
