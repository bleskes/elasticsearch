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

package com.prelert.proxy.plugin.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;

import org.apache.log4j.Logger;

import com.prelert.data.Attribute;
import com.prelert.data.ConnectionStatus;
import com.prelert.data.DataSource;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.Notification;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.TimeSeriesData;
import com.prelert.data.TimeSeriesDataPoint;
import com.prelert.proxy.data.SourceConnectionConfig;
import com.prelert.proxy.inputmanager.querymonitor.QueryTookTooLongException;
import com.prelert.proxy.plugin.InternalPlugin;
import com.prelert.proxy.plugin.NotificationPlugin;
import com.prelert.proxy.plugin.Plugin;


/**
 * Class which implements all the Plugin functionality in 
 * SQL queries. This is a generic implementation which does not support GUI
 * access through the <code>getDataPointsForTimeSpan</code> functions but 
 * can be used to collect data using <code>getAllDataPointsFroTimeSpan(Date, Date, in)</code>
 */
public class JdbcPlugin extends Plugin implements InternalPlugin, NotificationPlugin
{
	private static Logger s_Logger = Logger.getLogger(JdbcPlugin.class);
	
	protected String m_ConnectionUrl;
	
	// Calendar object for the servers time zone. 
	private Calendar m_ServerTimeZoneCalendar;
	
	/**
	 * Query Strings
	 */
	private String m_MetricsQuery;
	private String m_NotificationSourcesQuery;
	private String m_TimeSeriesSourcesQuery;
	private String m_NotificationsQuery;
	private String m_AllTimeSeriesDataQuery;
	private String m_AttributeNamesQuery;
	private String m_AttributeValuesQuery;
	
	protected java.sql.Connection m_Connection;
	
	private int m_DefaultPointIntervalSeconds;
	
	private DataSourceCategory m_DataSourceCategory;
	
	/**
	 * Default constructor.
	 */
	public JdbcPlugin()
	{
	}

	/**
	 * Returns a full duplicate of this instance with the same
	 * properties set.
	 */
	@Override
	public Plugin duplicate() 
	{
		JdbcPlugin clone = new JdbcPlugin();
		clone.setName(getName());
		
		try
		{
			clone.configure(null, m_Properties);
		}
		catch (InvalidPluginPropertyException e)
		{
			s_Logger.error("Error duplicating JdbcPlugin");
			s_Logger.error(e);
		}

		return clone;
	}
	
	
	/**
	 * The following properties MUST be defined for the plugin. If any of these
	 * properties are not defined a <code>InvalidPluginPropertyException</code>
	 * is thrown:
	 * 'connectionUrl' - The database connection string.
	 * 
	 * The following properties are optional:
	 * 'datatype' - the data type handled by the plugin
	 * 'category' - Either time_series or notification
	 * 
	 * @param config Isn't used in this function.
	 */
	@Override
	public boolean configure(SourceConnectionConfig config, Properties properties)
	throws InvalidPluginPropertyException 
	{
		String datatype = properties.getProperty("DataType");
		if (datatype == null)
		{
			throw new InvalidPluginPropertyException("Missing property 'DataType'" +
					" must be set."); 
		}

		String type = properties.getProperty("category");
		m_DataSourceCategory =  DataSourceCategory.getValue(type);
		switch (m_DataSourceCategory)
		{
		case TIME_SERIES:
			setDataSourceType(new DataSourceType(datatype, DataSourceCategory.TIME_SERIES));
			break;
		case NOTIFICATION:
			setDataSourceType(new DataSourceType(datatype, DataSourceCategory.NOTIFICATION));
			break;					
		default:
			throw new InvalidPluginPropertyException("Invalid property 'category'. " +
						"Should be either 'time_series' or 'notification'."); 
		}
		
		
		m_ConnectionUrl = loadAndValidateProperty("connectionUrl", properties);
		
		m_Properties = properties;
		
		connect();
		
		setConfigured(true);
		
		return true;
	}
	
	/**
	 * Loads the property with name <code>propertyName</code> and throws
	 * an exception if the property does not exist.
	 * @param propertyName
	 * @param properties
	 * @return
	 * @throws InvalidPluginPropertyException
	 */
	protected String loadAndValidateProperty(String propertyName, Properties properties)
	throws InvalidPluginPropertyException 
	{
		String result = properties.getProperty(propertyName);
		
		if (result == null)
		{
			throw new InvalidPluginPropertyException("Invalid property '" + 
												propertyName + "'");
		}
		
		return result;
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
			s_Logger.error("testConnection error: ", e);
			return new ConnectionStatus(ConnectionStatus.Status.CONNECTION_FAILED);
		}
		
