package com.prelert.service;

import com.extjs.gxt.ui.client.data.RpcProxy;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.prelert.data.DatePagingLoadResult;
import com.prelert.data.EventRecord;
import com.prelert.data.ExceptionPagingLoadConfig;


/**
 * RpcProxy that retrieves user evidence data for the Exception List 
 * from the exception query service via GWT remote procedure calls.
 */
public class GetExceptionDataRpcProxy<C extends ExceptionPagingLoadConfig, 
	D extends DatePagingLoadResult<EventRecord>>
	extends RpcProxy<ExceptionPagingLoadConfig, DatePagingLoadResult<EventRecord>> 
{

	private ExceptionQueryServiceAsync 	m_ExceptionQueryService = null;
	
	
	public GetExceptionDataRpcProxy(ExceptionQueryServiceAsync queryService)
	{
		m_ExceptionQueryService = queryService;
	}


    protected void load(ExceptionPagingLoadConfig loadConfig,
            AsyncCallback<DatePagingLoadResult<EventRecord>> callback)
    {
    	loadFirstPage((ExceptionPagingLoadConfig)loadConfig, callback);   
    }
    
    
    public void loadFirstPage(ExceptionPagingLoadConfig loadConfig,
            AsyncCallback<DatePagingLoadResult<EventRecord>> callback)
    {
    	m_ExceptionQueryService.getFirstPage(loadConfig, callback);
    }
    
    
    public void loadLastPage(ExceptionPagingLoadConfig loadConfig,
            AsyncCallback<DatePagingLoadResult<EventRecord>> callback)
    {
    	m_ExceptionQueryService.getLastPage(loadConfig, callback);
    }
    
    
    public void loadNextPage(ExceptionPagingLoadConfig loadConfig, String bottomRowId, 
    		AsyncCallback<DatePagingLoadResult<EventRecord>> callback)
    {
    	m_ExceptionQueryService.getNextPage(loadConfig, bottomRowId, callback);
    }
    
    
    public void loadPreviousPage(ExceptionPagingLoadConfig loadConfig, String topRowId, 
    		AsyncCallback<DatePagingLoadResult<EventRecord>> callback)
    {
    	m_ExceptionQueryService.getPreviousPage(loadConfig, topRowId, callback);
    }
    
    
    public void loadAtTime(ExceptionPagingLoadConfig loadConfig, 
    		AsyncCallback<DatePagingLoadResult<EventRecord>> callback)
    {
    	m_ExceptionQueryService.getAtTime(loadConfig, callback);
    }
    

}
