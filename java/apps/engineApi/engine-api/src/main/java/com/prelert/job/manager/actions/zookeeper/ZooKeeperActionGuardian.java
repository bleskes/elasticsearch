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
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.manager.actions.ActionState;
import com.prelert.job.manager.actions.ActionGuardian;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

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
import org.apache.zookeeper.KeeperException.NoNodeException;
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
 * that other nodes know what kind of action and who is holding the lock.
 * Writing the action description and acquiring the lock cannot be done in a
 * single operation so 2 locks are required. The action lock MUST only be
 * acquired or released when the description lock is held in this way all
 * clients will see a consistent view of the action's description.
 *
 * <em>A</em> Is the Action lock held continuously while the action is taking place
 * <em>D</em> Is the Description lock held only while updating the actions description
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
                    extends ActionGuardian<T> implements AutoCloseable, EngineApiHosts, Feature
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

    /**
     * Connection string is a comma separated list of host:port without whitespace
     * e.g. "server1:2181,server2:2181"
     *
     * @param defaultAction
     * @param connectionString
     */
    public ZooKeeperActionGuardian(T defaultAction, String connectionString)
    {
        super(defaultAction);

        initCuratorFrameworkClient(connectionString);
    }

    /**
     * Connection string is a comma separated list of host:port without whitespace
     * e.g. "server1:2181,server2:2181"
     *
     * @param defaultAction
     * @param connectionString
     * @param nextGuardian
     */
    public ZooKeeperActionGuardian(T defaultAction, String connectionString, ActionGuardian<T> nextGuardian)
    {
        super(defaultAction, nextGuardian);

        initCuratorFrameworkClient(connectionString);
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

    private void initCuratorFrameworkClient(String connectionString)
    {
        getAndSetHostName();

        m_Client = CuratorFrameworkFactory.newClient(connectionString,
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
        InterProcessReadWriteLock descriptionReadWriteLock = new InterProcessReadWriteLock(m_Client,
                                                    descriptionLockPath(jobId));

        InterProcessMutex descriptionReadLock = descriptionReadWriteLock.readLock();

        // blocks if the write lock is held
        if (acquireLock(descriptionReadLock, jobId))
        {
            try
            {
                return getHostActionOfLockedJob(jobId).m_Action;
            }
            finally
            {
                releaseDescriptionLock(descriptionReadLock, jobId);
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
        InterProcessReadWriteLock descriptionReadWriteLock= new InterProcessReadWriteLock(m_Client,
                                                            descriptionLockPath(jobId));

        InterProcessMutex descriptionWriteLock = descriptionReadWriteLock.writeLock();
        if (acquireLock(descriptionWriteLock, jobId))
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
                    InterProcessSemaphoreV2 actionLock = new InterProcessSemaphoreV2(m_Client,
                                                                            actionLockPath(jobId), 1);
                    lease = tryAcquiringLockNonBlocking(actionLock);

                    if (lease != null)
                    {
                        try
                        {
                            ActionTicket ticket = trySettingAction(lease, jobId, currentState, action);
                            m_LeaseByJob.put(jobId, lease);
                            return ticket;
                        }
                        catch (JobInUseException e)
                        {
                            try
                            {
                                lease.close();
                            }
                            catch (IOException e1)
                            {
                                LOGGER.error("Error releasing lease after failing to change "
                                                + "action for job " + jobId, e);
                            }
                            throw e;
                        }
                    }
                    else
                    {
                        String msg = action.getBusyActionError(jobId, currentState.m_Action,
                                                                currentState.m_Hostname);
                        LOGGER.warn(msg);
                        throw new JobInUseException(msg, action.getErrorCode(),
                                                currentState.m_Hostname);
                    }
                }
            }
            finally
            {
                releaseDescriptionLock(descriptionWriteLock, jobId);
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

    /**
     * If the action can't be set then release the lease
     *
     * @param lease
     * @param jobId
     * @param currentState
     * @param action
     * @return
     * @throws JobInUseException
     */
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
            throw new JobInUseException(msg, action.getErrorCode(), currentState.m_Hostname);
        }
    }

    @Override
    public void releaseAction(String jobId, T nextState)
    {
        InterProcessReadWriteLock descriptionReadWriteLock = new InterProcessReadWriteLock(m_Client,
                                descriptionLockPath(jobId));

        InterProcessMutex descriptionWriteLock = descriptionReadWriteLock.writeLock();
        if (acquireLock(descriptionWriteLock, jobId))
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

                    deleteHostActionDataForJob(jobId);
                    releaseActionLeaseAndDeleteNode(lease, jobId);
                }
            }
            finally
            {
                releaseDescriptionLock(descriptionWriteLock, jobId);
            }
        }

    }

    private void releaseActionLeaseAndDeleteNode(Lease actionLease, String jobId)
    {
        try
        {
            actionLease.close();

            // if no other locks for the job delete the job action node
            if (m_Client.getChildren().forPath(actionLockPath(jobId)).isEmpty())
            {
                m_Client.delete().forPath(actionLockPath(jobId));
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Error releasing lease for job " + jobId, e);
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

    private boolean releaseDescriptionLock(InterProcessMutex lock, String jobId)
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
            String data = new String(m_Client.getData().forPath(
                        descriptionPath(jobId)),
                                     StandardCharsets.UTF_8);
            return lockDataToHostAction(data);
        }
        catch (NoNodeException e)
        {
            return new HostnameAction("", m_NoneAction);
        }
        catch (Exception e)
        {
            LOGGER.error("Error reading lock data" , e);
            return new HostnameAction("", m_NoneAction);
        }
    }

    private void setHostActionOfLockedJob(String jobId, String hostname, T action)
    {
        byte[] data = hostActionToData(hostname, action).getBytes(StandardCharsets.UTF_8);
        try
        {
            String path = descriptionPath(jobId);

            try
            {
                m_Client.create().creatingParentsIfNeeded()
                        .withMode(CreateMode.EPHEMERAL)
                        .withACL(Ids.OPEN_ACL_UNSAFE).forPath(path, data);
            }
            catch (NodeExistsException e)
            {
                m_Client.setData().forPath(path, data);
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Error setting host action for lock", e);
        }
    }

    private void deleteHostActionDataForJob(String jobId)
    {
        try
        {
            m_Client.setData().forPath(descriptionPath(jobId));
        }
        catch (Exception e)
        {
            LOGGER.error("Error clearing host action for job " + jobId, e);
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

        return new HostnameAction(host, action);
    }

    @VisibleForTesting
    String hostActionToData(String hostname, T action)
    {
        return hostname + HOST_ACTION_SEPARATOR + action.toString();
    }

    private String actionLockPath(String jobId)
    {
        return LOCK_PATH_PREFIX + jobId + "/" + m_NoneAction.typename();
    }

    private String descriptionLockPath(String jobId)
    {
        return LOCK_PATH_PREFIX + jobId + "/" + m_NoneAction.typename() + "/description--lock" ;
    }

    private String descriptionPath(String jobId)
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
        catch (NoNodeException nne)
        {
            LOGGER.warn("Unexpected state: Hostname node already deleted");
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

    @Override
    public boolean configure(FeatureContext context)
    {
        return false;
    }
}
