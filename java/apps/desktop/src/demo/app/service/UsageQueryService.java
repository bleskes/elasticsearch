package demo.app.service;

import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import demo.app.data.*;

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
	public List<UsageRecord>	getTimeLineUsageData(Date zoomStart, Date zoomEnd,
			String metric, String source, String username);
	
}
