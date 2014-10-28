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
 * Class encapsulating a time series data point. It consists of a time and a value.
 * @author Pete Harverson
 */
public class TimeSeriesDataPoint implements Serializable, Comparable<TimeSeriesDataPoint>
{
	private static final long serialVersionUID = 3634668298369247586L;

	private long		m_NumMs;
	private double 		m_Value;
	
	private Evidence	m_Feature;

	
	/**
	 * Creates a blank time series data point.
	 */
	public TimeSeriesDataPoint()
	{

	}
	
	
	/**
	 * Creates a time series data point with the given time and value.
	 * @param time Date/Time of the data point represented by the number of 
	 * 		milliseconds since January 1, 1970, 00:00:00 GMT.
	 * @param value metric value of the time series data point.
	 */
	public TimeSeriesDataPoint(long time, double value)
	{
		m_NumMs = time;
		m_Value = value;
	}
	
	
	/**
	 * Copy constructor
	 * @param pt
	 */
	public TimeSeriesDataPoint(TimeSeriesDataPoint pt)
	{
		m_NumMs = pt.getTime();
		m_Value = pt.getValue();
	}


	/**
	 * Sets the time of the time series data point as the number of milliseconds 
	 * since January 1, 1970, 00:00:00 GMT.
	 * @param time the number of milliseconds since January 1, 1970, 00:00:00 GMT.
	 */
	public void setTime(long time)
	{
		m_NumMs = time;
	}
	
	
	/**
	 * Returns time of this time series data point as the number of milliseconds 
	 * since January 1, 1970, 00:00:00 GMT.
	 * @return the number of milliseconds since January 1, 1970, 00:00:00 GMT.
	 */
	public long getTime()
	{
		return m_NumMs;
	}


	/**
	 * Returns the value of the time series data point.
	 * @return metric value.
	 */
	public double getValue()
	{
		return m_Value;
	}


	/**
	 * Sets the value of the time series data point.
	 * @param value metric value of the time series data point.
	 */
	public void setValue(double value)
	{
		m_Value = value;
	}
	
	
	/**
	 * Returns the item of evidence, if any, corresponding to a feature in the time 
	 * series data.
	 * @return the time series feature, as an item of evidence, or <code>null</code>
	 * if there is no feature associated with this time series data point or if
	 * feature data is not available (e.g. it was not requested).
	 */
	public Evidence getFeature()
	{
		return m_Feature;
	}
	
	
	/**
	 * Sets the item of evidence corresponding to a feature in the time series data.
	 * @param the time series feature, as an item of evidence.
	 */
	public void setFeature(Evidence feature)
	{
		m_Feature = feature;
	}
	
	
	@Override
	public boolean equals(Object obj)
	{
		final double EPSILON = 0.00001; // For floating point rounding errors
		
		if (obj == this)
		{
			return true;
		}
		
		if (!(obj instanceof TimeSeriesDataPoint))
		{
			return false;
		}
		
		TimeSeriesDataPoint other = (TimeSeriesDataPoint)obj;
		
		boolean result = other.getTime() == this.getTime();
		
		//result = result && (Double.compare(other.getValue(), this.getValue()) == 0);
		result = result && (Math.abs(getValue() - other.getValue()) < EPSILON);
		
		boolean bothNull = other.getFeature() == null && this.getFeature() == null;
		
		result = result && (bothNull || other.getFeature().equals(this.getFeature()));
		
		return result;
	}
	
	
	/**
	 * Compare 2 <code>TimeSeriesDataPoints</code> for equality but don't test
	 * the point's features.
	 * @param other
	 * @return Returns true of all properties of the points are equal except
	 * the feature which isn't tested.
	 */
	public boolean equalDisregardingFeatures(TimeSeriesDataPoint other)
	{
		final double EPSILON = 0.00001;
		
		boolean result = other.getTime() == this.getTime();
		
		result = result && (Math.abs(getValue() - other.getValue()) < EPSILON);
		
		return result;
	}
	
	
	/**
	 * Returns a summary of this time series data point.
	 * @return String representation of this time series data point.
	 */
	@Override
	public String toString()
	{
		StringBuilder strRep = new StringBuilder('{');
		strRep.append("time=");
		strRep.append(new Date(m_NumMs));
		strRep.append(", value=");
		strRep.append(m_Value);
		if (m_Feature != null)
		{
			strRep.append(", evidence_id=");
			strRep.append(m_Feature.getId());
		}
		strRep.append('}');
		
		return strRep.toString();
	}


	/**
	 * The natural ordering of TimeSeriesDataPoint is by time.
	 */
	@Override
	public int compareTo(TimeSeriesDataPoint other) 
	{
		return new Long(getTime()).compareTo(other.getTime());
	}

}
