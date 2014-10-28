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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.prelert.data.CavStatus;
import com.prelert.data.AnalysisDuration;
import com.prelert.data.ConnectionStatus;
import com.prelert.data.CavStatus.CavRunState;
import com.prelert.proxy.dao.RemoteControlDAO;
import com.prelert.proxy.dao.RemoteObjectFactoryDAO;
import com.prelert.proxy.dao.configuration.RemoteConfigurationDAO;
import com.prelert.proxy.dao.configuration.introscope.RemoteIntroscopeConfigDAO;
import com.prelert.proxy.data.DataTypeConfig;
import com.prelert.proxy.data.InputManagerConfig;
import com.prelert.proxy.data.SourceConnectionConfig;
import com.prelert.proxy.data.introscope.IntroscopeConnectionConfig;
import com.prelert.proxy.data.introscope.MetricGroup;
import com.prelert.proxy.inputmanager.InputManagerType;
import static com.prelert.proxy.plugin.introscope.CatalogueIntroscope.*;
import com.prelert.proxy.plugin.introscope.IntroscopePlugin;
import com.prelert.proxy.regex.RegExUtilities;


/**
 * Test suite for the Proxy functions exposed by the RemoteConfigurationDAO interface.
 * 
 * Tests will only succeed if the Proxy is already running.
 */
