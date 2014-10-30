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
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import com.prelert.data.UsageRecord;

/**
 * Implementation of the UsageViewDAO interface which uses JDBC queries to
 * obtain information for Usage views from the Prelert database.
 * @author Pete Harverson
 */
public class UsageViewJdbcDAO extends SpringJdbcTemplateDAO implements UsageViewDAO
{
	
	static Logger logger = Logger.getLogger(UsageViewJdbcDAO.class);

	
	/**
	 * Returns the list of available 'servers' (sources) of Usage data.
	 * @return the list of available sources.
	 */
	public List<String> getSources()
	{
		String query = "select source from user_usage_sources order by source ASC";
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
		String query = "select username from user_usage_usernames " +
			"WHERE source = ? order by username ASC";
		logger.debug("getUsers() query: " + query);
		
		ParameterizedRowMapper<String> mapper = new ParameterizedRowMapper<String>(){

			@Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException
            {
				return rs.getString("username");
            }
		};
		
		return m_SimpleJdbcTemplate.query(query, mapper, source);
	}


	/**
	 * Returns the total user usage for the last week from all servers.
	 * @return list of total user usage data from all servers.
	 */
	public List<UsageRecord> getWeeklyUsageData(String metric)
	{
		String query = "select time, ? from user_usage_total order by time ASC";
		logger.debug("getWeeklyUsageData() query: " + query);
		
		return m_SimpleJdbcTemplate.query(query, new UsageRecordRowMapper(metric), metric);
	}


	/**
     * Returns the usage data for the last week for the given source (server).
     * @param source the name of the source (server) for which to obtain usage data.
     * @return list of usage records, in ascending time order, for the given source.
     */
	public List<UsageRecord> getWeeklyUsageData(String metric, String source)
	{
		String query = new String(
				"select time, ? from user_usage_source_total WHERE source = ? order by time ASC");
		logger.debug("getWeeklyUsageData() query: " + query);
		
		return m_SimpleJdbcTemplate.query(query, new UsageRecordRowMapper(metric), metric, source);
	}


    /**
     * Returns the usage data for the last week for the specified username on 
     * the given source (server).
     * @param source the name of the source (server) for which to obtain usage data.
     * @param username name of the user for which to obtain usage data.
     * @return list of usage records, in ascending time order, for the given source and user.
     */
	public List<UsageRecord> getWeeklyUsageData(String metric, 
			String source, String username)
	{
		String query = new String(
				"select time, ? from user_usage_source_username_total WHERE source = ? " +
				"AND username = ? order by time ASC");
		logger.debug("getWeeklyUsageData() query: " + query);
		
		return m_SimpleJdbcTemplate.query(query, new UsageRecordRowMapper(metric), 
				metric, source, username);
	}
	
	
	/**
	 * Returns the total user usage from all servers for the specified day.
	 * @param date day for which to obtain user usage data.
	 * @return list of total user usage data from all servers.
	 */
    public List<UsageRecord> getDailyUsageData(Date date, String metric)
    {
		// Query is in the form:
		// select * from user_usage_total WHERE time >= '2009-04-09 00:00' AND 
		// time <= ADDTIME('2009-04-09 00:00', '24:0:0') order by time ASC;
		StringBuilder qryBlder = new StringBuilder("select time, ? from user_usage_total ");
		qryBlder.append("WHERE time >= ? AND time <= ADDTIME(?, '24:0:0') order by time ASC");
		String query = qryBlder.toString();
		
		// Format the day parameter.
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 00:00");
		String day = formatter.format(date);
		
		logger.debug("getDailyUsageData() query: " + query);
		
		return m_SimpleJdbcTemplate.query(query, new UsageRecordRowMapper(metric), 
				metric, day, day);
    }



	/**
     * Returns the usage data for the specified day for the given source (server).
     * @param date day for which to obtain user usage data.
     * @param source the name of the source (server) for which to obtain usage data.
     * @return list of usage records, in ascending time order, for the given source.
     */
    public List<UsageRecord> getDailyUsageData(Date date, String metric, String source)
    {
    	// Query is in the form:
		// select * from user_usage_source_total WHERE time >= '2009-04-09 00:00' AND 
		// time <= ADDTIME('2009-04-09 00:00', '24:0:0') AND source = 'hank' order by time ASC;
    	StringBuilder qryBlder = new StringBuilder("select time, ? from user_usage_source_total ");
		qryBlder.append("WHERE time >= ? AND time <= ADDTIME(?, '24:0:0') AND source = ? order by time ASC");
		String query = qryBlder.toString();
		
		// Format the day parameter.
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 00:00");
		String day = formatter.format(date);
		
		logger.debug("getDailyUsageData() query: " + query);
		
		return m_SimpleJdbcTemplate.query(query, new UsageRecordRowMapper(metric), 
				metric, day, day, source);
    }
	
	
    /**
     * Returns the usage data for the specified day for the specified username on 
     * the given source (server).
     * @param date day for which to obtain user usage data.
     * @param source the name of the source (server) for which to obtain usage data.
     * @param username name of the user for which to obtain usage data.
     * @return list of usage records, in ascending time order, for the given source and user.
     */
    public List<UsageRecord> getDailyUsageData(Date date, String metric, 
    		String source, String username)
    {
    	// Query is in the form:
		// select * from user_usage_source_username_total WHERE time >= '2009-04-09 00:00' AND 
		// time <= ADDTIME('2009-04-09 00:00', '24:0:0') 
		// AND source = 'hank' AND username = 'pete' order by time ASC;
		StringBuilder qryBlder = new StringBuilder("select time, ? from user_usage_source_username_total ");
		qryBlder.append("WHERE time >= ? AND time <= ADDTIME(?, '24:0:0') ");
		qryBlder.append("AND source = ? AND username = ? order by time ASC");
		String query = qryBlder.toString();
		
		// Format the day parameter.
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 00:00");
		String day = formatter.format(date);
		
		logger.debug("getDailyUsageData() query: " + query);
		
		return m_SimpleJdbcTemplate.query(query, new UsageRecordRowMapper(metric), 
				metric, day, day, source, username);
    }


