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

package com.prelert.proxy.server.configuration.introscope;

import static com.prelert.proxy.plugin.introscope.CatalogueIntroscope.ALL_NO_HEURISTIC_METRICS_REGEX;
import static com.prelert.proxy.plugin.introscope.CatalogueIntroscope.METRIC_QUERIES_FILENAME;
import static com.prelert.proxy.plugin.introscope.CatalogueIntroscope.NOT_CUSTOM_AGENT_REGEX;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import com.prelert.data.AnalysisDuration;
import com.prelert.data.ConnectionStatus;
import com.prelert.data.AnalysisDuration.ErrorState;
import com.prelert.data.TimeSeriesData;
import com.prelert.proxy.Proxy;
import com.prelert.proxy.configuration.ConfigurationManager;
import com.prelert.proxy.dao.configuration.introscope.RemoteIntroscopeConfigDAO;
import com.prelert.proxy.data.CavAvailableDateRange;
import com.prelert.proxy.data.DataTypeConfig;
import com.prelert.proxy.data.SourceConnectionConfig;
import com.prelert.proxy.data.introscope.MetricGroup;
import com.prelert.proxy.datamanager.DataCollectionManager;
import com.prelert.proxy.datamanager.DataCollectionManager.CavStartEnd;
import com.prelert.proxy.plugin.Plugin;
import com.prelert.proxy.plugin.Plugin.InvalidPluginPropertyException;
import com.prelert.proxy.plugin.introscope.ClwConnectionPool;
import com.prelert.proxy.plugin.introscope.IntroscopeConnection;
import com.prelert.proxy.plugin.introscope.IntroscopeConnection.ConnectionException;
import com.prelert.proxy.plugin.introscope.IntroscopeLoadMonitor;
import com.prelert.proxy.plugin.introscope.IntroscopePlugin;
import com.prelert.proxy.plugin.introscope.OptimalUsage;
import com.prelert.proxy.pluginLocator.PluginLocator;
import com.prelert.proxy.pluginLocator.PluginLocator.ThreadLocalPlugin;
import com.prelert.proxy.regex.RegExUtilities;
import com.prelert.proxy.runpolicy.RunPolicy;
import com.prelert.server.ServerUtil;

/**
 * Class for setting the Introscope Configuration. 
 */
public class IntroscopeConfigServerRMI extends UnicastRemoteObject implements RemoteIntroscopeConfigDAO
{
	private static final long serialVersionUID = 8873513037786574011L;

	public static final String DATATYPE = "Introscope";
	
	private static final Logger s_Logger = Logger.getLogger(IntroscopeConfigServerRMI.class);

	private String m_ServerName;
	private PluginLocator m_PluginLocator;

	private OptimalUsage m_OptimalUsage;
	
	private String m_MetricRegex;

	private int m_MaxAgeOfData;
	private static final int DEFAULT_MAX_DATA_AGE = 30;
	
	private ConfigurationManager m_ConfigurationManager;
	
	private ClwConnectionPool m_ConnectionPool;
	

	/**
	 * File contains the selected agents to be monitored.
	 */
	public static final String AGENTS_FILENAME = "ca_apm_agents";
	
	public IntroscopeConfigServerRMI() throws RemoteException
	{
		super();
		
		m_MetricRegex = ALL_NO_HEURISTIC_METRICS_REGEX;
		m_MaxAgeOfData = DEFAULT_MAX_DATA_AGE;
		
		m_ConnectionPool = new ClwConnectionPool();
	}
	
	
	@Override
	public SourceConnectionConfig getConnectionConfig()
	{
		s_Logger.debug("getConnectionConfig()");
		
		return m_ConnectionPool.getConnectionConfig();
	}
		
	
	/**
	 * Lists all the IntroscopePlugins and tests the connection on
	 * the first one found.
	 * If no plugins are registered <code>false</code> is returned.
	 */
	@Override
	public ConnectionStatus testConnection(SourceConnectionConfig config) throws RemoteException
	{
		s_Logger.info("testConnection: " + config);
		
		ConnectionStatus status;
		
		IntroscopeConnection connection = m_ConnectionPool.testConnectionConfig(config);
		try
		{
			if (connection == null)
			{
				status = new ConnectionStatus(ConnectionStatus.Status.CONNECTION_FAILED);
			}
			else
			{
				Date stop = new Date();
				Date start = new Date(stop.getTime() - 1000 * 60);		
				Collection<TimeSeriesData> data = 
					connection.getMetricData(IntroscopeLoadMonitor.CUSTOM_METRIC_AGENT, 
							IntroscopeLoadMonitor.OVERALL_CAPACITY_METRIC,
							start, stop);

				if (data.size() == 0)
				{
					status = new ConnectionStatus(ConnectionStatus.Status.MISSING_HEALTH_METRICS);
				}

				status = new ConnectionStatus(ConnectionStatus.Status.CONNECTION_OK);
			}
		}
		catch (ConnectionException e) 
		{
			status = new ConnectionStatus(ConnectionStatus.Status.CONNECTION_FAILED);
		}		
		finally
		{
			if (connection != null)
			{
				connection.logoff();
			}
		}
		
		s_Logger.info("testConnection return" +
				" status: " + status);
		
		return status;
	}
	

