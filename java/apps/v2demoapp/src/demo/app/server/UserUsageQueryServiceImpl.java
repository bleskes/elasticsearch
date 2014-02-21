package demo.app.server;

import java.math.BigDecimal;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

import javax.sql.DataSource;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import demo.app.data.DatePagingLoadConfig;
import demo.app.data.DatePagingLoadResult;
import demo.app.data.TimeFrame;
import demo.app.data.UsageRecord;
import demo.app.service.UsageQueryService;


public class UserUsageQueryServiceImpl extends RemoteServiceServlet 
	implements UsageQueryService
{
	static Logger logger = Logger.getLogger(UserUsageQueryServiceImpl.class);
	
	private SimpleJdbcTemplate	m_SimpleJdbcTemplate;
	
	public UserUsageQueryServiceImpl()
	{

	}
	
	
	/**
	 * Sets the data source to be used for connections to the Prelert database.
	 * @param dataSource JDBC datasource.
	 */
	public void setDataSource(DataSource dataSource)
	{
		m_SimpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}
	
	
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
	 * Returns the total usage from all servers.
	 * @return list of total usage data from all servers.
	 */
    public List<UsageRecord> getTotalWeeklyUsageData(String metric)
    {
    	return getWeeklyUsageData(metric, null, null);
    }
    
    
	/**
	 * Returns the total usage from all servers on the day in the given date.
	 * @return list of total usage data from all servers on the given day.
	 */
    public List<UsageRecord> getTotalDailyUsageData(Date date, String metric)
    {
    	return getDailyUsageData(date, metric, null, null);
    }
    
    
	/**
	 * Returns the total usage from all servers on the hour in the given date.
	 * @return list of total usage data from all servers on the given hour.
	 */
    public List<UsageRecord> getTotalHourlyUsageData(Date date, String metric)
    {
    	return getHourlyUsageData(date, metric, null, null);
    }


    /**
     * Returns the usage data for the given source (server).
     * @param source the name of the source (server) for which to obtain usage data.
     * @return list of usage records, in time order, for the given source.
     */
    public List<UsageRecord> getWeeklyUsageData(String metric, String source)
    {
    	return getWeeklyUsageData(metric, source, null);
    }
	
	
    /**
     * Returns the usage data for the specified username on the given source (server).
     * @param source the name of the source (server) for which to obtain usage data.
     * @param username name of the user for which to obtain usage data.
     * @return list of usage records, in time order, for the given source and user.
     */
	public List<UsageRecord> getWeeklyUsageData(String metric, String source, String username)
	{
		
		// Query calls a stored procedure in the form:
    	// CALL usage_users_week_query('total', ADDTIME(?, '-144:00:0'), ADDTIME(?, '24:00:0'), 'source', 'user');
		String query = "CALL usage_users_week_query(?, ADDTIME(?, '-144:0:0'), ADDTIME(?, '24:0:0'), ?, ?)";
		logger.debug("getWeeklyUsageData() running query: " + query);
		
		// Go between the latest usage date, and one week before.
		List<UsageRecord> weekRecords = null;
		Date latestUsageDate = getLatestUsageDate();
		
		if (latestUsageDate != null)
		{
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 00:00");
			String weekEnding = formatter.format(latestUsageDate);
			weekRecords = m_SimpleJdbcTemplate.query(query, new UsageRecordRowMapper(metric), 
					metric, weekEnding, weekEnding, source, username);
		}
		else
		{
			weekRecords = new ArrayList<UsageRecord>(0);
		}
		
		return weekRecords;
	}


    public List<UsageRecord> getDailyUsageData(Date date, String metric, String source)
    {
		return getDailyUsageData(date, metric, source, null);
    }


    public List<UsageRecord> getHourlyUsageData(Date date, String metric, String source)
    {
		return getHourlyUsageData(date, metric, source, null);
    }


    public List<UsageRecord> getDailyUsageData(Date date, String metric, String source,
            String username)
    {
		// Query calls a stored procedure in the form:
    	// CALL usage_users_day_query('total', ?, ADDTIME(?, '24:0:0'), 'source', 'user');
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 00:00");
		String day = formatter.format(date);
		
		String query = "CALL usage_users_day_query(?, ?, ADDTIME(?, '24:0:0'), ?, ?)";
		logger.debug("getDailyUsageData() running query: " + query);			
		
		return m_SimpleJdbcTemplate.query(query, new UsageRecordRowMapper(metric), 
				metric, day, day, source, username);
    }


    public List<UsageRecord> getHourlyUsageData(Date date, String metric, String source,
            String username)
    {
		// Query calls a stored procedure in the form:
    	// CALL usage_users_query('total', ?, ADDTIME(?, '1:0:0'), 'source', 'user');
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:00");
		String hour = formatter.format(date);
		
		String query = "CALL usage_users_query(?, ?, ADDTIME(?, '1:0:0'), ?, ?)";
		logger.debug("getHourlyUsageData() running query: " + query);	
		
		return m_SimpleJdbcTemplate.query(query, new UsageRecordRowMapper(metric), 
				metric, hour, hour, source, username);
    }
	
	
	public List<UsageRecord> getTimeLineUsageData(Date zoomStart, Date zoomEnd,
			String metric, String source, String username)
	{
		// PH: 19-6-09 - To implement if a switch is made to the timeline usage charts.
		return getTotalWeeklyUsageData(metric);
	}
	
	
	public DatePagingLoadResult<UsageRecord> getUsageData(String metric, DatePagingLoadConfig config)
	{
		TimeFrame timeFrame = config.getTimeFrame();
		Date date = config.getDate();
		
		List<UsageRecord> records = new ArrayList<UsageRecord>();
		
		switch (timeFrame)
		{
			case WEEK:
				records = getTotalWeeklyUsageData(metric);
				break;
				
			case DAY:
				if (date != null)
				{
					records = getTotalDailyUsageData(date, metric);
				}
				break;
				
			case HOUR:
				if (date != null)
				{
					records = getTotalHourlyUsageData(date, metric);
				}
				break;
		
		}
		
		return new DatePagingLoadResult<UsageRecord>(records, timeFrame,
				getFirstResultTime(records), getEarliestUsageDate(), getLatestUsageDate());
	}
	
	
	public DatePagingLoadResult<UsageRecord> getUsageData(String metric, String source, DatePagingLoadConfig config)
	{
		if (source == null)
		{
			return getUsageData(metric, config);
		}
		
		TimeFrame timeFrame = config.getTimeFrame();
		Date date = config.getDate();
		
		List<UsageRecord> records = new ArrayList<UsageRecord>();
		
		switch (timeFrame)
		{
			case WEEK:
				records = getWeeklyUsageData(metric, source);
				break;
				
			case DAY:
				if (date != null)
				{
					records = getDailyUsageData(date, metric, source);
				}
				break;
				
			case HOUR:
				if (date != null)
				{
					records = getHourlyUsageData(date, metric, source);
				}
				break;
		
		}
		
		return new DatePagingLoadResult<UsageRecord>(records, timeFrame,
				getFirstResultTime(records), getEarliestUsageDate(), getLatestUsageDate());
	}
	
	
	public DatePagingLoadResult<UsageRecord> getUsageData(String metric, 
			String source, String username, DatePagingLoadConfig config)
	{
		if (username == null)
		{
			return getUsageData(metric, source, config);
		}
		
		TimeFrame timeFrame = config.getTimeFrame();
		Date date = config.getDate();
		
		List<UsageRecord> records = new ArrayList<UsageRecord>();
		
		switch (timeFrame)
		{
			case WEEK:
				records = getWeeklyUsageData(metric, source, username);
				break;
				
			case DAY:
				if (date != null)
				{
					records = getDailyUsageData(date, metric, source, username);
				}
				break;
				
			case HOUR:
				if (date != null)
				{
					records = getHourlyUsageData(date, metric, source, username);
				}
				break;
		
		}
		
		
		return new DatePagingLoadResult<UsageRecord>(records, timeFrame,
				getFirstResultTime(records), getEarliestUsageDate(), getLatestUsageDate());
	}
	
	
	public Date getEarliestUsageDate()
	{
		String query = "select MIN(time) from usage_users;";
		return m_SimpleJdbcTemplate.queryForObject(query, java.sql.Timestamp.class);
	}
	
	
	public Date getLatestUsageDate()
	{
		String query = "select MAX(time) from usage_users;";
		return m_SimpleJdbcTemplate.queryForObject(query, java.sql.Timestamp.class);
	}
	
	
	/**
	 * Returns the time of the first result in the specified list of usage records.
	 * @return the time of the first result, or <code>null</code> if the supplied
	 * list is <code>null</code> or empty.
	 */
	private Date getFirstResultTime(List<UsageRecord> usageRecords)
	{
		Date firstResultTime = null;
		if (usageRecords != null && usageRecords.size() > 0)
		{
			firstResultTime = usageRecords.get(0).getTime();
		}
		
		return firstResultTime;
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
