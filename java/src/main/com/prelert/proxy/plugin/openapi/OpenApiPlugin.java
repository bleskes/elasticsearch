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

package com.prelert.proxy.plugin.openapi;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.PoolingDataSource;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.DriverManagerConnectionFactory;

import org.apache.log4j.Logger;

import com.prelert.data.Attribute;
import com.prelert.data.ConnectionStatus;
import com.prelert.data.DataSource;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.TimeSeriesData;
import com.prelert.data.TimeSeriesDataPoint;
import com.prelert.proxy.data.DataBaseType;
import com.prelert.proxy.data.SourceConnectionConfig;
import com.prelert.proxy.plugin.ExternalPointsPlugin;
import com.prelert.proxy.plugin.ErrorGettingDataPointsException;
import com.prelert.proxy.plugin.Plugin;
import com.prelert.proxy.plugin.PluginProperty;
import com.prelert.proxy.plugin.TimeSeriesPlugin.ExternalKeyPeakValuePair;
import com.prelert.server.ServerUtil;


/**
 * A generic SQL plugin which connects to a database using a JDBC driver
 * and runs the queries it has been configured with. This plugin can be used 
 * with different types and databases but each must be configured as a individual
 * data type using setDataSourceType() or setDataType().
 * 
 * The plugin requires a number of properties to be set in {@link #configure} 
 * including the database queries.
 */
public class OpenApiPlugin extends Plugin implements ExternalPointsPlugin
{
	private static Logger s_Logger = Logger.getLogger(OpenApiPlugin.class);
	
	// Expected query replacement tokens.
	public final static String QUERY_ARG_START_MARKER = ":";
	
	public final static String START_TIME_TOKEN = "StartTime";
	public final static String END_TIME_TOKEN = "EndTime";
	
	public final static String METRIC_COLUMN_ID = "Metric_";
	public final static String KEY_COLUMN_ID = "Key_";
	public final static String ATTRIBUTE_COLUMN_ID = "Attribute_";
	
	public final static String SOURCE_COLUMN = "Source";
	public final static String DATETIME_COLUMN = "DateTime";
	public final static String DESCRIPTION_COLUMN = "Description";
	public final static String SUBTYPE_COLUMN = "SubType";
	
	public final static String METRIC = "metric";
	public final static String QUERY_ID = "Query_Id";

	public final static String EXTERNAL_KEY_DELIMITER = "&&";
	public final static String EXTERNAL_KEY_VALUE_DELIMITER = "=";
	
	// Map of data point counts by source by data type.
	private static Map<DataSourceType, Map<String, Integer>> s_PointCountByDataType 
					= new HashMap<DataSourceType, Map<String, Integer>>();
	
	// Track the time of the latest piece of data seen by data type.
	private static Map<DataSourceType, Date> s_LatestTimeByDataType 
					= new HashMap<DataSourceType, Date>();
					
	
	
	private String m_ExternalKeyDelimiter = EXTERNAL_KEY_DELIMITER;
	private String m_ExternalKeyValueDelimiter = EXTERNAL_KEY_VALUE_DELIMITER;
	
	private String m_QueryArgStartMarker = QUERY_ARG_START_MARKER;
	
	// Default metric path separators
	private String m_MetricPathDelimiter = ServerUtil.METRIC_PATH_DELIMITER;
	private String m_MetricPathMetricPrefix = ServerUtil.METRIC_PATH_METRIC_PREFIX;
	private String m_MetricPathSourcePrefix = ServerUtil.METRIC_PATH_SOURCE_PREFIX;
	
	// Source Query Strings
	private List<String> m_AllTimeSeriesQueries;
	private List<String> m_TimeSeriesQueries;
	
	private Set<String> m_EssentialColumns;
	private Set<String> m_ReservedColumns;
		
	// Earliest date before now in days.
	private int m_EarliestTimeOffset;
		
	// The connection pool
	private javax.sql.DataSource m_DataSource;
	
	private DataBaseType m_DatabaseType;
	
	private SourceConnectionConfig m_SourceConnectionConfig;
	
	/**
	 * The static set of properties for this plugin.
	 */
	static private final List<PluginProperty> s_Properties = new ArrayList<PluginProperty>();
	static 
	{
		List<String> values = new ArrayList<String>();
		DataBaseType types[] = DataBaseType.values();
		for (DataBaseType type : types)
		{
			values.add(type.toString());
		}
		PluginProperty databaseType = new PluginProperty("DataBaseType", true, values);
		
		s_Properties.add(databaseType);
		s_Properties.add(new PluginProperty("DataType", true));
		s_Properties.add(new PluginProperty("DataBaseName", true));
		s_Properties.add(new PluginProperty("AllTimeSeriesQuery[0-9]*", true, true));
		s_Properties.add(new PluginProperty("TimeSeriesQuery[0-9]*", true, true));
		
		s_Properties.add(new PluginProperty("MetricPathDelimiter", false));
		s_Properties.add(new PluginProperty("MetricPathMetricPrefix", false));
		s_Properties.add(new PluginProperty("MetricPathSourcePrefix", false));
	}
	
	
	/**
	 * Default constructor.
	 */
	public OpenApiPlugin()
	{
		setName("OpenApiPlugin");
		
		// The queries should contain these columns.
		m_EssentialColumns = new HashSet<String>();
		m_EssentialColumns.add(SOURCE_COLUMN);
		m_EssentialColumns.add(DATETIME_COLUMN);
		
		// The queries should not return any columns 
		// with these names.
		m_ReservedColumns = new HashSet<String>();
		m_ReservedColumns.add("type");
		m_ReservedColumns.add("source");
		m_ReservedColumns.add("time");
		m_ReservedColumns.add("metric");
		m_ReservedColumns.add("id");
		m_ReservedColumns.add("time_series_id");
		m_ReservedColumns.add("time_series_type_id");
		m_ReservedColumns.add("count");
		m_ReservedColumns.add("probable_cause");
		m_ReservedColumns.add("severity");
		m_ReservedColumns.add("description");
		
		m_AllTimeSeriesQueries = new ArrayList<String>();
		m_TimeSeriesQueries = new ArrayList<String>();
		
		// Defaults to 14 days of data.
		m_EarliestTimeOffset = 14;
	}
	
		
	@Override
	public Plugin duplicate() 
	{
		OpenApiPlugin clone = new OpenApiPlugin();
		
		// parent class members
		clone.setName(getName());
		clone.setDataSourceType(getDataSourceType());
		clone.setConfigured(isConfigured());
		clone.setProperties(new Properties(getProperties()));
		clone.setQueryMonitorPolicy(getQueryMonitorPolicy());
		
		// members of this class
		clone.m_AllTimeSeriesQueries = this.m_AllTimeSeriesQueries;
		clone.m_TimeSeriesQueries = this.m_TimeSeriesQueries;
		
		clone.m_DataSource = this.m_DataSource;
		clone.m_DatabaseType = this.m_DatabaseType;
		clone.m_SourceConnectionConfig = this.m_SourceConnectionConfig;
		
		clone.m_ExternalKeyDelimiter = this.m_ExternalKeyDelimiter;
		clone.m_ExternalKeyValueDelimiter = this.m_ExternalKeyValueDelimiter;
		clone.m_QueryArgStartMarker = this.m_QueryArgStartMarker;
		
		// Default metric path separators
		clone.m_MetricPathDelimiter = this.m_MetricPathDelimiter;
		clone.m_MetricPathMetricPrefix = this.m_MetricPathMetricPrefix;
		clone.m_MetricPathSourcePrefix = this.m_MetricPathSourcePrefix;
		
		clone.m_EarliestTimeOffset = this.m_EarliestTimeOffset;
		
		return clone;
	}
	

