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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.extjs.gxt.ui.client.data.BaseModelData;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.BaseObservable;
import com.extjs.gxt.ui.client.event.EventType;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.MenuEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.util.DateWrapper;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.extjs.gxt.ui.client.widget.menu.MenuItem;
import com.extjs.gxt.ui.client.widget.menu.SeparatorMenuItem;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.Widget;

import demo.app.client.ApplicationResponseHandler;
import demo.app.client.CSSColorChart;
import demo.app.client.ClientUtil;
import demo.app.client.ViewMenuItem;
import demo.app.client.ViewWidget;
import demo.app.client.event.GXTEvents;
import demo.app.client.event.RequestViewEvent;
import demo.app.client.event.ViewMenuItemListener;
import demo.app.data.CausalityViewTool;
import demo.app.data.DataSourceCategory;
import demo.app.data.DataSourceType;
import demo.app.data.Evidence;
import demo.app.data.ListViewTool;
import demo.app.data.TimeFrame;
import demo.app.data.TimeSeriesConfig;
import demo.app.data.TimeSeriesData;
import demo.app.data.TimeSeriesDataPoint;
import demo.app.data.Tool;
import demo.app.data.UsageViewTool;
import demo.app.data.gxt.EvidenceModel;
import demo.app.service.TimeSeriesGXTPagingServiceAsync;
import demo.app.splash.service.QueryServiceLocator;


/**
 * Implementation of the TimeSeriesChartWidget which uses the GChart Google
 * Web Toolkit (GWT) extension for the charting component 
 * (see {@link http://code.google.com/p/gchart/}).
 * @author Pete Harverson
 */
public class TimeSeriesGChartWidget implements TimeSeriesChartWidget
{
	private TimeSeriesGXTPagingServiceAsync	m_TimeSeriesPagingService;
	
	private List<TimeSeriesConfig>		m_TimeSeriesConfigs;
	
	private HashMap<TimeSeriesConfig, String>	m_TimeSeriesColours;
	
	private Date						m_StartTime;
	private Date						m_EndTime;
	private boolean						m_HighlightFeatures;
	
	private TimeSeriesGChart			m_Chart;
	private String						m_TitleText;
	private String						m_SubtitleText;
	
	private ViewMenuItem				m_ProbCauseMenuItem;
	
	private BaseObservable 				m_Observable;
	
	
	/**
	 * Creates a new time series chart widget which uses a GChart GWT component
	 * for plotting the time series data.
	 */
	public TimeSeriesGChartWidget()
	{
		m_TimeSeriesConfigs = new ArrayList<TimeSeriesConfig>();
		
		m_TimeSeriesPagingService = QueryServiceLocator.getInstance().getTimeSeriesGXTQueryService();
		
		m_Chart = new TimeSeriesGChart();
		
		m_Observable = new BaseObservable();
	}
	
	
	/**
	 * Returns the user interface Widget sub-class itself.
	 * @return the Widget which is added in the user interface.
	 */
	public Widget getChartWidget()
	{
		return m_Chart;
	}
	
	
	/**
	 * Sets the width of the chart, which should include space for y-axis 
	 * tick marks and labels.
	 * @param width width of the chart.
	 */
    public void setChartWidth(int width)
    {
    	// Account for y-axis ticks and labels.
    	int xChartSize = width - (m_Chart.getYAxis().getTickLabelThickness()+20);
    	if (xChartSize < 100)
    	{
    		xChartSize = 100;
    	}
    	
    	m_Chart.setXChartSize(xChartSize);
	
    	// Update chart in DeferredCommand as otherwise time series lines
		// may not be re-validated correctly i.e. they show up grey.
		DeferredCommand.addCommand(new Command()
		{
			public void execute()
			{
				m_Chart.update();
			}
		});

    }
    
    
    /**
     * Sets the height of the chart, which should include space for x-axis 
	 * tick marks and labels.
     * @param height height of the chart.
     */
    public void setChartHeight(int height)
    {
    	// Account for y-axis labels.
       	int yChartSize = height - (m_Chart.getXAxis().getAxisLabelThickness());
    	if (yChartSize < 100)
    	{
    		yChartSize = 100;
    	}
    	
    	m_Chart.setYChartSize(yChartSize);
    	m_Chart.update();
    }
    
    
    /**
	 * Loads all the data in the time series chart widget according to its 
	 * current configuration.
	 */
	public void load()
	{
		for (TimeSeriesConfig config : m_TimeSeriesConfigs)
		{
			load(config);
		}
	}


