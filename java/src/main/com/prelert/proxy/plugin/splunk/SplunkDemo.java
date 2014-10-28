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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.prelert.proxy.plugin.splunk.SplunkAuthManager.SessionKey;
import com.prelert.proxy.plugin.splunk.SplunkQueryManager.JobId;


public class SplunkDemo 
{
	static Logger s_Logger = Logger.getLogger(SplunkDemo.class);
	
	
    static Document executeRequest(String url, String sessionKey, String args, String method) {
        try {
                HttpURLConnection conn = (HttpURLConnection)(new URL(url).openConnection());
                if (conn instanceof HttpsURLConnection) {
                    setUpSSLCertificates((HttpsURLConnection)conn);
                }
                conn.setDoInput(true);
                conn.setDoOutput(true);
                if (sessionKey!=null) {
                    conn.setRequestProperty("Authorization", "Splunk " + sessionKey);
                }
                
                if (method.equals("POST")) {
                    try {
                        conn.setRequestMethod(method);
                    } catch (ProtocolException e) {
                        throw new IllegalArgumentException(e);
                    }
                    conn.setRequestProperty("Content-Type",
                                "application/x-www-form-urlencoded");
                                OutputStream os = conn.getOutputStream();
                                os.write(args.getBytes("utf-8"));
                    if (os != null) os.close();
                }
                                
                try {
                    InputSource is = new InputSource();
                    InputStream istr = conn.getInputStream();
                    if(conn.getResponseCode() == 204 || istr ==null || istr.available() == 0) return null;
                    is.setCharacterStream(new BufferedReader(new InputStreamReader(istr)));
                    return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
                } catch (ParserConfigurationException e) {
                    throw new RuntimeException(e.getMessage());
                } catch (SAXException e) {
                    throw new RuntimeException(e.getMessage());
                }
        } catch (IOException e) {
                throw new RuntimeException("Error opening http connection", e);
        }
    }
    
    static void setUpSSLCertificates(HttpsURLConnection con)
    {
        con.setHostnameVerifier(new HostnameVerifier() {
            //@Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });
        
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
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
        con.setSSLSocketFactory(sc.getSocketFactory());
    } 
	

	public static void main(String[] args) throws UnsupportedEncodingException
	{
		// Configure logging
        BasicConfigurator.configure();
		
		String host = "https://vm-win2008-64-1:8089";
		String user = "admin";
		String password = "splunk";
		String searchQuery = "source=wmi:memory";
		
		
		SplunkAuthManager authManager = new SplunkAuthManager();
		SplunkQueryManager queryManager = new SplunkQueryManager();
		
		try 
		{
			URL baseUrl = new URL(host);
			SessionKey sessionKey = authManager.login(baseUrl, user, password);
			
			@SuppressWarnings("unused")
			JobId jobId = queryManager.runQuery(baseUrl, searchQuery, sessionKey); 
				
			s_Logger.info(sessionKey);
		} 
		catch (MalformedURLException e) 
		{
			s_Logger.error(e);
		}
		catch (IOException e) 
		{
			s_Logger.error(e);
		}
		
		
		
		
		
		
/**************
		// Authenticate on the server and get the session key
		String url = baseurl + "/services/auth/login";
		String reqArgs = String.format("username=%s&password=%s", user, password);
		Document doc = executeRequest(url, null, reqArgs, "POST");
		String sessionKey = doc.getElementsByTagName("sessionKey").item(0).getTextContent();
		System.out.println("Successfully authenticated. Session key: " + sessionKey);

		// Dispatch a new search and return search id
		url = baseurl + "/services/search/jobs";
		reqArgs = String.format("search=search %s", URLEncoder.encode(searchQuery, "UTF-8").replace("+", "%20"));
		doc = executeRequest(url, sessionKey, reqArgs, "POST");
		String sid = doc.getElementsByTagName("sid").item(0).getTextContent();
		System.out.println(String.format("Search \"%s\" dispatched. Job id: %s. Waiting for the job to finish...", searchQuery, sid));

		// Wait until the search job is done
		url = baseurl + "/services/search/jobs/" + sid;
		doc = null;
		Boolean isDone = false;
		while(doc == null || !isDone) {
			if((doc = executeRequest(url, sessionKey, "", "GET")) == null) continue;
			NodeList nl = doc.getElementsByTagName("s:key");
			for(int i=0; i<nl.getLength(); i++){
				if(nl.item(i).getAttributes().item(0).getNodeValue().equals("isDone")) {
					isDone = (nl.item(i).getTextContent().equals("1"));
					break;
				}
			}
		}

		// Retrieve search results 
		url = baseurl + "/services/search/jobs/" + sid + "/results";
		doc = executeRequest(url, sessionKey, "", "GET");
		int resultCount = 0;
		if(doc != null) 
			resultCount = doc.getElementsByTagName("result").getLength();
	
	
		System.out.println("Search job is done. Results returned: " + resultCount);
************************/	
	}

}
