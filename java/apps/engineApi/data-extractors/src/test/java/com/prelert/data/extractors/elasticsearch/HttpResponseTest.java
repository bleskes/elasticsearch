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

package com.prelert.data.extractors.elasticsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.prelert.data.extractors.elasticsearch.HttpResponse;

public class HttpResponseTest
{
    @Test
    public void testGetResponseAsStream() throws IOException
    {
        InputStream stream = new ByteArrayInputStream("foo\nbar".getBytes(StandardCharsets.UTF_8));
        HttpResponse response = new HttpResponse(stream, 200);

        assertEquals("foo\nbar", response.getResponseAsString());
        assertEquals(200, response.getResponseCode());
    }

    @Test
    public void testGetResponseAsStream_GivenStreamThrows() throws IOException
    {
        InputStream stream = mock(InputStream.class);
        HttpResponse response = new HttpResponse(stream, 200);

        try
        {
            response.getResponseAsString();
            fail();
        }
        catch (UncheckedIOException e)
        {
            verify(stream).close();
        }
    }
}
