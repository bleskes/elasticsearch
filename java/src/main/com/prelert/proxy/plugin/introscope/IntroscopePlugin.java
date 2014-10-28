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

package com.prelert.proxy.plugin.introscope;


import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Semaphore;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import com.prelert.data.Attribute;
import com.prelert.data.ConnectionStatus;
import com.prelert.data.DataSource;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.MetricPath;
import com.prelert.data.MetricTreeNode;
import com.prelert.data.Notification;
import com.prelert.data.TimeSeriesData;
import com.prelert.data.TimeSeriesDataPoint;
import com.prelert.proxy.data.SourceConnectionConfig;
import com.prelert.proxy.data.introscope.IntroscopeConnectionConfig;
import com.prelert.proxy.data.introscope.MetricGroup;
import com.prelert.proxy.encryption.PasswordEncryption;
import com.prelert.proxy.inputmanager.querymonitor.QueryMonitorPolicy;
import com.prelert.proxy.inputmanager.querymonitor.QueryTookTooLongException;
import com.prelert.proxy.plugin.ExternalPlugin;
import com.prelert.proxy.plugin.NotificationPlugin;
import com.prelert.proxy.plugin.Plugin;
import com.prelert.proxy.plugin.PluginProperty;
import com.prelert.proxy.plugin.introscope.ClwConnectionPool;
import com.prelert.proxy.plugin.introscope.IntroscopeConnection;
import com.prelert.proxy.plugin.introscope.IntroscopeConnection.ConnectionException;
import com.prelert.proxy.regex.RegExUtilities;
import com.prelert.proxy.runpolicy.RunPolicy;
import com.prelert.server.ServerUtil;


/**
 * CA Wily Introscope Plugin
 */
public class IntroscopePlugin extends Plugin implements ExternalPlugin, NotificationPlugin
{
	private static Logger s_Logger = Logger.getLogger(IntroscopePlugin.class);
	
	/**
	 * Where a call to getNotifications() takes to long the this value will
	 * be passed back in the QueryTookTooLongExcpetion as a suggested time 
	 * for the manager thread to back of for.
	 */
	public static final long SERVER_OVERLOADED_BACK_OFF_PERIOD_MS = 5 * 60 * 1000;
	
	/*
	 * Limit the number of GUI threads which can make calls to Introscope 
	 * through the getDataPointsForTimeSpan(..) functions. The default is 
	 * DEFAULT_GUI_ACCESS_THREADS_COUNT but it is a configuration parameter.
	 */
	static final private Object s_SemaphoreLock = new Object();
	static private Semaphore s_GuiAccessSemaphore;
	static final private int DEFAULT_GUI_ACCESS_THREADS_COUNT = 4;
	
	private IntroscopeMetricCache m_MetricCache = new IntroscopeMetricCache();
	private IntroscopeDataPointCache m_DataPointCache = new IntroscopeDataPointCache();
	// Load Monitor
	private IntroscopeLoadMonitor m_LoadMonitor = new IntroscopeLoadMonitor();
	// Track number of points through the plugin
	private Map<String, Integer> m_DataSourceCountMap = new HashMap<String, Integer>();
	
	static volatile private int s_QueryHoursOffset = 0;
	
	/*
	 * Attribute Names
	 */
	public static final String AGENT_ATTRIBUTE = "Agent";
	public static final String PROCESS_ATTRIBUTE = "Process";
	public static final String METRIC_GROUP_ATTRIBUTE = "MetricGroup";
	public static final String RESOURCE_PATH_ATTRIBUTE = "ResourcePath";
	public static final String MANAGEMENT_MODULE_ATTRIBUTE = "Management Module";
	public static final String LOCATION_ATTRIBUTE = "Location";
	public static final String ALERT_PREVIOUS_STATUS = "Previous Status";
	public static final String PATH_SEPARATOR = "|";
	public static final String METRIC_SEPARATOR = ":";
	public static final String TYPE_FIELD_NAME = "type";
	public static final String SOURCE_FIELD_NAME = "source";
	public static final String METRIC_FIELD_NAME = "metric";
	
	/**
	 * Introscope Notifications and Metrics default to these type names
	 * but the type name can be change in the properties file.
	 */
	public static final String DEFAULT_METRIC_DATATYPE = "Introscope";
	public static final String DEFAULT_NOTIFICATIONS_DATATYPE = "Introscope Alerts";
	public static final String DATATYPE_SUFFIX = " : ";
	
	
	/**
	 * The static set of properties for this pluign.
	 */
	static private final List<PluginProperty> s_Properties = new ArrayList<PluginProperty>();
	static 
	{
		s_Properties.add(new PluginProperty("Agents", false));
		s_Properties.add(new PluginProperty("Interval", false));
		s_Properties.add(new PluginProperty("MetricRegex", false));
	}
	
	/**
	 * The list of agents for the EM.
	 */
	private List<String> m_Agents; 
	
	
	private int m_PointInterval;
	
	private List<AlertGroup> m_AlertGroups;
	
	private List<MetricGroup> m_MetricGroups;
	
	private DataSourceCategory m_DataSourceCategory;
	
	// Track the time of the latest piece of data seen by data type.
	private static Map<DataSourceType, Date> s_LatestTimeByDataType 
					= new HashMap<DataSourceType, Date>();
	
	
	/**
	 * The CLW Connection pool. This object will be 
	 * shared with any clones of this class.
	 */
	private ClwConnectionPool m_ClwConnectionPool;

	
	public IntroscopePlugin()
	{
		super();

		m_MetricGroups = new ArrayList<MetricGroup>();
		
		m_AlertGroups = new ArrayList<AlertGroup>();		
		
		m_ClwConnectionPool = new ClwConnectionPool();
		m_LoadMonitor.setClwConnectionPool(m_ClwConnectionPool);
		
		m_Agents = new ArrayList<String>();
	}	
	

	/**
	 * Loads the properties and creates the Introscope connection objects.
	 * 
	 * </br></br>
	 * The following is the set of valid properties:
	 * <dl>
	 * <dt>'Agents'</dt><dd> - A list of introscope agents separated by the string {@link com.prelert.server.ServerUtil#DELIMITER} 
	 * 				or the value of the 'AgentSepartor' property. If not specified the QueriesFile property should be set.</dd>
	 * <dt>'AgentSeparator'</dt><dd> - The string used to separate the individual agents in
	 * 					the 'Agents' property. If not set {@link com.prelert.server.ServerUtil#DELIMITER} is used.</dd>
	 * <dt>'QueriesFile'</dt><dd> - The name of the xml file that contains a list of Xml metric_group elements
	 * 							which are used as the metric queries. This overrides the 'Agents' property.
	 * 							The file can be anywhere on the class path.</dt>
	 * <dt>'minimumOpenConnections'</dt><dd> - Sets the minimum number of connections to be  
	 * 							  kept open in the connection pool.</dd>
	 * <dt>'Interval'</dt><dd> - The smallest period between consecutive time series points.
	 * 				This values must be a multiple of 15.
	 * 				Defaults to DEFAULT_TIME_SERIES_POINT_INTERVAL.</dd>
	 * <dt>'maxGuiConnections'</dt><dd> - The maximum number of gui connections that will be  
	 * 			    		 handled by the Introscope plugins. This is not a per
	 *                       plugin option. If multiple instances of this class
	 *                       are created the first one to read its properties will 
	 *                       set this value. 
	 * <dt>'AlertQueriesFile'</dt><dd> - name of the file containing a list of Management 
	 *                      modules/agents for which alerts are queried.</dt>
	 * </dl>     
	 */
	@Override
	public boolean configure(SourceConnectionConfig config, Properties properties)
	throws InvalidPluginPropertyException
	{	
		setConnectionConfig(config);
		
		setProperties(properties);
			
		// Set the minimum connection count 
		String minimumOpenConnections = properties.getProperty("minimumOpenConnections");
		if (minimumOpenConnections != null)
		{
			try
			{
				int minConnectionCount = Integer.parseInt(minimumOpenConnections);
				m_ClwConnectionPool.setMinOpenConnectionCount(minConnectionCount);
			}
			catch (NumberFormatException nfe)
			{
				s_Logger.error("Invalid value for 'minimumOpenConnections' = " + minimumOpenConnections);
			}
		}
		
		
		String agentsProp = properties.getProperty("Agents");
		if (agentsProp != null)
		{
			String agentSeparator = properties.getProperty("AgentSeparator");
			if (agentSeparator == null)
			{
				agentSeparator = ServerUtil.REGEX_SAFE_DELIMITER;	
			}
			m_Agents = Arrays.asList(agentsProp.split(agentSeparator));
			
			String metricRegex = properties.getProperty("MetricRegex");
			if (metricRegex == null)
			{
				metricRegex = CatalogueIntroscope.ALL_NO_HEURISTIC_METRICS_REGEX;
			}
			
			List<MetricGroup> queries = generateMetricQueries(m_Agents, metricRegex);
			m_MetricGroups.addAll(queries);
		}

		
		String intervalProp = properties.getProperty("Interval");
		if (intervalProp != null)
		{
			try
			{
				m_PointInterval = Integer.parseInt(intervalProp);
			}
			catch (NumberFormatException nfe)
			{
				m_PointInterval = IntroscopeConnection.DEFAULT_INTERVAL;
			}
		}
		else 
		{
			m_PointInterval = IntroscopeConnection.DEFAULT_INTERVAL;
		}

		String host = properties.getProperty("host");
		String username = properties.getProperty("username");
		String password = properties.getProperty("password");
		int port = IntroscopeConnectionConfig.DEFAULT_CLW_PORT;
		try
		{
			port = Integer.parseInt(properties.getProperty("port"));
		}
		catch (NumberFormatException nfe)
		{
		}
		
		if (password == null)
		{
			// look for the encrypted password
			String encryptedPassword = properties.getProperty("password*");
			if (encryptedPassword != null)
			{
				try
				{
					password = PasswordEncryption.decryptPassword(encryptedPassword);
				}
				catch (Exception e)
				{
					s_Logger.error("Error Decrypting password");
				}
			}
		}
		
		
		if (host != null && username != null && password != null)
		{
			SourceConnectionConfig connectionConfig = new IntroscopeConnectionConfig();

			connectionConfig.setHost(host);
			connectionConfig.setUsername(username);
			connectionConfig.setPassword(password);
			connectionConfig.setPort(port);
			
			setConnectionConfig(connectionConfig);
		}
		
		
		// Try to load the metric and alert queries. 
		String queriesFile = properties.getProperty("QueriesFile");
		String alertQueriesFile = properties.getProperty("AlertQueriesFile");
		
		if (RunPolicy.isCannedDemo())
		{
			loadMetricQueries("metric_queries.xml");
		}
		else 
		{
			loadMetricQueries(queriesFile);
			loadAlertQueries(alertQueriesFile);
		}


		int guiConcurrentConnectionsCount = DEFAULT_GUI_ACCESS_THREADS_COUNT;
		String connectionCountProp = properties.getProperty("maxGuiConnections");
		if (connectionCountProp != null)
		{
			try
			{
				guiConcurrentConnectionsCount = Integer.parseInt(connectionCountProp);
			}
			catch (NumberFormatException nfe)
			{
				guiConcurrentConnectionsCount = DEFAULT_GUI_ACCESS_THREADS_COUNT;
			}
		}
		
		
		validateConfig();

		createSemaphore(guiConcurrentConnectionsCount);
		
		return true;
	}
	
	
	/**
	 * @param properties Not used - can be <code>null</code>.
	 */
	@Override
	public ConnectionStatus testConnection(SourceConnectionConfig config, Properties properties)
	{
		IntroscopeConnection conn = m_ClwConnectionPool.testConnectionConfig(config);
		try
		{
			if (conn == null)
			{
				return new ConnectionStatus(ConnectionStatus.Status.CONNECTION_FAILED);
			}
			else
			{
				return new ConnectionStatus(ConnectionStatus.Status.CONNECTION_OK);			
			}
		}
		finally
		{
			if (conn != null)
			{
				conn.logoff();
			}
		}
	}
	
	
	/**
	 * Creates a list of Metric queries from the list of agents
	 * with the specified regex. 
	 * 
	 * @param agents - List of agents. Each agent will have any special
	 * 	regex characters escaped.
	 * @param metricRegex - The regex that will be used in the metric queries.
	 * @return
	 */
	private List<MetricGroup> generateMetricQueries(List<String> agents, String metricRegex)
	{
		List<MetricGroup> queries = new ArrayList<MetricGroup>();
		
		for (String agent : agents)
		{			
			String escacpedAgent = RegExUtilities.escapeRegex(agent);
			
			queries.add(new MetricGroup(escacpedAgent, metricRegex, true));
		}
		
		return queries;
	}
	
