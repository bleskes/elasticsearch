package demo.app.service;

import com.extjs.gxt.ui.client.data.RpcProxy;
import com.google.gwt.user.client.rpc.AsyncCallback;

import demo.app.data.DatePagingLoadResult;
import demo.app.data.EvidencePagingLoadConfig;
import demo.app.data.gxt.EvidenceModel;

/**
 * RpcProxy that retrieves user evidence data from the evidence query service via
 * GWT remote procedure calls.
 */
public class GetEvidenceDataRpcProxy<D extends DatePagingLoadResult>
	extends RpcProxy<DatePagingLoadResult<EvidenceModel>> 
{
	private EvidenceQueryServiceAsync 	m_EvidenceQueryService = null;
	
	
	public GetEvidenceDataRpcProxy(EvidenceQueryServiceAsync queryService)
	{
		m_EvidenceQueryService = queryService;
	}


    protected void load(Object loadConfig,
            AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback)
    {
    	loadFirstPage((EvidencePagingLoadConfig)loadConfig, callback);   
    }
    
    
    public void loadFirstPage(EvidencePagingLoadConfig loadConfig,
            AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback)
    {
    	m_EvidenceQueryService.getFirstPage(loadConfig, callback);
    }
    
    
    public void loadLastPage(EvidencePagingLoadConfig loadConfig,
            AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback)
    {
    	m_EvidenceQueryService.getLastPage(loadConfig, callback);
    }
    
    
    public void loadNextPage(EvidencePagingLoadConfig loadConfig, String bottomRowId, 
    		AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback)
    {
    	m_EvidenceQueryService.getNextPage(loadConfig, bottomRowId, callback);
    }
    
    
    public void loadPreviousPage(EvidencePagingLoadConfig loadConfig, String topRowId, 
    		AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback)
    {
    	m_EvidenceQueryService.getPreviousPage(loadConfig, topRowId, callback);
    }
    
    
    public void loadAtTime(EvidencePagingLoadConfig loadConfig, 
    		AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback)
    {
    	m_EvidenceQueryService.getAtTime(loadConfig, callback);
    }
    
    
    public void loadAtId(EvidencePagingLoadConfig loadConfig, int id,
    		AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback)
    {
    	m_EvidenceQueryService.getIdPage(loadConfig, id, callback);
    }
    
    
    public void loadForDescription(EvidencePagingLoadConfig loadConfig, String description,
    		AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback)
    {
    	m_EvidenceQueryService.getForDescription(loadConfig, description, callback);
    }

}
