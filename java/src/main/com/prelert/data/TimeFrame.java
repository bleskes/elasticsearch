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

package com.prelert.data;

import java.util.Date;

/**
 * Enum representing a time frame e.g. a week, day or hour.
 * @author Pete Harverson
 */
public enum TimeFrame
{
	ALL, WEEK, DAY, HOUR, MINUTE, SECOND;

	static final private long WEEK_IN_MS = 604800000l;  
	static final private long DAY_IN_MS = 86400000l;  
	static final private long HOUR_IN_MS = 3600000l;  
	static final private long MINUTE_IN_MS = 60000l;  
	static final private long SECOND_IN_MS = 1000l;  
	
	/**
	 * Returns the number of milliseconds in the interval corresponding to this
	 * time frame i.e. number of milliseconds in a week / day / hour.
	 * @return the number of milliseconds in the time frame interval.
	 */
	public long getInterval()
	{
		long interval = 0l;
		switch (this)
		{
			case ALL:
				return Long.MAX_VALUE;
				
			case WEEK:
				return WEEK_IN_MS;
			
			case DAY:
				return DAY_IN_MS;
				
			case HOUR:
				return HOUR_IN_MS;
				
			case MINUTE:
				return MINUTE_IN_MS;
				
			case SECOND:
				return SECOND_IN_MS;
		}
		
		return interval;
	}
	
	/**
	 * Returns the time frame for the period between <code>start</code>
	 * and <code>end</code> Dates.
	 * @param start: start Date
	 * @param end: finish Date
	 * @return TimeFrame
	 */
	static public TimeFrame timeFrameFromInterval(Date start, Date end)
	{
		long diff = Math.abs(end.getTime() - start.getTime());
		
		if (diff < SECOND_IN_MS)
		{
			return SECOND; 
		}
		else if (diff < MINUTE_IN_MS)
		{
			return MINUTE;
		}
		else if (diff < HOUR_IN_MS)
		{
			return HOUR;
		}
		else if (diff < DAY_IN_MS)
		{
			return DAY;
		}
		else if (diff < WEEK_IN_MS)
		{
			return WEEK;
		}
		else 
		{
			return ALL;			
		}
	}
}