	/**
	 * Load the alert queries from the file.
	 * 
	 * @param alertQueriesFilename
	 * @throws InvalidPluginPropertyException
	 */
	private void loadAlertQueries(String alertQueriesFilename) throws InvalidPluginPropertyException
	{
		// Try to load the alert modules. 
		if (alertQueriesFilename != null && !alertQueriesFilename.isEmpty())
		{
			InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream(alertQueriesFilename);
			if (inputStream == null)
			{
				throw new InvalidPluginPropertyException("Cannot locate Alert Queries File '" +
											alertQueriesFilename + "'.");
			}
			
			try
			{
				m_AlertGroups = AlertGroup.loadAlertGroups(inputStream);
				
				m_DataSourceCategory = DataSourceCategory.NOTIFICATION;
				setDataSourceType(new DataSourceType(DEFAULT_NOTIFICATIONS_DATATYPE, DataSourceCategory.NOTIFICATION));
			}
			catch (ParseException pe)
			{
				throw new InvalidPluginPropertyException("Introscope Plugin cannot " +
						"parse the file '" + alertQueriesFilename + "'.\n" +
						pe.getMessage());
			}
			catch (ParserConfigurationException e1) 
			{
				throw new InvalidPluginPropertyException("Introscope Plugin cannot " +
							"instantiate DocumentBuilder");
			}
	        catch (SAXException e1) 
	        {
				throw new InvalidPluginPropertyException("Introscope Plugin cannot " +
										 "parse the file '" + alertQueriesFilename + "'.");
			} 
	        catch (IOException e1) 
	        {
				throw new InvalidPluginPropertyException("Introscope Plugin cannot " +
								"read the file '" + alertQueriesFilename + "'.");
			}
	
		}
	}
	
	
	/**
	 * Load alert queries from file.
	 * 
	 * @param metricGroupingsFilename
	 * @throws InvalidPluginPropertyException
	 */
	private void loadMetricQueries(String metricGroupingsFilename) throws InvalidPluginPropertyException
	{
		if (metricGroupingsFilename != null && !metricGroupingsFilename.isEmpty())
		{
			m_DataSourceCategory = DataSourceCategory.TIME_SERIES;
			setDataSourceType(new DataSourceType(DEFAULT_METRIC_DATATYPE, DataSourceCategory.TIME_SERIES));
			
			InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream(metricGroupingsFilename);
			if (inputStream == null)
			{
				throw new InvalidPluginPropertyException("Cannot locate Metric Groupings File '" +
						metricGroupingsFilename + "'.");
			}

			try
			{
				s_Logger.debug("loading metric groups");
				List<MetricGroup> metricGroups = MetricGroup.loadMetricGroups(inputStream);
				s_Logger.debug("loaded " + metricGroups.size() + " metric groups");
				
				m_MetricGroups.clear();
				m_MetricGroups.addAll(metricGroups);
			}
			catch (ParseException pe)
			{
				throw new InvalidPluginPropertyException("Introscope Plugin cannot " +
						"parse the file '" + metricGroupingsFilename + "'.\n" +
						pe.getMessage());
			}
			catch (ParserConfigurationException e1) 
			{
				throw new InvalidPluginPropertyException("Introscope Plugin cannot " +
							"instantiate DocumentBuilder");
			}
	        catch (SAXException e1) 
	        {
				throw new InvalidPluginPropertyException("Introscope Plugin cannot " +
										 "parse the file '" + metricGroupingsFilename + "'.");
			} 
	        catch (IOException e1) 
	        {
				throw new InvalidPluginPropertyException("Introscope Plugin cannot " +
								"read the file '" + metricGroupingsFilename + "'.");
			}
			
		}
	}
	
	
	/**
	 * The plugin is only configured when the connection is 
	 * valid and either Notification queries or metric queries 
	 * have been set.
	 * 
	 * @throws InvalidPluginPropertyException
	 * @return true if valid.
	 */
	private boolean validateConfig()  throws InvalidPluginPropertyException
	{
		if (getDataSourceType() == null)
		{
			m_DataSourceCategory = DataSourceCategory.TIME_SERIES;
			setDataSourceType(new DataSourceType(DEFAULT_METRIC_DATATYPE, DataSourceCategory.TIME_SERIES));
		}

		
		if ((m_PointInterval % IntroscopeConnection.DEFAULT_INTERVAL) != 0)
		{
			throw new InvalidPluginPropertyException("Invalid Property: 'interval'" +
												"must be a multiple of 15 seconds.");
		}
		
		boolean queriesOk = (m_AlertGroups != null && m_AlertGroups.size() > 0) ||
		(m_MetricGroups != null && m_MetricGroups.size() > 0);
		
		if (queriesOk == false)
		{
			throw new InvalidPluginPropertyException("No queries have been set on the plugin " + getName());
		}

		boolean configured = queriesOk && m_ClwConnectionPool.isConfigured();
		
		setConfigured(configured);
		
		return configured;
	}
	

	/**
	 * Sets the connection params and creates a new connection 
	 * then validates the configuration properties.
	 * 
	 * @param config
	 * @result returns true if it connects successfully.
	 */
	private boolean setConnectionConfig(SourceConnectionConfig config)
	{
		// Set the connection config in the connection pool
		boolean result = m_ClwConnectionPool.setConnectionConfig(config);

		try 
		{
			if (validateConfig())
			{
				s_Logger.info("Plugin properties validated successfully.");
			}
		} 
		catch (InvalidPluginPropertyException e) 
		{
		}
		
		return result;
	}


	/**
	 * Returns the metric queries for the DEFAULT_METRIC_DATATYPE
	 * @return
	 */
	public List<MetricGroup> getMetricQueries()
	{
		return m_MetricGroups;
	}
	

