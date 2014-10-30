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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.TabPanelEvent;
import com.extjs.gxt.ui.client.widget.BoxComponent;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.TabItem;
import com.extjs.gxt.ui.client.widget.TabPanel;
import com.extjs.gxt.ui.client.widget.TabItem.HeaderItem;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.Element;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.client.ClientUtil;
import com.prelert.client.gxt.SourceViewWidget;
import com.prelert.client.event.GXTEvents;
import com.prelert.client.event.RequestViewEvent;
import com.prelert.data.Attribute;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.EvidenceView;
import com.prelert.data.TimeSeriesView;
import com.prelert.data.View;
import com.prelert.data.gxt.DataSourceModel;
import com.prelert.data.gxt.EvidenceModel;
import com.prelert.service.AsyncServiceLocator;
import com.prelert.service.DataSourceQueryServiceAsync;
import com.prelert.service.ViewCreationServiceAsync;


/**
 * An extension of the Ext GWT TabPanel for the main work area in the Explorer
 * module, displaying each data source type in a separate tab.
 * It applies the necessary style attributes and manages the addition and
 * removal of tabs for the various data source types.
 * @author Pete Harverson
 */
public class ExplorerTabPanel extends TabPanel
{
	private ViewCreationServiceAsync		m_ViewService;
	
	private List<DataSourceType>			m_DataSourceTypes;
	private HashMap<String, View>			m_DataViewsByType;// Data views against type name.
	
	private static final String ANALYSED_DATA_TAB_TEXT = ClientUtil.CLIENT_CONSTANTS.analysedData();
	
	
	/**
	 * Creates a new tab panel for the work area in the Explorer module.
	 */
	public ExplorerTabPanel()
	{
		m_ViewService = AsyncServiceLocator.getInstance().getViewCreationService();
		
		loadDataTypes();
		
		m_DataViewsByType = new HashMap<String, View>();
		
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
	}


