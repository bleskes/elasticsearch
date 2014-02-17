package demo.app.server;

import java.util.*;
import java.util.Date;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import org.apache.log4j.Logger;

import demo.app.dao.UsageViewDAO;
import demo.app.data.DatePagingLoadConfig;
import demo.app.data.DatePagingLoadResult;
import demo.app.data.TimeFrame;
import demo.app.data.UsageRecord;
import demo.app.service.UsageQueryService;


public class UsageQueryServiceImpl extends RemoteServiceServlet 
	implements UsageQueryService
{
	static Logger logger = Logger.getLogger(UsageQueryServiceImpl.class);
	
	private UsageViewDAO 	m_UsageViewDAO;
	
	
	/**
	 * Returns the UsageViewDAO being used by the usage query service.
	 * @return the data access object for the usage view.
	 */
	public UsageViewDAO getUsageViewDAO()
    {
    	return m_UsageViewDAO;
    }


	/**
	 * Sets the UsageViewDAO to be used by the usage query service.
	 * @param usageViewDAO the data access object for the usage view.
	 */
	public void setUsageViewDAO(UsageViewDAO usageViewDAO)
    {
    	m_UsageViewDAO = usageViewDAO;
    }
	
	
	/**
	 * Returns the list of sources (servers) for which usage data is held in the
	 * Prelert database.
	 * @return List of source names.
	 */
	public List<String> getSources()
	{
		return m_UsageViewDAO.getSources();
	}


	/**
	 * Returns the list of users for the specified source (server) for whom usage 
	 * data is held in the Prelert database.
	 * @param source name of source (server) for which to obtain the list of users.
	 * @return List of users for the specified source (server).
	 */
	public List<String> getUsers(String source)
	{
		return m_UsageViewDAO.getUsers(source);
	}


	/**
	 * Returns user usage data for all sources (servers) and all users for the 
	 * specified load config.
	 * @param metric name of usage metric to return data on e.g. serverload, pending.
	 * @param config load config specifying the range of data (e.g. time frame
	 * and date) to obtain.
	 * @return load result containing the requested usage data.
	 */
    public DatePagingLoadResult<UsageRecord> getUsageData(
    		String metric, DatePagingLoadConfig loadConfig)
    { 	
		TimeFrame timeFrame = loadConfig.getTimeFrame();
		Date date = loadConfig.getDate();
		
		List<UsageRecord> records = new ArrayList<UsageRecord>();
		
		switch (timeFrame)
		{
			case WEEK:
				records = m_UsageViewDAO.getWeeklyUsageData(metric);
				break;
				
			case DAY:
				if (date != null)
				{
					records = m_UsageViewDAO.getDailyUsageData(date, metric);
				}
				break;
				
			case HOUR:
				if (date != null)
				{
					records = m_UsageViewDAO.getHourlyUsageData(date, metric);
				}
				break;
		
		}
		
		Date startDate = m_UsageViewDAO.getEarliestUsageDate();
		Date endDate = m_UsageViewDAO.getLatestUsageDate();
		
		Date loadResultTime = date;
		if (records != null && records.size() > 0)
		{
			loadResultTime = records.get(0).getTime();
		}
		
		return new DatePagingLoadResult<UsageRecord>(records, timeFrame,
				loadResultTime, startDate, endDate);

    }


	/**
	 * Returns user usage data for all users on the specified source (server).
	 * @param metric name of usage metric to return data on e.g. serverload, pending.
	 * @param source name of source (server) for which to obtain user usage data.
	 * @param config load config specifying the range of data (e.g. time frame
	 * and date) to obtain.
	 * @return load result containing the requested usage data.
	 */
    public DatePagingLoadResult<UsageRecord> getUsageData(
    		String metric, String source, DatePagingLoadConfig loadConfig)
    {	
		if (source == null)
		{
			return getUsageData(metric, loadConfig);
		}
		
		TimeFrame timeFrame = loadConfig.getTimeFrame();
		Date date = loadConfig.getDate();
		
		List<UsageRecord> records = new ArrayList<UsageRecord>();
		
		switch (timeFrame)
		{
			case WEEK:
				records = m_UsageViewDAO.getWeeklyUsageData(metric, source);
				break;
				
			case DAY:
				if (date != null)
				{
					records = m_UsageViewDAO.getDailyUsageData(date, metric, source);
				}
				break;
				
			case HOUR:
				if (date != null)
				{
					records = m_UsageViewDAO.getHourlyUsageData(date, metric, source);
				}
				break;
		
		}
		
		Date startDate = m_UsageViewDAO.getEarliestUsageDate();
		Date endDate = m_UsageViewDAO.getLatestUsageDate();
		
		Date loadResultTime = date;
		if (records != null && records.size() > 0)
		{
			loadResultTime = records.get(0).getTime();
		}
	
		return new DatePagingLoadResult<UsageRecord>(records, timeFrame,
				loadResultTime, startDate, endDate);

	}


	/**
	 * Returns user usage data for the specified user on the specified source (server).
	 * @param metric name of usage metric to return data on e.g. serverload, pending.
	 * @param source name of source (server) for which to obtain usage data.
	 * @param user name of user for which to obtain usage data.
	 * @param config load config specifying the range of data (e.g. time frame
	 * and date) to obtain.
	 * @return load result containing the requested usage data.
	 */
    public DatePagingLoadResult<UsageRecord> getUsageData(
    		String metric, String source, String user, DatePagingLoadConfig loadConfig)
    {
		if (user == null)
		{
			return getUsageData(metric, source, loadConfig);
		}
		
		TimeFrame timeFrame = loadConfig.getTimeFrame();
		Date date = loadConfig.getDate();
		
		List<UsageRecord> records = new ArrayList<UsageRecord>();
		
		switch (timeFrame)
		{
			case WEEK:
				records = m_UsageViewDAO.getWeeklyUsageData(metric, 
						source, user);
				break;
				
			case DAY:
				if (date != null)
				{
					records = m_UsageViewDAO.getDailyUsageData(date, 
						metric, source, user);
				}
				break;
				
			case HOUR:
				if (date != null)
				{
					records = m_UsageViewDAO.getHourlyUsageData(date, 
						metric, source, user);
				}
				break;
		
		}
		
		Date startDate = m_UsageViewDAO.getEarliestUsageDate();
		Date endDate = m_UsageViewDAO.getLatestUsageDate();
		
		Date loadResultTime = date;
		if (records != null && records.size() > 0)
		{
			loadResultTime = records.get(0).getTime();
		}
		
		return new DatePagingLoadResult<UsageRecord>(records, timeFrame,
				loadResultTime, startDate, endDate);
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
			String metric, String source, String username)
	{
		// PH: 13-7-09 - To implement if a switch is made to the timeline usage charts.
		
		List<UsageRecord> records = null;
		
		if (source != null)
		{
			if (username != null)
			{
				records = m_UsageViewDAO.getWeeklyUsageData(metric, source, username);
			}
			else
			{
				records = m_UsageViewDAO.getWeeklyUsageData(metric, source);
			}
		}
		else
		{
			records = m_UsageViewDAO.getWeeklyUsageData(metric);
		}
			
		return records;
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
	
}
