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

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Class encapsulating the data for a probable cause.
 * 
 * @author Pete Harverson
 */
public class ProbableCause implements Serializable
{
	private DataSourceType 	m_DataSourceType;
	private int				m_EvidenceId;
	private Date 			m_Time;
	private String 			m_Description;
	private String 			m_Source;
	private int				m_Count;
	private List<Attribute>	m_Attributes;
	private int 			m_Significance;
	private double 			m_Magnitude;
	
	private int 			m_TimeSeriesTypeId;
	private double 			m_ScalingFactor;
	private double			m_PeakValue;
	private String 			m_Metric;


	/**
	 * Creates a new blank probable cause.
	 */
	public ProbableCause()
	{

	}


	/**
	 * Returns the data source type of this probable cause e.g. p2ps logs or
	 * UDP error data.
	 * @return the data source type.
	 */
	public DataSourceType getDataSourceType()
	{
		return m_DataSourceType;
	}


	/**
	 * Sets the data source type of this probable cause e.g. p2ps logs or
	 * UDP error data.
	 * @param dataSourceType the data source type.
	 */
	public void setDataSourceType(DataSourceType dataSourceType)
	{
		m_DataSourceType = dataSourceType;
	}
	
	
	/**
	 * Sets the id of the item of evidence (or feature for time series) mapped 
	 * to this probable cause.
	 * @return the evidence or feature id.
	 */
	public int getEvidenceId()
	{
		return m_EvidenceId;
	}
	
	
	/**
	 * 
	 * @param id the evidence or feature id.
	 */
	public void setEvidenceId(int id)
	{
		m_EvidenceId = id;
	}


	/**
	 * Returns the time of this probable cause.
	 * @return the start time.
	 */
	public Date getTime()
	{
		return m_Time;
	}


	/**
	 * Sets the value of the time property for this probable cause.
	 * @param time the time.
	 */
	public void setTime(Date time)
	{
		m_Time = time;
	}


	/**
	 * Returns the description of this probable cause.
	 * @return the description.
	 */
	public String getDescription()
	{
		return m_Description;
	}


	/**
	 * Sets the description for this probable cause.
	 * @param description  the description.
	 */
	public void setDescription(String description)
	{
		m_Description = description;
	}


	/**
	 * Returns the source (server) of this probable cause.
	 * @return the source (server).
	 */
	public String getSource()
	{
		return m_Source;
	}


	/**
	 * Sets the source (server) of this probable cause.
	 * @param source the source (server).
	 */
	public void setSource(String source)
	{
		m_Source = source;
	}
	

	/**
	 * Returns the occurrence count for this probable cause.
     * @return the count of occurrences
     */
    public int getCount()
    {
    	return m_Count;
    }


	/**
	 * Sets the occurrence count for this probable cause.
     * @param count the count of occurrences
     */
    public void setCount(int count)
    {
    	m_Count = count;
    }


	/**
	 * For time series type probable causes, returns the list of attributes
	 * for the time series.
	 * @return the list of attributes, or <code>null</code> if there are no attributes
	 * 	for the time series.
	 */
	public List<Attribute> getAttributes()
	{
		return m_Attributes;
	}
	
	
	/**
	 * For time series type probable causes, sets the optional list of attributes 
	 * for the time series.
	 * @param attributes the list of attributes.
	 */
	public void setAttributes(List<Attribute> attributes)
	{	
		m_Attributes = attributes;
	}

	
	/**
	 * Returns the significance of this probable cause, indicating a percentage
	 * between 1 and 100.
	 * @return the significance - a value between 1 and 100, or 0 if no significance
	 * 		property has been set. A value of -1 indicates that this ProbableCause 
	 * 		object refers to the event that has occurred.
	 */
	public int getSignificance()
	{
		return m_Significance;
	}


