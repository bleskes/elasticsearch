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

package com.prelert.service;

import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.prelert.data.Incident;


/**
 * Defines the methods to be implemented by the asynchronous client interface
 * to the Incident query service.
 * @author Pete Harverson
 */
public interface IncidentQueryServiceAsync
{
	/**
	 * Returns a list of incidents for the specified time window and anomaly threshold.
	 * @param minTime minimum (earliest) date/time to include.
	 * @param maxTime maximum (latest) date/time to include.
	 * @param anomalyThreshold minimum anomaly score threshold of incidents to return,
	 * 	which should be a value between 1 and 100. A value of 1 will return all incidents,
	 * 	whilst a value of 100 will return only the most infrequent (most 'anomalous') incidents.
	 * @param callback callback object to receive a response from the remote procedure call.
	 */
	public void getIncidents(Date minTime, Date maxTime, int anomalyThreshold,
			AsyncCallback<List<Incident>> callback);
	
	/**
	 * Returns the date/time of the earliest incident in the database.
	 * @return callback callback object to receive a response from the remote procedure call.
	 */
	public void getEarliestTime(AsyncCallback<Date> callback);
	
	
	/**
	 * Returns the date/time of the latest incident in the database.
	 * @return callback callback object to receive a response from the remote procedure call.
	 */
	public void getLatestTime(AsyncCallback<Date> callback);
	
	
	/**
	 * Returns the frequency for automatic refresh of the incidents time line, 
	 * in seconds.
	 * @param callback callback object to receive a response from the remote procedure call.
	 */
	public void getTimelineAutoRefreshFrequency(AsyncCallback<Integer> callback);
}
