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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import com.prelert.data.UsageRecord;

/**
 * Implementation for a MySQL database of the UsageViewDAO interface which 
 * predominantly uses calls to stored procedures to obtain information for 
 * User Usage views.
 * @author Pete Harverson
 */
public class UserUsageViewMySQLDAO extends SpringJdbcTemplateDAO implements UsageViewDAO
{
	
	static Logger logger = Logger.getLogger(UserUsageViewMySQLDAO.class);
	

	/**
	 * Returns the list of available 'servers' (sources) of Usage data.
	 * @return the list of available sources.
	 */
	public List<String> getSources()
	{
		String query = "select source from usage_users_sources order by source ASC";
		logger.debug("getSources() query: " + query);
		
		ParameterizedRowMapper<String> mapper = new ParameterizedRowMapper<String>(){

			@Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException
            {
				return rs.getString("source");
            }
		};
		
		return m_SimpleJdbcTemplate.query(query, mapper);
	}
	
	
	/**
	 * Returns the list of usernames for the specified source (server).
	 * @return list of usernames for the given source.
	 */
	public List<String> getUsers(String source)
	{
    	StringBuilder qryBlder = new StringBuilder();
    	qryBlder.append("select distinct username from usage_users_usernames ");
    	if (source != null)
    	{
    		qryBlder.append("WHERE source = ? ");
    	}
    	qryBlder.append("order by username ASC");
    	String query = qryBlder.toString();
		logger.debug("getUsers() running query: " + query);
		
		ParameterizedRowMapper<String> mapper = new ParameterizedRowMapper<String>(){

			@Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException
            {
				return rs.getString("username");
            }
		};
		
		List<String> users = null;
		if (source != null)
		{
			users = m_SimpleJdbcTemplate.query(query, mapper, source);
		}
		else
		{
			users = m_SimpleJdbcTemplate.query(query, mapper);
		}
		
		return users;
	}


	/**
	 * Returns the total user usage for the last week from all servers.
	 * @param metric name of usage metric to return data on e.g. total.
	 * @return list of total user usage data from all servers.
	 */
	public List<UsageRecord> getWeeklyUsageData(String metric)
	{
		return getWeeklyUsageData(metric, null, null);
	}


	/**
     * Returns the usage data for the last week for the given source (server).
     * @param metric name of usage metric to return data on e.g. total.
     * @param source the name of the source (server) for which to obtain usage data.
     * @return list of usage records, in ascending time order, for the given source.
     */
	public List<UsageRecord> getWeeklyUsageData(String metric, String source)
	{
		return getWeeklyUsageData(metric, source, null);
	}


    /**
     * Returns the usage data for the last week for the specified username on 
     * the given source (server).
     * @param metric name of usage metric to return data on e.g. total.
     * @param source the name of the source (server) for which to obtain usage data.
     * @param username name of the user for which to obtain usage data.
     * @return list of usage records, in ascending time order, for the given source and user.
     */
	public List<UsageRecord> getWeeklyUsageData(String metric, String source, String username)
	{	
		// Go between the latest usage date, and one week before.
		List<UsageRecord> weekRecords = null;
		Date latestUsageDate = getLatestUsageDate();
		
		if (latestUsageDate != null)
		{
			// Query calls a stored procedure in the form:
	    	// CALL usage_users_week_query(
			// 	'total', ADDTIME(?, '-144:00:0'), ADDTIME(?, '24:00:0'), 'source', 'user');
			String query = "CALL usage_users_week_query(?, ADDTIME(?, '-144:0:0'), ADDTIME(?, '24:0:0'), ?, ?)";
			
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 00:00");
			String weekEnding = formatter.format(latestUsageDate);
			
			String debugQuery = "CALL usage_users_week_query({0}, ADDTIME({1}, '-144:0:0'), " +
				"ADDTIME({2}, '24:0:0'), {3}, {4})";
			debugQuery = MessageFormat.format(
					debugQuery, metric, weekEnding, weekEnding, source, username);
			logger.debug("getWeeklyUsageData() query: " + debugQuery);
			
			weekRecords = m_SimpleJdbcTemplate.query(query, new UsageRecordRowMapper(metric), 
					metric, weekEnding, weekEnding, source, username);
		}
		else
		{
			weekRecords = new ArrayList<UsageRecord>(0);
		}
		
		return weekRecords;

	}
	
	
	/**
	 * Returns the total user usage from all servers for the specified day.
	 * @param date day for which to obtain user usage data.
	 * @param metric name of usage metric to return data on e.g. total.
	 * @return list of total user usage data from all servers.
	 */
    public List<UsageRecord> getDailyUsageData(Date date, String metric)
    {
    	return getDailyUsageData(date, metric, null, null);
    }



