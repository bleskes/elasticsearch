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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.extjs.gxt.ui.client.GXT;
import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.event.*;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.util.DateWrapper;
import com.extjs.gxt.ui.client.util.IconHelper;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.util.Padding;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.Label;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.SplitBar;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.ComboBox;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayout;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayoutData;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout;
import com.extjs.gxt.ui.client.widget.layout.BoxLayout.BoxLayoutPack;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayout.HBoxLayoutAlign;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout.VBoxLayoutAlign;
import com.extjs.gxt.ui.client.widget.toolbar.FillToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.LabelToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.SeparatorToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Widget;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.client.ClientUtil;
import com.prelert.client.chart.ChartToolBar;
import com.prelert.client.chart.TimeSeriesChartWidget;
import com.prelert.client.chart.TimeSeriesGChartWidget;
import com.prelert.client.event.GXTEvents;
import com.prelert.client.event.RequestViewEvent;
import com.prelert.client.gxt.SourceViewWidget;
import com.prelert.client.list.EvidenceViewPanel;
import com.prelert.data.Attribute;
import com.prelert.data.EvidenceView;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.TimeSeriesDataPoint;
import com.prelert.data.TimeSeriesView;
import com.prelert.data.Tool;
import com.prelert.data.gxt.AttributeModel;
import com.prelert.data.gxt.EvidenceModel;
import com.prelert.service.AsyncServiceLocator;
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
	
	private TimeSeriesView 			m_TimeSeriesView;
	private EvidenceView			m_FeaturesView;
	
	private String					m_Source;
	private List<Attribute>			m_Attributes;
	private String					m_Metric;
	
	private ComboBox<BaseModelData> m_MetricComboBox;
	private HashMap<String, ComboBox<AttributeModel>> m_AttrComboBoxes;

	private SelectionChangedListener<BaseModelData>		m_MetricComboListener;
	private SelectionChangedListener<AttributeModel>	m_AttrValueComboListener;
	
	private ContentPanel			m_ChartContainer;
	private TimeSeriesChartWidget	m_ChartWidget;
	private Label					m_ChartTitleLabel;
	private Label 					m_ChartSubtitleLabel;
	private ToolBar					m_ChartToolBar;
	protected Button 				m_RefreshBtn;
	
	private EvidenceViewPanel		m_FeaturesPanel;
	private Listener<SplitBarEvent>	m_SplitBarListener;
	

	/**
	 * Creates a new widget for displaying a time series view.
	 * @param timeSeriesView the view to display in the widget.
	 */
	public TimeSeriesViewWidget(TimeSeriesView timeSeriesView, EvidenceView featuresView)
	{
		m_TimeSeriesView = timeSeriesView;
		m_FeaturesView = featuresView;
		
		m_QueryService = AsyncServiceLocator.getInstance().getTimeSeriesGXTQueryService();
		
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
		
		// Create the toolbar holding the attribute and metric controls.
		m_ChartToolBar = createToolBar();

		m_ChartContainer.setTopComponent(m_ChartToolBar);
		
        VBoxLayout layout = new VBoxLayout(); 
        layout.setVBoxLayoutAlign(VBoxLayoutAlign.STRETCH);   
        m_ChartContainer.setLayout(layout);  
        m_ChartContainer.setSize(750, 500);
  
        
        LayoutContainer labelPanel = new LayoutContainer();
        VBoxLayout labelPanelLayout = new VBoxLayout();
        labelPanelLayout.setVBoxLayoutAlign(VBoxLayoutAlign.LEFT);
        labelPanel.setLayout(labelPanelLayout);  
        labelPanel.setHeight(32);
        
        m_ChartTitleLabel = new Label("Time series chart");
        m_ChartTitleLabel.addStyleName("prl-timeSeriesChart-title");
        
        m_ChartSubtitleLabel = new Label("");
        m_ChartSubtitleLabel.addStyleName("prl-timeSeriesChart-subtitle");
        
        labelPanel.add(m_ChartTitleLabel);
        labelPanel.add(m_ChartSubtitleLabel);
        
        
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
        
        
        // Configure context menu tools for launching other view types.
        List<Tool> viewTools = m_TimeSeriesView.getContextMenuItems();
        m_ChartWidget.setViewTools(viewTools);
        
        // Add the zooming and panning tools.
        final ChartToolBar<TimeSeriesDataPoint> zoomPanTools = 
        	new ChartToolBar<TimeSeriesDataPoint>(m_ChartWidget, true);    
       
        HBoxLayout zoomPanelLayout = new HBoxLayout();
        zoomPanelLayout.setHBoxLayoutAlign(HBoxLayoutAlign.MIDDLE);     
        zoomPanelLayout.setPadding(new Padding(0, 3, 0, 3));    
        zoomPanelLayout.setPack(BoxLayoutPack.END);   
        LayoutContainer zoomPanel = new LayoutContainer(zoomPanelLayout);
        zoomPanel.setSize(240, 25);
        zoomPanel.add(zoomPanTools);
        
        
        // Create a panel at the top to hold the chart title and subtitle,
        // and the zoom in / zoom out controls.
        LayoutContainer topCont = new LayoutContainer();   
        topCont.setHeight(32);
        HBoxLayout hblayout = new HBoxLayout();   
        hblayout.setHBoxLayoutAlign(HBoxLayoutAlign.MIDDLE);   
        topCont.setLayout(hblayout);   
  
        HBoxLayoutData flex = new HBoxLayoutData(new Margins(0, 5, 0, 0));   
    	flex.setFlex(1);   
    	topCont.add(labelPanel, flex);   
        topCont.add(zoomPanel, flex);   
        
        m_ChartContainer.add(topCont);
        m_ChartContainer.add(m_ChartWidget.getChartWidget()); 
        
        LoadListener loadListener = new LoadListener()
		{
			@Override
            public void loaderBeforeLoad(LoadEvent le)
			{
				m_RefreshBtn.setIcon(IconHelper.createStyle("x-tbar-loading"));
				zoomPanTools.setEnabled(false);
				m_ChartToolBar.setEnabled(false);
				m_ChartContainer.mask(GXT.MESSAGES.loadMask_msg());
			}


			@Override
            public void loaderLoad(LoadEvent le)
			{
				m_RefreshBtn.setIcon(IconHelper.createStyle("x-tbar-refresh"));
				zoomPanTools.setEnabled(true);
				m_ChartToolBar.setEnabled(true);
				m_ChartContainer.unmask();
			}


			@Override
            public void loaderLoadException(LoadEvent le)
			{
				m_RefreshBtn.setIcon(IconHelper.createStyle("x-tbar-refresh"));
				zoomPanTools.setEnabled(true);
				m_ChartToolBar.setEnabled(true);
				m_ChartContainer.unmask();
			}
		};
		m_ChartWidget.addLoadListener(loadListener);
        
        m_FeaturesPanel = createFeaturesListComponents();
          
        BorderLayoutData centerData = new BorderLayoutData(LayoutRegion.CENTER);   
	    centerData.setMargins(new Margins(0));  
	    
	    BorderLayoutData southData = new BorderLayoutData(LayoutRegion.SOUTH, 300, 125, 525);   
	    southData.setSplit(true);   
	    southData.setCollapsible(true);   
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

				m_ChartWidget.setChartHeight(m_ChartContainer.getHeight() - 80);
			}
		};
	    
	    add(m_ChartContainer, centerData);   
	    add(m_FeaturesPanel, southData);
		
		addListener(Events.AfterLayout, new Listener<ContainerEvent<LayoutContainer, Component>>(){

			@Override
            public void handleEvent(ContainerEvent<LayoutContainer, Component> be)
            {
				SplitBar splitBar = m_FeaturesPanel.getData("splitBar");
		        if (splitBar != null)
		        {
		        	splitBar.addListener(Events.DragEnd, m_SplitBarListener);
		        	TimeSeriesViewWidget.this.removeListener(Events.AfterLayout, this);
		        }
            }
			
		});
     		
     	borderLayout.addListener(Events.Collapse, new Listener<BorderLayoutEvent>(){

			@Override
            public void handleEvent(BorderLayoutEvent be)
            {
				m_ChartWidget.setChartHeight(m_ChartContainer.getHeight() - 80);
				
				SplitBar splitBar = m_FeaturesPanel.getData("splitBar");
	            if (splitBar != null)
	            {
	            	splitBar.removeListener(Events.DragEnd, m_SplitBarListener);
	            }  
            }
			
		});
		
     	borderLayout.addListener(Events.Expand, new Listener<BorderLayoutEvent>(){

			@Override
            public void handleEvent(BorderLayoutEvent be)
            {
				m_ChartWidget.setChartHeight(m_ChartContainer.getHeight() - 80);
				
				SplitBar splitBar = m_FeaturesPanel.getData("splitBar");
	            if (splitBar != null)
	            {
	            	splitBar.addListener(Events.DragEnd, m_SplitBarListener);
	            }  
            }
			
		});

	}
	
	
	/**
	 * Creates the source, attribute and value controls.
	 */
	protected ToolBar createToolBar()
	{
		// Create the toolbar which will hold the controls for setting the
		// parameters of the chart i.e. attributes (if any) and metrics.
	//	GXT.useShims = true;
		ToolBar toolbar = new ToolBar();
	//	toolbar.setShim(true);
		toolbar.setSpacing(1);
		
		// Create a Metric-type (e.g. Total, Pending) drop-down.
		m_MetricComboBox = new ComboBox<BaseModelData>();
		m_MetricComboBox.setWidth(180);
		m_MetricComboBox.setListStyle("prl-combo-list");
		m_MetricComboBox.setStore(getMetricList());
		m_Metric = getMetricList().getAt(0).get("metric");
		m_MetricComboBox.setEmptyText("Select metric...");
		m_MetricComboBox.setEditable(false);
		m_MetricComboBox.setDisplayField("metric");
		m_MetricComboBox.setTypeAhead(true);
		m_MetricComboBox.setTriggerAction(TriggerAction.ALL);
		
		m_MetricComboListener = new SelectionChangedListener<BaseModelData>() {
		      @Override
            public void selectionChanged(SelectionChangedEvent<BaseModelData> se) 
		      {
		    	  BaseModelData selectedMetric = se.getSelectedItem();
		    	  String metric = selectedMetric.get("metric");
		    	  setMetric(metric, true, false);
		      }
		};
		m_MetricComboBox.addSelectionChangedListener(m_MetricComboListener);
		
		// Specify a custom template for the drop-down list items which uses
		// Quicktip tooltips, allowing the use to read long items.
		StringBuilder toolTip = new StringBuilder();
        toolTip.append("<tpl for=\".\"><div class=\"prl-combo-list-item\" qtip=\"{metric");
        toolTip.append("}\" qtitle=\"\">{metric}</div></tpl>");
        m_MetricComboBox.setTemplate(toolTip.toString());
		
		m_RefreshBtn = new Button();
		m_RefreshBtn.setIcon(GXT.IMAGES.paging_toolbar_refresh());
		m_RefreshBtn.setToolTip(GXT.MESSAGES.pagingToolBar_refreshText());
		m_RefreshBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
			{
				refresh();
			}
		});
		
		toolbar.add(new LabelToolItem(ClientUtil.CLIENT_CONSTANTS.fieldShow()));
		toolbar.add(m_MetricComboBox);
		toolbar.add(new SeparatorToolItem());
		toolbar.add(m_RefreshBtn);
		toolbar.add(new FillToolItem());
		
		// Create attribute name and value drop-downs if necessary.
		if (m_TimeSeriesView.hasAttributes() == true)
		{
			m_Attributes = new ArrayList<Attribute>();
			m_AttrComboBoxes = new HashMap<String, ComboBox<AttributeModel>>();
			
			m_AttrValueComboListener = new SelectionChangedListener<AttributeModel>() {
				@Override
				public void selectionChanged(SelectionChangedEvent<AttributeModel> se) 
				{    	  
					AttributeModel selectedAttribute = se.getSelectedItem();	
					String attributeName = selectedAttribute.getAttributeName();
					ComboBox<AttributeModel> combo = m_AttrComboBoxes.get(attributeName);
			    	  
					if (combo.getStore().indexOf(selectedAttribute) == 0)
					{
						setAttributeValue(attributeName, null);  		  
					}
					else
					{
						setAttributeValue(attributeName, selectedAttribute.getAttributeValue());
					} 
				}
			};
			
			
			List<String> attributeNames = m_TimeSeriesView.getAttributeNames();
			ComboBox<AttributeModel> attributeValueCombo;
			for (String attributeName : attributeNames)
			{			
				// NB. The attribute values are only populated once the source name has been set.
				m_Attributes.add(new Attribute(attributeName, null));
				
				attributeValueCombo = new ComboBox<AttributeModel>();
				attributeValueCombo.setWidth(150);
				attributeValueCombo.setListStyle("prl-combo-list");
				attributeValueCombo.setEmptyText(m_TimeSeriesView.getSelectUserText());
				attributeValueCombo.setDisplayField("attributeValue");
				attributeValueCombo.setTypeAhead(true);
				attributeValueCombo.setTriggerAction(TriggerAction.ALL);
				attributeValueCombo.setStore(new ListStore<AttributeModel>());
				
				// Specify a custom template for the drop-down list items which uses
				// Quicktip tooltips, allowing the use to read long items.
				toolTip = new StringBuilder();
		        toolTip.append("<tpl for=\".\"><div class=\"prl-combo-list-item\" qtip=\"{attributeValue");
		        toolTip.append("}\" qtitle=\"\">{attributeValue}</div></tpl>");
		        attributeValueCombo.setTemplate(toolTip.toString());
		        
				attributeValueCombo.addSelectionChangedListener(m_AttrValueComboListener);
				m_AttrComboBoxes.put(attributeName, attributeValueCombo);
				
				if (attributeNames.indexOf(attributeName) > 0)
				{
					toolbar.add(new SeparatorToolItem());
				}
				
				toolbar.add(new LabelToolItem(attributeName + ": "));
				toolbar.add(attributeValueCombo);
			}

		}
		
		
		return toolbar;
	}
	
	
	/**
	 * Creates and adds the components for the time series features list.
	 * @return EvidenceViewPanel for displaying the time series features.
	 */
	protected EvidenceViewPanel createFeaturesListComponents()
	{
		final EvidenceViewPanel featuresPanel = new EvidenceViewPanel(m_FeaturesView);
		featuresPanel.setHeaderVisible(true);
		featuresPanel.setHeading(ClientUtil.CLIENT_CONSTANTS.timeSeriesFeaturesHeading(
     			m_FeaturesView.getDataType()));
		featuresPanel.setSize(650, 520);
		featuresPanel.setTitleCollapse(false);
		featuresPanel.getSelectionModel().addSelectionChangedListener(
     			new SelectionChangedListener<EvidenceModel>(){

			@Override
            public void selectionChanged(SelectionChangedEvent<EvidenceModel> se)
            {
				EvidenceModel selectedRow = se.getSelectedItem();
				if (selectedRow != null)
				{
					// Set the source for the chart, but do not want to change the 
					// setting on the list (for case when all sources is selected).
					String featuresSource = featuresPanel.getSource();
					setSource(selectedRow.getSource());
					featuresPanel.setSource(featuresSource);
					
					String metric = selectedRow.get("metric");
					setMetric(metric, false, false);
					
					if (m_TimeSeriesView.hasAttributes())
					{
						List<String> attributeNames = m_TimeSeriesView.getAttributeNames();
						String attributeValue;
						for (String attributeName : attributeNames)
						{
							attributeValue = selectedRow.get(attributeName);
							setAttributeValue(attributeName, attributeValue, false);
						}
						
					}
					
					Date featureTime = selectedRow.getTime(m_FeaturesView.getTimeFrame());
					loadAtTime(featureTime, false);
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
	 * Returns the list of metric names.
	 * @return store of usage metrics for this window.
	 */
	protected ListStore<BaseModelData> getMetricList()
	{
		ListStore<BaseModelData> metrics = new ListStore<BaseModelData>();
		
		List<String> metricNames = m_TimeSeriesView.getMetrics();
		
		for (String metricName : metricNames)
		{
			BaseModelData metric = new BaseModelData();
			metric.set("metric", metricName);
			metrics.add(metric);
		}
		
		return metrics;
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
				List<String> attributeNames = m_TimeSeriesView.getAttributeNames();
				for (String attributeName : attributeNames)
				{
					setAttributeValue(attributeName, null, false);
					
					// Re-populate the attribute value drop-down for the new Source value.
					populateAttributeValues(attributeName, m_Source);
				}
				
			}
			
			// Also clear the time marker, if any, on the chart.
			m_ChartWidget.clearTimeMarker();
			
			m_FeaturesPanel.setSource(m_Source);
		}
		else
		{
			// Populate the attribute values drop-down if they have yet to be populated.
			if (m_TimeSeriesView.hasAttributes())
			{
				List<String> attributeNames = m_TimeSeriesView.getAttributeNames();
				ComboBox<AttributeModel> valueCombo;
				for (String attributeName : attributeNames)
				{
					valueCombo = m_AttrComboBoxes.get(attributeName);
					if (valueCombo.getStore().getCount() == 0)
					{
						populateAttributeValues(attributeName, m_Source);
					}
				}
				
			}
		}
	}


	/**
	 * Loads the data in the widget according to its current configuration.
	 */
	public void load()
	{
		load(true);
	}
	
	
	/**
	 * Loads the data in the widget according to its current configuration.
	 * @param loadFeatureList flag indicating whether to reload the feature list
	 * 		as well as the chart.
	 */
	protected void load(boolean loadFeatureList)
	{
		// Set the toolbar fields.
		setMetricComboSelection(m_Metric);	
		
		if (m_Attributes != null)
		{
			for (Attribute attribute : m_Attributes)
			{
				setAttributeValueSelection(attribute.getAttributeName(), 
						attribute.getAttributeValue());
			}
		}
		
		
		// Load the chart.
		String dataType = m_TimeSeriesView.getDataType();
		m_ChartWidget.removeAllTimeSeries();
		TimeSeriesConfig config = new TimeSeriesConfig(
				dataType, m_Metric, m_Source, m_Attributes);
		
		GWT.log("TimeSeriesViewWidget.load() for config " + config, null);
		
		m_ChartWidget.addTimeSeries(config);
		m_ChartWidget.load(config);
		
		// Set the title on the chart.
		m_ChartTitleLabel.setText(m_TimeSeriesView.getDataType());
		String subTitle = new String(m_Metric);
		subTitle += ", ";
		if (m_Source != null)
		{
			subTitle += m_Source;
		}
		else
		{
			subTitle += ClientUtil.CLIENT_CONSTANTS.allSources();
		}
		m_ChartSubtitleLabel.setText(subTitle);
		
		if (loadFeatureList == true)
		{
			m_FeaturesPanel.load();
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
		
		load(loadFeatureList);
	}
	
	
	/**
	 * Loads the time series data for the feature with the specified id.
	 * Refreshes the chart with the feature, and loads the list so
	 * that the specified feature is the top row in the grid.
	 * @param evidenceId id of the time series feature to load.
	 */
	public void loadFeatureId(int evidenceId)
	{
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
		            
		            long timeSpan = 7 * 24 * 60 * 60 * 1000;
		            if (startTime != null && endTime != null)
		            {
		            	timeSpan = endTime.getTime() - startTime.getTime();         	
		            }
		            
		            Date newStart = new Date(latestTime.getTime()-timeSpan);
	    			m_ChartWidget.setDateRange(newStart, latestTime); 
	    			load(false);
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
	 * @return the current attribute value, which may be <code>null</code>.
	 */
	public String getAttributeValue(String attributeName)
	{		
		String attributeValue = null;
		
		if (m_Attributes != null)
		{
			for (Attribute attribute : m_Attributes)
			{
				if (attribute.getAttributeName().equals(attributeName))
				{
					attributeValue = attribute.getAttributeValue();
					break;
				}
			}
		}
		
		return attributeValue;
	}
	
	
	/**
	 * Sets the attribute value, and reloads the usage data in the window.
	 * @param value attribute value, which may be <code>null</code>.
	 */
	public void setAttributeValue(String attributeName, String attributeValue)
	{
		setAttributeValue(attributeName, attributeValue, true);
	}
	
	
	/**
	 * Sets the attribute value, and reloads the usage data in the window. if desired.
	 * @param value attribute value, which may be <code>null</code>.
	 * @param reload <code>true</code> to reload the data in the chart, <code>false</code> otherwise.
	 */
	protected void setAttributeValue(String attributeName, String attributeValue, boolean reload)
	{
		// Three possibilities:
		// 1. Value is non-null and is in the value combo store.
		// 2. Value is null - load data from all attribute values.
		// 3. Value is non-null, but does not appear in the value combo - 
		// 		set to new value anyway as the combo's store may still to load.
		
		if (m_Attributes != null)
		{
			// Check to see if this is a change to the current setting.
			String currentValue = getAttributeValue(attributeName);
			
			if ( (attributeValue != null &&  attributeValue.equals(currentValue) == true) ||
					(attributeValue == null && currentValue == null) )
			{
				// No change to current setting.
				return;
			}
			
			for (Attribute attribute : m_Attributes)
			{
				if (attribute.getAttributeName().equals(attributeName))
				{
					attribute.setAttributeValue(attributeValue);
					
					if (reload == true)
					{
						load(false);
					}
					break;
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
			ListStore<BaseModelData> metricsStore = m_MetricComboBox.getStore();
			
			BaseModelData metricData;
			for (int i = 0; i < metricsStore.getCount(); i++)
			{
				metricData = metricsStore.getAt(i);
				if (metricData.get("metric").equals(metric))
				{
					m_Metric = metric;
					if (reloadChart == true)
					{
						load(reloadFeatures);
					}
					break;
				}
			}
		}
	}
	
	
	/**
	 * Clears the list of values in the attribute value drop-down, and adds a single
	 * 'All' items into the drop-down combo box.
	 */
	public void clearAttributeValues(String attributeName)
	{	
		if (m_Attributes != null)
		{
			ComboBox<AttributeModel> valueCombo = m_AttrComboBoxes.get(attributeName);
			if (valueCombo != null)
			{
				// Clear out the ComboBox and add an 'All' item at the top.
				valueCombo.clearSelections();
				ListStore<AttributeModel> valuesStore = valueCombo.getStore();
				valuesStore.removeAll();
				
				// Add an 'All' item at the top.
				AttributeModel allValuesData = new AttributeModel(
						attributeName, ClientUtil.CLIENT_CONSTANTS.optionAll());
				valuesStore.add(allValuesData);
				
				// Disable the SelectionChangedListener whilst setting the initial
				// value to ensure it does not trigger another query.
				valueCombo.disableEvents(true);
				valueCombo.setValue(allValuesData);  
				valueCombo.enableEvents(true);
			}
		}
	}
	
	
	/**
	 * Populates the attribute value combo box with the list of values for the
	 * supplied attribute name and source (server).
	 * @param attributeName attribute name for which to obtain the values.
	 * @param source optional source (server) for which to obtain the attribute values.
	 */
	public void populateAttributeValues(String attributeName, String source)
	{
		if (m_Attributes != null)
		{
			final String attrName = attributeName;
			final ComboBox<AttributeModel> valueCombo = m_AttrComboBoxes.get(attributeName);	
			valueCombo.disableEvents(true);
			
			m_QueryService.getAttributeValues(m_TimeSeriesView.getDataType(), 
					attributeName, source, new ApplicationResponseHandler<List<String>>(){
	
		        @Override
                public void uponFailure(Throwable caught)
		        {
		        	valueCombo.enableEvents(true);
		        	
		        	 MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
				        		ClientUtil.CLIENT_CONSTANTS.errorNoServerResponse(), null);
		        }
	
	
		        @Override
                public void uponSuccess(List<String> attributeValues)
		        {
		        	// Clear out the ComboBox and repopulate with the values for this source.
					clearAttributeValues(attrName);
					ListStore<AttributeModel> valuesStore = valueCombo.getStore();
		        	
					AttributeModel attribute;
		        	
		        	for (String attributeValue : attributeValues)
		        	{
		        		attribute = new AttributeModel(attrName, attributeValue);
		        		valuesStore.add(attribute);
		        	}
		        	
		        	String currentValue = getAttributeValue(attrName);
		        	if (currentValue != null)
		        	{
		        		// Set the Value ComboBox in case when the window is first
		        		// loaded this call completes AFTER the data is loaded.
		        		setAttributeValueSelection(attrName, currentValue);
		        	}
		        	
		        	valueCombo.enableEvents(true);
		        }
	        });
		
		}
	}
	
	
	/**
	 * Sets the Metric Combo Box to the selected value. Note that this simply
	 * updates the Combo Box field, and does not update the chart.
	 * @param metric value to set.
	 */
	protected void setMetricComboSelection(String metric)
	{
		ListStore<BaseModelData> metricsStore = m_MetricComboBox.getStore();
		if (metricsStore.getCount() > 0)
		{
			m_MetricComboBox.disableEvents(true);
			
			List<BaseModelData> selectedMetrics = m_MetricComboBox.getSelection();
			String currentlySelectedMetric = null;
			if (selectedMetrics.size() > 0)
			{
				BaseModelData selectedMetricData = selectedMetrics.get(0);
				if (m_MetricComboBox.getStore().indexOf(selectedMetricData) != 0)
		  	  	{
					currentlySelectedMetric = selectedMetricData.get("metric");
		  	  	}
			}
			
			if (metric.equals(currentlySelectedMetric) == false)
			{
				BaseModelData metricData;
				for (int i = 0; i < metricsStore.getCount(); i++)
				{
					metricData = metricsStore.getAt(i);
					if (metricData.get("metric").equals(metric))
					{
						m_MetricComboBox.setValue(metricData);
						break;
					}
				}
			}
			
			m_MetricComboBox.enableEvents(true);
		}
	}
	
	
	/**
	 * Sets the attribute value Combo Box to the selected value. Note that this simply
	 * updates the Combo Box field, and does not update the chart.
	 * @param attributeValue value to set.
	 */
	protected void setAttributeValueSelection(String attributeName, String attributeValue)
	{	
		if (m_Attributes != null)
		{
			ComboBox<AttributeModel> valueCombo = m_AttrComboBoxes.get(attributeName);
			ListStore<AttributeModel> valuesStore = valueCombo.getStore();	
			if (valuesStore.getCount() > 0)
			{	
				if (attributeValue == null)
				{
					valueCombo.disableEvents(true);
					valueCombo.setValue(valuesStore.getAt(0));
					valueCombo.enableEvents(true);
				}
				else
				{	
					for (AttributeModel attribute : valuesStore.getModels())
					{
						if (attribute.getAttributeValue().equals(attributeValue))
						{
							valueCombo.disableEvents(true);
							valueCombo.setValue(attribute);		
							valueCombo.enableEvents(true);
							break;
						}
					}
				}
				
			}

		}
	}

	
	/**
	 * Overrides setWidth(int) to set the width of the widget components to the 
	 * specified size. This method fires the <i>Resize</i> event.
	 * @param width the new width to set.
	 */
	@Override
    public void setWidth(int width)
	{
		super.setWidth(width);
		
		m_ChartContainer.setWidth(width);
		m_ChartWidget.setChartWidth(width);
		m_FeaturesPanel.setWidth(width);
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
	    m_ChartWidget.setChartSize(width, m_ChartContainer.getHeight() - 80);	

	    m_FeaturesPanel.setWidth(width);
    }
		
}
