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

import java.util.Date;
import java.util.List;

import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.BaseObservable;
import com.extjs.gxt.ui.client.event.EventType;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.LoadListener;
import com.extjs.gxt.ui.client.event.MenuEvent;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.extjs.gxt.ui.client.widget.menu.MenuItem;
import com.google.gwt.user.client.ui.Widget;
import com.googlecode.gchart.client.GChart;
import com.prelert.client.ClientUtil;

import com.prelert.client.event.ChartWidgetEvent;
import com.prelert.client.event.ChartWidgetListener;
import com.prelert.client.event.GXTEvents;


/**
 * Implementation of the ChartWidget which uses the GChart Google
 * Web Toolkit (GWT) extension for the charting component 
 * (see {@link http://code.google.com/p/clientsidegchart/}).
 * 
 * @param <D> the data type being displayed in the chart widget.
 * @see ChartWidget
 * @author Pete Harverson
 */
public class GChartWidget<D> implements ChartWidget<D>
{
	protected Date						m_StartTime;
	protected Date						m_EndTime;
	
	protected TimeAxisGChart			m_Chart;
	
	private MenuItem					m_ZoomInItem;
	private MenuItem					m_ZoomOutItem;
	
	private BaseObservable 				m_Observable;
	
	private int							m_MaximumZoomMinutes = 15;
	private int							m_MinimumZoomMinutes = 40320; // 4 weeks.
	
	
	/**
	 * Creates a new GChartWidget.
	 */
	public GChartWidget()
	{
		this(new TimeAxisGChart());
	}
	
	
	/**
	 * Creates a new chart widget using the specified TimeAxisGChart component
	 * for plotting the time-based data.
	 */
	protected GChartWidget(TimeAxisGChart timeAxisChart)
	{
		m_Chart = timeAxisChart;
		
	    // Configure the right-click context menu.
	    Menu menu = new Menu();

	    // Add Zoom in and Zoom out items.
	    m_ZoomInItem = new MenuItem(ClientUtil.CLIENT_CONSTANTS.zoomInMenuItem());
	    m_ZoomInItem.addSelectionListener(new SelectionListener<MenuEvent>(){

            @Override
            public void componentSelected(MenuEvent ce)
            {
	            Date selectedTime = getSelectedTime();
	            if (selectedTime != null)
	            {
	            	zoomInDateAxis(selectedTime);
	            }
            }
	    });
	    
	    m_ZoomOutItem = new MenuItem(ClientUtil.CLIENT_CONSTANTS.zoomOutMenuItem());
	    m_ZoomOutItem.addSelectionListener(new SelectionListener<MenuEvent>(){

            @Override
            public void componentSelected(MenuEvent ce)
            {
	            Date selectedTime = getSelectedTime();
	            if (selectedTime != null)
	            {
	            	zoomOutDateAxis(selectedTime);
	            }
            }
	    });
	    
	    menu.add(m_ZoomInItem);
	    menu.add(m_ZoomOutItem);
	    
	    // When the menu is shown/hidden, enable or disable the 
	    // zoom in / zoom out items depending on the current zoom level.
	    menu.addListener(Events.BeforeShow, new Listener<MenuEvent>()
		{
			@Override
            public void handleEvent(MenuEvent be)
            {	  
                m_ZoomInItem.setEnabled(!isMaxZoomLevel());
                m_ZoomOutItem.setEnabled(!isMinZoomLevel());
            }
	
		});
	    
	    
	    m_Chart.setContextMenu(menu);
		
		m_Observable = new BaseObservable();
	}
	
	
	/**
	 * Returns the GChart component which is displaying the time-based data.
	 * @return the GChart GWT widget.
	 */
	public TimeAxisGChart getChart()
	{
		return m_Chart;
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
    	
    	m_Chart.update();
		
    }
    
    
    /**
     * Sets the height of the chart, which should include space for x-axis 
	 * tick marks and labels.
     * @param height height of the chart.
     */
    public void setChartHeight(int height)
    {
    	// Account for x-axis labels.
       	int yChartSize = height - (m_Chart.getXAxis().getAxisLabelThickness());
    	if (yChartSize < 100)
    	{
    		yChartSize = 100;
    	}
    	
    	m_Chart.setYChartSize(yChartSize);
    	
    	m_Chart.update();
    }
    
    
    /**
     * Sets the size of the chart, which should include space for axis tick
     * marks and labels.
     * @param width width of the chart.
     * @param height height of the chart.
     */
    public void setChartSize(int width, int height)
    {
    	// Account for y-axis ticks and labels.
    	int xChartSize = width - (m_Chart.getYAxis().getTickLabelThickness()+20);
    	if (xChartSize < 100)
    	{
    		xChartSize = 100;
    	}
    	
    	m_Chart.setXChartSize(xChartSize);
    	
    	
    	// Account for x-axis labels.
       	int yChartSize = height - (m_Chart.getXAxis().getAxisLabelThickness());
    	if (yChartSize < 100)
    	{
    		yChartSize = 100;
    	}
    	
    	m_Chart.setYChartSize(yChartSize);
    	
    	m_Chart.update();
    }
    
    
    /**
	 * Loads the data in chart widget according to its current configuration.
	 */
	public boolean load()
	{
		return false;
	}


