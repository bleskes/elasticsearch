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

package com.prelert.client.diagnostics;

import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout.VBoxLayoutAlign;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.client.ClientUtil;
import com.prelert.client.URLParameterNames;
import com.prelert.client.admin.AdminServiceLocator;
import com.prelert.client.event.GXTEvents;
import com.prelert.client.event.ShowModuleEvent;
import com.prelert.data.CavStatus;
import com.prelert.data.CavStatus.CavRunState;
import com.prelert.data.MarketingMessages;
import com.prelert.service.admin.AnalysisControlServiceAsync;
import com.prelert.service.AsyncServiceLocator;
import com.prelert.service.MarketingMessageServiceAsync;
import com.prelert.splash.AnalysisModule;
import com.prelert.splash.ExplorerModule;
import com.prelert.splash.GUIBuilder;
import com.prelert.splash.IncidentsModule;


/**
 * GWT entry point for the Prelert Diagnostics UI. It adds Welcome and Run Configuration
 * modules to the standard Activity, Analysis and Explorer modules.
 * @author Pete Harverson
 */
public abstract class DiagnosticsUIBuilder extends GUIBuilder
{
	public static DiagnosticsMessages DIAGNOSTIC_MESSAGES = GWT.create(DiagnosticsMessages.class);
	
	private AnalysisControlServiceAsync			m_AnalysisControlService;
	
	/** Frequency of query to obtain status of analysis, in seconds. */
	public static final int STATUS_QUERY_FREQUENCY_SECS = 10;
	
