package demo.app.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

import demo.app.data.DatePagingLoadConfig;
import demo.app.data.DatePagingLoadResult;

public interface ListViewQueryServiceAsync
{
	public  void getFirstPage(DatePagingLoadConfig config, 
			AsyncCallback<DatePagingLoadResult<EvidenceLog>> callback);
	
	public void getLastPage(DatePagingLoadConfig config, 
			AsyncCallback<DatePagingLoadResult<EvidenceLog>> callback);
	
	public void getNextPage(
			DatePagingLoadConfig config, int bottomRowId, 
			AsyncCallback<DatePagingLoadResult<EvidenceLog>> callback);
	
	public void getPreviousPage(
			DatePagingLoadConfig config, int topRowId, 
			AsyncCallback<DatePagingLoadResult<EvidenceLog>> callback);
	
	public void getForDescription(
			DatePagingLoadConfig config, String description, 
			AsyncCallback<DatePagingLoadResult<EvidenceLog>> callback);
	
	public void getAtTime(DatePagingLoadConfig config, 
			AsyncCallback<DatePagingLoadResult<EvidenceLog>> callback);
}