	/**
	 * Loads the data for the specified configuration into the chart. 
	 * The lower and upper time bounds for the data loaded will be
	 * set according to the current time span of the chart.
	 * @param config configuration of the data to load into the chart.
	 */
	@Override
	public boolean load(Object config)
	{
		return false;
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
			m_Chart.getYAxis().setTickLabelThickness(35);
			m_Chart.getYAxis().setTickLabelFontColor("white");
		}
		else
		{
			m_Chart.getYAxis().setTickLabelThickness(100);
			m_Chart.getYAxis().setTickLabelFontColor(
					GChart.DEFAULT_TICK_LABEL_FONT_COLOR);
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
		
		m_Chart.setDateAxisStart(startTime);
		m_Chart.setDateAxisEnd(endTime);
	}
	

	/**
	 * Returns the start date/time of the visible region of the chart.
	 * @return the start date/time.
	 */
	public Date getStartTime()
	{
		return m_StartTime;
	}
	
	
	/**
	 * Returns the end date/time of the visible region of the chart.
	 * @return the end date/time.
	 */
	public Date getEndTime()
	{
		return m_EndTime;
	}


	@Override
	public void zoomInDateAxis()
	{
		Date startTime = m_Chart.getDateAxisStart();
		Date endTime = m_Chart.getDateAxisEnd();
			
		Date midPoint = new Date((endTime.getTime() + startTime.getTime())/2);
		
		zoomInDateAxis(midPoint);
	}
	
	
	/**
	 * Zooms in the date axis, centred around the specified time.
	 * @param the time to zoom in on.
	 */
	public void zoomInDateAxis(Date centreOnTime)
	{
		fireEvent(GXTEvents.BeforeZoomIn, 
				new ChartWidgetEvent(this, GXTEvents.BeforeZoomIn));
		
		Date startTime = m_Chart.getDateAxisStart();
		Date endTime = m_Chart.getDateAxisEnd();
			
		long intervalInMins = (endTime.getTime() - startTime.getTime())/60000l;
		
		if (intervalInMins <= m_MaximumZoomMinutes)
		{
			return;
		}
		
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
		// 30 mins
		// 15 mins

		
		// NB. Work in ms and create new Date objects as GXT DateWrapper methods 
		// not safe when switching to/from daylight savings time.
		if (intervalInMins <= 60)
		{
			// Halve the range.
			long halfNewRangeInMins = intervalInMins/4l;
			
			zoomStart = new Date((centreOnTime.getTime() - (halfNewRangeInMins * 60000l) ));
			zoomEnd = new Date((centreOnTime.getTime() + (halfNewRangeInMins * 60000l) ));
		}
		else if (intervalInMins <= 180)
		{
			zoomStart = new Date((centreOnTime.getTime() - (30 * 60000) ));
			zoomEnd = new Date((centreOnTime.getTime() + (30 * 60000) ));
		}
		else if (intervalInMins <= 360)
		{
			zoomStart = new Date((centreOnTime.getTime() - (90 * 60000) ));
			zoomEnd = new Date((centreOnTime.getTime() + (90 * 60000) ));
		}
		else if (intervalInMins <= 720)
		{
			zoomStart = new Date((centreOnTime.getTime() - (180 * 60000) ));
			zoomEnd = new Date((centreOnTime.getTime() + (180 * 60000) ));
		}
		else if (intervalInMins <= 1440)
		{
			zoomStart = new Date((centreOnTime.getTime() - (360 * 60000) ));
			zoomEnd = new Date((centreOnTime.getTime() + (360 * 60000) ));
		}
		else if (intervalInMins <= 2880)
		{
			zoomStart = new Date((centreOnTime.getTime() - (720 * 60000) ));
			zoomEnd = new Date((centreOnTime.getTime() + (720 * 60000) ));
		}
		else if (intervalInMins <= 5760)
		{
			zoomStart = new Date((centreOnTime.getTime() - (1440 * 60000) ));
			zoomEnd = new Date((centreOnTime.getTime() + (1440 * 60000) ));
		}
		else if (intervalInMins <= 10080)
		{
			zoomStart = new Date((centreOnTime.getTime() - (2880 * 60000) ));
			zoomEnd = new Date((centreOnTime.getTime() + (2880 * 60000) ));
		}
		else if (intervalInMins <= 20160)
		{
			zoomStart = new Date((centreOnTime.getTime() - (5040 * 60000) ));
			zoomEnd = new Date((centreOnTime.getTime() + (5040 * 60000) ));
		}
		else if (intervalInMins <= 40320)
		{
			zoomStart = new Date((centreOnTime.getTime() - (10080 * 60000) ));
			zoomEnd = new Date((centreOnTime.getTime() + (10080 * 60000) ));
		}
		
		if (zoomStart != null)
		{
			setDateRange(zoomStart, zoomEnd);
		}
	}
	
	
	@Override
	public void zoomOutDateAxis()
	{
		Date startTime = m_Chart.getDateAxisStart();
		Date endTime = m_Chart.getDateAxisEnd();
			
		Date midPoint = new Date((endTime.getTime() + startTime.getTime())/2);
		
		zoomOutDateAxis(midPoint);
	}
	
	
	/**
	 * Zooms out the date axis, centred around the specified midpoint of the date range.
	 * @param the time to zoom out around.
	 */
	public void zoomOutDateAxis(Date centreOnTime)
	{
		fireEvent(GXTEvents.BeforeZoomOut, 
				new ChartWidgetEvent(this, GXTEvents.BeforeZoomOut));
		
		Date startTime = m_Chart.getDateAxisStart();
		Date endTime = m_Chart.getDateAxisEnd();
			
		long intervalInMins = (endTime.getTime() - startTime.getTime())/60000l;
		
		if (intervalInMins >= m_MinimumZoomMinutes)
		{
			return;
		}
		
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
		// 30 mins
		// 15 mins
		
		// NB. Work in ms and create new Date objects as GXT DateWrapper methods 
		// not safe when switching to/from daylight savings time.
		if (intervalInMins <= 30)
		{
			// Double the range
			zoomStart = new Date((centreOnTime.getTime() - (intervalInMins * 60000) ));
			zoomEnd = new Date((centreOnTime.getTime() + (intervalInMins * 60000) ));
		}
		else if (intervalInMins <= 60)
		{
			zoomStart = new Date((centreOnTime.getTime() - (90 * 60000) ));
			zoomEnd = new Date((centreOnTime.getTime() + (90 * 60000) ));
		}
		else if (intervalInMins <= 180)
		{
			zoomStart = new Date((centreOnTime.getTime() - (180 * 60000) ));
			zoomEnd = new Date((centreOnTime.getTime() + (180 * 60000) ));
		}
		else if (intervalInMins <= 360)
		{
			zoomStart = new Date((centreOnTime.getTime() - (360 * 60000) ));
			zoomEnd = new Date((centreOnTime.getTime() + (360 * 60000) ));
		}
		else if (intervalInMins <= 720)
		{
			zoomStart = new Date((centreOnTime.getTime() - (720 * 60000) ));
			zoomEnd = new Date((centreOnTime.getTime() + (720 * 60000) ));
		}
		else if (intervalInMins <= 1440)
		{
			zoomStart = new Date((centreOnTime.getTime() - (1440 * 60000) ));
			zoomEnd = new Date((centreOnTime.getTime() + (1440 * 60000) ));
		}
		else if (intervalInMins <= 2880)
		{	
			zoomStart = new Date((centreOnTime.getTime() - (2880 * 60000) ));
			zoomEnd = new Date((centreOnTime.getTime() + (2880 * 60000) ));
		}
		else if (intervalInMins <= 5760)
		{	
			zoomStart = new Date((centreOnTime.getTime() - (5040 * 60000) ));
			zoomEnd = new Date((centreOnTime.getTime() + (5040 * 60000) ));
		}
		else if (intervalInMins <= 10080)
		{	
			zoomStart = new Date((centreOnTime.getTime() - (10080 * 60000) ));
			zoomEnd = new Date((centreOnTime.getTime() + (10080 * 60000) ));
		}
		else if (intervalInMins <= 20160)
		{		
			zoomStart = new Date((centreOnTime.getTime() - (20160 * 60000) ));
			zoomEnd = new Date((centreOnTime.getTime() + (20160 * 60000) ));
		}
		
		if (zoomStart != null)
		{
			setDateRange(zoomStart, zoomEnd);
		}	
		
	}
	
	
	/**
	 * Returns whether or not the chart is set to the maximum zoom level.
	 * @return <code>true</code> if the chart is set to the maximum zoom 
	 * 	<code>false</code> otherwise.
	 */
	public boolean isMaxZoomLevel()
	{
		Date startTime = m_Chart.getDateAxisStart();
		Date endTime = m_Chart.getDateAxisEnd();
			
		long intervalInMins = (endTime.getTime() - startTime.getTime())/60000l;
		
		return (intervalInMins <= m_MaximumZoomMinutes);
	}
	
	
	/**
	 * Returns whether or not the chart is set to the minimum zoom level.
	 * @return <code>true</code> if the chart is set to the minimum zoom 
	 * 	of 4 weeks, <code>false</code> otherwise.
	 */
	public boolean isMinZoomLevel()
	{
		Date startTime = m_Chart.getDateAxisStart();
		Date endTime = m_Chart.getDateAxisEnd();
			
		long intervalInMins = (endTime.getTime() - startTime.getTime())/60000l;
		
		// Allow for time span straddling switch to daylight savings time.
		return (intervalInMins >= (m_MinimumZoomMinutes-60));
	}
	
	
	/**
	 * Sets the maximum zoom for the time axis i.e. the shortest time span that
	 * the x-axis can be zoomed in. The default setting is 15 minutes.
	 * @param maxZoomMinutes the maximum zoom, in minutes.
	 */
	protected void setMaxZoom(int maxZoomMinutes)
	{
		m_MaximumZoomMinutes = maxZoomMinutes;
	}
	
	
	/**
	 * Sets the minimum zoom for the time axis i.e. the longest time span that
	 * the x-axis can be zoomed out. The default setting is 4 weeks (40320 minutes).
	 * @param minZoomMinutes the minimum zoom, in minutes.
	 */
	protected void setMinZoom(int minZoomMinutes)
	{
		m_MinimumZoomMinutes = minZoomMinutes;
	}
	
	
	@Override
	public void panLeft()
	{
		fireEvent(GXTEvents.BeforePanLeft, 
				new ChartWidgetEvent(this, GXTEvents.BeforePanLeft));
		
		Date startTime = m_Chart.getDateAxisStart();
		Date endTime = m_Chart.getDateAxisEnd();
			
		long intervalInMins = endTime.getTime() - startTime.getTime();
		
		Date panStart = new Date(startTime.getTime() - (intervalInMins/2));
		Date panEnd = new Date(endTime.getTime() - (intervalInMins/2));
		
		setDateRange(panStart, panEnd);
	}
	
	
	@Override
	public void panRight()
	{
		fireEvent(GXTEvents.BeforePanRight, 
				new ChartWidgetEvent(this, GXTEvents.BeforePanRight));
		
		Date startTime = m_Chart.getDateAxisStart();
		Date endTime = m_Chart.getDateAxisEnd();
			
		long intervalInMins = endTime.getTime() - startTime.getTime();
		
		Date panStart = new Date(startTime.getTime() + (intervalInMins/2));
		Date panEnd = new Date(endTime.getTime() + (intervalInMins/2));
		
		setDateRange(panStart, panEnd);
	}
	
	
	@Override
	public void setTimeMarker(Date time)
	{
		m_Chart.setTimeMarker(time);
	}
	
	
	@Override
	public void clearTimeMarker()
	{
		m_Chart.clearTimeMarker();
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
     * Adds a listener for load events from the chart widget. 
     */
    @Override
    public void addLoadListener(LoadListener listener)
    {
    	addListener(BeforeLoad, listener);
        addListener(LoadException, listener);
        addListener(Load, listener);
    }


    /**
     * Removes a listener for load events from the chart widget.
     */
    @Override
    public void removeLoadListener(LoadListener listener)
    {
    	removeListener(BeforeLoad, listener);
        removeListener(LoadException, listener);
        removeListener(Load, listener);
    }
    

    @Override
    public void addChartWidgetListener(ChartWidgetListener listener)
    {
    	addListener(GXTEvents.BeforePanLeft, listener);
    	addListener(GXTEvents.BeforePanRight, listener);
    	addListener(GXTEvents.BeforeZoomIn, listener);
    	addListener(GXTEvents.BeforeZoomOut, listener);
    }


    @Override
    public void removeChartWidgetListener(ChartWidgetListener listener)
    {
    	removeListener(GXTEvents.BeforePanLeft, listener);
    	removeListener(GXTEvents.BeforePanRight, listener);
    	removeListener(GXTEvents.BeforeZoomIn, listener);
    	removeListener(GXTEvents.BeforeZoomOut, listener);
    }


	public void addListener(EventType eventType, Listener<? extends BaseEvent> listener)
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


    public void removeListener(EventType eventType, Listener<? extends BaseEvent> listener)
    {
		m_Observable.removeListener(eventType, listener);
    }
    
}
