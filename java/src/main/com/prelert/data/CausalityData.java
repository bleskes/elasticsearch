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
 * Class encapsulating causality data, representing a time series feature or a 
 * set of notifications with common attributes.
 * @author Pete Harverson
 */
public class CausalityData implements Serializable
{
	private static final long serialVersionUID = -548734336011449659L;

	private DataSourceType 	m_DataSourceType;
	private String 			m_Description;
	private String 			m_Source;
	private List<Attribute> m_Attributes;
	private Date 			m_StartTime;
	private Date 			m_EndTime;
	private int 			m_Count;
	private int 			m_Significance;
	private double 			m_Magnitude;
	
	private int				m_TimeSeriesTypeId;
	private int				m_TimeSeriesId;
	private double			m_ScalingFactor = 1d;


	/**
	 * Returns the source type of the causality data e.g. p2ps logs or
	 * UDP error data.
	 * @return the data source type.
	 */
	public DataSourceType getDataSourceType()
	{
		return m_DataSourceType;
	}


	/**
	 * Sets the source type of the causality data e.g. p2ps logs or UDP error data.
	 * @param dataSourceType the data source type.
	 */
	public void setDataSourceType(DataSourceType dataSourceType)
	{
		m_DataSourceType = dataSourceType;
	}


	/**
	 * Returns the description of the causality data, such as the notification
	 * description or the name of the time series metric.
	 * @return a description of the causality data.
	 */
	public String getDescription()
	{
		return m_Description;
	}


	/**
	 * Sets the description of the causality data, such as the notification
	 * description or the name of the time series metric.
	 * @param description description of the causality data.
	 */
	public void setDescription(String description)
	{
		m_Description = description;
	}


	/**
	 * Returns the source (server) of the causality data.
	 * @return the name of the source (server).
	 */
	public String getSource()
	{
		return m_Source;
	}


	/**
	 * Sets the name of the source (server) of the causality data.
	 * @param source the name of the source (server).
	 */
	public void setSource(String source)
	{
		m_Source = source;
	}


	/**
	 * Returns a list of additional attributes associated with the underlying 
	 * notification or time series data.
	 * @return the list of attributes, or <code>null</code> if there are no 
	 * 	additional attributes.
	 */
	public List<Attribute> getAttributes()
	{
		return m_Attributes;
	}


	/**
	 * Sets a list of additional attributes associated with the underlying 
	 * notification or time series data.
	 * @param attributes the list of additional attributes.
	 */
	public void setAttributes(List<Attribute> attributes)
	{
		m_Attributes = attributes;
	}


	/**
	 * Returns the start time of the causality data i.e. the time of
	 * the earliest notification or time series feature.
	 * @return the start time i.e. the time of the earliest notification or time 
	 * 		series feature.
	 */
	public Date getStartTime()
	{
		return m_StartTime;
	}


	/**
	 * Sets the start time of the causality data i.e. the time of
	 * the earliest notification or time series feature.
	 * @param startTime the start time i.e. the time of the earliest notification 
	 * 		or time series feature.
	 */
	public void setStartTime(Date startTime)
	{
		m_StartTime = startTime;
	}


	/**
	 * Returns the end time of the causality data i.e. the time of the latest 
	 * notification or time series feature.
	 * @return the end time i.e. the time of the latest notification or time 
	 * 		series feature.
	 */
	public Date getEndTime()
	{
		return m_EndTime;
	}


	/**
	 * Sets the end time of the causality data i.e. the time of the latest 
	 * notification or time series feature.
	 * @param endTime the end time i.e. the time of the latest notification or 
	 * 		time series feature. If there is only one item in the aggregated object,
	 * 		this is the time of that particular notification or feature.
	 */
	public void setEndTime(Date endTime)
	{
		m_EndTime = endTime;
	}


	/**
	 * Returns the total notification or time series feature count.
	 * @return the total count.
	 */
	public int getCount()
	{
		return m_Count;
	}


	/**
	 * Sets the total notification or time series feature count.
	 * @param count the total count.
	 */
	public void setCount(int count)
	{
		m_Count = count;
	}


	/**
	 * Returns the significance of the causality data, indicating a percentage
	 * between 1 and 100.
	 * @return the significance. Where this object encapsulates more than a single
	 * 	notification or time series feature, this will be the maximum significance
	 * 	of the underlying data.
	 */
	public int getSignificance()
	{
		return m_Significance;
	}


