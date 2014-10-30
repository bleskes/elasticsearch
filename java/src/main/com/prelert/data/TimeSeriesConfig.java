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
	private static final long serialVersionUID = -7330396377492783370L;
	
	private String	m_DataType;
	private String	m_Metric;
	private String	m_Source;
	private List<Attribute>	m_Attributes;
	private String	m_AttributeLabel;
	
	private String  m_MetricPrefix;
	private int 	m_MetricPosition = -1;
	private String  m_SourcePrefix;
	private int 	m_SourcePosition = -1;
	
	private String	m_Description;
	
	private Date	m_MinTime;
	private Date	m_MaxTime;
	
	private double	m_ScalingFactor = 1;
	
	private int		m_TimeSeriesId;
	
	private transient String m_ExternalKey;
	
	
	/**
	 * Creates a new, empty configuration for a time series.
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
	 * Returns the metric prefix on the metric path.
	 * The prefix may be <code>null</code> if not set.
	 * @return the metric prefix. May be <code>null</code>
	 */
	public String getMetricPrefix()
	{
		return m_MetricPrefix;
	}
	
	
	/**
	 * Set the metric prefix on the metric path.
	 * @param prefix
	 */
	public void setMetricPrefix(String prefix)
	{
		m_MetricPrefix = prefix;
	}
	
	
	/**
	 * Returns the position in the metric path of the metric.
	 * A negative value means the position has not be set and 
	 * the metric is at the end of metric path by default.
	 * @return A +ve number if the position is set else a
	 * 		   -ve value is returned.
	 */
	public int getMetricPosition()
	{
		return m_MetricPosition;
	}
	
	
	/**
	 * Sets the position of the in the metric path of the
	 * metric. A -ve value indicates the position is unset
	 * and the metric is at the end of metric path by default.
	 * @param position
	 */
	public void setMetricPosition(int position)
	{
		m_MetricPosition = position;
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
	 * Returns the Source's prefix on the metric path.
	 * The prefix may be <code>null</code> if not set.
	 * @return the metric prefix. May be <code>null</code>
	 */
	public String getSourcePrefix()
	{
		return m_SourcePrefix;
	}
	
	
	/**
	 * Set the source's prefix on the metric path.
	 * @param prefix
	 */
	public void setSourcePrefix(String prefix)
	{
		m_SourcePrefix = prefix;
	}
	
	
	/**
	 * Returns the position of the source in the metric path.
	 * A negative value means the position has not be set.
	 * A value of 0 means the source is the first element 
	 * in the metric path.
	 * @return A +ve number if the position is set else a
	 * 		   -ve value is returned.
	 */
	public int getSourcePosition()
	{
		return m_SourcePosition;
	}
	
	
	/**
	 * Sets the position of the source in the metric path.
	 * A -ve value indicates the position is  unset.
	 * A value of 0 means the source is the first element 
	 * in the metric path.
	 * @param position
	 */
	public void setSourcePosition(int position)
	{
		m_SourcePosition = position;
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
     * Returns the unique identifier for this time series, if known, which uniquely
     * identifies a time series by data type, source, metric and attributes.
     * @return the time series ID, or 0 if not known.
     */
    public int getTimeSeriesId()
    {
    	return m_TimeSeriesId;
    }
    
    
    /**
     * Returns the unique identifier for this time series, which uniquely
     * identifies the time series by data type, source, metric and attributes.
     * @param timeSeriesId the time series ID, or 0 if not known.
     */
    public void setTimeSeriesId(int timeSeriesId)
    {
    	m_TimeSeriesId = timeSeriesId;
    }
    

    /**
     * If this time series is an external time series get the time series 
     * external key.
     * This field is <code>transient</code> and will not be serialised.
     * @return the external key or <code>null</code> if it has not been set.
     */
    public String getExternalKey()
    {
    	return m_ExternalKey;
    }

    /**
     * Set the external key for a time series which is stored externally.
     * This field is <code>transient</code> and will not be serialised.
     * @param externalKey
     */
    public void setExternalKey(String externalKey)
    {
    	m_ExternalKey = externalKey;
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
    	if (bothStringsAreNullOrEqual(m_DataType, other.getDataType()) == false)
    	{
   			return false;
    	}

    	// Compare metrics.
    	if (bothStringsAreNullOrEqual(m_Metric, other.getMetric()) == false)
    	{
   			return false;
    	}

    	if (bothStringsAreNullOrEqual(m_MetricPrefix, other.getMetricPrefix()) == false)
    	{
    		return false;
    	}
    	
    	if (m_MetricPosition >= 0 && other.getMetricPosition() >= 0)
    	{
    		if (m_MetricPosition != other.getMetricPosition())
    		{
    			return false;
    		}
    	}
    	else if ((m_MetricPosition < 0 && other.getMetricPosition() < 0) == false)
    	{
    		// if both numbers are not -ve or not >= 0 then the signs
    		// are different and they are not equal.
    		// all -ve values are considered the same.
    		return false;
    	}
    	
    	// Compare sources
    	if (bothStringsAreNullOrEqual(m_Source, other.getSource()) == false)
    	{
    		return false;
    	}
    	
    	if (bothStringsAreNullOrEqual(m_SourcePrefix, other.getSourcePrefix()) == false)
    	{
    		return false;
    	}
    	
    	if (m_SourcePosition >= 0 && other.getSourcePosition() >= 0)
    	{
    		if (m_SourcePosition != other.getSourcePosition())
    		{
    			return false;
    		}
    	}
    	else if ((m_SourcePosition < 0 && other.getSourcePosition() < 0) == false)
    	{
    		// if both numbers are not -ve or not >= 0 then the signs
    		// are different and they are not equal.
    		// all -ve values are considered the same.
    		return false;
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
    	if (bothStringsAreNullOrEqual(m_AttributeLabel, other.getAttributeLabel()) == false)
    	{
    		return false;
    	}
    	
    	if (bothStringsAreNullOrEqual(m_ExternalKey, other.getExternalKey()) == false)
    	{
    		return false;
    	}
    	
    	// NB. Do not compare time series IDs in case the ID has not been set.
    	
        return true;
    }
    
    
    /**
     * Utility method to test for the equality of the two
     * String parameters. Handles <code>null</code> properly
     * returning true if both are <code>null</code>.
     * @param left
     * @param right
     * @return true if both parameters are <code>null</code>
     *         or equal.
     */
    private boolean bothStringsAreNullOrEqual(String left, String right)
    {
    	if (left == null && right == null)
    	{
    		return true;
    	}
    	
    	if (left != null && right != null)
    	{
    		return left.equals(right);
    	}
    	else
    	{
    		return false;
    	}
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
		strRep.append(", MetricPrefix=");
		strRep.append(m_MetricPrefix);
		strRep.append(", MetricPosition=");
		strRep.append(m_MetricPosition);
		
		strRep.append(", source=");
		strRep.append(m_Source);
		strRep.append(", SourcePrefix=");
		strRep.append(m_SourcePrefix);
		strRep.append(", SourcePosition=");
		strRep.append(m_SourcePosition);
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
		
		if (m_TimeSeriesId != 0)
		{
			strRep.append(", timeSeriesId=");
			strRep.append(m_TimeSeriesId);
		}
		
		if (m_ExternalKey != null)
		{
			strRep.append(", externalKey=");
			strRep.append(m_ExternalKey);
		}

		strRep.append('}');
		
		return strRep.toString();
	}
	
}
