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

package com.prelert.proxy.server.configuration;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

import com.prelert.data.AnalysisDuration;
import com.prelert.data.ConnectionStatus;
import com.prelert.data.TimeSeriesData;
import com.prelert.data.AnalysisDuration.ErrorState;
import com.prelert.proxy.configuration.ConfigurationManager;
import com.prelert.proxy.dao.configuration.RemoteConfigurationDAO;
import com.prelert.proxy.data.CavAvailableDateRange;
import com.prelert.proxy.data.DataTypeConfig;
import com.prelert.proxy.datamanager.DataCollectionManager;
import com.prelert.proxy.datamanager.DataCollectionManager.CavStartEnd;
import com.prelert.proxy.inputmanager.querymonitor.QueryTookTooLongException;
import com.prelert.proxy.plugin.ExternalPointsPlugin;
import com.prelert.proxy.plugin.ErrorGettingDataPointsException;
import com.prelert.proxy.plugin.Plugin;
import com.prelert.proxy.plugin.Plugin.InvalidPluginPropertyException;
import com.prelert.proxy.plugin.introscope.IntroscopePlugin;
import com.prelert.proxy.pluginLocator.PluginLocator;
import com.prelert.proxy.server.configuration.introscope.IntroscopeConfigServerRMI;

/**
 * Implements the {@link RemoteConfigurationDAO} interface.
 */
public class ConfigurationServerRMI extends UnicastRemoteObject implements RemoteConfigurationDAO 
{
	private static final long serialVersionUID = 4989708193411714798L;
	
	private static final Logger s_Logger = Logger.getLogger(ConfigurationServerRMI.class);

	private String m_ServerName;
	private PluginLocator m_PluginLocator;
	private ConfigurationManager m_ConfigurationManager;
	private IntroscopeConfigServerRMI m_IntroscopeConfigServer;
	private ApplicationContext m_ApplicationContext;
	private int m_MaxAgeOfData;
	
	public ConfigurationServerRMI() throws RemoteException
	{
		m_MaxAgeOfData = 180;
	}
	

	@Override
	public List<String> getConfiguredDataTypeNames() 
	{
		return m_ConfigurationManager.getDataTypeNames();
	}
	
	
	@Override
	public DataTypeConfig getConfiguredDataType(String datatype)
	{
		return m_ConfigurationManager.getDataTypeConfig(datatype);
	}

	
	@Override
	public List<DataTypeConfig> getConfiguredDataTypes() 
	{
		return m_ConfigurationManager.getDataTypeConfigs();
	}
	

	@Override
	public boolean addConfiguredDataType(DataTypeConfig source) 
	{
		boolean result = m_ConfigurationManager.addDataTypeConfig(source);
		m_ConfigurationManager.saveConfigurations();
		return result;
	}
	
	
	@Override
	public boolean removeConfiguredDataType(String datatype)
	{
		return m_ConfigurationManager.removeDataTypeConfig(datatype);
	}
	
	@Override
	public List<String> getTemplateDataTypeNames() 
	{
		return m_ConfigurationManager.getTemplateDataTypeNames();
	}
	
	@Override
	public DataTypeConfig getTemplateDataType(String datatype)
	{
		return m_ConfigurationManager.getTemplateDataTypeConfig(datatype);
	}
	
	@Override
	public List<DataTypeConfig> getTemplateDataTypes()
	{
		return m_ConfigurationManager.getTemplateDataTypeConfigs();
	}
	
