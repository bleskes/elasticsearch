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

package com.prelert.dao;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.TimeSeriesDataPoint;


/**
 * Implementation for a MySQL database of the TimeSeriesDAO interface which 
 * predominantly uses calls to stored procedures to obtain time series data.
 * @author Pete Harverson
 */
public class TimeSeriesMySQLDAO extends SpringJdbcTemplateDAO implements TimeSeriesDAO
{

	static Logger logger = Logger.getLogger(TimeSeriesMySQLDAO.class);
	
	
	/**
	 * Returns the list of available sources of time series data,
	 * ordered by name, for the given data type.
	 * @param dataType type of time series data e.g. system_udp or p2psmon_servers.
	 * @return the list of available sources.
	 */
	public List<String> getSourcesOrderByName(DataSourceType dataSourceType)
	{
		String dbCategory = toDatabaseCategory(dataSourceType.getDataCategory());
		
		String query = "CALL data_sources_by_name(?, ?)";
		logger.debug("getSourcesOrderByName() query: " + query);
		
		ParameterizedRowMapper<String> mapper = new ParameterizedRowMapper<String>(){

			@Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException
            {
				return rs.getString("source");
            }
		};
		
		return m_SimpleJdbcTemplate.query(query, mapper, dataSourceType.getName(), 
				dbCategory);
	}
	
	
	/**
	 * Returns the list of available sources of time series data,
	 * ordered by data point count, for the given data type.
	 * @param dataType type of time series data e.g. system_udp or p2psmon_servers.
	 * @return the list of available sources.
	 */
	public List<String> getSourcesOrderByCount(DataSourceType dataSourceType)
	{
		String dbCategory = toDatabaseCategory(dataSourceType.getDataCategory());
		
		String query = "CALL data_sources_by_count(?, ?)";
		logger.debug("getSourcesOrderByCount() query: " + query);
		
		ParameterizedRowMapper<String> mapper = new ParameterizedRowMapper<String>(){

			@Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException
            {
				return rs.getString("source");
            }
		};
		
		return m_SimpleJdbcTemplate.query(query, mapper, dataSourceType.getName(), 
				dbCategory);
	}
	
	
	/**
	 * Returns a list of the distinct values for the attribute with the given name
	 * for the specified data type e.g. the values of the 'username' attribute
	 * for p2psmon_users data.
	 * @param dataType type of time series data e.g. system_udp or p2psmon_servers.
	 * @param attributeName name of attribute for which to return the values.
	 * @param source optional source name.
	 * @return a list of the distinct values for the attribute.
	 */
	public List<String>	getAttributeValues(String dataType, String attributeName, String source)
	{
    	String query = "CALL time_series_attribute_values(?, ?, ?)";
		
		String debugQuery = "CALL time_series_attribute_values({0}, {1}, {2})";
		debugQuery = MessageFormat.format(debugQuery, dataType, attributeName, source);
		logger.debug("getAttributeValues() query: " + debugQuery);
		
		ParameterizedRowMapper<String> mapper = new ParameterizedRowMapper<String>(){

			@Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException
            {
				return rs.getString("value");
            }
		};
		
		return m_SimpleJdbcTemplate.query(query, mapper, dataType, attributeName, source);
	}
	
	
	/**
	 * Returns the time series data points for the given metric between the supplied
	 * start and end times.
	 * @param dataType type of time series data e.g. system_udp or p2psmon_servers.
	 * @param metric the time series metric.
	 * @param minTime minimum date/time to include.
	 * @param maxTime maximum date/time to include.
	 * @param source optional source name.
	 * @param attributeName optional name of attribute on which to filter.
	 * @param attributeValue value of optional attribute on which to filter.
	 * @return list of time series data points.
	 */
	public List<TimeSeriesDataPoint> getDataPoints(String dataType, String metric, 
			Date minTime, Date maxTime, String source, 
			String attributeName, String attributeValue)
	{
    	// Query calls a stored procedure in the form:
    	// CALL time_series_points_query(type, metric, minTime, maxTime, 
		//			optional_source, optional_attribute_name, optional_attribute_value);
		String query = "CALL time_series_points_query(?, ?, ?, ?, ?, ?, ?)";
		
		String debugQuery = "CALL time_series_points_query({0}, {1}, {2}, {3}, {4}, {5}, {6})";
		debugQuery = MessageFormat.format(debugQuery, dataType, metric, minTime,
				maxTime, source, attributeName, attributeValue);
		logger.debug("getDataPoints() query: " + debugQuery);
		
		return m_SimpleJdbcTemplate.query(query, new TimeSeriesDataPointRowMapper(metric), 
				dataType, metric, minTime, maxTime, source, attributeName, attributeValue);
	}
	
	
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
			String attributeName, String attributeValue)
	{
    	// Query calls a stored procedure in the form:
    	// CALL time_series_points_query_day(type, metric, minTime, maxTime, 
		//			optional_source, optional_attribute_name, optional_attribute_value);
		String query = "CALL time_series_points_query_day(?, ?, ?, ?, ?, ?, ?)";
		
		String debugQuery = "CALL time_series_points_query_day({0}, {1}, {2}, {3}, {4}, {5}, {6})";
		debugQuery = MessageFormat.format(debugQuery, dataType, metric, minTime,
				maxTime, source, attributeName, attributeValue);
		logger.debug("getDataPointsForDay() query: " + debugQuery);
		
		return m_SimpleJdbcTemplate.query(query, new TimeSeriesDataPointRowMapper(metric), 
				dataType, metric, minTime, maxTime, source, attributeName, attributeValue);
	}
	
	
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
			String attributeName, String attributeValue)
	{
    	// Query calls a stored procedure in the form:
    	// CALL time_series_points_query_week(type, metric, minTime, maxTime, 
		//			optional_source, optional_attribute_name, optional_attribute_value);
		String query = "CALL time_series_points_query_week(?, ?, ?, ?, ?, ?, ?)";
		
		String debugQuery = "CALL time_series_points_query_week({0}, {1}, {2}, {3}, {4}, {5}, {6})";
		debugQuery = MessageFormat.format(debugQuery, dataType, metric, minTime,
				maxTime, source, attributeName, attributeValue);
		logger.debug("getDataPointsForWeek() query: " + debugQuery);
		
		return m_SimpleJdbcTemplate.query(query, new TimeSeriesDataPointRowMapper(metric), 
				dataType, metric, minTime, maxTime, source, attributeName, attributeValue);
	}
	
	
	/**
	 * Returns the date/time of the earliest record in the database for the
	 * time series with the specified data type.
	 * @param dataType type of time series data e.g. system_udp or p2psmon_servers.
	 * @param source optional source name.
	 * @return date/time of earliest record.
	 */
	public Date getEarliestTime(String dataType, String source)
	{
		String query = "CALL time_series_min_time(?, ?)";
		return m_SimpleJdbcTemplate.queryForObject(
				query, java.sql.Timestamp.class, dataType, source);
	}
	
	
	/**
	 * Returns the date/time of the latest record in the database for the
	 * time series with the specified data type.
	 * @param dataType type of time series data e.g. system_udp or p2psmon_servers.
	 * @param source optional source name.
	 * @return date/time of latest record.
	 */
	public Date getLatestTime(String dataType, String source)
	{
		String query = "CALL time_series_max_time(?, ?)";
		return m_SimpleJdbcTemplate.queryForObject(
				query, java.sql.Timestamp.class, dataType, source);
	}
	
	
	/**
	 * Converts the given DataSourceCategory enum to the String value used by the
	 * database to represent the data source category.
	 * @param category	the DataSourceCategory enum.
	 * @return the value used by the database e.g. 'time series'.
	 */
	protected String toDatabaseCategory(DataSourceCategory category)
	{
		String categoryStr = "";
		
		if (category != null)
		{
			categoryStr = category.toString().replace('_', ' ').toLowerCase();
		}
		
		return categoryStr;
	}
	
	
	/**
	 * ParameterizedRowMapper class for mapping usage query result sets to
	 * Usage records.
	 */
	class TimeSeriesDataPointRowMapper implements ParameterizedRowMapper<TimeSeriesDataPoint>
	{
		private String m_Metric;
		
		public TimeSeriesDataPointRowMapper(String metric)
		{
			m_Metric = metric;
		}

		
		@Override
        public TimeSeriesDataPoint mapRow(ResultSet rs, int rowNum) throws SQLException
        {
			java.sql.Timestamp time = rs.getTimestamp("time");
			BigDecimal value = rs.getBigDecimal(m_Metric);	

            return new TimeSeriesDataPoint(time, value.intValue());
        }
		
	}

}