	private DiagnosticsWelcomeModule			m_WelcomeModule;
	private DiagnosticsConfigModuleComponent	m_ConfigModule;
	
	
	@Override
	protected void initComponents()
	{
		// Add the standard modules (Activity, Analysis, Explorer).
		super.initComponents();
		
		m_AnalysisControlService = AdminServiceLocator.getInstance().getAnalysisControlService();
		
		// Hide the Refresh button on the timeline.
		IncidentsModule incidentsModule = (IncidentsModule)(m_Modules.get(IncidentsModule.MODULE_ID));
		incidentsModule.showTimelineAutoRefreshButton(false);

		addWelcomeModule();
		
		m_ConfigModule = addConfigModule();
		m_ConfigModule.addListener(GXTEvents.ShowModuleClick, new Listener<ShowModuleEvent>(){

            public void handleEvent(ShowModuleEvent be)
            {
            	m_NavigationBar.setActiveModuleId(be.getModuleId(), false);
            	setActiveModule(be.getModuleId());
            }
			
		});
		
		// Create the marketing message banner.
		final MessageBanner banner = new MessageBanner();
		ApplicationResponseHandler<MarketingMessages> callback = 
			new ApplicationResponseHandler<MarketingMessages>()
		{

			@Override
            public void uponSuccess(MarketingMessages messages)
            {
	            banner.setMessages(messages);
            }

			@Override
            public void uponFailure(Throwable caught)
            {
	            GWT.log("Error obtaining marketing messages from server: " + caught);
            }	
		};
		
		// Populate message banner with marketing messages via server.
		MarketingMessageServiceAsync messageService = 
			AsyncServiceLocator.getInstance().getMarketingMessageService();
		messageService.getMessages(callback);
		
		
		// Add the navigation bar and message banner at the top of the viewport.
		LayoutContainer northPanel = new LayoutContainer();
		VBoxLayout layout = new VBoxLayout();   
		layout.setVBoxLayoutAlign(VBoxLayoutAlign.STRETCH);   
		northPanel.setLayout(layout); 
		
		northPanel.add(m_NavigationBar);
		northPanel.add(banner);
		
		BorderLayoutData northData = new BorderLayoutData(LayoutRegion.NORTH, 80);    
        northData.setFloatable(false);   
		  
        m_Viewport.add(northPanel, northData);
		
		
		// Disable Activity, Analysis and Explorer modules initially.
		setResultModulesEnabled(false);

		if (Window.Location.getParameter(URLParameterNames.MODULE) == null)
		{
			m_NavigationBar.setActiveModuleId(DiagnosticsWelcomeModule.MODULE_ID);
		}
		
		// Create the timer to query for the CAV status.
		Timer statusQueryTimer = new Timer()
        {
			@Override
            public void run()
            {
				updateAnalysisStatus();
            }
        };
        statusQueryTimer.scheduleRepeating(STATUS_QUERY_FREQUENCY_SECS * 1000);
        
        // Get the initial status.
        statusQueryTimer.run();
	}
	
	
	/**
	 * Adds the Welcome module.
	 */
	protected void addWelcomeModule()
	{
		m_WelcomeModule = new DiagnosticsWelcomeModule();
		m_WelcomeModule.setHeaderText(DIAGNOSTIC_MESSAGES.productName());
		
		m_ModulePanel.add(m_WelcomeModule.getComponent());
		m_Modules.put(DiagnosticsWelcomeModule.MODULE_ID, m_WelcomeModule);
		m_NavigationBar.insertModuleLink(DiagnosticsWelcomeModule.MODULE_ID, 
				ClientUtil.CLIENT_CONSTANTS.welcome(), 0);
		
		m_WelcomeModule.addListener(GXTEvents.ShowModuleClick, new Listener<ShowModuleEvent>(){

            public void handleEvent(ShowModuleEvent sme)
            {
            	String moduleId = sme.getModuleId();
            	if (moduleId.equals(m_ConfigModule.getModuleId()))
            	{
            		// If status is finished, reset fields in Config module to 
            		// allow a new analysis to be configured.
            		CavStatus status = m_ConfigModule.getAnalysisStatus();
            		if (status != null && status.getRunState() == CavRunState.CAV_FINISHED)
            		{
            			m_ConfigModule.resetForNewAnalysis();
            		}
            	}
            	
            	m_NavigationBar.setActiveModuleId(moduleId, false);
            	setActiveModule(moduleId);
            }
			
		});
	}
	
	
	/**
	 * Creates and adds the module used for configuring the diagnostics product.
	 * @return the Configuration module component.
	 */
	protected abstract DiagnosticsConfigModuleComponent addConfigModule();
	
	
	/**
	 * Queries the status of the analysis from the back-end in order to update
	 * the state of the modules.
	 */
	protected void updateAnalysisStatus()
	{
		ApplicationResponseHandler<CavStatus> callback = 
			new ApplicationResponseHandler<CavStatus>()
		{
			@Override
            public void uponFailure(Throwable caught)
			{
				GWT.log("Error obtaining status of analysis", caught);
				ClientUtil.showErrorMessage(DIAGNOSTIC_MESSAGES.errorLoadingAnalysisStatus());
			}


			@Override
            public void uponSuccess(CavStatus status)
			{
				GWT.log("DiagnosticsUIBuilder CavStatus: " + status);
				setAnalysisStatus(status);
			}
		};
		
		m_AnalysisControlService.getAnalysisStatus(callback);
	}
	
	
	/**
	 * Sets the current status of the analysis, enabling or disabling the various
	 * form controls in the module according to the run state.
	 * @param status the current status of the analysis.
	 */
	public void setAnalysisStatus(CavStatus status)
    {
		// Need to reset the result modules if status goes from running to finished.
		CavStatus oldStatus = m_ConfigModule.getAnalysisStatus();
		boolean wasRunning = false;
		if ( (oldStatus != null) && (oldStatus.getRunState() == CavRunState.CAV_RUNNING) )
		{
			wasRunning = true;
		}
		
		m_ConfigModule.setAnalysisStatus(status);
		
		// Enable/disable forms and controls depending on the run state.
		CavRunState runState = status.getRunState();
		switch (runState)
		{			
			case CAV_NOT_STARTED:
				setResultModulesEnabled(false);
				m_WelcomeModule.setRunAnalysisButtonText(DIAGNOSTIC_MESSAGES.runAnalysis());
				break;
				
			case CAV_RUNNING:
				setResultModulesEnabled(false);
				m_WelcomeModule.setRunAnalysisButtonText(DIAGNOSTIC_MESSAGES.viewAnalysisProgress());
				break;
				
			case CAV_PAUSED:
				// Not yet implemented pause/resume functionality.
				break;
				
			case CAV_STOPPED:
				setResultModulesEnabled(false);
				m_WelcomeModule.setRunAnalysisButtonText(DIAGNOSTIC_MESSAGES.runAnalysis());
				break;
			
			case CAV_FINISHED:
				if (wasRunning == true)
				{
					resetResultModules();
				}
				setResultModulesEnabled(true);
				m_WelcomeModule.setRunAnalysisButtonText(DIAGNOSTIC_MESSAGES.runNewAnalysis());
				break;
				
			case CAV_ERROR:
				// Don't change current state - just display error message.
				ClientUtil.showErrorMessage(DIAGNOSTIC_MESSAGES.errorAnalysisStatus(
						status.getFatalErrorMessage()));
				break;
		}
    }
	
	
	/**
	 * Enables or disables the links in the navigation bar to the 'result' modules 
	 * i.e. the Activity, Analysis and Explorer modules.
	 * @param enabled
	 */
	protected void setResultModulesEnabled(boolean enabled)
	{
		m_NavigationBar.setModuleLinkEnabled(IncidentsModule.MODULE_ID, enabled);
		m_NavigationBar.setModuleLinkEnabled(AnalysisModule.MODULE_ID, enabled);
		m_NavigationBar.setModuleLinkEnabled(ExplorerModule.MODULE_ID, enabled);
		
		m_WelcomeModule.showActivityModuleLink(enabled);
	}
	
	
	/**
	 * Resets the result modules to their default displays 
	 * i.e. the Activity, Analysis and Explorer modules.
	 */
	public void resetResultModules()
	{
		IncidentsModule incidentsModule = 
			(IncidentsModule)(m_Modules.get(IncidentsModule.MODULE_ID));
		incidentsModule.showLatestIncident();
		
		ExplorerModule explorerModule =
			(ExplorerModule)(m_Modules.get(ExplorerModule.MODULE_ID));
		explorerModule.resetView();
		
		AnalysisModule analysisModule = 
			(AnalysisModule)(m_Modules.get(AnalysisModule.MODULE_ID));
		analysisModule.clearAll();
		
		// TODO - if the Search module is ever added to the Diagnostics UI
		// it will need refreshing to populate:
		//  - display column names
		//  - list of data types for the 'Refine results to' Combo box
	}
}
