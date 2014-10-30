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

import java.util.Date;
import java.util.List;

import com.extjs.gxt.ui.client.GXT;
import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.Style.LayoutRegion;
import com.extjs.gxt.ui.client.Style.Scroll;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.ContainerEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.LoadListener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.event.SplitBarEvent;
import com.extjs.gxt.ui.client.store.StoreEvent;
import com.extjs.gxt.ui.client.store.StoreListener;
import com.extjs.gxt.ui.client.util.Margins;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.ContentPanel;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.SplitBar;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.layout.BorderLayout;
import com.extjs.gxt.ui.client.widget.layout.BorderLayoutData;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout;
import com.extjs.gxt.ui.client.widget.layout.VBoxLayout.VBoxLayoutAlign;
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
import com.prelert.data.gxt.CausalityAggregateModel;
import com.prelert.data.gxt.CausalityDataModel;
import com.prelert.data.gxt.EvidenceModel;
import com.prelert.data.gxt.IncidentModel;
import com.prelert.service.AsyncServiceLocator;
import com.prelert.service.CausalityQueryServiceAsync;


/**
 * Ext GWT (GXT) widget for displaying the summary of an incident. The widget 
 * consists of a grid component on the left, listing the aggregated causality
 * data, and a time line chart to the right displaying when the aggregate objects
 * occur in relation to each other.
 * 
 * <dl>
 * <dt><b>Events:</b></dt>
 * 
 * <dd><b>OpenCausalityViewClick</b> : RequestViewEvent<br>
 * <div>Fires when the 'Explore' button is selected.</div>
 * <ul>
 * <li>component : this</li>
 * <li>model: notification or time series feature whose causality data is being requested</li>
 * </ul>
 * </dd>
 * 
 * </dl>
 * 
 * @author Pete Harverson
 */
public class IncidentSummaryWidget extends LayoutContainer
{
	private CausalityQueryServiceAsync 	m_CausalityQueryService;
	
	private IncidentModel				m_Incident;
	
	private IncidentSummaryGrid			m_SummaryGrid;
	
