/************************************************************
 *                                                          *
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2012     *
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

package com.prelert.proxy.plugin.jdbc.scom;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import com.prelert.data.Attribute;
import com.prelert.data.DataSource;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.MetricPath;
import com.prelert.data.MetricTreeNode;
import com.prelert.data.Notification;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.TimeSeriesData;
import com.prelert.data.TimeSeriesDataPoint;
import com.prelert.proxy.data.SourceConnectionConfig;
import com.prelert.proxy.plugin.ExternalPlugin;
import com.prelert.proxy.plugin.Plugin;
import com.prelert.proxy.plugin.jdbc.JdbcPlugin;
import com.prelert.server.ServerUtil;

/**
 * 
 */
public class ScomPlugin extends JdbcPlugin implements ExternalPlugin
{
	private static Logger s_Logger = Logger.getLogger(ScomPlugin.class);
	
	/**
	 * SCOM Notifications and Metrics default to these type names.
	 */
	public static final String DEFAULT_ALERTS_DATATYPE = "SCOM Alerts";
	public static final String DEFAULT_EVENTS_DATATYPE = "SCOM Events"; 
	public static final String DEFAULT_METRIC_DATATYPE = "SCOM Performance";
	
	public static final String OBJECTNAME_ATTRIBUTE = "ObjectName";
	public static final String INSTANCENAME_ATTRIBUTE = "Instance";
	public static final String FULLNAME_ATTRIBUTE = "FullName";
	public static final String RULE_INSTANCE_ID_ATTRIBUTE = "RuleInstanceId";
	public static final String ENTITY_ID_ATTRIBUTE = "EntityId";
	
	
	private String m_ScomAlertsQuery;
	private String m_ScomEventsQuery;
	
	private String m_TimeSeriesQuery;
	
	private String m_InstanceNameAttributeValuesQuery;
	private String m_ObjectNameAttributeValuesQuery;
	private String m_FullNameAttributeValuesQuery;
	private String m_RuleInstanceIDAttributeValuesQuery;
	private String m_EnitityIdAttributeValuesQuery;
	
	
	private DataSourceCategory m_DataSourceType;
	
	
	public ScomPlugin()
	{
		setUsualPointIntervalSecs(20);
	}
	
	/**
	 * Returns a full duplicate of this instance with the same
	 * properties set.
	 */
	@Override
	public Plugin duplicate() 
	{
		ScomPlugin clone = new ScomPlugin();
		clone.setName(getName());
		
		clone.setScomAlertsQuery(m_ScomAlertsQuery);
		clone.setScomEventsQuery(m_ScomEventsQuery);
		clone.setTimeSeriesQuery(m_TimeSeriesQuery);		
		clone.setInstanceNameAttributeValuesQuery(m_InstanceNameAttributeValuesQuery);
		clone.setObjectNameAttributeValuesQuery(m_ObjectNameAttributeValuesQuery);
		clone.setFullNameAttributeValuesQuery(m_FullNameAttributeValuesQuery);
		clone.setEntityIDNameAttributeValuesQuery(m_EnitityIdAttributeValuesQuery);
		clone.setRuleInstanceIDNameAttributeValuesQuery(m_RuleInstanceIDAttributeValuesQuery);
		
		clone.setMetricsQuery(getMetricsQuery());
		clone.setNotificationSourcesQuery(getNotificationSourcesQuery());
		clone.setTimeSeriesSourcesQuery(getTimeSeriesSourcesQuery());
		clone.setNotificationsQuery(getNotificationsQuery());
		clone.setAllTimeSeriesDataQuery(getAllTimeSeriesDataQuery());
		clone.setAttributeNamesQuery(getAttributeNamesQuery());
		clone.setAttributeValuesQuery(getAttributeValuesQuery());
		
		
		try
		{
			clone.configure(null, m_Properties);
		}
		catch (InvalidPluginPropertyException e)
		{
			s_Logger.error("Error duplicating ScomPlugin");
			s_Logger.error(e);
		}

		return clone;
	}

