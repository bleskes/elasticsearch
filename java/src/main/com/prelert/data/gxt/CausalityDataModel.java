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

package com.prelert.data.gxt;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.extjs.gxt.ui.client.data.BaseModelData;
import com.prelert.data.Attribute;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;
import static com.prelert.data.PropertyNames.*;


/**
 * Extension of the GXT BaseModelData class for causality data, encapsulating a
 * time series feature or set of notifications with common attributes.
 * @author Pete Harverson
 */
public class CausalityDataModel extends BaseModelData
{
    private static final long serialVersionUID = 5010521215257320341L;
    
    @SuppressWarnings("unused")
	private List<Attribute> m_Attributes;		// DO NOT DELETE - custom field serializer to ensure successful GWT-RPC for attributes field!


	/**
	 * Returns the source type of the causality data e.g. p2ps logs or
	 * UDP error data.
	 * @return the data source type.
	 */
	public DataSourceType getDataSourceType()
	{
		DataSourceType dsType = null;
		
		if (getDataSourceName() != null && getDataSourceCategory() != null)
		{
			dsType = new DataSourceType(getDataSourceName(), getDataSourceCategory());
		}
		
		return dsType;
	}


	/**
	 * Sets the source type of the causality data e.g. p2ps logs or UDP error data.
	 * @param dataSourceType the data source type.
	 */
	public void setDataSourceType(DataSourceType dataSourceType)
	{
		set(TYPE, dataSourceType.getName());
		set(CATEGORY, dataSourceType.getDataCategory().toString());
	}
	
	
	/**
	 * Returns the category of the data source of the causality data
	 * e.g. notification or time series.
	 * @return the category of the causality data.
	 */
	public DataSourceCategory getDataSourceCategory()
	{
		String category = get(CATEGORY);
		return DataSourceCategory.getValue(category);
	}
	
	
	/**
	 * Returns the name of the data source of the causality data e.g. p2pslog or
	 * system_udp.
	 * @return the data source name.
	 */
	public String getDataSourceName()
	{
		return get(TYPE);
	}


	/**
	 * Returns the description of the causality data, such as the notification
	 * description or the name of the time series metric.
	 * @return a description of the causality data.
	 */
	public String getDescription()
	{
		return get(DESCRIPTION);
	}


	/**
	 * Sets the description of the causality data, such as the notification
	 * description or the name of the time series metric.
	 * @param description description of the causality data.
	 */
	public void setDescription(String description)
	{
		set(DESCRIPTION, description);
	}


	/**
	 * Returns the source (server) of the causality data.
	 * @return the name of the source (server).
	 */
	public String getSource()
	{
		return get(SOURCE);
	}


	/**
	 * Sets the name of the source (server) of the causality data.
	 * @param source the name of the source (server).
	 */
	public void setSource(String source)
	{
		set(SOURCE, source);
	}


	/**
	 * Returns a list of additional attributes associated with the underlying 
	 * notification or time series data.
	 * @return the list of attributes, or <code>null</code> if there are no 
	 * 	additional attributes.
	 */
	public List<Attribute> getAttributes()
	{
		return get(ATTRIBUTES);
	}


	/**
	 * Sets a list of additional attributes associated with the underlying 
	 * notification or time series data.
	 * @param attributes the list of additional attributes.
	 */
	public void setAttributes(List<Attribute> attributes)
	{
		// Clear out the previous attributes.
		List<Attribute> oldAttributes = getAttributes();
		if (oldAttributes != null)
		{
			for (Attribute attribute : oldAttributes)
			{
				remove(attribute.getAttributeName());
			}
		}
		
		set(ATTRIBUTES, attributes);
		if (attributes != null)
		{
			// Also store as separate properties.
			for (Attribute attribute : attributes)
			{
				set(attribute.getAttributeName(), attribute.getAttributeValue());
			}
		}
	}


	/**
	 * Returns the start time of the causality data i.e. the time of
	 * the earliest notification or time series feature.
	 * @return the start time i.e. the time of the earliest notification or time 
	 * 		series feature.
	 */
	public Date getStartTime()
	{
		return get(START_TIME);
	}


	/**
	 * Sets the start time of the causality data i.e. the time of
	 * the earliest notification or time series feature.
	 * @param startTime the start time i.e. the time of the earliest notification 
	 * 		or time series feature.
	 */
	public void setStartTime(Date startTime)
	{
		set(START_TIME, startTime);
	}


	/**
	 * Returns the end time of the causality data i.e. the time of the latest 
	 * notification or time series feature.
	 * @return the end time i.e. the time of the latest notification or time 
	 * 		series feature.
	 */
	public Date getEndTime()
	{
		return get(END_TIME);
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
		set(END_TIME, endTime);
	}


	/**
	 * Returns the total notification or time series feature count.
	 * @return the total count.
	 */
	public int getCount()
	{
		return get(COUNT, new Integer(0));
	}


	/**
	 * Sets the total notification or time series feature count.
	 * @param count the total count.
	 */
	public void setCount(int count)
	{
		set(COUNT, count);
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
		return get(SIGNIFICANCE, new Integer(0));
	}


	/**
	 * Sets the significance of the causality data, indicating a percentage
	 * between 1 and 100.
	 * @param significance the significance.
	 */
	public void setSignificance(int significance)
	{
		set(SIGNIFICANCE, significance);
	}


