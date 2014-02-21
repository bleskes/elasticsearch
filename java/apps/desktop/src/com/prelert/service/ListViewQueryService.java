package com.prelert.service;

import java.util.List;

import com.extjs.gxt.ui.client.data.PagingLoadConfig;
import com.google.gwt.user.client.rpc.RemoteService;

import com.prelert.data.*;
import com.prelert.data.gxt.GridRowInfo;


/**
 * Defines the methods for the interface to the generic List View Query service.
 * @author Pete Harverson
 */
public interface ListViewQueryService extends RemoteService
{	
	public List<String> getAllColumns(ListView view);
	
	public ListViewLoadResult<EventRecord> getRecords(ListView view, PagingLoadConfig config);
	
	public ListViewLoadResult<EventRecord> getRecordsWithRow(ListView view, int limit, 
			String selectRowFilterAttribute, String selectRowFilterValue);
	
	public List<GridRowInfo> getRowInfo(ListView view, int id);
	
	public int getRowNumber(ListView view, String selectRowFilterAttribute, 
			String selectRowFilterValue);
	
}
