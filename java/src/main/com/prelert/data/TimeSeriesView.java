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
import java.util.ArrayList;
import java.util.List;


/**
 * View subclass for a Time Series View. It defines properties that are specific
 * to time series views, including the name of the metric and the names of any 
 * attributes associated with the data.
 * @author Pete Harverson
 */
public class TimeSeriesView extends View implements Serializable
{
	private static final long serialVersionUID = 8513541388211923031L;
	
	private List<String>		m_AttributeNames;
	private List<String> 		m_Metrics;
	private boolean				m_IsAggregationSupported;
	private EvidenceView		m_FeaturesView;
	
	
	/**
	 * Creates a new View object to show time series data.
	 */
	public TimeSeriesView()
	{
		setDataCategory(DataSourceCategory.TIME_SERIES);
	}
	
	
	/**
	 * Returns a list of the metrics available for this time series view.
	 * @return list of the time series metrics for this view.
	 */
	public List<String> getMetrics()
	{
		return m_Metrics;
	}


	/**
	 * Sets the list of the metrics available for this time series view.
	 * @param list of the time series metrics for this view.
	 */
	public void setMetrics(List<String> metrics)
	{
		m_Metrics = metrics;
	}
	
	
	/**
	 * Adds a metric to the list of metrics available for this time series view.
	 * @param metric name of the metric to add for this view.
	 */
	public void addMetric(String metric)
	{
		if (m_Metrics == null)
		{
			m_Metrics = new ArrayList<String>();
		}
		
		m_Metrics.add(metric);
	}
	
	
	/**
	 * Returns the list of attribute names associated with this time series view
	 * e.g. username and app_id for p2psmon_users.
     * @return the list of attribute names for this view.
     */
    public List<String> getAttributeNames()
    {
    	return m_AttributeNames;
    }


	/**
	 * Sets the list of attribute names associated with this time series view
	 * e.g. username and app_id for p2psmon_users.
     * @param attributeNames list of attribute names for this view.
     */
    public void setAttributeNames(List<String> attributeNames)
    {
    	m_AttributeNames = attributeNames;
    }
    
    
	/**
	 * Adds an attribute name to the list of attributes available for this view.
	 * @param attributeName attribute name to add for this view, 
	 * 			such as 'username' or 'app_id'.
	 */
	public void addAttributeName(String attributeName)
	{
		if (m_AttributeNames == null)
		{
			m_AttributeNames = new ArrayList<String>();
		}
		
		m_AttributeNames.add(attributeName);
	}
	
	
    /**
     * Returns whether this time series view has attributes.
     * @return true if the view has attributes.
     */
    public boolean hasAttributes()
    {
    	return (m_AttributeNames != null && m_AttributeNames.size() > 0);
    }
	
	
	/**
	 * Returns whether aggregation is supported in this time series view,
	 * i.e. viewing of data points without specifying a value for every
	 * possible attribute.
	 * @return <code>true</code> if aggregate views are supported, 
	 * 	<code>false</code> otherwise.
	 */
	public boolean isAggregationSupported()
	{
		return m_IsAggregationSupported;
	}
	
	
	/**
	 * Sets whether aggregation is supported in this time series view,
	 * i.e. viewing of data points without specifying a value for every
	 * possible attribute.
	 * @param isAggregationSupported <code>true</code> if aggregate views 
	 * 		are supported, <code>false</code> otherwise.
	 */
	public void setAggregationSupported(boolean isAggregationSupported)
	{
		m_IsAggregationSupported = isAggregationSupported;
	}
	
	
	/**
	 * Returns a definition of the view to be used for listing features in the
	 * time series data displayed in this view.
     * @return EvidenceView object for listing the time series features.
     */
    public EvidenceView getFeaturesView()
    {
    	return m_FeaturesView;
    }


	/**
	 * Sets the definition of the view to be used for listing features in the
	 * time series data displayed in this view.
     * @param featuresView EvidenceView object for listing the time series features.
     */
    public void setFeaturesView(EvidenceView featuresView)
    {
    	m_FeaturesView = featuresView;
    }


	@Override
    public String toString()
    {
	   StringBuilder strRep = new StringBuilder("{");
	   
	   strRep.append("TimeSeriesView Name=");
	   strRep.append(getName());
	   
	   strRep.append(",Data Type=");
	   strRep.append(getDataType());
	   
	   strRep.append(",Metrics=");
	   if (m_Metrics != null)
	   {
		   strRep.append(m_Metrics);
	   }
	   
	   if (m_AttributeNames != null)
	   {
		   strRep.append(",Attributes=");
		   strRep.append(m_AttributeNames);
	   }
	   
	   strRep.append(",Aggregation=");
	   strRep.append(m_IsAggregationSupported);
	   
	   if (m_FeaturesView != null)
	   {
		   strRep.append(",Features View=");
		   strRep.append(m_FeaturesView);
	   }
	   
	   strRep.append('}');
	   
	   return strRep.toString();
    }
}
