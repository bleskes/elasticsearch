/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2010     *
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

package demo.app.dao;

import java.util.Date;
import java.util.List;

import demo.app.data.DataSourceType;
import demo.app.data.TimeSeriesDataPoint;


/**
 * Interface defining the methods to be implemented by a Data Access Object
 * to obtain time series data from the Prelert database.
 * 
 * @author Pete Harverson
 */
public interface TimeSeriesDAO
{
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
	 * @param attributeName optional name of attribute on which to filter.
	 * @param attributeValue value of optional attribute on which to filter.
	 * @param includeFeatures flag indicating whether time series features should
	 * 	be included in the returned data points.
	 * @return list of time series data points.
	 */
	public List<TimeSeriesDataPoint> getDataPoints(String dataType, String metric, 
			Date minTime, Date maxTime, String source, 
			String attributeName, String attributeValue, boolean includeFeatures);
	
	
	/**
	 * Returns the time series data points for the given metric between the supplied
	 * start and end times, with the data aggregated for a daily display.
	 * @param dataType type of time series data e.g. system_udp or p2psmon_servers.
	 * @param metric the time series metric.
	 * @param minTime minimum date/time to include.
	 * @param maxTime maximum date/time to include.
	 * @param source optional source name.
	 * @param attributeName optional name of attribute on which to filter.
	 * @param attributeValue value of optional attribute on which to filter.
	 * @return list of time series data points.
	 */
	public List<TimeSeriesDataPoint> getDataPointsForDay(String dataType, String metric, 
			Date minTime, Date maxTime, String source, 
			String attributeName, String attributeValue);
	
	
	/**
	 * Returns the time series data points for the given metric between the supplied
	 * start and end times, with the data aggregated for a weekly display.
	 * @param dataType type of time series data e.g. system_udp or p2psmon_servers.
	 * @param metric the time series metric.
	 * @param minTime minimum date/time to include.
	 * @param maxTime maximum date/time to include.
	 * @param source optional source name.
	 * @param attributeName optional name of attribute on which to filter.
	 * @param attributeValue value of optional attribute on which to filter.
	 * @return list of time series data points.
	 */
	public List<TimeSeriesDataPoint> getDataPointsForWeek(String dataType, String metric, 
			Date minTime, Date maxTime, String source, 
			String attributeName, String attributeValue);
	
	
	/**
	 * Returns the time series data points for the given metric between the supplied
	 * start and end times. The data is aggregated according to the span between the
	 * supplied dates so that a maximum of 500 points will be returned.
	 * @param dataType type of time series data e.g. system_udp or p2psmon_servers.
	 * @param metric the time series metric.
	 * @param minTime minimum date/time to include.
	 * @param maxTime maximum date/time to include.
	 * @param source optional source name.
	 * @param attributeName optional name of attribute on which to filter.
	 * @param attributeValue value of optional attribute on which to filter.
	 * @param includeFeatures flag indicating whether time series features should
	 * 	be included in the returned data points.
	 * @return list of time series data points.
	 */
	public List<TimeSeriesDataPoint> getDataPointsForTimeSpan(String dataType, String metric, 
			Date minTime, Date maxTime, String source, 
			String attributeName, String attributeValue, boolean includeFeatures);
	
	
	/**
	 * Returns the date/time of the earliest record in the database for the
	 * time series with the specified data type.
	 * @param dataType type of time series data e.g. system_udp or p2psmon_servers.
	 * @param source optional source name.
	 * @return date/time of earliest record.
	 */
	public Date getEarliestTime(String dataType, String source);
	
	
	/**
	 * Returns the date/time of the latest record in the database for the
	 * time series with the specified data type.
	 * @param dataType type of time series data e.g. system_udp or p2psmon_servers.
	 * @param source optional source name.
	 * @return date/time of latest record.
	 */
	public Date getLatestTime(String dataType, String source);
}
