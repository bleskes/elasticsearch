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
import com.prelert.job.manager.actions.Action;
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
 */
public class ZooKeeperActionGuardian extends ActionGuardian
{
    private static final Logger LOGGER = Logger.getLogger(ZooKeeperActionGuardian.class);

    public static final String LOCK_PATH_PREFIX = "/prelert/engineApi/jobs/";
    private static final String HOST_ACTION_SEPARATOR = "-";
    private static final int ACQUIRE_LOCK_TIMEOUT = 100;


    private final CuratorFramework m_Client;
    private final Map<String, InterProcessMutex> m_LocksByJob = new ConcurrentHashMap<>();
    private String m_Hostname;

    public ZooKeeperActionGuardian(String host, int port)
    {
        m_Client = initCuratorFrameworkClient(host, port);
        getAndSetHostName();
    }

    public ZooKeeperActionGuardian(String host, int port, ActionGuardian nextGuardian)
    {
        super(nextGuardian);
        m_Client = initCuratorFrameworkClient(host, port);
        getAndSetHostName();
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
    public Action currentAction(String jobId)
    {
        InterProcessMutex lock = new InterProcessMutex(m_Client, lockPath(jobId));
        if (tryAcquiringLockNonBlocking(lock))
        {
            try
            {
                return Action.NONE;
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
     * The interprocess mutex is re-entrant so not an error
     * if we already hold the lock
     *
     * @param jobId
     * @param action
     * @return
     * @throws JobInUseException
     */
    @Override
    public ActionTicket tryAcquiringAction(String jobId, Action action)
    throws JobInUseException
    {
        InterProcessMutex lock = m_LocksByJob.get(jobId);
        if (lock != null)
        {
            HostnameAction hostAction = getHostActionOfLockedJob(jobId);
            if (hostAction.m_Action == action)
            {
                return newActionTicket(jobId);
            }
            else
            {
                String msg = action.getErrorString(jobId, hostAction.m_Action, hostAction.m_Hostname);
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

            String msg = action.getErrorString(jobId, hostAction.m_Action, hostAction.m_Hostname);
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
            return new HostnameAction("", Action.NONE);
        }
    }

    private void setHostActionOfLockedJob(String jobId, String hostname, Action action)
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
            return new HostnameAction(data, Action.NONE);
        }

        Action action;
        String host = data.substring(0, lastIndex);
        try
        {
            action = Action.valueOf(data.substring(lastIndex +1));
        }
        catch (IllegalArgumentException e)
        {
            LOGGER.error("Cannot parse action from lock data", e);
            action = Action.NONE;
            host = data;
        }

        HostnameAction ha = new HostnameAction(host, action);
        return ha;
    }

    @VisibleForTesting
    String hostActionToData(String hostname, Action action)
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
        final Action m_Action;

        HostnameAction(String hostname, Action action)
        {
            m_Hostname = hostname;
            m_Action = action;
        }
    }
}