	/**
	 * Create the shared static instance of the semaphore..
	 * @param maxGuiThreadCount max number of gui connections which can
	 * 			access Introscope through the getDataPointsForTimeSpan().
	 */
	private void createSemaphore(int maxGuiThreadCount)
	{
		// Be careful not to create the semaphore more than once			
		if (s_GuiAccessSemaphore != null)
		{
			return;
		}

		// synchronise on the semaphore lock.
		synchronized(s_SemaphoreLock)
		{
			// double check
			if (s_GuiAccessSemaphore != null)
			{
				return;
			}

			s_GuiAccessSemaphore = new Semaphore(maxGuiThreadCount);
		}
	}


	/**
	 * Get a list of all the source machines this plugin currently knows about
	 * and might provide data for.
	 * @param type A data type to restrict the sources retrieved.  (This plugin
	 *             would only expect to be asked about the data type it's
	 *             providing though.)
	 * @return A list of all the source machines this plugin currently knows
	 *         about and might provide data for.
	 */
	public List<DataSource> getDataSources(DataSourceType type)
	{
		s_Logger.debug("getDataSources() type = " + type);
		
		List<DataSource> sources = new ArrayList<DataSource>();

		if (getDataSourceType().equals(type))
		{
			switch (type.getDataCategory())
			{
				case TIME_SERIES:
				{
					Collection<String> timeSeriesSources = listTimeSeriesSources();
					for (String timeSeriesSource : timeSeriesSources)
					{
						DataSource source = new DataSource();
						source.setDataSourceType(type);
						source.setSource(timeSeriesSource);
						source.setExternalPlugin(getName());
						
						synchronized(m_DataSourceCountMap)
						{
							int count = -1; // unknown
							if (m_DataSourceCountMap.containsKey(timeSeriesSource))
							{
								count = m_DataSourceCountMap.get(timeSeriesSource);
							}
							
							source.setCount(count);
						}

						sources.add(source);
					}
					break;
				}

				case NOTIFICATION:
				{
					throw new IllegalArgumentException(
							"Introscope plugin cannot return Notification data sources. " +
							"Invalid state Notification sources should never be requested.");
				}

				default:
				{
					s_Logger.error("Introscope plugin for types: " +
									getDataSourceType() + 
									" has been asked for sources for type " +
									type.toString());
					break;
				}
			}
		}
		else
		{
			s_Logger.error("Introscope plugin for types " + getDataSourceType() +
							" has been asked for sources for type " +
							type.toString());
		}

		return sources;
	}
	
	@Override
	public int getDataSourceItemCount(DataSource source)
	{
		synchronized(m_DataSourceCountMap)
		{
			int count = -1; // unknown
			if (m_DataSourceCountMap.containsKey(source.getSource()))
			{
				count = m_DataSourceCountMap.get(source.getSource());
			}
			
			return count;
		}
	}
	
	
	/**
	 * Returns the number of points that have been processed 
	 * for the given DataSourceType.
	 * 
	 * @param type
	 * @return the number of points processed or -1 if unknown.
	 */
	public int getDataTypeItemCount(DataSourceType type)
	{
		synchronized(m_DataSourceCountMap)
		{
			if (m_DataSourceCountMap.size() == 0)
			{
				return -1;
			}
			
			int count = 0;
			for (Integer pointCount : m_DataSourceCountMap.values())
			{
				count += pointCount;
			}

			return count;
		}
	}
	
	/**
	 * Returns the next level of the tree for Introscope
	 * time series metrics.
	 * If the datatype notifications then a <code>IllegalArgumentException</code>
	 * is thrown.
	 * 
	 * @param datatype
	 * @param previousPath
	 * @param currentValue
	 * @param opaqueNum - Unused
	 * @param opaqueStr - Unused
	 * @return
	 */
	@Override
	public List<MetricTreeNode> getDataSourceTreeNextLevel(String datatype, 
													String previousPath,
													String currentValue,
													int opaqueNum,
													String opaqueStr)
	{	
		List<MetricTreeNode> nodes;
		
		s_Logger.debug(String.format("getDataSourceTreeNextLevel(%s, %s, %s)", datatype, previousPath,
										currentValue));
		
		if (DEFAULT_METRIC_DATATYPE.equals(datatype))
		{
			nodes = getTimeSeriesDataSourceTreeNextLevel(datatype, previousPath,
											currentValue);
		}
		else if (DEFAULT_NOTIFICATIONS_DATATYPE.equals(datatype))
		{
			throw new IllegalArgumentException(
					"Introscope plugin does not support listing notification sources in the Metric tree. " +
					"Invalid state Notification tree path should never be requested.");
		}
		else
		{
			s_Logger.error("getDataSourceTreeNextLevel(): unknown datatype = " +  datatype);
			return Collections.emptyList();
		}
		
		Collections.sort(nodes);
		
		return nodes;		
	}
	
	/**
	 * For the given previousPath and currentValue return all
	 * child node values for the tree node these two parameters 
	 * represent. previousPath and currentValue combine to make a path 
	 * in the metric tree.
	 * 
	 * @param datatype
	 * @param previousPath - may be <code>null</code> or empty in which case
	 * 						 the data sources are returned.
	 * @param currentValue - If empty then all the children under previousPath
	 *  					 are returned, i.e. the siblings of currentValue.
	 * @return
	 */
	private List<MetricTreeNode> getTimeSeriesDataSourceTreeNextLevel(String datatype, 
													String previousPath, String currentValue)
	{
		List<MetricTreeNode> nodes = new ArrayList<MetricTreeNode>();
		
		// If all previousPath is null or empty then return the sources.
		if (previousPath == null || previousPath.isEmpty())
		{
			Collection<String> sources = listTimeSeriesSources();
			for (String source : sources)
			{
				nodes.add(createTreeNode(source, datatype, SOURCE_FIELD_NAME, 
										DEFAULT_METRIC_DATATYPE, source, true, null, 
										DataSourceCategory.TIME_SERIES_FEATURE, null));
			}
						
			return nodes;
		}
		
		List<String> pathNodes = new ArrayList<String>();
		
		String [] pathSplit = previousPath.split("\\" + PATH_SEPARATOR);
		for (String split : pathSplit)
		{
			if (!split.isEmpty())
			{
				pathNodes.add(split);
			}
		}
		
		if (!currentValue.isEmpty())
		{
			pathNodes.add(currentValue);
		}
		
		// Remove the first node which is the typename 'Introscope : '
		String firstElement = pathNodes.get(0);
		if (firstElement.equals(DEFAULT_METRIC_DATATYPE))
		{
			pathNodes.remove(0);
		}
		else
		{
			int lastIndex = firstElement.lastIndexOf(DATATYPE_SUFFIX);
			pathNodes.set(0, firstElement.substring(lastIndex + DATATYPE_SUFFIX.length()));
		}
		
		
		List<IntroscopeMetricCache.MetricNode> children = m_MetricCache.childValuesForPath(pathNodes);
		
		
		// Create the new path value for the returned nodes.
		String newPath = previousPath;
		if (!currentValue.isEmpty())
		{
			if (previousPath.equals(DEFAULT_METRIC_DATATYPE))
			{
				newPath += DATATYPE_SUFFIX + currentValue;
			}
			else
			{
				newPath += PATH_SEPARATOR + currentValue;
			}
		}
		
		String source = null;
		List<Attribute> attributes = new ArrayList<Attribute>();
		for (int i=0; i<pathNodes.size(); i++)
		{
			switch (i)
			{
			case 0:
				source = pathNodes.get(0);				
				break;
			case 1:
				attributes.add(new Attribute(IntroscopePlugin.PROCESS_ATTRIBUTE, pathNodes.get(1),
												IntroscopePlugin.PATH_SEPARATOR, 1));
				break;
			case 2:
				attributes.add(new Attribute(IntroscopePlugin.AGENT_ATTRIBUTE, pathNodes.get(2),
												IntroscopePlugin.PATH_SEPARATOR, 2));
				break;
			default: 
				attributes.add(new Attribute(IntroscopePlugin.RESOURCE_PATH_ATTRIBUTE + (i - 3), pathNodes.get(i),
												IntroscopePlugin.PATH_SEPARATOR, i));
			}
		}
		
		
		String defaultTypename;
		switch (pathNodes.size() + 1) // + 1 as want name of level below this i.e. currentValue
		{
		case 0:
		case IntroscopeMetricCache.HOST_LEVEL: defaultTypename = "Source";
			break;
		case IntroscopeMetricCache.PROCESS_LEVEL: defaultTypename = PROCESS_ATTRIBUTE;
			break;
		case IntroscopeMetricCache.AGENT_LEVEL: defaultTypename = AGENT_ATTRIBUTE;
			break;
		default: 
			defaultTypename = RESOURCE_PATH_ATTRIBUTE + (pathNodes.size() - IntroscopeMetricCache.AGENT_LEVEL);
		}
		
		for (IntroscopeMetricCache.MetricNode node : children)
		{
			// Only set some vales if the node is a metric.
			String typeName = defaultTypename;
			String metric = null;
			List<Attribute> attrs = null;
			
			if (node.isLeaf())
			{
				typeName = "Metric";
				metric = node.getValue();
				attrs = attributes;
			}
			
			nodes.add(createTreeNode(node.getValue(), datatype, 
									typeName, newPath, source, false, metric, 
									DataSourceCategory.TIME_SERIES_FEATURE,
									attrs));
		}
		
		
		return nodes;
	}
		
	
	/**
	 * Returns the child nodes for the parent of the path upto previous path
	 * i.e. the siblings of the node specified by by previous path. 
	 * 
	 * If datatype and previousPath are null then an empty list is 
	 * returned.
	 * 
	 * @param datatype
	 * @param previousPath
	 * @param opaqueNum - Unused
	 * @param opaqueStr - Unused
	 * @return
	 */
	@Override
	public List<MetricTreeNode> getDataSourceTreePreviousLevel(String datatype, String previousPath,
												int opaqueNum,String opaqueStr)
	{
		s_Logger.debug(String.format("getDataSourceTreePreviousLevel(%s, %s)", 
											datatype, previousPath));
		
		if (datatype == null || previousPath == null || previousPath.isEmpty())
		{
			return Collections.emptyList();
		}
		
		String parentNode = "";
		String path = "";
		
		String [] pathNodes = previousPath.split("\\" + PATH_SEPARATOR);
		if (pathNodes.length == 1)
		{
			parentNode = pathNodes[0];

			// set the path to the datatype 'Introscope ... : '
			// so only data sources will be returned from getNextLevel
			if (parentNode.startsWith(datatype))
			{
				parentNode = datatype + DATATYPE_SUFFIX;
			}
		}
		else
		{
			parentNode = pathNodes[pathNodes.length-2];
			for (int i=0; i<pathNodes.length -2; ++i)
			{
				if (i == pathNodes.length -3) 
				{
					path += pathNodes[i];
				}
				else
				{
					path += pathNodes[i] + PATH_SEPARATOR;
				}
			}
			
			// If parent node is like 'Introscope : someSource' then
			// split it into previous path and parent node becomes the source
			if (path.isEmpty() && parentNode.startsWith(datatype))
			{
				path = datatype;
				
				int lastIndex = parentNode.lastIndexOf(DATATYPE_SUFFIX);
				parentNode = parentNode.substring(lastIndex + DATATYPE_SUFFIX.length());
			}
		}
		
				
		return getDataSourceTreeNextLevel(datatype, path, parentNode, opaqueNum, opaqueStr);
	}
	
	
	/**
	 * Returns all the nodes of the tree under <code>previousPath</code>.
	 * 
	 * @param datatype
	 * @param previousPath
	 * @param opaqueNum - Unused
	 * @param opaqueStr - Unused
	 * @return
	 */
	@Override
	public List<MetricTreeNode> getDataSourceTreeCurrentLevel(String datatype, String previousPath,
											int opaqueNum,String opaqueStr)
	{
		s_Logger.debug(String.format("getDataSourceTreeCurrentLevel(%s, %s)", 
											datatype, previousPath));
		
		if (datatype == null || previousPath == null || previousPath.isEmpty())
		{
			return Collections.emptyList();
		}
		

		return getDataSourceTreeNextLevel(datatype, previousPath, "", opaqueNum, opaqueStr);
	}
		