	/**
	 * Adds a new tab to the work area tab panel to hold the specified widget.
	 * @param text	the text for the new tab.
	 * @param widget the widget to add to the tab.
	 * @return the new TabItem that has been added to the work area.
	 */
	public TabItem addTab(String text, Widget widget)
	{
		TabItem tab = new TabItem();

		tab.setClosable(true);
		tab.setText(text);

        tab.add(widget);
        tab.addStyleName("prl-workAreaTabItem");

		add(tab);
		
		if (getItemCount() == 1)
		{
			setFirstTabItemClosable(false);
		}
		
		return tab;
	}
	
	
	/**
	 * Adds a tab to show data for the specified view.
	 * @param view the notification or time series view for which to add a tab.
	 */
	protected void addTabForView(View view)
	{
		String dataType = view.getDataType();
		
		try
		{
			// Create the ViewWidget, and add to a new tab.
			SourceViewWidget viewWidget = createViewWidget(view);
			addTab(dataType, viewWidget.getWidget());
		}
		catch (UnsupportedOperationException e)
		{
			GWT.log("No view has been configured for data type: " + dataType, e);
			MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(), 
					ClientUtil.CLIENT_CONSTANTS.errorNoViewForType() + dataType, null);
		}
	}
	
	
	/**
	 * Searches for the tab item for the specified data source type.
	 * If the view for the specified data type has been loaded, but no tab is 
	 * currently open for that type, the tab will be created and added to the tab panel.
	 * @param dataSourceType data source type of tab to find.
	 * @return the tab item, or <code>null</code> if no view has been loaded for
	 * 		the specified data source type.
	 */
	public TabItem findItem(DataSourceType dataSourceType)
	{
		String typeName = dataSourceType.getName();
		
		TabItem tabItem = findItem(typeName, true);
		if (tabItem == null)
		{
			View view =  m_DataViewsByType.get(typeName);
			if (view != null)
			{
				addTabForView(view);
				tabItem = findItem(typeName, true);
			}
		}
		
		return tabItem;
	}
	
	
	/**
	 * Returns the data source type of the selected tab.
	 * @return the DataSourceType, or <code>null</code> if the currently selected
	 * 	tab does not contain data for a specific source type e.g. the Analysed Data tab.
	 */
	public DataSourceType getSelectedDataSourceType()
	{
		TabItem selectedTab = getSelectedItem();
		String tabName = selectedTab.getText();
		
		return getDataSourceType(tabName);
	}
	
	
	/**
	 * Returns the name of the source whose data is being viewed on the selected tab.
	 * @return the source name, or <code>null</code> if data from all sources is
	 * 		currently being viewed.
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
	 * Returns whether the 'Analysed Data' tab is currently selected.
	 * @return <code>true</code> if the 'Analysed Data' tab is selected,
	 * 		<code>false</code> otherwise.
	 */
	public boolean isAnalysedDataTabSelected()
	{
		TabItem selectedTab = getSelectedItem();
		String tabName = selectedTab.getText();
		
		return tabName.equals(ANALYSED_DATA_TAB_TEXT);
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
	 * Shows the tab which displays a summary of the analysed data.
	 */
	public void showAnalysedDataTab()
	{
		AnalysedDataWidget analysedDataWidget;
		
		TabItem tabItem = findItem(ANALYSED_DATA_TAB_TEXT, true);
		if (tabItem != null)
		{
			analysedDataWidget = (AnalysedDataWidget)(tabItem.getItem(0));
		}
		else
		{
			// Add listeners to the widget for 'Open Xxxx view' events.
			analysedDataWidget = new AnalysedDataWidget();
			tabItem = addTab(ANALYSED_DATA_TAB_TEXT, analysedDataWidget);
			tabItem.setScrollMode(Scroll.AUTO);
		}
		
		setSelection(tabItem);
		
		analysedDataWidget.load();
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
	 * specified source at the given time. If a tab does not exist on the tab 
	 * panel for the data source type, a new tab will be created.
	 * @param dataSourceType the DataSourceType for which to show the tab.
	 * @param source the name of the source (server) whose data is to be
	 * displayed. If <code>null</code>, data from all sources will be displayed.
	 */
	public void showDataTab(DataSourceType dataSourceType, String source)
	{
		TabItem tabItem = findItem(dataSourceType);
		if (tabItem != null)
		{
			SourceViewWidget viewWidget = getViewWidgetOnTab(dataSourceType);
			viewWidget.setSource(source);
			
			setSelection(dataSourceType);
			viewWidget.load();
		}
		else
		{
			// Load the View for this DataSourceType.
			final DataSourceType dsType = dataSourceType;
			final String sourceName = source;
			
			LoadViewCallback callback = new LoadViewCallback(dsType.getName())
			{
				public void uponSuccess(View view)
				{
					super.uponSuccess(view);
					showDataTab(dsType, sourceName);
				}
			};
			
			// Make the call to the ViewCreationService.
		    m_ViewService.getView(dataSourceType, callback);
		}
	}
	
	
	/**
	 * Shows the tab for the specified data source type, loading data for the
	 * notification or time series feature with the specified id. If a tab does not
	 * exist on the tab panel for the data source type, a new tab will be created.
	 * @param dataSourceType the DataSourceType for which to show the tab.
	 * @param source the name of the source (server) whose data is to be
	 * displayed. If <code>null</code>, data from all sources will be displayed.
	 * @param evidenceId id of the notification or time series feature to be loaded.
	 */
	public void showDataTab(DataSourceType dataSourceType, String source, int evidenceId)
	{
		if (dataSourceType.getDataCategory() == DataSourceCategory.NOTIFICATION)
		{
			showNotificationTab(dataSourceType, source, evidenceId);
		}
		else if (dataSourceType.getDataCategory() == DataSourceCategory.TIME_SERIES)
		{
			showTimeSeriesTab(dataSourceType, source, evidenceId);
		}
	}
	
	
	/**
	 * Shows the tab for the specified notification-type data source, loading data
	 * for the specified source (server) at the given time. If a tab does not exist 
	 * on the tab  panel for the data source type, a new tab will be created.
	 * @param dataSourceType the notification data source for which to show the tab.
	 * @param source the name of the source (server) whose data is to be
	 * displayed. If <code>null</code>, data from all sources will be displayed.
	 * @param date date/time of data to load.
	 * @param clearFilter <code>true</code> to clear any filter on the notification
	 * 		tab, <code>false</code> otherwise.
	 */
	public void showNotificationTab(DataSourceType dataSourceType, 
			String source, Date time, boolean clearFilter)
	{	
		TabItem tabItem = findItem(dataSourceType);
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
			viewWidget.loadAtTime(time);
		}
		else
		{
			// Load the View for this data type.
			final DataSourceType dsType = dataSourceType;
			final String sourceName = source;
			final Date loadTime = time;
			final boolean clearFilt = clearFilter;
			
			LoadViewCallback callback = new LoadViewCallback(dsType.getName())
			{
				public void uponSuccess(View view)
				{
					super.uponSuccess(view);
					showNotificationTab(dsType, sourceName, loadTime, clearFilt);
				}
			};
			
			// Make the call to the ViewCreationService.
		    m_ViewService.getView(dataSourceType, callback);
		}
	}
	
	
	/**
	 * Shows the tab for the specified notification-type data source, loading the 
	 * data in the tab so that the top of row of evidence will match the specified id. 
	 * If a tab does not exist on the tab panel for the data source type, a new tab 
	 * will be created.
	 * @param dataSourceType the notification data source for which to show the tab.
	 * @param source the name of the source (server) whose data is to be
	 * displayed. If <code>null</code>, data from all sources will be displayed.
	 * @param evidenceId id for the top row of evidence data to be loaded.
	 */
	public void showNotificationTab(DataSourceType dataSourceType, String source, int evidenceId)
	{	
		TabItem tabItem = findItem(dataSourceType);
		if (tabItem != null)
		{
			final NotificationViewWidget viewWidget = 
				(NotificationViewWidget)(getViewWidgetOnTab(dataSourceType));
			viewWidget.setSource(source);
			
			// Select tab and load in DeferredCommand to ensure tab is properly
			// displayed if launching from outside Explorer module.
			final DataSourceType dsType = dataSourceType;
			final int evId = evidenceId;
			Scheduler.get().scheduleDeferred(new ScheduledCommand()
			{
				@Override
				public void execute()
				{
					setSelection(dsType);
					viewWidget.loadAtId(evId);
				}
			});
		}
		else
		{
			// Load the View for this data type.
			final DataSourceType dsType = dataSourceType;
			final String sourceName = source;
			final int evId = evidenceId;
			
			LoadViewCallback callback = new LoadViewCallback(dsType.getName())
			{
				public void uponSuccess(View view)
				{
					super.uponSuccess(view);
					showNotificationTab(dsType, sourceName, evId);
				}
			};
			
			// Make the call to the ViewCreationService.
		    m_ViewService.getView(dataSourceType, callback);
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
	 * @param attributes optional list of attributes for the time series.
	 * @param date date/time of data to load.
	 */
	public void showTimeSeriesTab(DataSourceType dataSourceType, String metric, 
			String source, List<Attribute> attributes, Date time)
	{	
		TabItem tabItem = findItem(dataSourceType);
		if (tabItem != null)
		{
			TimeSeriesViewWidget viewWidget = 
				(TimeSeriesViewWidget)(getViewWidgetOnTab(dataSourceType));
			viewWidget.setMetric(metric, false, false);
			viewWidget.setSource(source);
			
			if (attributes != null)
			{
				for (Attribute attribute : attributes)
				{
					viewWidget.setAttributeValue(attribute.getAttributeName(), 
							attribute.getAttributeValue(), false);
				}
			}
			
			setSelection(dataSourceType);	
			viewWidget.loadAtTime(time);
		}
		else
		{
			// Load the View for this data type.
			final DataSourceType dsType = dataSourceType;
			final String metricName = metric;
			final String sourceName = source;
			final List<Attribute> attrs = attributes; 
			final Date loadTime = time;
			
			LoadViewCallback callback = new LoadViewCallback(dsType.getName())
			{
				public void uponSuccess(View view)
				{
					super.uponSuccess(view);
					showTimeSeriesTab(dsType, metricName, sourceName, attrs, loadTime);
				}
			};
			
			// Make the call to the ViewCreationService.
		    m_ViewService.getView(dataSourceType, callback);
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
		TimeSeriesView timeSeriesView = (TimeSeriesView)(m_DataViewsByType.get(dataSourceType.getName()));
		if (timeSeriesView != null)
		{
			List<Attribute> attributes = null;
			List<String> viewAttributes = timeSeriesView.getAttributeNames();
			if (viewAttributes != null)
			{
				attributes = new ArrayList<Attribute>();
				String attributeValue;
				for (String attributeName : viewAttributes)
				{
					attributeValue = model.get(attributeName);
					attributes.add(new Attribute(attributeName, attributeValue));
				}
			}
			
			showTimeSeriesTab(dataSourceType, metric, source, attributes, time);
		}
	}
	
	
	/**
	 * Shows the tab for the specified time series type data source, loading the 
	 * data in the tab for the feature with the specified id.
	 * @param dataSourceType type of time series data for which to show the tab.
	 * @param source the name of the source (server) whose data is to be
	 * displayed. If <code>null</code>, data from all sources will be displayed.
	 * @param evidenceId id for the time series feature to be loaded.
	 */
	public void showTimeSeriesTab(DataSourceType dataSourceType, String source, int featureId)
	{	
		TabItem tabItem = findItem(dataSourceType);
		if (tabItem != null)
		{
			TimeSeriesViewWidget viewWidget = 
				(TimeSeriesViewWidget)(getViewWidgetOnTab(dataSourceType));
			viewWidget.setSource(source);
			
			setSelection(dataSourceType);	
			viewWidget.loadFeatureId(featureId);
		}
		else
		{
			// Load the View for this data type.
			final DataSourceType dsType = dataSourceType;
			final String sourceName = source;
			final int evId = featureId;
			
			LoadViewCallback callback = new LoadViewCallback(dsType.getName())
			{
				public void uponSuccess(View view)
				{
					super.uponSuccess(view);
					showTimeSeriesTab(dsType, sourceName, evId);
				}
			};
			
			// Make the call to the ViewCreationService.
		    m_ViewService.getView(dataSourceType, callback);
		}
	}
	
	
	/**
	 * Creates a DataSourceType object for the data source with the specified name.
	 * @param dataSourceName name of data source.
	 * @return DataSourceType with the data source name and category.
	 */
	protected DataSourceType getDataSourceType(String dataSourceName)
	{
		DataSourceType dsType = null;
		
		for (DataSourceType dataType : m_DataSourceTypes)
		{
			if (dataType.getName().equals(dataSourceName))
			{
				dsType = dataType;
				break;
			}
		}
		
		return dsType;
	}
	
	
	/**
	 * Loads the list of data types from the server.
	 */
	protected void loadDataTypes()
	{
		m_DataSourceTypes = new ArrayList<DataSourceType>();
		
		ApplicationResponseHandler<List<DataSourceModel>> callback = 
			new ApplicationResponseHandler<List<DataSourceModel>>()
		{
			@Override
            public void uponFailure(Throwable caught)
			{
				GWT.log("Error loading list of data types", caught);
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(), 
						ClientUtil.CLIENT_CONSTANTS.errorLoadingDataSources(), null);
			}


			@Override
            public void uponSuccess(List<DataSourceModel> models)
			{
				for (DataSourceModel model : models)
				{
					m_DataSourceTypes.add(model.getDataSourceType());
				}
				
				GWT.log("ExplorerTabPanel, loaded " + models.size() + " data types");
				// TODO - if any views already open, pass list of data types for context menu.
			}
		};

		DataSourceQueryServiceAsync dataSourceQueryService = 
			AsyncServiceLocator.getInstance().getDataSourceQueryService();
		dataSourceQueryService.getDataSourceTypes(callback);
	}
	
	
	/**
	 * Loads the complete set of data views (notifications and time series).
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
					switch (view.getDataCategory())
					{
						case NOTIFICATION:
						case TIME_SERIES:
							m_DataViewsByType.put(view.getName(), view);
							break;
						case TIME_SERIES_FEATURE:
							// Time series feature views should only be received 
							// as part of the corresponding time series view.
							break;
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
		else if (view.getClass() == TimeSeriesView.class)
		{
			viewWidget = new TimeSeriesViewWidget((TimeSeriesView)view);
		}
		else
		{			
			throw new UnsupportedOperationException("Specified class " + 
					view.getClass().getName() + " of View " + 
					view.getName() + " not supported.");
		}
		
		final String viewType = view.getDataType();
		if (viewWidget != null)
		{
			viewWidget.setLinkToDataTypes(m_DataSourceTypes);
			
			// Add listeners to the widgets for 'Open Xxxx view' events.
			viewWidget.addListener(GXTEvents.OpenNotificationViewClick, 
					new Listener<RequestViewEvent<M>>(){

				@Override
                public void handleEvent(RequestViewEvent<M> rve)
                {
					DataSourceType viewToOpenDataType = rve.getViewToOpenDataType();
					Date timeToOpen = rve.getOpenAtTime();
					
					M selected = rve.getModel();
					if ( (selected != null) && (selected.getClass() == EvidenceModel.class) &&
							(viewType.equals(viewToOpenDataType.getName())) )
					{
						// Same notification type as source model - load for all sources at this ID.
						EvidenceModel selectedEvidence = (EvidenceModel)selected;
						showNotificationTab(viewToOpenDataType, null, selectedEvidence.getId());
						
						// Fire a Select event on the tab as the source has changed to 'All'.
						fireEvent(Events.Select, new TabPanelEvent(ExplorerTabPanel.this, getSelectedItem()));
					}
					else
					{
						// Load for all sources at this time, and clear filter.
						showNotificationTab(viewToOpenDataType, null, timeToOpen, true);
					}
                }
				
			});
			
			viewWidget.addListener(GXTEvents.OpenTimeSeriesViewClick, 
					new Listener<RequestViewEvent<M>>(){

				@Override
                public void handleEvent(RequestViewEvent<M> rve)
                {
					DataSourceType viewToOpenDataType = rve.getViewToOpenDataType();
					Date timeToOpen = rve.getOpenAtTime();
					
					// Load for all sources and attributes (if any) at this time.
					List<Attribute> attributes = null;
					showTimeSeriesTab(viewToOpenDataType, null, null, attributes, timeToOpen);
					
					/*
					String source = rve.getSourceName();
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
					*/
					
                }
				
			});
			
			viewWidget.addListener(GXTEvents.OpenCausalityViewClick, 
					new Listener<RequestViewEvent<EvidenceModel>>(){

				@Override
                public void handleEvent(RequestViewEvent<EvidenceModel> rve)
                {
					// Propagate the event.
	        		fireEvent(rve.getType(), rve);
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
			int widthToSet = getWidth() - 12; // Subtract margin plus border.
			
			Scroll scrollMode = selectedTab.getScrollMode();
			if (scrollMode == Scroll.ALWAYS || scrollMode == Scroll.AUTO ||
					scrollMode == Scroll.AUTOY)
			{
				widthToSet -= 20;
			}
			
			//int heightToSet = getHeight() - 40; // Allow for tab height, margin and border.
			int heightToSet = getHeight() - 36; // Allow for tab height, margin and border.

			BoxComponent widget = (BoxComponent)(selectedTab.getItem(0));
			widget.setSize(widthToSet, heightToSet);
			selectedTab.layout(true);
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
	
	
	/**
	 * Extension of ApplicationResponseHandler to handle common functionality 
	 * when loading View data for a new tab.
	 */
	class LoadViewCallback extends ApplicationResponseHandler<View>
	{
		private String m_DataType;
		
		LoadViewCallback(String dataType)
		{
			m_DataType = dataType;
		}
		
		
		public void uponFailure(Throwable caught)
		{
			GWT.log("Error loading view for data type: " + m_DataType, caught);
			MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(), 
					ClientUtil.CLIENT_CONSTANTS.errorLoadingViewForType(m_DataType), null);
		}


		public void uponSuccess(View view)
		{
			// Store the view in data structures, then add the new tab.
			m_DataViewsByType.put(m_DataType, view);
			addTabForView(view);
		}
	}
	
}
