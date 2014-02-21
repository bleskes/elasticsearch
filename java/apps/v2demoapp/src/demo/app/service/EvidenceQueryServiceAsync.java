package demo.app.service;

import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import demo.app.data.EvidencePagingLoadConfig;
import demo.app.data.DatePagingLoadResult;
import demo.app.data.TimeFrame;
import demo.app.data.gxt.EvidenceModel;
import demo.app.data.gxt.GridRowInfo;


/**
 * Defines the methods to be implemented by the asynchronous client interface
 * to the Evidence query service.
 * @author Pete Harverson
 */
public interface EvidenceQueryServiceAsync
{
	public  void getFirstPage(EvidencePagingLoadConfig config, 
			AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
	
	public void getLastPage(EvidencePagingLoadConfig config, 
			AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
	
	public void getNextPage(
			EvidencePagingLoadConfig config, String bottomRowId, 
			AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
	
	public void getPreviousPage(
			EvidencePagingLoadConfig config, String topRowId, 
			AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
	
	public void getForDescription(
			EvidencePagingLoadConfig config, String description, 
			AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
	
	public void getAtTime(EvidencePagingLoadConfig config, 
			AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
	
	public void getIdPage(
			EvidencePagingLoadConfig config, int id,
			AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
	
	public void getRowInfo(int rowId, 
			AsyncCallback<List<GridRowInfo>> callback);
	
	public void getEvidenceSingle(int id,
			AsyncCallback<EvidenceModel> callback);
	
	public void getAllColumns(String dataType, TimeFrame timeFrame,
			AsyncCallback<List<String>> callback);
	
	public void getFilterableColumns(String dataType, 
			boolean getCompulsory, boolean getOptional, 
			AsyncCallback<List<String>> callback);
	
	public void getColumnValues(String dataType, String columnName, 
			AsyncCallback<List<String>> callback);
	
	public void getEarliestDate(String dataType, String source, AsyncCallback<Date> callback);
	
	public void getLatestDate(String dataType, String source, AsyncCallback<Date> callback);
}
