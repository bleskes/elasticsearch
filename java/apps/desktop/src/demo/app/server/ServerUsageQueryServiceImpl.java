package demo.app.server;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import demo.app.client.TimeFrame;
import demo.app.client.UsageQueryService;
import demo.app.data.DatePagingLoadConfig;
import demo.app.data.DatePagingLoadResult;
import demo.app.data.UsageRecord;

public class ServerUsageQueryServiceImpl extends RemoteServiceServlet 
	implements UsageQueryService
{
	static Logger logger = Logger.getLogger(ServerUsageQueryServiceImpl.class);
	
	private Connection m_Connection;
	
	public ServerUsageQueryServiceImpl()
	{
		try
		{
			// Load the MySQL JDBC driver directly for this test.
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			logger.debug("Loaded MySQL JDBC driver");
		}
		catch (Exception ex)
		{
			logger.error("Error loading MySQL JDBC driver", null);
		}

		try
		{
			m_Connection = DriverManager.getConnection("jdbc:mysql://localhost/prelert?" +
			       	"user=root&password=root123");
		}
		catch (SQLException ex)
		{
			// handle any errors
			logger.error("Error obtaining database connection", ex);
		}
	}
	
	@Override
    public List<String> getSources()
    {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		ArrayList<String> sources = new ArrayList<String>();
		
		try
		{
			String query = "select source from usage_servers_sources order by source ASC";
			logger.debug("getSources() running query: " + query);
			
			pstmt = m_Connection.prepareStatement(query);
			rs = pstmt.executeQuery();
			
			while (rs.next()) 
			{				
				sources.add(rs.getString("source"));		
			}
		}
		catch (SQLException ex)
		{
			// handle any errors
			logger.error("Error running getSources() query", ex);
		}
		finally 
		{
			// it is a good idea to release
			// resources in a finally{} block
			// in reverse-order of their creation
			// if they are no-longer needed
			if (rs != null)
			{
				try
				{
					rs.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
				rs = null;
			}
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
			}
		}

	    return sources;
    }
	
	
	@Override
    public List<String> getUsers(String source)
    {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		ArrayList<String> servers = new ArrayList<String>();
		
		try
		{
			String query = "select server from usage_servers_servers " +
				"WHERE source = ? order by server ASC";
			logger.debug("getUsers() running query: " + query);
			
			pstmt = m_Connection.prepareStatement(query);
			pstmt.setString(1, source);
			rs = pstmt.executeQuery();
			logger.debug("getUsers() source: " + source);	
			
			while (rs.next()) 
			{				
				servers.add(rs.getString("server"));		
			}
		}
		catch (SQLException ex)
		{
			// handle any errors
			logger.error("Error running getUsers() query", ex);
		}
		finally 
		{
			// it is a good idea to release
			// resources in a finally{} block
			// in reverse-order of their creation
			// if they are no-longer needed
			if (rs != null)
			{
				try
				{
					rs.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
				rs = null;
			}
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
			}
		}

	    return servers;
    }
	
	
	@Override
    public List<UsageRecord> getTotalHourlyUsageData(Date date, String metric)
    {
		return getHourlyUsageData(date, metric, null, null);
    }
	

	@Override
    public List<UsageRecord> getHourlyUsageData(Date date, String metric,
            String source)
    {
		return getHourlyUsageData(date, metric, source, null);
    }
	
	
	@Override
    public List<UsageRecord> getHourlyUsageData(Date date, String metric,
            String source, String server)
    {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		ArrayList<UsageRecord> usageRecords = new ArrayList<UsageRecord>();
		
		try
		{
			// Query calls a stored procedure in the form:
	    	// CALL usage_servers_query('total', ?, ADDTIME(?, '1:0:0'), 'source', 'user');
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:00");
			String hour = formatter.format(date);
			
			String query = "CALL usage_servers_query(?, ?, ADDTIME(?, '1:0:0'), ?, ?)";
			logger.debug("getHourlyUsageData() running query: " + query);			
			
			pstmt = m_Connection.prepareStatement(query);
			pstmt.setString(1, metric);
			pstmt.setString(2, hour);
			pstmt.setString(3, hour);
			pstmt.setString(4, source);
			pstmt.setString(5, server);
			rs = pstmt.executeQuery();
			
			UsageRecord record;
			java.sql.Timestamp time;
			BigDecimal value;
			while (rs.next()) 
			{				
				time = rs.getTimestamp("time");
				value = rs.getBigDecimal(metric);
				if ( (time != null) && (value != null) )
				{
					record = new UsageRecord();
					record.set("time", time);
					record.set("value", value.intValue());
					usageRecords.add(record);
				}			
			}
		}
		catch (SQLException ex)
		{
			// handle any errors
			logger.error("Error running getHourlyUsageData() query", ex);
		}
		finally 
		{
			// it is a good idea to release
			// resources in a finally{} block
			// in reverse-order of their creation
			// if they are no-longer needed
			if (rs != null)
			{
				try
				{
					rs.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
				rs = null;
			}
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
			}
		}

	    return usageRecords;
    }


	@Override
    public List<UsageRecord> getTotalDailyUsageData(Date date, String metric)
    {
		return getDailyUsageData(date, metric, null, null);
    }
	
	
	@Override
    public List<UsageRecord> getDailyUsageData(Date date, String metric,
            String source)
    {
		return getDailyUsageData(date, metric, source, null);
    }
	
	
	@Override
    public List<UsageRecord> getDailyUsageData(Date date, String metric,
            String source, String service)
    {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		ArrayList<UsageRecord> usageRecords = new ArrayList<UsageRecord>();
		
		try
		{
			// Query calls a stored procedure in the form:
	    	// CALL usage_servers_day_query('total', ?, ADDTIME(?, '24:0:0'), 'source', 'user');
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 00:00");
			String day = formatter.format(date);
			
			String query = "CALL usage_servers_day_query(?, ?, ADDTIME(?, '24:0:0'), ?, ?)";
			//String query = "CALL usage_servers_day_query(?, ?, ADDTIME(?, '96:0:0'), ?, ?)";
			logger.debug("getDailyUsageData() running query: " + query);	
			logger.debug("getDailyUsageData() source: " + source);	
			logger.debug("getDailyUsageData() service: " + service);	
			logger.debug("getDailyUsageData() metric: " + metric);	
			
			pstmt = m_Connection.prepareStatement(query);
			pstmt.setString(1, metric);
			pstmt.setString(2, day);
			pstmt.setString(3, day);
			pstmt.setString(4, source);
			pstmt.setString(5, service);
			rs = pstmt.executeQuery();
			
			UsageRecord record;
			java.sql.Timestamp time;
			BigDecimal value;
			while (rs.next()) 
			{				
				time = rs.getTimestamp("time");
				value = rs.getBigDecimal(metric);
				if ( (time != null) && (value != null) )
				{
					record = new UsageRecord();
					record.set("time", time);
					record.set("value", value.intValue());
					usageRecords.add(record);
				}			
			}
		}
		catch (SQLException ex)
		{
			// handle any errors
			logger.error("Error running getDailyUsageData() query", ex);
		}
		finally 
		{
			// it is a good idea to release
			// resources in a finally{} block
			// in reverse-order of their creation
			// if they are no-longer needed
			if (rs != null)
			{
				try
				{
					rs.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
				rs = null;
			}
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
			}
		}

	    return usageRecords;
    }


	@Override
    public List<UsageRecord> getTotalWeeklyUsageData(String metric)
    {
		return getWeeklyUsageData(metric, null, null);
    }
	

	@Override
    public List<UsageRecord> getWeeklyUsageData(String metric, String source)
    {
		return getWeeklyUsageData(metric, source, null);
    }
	
	
	@Override
    public List<UsageRecord> getWeeklyUsageData(String metric, String source,
            String service)
    {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		ArrayList<UsageRecord> usageRecords = new ArrayList<UsageRecord>();
		
		try
		{
			// Query calls a stored procedure in the form:
	    	// CALL usage_servers_week_query('total', ADDTIME(?, '-144:00:0'), ADDTIME(?, '24:00:0'), 'source', 'user');
			String query = "CALL usage_servers_week_query(?, ADDTIME(?, '-144:0:0'), ADDTIME(?, '24:0:0'), ?, ?)";
			logger.debug("getTotalWeeklyUsageData() running query: " + query);
			
			// Go between the latest usage date, and one week before.
			Date latestUsageDate = getLatestUsageDate();
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 00:00");
			String latestDayStartDate = formatter.format(latestUsageDate);
			
			
			pstmt = m_Connection.prepareStatement(query);
			pstmt.setString(1, metric);
			pstmt.setString(2, latestDayStartDate);
			pstmt.setString(3, latestDayStartDate);
			pstmt.setString(4, source);
			pstmt.setString(5, service);
			
			logger.debug("getWeeklyUsageData() latestDayStartDate: " + latestDayStartDate);	
			logger.debug("getWeeklyUsageData() source: " + source);	
			logger.debug("getWeeklyUsageData() service: " + service);	
			
			
			rs = pstmt.executeQuery();
			
			UsageRecord record;
			java.sql.Timestamp time;
			BigDecimal value;
			while (rs.next()) 
			{				
				time = rs.getTimestamp("time");
				value = rs.getBigDecimal(metric);
				if ( (time != null) && (value != null) )
				{
					record = new UsageRecord();
					record.set("time", time);
					record.set("value", value.intValue());
					usageRecords.add(record);
				}			
			}
		}
		catch (SQLException ex)
		{
			// handle any errors
			logger.error("Error running getWeeklyUsageData() query", ex);
		}
		finally 
		{
			// it is a good idea to release
			// resources in a finally{} block
			// in reverse-order of their creation
			// if they are no-longer needed
			if (rs != null)
			{
				try
				{
					rs.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
				rs = null;
			}
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
			}
		}

	    return usageRecords;
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
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		ArrayList<UsageRecord> usageRecords = new ArrayList<UsageRecord>();
		
		try
		{
			// Query calls a stored procedure in the form:
	    	// CALL usage_servers_week_query(?, ?, ?, ?, ?);
			String query = "CALL usage_servers_week_query(?, ?, ?, ?, ?)";
			logger.debug("getTimeLineWeeklyUsageData() running query: " + query);
			
			// Go between the latest usage date, and one week before.
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			String minDate = formatter.format(startDate);
			String maxDate = formatter.format(endDate);
			
			logger.debug("getTimeLineWeeklyUsageData() minDate: " + minDate);
			logger.debug("getTimeLineWeeklyUsageData() maxDate: " + maxDate);
			
			
			pstmt = m_Connection.prepareStatement(query);
			pstmt.setString(1, metric);
			pstmt.setString(2, minDate);
			pstmt.setString(3, maxDate);
			pstmt.setString(4, source);
			pstmt.setString(5, service);
			
			
			rs = pstmt.executeQuery();
			
			UsageRecord record;
			java.sql.Timestamp time;
			BigDecimal value;
			while (rs.next()) 
			{				
				time = rs.getTimestamp("time");
				value = rs.getBigDecimal(metric);
				if ( (time != null) && (value != null) )
				{
					record = new UsageRecord();
					record.set("time", time);
					record.set("value", value.intValue());
					usageRecords.add(record);
				}			
			}
		}
		catch (SQLException ex)
		{
			// handle any errors
			logger.error("Error running getWeeklyUsageData() query", ex);
		}
		finally 
		{
			// it is a good idea to release
			// resources in a finally{} block
			// in reverse-order of their creation
			// if they are no-longer needed
			if (rs != null)
			{
				try
				{
					rs.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
				rs = null;
			}
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
			}
		}

	    return usageRecords;
    }
    
    
    protected List<UsageRecord> getTimeLineDailyUsageData(Date startDate, Date endDate,
    		String metric, String source, String service)
    {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		ArrayList<UsageRecord> usageRecords = new ArrayList<UsageRecord>();
		
		try
		{
			// Query calls a stored procedure in the form:
	    	// CALL usage_servers_week_query(?, ?, ?, ?, ?);
			String query = "CALL usage_servers_day_query(?, ?, ?, ?, ?)";
			logger.debug("getTimeLineDailyUsageData() running query: " + query);
			
			// Go between the latest usage date, and one week before.
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			String minDate = formatter.format(startDate);
			String maxDate = formatter.format(endDate);
			
			logger.debug("getTimeLineDailyUsageData() minDate: " + minDate);
			logger.debug("getTimeLineDailyUsageData() maxDate: " + maxDate);
			
			
			pstmt = m_Connection.prepareStatement(query);
			pstmt.setString(1, metric);
			pstmt.setString(2, minDate);
			pstmt.setString(3, maxDate);
			pstmt.setString(4, source);
			pstmt.setString(5, service);
			
			
			rs = pstmt.executeQuery();
			
			UsageRecord record;
			java.sql.Timestamp time;
			BigDecimal value;
			while (rs.next()) 
			{				
				time = rs.getTimestamp("time");
				value = rs.getBigDecimal(metric);
				if ( (time != null) && (value != null) )
				{
					record = new UsageRecord();
					record.set("time", time);
					record.set("value", value.intValue());
					usageRecords.add(record);
				}			
			}
		}
		catch (SQLException ex)
		{
			// handle any errors
			logger.error("Error running getTimeLineDailyUsageData() query", ex);
		}
		finally 
		{
			// it is a good idea to release
			// resources in a finally{} block
			// in reverse-order of their creation
			// if they are no-longer needed
			if (rs != null)
			{
				try
				{
					rs.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
				rs = null;
			}
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
			}
		}

	    return usageRecords;
    }
    
    
    protected List<UsageRecord> getTimeLineHourlyUsageData(Date startDate, Date endDate,
    		String metric, String source, String service)
    {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		ArrayList<UsageRecord> usageRecords = new ArrayList<UsageRecord>();
		
		try
		{
			// Query calls a stored procedure in the form:
	    	// CALL usage_servers_week_query(?, ?, ?, ?, ?);
			String query = "CALL usage_servers_query(?, ?, ?, ?, ?)";
			logger.debug("getTimeLineHourlyUsageData() running query: " + query);
			
			// Go between the latest usage date, and one week before.
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			String minDate = formatter.format(startDate);
			String maxDate = formatter.format(endDate);
			
			logger.debug("getTimeLineHourlyUsageData() minDate: " + minDate);
			logger.debug("getTimeLineHourlyUsageData() maxDate: " + maxDate);
			
			
			pstmt = m_Connection.prepareStatement(query);
			pstmt.setString(1, metric);
			pstmt.setString(2, minDate);
			pstmt.setString(3, maxDate);
			pstmt.setString(4, source);
			pstmt.setString(5, service);
			
			
			rs = pstmt.executeQuery();
			
			UsageRecord record;
			java.sql.Timestamp time;
			BigDecimal value;
			while (rs.next()) 
			{				
				time = rs.getTimestamp("time");
				value = rs.getBigDecimal(metric);
				if ( (time != null) && (value != null) )
				{
					record = new UsageRecord();
					record.set("time", time);
					record.set("value", value.intValue());
					usageRecords.add(record);
				}			
			}
		}
		catch (SQLException ex)
		{
			// handle any errors
			logger.error("Error running getTimeLineHourlyUsageData() query", ex);
		}
		finally 
		{
			// it is a good idea to release
			// resources in a finally{} block
			// in reverse-order of their creation
			// if they are no-longer needed
			if (rs != null)
			{
				try
				{
					rs.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
				rs = null;
			}
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
			}
		}

	    return usageRecords;
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
				if (records.size() > 0)
				{
					date = records.get(0).getTime();
				}
				break;
				
			case DAY:
				records = getTotalDailyUsageData(date, metric);
				break;
				
			case HOUR:
				records = getTotalHourlyUsageData(date, metric);
				break;
		
		}
		
		
		return new DatePagingLoadResult<UsageRecord>(records, timeFrame,
				date, getEarliestUsageDate(), getLatestUsageDate());
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
		logger.debug("getUsageData() - date in loadConfig:" + date);
		
		List<UsageRecord> records = new ArrayList<UsageRecord>();
		
		switch (timeFrame)
		{
			case WEEK:
				records = getWeeklyUsageData(metric, source);
				if (records.size() > 0)
				{
					date = records.get(0).getTime();
				}
				break;
				
			case DAY:
				records = getDailyUsageData(date, metric, source);
				break;
				
			case HOUR:
				records = getHourlyUsageData(date, metric, source);
				break;
		
		}
		
		
		return new DatePagingLoadResult<UsageRecord>(records, timeFrame,
				date, getEarliestUsageDate(), getLatestUsageDate());
    }

	
	@Override
    public DatePagingLoadResult<UsageRecord> getUsageData(String metric,
            String source, String server, DatePagingLoadConfig config)
    {
		if (server == null)
		{
			return getUsageData(metric, source, config);
		}
		
		TimeFrame timeFrame = config.getTimeFrame();
		Date date = config.getDate();
		
		List<UsageRecord> records = new ArrayList<UsageRecord>();
		
		switch (timeFrame)
		{
			case WEEK:
				records = getWeeklyUsageData(metric, source, server);
				if (records.size() > 0)
				{
					date = records.get(0).getTime();
				}
				break;
				
			case DAY:
				records = getDailyUsageData(date, metric, source, server);
				break;
				
			case HOUR:
				records = getHourlyUsageData(date, metric, source, server);
				break;
		
		}
		
		
		return new DatePagingLoadResult<UsageRecord>(records, timeFrame,
				date, getEarliestUsageDate(), getLatestUsageDate());
    }
	
	
	public Date getEarliestUsageDate()
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		Date minTime = new Date();
		
		try
		{
			String query = "select MIN(time) from usage_servers;";
			
			pstmt = m_Connection.prepareStatement(query);
			rs = pstmt.executeQuery();
			
			while (rs.next()) 
			{				
				minTime = rs.getTimestamp(1);
			}
		}
		catch (SQLException ex)
		{
			// handle any errors
			logger.error("Error running getEarliestUsageDate() query", ex);
		}
		finally 
		{
			// it is a good idea to release
			// resources in a finally{} block
			// in reverse-order of their creation
			// if they are no-longer needed
			if (rs != null)
			{
				try
				{
					rs.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
				rs = null;
			}
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
			}
		}

	    return minTime;
	}
	
	
	public Date getLatestUsageDate()
	{
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		Date maxTime = new Date();
		
		try
		{
			String query = "select MAX(time) from usage_servers;";
			
			pstmt = m_Connection.prepareStatement(query);
			rs = pstmt.executeQuery();
			
			while (rs.next()) 
			{				
				maxTime = rs.getTimestamp(1);
			}
		}
		catch (SQLException ex)
		{
			// handle any errors
			logger.error("Error running getLatestUsageDate() query", ex);
		}
		finally 
		{
			// it is a good idea to release
			// resources in a finally{} block
			// in reverse-order of their creation
			// if they are no-longer needed
			if (rs != null)
			{
				try
				{
					rs.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
				rs = null;
			}
			if (pstmt != null)
			{
				try
				{
					pstmt.close();
				}
				catch (SQLException sqlEx)
				{
				} // ignore
			}
		}

	    return maxTime;
	}



}
