package demo.app.client;

import com.extjs.gxt.ui.client.data.RpcProxy;
import com.google.gwt.user.client.rpc.AsyncCallback;

import demo.app.client.chart.UserUsageWindow;
import demo.app.data.*;
import demo.app.service.UsageQueryServiceAsync;


/**
 * RpcProxy that retrieves user usage data from the Usage query service via
 * GWT remote procedure calls.
 */
public class GetUserUsageDataRpcProxy<D extends DatePagingLoadResult>
	extends RpcProxy<DatePagingLoadResult<UsageRecord>>
{
	private UsageQueryServiceAsync 	m_UsageQueryService;
	private UserUsageWindow	m_UserUsageChartWindow;

	
	public GetUserUsageDataRpcProxy(UserUsageWindow chartWindow,
			UsageQueryServiceAsync queryService)
	{
		m_UserUsageChartWindow = chartWindow;
		m_UsageQueryService = queryService;
	}
	
	
    protected void load(Object loadConfig,
            AsyncCallback<DatePagingLoadResult<UsageRecord>> callback)
    {
    	String source = m_UserUsageChartWindow.getSource();
    	String metric = "total";
		String username = m_UserUsageChartWindow.getUsername();
		m_UsageQueryService.getUsageData(metric, source, username, 
				(DatePagingLoadConfig) loadConfig, callback);
    }

}
