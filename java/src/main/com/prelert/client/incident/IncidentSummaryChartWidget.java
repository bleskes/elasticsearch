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

package com.prelert.client.incident;

import java.util.List;

import com.google.gwt.user.client.ui.Widget;

import com.prelert.data.gxt.CausalityAggregateModel;


/**
 * Interface defining the methods to be implemented by a GWT widget for
 * charting the summary of an incident.
 * @author Pete Harverson
 */
public interface IncidentSummaryChartWidget
{
	
	/**
	 * Sets the list of aggregated causality data representing the summary of an
	 * incident for display in the chart.
	 * @param aggregateList  incident summary, as a list of aggregated causality objects.
	 */
	public void setCausalityData(List<CausalityAggregateModel> aggregateList);
	
	
	/**
	 * Removes all data from the chart.
	 */
	public void removeAll();
	
	
	/**
	 * Returns the user interface Widget sub-class itself.
	 * @return the Widget which is added in the user interface.
	 */
	public Widget getChartWidget();
	
	
    /**
     * Sets the size of the chart, which should include space for axis tick
     * marks and labels.
     * @param width width of the chart, including plot area, axes, ticks and labels.
     * @param height height of the chart, including plot area, axes, ticks and labels.
     */
    public void setChartSize(int width, int height);
    
    
	/**
	 * Sets the width of the chart.
	 * @param width width of the chart, including plot area, axes and any ticks and labels.
	 */
    public void setChartWidth(int width);
    
    
    /**
     * Sets the height of the chart, which should include space for x-axis 
	 * tick marks and labels.
     * @param height height of the chart, including plot area, axes, ticks and labels.
     */
    public void setChartHeight(int height);
}
