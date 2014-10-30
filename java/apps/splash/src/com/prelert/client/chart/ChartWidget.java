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

import com.extjs.gxt.ui.client.data.Loader;
import com.google.gwt.user.client.ui.Widget;
import com.prelert.client.event.ChartWidgetListener;


/**
 * Interface defining the methods that must be implemented by a GWT widget
 * holding a chart for displaying time based data.
 * 
 * @param <D> the data type being returned by this loader
 * @author Pete Harverson
 */
public interface ChartWidget<D> extends Loader<D>
{
	/**
	 * Returns the user interface Widget sub-class itself.
	 * @return the Widget which is added in the user interface.
	 */
	public Widget getChartWidget();
	
	
	/**
	 * Loads the data in the widget according to its current configuration.
	 * The lower and upper time bounds for the data loaded will be
	 * set according to the current time span of the chart.
	 * @return <code>true</code> if the load was requested.
	 */
	public boolean load();
	
	
	/**
	 * Sets the flag that determines whether or not the tick labels on the value
	 * axis are visible.
	 * @param visible <code>true</code> for the tick labels to be visible (the
	 * 	default), <code>false</code> otherwise.
	 */
	public void setValueTickLabelsVisible(boolean visible);
	
	
	/**
	 * Sets a flag that determines whether or not the value axis range is 
	 * automatically adjusted to fit the data.
	 * @param auto auto range flag.
	 */
	public void setAutoValueRange(boolean auto);
	
	
	/**
	 * Sets the value range for the chart. This defines the lower and upper bounds
	 * displayed on the value (y) axis.
	 * @param minValue minimum value visible on the value axis.
	 * @param maxValue maximum value visible on the value axis.
	 */
	public void setValueRange(double minValue, double maxValue);
	

	/**
	 * Sets the date range for the chart. This defines the start and end times
	 * of the data to load.
	 * @param startTime start time for the chart.
	 * @param endTime end time for the chart.
	 */
	public void setDateRange(Date startTime, Date endTime);
	
	
	/**
	 * Returns the start date/time of the visible region of the chart.
	 * @return the start date/time.
	 */
	public Date getStartTime();
	
	
	/**
	 * Returns the end date/time of the visible region of the chart.
	 * @return the end date/time.
	 */
	public Date getEndTime();
	

    /**
     * Returns the date/time of the point in the chart that is currently 'selected' 
     * e.g. when a context menu item has been run against a point in the chart.
     * @return the date/time of the selected point, or <code>null</code> if no
     * 		point is currently selected.
     */
    public Date getSelectedTime();
	
	
	/**
	 * Zooms in the date axis, centred around the current midpoint of the date range.
	 */
	public void zoomInDateAxis();
	
	
	/**
	 * Zooms out the date axis, centred around the current midpoint of the date range.
	 */
	public void zoomOutDateAxis();
	
	
	/**
	 * Returns whether or not the chart is set to the maximum zoom level.
	 * @return <code>true</code> if the chart is set to the maximum zoom, 
	 * 	<code>false</code> otherwise.
	 */
	public boolean isMaxZoomLevel();
	
	
	/**
	 * Returns whether or not the chart is set to the minimum zoom level.
	 * @return <code>true</code> if the chart is set to the minimum zoom 
	 * 	<code>false</code> otherwise.
	 */
	public boolean isMinZoomLevel();
	
	
	/**
	 * Pans the chart to the left.
	 */
	public void panLeft();
	
	
	/**
	 * Pans the chart to the right.
	 */
	public void panRight();
	
	
	/**
	 * Adds a marker to the chart to indicate a particular time.
	 * @param time the date/time to mark.
	 */
	public void setTimeMarker(Date time);
	
	
	/**
	 * Clears a time marker from the chart, if one has been added.
	 */
	public void clearTimeMarker();
    
    
    /**
     * Sets the size of the chart, which should include space for axis tick
     * marks and labels.
     * @param width width of the chart.
     * @param height height of the chart.
     */
    public void setChartSize(int width, int height);
    
    
	/**
	 * Sets the width of the chart, which should include space for y-axis 
	 * tick marks and labels.
	 * @param width width of the chart.
	 */
    public void setChartWidth(int width);
    
    
    /**
     * Sets the height of the chart, which should include space for x-axis 
	 * tick marks and labels.
     * @param height height of the chart.
     */
    public void setChartHeight(int height);
    
    
    /**
     * Adds a listener for <code>ChartWidgetEvents</code>.
     * @param listener the <code>ChartWidgetListener</code> to add.
     */
    public void addChartWidgetListener(ChartWidgetListener listener);
    
    
    /**
     * Removes a listener for <code>ChartWidgetEvents</code>.
     * @param listener the <code>ChartWidgetListener</code> to remove.
     */
    public void removeChartWidgetListener(ChartWidgetListener listener);

}
