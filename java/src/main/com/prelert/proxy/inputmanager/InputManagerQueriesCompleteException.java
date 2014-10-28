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

package com.prelert.proxy.inputmanager;

import java.util.Date;

/**
 * Exception thrown when a Historical input manager has finished 
 * running all its queries.
 */
public class InputManagerQueriesCompleteException extends Exception 
{
	private static final long serialVersionUID = 8369907633597391093L;
	
	private final Date m_TimeOfLastQueryDataPoint; 
	private final boolean m_ContinueCollectingRealTime;

	public InputManagerQueriesCompleteException()
	{
		super("Query complete");
		
		m_ContinueCollectingRealTime = false;
		m_TimeOfLastQueryDataPoint = null;
	}
	
	public InputManagerQueriesCompleteException(Date lastTime)
	{
		super("Query complete with the last data point at time = " + lastTime);
		
		m_ContinueCollectingRealTime = true;
		m_TimeOfLastQueryDataPoint = lastTime;
	}
	

	/**
	 * If true then the historical queries have completed 
	 * and the input manager should start collecting data in real-time.
	 * @return
	 */
	public boolean isContinueCollectionRealTime()
	{
		return m_ContinueCollectingRealTime;
	}
	
	/**
	 * Date may be <code>null</code> if not set.
	 * @return May be <code>null</code>
	 */
	public Date getTimeOfLastQueryDataPoint()
	{
		return m_TimeOfLastQueryDataPoint;
	}
	
}
