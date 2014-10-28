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

package com.prelert.devutils.introscope;

import static com.prelert.proxy.plugin.introscope.CatalogueIntroscope.ALL_NO_HEURISTIC_METRICS_REGEX;
import static com.prelert.proxy.plugin.introscope.CatalogueIntroscope.NOT_CUSTOM_AGENT_REGEX;

import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import com.prelert.proxy.dao.RemoteControlDAO;
import com.prelert.proxy.dao.RemoteObjectFactoryDAO;
import com.prelert.proxy.dao.configuration.RemoteConfigurationDAO;
import com.prelert.proxy.dao.configuration.introscope.RemoteIntroscopeConfigDAO;
import com.prelert.proxy.data.CavAvailableDateRange;
import com.prelert.proxy.data.DataTypeConfig;
import com.prelert.proxy.data.InputManagerConfig;
import com.prelert.proxy.data.SourceConnectionConfig;
import com.prelert.proxy.data.introscope.MetricGroup;
import com.prelert.data.CavStatus;
import com.prelert.data.ConnectionStatus;
import com.prelert.data.CavStatus.CavRunState;
import com.prelert.data.AnalysisDuration;
import com.prelert.proxy.regex.RegExUtilities;


/**
 * Class to simulate a simple CAV. 
 */
public class RunCavTest 
{
	private static Logger s_Logger = Logger.getLogger(RunCavTest.class);
	
	public static void main(String[] args) throws RemoteException, NotBoundException, InterruptedException 
	{
		// Configure logging
		BasicConfigurator.configure();
		
		// Create and install a security manager
		if (System.getSecurityManager() == null)
		{
			System.setSecurityManager(new RMISecurityManager());
		}
		
		Registry registry = LocateRegistry.getRegistry("localhost", Registry.REGISTRY_PORT);
		RemoteObjectFactoryDAO factory = (RemoteObjectFactoryDAO)registry.lookup("com.prelert.RemoteObjectFactory");
		
		RemoteConfigurationDAO configDAO = factory.getConfigurationDAO("");
		RemoteIntroscopeConfigDAO introscopeConfigDAO = (RemoteIntroscopeConfigDAO)configDAO.getConfigServer(
													"com.prelert.proxy.plugin.introscope.IntroscopePlugin");
		
		RemoteControlDAO controlDAO = (RemoteControlDAO)registry.lookup("com.prelert.ControlServer");
		
		// reset config
		introscopeConfigDAO.resetConfiguration();
		
		DataTypeConfig dataTypeConfig = configDAO.getTemplateDataType("Introscope");
		
		SourceConnectionConfig connectConfig = dataTypeConfig.getSourceConnectionConfig();
		connectConfig.setHost("192.168.62.224"); // vm-win2003r2-32-1
		//connectConfig.setHost("184.73.2.205"); // wily-win
		connectConfig.setUsername("Admin");
		connectConfig.setPassword("");
				
		//ConnectionStatus connectStatus = introscopeConfigDAO.testConnection(connectConfig);
		ConnectionStatus connectStatus = configDAO.testConnection(dataTypeConfig);
		if (connectStatus.getStatus() != ConnectionStatus.Status.CONNECTION_OK)
		{
			s_Logger.fatal("Error setting connection config: " + connectStatus.getErrorMessage());
			return;
		}
		
		CavAvailableDateRange dates = configDAO.getValidDateRange();
		if ((dates.getStart() == null || dates.getEnd() == null) ||
				(dates.getStart().equals(dates.getEnd())) || 
				dates.getStart().after(dates.getEnd()))
		{
			s_Logger.fatal("Invalid available date range.");
			return;
		}

		List<String> agents = introscopeConfigDAO.listAgentsOnEM(NOT_CUSTOM_AGENT_REGEX, connectConfig);
		s_Logger.info("Agents = " + agents);
		

		List<String> escapedAgents = new ArrayList<String>();
		List<MetricGroup> queries = new ArrayList<MetricGroup>();
		for (String agent : agents)
		{			
			String escacpedAgent = RegExUtilities.escapeRegex(agent);
			escapedAgents.add(escacpedAgent);

			queries.add(new MetricGroup(escacpedAgent, ALL_NO_HEURISTIC_METRICS_REGEX, true));
			 
			// just do one agent.
			break;
		}

		s_Logger.info("Estimating CAV duration");

		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -1);
		AnalysisDuration estimate = introscopeConfigDAO.estimateCompletionTime(agents, cal.getTime(), connectConfig);
		
		printDuration(estimate.getEstimatedAnalysisDurationMs() / 1000, "All Metrics No Heuristics.");


		// now start the cav.
		CavStatus status = controlDAO.getCavStatus();
		s_Logger.info(status);

		Date timeOfIncident = cal.getTime();
		
		dataTypeConfig.setSourceConnectionConfig(connectConfig);
		
		InputManagerConfig imConfig = dataTypeConfig.getInputManagerConfig();
		imConfig.setQueryLengthSecs(estimate.getOptimalQueryLengthSecs());
		imConfig.setUpdateIntervalSecs(0);
		
		dataTypeConfig.addPluginProperty("Interval", new Integer(estimate.getActualDataPointIntervalSecs()).toString());
		// String agentsStr = org.apache.commons.lang.StringUtils.join(agents.toArray(), "=%="); 
		
		String separator = "=%=";
		String agentsStr = agents.get(0) + separator + agents.get(1);
		dataTypeConfig.addPluginProperty("Agents", agentsStr);
		dataTypeConfig.addPluginProperty("AgentSeparator", separator);
		
		//dataTypeConfig.addPluginProperty("MetricRegex", "Average Response Time");
		
		configDAO.addConfiguredDataType(dataTypeConfig);

		// start the CAV
		controlDAO.startCav(timeOfIncident, Arrays.asList(dataTypeConfig));

		Thread.sleep(5000);

		status = controlDAO.getCavStatus();
		if (status.getRunState().equals(CavRunState.CAV_RUNNING))
		{
			s_Logger.info("CAV has started");
		}
		else
		{
			s_Logger.info("ERROR- CAV failed to start");
		}
		
		while (status.getRunState() == CavRunState.CAV_RUNNING)
		{
			s_Logger.info(status);
			
			try
			{
				Thread.sleep(20000);
			}
			catch (InterruptedException e)
			{
			}
			
			status = controlDAO.getCavStatus();
		}	
		s_Logger.info("Finished: " + status);
		
		
		
//		if (controlDAO.shutdown() == false)
//			s_Logger.error("Shutdown error");
	}
	
	
	static public void printDuration(long diffInSeconds, String message)
	{
		long secs = (diffInSeconds >= 60 ? diffInSeconds % 60 : diffInSeconds);
		long min = (diffInSeconds = (diffInSeconds / 60)) >= 60 ? diffInSeconds % 60 : diffInSeconds;
		long hours =  (diffInSeconds = (diffInSeconds / 60)) >= 24 ? diffInSeconds % 24 : diffInSeconds;
		long days = (diffInSeconds = (diffInSeconds / 24));
		
		s_Logger.info(String.format("%s %s days, %s hour, %s minutes, %s seconds", 
										message, days, hours, min, secs));
	}
}