	private CausalityView				m_ChartConfiguration;
	private ContentPanel				m_ChartContainer;
	private CausalityChartWidget		m_ChartWidget;

	
	/**
	 * Creates a new widget for displaying the summary of an incident.
	 */
	public IncidentSummaryWidget()
	{
		m_CausalityQueryService = AsyncServiceLocator.getInstance().getCausalityQueryService();
		
		initComponents();
	}
	
	
	/**
	 * Creates and initialises the graphical components in the widget.
	 */
	protected void initComponents()
	{
		setBorders(true);
		setScrollMode(Scroll.AUTOY);
		
		// Create the grid to list the aggregate summaries.
		m_SummaryGrid = createSummaryGrid();
		
		// Create the causality chart.
		m_ChartContainer = createCausalityChart();
		
		BorderLayout borderLayout = new BorderLayout();   
		borderLayout.setContainerStyle("prl-border-layout-ct");
	    setLayout(borderLayout); 
	    
	    BorderLayoutData westData = new BorderLayoutData(LayoutRegion.WEST, 0.4f, 300, 800); 
	    westData.setSplit(true);   
	    westData.setFloatable(false);   
	    westData.setMargins(new Margins(0, 2, 0, 0)); 
	    
	    BorderLayoutData centerData = new BorderLayoutData(LayoutRegion.CENTER);   
	    centerData.setMargins(new Margins(0));
	    
	    add(m_SummaryGrid, westData);
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
				SplitBar splitBar = m_SummaryGrid.getData("splitBar");
		        if (splitBar != null)
		        {
		        	splitBar.addListener(Events.DragEnd, splitBarListener);
		        	IncidentSummaryWidget.this.removeListener(Events.AfterLayout, this);
		        }
            }
			
		});
	}
	
	
	/**
	 * Creates the GXT Grid component which displays the aggregated data descriptions.
	 * @return the summary grid.
	 */
	protected IncidentSummaryGrid createSummaryGrid()
	{
		final IncidentSummaryGrid summaryGrid = new IncidentSummaryGrid(false);
		summaryGrid.setBodyBorder(false);
		summaryGrid.setMaxRows(ClientUtil.CAUSALITY_MAX_DISPLAY_ITEMS);
		
		Button exploreBtn = new Button(ClientUtil.CLIENT_CONSTANTS.analyse());
		exploreBtn.setIcon(AbstractImagePrototype.create(ClientUtil.CLIENT_IMAGES.toolbar_explore()));
		exploreBtn.setStyleAttribute("margin-left", "15px");
		exploreBtn.addSelectionListener(new SelectionListener<ButtonEvent>()
		{
			@Override
            public void componentSelected(ButtonEvent ce)
			{
				// Fire a RequestViewEvent to open the causality view.
				// Create an item of evidence, whose ID matches the 'headline' notification
				// or time series feature ID, with the description and time set to those of
				// the incident shown in the widget.
				RequestViewEvent<EvidenceModel> rve = 
					new RequestViewEvent<EvidenceModel>(IncidentSummaryWidget.this);
				rve.setGroupBy(m_SummaryGrid.getGroupBy());
				
				EvidenceModel evidence = new EvidenceModel();
				evidence.setId(m_Incident.getEvidenceId());
				evidence.setDescription(m_Incident.getDescription());
				evidence.setTime(TimeFrame.SECOND, m_Incident.getTime());
				rve.setModel(evidence);
				rve.setView(m_ChartConfiguration);
				fireEvent(GXTEvents.OpenCausalityViewClick, rve);
			}
		});
		
		summaryGrid.getToolBar().add(exploreBtn);
		
		return summaryGrid;
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
             
		
		m_SummaryGrid.addStoreListener(new StoreListener<CausalityAggregateModel>(){

			@Override
            public void storeBeforeDataChanged(
                    StoreEvent<CausalityAggregateModel> se)
            {
				m_ChartContainer.mask(GXT.MESSAGES.loadMask_msg());
            }
			
            @Override
            public void storeDataChanged(StoreEvent<CausalityAggregateModel> se)
            {
            	loadCausalityChart();
            }
            
	    });
        
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
		m_ChartContainer.mask(GXT.MESSAGES.loadMask_msg());
		m_SummaryGrid.mask();
		
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
				m_ChartContainer.unmask();
				m_SummaryGrid.unmask();
			}


			@Override
            public void uponSuccess(CausalityView causalityView)
			{	
				GWT.log("IncidentSummaryWidget - loaded CausalityView: " + causalityView);
				m_ChartConfiguration = causalityView;
				
				m_ChartWidget.setPeakValuesByTypeId(causalityView.getPeakValuesByTypeId());
				m_SummaryGrid.loadForEvidenceId(m_Incident.getEvidenceId(), causalityView.getAttributes());
			}
		};
		
		m_CausalityQueryService.getViewConfiguration(m_Incident.getEvidenceId(), 
				ClientUtil.CAUSALITY_METRICS_TIME_SPAN, callback);
	}
	
	
	/**
	 * Loads the causality chart with the top items from the summary grid.
	 */
	protected void loadCausalityChart()
	{
		m_ChartWidget.removeAll();
		
		// Plot the top probable causes (by significance) on the chart,
    	Date incidentTime = m_Incident.getTime();	
		m_ChartWidget.setDateRange(
				new Date(incidentTime.getTime() - (ClientUtil.CAUSALITY_METRICS_TIME_SPAN*1000)/2), 
				new Date(incidentTime.getTime() + (ClientUtil.CAUSALITY_METRICS_TIME_SPAN*1000)/2));
		m_ChartWidget.setTimeMarker(incidentTime);

    	List<CausalityAggregateModel> topItems = m_SummaryGrid.getTopCausalityData(
    			ClientUtil.CAUSALITY_MAX_DISPLAY_ITEMS, m_Incident.getEvidenceId());
    	
    	CausalityDataModel topCausalityData;
    	for (CausalityAggregateModel aggregate : topItems)
    	{
    		topCausalityData = aggregate.getTopCausalityData();
    		if (topCausalityData != null)
    		{
    			m_ChartWidget.addCausalityData(topCausalityData);
    			
    			// Set properties to display the appropriate symbol.
    			String hexColor = m_ChartWidget.getLineColour(topCausalityData);
		    	if (hexColor != null)
		    	{
		    		String colorName = CSSColorChart.getInstance().getColorName(hexColor);
		    		aggregate.set(ChartSymbolCellRenderer.DEFAULT_COLOR_PROPERTY, colorName);
		    	}
		    	
		    	aggregate.set(ChartSymbolCellRenderer.DEFAULT_SHAPE_PROPERTY, 
		    			m_ChartWidget.getSymbolShape(topCausalityData));
		    	
		    	m_SummaryGrid.getStore().update(aggregate);
    		}
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
    	m_SummaryGrid.setHeight(m_ChartContainer.getInnerHeight());
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

}
