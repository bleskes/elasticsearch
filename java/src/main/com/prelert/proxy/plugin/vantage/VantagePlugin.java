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

package com.prelert.proxy.plugin.vantage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import adlex.delta.ws.DMIData;
import adlex.delta.ws.DMIData2;

import com.prelert.data.Attribute;
import com.prelert.data.ConnectionStatus;
import com.prelert.data.DataSource;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.MetricPath;
import com.prelert.data.MetricTreeNode;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.TimeSeriesData;
import com.prelert.data.TimeSeriesDataPoint;
import com.prelert.proxy.data.SourceConnectionConfig;
import com.prelert.proxy.plugin.ExternalPlugin;
import com.prelert.proxy.plugin.Plugin;
import com.prelert.server.ServerUtil;


/**
 * Plugin for the Compuware Vantage family of applications.
 *
 * @author David Roberts
 */
public class VantagePlugin extends Plugin implements ExternalPlugin
{
	static Logger s_Logger = Logger.getLogger(VantagePlugin.class);

	private static final String DEFAULT_DATA_TYPE = "vantage";

	/**
	 * Vantage resolution strings, as might be returned by
	 * <code>getResolutions()</code>.  This array must have the same number of
	 * elements as <code>DEFAULT_RESOLUTION_MS</code>.
	 */
	private static final String[] RESOLUTION_TEXT = { "r", "1", "6", "d", "w" };

	/**
	 * The number of milliseconds corresponding to the time resolutions above.
	 * This array must have the same number of elements as
	 * <code>RESOLUTION_TEXT</code>.  The first value is a default and may be
	 * changed by the config within the working copy of this
	 * <code>m_ResolutionMS</code>.
	 */
	private static final long[] DEFAULT_RESOLUTION_MS = { 300000l,
															3600000l,
															21600000l,
															86400000l,
															604800000l };

	/**
	 * Command to tell Vantage to sort results in descending order.
	 */
	private static final String DESCENDING = "DESC";

	/**
	 * Magic number to tell Vantage to return all results.
	 */
	private static final int ALL = 0;

	/**
	 * Default timeout value for web service calls is 1 minute = 60000ms.
	 */
	private static final long DEFAULT_TIMEOUT = 60000l;

	/**
	 * Connection parameters.
	 */
	private int m_Port;
	private String m_Host;
	private String m_Username;
	private String m_Password;

	/**
	 * Which part of the VAS are we querying?
	 */
	private String m_Application;
	private String m_View;
	private String m_DataSource;


	/**
	 * The number of milliseconds corresponding to each time resolution.  This
	 * array must have the same number of elements as
	 * <code>RESOLUTION_TEXT</code>.  Most values are set equal to those in
	 * <code>DEFAULT_RESOLUTION_MS</code>, but the first value may be configured
	 * from the properties file.
	 */
	private long[] m_ResolutionMS;

	/**
	 * What can/should we get?
	 */
	private HashSet<String> m_AvailableMetrics;
	private TreeSet<String> m_ConfiguredMetrics;
	private HashSet<String> m_AvailableDimensions;
	private String m_TimeDimension;
	private String m_SourceDimension;
	private TreeSet<String> m_AttributeDimensions;

	/**
	 * The client we use to talk to Vantage via web services.  The DMIService
	 * class was generated from WSDL using Apache Axis.
	 */
	private VantageConnection m_Client;

	/**
	 * Web service timeout in milliseconds.
	 */
	private long m_TimeOut;

	/**
	 * When did Vantage last give us data?  Since its processing is a little
	 * behind real-time, we'll ask for data from this time rather than the
	 * minimum time we're given when asking for new points.
	 */
	private long m_LastDataTimeMS;

	/**
	 * Local copy of the Properties passed to loadProperties(Properties).
	 * Kept for the duplicate() function.
	 */
	private Properties m_Properties;


