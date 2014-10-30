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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Class encapsulating the properties of a time series such as the
 * type, source and metric.
 * @author Pete Harverson
 */
public class TimeSeriesConfig implements Serializable
{

	private String	m_DataType;
	private String	m_Metric;
	private String	m_Source;
	private List<Attribute>	m_Attributes;
	private String	m_AttributeLabel;
	
	private String	m_Description;
	
	private Date	m_MinTime;
	private Date	m_MaxTime;
	
	private double	m_ScalingFactor = 1;
	
	
	/**
	 * Creates a new, empty TimeSeriesConfig.
	 */
	public TimeSeriesConfig()
	{
		
	}
	
	
	/**
	 * Creates a new configuration for a time series with the given data type,
	 * metric and source.
	 * @param dataType the data type, such as 'mdhmon' or 'system_udp'.
	 * @param metric the time series metric e.g. total, inbound, outbound.
	 * @param source the name of the source, or <code>null</code> if the data is for
	 * 		all sources.
	 */
	public TimeSeriesConfig(String dataType, String metric, String source)
	{
		m_DataType = dataType;
		m_Metric = metric;
		m_Source = source;
	}
	
	
	/**
	 * Creates a new configuration for a time series with the given data type,
	 * metric, source and single attribute.
	 * @param dataType the data type, such as 'mdhmon' or 'system_udp'.
	 * @param metric the time series metric e.g. total, inbound, outbound.
	 * @param source the name of the source, or <code>null</code> if the data is for
	 * 		all sources.
	 * @param attributeName optional attribute name.
	 * @param attributeValue optional attribute value.
	 */
	public TimeSeriesConfig(String dataType, String metric, String source, 
			String attributeName, String attributeValue)
	{
		this(dataType, metric, source);
		
		if (attributeName != null)
		{
			m_Attributes = new ArrayList<Attribute>();
			m_Attributes.add(new Attribute(attributeName, attributeValue));
		}
	}
	
	
	/**
	 * Creates a new configuration for a time series with the given data type,
	 * metric, source and list of attribute.
	 * @param dataType the data type, such as 'mdhmon' or 'system_udp'.
	 * @param metric the time series metric e.g. total, inbound, outbound.
	 * @param source the name of the source, or <code>null</code> if the data is for
	 * 		all sources.
	 * @param attributes list of attributes.
	 */
	public TimeSeriesConfig(String dataType, String metric, String source, 
			List<Attribute> attributes)
	{
		this(dataType, metric, source);
		
		m_Attributes = attributes;
	}
		

	/**
	 * Returns the data type, such as 'mdhmon' or 'system_udp', which is used
	 * to identify the particular type of data in the time series.
	 * @return the data type.
	 */
    public String getDataType()
    {
    	return m_DataType;
    }


    /**
	 * Sets the data type, such as 'mdhmon' or 'system_udp', which is used
	 * to identify the particular type of data in the time series.
	 * @param dataType the data type.
	 */
    public void setDataType(String dataType)
    {
    	m_DataType = dataType;
    }


    /**
     * Returns the name of the time series metric.
     * @return the time series metric e.g. total, inbound, outbound.
     */
	public String getMetric()
	{
		return m_Metric;
	}
	
	
	/**
	 * Sets the name of the time series metric.
	 * @param metric the time series metric e.g. total, inbound, outbound.
	 */
	public void setMetric(String metric)
	{
		m_Metric = metric;
	}
	
	
	/**
	 * Returns the name of the source (server) for the time series.
	 * @return the name of the source, or <code>null</code> if the data is from
	 * 		all sources.
	 */
	public String getSource()
	{
		return m_Source;
	}
	
	
	/**
	 * Sets the name of the source (server) for the time series.
	 * @param source the name of the source, or <code>null</code> 
	 * 		if the data is from all sources.
	 */
	public void setSource(String source)
	{
		m_Source = source;
	}
	
	
	/**
	 * Returns the list of attributes, if any, for this time series.
	 * @return the list of attributes, or <code>null</code> if no attributes have been set.
	 */
	public List<Attribute> getAttributes()
	{
		return m_Attributes;
	}
	
	
	/**
	 * Sets the optional list of attributes for this time series.
	 * @param attributes the list of attributes.
	 */
	public void setAttributes(List<Attribute> attributes)
	{	
		m_Attributes = attributes;
	}
	
	
	/**
	 * Returns an optional attribute label, which is used to distinguish time series
	 * which are otherwise identical e.g. two closely spaced peaks in a time series
	 * which both feature in a correlation.
	 * @return the attribute label, or <code>null</code> if no attribute label has been set.
	 */
	public String getAttributeLabel()
	{
		return m_AttributeLabel;
	}
	
	
	/**
	 * Sets the value of the optional attribute label, which is used to distinguish time series
	 * which are otherwise identical e.g. two closely spaced peaks in a time series
	 * which both feature in a correlation.
	 * @param attributeLabel the attribute label.
	 */
	public void setAttributeLabel(String attributeLabel)
	{	
		m_AttributeLabel = attributeLabel;
	}
	

