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
 * Service Usage views.
 * @author Pete Harverson
 */
public class ServiceUsageViewMySQLDAO extends SpringJdbcTemplateDAO implements UsageViewDAO
{

	static Logger logger = Logger.getLogger(ServiceUsageViewMySQLDAO.class);
	

	
	/**
	 * Returns the list of available sources of Usage data.
	 * @return the list of available sources.
	 */
	public List<String> getSources()
	{
		String query = "select source from usage_servers_sources order by source ASC";
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
	 * Returns the list of users (services) for the specified source.
	 * @return list of servers for the given source.
	 */
	public List<String> getUsers(String source)
	{
    	StringBuilder qryBlder = new StringBuilder();
    	qryBlder.append("select distinct server from usage_servers_servers ");
    	if (source != null)
    	{
    		qryBlder.append("WHERE source = ? ");
    	}
    	qryBlder.append("order by server ASC");
    	String query = qryBlder.toString();
		logger.debug("getUsers() running query: " + query);
		
		ParameterizedRowMapper<String> mapper = new ParameterizedRowMapper<String>(){

			@Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException
            {
				return rs.getString("server");
            }
		};
		
		List<String> services = null;
		if (source != null)
		{
			services = m_SimpleJdbcTemplate.query(query, mapper, source);
		}
		else
		{
			services = m_SimpleJdbcTemplate.query(query, mapper);
		}
		
		return services;
	}


	/**
	 * Returns the total service usage for the last week from all sources.
	 * @param metric name of usage metric to return data on e.g. serverload, pending.
	 * @return list of total service usage data from all sources.
	 * 		Returns an empty list if the database has not been populated with usage data.
	 */
	public List<UsageRecord> getWeeklyUsageData(String metric)
	{
		return getWeeklyUsageData(metric, null, null);
	}


	/**
     * Returns the usage data for the last week for the given source.
     * @param metric name of usage metric to return data on e.g. serverload, pending.
     * @param source the name of the source for which to obtain usage data.
     * @return list of usage records, in ascending time order, for the given source.
     * 		Returns an empty list if the database has not been populated with usage data.
     */
	public List<UsageRecord> getWeeklyUsageData(String metric, String source)
	{
		return getWeeklyUsageData(metric, source, null);
	}


    /**
     * Returns the usage data for the last week for the specified service on 
     * the given source.
     * @param metric name of usage metric to return data on e.g. serverload, pending.
     * @param source the name of the source for which to obtain usage data.
     * @param service name of the service for which to obtain usage data.
     * @return list of usage records, in ascending time order, for the given source and service.
     * 		Returns an empty list if the database has not been populated with usage data.
     */
	public List<UsageRecord> getWeeklyUsageData(String metric, String source, String service)
	{
		// Go between the latest usage date, and one week before.
		List<UsageRecord> weekRecords = null;
		Date latestUsageDate = getLatestUsageDate();
		
		if (latestUsageDate != null)
		{
			// Query calls a stored procedure in the form:
	    	// CALL usage_servers_week_query(
			// 	'active', ADDTIME(?, '-144:00:0'), ADDTIME(?, '24:00:0'), 'source', 'service');
			String query = "CALL usage_servers_week_query(?, ADDTIME(?, '-144:0:0'), ADDTIME(?, '24:0:0'), ?, ?)";
			
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 00:00");
			String weekEnding = formatter.format(latestUsageDate);
			
			String debugQuery = "CALL usage_servers_week_query({0}, ADDTIME({1}, '-144:0:0'), " +
				"ADDTIME({2}, '24:0:0'), {3}, {4})";
			debugQuery = MessageFormat.format(
				debugQuery, metric, weekEnding, weekEnding, source, service);
			logger.debug("getWeeklyUsageData() query: " + debugQuery);
			
			weekRecords = m_SimpleJdbcTemplate.query(query, new UsageRecordRowMapper(metric), 
					metric, weekEnding, weekEnding, source, service);
		}
		else
		{
			weekRecords = new ArrayList<UsageRecord>(0);
		}
		
		return weekRecords;
	}
	
	
	/**
	 * Returns the total service usage from all sources for the specified day.
	 * @param date day for which to obtain service usage data.
	 * @param metric name of usage metric to return data on e.g. serverload, pending.
	 * @return list of total service usage data from all sources.
	 */
    public List<UsageRecord> getDailyUsageData(Date date, String metric)
    {
    	return getDailyUsageData(date, metric, null, null);
    }



	/**
     * Returns the usage data for the specified day for the given source.
     * @param date day for which to obtain service usage data.
     * @param metric name of usage metric to return data on e.g. serverload, pending.
     * @param source the name of the source for which to obtain usage data.
     * @return list of usage records, in ascending time order, for the given source.
     */
    public List<UsageRecord> getDailyUsageData(Date date, String metric, String source)
    {
    	return getDailyUsageData(date, metric, source, null);
    }
	
	
    /**
     * Returns the usage data for the specified day for the specified service on 
     * the given source.
     * @param date day for which to obtain user usage data.
     * @param metric name of usage metric to return data on e.g. serverload, pending.
     * @param source the name of the source for which to obtain usage data.
     * @param service name of the service for which to obtain usage data.
     * @return list of usage records, in ascending time order, for the given source and service.
     */
    public List<UsageRecord> getDailyUsageData(Date date, String metric, String source,
            String service)
    {
    	// Query calls a stored procedure in the form:
    	// CALL usage_servers_day_query('total', ?, ADDTIME(?, '24:0:0'), 'source', 'service');	
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 00:00");
		String day = formatter.format(date);
		
		String query = "CALL usage_servers_day_query(?, ?, ADDTIME(?, '24:0:0'), ?, ?)";
		
		String debugQuery = "CALL usage_servers_day_query({0}, {1}, " +
			"ADDTIME({2}, '24:0:0'), {3}, {4})";
		debugQuery = MessageFormat.format(
			debugQuery, metric, day, day, source, service);
		logger.debug("getDailyUsageData() query: " + debugQuery);
		
		return m_SimpleJdbcTemplate.query(query, new UsageRecordRowMapper(metric), 
				metric, day, day, source, service);
    }


	/**
	 * Returns the total service usage from all sources for the specified hour.
	 * @param date hour for which to obtain service usage data.
	 * @param metric name of usage metric to return data on e.g. serverload, pending.
	 * @return list of total service usage data from all sources.
	 */
    public List<UsageRecord> getHourlyUsageData(Date date, String metric)
    {
    	return getHourlyUsageData(date, metric, null, null);
    }


	/**
     * Returns the usage data for the specified hour for the given source.
     * @param date hour for which to obtain service usage data.
     * @param metric name of usage metric to return data on e.g. serverload, pending.
     * @param source the name of the source for which to obtain usage data.
     * @return list of usage records, in ascending time order, for the given source.
     */
    public List<UsageRecord> getHourlyUsageData(Date date, String metric, String source)
    {
    	return getHourlyUsageData(date, metric, source, null);
    }


    /**
     * Returns the usage data for the specified hour for the specified service on 
     * the given source.
     * @param date hour for which to obtain service usage data.
     * @param metric name of usage metric to return data on e.g. serverload, pending.
     * @param source the name of the source for which to obtain usage data.
     * @param service name of the service for which to obtain usage data.
     * @return list of usage records, in ascending time order, for the given source and service.
     */
    public List<UsageRecord> getHourlyUsageData(Date date, String metric, String source,
            String service)
    {
    	// Query calls a stored procedure in the form:
    	// CALL usage_servers_query('total', ?, ADDTIME(?, '1:0:0'), 'source', 'service');
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:00");
		String hour = formatter.format(date);
		
		String query = "CALL usage_servers_query(?, ?, ADDTIME(?, '1:0:0'), ?, ?)";
		
		String debugQuery = "CALL usage_servers_query({0}, {1}, " +
			"ADDTIME({2}, '1:0:0'), {3}, {4})";
		debugQuery = MessageFormat.format(
			debugQuery, metric, hour, hour, source, service);
		logger.debug("getHourlyUsageData() query: " + debugQuery);
		
		return m_SimpleJdbcTemplate.query(query, new UsageRecordRowMapper(metric), 
				metric, hour, hour, source, service);
    }
	
	
	/**
	 * Returns the date of the earliest usage record in the Prelert database.
	 * @return date of earliest usage record.
	 */
    public Date getEarliestUsageDate()
    {
		String query = "select MIN(time) from usage_servers;";
		return m_SimpleJdbcTemplate.queryForObject(query, java.sql.Timestamp.class);
    }


	/**
	 * Returns the date of the latest usage record in the Prelert database.
	 * @return date of latest usage record.
	 */
    public Date getLatestUsageDate()
    {
		String query = "select MAX(time) from usage_servers;";
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