	/**
	 * Sets the significance of this probable cause, indicating a percentage
	 * between 1 and 100.
	 * @param significance the significance - a value between 1 and 100.
	 * 		A value of -1 should be used to indicate that this ProbableCause 
	 * 		object refers to the event that has occurred.
	 */
	public void setSignificance(int significance)
	{
		m_Significance = significance;
	}
	
	
	/**
	 * Returns the magnitude of this probable cause. For notifications this is
	 * equal to the count. For time series types, this is equal to the magnitude 
	 * of the rate of change.
     * @return the magnitude, a double whose value is equal to or greater than 0.
     */
    public double getMagnitude()
    {
    	return m_Magnitude;
    }


	/**
	 * Sets the magnitude of this probable cause. For notifications this is
	 * equal to the count. For time series types, this is equal to the magnitude 
	 * of the rate of change.
     * @param magnitude the magnitude, a double whose value is equal to or greater than 0.
     */
    public void setMagnitude(double magnitude)
    {
    	m_Magnitude = magnitude;
    }


	/**
	 * For time series type probable causes, returns the factor by which the data
	 * values should be scaled in relation to other time series probable causes.
     * @return the scaling factor, a value between 0 and 1.
     */
    public double getScalingFactor()
    {
    	return m_ScalingFactor;
    }


	/**
	 * For time series type probable causes, sets the factor by which the data
	 * values should be scaled in relation to other time series probable causes.
     * @param scalingFactor the scaling factor, a value between 0 and 1.
     */
    public void setScalingFactor(double scalingFactor)
    {
    	m_ScalingFactor = scalingFactor;
    }
    
    
	/**
	 * For time series type probable causes, returns the 'type id', which
	 * distinguishes a time series type by its data type and metric
	 * e.g. system_udp/packets_received.
     * @return the time series type id.
     */
    public int getTimeSeriesTypeId()
    {
    	return m_TimeSeriesTypeId;
    }


	/**
	 * For time series type probable causes, sets the 'type id', which
	 * distinguishes a time series type by its data type and metric
	 * e.g. system_udp/packets_received.
     * @param timeSeriesTypeId the time series type id.
     */
    public void setTimeSeriesTypeId(int timeSeriesTypeId)
    {
    	m_TimeSeriesTypeId = timeSeriesTypeId;
    }


	/**
	 * For time series type probable causes, returns the peak value of the time
	 * series in the requested time span.
     * @return the peak value.
     */
    public double getPeakValue()
    {
    	return m_PeakValue;
    }


	/**
	 * For time series type probable causes, sets the peak value of the time
	 * series in the requested time span.
     * @param peakValue the peak value.
     */
    public void setPeakValue(double peakValue)
    {
    	m_PeakValue = peakValue;
    }


	/**
	 * For time series type probable causes, returns the time series metric.
	 * @return the time series metric.
	 */
	public String getMetric()
	{
		return m_Metric;
	}
	
	
	/**
	 * For time series type probable causes, sets the time series metric.
	 * @param metric the time series metric.
	 */
	public void setMetric(String metric)
	{
		m_Metric = metric;
	}
	
	
	/**
	 * Returns a summary of this probable cause.
	 * @return String representation of this probable cause.
	 */
	public String toString()
	{
		StringBuilder strRep = new StringBuilder('{');
		strRep.append("dataSourceType=");
		strRep.append(m_DataSourceType);
		strRep.append("evidence_id=");
		strRep.append(m_EvidenceId);
		strRep.append(", time=");
		strRep.append(m_Time);
		strRep.append(", description=");
		strRep.append(m_Description);
		strRep.append(", source=");
		strRep.append(m_Source);
		strRep.append(", count=");
		strRep.append(m_Count);
		
		if (m_Attributes != null)
		{
			strRep.append(", attributes=");
			strRep.append(m_Attributes);
		}
		
		strRep.append(", magnitude=");
		strRep.append(m_Magnitude);
		
		if (m_DataSourceType.getDataCategory() == DataSourceCategory.TIME_SERIES)
		{
			strRep.append(", time_series_type_id=");
			strRep.append(m_TimeSeriesTypeId);
			strRep.append(", metric=");
			strRep.append(m_Metric);
			strRep.append(", scaling_factor=");
			strRep.append(m_ScalingFactor);
			strRep.append(", peak_value=");
			strRep.append(m_PeakValue);
		}
		
		strRep.append('}');
		
		return strRep.toString();
	}

}
