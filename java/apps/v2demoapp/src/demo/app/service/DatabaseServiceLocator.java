package demo.app.service;

import java.util.HashMap;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.ServiceDefTarget;


/**
 * Service locator for returning references to the client-side asynchronous 
 * interfaces used for making queries to the server-side database services.
 * These services include ones for making queries for List Views and Usage Views.
 * 
 * @author Pete Harverson
 */
public class DatabaseServiceLocator
{
	private static DatabaseServiceLocator 	s_Instance = new DatabaseServiceLocator();
	
	private EvidenceQueryServiceAsync 		m_EvidenceQueryService = null;
	private ExceptionQueryServiceAsync 		m_ExceptionQueryService = null;
	private CausalityQueryServiceAsync 		m_CausalityQueryService = null;
	private HashMap<String, UsageQueryServiceAsync> m_UsageQueryServices = null;
	private TimeSeriesGXTPagingServiceAsync	m_TimeSeriesPagingService = null;
	private ViewDirectoryServiceAsync 		m_ViewDirectoryService = null;
	private StatesQueryServiceAsync 		m_StatesQueryService = null;	// Old functionality.
	
	
	protected DatabaseServiceLocator()
    {
		// Create the table of UsageQueryServices - 
		// hashed on their serviceId e.g. Users, Servers, IPC.
		m_UsageQueryServices = new HashMap<String, UsageQueryServiceAsync>();
    }
	
	public static DatabaseServiceLocator getInstance()
	{
		return s_Instance;
	}
	
	
	/**
	 * Returns a reference to the client-side asynchronous interface to the service
	 * used for making Evidence View queries.
	 * @return the client-side asynchronous interface to the Evidence View query service.
	 */
	public EvidenceQueryServiceAsync getEvidenceQueryService()
	{	
		if (m_EvidenceQueryService == null)
		{
			// Instantiate the service
			m_EvidenceQueryService = (EvidenceQueryServiceAsync)(GWT.create(EvidenceQueryService.class));

			// Specify the URL at which the service implementation is running.
			// The target URL must reside on the same domain and port from
			// which the host page was served.
			((ServiceDefTarget) m_EvidenceQueryService).setServiceEntryPoint(GWT.getModuleBaseURL() + 
											"services/evidenceQueryService");
		}
		
		return m_EvidenceQueryService;
	}
	
	
	/**
	 * Returns a reference to the client-side asynchronous interface to the service
	 * used for making queries for exception data.
	 * @return the client-side asynchronous interface to the Exception query service.
	 */
	public ExceptionQueryServiceAsync getExceptionQueryService()
	{	
		if (m_ExceptionQueryService == null)
		{
			// Instantiate the service
			m_ExceptionQueryService = (ExceptionQueryServiceAsync)(GWT.create(ExceptionQueryService.class));

			// Specify the URL at which the service implementation is running.
			// The target URL must reside on the same domain and port from
			// which the host page was served.
			((ServiceDefTarget) m_ExceptionQueryService).setServiceEntryPoint(GWT.getModuleBaseURL() + 
											"services/exceptionQueryService");
		}
		
		return m_ExceptionQueryService;
	}
	
	
	/**
	 * Returns a reference to the client-side asynchronous interface to the service
	 * used for making Causality View queries.
	 * @return the client-side asynchronous interface to the Causality View query service.
	 */
	public CausalityQueryServiceAsync getCausalityQueryService()
	{	
		if (m_CausalityQueryService == null)
		{
			// Instantiate the service
			m_CausalityQueryService = (CausalityQueryServiceAsync)(GWT.create(CausalityQueryService.class));

			// Specify the URL at which the service implementation is running.
			// The target URL must reside on the same domain and port from
			// which the host page was served.
			((ServiceDefTarget) m_CausalityQueryService).setServiceEntryPoint(GWT.getModuleBaseURL() + 
											"services/causalityQueryService");
		}
		
		return m_CausalityQueryService;
	}
	
	
	/**
	 * Returns a reference to the client-side asynchronous interface to the service
	 * used for making usage queries.
	 * @param serviceId the id of the usage service to be queried e.g. Users or Services,
	 * which will be appended to the end of the URL of the usage query service.
	 * @return the client-side asynchronous interface to the View Directory service.
	 */
	public UsageQueryServiceAsync getUsageQueryService(String serviceId)
	{	
		UsageQueryServiceAsync usageQueryService = m_UsageQueryServices.get(serviceId);
		
		if (usageQueryService == null)
		{
			// Instantiate the service
			usageQueryService = (UsageQueryServiceAsync)(GWT.create(UsageQueryService.class));
			
			// Specify the URL at which the service implementation is running.
			// The target URL must reside on the same domain and port from
			// which the host page was served.
			((ServiceDefTarget)usageQueryService).setServiceEntryPoint(GWT.getModuleBaseURL() + 
											"services/usageQueryService/" + serviceId);

			
			m_UsageQueryServices.put(serviceId, usageQueryService);
		}
		return usageQueryService;
	}
	
	
	/**
	 * Returns a reference to the client-side asynchronous interface to the service
	 * used for making usage queries.
	 * @param serviceId the id of the usage service to be queried e.g. Users or Services,
	 * which will be appended to the end of the URL of the usage query service.
	 * @return the client-side asynchronous interface to the View Directory service.
	 */
	public TimeSeriesGXTPagingServiceAsync getTimeSeriesGXTPagingService()
	{	
		if (m_TimeSeriesPagingService == null)
		{
			// Instantiate the service
			m_TimeSeriesPagingService = (TimeSeriesGXTPagingServiceAsync)(GWT.create(TimeSeriesGXTPagingService.class));

			// Specify the URL at which the service implementation is running.
			// The target URL must reside on the same domain and port from
			// which the host page was served.
			((ServiceDefTarget) m_TimeSeriesPagingService).setServiceEntryPoint(GWT.getModuleBaseURL() + 
											"services/timeSeriesGXTPagingService");
		}
		
		return m_TimeSeriesPagingService;
	}
	
	
	/**
	 * Returns a reference to the client-side asynchronous interface to the service
	 * used for making queries to the ViewDirectory.
	 * @return the client-side asynchronous interface to the View Directory service.
	 */
	public ViewDirectoryServiceAsync getViewDirectoryService()
	{	
		if (m_ViewDirectoryService == null)
		{
			// Instantiate the service
			m_ViewDirectoryService = (ViewDirectoryServiceAsync)(GWT.create(ViewDirectoryService.class));
			
			
			((ServiceDefTarget)m_ViewDirectoryService).setServiceEntryPoint(GWT.getModuleBaseURL() + 
					"services/viewDirectoryService");
		}
		return m_ViewDirectoryService;
	}
	
	
	public StatesQueryServiceAsync getStatesQueryService()
	{
		if (m_StatesQueryService == null)
		{
			// Instantiate the service
			m_StatesQueryService = (StatesQueryServiceAsync)(GWT.create(StatesQueryService.class));

			// Specify the URL at which the service implementation is running.
			// The target URL must reside on the same domain and port from
			// which the host page was served.
			((ServiceDefTarget) m_StatesQueryService).setServiceEntryPoint(GWT.getModuleBaseURL() + 
											"services/statesQueryService");
		}
		return m_StatesQueryService;
	}
}