	/**
	 * Sets the connection parameters for all the IntroscopePlugins
	 */
	@Override
	public boolean setConnectionConfig(SourceConnectionConfig config)
			throws RemoteException 
	{
		s_Logger.debug("setConnectionConfig()" + config);

		m_ConnectionPool.setConnectionConfig(config);
		
		for (IntroscopePlugin plugin : getIntroscopePlugins())
		{
			try 
			{
				plugin.configure(config, new Properties());
			}
			catch (InvalidPluginPropertyException e) 
			{
			}
		}
		
		DataTypeConfig datatypeConfig = m_ConfigurationManager.getDataTypeConfig(DATATYPE);
		if (datatypeConfig == null)
		{
			datatypeConfig = new DataTypeConfig();
			datatypeConfig.setDataType(DATATYPE);
			datatypeConfig.setSourceConnectionConfig(config);
			m_ConfigurationManager.addDataTypeConfig(datatypeConfig);
			m_ConfigurationManager.saveConfigurations();
		}
		else
		{
			datatypeConfig.setSourceConnectionConfig(config);
			m_ConfigurationManager.saveConfigurations();
		}

		return true;
	}

	
	/**
	 * By default Introscope stores data at 15 second interval for
	 * 7 days and at 60 seconds for a further 23 days. Therefore valid
	 * data should be available up to 30 days before now.
	 * 
	 * The valid data range starts at time now and goes back 
	 * <code>MaxAgeOfData</code> days.
	 */
	@Override
	public CavAvailableDateRange getValidDateRange() throws RemoteException
	{
		Date now = new Date();
		
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_MONTH, -m_MaxAgeOfData); 
		
		Date start = cal.getTime();
		