	/**
	 * Creates a MetricTreeNode from the parameters.
	 * @param value
	 * @param datatype
	 * @param typeName
	 * @param partialPath
	 * @param source - if not <code>null</code> specifies the source
	 * @param isSource - True if this is a source node.
	 * @param metric - if not <code>null</code> specifies the metric
	 * @param category
	 * @param attributes - May be <code>null</code>
	 * @return
	 */
	private MetricTreeNode createTreeNode(String value, String datatype, 
							String typeName, String partialPath,
							String source, boolean isSource,
							String metric,
							DataSourceCategory category,
							List<Attribute> attributes)
	{
		MetricTreeNode node = new MetricTreeNode();
		
		boolean isMetric = metric != null;
		
		node.setName(typeName);
		node.setValue(value);
		if (isSource)
		{
			node.setPrefix(DATATYPE_SUFFIX);
		}
		else
		{
			node.setPrefix(isMetric ? METRIC_SEPARATOR : PATH_SEPARATOR);
		}

		node.setPartialPath(partialPath); 

		node.setOpaqueNum(-1);
		node.setOpaqueStr("");

		node.setType(datatype);
		node.setCategory(category);

		if (source != null)
		{
			node.setSource(source);
		}

		node.setIsLeaf(isMetric);

		if (isMetric)
		{
			node.setMetric(metric);
			try
			{
				node.setExternalKey(AgentMetricPair.createExternalKey(metric,
																	source,
																	attributes));
			}
			catch (IllegalArgumentException e)
			{
				s_Logger.warn("Problem getting external key at leaf of metric path tree: " + e);
			}
		}

		if (attributes != null)
		{
			node.setAttributes(attributes);
		}
		else
		{
			node.setAttributes(Collections.<Attribute>emptyList());
		}

		return node;
	}

	
	/*
	 * Notifications.
	 */
	@Override
	public List<Notification> getNotifications(Date start, Date end) throws QueryTookTooLongException
	{
		s_Logger.debug("getNotifications() start = " + start + ", end = " + end);
		
		List<Notification> result = new ArrayList<Notification>();

		QueryMonitorPolicy monitorPolicy = getQueryMonitorPolicy();
		
		Date queryStart = start;
		Date queryEnd = end;
		if (s_QueryHoursOffset != 0)
		{
			Calendar calendar = Calendar.getInstance();
			
			calendar.setTime(start);
			calendar.add(Calendar.HOUR, s_QueryHoursOffset);
			queryStart = calendar.getTime();
			
			calendar.setTime(end);
			calendar.add(Calendar.HOUR, s_QueryHoursOffset);
			queryEnd = calendar.getTime();
			
			s_Logger.debug("Adjusting query time by " + s_QueryHoursOffset + " hour(s). " +
					"Adjusted start = " + queryStart + ", adjusted end = " + queryEnd);
		}
		
		IntroscopeConnection connection;
		try
		{
			connection = m_ClwConnectionPool.acquireConnection();
		}
		catch (Exception e)
		{
			s_Logger.error("Could not acquire connection from pool: " + 
					m_ClwConnectionPool.getConnectionConfig());
			
			return result;
		}
		
		try
		{
			connection.setAlertDataType(DEFAULT_NOTIFICATIONS_DATATYPE);

			for (AlertGroup alertQuery : m_AlertGroups)
			{
				result.addAll(connection.getAlerts(queryStart, queryEnd, 
								alertQuery.getModule(), alertQuery.getAgentIdentifier(), 
								alertQuery.getAlertIdentifier()));
				
				if (result.size() > 0)
				{
					Notification note = result.iterator().next();
					s_QueryHoursOffset = IntroscopeConnection.calcDstAdjustment(queryStart.getTime(), 
																				note.getTimeMs());
				}

				if (Thread.interrupted())
				{			
					s_Logger.info("Introscope getNotifications() Thread has been interrupted.");
					break;
				}

				if (!monitorPolicy.wasQueryInsideTimeLimit())
				{
					throw new QueryTookTooLongException("Introscope " +  connection.getHostUsernameString() +
							" getNotifications() has taken longer than allowed" +
							" to finish. Returning incomplete results.",
							result, SERVER_OVERLOADED_BACK_OFF_PERIOD_MS);
				}
			}
		}
		catch (ConnectionException e) 
		{
			s_Logger.error("Connection error in getNotifications()");
		}
		finally 
		{
			m_ClwConnectionPool.releaseConnection(connection);
		}

		s_Logger.debug("Finished getNotifications()");

		return result;
	}


	/*
	 *  Time Series methods
	 */

	@Override
	public List<String> getAttributeNames(String datatype) 
	{
		s_Logger.debug("getAttributeNames: " + datatype);
		
		List<String> names = new ArrayList<String>();

		names.add(PROCESS_ATTRIBUTE);
		names.add(AGENT_ATTRIBUTE);
				
		return names;
	}

	@Override
	public List<String> getAttributeValues(String datatype, String attributeName, String source) 
	{
		s_Logger.debug("getAttributeValues() datatype=" + datatype
					+ ", attributeName=" + attributeName + ", source=" + source);
		

		List<String> values = new ArrayList<String>();

		if (attributeName.equals(PROCESS_ATTRIBUTE))
		{
			values = listProcesses(datatype, source);
		}
		else if (attributeName.equals(AGENT_ATTRIBUTE))
		{
			values = listAgents(datatype, source);
		}

		return values;
	}


	/**
	 * Returns a list of all the <code>Attribute</code>s associated with a
	 * specific time series, identified by its external key.
	 * @param externalKey The external key of the time series.
	 * @return List of <code>Attribute</code>s (name, value pairs)
	 */
	
