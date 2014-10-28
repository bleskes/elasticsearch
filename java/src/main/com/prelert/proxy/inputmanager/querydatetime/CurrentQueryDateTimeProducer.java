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

package com.prelert.proxy.inputmanager.querydatetime;

import java.util.Date;

import com.prelert.proxy.inputmanager.DataCollectionMode;

/** 
 * For input managers collecting real time data this class 
 * returns the query start and end dates and the amount of 
 * time the inputmanager should sleep before making another 
 * query.
 * A configurable delay can be set so the queries run slightly 
 * behind the current time.
 */
public class CurrentQueryDateTimeProducer extends QueryDateTimeProducer 
{
	private Date m_StartDate;
	private final long m_UpdateIntervalMs;
	private long m_DelayMs;
	
	/**
	 * Default constructor sets update interval to the default.
	 */
	public CurrentQueryDateTimeProducer()
	{
		m_UpdateIntervalMs = DEFAULT_UPDATE_INTERVAL_SECS * 1000;
		m_DataCollectionMode = DataCollectionMode.REALTIME;
		
		m_DelayMs = 0;
	}
	
	/**
	 * 
	 * @param updateIntervalSecs sets the minimum upate interval to this. 
	 */
	public CurrentQueryDateTimeProducer(long updateIntervalSecs)
	{
		m_UpdateIntervalMs = updateIntervalSecs * 1000;
		m_DataCollectionMode = DataCollectionMode.REALTIME;
		
		m_DelayMs = 0;
	}
	
	
	/**
	 * Set the delay value. This is the period behind 
	 * real time that the queries run in.
	 *  
	 * The query end date is always now - delay secs.
	 * @param delaySecs
	 */
	public void setDelaySeconds(long delaySecs)
	{
		m_DelayMs = delaySecs * 1000;
	}
	
	public long getDelaySeconds()
	{
		return m_DelayMs / 1000;
	}
	
	
	/**
	 * If the start date has never been requested before 
	 * the time now is returned.
	 */
	@Override
	public Date getQueryStartDate() 
	{
		if (m_StartDate == null)
		{
			m_StartDate = new Date(new Date().getTime() - m_UpdateIntervalMs - m_DelayMs);
		}
		return m_StartDate;
	}
	
	@Override
	public Date getQueryEndDate() 
	{
		return new Date(new Date().getTime() - m_DelayMs);
	}
	
	@Override
	public void setLastQueryEndTime(Date value) 
	{
		// next start is 1ms after the end
		m_StartDate = new Date(value.getTime() + 1);
	}
	
	public long getSleepTimeMs(long lastQueryDurationMs)
	{
		if (lastQueryDurationMs < m_UpdateIntervalMs)
		{
			return m_UpdateIntervalMs - lastQueryDurationMs;
		}
		else 
		{
			return 0;
		}
	}

}
