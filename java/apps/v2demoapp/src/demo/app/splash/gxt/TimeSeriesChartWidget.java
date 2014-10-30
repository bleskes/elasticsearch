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

import java.util.Date;
import java.util.List;

import com.extjs.gxt.ui.client.event.Observable;
import com.google.gwt.user.client.ui.Widget;

import demo.app.data.Evidence;
import demo.app.data.TimeSeriesConfig;
import demo.app.data.Tool;


/**
 * Interface defining the methods that must be implemented by a GWT widget
 * holding a chart for displaying time series data.
 * @author Pete Harverson
 */
public interface TimeSeriesChartWidget extends Observable
{
	/**
	 * Returns the user interface Widget sub-class itself.
	 * @return the Widget which is added in the user interface.
	 */
	public Widget getChartWidget();
	
	
	/**
	 * Adds a time series to the chart.
	 * @param timeSeriesConfig TimeSeriesConfig object defining the properties of
	 * 		the time series to display (data type, metric, source, attributes etc).
	 */
	public void addTimeSeries(TimeSeriesConfig timeSeriesConfig);
	
	
	/**
	 * Adds a time series to the chart, with the line connecting the data points
	 * drawn in the specified colour.
	 * @param timeSeriesConfig TimeSeriesConfig object defining the properties of
	 * 		the time series to display (data type, metric, source, attributes etc).
	 * @param color  line colour, specified using the CSS hex colour notation e.g '#ff0000'. 
	 */
	public void addTimeSeries(TimeSeriesConfig timeSeriesConfig, String color);
	
	
	/**
	 * Removes the time series from the chart.
	 * @param timeSeriesConfig TimeSeriesConfig object defining the properties of
	 * 		the time series to remove (data type, metric, source, attributes).
	 */
	public void removeTimeSeries(TimeSeriesConfig timeSeriesConfig);
	
	
	/**
	 * Adds the specified notification data into the chart. Note that this may 
	 * not represent a single item of evidence but a number of items aggregated by
	 * time and description.
	 * @param notification notification to add to the chart.
	 */
	public void addNotification(Evidence notification);
	
	
	/**
	 * Removes the specified notification data into the chart. Note that this may 
	 * not represent a single item of evidence but a number of items aggregated by
	 * time and description.
	 * @param notification notification to remove from the chart.
	 */
	public void removeNotification(Evidence notification);
	
	
	/**
	 * Removes all time series from the chart.
	 */
	public void removeAllTimeSeries();
	
	
	/**
	 * Removes all notifications from the chart.
	 */
	public void removeAllNotifications();
	
	
	/**
	 * Removes all data from the chart i.e. time series, notifications and
	 * time marker.
	 */
	public void removeAll();
	
	
	/**
	 * Loads the data in the widget according to its current configuration.
	 * The lower and upper time bounds for the data loaded will be
	 * set according to the current time span of the chart.
	 */
	public void load();
	
	
	/**
	 * Loads the data for the specified time series configuration into the
	 * chart. The lower and upper time bounds for the data loaded will be
	 * set according to the current time span of the chart.
	 */
	public void load(TimeSeriesConfig timeSeriesConfig);
	
	
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
	 * of the time series data to load.
	 * @param startTime start time for the chart.
	 * @param endTime end time for the chart.
	 */
	public void setDateRange(Date startTime, Date endTime);
	
	
	/**
	 * Returns whether time series features should be highlighted on the chart.
     * @return <code>true</code> if any features are to be highlighted, 
     * 	<code>false</false> otherwise.
     */
    public boolean isHighlightingFeatures();


	/**
	 * Sets whether time series features should be highlighted on the chart.
     * @param markFeatues <code>true</code> if any features are to be highlighted, 
     * 	<code>false</false> otherwise.
     */
    public void setHighlightingFeatures(boolean highlight);
	
	
	/**
	 * Zooms in the date axis, centred around the current midpoint of the date range.
	 */
	public void zoomInDateAxis();
	
	
	/**
	 * Zooms out the date axis, centred around the current midpoint of the date range.
	 */
	public void zoomOutDateAxis();
	
	
	/**
	 * Pans the chart to the left.
	 */
	public void panLeft();
	
	
	/**
	 * Pans the chart to the right.
	 */
	public void panRight();
	
	
	/**
	 * Adds a marker to the chart to highlight a particular time interval.
	 * @param startTime the lower bound of the time interval to mark.
	 * @param endTime the upper bound of the time interval to mark.
	 */
	public void setTimeMarker(Date startTime, Date endTime);
	
	
	/**
	 * Clears a time marker from the chart, if one has been added.
	 */
	public void clearTimeMarker();
	
	
    /**
     * Sets the chart title to the specified text.
     * @param text the text for the title.
     */
    public void setChartTitle(String text);
    
    
    /**
     * Sets the chart subtitle to the specified text.
     * @param text the text for the subtitle).
     */
    public void setChartSubtitle(String text);
    
    
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
     * Sets the list of tools for launching other views from the chart.
     * @param viewTools the list of tools for launching other view types.
     */
    public void setViewTools(List<Tool> viewTools);
    
    
    /**
     * Returns the date/time of the point in the chart that is currently 'selected' 
     * e.g. when a context menu item has been run against a point in the chart.
     * @return the date/time of the selected point, or <code>null</code> if no
     * 		point is currently selected.
     */
    public Date getSelectedTime();
    
    
    /**
     * Returns the id of the notification that is currently 'selected' in the
     * chart e.g. when a context menu item has been run against a notification.
     * @return the id of the notification that is selected, or -1 if no notification
     * 		is currently selected.
     */
    public int getSelectedNotificationId();
    
    
    /**
	 * Returns the time series that is currently 'selected' in the chart 
	 * e.g. when a context menu item has been run against a time series data point.
	 * @return TimeSeriesConfig object defining the properties of the time series
	 *  	or <code>null</code> if no time series is currently selected.
	 */
    public TimeSeriesConfig getSelectedTimeSeries();
    
    
    /**
     * Returns the id of the time series feature that is currently 'selected' in the
     * chart e.g. when a context menu item has been run against a feature.
     * @return the id of the feature that is selected, or -1 if no feature
     * 		is currently selected.
     */
    public int getSelectedTimeSeriesFeatureId();
    
 
}
