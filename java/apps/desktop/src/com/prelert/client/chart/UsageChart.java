/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2009     *
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

import com.extjs.gxt.ui.client.GXT;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.BaseObservable;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.Observable;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Event;
import com.googlecode.gchart.client.GChart;
import com.googlecode.gchart.client.HoverParameterInterpreter;
import com.googlecode.gchart.client.GChart.Curve.Point;

import com.prelert.data.TimeFrame;
import com.prelert.data.UsageRecord;


/**
 * Extension of the Google GChart for plotting usage against time.
 * @author Pete Harverson
 */
public class UsageChart extends GChart implements Observable
{
	private Menu 				m_ContextMenu;
	
	private BaseObservable 		m_Observable;
	
	/**
	 * Creates a new chart for displaying usage data over time.
	 */
	public UsageChart()
	{	
		addStyleName("usage-chart");

		setBackgroundColor(USE_CSS);
		setBorderStyle(USE_CSS); 
		
		createChartWithTimeAxis();
		
		// Create the Observable object for registering listeners and firing events.
		m_Observable = new BaseObservable();
		
		// Double-clicking on a point loads up the drill-down view.
		sinkEvents(Event.ONDBLCLICK); 
	}
	
	
	/**
	 * Creates the chart itself, setting up the axes and curve.
	 */
	protected void createChartWithTimeAxis()
	{	
		setChartSize(570, 320);
		setChartTitle("<b><big>Usage vs Time</big></b>");
		setPadding("5px");
		setClipToPlotArea(true);
		
		// Set up the template for displaying tooltips on points.
		setHoverParameterInterpreter(new UsageDataHoverParameterInterpreter());

		getXAxis().setAxisLabel("<b>Time</b>");
		getXAxis().setHasGridlines(false);
		
		// For the Week chart, show one tick per day.
		getXAxis().setTickCount(8);
		getXAxis().setTicksPerLabel(2);
		// Except for "=(Date)", a standard GWT DateTimeFormat string
		getXAxis().setTickLabelFormat("=(Date)MMM dd HH:mm");

		setYAxisLabelText("Value");
	    getYAxis().setHasGridlines(false);
	    // last bar 'sticks out' over right edge, so extend 'grid' right:
	    getYAxis().setTickCount(11);
	    getYAxis().setTickLength(5); 
	    getYAxis().setAxisMin(0);
		

		// Add the curve for the dataset.
		addCurve();
		getCurve().setYAxis(Y_AXIS);
		getCurve().getSymbol().setBorderColor("red");
		getCurve().getSymbol().setBackgroundColor("red");
		getCurve().getSymbol().setSymbolType(SymbolType.LINE);
		getCurve().getSymbol().setFillThickness(2);
		//getCurve().getSymbol().setFillSpacing(1); 
		
		
		// Removes the symbols themselves, leaving only the line.
	    getCurve().getSymbol().setHeight(0);
	    getCurve().getSymbol().setWidth(0);
	    getCurve().getSymbol().setBrushSize(10, 10);


	}
	
	
	/**
	 * Sets the list of UsageRecord objects for display in the chart.
	 * Note a separate call to <code>update()</code> is needed to update
	 * the chart itself.
	 * @param records the UsageRecord objects to be plotted in the chart.
	 */
	public void setUsageRecords(List<UsageRecord> records)
	{
		// Clear out current data.
		getCurve(0).clearPoints();
		
		// Set the scales on the y axes.
		double yAxisMax = calculateYAxisMax(records);
		getYAxis().setAxisMax(yAxisMax);
		
		// If a timeline has been added, make it full height.
		if (getNCurves() > 1)
		{
			getCurve(1).getPoint().setY(yAxisMax);
		}
		
		for (UsageRecord record : records)
		{
			// Note that getTime() returns milliseconds since 1/1/70
			// required whenever "date cast" tick label
			// formats (those beginning with "=(Date)") are used.
			getCurve(0).addPoint(record.getTime().getTime(), record.getValue());
		}
	}
	
	
	/**
	 * Adds a 'timeline' to the usage chart to indicate a specific time.
	 * @param date date for the time line.
	 */
	public void addTimeLine(Date date)
	{
		// Create the timeline if one has not yet been created.
		if (getNCurves() == 1)
		{
			addCurve();
			getCurve().getSymbol().setSymbolType(SymbolType.BOX_CENTER);
		    getCurve().getSymbol().setWidth(2);
		    getCurve().getSymbol().setHeight(2);
		    getCurve().getSymbol().setBorderWidth(0);
		    getCurve().getSymbol().setBackgroundColor("navy");
		    getCurve().getSymbol().setFillThickness(2);
		    getCurve().getSymbol().setFillSpacing(5);
			getCurve().getSymbol().setHovertextTemplate("${x}");
		}
		else
		{
			getCurve(1).clearPoints();
		}
		
		getCurve().addPoint(date.getTime(), 0d);
		getCurve().addPoint(date.getTime(), getYAxis().getAxisMax());
		
	}
	
	
	/**
	 * Removes the timeline if one has been added to the chart.
	 */
	public void clearTimeLine()
	{
		if (getNCurves() > 1)
		{
			removeCurve(1);
		}
	}
	
	
	/**
	 * Sets the text for the title of the chart.
	 * @param title text to use for the title of the chart.
	 */
	public void setTitleText(String title)
	{
		String chartTitle = "<b><big>";
		
		if (title != null)
		{
			chartTitle += title;
		}
		
		chartTitle += "</big></b>";	
		setChartTitle(chartTitle);
	}
	
	
	/**
	 * Sets the text for the y axis label.
	 * @param label text to use for the y axis label.
	 */
	public void setYAxisLabelText(String label)
	{
		String labelText = "<b>";
		if (label != null)
		{
			labelText += label;
		}
		labelText += "</b>";
		
		getYAxis().setAxisLabel(labelText);
	}
	
	
	/**
	 * Sets the minimum and maximum values on the x axis to the specified start
	 * and end dates, and sets the tick count and ticks per label to the supplied
	 * values.
	 * @param startDate start date for the x axis.
	 * @param endDate end date for the x axis.
	 * @param tickCount the number of ticks for the x axis.
	 * @param ticksPerLabel the ratio of the number of ticks to the number of labelled ticks.
	 */
	public void setXAxisRange(Date startDate, Date endDate, int tickCount, int ticksPerLabel)
	{
		if (startDate != null)
		{
			getXAxis().setAxisMin(startDate.getTime());
		}
		if (endDate != null)
		{
			getXAxis().setAxisMax(endDate.getTime());
		}
		getXAxis().setTickCount(tickCount);
		getXAxis().setTicksPerLabel(ticksPerLabel);
	}
	
	
	/**
	 * Sets the x axis range for a chart showing the specified time frame
	 * with the x-axis scale beginning on the specified date.
	 * @param timeFrame TimeFrame of usage chart (e.g. WEEK, DAY or HOUR)
	 * @param startTime start date for x-axis.
	 */
	@SuppressWarnings("deprecation")
	public void setXAxisRange(TimeFrame timeFrame, Date startTime)
	{
		// Start time is null if there is no usage data in the DB.
		if (startTime != null)
		{
			Date startDate;
			Date endDate;
			String hoverTemplate;
			
			// Set the x-axis range, and  set the hover text template to show the
			// x-axis value down to seconds for the HOUR time frame.
			switch (timeFrame)
			{
				case WEEK:
					startDate = new Date(startTime.getYear(), startTime.getMonth(), startTime.getDate());
					endDate = new Date(startDate.getTime() + (7*24*60*60*1000));
					setXAxisRange(startDate, endDate, 8, 2);
					hoverTemplate = GChart.formatAsHovertext("${x}, ${y}");
					getCurve(0).getSymbol().setHovertextTemplate(hoverTemplate);
					break;
					
				case DAY:
					startDate = new Date(startTime.getYear(), startTime.getMonth(), startTime.getDate());
					endDate = new Date(startDate.getTime() + (24*60*60*1000));
					setXAxisRange(startDate, endDate, 9, 2);
					hoverTemplate = GChart.formatAsHovertext("${x}, ${y}");
					getCurve(0).getSymbol().setHovertextTemplate(hoverTemplate);
					break;
					
				case HOUR:
					startDate = new Date(startTime.getYear(), startTime.getMonth(), 
							startTime.getDate(), startTime.getHours(), 0);
					endDate = new Date(startDate.getTime() + (60*60*1000));
					setXAxisRange(startDate, endDate, 7, 2);
					hoverTemplate = GChart.formatAsHovertext("${fullTime}, ${y}");
					getCurve(0).getSymbol().setHovertextTemplate(hoverTemplate);
					break;
			}
		}
	}
	
	
	/**
	 * Returns the time value of the point that the mouse is currently touching
	 * i.e. the 'hovered over' point.
	 * @return the time of the touched point, or <code>null</code> if no point
	 * is currently hovered over.
	 */
	public Date getTouchedPointTime()
	{
		Date touchedPointTime = null;
		
		Point touchedPoint = getTouchedPoint();
		if (touchedPoint != null)
		{
			double touchedPointX = touchedPoint.getX();
			touchedPointTime = new Date((long)touchedPointX);
		}
		
		return touchedPointTime;
	}
	
	
	/**
	 * Sets the chart's context menu.
	 * @param menu the context menu.
	 */
	public void setContextMenu(Menu menu)
	{
		m_ContextMenu = menu;
		sinkEvents(GXT.isSafari && GXT.isMac ? Event.ONMOUSEDOWN : Event.ONMOUSEUP);
		sinkEvents(Event.ONCONTEXTMENU);
	}
	
	
	/**
	 * Appends an event handler to the usage chart.
	 * @param eventType the eventType
	 * @param listener the listener to be added
	 */
	public void addListener(int eventType, Listener listener)
	{
		m_Observable.addListener(eventType, listener);
	}


