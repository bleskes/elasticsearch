package demo.app.dao;

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

import demo.app.data.UsageRecord;




/**
 * Implementation for a MySQL database of the UsageViewDAO interface which 
 * predominantly uses calls to stored procedures to obtain information for 
 * IPC Usage views.
 * @author Pete Harverson
 */
public class IPCUsageViewMySQLDAO implements UsageViewDAO
{

	static Logger logger = Logger.getLogger(IPCUsageViewMySQLDAO.class);
	
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
		// PH - 14-7-09: this probably needs changing to use a usage_ipc_sources table.
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
	 * Returns <code>null</code> since there are no 'users' of IPC data.
	 * @return <code>null</code> since there are no 'users' of IPC data.
	 */
	public List<String> getUsers(String source)
	{
		// Just return null since there are no 'users' of IPC data.
    	return null;
	}


	/**
	 * Returns the total IPC usage for the last week from all sources.
	 * @param metric name of usage metric to return data on e.g. inbound, outbound.
	 * @return list of total IPC usage data from all sources.
	 * 		Returns an empty list if the database has not been populated with usage data.
	 */
	public List<UsageRecord> getWeeklyUsageData(String metric)
	{
		return getWeeklyUsageData(metric, null);
	}


	/**
     * Returns the usage data for the last week for the given source.
     * @param metric name of usage metric to return data on e.g. inbound, outbound.
     * @param source the name of the source for which to obtain usage data.
     * @return list of IPC usage records, in ascending time order, for the given source.
     * 		Returns an empty list if the database has not been populated with usage data.
     */
	public List<UsageRecord> getWeeklyUsageData(String metric, String source)
	{
		// Go between the latest usage date, and one week before.
		List<UsageRecord> weekRecords = null;
		Date latestUsageDate = getLatestUsageDate();
		
		if (latestUsageDate != null)
		{
			// Query calls a stored procedure in the form:
	    	// CALL usage_ipc_week_query('inbound', ADDTIME(?, '-144:00:0'), ADDTIME(?, '24:00:0'), 'source');
			String query = "CALL usage_ipc_week_query(?, ADDTIME(?, '-144:0:0'), ADDTIME(?, '24:0:0'), ?)";
			
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 00:00");
			String weekEnding = formatter.format(latestUsageDate);
			
			String debugQuery = "CALL usage_ipc_week_query({0}, ADDTIME({1}, '-144:0:0'), " +
					"ADDTIME({2}, '24:0:0'), {3})";
			debugQuery = MessageFormat.format(
					debugQuery, metric, weekEnding, weekEnding, source);
			logger.debug("getWeeklyUsageData() query: " + debugQuery);
			
			weekRecords = m_SimpleJdbcTemplate.query(query, new UsageRecordRowMapper(), 
					metric, weekEnding, weekEnding, source);
		}
		else
		{
			weekRecords = new ArrayList<UsageRecord>(0);
		}
		
		return weekRecords;
	}


    /**
     * Returns the usage data for the last week for the given source.
     * @param metric name of usage metric to return data on e.g. inbound, outbound.
     * @param source the name of the source for which to obtain usage data.
     * @param user not used.
     * @return list of usage records, in ascending time order, for the given source.
     * 		Returns an empty list if the database has not been populated with usage data.
     */
	public List<UsageRecord> getWeeklyUsageData(String metric, String source, String user)
	{
		return getWeeklyUsageData(metric, source);
	}
	
	
	/**
	 * Returns the total service usage from all sources for the specified day.
	 * @param date day for which to obtain service usage data.
	 * @param metric name of usage metric to return data on e.g. inbound, outbound.
	 * @return list of total IPC usage data from all sources.
	 */
    public List<UsageRecord> getDailyUsageData(Date date, String metric)
    {
    	return getDailyUsageData(date, metric, null);
    }



