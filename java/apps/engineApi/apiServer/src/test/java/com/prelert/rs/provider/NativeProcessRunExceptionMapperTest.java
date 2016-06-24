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

import static org.junit.Assert.*;

import java.io.IOException;

import javax.ws.rs.core.Response;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.process.exceptions.NativeProcessRunException;
import com.prelert.rs.data.ApiError;

public class NativeProcessRunExceptionMapperTest
{
    @Test
    public void testToResponse_onlyHasMessage() throws JsonProcessingException, IOException
    {
        NativeProcessRunException e = new NativeProcessRunException("blah");

        Response response = new NativeProcessRunExceptionMapper().toResponse(e);

        assertEquals(500, response.getStatus());

        ObjectReader reader = new ObjectMapper()
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false).reader()
                .forType(ApiError.class);
        ApiError apiError = reader.readValue((String) response.getEntity());

        assertEquals("blah", apiError.getMessage());
        assertEquals(ErrorCodes.NATIVE_PROCESS_ERROR, apiError.getErrorCode());
        assertNull(apiError.getCause());
    }

    @Test
    public void testToResponse_causeAndErrorCode() throws JsonProcessingException, IOException
    {
        NativeProcessRunException e = new NativeProcessRunException("blah", ErrorCodes.DATA_ERROR,
                    new IllegalArgumentException("Cause Exception"));

        Response response = new NativeProcessRunExceptionMapper().toResponse(e);

        assertEquals(500, response.getStatus());

        ObjectReader reader = new ObjectMapper()
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false).reader()
                .forType(ApiError.class);
        ApiError apiError = reader.readValue((String) response.getEntity());

        assertEquals("blah", apiError.getMessage());
        assertEquals(ErrorCodes.DATA_ERROR, apiError.getErrorCode());
        assertEquals("java.lang.IllegalArgumentException: Cause Exception", apiError.getCause());
    }
}
