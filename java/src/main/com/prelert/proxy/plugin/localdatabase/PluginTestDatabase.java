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

package com.prelert.proxy.plugin.localdatabase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import com.prelert.dao.DataSourceDAO;
import com.prelert.dao.EvidenceDAO;
import com.prelert.dao.IncidentDAO;
import com.prelert.dao.TimeSeriesDAO;
import com.prelert.data.Attribute;
import com.prelert.data.ConnectionStatus;
import com.prelert.data.DataSource;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.Evidence;
import com.prelert.data.MetricPath;
import com.prelert.data.MetricTreeNode;
import com.prelert.data.Notification;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.TimeSeriesData;
import com.prelert.data.TimeSeriesDataPoint;
import com.prelert.proxy.data.ExternalDataTypeConfig;
import com.prelert.proxy.data.SourceConnectionConfig;
import com.prelert.proxy.plugin.NotificationPlugin;
import com.prelert.proxy.plugin.Plugin;
import com.prelert.proxy.plugin.ExternalPlugin;
import com.prelert.proxy.pluginLocator.PluginLocatorDAO;
import com.prelert.server.ServerUtil;

/**
 * Test class, retrieves data from the local database.
 */

public class PluginTestDatabase extends Plugin implements ExternalPlugin, NotificationPlugin
{
	private DataSourceDAO m_DataSourceDAO;
	private EvidenceDAO m_EvidenceDAO;
	private IncidentDAO m_IncidentDAO;
	private TimeSeriesDAO m_TimeSeriesDAO;
	private PluginLocatorDAO m_PluginLocatorDAO;
	
	private List<DataSourceType> m_SupportedDataTypes;


	public PluginTestDatabase()
	{
		m_SupportedDataTypes = new ArrayList<DataSourceType>();
		
		// This is the wildcard type.
		setDataSourceType(new DataSourceType("*", DataSourceCategory.TIME_SERIES));

		setConfigured(true);
	}

	@Override
	public void setDataType(String datatype)
	{
		// override this to do nothing.
		// datatype is set in constructor.
	}

	public PluginLocatorDAO getPluginLocatorDAO()
	{
		return m_PluginLocatorDAO;
	}


	/**
	 * Get the count of data items for this data type (as displayed on the GUI's
	 * "Analysed Data" screen), or -1 if this information is not available.
	 * @param type The data type to get the item count for.
	 * @return The count of data items for this data type.  If this information
	 *         is not available returns -1.
	 */
	@Override
	public int getDataTypeItemCount(DataSourceType sourceType)
	{
		List<DataSourceType> types = m_DataSourceDAO.getDataSourceTypes();
		Iterator<DataSourceType> itr = types.iterator();
		
		while (itr.hasNext())
		{
			DataSourceType type = itr.next();
			if (type.getName().equals(sourceType.getName()) && 
					type.getDataCategory().equals(sourceType.getDataCategory())) 
			{
				return type.getCount();
			}		
		}	

		return -1;
	}
	
	@Override
	public int getDataSourceItemCount(DataSource source)
	{
		return source.getCount();
	}


	/**
	 * Get a list of sources.
	 * @param type A data type to restrict the sources retrieved.  We'll only return
	 *             sources that provide the given data type.
	 * @return A list of all the source machines that have provided data for the
	 *         given type.
	 */
	@Override
	public List<DataSource> getDataSources(DataSourceType type)
	{
		return m_DataSourceDAO.getDataSources(type);
	}

	
	/*
	 * Time Series functions
	 */
	@Override
	public List<String> getAttributeNames(String dataType)
	{
		return m_TimeSeriesDAO.getAttributeNames(dataType);
	}
	
	@Override
	public List<String>	getAttributeValues(String dataType, String attributeName, String source)
	{
		return m_TimeSeriesDAO.getAttributeValues(dataType, attributeName, source);
	}
	

	/**
	 * For a normal plugin, this would returns a list of all the
	 * <code>Attribute</code>s associated with a specific time series,
	 * identified by its external key.  However, since this plugin is talking to
	 * a local database it simply returns an empty list.
	 * @param externalKey The external key of the time series.
	 * @return Empty list.
	 */
	@Override
	public List<Attribute> getAttributesForKey(String externalKey)
	{
		List<Attribute> attributes = new ArrayList<Attribute>();

		return attributes;		
	}

	
	@Override
	public Date getLatestTime(String dataType, String source)
	{
		return m_TimeSeriesDAO.getLatestTime(dataType, source);		
	}
	

	@Override
	public List<String> getMetrics(String dataType)
	{
		return m_TimeSeriesDAO.getMetrics(dataType);
	}
	
