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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Class for logging into Splunk 
 */
public class SplunkAuthManager 
{
	public class SessionKey
	{
		final private String m_SessionKey;
		
		private SessionKey(String key)
		{
			m_SessionKey = key;
		}
		
		public String getSessionKey()
		{
			return m_SessionKey;
		}
		
		@Override
		public String toString()
		{
			return m_SessionKey;
		}
	}
	
	
	public static final String AUTH_PATH = "/services/auth/login/";
	
	/**
	 * 
	 * @param baseUrl Should be of the form "https://hostmachine:8089"
	 * @param username
	 * @param password
	 * @return
	 * @throws MalformedURLException
	 */
	public SessionKey login(URL baseUrl, String username, String password)
	throws MalformedURLException, IOException
	{
		HttpURLConnection httpConnection = 
				SplunkConnectionManager.openConnection(baseUrl, AUTH_PATH);
		

        httpConnection.setRequestMethod("POST");
        
        httpConnection.setDoInput(true);
        httpConnection.setDoOutput(true);
        
        httpConnection.setRequestProperty("Content-Type",
        					"application/x-www-form-urlencoded");
        
        String loginArgs = String.format("username=%s&password=%s", username, password);
        OutputStream os = httpConnection.getOutputStream();
        os.write(loginArgs.getBytes("utf-8"));
       	os.close();
        
       	
        try 
        {
            InputSource inputSource = new InputSource();
            InputStream inputStream = httpConnection.getInputStream();
            if(httpConnection.getResponseCode() == 204 || inputStream == null 
            							|| inputStream.available() == 0) 
            {
            	return null;
            }
            
            inputSource.setCharacterStream(new BufferedReader(new InputStreamReader(inputStream)));
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
            
            String sessionKey = doc.getElementsByTagName("sessionKey").item(0).getTextContent();
            
            return this.new SessionKey(sessionKey);
        } 
        catch (ParserConfigurationException e) 
        {
            throw new RuntimeException(e.getMessage());
        } 
        catch (SAXException e) 
        {
            throw new RuntimeException(e.getMessage());
        }
	}
	
}
