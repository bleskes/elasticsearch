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
import java.util.List;

import com.extjs.gxt.ui.client.GXT;
import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.event.*;
import com.extjs.gxt.ui.client.util.DateWrapper;
import com.extjs.gxt.ui.client.util.IconHelper;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.util.Padding;
import com.extjs.gxt.ui.client.widget.BoxComponent;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Label;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.SplitBar;
import com.extjs.gxt.ui.client.widget.TabItem;
import com.extjs.gxt.ui.client.widget.TabPanel;
import com.extjs.gxt.ui.client.widget.VerticalPanel;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.grid.GridSelectionModel;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.FlowLayout;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayout;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayoutData;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayout.HBoxLayoutAlign;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout.VBoxLayoutAlign;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.ui.Widget;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.client.ClientUtil;
import com.prelert.client.chart.ChartToolBar;
import com.prelert.client.chart.TimeSeriesChartWidget;
import com.prelert.client.chart.TimeSeriesGChartWidget;
import com.prelert.client.event.GXTEvents;
import com.prelert.client.event.RequestViewEvent;
import com.prelert.client.event.AttributesEditorEvent;
import com.prelert.client.gxt.SourceViewWidget;
import com.prelert.client.list.EvidenceViewPanel;
import com.prelert.data.Attribute;
import com.prelert.data.DataSourceType;
import static com.prelert.data.PropertyNames.*;

import com.prelert.data.EvidenceView;
import com.prelert.data.TimeFrame;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.TimeSeriesDataPoint;
import com.prelert.data.TimeSeriesView;
import com.prelert.data.gxt.EvidenceModel;
import com.prelert.service.AsyncServiceLocator;
import com.prelert.service.EvidenceQueryServiceAsync;
import com.prelert.service.TimeSeriesGXTPagingServiceAsync;


/**
 * ViewWidget implementation for displaying a time series view. The widget
 * consists of a chart for displaying time series data and a toolbar for 
 * controlling the data displayed i.e. time series attributes and metric.
 *
 * @author Pete Harverson
 */
public class TimeSeriesViewWidget extends LayoutContainer implements SourceViewWidget
{
	private TimeSeriesGXTPagingServiceAsync	m_QueryService;
	private EvidenceQueryServiceAsync 	m_EvidenceQueryService;
	
	private TimeSeriesView 			m_TimeSeriesView;
	
	private String					m_Source;
	private List<Attribute>			m_Attributes;
	private String					m_Metric;
	
	private ContentPanel			m_ChartContainer;
	private TimeSeriesChartWidget	m_ChartWidget;
	private ChartToolBar<TimeSeriesDataPoint> m_ChartTools;
	private Label					m_MetricLabel;
	private Label 					m_SourceLabel;
	private LayoutContainer			m_TopCont;
	private LayoutContainer 		m_AttributeLabelPanel;
	private List<Label>				m_AttributeLabels;
	protected Button 				m_RefreshBtn;
	
	private TabPanel				m_ConfigTabPanel;
	private AttributesEditor		m_AttributesEditor;
	private EvidenceViewPanel		m_FeaturesPanel;
	private Listener<SplitBarEvent>	m_SplitBarListener;
	

