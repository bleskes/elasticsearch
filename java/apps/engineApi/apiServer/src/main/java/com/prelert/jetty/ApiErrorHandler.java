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

import java.io.IOException;
import java.io.Writer;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.ErrorHandler;

import com.fasterxml.jackson.core.io.JsonStringEncoder;

/**
 * A Servlet error handler that overrides the default Jetty
 * handler which returns Jetty branded HTML.  Instead of this
 * HTML we return some JSON in the same format as our
 * documented error responses.
 *
 * This handler can get called in two situations:
 * 1) A bug in the Jetty code
 * 2) An unchecked exception being thrown by our code
 *
 * Testing shows that in case (2) the reason is not particularly
 * helpful and the cause is sometimes null, so this is not
 * great.  However, it's better than being completely silent
 * when an unexpected error occurs in our code.  Also, in case
 * (1) the output will be more informative.
 */
public class ApiErrorHandler extends ErrorHandler
{
    private static final Logger LOGGER = Logger.getLogger(ApiErrorHandler.class);

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request,
            HttpServletResponse response)
    throws IOException
    {
        JsonStringEncoder encoder = JsonStringEncoder.getInstance();

        StringBuilder builder = new StringBuilder();
        builder.append('{');

        if (response instanceof Response)
        {
            Response serverResponse = (Response)response;
            String reason = serverResponse.getReason();
            if (reason != null && !reason.isEmpty())
            {
                LOGGER.error("Unexpected server failure: " + reason);

                char [] message = encoder.quoteAsString(reason);
                builder.append("\n  \"message\" : \"").append(message).append("\",");
            }
        }

        // We don't have a clue what caused the underlying error in this case,
        // so just set the error code to 0
        builder.append("\n  \"errorCode\" : 0");

        Throwable th = (Throwable)request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        if (th != null)
        {
            LOGGER.error("Unexpected server exception was thrown", th);

            char [] cause = encoder.quoteAsString(th.toString());
            builder.append(",\n  \"cause\" : \"").append(cause).append('"');
        }

        builder.append("\n}\n");

        String errorText = builder.toString();

        response.setContentType(MediaType.APPLICATION_JSON);
        response.setContentLength(errorText.length());
        Writer writer = response.getWriter();
        writer.write(errorText);
        writer.flush();
    }

}
