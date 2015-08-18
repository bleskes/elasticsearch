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
package com.prelert.job.exceptions;

import com.prelert.job.JobException;
import com.prelert.job.errorcodes.ErrorCodes;

/**
 * This type of exception represents an error where an operation
 * would result in too many jobs running at the same time.
 */
public class TooManyJobsException extends JobException
{
    private static final long serialVersionUID = 8503362038035845948L;

    private final int m_Limit;

    /**
     * Create a new TooManyJobsException with an error code
     *
     * @param limit The limit on the number of jobs
     * @param message Details of error explaining the context
     * @param errorCode
     */
    public TooManyJobsException(int limit, String message, ErrorCodes errorCode)
    {
        super(message, errorCode);
        m_Limit = limit;
    }


    public TooManyJobsException(int limit, String message, ErrorCodes errorCode,
            Throwable cause)
    {
        super(message, errorCode, cause);
        m_Limit = limit;
    }


    /**
     * Get the limit on the number of concurrently running jobs.
     * @return
     */
    public int getLimit()
    {
        return m_Limit;
    }
}