	/**
	 * The following properties must be defined:
	 * <dl>
	 * <dt>DataType</dt><dd>The datatype collected by this plugin.</dd> 
	 * <dt>DataBaseType</dt><dd>One of the values of {@link com.prelert.proxy.data.DataBaseType}</dd> 
	 * <dt>DataBaseName</dt><dd>The name of the database to connect to.</dd>
	 * </dl>
	 * 
	 * Additionally the query pairs must be defined. If only a single pair is used 
	 * then they don't have to be numbered else each query pair should end in the 
	 * same number.
	 * <dl>
	 * <dt>AllTimeSeriesQuery[0-9]*</dt><dd>The query used to pull all data from the source
	 * 		database to be sent to the Prelert backends.</dd> 
	 * <dt>TimeSeriesQuery[0-9]*</dt><dd>The query to pull individual time series.</dd> 
	 * </dl>
	 * 
	 * The following properties are optional:
	 * <dl>
	 * <dt>MetricPathDelimiter</dt><dd>Defaults to {@link ServerUtil#METRIC_PATH_DELIMITER}</dd>
	 * <dt>MetricPathMetricPrefix</dt><dd>Defaults to {@link ServerUtil#METRIC_PATH_METRIC_PREFIX}</dd>
	 * <dt>MetricPathSourcePrefix</dt><dd>Defaults to {@link ServerUtil#METRIC_PATH_SOURCE_PREFIX}</dd>
	 * </dl>
	 */
	@Override
	public boolean configure(SourceConnectionConfig config, Properties properties)
	throws InvalidPluginPropertyException
	{
		SortedMap<Integer, String> allDataQueries = new TreeMap<Integer, String>();
		SortedMap<Integer, String> dataQueries = new TreeMap<Integer, String>();
		
		m_SourceConnectionConfig = config;
		
		for (PluginProperty pluginProp: s_Properties)
		{
			String propValue = properties.getProperty(pluginProp.getKey());

			if (pluginProp.isRegularExpression() == false)
			{
				if (pluginProp.isRequired())
				{
					if (propValue == null)
					{
							throw new InvalidPluginPropertyException("Missing property '" + 
									pluginProp.getKey() + "' must be set.");
					}

					if (pluginProp.getPreDefinedValues().isEmpty() == false)
					{
						if (pluginProp.getPreDefinedValues().contains(propValue) == false)
						{
							String msg = String.format("Property '%s' value=%s. Value should be one of: %s", 
									pluginProp.getKey(), propValue, pluginProp.getPreDefinedValues());

							throw new InvalidPluginPropertyException(msg);
						}
					}
				}
				
				if (propValue == null) // property isn't required but is null.
				{
					continue;
				}

				// Simple properties
				if ("DataType".equals(pluginProp.getKey()))
				{
					setDataSourceType(new DataSourceType(propValue, DataSourceCategory.TIME_SERIES));
				}
				else if ("DataBaseType".equals(pluginProp.getKey()))
				{
					m_DatabaseType = DataBaseType.enumValue(propValue);
				}
				else if ("MetricPathDelimiter".equals(pluginProp.getKey()))
				{
					m_MetricPathDelimiter = propValue;
				}
				else if ("MetricPathMetricPrefix".equals(pluginProp.getKey()))
				{
					m_MetricPathMetricPrefix = propValue;
				}
				else if ("MetricPathSourcePrefix".equals(pluginProp.getKey()))
				{
					m_MetricPathSourcePrefix = propValue;
				}

			}
			else // pluginProp.isRegularExpression() == true
			{
				List<String> matchingKeys = matchingPropertyNames(pluginProp.getKey(), properties);
				
				if (pluginProp.isRequired() && matchingKeys.isEmpty())
				{
					throw new InvalidPluginPropertyException("Missing property: No property matches " + 
									"the key " + pluginProp.getKey() + ".");
				}
				
				for (String matchedKey : matchingKeys)
				{
					propValue = properties.getProperty(matchedKey);
					
					if (matchedKey.startsWith("AllTimeSeriesQuery"))
					{
						int index = -1;
						try 
						{
							String indexStr = matchedKey.substring("AllTimeSeriesQuery".length());
							if (indexStr.isEmpty())
							{
								// if no index is specified use 0.
								index = 0;
							}
							else 
							{
								index = Integer.parseInt(indexStr);
							}
						}
						catch (NumberFormatException e)
						{
							s_Logger.error("Invalid property " + matchedKey + "=" + propValue);
							continue;
						}

						allDataQueries.put(index, propValue);
					}
					else if (pluginProp.getKey().startsWith("TimeSeriesQuery"))
					{
						int index = -1;
						try 
						{
							String indexStr = matchedKey.substring("TimeSeriesQuery".length());
							if (indexStr.isEmpty())
							{
								// if no index is specified use 0.
								index = 0;
							}
							else 
							{
								index = Integer.parseInt(indexStr);
							}
						}
						catch (NumberFormatException e)
						{
							s_Logger.error("Invalid property " + matchedKey + "=" + propValue);
							continue;
						}

						dataQueries.put(index, propValue);
					}
				}
			}

		}

		
		Set<Integer> allQueriesKeys = allDataQueries.keySet();
		Iterator<Integer> itr = allQueriesKeys.iterator();
		while (itr.hasNext())
		{
			Integer key = itr.next();
			if (dataQueries.containsKey(key))
			{
				String allQuery = allDataQueries.get(key);
				String oneQuery = dataQueries.get(key);
				if (QueryValidator.validateOpenApiQueryPair(allQuery, oneQuery))
				{
					m_AllTimeSeriesQueries.add(allQuery);
					m_TimeSeriesQueries.add(oneQuery);
				}
			}
			else
			{
				s_Logger.error("The all data query with index " + key + 
						" does not have a matching data query");
			}
		}
		
		
		if (m_AllTimeSeriesQueries.size() == 0)
		{
			String errorMsg = "No valid queries set on OpenAPI Plugin with datatype " + 
										getDataSourceType().getName();
			throw new InvalidPluginPropertyException(errorMsg);
		}
		
		
		Connection connection = null;
		try
		{
			Properties dbProps = new Properties();
			dbProps.setProperty("user", m_SourceConnectionConfig.getUsername());
			dbProps.setProperty("password", m_SourceConnectionConfig.getPassword());

			String connectionUrl = m_DatabaseType.buildConnectionUrl(m_SourceConnectionConfig.getHost(),
															m_SourceConnectionConfig.getPort(), 
															properties.getProperty("DataBaseName"));
			
			m_DataSource = createConnectionPool(connectionUrl, dbProps);
			connection = m_DataSource.getConnection();
			displayDbProperties(m_DataSource.getConnection());
		}
		catch (SQLException e)
		{
			throw new InvalidPluginPropertyException(e.getMessage());
		}
		finally
		{
			try
			{
				if (connection != null) connection.close();
			}
			catch (SQLException e)
			{
				s_Logger.error("Exception in closing connection in loadProperties: " + e);
			}
		}
		
		
		setProperties(properties);
		setConfigured(true);
		
		return true;
	}
	
	
	
