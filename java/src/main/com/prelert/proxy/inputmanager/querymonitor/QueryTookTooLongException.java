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

package com.prelert.proxy.inputmanager.querymonitor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.prelert.data.Notification;
import com.prelert.data.TimeSeriesData;

/**
 * Exception is thrown when a query collecting Time Series data or
 * Notifications takes too long to execute. In some cases the partial
 * results may be returned. 
 * 
 * The wait period is the suggested number of ms the process should 
 * wait for before retrying.
 */
public class QueryTookTooLongException extends Exception  
{
	private static final long serialVersionUID = 5324642496397472727L;

	private long m_WaitMs;
	private List<Notification> m_Notifications;
	private Collection<TimeSeriesData> m_TimeSeriesData;

	public QueryTookTooLongException(String msg, List<Notification> notifications, long waitMs)
	{
		super(msg);
		
		m_WaitMs = waitMs;
		m_Notifications = notifications;
		m_TimeSeriesData = Collections.emptyList();
	}
	
	public QueryTookTooLongException(String msg, Collection<TimeSeriesData> timeSeriesData, long waitMs)
	{
		super(msg);
		
		m_WaitMs = waitMs;
		m_TimeSeriesData = timeSeriesData; 
		m_Notifications = Collections.emptyList();
	}
	
	
	public List<Notification> getNotificationsPartialResults()
	{
		return m_Notifications;
	}
	
	public Collection<TimeSeriesData> getTimeSeriesPartialResults()
	{
		return m_TimeSeriesData;
	}
	
	public long getWaitMs()
	{
		return m_WaitMs;
	}
}
