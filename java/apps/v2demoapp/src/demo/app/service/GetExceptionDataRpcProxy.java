package demo.app.service;

import com.extjs.gxt.ui.client.data.RpcProxy;
import com.google.gwt.user.client.rpc.AsyncCallback;

import demo.app.data.DatePagingLoadResult;
import demo.app.data.ExceptionPagingLoadConfig;
import demo.app.data.gxt.EvidenceModel;


/**
 * RpcProxy that retrieves user evidence data for the Exception List 
 * from the exception query service via GWT remote procedure calls.
 */
public class GetExceptionDataRpcProxy<D extends DatePagingLoadResult<EvidenceModel>>
	extends RpcProxy<DatePagingLoadResult<EvidenceModel>> 
{

	private ExceptionQueryServiceAsync 	m_ExceptionQueryService = null;
	
	
	public GetExceptionDataRpcProxy(ExceptionQueryServiceAsync queryService)
	{
		m_ExceptionQueryService = queryService;
	}


    protected void load(Object loadConfig,
            AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback)
    {
    	loadFirstPage((ExceptionPagingLoadConfig)loadConfig, callback);   
    }
    
    
    public void loadFirstPage(ExceptionPagingLoadConfig loadConfig,
            AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback)
    {
    	m_ExceptionQueryService.getFirstPage(loadConfig, callback);
    }
    
    
    public void loadLastPage(ExceptionPagingLoadConfig loadConfig,
            AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback)
    {
    	m_ExceptionQueryService.getLastPage(loadConfig, callback);
    }
    
    
    public void loadNextPage(ExceptionPagingLoadConfig loadConfig, String bottomRowId, 
    		AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback)
    {
    	m_ExceptionQueryService.getNextPage(loadConfig, bottomRowId, callback);
    }
    
    
    public void loadPreviousPage(ExceptionPagingLoadConfig loadConfig, String topRowId, 
    		AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback)
    {
    	m_ExceptionQueryService.getPreviousPage(loadConfig, topRowId, callback);
    }
    
    
    public void loadAtTime(ExceptionPagingLoadConfig loadConfig, 
    		AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback)
    {
    	m_ExceptionQueryService.getAtTime(loadConfig, callback);
    }
    

}