	/**
	 * Returns an optional description for this time series.
     * @return the time series description.
     */
    public String getDescription()
    {
    	return m_Description;
    }


	/**
	 * Sets an optional description for this time series.
     * @param description the time series description.
     */
    public void setDescription(String description)
    {
    	m_Description = description;
    }


	/**
	 * Returns the minimum time to be displayed for the time series.
     * @return the minimum/start time.
     */
    public Date getMinTime()
    {
    	return m_MinTime;
    }


	/**
	 * Sets the minimum time to be displayed for the time series.
     * @param minTime the minimum/start time.
     */
    public void setMinTime(Date minTime)
    {
    	m_MinTime = minTime;
    }


	/**
	 * Returns the maximum time to be displayed for the time series.
     * @return the maximum/end time.
     */
    public Date getMaxTime()
    {
    	return m_MaxTime;
    }


	/**
	 * Sets the maximum time to be displayed for the time series.
     * @param maxTime the maximum/end time.
     */
    public void setMaxTime(Date maxTime)
    {
    	m_MaxTime = maxTime;
    }


	/**
	 * Returns the factor by which the values of points in this time series should
	 * be scaled before adding to a chart for display.
     * @return the scaling factor to use for the series values.
     */
    public double getScalingFactor()
    {
    	return m_ScalingFactor;
    }


	/**
	 * Sets the factor by which the values of points in this time series should
	 * be scaled before adding to a chart for display.
     * @param scalingFactor the scaling factor to use for the series values.
     */
    public void setScalingFactor(double scalingFactor)
    {
    	m_ScalingFactor = scalingFactor;
    }


	/**
	 * Tests this TimeSeriesConfig object for equality with another object.
	 * @return true if the comparison object is a TimeSeriesConfig object with
	 * 	identical property values to this TimeSeriesConfig (excluding minimum
	 * 	and maximum times).
     */
    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
        {
            return true;
        }
        if(!(obj instanceof TimeSeriesConfig))
        {
            return false;
        }

        TimeSeriesConfig other = (TimeSeriesConfig)obj;
    	
    	// Compare data types.
    	if (m_DataType != null)
    	{
    		if (m_DataType.equals(other.getDataType()) == false)
    		{
    			return false;
    		}
    	}
    	else
    	{
    		if (other.getDataType() != null)
    		{
    			return false;
    		}
    	}
    	
    	// Compare metrics.
    	if (m_Metric != null)
    	{
    		if (m_Metric.equals(other.getMetric()) == false)
    		{
    			return false;
    		}
    	}
    	else
    	{
    		if (other.getMetric() != null)
    		{
    			return false;
    		}
    	}
    	
    	// Compare sources
    	if (m_Source != null)
    	{
    		if (m_Source.equals(other.getSource()) == false)
    		{
    			return false;
    		}
    	}
    	else
    	{
    		if (other.getSource() != null)
    		{
    			return false;
    		}
    	}
    	
    	// Compare attributes.
    	if (m_Attributes != null)
    	{
    		if (m_Attributes.equals(other.getAttributes()) == false)
    		{
    			return false;
    		}
    	}
    	else
    	{
    		if (other.getAttributes() != null)
    		{
    			return false;
    		}
    	}
    	
    	// Compare attribute labels.
    	if (m_AttributeLabel != null)
    	{
    		if (m_AttributeLabel.equals(other.getAttributeLabel()) == false)
    		{
    			return false;
    		}
    	}
    	else
    	{
    		if (other.getAttributeLabel() != null)
    		{
    			return false;
    		}
    	}
        
        return true;
    }


	/**
	 * Returns a hash code for this object. This is obtained by a call
	 * to hashCode() on the String representation of the key properties of this
	 * TimeSeriesConfig (i.e. without minimum and maximum times).
	 * @return  a hash code value for this TimeSeriesConfig object.
     */
    @Override
    public int hashCode()
    {
	    String strVal = this.toString();
	    return strVal.hashCode();
    }


	/**
	 * Returns a summary of this time series configuration object.
	 * @return String representation of the TimeSeriesConfig.
	 */
	public String toString()
	{
		StringBuilder strRep = new StringBuilder();
		strRep.append("{data type=");
		strRep.append(m_DataType);
		if (m_Description != null)
		{
			strRep.append(", description=");
			strRep.append(m_Description);	
		}
		strRep.append(", metric=");
		strRep.append(m_Metric);
		strRep.append(", source=");
		strRep.append(m_Source);
		if (m_Attributes != null)
		{
			strRep.append(", attributes=");
			strRep.append(m_Attributes);
		}
		
		if (m_AttributeLabel != null)
		{
			strRep.append(", attributeLabel=");
			strRep.append(m_AttributeLabel);
		}
		
		strRep.append('}');
		
		return strRep.toString();
	}
}