	/**
	 * Sets the significance of the causality data, indicating a percentage
	 * between 1 and 100.
	 * @param significance the significance.
	 */
	public void setSignificance(int significance)
	{
		m_Significance = significance;
	}


	/**
	 * Returns the magnitude of the causality data. For notifications this is
	 * equal to the count. For time series types, this is equal to the magnitude 
	 * of the rate of change.
     * @return the magnitude, a double whose value is equal to or greater than 0.
     * 	Where this object encapsulates more than a single notification or time series
     *  feature, this will be the highest notification count or feature size.
	 */
	public double getMagnitude()
	{
		return m_Magnitude;
	}


	/**
	 * Sets the magnitude of the causality data. For notifications this is
	 * equal to the count. For time series types, this is equal to the magnitude 
	 * of the rate of change.
     * @param magnitude the magnitude, a double whose value is equal to or greater than 0.
     * 	Where this object encapsulates more than a single notification or time series
     *  feature, this should be the highest notification count or feature size.
	 */
	public void setMagnitude(double magnitude)
	{
		m_Magnitude = magnitude;
	}
	
	
	/**
	 * For time series type causality data, returns the 'type id', which
	 * distinguishes a time series type by its data type and metric
	 * e.g. system_udp/packets_received.
     * @return the time series type id.
     */
    public int getTimeSeriesTypeId()
    {
    	return m_TimeSeriesTypeId;
    }


	/**
	 * For time series type causality data, sets the 'type id', which
	 * distinguishes a time series type by its data type and metric
	 * e.g. system_udp/packets_received.
     * @param timeSeriesTypeId the time series type id.
     */
    public void setTimeSeriesTypeId(int timeSeriesTypeId)
    {
    	m_TimeSeriesTypeId = timeSeriesTypeId;
    }
    
    
    /**
     * For time series type causality data, returns the id of the time series, which
     * uniquely identifies a time series by data type, source, metric and attributes.
     * @return the time series id.
     */
    public int getTimeSeriesId()
    {
    	return m_TimeSeriesId;
    }
    
    
    /**
     * For time series type causality data, sets the id of the time series, which
     * uniquely identifies a time series by data type, source, metric and attributes.
     * @param timeSeriesId the time series id.
     */
    public void setTimeSeriesId(int timeSeriesId)
    {
    	m_TimeSeriesId = timeSeriesId;
    }
	
	
	/**
	 * For time series type causality data, returns the factor by which the data
	 * values should be scaled in relation to other time series.
     * @return the scaling factor, a value between 0 and 1.
     */
    public double getScalingFactor()
    {
    	return m_ScalingFactor;
    }


	/**
	 * For time series type causality data, sets the factor by which the data
	 * values should be scaled in relation to other time series.
     * @param scalingFactor the scaling factor, a value between 0 and 1.
     */
    public void setScalingFactor(double scalingFactor)
    {
    	m_ScalingFactor = scalingFactor;
    }


	/**
	 * Returns a summary of this item of causality data.
	 * @return String representation of this CausalityData object.
	 */
    @Override
    public String toString()
    {
    	StringBuilder strRep = new StringBuilder('{');
		strRep.append("dataSourceType=");
		strRep.append(m_DataSourceType);
		strRep.append(", description=");
		strRep.append(m_Description);
		strRep.append(", startTime=");
		strRep.append(m_StartTime);
		strRep.append(", endTime=");
		strRep.append(m_EndTime);
		strRep.append(", source=");
		strRep.append(m_Source);
		strRep.append(", count=");
		strRep.append(m_Count);
		
		if (m_Attributes != null)
		{
			strRep.append(", attributes=");
			strRep.append(m_Attributes);
		}
		
		strRep.append(", significance=");
		strRep.append(m_Significance);
		strRep.append(", magnitude=");
		strRep.append(m_Magnitude);
		
		if (m_DataSourceType.getDataCategory() == DataSourceCategory.TIME_SERIES)
		{
			strRep.append(", timeSeriesTypeId=");
			strRep.append(m_TimeSeriesTypeId);
			strRep.append(", timeSeriesId=");
			strRep.append(m_TimeSeriesId);
			strRep.append(", scalingFactor=");
			strRep.append(m_ScalingFactor);
		}
		
		
		strRep.append('}');
		
		return strRep.toString();
    }
}