	@Override
	public ConnectionStatus testConnection(SourceConnectionConfig config, Properties properties)
	{
		try 
		{
			configure(config, properties);
		}
		catch (InvalidPluginPropertyException e) 
		{
			String msg = "testConnection error: " + e.getMessage();
			s_Logger.error(msg);
			
			ConnectionStatus status = new ConnectionStatus(ConnectionStatus.Status.CONNECTION_FAILED);
			status.setErrorMessage(e.getMessage());
			return status;
		}
		
		return new ConnectionStatus(ConnectionStatus.Status.CONNECTION_OK);
	}
	
	
	@Override
	public List<PluginProperty> getRequriedProperties()
	{
		return s_Properties;
	}
	
	
	/**
	 * Returns a list of all the property names that match
	 * the specified regex.
	 * 
	 * @param propertyNameRegex Can be a regular expression or not
	 * @param properties The Properties to match.
	 * @return A list of matched property names.
	 */
	private List<String> matchingPropertyNames(String propertyNameRegex, Properties properties)
	throws InvalidPluginPropertyException 
	{
		List<String> matchingProps = new ArrayList<String>();
		// See if any of the property keys match the regular expression.
		Set<String> keys = properties.stringPropertyNames();
		for (String key : keys)
		{
			if (key.matches(propertyNameRegex))
			{
				matchingProps.add(key);
			}
		}

		return matchingProps;
	}
	
	
	/**
	 * Returns a Apache DBCP
	 * 
	 * @param connectionUrl
	 * @param props
	 * @return
	 * @throws SQLException
	 */
	private javax.sql.DataSource createConnectionPool(String connectionUrl, 
												Properties props)
	throws SQLException
	{
        // First, we'll create a ConnectionFactory that the
        // pool will use to create Connections.
        ConnectionFactory connectionFactory =
            new DriverManagerConnectionFactory(connectionUrl, props);
     
        // Now we'll need a ObjectPool that serves as the
        // actual pool of connections.
        ObjectPool connectionPool = new GenericObjectPool();
        
        // Next we'll create the PoolableConnectionFactory, which wraps
        // the "real" Connections created by the ConnectionFactory with
        // the classes that implement the pooling functionality.
        @SuppressWarnings("unused")
		PoolableConnectionFactory poolableConnectionFactory =
            new PoolableConnectionFactory(connectionFactory,  
            		connectionPool,
            		null, 
            		null, //"select 1;",
            		false, false);
        

        // Finally, we create the PoolingDriver itself,
        // passing in the object pool we created.
        PoolingDataSource dataSource = new PoolingDataSource(connectionPool);

        return dataSource;
	}
	
	
	private java.sql.Connection getPooledConnection() throws SQLException
	{
		return m_DataSource.getConnection();
	}
	
	    
    /**
     * Prints out the database information.
     * @param connection A valid and open JDBC connection.
     */
	public void displayDbProperties(java.sql.Connection connection)
	throws SQLException
	{
		if (connection != null)
		{
			java.sql.DatabaseMetaData dm = connection.getMetaData();
			s_Logger.info("Driver Information");
			s_Logger.info("\tDriver Name: "+ dm.getDriverName());
			s_Logger.info("\tDriver Version: "+ dm.getDriverVersion ());
			s_Logger.info("Database Information");
			s_Logger.info("\tDatabase Name: "+ dm.getDatabaseProductName());
			s_Logger.info("\tDatabase Version: "+ dm.getDatabaseProductVersion());
			s_Logger.info("Available Catalogs");
			java.sql.ResultSet rs = dm.getCatalogs();
			while (rs.next())
			{
				s_Logger.info("\tcatalog: "+ rs.getString(1));
			} 

			rs.close();
		}
		else
		{
			s_Logger.info("Error: No active Connection");
			}
	} 
	
	
	@Override
	public int getDataTypeItemCount(DataSourceType type) 
	{
		synchronized (s_PointCountByDataType)
		{
			if (getDataSourceType() != null && getDataSourceType().equals(type))
			{
				Map<String, Integer> countsBySource = s_PointCountByDataType.get(type);
				if (countsBySource != null)
				{
					int totalCount = 0;
					for (Integer pointCount : countsBySource.values())
					{
						totalCount += pointCount;
					}
					return totalCount;
				}
			}
		}
		return -1;
	}
	
	
	@Override
	public int getDataSourceItemCount(DataSource source) 
	{
		synchronized (s_PointCountByDataType)
		{
			if (getDataSourceType() != null && getDataSourceType().equals(source.getDataSourceType()))
			{
				Map<String, Integer> countsBySource = s_PointCountByDataType.get(source.getDataSourceType());
				if (countsBySource != null)
				{
					Integer count = countsBySource.get(source.getSource());
					if (count != null)
					{
						return count;
					}
				}
				else
				{
					s_Logger.error(String.format("Unable to get the point count for source = %s.", source));
					return -1;
				}
			}
			
			s_Logger.error(String.format("Plugin %s does not support the datatype %s. " +
					"Unable to get the point count for source = %s.", 
					getName(), source.getDataSourceType(), source));
		}
		return -1;
	}


	
	/**
	 * Returns true if the ':StartTime' token comes before ':EndTime' in 
	 * the query string. This affects the order of the arguments.
	 * 
	 * @param queryString - The SQL query string which must contain the 
	 *   	:StartTime and :EndTime tokens.
	 * @return
	 */
	private boolean startComesBeforeEnd(String queryString)
	{
		String startTimeToken = getQueryArgStartMarker() + START_TIME_TOKEN;
		String endTimeToken = getQueryArgStartMarker() + END_TIME_TOKEN;
		
		int startIndex = queryString.indexOf(startTimeToken);
		int endIndex = queryString.indexOf(endTimeToken);
		
		if (startIndex < 0 || endIndex < 0)
		{
			String msg = String.format("Invalid query does not contain %s and %s. Query = %s",
									startTimeToken, endTimeToken, queryString);
			s_Logger.error(msg);
			return true;
		}
		return startIndex < endIndex;
	}
	
	
	/**
	 * Replace the query :StartTime and :EndTime tags with '?'
	 * 
	 * @param queryString
	 * @return
	 */
	private String replaceDateParameters(String queryString)
	{
		String startTimeToken = getQueryArgStartMarker() + START_TIME_TOKEN;
		String endTimeToken = getQueryArgStartMarker() + END_TIME_TOKEN;

		String temp = queryString.replace(startTimeToken, "?");
		return temp.replace(endTimeToken, "?");
	}
	

