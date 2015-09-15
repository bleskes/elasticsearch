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

package com.prelert.jetty;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.rs.data.ApiError;

public class ApiErrorHandlerTest
{
    @Mock private Request m_BaseRequest;
    @Mock private HttpServletRequest m_Request;

    private final ApiErrorHandler m_ApiErrorHandler = new ApiErrorHandler();

    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testHandle_GivenNonJettyResponseWithoutCause() throws IOException
    {
        HttpServletResponse response = mock(HttpServletResponse.class);

        ApiError apiError = handle(response);

        assertNull(apiError.getMessage());
        assertNull(apiError.getCause());
        assertEquals(ErrorCodes.UNKNOWN_ERROR, apiError.getErrorCode());
    }

    @Test
    public void testHandle_GivenNonJettyResponseWithCause() throws IOException
    {
        HttpServletResponse response = mock(HttpServletResponse.class);
        Throwable throwable = new Throwable("A fake cause");
        when(m_Request.getAttribute(RequestDispatcher.ERROR_EXCEPTION)).thenReturn(throwable);

        ApiError apiError = handle(response);

        assertNull(apiError.getMessage());
        assertTrue(apiError.getCause().toString().contains("A fake cause"));
        assertEquals(ErrorCodes.UNKNOWN_ERROR, apiError.getErrorCode());
    }

    @Test
    public void testHandle_GivenJettyResponseWithNullReasonAndNoCause() throws IOException
    {
        Response response = mock(Response.class);
        when(response.getReason()).thenReturn(null);

        ApiError apiError = handle(response);

        assertNull(apiError.getMessage());
        assertNull(apiError.getCause());
        assertEquals(ErrorCodes.UNKNOWN_ERROR, apiError.getErrorCode());
    }

    @Test
    public void testHandle_GivenJettyResponseWithEmptyReasonAndNoCause() throws IOException
    {
        Response response = mock(Response.class);
        when(response.getReason()).thenReturn("");

        ApiError apiError = handle(response);

        assertNull(apiError.getMessage());
        assertNull(apiError.getCause());
        assertEquals(ErrorCodes.UNKNOWN_ERROR, apiError.getErrorCode());
    }

    @Test
    public void testHandle_GivenJettyResponseWithReasonAndNoCause() throws IOException
    {
        Response response = mock(Response.class);
        when(response.getReason()).thenReturn("A fake reason");

        ApiError apiError = handle(response);

        assertEquals("A fake reason", apiError.getMessage());
        assertNull(apiError.getCause());
        assertEquals(ErrorCodes.UNKNOWN_ERROR, apiError.getErrorCode());
    }

    @Test
    public void testHandle_GivenJettyResponseWithReasonAndCause() throws IOException
    {
        Response response = mock(Response.class);
        when(response.getReason()).thenReturn("Another fake reason");
        Throwable throwable = new Throwable("Another fake cause");
        when(m_Request.getAttribute(RequestDispatcher.ERROR_EXCEPTION)).thenReturn(throwable);

        ApiError apiError = handle(response);

        assertEquals("Another fake reason", apiError.getMessage());
        assertTrue(apiError.getCause().toString().contains("Another fake cause"));
        assertEquals(ErrorCodes.UNKNOWN_ERROR, apiError.getErrorCode());
    }

    /**
     * Helper method that attaches a StringWriter to the response,
     * calls handle on the {@code m_ApiErrorHandler}, parses the
     * text of the response as an {@link ApiError} and returns it.
     */
    private ApiError handle(HttpServletResponse response) throws IOException
    {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        m_ApiErrorHandler.handle("foo", m_BaseRequest, m_Request, response);

        verify(response).setContentType("application/json");

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(stringWriter.toString(), ApiError.class);
    }
}
