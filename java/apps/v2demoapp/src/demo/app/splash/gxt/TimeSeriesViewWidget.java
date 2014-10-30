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
import java.util.List;

import com.extjs.gxt.ui.client.GXT;
import com.extjs.gxt.ui.client.Style;
import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.store.ListStore;
import com.extjs.gxt.ui.client.util.DateWrapper;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.util.Padding;
import com.extjs.gxt.ui.client.widget.Label;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.VerticalPanel;
import com.extjs.gxt.ui.client.widget.form.ComboBox;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayout;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayoutData;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout;
import com.extjs.gxt.ui.client.widget.layout.BoxLayout.BoxLayoutPack;
import com.extjs.gxt.ui.client.widget.layout.HBoxLayout.HBoxLayoutAlign;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout.VBoxLayoutAlign;
import com.extjs.gxt.ui.client.widget.toolbar.FillToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.LabelToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.Anchor;

import demo.app.client.ApplicationResponseHandler;
import demo.app.client.ClientUtil;
import demo.app.client.SourceViewWidget;
import demo.app.client.event.GXTEvents;
import demo.app.client.event.RequestViewEvent;
import demo.app.data.EvidenceView;
import demo.app.data.TimeSeriesConfig;
import demo.app.data.Tool;
import demo.app.data.UsageView;
import demo.app.data.gxt.EvidenceModel;
import demo.app.service.TimeSeriesGXTPagingServiceAsync;
import demo.app.splash.service.QueryServiceLocator;


/**
 * ViewWidget implementation for displaying a time series view. The widget
 * consists of a chart for displaying time series data and a toolbar for 
 * controlling the data displayed i.e. time series attributes and metric.
 *
 * @author Pete Harverson
 */
public class TimeSeriesViewWidget extends VerticalPanel implements SourceViewWidget
{
	private TimeSeriesGXTPagingServiceAsync	m_QueryService;
	
	private UsageView 				m_TimeSeriesView;
	private EvidenceView			m_FeaturesView;
	
	private String					m_Source;
	private String					m_AttributeName;
	private String					m_AttributeValue;
	private String					m_Metric;
	
	private ComboBox<BaseModelData> m_AttrNameComboBox;
	private ComboBox<BaseModelData> m_AttrValueComboBox;
	private ComboBox<BaseModelData> m_MetricComboBox;

	private SelectionChangedListener<BaseModelData>	m_MetricComboListener;
	private SelectionChangedListener<BaseModelData>	m_AttrNameComboListener;
	private SelectionChangedListener<BaseModelData>	m_AttrValueComboListener;
	
	private LayoutContainer			m_ChartContainer;
	private TimeSeriesChartWidget 	m_ChartWidget;
	private Label					m_ChartTitleLabel;
	private Label 					m_ChartSubtitleLabel;
	
	private EvidenceViewPanel		m_FeaturesPanel;
	