	/**
	 * The query date parameter are all integer values seconds since
	 * the epoch.
	 * 
     * @param intervalSecs is not used.
     */
	@Override
	public Collection<TimeSeriesData> getAllDataPointsForTimeSpan(Date startTime,
			Date endTime, int intervalSecs)
	throws ErrorGettingDataPointsException
	{
		s_Logger.debug(String.format("getAllDataPointsForTimeSpan(%s, %s, %d)", 
							startTime, endTime, intervalSecs));
		
		List<TimeSeriesData> timeSeriesData = new ArrayList<TimeSeriesData>();
		java.sql.Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		String errorMessage = null;

		try 
		{
			connection = getPooledConnection();

			for (int i=0; i<m_AllTimeSeriesQueries.size(); ++i)
			{
				try
				{
					String query = m_AllTimeSeriesQueries.get(i);

					int startTimeArgPos = 1;
					int endTimeArgPos = 2;

					if (!startComesBeforeEnd(query))
					{
						startTimeArgPos = 2;
						endTimeArgPos = 1;
					}

					String parameterisedQuery = replaceDateParameters(query); 
					statement = connection.prepareStatement(parameterisedQuery);

					statement.setLong(startTimeArgPos, startTime.getTime() / 1000);
					statement.setLong(endTimeArgPos, endTime.getTime() / 1000);

					resultSet = statement.executeQuery();
					timeSeriesData.addAll(processAllTimeSeriesQueryResults(resultSet, i));
				}
				catch (SQLException e) 
				{
					errorMessage = e.getMessage();
					s_Logger.error("Exception in getAllDataPointsForTimeSpan(), query number = " + i +" Error=" + e);
				}
			}
			
			// No data was returned and an error has been logged.
			if (timeSeriesData.size() == 0 && errorMessage != null)
			{
				throw new ErrorGettingDataPointsException(errorMessage);
			}
		}
		catch (SQLException e) 
		{
			s_Logger.error("Could not connect to database in getAllDataPointsForTimeSpan(): " + e);
		}
		finally
		{
			try
			{
				if (resultSet != null)	resultSet.close();
				if (statement != null)	statement.close();
				if (connection != null)	connection.close();
			}
			catch (SQLException e)
			{
				s_Logger.error("Exception in closing connection in getAllDataPointsForTimeSpan: " + e);
			}
		}
		
		return timeSeriesData;
	}

	
    /**
     * @param intervalSecs is not used.
     */
	@Override
	public List<TimeSeriesDataPoint> getDataPointsForTimeSpan(
			String externalKey, Date startTime, Date endTime, int intervalSecs) 
	{
		s_Logger.debug(String.format("getDataPointsForTimeSpan(%s, %s, %s, %d)", 
					externalKey, startTime, endTime, intervalSecs));

		String metricKey = METRIC + getExternalKeyValueDelimiter();
		String queryIdKey = QUERY_ID + getExternalKeyValueDelimiter();
		int queryIndex = -1;
		String metric = null;
		
		String[] pairs = externalKey.split(getExternalKeyDelimiter());
		for (String keyValue : pairs)
		{
			int index = keyValue.indexOf(metricKey);
			if (index >= 0)
			{
				metric = keyValue.substring(index + metricKey.length());
			}
			else 
			{
				index = keyValue.indexOf(queryIdKey);
				if (index >= 0)
				{
					String value = keyValue.substring(index + queryIdKey.length());
					try
					{
						queryIndex = Integer.parseInt(value);
					}
					catch (NumberFormatException e)
					{
						s_Logger.error("getDataPointsForTimeSpan() Cannot parse " + QUERY_ID + " in external key=" + 
									externalKey);
					}
				}
			}
		}
		
		
		if (metric == null)
		{
			s_Logger.error("Invalid external key in getDataPointsForTimeSpan(). No metric specified in key=" 
													+ externalKey);
			return Collections.emptyList();
		}
		
		if (queryIndex < 0)
		{
			s_Logger.error("Invalid external key in getDataPointsForTimeSpan(). No query index specified in key=" 
													+ externalKey);
			return Collections.emptyList();
		}
		
		
		List<TimeSeriesDataPoint> timeSeriesPoints = new ArrayList<TimeSeriesDataPoint>();

		if (queryIndex >=0 && queryIndex < m_TimeSeriesQueries.size())
		{
			String rawQuery = m_TimeSeriesQueries.get(queryIndex);
			int startTimeArgPos = 1;
			int endTimeArgPos = 2;
			
			if (!startComesBeforeEnd(rawQuery))
			{
				startTimeArgPos = 2;
				endTimeArgPos = 1;
			}
			
			
			String query = replaceQueryArgs(rawQuery, externalKey);
			query = replaceDateParameters(query); 

			java.sql.Connection connection = null;
			PreparedStatement statement = null;
			ResultSet resultSet = null;
			
			try
			{
				connection = getPooledConnection();
				statement = connection.prepareStatement(query);
				statement.setLong(startTimeArgPos, startTime.getTime() / 1000);
				statement.setLong(endTimeArgPos, endTime.getTime() / 1000);

				resultSet = statement.executeQuery();
				timeSeriesPoints.addAll(processTimeSeriesQueryResults(resultSet, metric));
			}
			catch (SQLException e) 
			{
				s_Logger.error("Exception in getDataPointsForTimeSpan(), query number = " + queryIndex +" Error=" + e);
			}
			finally
			{
				try
				{
					if (resultSet != null)	resultSet.close();
					if (statement != null)	statement.close();
					if (connection != null)	connection.close();
				}
				catch (SQLException e)
				{
					s_Logger.error("Exception in closing connection in getDataPointsForTimeSpan: " + e);
				}
			}
		}
		
		return timeSeriesPoints;
	}
	
	
	/**
	 * First removes any duplicates from the list of externalKeys
	 * then finds the peak value for the time period for each
	 * time series for that external key.
	 */
	@Override
	public List<ExternalKeyPeakValuePair> getPeakValueForTimeSpan(List<String> externalKeys, 
										Date startTime, Date endTime,
										int intervalSecs)
	{
		s_Logger.debug(String.format("getPeakValueForTimeSpan(%s, %s, %s, %d)", 
				externalKeys, startTime, endTime, intervalSecs));

		List<ExternalKeyPeakValuePair> result = new ArrayList<ExternalKeyPeakValuePair>();
		
		// First make sure no externalKey is duplicated.
		Set<String> keys = new HashSet<String>();
		for (String externalKey : externalKeys)
		{
			keys.add(externalKey);
		}
		
		
		for (String key : keys)
		{
			List<TimeSeriesDataPoint> points = getDataPointsForTimeSpan(key, startTime, endTime, intervalSecs); 
			
			double max = 0.0;
			for (TimeSeriesDataPoint pt : points)
			{
				max = (pt.getValue() > max) ? pt.getValue() : max;
			}
			
			result.add(new ExternalKeyPeakValuePair(key, max));
		}
		
		return result;
	}

	
	/**
	 * Replace all the argument strings that start with ':Key_', 
	 * ':Attribute' or ':Source' with the string argument extracted 
	 * from the <code>externalKey</code>. If the query parameter 
	 * cannot be found in <code>externalKey</code> then an 
	 * <code>IllegalStateException</code> is thrown.
	 * 
	 * @param query The SQL query.
	 * @param externalKey A delimited list of name, value pairs.
	 * @return The query string with the arguments replaced.
	 * @throws IllegalStateException If the parameter listed in the 
	 * 		query cannot be found in <code>externalKey</code>.
	 */
	public String replaceQueryArgs(String query, String externalKey)
	{
		List<KeyValue> keys = parseExternalKey(externalKey);
		String result = query;
		
		// Find and replace ':Source'
		String sourceMarker = getQueryArgStartMarker() + SOURCE_COLUMN;
		if (result.contains(sourceMarker))
		{
			boolean replaced = false;
			for (KeyValue kv : keys)
			{
				if (kv.getKeyName().equals(sourceMarker))
				{
					result = result.replace(sourceMarker, "'" + kv.getValue() + "'");
					replaced = true;
					break;
				}
			}
			
			if (!replaced)
			{
				throw new IllegalStateException(
						String.format("No replacement was found for the query Source argument. " +
								"Cannot execute query with external key = %s. Query = %s", 
								externalKey, query));
			}
		}
		
		
		// Match anything starting with ':Metric_' or ':Key_' 
		String regexStr = getQueryArgStartMarker() + "(" + ATTRIBUTE_COLUMN_ID + "|" + KEY_COLUMN_ID + ")\\w+";
		Pattern pattern = Pattern.compile(regexStr);
		Matcher matcher = pattern.matcher(query);
		
		
		while (matcher.find())
		{
			String group = matcher.group();

			// find the replacement string.
			Iterator<KeyValue> iter = keys.iterator();
			boolean replaced = false;
			while (iter.hasNext())
			{
				KeyValue kv = iter.next();
				if (kv.getKeyName().equals(group))
				{
					result = result.replaceAll(group, "'" + kv.getValue() + "'");
					iter.remove();
					replaced = true;
					break;					
				}
			}
			
			if (!replaced)
			{
				throw new IllegalStateException(
						String.format("No replacement was found for the query argument %s. " +
								"Cannot execute query with external key = %s. Query = %s", 
								group, externalKey, query));
			}
		}
		
		return result;		
	}

	
	/**
	 * Processes the <code>ResultSetMetaData</code> extracting all
	 * the Metrics, Keys and Attributes returning a map of each to
	 * its column number.
	 * 
	 * Note ResultSet columns indexes are 1 based.
	 * 
	 * @param metaData
	 * @return
	 * @throws SQLException
	 */
	private Tokens processColumnNames(ResultSetMetaData metaData) throws SQLException
	{
		Tokens tokens = new Tokens();
			
		int columnCount = metaData.getColumnCount();
		for (int i=1; i<=columnCount; ++i)
		{
			String columnName = metaData.getColumnLabel(i);
			
			if (columnName.startsWith(METRIC_COLUMN_ID))
			{
				String name = columnName.substring(METRIC_COLUMN_ID.length());
				tokens.getMetrics().add(new NamePosition(name, i));
			}
			else if (columnName.startsWith(KEY_COLUMN_ID))
			{
				String name = columnName.substring(KEY_COLUMN_ID.length());
				tokens.getKeys().add(new NamePosition(name, i));
			}
			else if (columnName.startsWith(ATTRIBUTE_COLUMN_ID))
			{
				String name = columnName.substring(ATTRIBUTE_COLUMN_ID.length());
				tokens.getAttributes().add(new NamePosition(name, i));
			}
			else
			{
				if (m_ReservedColumns.contains(columnName))
				{
					s_Logger.error(String.format("Invalid column name '%s'. '%s' is a reserved word.", 
												columnName));
				}
				// else it must be one of source, datatype etc.
				else if (m_EssentialColumns.contains(columnName) == false)
				{
					s_Logger.error("Unknown Column " + columnName);
				}
			}
		}
		
		return tokens;
	}
	
	
	/**
	 * Process the result set from the AllTimeSeriesQuery
	 * for Time Series Data Points.
	 * 
	 * The result set should contain the following columns at least:
	 * <ul>
	 * <li>Source</li>
	 * <li>DateTime</li>
	 * <li>At least one field starting with Metric_</li>
	 * </ul>
	 * 
	 * The following field names are not allowed in the query:<br/>
	 * type, source, time, metric, id, time_series_id, time_series_type_id,
	 * count, probable_cause, severity
	 * <br/>
	 *
	 * Optionally the query can return a column called Description else
	 * the metric is used for the description of the time series.
	 * All other columns are added as Attributes.
	 * 
	 * This function also updates the lastestTime member if the last point 
	 * in one of the returned TimeSeriesData come after the current latest time.
	 * 
	 * @param resultSet The SQL query result set.
	 * @param queryIndex The index of the specific for retrieving the data.  
	 * @return
	 */
	private Collection<TimeSeriesData> processAllTimeSeriesQueryResults(ResultSet resultSet, 
									int queryIndex)
	{
		// Get the time series datatype name we are using.
		DataSourceType datatype = null;
		if (getDataSourceType() != null && getDataSourceType().getDataCategory() == DataSourceCategory.TIME_SERIES)
		{
			datatype = getDataSourceType();
		}
		
		if (datatype == null)
		{
			s_Logger.error("Plugin " + getName() + " has no associated datatype. ");
					
			datatype = new DataSourceType();
		}
		
		Map<String, TimeSeriesData> dataMap = new HashMap<String, TimeSeriesData>();
		
		// Get the current point counts for this datatype.
		Map<String, Integer> pointCountBySource;
		synchronized(s_PointCountByDataType)
		{
			Map<String, Integer> prevCounts = s_PointCountByDataType.get(datatype);
			if (prevCounts != null)
			{
				pointCountBySource = new HashMap<String, Integer>(prevCounts);
			}
			else
			{
				pointCountBySource = new HashMap<String, Integer>();
			}
		}
		
		
		try
		{
			ResultSetMetaData metaData = resultSet.getMetaData();
			Tokens tokens = processColumnNames(metaData);
			
			boolean hasSubType = false;
			for (NamePosition attribute : tokens.getAttributes())
			{
				if (attribute.getColumnName().equals(SUBTYPE_COLUMN))
				{
					hasSubType = true;
				}
			}
			
			int sourceColumn = resultSet.findColumn(SOURCE_COLUMN);
			int dateTimeColumn = resultSet.findColumn(DATETIME_COLUMN);
			int descriptionColumn = -1;
			try
			{
				descriptionColumn = resultSet.findColumn(DESCRIPTION_COLUMN);
			}
			catch (SQLException e)
			{
				// no desc
			}

			while (resultSet.next())
			{
				String source = resultSet.getString(sourceColumn);
				long dateTime = resultSet.getLong(dateTimeColumn) * 1000; // convert from seconds since epoch to milliseconds.
				
				// Get the current count for this source.
				Integer pointCount = pointCountBySource.get(source);
				if (pointCount == null)
				{
					pointCount = new Integer(0);
				}
				
				// Build attributes
				List<Attribute> attrs = new ArrayList<Attribute>();
				int i=1;
				for (NamePosition attribute : tokens.getAttributes())
				{
					String colValue = resultSet.getString(attribute.getColumnNumber());
					attrs.add(new Attribute(attribute.getColumnName(), colValue,
									getMetricPathDelimiter(), i++));
				}

				
				// External key is built of all a query Id which specifies 
				// the particular query used and all attributes and keys
				// so it may contain some redundant values.
				StringBuilder keyBuilder = new StringBuilder();
				keyBuilder.append(QUERY_ID);
				keyBuilder.append(getExternalKeyValueDelimiter());
				keyBuilder.append(Integer.toString(queryIndex));
				keyBuilder.append(getExternalKeyDelimiter());
				
				for (NamePosition key : tokens.getKeys())
				{
					keyBuilder.append(getQueryArgStartMarker()).append(KEY_COLUMN_ID).append(key.getColumnName());
					keyBuilder.append(getExternalKeyValueDelimiter());
					keyBuilder.append(resultSet.getString(key.getColumnNumber()));
					keyBuilder.append(getExternalKeyDelimiter());
				}
				
				for (NamePosition attr : tokens.getAttributes())
				{
					keyBuilder.append(getQueryArgStartMarker()).append(ATTRIBUTE_COLUMN_ID).append(attr.getColumnName());
					keyBuilder.append(getExternalKeyValueDelimiter());
					keyBuilder.append(resultSet.getString(attr.getColumnNumber()));
					keyBuilder.append(getExternalKeyDelimiter());
				}
			
				
				// now for each metric
				for (NamePosition namePosition : tokens.getMetrics())
				{
					String metric = namePosition.getColumnName();

					String externalKey = keyBuilder.toString() + getQueryArgStartMarker() + SOURCE_COLUMN + 
											getExternalKeyValueDelimiter() + source;
					externalKey = externalKey + getExternalKeyDelimiter() + METRIC + getExternalKeyValueDelimiter() + metric;
					
					TimeSeriesData data = dataMap.get(externalKey);
					if (data == null)
					{
						TimeSeriesConfig config = new TimeSeriesConfig(datatype.getName(), metric, source);
						String description = (descriptionColumn >= 0) ? resultSet.getString(descriptionColumn) : metric;
						config.setDescription(description);
						config.setAttributes(attrs); 
						config.setExternalKey(externalKey);
						config.setMetricPrefix(getMetricPathMetricPrefix());
						// If the query has a subtype then the source is second 
						// element in the metric path, else its the first.
						config.setSourcePosition(hasSubType ? 1 : 0); 
						config.setSourcePrefix(getMetricPathSourcePrefix());

						data = new TimeSeriesData(config, new ArrayList<TimeSeriesDataPoint>());
						
						dataMap.put(externalKey, data);
					}

					double value = resultSet.getDouble(namePosition.getColumnNumber());
					data.getDataPoints().add(new TimeSeriesDataPoint(dateTime, value));
					
					// track the number of points for this source.
					pointCount++;
				}
				
				pointCountBySource.put(source, pointCount);
			}				
		}
		catch (SQLException e)
		{
			s_Logger.error("Exception in processAllTimeSeriesQueryResults(): " + e);
		}
		
		
		// Update the number of points seen.
		synchronized(s_PointCountByDataType)
		{
			s_PointCountByDataType.put(datatype, pointCountBySource);
		}
		
		
		for (TimeSeriesData data : dataMap.values())
		{
			Collections.sort(data.getDataPoints());
		}
		
		// Get the time of the last point in the first time
		// series and compare that to the current latest time.
		Collection<TimeSeriesData> result = dataMap.values();
		if (result.size() > 0)
		{
			TimeSeriesData data = result.iterator().next();
			if (data.getDataPoints().size() > 0)
			{
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
		
		return dataMap.values();
	}
	

	/**
	 * Process the result set from the TimeSeriesQuery and return
	 * all the data points for the given metric.
	 * 
	 * The result set should contain the following columns at least:
	 * <ul>
	 * <li>DateTime</li>
	 * <li>At least one field starting with Metric_</li>
	 * </ul>
	 * 
	 * The following field names are not allowed in the query:<br/>
	 * type, source, time, metric, id, time_series_id, time_series_type_id,
	 * count, probable_cause, severity
	 * <br/>
	 *
	 *@param metric - The metric to get data for.
	 * @param resultSet - The result of running TimeSeriesQuery
	 * @return list of data points
	 */
	private List<TimeSeriesDataPoint> processTimeSeriesQueryResults(ResultSet resultSet, String metric)
	{
		List<TimeSeriesDataPoint> results = new ArrayList<TimeSeriesDataPoint>();
		
		try
		{
			ResultSetMetaData metaData = resultSet.getMetaData();
			Tokens tokens = processColumnNames(metaData);
			
			int metricColumn = -1;
			for (NamePosition pos : tokens.getMetrics())
			{
				if (pos.getColumnName().equals(metric))
				{
					metricColumn = pos.getColumnNumber();
					break;
				}
			}
			
			if (metricColumn == -1)
			{
				s_Logger.error(
						String.format("Unknown metric. '%s' not returned in query.", metric));
				return results;
			}
			
			int dateTimeColumn = resultSet.findColumn("DateTime");
			
			while (resultSet.next())
			{
				long dateTime = resultSet.getLong(dateTimeColumn) * 1000; // convert from seconds since epoch to milliseconds.
				double value = resultSet.getDouble(metricColumn);
				
				results.add(new TimeSeriesDataPoint(dateTime, value));
			}
		}
		catch (SQLException e)
		{
			s_Logger.error("Exception in processTimeSeriesQueryResults(): " + e);
		}
		
		return results;
	}
	
	
	/**
	 * Split <code>externalKey</code> into Key-Value pairs.
	 * 
	 * @param externalKey
	 * @return List of KeyValue's
	 */
	private List<KeyValue> parseExternalKey(String externalKey)
	{
		List<KeyValue> result = new ArrayList<KeyValue>();
		
		String[] pairs = externalKey.split(getExternalKeyDelimiter());
		for (String pair : pairs)
		{
			String[] split = pair.split(getExternalKeyValueDelimiter());
			
			if (split.length != 2)
			{
				throw new IllegalStateException("Invalid external key: " + externalKey);
			}
			
			result.add(new KeyValue(split[0], split[1]));
		}
		
		return result;
	}
	
	
	/**
	 * Aggregation is not supported through the open api.
	 */
	@Override
	public boolean isAggregationSupported(String dataType) 
	{
		return false;
	}
	
	
	/**
	 * This plugin does not support selecting an interval 
	 * for time series points data.
	 * This method always returns 60.
	 * 
	 * @return 60
	 */
	@Override
	public int getUsualPointIntervalSecs() 
	{
		return 60;
	}

	/**
	 * Empty function - does nothing.
	 */
	@Override
	public void setUsualPointIntervalSecs(int value) 
	{
	}


	/**
	 * If LatestTime has be set (set if getAllDataPointsForTimeSpan() has 
	 * been called and returned data) then that is returned else 
	 * the time now is returned.
	 */
	@Override
	public Date getLatestTime(String dataType, String source)
	{
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
	
	/** 
	 * Get the number of days before now used calculating the 
	 * result of {@linkplain #getEarliestTime}. The date returned
	 * by getEarliestTime is configurable through this value.
	 * @return
	 */
	public int getEarliestTimeOffset()
	{
		return m_EarliestTimeOffset;
	}
	
	public void setEarliestTimeOffset(int value)
	{
		m_EarliestTimeOffset = value;
	}
	
	/**
	 * The delimiter string used to between the key-value pairs
	 * in the external key. 
	 * Defaults to EXTERNAL_KEY_DELIMITER.
	 * 
	 * The default values create an externalKey string formatted as:<br/>
	 * key1=value1&&key2=value2&&...&&keyN=valueN
	 * where <code>ExternalKeyDelimiter</code> = '&&' 
	 * and <code>ExternalKeyValueDelimiter</code> = '='
	 * 
	 * @return
	 */
	public String getExternalKeyDelimiter()
	{
		return m_ExternalKeyDelimiter;
	}

	public void setExternalKeyDelimiter(String value)
	{
		m_ExternalKeyDelimiter = value;
	}
	
	
	/**
	 * The string that marks the beginning of an argument that
	 * will be replaced in the SQL query.
	 * Defaults to QUERY_ARG_START_MARKER so the default marker
	 * for the start time argument would be ':StartTime'
	 * 
	 * @return
	 */
	public String getQueryArgStartMarker()
	{
		return m_QueryArgStartMarker;
	}
	
	public void setQueryArgStartMarker(String value)
	{
		m_QueryArgStartMarker = value;
	}
	

	/**
	 * The delimiter string used to separate key-value pairs
	 * in the external key.
	 * Defaults to EXTERNAL_KEY_VALUE_DELIMITER.
	 * 
	 * The default values create an externalKey string formatted as:<br/>
	 * key1=value1&&key2=value2&&...&&keyN=valueN
	 * where <code>ExternalKeyDelimiter</code> = '&&' 
	 * and <code>ExternalKeyValueDelimiter</code> = '='
	 * 
	 * @return
	 */
	public String getExternalKeyValueDelimiter()
	{
		return m_ExternalKeyValueDelimiter;
	}
	
	public void setExternalKeyValueDelimiter(String value)
	{
		m_ExternalKeyValueDelimiter = value;
	}
	
	
	@Override
	public String getMetricPathDelimiter()
	{
		return m_MetricPathDelimiter;
	}
	
	public void setMetricPathDelimiter(String value)
	{
		m_MetricPathDelimiter = value;
	}
	
	
	@Override
	public String getMetricPathMetricPrefix()
	{
		return m_MetricPathMetricPrefix;
	}
	
	public void setMetricPathMetricPrefix(String value)
	{
		m_MetricPathMetricPrefix = value;
	}
	
	@Override
	public String getMetricPathSourcePrefix()
	{
		return m_MetricPathSourcePrefix;
	}
	
	public void setMetricPathSourcePrefix(String value)
	{
		m_MetricPathSourcePrefix = value;
	}
	
	
	/**
	 * Resets the total points that have passed through the plugin to 0
	 * for the datatype supported by this plugin.
	 * Also sets the latestTime of all the seen data points to null
	 * for the plugin's datatype.
	 */
	@Override
	public void reset()
	{
		if (getDataSourceType() != null)
		{
			DataSourceType datatype = getDataSourceType();

			synchronized (s_PointCountByDataType)
			{
				// Replace the map entry for this datatype.
				s_PointCountByDataType.put(datatype, new HashMap<String, Integer>());
			}
			
			synchronized (s_LatestTimeByDataType)
			{
				s_LatestTimeByDataType.put(datatype, null);
			}
		}
		else
		{
			s_Logger.warn("Plugin.reset(): Plugin " + getName() + " has no associated datatype.");
		}
		
	}
	
	
	/**
	 * Helper class maps a SQL result set column name to its
	 * position (column number).
	 * 	
	 */
	private class NamePosition
	{
		private int m_ColumnNumber;
		private String m_ColumnName;
		
		public NamePosition(String columnName, int columnNumber)
		{
			m_ColumnName = columnName;
			m_ColumnNumber = columnNumber;
		}
		
		public String getColumnName()
		{
			return m_ColumnName;
		}
		
		public int getColumnNumber()
		{
			return m_ColumnNumber;
		}
	}
	
	
	/**
	 * Class maps all the Metrics, Keys and Attributes in a SQL
	 * resultset to their column number. 
	 */
	private class Tokens
	{
		private List<NamePosition> m_Metrics = new ArrayList<NamePosition>();
		private List<NamePosition> m_Keys = new ArrayList<NamePosition>();
		private List<NamePosition> m_Attributes = new ArrayList<NamePosition>();
		
		/**
		 * Returns all the metrics.
		 * @return
		 */
		public List<NamePosition> getMetrics()
		{
			return m_Metrics;
		}
		
		/**
		 * Returns all the keys.
		 * @return
		 */
		public List<NamePosition> getKeys()
		{
			return m_Keys;
		}
		
		/**
		 * Returns all the attributes.
		 * @return
		 */
		public List<NamePosition> getAttributes()
		{
			return m_Attributes;
		}
	}
	
	
	/**
	 * Simple Key-Value pair class.
	 */
	private class KeyValue 
	{
		private String m_KeyName;
		private String m_Value;

		public KeyValue(String keyName, String value)
		{
			m_KeyName = keyName;
			m_Value = value;
		}
		
		public String getKeyName()
		{
			return m_KeyName;
		}
		
		public String getValue()
		{
			return m_Value;
		}
	}
}
