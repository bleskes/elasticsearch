/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.prelert.job.manager.actions;

import org.elasticsearch.xpack.prelert.job.messages.Messages;

import java.util.HashSet;
import java.util.Set;

/**
 * Job actions
 */
public enum Action implements ActionState<Action>
{
    CLOSED("", Messages.PROCESS_ACTION_CLOSED_JOB),
    SLEEPING("", Messages.PROCESS_ACTION_SLEEPING_JOB, true),
    CLOSING(Messages.JOB_DATA_CONCURRENT_USE_CLOSE, Messages.PROCESS_ACTION_CLOSING_JOB),
    DELETING(Messages.JOB_DATA_CONCURRENT_USE_DELETE, Messages.PROCESS_ACTION_DELETING_JOB),
    FLUSHING(Messages.JOB_DATA_CONCURRENT_USE_FLUSH, Messages.PROCESS_ACTION_FLUSHING_JOB),
    PAUSING(Messages.JOB_DATA_CONCURRENT_USE_PAUSE, Messages.PROCESS_ACTION_PAUSING_JOB),
    RESUMING(Messages.JOB_DATA_CONCURRENT_USE_RESUME, Messages.PROCESS_ACTION_RESUMING_JOB),
    REVERTING(Messages.JOB_DATA_CONCURRENT_USE_REVERT, Messages.PROCESS_ACTION_REVERTING_JOB),
    UPDATING(Messages.JOB_DATA_CONCURRENT_USE_UPDATE, Messages.PROCESS_ACTION_UPDATING_JOB),
    WRITING(Messages.JOB_DATA_CONCURRENT_USE_UPLOAD, Messages.PROCESS_ACTION_WRITING_JOB);

    private final String messageKey;
    private final String verbKey;
    private final boolean keepDistributedLock;

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
     */
    public static Action startingState()
    {
        return CLOSED;
    }

    private Action(String messageKey, String verbKey)
    {
        this(messageKey, verbKey, false);
    }

    private Action(String messageKey, String verbKey, boolean keepDistributedLock)
    {
        this.messageKey = messageKey;
        this.verbKey = verbKey;
        this.keepDistributedLock = keepDistributedLock;
    }

    @Override
    public String getActionVerb()
    {
        return verbKey;
    }

    public String getMessageKey()
    {
        return messageKey;
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
     * @see org.elasticsearch.xpack.prelert.job.manager.actions.ActionState#holdDistributedLock()
     */
    @Override
    public boolean holdDistributedLock()
    {
        return keepDistributedLock;
    }

    @Override
    public String typename()
    {
        return Action.class.getSimpleName();
    }
}
