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

package com.prelert.dao;

import java.util.Date;
import java.util.List;

import com.prelert.data.Attribute;
import com.prelert.data.DataSourceType;
import com.prelert.data.Evidence;
import com.prelert.data.MetricPath;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.TimeSeriesDataPoint;



/**
 * Interface defining the methods to be implemented by a Data Access Object
 * to obtain time series data from the Prelert database.
 * 
 * @author Pete Harverson
 */
public interface TimeSeriesDAO
{

	/**
	 * Is time series aggregation supported for the given data type, i.e.
	 * querying for time series points without specifying a value for every
	 * possible attribute?
	 * @param dataType The name of the data type, e.g. system_udp or
	 *                 p2psmon_servers.
	 * @return true if aggregation is supported; false if it's not.
	 */
	public boolean isAggregationSupported(String dataType);


	/**
	 * Returns the list of available sources of time series data,
	 * ordered by name, for the given data type.
	 * @param dataSourceType type of time series data e.g. system_udp or p2psmon_servers.
	 * @return the list of available sources.
	 */
	public List<String> getSourcesOrderByName(DataSourceType dataSourceType);
	
	
	/**
	 * Returns the list of available sources of time series data,
	 * ordered by data point count, for the given data type.
	 * @param dataSourceType type of time series data e.g. system_udp or p2psmon_servers.
	 * @return the list of available sources.
	 */
	public List<String> getSourcesOrderByCount(DataSourceType dataSourceType);
	
	
	/**
	 * Returns a list of the names of the attributes associated with the given
	 * time series data type.
	 * @param dataType type of time series data e.g. system_udp or p2psmon_servers.
	 * @return the list of attribute names associated with the data type, 
	 * 			ordered by name.
	 */
	public List<String> getAttributeNames(String dataType);
	
	
	/**
	 * Returns a list of the distinct values for the attribute with the given name
	 * for the specified data type e.g. the values of the 'username' attribute
	 * for p2psmon_users data.
	 * @param dataType type of time series data e.g. system_udp or p2psmon_servers.
	 * @param attributeName name of attribute for which to return the values.
	 * @param source optional source name.
	 * @return a list of the distinct values for the attribute.
	 */
	public List<String>	getAttributeValues(String dataType, String attributeName, String source);
	
	
	/**
	 * Returns the list of metrics associated with the given time series data type.
	 * @param dataType type of time series data e.g. system_udp or p2psmon_servers.
	 * @return the list of metrics associated with the data type, ordered by name.
	 */
	public List<String> getMetrics(String dataType);
	

