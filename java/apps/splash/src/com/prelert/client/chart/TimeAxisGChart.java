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

import com.extjs.gxt.ui.client.GXT;
import com.extjs.gxt.ui.client.widget.Html;
import com.extjs.gxt.ui.client.widget.menu.Menu;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Event;
import com.googlecode.gchart.client.GChart;
import com.googlecode.gchart.client.HoverUpdateable;
import com.googlecode.gchart.client.GChart.Curve.Point;

import com.prelert.client.ClientUtil;


/**
 * Extension of the client-side GChart GWT widget 
 * (see {@link http://code.google.com/p/clientsidegchart/})
 * which plots data against time.
 * 
 * @author Pete Harverson
 */
public class TimeAxisGChart extends GChart
{
	
	protected Menu 					m_ContextMenu;
	
	protected Curve					m_TimeLine;
	
	private boolean					m_AutoValueRange;


	/**
	 * Creates a new chart for displaying time-based data.
	 */
	public TimeAxisGChart()
	{
		addStyleName("prl-timeSeriesChart");

		setBackgroundColor(USE_CSS);
		setBorderStyle(USE_CSS); 
		
		setChartSize(800, 410);
		setChartTitleThickness(0);
		setPadding("3px");
		setClipToPlotArea(true);
		
		// Configure the date/time x-axis.
		getXAxis().setAxisLabel("<b>" + ClientUtil.CLIENT_CONSTANTS.time() + "</b>");
		getXAxis().setHasGridlines(false);
		getXAxis().setTickCount(8);
		getXAxis().setTickLength(4);
		getXAxis().setTicksPerLabel(2);
		getXAxis().setAxisLabelThickness(16);
		getXAxis().setTickLabelThickness(8);
		// Except for "=(Date)", a standard GWT DateTimeFormat string
		getXAxis().setTickLabelFormat("=(Date)MMM dd HH:mm");
		
		m_AutoValueRange = true;


		// Configure the value y-axis.
	    getYAxis().setHasGridlines(false);
	    getYAxis().setTickCount(5);
	    getYAxis().setTickLength(4); 
	    getYAxis().setAxisMin(0);
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
	
	
	/**
	 * Returns the chart's context menu.
	 * @return the context menu, or <code>null</code> if no menu has been set.
	 */
	public Menu getContextMenu()
	{
		return m_ContextMenu;
	}

	
	/**
	 * Adds a 'timeline' to the chart to indicate a specific time.
	 * @param date date for the time line.
	 */
	public void setTimeMarker(Date time)
	{
		// Create the timeline if one has not yet been created.
		if (m_TimeLine == null)
		{
			addCurve();
			m_TimeLine = getCurve();
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
			m_TimeLine.clearPoints();
		}
		
		m_TimeLine.addPoint(time.getTime(), 0d);
		
		double yAxisMax;
		if (m_AutoValueRange == true)
		{
			yAxisMax = calculateYAxisMax();
		}
		else
		{
			yAxisMax = getYAxis().getAxisMax();
		}
		
		m_TimeLine.addPoint(time.getTime(), yAxisMax);
		
	}
	
	
	/**
	 * Removes the timeline if one has been added to the chart.
	 */
	public void clearTimeMarker()
	{
		if (m_TimeLine != null)
		{
			removeCurve(m_TimeLine);
			m_TimeLine = null;
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
	 * Returns the time corresponding to the start of the date axis.
	 * @return the starting date on the x-axis.
	 */
	public Date getDateAxisStart()
	{
		double xAxisMin = getXAxis().getAxisMin();
		return new Date((long)xAxisMin);
	}
	
	
	/**
	 * Specifies the starting point of the date axis.
	 * @param start the start date/time for the x-axis.
	 */
	public void setDateAxisStart(Date start)
	{
		// Note that any overlapping notifications will not be re-positioned.
		getXAxis().setAxisMin(start.getTime());
	}
	
	
	/**
	 * Returns the time corresponding to the end of the date axis.
	 * @return the end date on the x-axis.
	 */
	public Date getDateAxisEnd()
	{
		double xAxisMax = getXAxis().getAxisMax();
		return new Date((long)xAxisMax);
	}
	
	
	/**
	 * Sets the end point of the date axis.
	 * @param end the end date/time for the x-axis.
	 */
	public void setDateAxisEnd(Date end)
	{
		// Note that any overlapping notifications will not be re-positioned.
		getXAxis().setAxisMax(end.getTime());
	}
	
	
	/**
	 * Returns the time corresponding to the midpoint of the date axis.
	 * @return the midpoint date on the x-axis.
	 */
	public Date getDateAxisMidpoint()
	{
		double xAxisMin = getXAxis().getAxisMin();
		double xAxisMax = getXAxis().getAxisMax();
		return new Date((long)(xAxisMin + xAxisMax)/2);
	}
	
	
	/**
	 * Returns the time span of the x-axis.
	 * @return the number of milliseconds between the start and end points of the
	 * 		date axis.
	 */
	public long getDateAxisSpan()
	{
		return (long)(getXAxis().getAxisMax() - getXAxis().getAxisMin());
	}
	
	
	/**
	 * Returns whether or not the value axis range is automatically adjusted 
	 * to fit the data.
	 * @return <code>true</code> if the value range is adjusted automatically,
	 * 		<code>false</code> otherwise.
	 */
	public boolean isAutoValueRange()
	{
		return m_AutoValueRange;
	}
	
	
	/**
	 * Sets a flag that determines whether or not the value axis range is 
	 * automatically adjusted to fit the data.
	 * @param auto auto range flag.
	 */
	public void setAutoValueRange(boolean auto)
	{
		m_AutoValueRange = auto;
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
			// Fudge to 'hide' labels.
			// Padding is needed to ensure white tick labels do not show on top
			// of off-chart scatter dot symbols.
			getYAxis().setTickLabelThickness(25);
		    getYAxis().setTickLabelPadding(10);
			getYAxis().setTickLabelFontColor("white");
		}
		else
		{
			getYAxis().setTickLabelThickness(100);
			getYAxis().setTickLabelFontColor(DEFAULT_TICK_LABEL_FONT_COLOR);
		}
	}
	
	
	/**
     * Delegates handling of any 'sunk' browser events to any registered listeners.
     * @param event the browser event that has been received.
     */
    @Override
    public void onBrowserEvent(Event event) 
    {
    	// Pass event on (a GChart should respond to mouse activity  - but never eat it).
    	super.onBrowserEvent(event);
    	
        //if (event.getTypeInt() == (GXT.isSafari && GXT.isMac ? Event.ONMOUSEDOWN : Event.ONMOUSEUP)
		//        && isRightClick(event))
    	// Note that the ONCONTEXTMENU does not fire in Opera.
    	if (event.getTypeInt() == Event.ONCONTEXTMENU)
		{
        	onContextMenu(event);
		}

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
			event.stopPropagation();
        	event.preventDefault();
        	
        	Point touchedPoint = getTouchedPoint();
        	if ( (touchedPoint != null) && (touchedPoint.getParent() != m_TimeLine) )
        	{        		
				final int x = event.getClientX();
				final int y = event.getClientY();
	
					DeferredCommand.addCommand(new Command()
					{
						public void execute()
						{
							GWT.log("Number of menu items:" + m_ContextMenu.getItemCount());
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
			if (DOM.eventGetButton(event) == NativeEvent.BUTTON_RIGHT
			        || (GXT.isMac && DOM.eventGetCtrlKey(event)))
			{
				return true;
			}
		}
		return false;
	}
	
	
	/**
	 * Calculates the maximum value to be displayed on the y axis.
	 * @param records the records being displayed in the chart.
	 * @return minimum value visible on the y axis.
	 */
	protected double calculateYAxisMax()
	{
		// Minimum max value on the y-axis is 10 for the case where there are 
		// no records, or the maximum usage value is less than 10.
		double maxVal = Math.max(9, getYAxis().getDataMax()); 	
		
		double powerFloor = Math.floor(Math.log10(maxVal)); // e.g. gives 5 for 416521
		double raiseToPower = Math.pow(10, powerFloor); // e.g. 100000 for 416521
		
		double nearestMax = (Math.floor(maxVal/raiseToPower) + 1) * raiseToPower; // e.g. 500000 for 416521
	
		return nearestMax;
	}
	
	
	/**
	 * Widget that provides tooltips for points on a GChart so that the tooltip is
	 * located in a direction from the hovered over point towards the centre of
	 * the chart. This ensures the tooltip is always fully visible in the chart area.
	 */
	class CentringHoverAnnotation extends Html implements HoverUpdateable
	{
		@Override
		public void hoverUpdate(Curve.Point hoveredOver)
		{
			Axis xAxis = getXAxis();
			double xAxisMidPoint = (xAxis.getAxisMin() + xAxis.getAxisMax())/2;
			
			Axis yAxis = getYAxis();
			double yAxisTwoThirds = 2*(yAxis.getAxisMin() + yAxis.getAxisMax())/3;
			
			Symbol symbol = hoveredOver.getParent().getSymbol();
			
			if (hoveredOver.getX() <= xAxisMidPoint)
			{
				if (hoveredOver.getY() <= yAxisTwoThirds)
				{
					symbol.setHoverLocation(AnnotationLocation.NORTHEAST);
				}
				else
				{
					symbol.setHoverLocation(AnnotationLocation.SOUTHEAST);
				}
			}
			else
			{
				if (hoveredOver.getY() <= yAxisTwoThirds)
				{
					symbol.setHoverLocation(AnnotationLocation.NORTHWEST);
				}
				else
				{
					symbol.setHoverLocation(AnnotationLocation.SOUTHWEST);
				}
			}
			
			setHtml(hoveredOver.getHovertext());
		}

		
		@Override
		public void hoverCleanup(Curve.Point hoveredAwayFrom)
		{
		}
	}
	
}
