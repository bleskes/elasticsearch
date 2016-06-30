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

import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.messages.Messages;

/**
 * Actions relating to the scheduler
 */
public enum ScheduledAction implements ActionState<ScheduledAction>
{
    START(Messages.JOB_SCHEDULER_CANNOT_START, Messages.JOB_SCHEDULER_STATUS_STARTED),
    STOP(Messages.JOB_SCHEDULER_CANNOT_STOP_IN_CURRENT_STATE, Messages.JOB_SCHEDULER_STATUS_STOPPED),
    DELETE(Messages.JOB_SCHEDULER_CANNOT_DELETE_IN_CURRENT_STATE, Messages.JOB_SCHEDULER_STATUS_DELETING),
    UPDATE(Messages.JOB_SCHEDULER_CANNOT_UPDATE_IN_CURRENT_STATE, Messages.JOB_SCHEDULER_STATUS_UPDATING);

    private final String m_MessageKey;
    private final String m_VerbKey;

    public static ScheduledAction startingState()
    {
        return STOP;
    }

    private ScheduledAction(String messageKey, String verbKey)
    {
        m_MessageKey = messageKey;
        m_VerbKey = verbKey;
    }

    /**
     * Return true if allowed to transition from this state to next.
     * Only allowed to transition from STOP to any other state.
     *
     * @param action
     * @return
     */
    @Override
    public boolean isValidTransition(ScheduledAction next)
    {
        // If stopped any transition is valid
        return this == START && next == STOP || this == STOP;
    }

    /**
     * If START the next state is STOP.
     * If UPDATE the next state is STOP.
     * If STOP the next state is STOP.
     * If DELETE the next state is STOP.
     */
    @Override
    public ScheduledAction nextState(ScheduledAction unused)
    {
        return STOP;
    }

    @Override
    public String getActionVerb()
    {
        return m_VerbKey;
    }

    public String getMessageKey()
    {
        return m_MessageKey;
    }

    @Override
    public String getBusyActionError(String jobId, ActionState<ScheduledAction> actionInUse)
    {
        return Messages.getMessage(getMessageKey(), jobId,
                            Messages.getMessage(actionInUse.getActionVerb()));
    }

    @Override
    public String getBusyActionError(String jobId, ActionState<ScheduledAction> actionInUse,
                                    String host)
    {
        return Messages.getMessage(getMessageKey(),
                                jobId,
                                Messages.getMessage(actionInUse.getActionVerb()) + " "
                                + Messages.getMessage(Messages.ON_HOST, host));
    }


    /**
     * Hold the lock if started only
     */
    @Override
    public boolean holdDistributedLock()
    {
        return this == START;
    }

    @Override
    public String typename()
    {
        return ScheduledAction.class.getSimpleName();
    }

    @Override
    public ErrorCodes getErrorCode()
    {
        switch (this)
        {
        case START:
            return ErrorCodes.CANNOT_START_JOB_SCHEDULER;
        case STOP:
            return ErrorCodes.CANNOT_STOP_JOB_SCHEDULER;
        case UPDATE:
            return ErrorCodes.CANNOT_UPDATE_JOB_SCHEDULER;
        case DELETE:
            return ErrorCodes.CANNOT_DELETE_JOB_SCHEDULER;
        default:
            // not needed only here to keep the compiler happy
            return ErrorCodes.CANNOT_START_JOB_SCHEDULER;
        }
    }
}
