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

package com.prelert.client.incident;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.extjs.gxt.ui.client.GXT;
import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.ContainerEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.GridEvent;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.LoadListener;
import com.extjs.gxt.ui.client.event.SelectionChangedEvent;
import com.extjs.gxt.ui.client.event.SelectionChangedListener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.event.SplitBarEvent;
import com.extjs.gxt.ui.client.store.Store;
import com.extjs.gxt.ui.client.store.StoreEvent;
import com.extjs.gxt.ui.client.store.StoreListener;
import com.extjs.gxt.ui.client.store.TreeStore;
import com.extjs.gxt.ui.client.store.TreeStoreEvent;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.SplitBar;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.SimpleComboBox;
import com.extjs.gxt.ui.client.widget.form.SimpleComboValue;
import com.extjs.gxt.ui.client.widget.form.ComboBox.TriggerAction;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout.VBoxLayoutAlign;
import com.extjs.gxt.ui.client.widget.toolbar.LabelToolItem;
import com.extjs.gxt.ui.client.widget.toolbar.ToolBar;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbstractImagePrototype;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.client.CSSColorChart;
import com.prelert.client.ClientUtil;
import com.prelert.client.chart.CausalityChartWidget;
import com.prelert.client.chart.CausalityGChartWidget;
import com.prelert.client.chart.ChartSymbolCellRenderer;
import com.prelert.client.chart.ChartToolBar;
import com.prelert.client.event.GXTEvents;
import com.prelert.client.event.RequestViewEvent;
import com.prelert.data.CausalityView;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.TimeFrame;
import com.prelert.data.TimeSeriesDataPoint;
import com.prelert.data.gxt.ActivityTreeModel;
import com.prelert.data.gxt.CausalityDataModel;
import com.prelert.data.gxt.EvidenceModel;
import com.prelert.data.gxt.IncidentModel;
import com.prelert.service.AsyncServiceLocator;
import com.prelert.service.CausalityQueryServiceAsync;
import com.prelert.service.IncidentQueryServiceAsync;


/**
 * Ext GWT (GXT) widget displaying the summary of an activity for use in the Activity View. 
 * The widget consists of a tree component on the left, displaying an analysis of 
 * the activity by shared attributes, and a time line chart to the right displaying 
 * when the top time series and notifications occur in relation to each other.
 * 
 * <dl>
 * <dt><b>Events:</b></dt>
 * 
 * <dd><b>OpenCausalityViewClick</b> : RequestViewEvent<br>
 * <div>Fires when the link to the Analysis View is selected.</div>
 * <ul>
 * <li>component : this</li>
 * <li>model: notification or time series feature whose causality data is being requested</li>
 * </ul>
 * </dd>
 * 
 * <dd><b>OpenNotificationViewClick</b> : RequestViewEvent<br>
 * <div>Fires after a link to a notification view is selected.</div>
 * <ul>
 * <li>component : CausalityChartWidget</li>
 * </ul>
 * </dd>
 * 
 * <dd><b>OpenTimeSeriesViewClick</b> : RequestViewEvent<br>
 * <div>Fires after a link to a time series view is selected.</div>
 * <ul>
 * <li>component : CausalityChartWidget</li>
 * </ul>
 * </dd>
 * 
 * </dl>
 * 
 * @author Pete Harverson
 */
public class ActivitySummaryTreeWidget extends ContentPanel
{
	private CausalityQueryServiceAsync 	m_CausalityQueryService;
	private IncidentQueryServiceAsync	m_IncidentQueryService;
	
	private IncidentModel				m_Incident;
	
	private ToolBar 					m_PathToolBar;
	private ActivityMetricPathLabel		m_PathField;
	private SimpleComboBox<String>		m_ViewByCombo;
	private ActivitySummaryTreeGrid		m_SummaryTree;
	private TreeStore<ActivityTreeModel> m_SummaryTreeStore;
	
