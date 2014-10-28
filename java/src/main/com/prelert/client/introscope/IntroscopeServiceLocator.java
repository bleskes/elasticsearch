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

package com.prelert.client.introscope;

import com.google.gwt.core.client.GWT;
import com.prelert.service.introscope.IntroscopeConfigService;
import com.prelert.service.introscope.IntroscopeConfigServiceAsync;


/**
 * Service locator for services specific to the Prelert Diagnostics for 
 * CA APM (Introscope) UI. It returns references to the client-side asynchronous 
 * interfaces used for making calls to the server-side configuration and control services.
 * 
 * @author Pete Harverson
 */
public class IntroscopeServiceLocator
{
	private static IntroscopeServiceLocator 	s_Instance = new IntroscopeServiceLocator();
	
	private IntroscopeConfigServiceAsync	m_ConfigService;
	
	
	private IntroscopeServiceLocator()
	{
		
	}
	
	
	/**
	 * Returns the singleton instance of the ServiceLocator object used to return
	 * references to the services specific to the Prelert Diagnostics for 
	 * CA APM (Introscope) UI.
	 * @return the <code>IntroscopeServiceLocator</code> instance.
	 */
	public static IntroscopeServiceLocator getInstance()
	{
		return s_Instance;
	}
	
	
	/**
	 * Returns a reference to the client-side asynchronous interface to the service
	 * used for making configuration requests.
	 * @return the client-side asynchronous interface to the User query service.
	 */
	public IntroscopeConfigServiceAsync getConfigService()
	{	
		if (m_ConfigService == null)
		{
			m_ConfigService = (IntroscopeConfigServiceAsync)(GWT.create(IntroscopeConfigService.class));
		}
		
		return m_ConfigService;
	}
	
}
