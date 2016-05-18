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

import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobInUseException;
import com.prelert.job.manager.actions.Action;
import com.prelert.job.manager.actions.ActionGuardian;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.log4j.Logger;

public class ZooKeeperActionGuardian extends ActionGuardian
{
    private static final Logger LOGGER = Logger.getLogger(ZooKeeperActionGuardian.class);

    public static final String LOCK_PATH_PREFIX = "/engineApi/jobs/";

    private final CuratorFramework m_Client;
    private final Map<String, InterProcessMutex> m_LocksByJob = new HashMap<>();

    public ZooKeeperActionGuardian(String host, int port)
    {
        m_Client = initCuratorFrameworkClient(host, port);
    }

    public ZooKeeperActionGuardian(String host, int port, ActionGuardian nextGuardian)
    {
        super(nextGuardian);
        m_Client = initCuratorFrameworkClient(host, port);
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
            try {
                return getActionOfLockedJob(jobId);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return Action.NONE;

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
            try
            {
                Action currentAction = getActionOfLockedJob(jobId);
                if (currentAction == action)
                {
                    return newActionTicket(jobId);
                }
                else
                {
                    String msg = action.getErrorString(jobId, currentAction);
                    LOGGER.warn(msg);
                    throw new JobInUseException(msg, ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR);
                }
            }
            catch (Exception e)
            {
                LOGGER.error("Error reading current action for job " + jobId, e);
            }
        }

        lock = new InterProcessMutex(m_Client, lockPath(jobId));
        if (tryAcquiringLockNonBlocking(lock))
        {
            try {
                setActionOfLockedJob(jobId, action);
            } catch (Exception e) {
                e.printStackTrace();
            }

            synchronized (this)
            {
                m_LocksByJob.put(jobId, lock);
            }

            if (m_NextGuardian.isPresent())
            {
                m_NextGuardian.get().tryAcquiringAction(jobId, action);
            }
            return newActionTicket(jobId);
        }
        else
        {
            Action currentAction;
            try {
                currentAction = getActionOfLockedJob(jobId);
            } catch (Exception e) {
                currentAction = Action.NONE;
                e.printStackTrace();
            }

            String msg = action.getErrorString(jobId, currentAction);
            LOGGER.warn(msg);
            throw new JobInUseException(msg, ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR);
        }


    }

    @Override
    public void releaseAction(String jobId)
    {
        synchronized (this)
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

    }

    private boolean tryAcquiringLockNonBlocking(InterProcessMutex lock)
    {
        try
        {
            if (lock.acquire(100, TimeUnit.MILLISECONDS))
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


    private Action getActionOfLockedJob(String jobId) throws Exception
    {
        String data = new String(m_Client.getData().forPath(lockPath(jobId)),
                                 StandardCharsets.UTF_8);
        return Action.valueOf(data);
    }

    private void setActionOfLockedJob(String jobId, Action action) throws Exception
    {
        String data = action.toString();
        m_Client.setData().forPath(lockPath(jobId), data.getBytes(StandardCharsets.UTF_8));
    }

    private String lockPath(String jobId)
    {
        return LOCK_PATH_PREFIX + jobId;
    }

}
