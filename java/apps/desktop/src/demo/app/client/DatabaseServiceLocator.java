package demo.app.client;

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
			GWT.log("getEvidenceQueryServiceInstance() URL: " + GWT.getModuleBaseURL() + 
					"DataQuery/EvidenceQueryService", null);
			((ServiceDefTarget) m_EvidenceQueryService).setServiceEntryPoint(GWT.getModuleBaseURL() + 
											"services/evidenceQueryService");
		}
		
		return m_EvidenceQueryService;
	}
}