	/**
	 * Loads the data for the specified time series configuration into the
	 * chart. The lower and upper time bounds for the data loaded will be
	 * set according to the current time span of the chart.
	 * @param config configuration of the time series to load into the chart.
	 * 			This configuration will be added to those stored in the chart
	 * 			widget if it has not been added already.
	 */
	public void load(TimeSeriesConfig config)
	{
		if (config != null)
		{
			addTimeSeries(config);
			
			config.setMinTime(m_StartTime);
			config.setMaxTime(m_EndTime);
			
			TimeSeriesQueryCallback callback = new TimeSeriesQueryCallback(config);
			
			// Make the call to the Time Series query service.
			m_TimeSeriesPagingService.getDataPoints(
					config, m_HighlightFeatures, callback);
		}
	}
	
	
	/**
	 * Sets the flag that determines whether or not the tick labels on the value
	 * axis are visible.
	 * @param visible <code>true</code> for the tick labels to be visible (the
	 * 	default), <code>false</code> otherwise.
	 */
	public void setValueTickLabelsVisible(boolean visible)
	{
		if (visible == false)
		{
			m_Chart.getYAxis().setTickLabelThickness(30);
			m_Chart.getYAxis().setTickLabelFontColor("white");
		}
		else
		{
			m_Chart.getYAxis().setTickLabelThickness(100);
			m_Chart.getYAxis().setTickLabelFontColor(
					TimeSeriesGChart.DEFAULT_TICK_LABEL_FONT_COLOR);
		}
	}
	
	
	/**
	 * Sets a flag that determines whether or not the value axis range is 
	 * automatically adjusted to fit the data.
	 * @param auto auto range flag.
	 */
	public void setAutoValueRange(boolean auto)
	{
		m_Chart.setAutoValueRange(auto);
	}
	
	
	/**
	 * Sets the value range for the chart. This defines the lower and upper bounds
	 * displayed on the value (y) axis.
	 * @param minValue minimum value visible on the value axis.
	 * @param maxValue maximum value visible on the value axis.
	 */
	public void setValueRange(double minValue, double maxValue)
	{
		m_Chart.getYAxis().setAxisMin(minValue);
		m_Chart.getYAxis().setAxisMax(maxValue);
	}


