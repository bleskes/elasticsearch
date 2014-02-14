package com.prelert.service;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.prelert.data.*;


/**
 * Defines the methods to be implemented by the asynchronous client interface
 * to the Usage query service.
 * @author Pete Harverson
 */
public interface UsageQueryServiceAsync
{
	/**
	 * Makes a request for the list of sources (servers) for which usage data 
	 * is held in the Prelert database.
	 */
	public void getSources(AsyncCallback<List<String>> callback);
	

	/**
	 * Makes a request for the list of users for the specified source (server) 
	 * for whom usage data is held in the Prelert database.
	 */
	public void getUsers(String source, AsyncCallback<List<String>> callback);
	
	
	/**
	 * Makes a request for user usage data for all sources (servers) and all 
	 * users for the specified load config.
	 */
	public void getUsageData(String metric, DatePagingLoadConfig config,
			AsyncCallback<DatePagingLoadResult<UsageRecord>> callback);
	
	
	/**
	 * Makes a request for user usage data for all users on the 
	 * specified source (server).
	 */
	public void getUsageData(String metric, String source, 
			DatePagingLoadConfig config,
			AsyncCallback<DatePagingLoadResult<UsageRecord>> callback);
	
	
	/**
	 * Makes a request for user usage data for the specified user on the 
	 * specified source (server).
	 */
	public void getUsageData(String metric, String source, String username, 
			DatePagingLoadConfig config, 
			AsyncCallback<DatePagingLoadResult<UsageRecord>> callback);
	
}