/************************************************************
 *                                                          *
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2012     *
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
import com.prelert.proxy.inputmanager.InputManagerQueriesCompleteException;

/**
 * Abstract class used by the InputManagers for tracking query 
 * start and end times. 
 * 
 * Implementing subclasses should set the DataCollectionMode
 * member for their particular implementation. 
 */
abstract public class QueryDateTimeProducer 
{
	/**
	 * Must be set by implementing subclasses
	 */
	protected DataCollectionMode m_DataCollectionMode;
	
	/**
	 * Default update period is every 2 minutes.
	 */
	public static final int DEFAULT_UPDATE_INTERVAL_SECS = 60 * 2;
	
	/**
	 * The start date/time for the next query.
	 * @return
	 */
	abstract public Date getQueryStartDate();
	
	/**
	 * The end date/time for the next query.
	 * @return
	 */
	abstract public Date getQueryEndDate();
	
	
	/**
	 * Returns the first date of the initial query.
	 * For anything other than a HistoricalQueryDateTimeProducer
	 * this method will return <code>null</code>. 
	 * 
	 * @return the start date or <code>null</code>
	 */
	public Date getFirstQueryStartDate()
	{
		return null;
	}
	
	/**
	 * Returns the final end date of the last query.
	 * For anything other than a HistoricalQueryDateTimeProducer
	 * this method will return <code>null</code>. 
	 *
	 * @return the end date or <code>null</code>
	 */
	public Date getFinalQueryEndDate()
	{
		return null;
	}
	
	/**
	 * After a query has completed calling this function with the 
	 * end date/time of the last query will update the start time of 
	 * the next query, i.e. the result retuned by <code>getQueryStartDate()</code>
	 * 
	 * The start date/time is usually 1ms after the last query end time.
	 * 
	 * @param value The last query end time.
	 * @throws InputManagerQueriesCompleteException
	 */
	abstract public void setLastQueryEndTime(Date value) throws InputManagerQueriesCompleteException;
	
	/**
	 * Returns the amount of time in ms should be waited before another
	 * query is run given the time it took to execute the last query.
	 * 
	 * @param lastQueryDurationMs The length of time in Ms the last query 
	 *                            took to execute.
	 * @return Sleep period in ms.
	 */
	abstract public long getSleepTimeMs(long lastQueryDurationMs);
	
	
	/**
	 * The Data collection mode.
	 * @return
	 */
	public DataCollectionMode getDataCollectionMode()
	{
		return m_DataCollectionMode;
	}
	
	
	/**
	 * Returns true if the queries have finished i.e. the query for time 
	 * <code>getFinalQueryEndDate()</code> has been run. 
	 * 
	 * For real-time query producers this will always be false only 
	 * historical producers can finish the set of queries.
	 * @return
	 */
	public boolean queriesHaveFinished()
	{
		return false;
	}
}