	/**
	 * Sets the date range for the chart. This defines the start and end times
	 * of the time series data to load.
	 * @param startTime start time for the chart.
	 * @param endTime end time for the chart.
	 */
	public void setDateRange(Date startTime, Date endTime)
	{
		m_StartTime = startTime;
		m_EndTime = endTime;
		
		m_Chart.setDateAxisStart(m_StartTime);
		m_Chart.setDateAxisEnd(m_EndTime);
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
	 * Zooms in the date axis, centred around the current midpoint of the date range.
	 */
	public void zoomInDateAxis()
	{
		Date startTime = m_Chart.getDateAxisStart();
		Date endTime = m_Chart.getDateAxisEnd();
			
		Date midPoint = new Date((endTime.getTime() + startTime.getTime())/2);
		
		zoomInDateAxis(midPoint);
	}
	
	
	/**
	 * Zooms in the date axis, centred around the specified time.
	 */
	public void zoomInDateAxis(Date centreOnTime)
	{
		Date startTime = m_Chart.getDateAxisStart();
		Date endTime = m_Chart.getDateAxisEnd();
			
		long intervalInMins = (endTime.getTime() - startTime.getTime())/60000l;
		
		Date zoomStart = null;
		Date zoomEnd = null;
		
		// Zoom levels are:
		// 4 weeks - 40320 mins
		// 2 weeks - 20160 mins
		// 1 week - 10080 mins
		// 4 days - 5760 mins
		// 2 days - 2880 mins
		// 1 day - 1440 mins
		// 12 hours - 720 mins
		// 6 hours - 360 mins
		// 3 hours - 180 mins
		// 1 hour - 60 mins
		// 30 mins.
		if (intervalInMins <= 60)
		{
			zoomStart = (new DateWrapper(centreOnTime)).addMinutes(-15).asDate();
			zoomEnd = (new DateWrapper(centreOnTime)).addMinutes(15).asDate();
		}
		else if (intervalInMins <= 180)
		{
			zoomStart = (new DateWrapper(centreOnTime)).addMinutes(-30).asDate();
			zoomEnd = (new DateWrapper(centreOnTime)).addMinutes(30).asDate();
		}
		else if (intervalInMins <= 360)
		{
			zoomStart = (new DateWrapper(centreOnTime)).addMinutes(-90).asDate();
			zoomEnd = (new DateWrapper(centreOnTime)).addMinutes(90).asDate();
		}
		else if (intervalInMins <= 720)
		{
			zoomStart = (new DateWrapper(centreOnTime)).addHours(-3).asDate();
			zoomEnd = (new DateWrapper(centreOnTime)).addHours(3).asDate();
		}
		else if (intervalInMins <= 1440)
		{
			zoomStart = (new DateWrapper(centreOnTime)).addHours(-6).asDate();
			zoomEnd = (new DateWrapper(centreOnTime)).addHours(6).asDate();
		}
		else if (intervalInMins <= 2880)
		{
			zoomStart = (new DateWrapper(centreOnTime)).addHours(-12).asDate();
			zoomEnd = (new DateWrapper(centreOnTime)).addHours(12).asDate();
		}
		else if (intervalInMins <= 5760)
		{
			zoomStart = (new DateWrapper(centreOnTime)).addDays(-1).asDate();
			zoomEnd = (new DateWrapper(centreOnTime)).addDays(1).asDate();
		}
		else if (intervalInMins <= 10080)
		{
			zoomStart = (new DateWrapper(centreOnTime)).addDays(-2).asDate();
			zoomEnd = (new DateWrapper(centreOnTime)).addDays(2).asDate();
		}
		else if (intervalInMins <= 20160)
		{
			zoomStart = (new DateWrapper(centreOnTime)).addHours(-84).asDate();
			zoomEnd = (new DateWrapper(centreOnTime)).addHours(84).asDate();
		}
		else if (intervalInMins <= 40320)
		{
			zoomStart = (new DateWrapper(centreOnTime)).addDays(-7).asDate();
			zoomEnd = (new DateWrapper(centreOnTime)).addDays(7).asDate();
		}
		
		if (zoomStart != null)
		{
			setDateRange(zoomStart, zoomEnd);
			
			if (m_TimeSeriesConfigs.size() > 0)
			{
				m_Chart.removeAllTimeSeries();
				load();
			}
			else
			{
				m_Chart.update();
			}
		}
	}
	
	
	/**
	 * Zooms out the date axis, centred around the current midpoint of the date range.
	 */
	public void zoomOutDateAxis()
	{
		Date startTime = m_Chart.getDateAxisStart();
		Date endTime = m_Chart.getDateAxisEnd();
			
		Date midPoint = new Date((endTime.getTime() + startTime.getTime())/2);
		
		zoomOutDateAxis(midPoint);
	}
	
	
	/**
	 * Zooms out the date axis, centred around the current midpoint of the date range.
	 */
	public void zoomOutDateAxis(Date centreOnTime)
	{
		Date startTime = m_Chart.getDateAxisStart();
		Date endTime = m_Chart.getDateAxisEnd();
			
		long intervalInMins = (endTime.getTime() - startTime.getTime())/60000l;
		
		Date zoomStart = null;
		Date zoomEnd = null;
		
		// Zoom levels are:
		// 4 weeks - 40320 mins
		// 2 weeks - 20160 mins
		// 1 week - 10080 mins
		// 4 days - 5760 mins
		// 2 days - 2880 mins
		// 1 day - 1440 mins
		// 12 hours - 720 mins
		// 6 hours - 360 mins
		// 3 hours - 180 mins
		// 1 hour - 60 mins
		// 30 mins.
		if (intervalInMins <= 30)
		{
			zoomStart = (new DateWrapper(centreOnTime)).addMinutes(-30).asDate();
			zoomEnd = (new DateWrapper(centreOnTime)).addMinutes(30).asDate();
		}
		else if (intervalInMins <= 60)
		{
			zoomStart = (new DateWrapper(centreOnTime)).addMinutes(-90).asDate();
			zoomEnd = (new DateWrapper(centreOnTime)).addMinutes(90).asDate();
		}
		else if (intervalInMins <= 180)
		{
			zoomStart = (new DateWrapper(centreOnTime)).addHours(-3).asDate();
			zoomEnd = (new DateWrapper(centreOnTime)).addHours(3).asDate();
		}
		else if (intervalInMins <= 360)
		{
			zoomStart = (new DateWrapper(centreOnTime)).addHours(-6).asDate();
			zoomEnd = (new DateWrapper(centreOnTime)).addHours(6).asDate();
		}
		else if (intervalInMins <= 720)
		{
			zoomStart = (new DateWrapper(centreOnTime)).addHours(-12).asDate();
			zoomEnd = (new DateWrapper(centreOnTime)).addHours(12).asDate();
		}
		else if (intervalInMins <= 1440)
		{
			zoomStart = (new DateWrapper(centreOnTime)).addDays(-1).asDate();
			zoomEnd = (new DateWrapper(centreOnTime)).addDays(1).asDate();
		}
		else if (intervalInMins <= 2880)
		{
			zoomStart = (new DateWrapper(centreOnTime)).addDays(-2).asDate();
			zoomEnd = (new DateWrapper(centreOnTime)).addDays(2).asDate();
		}
		else if (intervalInMins <= 5760)
		{
			zoomStart = (new DateWrapper(centreOnTime)).addHours(-84).asDate();
			zoomEnd = (new DateWrapper(centreOnTime)).addHours(84).asDate();
		}
		else if (intervalInMins <= 10080)
		{
			zoomStart = (new DateWrapper(centreOnTime)).addDays(-7).asDate();
			zoomEnd = (new DateWrapper(centreOnTime)).addDays(7).asDate();
		}
		else if (intervalInMins <= 20160)
		{
			zoomStart = (new DateWrapper(centreOnTime)).addDays(-14).asDate();
			zoomEnd = (new DateWrapper(centreOnTime)).addDays(14).asDate();
		}
		
		if (zoomStart != null)
		{
			setDateRange(zoomStart, zoomEnd);
			
			if (m_TimeSeriesConfigs.size() > 0)
			{
				m_Chart.removeAllTimeSeries();
				load();
			}
			else
			{
				m_Chart.update();
			}
		}
	}
	
	
	/**
	 * Pans the chart to the left.
	 */
	public void panLeft()
	{
		Date startTime = m_Chart.getDateAxisStart();
		Date endTime = m_Chart.getDateAxisEnd();
			
		long intervalInMins = endTime.getTime() - startTime.getTime();
		
		Date panStart = new Date(startTime.getTime() - (intervalInMins/2));
		Date panEnd = new Date(endTime.getTime() - (intervalInMins/2));
		
		setDateRange(panStart, panEnd);
		m_Chart.removeAllTimeSeries();
		load();
	}
	
	
	/**
	 * Pans the chart to the right.
	 */
	public void panRight()
	{
		Date startTime = m_Chart.getDateAxisStart();
		Date endTime = m_Chart.getDateAxisEnd();
			
		long intervalInMins = endTime.getTime() - startTime.getTime();
		
		Date panStart = new Date(startTime.getTime() + (intervalInMins/2));
		Date panEnd = new Date(endTime.getTime() + (intervalInMins/2));
		
		setDateRange(panStart, panEnd);
		m_Chart.removeAllTimeSeries();
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
		m_Chart.removeTimeSeries(timeSeriesConfig);
	}
	
	
	/**
	 * Adds the specified notification data into the chart. Note that this may 
	 * not represent a single item of evidence but a number of items aggregated by
	 * time and description.
	 * @param notification notification to add to the chart.
	 */
	public void addNotification(Evidence notification)
	{
		m_Chart.addNotification(notification);
		m_Chart.update();
	}
	
	
	/**
	 * Removes the specified notification data into the chart. Note that this may 
	 * not represent a single item of evidence but a number of items aggregated by
	 * time and description.
	 * @param notification notification to remove from the chart.
	 */
	public void removeNotification(Evidence notification)
	{
		m_Chart.removeNotification(notification);
		m_Chart.update();
	}
	
	
	
	/**
	 * Removes all time series from the chart.
	 */
	public void removeAllTimeSeries()
	{
		m_TimeSeriesConfigs.clear();
		m_Chart.removeAllTimeSeries();
	}
	
	
	/**
	 * Removes all notifications from the chart.
	 */
    public void removeAllNotifications()
    {
    	m_Chart.removeAllNotifications();
    }


	/**
	 * Removes all data from the chart i.e. time series, notifications and
	 * time marker.
	 */
	public void removeAll()
	{
		m_TimeSeriesConfigs.clear();
		
		m_Chart.removeAll();
		m_Chart.update();
	}
	
	
	/**
	 * Adds a marker to the chart to highlight a particular time interval.
	 * @param startTime the lower bound of the time interval to mark.
	 * @param endTime the upper bound of the time interval to mark.
	 */
	public void setTimeMarker(Date startTime, Date endTime)
	{
		m_Chart.setTimeMarker(startTime, endTime);
	}
	
	
	/**
	 * Clears a time marker from the chart, if one has been added.
	 */
	public void clearTimeMarker()
	{
		m_Chart.clearTimeMarker();
	}


	/**
     * Sets the chart title to the specified text.
     * @param text the text for the title.
     */
    public void setChartTitle(String text)
    {
    	m_TitleText = text;
    	m_Chart.setTitleText(m_TitleText, m_SubtitleText);
    }
    
    
    /**
     * Sets the chart subtitle to the specified text.
     * @param text the text for the subtitle).
     */
    public void setChartSubtitle(String text)
    {
    	m_SubtitleText = text;
    	m_Chart.setTitleText(m_TitleText, m_SubtitleText);
    }
    
    
    /**
     * Sets the list of tools for launching other views from the chart.
     * @param viewTools the list of tools for launching other view types.
     */
    public void setViewTools(List<Tool> viewTools)
    {
	    // Configure the right-click context menu.
	    Menu menu = new Menu();
	    
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
	    	
    	    for (Tool tool : viewTools)
	    	{
	    	    menuItem = new ViewMenuItem(tool);
	    	    menuItem.addSelectionListener(menuItemLsnr);
	    	    
	    	    if (tool.getClass() == CausalityViewTool.class)
	    		{
		    	    m_ProbCauseMenuItem = menuItem;
	    		}
	    		else
	    		{
		    	    menu.add(menuItem);
	    		}
	    	}
    	    
    	    menu.add(new SeparatorMenuItem());
	    }

	    // Add Zoom in and Zoom out items.
	    MenuItem zoomIn = new MenuItem(ClientUtil.CLIENT_CONSTANTS.zoomInMenuItem());
	    zoomIn.addSelectionListener(new SelectionListener<MenuEvent>(){

            public void componentSelected(MenuEvent ce)
            {
	            Date selectedTime = getSelectedTime();
	            if (selectedTime != null)
	            {
	            	zoomInDateAxis(selectedTime);
	            }
            }
	    });
	    
	    MenuItem zoomOut = new MenuItem(ClientUtil.CLIENT_CONSTANTS.zoomOutMenuItem());
	    zoomOut.addSelectionListener(new SelectionListener<MenuEvent>(){

            public void componentSelected(MenuEvent ce)
            {
	            Date selectedTime = getSelectedTime();
	            if (selectedTime != null)
	            {
	            	zoomOutDateAxis(selectedTime);
	            }
            }
	    });
	    
	    menu.add(zoomIn);
	    menu.add(zoomOut);
	    
	    
	    m_Chart.setContextMenu(menu);
	    
	    if (m_ProbCauseMenuItem != null)
	    {
		    menu.addListener(Events.BeforeShow, new Listener<MenuEvent>()
			{
				@Override
	            public void handleEvent(MenuEvent be)
	            {
	                Menu chartMenu = be.getContainer();
	                if (m_Chart.getTouchedTimeSeriesFeatureId() !=  -1)
	                {
	                	chartMenu.insert(m_ProbCauseMenuItem, 0);
	                }   
	            }
		
			});
		    
		    
		    menu.addListener(Events.Hide, new Listener<MenuEvent>()
			{
				@Override
	            public void handleEvent(MenuEvent be)
	            {
					Menu chartMenu = be.getContainer();
					if (chartMenu.indexOf(m_ProbCauseMenuItem) != -1)
					{
						chartMenu.remove(m_ProbCauseMenuItem);
					}
	            }
		
			});
	    }
    }
    
    
    /**
     * Returns the date/time of the point in the chart that is currently 'selected' 
     * e.g. when a context menu item has been run against a point in the chart.
     * @return the date/time of the selected point, or <code>null</code> if no
     * 		point is currently selected.
     */
    public Date getSelectedTime()
    {
    	return m_Chart.getTouchedPointTime();
    }
    
    
    /**
     * Returns the id of the notification that is currently 'selected' in the
     * chart e.g. when a context menu item has been run against a notification.
     * @return the id of the notification that is selected, or -1 if no notification
     * 		is currently selected.
     */
    public int getSelectedNotificationId()
    {
    	return m_Chart.getTouchedNotificationId();
    }
    
    
    /**
	 * Returns the time series that is currently 'selected' in the chart 
	 * e.g. when a context menu item has been run against a time series data point.
	 * @return TimeSeriesConfig object defining the properties of the time series
	 *  	or <code>null</code> if no time series is currently selected.
	 */
    public TimeSeriesConfig getSelectedTimeSeries()
    {
    	return m_Chart.getTouchedTimeSeries();
    }
    
    
    /**
     * Returns the id of the time series feature that is currently 'selected' in the
     * chart e.g. when a context menu item has been run against a feature.
     * @return the id of the feature that is selected, or -1 if no feature
     * 		is currently selected.
     */
    public int getSelectedTimeSeriesFeatureId()
    {
    	return m_Chart.getTouchedTimeSeriesFeatureId();
    }
    
    
    public void addListener(EventType eventType, Listener listener)
	{
		m_Observable.addListener(eventType, listener);
	}


