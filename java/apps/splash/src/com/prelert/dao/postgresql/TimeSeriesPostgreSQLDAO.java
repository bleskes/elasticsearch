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

package com.prelert.dao.postgresql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;

import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.prelert.dao.SpringJdbcTemplateDAO;
import com.prelert.dao.TimeSeriesDAO;
import com.prelert.dao.TimeSeriesDataPointRowMapper;
import com.prelert.data.Attribute;
import com.prelert.data.DataSourceType;
import com.prelert.data.Evidence;
import com.prelert.data.Severity;
import com.prelert.data.TimeFrame;
import com.prelert.data.TimeSeriesDataPoint;
import com.prelert.server.ServerUtil;


/**
 * Implementation for a PostgreSQL database of the TimeSeriesDAO interface which 
 * uses calls to functions to obtain time series data.
 * @author Pete Harverson
 */
public class TimeSeriesPostgreSQLDAO extends SpringJdbcTemplateDAO implements
        TimeSeriesDAO
{
	static Logger logger = Logger.getLogger(TimeSeriesPostgreSQLDAO.class);
	
	
	@Override
	public List<String> getSourcesOrderByName(DataSourceType dataSourceType)
	{
		String query = "select * from data_sources_by_name(?, ?)";
		logger.debug("getSourcesOrderByName() query: " + query);
		
		RowMapper<String> mapper = new RowMapper<String>(){

			@Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException
            {
				return rs.getString("source");
            }
		};
		
		return m_SimpleJdbcTemplate.query(query, mapper, dataSourceType.getName(), 
				dataSourceType.getDataCategory().toString());
	}
	
	
	@Override
	public List<String> getSourcesOrderByCount(DataSourceType dataSourceType)
	{
		String query = "select * from data_sources_by_count(?, ?)";
		logger.debug("getSourcesOrderByCount() query: " + query);
		
		RowMapper<String> mapper = new RowMapper<String>(){

			@Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException
            {
				return rs.getString("source");
            }
		};
		
		return m_SimpleJdbcTemplate.query(query, mapper, dataSourceType.getName(), 
				dataSourceType.getDataCategory().toString());
	}
	

	@Override
	public List<String> getAttributeNames(String dataType)
	{
		String query = "select * from time_series_attribute_names(?)";
		
		String debugQuery = "select * from time_series_attribute_names({0})";
		debugQuery = MessageFormat.format(debugQuery, dataType);
		logger.debug("getAttributeNames() query: " + debugQuery);
		
		RowMapper<String> mapper = new RowMapper<String>(){

			@Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException
            {
				return rs.getString("name");
            }
		};
		
		return m_SimpleJdbcTemplate.query(query, mapper, dataType);
	}


	@Override
	public List<String> getAttributeValues(String dataType,
	        String attributeName, String source)
	{
		String query = "select * from time_series_attribute_values(?, ?, ?)";
		
		String debugQuery = "select * from time_series_attribute_values({0}, {1}, {2})";
		debugQuery = MessageFormat.format(debugQuery, dataType, attributeName, source);
		logger.debug("getAttributeValues() query: " + debugQuery);
		
		RowMapper<String> mapper = new RowMapper<String>(){

			@Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException
            {
				return rs.getString("value");
            }
		};
		
		return m_SimpleJdbcTemplate.query(query, mapper, dataType, attributeName, source);
	}
	
	
	@Override
	public List<String> getMetrics(String dataType)
	{
		String query = "select * from time_series_metric_list(?)";
		
		String debugQuery = "select * from time_series_metric_list({0})";
		debugQuery = MessageFormat.format(debugQuery, dataType);
		logger.debug("getMetrics() query: " + debugQuery);
		
		RowMapper<String> mapper = new RowMapper<String>(){

			@Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException
            {
				return rs.getString("metric");
            }
		};
		
		return m_SimpleJdbcTemplate.query(query, mapper, dataType);
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
		logger.debug("getDataPoints() query: " + debugQuery);
		
		return m_SimpleJdbcTemplate.query(query, 
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
		logger.debug("getDataPointsForDay() query: " + debugQuery);
		
		return m_SimpleJdbcTemplate.query(query, new TimeSeriesDataPointRowMapper("value" , false), 
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
		logger.debug("getDataPointsForWeek() query: " + debugQuery);
		
		return m_SimpleJdbcTemplate.query(query, new TimeSeriesDataPointRowMapper("value", false), 
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
		logger.debug("getDataPointsForTimeSpan() query: " + debugQuery);
		
		List<TimeSeriesDataPoint> dataPoints = m_SimpleJdbcTemplate.query(query, 
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
	public Date getEarliestTime(String dataType, String source)
	{
		String query = "select * from time_series_min_time(?, ?)";
		return m_SimpleJdbcTemplate.queryForObject(
				query, java.sql.Timestamp.class, dataType, source);
	}


	@Override
	public Date getLatestTime(String dataType, String source)
	{
		String query = "select * from time_series_max_time(?, ?)";
		return m_SimpleJdbcTemplate.queryForObject(
				query, java.sql.Timestamp.class, dataType, source);
	}
	
	
	/**
	 * Returns the data model for a time series feature with the specified id.
	 * @param id the unique identifier for the time series feature.
	 * @return full data model for the evidence item with the specified id.
	 */
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public Evidence getFeature(int id)
	{
		String query = "select * from evidence_single_complete(?);";
		
		return m_SimpleJdbcTemplate.queryForObject(
				query, new FeatureEvidenceRowMapper(), id);
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
	
	
	public static void main(String[] args)
	{
		// main() method for standalone testing.
		
		TimeSeriesPostgreSQLDAO timeSeriesDAO = new TimeSeriesPostgreSQLDAO();
		
		// Initialise connection.
		Properties connectionProps = new Properties();
		connectionProps.setProperty("driverClassName", "org.postgresql.Driver");
		connectionProps.setProperty("url", "jdbc:postgresql://localhost:5432/statestreet");
		connectionProps.setProperty("username", "postgres");
		connectionProps.setProperty("password", "root123");
		connectionProps.setProperty("defaultAutoCommit", "false");
		
		javax.sql.DataSource dataSource = null;
        try
        {
	        dataSource = BasicDataSourceFactory.createDataSource(connectionProps);
	        timeSeriesDAO.setDataSource(dataSource);
	        logger.debug("Initialised PostgreSQL datasource");
	
	        logger.debug("+++ getFeature() +++");
	        Evidence evidence = timeSeriesDAO.getFeature(20087);
	        logger.debug("Feature ID 20087 is: " + evidence.getDescription());
	        
	        GregorianCalendar startTime = new GregorianCalendar(2010, 3, 8, 14, 34);
	        GregorianCalendar endTime = new GregorianCalendar(2010, 3, 8, 15, 34);
	        List<Attribute> attributes = new ArrayList<Attribute>();
	        attributes.add(new Attribute("service", "IDN_RDF"));
	        
	        logger.debug("+++ getDataPoints() +++");
	        List<TimeSeriesDataPoint> dataPoints = timeSeriesDAO.getDataPoints(
	        		"p2psmon_servers", "active", startTime.getTime(), endTime.getTime(), "aglas119", attributes, true);
	        logger.debug("Number of points: " + dataPoints.size());
	        
	        logger.debug("+++ getDataPointsForDay() +++");
	        startTime = new GregorianCalendar(2010, 3, 7);
	     	endTime = new GregorianCalendar(2010, 3, 8);
	        dataPoints = timeSeriesDAO.getDataPointsForDay(
	        		"p2psmon_servers", "active", startTime.getTime(), endTime.getTime(), "aglas119", null);
	        logger.debug("Number of points: " + dataPoints.size());
	        
	        logger.debug("+++ getDataPointsForWeek() +++");
	        startTime = new GregorianCalendar(2010, 3, 1);
	     	endTime = new GregorianCalendar(2010, 3, 8);
	     	dataPoints = timeSeriesDAO.getDataPointsForWeek(
	        		"p2psmon_servers", "active", startTime.getTime(), endTime.getTime(), "aglas119", attributes);
	        logger.debug("Number of points: " + dataPoints.size());
	        
        }
        catch (Exception e)
        {
	        e.printStackTrace();
        }
		
		
	}

}
