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
package com.prelert.job;

import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.messages.Messages;
//import com.prelert.job.messages.Messages;

/**
 * This type of exception represents an error where
 * an operation uses a <i>JobId</i> that does not exist.
 */
public class UnknownJobException extends JobException
{
    private static final long serialVersionUID = 8603362038035845948L;

    private final String m_JobId;

    /**
     * Create with the default message and error code
     * set to ErrorCode.MISSING_JOB_ERROR
     * @param jobId
     */
    public UnknownJobException(String jobId)
    {
        super(Messages.getMessage(Messages.JOB_UNKNOWN_ID, jobId), ErrorCodes.MISSING_JOB_ERROR);
        m_JobId = jobId;
    }

    /**
     * Create a new UnknownJobException with an error code
     *
     * @param jobId The Job Id that could not be found
     * @param message Details of error explaining the context
     * @param errorCode
     */
    public UnknownJobException(String jobId, String message, ErrorCodes errorCode)
    {
        super(message, errorCode);
        m_JobId = jobId;
    }

    public UnknownJobException(String jobId, String message, ErrorCodes errorCode,
            Throwable cause)
    {
        super(message, errorCode, cause);
        m_JobId = jobId;
    }

    /**
     * Get the unknown <i>JobId</i> that was the source of the error.
     * @return
     */
    public String getJobId()
    {
        return m_JobId;
    }
}