	/**
	 * Returns all the time series data points for the given metric between the supplied
	 * start and end times.
	 * @param dataType type of time series data e.g. system_udp or p2psmon_servers.
	 * @param metric the time series metric.
	 * @param minTime minimum date/time to include.
	 * @param maxTime maximum date/time to include.
	 * @param source optional source name.
	 * @param attributes optional list of attributes.
	 * @param includeFeatures flag indicating whether time series features should
	 * 	be included in the returned data points.
	 * @return list of time series data points.
	 */
	public List<TimeSeriesDataPoint> getDataPoints(String dataType, String metric, 
			Date minTime, Date maxTime, String source, 
			List<Attribute> attributes, boolean includeFeatures);
	
	
	/**
	 * Returns the time series data points for the given metric between the supplied
	 * start and end times, with the data aggregated for a daily display.
	 * @param dataType type of time series data e.g. system_udp or p2psmon_servers.
	 * @param metric the time series metric.
	 * @param minTime minimum date/time to include.
	 * @param maxTime maximum date/time to include.
	 * @param source optional source name.
	 * @param attributes optional list of attributes..
	 * @return list of time series data points.
	 */
	public List<TimeSeriesDataPoint> getDataPointsForDay(String dataType, String metric, 
			Date minTime, Date maxTime, String source, List<Attribute> attributes);
	
	
	/**
	 * Returns the time series data points for the given metric between the supplied
	 * start and end times, with the data aggregated for a weekly display.
	 * @param dataType type of time series data e.g. system_udp or p2psmon_servers.
	 * @param metric the time series metric.
	 * @param minTime minimum date/time to include.
	 * @param maxTime maximum date/time to include.
	 * @param source optional source name.
	 * @param attributes optional list of attributes..
	 * @return list of time series data points.
	 */
	public List<TimeSeriesDataPoint> getDataPointsForWeek(String dataType, String metric, 
			Date minTime, Date maxTime, String source, List<Attribute> attributes);
	
	
	/**
	 * Returns the time series data points for the given metric between the supplied
	 * start and end times. The data is aggregated according to the span between the
	 * supplied dates so that a maximum of around 700 points will be returned.
	 * @param dataType type of time series data e.g. system_udp or p2psmon_servers.
	 * @param metric the time series metric.
	 * @param minTime minimum date/time to include.
	 * @param maxTime maximum date/time to include.
	 * @param source optional source name.
	 * @param attributes optional list of attributes.
	 * @param includeFeatures flag indicating whether time series features should
	 * 	be included in the returned data points.
	 * @return list of time series data points.
	 */
	public List<TimeSeriesDataPoint> getDataPointsForTimeSpan(String dataType, String metric, 
			Date minTime, Date maxTime, String source, 
			List<Attribute> attributes, boolean includeFeatures);
	
	
	/**
	 * Returns the raw data points (time/value) for the time series with the specified
	 * id between the supplied start and end times. The data points are <i>not</i> 
	 * aggregated according to the span between the supplied dates, with the method 
	 * returning points at the finest granularity. Therefore the number of points
	 * returned will increase in line with the requested time span.
	 * @param timeSeriesId id of the time series.
	 * @param minTime minimum date/time to include.
	 * @param maxTime maximum date/time to include.
	 * @return list of time series data points.
	 */
	public List<TimeSeriesDataPoint> getDataPointsRaw(
			int timeSeriesId, Date minTime, Date maxTime);
	
	
	/**
	 * Returns the date/time of the latest record in the database for the
	 * time series with the specified data type.
	 * @param dataType type of time series data e.g. system_udp or p2psmon_servers.
	 * @param source optional source name.
	 * @return date/time of latest record.
	 */
	public Date getLatestTime(String dataType, String source);


	/**
	 * Returns the data model for a time series feature with the specified id.
	 * @param id the unique identifier for the time series feature.
	 * @return full data model for the evidence item with the specified id.
	 */
	public Evidence getFeature(int id);


	/**
	 * Returns the config of the time series corresponding to a specified
	 * feature id.
	 * @param id the unique identifier for the time series feature.
	 * @return config of the corresponding time series (or null if the
	 *         input id wasn't found).
	 */
	public TimeSeriesConfig getTimeSeriesFromFeature(int id);
	
	
	/**
	 * Returns the unique metric path for the Time series identified 
	 * by the parameter <code>id</code>. 
	 * If the metric path cannot be found <code>null</code> is
	 * returned. 
	 * 
	 * @param id - The unique time series id.
	 * @return MetricPath or <code>null</code>
	 */
	public MetricPath getMetricPathFromTimeSeriesId(int id);


	/**
	 * Returns the time series ID corresponding to a given external key.
	 * If no such time series ID is found <code>null</code> is returned.
	 *
	 * @param dataType The data type to which the external key belongs.
	 * @param externalKey The external key to be looked up.
	 * @return Time series ID or <code>null</code>
	 */
	public Integer getTimeSeriesIdFromExternalKey(String dataType, String externalKey);


	/**
	 * Returns the external key corresponding to a given time series ID.
	 * If the time series ID does not correspond to an external time series,
	 * <code>null</code> is returned.
	 *
	 * @param timeSeriesId The time series ID to look up.
	 * @return An external key or <code>null</code>
	 */
	public String getExternalKeyFromTimeSeriesId(int timeSeriesId);


	/**
	 * Returns the external key string for the time series with the given
	 * datatype, metric, source and attributes.
	 * 
	 * @param datatype
	 * @param metric
	 * @param source
	 * @param attributes
	 * @return
	 */
	public String getExternalKeyFromTimeSeriesDetails(String datatype, String metric,
										String source, List<Attribute> attributes);
}

