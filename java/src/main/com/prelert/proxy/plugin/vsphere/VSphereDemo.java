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

package com.prelert.proxy.plugin.vsphere;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import com.vmware.security.credstore.CredentialStore;
import com.vmware.security.credstore.CredentialStoreFactory;

/**
 * Demo development class for VCenter VSphere web services. 
 */
public class VSphereDemo 
{
	static Logger s_Logger = Logger.getLogger(VSphereDemo.class);

	VSphereTaskCollector m_TaskManager;
	VSphereEventCollector m_EventManager;
	VSpherePerformanceData m_PerformanceData;
	
	/*
	 * properties
	 */
	String m_Username;
	String m_Host;
	java.net.URL m_WebServiceUrl;
	
	boolean m_IgnoreCert;
	
	public VSphereDemo()
	{
	}
	
	
	public void loadProperties(Properties properties) throws Exception 
	{
		m_Username = getCompulsoryProperty(properties, "username");
		m_Host = getCompulsoryProperty(properties, "host");
		
		String urlString = getCompulsoryProperty(properties, "url");
		try
		{
			m_WebServiceUrl = new URL(urlString); 
		}
		catch (MalformedURLException me)
		{
			throw new Exception("Invalid Url for vSphere web services." +
					" Url = " + urlString);
		}
		
		m_IgnoreCert = (properties.getProperty("ignorecert") != null);			
	}
	
	private String getCompulsoryProperty(Properties props, String propName)
	throws Exception
	{
		String propValue = props.getProperty(propName);
		if (propValue == null)
		{
			throw new Exception("'" + propName +
								"' property not specified for vSphere plugin");
		}

		return propValue;
	}
	
	
	public void connect() throws Exception
	{
		CredentialStore credentialStore = CredentialStoreFactory.getCredentialStore();    
		String password = new String(credentialStore.getPassword(m_Host, m_Username)); 
		
		VSphereConnection connection;
		connection = new VSphereConnection();
		connection.connect(m_WebServiceUrl, m_Username, password, m_IgnoreCert);		
		m_EventManager = new VSphereEventCollector(connection);
		
		connection = new VSphereConnection();
		connection.connect(m_WebServiceUrl, m_Username, password, m_IgnoreCert);
		m_TaskManager = new VSphereTaskCollector(connection);
		
		connection = new VSphereConnection();
		connection.connect(m_WebServiceUrl, m_Username, password, m_IgnoreCert);
		m_PerformanceData = new VSpherePerformanceData(connection, Collections.<VSphereResourceSelection>emptyList());
	}
	
	/**
	 * List users in the credentials store
	 * @throws IOException
	 */
	public void listUsers() throws IOException
	{
		CredentialStore credentialStore = CredentialStoreFactory.getCredentialStore();      

		Set<String> usernames = credentialStore.getUsernames("vm-win2008-64-2");
		s_Logger.debug(usernames.size() + " users");

		for (String user : usernames)
		{
			s_Logger.debug(user);
		} 
	}
	
	/**
	 * Add a user to the credential store
	 * @param hostname
	 * @param username
	 * @param password
	 * @throws IOException
	 */
	public void writeUser(String hostname, String username, char[] password) throws IOException
	{
		CredentialStore credentialStore = CredentialStoreFactory.getCredentialStore();     
		
		credentialStore.addPassword(hostname, username, password);
	}
	

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException 
	{
		 // Configure logging
        BasicConfigurator.configure();
        
		// don't bother with certificates for now.
		System.setProperty("axis.socketSecureFactory",
						"org.apache.axis.components.net.SunFakeTrustSocketFactory");

		VSphereDemo demo = new VSphereDemo();
		
		final String PROPERTIES_FILE = "vspherePlugin.properties";
		InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream(PROPERTIES_FILE);
		Properties properties = new Properties();
		try 
		{
			properties.load(inputStream);
			demo.loadProperties(properties);
		}		
		catch (IOException e)
		{
			s_Logger.error("Could not load properties file '" + PROPERTIES_FILE + "'");
			return;
		}
		catch (Exception e)
		{
			s_Logger.error(e);
			return;
		}
		
		
		try
		{
			demo.connect();
			demo.m_EventManager.start();
			demo.m_TaskManager.start();
			
			
//			PerfQuerySpec querySpec = demo.m_PerformanceData.createPerfQuerySpec();
//			demo.m_PerformanceData.queryPerfData(querySpec);
		}
		catch (Exception e)
		{
			s_Logger.error(e);
			return;
		}
		
	}
}
