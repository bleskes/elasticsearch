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

import java.util.HashMap;

import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.Viewport;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.CardLayout;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.widgetideas.graphics.client.Color;
import com.google.gwt.widgetideas.graphics.client.GWTCanvas;
import com.googlecode.gchart.client.GChart;
import com.googlecode.gchart.client.GChartCanvasFactory;
import com.googlecode.gchart.client.GChartCanvasLite;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.client.ClientUtil;
import com.prelert.client.URLParameterNames;
import com.prelert.client.admin.AdminServiceLocator;
import com.prelert.client.event.GXTEvents;
import com.prelert.client.event.ShowModuleEvent;
import com.prelert.client.event.RequestViewEvent;
import com.prelert.client.gxt.ModuleComponent;
import com.prelert.client.list.CausalityEvidenceDialog;
import com.prelert.data.CausalityView;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.gxt.EvidenceModel;
import com.prelert.data.gxt.UserModel;
import com.prelert.service.AsyncServiceLocator;
import com.prelert.service.UserQueryServiceAsync;


/**
 * Base GWT entry point class for Prelert user interfaces. It assembles the UI 
 * components for the standard application views, and manages navigation between 
 * the application modules.
 * @author Pete Harverson
 */
public class GUIBuilder implements EntryPoint
{
	protected NavigationBar 		m_NavigationBar;
	private ChangePasswordDialog	m_ChangePasswordDialog;
	
	protected Viewport			m_Viewport;
	protected LayoutContainer 	m_ModulePanel;
	private CardLayout 			m_CardLayout;
	
	private AnalysisModule 	m_AnalysisModule;
	private ExplorerModule 	m_ExplorerModule;
	protected Listener<RequestViewEvent<EvidenceModel>> 	m_ShowActivityListener;
	protected Listener<RequestViewEvent<EvidenceModel>> 	m_ShowAnalysisListener;
	protected Listener<RequestViewEvent<EvidenceModel>> 	m_ShowEvidenceListener;
	protected Listener<RequestViewEvent<TimeSeriesConfig>> 	m_ShowTimeSeriesListener;
	
	protected HashMap<String, ModuleComponent> 	m_Modules;
	
	
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
        // Get details of the logged in user to determine the UI modules to create.
        UserQueryServiceAsync userQueryService = 
        	AdminServiceLocator.getInstance().getUserQueryService();
		
		ApplicationResponseHandler<UserModel> callback = 
			new ApplicationResponseHandler<UserModel>()
		{
			@Override
            public void uponFailure(Throwable caught)
			{
				GWT.log("Error obtaining name of authenticated user", caught);
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
						ClientUtil.CLIENT_CONSTANTS.errorLoadingUserData(), null);
			}


