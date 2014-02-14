package com.prelert.dao;

import java.math.BigDecimal;
import java.sql.ResultSet;
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
 * Implementation for a MySQL database of the UsageViewDAO interface which 
 * predominantly uses calls to stored procedures to obtain information for 
 * Server Usage views.
 * @author Pete Harverson
 */
public class ServerUsageViewMySQLDAO implements UsageViewDAO
{

	static Logger logger = Logger.getLogger(ServerUsageViewMySQLDAO.class);
	
	private SimpleJdbcTemplate	m_SimpleJdbcTemplate;
	
	
	/**
	 * Sets the data source to be used for connections to the Prelert database.
	 * @param dataSource
	 */
	public void setDataSource(DataSource dataSource)
	{
		m_SimpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}

	
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
	 * Returns the list of users (servers) for the specified source.
	 * @return list of servers for the given source.
	 */
	public List<String> getUsers(String source)
	{
		String query = "select server from usage_servers_servers " +
				"WHERE source = ? order by server ASC";
		logger.debug("getUsers() query: " + query);
		
		ParameterizedRowMapper<String> mapper = new ParameterizedRowMapper<String>(){

			@Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException
            {
				return rs.getString("server");
            }
		};
		
		return m_SimpleJdbcTemplate.query(query, mapper, source);
	}


	/**
	 * Returns the total user usage for the last week from all sources.
	 * @return list of total server usage data from all sources.
	 */
	public List<UsageRecord> getWeeklyUsageData(String metric)
	{
		return getWeeklyUsageData(metric, null, null);
	}


	/**
     * Returns the usage data for the last week for the given source.
     * @param source the name of the source for which to obtain usage data.
     * @return list of usage records, in ascending time order, for the given source.
     */
	public List<UsageRecord> getWeeklyUsageData(String metric, String source)
	{
		return getWeeklyUsageData(metric, source, null);
	}


    /**
     * Returns the usage data for the last week for the specified server on 
     * the given source.
     * @param source the name of the source for which to obtain usage data.
     * @param server name of the server for which to obtain usage data.
     * @return list of usage records, in ascending time order, for the given source and server.
     */
	public List<UsageRecord> getWeeklyUsageData(String metric, String source, String server)
	{
		// Query calls a stored procedure in the form:
    	// CALL usage_servers_week_query('active', ADDTIME(?, '-144:00:0'), ADDTIME(?, '24:00:0'), 'source', 'user');
		String query = "CALL usage_servers_week_query(?, ADDTIME(?, '-144:0:0'), ADDTIME(?, '24:0:0'), ?, ?)";
		logger.debug("getWeeklyUsageData() query: " + query);
		
		// Go between the latest usage date, and one week before.
		Date latestUsageDate = getLatestUsageDate();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 00:00");
		String weekEnding = formatter.format(latestUsageDate);
		
		return m_SimpleJdbcTemplate.query(query, new UsageRecordRowMapper(metric), 
				metric, weekEnding, weekEnding, source, server);
	}
	
	
	/**
	 * Returns the total server usage from all sources for the specified day.
	 * @param date day for which to obtain server usage data.
	 * @return list of total server usage data from all sources.
	 */
    public List<UsageRecord> getDailyUsageData(Date date, String metric)
    {
    	return getDailyUsageData(date, metric, null, null);
    }



	/**
     * Returns the usage data for the specified day for the given source.
     * @param date day for which to obtain server usage data.
     * @param source the name of the source for which to obtain usage data.
     * @return list of usage records, in ascending time order, for the given source.
     */
    public List<UsageRecord> getDailyUsageData(Date date, String metric, String source)
    {
    	return getDailyUsageData(date, metric, source, null);
    }
	
	
    /**
     * Returns the usage data for the specified day for the specified server on 
     * the given source.
     * @param date day for which to obtain user usage data.
     * @param source the name of the source for which to obtain usage data.
     * @param server name of the server for which to obtain usage data.
     * @return list of usage records, in ascending time order, for the given source and server.
     */
    public List<UsageRecord> getDailyUsageData(Date date, String metric, String source,
            String server)
    {
    	// Query calls a stored procedure in the form:
    	// CALL usage_users_day_query('total', ?, ADDTIME(?, '24:0:0'), 'source', 'user');
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 00:00");
		String day = formatter.format(date);
		
		String query = "CALL usage_servers_day_query(?, ?, ADDTIME(?, '24:0:0'), ?, ?)";
		logger.debug("getDailyUsageData() query: " + query);			
		
		return m_SimpleJdbcTemplate.query(query, new UsageRecordRowMapper(metric), 
				metric, day, day, source, server);
    }


	/**
	 * Returns the total server usage from all sources for the specified hour.
	 * @param date hour for which to obtain server usage data.
	 * @return list of total server usage data from all sources.
	 */
    public List<UsageRecord> getHourlyUsageData(Date date, String metric)
    {
    	return getHourlyUsageData(date, metric, null, null);
    }


	/**
     * Returns the usage data for the specified hour for the given source.
     * @param date hour for which to obtain server usage data.
     * @param source the name of the source for which to obtain usage data.
     * @return list of usage records, in ascending time order, for the given source.
     */
    public List<UsageRecord> getHourlyUsageData(Date date, String metric, String source)
    {
    	return getHourlyUsageData(date, metric, source, null);
    }


    /**
     * Returns the usage data for the specified hour for the specified server on 
     * the given source.
     * @param date hour for which to obtain server usage data.
     * @param source the name of the source for which to obtain usage data.
     * @param server name of the server for which to obtain usage data.
     * @return list of usage records, in ascending time order, for the given source and server.
     */
    public List<UsageRecord> getHourlyUsageData(Date date, String metric, String source,
            String server)
    {
    	// Query calls a stored procedure in the form:
    	// CALL usage_users_query('total', ?, ADDTIME(?, '1:0:0'), 'source', 'user');
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:00");
		String hour = formatter.format(date);
		
		String query = "CALL usage_servers_query(?, ?, ADDTIME(?, '1:0:0'), ?, ?)";
		logger.debug("getHourlyUsageData() query: " + query);
		
		return m_SimpleJdbcTemplate.query(query, new UsageRecordRowMapper(metric), 
				metric, hour, hour, source, server);
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
