/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2009     *
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
 * Paging load configuration for the Exception List window.
 * @author Pete Harverson
 */
public class ExceptionPagingLoadConfig extends DatePagingLoadConfig
{
	/**
	 * Returns the level of noise to act as the filter for the exception list.
	 * @return the noise level, a value from 0 to 100.
	 */
	public int getNoiseLevel()
	{
		return (Integer) get("noiseLevel");
	}


	/**
	 * Sets the level of noise to act as the filter for the exception list.
	 * @param noiseLevel the noise level, a value from 0 to 100.
	 */
	public void setNoiseLevel(int noiseLevel)
	{
		set("noiseLevel", noiseLevel);
	}


	/**
	 * Returns the time window for the Exception List in which an item of
	 * evidence is examined to see if it is an 'exception'.
	 * @return the time window e.g. WEEK, DAY or HOUR.
	 */
	public TimeFrame getTimeWindow()
	{
		return (TimeFrame) get("timeWindow");
	}


	/**
	 * Sets the time window for the Exception List in which an item of
	 * evidence is examined to see if it is an 'exception'.
	 * @param timeWindow the time window e.g. WEEK, DAY or HOUR.
	 */
	public void setTimeWindow(TimeFrame timeWindow)
	{
		set("timeWindow", timeWindow);
	}
}
