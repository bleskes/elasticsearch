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

package com.prelert.splash;

import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.Viewport;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.CardLayout;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.widgetideas.graphics.client.Color;
import com.google.gwt.widgetideas.graphics.client.GWTCanvas;
import com.googlecode.gchart.client.GChart;
import com.googlecode.gchart.client.GChartCanvasFactory;
import com.googlecode.gchart.client.GChartCanvasLite;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.client.event.GXTEvents;
import com.prelert.client.event.NavigationBarEvent;
import com.prelert.client.event.RequestViewEvent;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.gxt.EvidenceModel;
import com.prelert.service.AsyncServiceLocator;


/**
 * Entry point for V3 Prelert UI which builds the main UI components.
 */
public class GUIBuilder implements EntryPoint
{
	private NavigationBar 	m_NavigationBar;
	
	private LayoutContainer m_ModulePanel;
	private CardLayout 		m_CardLayout;
	private ExplorerModule 	m_ExplorerModule;
	
	
	// Static block to tell GChart to use the the GWTCanvas widget to render 
	// any continuous, non-rectangular, chart aspects (solid fill pie slices,
    // continuously connected lines, etc.)
	static 
	{
		final class GWTCanvasBasedCanvasLite extends GWTCanvas implements GChartCanvasLite
		{
			// GChartCanvasLite requires CSS/RGBA color strings, but
			// GWTCanvas uses its own Color class instead, so we wrap:
			public void setStrokeStyle(String cssColor)
			{
				// Sharp angles of default MITER can overwrite adjacent pie slices.
				setLineJoin(GWTCanvas.ROUND);
				setStrokeStyle(new Color(cssColor));
			}


			public void setFillStyle(String cssColor)
			{
				setFillStyle(new Color(cssColor));
			}
		}
		
		final class GWTCanvasBasedCanvasFactory implements GChartCanvasFactory
		{
			public GChartCanvasLite create()
			{
				GChartCanvasLite result = new GWTCanvasBasedCanvasLite();
				return result;
			}
		}

		GChart.setCanvasFactory(new GWTCanvasBasedCanvasFactory());
	} 
	
	
	public void onModuleLoad()
	{
		// Create the graphical components.
		initComponents();
	}
	
	
	/**
	 * Creates and initialises the graphical components in the window.
	 */
	protected void initComponents()
	{
		Viewport container = new Viewport();
		
		BorderLayout containerLayout = new BorderLayout();
		containerLayout.setContainerStyle("prl-viewport");
		
		container.setLayout(containerLayout);
	
		
		// Create the container to hold the module contents.
		m_CardLayout = new CardLayout();
		m_ModulePanel = new LayoutContainer();
		m_ModulePanel.setLayout(m_CardLayout);
		
		
		// Create the top navigation bar.
		m_NavigationBar = new NavigationBar();
		m_NavigationBar.addListener(GXTEvents.LogoutClick, new Listener<NavigationBarEvent>(){

            public void handleEvent(NavigationBarEvent be)
            {
	            logout();
            }
			
		});
		m_NavigationBar.addListener(GXTEvents.ShowModuleClick, new Listener<NavigationBarEvent>(){

            public void handleEvent(NavigationBarEvent be)
            {
            	Component moduleComponent = be.getModule().getComponent();
        		m_CardLayout.setActiveItem(moduleComponent);
        		
            	if (be.getModule().getClass() == ExplorerModule.class)
            	{
            		if (m_ExplorerModule.isViewReady() == false)
            		{
            			m_ExplorerModule.showAnalysedData();
            		}
            	}	
            }
			
		});
		
		// Create a listener for 'OpenViewClick' events to show the relevant data
		// in the Explorer Module.
		Listener<RequestViewEvent<EvidenceModel>> showDataListener = 
				new Listener<RequestViewEvent<EvidenceModel>>(){

            public void handleEvent(RequestViewEvent<EvidenceModel> rve)
            {
				m_NavigationBar.setActiveModuleId(m_ExplorerModule.getModuleId(), false);
            	m_CardLayout.setActiveItem(m_ExplorerModule);
            	
            	EvidenceModel selected = rve.getModel();
            	m_ExplorerModule.showData(selected);
            }
			
		};

		// Add the Incidents module.
		IncidentsModule incidentsModule = addIncidentsModule();
		incidentsModule.addListener(GXTEvents.OpenViewClick, showDataListener);
		
		// Add the Explorer module.		
		m_ExplorerModule = new ExplorerModule();
		m_ModulePanel.add(m_ExplorerModule.getComponent());
		m_NavigationBar.addModuleLink(m_ExplorerModule);
		
		// Add the Search module.
		SearchModule searchModule = addSearchModule();
		searchModule.addListener(GXTEvents.OpenViewClick, showDataListener);
		
		m_NavigationBar.setActiveModuleId(incidentsModule.getModuleId());
		
		BorderLayoutData northData = new BorderLayoutData(LayoutRegion.NORTH, 50);     
        northData.setFloatable(false);   
        northData.setMargins(new Margins(10, 10, 10, 10));  
        
        BorderLayoutData centerData = new BorderLayoutData(LayoutRegion.CENTER);   
        centerData.setMargins(new Margins(0, 10, 10, 10));
		  
        container.add(m_NavigationBar, northData);     
        container.add(m_ModulePanel, centerData);
        
        RootPanel.get("prelertGUI").add(container);
	}
	
	
	/**
	 * Adds in the Incidents module.
	 * @return the module that has been added.
	 */
	protected IncidentsModule addIncidentsModule()
	{		
		IncidentsModule incidentsModule = new IncidentsModule();
		m_ModulePanel.add(incidentsModule.getComponent());
		
		// Add listeners to the Incidents module for RequestViewEvents 
		// fired from the Causality View.
		incidentsModule.addListener(GXTEvents.OpenNotificationViewClick, 
				new Listener<RequestViewEvent<EvidenceModel>>(){

            public void handleEvent(RequestViewEvent<EvidenceModel> rve)
            {
            	m_NavigationBar.setActiveModuleId(m_ExplorerModule.getModuleId(), false);
            	m_CardLayout.setActiveItem(m_ExplorerModule);
            	
            	EvidenceModel evidence = rve.getModel();
            	m_ExplorerModule.showData(evidence);
            }
			
		});
		
		incidentsModule.addListener(GXTEvents.OpenTimeSeriesViewClick, 
				new Listener<RequestViewEvent<TimeSeriesConfig>>(){

            public void handleEvent(RequestViewEvent<TimeSeriesConfig> rve)
            {
            	m_NavigationBar.setActiveModuleId(m_ExplorerModule.getModuleId(), false);
            	m_CardLayout.setActiveItem(m_ExplorerModule);
            	
            	TimeSeriesConfig config = rve.getModel();
            	m_ExplorerModule.showTimeSeries(config, rve.getOpenAtTime());
            }
			
		});
		
		incidentsModule.addListener(GXTEvents.OpenViewClick, 
				new Listener<RequestViewEvent<EvidenceModel>>(){

            public void handleEvent(RequestViewEvent<EvidenceModel> rve)
            {
				m_NavigationBar.setActiveModuleId(m_ExplorerModule.getModuleId(), false);
            	m_CardLayout.setActiveItem(m_ExplorerModule);
            	
            	EvidenceModel selected = rve.getModel();
            	m_ExplorerModule.showData(selected);
            }
			
		});
		
		m_NavigationBar.addModuleLink(incidentsModule);
		
		return incidentsModule;
	}
	
	
	/**
	 * Adds in the Search functionality.
	 * @return the module that has been added.
	 */
	protected SearchModule addSearchModule()
	{
		// Create the Search module.
		final SearchModule searchModule = new SearchModule();
		m_ModulePanel.add(searchModule.getComponent());
		
		// Add a listener to the Search module for Open Causality View events.
		searchModule.addListener(GXTEvents.OpenCausalityViewClick, 
				new Listener<RequestViewEvent<EvidenceModel>>(){

			@Override
            public void handleEvent(RequestViewEvent<EvidenceModel> rve)
            {
				m_NavigationBar.setActiveModuleId(m_ExplorerModule.getModuleId(), false);
            	m_CardLayout.setActiveItem(m_ExplorerModule);
				
				EvidenceModel model = rve.getModel();
				if (model != null)
				{
					m_ExplorerModule.showCausalityData(model);
				}
            }
			
		});

		m_NavigationBar.addSearchBox();
		
		// Add a listener for a search being triggered from the search field in
		// the navigation bar.
		m_NavigationBar.addListener(GXTEvents.RunSearchClick, new Listener<NavigationBarEvent>(){

            public void handleEvent(NavigationBarEvent be)
            {
            	m_NavigationBar.setActiveModuleId(null, false);
        		m_CardLayout.setActiveItem(searchModule);
        		
        		// Run the search across all data types.
        		searchModule.runSearch(m_NavigationBar.getSearchText(), null);
            }
			
		});
		
		return searchModule;
	}
	
	
	/**
	 * Logs the user out of the application, returning them to the log in page.
	 */
	public void logout()
	{
		// Call the LogoutService to invalidate the Http session and
		// then reload the page.
		ApplicationResponseHandler<Void> callback = 
			new ApplicationResponseHandler<Void>() {
                @Override
                public void uponFailure(Throwable caught) 
                {
                	Window.Location.reload();
                }


                @Override
                public void uponSuccess(Void result)
                {
                	Window.Location.reload();
                }
          };
		
		AsyncServiceLocator.getInstance().getLogoutService().logout(callback);
	}

}