	@Override
	public List<TimeSeriesDataPoint> getDataPointsForTimeSpan(String dataType, String metric,
												String source,
												List<Attribute> attributes,
												Date minTime, Date maxTime, 
												int intervalSecs)
	{
		List<TimeSeriesDataPoint> points = m_TimeSeriesDAO.getDataPointsForTimeSpan(dataType, 
														metric, minTime, maxTime, source, 
														attributes, false);
		
		for (TimeSeriesDataPoint pt : points)  
		{
			Evidence e = pt.getFeature(); 
			assert (e != null);  // there should be no features in external time series data.
		}
		
		
		return points;
	}
	
	@Override
	public List<TimeSeriesDataPoint> getDataPointsForTimeSpan(String externalKey,
															Date minTime, Date maxTime, 
															int intervalSecs)
	{
		TimeSeriesKey key = new TimeSeriesKey(externalKey);
		
		List<TimeSeriesDataPoint> points = m_TimeSeriesDAO.getDataPointsForTimeSpan(key.getDataType(), 
														key.getMetric(), minTime, maxTime, key.getSource(), 
														null, false);
		
		for (TimeSeriesDataPoint pt : points)  
		{
			Evidence e = pt.getFeature(); 
			assert (e != null);  // there should be no features in external time series data.
		}
		
		
		return points;
	}
	

	@Override
	public Collection<TimeSeriesData> getAllDataPointsForTimeSpan(Date minTime, Date maxTime, 
														int intervalSecs)
	{
		HashMap<String, TimeSeriesData> dataMap = new HashMap<String, TimeSeriesData>();
		
		List<String> metrics = new ArrayList<String>();
		for (DataSourceType dataType : m_SupportedDataTypes)
		{
			metrics.addAll(m_TimeSeriesDAO.getMetrics(dataType.getName()));
		}
		
		List<DataSource> sources = m_DataSourceDAO.getAllDataSources();
		
		for (DataSource source : sources)
		{
			if (source.getDataSourceType().getDataCategory() == DataSourceCategory.TIME_SERIES)
			{
				for (String metric : metrics)
				{
					String externalKey = new TimeSeriesKey(source.getDataSourceType().getName(),
															metric, source.getSource()).toString();
					
					List<TimeSeriesDataPoint> dataPoints = m_TimeSeriesDAO.getDataPoints(
																source.getDataSourceType().getName(), metric,
																minTime, maxTime, source.getSource(), null, true);
					
					TimeSeriesData timeSeriesData = dataMap.get(externalKey);
					if (timeSeriesData == null)
					{
						timeSeriesData = createTimeSeriesDataFromData(source.getDataSourceType().getName(),
																	source.getSource(), metric);
						timeSeriesData.getConfig().setExternalKey(externalKey);
						
						dataMap.put(externalKey, timeSeriesData);
					}
					
					timeSeriesData.getDataPoints().addAll(dataPoints);
				}
			}
		}
		
		return dataMap.values(); 
	}
	
	
	private TimeSeriesData createTimeSeriesDataFromData(String dataType, String source, String metric)
	{
		TimeSeriesConfig config = new TimeSeriesConfig(dataType, metric, source);
		
		return new TimeSeriesData(config, new ArrayList<TimeSeriesDataPoint>());
	}
	
	/*
	 * Causality
	 */

	@Override
	public List<ExternalKeyPeakValuePair> getPeakValueForTimeSpan(
											List<String> externalKeys,
											Date minTime, Date maxTime,
											int interval)
	{
		List<ExternalKeyPeakValuePair> result = new ArrayList<ExternalKeyPeakValuePair>();

		for (String externalKey : externalKeys)
		{
			TimeSeriesKey key = new TimeSeriesKey(externalKey);

			List<TimeSeriesDataPoint> points = m_TimeSeriesDAO.getDataPointsForTimeSpan(key.getDataType(), 
					key.getMetric(), 
					minTime, maxTime, 
					key.getSource(), null, false);

			double peak = -1.0;

			for (TimeSeriesDataPoint point : points)
			{
				if (point.getValue() > peak)
				{
					peak = point.getValue();
				}
			}
			
			result.add(new ExternalKeyPeakValuePair(key.toString(), peak));
		}

		return result;
	}


	public DataSourceDAO getDataSourceDAO()
	{
		return m_DataSourceDAO;
	}


	public void setDataSourceDAO(DataSourceDAO dao)
	{
		m_DataSourceDAO = dao;
	}


