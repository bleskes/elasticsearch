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

import java.util.Optional;

import com.prelert.job.exceptions.JobInUseException;

/**
 * Guards and manages permissions for performing actions on jobs.
 *
 * The {@linkplain ActionTicket} returned by {@linkplain #tryAcquiringAction(String, Enum)}
 * must be closed and should be used only in a try-with-resource block.
 *
 * Guardians can be chained together by passing another to he
 * constructor. Locks are only granted when all locks are acquired.
 *
 * Implementing classes must acquire and release the next guardian.
 */
public abstract class ActionGuardian< T extends Enum<T> & ActionState<T>>
{
    protected final Optional<ActionGuardian<T>> m_NextGuardian;

    protected final T m_NoneAction;

    /**
     * noneAction is the enum value representing the state where
     * no action is taking place.
     * e.g. @
     *
     * @param noneAction
     */
    public ActionGuardian(T noneAction)
    {
        m_NoneAction = noneAction;
        m_NextGuardian = Optional.empty();
    }

    /**
     * noneAction is the enum value representing the state where
     * no action is taking place.
     * guardian is the next guard to check if this guard succeeds in
     * acquiring an action.
     *
     * @param noneAction
     * @param guardian
     */
    public ActionGuardian(T noneAction, ActionGuardian<T> guardian)
    {
        m_NoneAction = noneAction;
        m_NextGuardian = Optional.of(guardian);
    }

    /**
     * Get the action the job is currently processing
     * or {@linkplain Action#NONE} if the job is not active
     *
     * @param jobId
     * @return
     */
    public abstract T currentAction(String jobId);

    /**
     * The returned ActionTicket MUST be closed in a try-with-resource block
     *
     * Returns an {@code ActionTicket} if requested action is available for the given job.
     * @param jobId the job id
     * @param action the requested action
     * @return the {@code ActionTicket} granting permission to execute the action
     * @throws JobInUseException If the job is in use by another action
     */
    public abstract ActionTicket tryAcquiringAction(String jobId, T action) throws JobInUseException;

    /**
     * Releases the action for the given job
     * @param jobId the job id
     * @param nextState Put the guardian into this state after releasing the action
     */
    public abstract void releaseAction(String jobId, T nextState);

    /**
     * A token signifying that its owner has permission to execute an action for a job.
     * Designed to be used with try-with-resources to ensure the action is released.
     */
    public class ActionTicket implements AutoCloseable
    {
        private final String m_JobId;
        private final T m_NextState;

        private ActionTicket(String jobId, T nextState)
        {
            m_JobId = jobId;
            m_NextState = nextState;
        }

        @Override
        public void close()
        {
            ActionGuardian.this.releaseAction(m_JobId, m_NextState);
        }
    }

    protected ActionTicket newActionTicket(String jobId, T nextState)
    {
        return new ActionTicket(jobId, nextState);
    }
}
