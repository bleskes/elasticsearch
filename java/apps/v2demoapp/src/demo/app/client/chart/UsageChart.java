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

package demo.app.client.chart;

import java.util.Date;
import java.util.List;

import mx4j.log.Logger;

import com.extjs.gxt.ui.client.GXT;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.BaseObservable;
import com.extjs.gxt.ui.client.event.EventType;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.Observable;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Event;
import com.googlecode.gchart.client.GChart;
import com.googlecode.gchart.client.HoverParameterInterpreter;
import com.googlecode.gchart.client.GChart.Curve.Point;

import demo.app.data.TimeFrame;
import demo.app.data.UsageRecord;


/**
 * Extension of the Google GChart for plotting usage against time.
 * @author Pete Harverson
 */
public class UsageChart extends GChart implements Observable
{
	private Menu 					m_ContextMenu;
	
	private Curve					m_TimeLine;
	
	private BaseObservable 			m_Observable;
	
	
	/**
	 * Creates a new chart for displaying usage data over time.
	 */
	public UsageChart()
	{
		addStyleName("usage-chart");

		setBackgroundColor(USE_CSS);
		setBorderStyle(USE_CSS); 

		createChartWithTimeAxis();
		//createSimpleLineChart();
		
		m_Observable = new BaseObservable();
		
		// Double-clicking on a point loads up the drill-down view.
		sinkEvents(Event.ONDBLCLICK); 
	}
	
	

	
	
	/**
	 * Sets the chart's context menu.
	 * @param menu the context menu
	 */
	public void setContextMenu(Menu menu)
	{
		m_ContextMenu = menu;
		sinkEvents(GXT.isSafari && GXT.isMac ? Event.ONMOUSEDOWN : Event.ONMOUSEUP);
		sinkEvents(Event.ONCONTEXTMENU);
	}
	
	
	public void addTimeSeries(List<UsageRecord> records, String lineColour)
	{
		// Keep the timeline as the curve with the highest index.
		int indexToAdd = getNCurves();
		if (m_TimeLine != null)
		{
			indexToAdd--;
		}
		
		// Create the new curve.
		addCurve(indexToAdd);
		getCurve(indexToAdd).setYAxis(Y_AXIS);
		getCurve(indexToAdd).getSymbol().setBorderColor(lineColour);
		getCurve(indexToAdd).getSymbol().setBackgroundColor(lineColour);
		getCurve(indexToAdd).getSymbol().setSymbolType(SymbolType.LINE);
		getCurve(indexToAdd).getSymbol().setFillThickness(2);
		//getCurve(indexToAdd).getSymbol().setFillSpacing(1); 
		
		// Removes the symbols themselves, leaving only the line.
	    getCurve(indexToAdd).getSymbol().setHeight(0);
	    getCurve(indexToAdd).getSymbol().setWidth(0);
	    getCurve(indexToAdd).getSymbol().setBrushSize(10, 10);
		
	    // Add the dataset.
		for (UsageRecord record : records)
		{
			getCurve(indexToAdd).addPoint(record.getTime().getTime(), record.getValue());
		}
	}
	
	
	/**
	 * Sets the list of UsageRecord objects for display in the chart.
	 * All other records currently being displayed in the chart will be removed,
	 * although if a timeline has been added this will remain.
	 * @param source the name of the Source whose records have been supplied.
	 * @param records the UsageRecord objects to be plotted in the chart.
	 */
	public void setUsageRecords(List<UsageRecord> records)
	{
		// Clear out current data.
		int numCurves = getNCurves();
		if ( (numCurves == 0) || (numCurves == 1 && m_TimeLine != null) )
		{
			addTimeSeries(records, "#ff0000");
		}
		else
		{
			// Clear out any additional lines - leaving just one curve.
			if (m_TimeLine != null)
			{
				for (int i = 1; i < (numCurves-1); i++)
				{
					removeCurve(i);
				}
			}
			else
			{
				for (int i = 1; i < numCurves; i++)
				{
					removeCurve(i);
				}
			}
			
			getCurve(0).clearPoints();
			
			for (UsageRecord record : records)
			{
				// Note that getTime() returns milliseconds since 1/1/70
				// required whenever "date cast" tick label
				// formats (those beginning with "=(Date)") are used.
				getCurve(0).addPoint(record.getTime().getTime(), record.getValue());
			}
		}
		
		// Set the scales on the y axes.
		double yAxisMax = calculateYAxisMax(records);
		getYAxis().setAxisMax(yAxisMax);
		
		// If a timeline has been added, make it full height.
		if (m_TimeLine != null)
		{
			m_TimeLine.getPoint().setY(yAxisMax);
		}
	}
	
	
	/**
	 * Adds a 'timeline' to the usage chart to indicate a specific time.
	 * @param date date for the time line.
	 */
	public void addTimeLine(Date date)
	{
		// Create the timeline if one has not yet been created.
		if (m_TimeLine == null)
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
			m_TimeLine = getCurve();
		}
		else
		{
			m_TimeLine.clearPoints();
		}
		
