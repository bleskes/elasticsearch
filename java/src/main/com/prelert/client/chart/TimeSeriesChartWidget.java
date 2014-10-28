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

package com.prelert.client.chart;

import java.util.List;

import com.prelert.data.DataSourceType;
import com.prelert.data.TimeSeriesConfig;
import com.prelert.data.TimeSeriesDataPoint;


/**
 * Extension of the ChartWidget interface defining the methods to be implemented
 * by widgets displaying time series data.
 * 
 * @author Pete Harverson
 */
public interface TimeSeriesChartWidget extends ChartWidget<TimeSeriesDataPoint>
{

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
	 * Removes all time series from the chart.
	 */
	public void removeAllTimeSeries();
	
	
	/**
	 * Removes all data from the chart i.e. time series, notifications and
	 * time marker.
	 */
	public void removeAll();
	
	
	/**
     * Returns the line colour being used for plotting the specified time series 
     * configuration.
     * @param config time series configuration for which to obtain the line colour.
     * @return CSS hex colour notation e.g. '#ff0000'. 
     */
    public String getLineColour(TimeSeriesConfig config);

	
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
     * Sets the list of data types that can be linked to from points in the chart.
     * @param dataSourceTypes list of data types which should be accessible from the chart.
     */
    public void setLinkToDataTypes(List<DataSourceType> dataSourceTypes);
    
    
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
     * @return the id of the feature that is selected, or 0 if no feature
     * 		is currently selected.
     */
    public int getSelectedTimeSeriesFeatureId();
    
 
}
