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

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.process.exceptions.DataUploadException;
import com.prelert.rs.data.ApiError;

/**
 * Exception -> Response mapper for {@linkplain DataUploadException}.
 */
public class DataUploadExceptionMapper implements ExceptionMapper<DataUploadException>
{
    @Override
    public Response toResponse(DataUploadException e)
    {
        ApiError error = new ApiError(ErrorCodes.DATA_ERROR);
        error.setCause(e.getCause().toString());
        error.setMessage(e.getMessage());
        return Response.serverError()
                        .type(MediaType.APPLICATION_JSON)
                        .entity(error.toJson()).build();
    }
}