	/**
     * Returns the usage data for the specified day for the given source.
     * @param date day for which to obtain service usage data.
     * @param metric name of usage metric to return data on e.g. inbound, outbound.
     * @param source the name of the source for which to obtain usage data.
     * @return list of IPC usage records, in ascending time order, for the given source.
     */
    public List<UsageRecord> getDailyUsageData(Date date, String metric, String source)
    {
    	// Query calls a stored procedure in the form:
    	// CALL usage_ipc_day_query('inbound', ?, ADDTIME(?, '24:0:0'), 'source');
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 00:00");
		String day = formatter.format(date);
		
		String query = "CALL usage_ipc_day_query(?, ?, ADDTIME(?, '24:0:0'), ?)";
		
		String debugQuery = "CALL usage_ipc_day_query({0}, {1}, " +
			"ADDTIME({2}, '24:0:0'), {3})";
		debugQuery = MessageFormat.format(
				debugQuery, metric, day, day, source);
		logger.debug("getDailyUsageData() query: " + debugQuery);
		
		return m_SimpleJdbcTemplate.query(query, new UsageRecordRowMapper(), 
				metric, day, day, source);
    }
	
	
    /**
     * Returns the usage data for the specified day for the specified service on 
     * the given source.
     * @param date day for which to obtain IPC usage data.
     * @param metric name of usage metric to return data on e.g. inbound, outbound.
     * @param source the name of the source for which to obtain usage data.
     * @param user not used.
     * @return list of usage records, in ascending time order, for the given source.
     */
    public List<UsageRecord> getDailyUsageData(Date date, String metric, String source,
            String user)
    {
    	return getDailyUsageData(date, metric, source);
    }


	/**
	 * Returns the total service usage from all sources for the specified hour.
	 * @param date hour for which to obtain IPC usage data.
	 * @param metric name of usage metric to return data on e.g. inbound, outbound.
	 * @return list of total IPC usage data from all sources.
	 */
    public List<UsageRecord> getHourlyUsageData(Date date, String metric)
    {
    	return getHourlyUsageData(date, metric, null);
    }


	/**
     * Returns the usage data for the specified hour for the given source.
     * @param date hour for which to obtain IPC usage data.
     * @param metric name of usage metric to return data on e.g. inbound, outbound.
     * @param source the name of the source for which to obtain usage data.
     * @return list of IPC usage records, in ascending time order, for the given source.
     */
    public List<UsageRecord> getHourlyUsageData(Date date, String metric, String source)
    {
    	// Query calls a stored procedure in the form:
    	// CALL usage_users_query('inbound', ?, ADDTIME(?, '1:0:0'), 'source');
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:00");
		String hour = formatter.format(date);
		
		String query = "CALL usage_ipc_query(?, ?, ADDTIME(?, '1:0:0'), ?)";
		
		String debugQuery = "CALL usage_ipc_query({0}, {1}, " +
			"ADDTIME({2}, '1:0:0'), {3})";
		debugQuery = MessageFormat.format(
				debugQuery, metric, hour, hour, source);
		logger.debug("getHourlyUsageData() query: " + debugQuery);
		
		return m_SimpleJdbcTemplate.query(query, new UsageRecordRowMapper(), 
				metric, hour, hour, source);
    }


    /**
     * Returns the usage data for the specified hour for the given source.
     * @param date hour for which to obtain IPC usage data.
     * @param metric name of usage metric to return data on e.g. inbound, outbound.
     * @param source the name of the source for which to obtain usage data.
     * @param user not used.
     * @return list of IPC usage records, in ascending time order, for the given source.
     */
    public List<UsageRecord> getHourlyUsageData(Date date, String metric, String source,
            String service)
    {
    	return getHourlyUsageData(date, metric, source);
    }
	
	
	/**
	 * Returns the date of the earliest usage record in the Prelert database.
	 * @return date of earliest IPC usage record.
	 */
    public Date getEarliestUsageDate()
    {
		String query = "select MIN(time) from usage_ipc;";
		return m_SimpleJdbcTemplate.queryForObject(query, java.sql.Timestamp.class);
    }


	/**
	 * Returns the date of the latest usage record in the Prelert database.
	 * @return date of latest IPC usage record.
	 */
    public Date getLatestUsageDate()
    {
		String query = "select MAX(time) from usage_ipc;";
		return m_SimpleJdbcTemplate.queryForObject(query, java.sql.Timestamp.class);
    }

	
	/**
	 * ParameterizedRowMapper class for mapping usage query result sets to
	 * Usage records.
	 */
	class UsageRecordRowMapper implements ParameterizedRowMapper<UsageRecord>
	{

		@Override
        public UsageRecord mapRow(ResultSet rs, int rowNum) throws SQLException
        {
			java.sql.Timestamp time = rs.getTimestamp("time");
			BigDecimal value = rs.getBigDecimal("delta");

			UsageRecord	record = new UsageRecord();
			record.set("time", time);
			record.set("value", value.intValue());		

            return record;
        }
		
	}

}

