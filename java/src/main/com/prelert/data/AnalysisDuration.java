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

package com.prelert.data;

import java.io.Serializable;
import java.util.Date;


/**
 * Class to record the estimated time it will take to 
 * pull data for an analysis and the optimal size of data chunks
 * in seconds the data should be pulled in.
 * 
 * If the estimate is successful then getErrorState() will return 
 * NO_ERROR else an error state and possibly an error message will
 * be returned.
 */
public class AnalysisDuration implements Serializable
{
	private static final long serialVersionUID = -8973974049864070892L;
	
	public enum ErrorState {NO_ERROR, CONNECTION_FAILURE, NO_DATA, DATA_AT_TOO_LARGE_INTERVAL};
	
	private long m_QueryDuration;
	private int m_OptimalQueryLength;
	private Date m_ChangedStartTime;
	private ErrorState m_ErrorState;
	private String m_ErrorMessage; 
	private int m_MaxRequiredDataPointInterval;
	private int m_ActualDataPointInterval;

	public AnalysisDuration()
	{
		m_QueryDuration = -1;
		m_OptimalQueryLength = -1;
		m_ErrorState = ErrorState.NO_ERROR;
		m_ErrorMessage = "";
		m_MaxRequiredDataPointInterval = -1;
		m_ActualDataPointInterval = -1;
	}
	
	
	public AnalysisDuration(ErrorState errorState)
	{
		m_ErrorState = errorState;
		m_ErrorMessage = "";
		m_QueryDuration = -1;
		m_OptimalQueryLength = -1;
		m_MaxRequiredDataPointInterval = -1;
		m_ActualDataPointInterval = -1;
	}
		
	public AnalysisDuration(long queryDurationMs, int queryLengthSecs)
	{
		m_QueryDuration = queryDurationMs;
		m_OptimalQueryLength = queryLengthSecs;
		m_ErrorState = ErrorState.NO_ERROR;
		m_ErrorMessage = "";
		m_MaxRequiredDataPointInterval = -1;
		m_ActualDataPointInterval = -1;
	}
	
	public AnalysisDuration(long queryDurationMs, int queryLengthSecs, Date newStartTime)
	{
		m_QueryDuration = queryDurationMs;
		m_OptimalQueryLength = queryLengthSecs;
		m_ChangedStartTime = newStartTime;
		m_ErrorState = ErrorState.NO_ERROR;
		m_ErrorMessage = "";
		m_MaxRequiredDataPointInterval = -1;
		m_ActualDataPointInterval = -1;
	}
	
	public AnalysisDuration(long queryDurationMs, int queryLengthSecs, 
							Date newStartTime, ErrorState errorState)
	{
		m_QueryDuration = queryDurationMs;
		m_OptimalQueryLength = queryLengthSecs;
		m_ChangedStartTime = newStartTime;
		m_ErrorState = errorState;
		m_ErrorMessage = "";
		m_MaxRequiredDataPointInterval = -1;
		m_ActualDataPointInterval = -1;
	}

	/**
	 * Returns the estimated time it will take to pull the analysis 
	 * data. 
	 * @return
	 */
	public long getEstimatedAnalysisDurationMs()
	{
		return m_QueryDuration;
	}

	/**
	 * This is the optimal length query to make to pull the 
	 * analysis data at its fastest.
	 * @return
	 */
	public int getOptimalQueryLengthSecs()
	{
		return m_OptimalQueryLength;
	}
	
	
	/**
	 * If the start time of the analysis has to change because
	 * there is no data at the original time this returns the new
	 * start time.
	 * @return <code>null</code> if the start time hasn't changed 
	 * 		else a new time.
	 */
	public Date getChangedStartTime()
	{
		return m_ChangedStartTime;
	}
	

	/**
	 * If the estimate was successful this returns NO_ERROR else
	 * it returns the error state depending on whether a connection
	 * could be made or if no data could be found.
	 * 
	 * @return
	 */
	public ErrorState getErrorState()
	{
		return m_ErrorState;
	}
	
	
	/**
	 * If the error state is DATA_AT_TOO_LARGE_INTERVAL then this 
	 * function returns contains the minimum interval between data 
	 * points for the analysis to be run. Else -1 is returned.
	 * @return
	 */
	public int getRequiredDataPointIntervalSecs()
	{
		return m_MaxRequiredDataPointInterval;
	}
	
	public void setRequiredDataPointIntervalSecs(int value)
	{
		m_MaxRequiredDataPointInterval = value;
	}
	
	
	/**
	 * If getErrorState() returns NO_ERROR then this value contains 
	 * the interval in seconds between data points at the time the
	 * estimate was made. Else -1 is returned.
	 * @return
	 */
	public int getActualDataPointIntervalSecs()
	{
		return m_ActualDataPointInterval;
	}
	
	public void setActualDataPointIntervalSecs(int value)
	{
		m_ActualDataPointInterval = value;
	}
	
	
	/**
	 * If getErrorState() != NO_ERROR then this field might contain
	 * and associated error message.
	 * @return The error message or an empty string.
	 */
	public String getErrorMessage()
	{
		return m_ErrorMessage;
	}
	
	public void setErrorMessage(String value)
	{
		m_ErrorMessage = value;
	}
	
		
	/**
	 * Returns a String representation of this class.
	 */
	@Override
	public String toString()
	{
		StringBuilder strRep = new StringBuilder();

		strRep.append("{Query Duration ms=");
		strRep.append(m_QueryDuration);

		strRep.append(", Optimal Query Length Secs=");
		strRep.append(m_OptimalQueryLength);

		strRep.append(", Changed Start Time=");
		strRep.append(m_ChangedStartTime);

		strRep.append(", Error State=");
		strRep.append(m_ErrorState);
		
		strRep.append(", Error Message=");
		strRep.append(m_ErrorMessage);

		strRep.append(", Min Point Interval=");
		strRep.append(m_MaxRequiredDataPointInterval);
		
		strRep.append(", Actual Point Interval=");
		strRep.append(m_ActualDataPointInterval);

		strRep.append('}');

		return strRep.toString();
	}
}

