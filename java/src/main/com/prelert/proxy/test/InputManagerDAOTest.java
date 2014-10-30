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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.prelert.data.DataSourceCategory;
import com.prelert.data.TimeSeriesInterpretation;
import com.prelert.proxy.data.ExternalTimeSeriesConfig;
import com.prelert.proxy.inputmanager.dao.InputManagerDAO;
import com.prelert.proxy.pluginLocator.PluginLocator;

/**
 * Test suite for the Proxy functions exposed by the InputManagerDAO interface.
 * 
 * Tests adding new external time series to the database. The tests modify 
 * the database so subsequent tests may fail if they expect the database 
 * to be unchanged.
 * 
 * Tests will only succeed if the Proxy is already running.
 */
public class InputManagerDAOTest 
{
	private static InputManagerDAO s_InputManagerDAO;
	private static PluginLocator s_PluginLocator;
	
	
	@BeforeClass
	public static void oneTimeSetup() throws RemoteException, MalformedURLException, NotBoundException, Exception
	{
		// Create and install a security manager
		if (System.getSecurityManager() == null)
		{
			System.setSecurityManager(new RMISecurityManager());
		}
		
		
		ApplicationContext appContext =  
			new ClassPathXmlApplicationContext("referenceDAOs.xml");

		s_PluginLocator = appContext.getBean("pluginLocator", PluginLocator.class);
		s_InputManagerDAO = appContext.getBean("inputManagerDAO", InputManagerDAO.class);
	}


	// We need to guarantee that the type is added before the series, and JUnit
	// doesn't guarantee execution order, hence this test is called from
	// testAddExternalTimeSeries() below
	public void testAddExternalType() throws RemoteException
	{
		String datatype = "coffee";
		String metric = "consumption";
		TimeSeriesInterpretation interp = TimeSeriesInterpretation.ABSOLUTE;
		String graphName = "graph";
		String graphTitle = "title";
		String graphYAxisLabel = "Y Axis";
		int usualInterval = 30;
		String plugin = "testPlugin";
		
		s_InputManagerDAO.addExternalTimeSeriesType(datatype, metric, interp, graphName, 
									graphTitle, graphYAxisLabel, usualInterval, plugin);
		
		String externalKey = "coffeePluginKey";
		
		int timeSeriesId = s_InputManagerDAO.addExternalTimeSeries(datatype, metric, externalKey);
		
		ExternalTimeSeriesConfig desc = s_PluginLocator.getPluginDescriptionForTimeSeriesId(timeSeriesId);
		assertNotNull(desc);

		assertEquals(datatype, desc.getType());
		assertEquals(metric, desc.getMetric());
		assertEquals(DataSourceCategory.TIME_SERIES, desc.getCategory());
		assertEquals(usualInterval, desc.getUsualInterval());
		assertEquals(externalKey, desc.getExternalKey());
		assertEquals(plugin, desc.getExternalPlugin());

		// Insert another
		
		metric = "numberOfSugars";
		
		s_InputManagerDAO.addExternalTimeSeriesType(datatype, metric, interp, graphName, 
									graphTitle, graphYAxisLabel, usualInterval, plugin);
				
		timeSeriesId = s_InputManagerDAO.addExternalTimeSeries(datatype, metric, externalKey);
		
		desc = s_PluginLocator.getPluginDescriptionForTimeSeriesId(timeSeriesId);
		assertNotNull(desc);
		
		assertEquals(datatype, desc.getType());
		assertEquals(metric, desc.getMetric());
		assertEquals(DataSourceCategory.TIME_SERIES, desc.getCategory());
		assertEquals(usualInterval, desc.getUsualInterval());
		assertEquals(externalKey, desc.getExternalKey());
		assertEquals(plugin, desc.getExternalPlugin());
	}

	
	@Test
	public void testAddExternalTimeSeries() throws RemoteException
	{
		// We need to guarantee that the type is added before the series, and
		// JUnit doesn't guarantee execution order
		testAddExternalType();

		String datatype = "coffee";
		String metric = "consumption";
		
		int count = 0;
		String externalKey1 = "coffeePluginKey" + count++;
		int id1 = s_InputManagerDAO.addExternalTimeSeries(datatype, metric, externalKey1);
		assertTrue(id1 < 0);
		
		String externalKey2 = "coffeePluginKey" + count++;
		int id2 = s_InputManagerDAO.addExternalTimeSeries(datatype, metric, externalKey2);
		assertTrue(id2 < 0);
		
		String externalKey3 = "coffeePluginKey" + count++;
		int id3 = s_InputManagerDAO.addExternalTimeSeries(datatype, metric, externalKey3);
		assertTrue(id3 < 0);
		
		String externalKey4 = "coffeePluginKey" + count++;
		int id4 = s_InputManagerDAO.addExternalTimeSeries(datatype, metric, externalKey4);
		assertTrue(id4 < 0);
		
		ExternalTimeSeriesConfig desc = s_PluginLocator.getPluginDescriptionForTimeSeriesId(id1);
		assertNotNull(desc);

		assertEquals(datatype, desc.getType());
		assertEquals(metric, desc.getMetric());
		assertEquals(externalKey1, desc.getExternalKey());
		
		desc = s_PluginLocator.getPluginDescriptionForTimeSeriesId(id2);
		assertNotNull(desc);

		assertEquals(datatype, desc.getType());
		assertEquals(metric, desc.getMetric());
		assertEquals(externalKey2, desc.getExternalKey());
		
		desc = s_PluginLocator.getPluginDescriptionForTimeSeriesId(id3);
		assertNotNull(desc);

		assertEquals(datatype, desc.getType());
		assertEquals(metric, desc.getMetric());
		assertEquals(externalKey3, desc.getExternalKey());
		
		desc = s_PluginLocator.getPluginDescriptionForTimeSeriesId(id4);
		assertNotNull(desc);

		assertEquals(datatype, desc.getType());
		assertEquals(metric, desc.getMetric());
		assertEquals(externalKey4, desc.getExternalKey());
	}

}
