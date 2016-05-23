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

import com.prelert.job.messages.Messages;

/**
 * Error reporting functions for actions
 */
public interface ActionErrorMessage
{
    /**
     * Description of the actions activity
     * @return
     */
    String getActionVerb();

    String getMessageKey();

    /**
     * create error message saying that the action cannot be
     * started because another action <code>otherActionVerb</code>
     * is already running
     *
     * @param jobId
     * @param actionInUse The Action currently in progress
     * @return
     */
    default String getBusyActionError(String jobId, ActionErrorMessage actionInUse)
    {
        return Messages.getMessage(actionInUse.getMessageKey(), jobId,
                            Messages.getMessage(actionInUse.getActionVerb()), "");
    }

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
    default String getBusyActionError(String jobId, ActionErrorMessage actionInUse, String host)
    {
        // host needs a single white space appended to be formatted properly.
        // Review if the message string changes
        return Messages.getMessage(actionInUse.getMessageKey(),
                                jobId,
                                Messages.getMessage(actionInUse.getActionVerb()),
                                Messages.getMessage(Messages.ON_HOST, host + " "));
    }

}
