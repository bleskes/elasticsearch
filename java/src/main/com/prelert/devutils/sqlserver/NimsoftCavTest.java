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

package com.prelert.devutils.sqlserver;

import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import com.prelert.data.AnalysisDuration;
import com.prelert.data.CavStatus;
import com.prelert.data.ConnectionStatus;
import com.prelert.data.AnalysisDuration.ErrorState;
import com.prelert.data.CavStatus.CavRunState;
import com.prelert.proxy.dao.RemoteControlDAO;
import com.prelert.proxy.dao.RemoteObjectFactoryDAO;
import com.prelert.proxy.dao.configuration.RemoteConfigurationDAO;
import com.prelert.proxy.data.CavAvailableDateRange;
import com.prelert.proxy.data.DataTypeConfig;
import com.prelert.proxy.data.InputManagerConfig;
import com.prelert.proxy.data.SourceConnectionConfig;

public class NimsoftCavTest 
{
	private static Logger s_Logger = Logger.getLogger(NimsoftCavTest.class);
	
	public static void main(String[] args) throws RemoteException, NotBoundException, InterruptedException, ParseException 
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
		RemoteControlDAO controlDAO = (RemoteControlDAO)registry.lookup("com.prelert.ControlServer");
		
		
		DataTypeConfig dataTypeConfig = configDAO.getTemplateDataType("Nimsoft");
		
		SourceConnectionConfig connectConfig = dataTypeConfig.getSourceConnectionConfig();
		connectConfig.setHost("192.168.62.233"); // vm-win2008r2-64-1
		connectConfig.setUsername("dbUser");
		connectConfig.setPassword("pa55w0rd");
				

		dataTypeConfig.addPluginProperty("DataBaseName", "NimbusSLMsdp");
		//String dbName = dataTypeConfig.getPluginProperties().get("DataBaseName");
		
		ConnectionStatus connectStatus = configDAO.testConnection(dataTypeConfig);
		if (connectStatus.getStatus() != ConnectionStatus.Status.CONNECTION_OK)
		{
			s_Logger.fatal("Error setting connection config.");
			s_Logger.fatal(connectStatus.getErrorMessage());
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

		s_Logger.info("Estimating CAV duration");
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -1);
		
		//Date timeOfIncident = cal.getTime();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
		Date timeOfIncident = dateFormat.parse("2012/03/30 09:00");

		AnalysisDuration estimate = configDAO.estimateCompletionTime(dataTypeConfig, timeOfIncident);
		
		if (estimate.getErrorState() != ErrorState.NO_ERROR)
		{
			s_Logger.error("Estimate duration failed: " + estimate);
			return;
		}
		
		printDuration(estimate.getEstimatedAnalysisDurationMs() / 1000, "NetQoS pull duration");

		// now start the cav.
		CavStatus status = controlDAO.getCavStatus();
		s_Logger.info(status);

		
		dataTypeConfig.setSourceConnectionConfig(connectConfig);
		
		InputManagerConfig imConfig = dataTypeConfig.getInputManagerConfig();
		imConfig.setQueryLengthSecs(estimate.getOptimalQueryLengthSecs());
		imConfig.setUpdateIntervalSecs(0);
		
	
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
