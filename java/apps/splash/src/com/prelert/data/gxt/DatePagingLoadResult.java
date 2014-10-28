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

package com.prelert.data.gxt;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.extjs.gxt.ui.client.data.BaseListLoadResult;
import com.prelert.data.TimeFrame;


/**
 * An extension of ListLoadResult for a loader for paging through a date range.
 * @author Pete Harverson
 *
 * @param <Data> the data type
 */
public class DatePagingLoadResult<Data> extends BaseListLoadResult<Data>
	implements Serializable
{
	private TimeFrame 	m_TimeFrame;
	private Date		m_Date;
	private Date		m_StartDate;
	private Date 		m_EndDate;
	private boolean		m_IsEarlierResult;

	
	/**
	 * Creates a new, empty date paging load result.
	 */
	public DatePagingLoadResult()
	{
		this(null);
	}
	
	
	/**
	 * Creates a new date paging load result containing the supplied list of data.
	 * @param data list of the model data contained within the load result.
	 */
	public DatePagingLoadResult(List<Data> list)
    {
	    super(list);
    }
	

	/**
	 * Creates a new date paging load result containing the supplied list of data.
	 * @param data list of the model data contained within the load result.
	 * @param timeFrame the time frame for this load result e.g. week, day or hour.
	 * @param date the date for this load result. This should correspond to the 
	 * 		start date/time for the result's time frame i.e. the start of the
	 * 		week, day or hour.
	 * @param startDate the start (i.e. most recent) date of the full range of 
	 * 		results which are available.
	 * @param endDate the end (i.e. farthest back in time) date of the full range 
	 * 		of results which are available.
	 * @param isEarlierResult <code>true</code> if there are earlier results in the 
	 * 		database for this load configuration than the results contained in this
	 * 		load result, <code>false</code> if this load result contains the earliest
	 * 		record or if the load result contains no data.
	 */
	public DatePagingLoadResult(List<Data> list, TimeFrame timeFrame,
            Date date, Date startDate, Date endDate, boolean isEarlierResult)
    {
	    super(list);
	    m_TimeFrame = timeFrame;
	    m_Date = date;
	    m_StartDate = startDate;
	    m_EndDate = endDate;
	    m_IsEarlierResult = isEarlierResult;
    }


	/**
	 * Returns the time frame for this load result.
	 * @return time frame of this load result e.g. week, day or hour.
	 */
	public TimeFrame getTimeFrame()
    {
    	return m_TimeFrame;
    }


	/**
	 * Sets the time frame for this load result.
	 * @param timeFrame time frame of this load result e.g. week, day or hour.
	 */
	public void setTimeFrame(TimeFrame timeFrame)
    {
    	m_TimeFrame = timeFrame;
    }


	/**
	 * Returns the date for this load result. This should correspond to the 
	 * start date/time for the result's time frame i.e. the start of the
	 * week, day or hour.
	 * @return the date for this load result, which may or may not correspond to
	 * the date of the first result in this load result depending on the request
	 * that was made by the client.
	 */
	public Date getDate()
    {
    	return m_Date;
    }


	/**
	 * Sets the date for this load result. This should correspond to the 
	 * start date/time for the result's time frame i.e. the start of the
	 * week, day or hour.
	 * @param date the date for this load result, which may or may not correspond to
	 * the date of the first result in this load result depending on the request
	 * that was made by the client.
	 */
	public void setDate(Date date)
    {
    	m_Date = date;
    }


	/**
	 * Returns the start (i.e. most recent) date of the full range of results 
	 * which are available.
	 * @return the start date of the results which are available.
	 */
	public Date getStartDate()
    {
    	return m_StartDate;
    }


	/**
	 * Sets the start (i.e. most recent) date of the full range of results which 
	 * are available.
	 * @param startDate the start date of the results which are available.
	 */
	public void setStartDate(Date startDate)
    {
    	m_StartDate = startDate;
    }


	/**
	 * Returns the end (i.e. farthest back in time) date of the full range of  
	 * results which are available.
	 * @return the end date of the results which are available.
	 */
	public Date getEndDate()
    {
    	return m_EndDate;
    }


	/**
	 * Sets the end (i.e. farthest back in time) date of the full range of results 
	 * which  are available.
	 * @param endDate the end date of the results which are available.
	 */
	public void setEndDate(Date endDate)
    {
    	m_EndDate = endDate;
    }
	
	
	/**
	 * Returns a flag to indicate whether there is an earlier result in the 
	 * database to the data held in this load result.
	 * @return <code>true</code> if there is an earlier result, <code>false</code> 
	 * 		if there is no data in this load result, or the last row in the result 
	 * 		set is the record that is farthest back in time for the load config.
	 */
	public boolean isEarlierResult()
	{
		return m_IsEarlierResult;
	}
	
	
	/**
	 * Sets a flag to indicate whether there is an earlier result in the 
	 * database to the data held in this load result.
	 * @param isEarlierResult <code>true</code> if there is an earlier result, 
	 * 		<code>false</code> if there is no data in this load result, or the last
	 * 		row in the result set is the record that is farthest back in time for 
	 * 		the load config.
	 */
	public void setEarlierResult(boolean isEarlierResult)
	{
		m_IsEarlierResult = isEarlierResult;
	}
}
