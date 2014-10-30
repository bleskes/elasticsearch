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

import com.extjs.gxt.ui.client.data.BaseModelData;


/**
 * Extension of the GXT BaseModelData class for aggregated causality data.
 * <br>
 * The class summarizes a set of causality objects which have been aggregated 
 * by a particular attribute e.g. all causality data in an incident where the 
 * service attribute = 'PRISM'. Depending on the attribute by which the causality
 * objects have been aggregated, the object may represent both
 * notifications and time series features.
 * @author Pete Harverson
 */
public class CausalityAggregateModel extends BaseModelData
{
    private static final long serialVersionUID = -1839248283082661311L;
    
    @SuppressWarnings("unused")
	private CausalityDataModel		m_TopCausalityData;		// DO NOT DELETE - custom field serializer.
    
    
    /**
     * Returns a textual summary of the aggregated causality data.
     * @return a summary of the aggregated data.
     */
    public String getSummary()
    {
    	return get("summary");
    }
    
    
    /**
     * Sets a textual summary of the aggregated causality data.
     * @param summary a summary of the aggregated data.
     */
    public void setSummary(String summary)
    {
    	set("summary", summary);
    }
    
    
    /**
     * Returns the name of the attribute by which the causality data has been
     * aggregated.
     * @return the name of the aggregated attribute.
     */
    public String getAggregateBy()
    {
    	return get("by");
    }
    
    
    /**
     * Sets the name of the attribute by which the causality data has been aggregated.
     * @param attributeName the name of the aggregated attribute.
     */
    public void setAggregateBy(String attributeName)
    {
    	set("by", attributeName);
    }
    
    
	/**
	 * Returns the value of the attribute by which the causality data has been
	 * aggregated.
	 * @return the value of the aggregated attribute, or <code>null</code> if 
	 * 	the data for several aggregated values have been aggregated together,
	 * 	for example for an 'Others' result.
	 */
	public String getAggregateValue()
	{
		return get("value");
	}


	/**
	 * Sets the value of the attribute by which the causality data has been
	 * aggregated.
	 * @param aggregateValue the value of the aggregated attribute.
	 */
	public void setAggregateValue(String aggregateValue)
	{
		set("value", aggregateValue);
	}
	
	
	/**
	 * Returns a flag indicating whether the value of the attribute by which the
	 * causality data was aggregated is <code>null</code> in the source notifications
	 * or time series features.
	 * @return <code>true</code> if the value of the aggregated attribute is <code>null</code> 
	 * in the source notifications or time series features. Will return <code>false</code>
	 * for cases where data from several non-<code>null</code> aggregated 
	 * values have been aggregated together (for example for an 'Others' result).
	 */
	public boolean isAggregateValueNull()
	{
		return get("valueIsNull", new Boolean(false));
	}
	
	
	/**
	 * Sets the flag indicating whether the value of the attribute by which the
	 * causality data was aggregated is <code>null</code> in the source notifications
	 * or time series features.
	 * @param isNull <code>true</code> if the value of the aggregated attribute 
	 * is <code>null</code> in the source notifications or time series features. 
	 * Set to <code>false</code> for cases where data from several non-<code>null</code> 
	 * aggregated values have been aggregated together (for example for an 'Others' result).
	 */
	public void setAggregateValueNull(boolean isNull)
	{
		set("valueIsNull", isNull);
	}


	/**
	 * Returns the start time of the aggregated causality object i.e. the time of
	 * the earliest notification or time series feature.
	 * @return the start time i.e. the time of the earliest notification or time 
	 * 		series feature.
	 */
	public Date getStartTime()
	{
		return get("startTime");
	}


	/**
	 * Sets the start time of the aggregated causality object i.e. the time of
	 * the earliest notification or time series feature.
	 * @param startTime the start time i.e. the time of the earliest notification 
	 * 		or time series feature.
	 */
	public void setStartTime(Date startTime)
	{
		set("startTime", startTime);
	}


	/**
	 * Returns the end time of the aggregated causality object i.e. the time of
	 * the latest notification or time series feature.
	 * @return the end time i.e. the time of the latest notification or time 
	 * 		series feature. If there is only one item in the aggregated object,
	 * 		this is the time of that particular notification or feature.
	 */
	public Date getEndTime()
	{
		return get("endTime");
	}


