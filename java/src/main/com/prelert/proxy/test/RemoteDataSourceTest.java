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

import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.prelert.data.DataSource;
import com.prelert.data.DataSourceType;
import com.prelert.proxy.dao.RemoteDataSourceDAO;
import com.prelert.proxy.dao.RemoteObjectFactoryDAO;

/**
 * Test suite for the Proxy functions exposed by the DataSourceDAO interface.
 * 
 * Each test compares the results returned by the proxy to the results from 
 * a known good source.
 */
public class RemoteDataSourceTest 
{
	private static RemoteDataSourceDAO s_RemoteDAO; 
	private static LocalDatabaseTestUtil s_ReferenceDatabase;
	
	@BeforeClass
	public static void oneTimeSetup() throws RemoteException, MalformedURLException, NotBoundException, Exception
	{
		// Create and install a security manager
		if (System.getSecurityManager() == null)
		{
			System.setSecurityManager(new RMISecurityManager());
		}
		
		Registry registry = LocateRegistryForTests.getRegistry();
		RemoteObjectFactoryDAO factory = (RemoteObjectFactoryDAO)registry.lookup("com.prelert.RemoteObjectFactory");

		s_RemoteDAO = factory.getDataSourceDAO("");
		
		s_ReferenceDatabase = new LocalDatabaseTestUtil();
	}

	 		
	@Test 
	public void testGetDataSourceTypes() throws RemoteException
	{
		List<DataSourceType> refTypes = s_ReferenceDatabase.getDataSourceDAO().getDataSourceTypes();
		List<DataSourceType> testTypes = s_RemoteDAO.getDataSourceTypes();
		
		for (DataSourceType type : refTypes)
		{
			assertTrue(testTypes.contains(type));
		}
	}


	@Test
	public void testGetDataSources() throws RemoteException
	{
		List<DataSourceType> refTypes = s_ReferenceDatabase.getDataSourceDAO().getDataSourceTypes();

		for (DataSourceType type : refTypes)
		{
			List<DataSource> refSources = s_ReferenceDatabase.getDataSourceDAO().getDataSources(type);
			List<DataSource> testSources = s_RemoteDAO.getDataSources(type);
			
			assertTrue(dataSourcesAreEqual(refSources, testSources));			
		}
	}


	//@Test 
	public void testGetAllDataSources() throws RemoteException
	{
		List<DataSource> refSources = s_ReferenceDatabase.getDataSourceDAO().getAllDataSources();		
		List<DataSource> testSources = s_RemoteDAO.getAllDataSources();
		
		assertTrue(dataSourcesAreEqual(refSources, testSources));	
	}
	
	
	private boolean dataSourcesAreEqual(List<DataSource> a, List<DataSource> b)
	{
		boolean result = (a.size() == b.size());
		if (!result)
		{
			return false;
		}

		for (int i = 0; i < a.size(); i++)
		{
			result = a.get(i).equals(b.get(i));

			if (!result)
			{
				// If the comparison fails, we want to see what's different in
				// the unit test output
				System.out.println("Data source " + i + " differs between lists A and B");
				System.out.println("List A data source " + i + " is: " + a.get(i));
				System.out.println("List B data source " + i + " is: " + b.get(i));
				break;
			}
		}

		return result;
	}
	
}