	/**
	 * Reads the properties from the properties parameter.
	 * 
	 * The following properties are compulsary and must be set:
	 * 'category' - Either time_series or notification
	 * 
	 * 	The following are optional:
	 * 'alertsDatatype' - If the plugin is collecting Alerts i.e. ScomAlertsQuery
	 *                    is defined then the specific data type can be set for them
	 *                    The default is DEFAULT_ALERTS_DATATYPE
	 * 'eventsDatatype' - If the plugin is collecting Events i.e. ScomEventsQuery
	 *                    is defined then the specific data type can be set for them
	 *                    The default is DEFAULT_EVENTS_DATATYPE
	 * 'metricsDatatype' - If the plugin is collecting metrics then the specific 
	 * 					   data type can be set for them. 
	 * 					   The default is DEFAULT_METRIC_DATATYPE
	 *         
	 * @param config Isn't used in this function.
	 * 
	 */
	@Override
	public boolean configure(SourceConnectionConfig config, Properties properties)
	throws InvalidPluginPropertyException 
	{
		String type = properties.getProperty("category");
		m_DataSourceType =  DataSourceCategory.getValue(type);
		
		
		if (m_DataSourceType == DataSourceCategory.NOTIFICATION)
		{
			if (getScomAlertsQuery() != null)
			{
				String alertsDatatype = properties.getProperty("alertsDatatype", DEFAULT_ALERTS_DATATYPE); 
				setDataSourceType(new DataSourceType(alertsDatatype, DataSourceCategory.NOTIFICATION));
			}

			if (getScomEventsQuery() != null)
			{
				String eventsDatatype = properties.getProperty("eventsDatatype", DEFAULT_EVENTS_DATATYPE); 
				setDataSourceType(new DataSourceType(eventsDatatype, DataSourceCategory.NOTIFICATION));
			}
		}
		else
		{
			if (getAllTimeSeriesDataQuery() != null)
			{
				String metricDatatype = properties.getProperty("metricsDatatype", DEFAULT_METRIC_DATATYPE); 
				setDataSourceType(new DataSourceType(metricDatatype, DataSourceCategory.TIME_SERIES));
			}
		}
		
		
		m_ConnectionUrl = loadAndValidateProperty("connectionUrl", properties);
		
		m_Properties = properties;
		
		connect();
		
		return true;
	}	
	
	
	@Override
	public List<String> getAttributeNames(String dataType) 
	{
		return Arrays.asList(OBJECTNAME_ATTRIBUTE, INSTANCENAME_ATTRIBUTE,
								FULLNAME_ATTRIBUTE, ENTITY_ID_ATTRIBUTE, 
								RULE_INSTANCE_ID_ATTRIBUTE);
	}

	
    
	/**
	 * Returns a list of all the Metrics for this plugin.
	 * The <code>dataType</code> parameter is ignored.
	 * 
	 * @param dataType - This parameter is not used.
	 * @result
	 */
	@Override
	public List<String> getMetrics(String dataType) 
	{
		s_Logger.debug(String.format("getMetrics(%s) with query=%s", 
										dataType, getMetricsQuery()));
		List<String> metrics = new ArrayList<String>();
		try 
		{
			Statement statement = m_Connection.createStatement();
			ResultSet resultSet = statement.executeQuery(getMetricsQuery());
			
			while (resultSet.next())
			{
				metrics.add(resultSet.getString(1));
			}
		}
		catch (SQLException e) 
		{
			s_Logger.error("Exception in getMetrics(): " + e);
			return Collections.emptyList();
		}

		return metrics;
	}