		return new CavAvailableDateRange(start, now);
	}

	
	/**
	 * Returns the list of all the agents available in Introscope.
	 * The optional agentRegex parameter specifies a regex to filter 
	 * the agents by. If <code>null</code> or empty all agents 
	 * <em>excluding</em> the Custom Metric Agents are returned.
	 * To get everything pass ".*".
	 */
	@Override
	public List<String> listAgentsOnEM(String agentRegex, SourceConnectionConfig connectionParams)
	{
		s_Logger.debug("listAgentsOnEM()");
		
		try 
		{
			if (agentRegex == null || agentRegex.isEmpty())
			{
				agentRegex = NOT_CUSTOM_AGENT_REGEX;
			}
			
			IntroscopeConnection connection = m_ConnectionPool.testConnectionConfig(connectionParams);
			
			if (connection == null)
			{
				return Collections.emptyList();
			}
			
			try
			{
				return connection.listAgents(agentRegex);
			}
			finally
			{
				connection.logoff();
			}
		}
		catch (Exception e) 
		{
			s_Logger.error("Exception in listAgentsOnEM()", e);
			return Collections.emptyList();
		}
	}
	
	/**
	 * Get the list of agents the plugin has been configured to use.
	 * If the connectionParams do not match the current configured Introscope
	 * data type then an empty list is returned.
	 * @return list of agents for the specified connection configuration.
	 */
	@Override 
	public List<String> getAgents(SourceConnectionConfig connectionParams)
	{
		s_Logger.debug("getAgents() for " + connectionParams);
		
		List<String> agents = new ArrayList<String>();
		
		DataTypeConfig introscopeConfig = m_ConfigurationManager.getDataTypeConfig(DATATYPE);		
		if (introscopeConfig != null && introscopeConfig.getSourceConnectionConfig().equals(connectionParams))
		{
			Map<String, String> pluginProps = introscopeConfig.getPluginProperties();
			if (pluginProps != null)
			{
				String agentsAsStr = pluginProps.get("Agents");
				if (agentsAsStr != null)
				{
					String agentSeparator =  pluginProps.get("AgentSeparator");
					if (agentSeparator == null)
					{
						agentSeparator = ServerUtil.DELIMITER;
					}
					String[] agentsArray = StringUtils.split(agentsAsStr, agentSeparator);
					agents = Arrays.asList(agentsArray);
				}
			}	
		}
		
		return agents;
	}
	

	@Override
	public boolean resetConfiguration()
	{
		s_Logger.debug("resetConfiguration");
		
		// Wipe the configuration details.
		m_ConfigurationManager.removeDataTypeConfig(DATATYPE);
		
		// Delete the list of agents.
		String configDirectory = Proxy.getPluginsConfigDirectory();
		File agentsFile = new File(configDirectory, AGENTS_FILENAME);
		boolean agentsDeleted = true;
		if (agentsFile.exists() == true)
		{
			agentsDeleted = agentsFile.delete();
		}

		boolean queriesDeleted = true;

		if (!RunPolicy.isCannedDemo())
		{
			// delete the metric queries
			File metricFile = new File(configDirectory, METRIC_QUERIES_FILENAME);
			if (metricFile.exists() == true)
			{
				try
				{
					MetricGroup.writeAsXml(metricFile.toString(), Collections.<List<MetricGroup>>emptyList());
				}
				catch (ParserConfigurationException e)
				{
					s_Logger.error(String.format("Error writing metric queries file. " +
							"Exception = %s", e.toString()));

					queriesDeleted = false;
				}
				catch (TransformerFactoryConfigurationError e)
				{
					s_Logger.error(String.format("Error writing metric queries file. " +
							"Exception = %s", e.toString()));

					queriesDeleted = false;
				}
				catch (TransformerException e)
				{
					s_Logger.error(String.format("Error writing metric queries file. " +
							"Exception = %s", e.toString()));

					queriesDeleted = false;
				}
			}
		}

		
		return agentsDeleted && queriesDeleted;
	}
	
	
	/**
	 * Creates a list of Metric queries from the metric groups.
	 * 
	 * @param agents
	 * @return
	 */
	private List<MetricGroup> generateMetricQueries(List<String> agents)
	{
		List<MetricGroup> queries = new ArrayList<MetricGroup>();
		
		for (String agent : agents)
		{			
			String escacpedAgent = RegExUtilities.escapeRegex(agent);
			
			queries.add(new MetricGroup(escacpedAgent, m_MetricRegex, true));
		}
		
		return queries;
	}
	
	/**
	 * Generates the queries from the list of agents then calls
	 * <code>estimateCompletionTimeForQueries(List<MetricGroup> queries)</code> 
	 */
	@Override
	public AnalysisDuration estimateCompletionTime(List<String> agents, Date timeOfIncident, 
												SourceConnectionConfig connectionParams) 
	throws RemoteException
	{
		if (RunPolicy.isCannedDemo())
		{
			// Read the metric queries
			try
			{
				String configDirectory = Proxy.getPluginsConfigDirectory();
				File metricFile = new File(configDirectory, METRIC_QUERIES_FILENAME);
			
				InputStream inputStream = new FileInputStream(metricFile);
				List<MetricGroup> queries = MetricGroup.loadMetricGroups(inputStream);

				return estimateCompletionTimeForQueries(queries, timeOfIncident, connectionParams);
			}
			catch (ParserConfigurationException e)
			{
				s_Logger.error(String.format("Error reading metric queries file. " +
						"Exception = %s", e.toString()));
			}
			catch (TransformerFactoryConfigurationError e)
			{
				s_Logger.error(String.format("Error reading metric queries file. " +
						"Exception = %s", e.toString()));
			}
			catch (SAXException e)
			{
				s_Logger.error(String.format("Error reading metric queries file. " +
						"Exception = %s", e.toString()));
			}
			catch (ParseException e)
			{
				s_Logger.error(String.format("Error reading metric queries file. " +
						"Exception = %s", e.toString()));
			}
			catch (IOException e)
			{
				s_Logger.error(String.format("Error reading metric queries file. " +
						"Exception = %s", e.toString()));
			}

			return new AnalysisDuration(-1, -1, null, ErrorState.NO_DATA);
		}

		List<MetricGroup> queries = generateMetricQueries(agents);
		
		return estimateCompletionTimeForQueries(queries, timeOfIncident, connectionParams);
	}
	
	
	/**
	 * Finds the optimal query length to pull the data and 
	 * estimates the time it will take to pull all the data
	 * required for the analysis.
	 * 
	 * It a connection cannot be made to the EM or there is no
	 * data at estimate time than a error state is returned 
	 * in the AnalysisDuration object.
	 */
	@Override
	public AnalysisDuration estimateCompletionTimeForQueries(List<MetricGroup> queries, Date timeOfIncident,
												SourceConnectionConfig connectionParams) 
	throws RemoteException
	{
		s_Logger.info("estimateCompletionTimeForQueries " +  timeOfIncident + " " + connectionParams);
		
		// Get the CAV period
		CavStartEnd cavDates = DataCollectionManager.calcCavStartAndEndFromIncidentTime(timeOfIncident);
		
		// create a connection object in the pool so there is one available for the plugin.
		try 
		{
			IntroscopeConnection connection = m_ConnectionPool.testConnectionConfig(connectionParams);
			
			if (connection == null)
			{
				return new AnalysisDuration(-1, -1, null, ErrorState.CONNECTION_FAILURE);
			}
			
			
			try
			{
				AnalysisDuration optimalQuery = m_OptimalUsage.optimalQueryLength(
													connection, queries, cavDates.getStart());

				// Error getting estimate
				if (optimalQuery.getErrorState() != ErrorState.NO_ERROR)
				{
					return optimalQuery;
				}
				
				List<IntroscopePlugin> plugins = getIntroscopePlugins();
				for (IntroscopePlugin plugin : plugins)
				{
					plugin.setUsualPointIntervalSecs(optimalQuery.getActualDataPointIntervalSecs());
				}
				
				// Calc the collection period.	
				long cavDuration = cavDates.getEnd().getTime() - cavDates.getStart().getTime();
				long intervals = cavDuration / (optimalQuery.getOptimalQueryLengthSecs() * 1000);

				long estimatedTime = intervals * optimalQuery.getEstimatedAnalysisDurationMs();
				
				AnalysisDuration result = new AnalysisDuration(estimatedTime, 
												optimalQuery.getOptimalQueryLengthSecs());
				result.setActualDataPointIntervalSecs(optimalQuery.getActualDataPointIntervalSecs());
				
				return result;
			}
			catch (ConnectionException ce)
			{
				s_Logger.error("ConnectionException estimating the completion time for queries", ce);
				return new AnalysisDuration(-1, -1, null, ErrorState.CONNECTION_FAILURE);
			}
			finally
			{
				connection.logoff();
			}
			
		}
		catch (Exception e)
		{
			s_Logger.error("Error estimating CAV duration from queries", e);
			return new AnalysisDuration(-1, -1, null, ErrorState.CONNECTION_FAILURE);
		}
		
	}
	
	
	/**
	 * Returns all the Introscope plugins registered with the system.
	 * There may be more than one if time series and notifications are 
	 * being collected.
	 * 
	 * The seed plugin of the thread local variable is returned not 
	 * a duplicate.
	 * 
	 * @return
	 */
	private List<IntroscopePlugin> getIntroscopePlugins()
	{
		List<IntroscopePlugin> plugins = new ArrayList<IntroscopePlugin>();
		
		for (ThreadLocalPlugin threadPlugin : m_PluginLocator.getPlugins())
		{
			Plugin plugin = threadPlugin.getSeedPlugin();
			
			if (plugin instanceof IntroscopePlugin)
			{
				plugins.add((IntroscopePlugin)plugin);			
			}
		}
		
		return plugins;		
	}

	
	/**
	 * The Metric regex used in the Metric queries.
	 * The default value is ^(?!Heuristics).*
	 * @return
	 */
	public String getMetricRegex()
	{
		return m_MetricRegex;
	}

	public void setMetricRegex(String regex)
	{
		m_MetricRegex = regex;
	}
	
	/**
	 * The property responsible for finding the best query length
	 * for the fastest pull of historical data.
	 * @return
	 */
	public OptimalUsage getOptimalUsage()
	{
		return m_OptimalUsage;
	}
	
	public void setOptimalUsage(OptimalUsage value)
	{
		m_OptimalUsage = value;
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
	

	/**
	 * The Configuration manager.
	 * @return
	 */
	public ConfigurationManager getConfigurationManager()
	{
		return m_ConfigurationManager;
	}
	
	public void setConfigurationManager(ConfigurationManager value)
	{
		m_ConfigurationManager = value;
		
		DataTypeConfig type = m_ConfigurationManager.getDataTypeConfig(DATATYPE);
		if (type != null)
		{
			m_ConnectionPool.setConnectionConfig(type.getSourceConnectionConfig());
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
	
	public String getServerName()
	{
		return m_ServerName;
	}
	
	public void setServerName(String serverName)
	{
		m_ServerName = serverName;
	}
}
