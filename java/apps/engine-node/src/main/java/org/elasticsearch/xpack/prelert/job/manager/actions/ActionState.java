
package org.elasticsearch.xpack.prelert.job.manager.actions;

import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;

/**
 *  Functions for actions
 */
public interface ActionState<T>
{
    /**
     * Description of the actions activity
     * @return
     */
    String getActionVerb();

    /**
     * create error message saying that the action cannot be
     * started because another action <code>otherActionVerb</code>
     * is already running
     *
     * @param jobId
     * @param actionInUse The Action currently in progress
     * @return
     */
    String getBusyActionError(String jobId, ActionState<T> actionInUse);

    /**
     * create error message saying that the action cannot be
     * started because another action <code>otherActionVerb</code>
     * is already running on another machine (<code>host</code>)
     *
     * @param jobId
     * @param actionInUse The Action currently in progress
     * @param host The host the action is currently running on
     * @return
     */
    String getBusyActionError(String jobId, ActionState<T> actionInUse, String host);


    /**
     * Return true if allowed to transition from the current state to next
     *
     * @param next
     * @return
     */
    boolean isValidTransition(T next);

    /**
     * The next state to transition to once this state is finished
     *
     * @param previousState If extra context is needed for deciding the next state
     * @return
     */
    T nextState(T previousState);

    /**
     * Should the action hold the lock in a distributed system
     * @return
     */
    boolean holdDistributedLock();

    /**
     * Description of the {@code T} type
     * @return
     */
    String typename();

    /**
     * The error code associated with the failure to perform the action
     * @return
     */
    ErrorCodes getErrorCode();
}