	@Override
	public List<Attribute> getAttributesForKey(String externalKey)
	{
		s_Logger.debug("getAttributesForKey() key = " + externalKey);
		
		List<Attribute> attributes = new ArrayList<Attribute>();

		try
		{
			String [] agentMetric = externalKey.split(AgentMetricPair.EXTERNAL_KEY_JOIN_STRING);
			if (agentMetric.length != 2)		
			{
				throw new ParseException("Could not parse external key: " 
									+ externalKey, 0);
			}
			
			HostProcessAgent hostProcAgent = new HostProcessAgent(agentMetric[0]);			
			String process = hostProcAgent.getProcess();
			String agent = hostProcAgent.getAgent();
			
			String metricPath = "";
			String [] metricSplit = agentMetric[1].split(METRIC_SEPARATOR);
			if (metricSplit.length > 1)
			{
				metricPath = metricSplit[0]; // metricSplit[metricSplit-1] is the actual metric name.
			}
			
			attributes.add(new Attribute(AGENT_ATTRIBUTE, agent));
			attributes.add(new Attribute(PROCESS_ATTRIBUTE, process));
			
			// Add ResourcePath attributes
			String [] resources = metricPath.split("\\|");
			for (int i=0; i<resources.length; i++)
			{
				attributes.add(new Attribute(RESOURCE_PATH_ATTRIBUTE + i, resources[i]));
			}
		}
		catch (ParseException pe)
		{
			s_Logger.error("Invalid argument externalKey = '"
					+ externalKey + "'. Cannot parse externalKey");
			s_Logger.error(pe);
		}

		
		s_Logger.debug("getAttributesForKey() = " + attributes);
		
		return attributes;
	}
	
	
	@Override
	public MetricPath metricPathFromExternalKey(String datatype,
												String externalKey) 
	{
		MetricPath metricPath = new MetricPath();
		
		try
		{
			String [] agentMetric = externalKey.split(AgentMetricPair.EXTERNAL_KEY_JOIN_STRING);
			if (agentMetric.length != 2)		
			{
				throw new ParseException("Could not parse external key: " 
									+ externalKey, 0);
			}
			
			
			String resourcePath = "";
			String [] resourceSplit = agentMetric[1].split(METRIC_SEPARATOR);
			if (resourceSplit.length > 1)
			{
				resourcePath = resourceSplit[0]; // resourceSplit[resourceSplit.length-1] is the actual metric name.
			}
			String metric = resourceSplit[resourceSplit.length-1];
			
			
			metricPath.setDatatype(datatype);
			metricPath.setLastLevelName("Metric");
			metricPath.setLastLevelValue(metric);
			metricPath.setLastLevelPrefix(METRIC_SEPARATOR);
			
			String partialPath = datatype + DATATYPE_SUFFIX;
			partialPath += agentMetric[0];
			if (!resourcePath.isEmpty())
			{
				partialPath += PATH_SEPARATOR +  resourcePath;
			}
			metricPath.setPartialPath(partialPath);
			

		}
		catch (ParseException pe)
		{
			s_Logger.error("Invalid argument externalKey = '"
					+ externalKey + "'. Cannot parse externalKey");
			s_Logger.error(pe);
		}
		
		return metricPath;			
	}


	/**
	 * Returns the lastest time a data point occurred for the datatype and 
	 * source.
	 * 
	 * If LatestTime has be set (set if getAllDataPointsForTimeSpan() has 
	 * been called and returned data) then that is returned else 
	 * the time now is returned.
	 * 
	 * @param datatype
 	 * @param source - this parameter is not used.
 	 * @return The time of the latest point or now if no data has been seen.
	 */
	@Override
	public Date getLatestTime(String datatype, String source) 
	{
		if (RunPolicy.isCannedDemo())
		{
			try
			{
				DateFormat df = new SimpleDateFormat("dd MMM yyyy kk:mm:ss z");
				return df.parse("29 Feb 2012 13:00:00 GMT");
			}
			catch (ParseException e)
			{
				s_Logger.error("Cannot parse date " + e);
			}
		}

		Date latest = null;
		synchronized (s_LatestTimeByDataType)
		{
			latest = s_LatestTimeByDataType.get(getDataSourceType());
		}
		
		if (latest == null)
		{
			return new Date();
		}
		else
		{
			return latest;
		}
	}


	@Override
	public List<TimeSeriesDataPoint> getDataPointsForTimeSpan(String datatype, String metric,
														String source,
														List<Attribute> attributes,
														Date minTime, Date maxTime, 
														int intervalSecs)
	{
		String debugAttributeString = "";
		for (Attribute attr : attributes)
		{
			debugAttributeString = debugAttributeString + attr.toString() + ", ";
		}

		s_Logger.debug("getDataPointsForTimeSpan() datatype=" + datatype +
				", metric=" + metric +
				", source=" + source +
				", attributes= " + debugAttributeString +
				", start=" + minTime + 
				", end=" + maxTime);
		
		// Introscope does not support aggregated views so if the combination of 
		// source/metric/attributes does not constitute a valid key then don't 
		// make the query just return an empty list.
		AgentMetricPair pair;
		try
		{
			pair = new AgentMetricPair(metric, source, attributes);
		}
		catch (IllegalArgumentException e)
		{
			return new ArrayList<TimeSeriesDataPoint>();
		}
		
		// First look in the cache for the points
		String externalKey = AgentMetricPair.createExternalKey(metric, source, attributes);
		List<TimeSeriesDataPoint> points = m_DataPointCache.getPoints(externalKey, minTime, maxTime);
		if (points != null)
		{
			s_Logger.debug("getDataPointsForTimeSpan() returning points from the cache");
			return points;
		}

		points = getDataPoints(pair.getAgent(), pair.getMetric(), minTime, maxTime, intervalSecs);
				
		return points;
	}
	
	
	@Override
	public List<TimeSeriesDataPoint> getDataPointsForTimeSpan(String externalKey,
											Date minTime, Date maxTime, 
											int intervalSecs) 
	{
		s_Logger.debug("getDataPointsForTimeSpan() externalKey = " + externalKey);
		
		
		// First look in the cache for the points
		List<TimeSeriesDataPoint> points = m_DataPointCache.getPoints(externalKey, minTime, maxTime);
		if (points != null)
		{
			s_Logger.debug("getDataPointsForTimeSpan() returning points from the cache");
			return points;
		}
		
		AgentMetricPair keys;
		try
		{
			keys = new AgentMetricPair(externalKey);
		}
		catch (ParseException pe)
		{
			s_Logger.error("Invalid argument externalKey = '" + externalKey + "'. Exception = " + pe);
			return new ArrayList<TimeSeriesDataPoint>();
		}

		if (keys.getAgent().contains(".*"))
		{
			s_Logger.error("Wild cards cannot be used to specify agents in this function");
			return new ArrayList<TimeSeriesDataPoint>();
		}
		
		if (keys.getMetric().contains(".*"))
		{
			s_Logger.error("Wild cards cannot be used to specify metrics in this function");
			return new ArrayList<TimeSeriesDataPoint>();
		}
		
		return getDataPoints(keys.getAgent(), keys.getMetric(), minTime, maxTime, intervalSecs);
	}


	/**
	 * Runs all the metric queries for this plugin and returns all the 
	 * data points between <code>minTime</code> and <code>maxTime</code>.
	 * 
	 * This function will tests for the CLW Daylight Savings bug every
	 * time it is called. If it is detected in the first query an offset
	 * will be applied and the query run again. 
	 * 
	 * For real time queries the QueryMonitorPolicy will truncate any 
	 * queries that fall behind time so might not be in the fast memory
	 * cache. 
	 * 
	 * The latestTime member is updated by the results of this function.
	 * 
	 * This function also updates the Metric Cache.
	 */
	@Override 
	public Collection<TimeSeriesData> getAllDataPointsForTimeSpan(Date minTime, Date maxTime, 
												int intervalSecs) 
	{
		Collection<TimeSeriesData> results = getAllDataPointsForTimeSpanInner(minTime, maxTime, intervalSecs,
												false, m_MetricGroups);
		
		// Get the time of the last point in the first time
		// series and compare that to the current latest time.
		if (results.size() > 0)
		{
			TimeSeriesData data = results.iterator().next();
			if (data.getDataPoints().size() > 0)
			{
				// points are sorted in time order.
				TimeSeriesDataPoint pt = data.getDataPoints().get(data.getDataPoints().size() -1);
				Date latest = new Date(pt.getTime());
				
				synchronized (s_LatestTimeByDataType)
				{
					Date currentLatest = s_LatestTimeByDataType.get(getDataSourceType());
					try
					{
						if (latest.after(currentLatest))
						{
							s_LatestTimeByDataType.put(getDataSourceType(), latest);
						}
					}
					catch (NullPointerException e)
					{
						s_LatestTimeByDataType.put(getDataSourceType(), latest);
					}
				}
			}
		}
		
		return results;
	}