		m_TimeLine.addPoint(date.getTime(), 0d);
		m_TimeLine.addPoint(date.getTime(), getYAxis().getAxisMax());
		
	}
	
	
	/**
	 * Removes the timeline if one has been added to the chart.
	 */
	public void clearTimeLine()
	{
		if (m_TimeLine != null)
		{
			removeCurve(m_TimeLine);
			m_TimeLine = null;
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
		
		GWT.log("UsageChart setYAxisLabelText(" + label + 
				") thickness of label: " + getYAxis().getAxisLabelThickness(), null);
	}
	
	
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
	
	
	protected void createSimpleLineChart()
	{
		setChartTitle("<b>x<sup>2</sup> vs x</b>");
		setChartSize(300, 200);
		addCurve(); // solid, 2px thick, 1px resolution, connecting lines:
		getCurve().getSymbol().setSymbolType(SymbolType.LINE);
		getCurve().getSymbol().setFillThickness(2);
		getCurve().getSymbol().setFillSpacing(1); // Make center-fill of square
												  // point, markers same color
												  // as line:
		getCurve().getSymbol().setBackgroundColor(
		        getCurve().getSymbol().getBorderColor());
		for (int i = 0; i < 10; i++)
		{
			getCurve().addPoint(i, i * i);
		}

		getCurve().setLegendLabel("x<sup>2</sup>");
		getXAxis().setAxisLabel("x");
		getYAxis().setAxisLabel("x<sup>2</sup>");

		getXAxis().setTickLabelFontSize(8);
	}
	
	
	protected void createChartWithTimeAxis()
	{		
		setChartSize(570, 320);
		setChartTitle("<b><big>Usage vs Time</big></b>");
		setPadding("5px");
		//setShowOffChartPoints(false);
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

		getYAxis().setAxisLabel("<b>Value</b>");
	    getYAxis().setHasGridlines(false);
	    // last bar 'sticks out' over right edge, so extend 'grid' right:
	    getYAxis().setTickCount(11);
	    getYAxis().setTickLength(5); 
	    getYAxis().setAxisMin(0);
		//getYAxis().setAxisMax(100); 
	    

		// Double-clicking on a point loads up the drill-down view.
		sinkEvents(Event.ONDBLCLICK); 
	}
	
	
	protected void createDualAxisChart()
	{
		
		setChartSize(400, 300);
		setChartTitle("<b><big>Temperature and CPU vs Time</big></b>");
		setPadding("5px");

		getXAxis().setAxisLabel("<small><b><i>Time</i></b></small>");
		getXAxis().setHasGridlines(true);
		getXAxis().setTickCount(6);
		// Except for "=(Date)", a standard GWT DateTimeFormat string
		getXAxis().setTickLabelFormat("=(Date)dd/h:mm a");

		getYAxis().setAxisLabel("<small><b><i>&deg;C</i></b></small>");
		getYAxis().setHasGridlines(true);
		getYAxis().setTickCount(11);
		getYAxis().setAxisMin(11);
		getYAxis().setAxisMax(16);
		
	    getY2Axis().setAxisLabel("<small><b><i>%CPU</i></b></small>");
	    getY2Axis().setHasGridlines(false);
	    // last bar 'sticks out' over right edge, so extend 'grid' right:
	    getY2Axis().setTickCount(11);
	    getY2Axis().setTickLength(15); 
	    getY2Axis().setAxisMin(0);
		getY2Axis().setAxisMax(100);

	    // Add the first dataset.
		addCurve();
		getCurve().setLegendLabel("<i>T (&deg;C)</i>");
		getCurve().setYAxis(Y_AXIS);
		getCurve().getSymbol().setBorderColor("blue");
		getCurve().getSymbol().setBackgroundColor("blue");
		getCurve().getSymbol().setFillSpacing(10);
		getCurve().getSymbol().setFillThickness(3);

		for (int i = 0; i < dateSequence.length; i++)
		{
			// Note that getTime() returns milliseconds since
			// 1/1/70--required whenever "date cast" tick label
			// formats (those beginning with "=(Date)") are used.
			getCurve().addPoint(dateSequence[i].date.getTime(),
			        dateSequence[i].value);
		}
		
		
		// Add the second dataset.
		addCurve();
		getCurve().setLegendLabel("<i>CPU</i>");
		getCurve().setYAxis(Y2_AXIS);
		getCurve().getSymbol().setBorderColor("red");
		getCurve().getSymbol().setBackgroundColor("red");
		getCurve().getSymbol().setSymbolType(SymbolType.LINE);
		getCurve().getSymbol().setFillThickness(1);
		getCurve().getSymbol().setFillSpacing(1); 

		for (int i = 0; i < cpuSequence.length; i++)
		{
			// Note that getTime() returns milliseconds since
			// 1/1/70--required whenever "date cast" tick label
			// formats (those beginning with "=(Date)") are used.
			getCurve().addPoint(cpuSequence[i].date.getTime(),
					cpuSequence[i].value);
		}

	}
	
	
	/**
	 * Calculates the minimum value to be displayed on the x axis.
	 * @param records the records being displayed in the chart.
	 * @return minimum value visible on the x-axis.
	 */
	protected double calculateXAxisMin(List<UsageRecord> records)
	{
		UsageRecord lastRecord = records.get(records.size() - 1);
		long lastTimeMillis = lastRecord.getTime().getTime();
		
		long millisInWeek = 7 * 24 * 60 * 60 * 1000;
		
		return lastTimeMillis - millisInWeek;
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
		
		double nearestMax = (Math.floor(maxVal/raiseToPower) + 1) * raiseToPower; // e.g. 500000 for 416521
	
		return nearestMax;
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
     * Delegates handling of any 'sunk' browser events to any registered listeners.
     * @param event the browser event that has been received.
     */
    public void onBrowserEvent(Event event) 
    {
    	super.onBrowserEvent(event);
    	
        //if (event.getTypeInt() == (GXT.isSafari && GXT.isMac ? Event.ONMOUSEDOWN : Event.ONMOUSEUP)
		//        && isRightClick(event))
    	// Note that the ONCONTEXTMENU does not fire in Opera.
    	if (event.getTypeInt() == Event.ONCONTEXTMENU)
		{
        	onContextMenu(event);

		}
    	
    	if (event.getTypeInt() == Event.ONDBLCLICK)
    	{
    		// NB. Use com.extjs.gxt.ui.client.event.Events constant due to 
    		// behaviour of BaseObservable.getListeners(EventType) and lack of
    		// equals() method in com.extjs.gxt.ui.client.event.EventType.
    		fireEvent(Events.OnDoubleClick, new BaseEvent(this));
    	}
    	else
    	{
    		fireEvent(new EventType(event.getTypeInt()), new BaseEvent(this));
    	}
    	
    	// Pass event on (a GChart should respond to mouse activity  - but never eat it).
        //super.onBrowserEvent(event);
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
			event.cancelBubble(true);//This will stop the event from being propagated
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
	 * Returns <code>true</code> if the event is a right click.
	 * 
	 * @return the right click state
	 */
	protected boolean isRightClick(Event event)
	{
		if (event != null)
		{
			if (DOM.eventGetButton(event) == Event.BUTTON_RIGHT
			        || (GXT.isMac && DOM.eventGetCtrlKey(event)))
			{
				return true;
			}
		}
		return false;
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

	
	
	class DateStampedValue
	{
		Date date;
		double value;


		public DateStampedValue(String dateTimeString, double value)
		{
			this.date = new Date(dateTimeString);
			this.value = value;
		}
	}
	
	DateStampedValue[] dateSequence = {
	    new DateStampedValue("1/28/2009 03:00", 13.0),
	    new DateStampedValue("1/28/2009 03:30", 12.9),
	    new DateStampedValue("1/28/2009 03:51", 12.9),
	    new DateStampedValue("1/28/2009 04:11", 12.9),
	    new DateStampedValue("1/28/2009 04:24", 13.0),
	    new DateStampedValue("1/28/2009 04:46", 12.5),
	    new DateStampedValue("1/28/2009 05:00", 12.2),
	    new DateStampedValue("1/28/2009 05:30", 12.8),
	    new DateStampedValue("1/28/2009 06:00", 11.6),
	    new DateStampedValue("1/28/2009 06:30", 12.5),
	    new DateStampedValue("1/28/2009 07:00", 11.4),
	    new DateStampedValue("1/28/2009 07:30", 12.9),
	    new DateStampedValue("1/28/2009 08:00", 12.9),
	    new DateStampedValue("1/28/2009 08:30", 11.2),
	    new DateStampedValue("1/28/2009 09:00", 11.7),
	    new DateStampedValue("1/28/2009 09:30", 12.4),
	    new DateStampedValue("1/28/2009 10:00", 14.4),
	    new DateStampedValue("1/28/2009 10:12", 13.7),
	    new DateStampedValue("1/28/2009 10:30", 11.9),
	    new DateStampedValue("1/28/2009 11:00", 14.3),
	    new DateStampedValue("1/28/2009 11:30", 14.0),
	    new DateStampedValue("1/28/2009 12:00", 14.7),
	    new DateStampedValue("1/28/2009 12:30", 15.4),
	    new DateStampedValue("1/28/2009 13:00", 15.5),
	};
	
	
	DateStampedValue[] cpuSequence = {
		    new DateStampedValue("1/28/2009 00:00", 3.0),
		    new DateStampedValue("1/28/2009 00:15", 2.9),
		    new DateStampedValue("1/28/2009 00:30", 4.9),
		    new DateStampedValue("1/28/2009 00:45", 3.9),
		    new DateStampedValue("1/28/2009 01:00", 3.0),
		    new DateStampedValue("1/28/2009 01:15", 1.9),
		    new DateStampedValue("1/28/2009 01:30", 4.9),
		    new DateStampedValue("1/28/2009 01:45", 8.9),
		    new DateStampedValue("1/28/2009 02:00", 13.0),
		    new DateStampedValue("1/28/2009 02:15", 12.9),
		    new DateStampedValue("1/28/2009 02:30", 7.9),
		    new DateStampedValue("1/28/2009 02:45", 2.9),
		    new DateStampedValue("1/28/2009 03:00", 3.0),
		    new DateStampedValue("1/28/2009 03:15", 2.9),
		    new DateStampedValue("1/28/2009 03:30", 2.9),
		    new DateStampedValue("1/28/2009 03:45", 2.9),
		    new DateStampedValue("1/28/2009 04:00", 3.0),
		    new DateStampedValue("1/28/2009 04:15", 2.1),
		    new DateStampedValue("1/28/2009 04:30", 11.9),
		    new DateStampedValue("1/28/2009 04:45", 53.1),
		    new DateStampedValue("1/28/2009 05:00", 73.0),
		    new DateStampedValue("1/28/2009 05:15", 86.0),
		    new DateStampedValue("1/28/2009 05:30", 93.9),
		    new DateStampedValue("1/28/2009 05:45", 92.9),
		    new DateStampedValue("1/28/2009 06:00", 73.0),
		    new DateStampedValue("1/28/2009 06:15", 52.9),
		    new DateStampedValue("1/28/2009 06:30", 22.9),
		    new DateStampedValue("1/28/2009 06:45", 12.9),
		    new DateStampedValue("1/28/2009 07:00", 3.0),
		    new DateStampedValue("1/28/2009 07:15", 2.9),
		    new DateStampedValue("1/28/2009 07:30", 2.9),
		    new DateStampedValue("1/28/2009 07:45", 1.5),
		    new DateStampedValue("1/28/2009 08:00", 23.0)

		};

}
