package demo.app.client;

import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import demo.app.data.DatePagingLoadConfig;
import demo.app.data.DatePagingLoadResult;
import demo.app.data.UsageRecord;

public interface UsageQueryServiceAsync
{
	public void getSources(AsyncCallback<List<String>> callback);
	
	public void getUsers(String source, AsyncCallback<List<String>> callback);
	
	public void getTotalWeeklyUsageData(String metric, AsyncCallback<List<UsageRecord>> callback);
	
	public void getTotalDailyUsageData(Date date, String metric, AsyncCallback<List<UsageRecord>> callback);
	
	public void getTotalHourlyUsageData(Date date, String metric, AsyncCallback<List<UsageRecord>> callback);
	
	public void getWeeklyUsageData(String metric, String source, AsyncCallback<List<UsageRecord>> callback);
	
	public void getWeeklyUsageData(String metric, String source, String username,
			AsyncCallback<List<UsageRecord>> callback);
	
	public void getDailyUsageData(Date date, String metric, String source, 
			AsyncCallback<List<UsageRecord>> callback);
	
	public void getDailyUsageData(Date date, String metric, String source, String username,
			AsyncCallback<List<UsageRecord>> callback);
	
	public void getHourlyUsageData(Date date, String metric, String source, 
			AsyncCallback<List<UsageRecord>> callback);
	
	public void getHourlyUsageData(Date date, String metric, String source, String username,
			AsyncCallback<List<UsageRecord>> callback);
	
	
	public void getTimeLineUsageData(Date zoomStart, Date zoomEnd,
			String metric, String source, String username,
			AsyncCallback<List<UsageRecord>> callback);
	
	
	public void getUsageData(String metric, DatePagingLoadConfig config,
			AsyncCallback<DatePagingLoadResult<UsageRecord>> callback);
	
	public void getUsageData(String metric, String source, DatePagingLoadConfig config,
			AsyncCallback<DatePagingLoadResult<UsageRecord>> callback);
	
	public void getUsageData(String metric, String source, String user, 
			DatePagingLoadConfig config, 
			AsyncCallback<DatePagingLoadResult<UsageRecord>> callback);
}
