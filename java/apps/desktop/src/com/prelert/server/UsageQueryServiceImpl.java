package com.prelert.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.prelert.dao.UsageViewDAO;
import com.prelert.data.*;
import com.prelert.service.UsageQueryService;

import org.apache.log4j.Logger;

import org.springframework.transaction.*;
import org.springframework.transaction.support.*;


/**
 * Server-side implementation of the service for retrieving usage data from the
 * Prelert database.
 * @author Pete Harverson
 */
public class UsageQueryServiceImpl extends RemoteServiceServlet 
	implements UsageQueryService
{
	static Logger logger = Logger.getLogger(UsageQueryServiceImpl.class);
	
	private UsageViewDAO 	m_UsageViewDAO;
	private TransactionTemplate	m_TxTemplate;

	
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
	 * Sets the transaction manager to be used when running queries and updates
	 * to the Prelert database within transactions.
	 * @param txManager Spring PlatformTransactionManager to manage database transactions.
	 */
	public void setTransactionManager(PlatformTransactionManager txManager)
	{
		m_TxTemplate = new TransactionTemplate(txManager);
		m_TxTemplate.setReadOnly(true);
		m_TxTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
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
    		String metric, DatePagingLoadConfig config)
    {
    	// Run all the DB queries within a transaction.
    	final DatePagingLoadConfig loadConfig = config;
    	final String metricName = metric;
    	
    	Object pagingLoadResult = m_TxTemplate.execute(new TransactionCallback(){

            public DatePagingLoadResult<UsageRecord> doInTransaction(TransactionStatus status)
            {
				TimeFrame timeFrame = loadConfig.getTimeFrame();
				Date date = loadConfig.getDate();
				
				List<UsageRecord> records = new ArrayList<UsageRecord>();
				
				switch (timeFrame)
				{
					case WEEK:
						records = m_UsageViewDAO.getWeeklyUsageData(metricName);
						break;
						
					case DAY:
						if (date != null)
						{
							records = m_UsageViewDAO.getDailyUsageData(date, metricName);
						}
						break;
						
					case HOUR:
						if (date != null)
						{
							records = m_UsageViewDAO.getHourlyUsageData(date, metricName);
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
			
		});
		
		return (DatePagingLoadResult<UsageRecord>)pagingLoadResult;
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
    		String metric, String source, DatePagingLoadConfig config)
    {	
		if (source == null)
		{
			return getUsageData(metric, config);
		}
		
		// Run all the DB queries within a transaction.
    	final DatePagingLoadConfig loadConfig = config;
    	final String sourceName = source;
    	final String metricName = metric;
    	
    	Object pagingLoadResult = m_TxTemplate.execute(new TransactionCallback(){

            public DatePagingLoadResult<UsageRecord> doInTransaction(TransactionStatus status)
            {
		
				TimeFrame timeFrame = loadConfig.getTimeFrame();
				Date date = loadConfig.getDate();
				
				List<UsageRecord> records = new ArrayList<UsageRecord>();
				
				switch (timeFrame)
				{
					case WEEK:
						records = m_UsageViewDAO.getWeeklyUsageData(metricName, sourceName);
						break;
						
					case DAY:
						if (date != null)
						{
							records = m_UsageViewDAO.getDailyUsageData(date, 
								metricName, sourceName);
						}
						break;
						
					case HOUR:
						if (date != null)
						{
							records = m_UsageViewDAO.getHourlyUsageData(date, 
								metricName, sourceName);
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
			
		});
	
    	return (DatePagingLoadResult<UsageRecord>)pagingLoadResult;
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
    		String metric, String source, String user, DatePagingLoadConfig config)
    {
		if (user == null)
		{
			return getUsageData(metric, source, config);
		}
		
		// Run all the DB queries within a transaction.
    	final DatePagingLoadConfig loadConfig = config;
    	final String sourceName = source;
    	final String username = user;
    	final String metricName = metric;
    	
    	Object pagingLoadResult = m_TxTemplate.execute(new TransactionCallback(){
    		 public DatePagingLoadResult<UsageRecord> doInTransaction(TransactionStatus status)
             {
		
				TimeFrame timeFrame = loadConfig.getTimeFrame();
				Date date = loadConfig.getDate();
				
				List<UsageRecord> records = new ArrayList<UsageRecord>();
				
				switch (timeFrame)
				{
					case WEEK:
						records = m_UsageViewDAO.getWeeklyUsageData(metricName, 
								sourceName, username);
						break;
						
					case DAY:
						if (date != null)
						{
							records = m_UsageViewDAO.getDailyUsageData(date, 
								metricName, sourceName, username);
						}
						break;
						
					case HOUR:
						if (date != null)
						{
							records = m_UsageViewDAO.getHourlyUsageData(date, 
								metricName, sourceName, username);
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
    	});
    	
    	return (DatePagingLoadResult<UsageRecord>)pagingLoadResult;
    }
	
}