	/**
	 * Performs a particular query depending on the <code>attributeName</code>.
	 */
	@Override
	public List<String> getAttributeValues(String dataType,
			String attributeName, String source)
	{
		s_Logger.debug(String.format("getAttributeValues(%s, %s, %s)", 
								dataType, attributeName, source));

		String query = null;
		if (INSTANCENAME_ATTRIBUTE.equals(attributeName))
		{
			query = getInstanceNameAttributeValuesQuery();
		}
		else if (OBJECTNAME_ATTRIBUTE.equals(attributeName))
		{
			query = getObjectNameAttributeValuesQuery();
		}
		else if (FULLNAME_ATTRIBUTE.equals(attributeName))
		{
			query = getFullNameAttributeValuesQuery();
		}
		else if (ENTITY_ID_ATTRIBUTE.equals(attributeName))
		{
			query = getEntityInstanceIDNameAttributeValuesQuery();
		}
		else if (RULE_INSTANCE_ID_ATTRIBUTE.equals(attributeName))
		{
			query = getRuleInstanceIDNameAttributeValuesQuery();
		}
		
		if (query == null)
		{
			s_Logger.error("getAttributeValues(): Unknown attribute = " + attributeName);
			return Collections.emptyList();
		}
		
		List<String> values = new ArrayList<String>();
		try 
		{		
			PreparedStatement statement = m_Connection.prepareStatement(query);
			statement.setString(1, source);

			ResultSet resultSet = statement.executeQuery();

			while (resultSet.next())
			{
				values.add(resultSet.getString(1));
			}
		}
		catch (SQLException e) 
		{
			s_Logger.error("Exception in getAttributeValues(): " + e);
			return Collections.emptyList();
		}

		return values;
	}
	
	
	/**
	 * Parses <code>externalKey</code> returning a list of Attributes
	 * or an empty list if <code>externalKey</code> cannot be parsed.
	 * 
	 * @param externalKey
	 */
	@Override
	public List<Attribute> getAttributesForKey(String externalKey) 
	{
		try
		{
			return new ScomQueryParams(externalKey).getAttributes();
		}
		catch (ParseException pe)
		{
			s_Logger.error("getAttributesForKey() could not parse externalkey=" +
							externalKey);
			s_Logger.error(pe);
			
			return Collections.emptyList();
		}
	}
	

