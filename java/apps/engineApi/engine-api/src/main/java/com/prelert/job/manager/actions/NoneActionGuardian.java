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

import com.prelert.job.exceptions.JobInUseException;

/**
 * ActionGuardian that does nothing.
 * {@linkplain #currentAction(String)} is always the default.
 * {@linkplain #releaseAction(String, T)} does nothing
 * @param <T>
 */
public class NoneActionGuardian<T extends Enum<T> & ActionState<T>> extends ActionGuardian<T>
{
    public NoneActionGuardian(T noneAction)
    {
        super(noneAction);
    }

    /**
     * Always returns the default
     */
    @Override
    public T currentAction(String jobId)
    {
        return m_NoneAction;
    }

    @Override
    public ActionTicket tryAcquiringAction(String jobId, T action)
    throws JobInUseException
    {
        return newActionTicket(jobId, action.nextState(m_NoneAction));
    }

    /**
     * Dummy method does nothing
     */
    @Override
    public void releaseAction(String jobId, T nextState)
    {
        // Do nothing
    }

}
