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
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.prelert.proxy.plugin.splunk.SplunkAuthManager.SessionKey;


/**
 * Run splunk queries. 
 */
public class SplunkQueryManager 
{
	static final private String JOBS_SERVICE_PATH = "/services/search/jobs/";
	static final private String JOB_CONTROL_PATH = "/control";
	
	static final private String CANCEL_ACTION = "?action=cancel";
	
	public class JobId
	{
		final private String m_JobId;
		
		private JobId(String jobId)
		{
			m_JobId = jobId;
		}
		
		public String getJobId()
		{
			return m_JobId;
		}
		
		@Override
		public String toString()
		{
			return m_JobId;
		}
	}
	
	public JobId runQuery(URL baseUrl, String searchQuery, SessionKey sessionKey) 
	throws MalformedURLException, IOException
	{
		HttpURLConnection httpConnection = 
			SplunkConnectionManager.openConnection(baseUrl, JOBS_SERVICE_PATH);
		
        try 
        {
        	httpConnection.setRequestMethod("POST");
        }
        catch (ProtocolException e) 
        {
            throw new IllegalArgumentException(e);
        }
        
        httpConnection.setRequestProperty("Authorization", "Splunk " + 
        						sessionKey.getSessionKey());
        
        httpConnection.setRequestProperty("Content-Type",
        								"application/x-www-form-urlencoded");
        
        String encodedQuery = String.format("search=search %s", 
        							URLEncoder.encode(searchQuery, "UTF-8").replace("+", "%20"));
        
        OutputStream outputStream = httpConnection.getOutputStream();
        outputStream.write(encodedQuery.getBytes("utf-8"));
        outputStream.close();
        
        
        Document doc = null;
        try
        {
            InputSource inputSource = new InputSource();
            InputStream inputStream = httpConnection.getInputStream();
            
            if (httpConnection.getResponseCode() == 204 || 
            		inputStream ==null || inputStream.available() == 0)
            {
            	throw new IOException("Http error. Response code = " + 
            					httpConnection.getResponseCode());
            }
            
            inputSource.setCharacterStream(new BufferedReader(new InputStreamReader(inputStream)));
            
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputSource);
        }
        catch (ParserConfigurationException e) 
        {
            throw new RuntimeException(e.getMessage());
        }
        catch (SAXException e) 
        {
            throw new RuntimeException(e.getMessage());
        }

        String sid = doc.getElementsByTagName("sid").item(0).getTextContent();
		
		return this.new JobId(sid);
	}
	
	
	public boolean cancelQuery(URL baseUrl, JobId jobId, SessionKey sessionKey)
	throws MalformedURLException, IOException
	{
		String path = JOBS_SERVICE_PATH + jobId.getJobId() + JOB_CONTROL_PATH;
		
		HttpURLConnection httpConnection = 
			SplunkConnectionManager.openConnection(baseUrl, path);
		
        httpConnection.setRequestProperty("Authorization", "Splunk " + 
				sessionKey.getSessionKey());

        httpConnection.setRequestProperty("Content-Type",
						"application/x-www-form-urlencoded");
		
        OutputStream outputStream = httpConnection.getOutputStream();
        outputStream.write(CANCEL_ACTION.getBytes("utf-8"));
        outputStream.close();
        
		return false;
	}

}