	/**
	 * Creates a new widget for displaying a time series view.
	 * @param timeSeriesView the view to display in the widget.
	 */
	public TimeSeriesViewWidget(TimeSeriesView timeSeriesView)
	{
		m_TimeSeriesView = timeSeriesView;

		if (m_TimeSeriesView.hasAttributes() == true)
		{
			// NB. Attributes are only added when set through the controls/feature list.
			m_Attributes = new ArrayList<Attribute>();
		}
		
		m_QueryService = AsyncServiceLocator.getInstance().getTimeSeriesGXTQueryService();
		m_EvidenceQueryService = AsyncServiceLocator.getInstance().getEvidenceQueryService();
		
		initComponents();
	}
	
	
	/**
	 * Creates and initialises the graphical components in the window.
	 */
	protected void initComponents()
	{
		BorderLayout borderLayout = new BorderLayout();   
		borderLayout.setContainerStyle("prl-border-layout-ct");
	    setLayout(borderLayout); 
	      
		m_ChartContainer = new ContentPanel(); 
		m_ChartContainer.setHeaderVisible(false);
		
		VBoxLayout layout = new VBoxLayout(); 
        layout.setVBoxLayoutAlign(VBoxLayoutAlign.STRETCH);   
        m_ChartContainer.setLayout(layout);  
        m_ChartContainer.setSize(750, 500);

		
		// Create a panel at the top to hold the chart labels,
        // and the zoom and refresh controls.
        m_TopCont = new LayoutContainer();   
        m_TopCont.setHeight(45);
        HBoxLayout hblayout = new HBoxLayout();   
        hblayout.setPadding(new Padding(0, 3, 0, 3));    
        hblayout.setHBoxLayoutAlign(HBoxLayoutAlign.TOP);   
        m_TopCont.setLayout(hblayout); 
        HBoxLayoutData flex = new HBoxLayoutData();   
    	flex.setFlex(1);  

        
    	// Create the labels for displaying the metric and source name.
        m_MetricLabel = new Label(ClientUtil.CLIENT_CONSTANTS.timeSeriesChartSelectToLoad(
        		m_TimeSeriesView.getDataType()));
        m_MetricLabel.addStyleName("prl-timeSeriesChart-title");
        
        m_SourceLabel = new Label("");
        m_SourceLabel.addStyleName("prl-timeSeriesChart-title");
        
        VerticalPanel metricLabelPanel = new VerticalPanel();
        metricLabelPanel.add(m_MetricLabel);
        metricLabelPanel.add(m_SourceLabel);
        
        if (m_TimeSeriesView.hasAttributes())
        {
        	List<String> attributeNames = m_TimeSeriesView.getAttributeNames();
        	
        	// Create the labels for displaying the attributes.
        	m_AttributeLabels = new ArrayList<Label>();
        	
	        int numAttributes = attributeNames.size();
	        if (numAttributes <= 3)
	        {
	        	// One attribute label per row, no line wrap.
	        	m_AttributeLabelPanel = new VerticalPanel();
	        }
	        else
	        {
	        	// Flow layout of labels, with line wrap at white space.
	        	m_AttributeLabelPanel = new LayoutContainer(new FlowLayout());
	        }
	        
	        m_AttributeLabelPanel.setStyleAttribute("overflow", "hidden");
	        
	        Label attributeLabel;
			for (int i = 0; i < numAttributes; i++)
			{
				attributeLabel = new Label();
				attributeLabel.addStyleName("prl-timeSeriesChart-subtitle");
				if (numAttributes > 3)
				{
					attributeLabel.setStyleAttribute("white-space", "normal");
				}
				m_AttributeLabels.add(attributeLabel);
		        m_AttributeLabelPanel.add(attributeLabel);
			}      
	        
			m_TopCont.add(metricLabelPanel);
			flex.setMargins(new Margins(0, 5, 0, 15));  
	    	m_TopCont.add(m_AttributeLabelPanel, flex); 
        }
        else
        {
        	flex.setMargins(new Margins(0, 5, 0, 0)); 
        	m_TopCont.add(metricLabelPanel, flex); 
        }
        
        
        m_ChartWidget = new TimeSeriesGChartWidget();
        m_ChartWidget.setHighlightingFeatures(true);
        
        // Listen for events to open notification, time series and causality views.
		Listener<RequestViewEvent<TimeSeriesConfig>> chartListener = 
			new Listener<RequestViewEvent<TimeSeriesConfig>>(){

            public void handleEvent(RequestViewEvent<TimeSeriesConfig> rve)
            {
            	// Propagate the event.
        		fireEvent(rve.getType(), rve);
            }
			
		};
		
		m_ChartWidget.addListener(GXTEvents.OpenNotificationViewClick, chartListener);
		m_ChartWidget.addListener(GXTEvents.OpenTimeSeriesViewClick, chartListener);
		m_ChartWidget.addListener(GXTEvents.OpenCausalityViewClick, chartListener);

        
        // Add the zooming and panning tools, and the refresh button.
        m_ChartTools = new ChartToolBar<TimeSeriesDataPoint>(m_ChartWidget, true);
        m_ChartTools.setWidth(126);
        
        m_RefreshBtn = new Button();
		m_RefreshBtn.setIcon(GXT.IMAGES.paging_toolbar_refresh());
		m_RefreshBtn.setToolTip(GXT.MESSAGES.pagingToolBar_refreshText());
		m_RefreshBtn.setMouseEvents(false);		// Disable mouse events for plain white toolbar.
		m_RefreshBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
			{
				refresh();
			}
		});
		m_ChartTools.add(m_RefreshBtn);
		m_ChartTools.setEnabled(false);	// Disabled till a chart is loaded.
        
        m_TopCont.add(m_ChartTools);
        
        m_ChartContainer.add(m_TopCont);
        m_ChartContainer.add(m_ChartWidget.getChartWidget()); 
        
        LoadListener loadListener = new LoadListener()
		{
			@Override
            public void loaderBeforeLoad(LoadEvent le)
			{
				m_RefreshBtn.setIcon(IconHelper.createStyle("x-tbar-loading"));
				m_ChartTools.setEnabled(false);
				m_ChartContainer.mask(GXT.MESSAGES.loadMask_msg());
			}


			@Override
            public void loaderLoad(LoadEvent le)
			{
				m_RefreshBtn.setIcon(IconHelper.createStyle("x-tbar-refresh"));
				m_ChartTools.setEnabled(true);
				m_ChartContainer.unmask();
			}


			@Override
            public void loaderLoadException(LoadEvent le)
			{
				m_RefreshBtn.setIcon(IconHelper.createStyle("x-tbar-refresh"));
				m_ChartTools.setEnabled(true);
				m_ChartContainer.unmask();
			}
		};
		m_ChartWidget.addLoadListener(loadListener);
        
        m_ConfigTabPanel = createConfigurationTabPanel();
          
        BorderLayoutData centerData = new BorderLayoutData(LayoutRegion.CENTER);   
	    centerData.setMargins(new Margins(0));  
	    
	    BorderLayoutData southData = new BorderLayoutData(LayoutRegion.SOUTH, 0.45f, 30, 525);   
	    southData.setSplit(true);    
	    southData.setFloatable(false);   
	    southData.setMargins(new Margins(5, 0, 0, 0)); 
	    
	    m_SplitBarListener = new Listener<SplitBarEvent>()
		{
			public void handleEvent(SplitBarEvent sbe)
			{
				if (sbe.getSize() < 1)
				{
					return;
				}

				m_ChartWidget.setChartHeight(m_ChartContainer.getHeight() - m_TopCont.getHeight());
				m_AttributesEditor.setHeight(m_ConfigTabPanel.getHeight(true) - 30);
				m_FeaturesPanel.setHeight(m_ConfigTabPanel.getHeight(true) - 30);
			}
		};
	    
	    add(m_ChartContainer, centerData);   
	    add(m_ConfigTabPanel, southData);
		
		addListener(Events.AfterLayout, new Listener<ContainerEvent<LayoutContainer, Component>>(){

			@Override
            public void handleEvent(ContainerEvent<LayoutContainer, Component> be)
            {
				SplitBar splitBar = m_ConfigTabPanel.getData("splitBar");
		        if (splitBar != null)
		        {
		        	splitBar.addListener(Events.DragEnd, m_SplitBarListener);
		        	TimeSeriesViewWidget.this.removeListener(Events.AfterLayout, this);
		        }
            }
			
		});
	}
	
	
	/** 
	 * Creates the tab panel with Properties and Features tabs used for
	 * configuring the time series chart.
	 * @return TabPanel holding Properties and Features tabs.
	 */
	protected TabPanel createConfigurationTabPanel()
	{
		final TabPanel configPanel = new TabPanel();
		
		// Create an Attribute editor and add to a Properties tab.
		m_AttributesEditor = new AttributesEditor();
		TabItem propertiesTab = new TabItem(ClientUtil.CLIENT_CONSTANTS.properties());   
		propertiesTab.addStyleName("x-border-layout-ct");
		propertiesTab.add(m_AttributesEditor); 
	    configPanel.add(propertiesTab);
	    
	    // Add controls for the time series attributes, if any.
		if (m_TimeSeriesView.hasAttributes() == true)
		{
			List<String> attributeNames = m_TimeSeriesView.getAttributeNames();		
			for (String attributeName : attributeNames)
			{
				// NB. The attribute values are only populated once the source name has been set.
				m_AttributesEditor.addAttribute(attributeName, new ArrayList<String>());
			}
		}
	    
	    // Add the metric control.
	    List<String> metrics = m_TimeSeriesView.getMetrics();
	    m_AttributesEditor.addAttribute("metric", metrics);
	    
	    m_AttributesEditor.addListener(Events.Submit, new Listener<AttributesEditorEvent>(){

			@Override
            public void handleEvent(AttributesEditorEvent aee)
            {
				m_FeaturesPanel.getSelectionModel().deselectAll();
				
				String metric = m_AttributesEditor.getSelectedAttribute("metric");
				setMetric(metric, false, false);
				
				if (m_TimeSeriesView.hasAttributes() == true)
				{
					List<String> attributeNames = m_TimeSeriesView.getAttributeNames();		
					for (String attributeName : attributeNames)
					{
						setAttributeValue(attributeName,
								m_AttributesEditor.getSelectedAttribute(attributeName), false);
					}
				}
				
				loadChart();
            }
	    });
	    
	    // Create the features list and add to a Features tab.
	    m_FeaturesPanel = createFeaturesListComponents();
	    TabItem featuresTab = new TabItem(ClientUtil.CLIENT_CONSTANTS.features());     
	    featuresTab.add(m_FeaturesPanel);   
	    configPanel.add(featuresTab);
	    
	    // Listen for select events to set the size of widget in the selected tab.
		Listener<ComponentEvent> resizeListener = new Listener<ComponentEvent>(){
			
			public void handleEvent(ComponentEvent be)
            {
				
				TabItem selectedTab = configPanel.getSelectedItem();
				if (selectedTab != null)
				{		  
				    BoxComponent widget = (BoxComponent)(selectedTab.getItem(0));
					widget.setSize(configPanel.getWidth(), configPanel.getHeight(true) - 30);
					selectedTab.layout(true);
				}
            }
		};	
		configPanel.addListener(Events.Select, resizeListener);
		
		return configPanel;
	}
	
	
	/**
	 * Creates and adds the components for the time series features list.
	 * @return EvidenceViewPanel for displaying the time series features.
	 */
	protected EvidenceViewPanel createFeaturesListComponents()
	{
		EvidenceView featuresView = m_TimeSeriesView.getFeaturesView();
		EvidenceViewPanel featuresPanel = new EvidenceViewPanel(featuresView);
		featuresPanel.setHeaderVisible(false);
		featuresPanel.setSize(650, 250);
		featuresPanel.setBodyBorder(false);
		
		featuresPanel.getSelectionModel().addSelectionChangedListener(
     			new SelectionChangedListener<EvidenceModel>(){

			@Override
            public void selectionChanged(SelectionChangedEvent<EvidenceModel> se)
            {
				EvidenceModel selectedRow = se.getSelectedItem();
				if (selectedRow != null)
				{
					// NB. the features list may not have columns for all of 
					// the attributes of the time series.
					loadFeatureId(selectedRow.getId(), false);
				}  
            }
     		
     	});
     	
     	// Listen for events to open notification, time series and causality views.
		Listener<RequestViewEvent<EvidenceModel>> rveListener = new Listener<RequestViewEvent<EvidenceModel>>(){

            public void handleEvent(RequestViewEvent<EvidenceModel> rve)
            {
            	// Propagate the event.
        		fireEvent(rve.getType(), rve);
            }
			
		};
		
		featuresPanel.addListener(GXTEvents.OpenNotificationViewClick, rveListener);
		featuresPanel.addListener(GXTEvents.OpenTimeSeriesViewClick, rveListener);
		featuresPanel.addListener(GXTEvents.OpenCausalityViewClick, rveListener);
	    
	    return featuresPanel;
	}
	

	/**
	 * Returns the user interface Widget sub-class itself.
	 * @return the Widget which is added in the user interface.
	 */
	public Widget getWidget()
	{
		return this;
	}
	
	
	/**
	 * Returns the View displayed in the Widget.
	 * @return the view displayed in the Widget.
	 */
	public TimeSeriesView getView()
	{
		return m_TimeSeriesView;
	}
	
	
	/**
	 * Returns the name of the source (server) whose data will be viewed in the
	 * widget.
	 * @return the name of the source whose data is currently being viewed,
	 * or <code>null</code> if no specific source is currently selected
	 * i.e. 'All Sources' is selected.
	 */
	public String getSource()
	{
		return m_Source;
	}
	
	
	/**
	 * Sets the name of the source (server) whose data will be viewed in the widget.
	 * @param source the name of the source (server) whose data is to be
	 * displayed. If <code>null</code>, data from all sources will be displayed.
	 */
	public void setSource(String source)
	{	
		if  (( (source != null) && (source.equals(m_Source) == false) ) ||
				(source == null && m_Source != null) )
		{
			m_Source = source;
			
			// Clear the current attribute values since they may not be available
			// for the new source.
			if (m_TimeSeriesView.hasAttributes())
			{
				m_Attributes.clear();
				
				List<String> attributeNames = m_TimeSeriesView.getAttributeNames();
				for (String attributeName : attributeNames)
				{
					// Re-populate the attribute value drop-down for the new Source value.
					populateAttributeValues(attributeName, m_Source);
				}
				
			}
			
			// Also clear the time marker, if any, on the chart.
			m_ChartWidget.clearTimeMarker();
			
			m_FeaturesPanel.getLoader().setSource(m_Source);
		}
		else
		{
			// Populate the attribute values drop-down if they have yet to be populated.
			if (m_TimeSeriesView.hasAttributes())
			{
				List<String> attributeNames = m_TimeSeriesView.getAttributeNames();
				for (String attributeName : attributeNames)
				{
					List<String> currentValues = m_AttributesEditor.getAttributeValues(attributeName);
					if (currentValues.size() == 0)
					{
						populateAttributeValues(attributeName, m_Source);
					}
				}
				
			}
		}
	}


	/**
	 * Loads the data in the chart and feature list according to their current configuration.
	 */
	public void load()
	{
		loadChart();
		m_FeaturesPanel.load();
	}
	

    @Override
    public void setLinkToDataTypes(List<DataSourceType> dataSourceTypes)
    {
	    // Don't provide a link to the same view type.
    	ArrayList<DataSourceType> otherTypes = new ArrayList<DataSourceType>();
    	for (DataSourceType dataType : dataSourceTypes)
    	{
    		if (dataType.getName().equals(m_TimeSeriesView.getDataType()) == false)
    		{
    			otherTypes.add(dataType);
    		}
    	}
    	
	    m_ChartWidget.setLinkToDataTypes(otherTypes);
	    m_FeaturesPanel.setLinkToDataTypes(otherTypes);
    }


	/**
	 * Loads the data in the chart according to its current configuration.
	 */
	protected void loadChart()
	{	
		boolean loadChart = false;
		boolean aggregationSupported = m_TimeSeriesView.isAggregationSupported();
		if (aggregationSupported == true)
		{
			// On initial load, if no metric or attributes set, set to first metric
			// and load aggregate view if aggregation is supported.
			if (m_Metric == null)
			{
				List<String> metrics = m_TimeSeriesView.getMetrics();
				if (metrics != null && metrics.size() > 0)
				{
					m_Metric = metrics.get(0); 
				}
			}
			
			if (m_TimeSeriesView.hasAttributes())
			{
				if (m_Attributes.size() == 0 && aggregationSupported == true)
				{
					// Add 'all' values for each of the view's attributes.
					List<String> attributeNames = m_TimeSeriesView.getAttributeNames();
					for (String attributeName : attributeNames)
					{
						m_Attributes.add(new Attribute(attributeName, null));
					}
				}
			}
			
			loadChart = true;
		}
		else
		{
			// For views which do not support aggregation, check that
			// the metric and all attributes are set.
			if (m_Metric != null)
			{
				loadChart = true;
				if (m_TimeSeriesView.hasAttributes())
				{
					List<String> attributeNames = m_TimeSeriesView.getAttributeNames();
					for (String attributeName : attributeNames)
					{
						if (getAttributeValue(attributeName) == null)
						{
							loadChart = false;
							break;
						}
					}
				}
			}
		}
		
		if (loadChart == true)
		{	
			// Update the metric, source and attribute labels on the chart.
			m_MetricLabel.setText(m_Metric);
			if (m_Source != null)
			{
				m_SourceLabel.setText(m_Source);
			}
			else
			{
				m_SourceLabel.setText(ClientUtil.CLIENT_CONSTANTS.allSources());
			}
			
			setMetricComboSelection(m_Metric);	
			
			if (m_Attributes != null && m_Attributes.size() > 0)
			{
				Attribute attribute;
				String attributeName;
				String attributeValue;
				for (int i = 0; i < m_Attributes.size(); i++)
				{
					attribute = m_Attributes.get(i);
					attributeName = attribute.getAttributeName();
					attributeValue = attribute.getAttributeValue();

					setAttributeValueSelection(attributeName, attributeValue);
					if (attributeValue != null)
					{
						m_AttributeLabels.get(i).setText(attributeName+": " + attributeValue);
					}
					else
					{
						m_AttributeLabels.get(i).setText(attributeName+": " + 
								ClientUtil.CLIENT_CONSTANTS.optionAll());
					}
				}
			}
			
			m_TopCont.layout(true);
			
			
			// Load the chart.
			String dataType = m_TimeSeriesView.getDataType();
			m_ChartWidget.removeAllTimeSeries();
			TimeSeriesConfig config = new TimeSeriesConfig(
					dataType, m_Metric, m_Source, m_Attributes);
			
			GWT.log("TimeSeriesViewWidget.load() for config " + config, null);
			
			m_ChartWidget.addTimeSeries(config);
			m_ChartWidget.load(config);
		}
		else
		{
			// Chart is not being loaded - clear the chart and labels.
			m_ChartWidget.removeAllTimeSeries();
			m_ChartTools.setEnabled(false);
			m_MetricLabel.setText((ClientUtil.CLIENT_CONSTANTS.timeSeriesChartSelectToLoad(
	        		m_TimeSeriesView.getDataType())));
			m_SourceLabel.setText("");
			
			if (m_AttributeLabels != null)
	        {
	        	for (Label attributeLabel : m_AttributeLabels)
	        	{
	        		attributeLabel.setText("");
	        	}
	        }
			
			m_TopCont.layout(true);
		}
	}
	
	
	/**
	 * Loads time series data into the widget for the given time.
	 * @param date date/time of time series data to load.
	 */
	public void loadAtTime(Date time)
	{
		loadAtTime(time, true);
	}
	
	
	/**
	 * Loads time series data into the widget for the given time.
	 * @param date date/time of time series data to load.
	 * @param loadFeatureList flag indicating whether to reload the feature list
	 * 		as well as the chart.
	 */
	protected void loadAtTime(Date time, boolean loadFeatureList)
	{
		if (time != null)
		{
			// Load 30 mins before/after the supplied time.
			DateWrapper startTime = new DateWrapper(time);
			startTime = startTime.addMinutes(-30);
			DateWrapper endTime = new DateWrapper(time);
			endTime = endTime.addMinutes(30);
			
			m_ChartWidget.setDateRange(startTime.asDate(), endTime.asDate());
			
			m_ChartWidget.setTimeMarker(time);
		}
		
		loadChart();
		if (loadFeatureList == true)
		{
			m_FeaturesPanel.loadAtTime(time);
		}
	}
	
	
	/**
	 * Loads the time series data for the feature with the specified id.
	 * Refreshes the chart with the feature, and loads the list so
	 * that the specified feature is the top row in the grid.
	 * @param evidenceId id of the time series feature to load.
	 */
	public void loadFeatureId(int evidenceId)
	{
		/*
		final int featureId = evidenceId;
		m_FeaturesPanel.addLoadListener(new LoadListener(){

            @Override
            public void loaderLoad(LoadEvent le)
            {
            	m_FeaturesPanel.setSelectedEvidence(featureId);
            	m_FeaturesPanel.removeLoadListener(this);
            }
			
		});
		
		m_FeaturesPanel.loadAtId(evidenceId);
		*/
		
		loadFeatureId(evidenceId, true);
	}
	
	
	/**
	 * Loads the time series data for the feature with the specified id.
	 * Refreshes the chart to display the feature, and optionally loads the 
	 * list so that the specified feature is the top row in the grid.
	 * @param evidenceId id of the time series feature to load.
	 * @param loadFeatureList flag indicating whether to reload the feature list
	 * 		as well as the chart.
	 */
	protected void loadFeatureId(final int evidenceId, boolean loadFeatureList)
	{
		if (loadFeatureList == true)
		{
			m_FeaturesPanel.addLoadListener(new LoadListener(){

	            @Override
	            public void loaderLoad(LoadEvent le)
	            {
	            	GridSelectionModel<EvidenceModel> gridSelectionModel = 
	            		m_FeaturesPanel.getSelectionModel();
	            	gridSelectionModel.setFiresEvents(false);
	            	m_FeaturesPanel.setSelectedEvidence(evidenceId);
	            	gridSelectionModel.setFiresEvents(true);
	            	
	            	m_FeaturesPanel.removeLoadListener(this);
	            }
				
			});
			
			m_FeaturesPanel.loadAtId(evidenceId);
		}
		
		
		// Need to obtain the TimeSeriesConfig for this evidence id. 
		// NB. the features list may not have columns for all of the attributes
		// of the time series.
		ApplicationResponseHandler<EvidenceModel> callback = 
			new ApplicationResponseHandler<EvidenceModel>(){

				@Override
                public void uponSuccess(EvidenceModel feature)
                {
					if (feature != null)
					{
						// Set the source for the chart, but do not want to change the 
						// setting on the list (for case when all sources is selected).
						String featuresSource = m_FeaturesPanel.getLoader().getSource();
						setSource(feature.getSource());
						m_FeaturesPanel.getLoader().setSource(featuresSource);
						
						String metric = feature.get(METRIC);
						setMetric(metric, false, false);
						
						if (m_TimeSeriesView.hasAttributes())
						{
							List<String> attributeNames = m_TimeSeriesView.getAttributeNames();
							String attributeValue;
							for (String attributeName : attributeNames)
							{
								attributeValue = feature.get(attributeName);
								setAttributeValue(attributeName, attributeValue, false);
							}					
						}
						
						loadAtTime(feature.getTime(TimeFrame.SECOND), false);						
					}
                }

				@Override
                public void uponFailure(Throwable caught)
                {
					GWT.log("Error loading time series feature: ", caught);
					MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
							ClientUtil.CLIENT_CONSTANTS.errorLoadingEvidenceData(), null);
                }
			
		};
		
		m_EvidenceQueryService.getEvidenceSingle(evidenceId, callback);
	}
	
	
	/**
	 * Refreshes the time series data for the current configuration of source,
	 * metric and attribute, loading up the most recent data for the
	 * currently configured time span.
	 */
	public void refresh()
	{
		String dataType = m_TimeSeriesView.getDataType();
		TimeSeriesConfig config = new TimeSeriesConfig(
				dataType, m_Metric, m_Source, m_Attributes);
		
		// Obtain the time of the most recent data for this time series,
		// then load the data for the current time span in the chart.
		m_QueryService.getLatestTime(config, new ApplicationResponseHandler<Date>(){

			@Override
            public void uponFailure(Throwable problem)
            {
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
		        		ClientUtil.CLIENT_CONSTANTS.errorNoServerResponse(), null);
            }

			@Override
            public void uponSuccess(Date latestTime)
            {
	            if (latestTime != null)
	            {
		            Date startTime = m_ChartWidget.getStartTime();
		            Date endTime = m_ChartWidget.getEndTime();
		            
		            // Default time span of 1 hour.
		            long timeSpan = 60 * 60 * 1000;
		            if (startTime != null && endTime != null)
		            {
		            	timeSpan = endTime.getTime() - startTime.getTime();         	
		            }
		            
		            Date newStart = new Date(latestTime.getTime()-timeSpan);
	    			m_ChartWidget.setDateRange(newStart, latestTime); 
	    			loadChart();
	            }    
            }
		
		});
	}
	
	
	/**
	 * Returns the attributes that are currently set in the time series widget.
	 * @return the list of attributes, or <code>null</code> if the time series view
	 * 	has no attributes e.g. IPC data.
	 */
	public List<Attribute> getAttributes()
	{
		return m_Attributes;
	}
	
	
	/**
	 * Returns the currently selected value for the attribute with the given name.
	 * @return the current attribute value, which may be <code>null</code> to 
	 * indicate 'all values'.
	 */
	public String getAttributeValue(String attributeName)
	{		
		String attributeValue = null;
		
		Attribute attribute = getAttribute(attributeName);
		if (attribute != null)
		{
			attributeValue = attribute.getAttributeValue();
		}
		
		return attributeValue;
	}
	
	
	/**
	 * Returns the currently stored attribute with the given name.
	 * @return the stored attribute, or <code>null</code> if there is no
	 * 		attribute set with the specified name.
	 */
	protected Attribute getAttribute(String attributeName)
	{
		Attribute attribute = null;
		
		if (m_Attributes != null)
		{
			for (Attribute attr : m_Attributes)
			{
				if (attr.getAttributeName().equals(attributeName))
				{
					attribute = attr;
					break;
				}
			}
		}
		
		return attribute;
	}
	
	
	/**
	 * Sets the value of the attribute with the specified name, and reloads the 
	 * data in the widget.
	 * @param attributeName name of the attribute.
	 * @param attributeValue attribute value, where <code>null</code> should be
	 * 		passed to load data across all values.
	 */
	public void setAttributeValue(String attributeName, String attributeValue)
	{
		setAttributeValue(attributeName, attributeValue, true);
	}
	
	
	/**
	 * Sets the attribute value, and reloads the data in the widget, if desired.
	 * @param attributeName name of the attribute.
	 * @param attributeValue attribute value, where <code>null</code> should be
	 * 		passed to load data across all values.
	 * @param reload <code>true</code> to reload the data in the chart, <code>false</code> otherwise.
	 */
	protected void setAttributeValue(String attributeName, String attributeValue, boolean reload)
	{
		if (m_Attributes != null)
		{			
			Attribute storedAttribute = getAttribute(attributeName);
			if (storedAttribute == null)
			{
				m_Attributes.add(new Attribute(attributeName, attributeValue));
				if (reload == true)
				{
					loadChart();
				}
			}
			else
			{
				// Check to see if this is a change to the current setting.
				String currentValue = storedAttribute.getAttributeValue();
				
				if ( (attributeValue != null &&  attributeValue.equals(currentValue) == true) ||
						(attributeValue == null && currentValue == null) )
				{
					// No change to current setting.
					return;
				}
				
				storedAttribute.setAttributeValue(attributeValue);
				if (reload == true)
				{
					loadChart();
				}
			}
			
		}
	}
	
	
	/**
	 * Returns the currently selected metric.
	 * @return the selected metric.
	 */
	public String getMetric()
	{	
		return m_Metric;
	}
	
	
	/**
	 * Sets the name of the metric, and reloads all the data in the widget.
	 * @param metric the name of the usage metric to display.
	 */
	public void setMetric(String metric)
	{
		setMetric(metric, true, true);
	}
	
	
	/**
	 * Sets the name of the metric, and reloads the usage data in the window if desired.
	 * @param metric the name of the usage metric to display.
	 * @param reloadChart <code>true</code> to reload the chart, 
	 * 		<code>false</code> otherwise.
	 * @param reloadFeatures <code>true</code> to reload the features list,
	 * 	 	<code>false</code> otherwise.
	 */
	protected void setMetric(String metric, boolean reloadChart, boolean reloadFeatures)
	{
		if (metric != null && metric.equals(m_Metric) == false)
		{
			m_Metric = metric;
			if (reloadChart == true)
			{
				loadChart();
			}
			if (reloadFeatures == true)
			{
				m_FeaturesPanel.load();
			}
		}
	}
	
	
	/**
	 * Populates the combo box in the Attributes Editor for the attribute with 
	 * the specified name with the list of values for the given source (server).
	 * @param attributeName attribute name for which to obtain the values.
	 * @param source optional source (server) for which to obtain the attribute values.
	 */
	protected void populateAttributeValues(String attributeName, String source)
	{
		final String attrName = attributeName;
		m_QueryService.getAttributeValues(m_TimeSeriesView.getDataType(), 
				attributeName, source, new ApplicationResponseHandler<List<String>>(){

	        @Override
            public void uponFailure(Throwable caught)
	        {	
	        	 MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
			        		ClientUtil.CLIENT_CONSTANTS.errorNoServerResponse(), null);
	        }


	        @Override
            public void uponSuccess(List<String> attributeValues)
	        {
	        	m_AttributesEditor.setAttributeValues(attrName, attributeValues);
	        	String currentValue = getAttributeValue(attrName);
	        	if (currentValue != null || m_TimeSeriesView.isAggregationSupported())
	        	{
	        		// Set the Value ComboBox in case when the window is first
	        		// loaded this call completes AFTER the data is loaded.
	        		setAttributeValueSelection(attrName, currentValue);
	        	}
	        }
        });
	}
	
	
	/**
	 * Sets the Metric Combo Box to the selected value. Note that this simply
	 * updates the Combo Box field, and does not update the chart.
	 * @param metric value to set.
	 */
	protected void setMetricComboSelection(String metric)
	{
		m_AttributesEditor.setSelectedAttribute("metric", metric);
	}
	
	
	/**
	 * Sets the selected value of the combo box in the Attributes Editor for the 
	 * attribute with the specified name. Note that this simply updates the Combo 
	 * Box field, and does not update the chart.
	 * @param attributeName the name of the attribute whose selected value is being set.
	 * @param attributeValue the value of the attribute to select.
	 */
	protected void setAttributeValueSelection(String attributeName, String attributeValue)
	{
		m_AttributesEditor.setSelectedAttribute(attributeName, attributeValue);
	}
	
	
	/**
	 * Overrides setSize(int, int) to set the width of the widget components to the 
	 * specified width. This method fires the <i>Resize</i> event.
	 * @param width the new width to set
	 * @param height the new height to set
	 */
    @Override
    public void setSize(int width, int height)
    {
	    super.setSize(width, height);

	    m_ChartContainer.setWidth(width);
	    m_ChartWidget.setChartSize(width, m_ChartContainer.getHeight() - m_TopCont.getHeight());
	    
	    // Necessary to run in deferred command for IE.
	    Scheduler.get().scheduleDeferred(new ScheduledCommand()
		{
			@Override
			public void execute()
			{
				TabItem selectedTab = m_ConfigTabPanel.getSelectedItem();
				BoxComponent widget = (BoxComponent)(selectedTab.getItem(0));
				widget.setSize(getWidth(), m_ConfigTabPanel.getHeight(true) - 30);
			}
		});
    }
}
