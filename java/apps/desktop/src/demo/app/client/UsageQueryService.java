package demo.app.client;

import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import demo.app.data.*;

public interface UsageQueryService extends RemoteService
{
	public List<String>			getSources();
	
	public List<String>			getUsers(String source);
	
	public List<UsageRecord> 	getTotalWeeklyUsageData(String metric);
	
	public List<UsageRecord>	getTotalDailyUsageData(Date date, String metric);
	
	public List<UsageRecord>	getTotalHourlyUsageData(Date date, String metric);
	
	public List<UsageRecord>	getWeeklyUsageData(String metric, String source);
	
	public List<UsageRecord>	getWeeklyUsageData(String metric, 
			String source, String username);
	
	public List<UsageRecord>	getDailyUsageData(Date date, String metric, 
			String source);
	
	public List<UsageRecord>	getDailyUsageData(Date date, String metric, 
			String source, String username);
	
	public List<UsageRecord>	getHourlyUsageData(Date date, String metric, 
			String source);
	
	public List<UsageRecord>	getHourlyUsageData(Date date, String metric, 
			String source, String username);
	
	
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
	
	public DatePagingLoadResult<UsageRecord> getUsageData(String metric, 
			DatePagingLoadConfig config);
	
	public DatePagingLoadResult<UsageRecord> getUsageData(String metric, String source, 
			DatePagingLoadConfig config);
	
	public DatePagingLoadResult<UsageRecord> getUsageData(String metric, String source, String user, 
			DatePagingLoadConfig config);
}
