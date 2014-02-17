package demo.app.server;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
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


public class ServiceUsageQueryServiceImpl extends RemoteServiceServlet 
	implements UsageQueryService
{
	static Logger logger = Logger.getLogger(ServiceUsageQueryServiceImpl.class);
	
	private SimpleJdbcTemplate	m_SimpleJdbcTemplate;
	
	public ServiceUsageQueryServiceImpl()
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
	
	
	@Override
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
	
	
	@Override
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
	
	
    public List<UsageRecord> getTotalHourlyUsageData(Date date, String metric)
    {
		return getHourlyUsageData(date, metric, null, null);
    }
	

    public List<UsageRecord> getHourlyUsageData(Date date, String metric,
            String source)
    {
		return getHourlyUsageData(date, metric, source, null);
    }
	
	
    public List<UsageRecord> getHourlyUsageData(Date date, String metric,
            String source, String service)
    {
    	// Query calls a stored procedure in the form:
    	// CALL usage_users_query('total', ?, ADDTIME(?, '1:0:0'), 'source', 'user');
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:00");
		String hour = formatter.format(date);
		
		String query = "CALL usage_servers_query(?, ?, ADDTIME(?, '1:0:0'), ?, ?)";
		logger.debug("getHourlyUsageData() query: " + query);
		
		return m_SimpleJdbcTemplate.query(query, new UsageRecordRowMapper(metric), 
				metric, hour, hour, source, service);
    }


    public List<UsageRecord> getTotalDailyUsageData(Date date, String metric)
    {
		return getDailyUsageData(date, metric, null, null);
    }
	
	
    public List<UsageRecord> getDailyUsageData(Date date, String metric,
            String source)
    {
		return getDailyUsageData(date, metric, source, null);
    }
	
	
    public List<UsageRecord> getDailyUsageData(Date date, String metric,
            String source, String service)
    {
    	// Query calls a stored procedure in the form:
    	// CALL usage_users_day_query('total', ?, ADDTIME(?, '24:0:0'), 'source', 'user');
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 00:00");
		String day = formatter.format(date);
		
		String query = "CALL usage_servers_day_query(?, ?, ADDTIME(?, '24:0:0'), ?, ?)";
		logger.debug("getDailyUsageData() query: " + query);	
		
		return m_SimpleJdbcTemplate.query(query, new UsageRecordRowMapper(metric), 
				metric, day, day, source, service);
    }


    public List<UsageRecord> getTotalWeeklyUsageData(String metric)
    {
		return getWeeklyUsageData(metric, null, null);
    }
	

    public List<UsageRecord> getWeeklyUsageData(String metric, String source)
    {
		return getWeeklyUsageData(metric, source, null);
    }
	

    public List<UsageRecord> getWeeklyUsageData(String metric, String source,
            String service)
    {
		
		// Query calls a stored procedure in the form:
    	// CALL usage_servers_week_query('active', ADDTIME(?, '-144:00:0'), ADDTIME(?, '24:00:0'), 'source', 'user');
		String query = "CALL usage_servers_week_query(?, ADDTIME(?, '-144:0:0'), ADDTIME(?, '24:0:0'), ?, ?)";
		logger.debug("getWeeklyUsageData() query: " + query);
		
		// Go between the latest usage date, and one week before.
		List<UsageRecord> weekRecords = null;
		Date latestUsageDate = getLatestUsageDate();
		
		if (latestUsageDate != null)
		{
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 00:00");
			String weekEnding = formatter.format(latestUsageDate);
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
	 * Returns usage data for display in a time line covering the full range of 
	 * usage data in the database, zoomed between the specified start and end time.
	 * @param zoomStart start date of zoom period, or <code>null</code> if no
	 * zoom has been set.
	 * @param zoomEnd end date of zoom period, or <code>null</code> if no
	 * zoom has been set.
	 * @param metric name of usage metric to obtain e.g. pending or serverload.
	 * @param source source (server) for which to obtain service usage data.
	 * @param service name of service for which to obtain usage data.
	 * @return list of usage records, in ascending time order, for the given
	 *  source and service.
	 */
	public List<UsageRecord> getTimeLineUsageData(Date zoomStart, Date zoomEnd,
			String metric, String source, String service)
	{
		logger.debug("getTimeLineUsageData() zoom from :" +  zoomStart + " to " + zoomEnd);
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		ArrayList<UsageRecord> usageRecords = new ArrayList<UsageRecord>();
		
		Date earliestUsageDate = getEarliestUsageDate();
		Date latestUsageDate = getLatestUsageDate();
		
		// If there is no zoom, return the full range for WEEK granularity.
		if (zoomStart == null || zoomEnd == null)
		{
			return getTimeLineWeeklyUsageData(earliestUsageDate, latestUsageDate, metric, source, service);
		}
		
		// Get the timeframe of the zoom.
		long zoomEndMs = zoomEnd.getTime();
		long zoomStartMs = zoomStart.getTime();
		long zoomLength = zoomEndMs - zoomStartMs;
		
		TimeFrame zoomTimeFrame = TimeFrame.WEEK;
		
		long millisInHour = 60*60*1000;
		long millisInDay = 24*millisInHour;
		
		if (zoomLength <= millisInHour)
		{
			zoomTimeFrame = TimeFrame.HOUR;
			
			// Populate the start.
			usageRecords.addAll(getTimeLineWeeklyUsageData(earliestUsageDate, zoomStart, metric, source, service));
			
			// Populate the zoom area.
			usageRecords.addAll(getTimeLineHourlyUsageData(zoomStart, zoomEnd, metric, source, service));
			
			// Populate the end.
			usageRecords.addAll(getTimeLineWeeklyUsageData(zoomEnd, latestUsageDate, metric, source, service));
			
		}
		else if (zoomLength <= millisInDay)
		{
			zoomTimeFrame = TimeFrame.DAY;
			
			// Populate the start.
			usageRecords.addAll(getTimeLineWeeklyUsageData(earliestUsageDate, zoomStart, metric, source, service));
			
			// Populate the zoom area.
			usageRecords.addAll(getTimeLineDailyUsageData(zoomStart, zoomEnd, metric, source, service));
			
			// Populate the end.
			usageRecords.addAll(getTimeLineWeeklyUsageData(zoomEnd, latestUsageDate, metric, source, service));
		}
		else
		{
			zoomTimeFrame = TimeFrame.WEEK;
			
			// Populate the full range with WEEK data.
			usageRecords.addAll(getTimeLineWeeklyUsageData(earliestUsageDate, latestUsageDate, metric, source, service));
		}
		
	    return usageRecords;
	}
	
	
    protected List<UsageRecord> getTimeLineWeeklyUsageData(Date startDate, Date endDate,
    		String metric, String source, String service)
    {
		// Query calls a stored procedure in the form:
    	// CALL usage_servers_week_query('active', minTime, maxTime, 'source', 'user');
		String query = "CALL usage_servers_week_query(?, ?, ?, ?, ?)";
		logger.debug("getTimeLineWeeklyUsageData() query: " + query);
		
		// Go between the latest usage date, and one week before.
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		String minDate = formatter.format(startDate);
		String maxDate = formatter.format(endDate);
		
		logger.debug("getTimeLineWeeklyUsageData() minDate: " + minDate);
		logger.debug("getTimeLineWeeklyUsageData() maxDate: " + maxDate);
		
		return m_SimpleJdbcTemplate.query(query, new UsageRecordRowMapper(metric), 
				metric, minDate, maxDate, source, service);
    }
    
    
    protected List<UsageRecord> getTimeLineDailyUsageData(Date startDate, Date endDate,
    		String metric, String source, String service)
    {
		// Query calls a stored procedure in the form:
    	// CALL usage_servers_day_query('active', minTime, maxTime, 'source', 'user');
		String query = "CALL usage_servers_day_query(?, ?, ?, ?, ?)";
		logger.debug("getTimeLineDailyUsageData() running query: " + query);
		
		// Go between the latest usage date, and one week before.
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		String minDate = formatter.format(startDate);
		String maxDate = formatter.format(endDate);
		
		logger.debug("getTimeLineDailyUsageData() minDate: " + minDate);
		logger.debug("getTimeLineDailyUsageData() maxDate: " + maxDate);
		
		return m_SimpleJdbcTemplate.query(query, new UsageRecordRowMapper(metric), 
				metric, minDate, maxDate, source, service);
    }
    
    
    protected List<UsageRecord> getTimeLineHourlyUsageData(Date startDate, Date endDate,
    		String metric, String source, String service)
    {
		// Query calls a stored procedure in the form:
    	// CALL usage_servers_query(?, ?, ?, ?, ?);
    	String query = "CALL usage_servers_query(?, ?, ?, ?, ?)";
		logger.debug("getTimeLineHourlyUsageData() running query: " + query);
		
		// Go between the latest usage date, and one week before.
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		String minDate = formatter.format(startDate);
		String maxDate = formatter.format(endDate);
		
		logger.debug("getTimeLineHourlyUsageData() minDate: " + minDate);
		logger.debug("getTimeLineHourlyUsageData() maxDate: " + maxDate);
		
		return m_SimpleJdbcTemplate.query(query, new UsageRecordRowMapper(metric), 
				metric, minDate, maxDate, source, service);
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
		String query = "select MIN(time) from usage_servers;";
		return m_SimpleJdbcTemplate.queryForObject(query, java.sql.Timestamp.class);
	}
	
	
	public Date getLatestUsageDate()
	{
		String query = "select MAX(time) from usage_servers;";
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