	/**
	 * Runs the Scom Alerts and Scom Events queries for the given dates
	 * and returns the results combined in a list.
	 * 
	 * @param start
	 * @param end
	 * @return List of Scom Alerts/Events 
	 */
	@Override
	public List<Notification> getNotifications(Date start, Date end)
	{
		s_Logger.debug(String.format("getNotifications(%s, %s)", start, end));

		List<Notification> notifications = new ArrayList<Notification>();
		try 
		{
			java.sql.Timestamp sqlStart = new java.sql.Timestamp(start.getTime());
			java.sql.Timestamp sqlEnd = new java.sql.Timestamp(end.getTime());
			
			// Events
			if (getScomEventsQuery() != null)
			{
				PreparedStatement statement = m_Connection.prepareStatement(getScomEventsQuery());
				statement.setTimestamp(1, sqlStart, getServerTimeZoneCalendar());
				statement.setTimestamp(2, sqlEnd, getServerTimeZoneCalendar());
				
				ResultSet resultSet = statement.executeQuery();
				notifications.addAll(processNotificationQueryResults(DEFAULT_EVENTS_DATATYPE, resultSet,
														getServerTimeZoneCalendar()));
			}
			
			// Alerts
			if (getScomAlertsQuery() != null)
			{
				PreparedStatement statement = m_Connection.prepareStatement(getScomAlertsQuery());
				statement.setTimestamp(1, sqlStart, getServerTimeZoneCalendar());
				statement.setTimestamp(2, sqlEnd, getServerTimeZoneCalendar());
				
				ResultSet resultSet = statement.executeQuery();
				notifications.addAll(processNotificationQueryResults(DEFAULT_ALERTS_DATATYPE, resultSet,
															getServerTimeZoneCalendar()));
			}
		}
		catch (SQLException e) 
		{
			s_Logger.error("Error in getNotifications() = " + e);
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
	 * <li>EventSeverity or AlertSeverity</li>
	 * </ul>
	 * And optionally:
	 * <ul>
	 * <li>Count</li>
	 * </ul>
	 * 
	 * @param dataType - the name of the datatype.
	 * @param resultSet
	 * @param calendar - The calendar object used for creating the query. Ensures 
	 * 					 timezone changes are handled properly. 
	 * @return
	 */
	@Override
	protected List<Notification> processNotificationQueryResults(String dataType, ResultSet resultSet, 
																Calendar calendar)
	{
		Set<String> reservedNames = new HashSet<String>();
		reservedNames.add("Source");
		reservedNames.add("Description");
		reservedNames.add("DateTime");
		reservedNames.add("EventSeverity");
		reservedNames.add("AlertSeverity");
		reservedNames.add("Count");
		reservedNames.add("RepeatCount");
		reservedNames.add("Path");

		List<Notification> notifications = new ArrayList<Notification>();
				
		
		try
		{		
			ResultSetMetaData metaData = resultSet.getMetaData();
			int columnCount = metaData.getColumnCount();
			
			// Figure out whether we are handling Alerts or Events.
			boolean useAlertSeverity = false;
			boolean useEventSeverity = false;
			boolean usePath = false;
			for (int i=1; i<=columnCount; ++i)
			{
				if (metaData.getColumnLabel(i).equals("AlertSeverity"))
				{
					useAlertSeverity = true;
				}
				else if (metaData.getColumnLabel(i).equals("EventSeverity"))
				{
					useEventSeverity = true;
				}
				else if (metaData.getColumnLabel(i).equals("Path"))
				{
					usePath = true;
				}
			}
			
			while (resultSet.next())
			{
				String source = resultSet.getString("Source");
				Notification notification = new Notification(dataType, source);
				notification.setCount(1); // default count to 1.

				notification.setDescription(resultSet.getString("Description"));

				java.sql.Timestamp date = resultSet.getTimestamp("DateTime", calendar);
				notification.setTimeMs(date.getTime());
				
				if (useEventSeverity)
				{
					String eventLevel = resultSet.getString("EventSeverity");				
					notification.setSeverity(mapEventLevelToSeverity(eventLevel));
				}
				
				if (useAlertSeverity)
				{
					int severity = resultSet.getInt("AlertSeverity");				
					notification.setSeverity(mapAlertSevertiyToPrelertSeverity(severity));
				}
				
				if (usePath)
				{
					String path = resultSet.getString("Path");	
					if (path != null)
					{
						String[] split = path.split(";");
						Attribute attr = new Attribute("Path", split[0]);
						notification.addAttribute(attr);
					}
				}
				
				for (int i=1; i<=columnCount; i++)
				{
					String columnLabel = metaData.getColumnLabel(i);
					if ("Count".equals(columnLabel))
					{
						notification.setCount(resultSet.getInt("Count"));
					}
					else if ("RepeatCount".equals(columnLabel))
					{
						/*
						 *  If repeat count = 0 then count is actually 1.
						 */
						int count = resultSet.getInt("RepeatCount") + 1;
						notification.setCount(count);
					}
					else if (!reservedNames.contains(columnLabel))
					{
						Attribute attr = new Attribute(columnLabel, resultSet.getString(i));
						notification.addAttribute(attr);
					}
				}

				notifications.add(notification);
			}

		}
		catch (SQLException e)
		{
			s_Logger.error("Error processNotificationQueryResults() = " + e);
		}

		return notifications;
	}
	
	/** Maps a Scom Event Level Title to a Prelert severity status.
	 * 
	 * @param eventLevel
	 * @return
	 */
	private int mapEventLevelToSeverity(String eventLevel)
	{
		if ("Error".equals(eventLevel))
		{
			return 6; // Critical
		}
		else if ("Warning".equals(eventLevel))
		{
			return 4; // Minor
		}
		else if ("Information".equals(eventLevel))
		{
			return 3; // Warning
		}
		else if ("Success Audit".equals(eventLevel))
		{
			return 1; // Clear
		}
		else if ("Failure Audit".equals(eventLevel))
		{
			return 5; // 
		}
		else
		{
			return 2; // Unknown
		}
	}
	
	/** Maps a Scom Alert's severity to a Prelert severity status.
	 * 
	 * @param alertSeverity
	 * @return
	 */
	private int mapAlertSevertiyToPrelertSeverity(int alertSeverity)
	{
		int result;
		
		switch (alertSeverity)
		{
		case 1: result = 3; // Warning
				break;
		case 2: result = 6; // Critical
				break;
		
		default:
			result = 2; // Unknown
		}
		
		return result;
	}
	
	/**
	 * Process the result set for Time Series Points.
	 * 
	 * The result set should contain the following columns:
	 * <ul>
	 * <li>Source</li>
	 * <li>DateTime</li>
	 * <li>Value</li>
	 * <li>Description</li>
	 * <li>CounterName</li>
	 * <li>InstanceName</li>
	 * <li>ObjectName</li>
	 * </ul>
	 * 
	 * @param resultSet
	 * @param calendar - The calendar object used for creating the query. Ensures 
	 * 					 timezone changes are handled properly.
	 * @return
	 */
	@Override
	protected Collection<TimeSeriesData> processTimeSeriesQueryResults(ResultSet resultSet, 
														Calendar calendar)
	{
		Set<String> reservedNames = new HashSet<String>();
		reservedNames.add("Source");
		reservedNames.add("DateTime");
		reservedNames.add("Value");
		reservedNames.add("Description");
		reservedNames.add("CounterName");
		reservedNames.add("InstanceName");
		reservedNames.add("ObjectName");
		reservedNames.add(ScomPlugin.ENTITY_ID_ATTRIBUTE);
		reservedNames.add(ScomPlugin.RULE_INSTANCE_ID_ATTRIBUTE);

		Map<String, TimeSeriesData> externalKeyToDataMap = new HashMap<String, TimeSeriesData>(); 

		try
		{
			ResultSetMetaData metaData = resultSet.getMetaData();
			int columnCount = metaData.getColumnCount();
			while (resultSet.next())
			{
				String source = resultSet.getString("Source");
				java.sql.Timestamp date = resultSet.getTimestamp("DateTime", calendar);
				double value = resultSet.getDouble("Value");
				
				String description = resultSet.getString("Description");
				String counterName = resultSet.getString("CounterName");
				String instanceName = resultSet.getString("InstanceName");
				String objectName = resultSet.getString("ObjectName");
				String fullName = resultSet.getString("FullName");
				Integer entityId = resultSet.getInt(ScomPlugin.ENTITY_ID_ATTRIBUTE);
				Integer ruleInstanceId = resultSet.getInt(ScomPlugin.RULE_INSTANCE_ID_ATTRIBUTE);
				
				String externalKey = new ScomQueryParams(fullName, objectName, 
										instanceName, counterName, 
										entityId, ruleInstanceId).toExternalKey();
				
				TimeSeriesData data = externalKeyToDataMap.get(externalKey);
				if (data == null)
				{
					TimeSeriesConfig config = new TimeSeriesConfig(DEFAULT_METRIC_DATATYPE, 
															counterName, source);
					// TODO Metric Path hierarchy- sort out the order
					config.setMetricPrefix("):"); 
					config.setSourcePosition(0);
					config.setSourcePrefix("//");
					
					config.setDescription(description);
					config.setExternalKey(externalKey);

					List<Attribute> attributes = new ArrayList<Attribute>();
					config.setAttributes(attributes); 
					
					attributes.add(new Attribute(ScomPlugin.OBJECTNAME_ATTRIBUTE, objectName,
									"/", 1));
					attributes.add(new Attribute(ScomPlugin.INSTANCENAME_ATTRIBUTE, instanceName,
									"(", 2));					
					
					attributes.add(new Attribute(ScomPlugin.ENTITY_ID_ATTRIBUTE, entityId.toString()));
					attributes.add(new Attribute(ScomPlugin.RULE_INSTANCE_ID_ATTRIBUTE, ruleInstanceId.toString()));
					
					for (int i=1; i<=columnCount; i++)
					{
						String columnLabel = metaData.getColumnLabel(i);
						if (!reservedNames.contains(columnLabel))
						{
							config.getAttributes().add(new
									Attribute(columnLabel, resultSet.getString(i)));
						}
					}

					data = new TimeSeriesData(config, new ArrayList<TimeSeriesDataPoint>());
					externalKeyToDataMap.put(externalKey, data);
				}
				
				data.getDataPoints().add(new TimeSeriesDataPoint(date.getTime(), value));
			}

		}
		catch (SQLException e)
		{
			s_Logger.error("Error in processTimeSeriesQueryResults() = " + e);
		}
		
		for (TimeSeriesData tsData : externalKeyToDataMap.values())
		{
			Collections.sort(tsData.getDataPoints());
		}

		return externalKeyToDataMap.values();
	}

	
	/**
	 * Parses the query parameters from the external key then runs 
	 * the query. Returns points for exactly 1 time series.
	 * 
	 * @param externalKey
	 * @param minTime
	 * @param maxTime
	 * @param intervalSecs - Parameter is unused.
	 */
	@Override
	public List<TimeSeriesDataPoint> getDataPointsForTimeSpan(String externalKey, 
									Date minTime, Date maxTime, int intervalSecs) 
	{
		s_Logger.debug(String.format("getDataPointsForTimeSpan(%s, %s, %s) query=%s", 
									externalKey, minTime, maxTime, getTimeSeriesQuery()));

		ScomQueryParams queryParams;
		try
		{
			queryParams = new ScomQueryParams(externalKey);
		}
		catch (ParseException pe)
		{
			s_Logger.error("getDataPointsForTimeSpan() cannot parse external key = " + externalKey);
			s_Logger.error(pe);
			return Collections.emptyList();
		}
		
		return getDataPoints(queryParams, minTime, maxTime);
	}

	
	/**
	 * Generates the SQL query parameters from the <code>attributes</code>
	 * and the <code>source/metric</code> parameters.
	 * 
	 * @param dataType
	 * @param metric
	 * @param source
	 * @param attributes
	 * @param minTime
	 * @param maxTime
	 * @param intervalSecs - Parameter is unused.
	 */
	@Override
	public List<TimeSeriesDataPoint> getDataPointsForTimeSpan(String dataType,
										String metric, String source, List<Attribute> attributes,
										Date minTime, Date maxTime, int intervalSecs) 
	{
		s_Logger.debug(String.format("getDataPointsForTimeSpan(%s, %s, %s, %s, %s, %s) query=%s", 
										dataType, metric, source, attributes,
										minTime, maxTime, getTimeSeriesQuery()));

		ScomQueryParams queryParams;
		try
		{
			queryParams = new ScomQueryParams(metric, attributes);
		}
		catch (IllegalArgumentException e)
		{
			s_Logger.error("Cannot create ScomQueryAttributes due to invalid Attributes.");
			s_Logger.error(e);
			return Collections.emptyList();
		}

		return getDataPoints(queryParams, minTime, maxTime);
	}
	
	
	/**
	 * Runs the TimeSeriesQuery and returns points for exactly 1 time series.
	 * 
	 * @param queryParams
	 * @param minTime
	 * @param maxTime
	 */
	private List<TimeSeriesDataPoint> getDataPoints(ScomQueryParams queryParams, 
													Date minTime, Date maxTime) 
	{
		List<TimeSeriesDataPoint> timeSeriesPoints = new ArrayList<TimeSeriesDataPoint>();
		try 
		{
			java.sql.Timestamp sqlStart = new java.sql.Timestamp(minTime.getTime());
			java.sql.Timestamp sqlEnd = new java.sql.Timestamp(maxTime.getTime());  			

			if (getTimeSeriesQuery() != null)
			{
				PreparedStatement statement = m_Connection.prepareStatement(getTimeSeriesQuery());
				statement.setInt(1, queryParams.getEntityId());
				statement.setInt(2, queryParams.getRuleInstanceId());
				statement.setTimestamp(3, sqlStart, getServerTimeZoneCalendar());
				statement.setTimestamp(4, sqlEnd, getServerTimeZoneCalendar());
				
				ResultSet resultSet = statement.executeQuery();

				while (resultSet.next())
				{
					java.sql.Timestamp date = resultSet.getTimestamp(1, getServerTimeZoneCalendar());
					double value = resultSet.getDouble(2);
					timeSeriesPoints.add(new TimeSeriesDataPoint(date.getTime(), value));
				}
			}
		}
		catch (SQLException e) 
		{
			s_Logger.error("Exception in getDataPoints() = " + e);
		}

		Collections.sort(timeSeriesPoints);

		return timeSeriesPoints;
	}
	
	

	
	@Override
	public int getDataTypeItemCount(DataSourceType type) 
	{
		return -1;
	}
	
	@Override
	public int getDataSourceItemCount(DataSource source)
	{
		return -1;
	}
	
	/**
	 * Returns a list of DataSources depending on the <code>type</code> parameter.
	 * If the <code>DataSourceCategory</code> is <code>NOTIFICATION</code> then either
	 * Alerts or Event sources are returned depending on the type name.	 *  
	 * 
	 * @param type
     * @return
	 */
	@Override
	public List<DataSource> getDataSources(DataSourceType type)
	{
		s_Logger.debug(String.format("getDataSources(%s)", type));
		
		List<String> sourceNames = new ArrayList<String>();
		
		try 
		{
			String query = null;
			if (type.getDataCategory() == DataSourceCategory.NOTIFICATION)
			{
				throw new IllegalArgumentException(
						"SCOM plugin does not support listing Notification sources. " +
						"Invalid state Notification sources should never be requested.");
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
			s_Logger.error("Exception in getDataSources() = " + e);
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
	

	@Override
	public List<MetricTreeNode> getDataSourceTreeNextLevel(String datatype,
											String previousPath, String currentValue, 
											int opaqueNum, String opaqueStr) 
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<MetricTreeNode> getDataSourceTreePreviousLevel(String datatype,
													String previousPath, 
													int opaqueNum, String opaqueStr) 
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public List<MetricTreeNode> getDataSourceTreeCurrentLevel(String datatype,
													String previousPath, 
													int opaqueNum, String opaqueStr) 
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * Queries the points for each external key and returns the peak
	 * value for each.
	 * 
	 * @param queryParams
	 * @param minTime
	 * @param maxTime
	 * @param intervalSecs
	 * @return
	 */
	@Override
	public List<ExternalKeyPeakValuePair> getPeakValueForTimeSpan(
									List<String> externalKeys, Date minTime, 
									Date maxTime, int intervalSecs) 
	{
		List<ExternalKeyPeakValuePair> peakValues = new ArrayList<ExternalKeyPeakValuePair>();
		
		for (String externalKey : externalKeys)
		{
			List<TimeSeriesDataPoint> points = getDataPointsForTimeSpan(externalKey,
														minTime, maxTime, intervalSecs);
			
			double peak = 0.0;
			for (TimeSeriesDataPoint point : points)
			{
				if (point.getValue() > peak)
				{
					peak = point.getValue();
				}
			}
			
			peakValues.add(new ExternalKeyPeakValuePair(externalKey, peak));
		}
		
		return peakValues;
	}


	/**
	 * The actual SQL query string for pulling Alerts from the scom 
	 * data warehouse. The query parameters are a start and end date
	 * which will be substitute the '?' token
	 * 
	 * @return
	 */
	public String getScomAlertsQuery()
	{
		return m_ScomAlertsQuery;
	}
	
	public void setScomAlertsQuery(String query)
	{
		m_ScomAlertsQuery = query;
	}

	
	/**
	 * The actual SQL query string for pulling Events from the scom 
	 * data warehouse. The query parameters are a start and end date
	 * which will be substitute the '?' token
	 * 
	 * @return
	 */
	public String getScomEventsQuery()
	{
		return m_ScomEventsQuery;
	}
	
	public void setScomEventsQuery(String query)
	{
		m_ScomEventsQuery = query;
	}

	/**
	 * SQL query for the data points of an individual time series.
	 * 
	 * The query should return a table where each row represents a single point 
	 * and must contain the following columns in this order:
	 * <ol>
	 * <li>DateTime</li>
	 * <li>Value</li> 
	 * </ol>
	 * Other columns will be ignored.
	 * 
	 * The query takes 6 parameters in this order:
	 * <ol>
	 * <li>ManagedEntity.Path</li>
	 * <li>PerformanceRule.ObjectName</li>
	 * <li>PerformanceRuleInstance.InstanceName</li>
	 * <li>PerformanceRule.ConterInstance</li>
	 * <li>Start DateTime</li>
	 * <li>Finish DateTime</li>
	 * </ol>
	 * 
	 * @return
	 */
	public String getTimeSeriesQuery()
	{
		return m_TimeSeriesQuery;
	}
	
	public void setTimeSeriesQuery(String query)
	{
		m_TimeSeriesQuery = query;
	}
	
	
	public String getFullNameAttributeValuesQuery()
	{
		return m_FullNameAttributeValuesQuery;
	}
	
	public void setFullNameAttributeValuesQuery(String query)
	{
		m_FullNameAttributeValuesQuery = query;
	}
	
	public String getObjectNameAttributeValuesQuery()
	{
		return m_ObjectNameAttributeValuesQuery;
	}
	
	public void setObjectNameAttributeValuesQuery(String query)
	{
		m_ObjectNameAttributeValuesQuery = query;
	}
	
	public String getInstanceNameAttributeValuesQuery()
	{
		return m_InstanceNameAttributeValuesQuery;
	}
	
	public void setInstanceNameAttributeValuesQuery(String query)
	{
		m_InstanceNameAttributeValuesQuery = query;
	}
	
	public String getRuleInstanceIDNameAttributeValuesQuery()
	{
		return m_RuleInstanceIDAttributeValuesQuery;
	}
	
	public void setRuleInstanceIDNameAttributeValuesQuery(String query)
	{
		m_RuleInstanceIDAttributeValuesQuery = query;
	}
	
	public String getEntityInstanceIDNameAttributeValuesQuery()
	{
		return m_EnitityIdAttributeValuesQuery;
	}
	
	public void setEntityIDNameAttributeValuesQuery(String query)
	{
		m_EnitityIdAttributeValuesQuery = query;
	}

	@Override
	public boolean isAggregationSupported(String dataType) {
		// TODO Auto-generated method stub
		return false;
	}

	
	/**
	 * Returns the current time. Parameters are ignored.
	 */
	@Override
	public Date getLatestTime(String dataType, String source) 
	{
		return new Date();
	}

	
	// TODO implement this.
	@Override
	public MetricPath metricPathFromExternalKey(String datatype,
									String externalKey) 
	{
		throw new UnsupportedOperationException("metricPathFromExternalKey");
	}

	// TODO implement this.
	@Override
	public List<MetricTreeNode> metricPathNodesFromExternalKeys(
			List<MetricTreeNode> externalKeyNodes) 
	{
		throw new UnsupportedOperationException("metricPathNodesFromExternalKeys");
	}
	
	/**
	 * Default values as this plugin doesn't currently 
	 * support metric paths properly. 
	 */
	@Override
	public String getMetricPathDelimiter()
	{
		return ServerUtil.METRIC_PATH_DELIMITER;
	}
	
	@Override
	public String getMetricPathMetricPrefix()
	{
		return ServerUtil.METRIC_PATH_METRIC_PREFIX;
	}
	
	@Override
	public String getMetricPathSourcePrefix()
	{
		return ServerUtil.METRIC_PATH_SOURCE_PREFIX;
	}

}
