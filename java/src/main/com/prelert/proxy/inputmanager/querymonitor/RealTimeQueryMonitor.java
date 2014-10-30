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

import java.util.Date;

public class RealTimeQueryMonitor implements QueryMonitorPolicy
{
	// Default max age query for points older than this value 
	static private final long DEFAULT_MAX_AGE_FOR_POINTS_QUERY_MS = 4 * 60 * 1000;
	
	// Max time period for a query.
	static private final int DEFAULT_MAX_DURATION_FOR_QUERY_MS = 2 * 60 * 1000;  

	// Max time-slice for a query. Don't grab data in chunks larger than this.
	static private final int DEFAULT_MAX_TIMESLICE_FOR_QUERY_MS = 4 * 60 * 1000;  
	
	
	private Date m_QueryStartTime;
	private long m_MaxAgeForPointsMs;
	private long m_MaxTimeSliceForQueryParams;
	private long m_MaxQueryDuration;
	
	
	/**
	 *  Override the default constructor with package visibility. 
	 */
	public RealTimeQueryMonitor()
	{
		m_QueryStartTime = new Date();
		
		m_MaxAgeForPointsMs = DEFAULT_MAX_AGE_FOR_POINTS_QUERY_MS;
		m_MaxTimeSliceForQueryParams = DEFAULT_MAX_TIMESLICE_FOR_QUERY_MS;
		m_MaxQueryDuration = DEFAULT_MAX_DURATION_FOR_QUERY_MS;
	}
	
	/**
	 * Create a new RealTimeQueryMonitor with the params.
	 * 
	 * @param maxAgeForPointsMs Oldest points which can be queried for 
	 *                          relative to now.
	 * @param maxTimesliceLength Max time-slice length
	 * @param maxDuration Max duration of a query.
	 */
	public RealTimeQueryMonitor(long maxAgeForPointsMs, long maxTimesliceLength, long maxDuration)
	{
		m_QueryStartTime = new Date();
		
		m_MaxAgeForPointsMs = maxAgeForPointsMs;
		m_MaxTimeSliceForQueryParams = maxTimesliceLength;
		m_MaxQueryDuration = maxDuration;
	}

	
	/**
	 * Validate the span of the arguments. A QueryMonitorPolicy may limit the 
	 * length of a time a query is done for (end - start), if so start will
	 * be set to a new valid time and false returned.
	 * 
	 * @param start The start time argument to the query.
	 * 				If this function returns false start is modified to a 
	 * 			    valid time.
	 * @param end The end time argument to the query.
	 * @return
	 */
	@Override
	public boolean validateQueryArgsTimeSpan(Date start, Date end)
	{
		if ((end.getTime() - start.getTime()) > m_MaxTimeSliceForQueryParams)
		{
			start.setTime(end.getTime() - m_MaxTimeSliceForQueryParams);
			return false;
		}
		
		return true;
	}

	/**
	 * Validate the age of the start argument. A QueryMonitorPolicy may limit 
	 * the age of the start of the query to some value. In that case start will
	 * be changed to a new value and false returned.
	 * 
	 * @param start The start time argument to the query.
	 * 				If this function returns false start is modified to a 
	 * 			    valid time.
	 * @return True if
	 */
	@Override
	public boolean validateQueryDateParamsAge(Date start)
	{
		Date now = new Date();
		if (now.getTime() - start.getTime() > m_MaxAgeForPointsMs)
		{
			// We should not query for points older than m_MaxAgeForPointsMs
			start.setTime(now.getTime() - m_MaxAgeForPointsMs);
			return false;
		}
		
		return true;
	}
	
	@Override
	public boolean wasQueryInsideTimeLimit()
	{
		return ((new Date().getTime() - m_QueryStartTime.getTime()) < m_MaxQueryDuration);
		
	}
	
}