			@Override
            public void uponSuccess(UserModel user)
			{
				ClientUtil.setLoggedInUser(user);
				
				// Create the graphical components.
				initComponents();
				
				// Add UI to the body of the host HTML page.
				RootPanel.get("prelertGUI").add(m_Viewport);	
			}
		};
		
		userQueryService.getLoggedInUser(callback);
	}
	
	
	/**
	 * Creates and initialises the graphical components in the window.
	 */
	protected void initComponents()
	{
		m_Modules = new HashMap<String, ModuleComponent>();
		
		m_Viewport = new Viewport();
		
		BorderLayout containerLayout = new BorderLayout();
		containerLayout.setContainerStyle("prl-viewport");
		
		m_Viewport.setLayout(containerLayout);
	
		
		// Create the container to hold the module contents.
		m_CardLayout = new CardLayout();
		m_ModulePanel = new LayoutContainer();
		m_ModulePanel.setLayout(m_CardLayout);
		
		
		// Create the top navigation bar, and add listeners for events from the 
		// various controls and links.
		m_NavigationBar = new NavigationBar();
		m_NavigationBar.addListener(GXTEvents.ShowModuleClick, new Listener<ShowModuleEvent>(){

            public void handleEvent(ShowModuleEvent be)
            {
            	setActiveModule(be.getModuleId());
            }
			
		});
		m_NavigationBar.addListener(GXTEvents.ChangePasswordClick, new Listener<ComponentEvent>(){

            public void handleEvent(ComponentEvent be)
            {
	            showChangePasswordDialog();
            }
			
		});
		m_NavigationBar.addListener(GXTEvents.LogoutClick, new Listener<ComponentEvent>(){

            public void handleEvent(ComponentEvent be)
            {
	            logout();
            }
			
		});
		
		// Create a listeners for RequestViewEvents to show data 
		// in the relevant module.
		m_ShowActivityListener = new Listener<RequestViewEvent<EvidenceModel>>(){

            public void handleEvent(RequestViewEvent<EvidenceModel> rve)
            {
            	m_NavigationBar.setActiveModuleId(IncidentsModule.MODULE_ID, false);
				setActiveModule(IncidentsModule.MODULE_ID);
				
				EvidenceModel model = rve.getModel();
				if (model != null)
				{
					IncidentsModule incidentsModule = 
						(IncidentsModule)(m_Modules.get(IncidentsModule.MODULE_ID));
					incidentsModule.showIncidentForEvidence(model.getId());
				}
            }
			
		};
		
		m_ShowEvidenceListener = new Listener<RequestViewEvent<EvidenceModel>>(){

            public void handleEvent(RequestViewEvent<EvidenceModel> rve)
            {
				m_NavigationBar.setActiveModuleId(ExplorerModule.MODULE_ID, false);
				setActiveModule(ExplorerModule.MODULE_ID);
            	
            	EvidenceModel selected = rve.getModel();
            	m_ExplorerModule.showEvidence(selected);
            }
			
		};
		
		m_ShowTimeSeriesListener = new Listener<RequestViewEvent<TimeSeriesConfig>>(){

            public void handleEvent(RequestViewEvent<TimeSeriesConfig> rve)
            {
            	m_NavigationBar.setActiveModuleId(ExplorerModule.MODULE_ID, false);
            	setActiveModule(ExplorerModule.MODULE_ID);
            	
            	TimeSeriesConfig config = rve.getModel();
            	m_ExplorerModule.showTimeSeries(config, rve.getOpenAtTime());
            }
			
		};
		
		m_ShowAnalysisListener = new Listener<RequestViewEvent<EvidenceModel>>(){

            public void handleEvent(final RequestViewEvent<EvidenceModel> rve)
            {
            	m_NavigationBar.setActiveModuleId(AnalysisModule.MODULE_ID, false);
				setActiveModule(AnalysisModule.MODULE_ID);
				
				final EvidenceModel model = rve.getModel();
				if (model != null)
				{
					// Run in a ScheduledCommand as otherwise activity tree may not display expanded.
					Scheduler.get().scheduleDeferred(new ScheduledCommand()
					{
						@Override
						public void execute()
						{
							CausalityView causalityView = (CausalityView)(rve.getView());
							m_AnalysisModule.analyseEvidence(model, causalityView, rve.getGroupBy());
						}
					});
				}
            }
			
		};

		// Add the Activity module.
		addActivityModule();
		
		// Add the Analysis module.
		addAnalysisModule();
				
		// Add the Explorer module.
		addExplorerModule();
		
		
		BorderLayoutData northData = new BorderLayoutData(LayoutRegion.NORTH, 60);    
        northData.setFloatable(false);   
		  
        m_Viewport.add(m_NavigationBar, northData);     
        m_Viewport.add(m_ModulePanel, new BorderLayoutData(LayoutRegion.CENTER));	
	}
	
	
	/**
	 * Returns the ID of the item of evidence whose data should be displayed on opening.
	 * @return the evidence ID, or 0 if no particular notification or time series 
	 * 	feature has been specified in the URL.
	 */
	protected int getStartingEvidenceId()
	{
		int id = 0;
		
		String idParam = Window.Location.getParameter(URLParameterNames.ID);
		if (idParam != null)
		{
			try
			{
				id = Integer.parseInt(idParam);
			}
			catch (NumberFormatException nfe)
			{
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
						ClientUtil.CLIENT_CONSTANTS.errorInvalidId(idParam), null);
			}
		}
		
		return id;
	}
	
	
	/**
	 * Sets the module that is active (visible) in the UI.
	 * @param moduleId the ID of the {@link ModuleComponent} that should be made active.
	 */
	protected void setActiveModule(String moduleId)
	{
		ModuleComponent module = m_Modules.get(moduleId);
		if (module != null)
		{
			Component moduleComponent = module.getComponent();
			m_CardLayout.setActiveItem(moduleComponent);
		}
	}
	
	
	/**
	 * Adds the Activity module.
	 */
	protected void addActivityModule()
	{		
		IncidentsModule incidentsModule = new IncidentsModule();
		m_ModulePanel.add(incidentsModule.getComponent());
		m_Modules.put(incidentsModule.getModuleId(), incidentsModule);
		
		// Add listeners to the Incidents module for RequestViewEvents.
		incidentsModule.addListener(GXTEvents.OpenNotificationViewClick, m_ShowEvidenceListener);
		incidentsModule.addListener(GXTEvents.OpenTimeSeriesViewClick, m_ShowTimeSeriesListener);
		incidentsModule.addListener(GXTEvents.OpenCausalityViewClick, m_ShowAnalysisListener);
		
		m_NavigationBar.addModuleLink(IncidentsModule.MODULE_ID, 
				ClientUtil.CLIENT_CONSTANTS.activity());
		
		if (IncidentsModule.MODULE_ID.equals(
				Window.Location.getParameter(URLParameterNames.MODULE)))
		{
			m_NavigationBar.setActiveModuleId(IncidentsModule.MODULE_ID);
			
			int showEvId = getStartingEvidenceId();
			if (showEvId != 0)
			{
				incidentsModule.showIncidentForEvidence(showEvId);
			}
			else
			{
				incidentsModule.showLatestIncident();
			}
		}
		else
		{
			incidentsModule.showLatestIncident();
		}
	}
	
	
	/**
	 * Adds in the Analysis module, using a GWT split point.
	 */
	protected void addAnalysisModule()
	{
		m_NavigationBar.addModuleLink(AnalysisModule.MODULE_ID, ClientUtil.CLIENT_CONSTANTS.analysis());
		
		GWT.runAsync(new RunAsyncCallback()
		{
			public void onFailure(Throwable caught)
			{
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
						ClientUtil.CLIENT_CONSTANTS.errorDownloadingModule(), null);
			}


			public void onSuccess()
			{
				m_AnalysisModule = new AnalysisModule();
				m_ModulePanel.add(m_AnalysisModule.getComponent());
				m_Modules.put(AnalysisModule.MODULE_ID, m_AnalysisModule);
				
				if (AnalysisModule.MODULE_ID.equals(
						Window.Location.getParameter(URLParameterNames.MODULE)))
				{
					m_NavigationBar.setActiveModuleId(AnalysisModule.MODULE_ID);
					
					int showEvId = getStartingEvidenceId();
					if (showEvId != 0)
					{
						m_AnalysisModule.analyseActivity(showEvId);
					}
				}
				
				// Add a listeners for events to open notification and time series views.
				m_AnalysisModule.addListener(GXTEvents.OpenActivityViewClick, m_ShowActivityListener);
				m_AnalysisModule.addListener(GXTEvents.OpenNotificationViewClick, m_ShowEvidenceListener);
				m_AnalysisModule.addListener(GXTEvents.OpenTimeSeriesViewClick, m_ShowTimeSeriesListener);
				
				// Add a listener to the popup evidence dialog to show the Explorer module.
				CausalityEvidenceDialog.getInstance().addListener(
						GXTEvents.OpenViewClick, m_ShowEvidenceListener);
			}
		});
	}
	
	
	/**
	 * Adds in the Explorer module, using a GWT split point.
	 */
	protected void addExplorerModule()
	{
		m_NavigationBar.addModuleLink(ExplorerModule.MODULE_ID,
				ClientUtil.CLIENT_CONSTANTS.explorer());
		
		GWT.runAsync(new RunAsyncCallback()
		{
			public void onFailure(Throwable caught)
			{
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
						ClientUtil.CLIENT_CONSTANTS.errorDownloadingModule(), null);
			}


			public void onSuccess()
			{
				m_ExplorerModule = new ExplorerModule();
				m_ModulePanel.add(m_ExplorerModule.getComponent());
				m_Modules.put(ExplorerModule.MODULE_ID, m_ExplorerModule);
				
				if (ExplorerModule.MODULE_ID.equals(
						Window.Location.getParameter(URLParameterNames.MODULE)))
				{
					m_NavigationBar.setActiveModuleId(ExplorerModule.MODULE_ID);
					int showEvId = getStartingEvidenceId();
					if (showEvId != 0)
					{
						m_ExplorerModule.showEvidence(showEvId);
					}
				}
				
				// Add a listener to the module for Open Causality View events.
				m_ExplorerModule.addListener(
						GXTEvents.OpenCausalityViewClick, m_ShowAnalysisListener);
			}
		});
		
	}
	
	
	/**
	 * Shows the password allowing a user to change their Prelert password.
	 */
	protected void showChangePasswordDialog()
    {
		if (m_ChangePasswordDialog == null)
		{
			m_ChangePasswordDialog = new ChangePasswordDialog();
		}
	    
		m_ChangePasswordDialog.show();
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
