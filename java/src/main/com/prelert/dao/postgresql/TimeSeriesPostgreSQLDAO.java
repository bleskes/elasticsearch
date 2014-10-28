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

package com.prelert.dao.postgresql;

import java.sql.ResultSet;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
 * Implementation for a PostgreSQL database of the TimeSeriesDAO interface which 
 * uses calls to functions to obtain time series data.
 * @author Pete Harverson
 */
public class TimeSeriesPostgreSQLDAO extends SimpleJdbcDaoSupport implements
        TimeSeriesDAO
{
	static Logger s_Logger = Logger.getLogger(TimeSeriesPostgreSQLDAO.class);


	@Override
	public boolean isAggregationSupported(String dataType)
	{
		String query = "select * from time_series_supports_aggregation(?)";
		
		String debugQuery = "select * from time_series_supports_aggregation({0})";
		debugQuery = MessageFormat.format(debugQuery, dataType);
		s_Logger.debug("isAggregationSupported() query: " + debugQuery);
		
		return getSimpleJdbcTemplate().queryForObject(query, java.lang.Boolean.class, dataType);
	}


	@Override
	public List<String> getSourcesOrderByName(DataSourceType dataSourceType)
	{
		String query = "select * from data_sources_by_name(?, ?)";
		s_Logger.debug("getSourcesOrderByName() query: " + query);
		
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
	
	
	@Override
	public List<String> getSourcesOrderByCount(DataSourceType dataSourceType)
	{
		String query = "select * from data_sources_by_count(?, ?)";
		s_Logger.debug("getSourcesOrderByCount() query: " + query);
		
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
	

	@Override
	public List<String> getAttributeNames(String dataType)
	{
		String query = "select * from time_series_attribute_names(?)";
		
		String debugQuery = "select * from time_series_attribute_names({0})";
		debugQuery = MessageFormat.format(debugQuery, dataType);
		s_Logger.debug("getAttributeNames() query: " + debugQuery);
		
		RowMapper<String> mapper = new RowMapper<String>(){

			@Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException
            {
				return rs.getString("name");
            }
		};
		
		return getSimpleJdbcTemplate().query(query, mapper, dataType);
	}


	@Override
	public List<String> getAttributeValues(String dataType,
	        String attributeName, String source)
	{
		String query = "select * from time_series_attribute_values(?, ?, ?)";
		
		String debugQuery = "select * from time_series_attribute_values({0}, {1}, {2})";
		debugQuery = MessageFormat.format(debugQuery, dataType, attributeName, source);
		s_Logger.debug("getAttributeValues() query: " + debugQuery);
		
		RowMapper<String> mapper = new RowMapper<String>(){

			@Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException
            {
				return rs.getString("value");
            }
		};
		
		return getSimpleJdbcTemplate().query(query, mapper, dataType, attributeName, source);
	}
	
	
	@Override
	public List<String> getMetrics(String dataType)
	{
		String query = "select * from time_series_metric_list(?)";
		
		String debugQuery = "select * from time_series_metric_list({0})";
		debugQuery = MessageFormat.format(debugQuery, dataType);
		s_Logger.debug("getMetrics() query: " + debugQuery);
		
		RowMapper<String> mapper = new RowMapper<String>(){

			@Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException
            {
				return rs.getString("metric");
            }
		};
		
		return getSimpleJdbcTemplate().query(query, mapper, dataType);
	}

	@Override
	public List<TimeSeriesDataPoint> getDataPoints(String dataType,
	        String metric, Date minTime, Date maxTime, String source,
	        List<Attribute> attributes, boolean includeFeatures)
	{
    	// Function is:
    	// 	time_series_points_query(dataType, metric, minTime, maxTime, 
		//			optional_source, optional_attribute_name, optional_attribute_value);
		String query = "select * from time_series_points_query(?, ?, ?, ?, ?, ?, ?)";
		
		String attributeNames = ServerUtil.prepareAttributeNameArgument(attributes);
		String attributeValues = ServerUtil.prepareAttributeValueArgument(attributes);
		
		String debugQuery = "select * from time_series_points_query({0}, {1}, {2}, {3}, {4}, {5}, {6})";
		debugQuery = MessageFormat.format(debugQuery, dataType, metric, 
				ServerUtil.formatTimeField(minTime, TimeFrame.SECOND),
				ServerUtil.formatTimeField(maxTime, TimeFrame.SECOND),
				source, attributeNames, attributeValues);
		s_Logger.debug("getDataPoints() query: " + debugQuery);
		
		return getSimpleJdbcTemplate().query(query, 
				new TimeSeriesDataPointRowMapper("value", includeFeatures), 
				dataType, metric, minTime, maxTime, source, attributeNames, attributeValues);
	}


	@Override
	public List<TimeSeriesDataPoint> getDataPointsForDay(String dataType,
	        String metric, Date minTime, Date maxTime, String source,
	        List<Attribute> attributes)
	{
		// Function is:
    	// time_series_points_query_day(dataType, metric, minTime, maxTime, 
		//			optional_source, optional_attribute_names, optional_attribute_values);
		String query = "select * from time_series_points_query_day(?, ?, ?, ?, ?, ?, ?)";
		
		String attributeNames = ServerUtil.prepareAttributeNameArgument(attributes);
		String attributeValues = ServerUtil.prepareAttributeValueArgument(attributes);
		
		String debugQuery = "select * from time_series_points_query_day({0}, {1}, {2}, {3}, {4}, {5}, {6})";
		debugQuery = MessageFormat.format(debugQuery, dataType, metric, 
				ServerUtil.formatTimeField(minTime, TimeFrame.SECOND),
				ServerUtil.formatTimeField(maxTime, TimeFrame.SECOND), 
				source, attributeNames, attributeValues);
		s_Logger.debug("getDataPointsForDay() query: " + debugQuery);
		
		return getSimpleJdbcTemplate().query(query, new TimeSeriesDataPointRowMapper("value" , false), 
				dataType, metric, minTime, maxTime, source, attributeNames, attributeValues);
	}
	
	
	@Override
	public List<TimeSeriesDataPoint> getDataPointsForWeek(String dataType,
	        String metric, Date minTime, Date maxTime, String source,
	        List<Attribute> attributes)
	{
		// Function is:
    	// time_series_points_query_week(dataType, metric, minTime, maxTime, 
		//			optional_source, optional_attribute_names, optional_attribute_values);
		String query = "select * from time_series_points_query_week(?, ?, ?, ?, ?, ?, ?)";
		
		String attributeNames = ServerUtil.prepareAttributeNameArgument(attributes);
		String attributeValues = ServerUtil.prepareAttributeValueArgument(attributes);
		
		String debugQuery = "select * from time_series_points_query_week({0}, {1}, {2}, {3}, {4}, {5}, {6})";
		debugQuery = MessageFormat.format(debugQuery, dataType, metric, 
				ServerUtil.formatTimeField(minTime, TimeFrame.SECOND),
				ServerUtil.formatTimeField(maxTime, TimeFrame.SECOND),  
				source, attributeNames, attributeValues);
		s_Logger.debug("getDataPointsForWeek() query: " + debugQuery);
		
		return getSimpleJdbcTemplate().query(query, new TimeSeriesDataPointRowMapper("value", false), 
				dataType, metric, minTime, maxTime, source, attributeNames, attributeValues);
	}


	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public List<TimeSeriesDataPoint> getDataPointsForTimeSpan(String dataType,
	        String metric, Date minTime, Date maxTime, String source,
	        List<Attribute> attributes, boolean includeFeatures)
	{
		// Function is:
    	// time_series_points_query_adaptive(type, metric, minTime, maxTime, 
		//			optional_source, optional_attribute_names, optional_attribute_values);
		String query = "select * from time_series_points_query_adaptive(?, ?, ?, ?, ?, ?, ?)";
		
		String attributeNames = ServerUtil.prepareAttributeNameArgument(attributes);
		String attributeValues = ServerUtil.prepareAttributeValueArgument(attributes);
		
		String debugQuery = "select * from time_series_points_query_adaptive({0}, {1}, {2}, {3}, {4}, {5}, {6})";
		debugQuery = MessageFormat.format(debugQuery, dataType, metric, 
				ServerUtil.formatTimeField(minTime, TimeFrame.SECOND),
				ServerUtil.formatTimeField(maxTime, TimeFrame.SECOND),
				source, attributeNames, attributeValues);
		s_Logger.debug("getDataPointsForTimeSpan() query: " + debugQuery);
		
		List<TimeSeriesDataPoint> dataPoints = getSimpleJdbcTemplate().query(query, 
				new TimeSeriesDataPointRowMapper("value", includeFeatures), 
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
    	// Function is:
    	// time_series_points_query_raw(timeSeriesId, minTime, maxTime);
		String query = "select * from time_series_points_query_raw(?, ?, ?)";
		
		String debugQuery = "select * from time_series_points_query_raw({0}, {1}, {2})";
		debugQuery = MessageFormat.format(debugQuery, timeSeriesId, 
				ServerUtil.formatTimeField(minTime, TimeFrame.SECOND),
				ServerUtil.formatTimeField(maxTime, TimeFrame.SECOND));
		s_Logger.debug("getDataPointsRaw() query: " + debugQuery);
		
		List<TimeSeriesDataPoint> dataPoints = getSimpleJdbcTemplate().query(query, 
				new TimeSeriesDataPointRowMapper("value", false), 
				timeSeriesId, minTime, maxTime);
		
		return dataPoints;
	}
	

	@Override
	public Date getLatestTime(String dataType, String source)
	{
		String query = "select * from time_series_max_time(?, ?)";
		return getSimpleJdbcTemplate().queryForObject(
				query, java.sql.Timestamp.class, dataType, source);
	}


	/**
	 * Returns the data model for a time series feature with the specified id.
	 * @param id the unique identifier for the time series feature.
	 * @return full data model for the evidence item with the specified id.
	 */
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	@Override
	public Evidence getFeature(int id)
	{
		String query = "select * from evidence_single_complete(?);";

		return getSimpleJdbcTemplate().queryForObject(
				query, new FeatureEvidenceRowMapper(), id);
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
		String query = "SELECT * FROM time_series_from_feature(?);";

		return getSimpleJdbcTemplate().queryForObject(
				query, new TimeSeriesConfigRowMapper(), id);
	}
    
    
    @Override
	public MetricPath getMetricPathFromTimeSeriesId(int id)
	{
		String query = "select * from metric_path_from_time_series_id(?)";
		
		String debugQuery = "select * from metric_path_from_time_series_id({0})";
		debugQuery = MessageFormat.format(debugQuery, id);
		s_Logger.debug("getMetricPathFromTimeSeriesId() query: " + debugQuery);

		List<MetricPath> paths = getSimpleJdbcTemplate().query(query,
												new MetricPathRowMapper(), id);
		if (paths.size() > 0)
		{
			return paths.get(0);
		}
		else
		{
			return null;
		}
	}


	@Override
	public Integer getTimeSeriesIdFromExternalKey(String dataType, String externalKey)
	{
		String query = "select * from time_series_id_from_external_key(?, ?)";

		String debugQuery = "select * from time_series_id_from_external_key({0}, {1})";
		debugQuery = MessageFormat.format(debugQuery, dataType, externalKey);
		s_Logger.debug("getTimeSeriesIdFromExternalKey() query: " + debugQuery);

		return getSimpleJdbcTemplate().queryForObject(
				query, Integer.class, dataType, externalKey);
	}


	@Override
	public String getExternalKeyFromTimeSeriesId(int timeSeriesId)
	{
		String query = "select * from external_key_from_time_series_id(?)";

		String debugQuery = "select * from external_key_from_time_series_id({0})";
		debugQuery = MessageFormat.format(debugQuery, timeSeriesId);
		s_Logger.debug("getExternalKeyFromTimeSeriesId() query: " + debugQuery);

		return getSimpleJdbcTemplate().queryForObject(
				query, String.class, timeSeriesId);
	}


	@Override
	public String getExternalKeyFromTimeSeriesDetails(String datatype, String metric,
										String source, List<Attribute> attributes)
	{
		String query = "select * from external_key_from_time_series_details(?, ?, ?, ?, ?)";
		
		String attributeNames = ServerUtil.prepareAttributeNameArgument(attributes);
		String attributeValues = ServerUtil.prepareAttributeValueArgument(attributes);

		String debugQuery = "select * from external_key_from_time_series_details({0}, {1}, {2}, {3}, {4})";
		debugQuery = MessageFormat.format(debugQuery, datatype, metric, source, 
											attributeNames, attributeValues);
		
		s_Logger.debug("getExternalKeyFromTimeSeriesDetails() query: " + debugQuery);

		return getSimpleJdbcTemplate().queryForObject(
								query, String.class, datatype, metric, source,
								attributeNames, attributeValues);
	}

	/**
	 * RowMapper class for mapping the result of a time series feature query to
	 * an item of Evidence.
	 */
	class FeatureEvidenceRowMapper implements RowMapper<Evidence>
	{

		@Override
        public Evidence mapRow(ResultSet rs, int rowNum) throws SQLException
        {
			Evidence feature = new Evidence();
			
			ResultSet refCursor = (ResultSet) rs.getObject(1);
			while (refCursor.next())
			{
				int id = refCursor.getInt("id");
				java.sql.Timestamp time = refCursor.getTimestamp("time");
				String description = refCursor.getString("description");
				String severityStr = refCursor.getString("severity");
				String source = refCursor.getString("source");
				
				feature.setId(id);
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
				
			}
			
			return feature;
        }
		
	}

}
