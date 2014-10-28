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

import com.prelert.data.gxt.EvidenceModel;
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
     * Returns the notification that is currently 'selected' in the chart e.g. when
     * a context menu item has been run against a notification type probable cause.
     * Note that only the key fields in the model will be set (id, data type, source, time).
     * @return the notification that is selected, or <code>null</code> if no 
     * 		notification is currently selected.
     */
    public EvidenceModel getSelectedNotification();
    
}
