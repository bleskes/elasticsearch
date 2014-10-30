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

import com.prelert.client.diagnostics.DiagnosticsConfigModuleComponent;
import com.prelert.client.diagnostics.DiagnosticsUIBuilder;
import com.prelert.client.diagnostics.DiagnosticsWelcomeModule;


/**
 * GWT entry point for the Prelert Diagnostics for CA APM (Introscope) UI.
 * @author Pete Harverson
 */
public class IntroscopeDiagnosticsUIBuilder extends DiagnosticsUIBuilder
{
	public static IntroscopeMessages INTROSCOPE_MESSAGES = GWT.create(IntroscopeMessages.class);
	
	
	@Override
	protected void initComponents()
	{
		// Add the modules (Welcome, Run, Activity, Analysis, Explorer).
		super.initComponents();
		
		// Set the intro text on the Welcome page.
		DiagnosticsWelcomeModule welcomeModule = (DiagnosticsWelcomeModule)(m_Modules.get(DiagnosticsWelcomeModule.MODULE_ID));
		welcomeModule.setIntroText(INTROSCOPE_MESSAGES.welcomeIntroscopeIntro());
	}
	
	
	@Override
	protected DiagnosticsConfigModuleComponent addConfigModule()
	{
		IntroscopeConfigModule configModule = new IntroscopeConfigModule();
		
		m_ModulePanel.add(configModule.getComponent());
		m_Modules.put(configModule.getModuleId(), configModule);
		m_NavigationBar.insertModuleLink(configModule.getModuleId(), 
				DIAGNOSTIC_MESSAGES.configModuleLabel(), 1);
		
		return configModule;
	}
}
