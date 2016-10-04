
package org.elasticsearch.xpack.prelert.job.manager.actions;

import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;

/**
 * Actions relating to the scheduler
 */
public enum ScheduledAction implements ActionState<ScheduledAction>
{
    STARTED(Messages.JOB_SCHEDULER_CANNOT_START, Messages.JOB_SCHEDULER_STATUS_STARTED),
    STOPPING(Messages.JOB_SCHEDULER_CANNOT_STOP_IN_CURRENT_STATE, Messages.JOB_SCHEDULER_STATUS_STOPPING),
    STOPPED(Messages.JOB_SCHEDULER_CANNOT_STOP_IN_CURRENT_STATE, Messages.JOB_SCHEDULER_STATUS_STOPPED),
    DELETE(Messages.JOB_SCHEDULER_CANNOT_DELETE_IN_CURRENT_STATE, Messages.JOB_SCHEDULER_STATUS_DELETING),
    UPDATE(Messages.JOB_SCHEDULER_CANNOT_UPDATE_IN_CURRENT_STATE, Messages.JOB_SCHEDULER_STATUS_UPDATING);

    private final String messageKey;
    private final String verbKey;

    public static ScheduledAction startingState()
    {
        return STOPPED;
    }

    private ScheduledAction(String messageKey, String verbKey)
    {
        this.messageKey = messageKey;
        this.verbKey = verbKey;
    }

    @Override
    public boolean isValidTransition(ScheduledAction next)
    {
        switch (next)
        {
            case STARTED:
                return this == STOPPED;
            case UPDATE:
                return this == STOPPED;
            case STOPPING:
                return this == STARTED || this == STOPPED;
            case STOPPED:
                return this == STOPPING || this == UPDATE || this == DELETE;
            case DELETE:
                return this == STARTED || this == STOPPED;
            default:
                return false;
        }
    }

    @Override
    public ScheduledAction nextState(ScheduledAction previousState)
    {
        return STOPPED;
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
     * Hold the lock if started or stopping
     */
    @Override
    public boolean holdDistributedLock()
    {
        return this == STARTED || this == STOPPING;
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
        case STARTED:
            return ErrorCodes.CANNOT_START_JOB_SCHEDULER;
        case STOPPING:
            return ErrorCodes.CANNOT_STOP_JOB_SCHEDULER;
        case STOPPED:
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
