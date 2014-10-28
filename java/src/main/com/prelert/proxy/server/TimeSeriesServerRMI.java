/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2013     *
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

package com.prelert.proxy.server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.prelert.dao.TimeSeriesDAO;
import com.prelert.data.Attribute;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.Evidence;
import com.prelert.data.MetricPath;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.TimeSeriesDataPoint;
import com.prelert.proxy.dao.RemoteTimeSeriesDAO;
import com.prelert.proxy.data.ExternalTimeSeriesConfig;
import com.prelert.proxy.plugin.ExternalPlugin;
import com.prelert.proxy.plugin.ExternalPointsPlugin;
import com.prelert.proxy.plugin.Plugin;
import com.prelert.proxy.plugin.TimeSeriesPlugin;
import com.prelert.proxy.pluginLocator.PluginLocator;
import com.prelert.proxy.pluginLocator.PluginLocator.ThreadLocalPlugin;


public class TimeSeriesServerRMI extends UnicastRemoteObject implements RemoteTimeSeriesDAO
{
	private static final long serialVersionUID = 8334489366391294256L;
	private static Logger s_Logger = Logger.getLogger(TimeSeriesServerRMI.class);

	private String m_ServerName;
	private PluginLocator m_PluginLocator;
	private TimeSeriesDAO m_TimeSeriesDAO;
	private EvidenceServerRMI m_EvidenceServer;
	
	
	public TimeSeriesServerRMI() throws RemoteException
	{
		super();
	}
	

	public String getServerName()
	{
		return m_ServerName;
	}
	
	public void setServerName(String serverName)
	{
		m_ServerName = serverName;
	}
	
	public PluginLocator getPluginLocator()
	{
		return m_PluginLocator;
	}

	public void setPluginLocator(PluginLocator pluginLocator)
	{
		m_PluginLocator = pluginLocator;
	}
	
	public TimeSeriesDAO getTimeSeriesDAO()
	{
		return m_TimeSeriesDAO;
	}
	
	public void setTimeSeriesDAO(TimeSeriesDAO dao)
	{
		m_TimeSeriesDAO = dao;
	}
	
	public EvidenceServerRMI getEvidenceServer()
	{
		return m_EvidenceServer;
	}
	
	public void setEvidenceServer(EvidenceServerRMI dao)
	{
		m_EvidenceServer = dao;
	}


	/**
	 * Is time series aggregation supported for the given data type, i.e.
	 * querying for time series points without specifying a value for every
	 * possible attribute?
	 * 
	 * @param dataType The name of the data type.
	 * @return true if aggregation is supported; false if it's not 
	 * 	and false if an error occours locating the external plugin.
	 */
	@Override
	public boolean isAggregationSupported(String dataType)
	{
		DataSourceType dataSourceType = new DataSourceType(dataType, 
								DataSourceCategory.TIME_SERIES);
		
		boolean isExternal = m_PluginLocator.isExternalPoints(dataSourceType) ||
								m_PluginLocator.isExternal(dataSourceType);
		if (isExternal == false)
		{
			return m_TimeSeriesDAO.isAggregationSupported(dataType);
		}

		ExternalPointsPlugin plugin = m_PluginLocator.getExternalPointsPluginForDataType(dataSourceType);
		if (plugin != null)
		{
			return plugin.isAggregationSupported(dataType);
		}
		else
		{
			return false;
		}
	}


	@Override
	public List<String> getAttributeNames(String dataType) 
	{
		DataSourceType dataSourceType = new DataSourceType(dataType, 
			DataSourceCategory.TIME_SERIES);
	
		boolean isInternal = !m_PluginLocator.isExternal(dataSourceType);
		if (isInternal)
		{
			return m_TimeSeriesDAO.getAttributeNames(dataType);
		}

		TimeSeriesPlugin plugin = m_PluginLocator.getExternalPluginForDataType(dataSourceType);
		if (plugin != null)
		{
			return plugin.getAttributeNames(dataType);
		}
		else
		{
			return Collections.emptyList();
		}
	}