	/**
	 * Returns the total user usage from all servers for the specified hour.
	 * @param date hour for which to obtain user usage data.
	 * @return list of total user usage data from all servers.
	 */
    public List<UsageRecord> getHourlyUsageData(Date date, String metric)
    {
		// Query is in the form:
		// select * from user_usage_total WHERE time >= '2009-04-07 23:00:00' AND 
		// time <= ADDTIME('2009-04-07 23:00:00', '1:0:0') order by time ASC	
		StringBuilder qryBlder = new StringBuilder("select time, ? from user_usage_total ");
		qryBlder.append("WHERE time >= ? AND time <= ADDTIME(?, '1:0:0') ");
		qryBlder.append("order by time ASC");
		String query = qryBlder.toString();
		
		// Format the hour parameter.
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:00");
		String hour = formatter.format(date);
		
		logger.debug("getHourlyUsageData() query: " + query);
		
		return m_SimpleJdbcTemplate.query(query, new UsageRecordRowMapper(metric),
				metric, hour, hour);
		
    }


	/**
     * Returns the usage data for the specified hour for the given source (server).
     * @param date hour for which to obtain user usage data.
     * @param source the name of the source (server) for which to obtain usage data.
     * @return list of usage records, in ascending time order, for the given source.
     */
    public List<UsageRecord> getHourlyUsageData(Date date, String metric, String source)
    {
		// Query is in the form:
		// select * from user_usage_total 
		// WHERE DATE_FORMAT(time,'%Y-%m-%d %H:00-59')='2009-04-07 17:00-59' 
		// AND source='hank' order by time ASC;
		StringBuilder qryBlder = new StringBuilder("select time, ? from user_usage_source_total ");
		qryBlder.append("WHERE time >= ? AND time <= ADDTIME(?, '1:0:0') AND source = ? ");
		qryBlder.append("order by time ASC");
		String query = qryBlder.toString();
		
		// Format the hour parameter.
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:00");
		String hour = formatter.format(date);
		
		logger.debug("getHourlyUsageData() query: " + query);
		
		return m_SimpleJdbcTemplate.query(query, new UsageRecordRowMapper(metric),
				metric, hour, hour, source);
    }


    /**
     * Returns the usage data for the specified hour for the specified username on 
     * the given source (server).
     * @param date hour for which to obtain user usage data.
     * @param source the name of the source (server) for which to obtain usage data.
     * @param username name of the user for which to obtain usage data.
     * @return list of usage records, in ascending time order, for the given source and user.
     */
    public List<UsageRecord> getHourlyUsageData(Date date, String metric, 
    		String source, String username)
    {
		// Query is in the form:
		// select * from user_usage_total 
		// WHERE DATE_FORMAT(time,'%Y-%m-%d %H:00-59')='2009-04-07 17:00-59' 
		// AND source='hank' AND username='pete' order by time ASC;		
		StringBuilder qryBlder = new StringBuilder("select time, ? from user_usage_source_username_total ");
		qryBlder.append("WHERE time >= ? AND time <= ADDTIME(?, '1:0:0') AND source = ? AND username = ? ");
		qryBlder.append("order by time ASC");
		String query = qryBlder.toString();
		
		// Format the hour parameter.
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:00");
		String hour = formatter.format(date);
		
		logger.debug("getHourlyUsageData() query: " + query);
		
		return m_SimpleJdbcTemplate.query(query, new UsageRecordRowMapper(metric),
				metric, hour, hour, source, username);
    }
	
	
	/**
	 * Returns the date of the earliest usage record in the Prelert database.
	 * @return date of earliert usage record.
	 */
    public Date getEarliestUsageDate()
    {
		String query = "select MIN(time) from user_usage_source_username_total;";
		return m_SimpleJdbcTemplate.queryForObject(query, java.sql.Timestamp.class);
    }


	/**
	 * Returns the date of the latest usage record in the Prelert database.
	 * @return date of latest usage record.
	 */
    public Date getLatestUsageDate()
    {
		String query = "select MAX(time) from user_usage_source_username_total;";
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
