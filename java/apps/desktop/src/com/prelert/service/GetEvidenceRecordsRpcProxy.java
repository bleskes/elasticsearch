package com.prelert.service;

import com.extjs.gxt.ui.client.data.RpcProxy;
import com.extjs.gxt.ui.client.widget.Info;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.prelert.data.*;


/**
 * RpcProxy that retrieves user evidence data from the evidence query service via
 * GWT remote procedure calls.
 */
public class GetEvidenceRecordsRpcProxy<C extends EvidencePagingLoadConfig, D extends DatePagingLoadResult>
	extends RpcProxy<EvidencePagingLoadConfig, DatePagingLoadResult<EventRecord>> 
{
	private EvidenceQueryServiceAsync 	m_EvidenceQueryService = null;
	
	
	public GetEvidenceRecordsRpcProxy(EvidenceQueryServiceAsync queryService)
	{
		m_EvidenceQueryService = queryService;
	}

	
    public void load(EvidencePagingLoadConfig loadConfig,
            AsyncCallback<DatePagingLoadResult<EventRecord>> callback)
    {
    	loadFirstPage(loadConfig, callback);
    }
    
    
    public void loadFirstPage(EvidencePagingLoadConfig loadConfig,
            AsyncCallback<DatePagingLoadResult<EventRecord>> callback)
    {
    	m_EvidenceQueryService.getFirstPage(loadConfig, callback);
    }
    
    
    public void loadLastPage(EvidencePagingLoadConfig loadConfig,
            AsyncCallback<DatePagingLoadResult<EventRecord>> callback)
    {
    	m_EvidenceQueryService.getLastPage(loadConfig, callback);
    }
    
    
    public void loadNextPage(EvidencePagingLoadConfig loadConfig, String bottomRowId, 
    		AsyncCallback<DatePagingLoadResult<EventRecord>> callback)
    {
    	m_EvidenceQueryService.getNextPage(loadConfig, bottomRowId, callback);
    }
    
    
    public void loadPreviousPage(EvidencePagingLoadConfig loadConfig, String topRowId, 
    		AsyncCallback<DatePagingLoadResult<EventRecord>> callback)
    {
    	m_EvidenceQueryService.getPreviousPage(loadConfig, topRowId, callback);
    }
    
    
    public void loadAtTime(EvidencePagingLoadConfig loadConfig, 
    		AsyncCallback<DatePagingLoadResult<EventRecord>> callback)
    {
    	m_EvidenceQueryService.getAtTime(loadConfig, callback);
    }
    
    
    public void loadAtId(EvidencePagingLoadConfig loadConfig, int id,
    		AsyncCallback<DatePagingLoadResult<EventRecord>> callback)
    {
    	m_EvidenceQueryService.getIdPage(loadConfig, id, callback);
    }
    
    
    public void loadForDescription(EvidencePagingLoadConfig loadConfig, String description,
    		AsyncCallback<DatePagingLoadResult<EventRecord>> callback)
    {
    	m_EvidenceQueryService.getForDescription(loadConfig, description, callback);
    }

}