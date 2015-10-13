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
import com.prelert.rs.data.ApiError;

public final class MapperUtils
{
    private MapperUtils()
    {

    }

    /**
     * Static method to create an {@link ApiError} from a {@linkplain JobException}<br>
     *
     * If the exception does not have a message string and it is an instance
     * of {@link UnknownJobException} then this function sets the default
     * error message
     *
     * @param e
     * @return
     */
    public static ApiError apiErrorFromJobException(JobException jobException)
    {
        ApiError error = new ApiError(jobException.getErrorCode());
        if (jobException.getCause() != null)
        {
            error.setCause(jobException.getCause().toString());
        }
        error.setMessage(jobException.getMessage());
        return error;
    }
}
