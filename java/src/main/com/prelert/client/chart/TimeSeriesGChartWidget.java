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
 * on +44 (0)20 7953 7243 or email to legal@prelert.com.    *
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

package com.prelert.client.chart;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.MenuEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.Component;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.extjs.gxt.ui.client.widget.menu.MenuItem;
import com.extjs.gxt.ui.client.widget.menu.SeparatorMenuItem;
import com.google.gwt.core.client.GWT;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.client.ClientUtil;
import com.prelert.client.CSSColorChart;
import com.prelert.client.event.GXTEvents;
import com.prelert.client.event.RequestViewEvent;
import com.prelert.client.gxt.ViewMenuItem;
import com.prelert.client.list.AttributeListDialog;
import com.prelert.data.DataSourceType;
import com.prelert.data.Evidence;
import com.prelert.data.TimeFrame;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.TimeSeriesData;
import com.prelert.data.TimeSeriesDataPoint;
import com.prelert.data.gxt.EvidenceModel;
import com.prelert.service.AsyncServiceLocator;
import com.prelert.service.TimeSeriesGXTPagingServiceAsync;


/**
 * Implementation of the TimeSeriesChartWidget which uses the GChart Google
 * Web Toolkit (GWT) extension for the charting component 
 * (see {@link http://code.google.com/p/clientsidegchart/}).
 * @author Pete Harverson
 */
