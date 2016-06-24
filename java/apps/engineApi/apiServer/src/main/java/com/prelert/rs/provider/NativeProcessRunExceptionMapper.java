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

package com.prelert.rs.provider;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.rs.data.ApiError;

/**
 * Exception -> Response mapper for {@linkplain NativeProcessRunException}.
 *
 * Different to the JobExceptionMapper as it returns a
 * 500 Internal Server Error HTTP status code
 */
public class NativeProcessRunExceptionMapper implements ExceptionMapper<NativeProcessRunException>
{

    @Override
    public Response toResponse(NativeProcessRunException e)
    {
        ApiError error = new ApiError(e.getErrorCode());
        if (e.getCause() != null)
        {
            error.setCause(e.getCause().toString());
        }
        error.setMessage(e.getMessage());

        return Response.serverError()
                .type(MediaType.APPLICATION_JSON)
                .entity(error.toJson()).build();
    }
}
