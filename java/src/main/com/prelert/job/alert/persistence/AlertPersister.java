/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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
 * on +44 (0)20 7953 7243 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/

package com.prelert.job.alert.persistence;

import java.io.IOException;
import java.util.List;

import com.prelert.job.UnknownJobException;
import com.prelert.job.alert.Alert;
import com.prelert.rs.data.Pagination;

public interface AlertPersister 
{
	/**
	 * Persist the alert 
	 * @param alertId
	 * @param jobId
	 * @param alert
	 */
	public void persistAlert(String alertId, String jobId, Alert alert) 
	throws IOException;
	
	
	/**
	 * Get a page of alerts from <i>all</i> jobs 
	 * optionally filtered by date
	 * 
	 * @param skip Skip the first N alerts
	 * @param take Return at most this many alerts
	 * @param startEpoch Get alerts occuring after or at this time. 
	 * If <= 0 then ignore.
	 * @param endEpoch Get alerts before this time. If <= 0 then ignore
	 * @return
	 */
	public Pagination<Alert> alerts(int skip, int take, 
			long startEpoch, long endEpoch);
	
	
	/**
	 * Get a page of alerts for the job optionally filtered by 
	 * date 
	 * 
	 * @param jobId  
	 * @param skip Skip the first N alerts
	 * @param take Return at most this many alerts
	 * @param startEpoch Get alerts occuring after or at this time. 
	 * If <= 0 then ignore.
	 * @param endEpoch Get alerts before this time. If <= 0 then ignore
	 * @return
	 */
	public Pagination<Alert> alertsForJob(String jobId,
			int skip, int take, long startEpoch, long endEpoch)
	throws UnknownJobException;
	
	
	/**
	 * Get the Id of the latest alert in the datastore.
	 * 
	 * Alert Ids are a sequence the next in the sequence 
	 * must follow this one.
	 * 
	 * @return The last alert id or <code>null</code> if no 
	 * alert Ids in the datastore
	 */
	public String lastAlertId();
	
	
	/**
	 * Return all alerts in the datastore occurring after 
	 * <code>alertId</code> but not including <code>alertId</code>.
	 * Alert Ids are sequence numbers.
	 * 
	 * @param alertId.
	 * @return
	 */
	public List<Alert> alertsAfter(String alertId);
	
	
	/**
	 * Return all alerts for the given job in the datastore 
	 * occurring after <code>alertId</code> but not including 
	 * <code>alertId</code>.
	 *
	 * Alert Ids are sequence numbers.
	 * 
	 * @param alertId
	 * @param jobId
	 * @return
	 */
	public List<Alert> alertsAfter(String alertId, String jobId);
}
