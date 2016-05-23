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
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.log4j.Logger;

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

    public static final String LOCK_PATH_PREFIX = "/prelert/engineApi/jobs/";
    private static final String HOST_ACTION_SEPARATOR = "-";
    private static final int ACQUIRE_LOCK_TIMEOUT = 0;


    private final CuratorFramework m_Client;
    private final Map<String, InterProcessMutex> m_LocksByJob = new ConcurrentHashMap<>();
    private String m_Hostname;

    public ZooKeeperActionGuardian(T defaultAction, String host, int port)
    {
        super(defaultAction);

        m_Client = initCuratorFrameworkClient(host, port);
        getAndSetHostName();
    }

    public ZooKeeperActionGuardian(T defaultAction, String host, int port, ActionGuardian<T> nextGuardian)
    {
        super(defaultAction, nextGuardian);
        m_Client = initCuratorFrameworkClient(host, port);
        getAndSetHostName();
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

    private CuratorFramework initCuratorFrameworkClient(String host, int port)
    {
        CuratorFramework client = CuratorFrameworkFactory.newClient(host + ":" + Integer.toString(port),
                                        new ExponentialBackoffRetry(1000, 3));
        client.start();
        return client;
    }

    @Override
    public T currentAction(String jobId)
    {
        InterProcessMutex lock = new InterProcessMutex(m_Client, lockPath(jobId));
        if (tryAcquiringLockNonBlocking(lock))
        {
            try
            {
                return m_NoneAction;
            }
            finally
            {
                try
                {
                    lock.release();
                }
                catch (Exception e)
                {
                    LOGGER.error("Error releasing lock for job " + jobId, e);
                }
            }
        }
        else
        {
            return getHostActionOfLockedJob(jobId).m_Action;
        }
    }

    /**
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
        InterProcessMutex lock = m_LocksByJob.get(jobId);
        if (lock != null)
        {
            HostnameAction hostAction = getHostActionOfLockedJob(jobId);
            if (hostAction.m_Action.isValidTransition(action))
            {
                return newActionTicket(jobId);
            }
            else
            {
                String msg = action.getBusyActionError(jobId, hostAction.m_Action, hostAction.m_Hostname);
                LOGGER.warn(msg);
                throw new JobInUseException(msg, ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR);
            }
        }

        lock = new InterProcessMutex(m_Client, lockPath(jobId));
        if (tryAcquiringLockNonBlocking(lock))
        {
            setHostActionOfLockedJob(jobId, m_Hostname, action);

            m_LocksByJob.put(jobId, lock);

            if (m_NextGuardian.isPresent())
            {
                m_NextGuardian.get().tryAcquiringAction(jobId, action);
            }
            return newActionTicket(jobId);
        }
        else
        {
            HostnameAction hostAction =  getHostActionOfLockedJob(jobId);

            String msg = action.getBusyActionError(jobId, hostAction.m_Action, hostAction.m_Hostname);
            LOGGER.warn(msg);
            throw new JobInUseException(msg, ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR);
        }


    }

    @Override
    public void releaseAction(String jobId)
    {
        InterProcessMutex lock = m_LocksByJob.remove(jobId);
        if (lock == null)
        {
            throw new IllegalStateException("Job " + jobId +
                    " is not locked by this ZooKeeperActionGuardian");
        }

        try
        {
            // clear data
            m_Client.setData().forPath(lockPath(jobId));

            lock.release();
        }
        catch (Exception e)
        {
            LOGGER.error("Error releasing lock for job " + jobId, e);
        }

        if (m_NextGuardian.isPresent())
        {
            m_NextGuardian.get().releaseAction(jobId);
        }
    }

    private boolean tryAcquiringLockNonBlocking(InterProcessMutex lock)
    {
        try
        {
            if (lock.acquire(ACQUIRE_LOCK_TIMEOUT, TimeUnit.MILLISECONDS))
            {
                return true;
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Exception acquiring lock", e);
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

    private String lockPath(String jobId)
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
}
