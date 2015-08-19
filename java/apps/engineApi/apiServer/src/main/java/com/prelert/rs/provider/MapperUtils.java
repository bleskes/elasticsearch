/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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
package com.prelert.rs.provider;

import com.prelert.job.JobException;
import com.prelert.job.UnknownJobException;
import com.prelert.job.messages.Messages;
import com.prelert.rs.data.ApiError;

public class MapperUtils
{
    private MapperUtils()
    {

    }

    /**
     * Static method to create an {@link ApiError} from a {@linkplain JobException}
     * @param e
     * @return
     */
    public static ApiError apiErrorFromJobException(JobException e)
    {
        ApiError error = new ApiError(e.getErrorCode());
        error.setCause(e.getCause());
        error.setMessage(e.getMessage());

        if (e.getMessage() == null || e.getMessage().isEmpty())
        {
            if (e instanceof UnknownJobException)
            {
                // set the default message
                UnknownJobException uje = (UnknownJobException)e;
                error.setMessage(Messages.getMessage(Messages.JOB_UNKNOWN_ID, uje.getJobId()));
            }

        }

        return error;
    }
}
