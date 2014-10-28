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
 * Class encapsulating the properties of a time series such as the
 * type, source and metric.
 * @author Pete Harverson
 */
public class TimeSeriesConfig implements Serializable
{

	private String	m_DataType;
	private String	m_Metric;
	private String	m_Source;
	private String	m_AttributeName;
	private String	m_AttributeValue;
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
		this(dataType, metric, source, null, null);
	}
	
	
	/**
	 * Creates a new configuration for a time series with the given data type,
	 * metric, source and attribute.
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
		m_DataType = dataType;
		m_Metric = metric;
		m_Source = source;
		m_AttributeName = attributeName;
		m_AttributeValue = attributeValue;
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
	 * Returns the name of the attribute, if any, on which the time series data will
	 * be filtered.
	 * @return the attribute name, or <code>null</code> if no attribute has been set.
	 */
	public String getAttributeName()
	{
		return m_AttributeName;
	}
	
	
	/**
	 * Sets the name of the optional attribute on which the time series data will
	 * be filtered.
	 * @param attributeName the attribute name.
	 */
	public void setAttributeName(String attributeName)
	{	
		m_AttributeName = attributeName;
	}
	
	
	/**
	 * Returns the value of the attribute, if any, on which the time series data will
	 * be filtered.
	 * @return the attribute value, or <code>null</code> if no attribute has been set.
	 */
	public String getAttributeValue()
	{
		return m_AttributeValue;
	}
	
	
	/**
	 * Sets the value of the optional attribute on which the time series data will
	 * be filtered.
	 * @param attributeValue the attribute value.
	 */
	public void setAttributeValue(String attributeValue)
	{	
		m_AttributeValue = attributeValue;
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
     * @return the m_ScalingFactor
     */
    public double getScalingFactor()
    {
    	return m_ScalingFactor;
    }


	/**
     * @param scalingFactor the m_ScalingFactor to set
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
    	
    	// Compare attribute names.
    	if (m_AttributeName != null)
    	{
    		if (m_AttributeName.equals(other.getAttributeName()) == false)
    		{
    			return false;
    		}
    	}
    	else
    	{
    		if (other.getAttributeName() != null)
    		{
    			return false;
    		}
    	}
    	
    	// Compare attribute values.
    	if (m_AttributeValue != null)
    	{
    		if (m_AttributeValue.equals(other.getAttributeValue()) == false)
    		{
    			return false;
    		}
    	}
    	else
    	{
    		if (other.getAttributeValue() != null)
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
		if (m_AttributeName != null)
		{
			strRep.append(',');
			strRep.append(m_AttributeName);
			strRep.append('=');
			strRep.append(m_AttributeValue);
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