	/**
	 * Fires an event.
	 * @param eventType the event type.
	 * @param be the base event.
	 */
    public boolean fireEvent(int eventType, BaseEvent be)
    {
	    return m_Observable.fireEvent(eventType, be);
    }


    /**
     * Removes all registered listeners from the usage chart.
     */
    public void removeAllListeners()
    {
		m_Observable.removeAllListeners();
    }


    /**
     * Removes an event handler from the usage chart.
     * @param eventType the event type.
     * @param listener the listener to remove.
     */
    public void removeListener(int eventType, Listener listener)
    {
		m_Observable.removeListener(eventType, listener);
    }
	
	
	/**
	 * Calculates the maximum value to be displayed on the y axis.
	 * @param records the records being displayed in the chart.
	 * @return minimum value visible on the y axis.
	 */
	protected double calculateYAxisMax(List<UsageRecord> records)
	{
		// Minimum max value on the y-axis is 10 for the case where there are 
		// no records, or the maximum usage value is less than 10.
		double maxVal = 9; 	
		
		for (UsageRecord record : records)
		{
			// Note that getTime() returns milliseconds since 1/1/70
			// required whenever "date cast" tick label
			// formats (those beginning with "=(Date)") are used.
			maxVal = Math.max(maxVal, record.getValue());
		}
		
		double powerFloor = Math.floor(Math.log10(maxVal)); // e.g. gives 5 for 416521
		double raiseToPower = Math.pow(10, powerFloor); // e.g. 100000 for 416521
		
		double nearestMax = (Math.floor(maxVal/raiseToPower) + 1) * raiseToPower;
		
		return nearestMax;
	}
	
	
	/**
     * Fires whenever a browser event is received.
     * @param event the browser event that has been received.
     */
    public void onBrowserEvent(Event event) 
    {  	
    	// Note that the ONCONTEXTMENU does not fire in Opera.
    	if (event.getTypeInt() == Event.ONCONTEXTMENU)
		{
        	onContextMenu(event);

		}
    	
    	fireEvent(event.getTypeInt(), new BaseEvent(this));
    	
    	// Pass event on (a GChart should respond to mouse activity  - but never eat it).
        super.onBrowserEvent(event);
    }
    
    
    /**
     * Fires when a context menu event is received, displaying the context
     * menu at the coordinates on the chart that the event was triggered.
     * @param event the context menu event.
     */
    protected void onContextMenu(Event event)
	{
		if (m_ContextMenu != null)
		{
			// Stop the event from being propagated to prevent the browser's
			// normal context menu being displayed.
			event.cancelBubble(true);
        	event.preventDefault();
        	
        	Point touchedPoint = getTouchedPoint();
        	if (touchedPoint != null)
        	{        		
				final int x = event.getClientX();
				final int y = event.getClientY();
	
					DeferredCommand.addCommand(new Command()
					{
						public void execute()
						{
							m_ContextMenu.showAt(x, y);
						}
					});
        	}
		}
	}
    
    
	/** 
	 * Custom GChart HoverParameterInterpreter to display the x-axis time value
	 * down to seconds if a 'fullTime' parameter is passed to the 
	 * getHoverParameter() method.
	 */
	 class UsageDataHoverParameterInterpreter implements
	        HoverParameterInterpreter
	{

		public String getHoverParameter(String paramName,
		        GChart.Curve.Point hoveredOver)
		{

			// Returning null tells GChart "I don't know how to expand that
			// parameter name". The built-in parameters (${x}, ${y}, etc.) won't
			// be processed correctly unless you return null for this "no
			// matching parameter" case.
			String result = null;
			if (paramName.equals("fullTime"))
			{
				// Display the value of the time down to seconds.
				DateTimeFormat dateFormatter = DateTimeFormat.getFormat("MMM dd HH:mm:ss");
				double value = hoveredOver.getX();
				Date transDate = new Date((long) value);
		        result = dateFormatter.format(transDate);
			}

			return result;
		}

	}

}
