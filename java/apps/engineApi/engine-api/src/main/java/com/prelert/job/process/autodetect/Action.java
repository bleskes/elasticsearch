/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
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

package com.prelert.job.process.autodetect;

import com.prelert.job.messages.Messages;

enum Action {
    CLOSING(Messages.PROCESS_ACTION_CLOSING_JOB),
    FLUSHING(Messages.PROCESS_ACTION_FLUSHING_JOB),
    WRITING(Messages.PROCESS_ACTION_WRITING_JOB),
    NONE(Messages.PROCESS_ACTION_USING_JOB);

    private final String m_ErrorString;

    private Action(String messageKey)
    {
        m_ErrorString = Messages.getMessage(messageKey);
    }

    public String getErrorString()
    {
        return m_ErrorString;
    }
}
