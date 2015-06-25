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

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;

import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prelert.job.DataCounts;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.rs.data.ApiError;
import com.prelert.rs.data.DataPostResult;
import com.prelert.rs.data.MultiDataPostResult;

public class MultiDataPostResultWriterTest
{
    @Test
    public void testIsWritable()
    {
        MultiDataPostResultWriter writer = new MultiDataPostResultWriter();

        assertFalse(writer.isWriteable(String.class, mock(Type.class), null, null));
        assertTrue(writer.isWriteable(
                MultiDataPostResult.class, mock(ParameterizedType.class), null, null));
    }

    @Test
    public void testSerialise() throws WebApplicationException, IOException
    {
        MultiDataPostResult result = createResults();
        MultiDataPostResultWriter writer = new MultiDataPostResultWriter();

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        writer.writeTo(result, MultiDataPostResult.class, mock(Type.class),
                new Annotation[] {}, MediaType.APPLICATION_JSON_TYPE,
                new MultivaluedHashMap<String, Object>(),
                output);

        String content = new String(output.toByteArray());

        ObjectMapper jsonMapper = new ObjectMapper();
        MultiDataPostResult out = jsonMapper.readValue(content, new TypeReference<MultiDataPostResult>() {} );

        assertEquals(result, out);
    }


    private MultiDataPostResult createResults()
    {
        MultiDataPostResult results = new MultiDataPostResult();

        DataPostResult result = new DataPostResult();
        DataCounts dc = new DataCounts();
        dc.setInputBytes(1000L);
        result.setJobId("foo");
        result.setUploadSummary(dc);
        results.addResult(result);

        DataPostResult errorResult = new DataPostResult();
        ApiError error = new ApiError(ErrorCodes.BUCKET_RESET_NOT_SUPPORTED);
        error.setMessage("bar");
        errorResult.setJobId("foo");
        errorResult.setError(error);
        results.addResult(errorResult);

        return results;
    }

}
