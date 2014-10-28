/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2012     *
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
 ***********************************************************/


package com.prelert.proxy.plugin.splunk;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


/**
 * Opens a http connection. 
 */
public class SplunkConnectionManager 
{
	static public HttpURLConnection openConnection(URL baseUrl, String path) 
	throws MalformedURLException, IOException
	{
		URL url = new URL(baseUrl, path);
		URLConnection urlConnection = url.openConnection();
		
		HttpURLConnection httpConnection;
		if (urlConnection instanceof HttpURLConnection)
		{
			httpConnection = (HttpURLConnection)urlConnection;
		}
		else
		{
			throw new IllegalArgumentException("Url is not http.");
		}
		
        if (httpConnection instanceof HttpsURLConnection) 
        {
            setUpSSLCertificates((HttpsURLConnection)httpConnection);
        }	
        
        return httpConnection;
	}
	
	
	
    static void setUpSSLCertificates(HttpsURLConnection connection)
    {
        connection.setHostnameVerifier(new HostnameVerifier() 
        		{
					@Override
				    public boolean verify(String hostname, SSLSession session) 
					{
						return true;
					}
        		});
        
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() 
	        {
	            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
	                                return null;
	            }
	            public void checkClientTrusted(
	                java.security.cert.X509Certificate[] certs, String authType) {
	            }
	            public void checkServerTrusted(
	                java.security.cert.X509Certificate[] certs, String authType) {
	            }
	        }};
        
        // Install the all-trusting trust manager
        SSLContext sc;
        try {
            sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException(e);
        }
        catch (KeyManagementException e) 
        {
            throw new RuntimeException(e);
        }
        connection.setSSLSocketFactory(sc.getSocketFactory());
    } 
}