	/**
	 * Construct a plugin - this isn't suitable for use until the properties
	 * have been loaded.
	 */
	public VantagePlugin()
	{
		super();

		m_AvailableMetrics = new HashSet<String>();
		m_ConfiguredMetrics = new TreeSet<String>();
		m_AvailableDimensions = new HashSet<String>();
		m_AttributeDimensions = new TreeSet<String>();

		m_TimeOut = DEFAULT_TIMEOUT;

		m_ResolutionMS = new long[DEFAULT_RESOLUTION_MS.length];
		System.arraycopy(DEFAULT_RESOLUTION_MS, 0,
						m_ResolutionMS, 0,
						DEFAULT_RESOLUTION_MS.length);
	}

 
	/**
	 * Load the settings required by this plugin from its properties file.
	 * Compulsory settings are:
	 *  1) host
	 *  2) port
	 *  3) username
	 *  4) password
	 *  5) application
	 *  6) view
	 *  7) timeDimension
	 *  8) sourceDimension
	 *  9) attributeDimensions
	 * 10) metrics
	 * Optional settings are:
	 *  1) dataType
	 *  2) timeout
	 *  3) dataSource
	 *  4) retentionDays
	 *  5) dataIntervalSeconds
	 *  
	 *  @param config isn't used.
	 */
	@Override
	public boolean configure(SourceConnectionConfig config, Properties props)
	throws InvalidPluginPropertyException 
	{
		m_Properties = props; // keep copy for duplicates

		String dataType = props.getProperty("dataType", DEFAULT_DATA_TYPE);
		setDataSourceType(new DataSourceType(dataType, DataSourceCategory.TIME_SERIES));

		m_Host = getCompulsoryProperty(props, "host");
		String portStr = getCompulsoryProperty(props, "port");
		try
		{
			m_Port = Integer.parseInt(portStr);
		}
		catch (NumberFormatException e)
		{
			throw new InvalidPluginPropertyException("Vantage port '" +
											portStr +
											"' cannot be parsed to an integer");
		}

		m_Username = getCompulsoryProperty(props, "username");
		m_Password = getCompulsoryProperty(props, "password");

		m_Application = getCompulsoryProperty(props, "application");
		m_View = getCompulsoryProperty(props, "view");

		// m_DataSource is allowed to be null, and we translate the string
		// "null" to a null reference
		m_DataSource = props.getProperty("dataSource");
		if (m_DataSource != null)
		{
			if (m_DataSource.equals("null"))
			{
				m_DataSource = null;
			}
		}

		m_TimeDimension = getCompulsoryProperty(props, "timeDimension");
		m_SourceDimension = getCompulsoryProperty(props, "sourceDimension");
		String dimensionStr = getCompulsoryProperty(props, "attributeDimensions");
		for (String attributeDimension : dimensionStr.split(","))
		{
			m_AttributeDimensions.add(attributeDimension.trim());
		}

		String metricStr = getCompulsoryProperty(props, "metrics");
		for (String metricID : metricStr.split(","))
		{
			m_ConfiguredMetrics.add(metricID.trim());
		}

		// Timeout is optional
		String timeOutStr = props.getProperty("timeout",
												Long.toString(DEFAULT_TIMEOUT));
		try
		{
			m_TimeOut = Long.parseLong(timeOutStr);
		}
		catch (NumberFormatException e)
		{
			throw new InvalidPluginPropertyException("Vantage web service timeout '" +
							timeOutStr + "' cannot be parsed to a long");
		}

		// Data interval is optional
		String dataIntervalStr = props.getProperty("dataIntervalSeconds");
		if (dataIntervalStr != null)
		{
			try
			{
				m_ResolutionMS[0] = 1000l * Long.parseLong(dataIntervalStr);
			}
			catch (NumberFormatException e)
			{
				throw new InvalidPluginPropertyException("Vantage data interval '" +
							dataIntervalStr + "' cannot be parsed to a long");
			}
		}

		try
		{
			m_Client = new VantageConnection(m_Host, m_Port,
											m_Username, m_Password);
		}
		catch (Exception e)
		{
			throw new InvalidPluginPropertyException("Vantage plugin configuration failed to create a connection to Vantage");
		}

		// These throw InvalidPluginPropertyException if they detect an error
		validateConnection();
		validateDimensions();
		validateMetrics();
		
		return true;
	}

	
	/**
	 * Warning this a
	 */
	@Override
	public ConnectionStatus testConnection(SourceConnectionConfig config, Properties properties)
	{
		try 
		{
			configure(config, properties);
			validateConnection();
		}
		catch (InvalidPluginPropertyException e) 
		{
			s_Logger.error("testConnection error: ", e);
			return new ConnectionStatus(ConnectionStatus.Status.CONNECTION_FAILED);
		}
		
		return new ConnectionStatus(ConnectionStatus.Status.CONNECTION_OK);
	}

	

	/**
	 * Validate that the configured Vantage application, view and data source
	 * exist.
	 */
	private void validateConnection() throws InvalidPluginPropertyException
	{
		try
		{
			boolean foundApplication = false;
			String[][] applications = m_Client.getApplications();
			for (String[] application : applications)
			{
				if (m_Application.equals(application[0]))
				{
					foundApplication = true;
					break;
				}
			}

			if (!foundApplication)
			{
				throw new InvalidPluginPropertyException("Vantage application '" +
											m_Application + "' not available");
			}

			boolean foundView = false;
			String[][] views = m_Client.getDataViews(m_Application);
			for (String[] view : views)
			{
				if (m_View.equals(view[0]))
				{
					foundView = true;
					break;
				}
			}

			if (!foundView)
			{
				throw new InvalidPluginPropertyException("Vantage view '" +
													m_View + "' not available");
			}

			// Data source is allowed to be null, in which case we shouldn't
			// validate it
			if (m_DataSource != null)
			{
				boolean foundDataSource = false;
				String[][] dataSources = m_Client.getDataSources(m_Application,
																m_View);
				for (String[] dataSource : dataSources)
				{
					if (m_DataSource.equals(dataSource[0]))
					{
						foundDataSource = true;
						break;
					}
				}

				if (!foundDataSource)
				{
					throw new InvalidPluginPropertyException("Vantage data source '" +
												m_DataSource + "' not available");
				}
			}
		}
		catch (Exception e)
		{
			s_Logger.error("Error whilst validating Vantage connection", e);
			throw new InvalidPluginPropertyException("Cannot validate Vantage connection");
		}
	}


	/**
	 * Validate the configured dimensions.  The time and source dimensions must
	 * exist, and an exception will be thrown if they don't.  Additionally, the
	 * time and source dimensions should not be included in the attribute
	 * dimensions.  Attribute dimensions that don't exist are removed.
	 */
	private void validateDimensions() throws InvalidPluginPropertyException
	{
		try
		{
			String resolution = RESOLUTION_TEXT[0];

			String[][] dimensions = m_Client.getDimensions(m_Application,
															m_View, resolution);
			for (int count = 0; count < dimensions.length; ++count)
			{
				// Add the internal Vantage metric IDs to a hash
				m_AvailableDimensions.add(dimensions[count][0]);
			}
		}
		catch (Exception e)
		{
			s_Logger.error("Unable to obtain available dimension IDs", e);
		}

		// Check time dimension
		if (!m_AvailableDimensions.contains(m_TimeDimension))
		{
			throw new InvalidPluginPropertyException("Time dimension '" +
						m_TimeDimension + "' is not available from Vantage");
		}
		m_AttributeDimensions.remove(m_TimeDimension);

		// Check source dimension
		if (!m_AvailableDimensions.contains(m_SourceDimension))
		{
			throw new InvalidPluginPropertyException("Source dimension '" +
						m_SourceDimension + "' is not available from Vantage");
		}
		m_AttributeDimensions.remove(m_SourceDimension);

		if (m_TimeDimension.equals(m_SourceDimension))
		{
			throw new InvalidPluginPropertyException("Vantage plugin time dimension and source dimension cannot be the same: '" +
													m_TimeDimension + "'");
		}

		// Remove configured attribute dimensions that aren't available
		m_AttributeDimensions.retainAll(m_AvailableDimensions);
	}