	/**
	 * Sets the end time of the aggregated causality object i.e. the time of
	 * the latest notification or time series feature.
	 * @param endTime the end time i.e. the time of the latest notification or 
	 * 		time series feature. If there is only one item in the aggregated object,
	 * 		this is the time of that particular notification or feature.
	 */
	public void setEndTime(Date endTime)
	{
		set("endTime", endTime);
	}


	/**
	 * Returns the id of the 'top' (often measured by significance) notification
	 * or time series features in the aggregated set.
	 * @return the id of the 'top' notification or time series feature, 
	 * 	or 0 if no evidence id has been set.
	 */
	public int getTopEvidenceId()
	{
		return get("topEvidenceId", new Integer(0));
	}


	/**
	 * Sets the id of the 'top' (often measured by significance) notification
	 * or time series features in the aggregated set.
	 * @param evidenceId the id of the 'top' notification or time series feature.
	 */
	public void setTopEvidenceId(int evidenceId)
	{
		set("topEvidenceId", evidenceId);
	}


	/**
	 * Returns the count of notifications in the aggregated causality object.
	 * @return the total notification count.
	 */
	public int getNotificationCount()
	{
		return get("notificationCount", new Integer(0));
	}


	/**
	 * Sets the count of notifications in the aggregated causality object.
	 * @param count the total notification count.
	 */
	public void setNotificationCount(int count)
	{
		set("notificationCount", count);
	}


	/**
	 * Returns the count of time series features in the aggregated causality object.
	 * @return the total feature count.
	 */
	public int getFeatureCount()
	{
		return get("featureCount", new Integer(0));
	}


	/**
	 * Sets the count of time series features in the aggregated causality object.
	 * @param count the total feature count.
	 */
	public void setFeatureCount(int count)
	{
		set("featureCount", count);
	}


	/**
	 * Returns the number of different sources (servers) on which notifications or
	 * time series features in the aggregated set have occurred.
	 * @return the number of sources.
	 */
	public int getSourceCount()
	{
		return get("sourceCount", new Integer(0));
	}


	/**
	 * Sets the number of different sources (servers) on which notifications or
	 * time series features in the aggregated set have occurred.
	 * @param count the number of sources.
	 */
	public void setSourceCount(int count)
	{
		set("sourceCount", count);
	}


	/**
	 * Returns the name of the source which has the highest sum of notifications
	 * and features.
	 * @return the name of the source which has the highest sum of notifications
	 * 	and features.
	 */
	public String getTopSourceName()
	{
		return get("topSource");
	}


	/**
	 * Sets the name of the source which has the highest sum of notifications
	 * and features.
	 * @param sourceNames the name of the source which has the highest sum 
	 * 	of notifications and features.
	 */
	public void setTopSourceName(String sourceName)
	{
		set("topSource", sourceName);
	}
	
	
	/**
	 * For cases where causality data has been aggregated by type, returns the
	 * name of that data source.
	 * @return the name of the data source, or <code>null</code> if 
	 * 	causality data has not been aggregated by type.
	 */
	public String getDataSourceName()
	{
		return get("dataSourceName");
	}
	
	
	/**
	 * For cases where causality data has been aggregated by type, sets the
	 * name of that data source.
	 * @param dataSourceName the name of the data source.
	 */
	public void setDataSourceName(String dataSourceName)
	{
		set("dataSourceName", dataSourceName);
	}
	
	
	/**
	 * Returns the 'top' (often measured by significance) item of causality data
	 * in the aggregated set.
	 * @return CausalityDataModel encapsulating the 'top' notification(s) or time series feature.
	 * 	Note that all the fields in the object may not have been populated.
	 */
	public CausalityDataModel getTopCausalityData()
	{
		return get("topCausalityData");
	}
	
	
	/**
	 * Sets the 'top' (often measured by significance) item of causality data
	 * in the aggregated set.
	 * @param causalityData CausalityData encapsulating the 'top' notification(s)
	 * 	or time series feature.
	 */
	public void setTopCausalityData(CausalityDataModel causalityData)
	{
		set("topCausalityData", causalityData);
	}
	
	
	@Override
	public String toString()
	{
		return getProperties().toString();
	}
}
