package com.prelert.dao;

import java.util.Date;
import java.util.List;

import com.prelert.data.UsageRecord;

/**
 * Interface defining the methods to be implemented by a Data Access Object
 * to obtain information for Usage views from information stored in 
 * the Prelert database.
 * 
 * @author Pete Harverson
 */
public interface UsageViewDAO
{
	/**
	 * Returns the list of available sources of Usage data.
	 * @return the list of available sources.
	 */
	public List<String>			getSources();
	
	
	/**
	 * Returns the list of users for the specified source.
	 * @return list of users for the given source.
	 */
	public List<String>			getUsers(String source);
	
	
	/**
	 * Returns the total user usage for the last week from all servers.
	 * @return list of total user usage data from all servers.
	 */
	public List<UsageRecord>	getWeeklyUsageData(String metric);
	
	
	/**
     * Returns the usage data for the last week for the given source (server).
     * @param source the name of the source (server) for which to obtain usage data.
     * @return list of usage records, in ascending time order, for the given source.
     */
	public List<UsageRecord>	getWeeklyUsageData(String metric, String source);
	
	
    /**
     * Returns the usage data for the last week for the specified username on 
     * the given source (server).
     * @param source the name of the source (server) for which to obtain usage data.
     * @param username name of the user for which to obtain usage data.
     * @return list of usage records, in ascending time order, for the given source and user.
     */
	public List<UsageRecord>	getWeeklyUsageData(String metric, 
			String source, String username);
	
	
	/**
	 * Returns the total user usage from all servers for the specified day.
	 * @param date day for which to obtain user usage data.
	 * @return list of total user usage data from all servers.
	 */
	public List<UsageRecord>	getDailyUsageData(Date date, String metric);
	
	
	/**
     * Returns the usage data for the specified day for the given source (server).
     * @param date day for which to obtain user usage data.
     * @param source the name of the source (server) for which to obtain usage data.
     * @return list of usage records, in ascending time order, for the given source.
     */
	public List<UsageRecord>	getDailyUsageData(Date date, String metric, String source);
	
	
    /**
     * Returns the usage data for the specified day for the specified username on 
     * the given source (server).
     * @param date day for which to obtain user usage data.
     * @param source the name of the source (server) for which to obtain usage data.
     * @param username name of the user for which to obtain usage data.
     * @return list of usage records, in ascending time order, for the given source and user.
     */
	public List<UsageRecord>	getDailyUsageData(Date date, String metric, 
			String source, String username);
	
	
	/**
	 * Returns the total user usage from all servers for the specified hour.
	 * @param date hour for which to obtain user usage data.
	 * @return list of total user usage data from all servers.
	 */
	public List<UsageRecord>	getHourlyUsageData(Date date, String metric);
	
	
	/**
     * Returns the usage data for the specified hour for the given source (server).
     * @param date hour for which to obtain user usage data.
     * @param source the name of the source (server) for which to obtain usage data.
     * @return list of usage records, in ascending time order, for the given source.
     */
	public List<UsageRecord>	getHourlyUsageData(Date date, String metric, String source);
	
	
    /**
     * Returns the usage data for the specified hour for the specified username on 
     * the given source (server).
     * @param date hour for which to obtain user usage data.
     * @param source the name of the source (server) for which to obtain usage data.
     * @param username name of the user for which to obtain usage data.
     * @return list of usage records, in ascending time order, for the given source and user.
     */
	public List<UsageRecord>	getHourlyUsageData(Date date, 
			String metric, String source, String username);
	
	
	/**
	 * Returns the date of the earliest usage record in the Prelert database.
	 * @return date of earliest usage record.
	 */
	public Date 				getEarliestUsageDate();
	
	
	/**
	 * Returns the date of the latest usage record in the Prelert database.
	 * @return date of latest usage record.
	 */
	public Date 				getLatestUsageDate();
}
