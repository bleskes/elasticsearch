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

package com.prelert.proxy.test;

import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Properties;

public class LocateRegistryForTests 
{
	static final String TEST_PROPERTIES_FILENAME = "tests.properties";
	static final String DEFAULT_HOST = "localhost";
	static final Integer DEFAULT_PORT = new Integer(1098);
	
	static public Registry getRegistry() throws RemoteException
	{
		int port = DEFAULT_PORT;
		String host = DEFAULT_HOST;

		InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream(TEST_PROPERTIES_FILENAME);
		
		Properties properties = new Properties();
		try 
		{
			properties.load(inputStream);
			

			host = properties.getProperty("registryHost");
			if (host == null)
			{
				System.out.println("Cannot read property 'registryHost'. Using default = " + DEFAULT_HOST);
			}
			
			String portStr = properties.getProperty("registryPort");
			if (portStr != null)
			{
				try
				{
					port = Integer.parseInt(portStr);
				}
				catch (NumberFormatException e)
				{
					System.out.println("RMI registry port '" + portStr +
									"' cannot be parsed to an integer.");
					throw e;
				}
			}
			
		}
		catch (IOException e)
		{
			System.out.println("ERROR: Could not load properties file '" + TEST_PROPERTIES_FILENAME + "'");
			System.out.println("ERROR: " + e);
			System.out.println(String.format("INFO: Using defaults, host=%s1, port=%s2", DEFAULT_HOST, DEFAULT_PORT));
		}
		
		
		
		return LocateRegistry.getRegistry(host, port);
	}

}
