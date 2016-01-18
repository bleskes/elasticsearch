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

package com.prelert.job.manager;

import com.prelert.job.JobException;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.messages.Messages;

public class CannotStartSchedulerWhileItIsStoppingException extends JobException
{
    private static final long serialVersionUID = -2359537142811349135L;

    public CannotStartSchedulerWhileItIsStoppingException(String jobId)
    {
        super(Messages.getMessage(Messages.JOB_SCHEDULER_CANNOT_START_WHILE_STOPPING, jobId),
                ErrorCodes.BUCKET_RESET_NOT_SUPPORTED);
    }
}