	/**
	 * Returns the magnitude of the causality data. For notifications this is
	 * equal to the count. For time series types, this is equal to the magnitude 
	 * of the rate of change.
     * @return the magnitude, a double whose value is equal to or greater than 0.
     * 	Where this object encapsulates more than a single notification or time series
     *  feature, this will be the highest notification count or feature size.
	 */
	public int getMagnitude()
	{
		return get(MAGNITUDE, new Integer(0));
	}


	/**
	 * Sets the magnitude of the causality data. For notifications this is
	 * equal to the count. For time series types, this is equal to the magnitude 
	 * of the rate of change.
     * @param magnitude the magnitude, a double whose value is equal to or greater than 0.
     * 	Where this object encapsulates more than a single notification or time series
     *  feature, this should be the highest notification count or feature size.
	 */
	public void setMagnitude(int magnitude)
	{
		set(MAGNITUDE, magnitude);
	}
	
	
	/**
	 * Returns the id of one of the set of notifications or time series features 
	 * mapped to this item of causality data
	 * @return the evidence id, or 0 if no evidence id has been set.
	 */
	public int getEvidenceId()
	{
		Integer idInt = get(EVIDENCE_ID, new Integer(0));
    	return idInt.intValue();
	}
	
	
	/**
	 * Sets the id of one of the set of notifications or time series features 
	 * mapped to this item of causality data
	 * @param id the evidence id.
	 */
	public void setEvidenceId(int id)
	{
		set(EVIDENCE_ID, id);
	}
	
	
	/**
	 * For time series type causality data, returns the 'type id', which distinguishes 
	 * a time series type by its data type and metric e.g. system_udp/packets_received.
     * @return the time series type id.
     */
    public int getTimeSeriesTypeId()
    {
    	Integer idInt = get(TIME_SERIES_TYPE_ID, new Integer(-1));
    	return idInt.intValue();
    }


	/**
	 * For time series type causality data, sets the 'type id', which distinguishes a 
	 * time series type by its data type and metric e.g. system_udp/packets_received.
     * @param timeSeriesTypeId the time series type id.
     */
    public void setTimeSeriesTypeId(int timeSeriesTypeId)
    {
    	set(TIME_SERIES_TYPE_ID, timeSeriesTypeId);
    }
    
    
    /**
     * For time series type causality data, returns the id of the time series, which
     * uniquely identifies a time series by data type, source, metric and attributes.
     * @return the time series id, or 0 if no id has been set.
     */
    public int getTimeSeriesId()
    {
    	Integer idInt = get(TIME_SERIES_ID, new Integer(0));
    	return idInt.intValue();
    }
    
    
    /**
     * For time series type causality data, sets the id of the time series, which
     * uniquely identifies a time series by data type, source, metric and attributes.
     * @param timeSeriesId the time series id.
     */
    public void setTimeSeriesId(int timeSeriesId)
    {
    	set(TIME_SERIES_ID, timeSeriesId);
    }
    
    
    /**
	 * For time series type causality data, returns the factor by which the data
	 * values should be scaled in relation to other time series.
     * @return the scaling factor, a value between 0 and 1.
     */
    public double getScalingFactor()
    {
    	return get("scalingFactor", new Double(1));
    }


	/**
	 * For time series type causality data, sets the factor by which the data
	 * values should be scaled in relation to other time series.
     * @param scalingFactor the scaling factor, a value between 0 and 1.
     */
    public void setScalingFactor(double scalingFactor)
    {
    	set("scalingFactor", scalingFactor);
    }
	
	
	/**
	 * Tests this CausalityDataModel object for equality with another object.
	 * @return true if the comparison object is a CausalityDataModel object with
	 * 	identical properties to this CausalityDataModel.
     */
	@Override
    public boolean equals(Object obj)
    {
		if (obj == null)
		{
			return false;
		}
		
		if (this == obj)
		{
			return true;
		}
		
		if ((obj instanceof CausalityDataModel) == false)
		{
			return false;
		}
		
		CausalityDataModel other = (CausalityDataModel)obj;

        Map<String, Object> thisProps = getProperties();
        Map<String, Object> otherProps = other.getProperties(); 
        
        return thisProps.equals(otherProps);
    }
    
    
    /**
     * Compares this CausalityDataModel to another CausalityDataModel, ignoring 
     * metric fields such as time, significance, magnitude and count. Two CausalityDataModel
     * objects are considered equal if the following fields are identical:
     * <ul>
     * <li>dataSourceType</li>
     * <li>description</li>
     * <li>source</li>
     * <li>attributes</li>
     * </ul>
     * @param other the CausalityDataModel to compare with this.
     * @return <code>true</code> if the argument is not null and it represents an 
     * 		equivalent CausalityDataModel ignoring metric and any additional
     * 		user-defined properties; <code>false</code> otherwise.
     */
    public boolean equalsIgnoreMetrics(CausalityDataModel other)
    {
		if (other == null)
		{
			return false;
		}
		
		if (this == other)
		{
			return true;
		}
		

		// Compare each of the non-metric fields that define the time series or notification.
		boolean result = getDataSourceType().equals(other.getDataSourceType());
		result = result && (getDescription().equals(other.getDescription()));
		result = result && (getSource().equals(other.getSource()));
		
		boolean bothNull = (getAttributes() == null && other.getAttributes() == null);
		if (!bothNull)
		{
			boolean bothNonNull = (getAttributes() != null && other.getAttributes() != null);
			result = result && bothNonNull && (getAttributes().equals(other.getAttributes()));
		}
		
		return result;
    }
	
	
	@Override
	public String toString()
	{
		return getProperties().toString();
	}
}
