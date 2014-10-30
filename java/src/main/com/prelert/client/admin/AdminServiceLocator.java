/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2012     *
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

package com.prelert.client.admin;

import com.google.gwt.core.client.GWT;

import com.prelert.service.AlertingConfigService;
import com.prelert.service.AlertingConfigServiceAsync;
import com.prelert.service.UserQueryService;
import com.prelert.service.UserQueryServiceAsync;
import com.prelert.service.admin.AnalysisConfigService;
import com.prelert.service.admin.AnalysisConfigServiceAsync;
import com.prelert.service.admin.AnalysisControlService;
import com.prelert.service.admin.AnalysisControlServiceAsync;


/**
 * Service locator for the Admin module for returning references to the 
 * client-side asynchronous interfaces used for making calls to the 
 * server-side admin services.
 * 
 * @author Pete Harverson
 */
public class AdminServiceLocator
{
	private static AdminServiceLocator 	s_Instance = new AdminServiceLocator();
	
	private AlertingConfigServiceAsync			m_AlertingService = null;
	private UserQueryServiceAsync				m_UserService = null;
	private AnalysisConfigServiceAsync			m_AnalysisConfigService = null;
	private AnalysisControlServiceAsync			m_AnalysisControlService = null;
	
	
	private AdminServiceLocator()
	{
		
	}
	
	
	/**
	 * Returns the singleton instance of the service locator object used 
	 * to return references to the admin services in the server-side web layer.
	 * @return the ServiceLocator instance.
	 */
	public static AdminServiceLocator getInstance()
	{
		return s_Instance;
	}
	
	
	/**
	 * Returns a reference to the client-side asynchronous interface to the service
	 * used for configuring alerting functionality.
	 * @return the client-side asynchronous interface to the alerting configuration service.
	 */
	public AlertingConfigServiceAsync getAlertingConfigService()
	{
		if (m_AlertingService == null)
		{
			m_AlertingService = (AlertingConfigServiceAsync)(GWT.create(AlertingConfigService.class));
		}
		
		return m_AlertingService;
	}
	
	
	/**
	 * Returns a reference to the client-side asynchronous interface to the service
	 * used for making User queries.
	 * @return the client-side asynchronous interface to the User query service.
	 */
	public UserQueryServiceAsync getUserQueryService()
	{	
		if (m_UserService == null)
		{
			m_UserService = (UserQueryServiceAsync)(GWT.create(UserQueryService.class));
		}
		
		return m_UserService;
	}
	
	
	/**
	 * Returns a reference to the client-side asynchronous interface to
	 * the service used for configuring the analysis of data by the Prelert engine.
	 * @return the client-side asynchronous interface to the Analysis configuration service.
	 */
	public AnalysisConfigServiceAsync getAnalysisConfigService()
	{	
		if (m_AnalysisConfigService == null)
		{
			m_AnalysisConfigService = 
				(AnalysisConfigServiceAsync)(GWT.create(AnalysisConfigService.class));
		}
		
		return m_AnalysisConfigService;
	}
	
	
	/**
	 * Returns a reference to the client-side asynchronous interface to
	 * the service used for controlling the analysis of data by the Prelert engine.
	 * @return the client-side asynchronous interface to the Analysis control service.
	 */
	public AnalysisControlServiceAsync getAnalysisControlService()
	{	
		if (m_AnalysisControlService == null)
		{
			m_AnalysisControlService = 
				(AnalysisControlServiceAsync)(GWT.create(AnalysisControlService.class));
		}
		
		return m_AnalysisControlService;
	}
}
