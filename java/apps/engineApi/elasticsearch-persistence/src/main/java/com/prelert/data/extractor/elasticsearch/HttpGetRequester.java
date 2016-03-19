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

package com.prelert.data.extractor.elasticsearch;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

class HttpGetRequester
{
    private static final String GET = "GET";
    private static final String AUTH_HEADER = "Authorization";

    static
    {
        // This is the equivalent of "curl -k", i.e. tolerate connecting to an
        // Elasticsearch with a certificate that doesn't match its hostname.
        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session)
            {
                return true;
            }
        });
    }

    public HttpGetResponse get(String url, String authHeader, String requestBody) throws IOException
    {
        URL urlObject = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();
        connection.setRequestMethod(GET);
        if (authHeader != null)
        {
            connection.setRequestProperty(AUTH_HEADER, authHeader);
        }
        connection.setDoOutput(true);
        writeRequestBody(requestBody, connection);
        return new HttpGetResponse(connection.getInputStream(), connection.getResponseCode());
    }

    private static void writeRequestBody(String requestBody, HttpURLConnection connection)
            throws IOException
    {
        DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream());
        dataOutputStream.writeBytes(requestBody);
        dataOutputStream.flush();
        dataOutputStream.close();
    }
}