	/**
	 * Validate the configured metrics, removing any that don't exist, and
	 * throwing an exception if none are left.
	 */
	private void validateMetrics() throws InvalidPluginPropertyException
	{
		try
		{
			String resolution = RESOLUTION_TEXT[0];

			String[][] metrics = m_Client.getMetrics(m_Application, m_View,
														resolution);
			for (int count = 0; count < metrics.length; ++count)
			{
				// Add the internal Vantage metric IDs to a hash
				m_AvailableMetrics.add(metrics[count][0]);
			}
		}
		catch (Exception e)
		{
			s_Logger.error("Unable to obtain available metric IDs", e);
		}

		// Remove configured metrics that aren't available
		m_ConfiguredMetrics.retainAll(m_AvailableMetrics);

		if (m_ConfiguredMetrics.isEmpty())
		{
			throw new InvalidPluginPropertyException("No configured metrics are supported");
		}
	}


	/**
	 * Get a property from a properties object, throwing an exception if it's
	 * not present.
	 * @param props The properties to search.
	 * @param propName The name of the property to search for.
	 * @return The property value.
	 */
	private String getCompulsoryProperty(Properties props, String propName)
										throws InvalidPluginPropertyException
	{
		String propValue = props.getProperty(propName);
		if (propValue == null)
		{
			throw new InvalidPluginPropertyException("'" + propName +
								"' property not specified for Vantage plugin");
		}

		return propValue;
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
	@Override
	public List<DataSource> getDataSources(DataSourceType type)
	{
		List<DataSource> sources = new ArrayList<DataSource>();

		if (getDataSourceType().equals(type))
		{
			try
			{
				String[] dimensionValues = m_Client.getDimensionValues(m_Application,
							m_View, m_SourceDimension, m_DataSource, ALL, "*");

				for (String dimensionValue : dimensionValues)
				{
					// Ignore blank source names
					if (dimensionValue == null ||
						dimensionValue.trim().length() == 0)
					{
						continue;
					}

					DataSource source = new DataSource();
					source.setDataSourceType(type);
					source.setSource(dimensionValue);
					source.setExternalPlugin(getName());

					sources.add(source);
				}
			}
			catch (Exception e)
			{
				s_Logger.error(e);
			}

			s_Logger.debug("Vantage plugin knows " + sources.size() +
							" sources for type " + type);
		}
		else
		{
			s_Logger.error("Vantage plugin for types " + getDataSourceType() +
							" has been asked for sources for type " + type);
		}

		return sources;
	}


	/**
	 * Retrieve the possible attribute names.  These are limited to the
	 * intersection of what's in our properties file and what's available from
	 * Vantage.
	 * @param dataType This parameter is not used as this plugin only supports
	 *                 a single data type.
	 * @return The list of possible attribute names.
	 */
	@Override
	public List<String> getAttributeNames(String dataType)
	{
		List<String> names = new ArrayList<String>();

		try
		{
			String resolution = RESOLUTION_TEXT[0];

			String[][] dimensions = m_Client.getDimensions(m_Application,
															m_View, resolution);

			for (int count = 0; count < dimensions.length; ++count)
			{
				if (m_AttributeDimensions.contains(dimensions[count][0]))
				{
					// Each dimension has two names, one human readable and one
					// internal Vantage ID - we need to combine both
					names.add(createFull(dimensions[count][1],
										dimensions[count][0]));
				}
			}
		}
		catch (Exception e)
		{
			s_Logger.error(e);
		}

		return names;
	}


	/**
	 * Retrieve the different values that exist for a given attribute name.
	 * Vantage can cope with queries aggregated over attributes, so the
	 * returned list begins with a null to indicate to the GUI that it should
	 * include the option of "all" at the top of the dropdown list.
	 * @param dataType This parameter is not used as this plugin only supports
	 *                 a single data type.
	 * @param attributeName The name of the attribute to retrieve values for.
	 * @param source Not used, as Vantage doesn't support this.
	 * @return A list of values that exist for the given attribute name.
	 */
	@Override
	public List<String> getAttributeValues(String dataType,
											String attributeName,
											String source)
	{
		List<String> values = new ArrayList<String>();

		// Always return NULL as the first result, indicating that "all" is an
		// option
		values.add(null);

		try
		{
			// Expected attribute format is:
			// human readable [internal ID]
			String dimensionID = extractID(attributeName);
			if (dimensionID == null)
			{
				throw new IllegalArgumentException("Attribute name " +
									attributeName + " not in required format");
			}

			String[] dimensionValues = m_Client.getDimensionValues(m_Application,
								m_View, dimensionID, m_DataSource, ALL, "*");

			for (String dimensionValue : dimensionValues)
			{
				if (dimensionValue != null)
				{
					values.add(dimensionValue);
				}
			}
		}
		catch (Exception e)
		{
			s_Logger.error(e);
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
		List<Attribute> attributes;

		try
		{
			VantageExternalKey extractedKey = new VantageExternalKey(externalKey);

			attributes = extractedKey.getAttributes();
		}
		catch (Exception e)
		{
			s_Logger.error(e);

			attributes = new ArrayList<Attribute>();
		}

		return attributes;
	}


	/**
	 * Get the latest time for which data is available from Vantage.
	 * @param dataType This parameter is not used as this plugin only supports
	 *                 a single data type.
	 * @param source Not used, as Vantage doesn't support this.
	 * @return The latest time for which data is available.
	 */
	@Override
	public Date getLatestTime(String dataType, String source)
	{
		Date date = new Date();

		try
		{
			String resolution = RESOLUTION_TEXT[0];
			long lastSampleTime = m_Client.getLastSampleTime(m_Application,
															m_View, resolution);

			date.setTime(lastSampleTime);
		}
		catch (Exception e)
		{
			s_Logger.error(e);
		}

		return date;
	}


	/**
	 * Retrieve the available metrics.  These are limited to the intersection
	 * of what's in our properties file and what's available from Vantage.
	 * @param dataType This parameter is not used as this plugin only supports
	 *                 a single data type.
	 * @return The list of metrics that are both configured and available.
	 */
	@Override
	public List<String> getMetrics(String dataType)
	{
		List<String> retMetrics = new ArrayList<String>();

		try
		{
			String resolution = RESOLUTION_TEXT[0];

			String[][] metrics = m_Client.getMetrics(m_Application, m_View,
													resolution);

			for (int count = 0; count < metrics.length; ++count)
			{
				if (m_ConfiguredMetrics.contains(metrics[count][0]))
				{
					// Each metric has two names, one human readable and one
					// internal Vantage ID - we need to combine both
					retMetrics.add(createFull(metrics[count][1],
												metrics[count][0]));
				}
			}
		}
		catch (Exception e)
		{
			s_Logger.error(e);
		}

		return retMetrics;
	}


	/**
	 * Retrieve points for a given time span.  The interval between points
	 * will be no smaller than the requested interval, but may be bigger if
	 * Vantage doesn't support the exact interval requested.
	 * @param externalKey The external key that corresponds to the time series
	 *                    for which points are required.
	 * @param minTime The earliest time for which to return a point.
	 * @param maxTime The latest time for which to return a point.
	 * @param intervalSecs The ideal interval between the points returned.  The
	 *                     actual interval may be bigger than this, depending on
	 *                     what Vantage supports.
	 * @return A list of time series data points.
	 */
	@Override
	public List<TimeSeriesDataPoint> getDataPointsForTimeSpan(String externalKey,
													Date minTime, Date maxTime,
													int intervalSecs)
	{
		try
		{
			VantageExternalKey extractedKey = new VantageExternalKey(externalKey);

			return getDataPointsForTimeSpan("", extractedKey.getMetric(),
											extractedKey.getSource(),
											extractedKey.getAttributes(),
											minTime,
											maxTime,
											intervalSecs);
		}
		catch (Exception e)
		{
			s_Logger.error(e);
		}

		return new ArrayList<TimeSeriesDataPoint>();
	}


	/**
	 * Retrieve points for a given time span.  The interval between points
	 * will be no smaller than the requested interval, but may be bigger if
	 * Vantage doesn't support the exact interval requested.
	 * @param dataType This parameter is not used as this plugin only supports
	 *                 a single data type.
	 * @param metric The metric for which points are required.  This must not be
	 *               null.
	 * @param source The source for which points are required, or null to
	 *               aggregate over all sources.
	 * @param attributes A list of attributes to use to restrict the points
	 *                   returned.
	 * @param minTime The earliest time for which to return a point.
	 * @param maxTime The latest time for which to return a point.
	 * @param intervalSecs The ideal interval between the points returned.  The
	 *                     actual interval may be bigger than this, depending on
	 *                     what Vantage supports.
	 * @return A list of time series data points.
	 */
	@Override
	public List<TimeSeriesDataPoint> getDataPointsForTimeSpan(String dataType,
													String metric,
													String source,
													List<Attribute> attributes,
													Date minTime, Date maxTime,
													int intervalSecs)
	{
		List<TimeSeriesDataPoint> points;

		try
		{
			// Create the query arguments in Vantage format
			String[] dimensionIDs = new String[]{ m_TimeDimension };
			if (metric == null)
			{
				throw new NullPointerException("No metric name provided");
			}
			String metricID = extractID(metric);
			if (metricID == null)
			{
				throw new IllegalArgumentException("Metric name " + metric +
													" not in required format");
			}
			String[] metricIDs = new String[]{ metricID };
			s_Logger.debug("getDataPointsForTimeSpan - metric ID = " + metricID);
			ArrayList<String[]> filterList = new ArrayList<String[]>();
			if (attributes != null)
			{
				for (Attribute attribute : attributes)
				{
					String dimensionID = extractID(attribute.getAttributeName());
					if (dimensionID == null)
					{
						throw new IllegalArgumentException("Attribute name " +
												attribute.getAttributeName() +
												" not in required format");
					}

					if (m_AvailableDimensions.contains(dimensionID))
					{
						String val = attribute.getAttributeValue();

						// The GUI may pass through attributes with null values,
						// indicating that it doesn't want to filter on that
						// particular attribute
						if (val != null)
						{
							String[] dimFilter = new String[]{ dimensionID,
																 val };
							filterList.add(dimFilter);
							s_Logger.debug("Added dimension filter " +
											dimensionID + " = " + val);
						}
					}
					else
					{
						s_Logger.warn("Requested attribute " +
										attribute.getAttributeName() +
										" with ID " + dimensionID +
										" not available");
						for (String avDim : m_AvailableDimensions)
						{
							s_Logger.debug("Available dimension: " + avDim);
						}
					}
				}
			}
			if (source != null)
			{
				String[] dimFilter = new String[]{ m_SourceDimension, source };
				filterList.add(dimFilter);
				s_Logger.debug("Adding source filter " +
								m_SourceDimension + " = " + source);
			}
			String[][] dimFilters = null;
			if (filterList.size() > 0)
			{
				dimFilters = new String[filterList.size()][2];
				dimFilters = filterList.toArray(dimFilters);
			}
			String[][] metricFilters = null;
			String[][] sort = new String[][]{ { m_TimeDimension, null } };
			int top = ALL;
			String resolution = intervalToResolution(intervalSecs);

			// Query the Vantage web service
			DMIData data = m_Client.getDMIData(m_Application,
												m_View,
												m_DataSource,
												dimensionIDs,
												metricIDs,
												dimFilters,
												metricFilters,
												sort,
												top,
												resolution,
												null,
												null,
												minTime.getTime(),
												maxTime.getTime(),
												m_TimeOut);
			if (data.isTimeout())
			{
				throw new TimeoutException("getDMIData timed out after " +
											data.getTimeoutValue() + "ms");
			}

			// Convert the data from Vantage format to Prelert format
			points = decodeDMIDataSingle(metricID, data);
		}
		catch (Exception e)
		{
			s_Logger.error(e);
			points = new ArrayList<TimeSeriesDataPoint>();
		}

		return points;
	}


	/**
	 * Decode a <code>DMIData</code> object into the points for a single time
	 * series.
	 * @param metricID The Vantage internal ID of the metric we're looking for.
	 * @param data A <code>DMIData</code> object, as returned by
	 *             <code>getDMIData()</code>.
	 * @return A list of <code>TimeSeriesDataPoint</code>s retrieved from the
	 *         call to Vantage.
	 */
	private List<TimeSeriesDataPoint> decodeDMIDataSingle(String metricID,
															DMIData data)
																throws Exception
	{
		ArrayList<TimeSeriesDataPoint> points = new ArrayList<TimeSeriesDataPoint>();

		String[] columnHeaders = data.getColumnHeader();

		Double[][] rawData = data.getRawData();
		String[][] formattedData = data.getFormattedData();
		if (rawData.length != formattedData.length)
		{
			throw new IndexOutOfBoundsException("DMI data inconsistency: got " +
					formattedData.length + " rows of formatted data but " +
					rawData.length + " rows of raw data");
		}

		s_Logger.debug("decodeDMIDataSingle got " + columnHeaders.length +
						" columns and " + formattedData.length + " rows");

		for (int row = 0; row < formattedData.length; ++row)
		{
			long timeMS = -1;
			Double value = null;

			String[] formattedRow = formattedData[row];
			Double[] rawRow = rawData[row];
			if (rawRow.length != formattedRow.length)
			{
				throw new IndexOutOfBoundsException("DMI data inconsistency: got " +
						formattedRow.length + " pieces of formatted data but " +
						rawRow.length + " pieces of raw data in row " + row);
			}

			for (int field = 0; field < formattedRow.length; ++field)
			{
				if (m_TimeDimension.equals(columnHeaders[field]))
				{
					if (rawRow[field] != null)
					{
						timeMS = rawRow[field].longValue();
					}
				}
				else if (metricID.equals(columnHeaders[field]))
				{
					value = rawRow[field];
				}
			}

			if (timeMS >= 0 && value != null)
			{
				points.add(new TimeSeriesDataPoint(timeMS,
													value.doubleValue()));
			}
			else
			{
				s_Logger.warn("Failed to find time and metric fields in row " +
								row);
			}
		}

		return points;
	}


	/**
	 * Retrieve points from all configured time series for the given time span.
	 * The interval between points will be no smaller than the requested
	 * interval, but may be bigger if Vantage doesn't support the exact interval
	 * requested.
	 * @param minTime The earliest time for which to return a point.  This
	 *                plugin may return earlier points, providing they have
	 *                times after the latest point returned by the previous call
	 *                to this method.
	 * @param maxTime The latest time for which to return a point.
	 * @param intervalSecs The ideal interval between the points returned.  The
	 *                     actual interval may be bigger than this, depending on
	 *                     what Vantage supports.
	 * @return Collection of <code>TimeSeriesData</code> where its
	 *         <code>TimeSeriesConfig</code> has its externalKey property set.
	 */
	@Override
	public Collection<TimeSeriesData> getAllDataPointsForTimeSpan(
													Date minTime, Date maxTime,
													int intervalSecs)
	{
		Collection<TimeSeriesData> result;

		try
		{
			// Create the query arguments in Vantage format
			int numDimensions = m_AttributeDimensions.size() + 2;
			String[] dimensionIDs = new String[numDimensions];
			m_AttributeDimensions.toArray(dimensionIDs);
			dimensionIDs[numDimensions - 1] = m_TimeDimension;
			dimensionIDs[numDimensions - 2] = m_SourceDimension;
			int numMetrics = m_ConfiguredMetrics.size();
			String[] metricIDs = new String[numMetrics];
			m_ConfiguredMetrics.toArray(metricIDs);
			String[][] dimFilters = null;
			String[][] metricFilters = null;
			String[][] sort = new String[][]{ { m_TimeDimension, null } };
			int top = ALL;
			String resolution = intervalToResolution(intervalSecs);
			if (m_LastDataTimeMS == 0)
			{
				m_LastDataTimeMS = minTime.getTime() - 1;
			}
			long timeBegin = m_LastDataTimeMS + 1;
			long timeEnd = maxTime.getTime();

			// Adjust the end time so that we don't get partial data
			long lastSampleTime = m_Client.getLastSampleTime(m_Application,
															m_View, resolution);
			if (timeEnd > lastSampleTime)
			{
				timeEnd = lastSampleTime + 1;
			}

			// After the adjustment, it's possible that we won't be asking for
			// any data, in which case don't do the main query
			if (timeEnd > timeBegin)
			{
				s_Logger.debug("Querying getDMIData2() timeBegin = " +
								timeBegin + " timeEnd = " + timeEnd +
								" last sample time = " + lastSampleTime);

				// Query the Vantage web service
				DMIData2 data = m_Client.getDMIData2(m_Application,
													m_View,
													m_DataSource,
													dimensionIDs,
													metricIDs,
													dimFilters,
													metricFilters,
													sort,
													top,
													resolution,
													null,
													null,
													timeBegin,
													timeEnd,
													m_TimeOut);
				if (data.isTimeout())
				{
					throw new TimeoutException("getDMIData2 timed out after " +
											data.getTimeoutValue() + "ms");
				}

				// Convert the data from Vantage format to Prelert format
				result = decodeDMIData2Multi(data);
			}
			else
			{
				s_Logger.debug("Not querying getDMIData2() because timeBegin = " +
								timeBegin + " timeEnd = " + timeEnd +
								" last sample time = " + lastSampleTime);

				result = new ArrayList<TimeSeriesData>();
			}
		}
		catch (Exception e)
		{
			s_Logger.error(e);
			result = new ArrayList<TimeSeriesData>();
		}

		return result;
	}


	/**
	 * Decode a <code>DMIData2</code> object into the points for an arbitrary
	 * number of time series.
	 * @param data A <code>DMIData2</code> object, as returned by
	 *             <code>getDMIData2()</code>.
	 * @return A collection of <code>TimeSeriesData</code> objects, each
	 *         containing a set of points retrieved from Vantage for a
	 *         particular metric/source/attributes.
	 */
	private Collection<TimeSeriesData> decodeDMIData2Multi(DMIData2 data)
																throws Exception
	{
		HashMap<VantageExternalKey, TimeSeriesData> externalKeyToDataMap =
							new HashMap<VantageExternalKey, TimeSeriesData>();

		String[] columnHeaders = data.getColumnHeader();
		String[] columnHeaderNames = data.getColumnHeaderName();
		if (columnHeaders.length != columnHeaderNames.length)
		{
			throw new IndexOutOfBoundsException("DMI data inconsistency: got " +
					columnHeaders.length + " column header IDs but " +
					columnHeaderNames.length + " column header names");
		}

		Double[][] rawData = data.getRawData();
		String[][] formattedData = data.getFormattedData();
		if (rawData.length != formattedData.length)
		{
			throw new IndexOutOfBoundsException("DMI data inconsistency: got " +
					formattedData.length + " rows of formatted data but " +
					rawData.length + " rows of raw data");
		}

		s_Logger.debug("decodeDMIData2Multi got " + columnHeaders.length +
						" columns and " + formattedData.length + " rows");

		int nullValues = 0;
		int nonNullValues = 0;
		for (int row = 0; row < formattedData.length; ++row)
		{
			long timeMS = -1;
			String source = null;
			List<Attribute> attributes = new ArrayList<Attribute>();

			String[] formattedRow = formattedData[row];
			Double[] rawRow = rawData[row];
			if (rawRow.length != formattedRow.length)
			{
				throw new IndexOutOfBoundsException("DMI data inconsistency: got " +
						formattedRow.length + " pieces of formatted data but " +
						rawRow.length + " pieces of raw data in row " + row);
			}

			// Make one pass to get the data that won't vary between
			// metrics, namely time, source and attributes
			for (int field = 0; field < formattedRow.length; ++field)
			{
				if (m_TimeDimension.equals(columnHeaders[field]))
				{
					if (rawRow[field] != null)
					{
						timeMS = rawRow[field].longValue();
						if (m_LastDataTimeMS < timeMS)
						{
							m_LastDataTimeMS = timeMS;
						}
					}
				}
				else if (m_SourceDimension.equals(columnHeaders[field]))
				{
					source = formattedRow[field];
				}
				else if (m_AttributeDimensions.contains(columnHeaders[field]))
				{
					String attrName = createFull(columnHeaderNames[field],
												columnHeaders[field]);
					Attribute attribute = new Attribute(attrName,
														formattedRow[field]);
					attributes.add(attribute);
				}
			}

			if (timeMS >= 0 && source != null)
			{
				// Now make a second pass to get the metric names and values
				for (int field = 0; field < formattedRow.length; ++field)
				{
					if (m_ConfiguredMetrics.contains(columnHeaders[field]))
					{
						String metric = createFull(columnHeaderNames[field],
													columnHeaders[field]);
						Double value = rawRow[field];

						if (value == null)
						{
							++nullValues;
						}
						else
						{
							++nonNullValues;

							VantageExternalKey externalKey =
								new VantageExternalKey(metric, source, attributes);

							TimeSeriesData timeSeriesData =
								externalKeyToDataMap.get(externalKey);
							if (timeSeriesData == null)
							{
								TimeSeriesConfig config =
									externalKey.toTimeSeriesConfig(getDataSourceType().getName());
								List<TimeSeriesDataPoint> dataPoints =
										new ArrayList<TimeSeriesDataPoint>();
								timeSeriesData =
										new TimeSeriesData(config, dataPoints);
								externalKeyToDataMap.put(externalKey,
														timeSeriesData);
							}

							TimeSeriesDataPoint point =
								new TimeSeriesDataPoint(timeMS, value.doubleValue());
							timeSeriesData.getDataPoints().add(point);
						}
					}
				}
			}
			else
			{
				s_Logger.warn("Failed to find time and source fields in row " +
								row);
			}
		}

		s_Logger.debug("Decoded " + nullValues + " null values and " +
						nonNullValues + " non-null values from " +
						formattedData.length + " rows");

		Collection<TimeSeriesData> result = externalKeyToDataMap.values();

		// Set the min and max times in the results
		for (TimeSeriesData timeSeries : result)
		{
			TimeSeriesConfig config = timeSeries.getConfig();
			List<TimeSeriesDataPoint> points = timeSeries.getDataPoints();
			if (config != null && points != null)
			{
				int lastPoint = points.size() - 1;
				if (lastPoint >= 0)
				{
					config.setMinTime(new Date(points.get(0).getTime()));
					config.setMaxTime(new Date(points.get(lastPoint).getTime()));
				}
				else
				{
					s_Logger.warn("Time series data with config " +
									config + " has no points");
				}
			}
		}

		return result;
	}


	/**
	 * Retrieve the highest value for all the time series identified by
	 * each externalKey in <code>externalKeys</code> over a specified
	 * time span.
	 * @param externalKeys The external keys that corresponds to the each
	 *                     time series for which points are required.
	 * @param minTime The earliest time for which to return a point.
	 * @param maxTime The latest time for which to return a point.
	 * @param intervalSecs The data granularity to tell Vantage to use.  The
	 *                     actual interval used may be bigger than this,
	 *                     depending on what Vantage supports.
	 * @return For each time series return the highest value from the
	 *         specified time series over the specified interval paired
	 *         with the external key for that time series.
	 */
	@Override
	public List<ExternalKeyPeakValuePair> getPeakValueForTimeSpan(
													List<String> externalKeys,
													Date minTime, Date maxTime,
													int intervalSecs)
	{
		List<ExternalKeyPeakValuePair> result = new ArrayList<ExternalKeyPeakValuePair>();

		for (String externalKey : externalKeys)
		{
			double peak = 0d;

			try
			{
				VantageExternalKey extractedKey = new VantageExternalKey(externalKey);

				String[] dimensionIDs = new String[]{ m_TimeDimension };
				String metricID = extractID(extractedKey.getMetric());
				if (metricID == null)
				{
					throw new IllegalArgumentException("Metric name " +
													extractedKey.getMetric() +
													" not in required format");
				}
				String[] metricIDs = new String[]{ metricID };
				s_Logger.debug("getPeakValueForTimeSpan - metric ID = " +
								metricID);
				ArrayList<String[]> filterList = new ArrayList<String[]>();
				for (Attribute attribute : extractedKey.getAttributes())
				{
					String dimensionID = extractID(attribute.getAttributeName());
					if (dimensionID == null)
					{
						throw new IllegalArgumentException("Attribute name " +
								attribute.getAttributeName() +
								" not in required format");
					}

					if (m_AvailableDimensions.contains(dimensionID))
					{
						String val = attribute.getAttributeValue();

						// The GUI may pass through attributes with null values,
						// indicating that it doesn't want to filter on that
						// particular attribute
						if (val != null)
						{
							String[] dimFilter = new String[]{ dimensionID,
																val };
							filterList.add(dimFilter);
							s_Logger.debug("Added dimension filter " +
									dimensionID + " = " + val);
						}
					}
					else
					{
						s_Logger.warn("Requested attribute " +
								attribute.getAttributeName() +
								" with ID " + dimensionID +
								" not available");
						for (String avDim : m_AvailableDimensions)
						{
							s_Logger.debug("Available dimension: " + avDim);
						}
					}
				}
				String[] dimFilter = new String[]{ m_SourceDimension,
						extractedKey.getSource() };
				filterList.add(dimFilter);
				s_Logger.debug("Adding source filter " + m_SourceDimension +
								" = " + extractedKey.getSource());
				String[][] dimFilters = new String[filterList.size()][2];
				dimFilters = filterList.toArray(dimFilters);
				String[][] metricFilters = null;
				String[][] sort = new String[][]{ { metricID, DESCENDING } };
				int top = 1;
				String resolution = intervalToResolution(intervalSecs);

				DMIData2 data = m_Client.getDMIData2(m_Application,
						m_View,
						m_DataSource,
						dimensionIDs,
						metricIDs,
						dimFilters,
						metricFilters,
						sort,
						top,
						resolution,
						null,
						null,
						minTime.getTime(),
						maxTime.getTime(),
						m_TimeOut);
				if (data.isTimeout())
				{
					throw new TimeoutException("getDMIData2 timed out after " +
							data.getTimeoutValue() + "ms");
				}

				String[] columnHeaders = data.getColumnHeader();
				if (columnHeaders.length != 2)
				{
					throw new IndexOutOfBoundsException("Expected 2 column headers in DMI data for metric " +
							extractedKey.getMetric() + ", got " + columnHeaders.length);
				}

				Double[][] rawData = data.getRawData();
				if (rawData.length != 1)
				{
					throw new IndexOutOfBoundsException("Expected 1 row of DMI data for metric " +
							extractedKey.getMetric() + ", got " + rawData.length);
				}

				Double[] rawRow = rawData[0];
				if (rawRow.length != 2)
				{
					throw new IndexOutOfBoundsException("Expected 2 columns in DMI data for metric " +
							extractedKey.getMetric() + ", got " + rawRow.length);
				}

				int timeField = 0;
				int metricField = 1;
				if (metricID.equals(columnHeaders[0]))
				{
					timeField = 1;
					metricField = 0;
				}

				Double metricValue = rawRow[metricField];
				if (metricValue == null)
				{
					throw new NullPointerException("DMI data contains null value for metric " +
							extractedKey.getMetric());
				}

				peak = metricValue.doubleValue();

				if (rawRow[timeField] != null)
				{
					Date peakTime = new Date(rawRow[timeField].longValue());

					s_Logger.debug("Peak value obtained for metric " +
							extractedKey.getMetric() + " between " +
							minTime + " and " + maxTime +
							" is " + peak + " at " + peakTime);
				}
			}
			catch (Exception e)
			{
				s_Logger.error(e);
			}

			result.add(new ExternalKeyPeakValuePair(externalKey, peak));
		}

		return result;
	}


	/**
	 * Find the Vantage resolution code that represents the smallest interval
	 * that's at least as big as the specified interval.  If no such interval
	 * exists, return the biggest known interval.
	 * @param intervalSecs The interval (in seconds) to convert to a resolution.
	 * @return the resolution code, or null if no resolution code can be found.
	 */
	private String intervalToResolution(int intervalSecs)
	{
		long intervalMS = intervalSecs * 1000;
		String result = null;

		try
		{
			List<String> possibleResolutions =
					Arrays.asList(m_Client.getResolutions(m_Application, m_View));

			for (int count = 0; count < RESOLUTION_TEXT.length; ++count)
			{
				// Only consider resolutions that we know about and that are
				// available
				if (possibleResolutions.contains(RESOLUTION_TEXT[count]))
				{
					result = RESOLUTION_TEXT[count];
					if (intervalMS <= m_ResolutionMS[count])
					{
						break;
					}
				}
			}
		}
		catch (Exception e)
		{
			s_Logger.error(e);
		}

		s_Logger.debug("intervalToResolution(" + intervalSecs + ") = " +
						result);

		return result;
	}


	/**
	 * Find the smallest available Vantage resolution, and return the time
	 * interval it corresponds to.

	 * @return the usual interval in seconds, or 0 if an error occurs.
	 */
	@Override
	public int getUsualPointIntervalSecs()
	{
		int intervalSecs = 0;

		try
		{
			List<String> possibleResolutions =
					Arrays.asList(m_Client.getResolutions(m_Application, m_View));

			for (int count = 0; count < RESOLUTION_TEXT.length; ++count)
			{
				// Only consider resolutions that we know about and that are
				// available
				if (possibleResolutions.contains(RESOLUTION_TEXT[count]))
				{
					intervalSecs = (int)(m_ResolutionMS[count] / 1000l);
					break;
				}
			}
		}
		catch (Exception e)
		{
			s_Logger.error(e);
		}

		return intervalSecs;
	}

	
	/**
	 * This function does not do anything.
	 * @param value - unused.
	 */
	@Override
	public void setUsualPointIntervalSecs(int value)
	{
	}

	/**
	 * Does this plugin support time series aggregation for a given data type,
	 * i.e. querying for time series points without specifying a value for every
	 * possible attribute?  For Vantage the answer is always yes.
	 * @param dataType The name of the data type.  This is not needed as all
	 *                 Vantage applications support arbitrary aggregation over
	 *                 dimensions.
	 * @return true always.
	 */
	public boolean isAggregationSupported(String dataType)
	{
		return true;
	}


	/**
	 * Get the port to use to connect to Vantage.
	 * @return the port to use to connect to Vantage.
	 */
	public int getPort()
	{
		return m_Port;
	}


	/**
	 * Set the port to use to connect to Vantage.
	 * @param port The port to use to connect to Vantage.
	 */
	public void setPort(int port)
	{
		m_Port = port;
	}


	/**
	 * Get the host name to use to connect to Vantage.
	 * @return The host name to use to connect to Vantage.
	 */
	public String getHost()
	{
		return m_Host;
	}


	/**
	 * Set the host name to use to connect to Vantage.
	 * @param host The host name to use to connect to Vantage.
	 */
	public void setHost(String host)
	{
		m_Host = host;
	}


	/**
	 * Get the user name to use to connect to Vantage.
	 * @return The user name to use to connect to Vantage.
	 */
	public String getUsername()
	{
		return m_Username;
	}


	/**
	 * Set the user name to use to connect to Vantage.
	 * @param username The user name to use to connect to Vantage.
	 */
	public void setUsername(String username)
	{
		m_Username = username;
	}


	/**
	 * Get the password to use to connect to Vantage.
	 * @return The password to use to connect to Vantage.
	 */
	public String getPassword()
	{
		return m_Password;
	}


	/**
	 * Set the password to use to connect to Vantage.
	 * @param password The password to use to connect to Vantage.
	 */
	public void setPassword(String password)
	{
		m_Password = password;
	}


	/**
	 * Get the Vantage application to connect to.
	 * @return The ID of the Vantage application to connect to.
	 */
	public String getApplication()
	{
		return m_Application;
	}


	/**
	 * Set the Vantage application to connect to.
	 * @param application The ID of the Vantage application to connect to.
	 */
	public void setApplication(String application)
	{
		m_Application = application;
	}


	/**
	 * Get the Vantage view to connect to.
	 * @return The ID of the Vantage view to connect to.
	 */
	public String getView()
	{
		return m_View;
	}


	/**
	 * Set the Vantage view to connect to.
	 * @param view The ID of the Vantage view to connect to.
	 */
	public void setView(String view)
	{
		m_View = view;
	}


	/**
	 * Get the Vantage data source to use (not to be confused with Prelert's
	 * definition of data source).
	 * @return The ID of the Vantage data source to use.
	 */
	public String getDataSource()
	{
		return m_DataSource;
	}


	/**
	 * Set the Vantage data source to use (not to be confused with Prelert's
	 * definition of data source).
	 * @param view The ID of the Vantage data source to use.
	 */
	public void setDataSource(String dataSource)
	{
		m_DataSource = dataSource;
	}


	/**
	 * Given a human readable description and internal ID, create a string in
	 * the form:
	 * human readable [internal ID]
	 * @param desc The human readable form or a metric/dimension
	 * @param id The internal ID for a metric/dimension
	 * @return The full string in the form human readable [internal ID]
	 */
	private String createFull(String desc, String id)
	{
		StringBuilder strRep = new StringBuilder(desc);
		strRep.append(" [");
		strRep.append(id);
		strRep.append(']');

		return strRep.toString();
	}


	/**
	 * Extracts the internal ID from a string in the form:
	 * human readable [internal ID]
	 * @param full The string in the form human readable [internal ID]
	 * @return The ID, or null if the input string did not have the correct
	 *         form.
	 */
	private String extractID(String full)
	{
		String id = null;

		int start = full.lastIndexOf('[');
		int end = full.lastIndexOf(']');
		if (start > -1 && end > start + 1)
		{
			id = full.substring(start + 1, end);
		}

		return id;
	}


	/**
	 * Returns a new instance of a <code>VantagePlugin</code> which has all
	 * its members set to copies of this object's members.
	 */
	@Override
	public Plugin duplicate()
	{
		s_Logger.info("Duplicating Vantage Plugin");

		VantagePlugin clone = new VantagePlugin();

		clone.setName(getName());
		try
		{
			clone.configure(null, m_Properties);
		}
		catch (InvalidPluginPropertyException e)
		{
			s_Logger.error("Error duplicating VantagePlugin");
			s_Logger.error(e);
		}

		return clone;
	}


	@Override
	public MetricPath metricPathFromExternalKey(String datatype,
											String externalKey)
	{
		throw new UnsupportedOperationException("metricPathFromExternalKey");
	}


	/**
	 * Given a list of external keys, determine which one corresponds to the
	 * longest metric path, then return a list of partially populated
	 * <code>MetricTreeNode</code> objects containing the name and prefix for
	 * each level of that longest metric path, in order of their position in the
	 * metric path.
	 *
	 * @param externalKeyNodes List of partially populated
	 *                         <code>MetricTreeNode</code> objects containing
	 *                         external keys.
	 * @return A list of partially populated <code>MetricTreeNode</code> objects
	 *         containing the name and prefix for each level of the longest
	 *         metric path corresponding to any of the input external keys, in
	 *         order of their position in the metric path.
	 */
	@Override
	public List<MetricTreeNode> metricPathNodesFromExternalKeys(List<MetricTreeNode> externalKeyNodes)
	{
		// TODO Implement this.
		return Collections.emptyList();
	}


	/**
	 * Stub implementation, returns -1.
	 */
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
	 * Stub implementation, returns an empty list.
	 */
	@Override
	public List<MetricTreeNode> getDataSourceTreeNextLevel(String datatype,
			String previousPath, String currentValue, int opaqueNum,
			String opaqueStr) 
	{
		return Collections.emptyList();
	}

	/**
	 * Stub implementation, returns an empty list.
	 */
	@Override
	public List<MetricTreeNode> getDataSourceTreePreviousLevel(String datatype,
			String previousPath, int opaqueNum, String opaqueStr) 
	{
		return Collections.emptyList();
	}

	/**
	 * Stub implementation, returns an empty list.
	 */
	@Override
	public List<MetricTreeNode> getDataSourceTreeCurrentLevel(String datatype,
			String previousPath, int opaqueNum, String opaqueStr) 
	{
		return Collections.emptyList();
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
