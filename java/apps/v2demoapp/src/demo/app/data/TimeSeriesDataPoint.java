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

package demo.app.data;

import java.io.Serializable;
import java.util.Date;


/**
 * Class encapsulating a time series data point. It consists of a time and a value.
 * @author Pete Harverson
 */
public class TimeSeriesDataPoint implements Serializable
{
	private Date 		m_Time;
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
	 * @param time recorded time of the time series data point.
	 * @param value metric value of the time series data point.
	 */
	public TimeSeriesDataPoint(Date time, double value)
	{
		m_Time = time;
		m_Value = value;
	}


	/**
	 * Returns the time of the time series data point.
	 * @return recorded time of the time series data point.
	 */
	public Date getTime()
	{
		return m_Time;
	}


	/**
	 * Sets the time of the time series data point.
	 * @param time recorded time of the time series data point.
	 */
	public void setTime(Date time)
	{
		m_Time = time;
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
	
	
	/**
	 * Returns a summary of this time series data point.
	 * @return String representation of this time series data point.
	 */
	public String toString()
	{
		StringBuilder strRep = new StringBuilder('{');
		strRep.append("time=");
		strRep.append(m_Time);
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

}