public class RemoteIntroscopeConfigTest 
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
		
		assertNotNull(s_RemoteDAO);
	}


	@Test
	public void testIntroscopeConfigServer() throws RemoteException
	{
		// TODO - rewrite test to use new way of getting connection config 
		// via ConfigurationServerRMI.getConfiguredDataType("Introscope").
		RemoteIntroscopeConfigDAO configServer = (RemoteIntroscopeConfigDAO)s_RemoteDAO.getConfigServer(IntroscopePlugin.class);
		assertNotNull(configServer);
		
		configServer = (RemoteIntroscopeConfigDAO)s_RemoteDAO.getConfigServer(
										"com.prelert.proxy.plugin.introscope.IntroscopePlugin");
		assertNotNull(configServer);
		
		SourceConnectionConfig connectConfig = configServer.getConnectionConfig();
		assertNotNull(connectConfig);
	}


	@Test
	public void testValidateConnection() throws RemoteException
	{
		RemoteIntroscopeConfigDAO configServer = (RemoteIntroscopeConfigDAO)s_RemoteDAO.getConfigServer(
				"com.prelert.proxy.plugin.introscope.IntroscopePlugin");
		
		// connect to the wily-win machine
		IntroscopeConnectionConfig connectConfig = new IntroscopeConnectionConfig("184.73.2.205",
															IntroscopeConnectionConfig.DEFAULT_CLW_PORT,
															"Admin", "");

		assertEquals(ConnectionStatus.Status.CONNECTION_OK, configServer.testConnection(connectConfig).getStatus());

//		connectConfig.setHost("127.0.0.1");
//		assertFalse(configServer.testConnection(connectConfig));
//		
//		connectConfig.setHost("184.73.2.205");
//		connectConfig.setPort(1000);
//		assertFalse(configServer.testConnection(connectConfig));
//
//		connectConfig.setPort(IntroscopeConnectionConfig.DEFAULT_CLW_PORT);
//		connectConfig.setUsername("noone");
//		assertFalse(configServer.testConnection(connectConfig));
//	
//		connectConfig.setUsername("Admin");
//		connectConfig.setPassword("pass123");
//		assertFalse(configServer.testConnection(connectConfig));
	}


	@Test
	public void testSetConfiguration() throws RemoteException, NotBoundException, InterruptedException
	{
		// TODO - rewrite test to use new way of setting connection configuration
		// via ConfigurationServerRMI.addConfiguredDataType(DataTypeConfig).
		RemoteIntroscopeConfigDAO configServer = (RemoteIntroscopeConfigDAO)s_RemoteDAO.getConfigServer(
											"com.prelert.proxy.plugin.introscope.IntroscopePlugin");
		
		IntroscopeConnectionConfig connectConfig = new IntroscopeConnectionConfig("184.73.2.205",
										IntroscopeConnectionConfig.DEFAULT_CLW_PORT,
										"Admin", "");
		
		assertTrue(configServer.setConnectionConfig(connectConfig));
		
		List<String> agents = configServer.listAgentsOnEM(NOT_CUSTOM_AGENT_REGEX, connectConfig);
		assertTrue(agents.size() > 0);	
		
		// now configure the input manager.
		Registry registry = LocateRegistryForTests.getRegistry();
		RemoteControlDAO controlDAO = (RemoteControlDAO)registry.lookup("com.prelert.ControlServer");
		
		CavStatus status = controlDAO.getCavStatus();
		assertEquals(CavRunState.CAV_NOT_STARTED, status.getRunState());		
	}
	
	
	//@Test
	public void testEstimateTime() throws RemoteException
	{
		// TODO - rewrite test to use new way of setting connection configuration
		// via ConfigurationServerRMI.addConfiguredDataType(DataTypeConfig).
		RemoteIntroscopeConfigDAO configServer = (RemoteIntroscopeConfigDAO)s_RemoteDAO.getConfigServer(
											"com.prelert.proxy.plugin.introscope.IntroscopePlugin");
		
		IntroscopeConnectionConfig connectConfig = new IntroscopeConnectionConfig("184.73.2.205",
				IntroscopeConnectionConfig.DEFAULT_CLW_PORT,
				"Admin", "");

		assertTrue(configServer.setConnectionConfig(connectConfig));


		List<String> agents = configServer.listAgentsOnEM(NOT_CUSTOM_AGENT_REGEX, connectConfig);
		assertTrue(agents.size() > 0);
		

		List<MetricGroup> queries = new ArrayList<MetricGroup>();
		for (String agent : agents)
		{			
			String escacpedAgent = RegExUtilities.escapeRegex(agent);
			
			queries.add(new MetricGroup(escacpedAgent, ART_METRIC_REGEX, true));
			queries.add(new MetricGroup(escacpedAgent, CONCURRENT_METRIC_REGEX, true));
			queries.add(new MetricGroup(escacpedAgent, EPI_METRIC_REGEX, true));
			queries.add(new MetricGroup(escacpedAgent, RPI_METRIC_REGEX, true));
			queries.add(new MetricGroup(escacpedAgent, STALLS_METRIC_REGEX, true));
		}
	
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -1);
		Date incidentDate = cal.getTime();
		
		AnalysisDuration estimate = configServer.estimateCompletionTimeForQueries(queries, incidentDate, connectConfig);
		printDuration(estimate.getEstimatedAnalysisDurationMs() / 1000, "5 main Metrics.");
		
		queries.clear();
		for (String agent : agents)
		{			
			String escacpedAgent = RegExUtilities.escapeRegex(agent);
			
			queries.add(new MetricGroup(escacpedAgent, ALL_METRICS_REGEX, true));
		}
		estimate = configServer.estimateCompletionTimeForQueries(queries, incidentDate, connectConfig);
		printDuration(estimate.getEstimatedAnalysisDurationMs() / 1000, "All Metrics.");
		
		
		queries.clear();
		for (String agent : agents)
		{			
			String escacpedAgent = RegExUtilities.escapeRegex(agent);
			
			queries.add(new MetricGroup(escacpedAgent, ALL_NO_HEURISTIC_METRICS_REGEX, true));
		}

		estimate = configServer.estimateCompletionTimeForQueries(queries, incidentDate, connectConfig);
		printDuration(estimate.getEstimatedAnalysisDurationMs() / 1000, "All Metrics No Heuristics.");
	}


	/**
	 * This test does too much for a unit test, and is hence commented out - if
	 * uncommented it tries to start the back-end C++ processes, and this is
	 * beyond the scope of what a Java unit test should be doing
	 */
	// @Test
	public void testStartCav() throws RemoteException, NotBoundException, InterruptedException
	{
		// TODO - rewrite test to use new way of setting connection configuration
		// via ConfigurationServerRMI.addConfiguredDataType(DataTypeConfig).
		RemoteIntroscopeConfigDAO configServer = (RemoteIntroscopeConfigDAO)s_RemoteDAO.getConfigServer(
											"com.prelert.proxy.plugin.introscope.IntroscopePlugin");
		
		IntroscopeConnectionConfig connectConfig = new IntroscopeConnectionConfig("184.73.2.205",
				IntroscopeConnectionConfig.DEFAULT_CLW_PORT,
				"Admin", "");

		assertTrue(configServer.setConnectionConfig(connectConfig));


		List<String> agents = configServer.listAgentsOnEM(NOT_CUSTOM_AGENT_REGEX, connectConfig);
		assertTrue(agents.size() > 0);
		

		List<MetricGroup> queries = new ArrayList<MetricGroup>();
		for (String agent : agents)
		{			
			String escacpedAgent = RegExUtilities.escapeRegex(agent);
			
			queries.add(new MetricGroup(escacpedAgent, ALL_NO_HEURISTIC_METRICS_REGEX, true));
		}
		
		
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -1);
		Date incidentDate = cal.getTime();
		
		AnalysisDuration estimate = configServer.estimateCompletionTimeForQueries(queries, incidentDate, connectConfig);
		printDuration(estimate.getEstimatedAnalysisDurationMs() / 1000, "All Metrics No Heuristics.");
		
		
		List<String> returnedAgents = configServer.getAgents(connectConfig);
		
		Collections.sort(agents);
		Collections.sort(returnedAgents);
		assertEquals(agents, returnedAgents);
		
		
		// now start the cav.
		Registry registry = LocateRegistryForTests.getRegistry();
		RemoteControlDAO controlDAO = (RemoteControlDAO)registry.lookup("com.prelert.ControlServer");
		
		Date timeOfIncident = new Date(new Date().getTime() - 1000 * 60 * 60); // 1 hour ago.
		
		DataTypeConfig config = new DataTypeConfig("Introscope", "introscopePlugin");
		config.setSourceConnectionConfig(connectConfig);
		
		InputManagerConfig imConfig = new InputManagerConfig();
		imConfig.setInputManagerType(InputManagerType.EXTERNAL);
		imConfig.setHost("localhost");
		imConfig.setPort(49996);
		imConfig.setQueryLengthSecs(estimate.getOptimalQueryLengthSecs());
		imConfig.setUpdateIntervalSecs(0);

		config.setInputManagerConfig(imConfig);
	
		config.addPluginProperty("Interval", new Integer(estimate.getActualDataPointIntervalSecs()).toString());
		// String agentsStr = org.apache.commons.lang.StringUtils.join(agents.toArray(), "=%="); 
		
		String separator = "=%=";
		String agentsStr = agents.get(0) + separator + agents.get(1);
		config.addPluginProperty("Agents", agentsStr);
		config.addPluginProperty("AgentSepartor", separator);
		
		// set properties.
		
		// start the CAV
		controlDAO.startCav(timeOfIncident, Arrays.asList(config));
		
		Thread.sleep(5000);
		
		CavStatus status = controlDAO.getCavStatus();
		assertEquals(CavRunState.CAV_RUNNING, status.getRunState());
	}


	private void printDuration(long diffInSeconds, String message)
	{
		long secs = (diffInSeconds >= 60 ? diffInSeconds % 60 : diffInSeconds);
		long min = (diffInSeconds = (diffInSeconds / 60)) >= 60 ? diffInSeconds % 60 : diffInSeconds;
		long hours =  (diffInSeconds = (diffInSeconds / 60)) >= 24 ? diffInSeconds % 24 : diffInSeconds;
		long days = (diffInSeconds = (diffInSeconds / 24));
		
		System.out.println(String.format("%s %s days, %s hour, %s minutes, %s seconds", 
										message, days, hours, min, secs));
	}
}
