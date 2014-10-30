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

/**
 * Enum representing a time frame e.g. a week, day or hour.
 * @author Pete Harverson
 */
public enum TimeFrame
{
	ALL, WEEK, DAY, HOUR, MINUTE, SECOND;

	
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
				return 604800000l;
			
			case DAY:
				return 86400000l;
				
			case HOUR:
				return 3600000l;
				
			case MINUTE:
				return 60000l;
				
			case SECOND:
				return 1000l;
				
		}
		
		return interval;
	}
}
