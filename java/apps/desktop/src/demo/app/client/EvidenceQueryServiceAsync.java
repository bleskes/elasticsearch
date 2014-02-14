package demo.app.client;

import java.util.List;

import com.extjs.gxt.ui.client.data.BaseListLoadResult;
import com.google.gwt.user.client.rpc.AsyncCallback;

import demo.app.data.EventRecord;
import demo.app.data.EvidencePagingLoadConfig;
import demo.app.data.DatePagingLoadResult;
import demo.app.data.GridRowInfo;
import demo.app.data.TimeFrame;

public interface EvidenceQueryServiceAsync
{
	public  void getFirstPage(EvidencePagingLoadConfig config, 
			AsyncCallback<DatePagingLoadResult<EventRecord>> callback);
	
	public void getLastPage(EvidencePagingLoadConfig config, 
			AsyncCallback<DatePagingLoadResult<EventRecord>> callback);
	
	public void getNextPage(
			EvidencePagingLoadConfig config, String bottomRowId, 
			AsyncCallback<DatePagingLoadResult<EventRecord>> callback);
	
	public void getPreviousPage(
			EvidencePagingLoadConfig config, String topRowId, 
			AsyncCallback<DatePagingLoadResult<EventRecord>> callback);
	
	public void getForDescription(
			EvidencePagingLoadConfig config, String description, 
			AsyncCallback<DatePagingLoadResult<EventRecord>> callback);
	
	public void getAtTime(EvidencePagingLoadConfig config, 
			AsyncCallback<DatePagingLoadResult<EventRecord>> callback);
	
	public void getRowInfo(int rowId, 
			AsyncCallback<List<GridRowInfo>> callback);
	
	public void getAllColumns(TimeFrame timeFrame,
			AsyncCallback<List<String>> callback);
	
	public void getColumnValues(String columnName, 
			AsyncCallback<List<String>> callback);
}