	public EvidenceDAO getEvidenceDAO()
	{
		return m_EvidenceDAO;
	}


	public void setEvidenceDAO(EvidenceDAO dao)
	{
		m_EvidenceDAO = dao;
	}


	public IncidentDAO getIncidentDAO()
	{
		return m_IncidentDAO;
	}


	public void setIncidentDAO(IncidentDAO dao)
	{
		m_IncidentDAO = dao;
	}


	public TimeSeriesDAO getTimeSeriesDAO()
	{
		return m_TimeSeriesDAO;
	}


	public void setTimeSeriesDAO(TimeSeriesDAO dao)
	{
		m_TimeSeriesDAO = dao;
	}


	public void setPluginLocatorDAO(PluginLocatorDAO dao)
	{
		m_PluginLocatorDAO = dao;
	}


	@Override 
	public int getUsualPointIntervalSecs()
	{
		return 30;
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
	 * possible attribute?
	 * @param dataType The name of the data type.
	 * @return true if aggregation is supported; false if it's not.
	 */
	@Override
	public boolean isAggregationSupported(String dataType)
	{
		return m_TimeSeriesDAO.isAggregationSupported(dataType);
	}


	private class TimeSeriesKey
	{
		private String m_Datatype;
		private String m_Metric;
		private String m_Source;
		
		public TimeSeriesKey(String externalKeyString)
		{
			String [] splits = externalKeyString.split("&");
			m_Datatype = splits[0];
			m_Metric = splits[1];
			m_Source = splits[2];
		}
		
		public TimeSeriesKey(String dataType, String metric, String source)
		{
			m_Datatype = dataType;
			m_Metric = metric;
			m_Source = source;
		}
		
		public String toString()
		{
			return m_Datatype + "&" + m_Metric + "&" + m_Source;
		}
		
		public String getMetric()
		{
			return m_Metric;
		}
		
		public String getSource()
		{
			return m_Source;
		}

		public String getDataType()
		{
			return m_Datatype;
		}
	}


	/*
	 * Notifications.
	 */
	@Override
	public List<Notification> getNotifications(Date start, Date end)
	{
		return new ArrayList<Notification>();
	}


	@Override
	public boolean configure(SourceConnectionConfig config, Properties properties)
	{
		List<ExternalDataTypeConfig> configs = m_PluginLocatorDAO.getExternalPluginsDescriptions();
		for (ExternalDataTypeConfig type : configs)
		{
			m_SupportedDataTypes.add(new DataSourceType(type.getType(), DataSourceCategory.TIME_SERIES));
		}
		
		// This is the wildcard type.
		setDataSourceType(new DataSourceType("*", DataSourceCategory.TIME_SERIES));
		
		return true;
	}
	
	
	@Override
	public ConnectionStatus testConnection(SourceConnectionConfig config, Properties properties)
	{
		return new ConnectionStatus(ConnectionStatus.Status.CONNECTION_OK);
	}


	/**
	 * Creates a new instance of <code>PluginTestDatabase</code> and sets its
	 * properties to this objects properties.
	 */
	@Override 
	public Plugin duplicate() 
	{
		PluginTestDatabase clone =  new PluginTestDatabase();

		clone.setName(getName());
		
		clone.setDataSourceDAO(m_DataSourceDAO);
		clone.setEvidenceDAO(m_EvidenceDAO);
		clone.setIncidentDAO(m_IncidentDAO);
		clone.setTimeSeriesDAO(m_TimeSeriesDAO);
		clone.setPluginLocatorDAO(m_PluginLocatorDAO);
		
		clone.configure(null, null);
		return clone;
	}


	@Override
	public List<MetricTreeNode> getDataSourceTreeNextLevel(String datatype, 
													String previousPath, 
													String currentValue, int opaqueNum,
													String opaqueStr) 
	{
		return Collections.emptyList();
	}


	@Override
	public List<MetricTreeNode> getDataSourceTreePreviousLevel(String datatype, 
											String previousPath, 
												int opaqueNum, String opaqueStr) 
	{
		return Collections.emptyList();
	}


	@Override
	public List<MetricTreeNode> getDataSourceTreeCurrentLevel(String datatype, 
											String previousPath, 
												int opaqueNum, String opaqueStr) 
	{
		return Collections.emptyList();
	}


	@Override
	public MetricPath metricPathFromExternalKey(String datatype,
										String externalKey) 
	{
		// TODO Implement this.
		return new MetricPath();
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
		// NB: This is hardcoded to work with the unit test in RemoteIncidentTest.java
		return m_IncidentDAO.getIncidentMetricPathNodes(16954);
	}

}
