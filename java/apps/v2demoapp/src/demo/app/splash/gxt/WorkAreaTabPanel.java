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

package demo.app.splash.gxt;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.extjs.gxt.ui.client.Style;
import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.TabPanelEvent;
import com.extjs.gxt.ui.client.widget.BoxComponent;
import com.extjs.gxt.ui.client.widget.Info;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.TabItem;
import com.extjs.gxt.ui.client.widget.TabPanel;
import com.extjs.gxt.ui.client.widget.TabItem.HeaderItem;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Element;

import demo.app.client.ApplicationResponseHandler;
import demo.app.client.ClientUtil;
import demo.app.client.SourceViewWidget;
import demo.app.client.event.GXTEvents;
import demo.app.client.event.RequestViewEvent;
import demo.app.data.CausalityView;
import demo.app.data.DataSourceCategory;
import demo.app.data.DataSourceType;
import demo.app.data.EvidenceView;
import demo.app.data.TimeSeriesConfig;
import demo.app.data.UsageView;
import demo.app.data.View;
import demo.app.data.gxt.EvidenceModel;
import demo.app.splash.service.QueryServiceLocator;
import demo.app.splash.service.ViewCreationServiceAsync;

/**
 * An extension of the Ext GWT TabPanel for the main work area tab panel.
 * It applies the necessary style attributes and manages the addition and
 * removal of tabs for the various data source types.
 * @author Pete Harverson
 */
public class WorkAreaTabPanel extends TabPanel
{
	private ViewCreationServiceAsync		m_ViewService;
	
	private HashMap<String, View>			m_DataViewsByType;// Data views against type name.
	private HashMap<String, EvidenceView>	m_TimeSeriesFeatureViews; // Evidence View against type.
	private CausalityView					m_CausalityView;
	
	private static final String ANALYSED_TAB_TEXT = "Analysed Data";
	private static final String CAUSALITY_TAB_TEXT = "Causality";

	
	public WorkAreaTabPanel()
	{
		super();
		
		// Define the JSNI bridge methods.
		defineBridgeMethod();
		defineBridgeMethod2();
		
		m_ViewService = QueryServiceLocator.getInstance().getViewCreationService();
		m_DataViewsByType = new HashMap<String, View>();
		m_TimeSeriesFeatureViews = new HashMap<String, EvidenceView>();
		
		// Apply styles.
		setBorderStyle(true);
		setPlain(true);
		setBodyBorder(true);
		addStyleName("prl-workAreaTabPanel");
		setAnimScroll(true);
		
		// Listen for resize and select events to set the width of the selected tab.
		Listener<ComponentEvent> resizeListener = new Listener<ComponentEvent>(){
			
			public void handleEvent(ComponentEvent be)
            {
				 fitSelectedToSize();
            }
		};	
		addListener(Events.Select, resizeListener);
		addListener(Events.Resize, resizeListener);
		
		
		// Listen for tab open and close events to set the last
		// remaining tab to non-closeable.
		addListener(Events.Remove, new Listener<TabPanelEvent>(){

            public void handleEvent(TabPanelEvent be)
            {
	            if (getItemCount() == 1)
	            {
	            	setFirstTabItemClosable(false);
	            }
            }
			
		});
		
		addListener(Events.BeforeAdd, new Listener<TabPanelEvent>(){

            public void handleEvent(TabPanelEvent be)
            {    	
            	if (getItemCount() == 1)
	            {
            		setFirstTabItemClosable(true);	
            	}
            }
			
		});
		
		addListener(Events.Render, new Listener<ComponentEvent>(){

            public void handleEvent(ComponentEvent be)
            {
	            // If opened with one tab, make it non-closeable.
	            if (getItemCount() == 1)
	            {
            		setFirstTabItemClosable(false);	
            	}
            }
			
		});
		
		// Load up the configuration for all the data tabs.
		loadDataViews();
	}


