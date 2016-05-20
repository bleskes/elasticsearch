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

public enum Action {
    CLOSING(Messages.JOB_DATA_CONCURRENT_USE_CLOSE, Messages.PROCESS_ACTION_CLOSING_JOB),
    DELETING(Messages.JOB_DATA_CONCURRENT_USE_DELETE, Messages.PROCESS_ACTION_DELETING_JOB),
    FLUSHING(Messages.JOB_DATA_CONCURRENT_USE_FLUSH, Messages.PROCESS_ACTION_FLUSHING_JOB),
    PAUSING(Messages.JOB_DATA_CONCURRENT_USE_PAUSE, Messages.PROCESS_ACTION_PAUSING_JOB),
    RESUMING(Messages.JOB_DATA_CONCURRENT_USE_RESUME, Messages.PROCESS_ACTION_RESUMING_JOB),
    REVERTING(Messages.JOB_DATA_CONCURRENT_USE_REVERT, Messages.PROCESS_ACTION_REVERTING_JOB),
    UPDATING(Messages.JOB_DATA_CONCURRENT_USE_UPDATE, Messages.PROCESS_ACTION_UPDATING_JOB),
    WRITING(Messages.JOB_DATA_CONCURRENT_USE_UPLOAD, Messages.PROCESS_ACTION_WRITING_JOB),
    NONE("", Messages.PROCESS_ACTION_UNKNOWN);

    private final String m_MessageKey;
    private final String m_VerbKey;

    private Action(String messageKey, String verbKey)
    {
        m_MessageKey = messageKey;
        m_VerbKey = verbKey;
    }

    public String getErrorString(String jobId, Action actionInUse)
    {
        return Messages.getMessage(m_MessageKey, jobId, Messages.getMessage(actionInUse.m_VerbKey), "");
    }

    public String getErrorString(String jobId, Action actionInUse, String host)
    {
        // host needs a single white space appended to be formatted properly.
        // Review if the message string changes
        return Messages.getMessage(m_MessageKey,
                                jobId,
                                Messages.getMessage(actionInUse.m_VerbKey),
                                Messages.getMessage(Messages.ON_HOST, host + " "));
    }
}
