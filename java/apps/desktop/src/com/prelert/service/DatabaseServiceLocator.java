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

package com.prelert.service;

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
	
	private ListViewQueryServiceAsync 		m_ListViewQueryService = null;
	private EvidenceQueryServiceAsync 		m_EvidenceQueryService = null;
	private ExceptionQueryServiceAsync 		m_ExceptionQueryService = null;
	private CausalityQueryServiceAsync 		m_CausalityQueryService = null;
	private TimeSeriesGXTPagingServiceAsync	m_TimeSeriesPagingService = null;
	private ViewDirectoryServiceAsync 		m_ViewDirectoryService = null;

	
	protected DatabaseServiceLocator()
    {

    }
	
	
	public static DatabaseServiceLocator getInstance()
	{
		return s_Instance;
	}
	
	
	/**
	 * Returns a reference to the client-side asynchronous interface to the service
	 * used for making List View queries.
	 * @return the client-side asynchronous interface to the List View query service.
	 */
	public ListViewQueryServiceAsync getListViewQueryService()
	{	
		if (m_ListViewQueryService == null)
		{
			// Instantiate the service
			m_ListViewQueryService = (ListViewQueryServiceAsync)(GWT.create(ListViewQueryService.class));
			
			// Specify the URL at which the service implementation is running.
			// The target URL must reside on the same domain and port from
			// which the host page was served.
			((ServiceDefTarget)m_ListViewQueryService).setServiceEntryPoint(GWT.getModuleBaseURL() + 
											"services/listViewQueryService");
			
			/*
			if (GWT.isScript() == true)
			{
				// Specify the URL at which the service implementation is running.
				// The target URL must reside on the same domain and port from
				// which the host page was served.
				((ServiceDefTarget)m_ListViewQueryService).setServiceEntryPoint(GWT.getModuleBaseURL() + 
												"services/listViewQueryService");
			}
			else
			{
				// For when running in hosted mode in the GWT browser.
	
				// Specify the URL at which the service implementation is running.
				// The target URL must reside on the same domain and port from
				// which the host page was served.
				((ServiceDefTarget)m_ListViewQueryService).setServiceEntryPoint(GWT.getModuleBaseURL() + 
												"/hostedservices/listViewQueryService");
			}
			*/
		}
		return m_ListViewQueryService;
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
			((ServiceDefTarget)m_EvidenceQueryService).setServiceEntryPoint(GWT.getModuleBaseURL() + 
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
			
			/*
			if (GWT.isScript() == true)
			{
				// Specify the URL at which the service implementation is running.
				// The target URL must reside on the same domain and port from
				// which the host page was served.
				((ServiceDefTarget)m_ViewDirectoryService).setServiceEntryPoint(GWT.getModuleBaseURL() + 
												"services/viewDirectoryService");
			}
			else
			{
				// For when running in hosted mode in the GWT browser.
	
				// Specify the URL at which the service implementation is running.
				// The target URL must reside on the same domain and port from
				// which the host page was served.
				((ServiceDefTarget)m_ViewDirectoryService).setServiceEntryPoint(GWT.getModuleBaseURL() + 
												"/hostedservices/viewDirectoryService");
			}
			*/
		}
		return m_ViewDirectoryService;
	}
	
}