	@Override
	public List<String>	getAttributeValues(String dataType, String attributeName, String source) 
	{
		DataSourceType dataSourceType = new DataSourceType(dataType, 
				DataSourceCategory.TIME_SERIES);
		
		boolean isInternal = !m_PluginLocator.isExternal(dataSourceType);
		if (isInternal)
		{
			return m_TimeSeriesDAO.getAttributeValues(dataType, attributeName, source);
		}

		TimeSeriesPlugin plugin = m_PluginLocator.getExternalPluginForDataType(dataSourceType);
		if (plugin != null)
		{
			return plugin.getAttributeValues(dataType, attributeName, source);
		}
		else
		{
			return Collections.emptyList();
		}
	}
	
	@Override
	public Date getLatestTime(String dataType, String source) 
	{
		Date latestTime = null;

		if (dataType == null)
		{
			latestTime = m_TimeSeriesDAO.getLatestTime(dataType, source);
			
			Date externalLatest = new Date(0L);
			
			Collection<ThreadLocalPlugin> plugins = m_PluginLocator.getPlugins();
			Iterator<ThreadLocalPlugin> itr = plugins.iterator();
			
			while (itr.hasNext())
			{
				Plugin plugin = itr.next().get();
				if (plugin instanceof ExternalPlugin)
				{
					Date pluginLatest = ((ExternalPlugin)plugin).getLatestTime(dataType, source);
					
					if (pluginLatest != null && pluginLatest.after(externalLatest))
					{
						externalLatest = pluginLatest;
					}
					
				}
			}

			if (latestTime != null)
			{
				latestTime = (externalLatest.after(latestTime)) ? externalLatest : latestTime;
			}
		}
		else
		{
			DataSourceType dataSourceType = new DataSourceType(dataType, 
							DataSourceCategory.TIME_SERIES);
			
			boolean isExternalPoints = m_PluginLocator.isExternalPoints(dataSourceType);
			if (isExternalPoints)
			{
				ExternalPointsPlugin plugin = m_PluginLocator.getExternalPointsPluginForDataType(dataSourceType);
				if (plugin != null)
				{
					latestTime = plugin.getLatestTime(dataType, source);
				}
			}
			else 
			{
				latestTime = m_TimeSeriesDAO.getLatestTime(dataType, source);
			}
		}

		return latestTime;
	}


	/**
	 * Returns the data model for a time series feature with the specified id.
	 * Time series features are always internal, so this is always obtained from
	 * the database, never a plugin.
	 * @param id the unique identifier for the time series feature.
	 * @return full data model for the evidence item with the specified id.
	 */
	@Override
	public Evidence getFeature(int id)
	{
		return m_TimeSeriesDAO.getFeature(id);
	}


	/**
	 * Returns the config of the time series corresponding to a specified
	 * feature id.
	 * @param id the unique identifier for the time series feature.
	 * @return config of the corresponding time series (or null if the
	 *         input id wasn't found).
	 */
	@Override
	public TimeSeriesConfig getTimeSeriesFromFeature(int id)
	{
		TimeSeriesConfig config = m_TimeSeriesDAO.getTimeSeriesFromFeature(id);

		if (config != null)
		{
			// For external time series, fetch attributes from the appropriate
			// plugin
			String externalKey = config.getExternalKey();
			if (externalKey != null && !externalKey.isEmpty())
			{
				List<Attribute> attributes = Collections.emptyList();

				DataSourceType dataSourceType = new DataSourceType(config.getDataType(),
						DataSourceCategory.TIME_SERIES);
				
				// Get attributes from the plugin if it is an external type and
				// no attributes are currently set.
				if (m_PluginLocator.isExternal(dataSourceType) && 
						(config.getAttributes() == null || config.getAttributes().isEmpty()))
				{
					ExternalPlugin plugin = m_PluginLocator.getExternalPluginForDataType(dataSourceType);
					if (plugin != null)
					{
						attributes = plugin.getAttributesForKey(externalKey);
					}
					config.setAttributes(attributes);
				}
			}
		}

		return config;
	}