public class TimeSeriesGChartWidget extends GChartWidget<TimeSeriesDataPoint> 
	implements TimeSeriesChartWidget
{
	private TimeSeriesGXTPagingServiceAsync	m_TimeSeriesPagingService;
	
	private List<TimeSeriesConfig>		m_TimeSeriesConfigs;
	
	private HashMap<TimeSeriesConfig, String>	m_TimeSeriesColours;
	
	private boolean						m_HighlightFeatures;
	
	private SelectionListener<MenuEvent>	m_ViewMenuItemListener;
	
	
	/**
	 * Creates a new time series chart widget which uses a GChart GWT component
	 * for plotting the time series data.
	 */
	public TimeSeriesGChartWidget()
	{
		this(new TimeSeriesGChart());
	}
	
	
	/**
	 * Creates a new time series chart widget using the specified TimeSeriesGChart 
	 * component for plotting the time-based data.
	 */
	protected TimeSeriesGChartWidget(TimeSeriesGChart timeSeriesChart)
	{
		super(timeSeriesChart);
		
		m_TimeSeriesConfigs = new ArrayList<TimeSeriesConfig>();
		m_TimeSeriesColours = new HashMap<TimeSeriesConfig, String>();
		
		m_TimeSeriesPagingService = AsyncServiceLocator.getInstance().getTimeSeriesGXTQueryService();
		
		initContextMenu();
	}
	
	
	/**
	 * Initialises the right-click context menu, adding items specific to time series charts.
	 */
	protected void initContextMenu()
	{
		// Add 'Show Analysis' and 'Show Details' items at the top of the 
    	// existing menu (which `will already contain zoom in and zoom out items).
	    Menu menu = getChart().getContextMenu();
	    
	    final MenuItem probCauseMenuItem = new MenuItem(ClientUtil.CLIENT_CONSTANTS.showAnalysis());
		SelectionListener<MenuEvent> showCauseListener = new SelectionListener<MenuEvent>()
	    {
			@Override
            public void componentSelected(MenuEvent ce)
            {
				// Get the selected feature (evidence) id. If no feature is selected, do nothing.
				Evidence feature = getChart().getTouchedTimeSeriesFeature();
				if (feature != null)
				{
					RequestViewEvent<EvidenceModel> rve = new RequestViewEvent<EvidenceModel>(m_Chart);
					
					EvidenceModel model = new EvidenceModel();// Just set the id and time.
					model.setTime(TimeFrame.SECOND, getSelectedTime());
					model.setId(feature.getId()); 
					model.setDescription(feature.getDescription());
					rve.setModel(model);	
					fireEvent(GXTEvents.OpenCausalityViewClick, rve);
				}
            }
			
	    };
	    
	    probCauseMenuItem.addSelectionListener(showCauseListener);
	    menu.insert(probCauseMenuItem, 0);	
	    
	    final MenuItem showDetailsMenuItem = new MenuItem(ClientUtil.CLIENT_CONSTANTS.showDetails());
	    SelectionListener<MenuEvent> showDetailsListener = new SelectionListener<MenuEvent>()
	    {
			@Override
            public void componentSelected(MenuEvent ce)
            {
				int evidenceId = getSelectedTimeSeriesFeatureId();	
				if (evidenceId != 0)
				{
					AttributeListDialog dialog = AttributeListDialog.getInstance();
					dialog.showEvidenceAttributes(evidenceId);
				}
				else
				{
					showDetailsOnSelectedSeries();
				}
            }
			
	    };
	    
	    showDetailsMenuItem.addSelectionListener(showDetailsListener);
	    menu.insert(showDetailsMenuItem, 1);
	    menu.insert(new SeparatorMenuItem(), 2);

	    
	    // When the menu is shown, enable or disable 'Show Analysis' 
	    // depending on whether a feature is selected.
	    // NB. GXT 2.2.0 and earlier - rendering errors occur if the menu items 
	    // are dynamically added or removed.
	    menu.addListener(Events.BeforeShow, new Listener<MenuEvent>()
		{
			@Override
            public void handleEvent(MenuEvent be)
            {	 
				if (getChart().getTouchedTimeSeriesFeature() !=  null)
				{
					probCauseMenuItem.setEnabled(true);
				}
				else
				{
					probCauseMenuItem.setEnabled(false);
				}
            }
	
		});
	}
	
	
	/**
	 * Returns the TimeSeriesGChart component which is displaying the time series 
	 * and notification data.
	 * @return the TimeSeriesGChart GWT widget.
	 */
    @Override
    public TimeSeriesGChart getChart()
    {
	    return (TimeSeriesGChart)m_Chart;
    }
    
    
    /**
	 * Loads all the data in the time series chart widget according to its 
	 * current configuration.
	 */
    @Override
	public boolean load()
	{
		for (TimeSeriesConfig config : m_TimeSeriesConfigs)
		{
			load(config);
		}
		
		return true;
	}


	/**
	 * Loads the data for the specified time series configuration into the
	 * chart. The lower and upper time bounds for the data loaded will be
	 * set according to the current time span of the chart.
	 * @param config configuration of the time series to load into the chart.
	 * 			This configuration will be added to those stored in the chart
	 * 			widget if it has not been added already.
	 */
	@Override
	public boolean load(Object config)
	{
		GWT.log("TimeSeriesGChartWidget load: " + config);
		
		if (config != null && config.getClass() == TimeSeriesConfig.class)
		{
			TimeSeriesConfig timeSeriesConfig = (TimeSeriesConfig)config;
			addTimeSeries(timeSeriesConfig);
			
			timeSeriesConfig.setMinTime(m_StartTime);
			timeSeriesConfig.setMaxTime(m_EndTime);
			
			fireEvent(BeforeLoad, new LoadEvent(this, config));
			
			TimeSeriesQueryCallback callback = new TimeSeriesQueryCallback(timeSeriesConfig);
			
			// Make the call to the Time Series query service.
			m_TimeSeriesPagingService.getDataPoints(
					timeSeriesConfig, m_HighlightFeatures, callback);
			
			return true;
		}
		else
		{
			return false;
		}
	}


	/**
	 * Returns whether time series features should be highlighted on the chart.
     * @return <code>true</code> if any features are to be highlighted, 
     * 	<code>false</false> otherwise.
     */
    @Override
	public boolean isHighlightingFeatures()
    {
    	return m_HighlightFeatures;
    }


	/**
	 * Sets whether time series features should be highlighted on the chart.
     * @param markFeatues <code>true</code> if any features are to be highlighted, 
     * 	<code>false</false> otherwise.
     */
    @Override
	public void setHighlightingFeatures(boolean highlight)
    {
    	m_HighlightFeatures = highlight;
    }
	
	
	/**
	 * Zooms in the date axis, centred around the specified time.
	 */
	@Override
    public void zoomInDateAxis(Date centreOnTime)
	{
		super.zoomInDateAxis(centreOnTime);

		if (m_TimeSeriesConfigs.size() > 0)
		{
			getChart().removeAllTimeSeries();
			load();
		}
		else
		{
			fireEvent(BeforeLoad, new LoadEvent(this));
			m_Chart.update();
			fireEvent(Load, new LoadEvent(this));
		}
	}
	
	
	/**
	 * Zooms out the date axis, centred around the current midpoint of the date range.
	 */
	@Override
    public void zoomOutDateAxis(Date centreOnTime)
	{	
		super.zoomOutDateAxis(centreOnTime);
		
		if (m_TimeSeriesConfigs.size() > 0)
		{
			getChart().removeAllTimeSeries();
			load();
		}
		else
		{
			fireEvent(BeforeLoad, new LoadEvent(this));
			m_Chart.update();
			fireEvent(Load, new LoadEvent(this));
		}
	}
	
	
	/**
	 * Pans the chart to the left.
	 */
	@Override
    public void panLeft()
	{
		super.panLeft();

		getChart().removeAllTimeSeries();
		load();
	}
	
	
	/**
	 * Pans the chart to the right.
	 */
	@Override
    public void panRight()
	{
		super.panRight();
		getChart().removeAllTimeSeries();
		
		load();
	}


	/**
	 * Adds a time series configuration to the chart, with the line connecting the
	 * data points set to the default line colour. Note that a separate call 
	 * is needed to load the data into the chart.
	 * @param timeSeriesConfig TimeSeriesConfig object defining the properties of
	 * 		the time series to display (data type, metric, source, attributes etc).
	 */
	@Override
	public void addTimeSeries(TimeSeriesConfig timeSeriesConfig)
	{		
		addTimeSeries(timeSeriesConfig, null);
	}
	
	
	/**
	 * Adds a time series to the chart, with the line connecting the data points
	 * drawn in the specified colour.
	 * @param timeSeriesConfig TimeSeriesConfig object defining the properties of
	 * 		the time series to display (data type, metric, source, attributes etc).
	 * @param color  line colour, specified using the CSS hex colour notation e.g '#ff0000'. 
	 */
	@Override
	public void addTimeSeries(TimeSeriesConfig timeSeriesConfig, String color)
	{
		if (timeSeriesConfig != null)
		{
			if (m_TimeSeriesConfigs.contains(timeSeriesConfig) == false) 
			{
				m_TimeSeriesConfigs.add(timeSeriesConfig);
			}

			String lineColor = color;
			if (lineColor == null)
	    	{
				lineColor = getLineColour(timeSeriesConfig);
				if (lineColor == null)
				{
		    		// Find the next available line colour from the chart.
		    		CSSColorChart colorChart = CSSColorChart.getInstance();
		    		int numColoursInChart = colorChart.getNumberOfColors();
		    		for (int i = 0; i < numColoursInChart; i++)
		    		{
		    			lineColor = colorChart.getColor(i);
		    			if (m_TimeSeriesColours.containsValue(lineColor) == false)
		    			{
		    				break;
		    			}
		    		}
				}
	    	}

			m_TimeSeriesColours.put(timeSeriesConfig, lineColor);
			
		}
	}
	
	
	/**
	 * Removes the time series from the chart, if it is currently being displayed.
	 * @param timeSeriesConfig TimeSeriesConfig object defining the properties of
	 * 		the time series to remove (data type, metric, source, attributes).
	 */
	@Override
	public void removeTimeSeries(TimeSeriesConfig timeSeriesConfig)
	{
		boolean onChart = m_TimeSeriesConfigs.remove(timeSeriesConfig);
		if (onChart == true)
		{
			m_TimeSeriesColours.remove(timeSeriesConfig);
			getChart().removeTimeSeries(timeSeriesConfig);
		}
	}

	
	/**
	 * Removes all time series from the chart.
	 */
	@Override
	public void removeAllTimeSeries()
	{
		m_TimeSeriesConfigs.clear();
		m_TimeSeriesColours.clear();
		getChart().removeAllTimeSeries();
		getChart().update();
	}


	/**
	 * Removes all data from the chart i.e. time series, notifications and
	 * time marker.
	 */
	@Override
	public void removeAll()
	{
		m_TimeSeriesConfigs.clear();
		m_TimeSeriesColours.clear();
		getChart().removeAll();
		getChart().update();
	}
	

    @Override
    public void setLinkToDataTypes(List<DataSourceType> dataSourceTypes)
    {
    	if (dataSourceTypes.size() > 0)
    	{
    		ViewMenuItem menuItem;
		    Menu chartMenu = getChart().getContextMenu();
		    int itemIndex = 3; // After Show Probable Cause, Show Details and separator.
    		
    		if (m_ViewMenuItemListener == null)
    		{
    			m_ViewMenuItemListener = new SelectionListener<MenuEvent>()
    		    {
    				@Override
    	            public void componentSelected(MenuEvent ce)
    	            {	
    					ViewMenuItem vmenuItem = (ViewMenuItem)(ce.getItem());
    					fireViewMenuItemEvent(vmenuItem);
    	            }    				
    		    };
    		    
    		    chartMenu.insert(new SeparatorMenuItem(), itemIndex);
    		}
    		else
	    	{
	    		// Remove any existing view menu items.
	    		List<Component> menuItems = chartMenu.getItems();
	    		List<Component> viewMenuItems = new ArrayList<Component>();
	    		for(Component item : menuItems)
	    		{
	    			if (item.getClass() == ViewMenuItem.class)
	    			{
	    				viewMenuItems.add(item);
	    			}
	    		}
	    		
	    		for(Component viewMenuItem : viewMenuItems)
	    		{
	    			chartMenu.remove(viewMenuItem);
	    		}	 
	    	}
		    
    		// Add an item for each type to the end of context menu.
		    for (DataSourceType dataType : dataSourceTypes)
	    	{
	    	    menuItem = new ViewMenuItem(dataType);
	    	    menuItem.addSelectionListener(m_ViewMenuItemListener);
	    	    chartMenu.insert(menuItem, itemIndex);
	    	    
	    	    itemIndex++;
	    	}
		    
		    
    	}
    	
    }
    
    
    /**
	 * Returns the time series that is currently 'selected' in the chart 
	 * e.g. when a context menu item has been run against a time series data point.
	 * @return TimeSeriesConfig object defining the properties of the time series
	 *  	or <code>null</code> if no time series is currently selected.
	 */
    @Override
	public TimeSeriesConfig getSelectedTimeSeries()
    {
    	return getChart().getTouchedTimeSeries();
    }
    
    
    @Override
    public int getSelectedTimeSeriesFeatureId()
    {
    	int evidenceId = 0;
    	Evidence feature = getChart().getTouchedTimeSeriesFeature();
    	if (feature != null)
    	{
    		evidenceId = feature.getId();
    	}
    	return evidenceId;
    }
    
    
    /**
	 * Opens the 'Show Attributes' dialog to display the details of the selected series.
	 */
    protected void showDetailsOnSelectedSeries()
    {
    	TimeSeriesConfig config = getSelectedTimeSeries();
    	
    	if (config != null)
    	{
    		Date pointTime = getChart().getTouchedPointTime();
	    	double pointValue = getChart().getTouchedPointDataValue();
	    	TimeSeriesDataPoint point = new TimeSeriesDataPoint(pointTime.getTime(), pointValue);
	    	
	    	AttributeListDialog dialog = AttributeListDialog.getInstance();
			dialog.showTimeSeriesAttributes(config, point);
    	}
    }
    
    
    /**
	 * Fires an event from a ViewMenuItem against the selected point in the chart.
     * @param <M>
	 * @param viewMenuItem the ViewMenuItem that has been selected.
	 */
	@SuppressWarnings("unchecked")
    protected <M> void fireViewMenuItemEvent(ViewMenuItem viewMenuItem)
	{	
		// Get the date/time of the 'selected' point. If nothing is selected, do nothing.
		Date selectedTime = getSelectedTime();
		if (selectedTime != null)
		{
			RequestViewEvent<M> rve = new RequestViewEvent<M>(m_Chart);
			
			DataSourceType dataType = viewMenuItem.getDataType();
			rve.setViewToOpenDataType(dataType);
			rve.setOpenAtTime(selectedTime);
			
			// Set the model in the RequestViewEvent.
			TimeSeriesConfig timeSeries = getSelectedTimeSeries();
			if (timeSeries != null)
			{
				rve.setSourceName(timeSeries.getSource());
				rve.setModel((M) timeSeries);
			}
			
			switch (dataType.getDataCategory())
			{
				case NOTIFICATION :
					fireEvent(GXTEvents.OpenNotificationViewClick, rve);
					break;

				case TIME_SERIES:
					fireEvent(GXTEvents.OpenTimeSeriesViewClick, rve);
					break;
					
				case TIME_SERIES_FEATURE:
					break;
			}
		}
	
	}
    
    
    @Override
    public String getLineColour(TimeSeriesConfig config)
    {
    	return m_TimeSeriesColours.get(config);
    }
    
    
    /**
 	 * Response handler for time series queries. 
 	 * Before adding the results to the chart, the handler checks
 	 * that its configuration has not been cleared since the query was made.
     */
    class TimeSeriesQueryCallback extends ApplicationResponseHandler<List<TimeSeriesDataPoint>>
    {
    	private TimeSeriesConfig	m_QueryConfig;
    	
    	public TimeSeriesQueryCallback(TimeSeriesConfig config)
    	{
    		m_QueryConfig = config;
    	}
    	
		@Override
        public void uponFailure(Throwable caught)
		{
			GWT.log("Error loading time series.", caught);
			
			LoadEvent evt = new LoadEvent(TimeSeriesGChartWidget.this, m_QueryConfig, caught);
		    fireEvent(LoadException, evt);
			
			MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(), 
					ClientUtil.CLIENT_CONSTANTS.errorLoadingTimeSeriesData(), null);
		}


		@Override
        public void uponSuccess(List<TimeSeriesDataPoint> dataPoints)
		{
			// Check that we still want these results i.e. this configuration 
			// hasn't been cleared since these points were requested.
			if (m_TimeSeriesConfigs.contains(m_QueryConfig) == true)
			{	
				GWT.log("Points added to TimeSeriesGChartWidget: " + dataPoints.size(), null);
				String lineColour = getLineColour(m_QueryConfig);
				if (m_QueryConfig.getScalingFactor() != 1)
				{
					for (TimeSeriesDataPoint point : dataPoints)
					{	
						point.setValue(point.getValue() * m_QueryConfig.getScalingFactor());
					}
				}
				
				getChart().addTimeSeries(
						new TimeSeriesData(m_QueryConfig, dataPoints), lineColour);
				
				if (m_StartTime == null && dataPoints.size() > 0)
				{
					m_StartTime = m_Chart.getDateAxisStart();
					m_EndTime = m_Chart.getDateAxisEnd();
					
					setDateRange(m_StartTime, m_EndTime);
				}
				
				m_Chart.update();
				
				LoadEvent evt = new LoadEvent(TimeSeriesGChartWidget.this, m_QueryConfig, dataPoints);
			    fireEvent(Load, evt);
			}
		}
    	
    }
    
}
