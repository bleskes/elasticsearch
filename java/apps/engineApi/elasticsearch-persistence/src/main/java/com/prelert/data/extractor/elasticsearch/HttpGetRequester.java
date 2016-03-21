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
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.log4j.Logger;


/**
 * Gets data from an HTTP or HTTPS URL by sending a request body.
 * HTTP or HTTPS is deduced from the supplied URL.
 * Invalid certificates are tolerated for HTTPS access, similar to "curl -k".
 */
class HttpGetRequester
{
    private static final Logger LOGGER = Logger.getLogger(HttpGetRequester.class);

    private static final String TLS = "TLS";
    private static final String GET = "GET";
    private static final String AUTH_HEADER = "Authorization";

    private static final SSLSocketFactory TRUSTING_SOCKET_FACTORY;
    private static final HostnameVerifier TRUSTING_HOSTNAME_VERIFIER;

    /**
     * Hostname verifier that ignores hostname discrepancies.
     */
    private static final class NoOpHostnameVerifier implements HostnameVerifier
    {
        public boolean verify(String hostname, SSLSession session)
        {
            return true;
        }
    }

    /**
     * Certificate trust manager that ignores certificate issues.
     */
    private static final class NoOpTrustManager implements X509TrustManager
    {
        private static final X509Certificate[] EMPTY_CERTIFICATE_ARRAY = new X509Certificate[0];

        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException
        {
            // Ignore certificate problems
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException
        {
            // Ignore certificate problems
        }

        public X509Certificate[] getAcceptedIssuers()
        {
            return EMPTY_CERTIFICATE_ARRAY;
        }
    }

    static
    {
        SSLSocketFactory trustingSocketFactory = null;
        try
        {
            SSLContext sslContext = SSLContext.getInstance(TLS);
            sslContext.init(null, new TrustManager[]{ new NoOpTrustManager() }, null);
            trustingSocketFactory = sslContext.getSocketFactory();
        }
        catch (KeyManagementException | NoSuchAlgorithmException e)
        {
            LOGGER.warn("Unable to set up trusting socket factory", e);
        }

        TRUSTING_SOCKET_FACTORY = trustingSocketFactory;
        TRUSTING_HOSTNAME_VERIFIER = new NoOpHostnameVerifier();
    }

    public HttpGetResponse get(String url, String authHeader, String requestBody) throws IOException
    {
        URL urlObject = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();
        // TODO: we could add a config option to allow users who want to
        // rigorously enforce valid certificates to do so
        if (connection instanceof HttpsURLConnection)
        {
            // This is the equivalent of "curl -k", i.e. tolerate connecting to
            // an Elasticsearch with a self-signed certificate or a certificate
            // that doesn't match its hostname.
            HttpsURLConnection httpsConnection = (HttpsURLConnection)connection;
            if (TRUSTING_SOCKET_FACTORY != null)
            {
                httpsConnection.setSSLSocketFactory(TRUSTING_SOCKET_FACTORY);
            }
            httpsConnection.setHostnameVerifier(TRUSTING_HOSTNAME_VERIFIER);
        }
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
