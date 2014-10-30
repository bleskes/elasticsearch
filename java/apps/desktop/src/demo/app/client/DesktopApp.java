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

package demo.app.client;

import java.util.*;

import com.extjs.gxt.desktop.client.*;
import com.extjs.gxt.ui.client.*;
import com.extjs.gxt.ui.client.data.*;
import com.extjs.gxt.ui.client.event.*;
import com.extjs.gxt.ui.client.store.*;
import com.extjs.gxt.ui.client.widget.*;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.grid.*;
import com.extjs.gxt.ui.client.widget.layout.*;
import com.extjs.gxt.ui.client.widget.menu.*;
import com.extjs.gxt.ui.client.widget.toolbar.*;

import com.googlecode.gchart.client.GChart;
import com.googlecode.gchart.client.GChartCanvasFactory;
import com.googlecode.gchart.client.GChartCanvasLite;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.widgetideas.graphics.client.Color;
import com.google.gwt.widgetideas.graphics.client.GWTCanvas;

import demo.app.client.chart.*;
import demo.app.client.list.EvidenceHistoryWindow;
import demo.app.client.list.EvidenceViewWindow;
import demo.app.client.list.ExceptionListWindow;
import demo.app.client.list.SeverityGridView;
import demo.app.data.CausalityView;
import demo.app.data.DesktopViewConfig;
import demo.app.data.EvidenceView;
import demo.app.data.ExceptionView;
import demo.app.data.HistoryView;
import demo.app.data.TimeFrame;
import demo.app.data.UsageView;
import demo.app.data.View;
import demo.app.data.gxt.EvidenceModel;
import demo.app.data.gxt.GridRowInfo;
import demo.app.service.DatabaseServiceLocator;
import demo.app.service.StatesQueryServiceAsync;
import demo.app.service.ViewDirectoryServiceAsync;

/**
 * Application to create a Prelert Desktop for testing and prototyping functionality.
 */
public class DesktopApp implements EntryPoint
{
	private Desktop m_GXTDesktop = new Desktop();
	
	private StatesQueryServiceAsync 	m_StatesQueryService;
	private ViewDirectoryServiceAsync 	m_ViewDirectoryService;
	
	private DesktopMessages m_Messages;
	
	private WindowLayoutManager 				m_WindowManager;
	private SelectionListener<ComponentEvent>	m_ShortcutListener;
	private SelectionListener<MenuEvent>		m_StartMenuListener;
	
	private HashMap<String, EvidenceViewWindow>	m_EvidenceWindows; 		// Keyed on view name.
	private HashMap<String, String>				m_EvidenceWindowsByType;// View name against data type.
	private CausalityViewWindow					m_CausalityWindow;
	private EvidenceHistoryWindow				m_HistoryWindow;
	private HashMap<String, UsageWindow>		m_UsageWindows; 		// Keyed on view name.
	private HashMap<String, String>				m_UsageWindowsByType; 	// View name against data type.
	private ShowInfoWindow						m_ShowInfoWindow;
	
	
	
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
	 * This is the entry point method.
	 */
	public void onModuleLoad()
	{
		m_ViewDirectoryService =  DatabaseServiceLocator.getInstance().getViewDirectoryService();
		m_StatesQueryService =  DatabaseServiceLocator.getInstance().getStatesQueryService();
		
		m_Messages = GWT.create(DesktopMessages.class);
		m_WindowManager = new WindowLayoutManager(100, 10, 25);
		
		m_ShortcutListener = new SelectionListener<ComponentEvent>()
		{
			@Override
			public void componentSelected(ComponentEvent ce)
			{
				Window viewWin = ce.getComponent().getData("window");
				
				GWT.log("DesktopApp.componentSelected(): " + viewWin.getHeading(), null);
				
				addWindow(viewWin);

				if ( (viewWin != null) && (viewWin.isVisible() == false) )
				{
					viewWin.show();
					if (viewWin instanceof ViewWindow)
					{
						((ViewWindow) viewWin).load();
					}
					
					// Code to set the tooltip and text of the TaskButton 
					// independently of the window heading:
					/*
					Button taskBtn = viewWin.getData("taskButton");	
					GWT.log("TaskButton is " + taskBtn, null);
					String winIconStyle = viewWin.getIconStyle();
					taskBtn.setToolTip("View Window with very long tooltip indeed");
					taskBtn.el().childElement("button." + winIconStyle).setInnerText("Short");
					*/
								
				}
				else
				{
					viewWin.toFront();
				}
			}

		};
		
		
		m_StartMenuListener = new SelectionListener<MenuEvent>()
		{
			@Override
			public void componentSelected(MenuEvent e)
			{
				Window viewWin = null;
				viewWin = e.getItem().getData("window");
				
				GWT.log("DesktopApp.componentSelected(): " + viewWin.getHeading(), null);
				
				addWindow(viewWin);

				if ( (viewWin != null) && (viewWin.isVisible() == false) )
				{
					viewWin.show();
					if (viewWin instanceof ViewWindow)
					{
						((ViewWindow) viewWin).load();
					}
					
					// Code to set the tooltip and text of the TaskButton 
					// independently of the window heading:
					/*
					Button taskBtn = viewWin.getData("taskButton");	
					GWT.log("TaskButton is " + taskBtn, null);
					String winIconStyle = viewWin.getIconStyle();
					taskBtn.setToolTip("View Window with very long tooltip indeed");
					taskBtn.el().childElement("button." + winIconStyle).setInnerText("Short");
					*/
								
				}
				else
				{
					viewWin.toFront();
				}
			}

		};
		
		
	    
	    
	    // Load up pre-configured views from the View XML Config file.
		// Once complete, this will add in the prototype views.
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
			        	evidenceWindow = new EvidenceViewWindow(DesktopApp.this, evidenceView);
			        	
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
	        		ExceptionListWindow exceptionListWindow = 
	        			new ExceptionListWindow(DesktopApp.this, exceptionView);
	        		addDesktopShortcut(exceptionListWindow, exceptionView);
			        addStartMenuItem(exceptionListWindow, exceptionView);
	        	}
	        	
