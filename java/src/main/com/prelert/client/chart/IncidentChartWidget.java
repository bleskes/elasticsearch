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

import com.extjs.gxt.ui.client.event.SelectionProvider;

import com.prelert.data.gxt.IncidentModel;


/**
 * Extension of the ChartWidget interface defining the methods to be implemented
 * by widgets displaying incidents.
 * @author Pete Harverson
 */
public interface IncidentChartWidget extends ChartWidget<IncidentModel>, SelectionProvider<IncidentModel>
{
	/**
	 * Adds an incident to the chart widget.
	 * @param incident the incident to add.
	 */
	public void addIncident(IncidentModel incident);
	
	
	/**
	 * Removes all incidents from the chart widget.
	 */
	public void removeAllIncidents();
	
	
	/**
	 * If currently displayed on the chart, returns the incident with the specified
	 * 'headline' evidence ID.
	 * @param evidenceId headline notification or time series feature evidence ID.
	 * @return The incident with the specified 'headline' evidence ID if it is
	 * 	currently displayed on the chart, or <code>null</code> otherwise.
	 */
	public IncidentModel getIncident(int evidenceId);
	
	
	/**
	 * Returns the anomaly threshold of the timeline, which should be a value 
	 * between 1 and 100. A value of 1 means that all incidents will be displayed,
	 * whilst a value of 100 indicates that only the most infrequent (most 'anomalous')
	 * incidents are shown. 
	 * @return the anomaly threshold.
	 */
	public int getAnomalyThreshold();
	
	
	/**
	 * Sets the anomaly threshold of the timeline, which should be a value 
	 * between 1 and 100. A value of 1 means that all incidents will be displayed,
	 * whilst a value of 100 indicates that only the most infrequent (most 'anomalous')
	 * incidents are shown. 
	 * @param threshold the anomaly threshold.
	 */
	public void setAnomalyThreshold(int threshold);
	
}