	@Override
	public List<String> getMetrics(String dataType)
	{
		List<String> metrics = Collections.emptyList();
		
		DataSourceType dataSourceType = new DataSourceType(dataType,
				DataSourceCategory.TIME_SERIES);
		
		boolean isInternal = !m_PluginLocator.isExternal(dataSourceType);
		if (isInternal)
		{
			metrics = m_TimeSeriesDAO.getMetrics(dataType);
		}
		else 
		{
			TimeSeriesPlugin plugin = m_PluginLocator.getExternalPluginForDataType(dataSourceType);
			if (plugin != null)
			{
				metrics = plugin.getMetrics(dataType);
			}
		}
		
		return metrics;		
	}


	/**
	 * Called primarily by the GUI to get points for a given time window,
	 * filtered using the functionality available in the GUI, i.e. type, metric,
	 * source and attributes.
	 * @param dataType The Prelert data type of the points to be retrieved.
	 *                 Should not be null.
	 * @param metric The metric to be retrieved.  Should not be null.
	 * @param minTime The earliest time to retrieve points.
	 * @param maxTime The latest time to retrieve points.
	 * @param source The name of the machine that generated the points.  If null
	 *               this means points should be combined across all sources.
	 * @param attributes A list of attributes to narrow down the data retrieved.
	 *                   Note that this list may contain attributes with null
	 *                   values.  This implies that all values of that attribute
	 *                   are to be combined in the results.
	 * @param includeFeatures Should evidence IDs of features be added to the
	 *                        output?
	 * @return A list of data points that match the filter conditions.
	 */
	@Override
	public List<TimeSeriesDataPoint> getDataPointsForTimeSpan(String dataType, String metric, 
			Date minTime, Date maxTime, String source, 
			List<Attribute> attributes, boolean includeFeatures) 
	{
		DataSourceType dataSourceType = new DataSourceType(dataType,
						DataSourceCategory.TIME_SERIES);
		
		boolean isExternal = m_PluginLocator.isExternalPoints(dataSourceType) ||
								m_PluginLocator.isExternal(dataSourceType);
		if (isExternal == false)
		{
			return m_TimeSeriesDAO.getDataPointsForTimeSpan(dataType, 
													metric, minTime, maxTime, source, 
													attributes, includeFeatures);
		}
		
		List<TimeSeriesDataPoint> points = Collections.emptyList();

		if (m_PluginLocator.isExternal(dataSourceType))
		{
			ExternalPlugin plugin = m_PluginLocator.getExternalPluginForDataType(dataSourceType);
			if (plugin != null)
			{
				int usualInterval = plugin.getUsualPointIntervalSecs();
				int interval = calcRequiredDataPointIntervalForPeriod(minTime, maxTime, usualInterval);
				
				points = plugin.getDataPointsForTimeSpan(dataType, metric, source,
						attributes, minTime, maxTime, interval);
			}
		}
		else if (m_PluginLocator.isExternalPoints(dataSourceType)) 
		{
			// Check for an external points plugin.
			ExternalPointsPlugin epPlugin = m_PluginLocator.getExternalPointsPluginForDataType(dataSourceType);
			if (epPlugin != null)
			{
				int usualInterval = epPlugin.getUsualPointIntervalSecs();
				int interval = calcRequiredDataPointIntervalForPeriod(minTime, maxTime, usualInterval);
				
				String externalKey = externalKeyFromAttributes(dataType, metric, source, attributes);
				points = epPlugin.getDataPointsForTimeSpan(externalKey, minTime, maxTime, interval);
			}
		}

		
		
		if (includeFeatures)
		{
			List<Evidence> evidence = getEvidenceInTimePeriod(minTime, maxTime, dataType,
					source, metric, attributes);

			points = syncTimeSeriesPointsAndEvidence(points, evidence);
		}
		else // Ensure the points are sorted.
		{
			Collections.sort(points);
		}
		
		return points;
	}