	/**
	 * Adds a new tab to the work area tab panel to hold the specified widget.
	 * @param text	the text for the new tab.
	 * @param widget the widget to add to the tab.
	 */
	public void addTab(String text, Widget widget)
	{
		TabItem tab = new TabItem();

		tab.setClosable(true);
		tab.setScrollMode(Style.Scroll.AUTO);
		tab.setText(text);
		  
		// Try giving the tab a VBoxLayout, center-aligned.
		/*
        VBoxLayout layout = new VBoxLayout();   
        layout.setPadding(new Padding(5));   
        layout.setVBoxLayoutAlign(VBoxLayoutAlign.CENTER);   
        tab.setLayout(layout);
        */

        tab.add(widget);
        tab.addStyleName("prl-workAreaTabItem");
		 
		add(tab);
		
		if (getItemCount() == 1)
		{
			setFirstTabItemClosable(false);
		}
	}
	
	
	/**
	 * Adds a tab to show data for the specified data source type.
	 * @param dataSourceType the DataSourceType for which to add a tab.
	 * @param source name of the source (server) whose data will be displayed
	 * 		after the tab is added, or <code>null</code> to show data from all servers.
	 */
	public void addTab(DataSourceType dataSourceType, String source)
	{
		String dsTypeName = dataSourceType.getName();
		View view = m_DataViewsByType.get(dsTypeName);
		if (view != null)
		{
			// Create the ViewWidget, and add to a new tab.
			try
			{
				SourceViewWidget viewWidget = createViewWidget(view);
				addTab(dsTypeName, viewWidget.getWidget());
			}
			catch (UnsupportedOperationException e)
			{
				GWT.log("No view has been configured for data type: " + dsTypeName, e);
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(), 
						ClientUtil.CLIENT_CONSTANTS.errorNoViewForType() + dsTypeName, null);
			}
		}
		else
		{
			String errMsg = "No view has been loaded for data type '" + 
				dsTypeName +  "'.";
			GWT.log(errMsg, null);

			// This may be reached before the data for the views has been loaded.
			// Make a request for this particular view.
			loadViewForTab(dataSourceType, source);
		}
	}
	
	
	/**
	 * Searches for the tab item for the specified data source type.
	 * @param dataSourceType data source type of tab to find.
	 * @return the tab item, or <code>null</code> if no tab has been created 
	 * 		to display data for the specified data source type.
	 */
	public TabItem findItem(DataSourceType dataSourceType)
	{
		String typeName = dataSourceType.getName();
		
		return findItem(typeName, true);
	}
	
	
	/**
	 * Returns the data source type of the selected tab.
	 * @return the DataSourceType, or <code>null</code> if the currently selected
	 * 	tab does not contain data for a specific source type e.g. the Causality tab.
	 */
	public DataSourceType getSelectedDataSourceType()
	{
		TabItem selectedTab = getSelectedItem();
		String tabName = selectedTab.getText();
		
		DataSourceType dsType = null;
		
		if (tabName != CAUSALITY_TAB_TEXT)
		{
			dsType = getDataSourceType(tabName);
		}
		return dsType;
	}
	
	
	/**
	 * Returns the name of the source whose data is being viewed on the selected tab.
	 * @return the source name, or <code>null</code> if the currently selected
	 * 	tab does not contain data for a specific source e.g. the Causality tab.
	 */
	public String getSelectedSource()
	{
		String source = null;
		
		DataSourceType selectedDsType = getSelectedDataSourceType();
		if (selectedDsType != null)
		{
			SourceViewWidget sourceViewWidget = getViewWidgetOnTab(selectedDsType);
			source = sourceViewWidget.getSource();
		}
		
		return source;
	}
	
	
	/**
	 * Selects the tab item which has been created for the specified data source type.
	 * Note that a tab must have already been added for the data source type.
	 * @param dataSourceType the data source type to select.
	 */
	public void setSelection(DataSourceType dataSourceType)
	{
		TabItem tabItem = findItem(dataSourceType);
		if (tabItem != null)
		{
			setSelection(tabItem);
			fitSelectedToSize();
		}
	}
	
	
	/**
	 * Shows the tab for the specified data source type. If a tab does not
	 * exist on the tab panel for the data source type, a new tab will be created.
	 * @param dataSourceType the DataSourceType for which to show the tab.
	 */
	public void showDataTab(DataSourceType dataSourceType)
	{
		showDataTab(dataSourceType, null);
	}
	
	
	/**
	 * Shows the tab for the specified data source type, loading data for the
	 * specified source. If a tab does not exist on the tab panel for the data
	 * source type, a new tab will be created.
	 * @param dataSourceType the DataSourceType for which to show the tab.
	 * @param source the name of the source (server) whose data is to be
	 * displayed. If <code>null</code>, data from all sources will be displayed.
	 */
	public void showDataTab(DataSourceType dataSourceType, String source)
	{
		TabItem tabItem = findItem(dataSourceType);
		if (tabItem == null)
		{
			addTab(dataSourceType, source);
			tabItem = findItem(dataSourceType);
		}
		
		if (tabItem != null)
		{
			SourceViewWidget viewWidget = getViewWidgetOnTab(dataSourceType);
			viewWidget.setSource(source);

			setSelection(dataSourceType);
	
			GWT.log("WorkAreaTabPanel showDataTab(DataSourceType, String) about to call load() ", null);
			viewWidget.load();
		}
	}
	
	
	/**
	 * Shows the tab for the specified data source type, loading data for the
	 * specified source at the given time. If a tab does not exist on the tab 
	 * panel for the data source type, a new tab will be created.
	 * @param dataSourceType the DataSourceType for which to show the tab.
	 * @param source the name of the source (server) whose data is to be
	 * displayed. If <code>null</code>, data from all sources will be displayed.
	 * @param date date/time of data to load.
	 */
	public void showDataTab(DataSourceType dataSourceType, String source, Date time)
	{
		TabItem tabItem = findItem(dataSourceType);
		if (tabItem == null)
		{
			addTab(dataSourceType, source);
			tabItem = findItem(dataSourceType);
		}
		
		if (tabItem != null)
		{
			SourceViewWidget viewWidget = getViewWidgetOnTab(dataSourceType);
			viewWidget.setSource(source);
			
			setSelection(dataSourceType);
			
			GWT.log("WorkAreaTabPanel showDataTab(DataSourceType, String, Date) about to call loadAtTime() ", null);
			viewWidget.loadAtTime(time);
		}
	}
	
	
	public void showNotificationTab(DataSourceType dataSourceType, String source)
	{
		showDataTab(dataSourceType, source);
	}
	
	
	/**
	 * 
	 * @param dataSourceType
	 * @param source
	 * @param date date/time of evidence data to load.
	 */
	public void showNotificationTab(DataSourceType dataSourceType, 
			String source, Date time, boolean clearFilter)
	{
		TabItem tabItem = findItem(dataSourceType);
		if (tabItem == null)
		{
			addTab(dataSourceType, source);
			tabItem = findItem(dataSourceType);
		}
		
		if (tabItem != null)
		{
			NotificationViewWidget viewWidget = 
				(NotificationViewWidget)(getViewWidgetOnTab(dataSourceType));
			viewWidget.setSource(source);
			if (clearFilter == true)
			{
				viewWidget.setFilter(null, null);
			}
			
			setSelection(dataSourceType);
			
			GWT.log("WorkAreaTabPanel showDataTab(DataSourceType, String, Date) about to call loadAtTime() ", null);
			viewWidget.loadAtTime(time);
		}
	}
	
	
	public void showNotificationTab(DataSourceType dataSourceType, String source, int evidenceId)
	{	
		TabItem tabItem = findItem(dataSourceType);
		if (tabItem == null)
		{
			addTab(dataSourceType, source);
			tabItem = findItem(dataSourceType);
		}
		
		if (tabItem != null)
		{
			NotificationViewWidget viewWidget = 
				(NotificationViewWidget)(getViewWidgetOnTab(dataSourceType));
			viewWidget.setSource(source);
			
			setSelection(dataSourceType);
			
			viewWidget.loadAtId(evidenceId);
		}
	}
	
	
	/**
	 * Shows the tab for the specified time series type, loading data for the
	 * specified source at the given time. If a tab does not exist on the tab 
	 * panel for the data source type, a new tab will be created.
	 * @param dataSourceType type of time series data for which to show the tab.
	 * @param metric the metric to display. If <code>null</code>, the current
	 * metric setting on the time series tab will be preserved.
	 * @param source the name of the source (server) whose data is to be
	 * displayed. If <code>null</code>, data from all sources will be displayed.
	 * @param attributeName optional attribute name for the time series to display.
	 * @param attributeValue optional attribute value for the time series to display.
	 * @param date date/time of data to load.
	 */
	public void showTimeSeriesTab(DataSourceType dataSourceType, String metric, 
			String source, String attributeName, String attributeValue, Date time)
	{
		GWT.log("WorkAreaTabPanel showTimeSeriesTab() metric=" + metric + ", source="+ source + 
				", " + attributeName + "=" + attributeValue, null);
		
		TabItem tabItem = findItem(dataSourceType);
		if (tabItem == null)
		{
			addTab(dataSourceType, source);
			tabItem = findItem(dataSourceType);
		}
		
		if (tabItem != null)
		{
			TimeSeriesViewWidget viewWidget = 
				(TimeSeriesViewWidget)(getViewWidgetOnTab(dataSourceType));
			viewWidget.setMetric(metric);
			viewWidget.setSource(source);
			viewWidget.setAttributeName(attributeName, false);
			viewWidget.setAttributeValue(attributeValue, false);
			
			setSelection(dataSourceType);
			
			viewWidget.loadAtTime(time);
		}
	}
	
	
	/**
	 * Shows the tab for the specified time series type, loading data for the
	 * specified source at the given time. If a tab does not exist on the tab 
	 * panel for the data source type, a new tab will be created.
	 * @param dataSourceType type of time series data for which to show the tab.
	 * @param metric the metric to display. If <code>null</code>, the current
	 * metric setting on the time series tab will be preserved.
	 * @param source the name of the source (server) whose data is to be
	 * displayed. If <code>null</code>, data from all sources will be displayed.
	 * @param model data model which is to be used to provide values for attributes
	 * available in the time series view.
	 * @param date date/time of data to load.
	 */
	public void showTimeSeriesTab(DataSourceType dataSourceType, String metric,
			String source, BaseModelData model, Date time)
	{
		// Examine the supplied model to check if it has non-null values for any
		// of the Time Series View's attributes.
		UsageView timeSeriesView = (UsageView)(m_DataViewsByType.get(dataSourceType.getName()));
		if (timeSeriesView != null)
		{
			String attributeName = null;
			String attributeValue = null;
			List<String> viewAttributes = timeSeriesView.getAttributeNames();
			if (viewAttributes != null)
			{
				for (String attribute : viewAttributes)
				{
					attributeValue = model.get(attribute);
					if (attributeValue != null)
					{
						attributeName = attribute;
						break;
					}
				}
			}
			
			showTimeSeriesTab(dataSourceType, metric, source, 
					attributeName, attributeValue, time);
		}
	}
	
	
	/**
	 * Shows the tab which displays a summary of the analysed data.
	 */
	protected void showAnalysedDataTab()
	{
		AnalysedDataWidget analysedDataWidget;
		
		TabItem tabItem = findItem(ANALYSED_TAB_TEXT, true);
		if (tabItem != null)
		{
			analysedDataWidget = (AnalysedDataWidget)(tabItem.getItem(0));
		}
		else
		{
			analysedDataWidget = new AnalysedDataWidget();
			
			addTab(ANALYSED_TAB_TEXT, analysedDataWidget);
			tabItem = findItem(ANALYSED_TAB_TEXT, true);
		}
		
		setSelection(tabItem);
		
		analysedDataWidget.load();
	}
	
	
	/**
	 * Shows the causality tab to display the probable causes and symptoms for
	 * the item of evidence or time series discord with the specified id.
	 * @param <M>
	 * @param model the item of evidence whose probable causes are being displayed.
	 */
	public <M> void showCausalityTab(EvidenceModel evidence)
	{	
		CausalityViewWidget causalityViewWidget;
		
		TabItem tabItem = findItem(CAUSALITY_TAB_TEXT, true);
		if (tabItem != null)
		{
			causalityViewWidget = (CausalityViewWidget)(tabItem.getItem(0));
		}
		else
		{
			causalityViewWidget = new CausalityViewWidget(m_CausalityView);
			
			// Add listeners to the widgets for 'Open Xxxx view' events.
			causalityViewWidget.addListener(GXTEvents.OpenNotificationViewClick, 
					new Listener<RequestViewEvent<M>>(){

                public void handleEvent(RequestViewEvent<M> rve)
                {
                	DataSourceType viewToOpenDataType = rve.getViewToOpenDataType();
                	String source = rve.getSourceName();
					M selected = rve.getModel();
					if ( (selected != null) && (selected.getClass() == EvidenceModel.class) )
					{
						EvidenceModel selectedEvidence = (EvidenceModel)selected;
						showNotificationTab(viewToOpenDataType, source, selectedEvidence.getId());
					}
					else
					{
						showNotificationTab(viewToOpenDataType, source, 
								rve.getOpenAtTime(), true);
					}
                }
				
			});
			
			causalityViewWidget.addListener(GXTEvents.OpenTimeSeriesViewClick, 
					new Listener<RequestViewEvent<M>>(){

				@Override
                public void handleEvent(RequestViewEvent<M> rve)
                {
					DataSourceType viewToOpenDataType = rve.getViewToOpenDataType();
					String metric = null;
                	String source = rve.getSourceName();
                	String attrName = null;
                	String attrVal = null;
                	Date openAtTime = rve.getOpenAtTime();
                	M selected = rve.getModel();
                	if ( (selected != null) && (selected.getClass() == TimeSeriesConfig.class) )
					{
						TimeSeriesConfig selectedTimeSeries = (TimeSeriesConfig)selected;
						metric = selectedTimeSeries.getMetric();
						attrName = selectedTimeSeries.getAttributeName();
						attrVal = selectedTimeSeries.getAttributeValue();
					}
					
					showTimeSeriesTab(viewToOpenDataType, metric, source, 
							attrName, attrVal, openAtTime);
                }
				
			});
			
			
			addTab(CAUSALITY_TAB_TEXT, causalityViewWidget);
			tabItem = findItem(CAUSALITY_TAB_TEXT, true);
		}
		
		causalityViewWidget.setEvidence(evidence);
		causalityViewWidget.load();
		
		setSelection(tabItem);
		fitSelectedToSize();
	}
	
	
	/**
	 * Creates a DataSourceType object for the data source with the specified name.
	 * @param dataSourceName name of data source.
	 * @return DataSourceType with the data source name and category.
	 */
	protected DataSourceType getDataSourceType(String dataSourceName)
	{
		// Determine the category of view (notification / time series).
		View view = m_DataViewsByType.get(dataSourceName);
		
		DataSourceType dsType = null;
		
		if (view != null)
		{
			if (view.getClass() == EvidenceView.class)
			{
				dsType = new DataSourceType(dataSourceName, DataSourceCategory.NOTIFICATION);
			}
			else if (view.getClass() == UsageView.class)
			{
				dsType = new DataSourceType(dataSourceName, DataSourceCategory.TIME_SERIES);
			}
		}
		
		return dsType;
	}
	
	
	/**
	 * Loads the view for the specified data source type.
	 * @param dataSourceType the DataSourceType for which to load the view.
	 * @param source the name of the source (server) whose data is to be
	 * displayed. If <code>null</code>, data from all sources will be displayed.
	 */
	protected void loadViewForTab(DataSourceType dataSourceType, String source)
	{
		GWT.log("WorkAreaTabPanel load view for type " + dataSourceType, null);
		
		// Load the View for this DataSourceType.
		final DataSourceType dsType = dataSourceType;
		final String sourceName = source;
		
		ApplicationResponseHandler<View> callback = new ApplicationResponseHandler<View>()
		{
			public void uponFailure(Throwable caught)
			{
				GWT.log("Error loading view for data type: " + dsType, caught);
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(), 
						ClientUtil.CLIENT_CONSTANTS.errorLoadingViewForType(), null);
			}


			public void uponSuccess(View view)
			{
				// Store the view in data structures, then show the new tab.
				m_DataViewsByType.put(dsType.getName(), view);
				showDataTab(dsType, sourceName);
			}
		};

	    // Make the call to the ViewCreationService.
	    m_ViewService.getView(dataSourceType, callback);
	}
	
	
	/**
	 * Loads the configuration for the data views (notifications and time series).
	 */
	protected void loadDataViews()
	{
		ApplicationResponseHandler<List<View>> callback = 
			new ApplicationResponseHandler<List<View>>()
		{
			public void uponFailure(Throwable caught)
			{
				GWT.log("Error loading data views", caught);
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(), 
						ClientUtil.CLIENT_CONSTANTS.errorLoadingViews(), null);
			}


			public void uponSuccess(List<View> views)
			{
				// Store the view in the relevant data structure.
				for (View view : views)
				{	
					if (view.getClass() == EvidenceView.class)
					{
						switch (view.getDataCategory())
						{
							case NOTIFICATION:
								m_DataViewsByType.put(view.getName(), view);
								break;
							case TIME_SERIES_FEATURE:
								m_TimeSeriesFeatureViews.put(view.getDataType(), (EvidenceView)view);
								break;
						}
					}
					else if (view.getClass() == UsageView.class)
					{
						m_DataViewsByType.put(view.getName(), view);
					}
					else if (view.getClass() == CausalityView.class)
					{
						m_CausalityView = (CausalityView)view;
					}
				}
			}
		};

	    // Make the call to the ViewCreationService.
	    m_ViewService.getViews(callback);
	}
	
	
	/**
	 * Sets the first tab item to be closable, or not, hiding the close icon
	 * on the tab header.
	 * @param closable true to make the tab closable, false otherwise.
	 */
	protected void setFirstTabItemClosable(boolean closable)
	{
		getItem(0).setClosable(closable);
    	
    	// Hide the Close icon on the tab, by setting the style
    	// of the link to 'display:none'.
    	// Not particularly elegant...
    	HeaderItem tabHeader = getItem(0).getHeader();
    	Element tabHeaderElement = tabHeader.getElement();
    	
    	NodeList<com.google.gwt.dom.client.Element> headerLinks = tabHeaderElement.getElementsByTagName("a");
    	if (headerLinks != null && headerLinks.getLength() > 0)
    	{
    		if (closable == true)
    		{
    			headerLinks.getItem(0).getStyle().setProperty("display", "block");
    		}
    		else
    		{
    			headerLinks.getItem(0).getStyle().setProperty("display", "none");
    		}
    	}
	}
	
	
	/**
	 * Creates and returns a widget containing a view for the specified data
	 * source type.
	 * @param <M> model data held in events fired by view widget.
	 * @param dataSourceType the data source type to display in the new widget.
	 * @return ViewWidget sub-class for displaying the provided data source.
	 * @throws UnsupportedOperationException if display of the supplied View type
	 * is not currently supported.
	 */
	protected <M> SourceViewWidget createViewWidget(View view)
	{
		SourceViewWidget viewWidget = null;
		
		if (view.getClass() == EvidenceView.class)
		{
			viewWidget = new NotificationViewWidget((EvidenceView)view);
		}
		else if (view.getClass() == UsageView.class)
		{
			// Pass in the corresponding evidence view for the features list.
			EvidenceView featuresView = m_TimeSeriesFeatureViews.get(view.getDataType());
			viewWidget = new TimeSeriesViewWidget((UsageView)view, featuresView);
		}
		else
		{			
			throw new UnsupportedOperationException("Specified class " + 
					view.getClass().getName() + " of View " + 
					view.getName() + " not supported.");
		}
		
		if (viewWidget != null)
		{
			// Add listeners to the widgets for 'Open Xxxx view' events.
			viewWidget.addListener(GXTEvents.OpenNotificationViewClick, 
					new Listener<RequestViewEvent<M>>(){

				@Override
                public void handleEvent(RequestViewEvent<M> rve)
                {
					DataSourceType viewToOpenDataType = rve.getViewToOpenDataType();
					String sourcename = rve.getSourceName();
					Date timeToOpen = rve.getOpenAtTime();
					showNotificationTab(viewToOpenDataType, sourcename, timeToOpen, false);
                }
				
			});
			
			viewWidget.addListener(GXTEvents.OpenTimeSeriesViewClick, 
					new Listener<RequestViewEvent<M>>(){

				@Override
                public void handleEvent(RequestViewEvent<M> rve)
                {
					DataSourceType viewToOpenDataType = rve.getViewToOpenDataType();
					String source = rve.getSourceName();
					Date timeToOpen = rve.getOpenAtTime();
					M model = rve.getModel();
					if (model.getClass() == EvidenceModel.class)
					{
						EvidenceModel evidence = (EvidenceModel)model;
						showTimeSeriesTab(viewToOpenDataType, null, source, evidence, timeToOpen);
					}
					else
					{
						TimeSeriesConfig timeSeries = (TimeSeriesConfig)model;
						showTimeSeriesTab(viewToOpenDataType, null, source, 
								timeSeries.getAttributeName(), 
								timeSeries.getAttributeValue(), timeToOpen);
					}
					
					
                }
				
			});
			
			viewWidget.addListener(GXTEvents.OpenCausalityViewClick, 
					new Listener<RequestViewEvent<EvidenceModel>>(){

				@Override
                public void handleEvent(RequestViewEvent<EvidenceModel> rve)
                {
					EvidenceModel model = rve.getModel();
					if (model != null)
					{
						showCausalityTab(model);
					}
					
                }
				
			});
		}
		
		return viewWidget;
	}
	
	
	/**
	 * Resizes the component on the selected tab to fit the available width.
	 */
	protected void fitSelectedToSize()
	{
		TabItem selectedTab = getSelectedItem();
		if (selectedTab != null)
		{
			BoxComponent widget = (BoxComponent)(selectedTab.getItem(0));
			
			int tabPanelWidth = getWidth();
			if (tabPanelWidth > 20)
			{
				widget.setWidth(tabPanelWidth - 32);
			}
			
			selectedTab.layout(true);
		}
	}
	
	
	/**
	 * Reloads the data on the currently selected tab.
	 */
	protected void reloadTab()
	{
		GWT.log("! reloadTab()", null);
		
		TabItem selectedTab = getSelectedItem();
		if (selectedTab != null)
		{
			final SourceViewWidget widget = (SourceViewWidget)(selectedTab.getItem(0));
			
			// Only reload for time series??
			//String dataType = widget.getView().getDataType();
			//DataSourceType dsType = getDataSourceType(dataType);
			
			DeferredCommand.addCommand(new Command()
			{
				public void execute()
				{
					widget.load();
				}
			});
						
		}
	}
	
	
	/**
	 * Returns the SourceViewWidget on the tab for the specified data source type.
	 * @param dataSourceType data source type.
	 * @return the SourceViewWidget, or <code>null</code> if no tab has been created 
	 * 		to display data for the specified data source type.
	 */
	protected SourceViewWidget getViewWidgetOnTab(DataSourceType dataSourceType)
	{
		TabItem tabItem = findItem(dataSourceType);
		SourceViewWidget widget = (SourceViewWidget)(tabItem.getItem(0));
		
		return widget;
	}
	
	
	public void appletViewToolRun(String viewToOpenDataType, String source, String timeStr)
	{
		//MessageBox.alert("TimeSeriesChartApplet", "show view - " + viewToOpenDataType, null);
		
		Date time = null;
		DateTimeFormat formatter = ClientUtil.SECOND_TIME_FORMAT;
		try
		{
			time = formatter.parse(timeStr);
		}
		catch (IllegalArgumentException e)
		{
			GWT.log("WorkAreaTabPanel.appletViewToolRun() - time could not be parsed: " + 
					timeStr, e);
		}

		DataSourceType dataSourceType = getDataSourceType(viewToOpenDataType);
		if (dataSourceType != null)
		{
			if (dataSourceType.getDataCategory() == DataSourceCategory.NOTIFICATION)
			{
				showNotificationTab(dataSourceType, source, time, false);
			}
			else
			{		
				TabItem tabItem = findItem(dataSourceType);
				if (tabItem == null)
				{
					addTab(dataSourceType, source);
					tabItem = findItem(dataSourceType);
				}
				
				if (tabItem != null)
				{
					setSelection(dataSourceType);
					
					final SourceViewWidget viewWidget = getViewWidgetOnTab(dataSourceType);
					final Date loadTime = time;
					viewWidget.setSource(source);
					
					DeferredCommand.addCommand(new Command()
					{
						public void execute()
						{
							((TimeSeriesViewWidget)viewWidget).loadAtTime(loadTime);
						}
					});
					
					
				}
			}
		}
	}
	
	
	public void notifyChartAppletStopped(String appletId)
	{
		Info.display("Applet stopped", "Stopped: " + appletId);
	}
	
	
	public void showProbableCause(String probCauseId)
	{
		GWT.log("show on causality chart: " + probCauseId, null);
	}
	
	
	
	public native void defineBridgeMethod() /*-{      
	var tabPanel = this;
	$wnd.appletViewToolRun = function(dataType, source, time)  {          
		tabPanel.@demo.app.splash.gxt.WorkAreaTabPanel::appletViewToolRun(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)(dataType, source, time);       
	}    
}-*/;
	
	
	public native void defineBridgeMethod2() /*-{      
	var tabPanel = this;
	$wnd.notifyChartAppletStopped = function(appletId)  {          
		tabPanel.@demo.app.splash.gxt.WorkAreaTabPanel::notifyChartAppletStopped(Ljava/lang/String;)(appletId);       
	}    
}-*/;
	

	
}
