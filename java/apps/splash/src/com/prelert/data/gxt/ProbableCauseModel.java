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

package com.prelert.data.gxt;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.extjs.gxt.ui.client.data.BaseModelData;

import com.prelert.data.Attribute;
import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;

/**
 * Extension of the GXT BaseModelData class for probable cause data.
 * @author Pete Harverson
 */
public class ProbableCauseModel extends BaseModelData implements Serializable
{
	private List<Attribute> m_Attributes;	// DO NOT DELETE - custom field serializer to ensure successful GWT-RPC for attributes field!


	/**
	 * Sets the data source type of this probable cause e.g. p2ps logs or
	 * UDP error data.
	 * @param dataSourceType the data source type.
	 */
	public void setDataSourceType(DataSourceType dataSourceType)
	{
		set("dataSourceName", dataSourceType.getName());
		
		// Jan 2010: Store the category as a String as otherwise errors may occur
		// when this object is transported via GWT RPC.
		set("dataSourceCategory", dataSourceType.getDataCategory().toString());
	}
	
	
	/**
	 * Returns the data source type of this model object. Convenience method
	 * for retrieving both the data source name (e.g. p2ps logs, p2psmon user usage) 
	 * and the data source category (e.g. notification, time series) in one call.
	 * @return dataSourceType the data source type.
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
	 * Returns the category of the data source of this probable cause
	 * e.g. notification or time series.
	 * @return the category of this probable cause's data source.
	 */
	public DataSourceCategory getDataSourceCategory()
	{
		String category = get("dataSourceCategory");
		return DataSourceCategory.getValue(category);
	}
	
	
	/**
	 * Returns the name of the data source of this probable cause e.g. p2ps logs or
	 * UDP error data.
	 * @return the data source name.
	 */
	public String getDataSourceName()
	{
		return get("dataSourceName");
	}
	
	
	/**
	 * Returns the id of the item of evidence (or feature for time series) mapped 
	 * to this probable cause.
	 * @return the evidence or feature id.
	 */
	public int getEvidenceId()
	{
		Integer idInt = get("evidenceId", new Integer(-1));
    	return idInt.intValue();
	}
	
	
	/**
	 * Sets the id of the item of evidence (or feature for time series) mapped 
	 * to this probable cause.
	 * @param id the evidence or feature id.
	 */
	public void setEvidenceId(int id)
	{
		set("evidenceId", id);
	}


	/**
	 * Returns the time of this probable cause.
	 * @return the time.
	 */
	public Date getTime()
	{
		return get("time");
	}


	/**
	 * Sets the value of the start time property for this probable cause.
	 * @param startTime the start time.
	 */
	public void setTime(Date time)
	{
		set("time", time);
	}


	/**
	 * Returns the description of this probable cause.
	 * @return the description.
	 */
	public String getDescription()
	{
		return get("description");
	}


	/**
	 * Sets the description for this probable cause.
	 * @param description  the description.
	 */
	public void setDescription(String description)
	{
		set("description", description);
	}


	/**
	 * Returns the source (server) of this probable cause.
	 * @return the source (server).
	 */
	public String getSource()
	{
		return get("source");
	}


	/**
	 * Sets the source (server) of this probable cause.
	 * @param source the source (server).
	 */
	public void setSource(String source)
	{
		set("source", source);
	}
	
	
	/**
	 * Returns the count of this probable cause.
     * @return the count.
     */
    public int getCount()
    {
    	int count = get("count", new Integer(1));
    	return count;
    }


	/**
	 * Sets the occurrence count of this probable cause.
     * @param count the count.
     */
    public void setCount(int count)
    {
    	set("count", count);
    }


	/**
	 * For time series type probable causes, returns the list of attributes
	 * for the time series.
	 * @return the list of attributes, or <code>null</code> if there are no attributes
	 * 	for the time series.
	 */
	public List<Attribute> getAttributes()
	{
		return get("attributes");
	}
	
	
	/**
	 * For time series type probable causes, sets the optional list of attributes 
	 * for the time series.
	 * @param attributes the list of attributes.
	 */
	public void setAttributes(List<Attribute> attributes)
	{	
		set("attributes", attributes);
	}
	
	
	/**
	 * For time series type probable causes, returns the value of the 'attributeLabel' 
	 * property. This property holds a String representation of the time series
	 * attributes for use in a UI display. For example, a p2psmon user usage probable
	 * cause may have both 'username' and 'appid' attributes whose values are returned 
	 * by this method in a format  such as 'app_id=418, username=rjones'.
	 * @return the value of the attribute label property for this probable cause, or
	 * 		an empty String if this property has not been set.
	 */
	public String getAttributeLabel()
	{
		return get("attributeLabel", "");
	}


