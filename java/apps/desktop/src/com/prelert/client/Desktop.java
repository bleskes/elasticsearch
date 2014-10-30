/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2009     *
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

package com.prelert.client;

import java.util.*;

import com.google.gwt.core.client.*;
import com.google.gwt.widgetideas.graphics.client.Color;
import com.google.gwt.widgetideas.graphics.client.GWTCanvas;
import com.googlecode.gchart.client.GChart;
import com.googlecode.gchart.client.GChartCanvasFactory;
import com.googlecode.gchart.client.GChartCanvasLite;

import com.extjs.gxt.desktop.client.*;
import com.extjs.gxt.ui.client.event.*;
import com.extjs.gxt.ui.client.widget.*;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.menu.*;

import com.prelert.client.chart.UsageWindow;
import com.prelert.client.list.EvidenceHistoryWindow;
import com.prelert.client.list.EvidenceViewWindow;
import com.prelert.client.list.ExceptionListWindow;
import com.prelert.data.CausalityView;
import com.prelert.data.DesktopViewConfig;
import com.prelert.data.EventRecord;
import com.prelert.data.EvidenceView;
import com.prelert.data.ExceptionView;
import com.prelert.data.HistoryView;
import com.prelert.data.TimeFrame;
import com.prelert.data.UsageView;
import com.prelert.data.View;
import com.prelert.data.gxt.GridRowInfo;
import com.prelert.service.*;



/**
 * The main Prelert Desktop class, built with the Ext GWT toolkit.
 * The class creates and assembles all the required components, 
 * listeners and managers.
 * @author Pete Harverson
 */
public class Desktop implements EntryPoint
{

	private com.extjs.gxt.desktop.client.Desktop m_GXTDesktop = new com.extjs.gxt.desktop.client.Desktop();
	
	private WindowLayoutManager 				m_WindowManager;
	private SelectionListener<ComponentEvent> 	m_WindowSelectionListener;
	
	private HashMap<String, EvidenceViewWindow>	m_EvidenceWindows; 		// Keyed on view name.
	private HashMap<String, String>				m_EvidenceWindowsByType;// View name against data type.
	private CausalityViewWindow					m_CausalityWindow;
	private EvidenceHistoryWindow				m_HistoryWindow;
	private HashMap<String, UsageWindow>		m_UsageWindows; // Keyed on view name.
	private HashMap<String, String>				m_UsageWindowsByType; // View name against data type.
	private ShowInfoWindow						m_ShowInfoWindow;
	
