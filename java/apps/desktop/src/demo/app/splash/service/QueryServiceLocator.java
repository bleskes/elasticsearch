/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2010     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ***********************************************************/

package demo.app.splash.service;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

import demo.app.service.CausalityQueryService;
import demo.app.service.CausalityQueryServiceAsync;
import demo.app.service.EvidenceQueryService;
import demo.app.service.EvidenceQueryServiceAsync;
import demo.app.service.TimeSeriesGXTPagingService;
import demo.app.service.TimeSeriesGXTPagingServiceAsync;


/**
 * Service locator for returning references to the client-side asynchronous 
 * interfaces used for making queries to the server-side query services.
 * 
 * @author Pete Harverson
 */
public class QueryServiceLocator
{
	private static QueryServiceLocator 	s_Instance = new QueryServiceLocator();
	
	private DataSourceQueryServiceAsync 	m_DataSourceQueryService = null;
	private ViewCreationServiceAsync		m_ViewService = null;
	private EvidenceQueryServiceAsync 		m_EvidenceQueryService = null;
	private TimeSeriesGXTPagingServiceAsync	m_TimeSeriesQueryService = null;
	private CausalityQueryServiceAsync 		m_CausalityQueryService = null;
	private LogoutServiceAsync				m_LogoutService = null;
	
	
	public static QueryServiceLocator getInstance()
	{
		return s_Instance;
	}
	
	
	/**
	 * Returns a reference to the client-side asynchronous interface to the service
	 * used for making data source queries.
	 * @return the client-side asynchronous interface to the data source query service.
	 */
	public DataSourceQueryServiceAsync getDataSourceQueryService()
	{	
		if (m_DataSourceQueryService == null)
		{
			// Instantiate the service
			m_DataSourceQueryService = (DataSourceQueryServiceAsync)(GWT.create(DataSourceQueryService.class));

			// Specify the URL at which the service implementation is running.
			// The target URL must reside on the same domain and port from
			// which the host page was served.
			((ServiceDefTarget) m_DataSourceQueryService).setServiceEntryPoint(GWT.getModuleBaseURL() + 
											"services/dataSourceQueryService");
		}
		
		return m_DataSourceQueryService;
	}
	
	
	/**
	 * Returns a reference to the client-side asynchronous interface to the service
	 * used for making calls to the View Creation service queries.
	 * @return the client-side asynchronous interface to the data source query service.
	 */
	public ViewCreationServiceAsync getViewCreationService()
	{	
		if (m_ViewService == null)
		{
			// Instantiate the service
			m_ViewService = (ViewCreationServiceAsync)(GWT.create(ViewCreationService.class));

			// Specify the URL at which the service implementation is running.
			// The target URL must reside on the same domain and port from
			// which the host page was served.
			((ServiceDefTarget) m_ViewService).setServiceEntryPoint(GWT.getModuleBaseURL() + 
											"services/viewCreationService");
		}
		
		return m_ViewService;
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
	 * used for making time series queries.
	 * @return the client-side asynchronous interface to the View Directory service.
	 */
	public TimeSeriesGXTPagingServiceAsync getTimeSeriesGXTQueryService()
	{	
		if (m_TimeSeriesQueryService == null)
		{
			// Instantiate the service
			m_TimeSeriesQueryService = (TimeSeriesGXTPagingServiceAsync)(GWT.create(TimeSeriesGXTPagingService.class));

			// Specify the URL at which the service implementation is running.
			// The target URL must reside on the same domain and port from
			// which the host page was served.
			((ServiceDefTarget) m_TimeSeriesQueryService).setServiceEntryPoint(GWT.getModuleBaseURL() + 
											"services/timeSeriesGXTQueryService");
		}
		
		return m_TimeSeriesQueryService;
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
	 * used for logging the user out of the application.
	 * @return the client-side asynchronous interface to the Logout service.
	 */
	public LogoutServiceAsync getLogoutService()
	{
		if (m_LogoutService == null)
		{
			// Instantiate the service
			m_LogoutService = (LogoutServiceAsync)(GWT.create(LogoutService.class));

			// Specify the URL at which the service implementation is running.
			// The target URL must reside on the same domain and port from
			// which the host page was served.
			((ServiceDefTarget) m_LogoutService).setServiceEntryPoint(GWT.getModuleBaseURL() + 
											"services/logoutService");
		}
		
		return m_LogoutService;
	}
}
