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

package com.prelert.master.streaming;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.eclipse.jetty.client.util.OutputStreamContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;


public class HttpDataStreamer implements AutoCloseable
{
    private static final Logger LOGGER = Logger.getLogger(HttpDataStreamer.class);
    private final String m_BaseUrl;
    private final HttpClient m_HttpClient;

    public HttpDataStreamer(String baseUrl)
    {
        m_BaseUrl = baseUrl;
        m_HttpClient = new HttpClient();
        try
        {
            m_HttpClient.start();
        }
        catch (Exception e)
        {
            LOGGER.fatal("Failed to start the HTTP client", e);
        }
    }

    public OutputStream openStream(String jobId)
    {
        String postUrl = String.format("%s/data/%s", m_BaseUrl, encode(jobId));

        OutputStreamContentProvider content = new OutputStreamContentProvider();

        OutputStream output = content.getOutputStream();


        BufferingResponseListener responseListener = new BufferingResponseListener()
        {
            @Override
            public void onComplete(Result result)
            {
                LOGGER.info("content complete for job " + jobId);
                LOGGER.info(result.getResponse());
                LOGGER.info("Content=" + this.getContentAsString());
            }
        };

        m_HttpClient.POST(postUrl)
            .content(content)
            .header(HttpHeader.CONTENT_TYPE, "application/octet-stream")
            .send(responseListener);

        return output;
    }

    public boolean closeJob(String jobId)
    throws IOException
    {
        // Send finish message
        String closeUrl = m_BaseUrl + "/data/" + encode(jobId) + "/close";
        LOGGER.debug("Closing job " + closeUrl);

        try
        {
            ContentResponse response =  m_HttpClient.POST(closeUrl).send();
            String content = response.getContentAsString();

            if (response.getStatus() != HttpStatus.ACCEPTED_202
                    && response.getStatus() != HttpStatus.OK_200)
            {
                String msg = String.format(
                        "Error closing job %s, status code = %d. Returned content: %s",
                        jobId, response.getStatus(), content);
                LOGGER.error(msg);
                return false;
            }
        }
        catch (InterruptedException | TimeoutException | ExecutionException e)
        {
            LOGGER.error("An error occurred while executing an HTTP request", e);
            throw new IOException(e);
        }

        return true;
    }


    private String encode(String s)
    {
        try
        {
            return URLEncoder.encode(s, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            LOGGER.error("Encoding error for " + s + ": " + e.getMessage());
        }
        return "";
    }

    /**
     * Close the http client
     */
    @Override
    public void close()
    {
        try
        {
            m_HttpClient.stop();
        }
        catch (Exception e)
        {
            LOGGER.warn("Error closing HttpClient", e);
        }
    }
}
