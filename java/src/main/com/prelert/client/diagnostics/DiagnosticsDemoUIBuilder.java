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

import java.util.HashMap;

import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.event.BorderLayoutEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.LayoutEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.CollapsePanel;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.button.ToolButton;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout.VBoxLayoutAlign;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.client.ClientUtil;
import com.prelert.client.StaticFileURLs;
import com.prelert.client.URLParameterNames;
import com.prelert.client.event.GXTEvents;
import com.prelert.client.event.ShowModuleEvent;
import com.prelert.client.gxt.ModuleHelpPanel;
import com.prelert.client.openapi.OpenAPIConfigModule;
import com.prelert.data.MarketingMessages;
import com.prelert.service.AsyncServiceLocator;
import com.prelert.service.MarketingMessageServiceAsync;
import com.prelert.splash.AnalysisModule;
import com.prelert.splash.ExplorerModule;
import com.prelert.splash.GUIBuilder;
import com.prelert.splash.IncidentsModule;


/**
 * GWT entry point for the Diagnostics Demo UI. It adds the OpenAPI Run configuration
 * module to the standard Activity, Analysis and Explorer modules, and displays
 * help text for the UI in a separate panel on the left hand side of the viewport.
 * @author Pete Harverson
 */
public class DiagnosticsDemoUIBuilder extends GUIBuilder
{
	public static DiagnosticsMessages DIAGNOSTIC_MESSAGES = GWT.create(DiagnosticsMessages.class);
	
	private ModuleHelpPanel	m_HelpPanel;
	
	
	@Override
	public void onModuleLoad()
	{
		// No authentication required, just create the graphical components.
		initComponents();
		
		// Add UI to the body of the host HTML page.
		RootPanel.get("prelertGUI").add(m_Viewport);	
	}


	@Override
	protected void initComponents()
	{	
		// Add the standard modules (Activity, Analysis, Explorer).
		super.initComponents();
		
		// Hide the Refresh button on the timeline.
		IncidentsModule incidentsModule = (IncidentsModule)(m_Modules.get(IncidentsModule.MODULE_ID));
		incidentsModule.showTimelineAutoRefreshButton(false);
		
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
		
		// Add the Run Config module.
        DiagnosticsConfigModuleComponent configModule = addConfigModule();
        configModule.addListener(GXTEvents.ShowModuleClick, new Listener<ShowModuleEvent>(){

            @Override
			public void handleEvent(ShowModuleEvent be)
            {
            	m_NavigationBar.setActiveModuleId(be.getModuleId(), false);
            	setActiveModule(be.getModuleId());
            }
			
		});
		
		// Create the Help panel.
		StaticFileURLs fileURLs = GWT.create(StaticFileURLs.class);
		HashMap<String, String> helpPages = new HashMap<String, String>();
		helpPages.put(configModule.getModuleId(), fileURLs.help_run_view());
		helpPages.put(IncidentsModule.MODULE_ID, fileURLs.help_activity_view());
		helpPages.put(AnalysisModule.MODULE_ID, fileURLs.help_analysis_view());
		helpPages.put(ExplorerModule.MODULE_ID, fileURLs.help_explorer_view());
		
		m_HelpPanel = new ModuleHelpPanel();
		m_HelpPanel.setPagesByModule(helpPages);
		
		BorderLayoutData westData = new BorderLayoutData(LayoutRegion.WEST, 300, 200, 500);   
		westData.setSplit(true);    
		westData.setCollapsible(true); 
		westData.setFloatable(false); 
		westData.setMargins(new Margins(10, 0, 10, 10)); 
		m_Viewport.add(m_HelpPanel, westData);  
		
		// Add tooltips to the Help panel collapse/expand buttons.
		BorderLayout viewportLayout = (BorderLayout)(m_Viewport.getLayout());
		viewportLayout.addListener(Events.Collapse, new Listener<BorderLayoutEvent>(){
	    	 
 			@Override
             public void handleEvent(BorderLayoutEvent be)
             {
 				// Note a CollapsePanel is recreated each time a layout region is collapsed.
 				CollapsePanel cp = (CollapsePanel)(m_HelpPanel.getData("collapse"));
 				if (cp != null)
 				{
 					cp.getCollapseButton().setToolTip(ClientUtil.CLIENT_CONSTANTS.showHelpText());
 				}
             }
 			
 		});
		
		viewportLayout.addListener(Events.AfterLayout, new Listener<LayoutEvent>(){

			@Override
            public void handleEvent(LayoutEvent be)
            {	
				ToolButton collapseBtn = (ToolButton) m_HelpPanel.getData("collapseBtn");
				if (collapseBtn != null)
				{
					collapseBtn.setToolTip(ClientUtil.CLIENT_CONSTANTS.hideHelpText());
				}
            }
			
		});
		
		// Show the Activity module by default on opening.
		String openingModuleId = Window.Location.getParameter(URLParameterNames.MODULE);
		if (openingModuleId == null)
		{
			m_NavigationBar.setActiveModuleId(IncidentsModule.MODULE_ID);
		}
		else
		{
			m_HelpPanel.setActiveModule(openingModuleId);
		}
	}
	
	
	/**
	 * Creates and adds the module used for configuring the diagnostics product.
	 * @return the OpenAPI configuration module component.
	 */
    protected DiagnosticsConfigModuleComponent addConfigModule()
    {
    	// Use OpenAPI Run Config page, connecting to the demo implementation of the
    	// AnalysisConfigService and AnalysisControlService.
		OpenAPIConfigModule configModule = new OpenAPIConfigModule();
		
		m_ModulePanel.add(configModule.getComponent());
		m_Modules.put(configModule.getModuleId(), configModule);
		m_NavigationBar.insertModuleLink(configModule.getModuleId(), 
				DIAGNOSTIC_MESSAGES.configModuleLabel(), 0);
		
		return configModule;
    }
    
    
    @Override
	protected void setActiveModule(String moduleId)
	{
    	super.setActiveModule(moduleId);
    	
		// Update the contents of the help panel.
		// (Note this method is called before creation of the Help panel if
		// an opening module ID is passed in the URL).
		if (m_HelpPanel != null)
		{
			m_HelpPanel.setActiveModule(moduleId);
		}
	}

}