	@Override
	public List<TimeSeriesDataPoint> getDataPoints(String dataType, String metric, 
			Date minTime, Date maxTime, String source, 
			List<Attribute> attributes, boolean includeFeatures) 
	{
		throw new UnsupportedOperationException();
	}
		
	@Override
	public List<TimeSeriesDataPoint> getDataPointsForDay(String dataType, String metric, 
			Date minTime, Date maxTime, String source, List<Attribute> attributes)
	{
		throw new UnsupportedOperationException();		
	}
	
	@Override
	public List<TimeSeriesDataPoint> getDataPointsForWeek(String dataType, String metric, 
			Date minTime, Date maxTime, String source, List<Attribute> attributes)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public List<String> getSourcesOrderByName(DataSourceType dataSourceType) 
	{
		return m_TimeSeriesDAO.getSourcesOrderByName(dataSourceType);
	}

	@Override
	public List<String> getSourcesOrderByCount(DataSourceType dataSourceType) 
	{
		return m_TimeSeriesDAO.getSourcesOrderByCount(dataSourceType);
	}
		
		
	/**
	 * If <code>timeSeriesId</code> is negative then the data points have
	 * an external source.
	 */
	@Override
	public List<TimeSeriesDataPoint> getDataPointsRaw(int timeSeriesId,
													Date minTime, Date maxTime)
	{
		if (timeSeriesId > 0)
		{
			return m_TimeSeriesDAO.getDataPointsRaw(timeSeriesId, minTime, maxTime);
		}

		// We expect this list to contain exactly one time series config given
		// that we've queried for the primary key of the table
		ExternalTimeSeriesConfig series =
				m_PluginLocator.getPluginDescriptionForTimeSeriesId(timeSeriesId);
		if (series == null)
		{
			s_Logger.error("Could not retrieve external key for time series ID " +
							timeSeriesId);

			return new ArrayList<TimeSeriesDataPoint>();
		}

		DataSourceType dataSourceType = new DataSourceType(series.getType(), DataSourceCategory.TIME_SERIES);
		ExternalPointsPlugin plugin = m_PluginLocator.getExternalPointsPluginForDataType(dataSourceType);
		if (plugin != null)
		{
			int usualInterval = plugin.getUsualPointIntervalSecs();

			// Raw points are always supplied at the usual interval, never a
			// summarised interval
			return plugin.getDataPointsForTimeSpan(series.getExternalKey(),
												minTime, maxTime,
												usualInterval);
		}
		else
		{
			return Collections.emptyList();
		}
	}


	/**
	 * Return a list of Evidence for the time period of type <code>dataType</code>
	 * from source <code>source</code> with attributes <code>attributes</code>.
	 * @param minTime Start of time period
	 * @param maxTime End of time period
	 * @param dataType Filter by dataType
	 * @param source Filter by source
	 * @param metric
	 * @param attributes Filter by attributes
	 * @return List of <code>Evidence</code>.
	 */
	private List<Evidence> getEvidenceInTimePeriod(Date minTime, Date maxTime,
													String dataType,
													String source,
													String metric,
													List<Attribute> attributes)
	{
		return m_EvidenceServer.getEvidenceInExternalTimeSeries(dataType, metric,
														source, attributes,
										 				minTime, maxTime);
	}


