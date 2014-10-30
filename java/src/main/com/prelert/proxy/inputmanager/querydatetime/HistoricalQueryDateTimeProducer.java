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

import org.apache.log4j.Logger;

import com.prelert.proxy.inputmanager.DataCollectionMode;
import com.prelert.proxy.inputmanager.InputManagerQueriesCompleteException;

/**
 * For historical input managers.
 * This class will produce query start/end dates at intervals then
 * throw a <code>InputManagerQueriesCompleteException</code> when 
 * the queries are complete.
 * 
 * If the end date is <code>null</code> then this class will continue 
 * generating query dates until the last query end time is within
 * <code>MarginOfTimeUntilNowMs</code> of the current time.
 */
public class HistoricalQueryDateTimeProducer extends QueryDateTimeProducer 
{
	private static Logger s_Logger = Logger.getLogger(HistoricalQueryDateTimeProducer.class);
	
	private Date m_StartDate;
	private final Date m_StartQueriesDate;
	private final Date m_FinalQueryEndDate;
	private final long m_QueryTimeMs;
	private final long m_SleepBetweenQueriesMs;
	private long m_MarginOfTimeUntilNowMs;
	private boolean m_QueriesHaveFinished;
	
	/**
	 * As historical queries approach 'now' when they are within this time 
	 * period of 'now' 
	 */
	public static final int DEFAULT_MARGIN_OF_TIME_UNTIL_NOW_MS = 2 * 60 * 1000;
	
	/**
	 * Default length of the slices of each historical query.
	 */
	public static final int DEFAULT_HISTORICAL_QUERY_LENGTH_SECS = 3 * 60;
	public static final int DEFAULT_HISTORICAL_UPDATE_INTERVAL_SECS = 0;
	
	/**
	 * The period between <code>start</code> and <code>end</code>n inclusive will
	 * be sliced into chunks of duration  <code>queryTimeSecs.</code>
	 * 
	 * @param start The query start date.
	 * @param end The query end date.
	 * @param queryTimeSecs The size in seconds of each query. 
	 * @param sleepBetweenQueriesSecs The length of time to sleep between 
	 * 									executing queries.
	 */
	public HistoricalQueryDateTimeProducer(Date start, Date end, long queryTimeSecs, 
											long sleepBetweenQueriesSecs)
	{
		m_StartDate = start;
		m_StartQueriesDate = start;
		m_FinalQueryEndDate = end;
		m_QueryTimeMs = queryTimeSecs * 1000;
		m_SleepBetweenQueriesMs = sleepBetweenQueriesSecs * 1000;
		
		m_QueriesHaveFinished = false;
		m_MarginOfTimeUntilNowMs = DEFAULT_MARGIN_OF_TIME_UNTIL_NOW_MS;
		
		m_DataCollectionMode = DataCollectionMode.HISTORICAL;
	}
	
	
	/**
	 * Constructs a new <code>HistoricalQueryDateTimeProducer</code> 
	 * using default values for <code>queryTimeSecs</code> and 
	 * <code>sleepBetweenQueriesSecs</code>
	 * 
	 * @param start
	 * @param end
	 */
	public HistoricalQueryDateTimeProducer(Date start, Date end)
	{
		this(start, end, 
			DEFAULT_HISTORICAL_QUERY_LENGTH_SECS,
			DEFAULT_HISTORICAL_UPDATE_INTERVAL_SECS);
	}
	
	@Override
	public Date getQueryStartDate() 
	{
		return m_StartDate;
	}

	@Override
	public Date getQueryEndDate() 
	{
		Date now = new Date();
		Date calcEndDate = new Date(m_StartDate.getTime() + m_QueryTimeMs);
		
		if (calcEndDate.after(now))
		{
			return now;
		}
		else
		{
			return calcEndDate;
		}
	}
	
	/**
	 * Returns the Date of the last query which has to be made.
	 */
	@Override
	public Date getFinalQueryEndDate()
	{
		return m_FinalQueryEndDate;
	}
	
	/**
	 * Returns the very first start Date of the first query.
	 */
	@Override 
	public Date getFirstQueryStartDate()
	{
		return m_StartQueriesDate;
	}
	
	
	/**
	 * Once a <code>InputManagerQueriesCompleteException</code> has been 
	 * thrown <code>getQueriesHaveFinished()</code> will always return true; 
	 */
	@Override
	public void setLastQueryEndTime(Date value) throws InputManagerQueriesCompleteException
	{
		if (m_FinalQueryEndDate != null)
		{
			if (m_StartDate.after(m_FinalQueryEndDate) || m_StartDate.equals(m_FinalQueryEndDate))
			{
				m_QueriesHaveFinished = true;
				throw new InputManagerQueriesCompleteException();
			}
		}
		else
		{
			Date now = new Date();
			if (now.getTime() - value.getTime() < m_MarginOfTimeUntilNowMs)
			{
				throw new InputManagerQueriesCompleteException(value);
			}
		}

		// next start is 1ms after the end
		m_StartDate = new Date(value.getTime() + 1);
	}


	/**
	 * @param lastQueryDurationMs - unused.
	 * 
	 * @return 
	 */
	@Override
	public long getSleepTimeMs(long lastQueryDurationMs) 
	{
		if (lastQueryDurationMs > m_QueryTimeMs)
		{
			s_Logger.info("Historical query takes longer to execute then the length of data it returns. " +
						"Consider changing the query.");
		}
		
		return m_SleepBetweenQueriesMs;
	}
	
	/**
	 * Returns the size of the query in seconds. Queries between
	 * 2 time periods are sliced into periods of this size.
	 * @return
	 */
	public long getQueryLengthSecs()
	{
		return m_QueryTimeMs / 1000;
	}
	
	/**
	 * Returns the number of seconds to sleep between queries.
	 * @return
	 */
	public long getSleepBetweenQueriesSecs()
	{
		return m_SleepBetweenQueriesMs / 1000;
	}
	
	@Override
	public boolean queriesHaveFinished()
	{
		return m_QueriesHaveFinished;
	}

}