	@Override
	public boolean reloadDataTypes() 
	{
		return m_ConfigurationManager.reload();
	}
	
	
	@Override
	public ConnectionStatus testConnection(DataTypeConfig config) throws RemoteException
	{
		Plugin plugin = m_ApplicationContext.getBean(config.getPluginName(), Plugin.class);
		plugin.setDataType(config.getDataType());
		
		Properties pluginProps = new Properties();
		pluginProps.putAll(config.getPluginProperties());
		pluginProps.put("DataType", config.getDataType());

		return plugin.testConnection(config.getSourceConnectionConfig(), pluginProps);
	}
	
	
	@Override
	public AnalysisDuration estimateCompletionTime(DataTypeConfig config, Date timeOfIncident) 
	throws RemoteException
	{
		Plugin plugin = m_ApplicationContext.getBean(config.getPluginName(), Plugin.class);
		plugin.setDataType(config.getDataType());
		
		Properties pluginProps = new Properties();
		pluginProps.putAll(config.getPluginProperties());
		pluginProps.put("DataType", config.getDataType());
		
		
		try 
		{
			plugin.configure(config.getSourceConnectionConfig(), pluginProps);
		}
		catch (InvalidPluginPropertyException e) 
		{
			AnalysisDuration ad = new AnalysisDuration(ErrorState.CONNECTION_FAILURE);
			ad.setErrorMessage(e.getMessage());
			return ad;
		}
		
		
		if (!(plugin instanceof ExternalPointsPlugin))
		{
			String error = String.format("Plugin %s for datatype '%s' is not an External Points Plugin.", 
					plugin.getName(), plugin.getDataSourceType().getName());

			AnalysisDuration ad = new AnalysisDuration(ErrorState.NO_DATA);
			ad.setErrorMessage(error);
			return ad;
		}
		
		ExternalPointsPlugin extPlugin = (ExternalPointsPlugin)plugin;
		CavStartEnd cavDates = DataCollectionManager.calcCavStartAndEndFromIncidentTime(timeOfIncident);
		
		AnalysisDuration duration = estimateDuration(extPlugin, cavDates.getStart(), 
														config.getInputManagerConfig().getQueryLengthSecs());
		

		if (duration.getErrorState() != ErrorState.NO_ERROR)
		{
			return duration;
		}
		
		// Scale timings over duration of the analysis.
		long cavDuration = cavDates.getEnd().getTime() - cavDates.getStart().getTime();
		long intervals = cavDuration / (duration.getOptimalQueryLengthSecs() * 1000);

		long estimatedTime = intervals * duration.getEstimatedAnalysisDurationMs();
		
		AnalysisDuration result = new AnalysisDuration(estimatedTime, 
									duration.getOptimalQueryLengthSecs());
		result.setActualDataPointIntervalSecs(duration.getActualDataPointIntervalSecs());
		
		return result;
	}
	
	
	/**
	 * Run the plugin queries and time how long they take. The result contains 
	 * the timing of the query which needs to be scaled up over the period
	 * analysis data will be pulled for. 
	 * 
	 * If no data can be found at <code>startTime</code> then search forward 
	 * for MAX_HOURS_TO_FIND_DATA looking for data. If no data can be found
	 * at all times then return a AnalysisDuration object with its error 
	 * state set to no data.
	 * 
	 * @param plugin
	 * @param startTime
	 * @param queryLengthSecs
	 * @return
	 */
	private AnalysisDuration estimateDuration(ExternalPointsPlugin plugin, Date startTime, 
									int queryLengthSecs)
	{
		s_Logger.info(String.format("Estimating the query time at time %s", startTime));

		s_Logger.info("Find query duration for queries with length = " + queryLengthSecs + 
				" seconds at time " + startTime);
		
		String errorMessage = null;
		
		Date queryEndTime = new Date(startTime.getTime() + queryLengthSecs * 1000);

		// First check for data.
		Date timerStart = new Date();

		Collection<TimeSeriesData> data;
		try
		{
			data = plugin.getAllDataPointsForTimeSpan(startTime, queryEndTime, 
														plugin.getUsualPointIntervalSecs());
		}
		catch (ErrorGettingDataPointsException ndp)
		{
			errorMessage = ndp.getMessage();
			data = Collections.emptyList();
		}
		catch (QueryTookTooLongException e) 
		{
			data = e.getTimeSeriesPartialResults();
		}
		
		Date timerEnd = new Date();
		
		
		boolean gotSomeData = data.size() > 0;
		

		Date modifiedStartTime = null;
		// No data at time so go searching for it.
		if (gotSomeData == false)
		{
			// Go find some data
			Calendar cal = Calendar.getInstance();
			cal.setTime(startTime);
			final int MAX_HOURS_TO_FIND_DATA = 12;
			for (int i=0; i < MAX_HOURS_TO_FIND_DATA; i++)
			{
				cal.add(Calendar.HOUR_OF_DAY, 1);

				s_Logger.info("No valid data at previous time. Advancing 1 hour and querying at " + cal.getTime());

				queryEndTime = new Date(cal.getTime().getTime() + queryLengthSecs * 1000);

				timerStart = new Date();

				try 
				{
					data = plugin.getAllDataPointsForTimeSpan(startTime, cal.getTime(), 
												plugin.getUsualPointIntervalSecs());
				}
				catch (ErrorGettingDataPointsException ndp)
				{
					errorMessage = ndp.getMessage();
					continue;
				}
				catch (QueryTookTooLongException e) 
				{
					continue;
				}

				timerEnd = new Date();

				gotSomeData = data.size() > 0;
				if (gotSomeData)
				{
					s_Logger.info("Found data at time " + cal.getTime());

					modifiedStartTime = cal.getTime();
					startTime = cal.getTime();

					break;
				}
			}			
		}


		// If no data can be found return
		// else calc the duration etc. 
		if (gotSomeData == false) 
		{
			AnalysisDuration result = new AnalysisDuration(-1, -1, null, ErrorState.NO_DATA);
			
			if (errorMessage == null)
			{
				errorMessage = "No data could be found at time " + startTime + ".";
			}
			result.setErrorMessage(errorMessage);
			
			s_Logger.info("No data found. estimateDuration returning = " + result);
			return result; 
		}

		
		long queryDurationMs = timerEnd.getTime() - timerStart.getTime();
		AnalysisDuration result = new AnalysisDuration(queryDurationMs,
									queryLengthSecs, modifiedStartTime);
		
		s_Logger.info("optimalQueryLength returning = " + result);
		return result;
	}
	
	
	@Override
	public CavAvailableDateRange getValidDateRange()
	{
		Date now = new Date();
		
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_MONTH, -m_MaxAgeOfData); 
		