	/**
     * Returns the usage data for the specified day for the given source (server).
     * @param date day for which to obtain user usage data.
     * @param metric name of usage metric to return data on e.g. total.
     * @param source the name of the source (server) for which to obtain usage data.
     * @return list of usage records, in ascending time order, for the given source.
     */
    public List<UsageRecord> getDailyUsageData(Date date, String metric, String source)
    {
    	return getDailyUsageData(date, metric, source, null);
    }
	
	
    /**
     * Returns the usage data for the specified day for the specified username on 
     * the given source (server).
     * @param date day for which to obtain user usage data.
     * @param metric name of usage metric to return data on e.g. total.
     * @param source the name of the source (server) for which to obtain usage data.
     * @param username name of the user for which to obtain usage data.
     * @return list of usage records, in ascending time order, for the given source and user.
     */
    public List<UsageRecord> getDailyUsageData(Date date, String metric, String source,
            String username)
    {
    	// Query calls a stored procedure in the form:
    	// CALL usage_users_day_query('total', ?, ADDTIME(?, '24:0:0'), 'source', 'user');
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 00:00");
		String day = formatter.format(date);
		
		String query = "CALL usage_users_day_query(?, ?, ADDTIME(?, '24:0:0'), ?, ?)";
		
		String debugQuery = "CALL usage_users_day_query({0}, {1}, " +
			"ADDTIME({2}, '24:0:0'), {3}, {4})";
		debugQuery = MessageFormat.format(
			debugQuery, metric, day, day, source, username);
		logger.debug("getDailyUsageData() query: " + debugQuery);
		
		return m_SimpleJdbcTemplate.query(query, new UsageRecordRowMapper(metric), 
				metric, day, day, source, username);
    }


	/**
	 * Returns the total user usage from all servers for the specified hour.
	 * @param date hour for which to obtain user usage data.
	 * @param metric name of usage metric to return data on e.g. total.
	 * @return list of total user usage data from all servers.
	 */
    public List<UsageRecord> getHourlyUsageData(Date date, String metric)
    {
    	return getHourlyUsageData(date, metric, null, null);
    }


	/**
     * Returns the usage data for the specified hour for the given source (server).
     * @param date hour for which to obtain user usage data.
     * @param metric name of usage metric to return data on e.g. total.
     * @param source the name of the source (server) for which to obtain usage data.
     * @return list of usage records, in ascending time order, for the given source.
     */
    public List<UsageRecord> getHourlyUsageData(Date date, String metric, String source)
    {
    	return getHourlyUsageData(date, metric, source, null);
    }


    /**
     * Returns the usage data for the specified hour for the specified username on 
     * the given source (server).
     * @param date hour for which to obtain user usage data.
     * @param metric name of usage metric to return data on e.g. total.
     * @param source the name of the source (server) for which to obtain usage data.
     * @param username name of the user for which to obtain usage data.
     * @return list of usage records, in ascending time order, for the given source and user.
     */
    public List<UsageRecord> getHourlyUsageData(Date date, String metric, String source,
            String username)
    {
    	// Query calls a stored procedure in the form:
    	// CALL usage_users_query('total', ?, ADDTIME(?, '1:0:0'), 'source', 'user');
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:00");
		String hour = formatter.format(date);
		
		String query = "CALL usage_users_query(?, ?, ADDTIME(?, '1:0:0'), ?, ?)";
		
		String debugQuery = "CALL usage_users_query({0}, {1}, " +
			"ADDTIME({2}, '1:0:0'), {3}, {4})";
		debugQuery = MessageFormat.format(
			debugQuery, metric, hour, hour, source, username);
		logger.debug("getHourlyUsageData() query: " + debugQuery);
		
		return m_SimpleJdbcTemplate.query(query, new UsageRecordRowMapper(metric), 
				metric, hour, hour, source, username);
    }
	
	
	/**
	 * Returns the date of the earliest usage record in the Prelert database.
	 * @return date of earliest usage record.
	 */
    public Date getEarliestUsageDate()
    {
		String query = "select MIN(time) from usage_users;";
		return m_SimpleJdbcTemplate.queryForObject(query, java.sql.Timestamp.class);
    }


	/**
	 * Returns the date of the latest usage record in the Prelert database.
	 * @return date of latest usage record.
	 */
    public Date getLatestUsageDate()
    {
		String query = "select MAX(time) from usage_users;";
		return m_SimpleJdbcTemplate.queryForObject(query, java.sql.Timestamp.class);
    }

	
	/**
	 * ParameterizedRowMapper class for mapping usage query result sets to
	 * Usage records.
	 */
	class UsageRecordRowMapper implements ParameterizedRowMapper<UsageRecord>
	{
		private String m_Metric;
		
		public UsageRecordRowMapper(String metric)
		{
			m_Metric = metric;
		}

		
		@Override
        public UsageRecord mapRow(ResultSet rs, int rowNum) throws SQLException
        {
			java.sql.Timestamp time = rs.getTimestamp("time");
			BigDecimal value = rs.getBigDecimal(m_Metric);

			UsageRecord	record = new UsageRecord();
			record.set("time", time);
			record.set("value", value.intValue());		

            return record;
        }
		
	}
	
}