	/**
	 * Contains most of the implementation of getAllDataPointsForTimeSpan(),
	 * but allows the option of pulling different metrics to the standard ones.
	 */
	private Collection<TimeSeriesData> getAllDataPointsForTimeSpanInner(
							Date minTime, Date maxTime, 
							int intervalSecs, boolean isForGui,
							List<MetricGroup> listOfGroups)
	{
		s_Logger.info("getAllDataPointsForTimeSpanInner() start = " + minTime +
							", end = " + maxTime + ", interval = " + intervalSecs);

		Date functionEntryTime = new Date();

		intervalSecs = dataPointResolutionForInterval(intervalSecs);

		Date start = (Date)minTime.clone();
		Date end = (Date)maxTime.clone();

		QueryMonitorPolicy monitorPolicy = getQueryMonitorPolicy();	
		if (!monitorPolicy.validateQueryArgsTimeSpan(start, end))
		{
			s_Logger.error("This query exceeds the maximum duration. " +
					"Changing start time to " + start);
		}

		// We may need to compensate for the CLW daylight savings bug, but do
		// this in separate variables
		Date adjStart = (Date)start.clone();
		Date adjEnd = (Date)end.clone();

		if (s_QueryHoursOffset != 0)
		{
			Calendar calendar = Calendar.getInstance();
			
			calendar.setTime(start);
			calendar.add(Calendar.HOUR, s_QueryHoursOffset);
			adjStart = calendar.getTime();
			
			calendar.setTime(end);
			calendar.add(Calendar.HOUR, s_QueryHoursOffset);
			adjEnd = calendar.getTime();
			
			s_Logger.debug("Adjusting query time by " + s_QueryHoursOffset + " hour(s). " +
					"Adjusted start = " + adjStart + ", adjusted end = " + adjEnd);
		}

		List<TimeSeriesData> allDataPoints = new ArrayList<TimeSeriesData>();

		
		// Get the connection.
		IntroscopeConnection connection;
		try
		{
			connection = m_ClwConnectionPool.acquireConnection();
		}
		catch (Exception e)
		{
			s_Logger.error("Could not acquire connection from pool: " + 
					m_ClwConnectionPool.getConnectionConfig());

			return Collections.emptyList();
		}
		
		Date queriesStartTime = new Date();

		try
		{
			connection.setMetricDataType(DEFAULT_METRIC_DATATYPE);
			
			boolean correctedForDSTBug = false;

			OUTER_LOOP:
			{	

				Iterator<MetricGroup> groupItr = listOfGroups.iterator();
				while (groupItr.hasNext())
				{
					MetricGroup metricGroup = groupItr.next();

					Date prevStart = (Date)start.clone();
					if (!monitorPolicy.validateQueryDateParamsAge(start))
					{
						// The start time has been moved forward, so move
						// the adjusted start time forward by the same
						// amount
						adjStart.setTime(adjStart.getTime() + (start.getTime() - prevStart.getTime()));

						s_Logger.info("Query start time has been moved" +
								" forward to stay within the in-memory" +
								" cache. Moved start = " + start +
								", moved adjusted start = " + adjStart);

						if (adjStart.after(adjEnd))
						{
							// start time is now after the end time so don't make anymore queries.
							s_Logger.warn(String.format("Query start time (%s) comes after the end time (%s) " +
									"Returning from query loop.", adjStart, adjEnd));

							break OUTER_LOOP;
						}
					}

					Collection<TimeSeriesData> timeSeriesData = connection.getMetricData(
							metricGroup.getAgent(), metricGroup.getMetric(), 
							adjStart, adjEnd, intervalSecs);

					boolean repeatQuery = false;


					if (!correctedForDSTBug && timeSeriesData.size() > 0)
					{
						List<TimeSeriesDataPoint> points = timeSeriesData.iterator().next().getDataPoints();
						if (points.size() > 0)
						{
							Collections.sort(points);
							int queryHoursOffset = IntroscopeConnection.calcDstAdjustment(adjStart.getTime(), 
									points.get(0).getTime());

							// If a difference between requested time and returned time
							// then the daylight savings bug is occurring. Apply the
							// required offset if it is non-zero and run the queries again.
							boolean offsetHasChanged = s_QueryHoursOffset != queryHoursOffset;
							if (offsetHasChanged)
							{
								s_QueryHoursOffset = queryHoursOffset;
								s_Logger.info("Making correction for daylight savings bug. New offset = " 
										+ s_QueryHoursOffset + " hours.");
							}

							// There's no point repeating the query if this
							// is just for the GUI
							repeatQuery = offsetHasChanged && (queryHoursOffset != 0) && !isForGui;

							if (repeatQuery)
							{
								Calendar calendar = Calendar.getInstance();

								calendar.setTime(start);
								calendar.add(Calendar.HOUR, s_QueryHoursOffset);
								adjStart = calendar.getTime();

								calendar.setTime(end);
								calendar.add(Calendar.HOUR, s_QueryHoursOffset);
								adjEnd = calendar.getTime();
							}

							correctedForDSTBug = true;
						}
					}

					// If the DST bug happened then reset the iterator to 
					// the start of the list and run again 
					// else add the points to the results and move on.
					if (repeatQuery)
					{
						groupItr = listOfGroups.iterator();
					}
					else
					{
						allDataPoints.addAll(timeSeriesData);
					}
				}


				if (Thread.interrupted())
				{			
					s_Logger.info("Introscope getAllDataPointsForTimeSpanInner() Thread has been interrupted.");
					break OUTER_LOOP;
				}
			}
			
		}
		catch (ConnectionException e) 
		{
			s_Logger.error("Connection error in getAllDataPointsForTimeSpanInner()");
		}
		finally
		{
			m_ClwConnectionPool.releaseConnection(connection);
		}

		Date queriesEndTime = new Date();

		sortDataPointsAndRemoveDuplicates(allDataPoints);

		if (!isForGui)
		{
			countPointsProcessed(allDataPoints);
		}

		Date sortPointsEndTime = new Date();

		// Update the metric cache if necessary.  For a canned demo, only do
		// this when called from the initialiseForGui() method, as it may be
		// getting more metrics.
		if ((m_MetricCache.cacheNeedsUpdating() && !RunPolicy.isCannedDemo()) ||
			(isForGui && RunPolicy.isCannedDemo() && !allDataPoints.isEmpty()))
		{
			m_MetricCache.updateMetricTree(allDataPoints);
		}
		
		long functionTime = sortPointsEndTime.getTime() - functionEntryTime.getTime();
		long initTime = queriesStartTime.getTime() - functionEntryTime.getTime();
		long queryTime = queriesEndTime.getTime() - queriesStartTime.getTime();
		long sortTime = sortPointsEndTime.getTime() - queriesEndTime.getTime();

		String info = "getAllDataPointsForTimeSpanInner = %s time series collected in %s ms. Initialisation time = %s ms. Query time = %s ms. Sort and filter points time = %s ms.";
		s_Logger.info(String.format(info, allDataPoints.size(), functionTime, initTime, queryTime, sortTime));

		return allDataPoints;
	}
	
	
	/**
	 * Sometimes queries may return duplicate points if two the the regex 
	 * argument used overlap. The Prelert feature detector doesn't like 
	 * duplicated points so we filter them out here.
	 * 
	 * This is quite an expensive operation and may want to be reviewed 
	 * going forward but it is required now.
	 *  
	 * @param allData parameter may be modified.
	 */
	private void sortDataPointsAndRemoveDuplicates(Collection<TimeSeriesData> allData)
	{		
		for (TimeSeriesData data : allData)
		{
			// add points to a sorted tree set
			// This sorts and de-duplicate points
			TreeSet<TimeSeriesDataPoint> sortedSet = new TreeSet<TimeSeriesDataPoint>(data.getDataPoints());
			
			data.setDataPoints(Arrays.asList(sortedSet.toArray(new TimeSeriesDataPoint[0])));
		}
	}
	
	
	/**
	 * Updates <code>s_DataSourceCountMap</code> with the running 
	 * count of all the points processed by time series source.
	 * 
	 * @param allData
	 */
	private void countPointsProcessed(Collection<TimeSeriesData> allData)
	{
		synchronized(m_DataSourceCountMap)
		{
			for (TimeSeriesData data : allData)
			{
				String source = data.getConfig().getSource();
				Integer count = m_DataSourceCountMap.get(source);
				if (count == null)
				{
					count = new Integer(0);
				}
				
				count += data.getDataPoints().size();
				
				m_DataSourceCountMap.put(source, count);
			}
		}
	}
	
	/**
	 * Get time series data for the given agent and metric in the time frame
	 * <code>minTime, maxTime</code>. 
	 * 
	 * This function will block on the GuiAccessSemaphore if the maximum number
	 * of GUI connections is exceeded. See DEFAULT_GUI_ACCESS_THREADS_COUNT.
	 * 
	 * @param agent regex characters in this string should be escaped properly.
	 * @param metric regex characters in this string should be escaped properly.
	 * @param minTime
	 * @param maxTime
	 * @param intervalSecs
	 * @return
	 */
	private List<TimeSeriesDataPoint> getDataPoints(String escapedAgent, 
												String escapedMetric, 
												Date minTime, Date maxTime,
												int intervalSecs) 
	{
		s_Logger.debug("getDataPoints()");
		
		List<TimeSeriesDataPoint> points = new ArrayList<TimeSeriesDataPoint>();

		Collection<TimeSeriesData> data = Collections.emptyList();
		try 
		{
			s_GuiAccessSemaphore.acquire();

			data = getIScopeDataPoints(escapedAgent, escapedMetric,
										minTime, maxTime, intervalSecs);
		}
		catch (InterruptedException e) 
		{
			s_Logger.error("getDataPoints() was interrupted");
			s_Logger.error(e);
			
			return points;
		}
		finally
		{
			s_GuiAccessSemaphore.release();
		}


		if (data.size() > 0)
		{
			points.addAll(data.iterator().next().getDataPoints());
			Collections.sort(points);
		}
		

		s_Logger.debug("getDataPoints() returning " + points.size() + " points");
		
		return points;
	}
	