	private CausalityView				m_ChartConfiguration;
	private ContentPanel				m_ChartContainer;
	private CausalityChartWidget		m_ChartWidget;
	
	
	/**
	 * Creates a new widget for displaying the summary of an activity.
	 */
	public ActivitySummaryTreeWidget()
	{
		m_CausalityQueryService = AsyncServiceLocator.getInstance().getCausalityQueryService();
		m_IncidentQueryService = AsyncServiceLocator.getInstance().getIncidentQueryService();
		
		initComponents();
	}
	
	
	/**
	 * Creates and initialises the graphical components in the widget.
	 */
	protected void initComponents()
	{
		setHeaderVisible(false);
		setScrollMode(Scroll.NONE);
		
		// Create the toolbar displaying the metric path.
		m_PathToolBar = new ToolBar();
		m_PathToolBar.setEnableOverflow(false);
		m_PathToolBar.setSpacing(3);
		setTopComponent(m_PathToolBar);
		
		final LabelToolItem pathLabel = new LabelToolItem("Path:");
		final LabelToolItem viewLabel = new LabelToolItem("Analyze by:");
		
		
		m_ViewByCombo = new SimpleComboBox<String>();
		m_ViewByCombo.setWidth(130);
		m_ViewByCombo.setTriggerAction(TriggerAction.ALL); 
		m_ViewByCombo.setEditable(false);
		m_ViewByCombo.setListStyle("prl-combo-list");
		m_ViewByCombo.addListener(Events.SelectionChange, 
				new SelectionChangedListener<SimpleComboValue<String>>(){

	        @Override
	        public void selectionChanged(SelectionChangedEvent<SimpleComboValue<String>> se)
	        {
	        	loadTree();
	        }
        });
		m_ViewByCombo.enableEvents(false);
		
		
		m_PathField = new ActivityMetricPathLabel();
		final LayoutContainer pathFieldContainer = new LayoutContainer();
		pathFieldContainer.add(m_PathField);
		pathFieldContainer.setBorders(true);
		pathFieldContainer.addStyleName("prl-title-label");	
	
		
		// Auto resize metric path field to fill available width.
        Listener<ComponentEvent> resizeListener = new Listener<ComponentEvent>(){
			
			@Override
            public void handleEvent(ComponentEvent be)
            {
				pathFieldContainer.setWidth(m_PathToolBar.getWidth() - m_ViewByCombo.getWidth() -
						pathLabel.getWidth() - viewLabel.getWidth() - 20);
            }
		};
		addListener(Events.Resize, resizeListener);
		
		m_PathToolBar.add(pathLabel);
		m_PathToolBar.add(pathFieldContainer);
		
		m_PathToolBar.add(viewLabel);
		m_PathToolBar.add(m_ViewByCombo);
		
		
		// Create the tree to display a summary of the activity.
		m_SummaryTreeStore = new TreeStore<ActivityTreeModel>();	
		m_SummaryTree = new ActivitySummaryTreeGrid(m_SummaryTreeStore, false);
		m_SummaryTree.getSelectionModel().setLocked(true);	// No row selection effects.
		m_SummaryTree.addListener(Events.CellClick, new Listener<GridEvent<ActivityTreeModel>>(){
	    	public void handleEvent(GridEvent<ActivityTreeModel> event) 
	    	{
	    		if(event.getTarget(".prl-analysis-link", 1) != null)
	    		{
	    			// Fire a RequestViewEvent to open the Analysis view.
					// Create an item of evidence, whose ID matches the 'headline' notification
					// or time series feature ID, with the description and time set to those of
					// the incident shown in the widget.
					RequestViewEvent<EvidenceModel> rve = 
						new RequestViewEvent<EvidenceModel>(ActivitySummaryTreeWidget.this);
					rve.setGroupBy(getAnalyzeBy());
					
					EvidenceModel evidence = new EvidenceModel();
					evidence.setId(m_Incident.getEvidenceId());
					evidence.setDescription(m_Incident.getDescription());
					evidence.setTime(TimeFrame.SECOND, m_Incident.getTime());
					rve.setModel(evidence);
					rve.setView(m_ChartConfiguration);
					fireEvent(GXTEvents.OpenCausalityViewClick, rve);
	    		}
	    	} 
	    });
		
		// Create the causality chart.
		m_ChartContainer = createCausalityChart();
		
		m_SummaryTreeStore.addStoreListener(new StoreListener<ActivityTreeModel>(){

			@Override
            public void storeClear(StoreEvent<ActivityTreeModel> se)
            {
				m_PathField.setText("");
            }

			
			@Override
            public void storeBeforeDataChanged(
                  	StoreEvent<ActivityTreeModel> se)
            {
				m_PathToolBar.setEnabled(false);
				m_ChartContainer.mask(GXT.MESSAGES.loadMask_msg());
            }
			
			
            @Override
            public void storeDataChanged(StoreEvent<ActivityTreeModel> se)
            {
            	// Display the 'top' causality data in the chart,
            	// and update the path field.
            	m_PathField.setPartialPathTreeNode(m_SummaryTree.getFirstLeaf());
            	m_PathToolBar.setEnabled(true);
        		List<CausalityDataModel> topItems = 
        			m_SummaryTree.getTopCausalityData(ClientUtil.CAUSALITY_MAX_DISPLAY_ITEMS, 0);
        		
            	loadCausalityChart(topItems);
            }
            
	    });
		
		BorderLayout borderLayout = new BorderLayout();   
		borderLayout.setContainerStyle("prl-border-layout-ct");
	    setLayout(borderLayout); 
	    
	    BorderLayoutData westData = new BorderLayoutData(LayoutRegion.WEST, 0.4f, 300, 800); 
	    westData.setSplit(true);   
	    westData.setFloatable(false);   
	    westData.setMargins(new Margins(0, 2, 0, 0)); 
	    
	    BorderLayoutData centerData = new BorderLayoutData(LayoutRegion.CENTER);   
	    centerData.setMargins(new Margins(0));
	    
	    add(m_SummaryTree, westData);
	    add(m_ChartContainer, centerData);   
	    
	    
	    // Add a listener to the SplitBar to size the components to the available height.		
		final Listener<SplitBarEvent> splitBarListener = new Listener<SplitBarEvent>()
		{
			@Override
            public void handleEvent(SplitBarEvent sbe)
			{
				sizeComponentsToFit();
			}
		};
		
		
		addListener(Events.AfterLayout, new Listener<ContainerEvent<LayoutContainer, Component>>(){

			@Override
            public void handleEvent(ContainerEvent<LayoutContainer, Component> be)
            {		        
				SplitBar splitBar = m_SummaryTree.getData("splitBar");
		        if (splitBar != null)
		        {
		        	splitBar.addListener(Events.DragEnd, splitBarListener);
		        	ActivitySummaryTreeWidget.this.removeListener(Events.AfterLayout, this);
		        }
            }
			
		});
	}

	
	/**
	 * Creates the graphical components for the causality chart.
	 * @return ContentPanel holding the chart component.
	 */
    protected ContentPanel createCausalityChart()
	{  
        VBoxLayout layout = new VBoxLayout();    
        layout.setVBoxLayoutAlign(VBoxLayoutAlign.STRETCH);   
        ContentPanel chartContainer = new ContentPanel(layout);  
        chartContainer.setHeaderVisible(false);
        chartContainer.setBodyBorder(false);

        
        // Create the chart widget for plotting notifications and time series.
        m_ChartWidget = new CausalityGChartWidget(false);
        m_ChartWidget.setChartHeight(410);

        
        // Listen for events to open notification and time series views.
		Listener<RequestViewEvent<?>> chartListener = new Listener<RequestViewEvent<?>>(){

            @Override
            public void handleEvent(RequestViewEvent<?> rve)
            {
            	// Propagate the event.
            	if (rve.getViewToOpenDataType().getDataCategory() == DataSourceCategory.NOTIFICATION)
            	{
            		CausalityDataModel causalityData = (CausalityDataModel)(rve.getModel());
            		fireOpenNotificationViewClick(causalityData);
            	}
            	else
            	{
            		fireEvent(rve.getType(), rve);
            	}
            }
			
		};
		
		m_ChartWidget.addListener(GXTEvents.OpenNotificationViewClick, chartListener);
		m_ChartWidget.addListener(GXTEvents.OpenTimeSeriesViewClick, chartListener);
		
        
        // Create the chart toolbar holding zooming tools. 
        final ChartToolBar<TimeSeriesDataPoint> chartTools = 
        	new ChartToolBar<TimeSeriesDataPoint>(m_ChartWidget, false);
        chartTools.setAlignment(HorizontalAlignment.RIGHT);

        
        // Add a Scale to Fit button.
		Button scaleBtn = new Button();
		scaleBtn.setIcon(AbstractImagePrototype.create(ClientUtil.CLIENT_IMAGES.toolbar_scale_to_fit()));
		scaleBtn.setToolTip(ClientUtil.CLIENT_CONSTANTS.scaleToFit());
		scaleBtn.setMouseEvents(false);		// Disable mouse events for plain white toolbar.
		scaleBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
			{
				m_ChartWidget.scaleToFit();
			}
		});
		chartTools.add(scaleBtn);
        
        LoadListener loadListener = new LoadListener()
		{
			@Override
            public void loaderBeforeLoad(LoadEvent le)
			{
				m_ChartContainer.mask(GXT.MESSAGES.loadMask_msg());
				chartTools.setEnabled(false);
			}


			@Override
            public void loaderLoad(LoadEvent le)
			{
				m_ChartContainer.unmask();
				chartTools.setEnabled(true);
			}


			@Override
            public void loaderLoadException(LoadEvent le)
			{
				m_ChartContainer.unmask();
				chartTools.setEnabled(true);
			}
		};
		m_ChartWidget.addLoadListener(loadListener);
        
        chartContainer.add(chartTools);
        chartContainer.add(m_ChartWidget.getChartWidget());
     	
     	return chartContainer;
	}
	
	
	/**
	 * Sets the incident whose causality data is to be summarised in the widget.
	 * @param incident incident for which to display a summary.
	 */
	public void setIncident(IncidentModel incident)
	{
		m_Incident = incident;
		
		m_ChartConfiguration = null;
		m_ChartWidget.removeAll();
		m_PathToolBar.setEnabled(false);
		m_ChartContainer.mask(GXT.MESSAGES.loadMask_msg());
		m_SummaryTree.mask(GXT.MESSAGES.loadMask_msg());
		
		// Load up the causality view config, needed by the grid and chart.
		ApplicationResponseHandler<CausalityView> callback = 
			new ApplicationResponseHandler<CausalityView>()
		{
			@Override
            public void uponFailure(Throwable caught)
			{
				GWT.log(ClientUtil.CLIENT_CONSTANTS.errorLoadingActivityData() + ": ", caught);
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
						ClientUtil.CLIENT_CONSTANTS.errorLoadingActivityData(), null);
				m_PathToolBar.setEnabled(true);
				m_ChartContainer.unmask();
				m_SummaryTree.unmask();
			}


			@Override
            public void uponSuccess(CausalityView causalityView)
			{	
				GWT.log("ActivitySummaryTreeWidget - loaded CausalityView: " + causalityView);
				
				m_ChartConfiguration = causalityView;
				
				m_PathField.setMetricPathTemplate(m_ChartConfiguration.getMetricPathTreeNodes());
				setAnalyzeByAttributes(m_ChartConfiguration.getAttributes());
				m_ChartWidget.setPeakValuesByTypeId(causalityView.getPeakValuesByTypeId());
				
				m_SummaryTree.setIncident(m_Incident);
				loadTree();
			}
		};
		
		m_CausalityQueryService.getViewConfiguration(m_Incident.getEvidenceId(), 
				ClientUtil.CAUSALITY_METRICS_TIME_SPAN, callback);
	}
	
	
	/**
	 * Returns the selected value of the 'Analyze by' combo box.
	 * @return the current 'Analyze by' attribute name,
	 * {@link com.prelert.client.ClientMessages#analyseOptionMostCommon()} or
	 * {@link com.prelert.client.ClientMessages#analyseOptionPathOrder()}.
	 */
	public String getAnalyzeBy()
	{
		return m_ViewByCombo.getSimpleValue();
	}
	
	
	/**
	 * Sets the list of attributes specific to this activity which are available
	 * for analysis.
	 * @param attributeNames list of names of the attributes in the metric path.
	 */
	protected void setAnalyzeByAttributes(List<String> attributeNames)
	{
		// Add options for 'Path order' and 'Most common'.
		ArrayList<String> fullList = new ArrayList<String>(attributeNames);		
		fullList.add(0, ClientUtil.CLIENT_CONSTANTS.analyseOptionPathOrder());
		fullList.add(1, ClientUtil.CLIENT_CONSTANTS.analyseOptionMostCommon());
		
		String viewBy = m_ViewByCombo.getSimpleValue();
		if (viewBy == null || viewBy.length() == 0 ||
				fullList.indexOf(viewBy) == -1)
		{
			viewBy = ClientUtil.CLIENT_CONSTANTS.analyseOptionPathOrder();
		}
		
		m_ViewByCombo.enableEvents(false);
		m_ViewByCombo.clearSelections();
		m_ViewByCombo.removeAll();
		m_ViewByCombo.add(fullList);
		m_ViewByCombo.setSimpleValue(viewBy);
		m_ViewByCombo.enableEvents(true);
	}
	
	
	/**
	 * Loads a summary tree of the causality data for an activity, analyzed by 
	 * the specified attribute.
	 */
	protected void loadTree()
	{
		String analyzeBy = getAnalyzeBy();
		m_SummaryTree.mask(GXT.MESSAGES.loadMask_msg());
		m_SummaryTreeStore.fireEvent(Store.BeforeDataChanged, 
				new TreeStoreEvent<ActivityTreeModel>(m_SummaryTreeStore));
		m_SummaryTreeStore.removeAll();
		
		boolean metricPathOrder = analyzeBy.equals(ClientUtil.CLIENT_CONSTANTS.analyseOptionPathOrder());
		String useAnalyzeBy = null;
		if ( (metricPathOrder == false) && 
				(analyzeBy.equals(ClientUtil.CLIENT_CONSTANTS.analyseOptionMostCommon()) == false) )
		{
			useAnalyzeBy = analyzeBy;
		}
		
		m_IncidentQueryService.getSummaryTree(m_Incident.getEvidenceId(), 
				m_ChartConfiguration.getAttributes(), 
				metricPathOrder, useAnalyzeBy, ClientUtil.CAUSALITY_MAX_DISPLAY_ITEMS, 
				new TreeLoadQueryCallback());
	}
	
	
	/**
	 * Loads the causality chart with the top items from the summary grid.
	 */
	protected void loadCausalityChart(List<CausalityDataModel> topItems)
	{
		m_ChartWidget.removeAll();
		
		// Plot the 'top' probable causes on the chart,
    	Date incidentTime = m_Incident.getTime();	
		m_ChartWidget.setDateRange(
				new Date(incidentTime.getTime() - (ClientUtil.CAUSALITY_METRICS_TIME_SPAN*1000)/2), 
				new Date(incidentTime.getTime() + (ClientUtil.CAUSALITY_METRICS_TIME_SPAN*1000)/2));
		m_ChartWidget.setTimeMarker(incidentTime);
    	
    	for (CausalityDataModel topCausalityData : topItems)
    	{
			m_ChartWidget.addCausalityData(topCausalityData);
			
			// Display the appropriate symbol for the key in the summary tree grid.
			String hexColor = m_ChartWidget.getLineColour(topCausalityData);
	    	if (hexColor != null)
	    	{
	    		String colorName = CSSColorChart.getInstance().getColorName(hexColor);
	    		topCausalityData.set(ChartSymbolCellRenderer.DEFAULT_COLOR_PROPERTY, colorName);
	    	}
	    	
	    	topCausalityData.set(ChartSymbolCellRenderer.DEFAULT_SHAPE_PROPERTY, 
	    			m_ChartWidget.getSymbolShape(topCausalityData));
    	}
	}
	
	
	/**
	 * Overrides setSize(int, int) to set the size of the components to fill the 
	 * space available. This method fires the <i>Resize</i> event.
	 * @param width the new width to set.
	 * @param height the new height to set.
     */
    @Override
    public void setSize(int width, int height)
    {
	    super.setSize(width, height);
	    sizeComponentsToFit();
    }
    
    
    /**
     * Sizes the components in the widget to fill the available space.
     */
    protected void sizeComponentsToFit()
    {
	    // For causality chart, subtract height for the toolbar.
    	m_SummaryTree.setHeight(m_ChartContainer.getInnerHeight());
    	int chartHeight = m_ChartContainer.getInnerHeight() - 25;
    	m_ChartWidget.setChartSize(m_ChartContainer.getWidth(), chartHeight);
    }

    
    /**
     * Fires an OpenNotificationViewClick event for the specified notification type
     * causality data.
     * @param causalityData notification data for which to fire an event.
     */
    protected void fireOpenNotificationViewClick(CausalityDataModel causalityData)
    {
    	// Need to obtain the evidence ID for the latest notification in the set.
    	final CausalityDataModel notificationData = causalityData;
    	
    	ApplicationResponseHandler<Integer> callback = 
			new ApplicationResponseHandler<Integer>()
		{
			@Override
            public void uponFailure(Throwable caught)
			{
				GWT.log(ClientUtil.CLIENT_CONSTANTS.errorLoadingEvidenceData() + ": ", caught);
				MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
						ClientUtil.CLIENT_CONSTANTS.errorLoadingEvidenceData(), null);
			}


			@Override
            public void uponSuccess(Integer evidenceId)
			{	
				EvidenceModel notification = new EvidenceModel();
				notification.setId(evidenceId);
				notification.setDataType(notificationData.getDataSourceName());
				notification.setSource(notificationData.getSource());
				
				RequestViewEvent<EvidenceModel> rve = 
					new RequestViewEvent<EvidenceModel>(m_ChartWidget.getChartWidget());
				rve.setViewToOpenDataType(notificationData.getDataSourceType());
				rve.setSourceName(notificationData.getSource());	
				rve.setModel(notification);	
				fireEvent(GXTEvents.OpenNotificationViewClick, rve);
			}
		};
    	
    	m_CausalityQueryService.getLatestEvidenceId(m_Incident.getEvidenceId(), 
    			causalityData.getDataSourceName(), causalityData.getDescription(), 
    			causalityData.getSource(), causalityData.getAttributes(), callback);
    }
    
    
    /**
 	 * Response handler for tree load queries. 
     */
    class TreeLoadQueryCallback extends ApplicationResponseHandler<ActivityTreeModel>
    {
    	@Override
        public void uponSuccess(ActivityTreeModel rootNode)
        {
    		m_SummaryTreeStore.add(rootNode, true);
    		m_SummaryTreeStore.fireEvent(Store.DataChanged, 
    				new TreeStoreEvent<ActivityTreeModel>(m_SummaryTreeStore));
			
			// Add a node at the bottom to act as a link to the Analysis View.
    		m_SummaryTreeStore.add(new ActivityTreeModel(), false);
			
    		m_SummaryTree.expandAll();
    		m_SummaryTree.unmask();
        }

		@Override
        public void uponFailure(Throwable caught)
        {
			GWT.log(ClientUtil.CLIENT_CONSTANTS.errorLoadingActivityData() + ": ", caught);
			MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(),
					ClientUtil.CLIENT_CONSTANTS.errorLoadingActivityData(), null);
			m_SummaryTree.unmask();
        }
    	
    }
}