		return new ConnectionStatus(ConnectionStatus.Status.CONNECTION_OK);
	}
	
	
	/**
	 * Connects to the database or throws.
	 * If connected it finds the time zone the SQL server is in
	 * and creates an appropriate calendar object.
	 * 
	 * @throws InvalidPluginPropertyException
	 */
	protected void connect() throws InvalidPluginPropertyException 
	{
		m_Connection = getConnection(m_ConnectionUrl);
		
		TimeZone tz = getTimeZone();
		m_ServerTimeZoneCalendar = Calendar.getInstance(tz);
	}
	

	/**
	 * Creates the JDBC connection or throws if the connection can't
	 * be made.
	 * 
	 * @return
	 * @throws InvalidPluginPropertyException
	 */
    protected java.sql.Connection getConnection(String connectionUrl) throws InvalidPluginPropertyException 
    {
    	java.sql.Connection connection = null;
        try
        {
             // The DriverManager will load the correct class for the type in
        	 // connection url.
             connection = java.sql.DriverManager.getConnection(connectionUrl);
             
             connection.setReadOnly(true);
        }
        catch(Exception e)
        {
        	s_Logger.error("Exception in getConnection(): " + e);

        	String msg = String.format("Could not create SQL connection with url = %s",
        							connectionUrl);
        	throw new InvalidPluginPropertyException(msg);
        }
        
        return connection;
    }	

    
    /**
     * If a connection has been successful the display the DB properties.
     */
	public void displayDbProperties()
	{
		try
		{
			if (m_Connection != null)
			{
				java.sql.DatabaseMetaData dm = m_Connection.getMetaData();
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
		catch (Exception e)
		{
			s_Logger.error("Exception in displayDbProperties(): " + e);
		}
	}   
	
	
	/**
	 * Find the time zone the sql server is in by comparing 
	 * its local date time to UTC date time.
	 * If that fails the default timezone (timezone this program
	 * is running in) is returned.
	 * 
	 * @return The TimeZone object representing the server's timezone
	 * 			or the default TimeZone
	 */
	public TimeZone getTimeZone()
	{
		final String TIMEZONE_QUERY = "SELECT DATEDIFF(hh, GETUTCDATE(), GETDATE()) AS ServerTimeZone;";
		
		try
		{
			PreparedStatement statement = m_Connection.prepareStatement(TIMEZONE_QUERY);	
			ResultSet resultSet = statement.executeQuery();
			
			if (resultSet.next())
			{
				int offset = resultSet.getInt(1);
				String timeZoneId = String.format("GMT%+d", offset);
				TimeZone tz = TimeZone.getTimeZone(timeZoneId);
				
				s_Logger.info("SQL Server Timezone = " + tz.getID());

				return tz;
			}
		}
		catch (SQLException e) 
		{
			s_Logger.error("Error finding the SQL Server's timezone: " + e);
		}
		
		return TimeZone.getDefault();
	}
	
    
    /**
     * Closes the SQL driver connection. Resources are released
     * immediately. 
     */
    private void closeConnection()
    {
    	try
    	{
    		if (m_Connection != null)
    		{
    			m_Connection.close();
    		}

    		m_Connection = null;
    	}
    	catch(Exception e)
    	{
    		s_Logger.error("Error closing JDBC connection");
    	}
    }


    /**
     * Executes either the <code>NotificationSourcesQuery</code> or 
     * <code>TimeSeriesSourcesQuery</code> depending on the <code>type</code> parameter.
     * 
     * Returns all the data sources for the specified type.
     *  
     * @param type
     * @return
     */
    public List<DataSource> getDataSources(DataSourceType type)
    {
    	s_Logger.debug(String.format("getDataSources(%s)", type));

    	List<String> sourceNames = new ArrayList<String>();

    	try 
    	{
    		String query = null;
    		if (type.getDataCategory() == DataSourceCategory.NOTIFICATION)
    		{
    			query = getNotificationSourcesQuery();
    		}
    		else if (type.getDataCategory() == DataSourceCategory.TIME_SERIES)
    		{
    			query = getTimeSeriesSourcesQuery();
    		}


    		if (query == null)
    		{
    			s_Logger.error("Cannot get DataSource Query for type " + 
    					type.getDataCategory());
    			return Collections.emptyList();
    		}

    		Statement statement = m_Connection.createStatement();
    		ResultSet resultSet = statement.executeQuery(query);

    		while (resultSet.next())
    		{
    			sourceNames.add(resultSet.getString(1));
    		}
    	}
    	catch (SQLException e) 
    	{
    		s_Logger.error("Exception in getDataSources(): " + e);
    		return Collections.emptyList();
    	}

    	List<DataSource> dataSources = new ArrayList<DataSource>();

    	for (String sourceName : sourceNames)
    	{
    		DataSource dataSource = new DataSource();
    		dataSource.setDataSourceType(type);
    		dataSource.setSource(sourceName);
    		dataSource.setExternalPlugin(getName());

    		dataSources.add(dataSource);
    	}

    	return dataSources;
    }
    

    /**
	 * Runs the Notifications query
	 * 
	 * @param start
	 * @param end
	 * @return 
	 */
	@Override
	public List<Notification> getNotifications(Date start, Date end)
	{
		s_Logger.debug(String.format("getNotifications(%s, %s)", start, end));
		
		// Get the notification datatype name we are using.
		String dataType = "";
		if (getDataSourceType().getDataCategory() == DataSourceCategory.NOTIFICATION)
		{
			dataType = getDataSourceType().getName();
		}

		List<Notification> notifications = new ArrayList<Notification>();
		try 
		{
			
			if (getNotificationsQuery() != null)
			{
				java.sql.Timestamp sqlStart = new java.sql.Timestamp(start.getTime());
				java.sql.Timestamp sqlEnd = new java.sql.Timestamp(end.getTime());
				
				PreparedStatement statement = m_Connection.prepareStatement(getNotificationsQuery());
				statement.setTimestamp(1, sqlStart, getServerTimeZoneCalendar());
				statement.setTimestamp(2, sqlEnd, getServerTimeZoneCalendar());
				
				ResultSet resultSet = statement.executeQuery();
				notifications.addAll(processNotificationQueryResults(dataType, resultSet,
																getServerTimeZoneCalendar()));
			}
		}
		catch (SQLException e) 
		{
			s_Logger.error("Exception in getNotifications(): " + e);
			return notifications;
		}
	
		return notifications;
	}
	
	/**
	 * Processes a SQL result set into a list of notifications.
	 * The result set should contain the following columns
	 * <ul>
	 * <li>Source</li>
	 * <li>Description</li>
	 * <li>DateTime</li>
	 * <li>Severity</li>
	 * </ul>
	 * And optionally:
	 * <ul>
	 * <li>Severity</li>
	 * </ul>
	 * 
	 * @param dataType - the name of the datatype.
	 * @param resultSet
	 * @param calendar - The calendar object used for creating the query. Ensures 
	 * 					 timezone changes are handled properly.
	 * @return
	 */
	protected List<Notification> processNotificationQueryResults(String dataType, ResultSet resultSet,
													Calendar calendar)
	{
		Set<String> reservedNames = new HashSet<String>();
		reservedNames.add("Source");
		reservedNames.add("Description");
		reservedNames.add("DateTime");
		reservedNames.add("Severity");
		reservedNames.add("Count");


		List<Notification> notifications = new ArrayList<Notification>();

		try
		{
			ResultSetMetaData metaData = resultSet.getMetaData();
			int columnCount = metaData.getColumnCount();
			while (resultSet.next())
			{
				String source = resultSet.getString("Source");
				Notification notification = new Notification(dataType, source);

				notification.setDescription(resultSet.getString("Description"));

				java.sql.Timestamp date = resultSet.getTimestamp("DateTime", calendar);
				notification.setTimeMs(date.getTime());

				notification.setSeverity(resultSet.getInt("Severity"));

				for (int i=1; i<=columnCount; i++)
				{
					String columnName = metaData.getColumnName(i);
					if ("Count".equals(columnName))
					{
						int count = resultSet.getInt("Count");
						notification.setCount(count);
					}
					else if (!reservedNames.contains(columnName))
					{
						Attribute attr = new Attribute(columnName, resultSet.getString(i));
						notification.addAttribute(attr);
					}
				}

				notifications.add(notification);
			}

		}
		catch (SQLException e)
		{
			s_Logger.error("Exception in processNotificationQueryResults(): " + e);
		}

		return notifications;
	}
	

	/**
	 * Run the AllTimeSeriesData query and return the time series points.
	 * 
	 * The query should contain the following columns at least:
	 * <ul>
	 * <li>Source</li>
	 * <li>DateTime</li>
	 * <li>Value</li>
	 * <li>Description</li>
	 * <li>Metric</li>
	 * </ul>
	 * 
	 * All other columns are added as Attributes.
	 * 
	 * @param minTime - Query start time
	 * @param maxTime - Query end time
	 * @param intervalSecs - This parameter is unused.
	 * @return
	 */
	@Override
	public Collection<TimeSeriesData> getAllDataPointsForTimeSpan(Date minTime,
													Date maxTime, int intervalSecs) 
	throws QueryTookTooLongException 
	{
		s_Logger.debug(String.format("getAllDataPointsForTimeSpan(%s, %s, %d) query=%s", 
								minTime, maxTime, intervalSecs, getAllTimeSeriesDataQuery()));

		List<TimeSeriesData> timeSeriesData = new ArrayList<TimeSeriesData>();
		try 
		{

			if (getAllTimeSeriesDataQuery() != null)
			{
				java.sql.Timestamp sqlStart = new java.sql.Timestamp(minTime.getTime());
				java.sql.Timestamp sqlEnd = new java.sql.Timestamp(maxTime.getTime());

				PreparedStatement statement = m_Connection.prepareStatement(getAllTimeSeriesDataQuery());

				statement.setTimestamp(1, sqlStart, getServerTimeZoneCalendar());
				statement.setTimestamp(2, sqlEnd, getServerTimeZoneCalendar());
				
				ResultSet resultSet = statement.executeQuery();
				timeSeriesData.addAll(processTimeSeriesQueryResults(resultSet, getServerTimeZoneCalendar()));
			}
		}
		catch (SQLException e) 
		{
			s_Logger.error("Exception in getAllDataPointsForTimeSpan(): " + e);
		}
	
		
		return timeSeriesData;
	}
	
	
	/**
	 * Process the result set from the TimeSeriesDataQuery
	 * for Time Series Data Points.
	 * 
	 * The result set should contain the following columns at least:
	 * <ul>
	 * <li>Source</li>
	 * <li>DateTime</li>
	 * <li>Value</li>
	 * <li>Description</li>
	 * <li>Metric</li>
	 * </ul>
	 * 
	 * All other columns are added as Attributes.
	 * 
	 * @param resultSet
	 * @param calendar - The calendar object used for creating the query. Ensures 
	 * 					 timezone changes are handled properly.
	 * @return
	 */
	protected Collection<TimeSeriesData> processTimeSeriesQueryResults(ResultSet resultSet,
													Calendar calendar)
	{
		Set<String> reservedNames = new HashSet<String>();
		reservedNames.add("Source");
		reservedNames.add("DateTime");
		reservedNames.add("Value");
		reservedNames.add("Description");
		reservedNames.add("Metric");

		// Get the time series datatype name we are using.
		String dataType = "";
		if (getDataSourceType().getDataCategory() == DataSourceCategory.TIME_SERIES)
		{
			dataType = getDataSourceType().getName();
		}
		
		Map<String, TimeSeriesData> dataMap = new HashMap<String, TimeSeriesData>();

		try
		{
			ResultSetMetaData metaData = resultSet.getMetaData();
			int columnCount = metaData.getColumnCount();
			while (resultSet.next())
			{
				StringBuilder key = new StringBuilder();
				
				String source = resultSet.getString("Source");
				java.sql.Timestamp date = resultSet.getTimestamp("DateTime", calendar);
				double value = resultSet.getDouble("Value");
				String description = resultSet.getString("Description");
				String metric = resultSet.getString("Metric");
				
				key.append(source).append(description).append(metric);

				List<Attribute> attrs = new ArrayList<Attribute>();
				for (int i=1; i<=columnCount; i++)
				{
					String columnName = metaData.getColumnName(i);
					if (!reservedNames.contains(columnName))
					{
						String colValue = resultSet.getString(i);
						attrs.add(new Attribute(columnName, colValue));
						
						key.append(colValue);
					}
				}

				TimeSeriesData data = dataMap.get(key.toString());
				if (data == null)
				{
					TimeSeriesConfig config = new TimeSeriesConfig(dataType, metric, source);
					config.setDescription(description);
					config.setAttributes(attrs); 

					data = new TimeSeriesData(config, new ArrayList<TimeSeriesDataPoint>());
					
					dataMap.put(key.toString(), data);
				}

				data.getDataPoints().add(new TimeSeriesDataPoint(date.getTime(), value));
			}				

		}
		catch (SQLException e)
		{
			s_Logger.error("Exception in processTimeSeriesQueryResults(): " + e);
		}
		
		for (TimeSeriesData data : dataMap.values())
		{
			Collections.sort(data.getDataPoints());
		}
		
		return dataMap.values();
	}
	

	@Override
	public void stop()
	{
		closeConnection();
	}

	/**
	 * The metric SQL query which will be used in calls to 
	 * <code>getMetrics(String)</code>.
	 * The query should return a table with one column which contains
	 * all the metric strings.
	 * 
	 * @return
	 */
	public String getMetricsQuery()
	{
		return m_MetricsQuery;
	}
	
	public void setMetricsQuery(String query)
	{
		m_MetricsQuery = query;
	}
	
	
	/**
	 * SQL query for all the Time Series data sources.
	 * The query should return a  table with a single column of strings.
	 * 
	 * @return
	 */
	public String getTimeSeriesSourcesQuery()
	{
		return m_TimeSeriesSourcesQuery;
	}
	
	public void setTimeSeriesSourcesQuery(String query)
	{
		m_TimeSeriesSourcesQuery = query;
	}
	
	
	/**
	 * SQL query for all the Notification data sources.
	 * The query should return a table with a single column of strings.
	 * 
	 * @return
	 */
	public String getNotificationSourcesQuery()
	{
		return m_NotificationSourcesQuery;
	}
	
	public void setNotificationSourcesQuery(String query)
	{
		m_NotificationSourcesQuery = query;
	}
	
	
	/**
	 * SQL query for the Notifications.
	 * The query takes 2 sql Date parameters which are represented by '?' in the query 
	 * string and returns a table with the following columns at a minimum:
	 * <ul>
	 * <li>Source</li>
	 * <li>Description</li>
	 * <li>DateTime</li>
	 * <li>Severity - a number between 0 - 7</li> 
	 * </ul>
	 * 
	 * Any other columns will be converted to attributes.
	 * 
	 * 
	 * @return
	 */
	public String getNotificationsQuery()
	{
		return m_NotificationsQuery;
	}
	
	public void setNotificationsQuery(String query)
	{
		m_NotificationsQuery = query;
	}
	
	
	/**
	 * SQL query for the Time Series data points.
	 * The query should return a table where each row represents a single point
	 * 
	 * The following columns must be returned by the query.
	 * <ul>
	 * <li>Source</li>
	 * <li>Description</li>
	 * <li>DateTime</li>
	 * <li>Value</li> 
	 * </ul>
	 * 
	 * Any other columns will be converted to attributes.
	 * 
	 * 
	 * @return
	 */
	public String getAllTimeSeriesDataQuery()
	{
		return m_AllTimeSeriesDataQuery;
	}
	
	public void setAllTimeSeriesDataQuery(String query)
	{
		m_AllTimeSeriesDataQuery = query;
	}
	
	/**
	 * SQL query for all the Attribute names.
	 * The query should return a table with a single column of strings.
	 */
	public String getAttributeNamesQuery()
	{
		return m_AttributeNamesQuery;
	}

	public void setAttributeNamesQuery(String query)
	{
		m_AttributeNamesQuery = query;
	}

	
	/**
	 * SQL query for all the Attribute Values.
	 * The query has 2 parameters: attributeName & source in that order 
	 * which will be replaced for the '?' marker.
	 * 
	 * The query should return a table with a single column of strings.
	 */
	public String getAttributeValuesQuery()
	{
		return m_AttributeValuesQuery;
	}
	
	public void setAttributeValuesQuery(String query)
	{
		m_AttributeValuesQuery = query;
	}
	
	
	/**
	 * Sets the default point interval that will be returned by
	 * <code>getUsualPointIntervalSecs(String, String )</code> if that 
	 * function is not overridden in a subclass.
	 */
	@Override
	public void setUsualPointIntervalSecs(int intervalSeconds)
	{
		m_DefaultPointIntervalSeconds = intervalSeconds;
	}
	
	@Override
	public int getUsualPointIntervalSecs() 
	{
		return m_DefaultPointIntervalSeconds;
	}
	
	
	/**
	 * Returns a Calendar with its time zone set to the servers time zone.
	 * This is required to correctly translate date/time objects over different 
	 * time zones.
	 * 
	 * @return
	 */
	public Calendar getServerTimeZoneCalendar()
	{
		return m_ServerTimeZoneCalendar;
	}

}