	/**
	 * Get time series data for the given agent and metric in the time frame
	 * <code>minTime, maxTime</code> and return the raw Introscope points.
	 * 
	 * Times are possibly adjusted by 1 hour due to the CLW daylight savings
	 * bug.
	 * 
	 * @param agent regex characters in this string should be escaped properly.
	 * @param metric regex characters in this string should be escaped properly.
	 * @param minTime
	 * @param maxTime
	 * @param intervalSecs
	 * @return
	 */
	private Collection<TimeSeriesData> getIScopeDataPoints(String escapedAgent, 
															String escapedMetric, 
															Date minTime, Date maxTime,
															int intervalSecs)
	{		
		intervalSecs = dataPointResolutionForInterval(intervalSecs);
		
		s_Logger.debug("getIScopeDataPoints() escaped agent=" + escapedAgent
				+ ", escaped metric=" + escapedMetric +
				", start=" + minTime + 
				", end=" + maxTime +
				", interval=" + intervalSecs);

		
		Date queryStart = minTime;
		Date queryEnd = maxTime;
		if (s_QueryHoursOffset != 0)
		{
			Calendar calendar = Calendar.getInstance();
			
			calendar.setTime(minTime);
			calendar.add(Calendar.HOUR, s_QueryHoursOffset);
			queryStart = calendar.getTime();
			
			calendar.setTime(maxTime);
			calendar.add(Calendar.HOUR, s_QueryHoursOffset);
			queryEnd = calendar.getTime();
			
			s_Logger.debug("Adjusting query time by " + s_QueryHoursOffset + " hour(s). " +
					"Adjusted start = " + queryStart + ", adjusted end = " + queryEnd);
		}
		
		
		// Get the connection.
		IntroscopeConnection connection;
		try
		{
			connection = m_ClwConnectionPool.acquireConnection();
		}
		catch (Exception e)
		{
			s_Logger.error("Could not acquire connection from pool: " + 
					m_ClwConnectionPool.getConnectionConfig());

			return Collections.emptyList();
		}

		try
		{
			connection.setMetricDataType(DEFAULT_METRIC_DATATYPE);

			Collection<TimeSeriesData> result = connection.getMetricData(escapedAgent, 
															escapedMetric, 
															queryStart, queryEnd, intervalSecs);
			
			if (result.size() > 0)
			{
				List<TimeSeriesDataPoint> points = result.iterator().next().getDataPoints();
				if (points.size() > 0)
				{
					s_QueryHoursOffset = IntroscopeConnection.calcDstAdjustment(queryStart.getTime(), 
																	points.get(0).getTime());
				}
			}
			
			return result;
		}
		catch (ConnectionException e) 
		{
			s_Logger.error("Connection error in getIScopeDataPoints()");
			return Collections.emptyList();
		}
		finally
		{
			m_ClwConnectionPool.releaseConnection(connection);
		}
	}
	

	/**
	 * This function returns the peak value for the the time series 
	 * identified by <code>externalKey</code> for the given time period.
	 * As we expect the GUI to make another call to getDataPoints() with
	 * the same parameters shortly after this call returns we cache the 
	 * resulting <code>TimeSeriesDataPoint</code>.
	 */
	@Override
	public List<ExternalKeyPeakValuePair> getPeakValueForTimeSpan(List<String> externalKeys,
											Date minTime,Date maxTime, int intervalSecs) 
	{
		s_Logger.debug("getPeakValueForTimeSpan() " + externalKeys.toString() + 
						", start=" + minTime + ", end=" + maxTime +
						", interval=" + intervalSecs);
		
		intervalSecs = dataPointResolutionForInterval(intervalSecs);
		
		Map<String, Double> peakValueByExternalKey = new HashMap<String, Double>();
		for (String externalKey : externalKeys)
		{
			peakValueByExternalKey.put(externalKey, new Double(0.0));
		}
		
		// Map of time series points by key which will be added to the cache
		Map<String, List<TimeSeriesDataPoint>> timeSeriesPointsByKey = 
					new HashMap<String, List<TimeSeriesDataPoint>>();
		
		List<AgentMetricPair> metricPairs = IntroscopeRegExUtilities.mergeRelatedExternalKeysIntoRegex(externalKeys);
		
		s_Logger.debug("MergeRelatedKeys returned " + metricPairs.size() +
						" merged keys from " + externalKeys.size());
		
		for (AgentMetricPair pair : metricPairs) 
		{
			Collection<TimeSeriesData> data = getIScopeDataPoints(pair.getAgent(), 
																pair.getMetric(),
																minTime, maxTime,
																intervalSecs);
			
			for (TimeSeriesData timeSeries : data)
			{
				double prevPeak = 0.0;


				for (TimeSeriesDataPoint point : timeSeries.getDataPoints())
				{

					String key = timeSeries.getConfig().getExternalKey();

					Double value = point.getValue();

					if (point.getValue() > prevPeak)
					{
						prevPeak = point.getValue();
						peakValueByExternalKey.put(key, value);
					}
				}

				Collections.sort(timeSeries.getDataPoints());
				// Store points for the cache
				timeSeriesPointsByKey.put(timeSeries.getConfig().getExternalKey(), 
										timeSeries.getDataPoints());
			}
		}
		
		// insert into cache
		Set<String> keys = timeSeriesPointsByKey.keySet();
		for (String key : keys)
		{
			m_DataPointCache.insertPoints(key, minTime, maxTime, 
					timeSeriesPointsByKey.get(key));
		}
					
		List<ExternalKeyPeakValuePair> result = new ArrayList<ExternalKeyPeakValuePair>();
		
		Set<String> keySet = peakValueByExternalKey.keySet();
		for (String setKey : keySet)
		{
			result.add(new ExternalKeyPeakValuePair(setKey, 
									peakValueByExternalKey.get(setKey)));
		}
		
		if (result.size() != externalKeys.size())
		{
			s_Logger.error("getPeakValues() did not match every key");
		}
		
		return result;		
	}
	

	/**
	 * Introscope data is collected in multiples of 15 seconds
	 * @return 
	 */
	@Override
	public int getUsualPointIntervalSecs()
	{
		return m_PointInterval;
	}
	
	/**
	 * Set the interval that will be used in the queries for data points.
	 * @param value
	 */
	public void setUsualPointIntervalSecs(int value)
	{
		if ((value < 0) || (value % IntroscopeConnection.DEFAULT_INTERVAL) != 0)
		{
			throw new IllegalArgumentException("Invalid argument: Interval = " + value +
												" must be a multiple of 15 seconds.");
		}
		
		m_PointInterval = value;
	}
	
	

	/**
	 * Does this plugin support time series aggregation for a given data type,
	 * i.e. querying for time series points without specifying a value for every
	 * possible attribute?  For Introscope the answer is always no.
	 * @param datatype The name of the data type.  This is not needed as
	 *                 Introscope does not support arbitrary aggregation.
	 * @return false always.
	 */
	public boolean isAggregationSupported(String datatype)
	{
		return false;
	}


	/**
	 * Introscope data is collected in multiples of 15 seconds so round 
	 * <code>interval</code> to a multiple of 15.
	 * @param interval
	 * @return the interval rounded up to a multiple of 15.
	 */
	private int dataPointResolutionForInterval(int interval)
	{
		int rem = (interval -1) / getUsualPointIntervalSecs();
		
		return (rem + 1) * getUsualPointIntervalSecs();	
	}
	
	
	/**
	 * Returns metrics from the metric cache. Will block if the 
	 * metric cache has not finished loading.
	 * 
	 * @param datatype - unused.
	 */
	@Override
	public List<String> getMetrics(String datatype)
	{
		s_Logger.debug("getMetrics() datatype = " + datatype);
		
		return m_MetricCache.getMetrics();
	}
	
	/**
	 * Returns the number of metrics in the metric cache. Will block if the 
	 * metric cache has not finished loading.
	 * 
	 * @return
	 */
	public int getNumberOfMetrics()
	{
		return m_MetricCache.getNumberOfMetrics();
	}
	
	/**
	 * Get a list of all the Processes on the <code>source</code> parameter
	 * for the given datatype. If <code>source</code> is <code>null</code>
	 * then return all processes.
	 * 
	 * Uses the metric cache to get a list of HostProcessAgent.
	 *   
	 * @param datatype
	 * @param source may be <code>null</code>
	 * @return
	 */
	private List<String> listProcesses(String datatype, String source)
	{
		s_Logger.debug("listProcesses() datatype = " + datatype +
							"souce = " + source);
		
		return m_MetricCache.getMetricPathValuesAtLevel(IntroscopeMetricCache.PROCESS_LEVEL);
	}
	
	
	/**
	 * Get a list of all the agents on the <code>source</code> parameter
	 * for the given datatype. If source is <code>null</code> then don't
	 * filter.
	 * 
	 * Uses the metric cache to get a list of HostProcessAgent.  
	 * @param datatype
	 * @param source may be <code>null</code>
	 * @return
	 */
	private List<String> listAgents(String datatype, String source) 
	{
		s_Logger.debug("listAgents() datatype = " + datatype +
						"souce = " + source);
		
		return m_MetricCache.getMetricPathValuesAtLevel(IntroscopeMetricCache.AGENT_LEVEL);
	}
		

	/**
	 * Returns a list of the Introscope Host machines.
	 * @param datatype
	 * @return
	 */
	public Collection<String> listTimeSeriesSources()
	{
		s_Logger.debug("listTimeSeriesSources()");

		return m_MetricCache.getMetricPathValuesAtLevel(IntroscopeMetricCache.HOST_LEVEL);
	}
	

