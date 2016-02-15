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

package com.prelert.job.manager.actions;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobInUseException;

/**
 * Guards and manages permissions for performing actions on jobs.
 */
public class ActionGuardian
{
    private static final Logger LOGGER = Logger.getLogger(ActionGuardian.class);

    private final Map<String, Action> m_ActionsByJob;

    public ActionGuardian()
    {
        m_ActionsByJob = new HashMap<>();
    }

    public Action getAction(String jobId)
    {
        synchronized (this)
        {
            return m_ActionsByJob.getOrDefault(jobId, Action.NONE);
        }
    }

    /**
     * Returns an {@code ActionTicket} if requested action is available for the given job.
     * @param jobId the job id
     * @param action the requested action
     * @return the {@code ActionTicket} granting permission to execute the action
     * @throws JobInUseException If the job is in use by another action
     */
    public ActionTicket tryAcquiringAction(String jobId, Action action) throws JobInUseException
    {
        synchronized (this)
        {
            Action currentAction = m_ActionsByJob.getOrDefault(jobId, Action.NONE);
            if (currentAction == Action.NONE)
            {
                m_ActionsByJob.put(jobId, action);
                return new ActionTicket(jobId);
            }
            else
            {
                String msg = action.getErrorString(jobId, currentAction);
                LOGGER.warn(msg);
                throw new JobInUseException(msg, ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR);
            }
        }
    }

    /**
     * Releases the action for the given job
     * @param jobId the job id
     */
    public void releaseAction(String jobId)
    {
        synchronized (this)
        {
            m_ActionsByJob.remove(jobId);
        }
    }

    /**
     * A token signifying that its owner has permission to execute an action for a job.
     * Designed to be used with try-with-resources to ensure the action is released.
     */
    public class ActionTicket implements AutoCloseable
    {
        private final String m_JobId;

        private ActionTicket(String jobId)
        {
            m_JobId = jobId;
        }

        @Override
        public void close()
        {
            ActionGuardian.this.releaseAction(m_JobId);
        }
    }
}
