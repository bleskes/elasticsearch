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

import com.google.common.annotations.VisibleForTesting;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.manager.actions.ActionState;
import com.prelert.job.manager.actions.ActionGuardian;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreV2;
import org.apache.curator.framework.recipes.locks.Lease;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.log4j.Logger;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.ZooDefs.Ids;

/**
 * Distributed lock for restricting actions on jobs in a network
 * of engine API nodes.
 *
 * If another node has the lock {@linkplain #tryAcquiringAction(String, Enum)}
 * will throw. If this node has the lock {@linkplain #tryAcquiringAction(String, Enum)}
 * may throw depending on the result of {@linkplain T#isValidTransition(T)}
 */
public class ZooKeeperActionGuardian<T extends Enum<T> & ActionState<T>>
                    extends ActionGuardian<T> implements AutoCloseable
{
    private static final Logger LOGGER = Logger.getLogger(ZooKeeperActionGuardian.class);

    private static final String BASE_DIR = "/prelert";
    private static final String ENGINE_API_DIR = "/engineApi";
    public static final String LOCK_PATH_PREFIX = BASE_DIR + ENGINE_API_DIR + "/jobs/";
    public static final String NODES_PATH = BASE_DIR + ENGINE_API_DIR + "/nodes";

    private static final String HOST_ACTION_SEPARATOR = "-";
    private static final int ACQUIRE_LOCK_TIMEOUT = 0;


    private CuratorFramework m_Client;
    private final Map<String, Lease> m_LeaseByJob = new ConcurrentHashMap<>();
    private String m_Hostname;

    public ZooKeeperActionGuardian(T defaultAction, String host, int port)
    {
        super(defaultAction);

        initCuratorFrameworkClient(host, port);
    }

    public ZooKeeperActionGuardian(T defaultAction, String host, int port, ActionGuardian<T> nextGuardian)
    {
        super(defaultAction, nextGuardian);

        initCuratorFrameworkClient(host, port);
    }

    @Override
    public void close()
    {
        m_Client.close();
    }

    private void getAndSetHostName()
    {
        try
        {
            m_Hostname = Inet4Address.getLocalHost().getHostName();
        }
        catch (UnknownHostException e)
        {
            m_Hostname = "localhost";
            LOGGER.error("Cannot resolve hostname", e);
        }
    }

    private void initCuratorFrameworkClient(String host, int port)
    {
        getAndSetHostName();

        m_Client = CuratorFrameworkFactory.newClient(host + ":" + Integer.toString(port),
                                        new ExponentialBackoffRetry(1000, 3));

        m_Client.start();
        try
        {
            m_Client.blockUntilConnected(30, TimeUnit.SECONDS);
        }
        catch (InterruptedException e)
        {
            LOGGER.error("ZooKeeper block until connection interrrupted");
        }

        createBasePath(m_Client);
        registerSelf(m_Client);

        // if the connection is lost then reconnected then
        // recreate the ephemeral hostname node
        m_Client.getConnectionStateListenable().addListener(new ConnectionStateListener()
        {
            @Override
            public void stateChanged(CuratorFramework client, ConnectionState newState)
            {
                if (newState == ConnectionState.RECONNECTED)
                {
                    registerSelf(client);
                }
            }
        });
    }

    @Override
    public T currentAction(String jobId)
    {
        InterProcessSemaphoreV2 lock = new InterProcessSemaphoreV2(m_Client, lockPath(jobId), 1);
        Lease lease = tryAcquiringLockNonBlocking(lock);

        if (lease != null)
        {
            try
            {
                return m_NoneAction;
            }
            finally
            {
                releaseLeaseAndDeleteNode(lease, jobId);
            }
        }
        else
        {

            return getHostActionOfLockedJob(jobId).m_Action;
        }
    }

    /**
     * The returned ActionTicket MUST be closed in a try-with-resource block
     *
     * The interprocess mutex is reentrant so not an error
     * if we already hold the lock
     *
     * @param jobId
     * @param action
     * @return
     * @throws JobInUseException
     */
    @Override
    public ActionTicket tryAcquiringAction(String jobId, T action)
    throws JobInUseException
    {
        Lease lease = m_LeaseByJob.get(jobId);
        if (lease != null)
        {
            HostnameAction hostAction = getHostActionOfLockedJob(jobId);
            if (hostAction.m_Action.isValidTransition(action))
            {
                return newActionTicket(jobId, action.nextState(hostAction.m_Action));
            }
            else
            {
                String msg = action.getBusyActionError(jobId, hostAction.m_Action, hostAction.m_Hostname);
                LOGGER.warn(msg);
                throw new JobInUseException(msg, ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR);
            }
        }

        InterProcessSemaphoreV2 lock = new InterProcessSemaphoreV2(m_Client, lockPath(jobId), 1);
        lease = tryAcquiringLockNonBlocking(lock);

        if (lease != null)
        {
            if (m_NextGuardian.isPresent())
            {
                m_NextGuardian.get().tryAcquiringAction(jobId, action);
            }

            setHostActionOfLockedJob(jobId, m_Hostname, action);
            m_LeaseByJob.put(jobId, lease);

            return newActionTicket(jobId, action.nextState(m_NoneAction));
        }
        else
        {
            HostnameAction hostAction = getHostActionOfLockedJob(jobId);
            String msg = action.getBusyActionError(jobId, hostAction.m_Action, hostAction.m_Hostname);
            LOGGER.warn(msg);
            throw new JobInUseException(msg, ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR);
        }
    }

    @Override
    public void releaseAction(String jobId, T nextState)
    {
        if (nextState.holdDistributedLock())
        {
            setHostActionOfLockedJob(jobId, m_Hostname, nextState);
        }
        else
        {
            // release the lock
            Lease lease = m_LeaseByJob.remove(jobId);
            if (lease == null)
            {
                throw new IllegalStateException("Job " + jobId +
                        " is not locked by this ZooKeeperActionGuardian");
            }

            releaseLeaseAndDeleteNode(lease, jobId);
        }

        if (m_NextGuardian.isPresent())
        {
            m_NextGuardian.get().releaseAction(jobId, nextState);
        }
    }

    /**
     * returned lease is null if lease wasn't acquired
     * @param lock
     * @return
     */
    private Lease tryAcquiringLockNonBlocking(InterProcessSemaphoreV2 lock)
    {
        try
        {
            return lock.acquire(ACQUIRE_LOCK_TIMEOUT, TimeUnit.MILLISECONDS);
        }
        catch (Exception e)
        {
            LOGGER.error("Exception acquiring lock", e);
        }

        return null;
    }

    private void releaseLeaseAndDeleteNode(Lease lease, String jobId)
    {
        try
        {
            lease.close();

            // clear data and delete job node
            m_Client.setData().forPath(lockPath(jobId));
            m_Client.delete().deletingChildrenIfNeeded().forPath(lockPath(jobId));

            // if no other locks for the job delete the job node
            if (m_Client.getChildren().forPath(jobPath(jobId)).isEmpty())
            {
                m_Client.delete().forPath(jobPath(jobId));
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Error releasing lock for job " + jobId, e);
        }
    }

    private HostnameAction getHostActionOfLockedJob(String jobId)
    {
        try
        {
            String data = new String(m_Client.getData().forPath(lockPath(jobId)),
                                     StandardCharsets.UTF_8);
            return lockDataToHostAction(data);
        }
        catch (Exception e)
        {
            LOGGER.error("Error reading lock data" , e);
            return new HostnameAction("", m_NoneAction);
        }
    }

    private void setHostActionOfLockedJob(String jobId, String hostname, T action)
    {
        String data = hostActionToData(hostname, action);
        try
        {
            m_Client.setData().forPath(lockPath(jobId), data.getBytes(StandardCharsets.UTF_8));
        }
        catch (Exception e)
        {
            LOGGER.error("Error setting host action for lock", e);
        }
    }

    @VisibleForTesting
    HostnameAction lockDataToHostAction(String data)
    {
        int lastIndex = data.lastIndexOf(HOST_ACTION_SEPARATOR);

        // error if separator not found or if not followed by anything
        if (lastIndex < 0 || (lastIndex + 1 >= data.length()))
        {
            LOGGER.error("Invalid lock data cannot be parsed: " + data);
            return new HostnameAction(data, m_NoneAction);
        }

        T action = m_NoneAction;
        String host = data.substring(0, lastIndex);
        try
        {
            Class<T> type = m_NoneAction.getDeclaringClass();
            action = Enum.<T>valueOf(type, data.substring(lastIndex +1));
        }
        catch (IllegalArgumentException e)
        {
            LOGGER.error("Cannot parse action from lock data", e);
            host = data;
        }

        HostnameAction ha = new HostnameAction(host, action);
        return ha;
    }

    @VisibleForTesting
    String hostActionToData(String hostname, T action)
    {
        return hostname + HOST_ACTION_SEPARATOR + action.toString();
    }

    private String lockPath(String jobId)
    {
        return LOCK_PATH_PREFIX + jobId + "/" + m_NoneAction.typename();
    }

    private String jobPath(String jobId)
    {
        return LOCK_PATH_PREFIX + jobId;
    }

    @VisibleForTesting
    class HostnameAction
    {
        final String m_Hostname;
        final T m_Action;

        HostnameAction(String hostname, T action)
        {
            m_Hostname = hostname;
            m_Action = action;
        }
    }

    private void createBasePath(CuratorFramework client)
    {
        for (String path : new String [] {BASE_DIR, BASE_DIR + ENGINE_API_DIR, NODES_PATH})
        {
            try
            {
                client.create().withMode(CreateMode.PERSISTENT).forPath(path);
            }
            catch (NodeExistsException e)
            {
            }
            catch (Exception e)
            {
                LOGGER.warn("Error registering node with hostname '" + m_Hostname + "' in ZooKeeper", e);
            }
        }
    }

    private void registerSelf(CuratorFramework client)
    {
        try
        {
            client.create().withMode(CreateMode.EPHEMERAL)
                    .withACL(Ids.READ_ACL_UNSAFE).forPath(NODES_PATH + "/" + m_Hostname);
        }
        catch (NodeExistsException e)
        {
        }
        catch (Exception e)
        {
            LOGGER.warn("Error registering node with hostname '" + m_Hostname + "' in ZooKeeper", e);
        }
    }
}