	        	// Create the Causality View.
	        	CausalityView causalityView = desktopConfig.getCausalityView();
	        	m_CausalityWindow = (CausalityViewWindow)ExtGWTWindowFactory.createWindow(
	        			DesktopApp.this, causalityView);
	        	
	        	// Create the History View.
	        	HistoryView historyView = desktopConfig.getHistoryView();
	        	m_HistoryWindow = new EvidenceHistoryWindow(
						DesktopApp.this, TimeFrame.SECOND, historyView);
	        	
	        	// Create the Usage View windows.
	        	List<UsageView> timeSeriesViews = desktopConfig.getTimeSeriesViews();
	        	if (timeSeriesViews != null)
	        	{
		        	UsageWindow usageWindow;
		        	String usageViewName;
			        for (UsageView usageView : timeSeriesViews)
			        {
			        	usageWindow = new UsageWindow(DesktopApp.this, usageView);
			        	
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
	        	
	        	// Add the prototype views onto the desktop.
	        	loadPrototypeViews();
	        	
	        	// Open up the Evidence View on start-up.
	        	if (evidenceViews != null && evidenceViews.size() > 0)
	        	{
	        		openEvidenceView(evidenceViews.get(0).getName());
	        	}
	        }
        });	
	}
	
	
	/**
	 * Loads up prototype views onto the Desktop.
	 */
	private void loadPrototypeViews()
	{
		//Window weeklyView = createWeeklyWindow();
		
		//Window barGraphView = createBarGraphWindow();
		
		
		// Test out some charting components.
		Window flashChartWindow = new ScatterFlashChartWindow();
		//Window flashChartWindow = new LineFlashChartWindow();
		Window gChartWindow = new ScatterGChartWindow();
		//Window timelineWindow = new UsageTimeLineWindow(DesktopApp.this);  // Google AnnotatedTimeLine visualization. 
		Window timelineWindow = new AnnotatedTimeLineWindow();  // Google AnnotatedTimeLine visualization.
		
		//Window toolbarTestWindow = createToolBarWindow();
		//Window modelWindow = new ProbableCauseViewer();
		

	    
//	    Shortcut stateViewS = new Shortcut();
//	    stateViewS.setText(m_Messages.stateView());
//	    stateViewS.setId("stateview-win-shortcut");
//	    stateViewS.setData("window", probCauseWindow);
//	    stateViewS.addSelectionListener(shortcutListener);
//	    m_GXTDesktop.addShortcut(stateViewS);
	    
	    
//	    Shortcut weeklyViewS = new Shortcut();
//	    weeklyViewS.setText("Weekly aggregate");
//	    weeklyViewS.setId("stateview-win-shortcut");
//	    weeklyViewS.setData("window", weeklyView);
//	    weeklyViewS.addSelectionListener(m_ShortcutListener);
//	    m_GXTDesktop.addShortcut(weeklyViewS);
	    
		
//		Shortcut googleVisViewS = new Shortcut();
//	    googleVisViewS.setText("Service TimeLine");
//	    googleVisViewS.setId("bar-graph-win-shortcut");
//	    googleVisViewS.setData("window", googleVizView);
//	    googleVisViewS.addSelectionListener(shortcutListener);
//	    m_GXTDesktop.addShortcut(googleVisViewS);
	    
	    
	    Shortcut flashChartS = new Shortcut();
	    flashChartS.setText(m_Messages.flashChart());
	    flashChartS.setId("line-graph-win-shortcut");
	    flashChartS.setData("window", flashChartWindow);
	    flashChartS.addSelectionListener(m_ShortcutListener);
	    m_GXTDesktop.addShortcut(flashChartS);
	    
	    
	    Shortcut gChartS = new Shortcut();
	    gChartS.setText("GChart Scatter");
	    gChartS.setId("line-graph-win-shortcut");
	    gChartS.setData("window", gChartWindow);
	    gChartS.addSelectionListener(m_ShortcutListener);
	    m_GXTDesktop.addShortcut(gChartS);
	    
	    
	    Shortcut timelineS = new Shortcut();
	    timelineS.setText("Google Timeline");
	    timelineS.setId("bar-graph-win-shortcut");
	    timelineS.setData("window", timelineWindow);
	    timelineS.addSelectionListener(m_ShortcutListener);
	    m_GXTDesktop.addShortcut(timelineS);
	    
	    
	    
	    //Shortcut modelViewS = new Shortcut();
	    //modelViewS.setText("Model View");
	    //modelViewS.setId("stateview-win-shortcut");
	    //modelViewS.setData("window", modelWindow);
	    //modelViewS.addSelectionListener(shortcutListener);
	    //m_GXTDesktop.addShortcut(modelViewS);
	    
	    
	    //Shortcut barGraphViewS = new Shortcut();
	    //barGraphViewS.setText("Bar Graph");
	    //barGraphViewS.setId("bar-graph-win-shortcut");
	    //barGraphViewS.setData("window", barGraphView);
	    //barGraphViewS.addSelectionListener(listener);
	    //m_GXTDesktop.addShortcut(barGraphViewS);

	    
	    // Configure the Start menu.
	    TaskBar taskBar = m_GXTDesktop.getTaskBar();
	    StartMenu menu = taskBar.getStartMenu();
	    menu.setHeading("Pete");
	    menu.setIconStyle("user");
	    
	    
//	    MenuItem stMenuItem = new MenuItem(m_Messages.stateView());
//	    stMenuItem.setData("window", probCauseWindow);
//	    stMenuItem.setIconStyle("icon-grid");
//	    stMenuItem.addSelectionListener(startMenuListener);
//	    menu.add(stMenuItem);
	    
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
		menu.addTool(logoutItem);
	}
	
	
	/**
	 * Opens the Evidence Window with the given view name, refreshing the list
	 * of evidence to display the first (most recent) page of evidence data with
	 * the current filter, if any.
	 * @param viewName the name of the Evidence View to open.
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
	 * Opens the Evidence Window, reloading the list so that the top of row of
	 * evidence matches the specified time and description.
	 * @param viewName the name of the Evidence View to open.
	 * @param date date/time of evidence data to load.
	 * @param description the value of the description column to match on for the
	 * top row of evidence data that will be returned.
	 */
	public void openEvidenceWindow(String viewName, Date date, String description)
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
	public void openEvidenceWindow(String viewName, int evidenceId)
	{
		EvidenceViewWindow evidenceWindow = m_EvidenceWindows.get(viewName);
		
		if (evidenceWindow != null)
		{
			// Clear any filter, and reload for this id.
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
	public void openEvidenceWindowForType(String dataType, int evidenceId)
	{
		String viewName = m_EvidenceWindowsByType.get(dataType);
		if (viewName != null)
		{
			openEvidenceWindow(viewName, evidenceId);
		}
		else
		{
			MessageBox.alert("Prelert - Error", "No view has been configured for " +
					"evidence data type '" + dataType +  "'.", null);
		}
	}
	
	
	/**
	 * Opens the Causality Window to display the probable cause(s) of the item
	 * of evidence with the specified id.
	 * @param evidenceRecord evidence for which to display the probable cause.
	 */
	public void openCausalityWindow(EvidenceModel evidenceRecord)
	{
		// Reload the Causality Window for this item of evidence.
		m_CausalityWindow.setEvidence(evidenceRecord);
		
		addWindow(m_CausalityWindow);
		
		if (m_CausalityWindow.isVisible() == false)
		{
			m_CausalityWindow.show();
		}
		
		m_CausalityWindow.toFront();
	}
	
	
	/**
	 * Opens the Evidence History window to display the history of evidence
	 * with the specified data type and description.
	 * @param dataType data type of evidence to display e.g. p2pslog, mdhlog.
	 * @param timeFrame the time frame to display in the History Window on opening.
	 * @param description description of evidence to display.
	 * @param time time to display in the History Window on opening.
	 */
	public void openHistoryWindow(String dataType, TimeFrame timeFrame,  
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
	 * Opens the Usage window with the given view name, to display the corresponding 
	 * usage for the specified time, source and service.
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
	public void openUsageWindow(String viewName, String metric, String source, 
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
	 * Opens the Usage window showing time series of the specified type to display
	 * the corresponding usage for the specified time, source and service.
	 * @param dataType the data type of the Usage View to open.
	 * @param metric the metric to display.
	 * @param source name of the source (server) for which to display the usage.
	 * 		If <code>null</code> the total service usage across all sources and 
	 * 		servers will be displayed.
	 * @param attributeName optional attribute name for the time series to display.
	 * @param attributeValue optional attribute value for the time series to display.
	 * @param date date/time of usage to display.
	 * @param timeFrame time frame of usage to display i.e. WEEK, DAY or HOUR.
	 */
	public void openUsageWindowForType(String dataType, String metric, String source, 
			String attributeName, String attributeValue, Date date, TimeFrame timeFrame)
	{
		String viewName = m_UsageWindowsByType.get(dataType);
		if (viewName != null)
		{
			openUsageWindow(viewName, metric, source, 
					attributeName, attributeValue, date, timeFrame);
		}
		else
		{
			MessageBox.alert("Prelert - Error", "No view has been configured for " +
					"time series data type '" + dataType +  "'.", null);
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
		
		// Update the text on the TaskButton.
		Button button = m_ShowInfoWindow.getData("taskButton");
		if (button != null)
		{
			button.setText(windowTitle);
		}
		
		if (m_ShowInfoWindow.isVisible() == false)
		{
			m_ShowInfoWindow.show();
		}
		else
		{
			m_ShowInfoWindow.toFront();
		}
	}
	
	
	/**
	 * Creates a Window which displays evidence in a numerical paging view.
	 * Note that this is not scalable, performing poorly when there are more
	 * than 500-1000 rows in the evidence table.
	 * @return the new Evidence window.
	 */
	private Window createNumericalEvidenceWindow()
	{
		Window evidenceWin = new Window();
		evidenceWin.setIconStyle("icon-grid");
		evidenceWin.setMinimizable(true);
		evidenceWin.setMaximizable(true);
		evidenceWin.setHeading(m_Messages.evidenceView());
		evidenceWin.setSize(500, 400);
		evidenceWin.setLayout(new FitLayout());
		evidenceWin.setResizable(true);
		

		RpcProxy proxy = new RpcProxy()
		{
			@Override
			public void load(Object loadConfig, AsyncCallback callback)
			{
				getStatesQueryServiceInstance().getEvidenceData((PagingLoadConfig)loadConfig, callback);
			}
		};
		

		// Use a PagingLoader to populate the list.
		BasePagingLoader loader = new BasePagingLoader(proxy);
		loader.setRemoteSort(true);
		loader.load(0, 10);
		
		ListStore<EvidenceModel> store = new ListStore<EvidenceModel>(loader);
		
		// Add a couple of default columns, 
		// then get the complete list of columns from the server.
		ColumnConfig id = new ColumnConfig("id", "Id", 50);
	    ColumnConfig severity = new ColumnConfig("severity", m_Messages.severity(), 80);

	    List<ColumnConfig> config = new ArrayList<ColumnConfig>();
	    config.add(id);
	    config.add(severity);

	    ColumnModel columnModel = new ColumnModel(config);
	    
	    final Grid<EvidenceModel> evidenceGrid = new Grid<EvidenceModel>(store, columnModel);
	    evidenceGrid.setLoadMask(true);
	    evidenceGrid.setBorders(true);
	    evidenceGrid.setTrackMouseOver(false);
	    evidenceGrid.setView(new SeverityGridView(store));
	    evidenceGrid.getSelectionModel().setSelectionMode(Style.SelectionMode.SINGLE);

		configureGridColumns(evidenceGrid, "evidence");
		
		PagingToolBar toolBar = new PagingToolBar(10);
		toolBar.getMessages().setDisplayMsg("Rows {0} - {1} of {2}");
		toolBar.bind(loader);
	    	
	    
	    // Set up a test right-click context menu
	    Menu menu = new Menu();

	    MenuItem probCauseMenuItem = new MenuItem(m_Messages.showProbableCause());
	    menu.add(probCauseMenuItem);
	    
	    evidenceGrid.setContextMenu(menu);
	    
	    evidenceWin.add(evidenceGrid);
	    evidenceWin.setTopComponent(toolBar);

		return evidenceWin;
	}
	
	
	private Window createWeeklyWindow()
	{
		Window weeklyWin = new Window();
		weeklyWin.setIconStyle("icon-grid");
		weeklyWin.setMinimizable(true);
		weeklyWin.setMaximizable(true);
		//weeklyWin.setHeading("Weekly aggregate");
		weeklyWin.setHeading("Investigator");
		weeklyWin.setSize(500, 300);
		weeklyWin.setLayout(new FitLayout());
		weeklyWin.setResizable(true);
		
		RpcProxy proxy = new RpcProxy()
		{
			@Override
			public void load(Object loadConfig, AsyncCallback callback)
			{
				getStatesQueryServiceInstance().getWeeklyAggregate(callback);
			}
		};
		
		BaseListLoader loader = new BaseListLoader(proxy);
		loader.setRemoteSort(false);
		loader.load();
		
		ListStore<BaseModel> store = new ListStore<BaseModel>(loader);
		
	    ColumnConfig descColumn = new ColumnConfig("description", "Description", 300);
	    ColumnConfig valueColumn = new ColumnConfig("sum", "Count", 200);
	    
	    ColumnConfig probCauseColumn = new ColumnConfig();
	    probCauseColumn.setId("prob_cause");  // not forget to set this or you will get a nullpointer :D (i think this really should be fixed that there is no NE thrown)
	    //probCauseColumn.setHeader("Probable Cause");
	    probCauseColumn.setHeader("Significance");
	    probCauseColumn.setWidth(100);
	    probCauseColumn.setRenderer(ProbableCauseRenderer.getInstance());

	    List<ColumnConfig> config = new ArrayList<ColumnConfig>();
	    config.add(descColumn);
	    config.add(valueColumn);
	    config.add(probCauseColumn);

	    ColumnModel columnModel = new ColumnModel(config);
		
		Grid<BaseModel> grid = new Grid<BaseModel>(store, columnModel);
		grid.setLoadMask(true);
	    grid.setBorders(true);
	    grid.setAutoExpandColumn("description");
	    
	    grid.addListener(Events.CellClick, new Listener<GridEvent<BaseModel>>(){
	    	public void handleEvent(GridEvent<BaseModel> event) {
	    		if(event.getTarget(".probcause", 1) != null)
	    		{
	    			BaseModel model = event.getModel();
	    			String description = model.get("description");
	    			
	    			Info.display("Grid Event","Description: " + description);
	    		}
	    	}
	    });


	    weeklyWin.add(grid);

		return weeklyWin;
	}
	
	
	private Window createBarGraphWindow()
	{
		Window barGraphWin = new Window();
		barGraphWin.setIconStyle("bar-graph-win-icon");
		barGraphWin.setMinimizable(true);
		barGraphWin.setMaximizable(true);
		barGraphWin.setHeading("Bar Graph");
		barGraphWin.setSize(700, 450);
		barGraphWin.setLayout(new FitLayout());
		barGraphWin.setResizable(true);
		
		BarChart barChart = new BarChart();
		barChart.setDesktop(this);
		barGraphWin.add(barChart);
		barChart.update();

		return barGraphWin;
	}
	
	
	private Window createProbableCausesWindow()
    {
    	Window statesWin = new Window();
    	statesWin.setIconStyle("icon-grid");
    	statesWin.setMinimizable(true);
    	statesWin.setMaximizable(true);
    	statesWin.setHeading(m_Messages.stateView());
    	statesWin.setSize(500, 400);
    	statesWin.setLayout(new FitLayout());
    	statesWin.setResizable(true);
    	
    
    	RpcProxy proxy = new RpcProxy()
    	{
    		@Override
    		public void load(Object loadConfig, AsyncCallback callback)
    		{
    			getStatesQueryServiceInstance().getProbableCauseData((PagingLoadConfig)loadConfig, callback);
    		}
    	};
    
    	// loader
    	BasePagingLoader loader = new BasePagingLoader(proxy);
    	loader.setRemoteSort(true);
    	
    	loader.addLoadListener(new LoadListener() {
    
           public void loaderLoadException(LoadEvent le) {
                System.out.println("State View window load exception = " + le.exception.getLocalizedMessage());
           }
        
    	});
    
    	loader.load(0, 10);
    
    	ListStore<EvidenceModel> store = new ListStore<EvidenceModel>(loader);
    
    	final PagingToolBar toolBar = new PagingToolBar(10);
    	toolBar.getMessages().setDisplayMsg("Rows {0} - {1} of {2}");
    	toolBar.bind(loader);
        
        ColumnConfig entity = new ColumnConfig("source", m_Messages.source(), 100);
        ColumnConfig description = new ColumnConfig("description", m_Messages.source(), 100);
        ColumnConfig severity = new ColumnConfig("severity", m_Messages.severity(), 80);
    
        List<ColumnConfig> config = new ArrayList<ColumnConfig>();
        config.add(entity);
        config.add(description);
        config.add(severity);
    
        ColumnModel columnModel = new ColumnModel(config);
    	
        
    	Grid<EvidenceModel> grid = new Grid<EvidenceModel>(store, columnModel);
    	grid.setLoadMask(true);
        grid.setBorders(true);
        grid.setAutoExpandColumn("description");
        
        grid.getView().setViewConfig(new GridViewConfig()
        {
        	  public String getRowStyle(ModelData model, int rowIndex, ListStore ds)
        	  {
        		  return "severity_" + model.get("severity");
        	  }
        });
    
        configureGridColumns(grid, "probable_cause_view");
        
        statesWin.add(grid);
        statesWin.setTopComponent(toolBar);
    
    	return statesWin;
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
	    viewShortcut.addSelectionListener(m_ShortcutListener);
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
	    evMenuItem.addSelectionListener(m_StartMenuListener);
	    
	    TaskBar taskBar = m_GXTDesktop.getTaskBar();
		StartMenu startMenu = taskBar.getStartMenu();
	    startMenu.add(evMenuItem);
	}
	
	
	public void configureGridColumns(Grid grid, String table)
	{
		final Grid listViewGrid = grid;
		
		getStatesQueryServiceInstance().getTableColumns(table, new AsyncCallback<List<String>>(){

	        public void onFailure(Throwable caught)
	        {
		        MessageBox.alert("Prelert - Error",
		                "Failed to get a response from the server", null);
	        }

	        public void onSuccess(List<String> columns)
	        {
	        	List<ColumnConfig> newConfig = new ArrayList<ColumnConfig>();
				
				ColumnConfig columnConf;
				String columnName;
				Iterator<String> colIterator = columns.iterator();
				while (colIterator.hasNext() == true)
				{
					columnName = colIterator.next();
					columnConf = new ColumnConfig(columnName, columnName, 80);
					
					if (columnName.equals("probability"))
					{
						columnConf.setRenderer(ProgressBarCellRenderer.getInstance());
					}
					
					newConfig.add(columnConf);
					
				}
				ColumnModel newColumnModel = new ColumnModel(newConfig);
				listViewGrid.reconfigure(listViewGrid.getStore(), newColumnModel);
	        }
        });
	}
	
	
	protected StatesQueryServiceAsync getStatesQueryServiceInstance()
	{
		return m_StatesQueryService;
	}

}
