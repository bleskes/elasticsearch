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


    T startingState();

    String typename();
}
