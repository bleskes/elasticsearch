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

package com.prelert.rs.provider;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.ws.rs.core.Response;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.prelert.job.UnknownJobException;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobConfigurationException;
import com.prelert.rs.data.ApiError;

public class JobExceptionMapperTest
{
    private final JobExceptionMapper m_Mapper = new JobExceptionMapper();

    @Test
    public void testToResponse() throws JsonProcessingException, IOException
    {
        Response response = m_Mapper.toResponse(new JobConfigurationException("foobar",
                ErrorCodes.JOB_ID_TOO_LONG));

        assertEquals(400, response.getStatus());

        ObjectReader reader = new ObjectMapper()
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false).reader()
                .forType(ApiError.class);
        ApiError apiError = reader.readValue((String) response.getEntity());

        assertEquals("foobar", apiError.getMessage());
        assertEquals(ErrorCodes.JOB_ID_TOO_LONG, apiError.getErrorCode());
    }

    @Test
    public void testToResponse_GivenUnknownJobException() throws JsonProcessingException, IOException
    {
        Response response = m_Mapper.toResponse(new UnknownJobException("foo"));

        assertEquals(404, response.getStatus());

        ObjectReader reader = new ObjectMapper()
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false).reader()
                .forType(ApiError.class);
        ApiError apiError = reader.readValue((String) response.getEntity());

        assertEquals("No known job with id 'foo'", apiError.getMessage());
        assertEquals(ErrorCodes.MISSING_JOB_ERROR, apiError.getErrorCode());
    }
}
