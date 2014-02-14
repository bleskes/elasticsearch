package demo.app.server;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import demo.app.data.DatePagingLoadConfig;
import demo.app.data.DatePagingLoadResult;
import demo.app.data.TimeFrame;
import demo.app.data.UsageRecord;
import demo.app.service.UsageQueryService;


/**
 * Server-side implementation of the service for retrieving IPC usage data from the
 * Prelert database.
 * @author Pete Harverson
 */
public class IPCUsageQueryServiceImpl extends RemoteServiceServlet 
	implements UsageQueryService
{
	static Logger logger = Logger.getLogger(IPCUsageQueryServiceImpl.class);
	
	private SimpleJdbcTemplate	m_SimpleJdbcTemplate;
	
	public IPCUsageQueryServiceImpl()
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
	
	
    public List<String> getUsers(String source)
    {
		// Just return null since there are no 'users' of IPC data.
    	return null;
    }
	
	
    public List<UsageRecord> getTotalHourlyUsageData(Date date, String metric)
    {
		return getHourlyUsageData(date, metric, null, null);
    }
	

    public List<UsageRecord> getHourlyUsageData(Date date, String metric,
            String source)
    {
    	// Query calls a stored procedure in the form:
    	// CALL usage_ipc_query('inbound', ?, ADDTIME(?, '1:0:0'), 'source');
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:00");
		String hour = formatter.format(date);
		
		String query = "CALL usage_ipc_query(?, ?, ADDTIME(?, '1:0:0'), ?)";
		logger.debug("getHourlyUsageData() query: " + query);
		
		return m_SimpleJdbcTemplate.query(query, new UsageRecordRowMapper(), 
				metric, hour, hour, source);
    }
	
	
    public List<UsageRecord> getHourlyUsageData(Date date, String metric,
            String source, String service)
    {
		return getHourlyUsageData(date, metric, source);
    }


    public List<UsageRecord> getTotalDailyUsageData(Date date, String metric)
    {
		return getDailyUsageData(date, metric, null);
    }
	
	
    public List<UsageRecord> getDailyUsageData(Date date, String metric,
            String source)
    {
    	// Query calls a stored procedure in the form:
    	// CALL usage_ipc_day_query('inbound', ?, ADDTIME(?, '24:0:0'), 'source');
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 00:00");
		String day = formatter.format(date);
		
		String query = "CALL usage_ipc_day_query(?, ?, ADDTIME(?, '24:0:0'), ?)";
		logger.debug("getDailyUsageData() query: " + query);	
		
		return m_SimpleJdbcTemplate.query(query, new UsageRecordRowMapper(), 
				metric, day, day, source);
    }
	
	
    public List<UsageRecord> getDailyUsageData(Date date, String metric,
            String source, String service)
    {
		return getDailyUsageData(date, metric, source);
    }


    public List<UsageRecord> getTotalWeeklyUsageData(String metric)
    {
		return getWeeklyUsageData(metric, null);
    }
	

    public List<UsageRecord> getWeeklyUsageData(String metric, String source)
    {
		// Query calls a stored procedure in the form:
    	// CALL usage_ipc_week_query('inbound', ADDTIME(?, '-144:00:0'), ADDTIME(?, '24:00:0'), 'source');
		String query = "CALL usage_ipc_week_query(?, ADDTIME(?, '-144:0:0'), ADDTIME(?, '24:0:0'), ?)";
		logger.debug("getWeeklyUsageData() query: " + query);
		
		// Go between the latest usage date, and one week before.
		List<UsageRecord> weekRecords = null;
		Date latestUsageDate = getLatestUsageDate();
		
		if (latestUsageDate != null)
		{
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 00:00");
			String weekEnding = formatter.format(latestUsageDate);
			weekRecords = m_SimpleJdbcTemplate.query(query, new UsageRecordRowMapper(), 
					metric, weekEnding, weekEnding, source);
		}
		else
		{
			weekRecords = new ArrayList<UsageRecord>(0);
		}
		
		logger.debug("getWeeklyUsageData returning number of results: " + weekRecords.size());
		return weekRecords;
    }
	
	
    public List<UsageRecord> getWeeklyUsageData(String metric, String source,
            String service)
    {
		return getWeeklyUsageData(metric, source);
    }
	
	
    public List<UsageRecord> getTimeLineUsageData(Date zoomStart, Date zoomEnd,
            String metric, String source, String username)
    {
		// PH: 13-7-09 - To implement if a switch is made to the timeline usage charts.
		return getTotalWeeklyUsageData(metric);
    }
	
	
	@Override
    public DatePagingLoadResult<UsageRecord> getUsageData(String metric,
            DatePagingLoadConfig config)
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

	
	@Override
    public DatePagingLoadResult<UsageRecord> getUsageData(String metric,
            String source, DatePagingLoadConfig config)
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

	
	@Override
    public DatePagingLoadResult<UsageRecord> getUsageData(String metric,
            String source, String service, DatePagingLoadConfig config)
    {
		if (service == null)
		{
			return getUsageData(metric, source, config);
		}
		
		TimeFrame timeFrame = config.getTimeFrame();
		Date date = config.getDate();
		
		List<UsageRecord> records = new ArrayList<UsageRecord>();
		
		switch (timeFrame)
		{
			case WEEK:
				records = getWeeklyUsageData(metric, source, service);
				break;
				
			case DAY:
				if (date != null)
				{
					records = getDailyUsageData(date, metric, source, service);
				}
				break;
				
			case HOUR:
				if (date != null)
				{
					records = getHourlyUsageData(date, metric, source, service);
				}
				break;
		
		}
		
		return new DatePagingLoadResult<UsageRecord>(records, timeFrame,
				getFirstResultTime(records), getEarliestUsageDate(), getLatestUsageDate());
    }
	
	
	public Date getEarliestUsageDate()
	{
		String query = "select MIN(time) from usage_ipc;";
		return m_SimpleJdbcTemplate.queryForObject(query, java.sql.Timestamp.class);
	}
	
	
	public Date getLatestUsageDate()
	{
		String query = "select MAX(time) from usage_ipc;";
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
