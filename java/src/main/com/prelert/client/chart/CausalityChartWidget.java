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
import java.util.Map;

import com.prelert.client.CSSSymbolChart.Shape;

import com.prelert.data.gxt.CausalityDataModel;
import com.prelert.data.gxt.ProbableCauseModel;
import com.prelert.data.gxt.ProbableCauseModelCollection;


/**
 * Extension of the TimeSeriesChartWidget interface defining the methods to be 
 * implemented by chart widgets displaying causality data.
 * @author Pete Harverson
 */
public interface CausalityChartWidget extends TimeSeriesChartWidget
{
	
	/**
	 * Adds the specified collection of notification type probable causes to
	 * the chart.
	 * @param collection notification type ProbableCauseModelCollection to add.
	 */
	public void addNotifications(ProbableCauseModelCollection collection);
	
	
	/**
	 * Removes the specified collection of notification type probable causes from
	 * the chart.
	 * @param collection notification type ProbableCauseModelCollection to remove.
	 */
	public void removeNotifications(ProbableCauseModelCollection collection);
	
	
	/**
	 * Adds a time series type probable cause from the specified aggregation to
	 * the chart.
	 * @param collection ProbableCauseModelCollection containing the probable cause.
	 * @param probCause time series type ProbableCauseModel to add.
	 */
	public void addTimeSeries(ProbableCauseModelCollection collection,
			ProbableCauseModel probCause);
	
	
	/**
	 * Removes the time series type probable cause from the specified aggregation
	 * from the chart.
	 * @param collection ProbableCauseModelCollection containing the probable cause.
	 * @param probCause time series type ProbableCauseModel to remove.
	 */
	public void removeTimeSeries(
			ProbableCauseModelCollection collection, ProbableCauseModel probCause);
	
	
	/**
     * Returns the line colour being used for plotting the specified aggregation.
     * @param collection ProbableCauseModelCollection for which to obtain the line colour.
     * @return CSS hex colour notation e.g. '#ff0000'. 
     */
	public String getLineColour(ProbableCauseModelCollection collection);
	
	
	/**
	 * Adds the specified causality data to the chart.
	 * @param causalityData the notifications or time series to add.
	 */
	public void addCausalityData(CausalityDataModel causalityData);
	
	
	/**
	 * Removes the specified causality data from the chart.
	 * @param causalityData the notifications or time series to remove.
	 */
	public void removeCausalityData(CausalityDataModel causalityData);
	
	
	/**
	 * Returns a list of all the causality data that is currently plotted on the chart.
	 * @return list of causality data shown on the chart.
	 */
	public List<CausalityDataModel> getCausalityData();
	
	
	/**
	 * Sets a map of the peak values for each time series type, as identified by
	 * data type and metric e.g. system_udp/packets_received.
	 * @param peakValuesByTypeId
	 */
	public void setPeakValuesByTypeId(Map<Integer, Double> peakValuesByTypeId);
	
	
	/**
	 * Returns whether the specified causality data is currently displayed on the chart.
	 * @param causalityData notifications or time series causality data.
	 * @return <code>true</code> if it is displayed, <code>false</code> if it has 
	 * 	not been added to the chart.
	 */
	public boolean isDisplayedOnChart(CausalityDataModel causalityData);
	
	
	/**
     * Returns the line colour being used for plotting the item of causality data.
     * @param causalityData causality data for which to obtain the line colour.
     * @return CSS hex colour notation e.g. '#ff0000'. 
     */
	public String getLineColour(CausalityDataModel causalityData);
	
	
	/**
	 * Returns the symbol shape being used for plotting the item of causality data.
	 * @param causalityData causality data for which to obtain the symbol shape.
	 * @return the symbol shape, or <code>null</code> if the data is plotted as a line.
	 */
	public Shape getSymbolShape(CausalityDataModel causalityData);
	
	
	/**
	 * Scales the chart so that all time series currently displayed are fully visible,
	 * bringing all points into the plot area.
	 */
	public void scaleToFit();
	
	
	/**
	 * Returns the factor by which the y axis has been scaled relative to the 
	 * default scale against which time series of different types are normalised
	 * prior to display.
	 * @return the current scaling factor, which will be 1 if the chart has not been
	 * 	scaled to fit against a non-full scale series.
	 */
	public double getYAxisScalingFactor();
    
}
