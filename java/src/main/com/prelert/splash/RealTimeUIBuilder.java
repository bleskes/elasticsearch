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

package com.prelert.splash;

import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.user.client.Window;
import com.prelert.client.ClientUtil;
import com.prelert.client.URLParameterNames;
import com.prelert.client.admin.AdminModule;
import com.prelert.client.event.GXTEvents;
import com.prelert.client.event.ShowModuleEvent;
import com.prelert.client.gxt.ModuleComponent;
import com.prelert.data.gxt.UserModel;


/**
 * GWT entry point for the Prelert real-time UI. It adds Admin and Search modules
 * in addition to the standard Activity, Analysis and Explorer modules.
 * @author Pete Harverson
 */
public class RealTimeUIBuilder extends GUIBuilder
{

	/**
	 * Creates and initialises the graphical components in the window.
	 */
	@Override
    protected void initComponents()
	{
		// Add the standard modules (Activity, Analysis, Explorer).
		super.initComponents();
		
		// Add a search box to the Navigation bar.
		m_NavigationBar.addSearchBox();
		m_NavigationBar.addListener(GXTEvents.RunSearchClick, new Listener<ShowModuleEvent>(){

            @Override
            public void handleEvent(ShowModuleEvent be)
            {
            	m_NavigationBar.setActiveModuleId(null, false);
            	
            	ModuleComponent module = m_Modules.get(SearchModule.MODULE_ID);
            	if (module == null)
            	{
            		addSearchModule(m_NavigationBar.getSearchText());
            	}
            	else
            	{
            		setActiveModule(SearchModule.MODULE_ID);
        		
            		// Run the search across all data types.
            		SearchModule searchModule = (SearchModule)module;
            		searchModule.runSearch(m_NavigationBar.getSearchText(), null);
            	}
            }
			
		});
		
		// Add the Admin module for user with the administrator role.
		if (ClientUtil.getLoggedInUser().getRoleName().equals(UserModel.ROLE_NAME_ADMINISTRATOR))
		{
			m_NavigationBar.addModuleLink(AdminModule.MODULE_ID, 
					AdminModule.ADMIN_MESSAGES.admin());
		}
		
		if (Window.Location.getParameter(URLParameterNames.MODULE) == null)
		{
			m_NavigationBar.setActiveModuleId(IncidentsModule.MODULE_ID);
		}
		
	}
	

    @Override
    protected void setActiveModule(String moduleId)
    {
    	if ( (moduleId == AdminModule.MODULE_ID) &&
    			(m_Modules.get(moduleId) == null) )
    	{
    		// Admin module is created on demand.
    		addAdminModule();
    	}
    	else
    	{
    		 super.setActiveModule(moduleId);
    	}
    }


	/**
	 * Adds in the Admin module, using a GWT split point.
	 */
	protected void addAdminModule()
	{
		GWT.runAsync(new RunAsyncCallback()
		{
			@Override
            public void onFailure(Throwable caught)
			{
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
						ClientUtil.CLIENT_CONSTANTS.errorDownloadingModule(), null);
			}

			@Override
            public void onSuccess()
			{
				AdminModule adminModule = new AdminModule();
				
				m_ModulePanel.add(adminModule.getComponent());
				m_Modules.put(adminModule.getModuleId(), adminModule);
				
				setActiveModule(adminModule.getModuleId());
			}
		});
	}
	
	
	/**
	 * Adds the Search module, using a GWT split point, and then runs a search
	 * for the notifications or time series features containing the specified text.
	 * @param searchText  the text to search for within attribute values.
	 */
	protected void addSearchModule(final String searchText)
	{
		GWT.runAsync(new RunAsyncCallback()
		{
			@Override
            public void onFailure(Throwable caught)
			{
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
						ClientUtil.CLIENT_CONSTANTS.errorDownloadingModule(), null);
			}

			@Override
            public void onSuccess()
			{
				SearchModule searchModule = new SearchModule();
				
				// Add a listener to the Search module for Open Causality View events.
				searchModule.addListener(
						GXTEvents.OpenCausalityViewClick, m_ShowAnalysisListener);
				
				// Add a listener to the Search Module for 'Show Data' events.
				searchModule.addListener(GXTEvents.OpenViewClick, m_ShowEvidenceListener);
				
				m_ModulePanel.add(searchModule.getComponent());
				m_Modules.put(SearchModule.MODULE_ID, searchModule);
				
				setActiveModule(SearchModule.MODULE_ID);
				searchModule.runSearch(searchText, null);
			}
		});
	}

}