	/**
	 * Syncs TimeSeriesDataPoints with evidence items.  Matches by time.  There
	 * may be evidence with times for which there is no time series point (for
	 * example if the graph is zoomed out and we're only showing points every
	 * hour or two).
	 * @param points Will be modified by this function with any appropriate
	 *               time series features added.
	 * @param evidence List of partial <code>Evidence</code> objects (populated
	 *                 with time and ID only) to try to match up with time
	 *                 series points.
	 * @return Returns the modified <code>points</code> parameter.
	 */
	private List<TimeSeriesDataPoint> syncTimeSeriesPointsAndEvidence(
												List<TimeSeriesDataPoint> points,
												List<Evidence> evidence)
	{
		Collections.sort(points);

		TimeSeriesDataPoint point = new TimeSeriesDataPoint(0, 0.0);
		for (Evidence e : evidence)
		{
			point.setTime(e.getTime().getTime());

			int index = Collections.binarySearch(points, point);
			if (index >= 0)
			{
				// Get the full time series feature details from the database
				Evidence feature = getFeature(e.getId());
				points.get(index).setFeature(feature);
			}
		}

		return points;
	}


	/**
	 * The intention here is to return no more than 750 points in one go to the
	 * GUI, as this is about the limit of what the graph widget can cope with.
	 * @param start Start of time period for which points have been requested.
	 * @param end End of time period for which points have been requested.
	 * @param usualInterval Usual interval between points (in seconds).
	 * @return The ideal interval (in seconds) between points provided to the
	 *         GUI.
	 */
	private int calcRequiredDataPointIntervalForPeriod(Date start, Date end, int usualInterval)
	{
		final int MAX_POINTS = 750;
		
		int differenceS = (int)((end.getTime() - start.getTime()) / 1000l);

		// Calculate the interval that would yield 750 points
		int interval = differenceS / MAX_POINTS;

		// If that interval is less than the usual interval, use that instead
		if (interval < usualInterval)
		{
			interval = usualInterval;
		}

		return interval;
	}

	
	/**
	 * If the <code>id</code> is a positive number then the 
	 * time series is internal so get the metric path from the database.
	 * Else the type is external so get the external key from the database
	 * and ask the plugin to build the metric path from the key.
	 */
	@Override
	public MetricPath getMetricPathFromTimeSeriesId(int id) throws RemoteException 
	{
		MetricPath metricPath = null;
		if (id > 0)
		{
			metricPath =  m_TimeSeriesDAO.getMetricPathFromTimeSeriesId(id);
		}
		else
		{
			metricPath =  m_TimeSeriesDAO.getMetricPathFromTimeSeriesId(id);
			
			DataSourceType dataSourceType = new DataSourceType(metricPath.getDatatype(),
										DataSourceCategory.TIME_SERIES);
			ExternalPlugin plugin = m_PluginLocator.getExternalPluginForDataType(dataSourceType);
			if (plugin != null)
			{
				metricPath = plugin.metricPathFromExternalKey(
										metricPath.getDatatype(), metricPath.getExternalKey());
			}
		}
		
		return metricPath;
	}


	/**
	 * Returns the time series ID corresponding to a given external key.
	 * If no such time series ID is found <code>null</code> is returned.
	 *
	 * @param dataType The data type to which the external key belongs.
	 * @param externalKey The external key to be looked up.
	 * @return Time series ID or <code>null</code>
	 */
	public Integer getTimeSeriesIdFromExternalKey(String dataType,
												String externalKey) throws RemoteException
	{
		return m_TimeSeriesDAO.getTimeSeriesIdFromExternalKey(dataType,
															externalKey);
	}


	/**
	 * Returns the external key corresponding to a given time series ID.
	 * If the time series ID does not correspond to an external time series,
	 * <code>null</code> is returned.
	 *
	 * @param timeSeriesId The time series ID to look up.
	 * @return An external key or <code>null</code>
	 */
	public String getExternalKeyFromTimeSeriesId(int timeSeriesId) throws RemoteException
	{
		return m_TimeSeriesDAO.getExternalKeyFromTimeSeriesId(timeSeriesId);
	}

	
	private String externalKeyFromAttributes(String datatype, String metric, 
							String source, List<Attribute> attributes)
	{
		return m_TimeSeriesDAO.getExternalKeyFromTimeSeriesDetails(datatype, 
									metric, source, attributes);
	}

}
