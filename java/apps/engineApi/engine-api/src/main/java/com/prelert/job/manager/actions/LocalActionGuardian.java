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
 * Prevents concurrent actions on a job based on the contents of a local
 * map. If a job currently partaking in an action {@linkplain #tryAcquiringAction(String, Enum)}
 * will throw otherwise an ActionTicket is returned
 *
 * @param <T>
 */
public class LocalActionGuardian<T extends Enum<T> & ActionState<T>>
                            extends ActionGuardian<T>
{
    private static final Logger LOGGER = Logger.getLogger(LocalActionGuardian.class);

    private final Map<String, T> m_ActionsByJob = new HashMap<>();

    public LocalActionGuardian(T defaultAction)
    {
        super(defaultAction);
    }

    public LocalActionGuardian(T defaultAction, ActionGuardian<T> guardian)
    {
        super(defaultAction, guardian);
    }

    @Override
    public T currentAction(String jobId)
    {
        synchronized (this)
        {
            return m_ActionsByJob.getOrDefault(jobId, m_NoneAction);
        }
    }

    /**
     * Returns an {@code ActionTicket} if requested action is available for the given job.
     * @param jobId the job id
     * @param action the requested action
     * @return the {@code ActionTicket} granting permission to execute the action
     * @throws JobInUseException If the job is in use by another action
     */
    @Override
    public ActionTicket tryAcquiringAction(String jobId, T action) throws JobInUseException
    {
        synchronized (this)
        {
            T currentAction = m_ActionsByJob.getOrDefault(jobId, m_NoneAction);

            if (currentAction.isValidTransition(action))
            {
                m_ActionsByJob.put(jobId, action);

                if (m_NextGuardian.isPresent())
                {
                    m_NextGuardian.get().tryAcquiringAction(jobId, action);
                }

                return newActionTicket(jobId);
            }
            else
            {
                String msg = action.getBusyActionError(jobId, currentAction);
                LOGGER.warn(msg);
                throw new JobInUseException(msg, ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR);
            }
        }
    }


    @Override
    public void releaseAction(String jobId)
    {
        synchronized (this)
        {
            m_ActionsByJob.remove(jobId);

            if (m_NextGuardian.isPresent())
            {
                m_NextGuardian.get().releaseAction(jobId);
            }
        }
    }

}
