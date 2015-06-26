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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.errorcodes.HasErrorCode;
import com.prelert.rs.data.ApiError;

/**
 * Overrides the default {@linkplain WebApplicationException}
 * response to include the error code and message
 */
public class RestApiException extends WebApplicationException implements HasErrorCode
{
    private static final long serialVersionUID = -4162139513941557651L;

    private final ErrorCodes m_ErrorCode;
    private final Response.Status m_Status;

    public RestApiException(String msg, ErrorCodes errorCode, Response.Status status)
    {
        super(msg);
        m_ErrorCode = errorCode;
        m_Status = status;
    }

    @Override
    public Response getResponse()
    {
        ApiError error = new ApiError(m_ErrorCode);
        error.setMessage(this.getMessage());

        return Response.status(m_Status)
                .type(MediaType.APPLICATION_JSON)
                .entity(error.toJson()).build();
    }

    @Override
    public ErrorCodes getErrorCode()
    {
        return m_ErrorCode;
    }
}
