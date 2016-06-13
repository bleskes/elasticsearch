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

import java.util.HashSet;
import java.util.Set;

import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.messages.Messages;

/**
 * Job actions
 */
public enum Action implements ActionState<Action>
{
    CLOSED("", Messages.PROCESS_ACTION_CLOSED_JOB),
    SLEEPING("", Messages.PROCESS_ACTION_SLEEPING_JOB, ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR, true),
    CLOSING(Messages.JOB_DATA_CONCURRENT_USE_CLOSE, Messages.PROCESS_ACTION_CLOSING_JOB),
    DELETING(Messages.JOB_DATA_CONCURRENT_USE_DELETE, Messages.PROCESS_ACTION_DELETING_JOB),
    FLUSHING(Messages.JOB_DATA_CONCURRENT_USE_FLUSH, Messages.PROCESS_ACTION_FLUSHING_JOB),
    PAUSING(Messages.JOB_DATA_CONCURRENT_USE_PAUSE, Messages.PROCESS_ACTION_PAUSING_JOB, ErrorCodes.CANNOT_PAUSE_JOB),
    RESUMING(Messages.JOB_DATA_CONCURRENT_USE_RESUME, Messages.PROCESS_ACTION_RESUMING_JOB, ErrorCodes.CANNOT_RESUME_JOB),
    REVERTING(Messages.JOB_DATA_CONCURRENT_USE_REVERT, Messages.PROCESS_ACTION_REVERTING_JOB),
    UPDATING(Messages.JOB_DATA_CONCURRENT_USE_UPDATE, Messages.PROCESS_ACTION_UPDATING_JOB),
    WRITING(Messages.JOB_DATA_CONCURRENT_USE_UPLOAD, Messages.PROCESS_ACTION_WRITING_JOB);

    private final String m_MessageKey;
    private final String m_VerbKey;
    private final boolean m_KeepDistributedLock;
    private final ErrorCodes m_ErrorCode;

    /**
     * The set of valid transitions from SLEEPING
     */
    private static final Set<Action> VALID_WHEN_SLEEPING = new HashSet<>();

    static
    {
        VALID_WHEN_SLEEPING.add(UPDATING);
        VALID_WHEN_SLEEPING.add(FLUSHING);
        VALID_WHEN_SLEEPING.add(CLOSING);
        VALID_WHEN_SLEEPING.add(DELETING);
        VALID_WHEN_SLEEPING.add(WRITING);
        VALID_WHEN_SLEEPING.add(PAUSING);
    }

    /**
     * The initial action state
     * @return
     */
    public static Action startingState()
    {
        return CLOSED;
    }

    private Action(String messageKey, String verbKey)
    {
        this(messageKey, verbKey, ErrorCodes.NATIVE_PROCESS_CONCURRENT_USE_ERROR, false);
    }

    private Action(String messageKey, String verbKey, ErrorCodes errorCode)
    {
        this(messageKey, verbKey, errorCode, false);
    }

    private Action(String messageKey, String verbKey, ErrorCodes errorCode,
                boolean keepDistributedLock)
    {
        m_MessageKey = messageKey;
        m_VerbKey = verbKey;
        m_KeepDistributedLock = keepDistributedLock;
        m_ErrorCode = errorCode;
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
    public String getBusyActionError(String jobId, ActionState<Action> actionInUse)
    {
        return Messages.getMessage(getMessageKey(), jobId,
                            Messages.getMessage(actionInUse.getActionVerb()), "");
    }

    @Override
    public String getBusyActionError(String jobId, ActionState<Action> actionInUse, String host)
    {
        // host needs a single white space appended to be formatted properly.
        // Review if the message string changes
        return Messages.getMessage(getMessageKey(),
                                jobId,
                                Messages.getMessage(actionInUse.getActionVerb()),
                                Messages.getMessage(Messages.ON_HOST, host + " "));
    }

    @Override
    public ErrorCodes getErrorCode()
    {
        return m_ErrorCode;
    }

    /**
     * If this state is NONE or CLOSED then any next state is valid.
     *
     * If the job is sleeping i.e the process is running but not
     * handling data some transitions are valid
     */
    @Override
    public boolean isValidTransition(Action next)
    {
        if (this == CLOSED)
        {
            return true;
        }

        if (this == SLEEPING)
        {
            return VALID_WHEN_SLEEPING.contains(next);
        }

        return false;
    }

    @Override
    public Action nextState(Action previousState)
    {
        if (this == UPDATING)
        {
            return previousState;
        }

        if (this == SLEEPING || this == FLUSHING || this == WRITING)
        {
            return SLEEPING;
        }

        return CLOSED;
    }

    /*
     * Hold the lock when sleeping
     * @see com.prelert.job.manager.actions.ActionState#holdDistributedLock()
     */
    @Override
    public boolean holdDistributedLock()
    {
        return m_KeepDistributedLock;
    }

    @Override
    public String typename()
    {
        return Action.class.getSimpleName();
    }
}