		Date start = cal.getTime();
		
		return new CavAvailableDateRange(start, now);
	}
	
	
	/**
	 * Returns one of:
	 * - IntroscopeConfigServerRMI
	 * 
	 * or <code>null</code>
	 * 
	 * The result needs to be cast to the specific server type.
	 */
	@Override
	public Remote getConfigServer(Class<? extends Plugin> pluginClass) 
	throws RemoteException
	{
		if (pluginClass.equals(IntroscopePlugin.class))
		{
			return getIntroscopeConfigServer();
		}
		else 
		{
			return null;
		}
	}
		
	@Override
	public Remote getConfigServer(String className)
	{
		if (IntroscopePlugin.class.getName().equals(className))
		{
			return getIntroscopeConfigServer();
		}
		else 
		{
			return null;
		}
	}

	
	/**
	 * The PluginLocator object.
	 * @return
	 */
	public PluginLocator getPluginLocator()
	{
		return m_PluginLocator;
	}
	
	public void setPluginLocator(PluginLocator pluginLocator)
	{
		m_PluginLocator = pluginLocator;
	}
	
	
	/**
	 * Returns the ConfigurationManager.
	 * 
	 * @return The ConfigurationManager or <code>null</code> if it hasn't
	 * been set.
	 */
	public ConfigurationManager getConfigurationManager()
	{
		return m_ConfigurationManager;
	}
	
	public void setConfigurationManager(ConfigurationManager manager)
	{
		m_ConfigurationManager = manager;
	}
	
	
	/**
	 * The Application context object.
	 * @return
	 */
	public ApplicationContext getApplicationContext()
	{
		return m_ApplicationContext;
	}
	
	public void setApplicationContext(ApplicationContext value)
	{
		m_ApplicationContext = value;
	}
	
	/**
	 * The Introscope configuration server.
	 * @return
	 */
	private IntroscopeConfigServerRMI getIntroscopeConfigServer()
	{
		if (m_IntroscopeConfigServer == null)
		{
			if (m_ApplicationContext != null)
			{
				m_IntroscopeConfigServer = m_ApplicationContext.getBean("introscopeConfigServer", 
											IntroscopeConfigServerRMI.class);
				
				m_IntroscopeConfigServer.setConfigurationManager(m_ConfigurationManager);
			}
		}
		
		return m_IntroscopeConfigServer;
	}
	
	
	public String getServerName()
	{
		return m_ServerName;
	}
	
	public void setServerName(String serverName)
	{
		m_ServerName = serverName;
	}
	
	
	/**
	 * The number of days used to calculate the valid
	 * CAV time range in the {@link #getValidDateRange()} function. 
	 */
	public int getMaxAgeOfData()
	{
		return m_MaxAgeOfData;
	}
	
	public void setMaxAgeOfData(int value)
	{
		m_MaxAgeOfData = value;
	}
	
}
