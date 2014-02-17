package com.prelert.service;

import java.util.List;

import com.extjs.gxt.ui.client.data.PagingLoadConfig;
import com.extjs.gxt.ui.client.data.PagingLoadResult;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.prelert.data.*;
import com.prelert.data.gxt.GridRowInfo;


/**
 * Defines the methods to be implemented by the asynchronous client interface
 * to the List View query service.
 * @author Pete Harverson
 */
public interface ListViewQueryServiceAsync
{	
	public void getAllColumns(ListView view, AsyncCallback<List<String>> callback);
	
	public void getRecords(ListView view, PagingLoadConfig config, 
				AsyncCallback<ListViewLoadResult<EventRecord>> callback);
	
	public void getRecordsWithRow(ListView view, int limit, 
			String selectRowFilterAttribute, String selectRowFilterValue, 
			AsyncCallback<ListViewLoadResult<EventRecord>> callback);
	
	public void getRowInfo(ListView view, int id, AsyncCallback<List<GridRowInfo>> callback);
	
	public void getRowNumber(ListView view, String selectRowFilterAttribute, 
			String selectRowFilterValue, AsyncCallback<Integer> callback);
}
