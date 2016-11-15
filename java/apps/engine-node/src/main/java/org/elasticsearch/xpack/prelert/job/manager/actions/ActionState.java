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

/**
 *  Functions for actions
 */
public interface ActionState<T>
{
    /**
     * Description of the actions activity
     */
    String getActionVerb();

    /**
     * create error message saying that the action cannot be
     * started because another action <code>otherActionVerb</code>
     * is already running
     *
     * @param jobId the jobId
     * @param actionInUse The Action currently in progress
     */
    String getBusyActionError(String jobId, ActionState<T> actionInUse);

    /**
     * create error message saying that the action cannot be
     * started because another action <code>otherActionVerb</code>
     * is already running on another machine (<code>host</code>)
     *
     * @param jobId the jobId
     * @param actionInUse The Action currently in progress
     * @param host The host the action is currently running on
     */
    String getBusyActionError(String jobId, ActionState<T> actionInUse, String host);


    /**
     * Return true if allowed to transition from the current state to next
     */
    boolean isValidTransition(T next);

    /**
     * The next state to transition to once this state is finished
     *
     * @param previousState If extra context is needed for deciding the next state
     */
    T nextState(T previousState);

    /**
     * Should the action hold the lock in a distributed system
     */
    boolean holdDistributedLock();

    /**
     * Description of the {@code T} type
     */
    String typename();
}
