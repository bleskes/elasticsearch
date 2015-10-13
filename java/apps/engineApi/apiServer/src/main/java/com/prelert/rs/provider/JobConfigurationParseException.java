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

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.errorcodes.HasErrorCode;
import com.prelert.rs.data.ApiError;

/**
 * Represents an error in parsing the configuration for a new job.
 * Returns a 400 Bad Request status code and a message.
 */
public class JobConfigurationParseException extends WebApplicationException implements HasErrorCode
{
    private static final long serialVersionUID = -7189040309467301076L;

    private final ErrorCodes m_ErrorCode;

    public JobConfigurationParseException(String message, Throwable cause)
    {
        super(message, cause);
        m_ErrorCode = ErrorCodes.JOB_CONFIG_PARSE_ERROR;
    }


    public JobConfigurationParseException(String message, Throwable cause,
            ErrorCodes errorCode)
    {
        super(message, cause);
        m_ErrorCode = errorCode;
    }

    @Override
    public Response getResponse()
    {
        ApiError err = new ApiError(m_ErrorCode);
        err.setMessage(this.getMessage());
        err.setCause(this.getCause().toString());

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(err.toJson()).build();
    }

    @Override
    public ErrorCodes getErrorCode()
    {
        return m_ErrorCode;
    }
}
