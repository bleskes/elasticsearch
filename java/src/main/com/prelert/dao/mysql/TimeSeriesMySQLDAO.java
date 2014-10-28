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

package com.prelert.dao.mysql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcDaoSupport;

import com.prelert.dao.TimeSeriesDAO;
import com.prelert.data.Attribute;
import com.prelert.data.DataSourceType;
import com.prelert.data.Evidence;
import com.prelert.data.MetricPath;
import com.prelert.data.Severity;
import com.prelert.data.TimeFrame;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.TimeSeriesDataPoint;
import com.prelert.dao.spring.MetricPathRowMapper;
import com.prelert.dao.spring.TimeSeriesConfigRowMapper;
import com.prelert.dao.spring.TimeSeriesDataPointRowMapper;
import com.prelert.server.ServerUtil;


/**
 * Implementation for a MySQL database of the TimeSeriesDAO interface which 
 * predominantly uses calls to stored procedures to obtain time series data.
 * @author Pete Harverson
 */
public class TimeSeriesMySQLDAO extends SimpleJdbcDaoSupport implements TimeSeriesDAO
{
	static Logger s_logger = Logger.getLogger(TimeSeriesMySQLDAO.class);


	@Override
	public boolean isAggregationSupported(String dataType)
	{
		String query = "CALL time_series_supports_aggregation(?)";
		
		String debugQuery = "CALL time_series_supports_aggregation({0})";
		debugQuery = MessageFormat.format(debugQuery, dataType);
		s_logger.debug("isAggregationSupported() query: " + debugQuery);
		
		// MySQL has no boolean value, so check is:
		// value is zero - aggregation not supported
		// value non-zero - aggregation supported
		int supportedAsInt = getSimpleJdbcTemplate().queryForInt(query, dataType);
		return (supportedAsInt != 0);
	}


