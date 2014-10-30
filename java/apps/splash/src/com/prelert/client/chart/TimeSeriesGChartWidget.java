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
import com.prelert.client.list.EvidenceAttributesDialog;
import com.prelert.data.CausalityViewTool;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import com.prelert.data.Evidence;
import com.prelert.data.ListViewTool;
import com.prelert.data.TimeFrame;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.TimeSeriesData;
import com.prelert.data.TimeSeriesDataPoint;
import com.prelert.data.TimeSeriesViewTool;
import com.prelert.data.Tool;
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
	
	private MenuItem					m_ShowDetailsMenuItem;
	private ViewMenuItem				m_ProbCauseMenuItem;
	
	
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
		
		m_TimeSeriesPagingService = AsyncServiceLocator.getInstance().getTimeSeriesGXTQueryService();
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
    public boolean isHighlightingFeatures()
    {
    	return m_HighlightFeatures;
    }


	/**
	 * Sets whether time series features should be highlighted on the chart.
     * @param markFeatues <code>true</code> if any features are to be highlighted, 
     * 	<code>false</false> otherwise.
     */
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
	public void addTimeSeries(TimeSeriesConfig timeSeriesConfig, String color)
	{
		if (timeSeriesConfig != null)
		{
			if (m_TimeSeriesConfigs.contains(timeSeriesConfig) == false) 
			{
				m_TimeSeriesConfigs.add(timeSeriesConfig);
			}
	
			if (color != null)
			{
				if (m_TimeSeriesColours == null)
				{
					m_TimeSeriesColours = new HashMap<TimeSeriesConfig, String>();
				}
				
				m_TimeSeriesColours.put(timeSeriesConfig, color);
			}
		}
	}
	
	
	/**
	 * Removes the time series from the chart.
	 * @param timeSeriesConfig TimeSeriesConfig object defining the properties of
	 * 		the time series to remove (data type, metric, source, attributes).
	 */
	public void removeTimeSeries(TimeSeriesConfig timeSeriesConfig)
	{
		m_TimeSeriesConfigs.remove(timeSeriesConfig);
		getChart().removeTimeSeries(timeSeriesConfig);
	}
	
	
	/**
	 * Adds the specified notification data into the chart. Note that this may 
	 * not represent a single item of evidence but a number of items aggregated by
	 * time and description.
	 * @param notification notification to add to the chart.
	 */
	public void addNotification(Evidence notification)
	{
		fireEvent(BeforeLoad, new LoadEvent(this, notification));
		
		getChart().addNotification(notification);
		getChart().update();
		
		LoadEvent evt = new LoadEvent(TimeSeriesGChartWidget.this, notification, notification);
	    fireEvent(Load, evt);
	}
	
	
	/**
	 * Removes the specified notification data into the chart. Note that this may 
	 * not represent a single item of evidence but a number of items aggregated by
	 * time and description.
	 * @param notification notification to remove from the chart.
	 */
	public void removeNotification(Evidence notification)
	{
		getChart().removeNotification(notification);
		getChart().update();
	}

	
	/**
	 * Removes all time series from the chart.
	 */
	public void removeAllTimeSeries()
	{
		m_TimeSeriesConfigs.clear();
		getChart().removeAllTimeSeries();
	}
	
	
	/**
	 * Removes all notifications from the chart.
	 */
    public void removeAllNotifications()
    {
    	getChart().removeAllNotifications();
    }


	/**
	 * Removes all data from the chart i.e. time series, notifications and
	 * time marker.
	 */
	public void removeAll()
	{
		m_TimeSeriesConfigs.clear();
		
		getChart().removeAll();
		getChart().update();
	}
	
    
    /**
     * Sets the list of tools for launching other views from the chart.
     * @param viewTools the list of tools for launching other view types.
     */
    public void setViewTools(List<Tool> viewTools)
    {
	    // Insert the view tools at the top of the existing menu (which should
    	// contain zoom in and zoom out items).
	    Menu menu = getChart().getContextMenu();
	    if (menu == null)
	    {
	    	menu = new Menu();
	    	getChart().setContextMenu(menu);
	    }
	    
	    m_ShowDetailsMenuItem = new MenuItem(ClientUtil.CLIENT_CONSTANTS.showDetails());
	    SelectionListener<MenuEvent> showDetailsListener = new SelectionListener<MenuEvent>()
	    {
			@Override
            public void componentSelected(MenuEvent ce)
            {
				int evidenceId = getSelectedNotificationId();
				if (evidenceId == -1)
				{
					evidenceId = getSelectedTimeSeriesFeatureId();
				}
				
				if (evidenceId > -1)
				{
					EvidenceAttributesDialog dialog = ClientUtil.getEvidenceAttributesDialog();
					dialog.setEvidenceId(evidenceId);
					dialog.show();
				}
            }
			
	    };
	    
	    m_ShowDetailsMenuItem.addSelectionListener(showDetailsListener);
	    menu.insert(m_ShowDetailsMenuItem, 0);
	    
	    if (viewTools != null)
	    {
	    	ViewMenuItem menuItem;
	    	SelectionListener<MenuEvent> menuItemLsnr = new SelectionListener<MenuEvent>()
    	    {
				@Override
                public void componentSelected(MenuEvent ce)
                {	
					ViewMenuItem vmenuItem = (ViewMenuItem)(ce.getItem());
					fireViewMenuItemEvent(vmenuItem);
                }
				
    	    };

    	    int itemIndex = 1;
    	    for (Tool tool : viewTools)
	    	{
	    	    menuItem = new ViewMenuItem(tool);
	    	    menuItem.addSelectionListener(menuItemLsnr);
	    	    
	    	    if (tool.getClass() == CausalityViewTool.class)
	    		{
		    	    m_ProbCauseMenuItem = menuItem;
		    	    menu.insert(m_ProbCauseMenuItem, 0);
	    		}
	    		else
	    		{
		    	    menu.insert(menuItem, itemIndex);
	    		}
	    	    
	    	    itemIndex++;
	    	}
    	    
    	    menu.insert(new SeparatorMenuItem(), itemIndex);
	    }

	    
	    // When the menu is shown, enable or disable the 'Show Details'
	    // and 'Show Probable Cause' items depending on what is selected.
	    // NB. GXT 2.2.0 and earlier - rendering errors occur if the menu items 
	    // are dynamically added or removed.
	    menu.addListener(Events.BeforeShow, new Listener<MenuEvent>()
		{
			@Override
            public void handleEvent(MenuEvent be)
            {	 
				if (getChart().getTouchedTimeSeriesFeatureId() !=  -1)
				{
					// Time series feature selected.
					m_ShowDetailsMenuItem.setEnabled(true);
					if (m_ProbCauseMenuItem != null)
            		{
            			m_ProbCauseMenuItem.setEnabled(true);
            		}
				}
				else
				{
					if (m_ProbCauseMenuItem != null)
            		{
            			m_ProbCauseMenuItem.setEnabled(false);
            		}
					
					if (getChart().getTouchedNotificationId() != -1)
	                {
						// Notification selected.
	                	m_ShowDetailsMenuItem.setEnabled(true);
	                }
					else
					{
						// Time series selected.
						m_ShowDetailsMenuItem.setEnabled(false);
					}
				}
            }
	
		});
    }
    
    
    /**
     * Returns the id of the notification that is currently 'selected' in the
     * chart e.g. when a context menu item has been run against a notification.
     * @return the id of the notification that is selected, or -1 if no notification
     * 		is currently selected.
     */
    public int getSelectedNotificationId()
    {
    	return getChart().getTouchedNotificationId();
    }
    
    
    /**
	 * Returns the time series that is currently 'selected' in the chart 
	 * e.g. when a context menu item has been run against a time series data point.
	 * @return TimeSeriesConfig object defining the properties of the time series
	 *  	or <code>null</code> if no time series is currently selected.
	 */
    public TimeSeriesConfig getSelectedTimeSeries()
    {
    	return getChart().getTouchedTimeSeries();
    }
    
    
    /**
     * Returns the id of the time series feature that is currently 'selected' in the
     * chart e.g. when a context menu item has been run against a feature.
     * @return the id of the feature that is selected, or -1 if no feature
     * 		is currently selected.
     */
    public int getSelectedTimeSeriesFeatureId()
    {
    	return getChart().getTouchedTimeSeriesFeatureId();
    }
    
    
    /**
	 * Fires an event from a ViewMenuItem against the selected point in the chart.
     * @param <M>
	 * @param viewMenuItem the ViewMenuItem that has been selected.
	 */
	@SuppressWarnings("unchecked")
    protected <M> void fireViewMenuItemEvent(ViewMenuItem viewMenuItem)
	{
		Tool tool = viewMenuItem.getTool();
		Date selectedTime = getSelectedTime();
		
		RequestViewEvent<M> rve = new RequestViewEvent<M>(m_Chart);
		
		// Note that GWT does not support reflection, so need to use getClass().
		if (tool.getClass() == CausalityViewTool.class)
		{
			// Get the selected feature (evidence) id. If no feature is selected, do nothing.
			int featureId = getSelectedTimeSeriesFeatureId();
			if (featureId != -1)
			{
				EvidenceModel model = new EvidenceModel();// Just set the id and time.
				model.setTime(TimeFrame.SECOND, selectedTime);
				model.setId(featureId); 
				rve.setModel((M) model);	
				fireEvent(GXTEvents.OpenCausalityViewClick, rve);
			}
		}
		else if (tool.getClass() == ListViewTool.class)
		{
			// Get the date/time of the 'selected' point. If nothing is selected, do nothing.
			if (selectedTime != null)
			{
				ListViewTool listViewTool = (ListViewTool)tool;
				String viewToOpen = listViewTool.getViewToOpen();
				DataSourceType dsType = new DataSourceType(
						viewToOpen, DataSourceCategory.NOTIFICATION);
				rve.setViewToOpenDataType(dsType);
				rve.setOpenAtTime(selectedTime);
				
				// Set the model in the RequestViewEvent.
				int evidenceId = getSelectedNotificationId();
				if (evidenceId > -1)
				{
					EvidenceModel model = new EvidenceModel();// Just set the id and time.
					model.setTime(TimeFrame.SECOND, selectedTime);
					model.setId(evidenceId); 
					rve.setModel((M) model);	
				}
				else 
				{
					TimeSeriesConfig timeSeries = getSelectedTimeSeries();
					if (timeSeries != null)
					{
						rve.setSourceName(timeSeries.getSource());
						rve.setModel((M) timeSeries);
					}
				}	
				
				fireEvent(GXTEvents.OpenNotificationViewClick, rve);
			}
		}
		else if (tool.getClass() == TimeSeriesViewTool.class)
		{
			// Get the date/time of the 'selected' point. If nothing is selected, do nothing.
			if (selectedTime != null)
			{	
				TimeSeriesViewTool timeSeriesViewTool = (TimeSeriesViewTool)tool;
				String viewToOpen = timeSeriesViewTool.getViewToOpen();
				DataSourceType dsType = new DataSourceType(
						viewToOpen, DataSourceCategory.TIME_SERIES);
				rve.setViewToOpenDataType(dsType);
				rve.setOpenAtTime(selectedTime);
				
				// Set the model in the RequestViewEvent.
				int evidenceId = getSelectedNotificationId();
				if (evidenceId > -1)
				{
					EvidenceModel model = new EvidenceModel();// Just set the id and time.
					model.setTime(TimeFrame.SECOND, selectedTime);
					model.setId(evidenceId); 
					rve.setModel((M) model);	
				}
				else 
				{
					TimeSeriesConfig timeSeries = getSelectedTimeSeries();
					if (timeSeries != null)
					{
						rve.setSourceName(timeSeries.getSource());
						rve.setModel((M) timeSeries);
					}
				}
				
				fireEvent(GXTEvents.OpenTimeSeriesViewClick, rve);
			}
		}
	
	}
    
    
    /**
     * Returns the line colour to use for the specified time series configuration.
     * @param config time series configuration for which to obtain the line colour.
     * @return CSS hex colour notation e.g. '#ff0000'. 
     */
    protected String getLineColour(TimeSeriesConfig config)
    {
    	String hexColour = null;
    	if (m_TimeSeriesColours != null)
    	{
    		hexColour = m_TimeSeriesColours.get(config);
    	}
    	
    	if (hexColour == null)
    	{
    		hexColour = CSSColorChart.getInstance().getColor(0);
    	}
    	
    	return hexColour;
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
					ClientUtil.CLIENT_CONSTANTS.errorLoadingTimeSeries(), null);
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
				m_Chart.update();
				
				if (m_StartTime == null && dataPoints.size() > 0)
				{
					m_StartTime = m_Chart.getDateAxisStart();
					m_EndTime = m_Chart.getDateAxisEnd();
				}
				
				LoadEvent evt = new LoadEvent(TimeSeriesGChartWidget.this, m_QueryConfig, dataPoints);
			    fireEvent(Load, evt);
			}
		}
    	
    }
    
}