	/**
	 * Returns a new instance of a <code>IntroscopePlugin</code> which has all 
	 * its members set to copies of this object's members. 
	 * It does not need to create a new client as all plugins share a single 
	 * instance and it does not call <code>loadProperties()</code> instead it
	 * sets the properties on the new object and uses <code>Collections.unmodifiableList</code>.
	 */
	@Override
	public Plugin duplicate()
	{
		s_Logger.debug("Duplicating Introscope Plugin");
		
		
		// TODO should we keep this code???
		// If the plugin is not configured then don't duplicate it.
//		if (!isConfigured())
//		{
//			throw new UnsupportedOperationException("Cannot clone an unconfigured plugin");
//		}
		
		IntroscopePlugin clone = new IntroscopePlugin();
		clone.setConfigured(isConfigured());
		clone.setProperties(new Properties(getProperties()));
		clone.setName(getName());
			
		clone.m_PointInterval = this.m_PointInterval;
		clone.m_DataSourceCategory = this.m_DataSourceCategory;

		clone.setDataSourceType(getDataSourceType());
		
		clone.setQueryMonitorPolicy(getQueryMonitorPolicy());
		
		clone.m_AlertGroups = Collections.unmodifiableList(m_AlertGroups);
		clone.m_MetricGroups = Collections.unmodifiableList(m_MetricGroups);
		
		// Set shared objects.
		clone.m_MetricCache = this.m_MetricCache;
		clone.m_DataPointCache = this.m_DataPointCache;
		clone.m_LoadMonitor = this.m_LoadMonitor;
		clone.m_DataSourceCountMap = this.m_DataSourceCountMap;
		
		clone.m_ClwConnectionPool = this.m_ClwConnectionPool;
		
		return clone;
	}


	/**
	 * Calling this method will populate the metric cache
	 * with the metrics at time <code>notableDate</code>
	 * or the current time if <code>notableDate == null</code>.
	 *
	 * If no data is returned for time <code>notableDate</code>
	 * then the function tries to update with the current time.
	 */
	@Override
	public void initialiseForGui(Date notableDate)
	{
		s_Logger.info("initialiseForGui starting with date " + notableDate);

		Date end;
		Date start = null;

		// Usually, the metrics visible in the GUI will match those we're
		// pulling data for, but for a canned demo the GUI shows more than we
		// pull data for
		List<MetricGroup> listOfGroups;

		if (RunPolicy.isCannedDemo())
		{
			try
			{
				// Use a time in the middle of the available range in case the
				// CLW daylight savings bug affects us
				DateFormat df = new SimpleDateFormat("dd MMM yyyy kk:mm:ss z");
				start = df.parse("29 Feb 2012 10:00:00 GMT");
			}
			catch (ParseException e)
			{
				s_Logger.error("Cannot parse date " + e);
			}

			// Change to "^(?!Custom Metric Host \\(Virtual\\)).+" if you don't want the custom metric host.
			MetricGroup allMetrics = new MetricGroup(".*", ".*", true);

			listOfGroups = Arrays.asList(new MetricGroup[]{allMetrics});
		}
		else
		{
			start = notableDate;

			listOfGroups = m_MetricGroups;
		}

		if (start == null)
		{
			end = new Date();
			start = new Date(end.getTime() - (120 * 1000));

			getAllDataPointsForTimeSpanInner(start, end, IntroscopeConnection.DEFAULT_INTERVAL, true, listOfGroups);
		}
		else
		{
			end = new Date(start.getTime() + (120 * 1000));

			Collection<TimeSeriesData> data = getAllDataPointsForTimeSpanInner(start, end, IntroscopeConnection.DEFAULT_INTERVAL, true, listOfGroups);
			if (data.size() == 0)
			{
				end = new Date();
				start = new Date(end.getTime() - (120 * 1000));

				getAllDataPointsForTimeSpanInner(start, end, IntroscopeConnection.DEFAULT_INTERVAL, true, listOfGroups);
			}
		}

		s_Logger.info("initialiseForGui completed using date " + start);
	}


	/**
	 * Start the load monitor thread.
	 */
	@Override
	public void start()
	{
		if (isConfigured())
		{
			m_LoadMonitor.start();
		}
	}


	/**
	 * Blocking call stops the MetricCache and LoadMonitor threads then
	 * wait for the join.
	 * 
	 * The cache and load monitor threads are reset so they can be 
	 * started again.
	 */
	@Override
	public void stop()
	{
		m_LoadMonitor.stop();  
	}


	/**
	 * Resets the metric cache and load monitor so they can be 
	 * started again from a blank state.
	 * Also sets the latestTime of all the seen data points 
	 * for this datatype to null.
	 */
	@Override
	public void reset()
	{
		m_MetricCache.reset();
		m_LoadMonitor.reset();
		
		synchronized(m_DataSourceCountMap)
		{
			m_DataSourceCountMap.clear();
		}
		
		if (getDataSourceType() != null)
		{
			synchronized (s_LatestTimeByDataType)
			{
				s_LatestTimeByDataType.put(getDataSourceType(), null);
			}
		}
	}


	/**
	 * Get a map of arguments specific to this plugin.  For Introscope this
	 * consists of:
	 * 1) plg = The name of the plugin
	 * 2) agnt = The number of agents
	 * 3) met = The number of metrics
	 * @return A map of plugin-specific usage data.
	 */
	public Map<String, String> getPluginSpecificUsageData()
	{
		HashMap<String, String> pluginArgs = new HashMap<String, String>();

		pluginArgs.put("plg", getName());
		pluginArgs.put("agnt", Integer.toString(m_Agents.size()));
		pluginArgs.put("met", Integer.toString(m_MetricCache.getNumberOfMetrics()));

		return pluginArgs;
	}
	
	
	@Override
	public List<PluginProperty> getRequriedProperties()
	{
		return s_Properties;
	}

	
	@Override
	public String getMetricPathDelimiter()
	{
		return PATH_SEPARATOR;
	}
	
	@Override
	public String getMetricPathMetricPrefix()
	{
		return METRIC_SEPARATOR;
	}
	
	@Override
	public String getMetricPathSourcePrefix()
	{
		return "";
	}


	/**
	 * For the given list of partially populated <code>MetricTreeNode</code>s which have
	 * no members set apart from the <code>externalKey</code> this function
	 * returns a new list of <code>MetricTreeNode</code>s containing 
	 * the name and prefix for each level of that longest metric path, in order 
	 * of their position in the metric path.
	 * 
	 * @param externalKeyNodes List of partially populated
	 *                         <code>MetricTreeNode</code> objects containing
	 *                         external keys.
	 * @return A list of partially populated <code>MetricTreeNode</code> objects
	 *         containing the name and prefix of each constituent of the metric
	 *         path.
	 */
	@Override
	public List<MetricTreeNode> metricPathNodesFromExternalKeys(List<MetricTreeNode> externalKeyNodes)
	{
		// For Introscope, the metric path names will be as follows:
		//
		// type
		//  : source
		// |Process
		// |Agent
		// |ResourcePath0
		// |ResourcePath1
		// ...
		// :metric
		//
		// So, all we need to do is determine the correct number of ResourcePath# nodes
		int numResourcePathAttributes = 0;

		for (MetricTreeNode externalKeyNode : externalKeyNodes)
		{
			String externalKey = externalKeyNode.getExternalKey();

			try
			{
				AgentMetricPair agentMetricPair = new AgentMetricPair(externalKey);
				String metricPathSection = agentMetricPair.getMetric();
				String [] pathSplit = metricPathSection.split("\\" + PATH_SEPARATOR);
				if (pathSplit.length > numResourcePathAttributes)
				{
					numResourcePathAttributes = pathSplit.length;
				}
			}
			catch (ParseException pe)
			{
				s_Logger.error("Invalid argument externalKey = '"
						+ externalKey + "'. Cannot parse externalKey");
				s_Logger.error(pe);
			}
		}

		ArrayList<MetricTreeNode> result = new ArrayList<MetricTreeNode>();

		MetricTreeNode node = new MetricTreeNode();
		node.setType(DEFAULT_METRIC_DATATYPE);
		node.setPrefix("");
		node.setName(TYPE_FIELD_NAME);
		result.add(node);

		node = new MetricTreeNode();
		node.setType(DEFAULT_METRIC_DATATYPE);
		node.setPrefix(DATATYPE_SUFFIX);
		node.setName(SOURCE_FIELD_NAME);
		result.add(node);

		node = new MetricTreeNode();
		node.setType(DEFAULT_METRIC_DATATYPE);
		node.setPrefix(PATH_SEPARATOR);
		node.setName(PROCESS_ATTRIBUTE);
		result.add(node);

		node = new MetricTreeNode();
		node.setType(DEFAULT_METRIC_DATATYPE);
		node.setPrefix(PATH_SEPARATOR);
		node.setName(AGENT_ATTRIBUTE);
		result.add(node);

		for (int count = 0; count < numResourcePathAttributes; ++count)
		{
			node = new MetricTreeNode();
			node.setType(DEFAULT_METRIC_DATATYPE);
			node.setPrefix(PATH_SEPARATOR);
			node.setName(RESOURCE_PATH_ATTRIBUTE + count);
			result.add(node);
		}

		node = new MetricTreeNode();
		node.setType(DEFAULT_METRIC_DATATYPE);
		node.setPrefix(METRIC_SEPARATOR);
		node.setName(METRIC_FIELD_NAME);
		result.add(node);

		return result;
	}

}

