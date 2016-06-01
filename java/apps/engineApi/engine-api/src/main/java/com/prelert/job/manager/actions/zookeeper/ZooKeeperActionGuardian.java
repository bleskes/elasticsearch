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
import com.prelert.distributed.EngineApiHosts;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.manager.actions.ActionState;
import com.prelert.job.manager.actions.ActionGuardian;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.framework.recipes.locks.InterProcessReadWriteLock;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreV2;
import org.apache.curator.framework.recipes.locks.Lease;
import org.apache.curator.framework.state.ConnectionState;
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
 *
 * Internally the locking procedure is a little more complicated as we not
 * only want to hold the action lock but write a description of the action so
 * that other nodes know what kind of action is holding the lock.
 * Writing the action description and acquiring the lock cannot be done in a
 * single operation so 2 locks are required. The action lock MUST only be
 * acquired or released when the description lock is held in this way all
 * clients will see a consistent view of the action's description.
 *
 * <em>A</em> Is the Action lock held continuously while the action is taking place
 * <em>D</em> Is the Description lock held only while update the actions description
 *
 *                      A-------------------A
 *                     /                     \
 *                  D----D                 D----D
 *      ___________/       \______________/      \_______ Client 1
 *
 *                            try-acquire A here fails
 *                             |
 *                      xxxx D----D
 *      _______________/            \____________________ Client 2
 *
 *
 * Note the Action lock is a Semaphore as it may be released by a different
 * thread. The Description lock is a read-write lock, multiple readers can
 * read as long as the write lock isn't acquired
 */