	/**
	 * Returns the list of available sources of time series data,
	 * ordered by name, for the given data type.
	 * @param dataType type of time series data e.g. system_udp or p2psmon_servers.
	 * @return the list of available sources.
	 */
	@Override
	public List<String> getSourcesOrderByName(DataSourceType dataSourceType)
	{
		String query = "CALL data_sources_by_name(?, ?)";
		s_logger.debug("getSourcesOrderByName() query: " + query);
		
		RowMapper<String> mapper = new RowMapper<String>(){

			@Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException
            {
				return rs.getString("source");
            }
		};
		
		return getSimpleJdbcTemplate().query(query, mapper, dataSourceType.getName(), 
				dataSourceType.getDataCategory().toString());
	}
	
	
	/**
	 * Returns the list of available sources of time series data,
	 * ordered by data point count, for the given data type.
	 * @param dataType type of time series data e.g. system_udp or p2psmon_servers.
	 * @return the list of available sources.
	 */
	@Override
	public List<String> getSourcesOrderByCount(DataSourceType dataSourceType)
	{
		String query = "CALL data_sources_by_count(?, ?)";
		s_logger.debug("getSourcesOrderByCount() query: " + query);
		
		RowMapper<String> mapper = new RowMapper<String>(){

			@Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException
            {
				return rs.getString("source");
            }
		};
		
		return getSimpleJdbcTemplate().query(query, mapper, dataSourceType.getName(), 
				dataSourceType.getDataCategory().toString());
	}
	
	
	/**
	 * Returns a list of the names of the attributes associated with the given
	 * time series data type.
	 * @param dataType type of time series data e.g. system_udp or p2psmon_servers.
	 * @return the list of attribute names associated with the data type, 
	 * 			ordered by name.
	 */
	@Override
	public List<String> getAttributeNames(String dataType)
	{
		String query = "CALL time_series_attribute_names(?)";
		
		String debugQuery = "CALL time_series_attribute_names({0})";
		debugQuery = MessageFormat.format(debugQuery, dataType);
		s_logger.debug("getAttributeNames() query: " + debugQuery);
		
		RowMapper<String> mapper = new RowMapper<String>(){

			@Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException
            {
				return rs.getString("name");
            }
		};
		
		return getSimpleJdbcTemplate().query(query, mapper, dataType);
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
	@Override
	public List<String>	getAttributeValues(String dataType, String attributeName, String source)
	{
    	String query = "CALL time_series_attribute_values(?, ?, ?)";
		
		String debugQuery = "CALL time_series_attribute_values({0}, {1}, {2})";
		debugQuery = MessageFormat.format(debugQuery, dataType, attributeName, source);
		s_logger.debug("getAttributeValues() query: " + debugQuery);
		
		RowMapper<String> mapper = new RowMapper<String>(){

			@Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException
            {
				return rs.getString("value");
            }
		};
		
		return getSimpleJdbcTemplate().query(query, mapper, dataType, attributeName, source);
	}
	
	
	/**
	 * Returns the list of metrics associated with the given time series data type.
	 * @param dataType type of time series data e.g. system_udp or p2psmon_servers.
	 * @return the list of metrics associated with the data type, ordered by name.
	 */
	@Override
	public List<String> getMetrics(String dataType)
	{
		String query = "CALL time_series_metric_list(?)";
		
		String debugQuery = "CALL time_series_metric_list({0})";
		debugQuery = MessageFormat.format(debugQuery, dataType);
		s_logger.debug("getMetrics() query: " + debugQuery);
		
		RowMapper<String> mapper = new RowMapper<String>(){

			@Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException
            {
				return rs.getString("metric");
            }
		};
		
		return getSimpleJdbcTemplate().query(query, mapper, dataType);
	}
	
	
	/**
	 * Returns the time series data points for the given metric between the supplied
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
	@Override
	public List<TimeSeriesDataPoint> getDataPoints(String dataType, String metric, 
			Date minTime, Date maxTime, String source, 
			List<Attribute> attributes, boolean includeFeatures)
	{
    	// Query calls a stored procedure in the form:
    	// CALL time_series_points_query(type, metric, minTime, maxTime, 
		//			optional_source, optional_attribute_name, optional_attribute_value);
		String query = "CALL time_series_points_query(?, ?, ?, ?, ?, ?, ?)";
		
		String attributeNames = ServerUtil.prepareAttributeNameArgument(attributes);
		String attributeValues = ServerUtil.prepareAttributeValueArgument(attributes);
		
		String debugQuery = "CALL time_series_points_query({0}, {1}, {2}, {3}, {4}, {5}, {6})";
		debugQuery = MessageFormat.format(debugQuery, dataType, metric, minTime,
				maxTime, source, attributeNames, attributeValues);
		s_logger.debug("getDataPoints() query: " + debugQuery);
		
		return getSimpleJdbcTemplate().query(query, 
				new TimeSeriesDataPointRowMapper(metric, includeFeatures), 
				dataType, metric, minTime, maxTime, source, attributeNames, attributeValues);
	}
	
	
	/**
	 * Returns the time series data points for the given metric between the supplied
	 * start and end times, with the data aggregated for a daily display.
	 * @param dataType type of time series data e.g. system_udp or p2psmon_servers.
	 * @param metric the time series metric.
	 * @param minTime minimum date/time to include.
	 * @param maxTime maximum date/time to include.
	 * @param source optional source name.
	 * @param attributes optional list of attributes.
	 * @return list of time series data points.
	 */
	public List<TimeSeriesDataPoint> getDataPointsForDay(String dataType, String metric, 
			Date minTime, Date maxTime, String source, List<Attribute> attributes)
	{
    	// Query calls a stored procedure in the form:
    	// CALL time_series_points_query_day(type, metric, minTime, maxTime, 
		//			optional_source, optional_attribute_names, optional_attribute_values);
		String query = "CALL time_series_points_query_day(?, ?, ?, ?, ?, ?, ?)";
		
		String attributeNames = ServerUtil.prepareAttributeNameArgument(attributes);
		String attributeValues = ServerUtil.prepareAttributeValueArgument(attributes);
		
		String debugQuery = "CALL time_series_points_query_day({0}, {1}, {2}, {3}, {4}, {5}, {6})";
		debugQuery = MessageFormat.format(debugQuery, dataType, metric, minTime,
				maxTime, source, attributeNames, attributeValues);
		s_logger.debug("getDataPointsForDay() query: " + debugQuery);
		
		return getSimpleJdbcTemplate().query(query, new TimeSeriesDataPointRowMapper(metric , false), 
				dataType, metric, minTime, maxTime, source, attributeNames, attributeValues);
	}
	
	
	/**
	 * Returns the time series data points for the given metric between the supplied
	 * start and end times, with the data aggregated for a weekly display.
	 * @param dataType type of time series data e.g. system_udp or p2psmon_servers.
	 * @param metric the time series metric.
	 * @param minTime minimum date/time to include.
	 * @param maxTime maximum date/time to include.
	 * @param source optional source name.
	 * @param attributes optional list of attributes.
	 * @return list of time series data points.
	 */
	@Override
	public List<TimeSeriesDataPoint> getDataPointsForWeek(String dataType, String metric, 
			Date minTime, Date maxTime, String source, List<Attribute> attributes)
	{
    	// Query calls a stored procedure in the form:
    	// CALL time_series_points_query_week(type, metric, minTime, maxTime, 
		//			optional_source, optional_attribute_names, optional_attribute_values);
		String query = "CALL time_series_points_query_week(?, ?, ?, ?, ?, ?, ?)";
		
		String attributeNames = ServerUtil.prepareAttributeNameArgument(attributes);
		String attributeValues = ServerUtil.prepareAttributeValueArgument(attributes);
		
		String debugQuery = "CALL time_series_points_query_week({0}, {1}, {2}, {3}, {4}, {5}, {6})";
		debugQuery = MessageFormat.format(debugQuery, dataType, metric, minTime,
				maxTime, source, attributeNames, attributeValues);
		s_logger.debug("getDataPointsForWeek() query: " + debugQuery);
		
		return getSimpleJdbcTemplate().query(query, new TimeSeriesDataPointRowMapper(metric, false), 
				dataType, metric, minTime, maxTime, source, attributeNames, attributeValues);
	}
	
	
	/**
	 * Returns the time series data points for the given metric between the supplied
	 * start and end times. The data is aggregated according to the span between the
	 * supplied dates so that a maximum of 500 points will be returned.
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
	@Override
	public List<TimeSeriesDataPoint> getDataPointsForTimeSpan(String dataType, String metric, 
			Date minTime, Date maxTime, String source, 
			List<Attribute> attributes, boolean includeFeatures)
	{
    	// Query calls a stored procedure in the form:
    	// CALL time_series_points_query(type, metric, minTime, maxTime, 
		//			optional_source, optional_attribute_names, optional_attribute_values);
		String query = "CALL time_series_points_query_adaptive(?, ?, ?, ?, ?, ?, ?)";
		
		String attributeNames = ServerUtil.prepareAttributeNameArgument(attributes);
		String attributeValues = ServerUtil.prepareAttributeValueArgument(attributes);
		
		String debugQuery = "CALL time_series_points_query_adaptive({0}, {1}, {2}, {3}, {4}, {5}, {6})";
		debugQuery = MessageFormat.format(debugQuery, dataType, metric, minTime,
				maxTime, source, attributeNames, attributeValues);
		s_logger.debug("getDataPointsForTimeSpan() query: " + debugQuery);
		
		List<TimeSeriesDataPoint> dataPoints = getSimpleJdbcTemplate().query(query, 
				new TimeSeriesDataPointRowMapper(metric, includeFeatures), 
				dataType, metric, minTime, maxTime, source, 
				attributeNames, attributeValues);
		
		if (includeFeatures == true)
		{
			Evidence feature;
			for (TimeSeriesDataPoint dataPoint : dataPoints)
			{
				feature = dataPoint.getFeature();
				if (feature != null)
				{
					feature = getFeature(feature.getId());
					dataPoint.setFeature(feature);
				}
			}
		}
		
		return dataPoints;
	}
	
	
	@Override
	public List<TimeSeriesDataPoint> getDataPointsRaw(
			int timeSeriesId, Date minTime, Date maxTime)
	{
    	// Query calls a stored procedure in the form:
    	// CALL time_series_points_query_raw(timeSeriesId, minTime, maxTime);
		String query = "CALL time_series_points_query_raw(?, ?, ?)";
		
		String debugQuery = "select * from time_series_points_query_raw({0}, {1}, {2})";
		debugQuery = MessageFormat.format(debugQuery, timeSeriesId, 
				ServerUtil.formatTimeField(minTime, TimeFrame.SECOND),
				ServerUtil.formatTimeField(maxTime, TimeFrame.SECOND));
		s_logger.debug("getDataPointsRaw() query: " + debugQuery);
		
		List<TimeSeriesDataPoint> dataPoints = getSimpleJdbcTemplate().query(query, 
				new TimeSeriesDataPointRowMapper("value", false), 
				timeSeriesId, minTime, maxTime);
		
		return dataPoints;
	}
	
	
	/**
	 * Returns the date/time of the latest record in the database for the
	 * time series with the specified data type.
	 * @param dataType type of time series data e.g. system_udp or p2psmon_servers.
	 * @param source optional source name.
	 * @return date/time of latest record.
	 */
	@Override
	public Date getLatestTime(String dataType, String source)
	{
		String query = "CALL time_series_max_time(?, ?)";
		return getSimpleJdbcTemplate().queryForObject(
				query, java.sql.Timestamp.class, dataType, source);
	}
	
	
	/**
	 * Returns the data model for a time series feature with the specified id.
	 * @param id the unique identifier for the time series feature.
	 * @return full data model for the evidence item with the specified id.
	 */
	@Override
	public Evidence getFeature(int id)
	{
		String query = "CALL evidence_single_complete(?);";

		return getSimpleJdbcTemplate().queryForObject(
				query, new TimeSeriesFeatureRowMapper(), id);
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
		String query = "CALL time_series_from_feature(?);";

		return getSimpleJdbcTemplate().queryForObject(
				query, new TimeSeriesConfigRowMapper(), id);
	}
	
	@Override
	public MetricPath getMetricPathFromTimeSeriesId(int id)
	{
		String query = "CALL metric_path_from_time_series_id(?)";
		
		String debugQuery = "CALL metric_path_from_time_series_id({0})";
		debugQuery = MessageFormat.format(debugQuery, id);
		s_logger.debug("getMetricPathFromTimeSeriesId() query: " + debugQuery);
		
		List<MetricPath> paths = getSimpleJdbcTemplate().query(query,
		        new MetricPathRowMapper(), id);

		if (paths.size() > 0)
		{
			return paths.get(0);
		}

		return null;
	}


	@Override
	public Integer getTimeSeriesIdFromExternalKey(String dataType, String externalKey)
	{
		String query = "CALL time_series_id_from_external_key(?, ?)";

		String debugQuery = "CALL time_series_id_from_external_key({0}, {1})";
		debugQuery = MessageFormat.format(debugQuery, dataType, externalKey);
		s_logger.debug("getTimeSeriesIdFromExternalKey() query: " + debugQuery);

		return getSimpleJdbcTemplate().queryForObject(
				query, Integer.class, dataType, externalKey);
	}
	

	@Override
	public String getExternalKeyFromTimeSeriesId(int timeSeriesId)
	{
		String query = "CALL external_key_from_time_series_id(?)";

		String debugQuery = "CALL external_key_from_time_series_id({0})";
		debugQuery = MessageFormat.format(debugQuery, timeSeriesId);
		s_logger.debug("getExternalKeyFromTimeSeriesId() query: " + debugQuery);

		return getSimpleJdbcTemplate().queryForObject(
				query, String.class, timeSeriesId);
	}
	
	
	@Override
	public String getExternalKeyFromTimeSeriesDetails(String datatype, String metric,
										String source, List<Attribute> attributes)
	{
		String query = "CALL external_key_from_time_series_details(?, ?, ?, ?, ?)";
		
		String attributeNames = ServerUtil.prepareAttributeNameArgument(attributes);
		String attributeValues = ServerUtil.prepareAttributeValueArgument(attributes);

		String debugQuery = "CALL external_key_from_time_series_details({0}, {1}, {2}, {3}, {4})";
		debugQuery = MessageFormat.format(debugQuery, datatype, metric, source, 
							attributeNames, attributeValues);
		
		s_logger.debug("getExternalKeyFromTimeSeriesDetails() query: " + debugQuery);

		return getSimpleJdbcTemplate().queryForObject(
								query, String.class, datatype, metric, source,
								attributeNames, attributeValues);
	}


	/**
	 * RowMapper class for mapping the result of a time series feature query to
	 * an item of Evidence.
	 */
	class TimeSeriesFeatureRowMapper implements RowMapper<Evidence>
	{

		@Override
        public Evidence mapRow(ResultSet rs, int rowNum) throws SQLException
        {
			int id = rs.getInt("id");
			java.sql.Timestamp time = rs.getTimestamp("time");
			String description = rs.getString("description");
			String severityStr = rs.getString("severity");
			String source = rs.getString("source");
			
			Evidence feature = new Evidence(id);
			feature.setTime(time);
			feature.setDescription(description);
			feature.setSource(source);
			
			try
			{
				feature.setSeverity(Enum.valueOf(Severity.class, severityStr.toUpperCase()));
			}
			catch (IllegalArgumentException e)
			{
				feature.setSeverity(Severity.NONE);
			}

            return feature;
        }
		
	}

}