	/**
	 * For time series type probable causes, sets the value of the 'attributeLabel' 
	 * property. This property holds a String representation of the time series
	 * attributes for use in a UI display.
	 * @param attributeLabel the attribute label for this probable cause.
	 */
	public void setAttributeLabel(String attributeLabel)
	{
		set("attributeLabel", attributeLabel);
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
    	int significance = 0;
    	Integer significanceInt = (Integer)(get("significance"));
    	if (significanceInt != null)
    	{
    		significance = significanceInt.intValue();
    	}
    	return significance;
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
		set("significance", significance);
	}
	
	
	/**
	 * Returns the magnitude of this probable cause. For notifications this is
	 * equal to the count. For time series types, this is equal to the magnitude 
	 * of the rate of change.
     * @return the magnitude, a double whose value is equal to or greater than 0.
     */
    public double getMagnitude()
    {
    	Double magnitiude = get("magnitude", new Double(1));
    	return magnitiude.doubleValue();
    }


	/**
	 * Sets the magnitude of this probable cause. For notifications this is
	 * equal to the count. For time series types, this is equal to the magnitude 
	 * of the rate of change.
     * @param magnitude the magnitude, a double whose value is equal to or greater than 0.
     */
    public void setMagnitude(double magnitude)
    {
    	set("magnitude", magnitude);
    }
    
    
    /**
	 * For time series type probable causes, returns the 'type id', which
	 * distinguishes a time series type by its data type and metric
	 * e.g. system_udp/packets_received.
     * @return the time series type id.
     */
    public int getTimeSeriesTypeId()
    {
    	Integer idInt = get("timeSeriesTypeId", new Integer(-1));
    	return idInt.intValue();
    }


	/**
	 * For time series type probable causes, sets the 'type id', which
	 * distinguishes a time series type by its data type and metric
	 * e.g. system_udp/packets_received.
     * @param timeSeriesTypeId the time series type id.
     */
    public void setTimeSeriesTypeId(int timeSeriesTypeId)
    {
    	set("timeSeriesTypeId", timeSeriesTypeId);
    }
	
	
	/**
	 * For time series type probable causes, returns the factor by which the data
	 * values should be scaled in relation to other time series probable causes.
     * @return the scaling factor, a value between 0 and 1.
     */
    public double getScalingFactor()
    {
    	return get("scalingFactor", new Double(1));
    }


	/**
	 * For time series type probable causes, sets the factor by which the data
	 * values should be scaled in relation to other time series probable causes.
     * @param scalingFactor the scaling factor, a value between 0 and 1.
     */
    public void setScalingFactor(double scalingFactor)
    {
    	set("scalingFactor", scalingFactor);
    }
    

	/**
	 * For time series type probable causes, returns the peak value of the time
	 * series in the requested time span.
     * @return the peak value.
     */
    public double getPeakValue()
    {
    	Double peakValue = get("peakValue", new Double(0));
    	return peakValue.doubleValue();
    }


	/**
	 * For time series type probable causes, sets the peak value of the time
	 * series in the requested time span.
     * @param peakValue the peak value.
     */
    public void setPeakValue(double peakValue)
    {
    	set("peakValue", peakValue);
    }
	
	
	/**
	 * For time series type probable causes, returns the time series metric.
	 * @return the time series metric.
	 */
	public String getMetric()
	{
		return get("metric");
	}
	
	
	/**
	 * For time series type probable causes, sets the time series metric.
	 * @param metric the time series metric.
	 */
	public void setMetric(String metric)
	{
		set("metric", metric);
	}
	
	
	/**
	 * Returns the flag which indicates if this probable cause is being displayed.
	 * @return <code>true</code> if it is being shown, <code>false</code> otherwise.
	 */
	public boolean getDisplay()
	{
		boolean display = get("display", false);
		return display;
	}
	
	
	/**
	 * Sets a flag to indicate whether this probable cause is being displayed.
	 * @param display <code>true</code> if it is being shown, <code>false</code>
	 * 		otherwise.
	 */
	public void setDisplay(boolean display)
	{
		set("display", display);
	}
	
	
    /**
	 * Tests this ProbableCauseModel object for equality with another object.
	 * @return true if the comparison object is a ProbableCauseModel object with
	 * 	identical properties to this ProbableCauseModel.
     */
    @Override
    public boolean equals(Object obj)
    {
    	if(this == obj)
        {
            return true;
        }
        if(!(obj instanceof ProbableCauseModel))
        {
            return false;
        }

        ProbableCauseModel other = (ProbableCauseModel)obj;
        
        Map<String, Object> thisProps = getProperties();
        Map<String, Object> otherProps = other.getProperties(); 
        
        return thisProps.equals(otherProps);
    }
	
	
	/**
	 * Returns a summary of this probable cause.
	 * @return String representation of this probable cause.
	 */
	public String toString()
	{
		return getProperties().toString();
	}
}