	/**
	 * Creates a new widget for displaying a time series view.
	 * @param timeSeriesView the view to display in the widget.
	 */
	public TimeSeriesViewWidget(UsageView timeSeriesView, EvidenceView featuresView)
	{
		m_TimeSeriesView = timeSeriesView;
		m_FeaturesView = featuresView;
		
		m_QueryService = QueryServiceLocator.getInstance().getTimeSeriesGXTQueryService();
		
		setSize(850, 1045);
		
		initComponents();
	}
	
	
	/**
	 * Creates and initialises the graphical components in the window.
	 */
	protected void initComponents()
	{
		setBorders(true);
		
		// Create the toolbar holding the attribute and metric controls.
		ToolBar toolbar = createToolBar();
		add(toolbar);
		
		m_ChartContainer = new LayoutContainer();   
        VBoxLayout layout = new VBoxLayout(); 
        layout.setVBoxLayoutAlign(VBoxLayoutAlign.STRETCH);   
        m_ChartContainer.setLayout(layout);  
        m_ChartContainer.setSize(750, 500);
  
        
        // Create a panel at the top to hold the chart title and subtitle,
        // and the zoom in / zoom out controls.
        LayoutContainer labelPanel = new LayoutContainer();
        VBoxLayout labelPanelLayout = new VBoxLayout();
        labelPanelLayout.setVBoxLayoutAlign(VBoxLayoutAlign.LEFT);
        labelPanel.setLayout(labelPanelLayout);  
        labelPanel.setHeight(50);
        
        m_ChartTitleLabel = new Label("Time series chart");
        m_ChartTitleLabel.addStyleName("prl-timeSeriesChart-title");
        
        m_ChartSubtitleLabel = new Label("");
        m_ChartSubtitleLabel.addStyleName("prl-timeSeriesChart-subtitle");
        
        labelPanel.add(m_ChartTitleLabel);
        labelPanel.add(m_ChartSubtitleLabel);
        
        
        LayoutContainer zoomBtnsC = new LayoutContainer();   
        HBoxLayout zbcLayout = new HBoxLayout();   
        zbcLayout.setPadding(new Padding(5));   
        zbcLayout.setHBoxLayoutAlign(HBoxLayoutAlign.TOP);   
        zbcLayout.setPack(BoxLayoutPack.END);   
        zoomBtnsC.setLayout(zbcLayout);   
        zoomBtnsC.setHeight(50);
        zoomBtnsC.setWidth(280);
        
        Anchor panLeftAnchor = new Anchor(
        		"<img src=\"splash/gxt/images/default/grid/page-prev.gif\" width=\"16\" height=\"16\" >" +
        		ClientUtil.CLIENT_CONSTANTS.panLeftLink(), true);
        panLeftAnchor.setStyleName("prl-timeSeriesChart-panText");
        panLeftAnchor.addClickHandler(new ClickHandler(){

			@Override
            public void onClick(ClickEvent event)
            {
				m_ChartWidget.panLeft();
            }
        	
        });
        
        Anchor panRightAnchor = new Anchor(
        		"<img src=\"splash/gxt/images/default/grid/page-next.gif\" width=\"16\" height=\"16\" >" +
        		ClientUtil.CLIENT_CONSTANTS.panRightLink(), true);
        panRightAnchor.setStyleName("prl-timeSeriesChart-panText");
        panRightAnchor.addClickHandler(new ClickHandler(){

			@Override
            public void onClick(ClickEvent event)
            {
				m_ChartWidget.panRight();
            }
        	
        });
  
        Anchor zoomInAnchor = new Anchor("<img src=\"images/zoom_in.png\" width=\"16\" height=\"16\" >" +
        		ClientUtil.CLIENT_CONSTANTS.zoomInLink(), true);
        zoomInAnchor.setStyleName("prl-timeSeriesChart-zoomText");
        zoomInAnchor.addClickHandler(new ClickHandler(){

			@Override
            public void onClick(ClickEvent event)
            {
	            m_ChartWidget.zoomInDateAxis();
            }
        	
        });
        
        Anchor zoomOutAnchor = new Anchor("<img src=\"images/zoom_out.png\" width=\"16\" height=\"16\" />" +
        		ClientUtil.CLIENT_CONSTANTS.zoomOutLink(), true);
        zoomOutAnchor.addStyleName("prl-timeSeriesChart-zoomText");
        zoomOutAnchor.addClickHandler(new ClickHandler(){

			@Override
            public void onClick(ClickEvent event)
            {
	            m_ChartWidget.zoomOutDateAxis();
            }
        	
        });
        
        zoomBtnsC.add(panLeftAnchor, new HBoxLayoutData(new Margins(0, 10, 0, 0)));    
        zoomBtnsC.add(panRightAnchor, new HBoxLayoutData(new Margins(0, 15, 0, 0)));    
        zoomBtnsC.add(zoomInAnchor, new HBoxLayoutData(new Margins(0, 10, 0, 0)));    
        zoomBtnsC.add(zoomOutAnchor, new HBoxLayoutData(new Margins(0)));   
        
        
        LayoutContainer zoomPanel = new LayoutContainer();
        VBoxLayout zoomPanelLayout = new VBoxLayout();
        zoomPanelLayout.setVBoxLayoutAlign(VBoxLayoutAlign.RIGHT);
        zoomPanel.setLayout(zoomPanelLayout);
        zoomPanel.setHeight(50);
        
        zoomPanel.add(zoomBtnsC);
        
        
        LayoutContainer topCont = new LayoutContainer();   
        topCont.setHeight(50);
        HBoxLayout hblayout = new HBoxLayout();   
        hblayout.setHBoxLayoutAlign(HBoxLayoutAlign.MIDDLE);   
        topCont.setLayout(hblayout);   
  
        HBoxLayoutData flex = new HBoxLayoutData(new Margins(0, 5, 0, 0));   
    	flex.setFlex(1);   
    	topCont.add(labelPanel, flex);   
        topCont.add(zoomPanel, flex);   
        
        m_ChartContainer.add(topCont);
        
	    
		// Add in the charting applet.
        //String appletId = m_TimeSeriesView.getDataType();
        //m_ChartWidget = new TimeSeriesChartAppletWidget(appletId);
        
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
        
        m_ChartContainer.add(m_ChartWidget.getChartWidget()); 
     	add(m_ChartContainer);  
     	
     	createFeaturesListComponents();

	}
	
	
	/**
	 * Creates the source, attribute and value controls.
	 */
	protected ToolBar createToolBar()
	{
		// Create the toolbar which will hold the controls for setting the
		// parameters of the chart i.e. attributes (if any) and metrics.
		GXT.useShims = true;
		ToolBar toolbar = new ToolBar();
		toolbar.setShim(true);
		
		// Create attribute name and value drop-downs if necessary.
		if (m_TimeSeriesView.hasAttributes() == true)
		{
			m_AttrNameComboBox = new ComboBox<BaseModelData>();
			m_AttrNameComboBox.setListStyle("prelert-combo-list");
			m_AttrNameComboBox.setWidth(90);
			m_AttrNameComboBox.setDisplayField("attributeName");
			m_AttrNameComboBox.setTriggerAction(TriggerAction.ALL);
			m_AttrNameComboBox.setStore(getAttributeNameList());
			m_AttributeName = getAttributeNameList().getAt(0).get("attributeName");
			
			m_AttrNameComboListener = new SelectionChangedListener<BaseModelData>() {

				@Override
                public void selectionChanged(SelectionChangedEvent<BaseModelData> se) 
				{    	  
					m_AttributeValue = null;
					
					BaseModelData selectedAttr = se.getSelectedItem();
					String attributeName = selectedAttr.get("attributeName");
					setAttributeName(attributeName);
				}
			};
			m_AttrNameComboBox.addSelectionChangedListener(m_AttrNameComboListener);
			
			// NB. The attribute values are only populated once the source name has been set.
			m_AttrValueComboBox = new ComboBox<BaseModelData>();
			m_AttrValueComboBox.setListStyle("prelert-combo-list");
			m_AttrValueComboBox.setEmptyText(m_TimeSeriesView.getSelectUserText());
			m_AttrValueComboBox.setDisplayField("attributeValue");
			m_AttrValueComboBox.setTypeAhead(true);
			m_AttrValueComboBox.setTriggerAction(TriggerAction.ALL);
			m_AttrValueComboBox.setStore(new ListStore<BaseModelData>());
	
			m_AttrValueComboListener = new SelectionChangedListener<BaseModelData>() {
			      @Override
                public void selectionChanged(SelectionChangedEvent<BaseModelData> se) 
			      {    	  
			    	  BaseModelData selectedUser = se.getSelectedItem();
			    	  
			    	  if (m_AttrValueComboBox.getStore().indexOf(selectedUser) == 0)
			    	  {
			    		  setAttributeValue(null);  		  
			    	  }
			    	  else
			    	  {
			    		  String username = selectedUser.get("attributeValue");
			    		  setAttributeValue(username);
			    	  } 
			      }
			};
			m_AttrValueComboBox.addSelectionChangedListener(m_AttrValueComboListener);
			
			toolbar.add(m_AttrNameComboBox);
			toolbar.add(new LabelToolItem(" = "));
			toolbar.add(m_AttrValueComboBox);
		}
		
		
		// Create a Metric-type (e.g. Total, Pending) drop-down.
		m_MetricComboBox = new ComboBox<BaseModelData>();
		m_MetricComboBox.setListStyle("prelert-combo-list");
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
		
		
		toolbar.add(new FillToolItem());
		toolbar.add(new LabelToolItem(ClientUtil.CLIENT_CONSTANTS.fieldShow()));
		toolbar.add(m_MetricComboBox);
		
		return toolbar;
	}
	
	
	/**
	 * Creates and adds the components for the features list.
	 */
	protected void createFeaturesListComponents()
	{
		Label featuresLabel = new Label(m_FeaturesView.getDataType() + " " + 
				ClientUtil.CLIENT_CONSTANTS.features());
		featuresLabel.addStyleName("prl-timeSeriesChart-title");
        add(featuresLabel);
		
		// Add an evidence view to list time series features.
     	m_FeaturesPanel = new EvidenceViewPanel(m_FeaturesView, Style.LayoutRegion.SOUTH);
     	m_FeaturesPanel.addStyleName("prl-timeSeriesFeaturesPanel");
     	m_FeaturesPanel.getSelectionModel().addSelectionChangedListener(
     			new SelectionChangedListener<EvidenceModel>(){

			@Override
            public void selectionChanged(SelectionChangedEvent<EvidenceModel> se)
            {
				EvidenceModel selectedRow = se.getSelectedItem();
				if (selectedRow != null)
				{
					// Set the source for the chart, but do not want to change the 
					// setting on the list (for case when all sources is selected).
					String featuresSource = m_FeaturesPanel.getSource();
					setSource(selectedRow.getSource());
					m_FeaturesPanel.setSource(featuresSource);
					
					String metric = selectedRow.get("metric");
					setMetric(metric, false, false);
					
					Date featureTime = ClientUtil.parseTimeField(
							selectedRow, m_FeaturesView.getTimeFrame());
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
		
		m_FeaturesPanel.addListener(GXTEvents.OpenNotificationViewClick, rveListener);
		m_FeaturesPanel.addListener(GXTEvents.OpenTimeSeriesViewClick, rveListener);
		m_FeaturesPanel.addListener(GXTEvents.OpenCausalityViewClick, rveListener);
     	
     	add(m_FeaturesPanel);
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
	 * Returns the list of attribute names.
	 * @return store of attribute names for this time series window.
	 */
	protected ListStore<BaseModelData> getAttributeNameList()
	{
		ListStore<BaseModelData> names = new ListStore<BaseModelData>();
		
		if (m_TimeSeriesView.hasAttributes() == true)
		{
			List<String> attributeNames = m_TimeSeriesView.getAttributeNames();
			
			for (String attributeName : attributeNames)
			{
				BaseModelData name = new BaseModelData();
				name.set("attributeName", attributeName);
				names.add(name);
			}
		}
		
		return names;
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
	public UsageView getView()
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
			
			// Clear the current attribute value since it may not be available
			// for the new source.
			m_AttributeValue = null;
			
			// Re-populate the attribute value drop-down for the new Source value.
			populateAttributeValues(m_AttributeName, m_Source);
			
			// Also clear the time marker, if any, on the chart.
			m_ChartWidget.clearTimeMarker();
			
			m_FeaturesPanel.setSource(m_Source);
		}
		else
		{
			// Populate the attribute values drop-down if it has yet to be populated.
			if  ( (m_TimeSeriesView.hasAttributes() == true) &&
					(m_AttrValueComboBox.getStore().getCount() == 0) )
			{
				populateAttributeValues(m_AttributeName, m_Source);
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
		setAttributeNameSelection(m_AttributeName);
		setAttributeValueSelection(m_AttributeValue);
		
		
		// Load the chart.
		String dataType = m_TimeSeriesView.getDataType();
		m_ChartWidget.removeAllTimeSeries();
		TimeSeriesConfig config = new TimeSeriesConfig(
				dataType, m_Metric, m_Source, m_AttributeName, m_AttributeValue);
		
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
			subTitle += "All sources";
		}
		m_ChartSubtitleLabel.setText(subTitle);
		
		if (loadFeatureList == true)
		{
			m_FeaturesPanel.setFilter(m_AttributeName, m_AttributeValue);
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
		GWT.log("TimeSeriesViewWidget.loadAtTime() : " + time, null);
		
		if (time != null)
		{
			// Load 30 mins before/after the supplied time.
			DateWrapper startTime = new DateWrapper(time);
			startTime = startTime.addMinutes(-30);
			DateWrapper endTime = new DateWrapper(time);
			endTime = endTime.addMinutes(30);
			
			m_ChartWidget.setDateRange(startTime.asDate(), endTime.asDate());
			
			m_ChartWidget.setTimeMarker(time, null);
		}
		
		load(loadFeatureList);
	}
	
	
	/**
	 * Runs a tool on the view in the widget.
	 * @param tool the tool to run.
	 */
	public void runTool(Tool tool)
	{
		// No longer used. Needs to be removed from ViewWidget interface.
	}
	
	
	/**
	 * Returns the attribute name for this usage window.
	 * @return the attribute name. If <code>null</code> then the usage view has
	 * no attributes e.g. IPC data.
	 */
	public String getAttributeName()
	{
		return m_AttributeName;
	}
	
	
	/**
	 * Sets the attribute name.
	 * @param name attribute name. If <code>null</code> then the attribute name
	 * 	drop-down will be set to the first item in the list.
	 */
	public void setAttributeName(String name)
	{
		setAttributeName(name, true);
	}
	
	
	/**
	 * Sets the attribute name.
	 * @param name attribute name. If <code>null</code> then the attribute name
	 * 	drop-down will be set to the first item in the list.
	 * @param reload <code>true</code> to reload the data in the chart, <code>false</code> otherwise.
	 */
	protected void setAttributeName(String name, boolean reload)
	{
		if ( (m_TimeSeriesView.hasAttributes() == false) || 
				(name != null && name.equals(m_AttributeName) == true) )
		{
			// No change to current setting.
			return;
		}
		
		// Three possibilities:
		// 1. Name is non-null and is in the name combo store.
		// 2. Name is null - set value to first one in store.
		// 3. Name is non-null, but does not appear in the name combo store - 
		// 			set value to first one in store.
		boolean chartNeedsReloading = false;
		boolean attrValuesNeedReloading = false;
		ListStore<BaseModelData> namesStore = m_AttrNameComboBox.getStore();
		if (name != null)
		{
			boolean inStore = false;
			
			BaseModelData namesData;
			for (int i = 0; i < namesStore.getCount(); i++)
			{
				namesData = namesStore.getAt(i);
				if (namesData.get("attributeName").equals(name))
				{
					// i.e. option 1.
					m_AttributeName = name;
					chartNeedsReloading = true;
					attrValuesNeedReloading = true;
					inStore = true;
					break;
				}
			}
			
			if (inStore == false)
			{
				// i.e. option 3 - supplied attribute name is not in the store - 
				// set to the first value in the Combo instead.
				String firstAttrName = namesStore.getAt(0).get("attributeName");
				if ( (m_AttributeName != null) && 
						(m_AttributeName.equals(firstAttrName) == false) )
				{
					attrValuesNeedReloading = true;
				}
				
				m_AttributeName = firstAttrName;
				chartNeedsReloading = true;
			}
		}
		else
		{
			if (m_AttributeName != null)
			{
				// i.e. option 2 
				// set to the first value in the Combo instead.
				String firstAttrName = namesStore.getAt(0).get("attributeName");
				if (m_AttributeName.equals(firstAttrName) == false)
				{
					attrValuesNeedReloading = true;
				}
				
				m_AttributeName = namesStore.getAt(0).get("attributeName");
				chartNeedsReloading = true;
			}
		}
		
		if (reload == true && chartNeedsReloading == true)
		{
			load();	
		}
		
		if (attrValuesNeedReloading == true)
		{
			populateAttributeValues(m_AttributeName, m_Source);
		}
	}
	
	
	/**
	 * Returns the currently selected attribute value.
	 * @return the current attribute value, which may be <code>null</code>.
	 */
	public String getAttributeValue()
	{		
		return m_AttributeValue;
	}
	
	
	/**
	 * Sets the attribute value, and reloads the usage data in the window.
	 * @param value attribute value, which may be <code>null</code>.
	 */
	public void setAttributeValue(String value)
	{
		setAttributeValue(value, true);
	}
	
	
	/**
	 * Sets the attribute value, and reloads the usage data in the window. if desired.
	 * @param value attribute value, which may be <code>null</code>.
	 * @param reload <code>true</code> to reload the data in the chart, <code>false</code> otherwise.
	 */
	protected void setAttributeValue(String value, boolean reload)
	{
		if ( (m_TimeSeriesView.hasAttributes() == false) || 
				(value != null && value.equals(m_AttributeValue) == true) ||
				(value == null && m_AttributeValue == null) )
		{
			// No change to current setting.
			return;
		}
		
		// Three possibilities:
		// 1. User is non-null and is in the user combo store.
		// 2. User is null - load data from all attribute values.
		// 3. User is non-null, but does not appear in the users combo - 
		// 		set to new value anyway as the attribute name may have changed too.
		
		// Jan 2010: TO DO: validate that the attribute value is valid for the
		// current source and attribute name. May have to be done in server-side load.
		
		m_AttributeValue = value;
		
		if (reload == true)
		{
			load();
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
	public void clearAttributeValues()
	{	
		GWT.log("clearAttributeValues()", null);
		
		if (m_TimeSeriesView.hasAttributes() == true)
		{
			// Clear out the User ComboBox and repopulate with the users for this source.
			m_AttrValueComboBox.clearSelections();
			ListStore<BaseModelData> valuesStore = m_AttrValueComboBox.getStore();
			valuesStore.removeAll();
			
			// Add an 'All' item at the top.
			BaseModelData allValuesData = new BaseModelData();
			allValuesData.set("attributeValue", ClientUtil.CLIENT_CONSTANTS.optionAll());
			valuesStore.add(allValuesData);
			
			// Disable the SelectionChangedListener whilst setting the initial
			// value to ensure it does not trigger another query.
			m_AttrValueComboBox.disableEvents(true);
			m_AttrValueComboBox.setValue(allValuesData);  
			m_AttrValueComboBox.enableEvents(true);
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
		GWT.log("populateAttributeValues(" + attributeName + "," + source, null);
		
		if (m_TimeSeriesView.hasAttributes() == true)
		{
			m_AttrValueComboBox.disableEvents(true);
			
			m_QueryService.getAttributeValues(m_TimeSeriesView.getDataType(), 
					attributeName, source, new ApplicationResponseHandler<List<String>>(){
	
		        @Override
                public void uponFailure(Throwable caught)
		        {
		        	 m_AttrValueComboBox.enableEvents(true);
		        	
			        MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
			        		ClientUtil.CLIENT_CONSTANTS.errorNoServerResponse(), null);
		        }
	
	
		        @Override
                public void uponSuccess(List<String> usernames)
		        {
		        	// Clear out the User ComboBox and repopulate with the users for this source.
					clearAttributeValues();
					ListStore<BaseModelData> usernamesStore = m_AttrValueComboBox.getStore();
		        	
		        	BaseModelData userData;
		        	
		        	for (String username : usernames)
		        	{
		        		userData = new BaseModelData();
		        		userData.set("attributeValue", username);
		        		usernamesStore.add(userData);
		        	}
		        	
		        	if (m_AttributeValue != null)
		        	{
		        		// Set the User ComboBox in case when the window is first
		        		// loaded this call completes AFTER the data is loaded.
		        		setAttributeValueSelection(m_AttributeValue);
		        	}
		        	
		        	m_AttrValueComboBox.enableEvents(true);
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
	 * Sets the attribute name Combo Box to the specified name. Note that this simply
	 * updates the Combo Box field, and does not update the chart.
	 * @param attributeName name to set.
	 */
	protected void setAttributeNameSelection(String attributeName)
	{
		if (m_TimeSeriesView.hasAttributes() == true)
		{
			ListStore<BaseModelData> namesStore = m_AttrNameComboBox.getStore();
			if (namesStore.getCount() > 0)
			{
				m_AttrNameComboBox.disableEvents(true);
				
				List<BaseModelData> selectedNames = m_AttrNameComboBox.getSelection();
				String currentlySelectedName = null;
				if (selectedNames.size() > 0)
				{
					BaseModelData selectedNameData = selectedNames.get(0);
					if (m_AttrNameComboBox.getStore().indexOf(selectedNameData) != 0)
			  	  	{
						currentlySelectedName = selectedNameData.get("attributeName");
			  	  	}
				}
				
				if (attributeName.equals(currentlySelectedName) == false)
				{
					BaseModelData nameData;
					for (int i = 0; i < namesStore.getCount(); i++)
					{
						nameData = namesStore.getAt(i);
						if (nameData.get("attributeName").equals(attributeName))
						{
							m_AttrNameComboBox.setValue(nameData);
							break;
						}
					}
				}
				
				m_AttrNameComboBox.enableEvents(true);
			}
		}
	}
	
	
	/**
	 * Sets the attribute value Combo Box to the selected value. Note that this simply
	 * updates the Combo Box field, and does not update the chart.
	 * @param attributeValue value to set.
	 */
	protected void setAttributeValueSelection(String attributeValue)
	{	
		if (m_TimeSeriesView.hasAttributes() == true)
		{
			ListStore<BaseModelData> usersStore = m_AttrValueComboBox.getStore();	
			if (usersStore.getCount() > 0)
			{	
				if (attributeValue == null)
				{
					m_AttrValueComboBox.disableEvents(true);
					m_AttrValueComboBox.setValue(usersStore.getAt(0));
					m_AttrValueComboBox.enableEvents(true);
				}
				else
				{	
					List<BaseModelData> selectedServices = m_AttrValueComboBox.getSelection();
					String currentlySelectedUser = null;
					if (selectedServices.size() > 0)
					{
						BaseModelData selectedServiceData = selectedServices.get(0);
						if (m_AttrValueComboBox.getStore().indexOf(selectedServiceData) != 0)
				  	  	{
							currentlySelectedUser = selectedServiceData.get("attributeValue");
				  	  	}
					}
					
					if (attributeValue.equals(currentlySelectedUser) == false)
					{
						BaseModelData userData;
						for (int i = 0; i < usersStore.getCount(); i++)
						{
							userData = usersStore.getAt(i);
							if (userData.get("attributeValue").equals(attributeValue))
							{
								m_AttrValueComboBox.disableEvents(true);
								m_AttrValueComboBox.setValue(userData);		
								m_AttrValueComboBox.enableEvents(true);
								break;
							}
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
	
	
}