public class ZooKeeperActionGuardian<T extends Enum<T> & ActionState<T>>
                    extends ActionGuardian<T> implements AutoCloseable, EngineApiHosts
{
    private static final Logger LOGGER = Logger.getLogger(ZooKeeperActionGuardian.class);

    private static final String BASE_DIR = "/prelert";
    private static final String ENGINE_API_DIR = "/engineApi";
    public static final String LOCK_PATH_PREFIX = BASE_DIR + ENGINE_API_DIR + "/jobs/";
    public static final String NODES_PATH = BASE_DIR + ENGINE_API_DIR + "/nodes";

    private static final String HOST_ACTION_SEPARATOR = "-";
    private static final int ACQUIRE_ACTION_LOCK_TIMEOUT = 0;


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
        deregisterSelf();
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
        m_Client.getConnectionStateListenable().addListener(
                (client, newState) ->
                    {if (newState == ConnectionState.RECONNECTED) { registerSelf(client); }}
       );
    }

    @Override
    public T currentAction(String jobId)
    {
        InterProcessReadWriteLock readWriteLock = new InterProcessReadWriteLock(m_Client,
                                                    descriptionLockPath(jobId));

        InterProcessMutex readLock = readWriteLock.readLock();

        // blocks if the write lock is held
        if (acquireLock(readLock, jobId))
        {
            try
            {
                return getHostActionOfLockedJob(jobId).m_Action;
            }
            finally
            {
                releaseLock(readLock, jobId);
            }
        }

        return m_NoneAction;
    }

    /**
     * The returned ActionTicket MUST be closed in a try-with-resource block
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
        InterProcessReadWriteLock readWriteLock = new InterProcessReadWriteLock(m_Client,
                                                            descriptionLockPath(jobId));

        InterProcessMutex writeLock = readWriteLock.writeLock();
        if (acquireLock(writeLock, jobId))
        {
            try
            {
                HostnameAction currentState = getHostActionOfLockedJob(jobId);

                // if this already holds the lease return a new ticket
                // if a valid state transition
                Lease lease = m_LeaseByJob.get(jobId);
                if (lease != null)
                {
                    return trySettingAction(lease, jobId, currentState, action);
                }
                else
                {
                    InterProcessSemaphoreV2 lock = new InterProcessSemaphoreV2(m_Client, lockPath(jobId), 1);
                    lease = tryAcquiringLockNonBlocking(lock);

                    if (lease != null)
                    {
                        ActionTicket ticket = trySettingAction(lease, jobId, currentState, action);
                        m_LeaseByJob.put(jobId, lease);

                        return ticket;
                    }
                    else
                    {
                        String msg = action.getBusyActionError(jobId, currentState.m_Action,
                                                                currentState.m_Hostname);
                        LOGGER.warn(msg);
                        throw new JobInUseException(msg, ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR);
                    }
                }
            }
            finally
            {
                releaseLock(writeLock, jobId);
            }
        }
        else
        {
            LOGGER.error("Failed to acquire readwrite lock for job " + jobId);
            return newActionTicket("", m_NoneAction);
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
            return lock.acquire(ACQUIRE_ACTION_LOCK_TIMEOUT, TimeUnit.MILLISECONDS);
        }
        catch (Exception e)
        {
            LOGGER.error("Exception acquiring lock", e);
        }

        return null;
    }

    private ActionTicket trySettingAction(Lease lease, String jobId,
                                            HostnameAction currentState, T action)
    throws JobInUseException
    {
        if (currentState.m_Action.isValidTransition(action))
        {
            if (m_NextGuardian.isPresent())
            {
                m_NextGuardian.get().tryAcquiringAction(jobId, action);
            }

            setHostActionOfLockedJob(jobId, m_Hostname, action);
            return newActionTicket(jobId, action.nextState(currentState.m_Action));
        }
        else
        {
            String msg = action.getBusyActionError(jobId, currentState.m_Action,
                    currentState.m_Hostname);
            LOGGER.warn(msg);
            throw new JobInUseException(msg, ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR);
        }
    }

    @Override
    public void releaseAction(String jobId, T nextState)
    {
        InterProcessReadWriteLock readWriteLock = new InterProcessReadWriteLock(m_Client,
                                descriptionLockPath(jobId));

        InterProcessMutex writeLock = readWriteLock.writeLock();
        if (acquireLock(writeLock, jobId))
        {
            try
            {
                if (m_NextGuardian.isPresent())
                {
                    m_NextGuardian.get().releaseAction(jobId, nextState);
                }

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
            }
            finally
            {
                releaseLock(writeLock, jobId);
            }
        }

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

    private boolean acquireLock(InterProcessMutex lock, String jobId)
    {
        try
        {
            lock.acquire();
            return true;
        }
        catch (Exception e)
        {
            LOGGER.error("Error acquiring lock for job " + jobId, e);
        }

        return false;
    }

    private boolean releaseLock(InterProcessMutex lock, String jobId)
    {
        try
        {
            lock.release();
            return true;
        }
        catch (Exception e)
        {
            LOGGER.error("Error releasing lock for job " + jobId, e);
        }

        return false;
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

    private String jobPath(String jobId)
    {
        return LOCK_PATH_PREFIX + jobId;
    }

    private String lockPath(String jobId)
    {
        return LOCK_PATH_PREFIX + jobId + "/" + m_NoneAction.typename();
    }

    private String descriptionLockPath(String jobId)
    {
        return LOCK_PATH_PREFIX + jobId + "/" + m_NoneAction.typename() + "/description";
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

    /**
     * Write hostname to an ephemeral node
     * @param client
     */
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

    /**
     * Delete the ephemeral node with this hostname
     */
    private void deregisterSelf()
    {
        try
        {
            m_Client.delete().deletingChildrenIfNeeded().forPath(NODES_PATH + "/" + m_Hostname);
        }
        catch (NodeExistsException e)
        {
        }
        catch (Exception e)
        {
            LOGGER.warn("Error de-registering node with hostname '" + m_Hostname + "' in ZooKeeper", e);
        }
    }

    @Override
    public List<String> engineApiHosts()
    {
        try
        {
            return m_Client.getChildren().forPath(NODES_PATH);
        }
        catch (Exception e)
        {
            LOGGER.error("Error reading Engine API nodes", e);
            return Collections.emptyList();
        }
    }
}
