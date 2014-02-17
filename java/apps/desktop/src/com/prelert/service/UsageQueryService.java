package com.prelert.service;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import com.prelert.data.*;

/**
 * Defines the methods for the interface to the Usage Query service.
 * @author Pete Harverson
 */
public interface UsageQueryService extends RemoteService
{
	/**
	 * Returns the list of sources (servers) for which usage data is held in the
	 * Prelert database.
	 * @return List of source names.
	 */
	public List<String>			getSources();
	
	
	/**
	 * Returns the list of users for the specified source (server) for whom usage 
	 * data is held in the Prelert database.
	 * @param source name of source (server) for which to obtain the list of users.
	 * @return List of users for the specified source (server).
	 */
	public List<String>			getUsers(String source);
	
	
	/**
	 * Returns user usage data for all sources (servers) and all users for the 
	 * specified load config.
	 * @param metric name of usage metric to return data on e.g. serverload, pending.
	 * @param config load config specifying the range of data (e.g. time frame
	 * and date) to obtain.
	 * @return load result containing the requested usage data.
	 */
	public DatePagingLoadResult<UsageRecord> getUsageData(String metric, 
			DatePagingLoadConfig config);
	
	
	/**
	 * Returns user usage data for all users on the specified source (server).
	 * @param metric name of usage metric to return data on e.g. serverload, pending.
	 * @param name of source (server) for which to obtain user usage data.
	 * @param config load config specifying the range of data (e.g. time frame
	 * and date) to obtain.
	 * @return load result containing the requested usage data.
	 */
	public DatePagingLoadResult<UsageRecord> getUsageData(String metric,
			String source, DatePagingLoadConfig config);
	
	
	/**
	 * Returns user usage data for the specified user on the specified source (server).
	 * @param metric name of usage metric to return data on e.g. serverload, pending.
	 * @param name of source (server) for which to obtain usage data.
	 * @param name of user for which to obtain usage data.
	 * @param config load config specifying the range of data (e.g. time frame
	 * and date) to obtain.
	 * @return load result containing the requested usage data.
	 */
	public DatePagingLoadResult<UsageRecord> getUsageData(String metric,
			String source, String username, DatePagingLoadConfig config);
}