    public boolean fireEvent(EventType eventType, BaseEvent be)
    {
	    return m_Observable.fireEvent(eventType, be);
    }
    
    
    public List<Listener<? extends BaseEvent>> getListeners(EventType eventType)
    {
    	return m_Observable.getListeners(eventType);
    }


	public boolean hasListeners()
    {
	    return m_Observable.hasListeners();
    }


    public boolean hasListeners(EventType eventType)
    {
	    return m_Observable.hasListeners(eventType);
    }


    public void removeAllListeners()
    {
		m_Observable.removeAllListeners();
    }


    public void removeListener(EventType eventType, Listener listener)
    {
		m_Observable.removeListener(eventType, listener);
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
		
		RequestViewEvent<M> rve = new RequestViewEvent<M>(m_Chart);
		
		// Note that GWT does not support reflection, so need to use getClass().
		if (tool.getClass() == CausalityViewTool.class)
		{
			// Get the selected feature (evidence) id. If no feature is selected, do nothing.
			int featureId = getSelectedTimeSeriesFeatureId();
			if (featureId != -1)
			{
				EvidenceModel model = new EvidenceModel();// Just set the id and time.
				model.setTime(TimeFrame.SECOND, getSelectedTime());
				model.setId(featureId); 
				rve.setModel((M) model);	
				fireEvent(GXTEvents.OpenCausalityViewClick, rve);
			}
		}
		else if (tool.getClass() == ListViewTool.class)
		{
			// Get the date/time of the 'selected' point. If nothing is selected, do nothing.
			Date selectedTime = getSelectedTime();
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
					model.setTime(TimeFrame.SECOND, getSelectedTime());
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
		else if (tool.getClass() == UsageViewTool.class)
		{
			// Get the date/time of the 'selected' point. If nothing is selected, do nothing.
			Date selectedTime = getSelectedTime();
			if (selectedTime != null)
			{	
				UsageViewTool timeSeriesViewTool = (UsageViewTool)tool;
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
					model.setTime(TimeFrame.SECOND, getSelectedTime());
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
    	
		public void uponFailure(Throwable caught)
		{
			GWT.log("Error loading time series.", caught);
			MessageBox.alert(ClientUtil.CLIENT_CONSTANTS.errorTitle(), 
					ClientUtil.CLIENT_CONSTANTS.errorLoadingTimeSeries(), null);
		}


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
				
				m_Chart.addTimeSeries(
						new TimeSeriesData(m_QueryConfig, dataPoints), lineColour);
				m_Chart.update();
			}
		}
    	
    }
    
}
