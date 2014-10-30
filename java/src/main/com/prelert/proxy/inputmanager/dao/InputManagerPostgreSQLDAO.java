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

package com.prelert.proxy.inputmanager.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcDaoSupport;

import com.prelert.dao.spring.ExternalTimeSeriesDetailsRowMapper;
import com.prelert.data.Attribute;
import com.prelert.data.ExternalTimeSeriesDetails;
import com.prelert.data.TimeSeriesInterpretation;
import com.prelert.server.ServerUtil;


public class InputManagerPostgreSQLDAO extends SimpleJdbcDaoSupport implements InputManagerDAO 
{
	static Logger s_Logger = Logger.getLogger(InputManagerPostgreSQLDAO.class);
	
	private static RowMapper<Integer> s_IntRowMapper = new RowMapper<Integer>() 
	{
		@Override
	    public Integer mapRow(ResultSet rs, int rowNum) throws SQLException
	    {	
			return rs.getInt("id");
	    }
	};


	@Override
	public int addExternalTimeSeriesType(String datatype, String metric,
									TimeSeriesInterpretation interp, String graphName,
									String graphTitle, String graphYAxisLabel, int usualInterval,
									String plugin)
	{
		String query = "select * from add_external_time_series_type(?, ?, ?, ?, ?, ?, ?, ?)";
		s_Logger.trace(query + " datatype=" + datatype + ", metric=" + metric);
		
		try
		{
			List<Integer> ids = getSimpleJdbcTemplate().query(query, s_IntRowMapper, datatype, 
										metric, interp.toString().toLowerCase(), 
										graphName, graphTitle, graphYAxisLabel, usualInterval, plugin);

			if (ids.size() > 0)
			{
				s_Logger.trace("add_external_time_series_type() returning 1 id");
				return ids.get(0);
			}
			else
			{
				s_Logger.trace("add_external_time_series_type() returning no matching id");
				return -1;
			}
		}
		catch(DataAccessException e)
		{
			s_Logger.error("addExternalTimeSeriesType ", e);
			return -1;
		}

	}

	@Override
	public int addExternalTimeSeries(String datatype, String metric,
										String key) 
	{
		String query = "select * from add_external_time_series_key(?, ?, ?)";

		// Log at the trace level as this can be printed hundreds of thousands
		// of times
		s_Logger.trace(query);
		
		return getSimpleJdbcTemplate().queryForInt(query, datatype, metric, key);
	}


	@Override
	public List<Integer> externalTimeSeriesIdsForType(String datatype, String metric)
	{
		String query = "select * from external_time_series_ids_for_type(?, ?)";
		s_Logger.trace(query);	
		
		return getSimpleJdbcTemplate().query(query, s_IntRowMapper, datatype, metric);		
	}


	@Override
	public boolean setExternalTimeSeriesDetails(int timeSeriesId, String source, 
											List<Attribute> attributes)
	{
		return setExternalTimeSeriesDetails(timeSeriesId, source, attributes,
									ServerUtil.METRIC_PATH_SOURCE_PREFIX,
									0,
									ServerUtil.METRIC_PATH_METRIC_PREFIX,
									ServerUtil.METRIC_PATH_DELIMITER);
	}
	
	@Override
	public boolean setExternalTimeSeriesDetails(int timeSeriesId, String source, 
			List<Attribute> attributes,
			String sourcePrefix, int sourcePosition,
			String metricPrefix,
			String metricPathDelimiter)
	{
		String query = "select * from set_external_time_series_details(?, ?, ?, ?, ?)";
		
		String delimiter = ServerUtil.delimiterForAttributes(attributes); 
		String attributeStr = ServerUtil.prepareAttributeNameValueArgument(delimiter, attributes);
		String metricPath = ServerUtil.prepareMetricPath(delimiter, attributes,
										sourcePrefix, sourcePosition, metricPathDelimiter, metricPrefix);
		
		String debugQuery = "select * from set_external_time_series_details({0}, {1}, {2}, {3}, {4})";
		debugQuery = MessageFormat.format(debugQuery, timeSeriesId, source, delimiter,
										attributeStr, metricPath);
		
		s_Logger.trace("setExternalTimeSeriesDetails() query: " + debugQuery);

		return getSimpleJdbcTemplate().queryForObject(
								query, Boolean.class, timeSeriesId, source, delimiter,
								attributeStr, metricPath);
	}
	
	
	@Override
	public List<ExternalTimeSeriesDetails> getExternalTimeSeriesDetails(boolean activeOnly)
	{
		String query = "select * from get_current_external_time_series(?)";
		
		String debugQuery = "select * from get_current_external_time_series({0})";
		debugQuery = MessageFormat.format(debugQuery, activeOnly);
		
		return getSimpleJdbcTemplate().query(query,
				new ExternalTimeSeriesDetailsRowMapper(), activeOnly);
	}
	
	
	@Override
	public boolean setExternalTimeSeriesActive(List<Integer> timeSeriesIds)
	{
		String query = "select * from set_external_time_series_active(?, ?)";
		
		try 
		{
			PreparedStatement statement = getConnection().prepareStatement(query);
			for (Integer id : timeSeriesIds)
			{
				statement.setInt(1, id);
				statement.setBoolean(2, true);
				statement.execute();
			}
		} 
		catch (CannotGetJdbcConnectionException e) 
		{
			s_Logger.error("Error setting external time series active", e);
		} 
		catch (SQLException e) 
		{
			s_Logger.error("Error setting external time series active", e);
		}
		

		return true;
	}

}