	private ViewDirectoryServiceAsync 			m_ViewDirectoryService;
	
	

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
	
	
	/**
	 * Entry point called when the module loads.
	 */
	public void onModuleLoad()
	{
		m_ViewDirectoryService = 
			DatabaseServiceLocator.getInstance().getViewDirectoryService();
		
		m_WindowManager = new WindowLayoutManager(100, 10, 25);
		
		// Create the listener for opening desktop views.
		m_WindowSelectionListener = new SelectionListener<ComponentEvent>()
		{
			@Override
			public void componentSelected(ComponentEvent ce)
			{
				ViewWindow viewWin = null;
				if (ce instanceof MenuEvent)
				{
					MenuEvent me = (MenuEvent) ce;
					viewWin = me.item.getData("window");
				}
				else
				{
					viewWin = ce.component.getData("window");
				}

				addWindow(viewWin);
				
				if ( (viewWin != null) && (viewWin.isVisible() == false) )
				{
					viewWin.load();
					viewWin.show();
				}
				else
				{
					viewWin.toFront();
				}
			}

		};
		
	    
	    // Create the Start menu.
		// TO DO: Display the logged in user ID,
		// and add a Logout tool.
	    TaskBar taskBar = m_GXTDesktop.getTaskBar(); 
	    StartMenu startMenu = taskBar.getStartMenu();
	    startMenu.setHeading("Prelert");
	    startMenu.setIconStyle("user");
	    
	    // Add a logout tool on the Start Menu.
	    MenuItem logoutItem = new MenuItem("Logout");
		logoutItem.setIconStyle("logout");
		logoutItem.addSelectionListener(new SelectionListener<MenuEvent>()
		{
			@Override
			public void componentSelected(MenuEvent ce)
			{
				String logoutUrl = GWT.getModuleBaseURL() + "services/logoutService";
				com.google.gwt.user.client.Window.Location.assign(logoutUrl);
			}
		});
		startMenu.addTool(logoutItem);
	    
	    
	    // Load up pre-configured views from the View XML Config file.
	    loadConfiguredViews();
	    
	}
	
	
	/**
	 * Loads up the configured Prelert Desktop views.
	 */
	private void loadConfiguredViews()
	{
		m_EvidenceWindows = new HashMap<String, EvidenceViewWindow>();
		m_EvidenceWindowsByType = new HashMap<String, String>();
		
		m_UsageWindows = new HashMap<String, UsageWindow>();
		m_UsageWindowsByType = new HashMap<String, String>();

		m_ViewDirectoryService.getDesktopViewConfig(
				new ApplicationResponseHandler<DesktopViewConfig>(){

	        public void uponFailure(Throwable caught)
	        {
		        MessageBox.alert("Prelert - Error",
		                "Error loading desktop view configuration.",
		                null);
	        }


	        public void uponSuccess(DesktopViewConfig desktopConfig)
	        {
	        	// Create the Evidence View windows.
	        	List<EvidenceView> evidenceViews = desktopConfig.getEvidenceViews();
	        	if (evidenceViews != null)
	        	{
		        	EvidenceViewWindow evidenceWindow;
		        	String evidenceViewName;
			        for (EvidenceView evidenceView : evidenceViews)
			        {
			        	evidenceWindow = new EvidenceViewWindow(Desktop.this, evidenceView);
			        	
			        	if (evidenceView.isDesktopShortcut() == true)
			        	{
			        		addDesktopShortcut(evidenceWindow, evidenceView);
					        addStartMenuItem(evidenceWindow, evidenceView);
			        	}
			        	
			        	evidenceViewName = evidenceView.getName();
			        	m_EvidenceWindows.put(evidenceViewName, evidenceWindow);
			        	
			        	if (m_EvidenceWindowsByType.get(evidenceView.getDataType()) == null)
			        	{
			        		m_EvidenceWindowsByType.put(evidenceView.getDataType(), evidenceViewName);
			        	}
					}
	        	}
	        	
	        	// Create the Exception List.
	        	ExceptionView exceptionView = desktopConfig.getExceptionView();
	        	if (exceptionView != null)
	        	{
	        		ExceptionListWindow exceptionListWindow = new ExceptionListWindow(Desktop.this, exceptionView);
	        		addDesktopShortcut(exceptionListWindow, exceptionView);
			        addStartMenuItem(exceptionListWindow, exceptionView);
	        	}
	        	
	        	// Create the Causality View.
	        	CausalityView causalityView = desktopConfig.getCausalityView();
	        	m_CausalityWindow = (CausalityViewWindow)ExtGWTWindowFactory.createWindow(
	        			Desktop.this, causalityView);
	        	
	        	// Create the History View.
	        	HistoryView historyView = desktopConfig.getHistoryView();
	        	m_HistoryWindow = new EvidenceHistoryWindow(
						Desktop.this, TimeFrame.SECOND, historyView);
	        	
	        	// Create the Time Series View windows.
	        	List<UsageView> timeSeriesViews = desktopConfig.getTimeSeriesViews();
	        	if (timeSeriesViews != null)
	        	{
		        	UsageWindow usageWindow;
		        	String usageViewName;
			        for (UsageView usageView : timeSeriesViews)
			        {
			        	usageWindow = new UsageWindow(Desktop.this, usageView);
			        	
			        	if (usageView.isDesktopShortcut() == true)
			        	{
			        		addDesktopShortcut(usageWindow, usageView);
					        addStartMenuItem(usageWindow, usageView);
			        	}
				        
			        	usageViewName = usageView.getName();
			        	m_UsageWindows.put(usageViewName, usageWindow);
			        	
			        	if (m_UsageWindowsByType.get(usageView.getDataType()) == null)
			        	{
			        		m_UsageWindowsByType.put(usageView.getDataType(), usageViewName);
			        	}
					}
	        	}
		        
		        // Add any user-defined views.
		        List<View> userDefinedViews = desktopConfig.getUserDefinedViews();
		        if (userDefinedViews != null)
		        {
			        for (View userView : userDefinedViews)
			        {
				        // Create the new view window and 
			        	// add a desktop shortcut and Start Menu item.
			        	try
			        	{
			        		ViewWindow window = ExtGWTWindowFactory.createWindow(Desktop.this, userView);
			        		if (userView.isDesktopShortcut() == true)
			        		{
			        			addDesktopShortcut(window, userView);
			        			addStartMenuItem(window, userView);
			        		}
			        	}
			        	catch (UnsupportedOperationException e)
			        	{
			        		MessageBox.alert("Prelert Desktop error", e.getMessage(), 
			        				null);
			        	}
	
			        }
		        }  
		        
		        // Open up the first Evidence View on start-up.
	        	if (evidenceViews != null && evidenceViews.size() > 0)
	        	{
	        		openEvidenceView(evidenceViews.get(0).getName());
	        	}
	        }
        });	
	}
	
	
	/**
	 * Opens the Evidence Window, refreshing the list of evidence to display the
	 * first (most recent) page of evidence data with the current filter, if any.
	 */
	public void openEvidenceView(String viewName)
	{
		EvidenceViewWindow evidenceWindow = m_EvidenceWindows.get(viewName);
		
		if (evidenceWindow != null)
		{
			// Reload with current filter.
			evidenceWindow.load();
			
			addWindow(evidenceWindow);
			
			if (evidenceWindow.isVisible() == false)
			{
				evidenceWindow.show();
			}
			else
			{
				evidenceWindow.toFront();
			}
		}
		else
		{
			MessageBox.alert("Prelert - Error", "View '" + viewName + 
					"' has not been configured. ", null);
		}
	}
	
	
	/**
	 * Opens the Evidence Window, reloading the list so that the top of row of
	 * evidence matches the specified time and description.
	 * @param viewName the name of the Evidence View to open.
	 * @param date date/time of evidence data to load.
	 * @param description the value of the description column to match on for the
	 * top row of evidence data that will be returned.
	 */
	public void openEvidenceView(String viewName, Date date, String description)
	{
		EvidenceViewWindow evidenceWindow = m_EvidenceWindows.get(viewName);
		
		if (evidenceWindow != null)
		{
			// Clear any filter, and reload for this time/description.
			evidenceWindow.setFilter(null, null);
			evidenceWindow.loadAtTime(date, description);
			
			addWindow(evidenceWindow);
			
			if (evidenceWindow.isVisible() == false)
			{
				evidenceWindow.show();
			}

			evidenceWindow.toFront();
		}
		else
		{
			MessageBox.alert("Prelert - Error", "View '" + viewName + 
					"' has not been configured. ", null);
		}
	}
	
	
	/**
	 * Opens the Evidence Window with the specified name, reloading the list 
	 * to display the evidence with the specified id in the top row.
	 * @param viewName the name of the Evidence View to open.
	 * @param evidenceId id for the top row of evidence data to be displayed.
	 */
	public void openEvidenceView(String viewName, int evidenceId)
	{
		EvidenceViewWindow evidenceWindow = m_EvidenceWindows.get(viewName);
		
		if (evidenceWindow != null)
		{
			// Clear any filter, and reload for this time/description.
			evidenceWindow.setFilter(null, null);
			evidenceWindow.loadAtId(evidenceId);
			
			addWindow(evidenceWindow);
			
			if (evidenceWindow.isVisible() == false)
			{
				evidenceWindow.show();
			}
			
			evidenceWindow.toFront();
		}
		else
		{
			MessageBox.alert("Prelert - Error", "View '" + viewName + 
					"' has not been configured. ", null);
		}
	}
	
	
	/**
	 * Opens the Evidence Window showing notifications with the specified data type,
	 * reloading the list to display the evidence with the specified id in the top row.
	 * @param dataType data type of the Evidence View to open.
	 * @param evidenceId id for the top row of evidence data to be displayed.
	 */
	public void openEvidenceViewForType(String dataType, int evidenceId)
	{
		String viewName = m_EvidenceWindowsByType.get(dataType);
		if (viewName != null)
		{
			openEvidenceView(viewName, evidenceId);
		}
		else
		{
			MessageBox.alert("Prelert - Error", "No view has been configured for " +
					"evidence data type '" + dataType +  "'.", null);
		}
	}
	
	
	/**
	 * Opens the Evidence History window to display the history of evidence
	 * with the specified data type and description.
	 * @param dataType data type of evidence to display e.g. p2pslog, mdhlog.
	 * @param timeFrame the time frame to display in the History Window on opening.
	 * @param description description of evidence to display.
	 * @param time time to display in the History Window on opening.
	 */
	public void openHistoryView(String dataType, TimeFrame timeFrame,  
			String description, Date time)
	{
		m_HistoryWindow.setDataType(dataType);
		
		// Set the evidence second view to ensure that History Window will display
		// the appropriate columns in the SECOND time frame.
		String viewName = m_EvidenceWindowsByType.get(dataType);
		EvidenceViewWindow evidenceWindow = m_EvidenceWindows.get(viewName);
		m_HistoryWindow.setEvidenceSecondView(evidenceWindow.getView());
		
		m_HistoryWindow.setTimeFrame(timeFrame);
		m_HistoryWindow.setFilter("description", description);
		m_HistoryWindow.loadAtTime(time);
		
		addWindow(m_HistoryWindow);
		
		if (m_HistoryWindow.isVisible() == false)
		{
			m_HistoryWindow.show();
		}
		else
		{
			m_HistoryWindow.toFront();
		}
	}
	
	
	/**
	 * Opens the Usage window to display the corresponding usage for the
	 * specified time, source and service.
	 * @param viewName the name of the Usage View to open.
	 * @param metric the metric to display.
	 * @param source name of the source (server) for which to display the usage.
	 * 		If <code>null</code> the total service usage across all sources and 
	 * 		servers will be displayed.
	 * @param attributeName optional attribute name for the time series to display.
	 * @param attributeValue optional attribute value for the time series to display.
	 * @param date date/time of usage to display.
	 * @param timeFrame time frame of usage to display i.e. WEEK, DAY or HOUR.
	 */
	public void openUsageView(String viewName, String metric, String source, 
			String attributeName, String attributeValue, Date date, TimeFrame timeFrame)
	{
		UsageWindow usageWindow = m_UsageWindows.get(viewName);
		
		if (usageWindow != null)
		{
			usageWindow.addTimeLine(date);
			usageWindow.setUsageProperties(metric, source, 
					attributeName, attributeValue, date, timeFrame);
			
			addWindow(usageWindow);
			
			if (usageWindow.isVisible() == false)
			{
				usageWindow.show();
			}
			else
			{
				usageWindow.toFront();
			}
		}
		else
		{
			MessageBox.alert("Prelert - Error", "View '" + viewName + 
					"' has not been configured. ", null);
		}
	}
	
	
	/**
	 * Opens the Usage window to display the corresponding usage for the
	 * specified time, source and service.
	 * @param viewName the name of the Usage View to open.
	 * @param metric the metric to display.
	 * @param source name of the source (server) for which to display the usage.
	 * 		If <code>null</code> the total service usage across all sources and 
	 * 		servers will be displayed.
	 * @param user name of the user for which to display the usage.
	 * 		If <code>null</code> the total usage for the supplied source 
	 * 		will be displayed.
	 * @param date date/time of usage to display.
	 * @param timeFrame time frame of usage to display i.e. WEEK, DAY or HOUR.
	 */
	public void openUsageViewForType(String dataType, String metric, String source, 
			String attributeName, String attributeValue, Date date, TimeFrame timeFrame)
	{
		String viewName = m_UsageWindowsByType.get(dataType);
		if (viewName != null)
		{
			openUsageView(viewName, metric, source, 
					attributeName, attributeValue, date, timeFrame);
		}
		else
		{
			MessageBox.alert("Prelert - Error", "No view has been configured for " +
					"time series data type '" + dataType +  "'.", null);
		}
	}
	
	
	/**
	 * Opens the Causality View window to display the probable cause(s) of
	 * the item of evidence with the specified id.
	* @param evidenceRecord evidence for which to display the probable cause.
	 */
	public void openCausalityView(EventRecord evidence)
	{
		// Reload the Causality Window for this item of evidence.
		m_CausalityWindow.setEvidence(evidence);
		
		addWindow(m_CausalityWindow);
		
		if (m_CausalityWindow.isVisible() == false)
		{
			m_CausalityWindow.show();
		}
		else
		{
			m_CausalityWindow.toFront();
		}
	}
	
	
	/**
	 * Opens the Show Info window to display the supplied data.
	 * @param windowTitle title to display on the Show Info window.
	 * @param data	list of GridRowInfo objects to display in the window.
	 */
	public void openShowInfoWindow(String windowTitle, List<GridRowInfo> data)
	{
		if (m_ShowInfoWindow == null)
		{
			m_ShowInfoWindow = new ShowInfoWindow(this);
		}
		
		m_ShowInfoWindow.setHeading(windowTitle);
		m_ShowInfoWindow.setModelData(data);
		
		addWindow(m_ShowInfoWindow);
		
		if (m_ShowInfoWindow.isVisible() == false)
		{
			m_ShowInfoWindow.show();
		}
		else
		{
			m_ShowInfoWindow.toFront();
			
			// Update the text on the TaskButton.
			Button button = m_ShowInfoWindow.getData("taskButton");
			if (button != null)
			{
				try
				{
					// Note there is a bug in GXT 1.2 with TaskButton.setText().
					button.el().child("td.ux-taskbutton-center button").update(windowTitle);
				}
				catch (Exception e)
				{
					// Don't update text.
				}
			}
		}
	}
	
	
	/**
	 * Adds a window to the desktop.
	 * @param viewWindow the window to add.
	 */
	public void addWindow(Window viewWindow)
	{
		if (m_GXTDesktop.getWindows().contains(viewWindow) == false)
		{
			m_GXTDesktop.addWindow(viewWindow);	
		}
		
		if (m_WindowManager.getWindows().contains(viewWindow) == false)
		{
			m_WindowManager.addWindow(viewWindow);
		}
	}
	
	
	/**
	 * Adds a shortcut to the desktop for the view contained within the 
	 * specified Window.
	 * @param viewWindow Window object containing the view to add to the desktop.
	 * @param view Prelert View for which a shortcut is to be added to the desktop.
	 */
	public void addDesktopShortcut(Window viewWindow, View view)
	{
		Shortcut viewShortcut = new Shortcut();
	    viewShortcut.setText(view.getName());
	    if (view.getStyleId() != null)
	    {
	    	viewShortcut.setId(view.getStyleId()+ "-win-shortcut");
	    }
	    else
	    {
	    	viewShortcut.setId("list-evidence-win-shortcut");
	    }
	    
	    viewShortcut.setData("window", viewWindow);
	    viewShortcut.addSelectionListener(m_WindowSelectionListener);
	    m_GXTDesktop.addShortcut(viewShortcut);
	}
	
	
	/**
	 * Adds the specified View as an item to the desktop Start menu.
	 * @param viewWindow Window object containing the view to add to the Start menu.
	 * @param view Prelert View for which a Start Menu item is to be added.
	 */
	public void addStartMenuItem(Window viewWindow, View view)
	{
	    MenuItem evMenuItem = new MenuItem(view.getName());
	    evMenuItem.setData("window", viewWindow);
	    if (view.getStyleId() != null)
	    {
	    	evMenuItem.setIconStyle(view.getStyleId()+ "-win-icon");
	    }
	    else
	    {
	    	evMenuItem.setIconStyle("list-evidence-win-icon");
	    }
	    evMenuItem.addSelectionListener(m_WindowSelectionListener);
	    
	    TaskBar taskBar = m_GXTDesktop.getTaskBar();
		StartMenu startMenu = taskBar.getStartMenu();
	    startMenu.add(evMenuItem);
	}	
	
}
