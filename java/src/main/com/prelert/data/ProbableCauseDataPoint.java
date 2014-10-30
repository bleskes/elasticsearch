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
import java.util.List;


/**
 * Class encapsulating a single item of probable cause data, containing a reference
 * to the <code>ProbableCause</code> object, and for time series type probable causes,
 * the value of the time series metric at a particular time.
 * @author Pete Harverson
 */

public class ProbableCauseDataPoint implements Serializable
{
	private static final long serialVersionUID = -3308583977220213015L;

	private ProbableCause 		m_ProbableCause;
	private Date				m_Time;
	private Double				m_Value;

	
	/**
	 * Creates a new ProbableCauseDataPoint, with a blank (<code>non-null</code>)
	 * ProbableCause  object.
	 */
	public ProbableCauseDataPoint()
	{
		this(new ProbableCause(), null);
	}
	
	
	/**
	 * Creates a ProbableCauseDataPoint object for the specified probable cause
	 * and time series data point.
	 * @param probableCause ProbableCause, either time series or notification type.
	 * @param dataPoint time/value for time series data, or <code>null</code> for
	 * 	notification type probable causes.
	 */
	public ProbableCauseDataPoint(ProbableCause probableCause, TimeSeriesDataPoint dataPoint)
	{
		m_ProbableCause = probableCause;
		if (dataPoint != null)
		{
			m_Time = new Date(dataPoint.getTime());
			m_Value = new Double(dataPoint.getValue());
		}
		else
		{
			m_Time = m_ProbableCause.getTime();
		}
	}
	

	/**
	 * Returns the probable cause encapsulated in this data point.
	 * @return the probable cause.
	 */
	public ProbableCause getProbableCause()
	{
		return m_ProbableCause;
	}
	
	
	/**
	 * Returns the name of the data source type of this probable cause.
	 * @return the name of the data source type.
	 */
	public String getType()
	{
		String type = null;
		DataSourceType dsType = m_ProbableCause.getDataSourceType();
		if (dsType != null)
		{
			type = dsType.getName();
		}
		
		return type;
	}
	
	
	/**
	 * Returns the category of the data source type of this probable cause..
     * @return the category of the data source type.
     */
	public DataSourceCategory getCategory()
	{
		DataSourceCategory category = null;
		DataSourceType dsType = m_ProbableCause.getDataSourceType();
		if (dsType != null)
		{
			category = dsType.getDataCategory();
		}
		
		return category;
	}
	
	
	/**
	 * Returns the time of this ProbableCauseDataPoint. For time series type
	 * probable causes, this is the time of this particular data point and not
	 * the time of the probable cause feature.
	 * @return the time of this ProbableCauseDataPoint.
	 */
	public Date getTime()
	{
		return m_Time;
	}
	
	
	/**
	 * Returns the description of the probable cause.
	 * @return the description.
	 */
	public String getDescription()
	{
		return m_ProbableCause.getDescription();
	}

	
	/**
	 * Returns the name of the source (server) of the probable cause.
	 * @return the source (server) name.
	 */
	public String getSource()
	{
		return m_ProbableCause.getSource();
	}
	
	
	/**
	 * Returns the occurrence count for the probable cause.
     * @return the count of occurrences.
     */
	public int getCount()
	{
		return m_ProbableCause.getCount();
	}
	
	
	/**
	 * Returns the significance of the probable cause, indicating a percentage
	 * between 1 and 100.
	 * @return the significance - a value between 1 and 100, or 0 if no significance
	 * 		property has been set. A value of -1 indicates that the ProbableCause 
	 * 		object refers to the event that has occurred.
	 */
	public int getSignificance()
	{
		return m_ProbableCause.getSignificance();
	}
	
	
	/**
	 * Returns the magnitude of the probable cause. For notifications this is
	 * equal to the count. For time series types, this is equal to the magnitude 
	 * of the rate of change.
     * @return the magnitude, a double whose value is equal to or greater than 0.
     */
	public double getMagnitude()
    {
    	return m_ProbableCause.getMagnitude();
    }
	
	
	/**
	 * For time series type probable causes, returns the time series metric.
	 * @return the time series metric, or <code>null</code> for notification
	 * 	type probable causes.
	 */
	public String getMetric()
	{
		return m_ProbableCause.getMetric();
	}
	
	
	/**
	 * Returns the value of this ProbableCauseDataPoint for time series type
	 * probable causes i.e. the value of the metric at this data point's time.
	 * @return the value, or <code>null</code> for notification
	 * 	type probable causes.
	 */
	public Double getValue()
	{
		return m_Value;
	}
	
	
	/**
	 * For notification types, returns the severity of the probable cause.
	 * @return the severity of the notification data, such as 'minor',
	 * 'major' or 'critical'. Returns <code>null</code> for time series types.
	 */
	public Severity getSeverity()
	{
		return m_ProbableCause.getSeverity();
	}
	
	
	/**
	 * Returns the list of attributes for the probable cause.
	 * @return the list of attributes, or <code>null</code> if there are no 
	 * 	additional attributes.
	 */
	public List<Attribute> getAttributes()
	{
		return m_ProbableCause.getAttributes();
	}
}
