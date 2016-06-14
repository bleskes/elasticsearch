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
package com.prelert.job.exceptions;

import com.prelert.job.JobException;
import com.prelert.job.errorcodes.ErrorCodes;

/**
 * This exception is thrown is an operation is attempted on a job
 * that can't be executed as the job is already being used.
 */
public class JobInUseException extends JobException
{
    private static final long serialVersionUID = -2759814168552580059L;

    private String m_Host;

    /**
     * Create a new JobInUseException.
     *
     * @param message Details of error explaining the context
     * @param The error code
     * @see ErrorCodes
     */
    public JobInUseException(String message, ErrorCodes errorCode)
    {
        super(message, errorCode);
    }

    /**
     *
     * @param message Details of error explaining the context
     * @param The error code
     * @param hostname The host the job is running on
     */
    public JobInUseException(String message, ErrorCodes errorCode, String hostname)
    {
        super(message, errorCode);
        m_Host = hostname;
    }

    public JobInUseException(String message, ErrorCodes errorCode, Throwable cause)
    {
        super(message, errorCode, cause);
    }

    public String getHost()
    {
        return m_Host;
    }

    public void setHost(String host)
    {
        this.m_Host = host;
    }
}

