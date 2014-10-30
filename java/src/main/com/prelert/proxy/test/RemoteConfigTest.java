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

import static org.junit.Assert.*;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.Date;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.prelert.proxy.dao.RemoteObjectFactoryDAO;
import com.prelert.proxy.dao.configuration.RemoteConfigurationDAO;
import com.prelert.proxy.data.DataTypeConfig;
import com.prelert.proxy.data.InputManagerConfig;
import com.prelert.proxy.data.SourceConnectionConfig;
import com.prelert.proxy.inputmanager.InputManagerType;

public class RemoteConfigTest
{
	private static RemoteConfigurationDAO s_RemoteDAO; 

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
		
		s_RemoteDAO = factory.getConfigurationDAO("");
	}
	
	
	@Test
	public void testAddDataTypeConfigs() throws RemoteException
	{
		String newType = "TestType2";
		s_RemoteDAO.removeConfiguredDataType(newType);
		
		List<DataTypeConfig> origConfigs = s_RemoteDAO.getConfiguredDataTypes();

		InputManagerConfig imConfig = new InputManagerConfig();
		imConfig.setInputManagerType(InputManagerType.INTERNAL);
		imConfig.setHost("localhost");
		imConfig.setPort(49996);
		imConfig.setStartDate(new Date());
		imConfig.setQueryLengthSecs(120);
		imConfig.setUpdateIntervalSecs(120);

		SourceConnectionConfig source = new SourceConnectionConfig();
		source.setHost("localhost");
		source.setPort(1433);
		source.setUsername("user");
		source.setPassword("pass");

		DataTypeConfig dataTypeConfig = new DataTypeConfig(newType, "testPlugin");
		dataTypeConfig.setInputManagerConfig(imConfig);
		dataTypeConfig.setSourceConnectionConfig(source);

		dataTypeConfig.addPluginProperty("key1", "value1");
		dataTypeConfig.addPluginProperty("key2", "value2");
		dataTypeConfig.addPluginProperty("key3", "value3");
		
		boolean added = s_RemoteDAO.addConfiguredDataType(dataTypeConfig);
		assertTrue(added);
		
		List<DataTypeConfig> newConfigs = s_RemoteDAO.getConfiguredDataTypes();
		assertEquals(origConfigs.size() + 1, newConfigs.size());
		
		List<String> types = s_RemoteDAO.getConfiguredDataTypeNames();
		assertTrue(types.contains(newType));
		
		DataTypeConfig returnedConfig = s_RemoteDAO.getConfiguredDataType(newType);
		assertTrue(returnedConfig != null);
		assertEquals(dataTypeConfig, returnedConfig);
		
		
		boolean removed = s_RemoteDAO.removeConfiguredDataType(newType);
		assertTrue(removed);
		
		List<DataTypeConfig> updatedConfigs = s_RemoteDAO.getConfiguredDataTypes();
		assertEquals(origConfigs, updatedConfigs);
		
		boolean reloaded = s_RemoteDAO.reloadDataTypes();
		assertTrue(reloaded);
		List<DataTypeConfig> reloadedConfigs = s_RemoteDAO.getConfiguredDataTypes();
		assertEquals(reloadedConfigs, updatedConfigs);
	}
	
	
	/**
	 * Relies on the TemplateType.xml config file being installed in
	 * the config/templates directory. If run from the ant build file
	 * this should be setup.
	 */
	@Test
	public void testTemplateDataTypesConfigs() throws RemoteException
	{
		List<DataTypeConfig> templates = s_RemoteDAO.getTemplateDataTypes();
		assertTrue("at least one template", templates.size() > 0);
		
		DataTypeConfig templateType = s_RemoteDAO.getTemplateDataType("TemplateType");
		assertNotNull(templateType);
				
		List<String> types = s_RemoteDAO.getTemplateDataTypeNames();
		assertTrue(types.contains(templateType.getDataType()));
		
		List<String> fullTypes = s_RemoteDAO.getConfiguredDataTypeNames();
		assertFalse(fullTypes.contains(templateType.getDataType()));
	}

}
