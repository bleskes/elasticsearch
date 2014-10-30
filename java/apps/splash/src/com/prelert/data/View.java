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

/**
 * Abstract base class for encapsulating configuration properties of a desktop view
 * which are common to all types of View, such as the view name, and style ID.
 * <p>
 * Desktop views should extend this class, adding properties which
 * are specific to that particular type of view.
 * @author Pete Harverson
 */
public abstract class View implements Serializable
{
	private String				m_Name;
	private String				m_DataType;
	private DataSourceCategory	m_DataCategory;
	private int					m_AutoRefreshFrequency = 60000;	// Default of 1 minute.

	
	
	/**
	 * Returns the name of the View.
	 * @return name of the View.
	 */
	public String getName()
    {
    	return m_Name;
    }


	/**
	 * Sets the name of the View.
	 * @param name the name of the View.
	 */
	public void setName(String name)
    {
    	m_Name = name;
    }
	
	
	/**
	 * Returns the data type, such as 'apache_logs' or 'system_udp', which is used
	 * to identify the particular type of data being displayed.
	 * @return the data type.
	 */
	public String getDataType()
    {
    	return m_DataType;
    }


	/**
	 * Sets the data type, such as 'apache_logs' or 'system_udp', which is used
	 * to identify the particular type of data being displayed.
	 * @param dataType the data type.
	 */
	public void setDataType(String dataType)
    {
    	m_DataType = dataType;
    }
	
	
	/**
	 * Returns the category of the data displayed in the view.
     * @return the category of data displayed in this view, or <code>null</code>
     * 		if the view is not restricted to one particular category e.g. causality views.
     */
    public DataSourceCategory getDataCategory()
    {
    	return m_DataCategory;
    }

    
    /**
	 * Sets the category of data displayed in the view.
     * @param dataCategory the category of data displayed in this view.
     */
    public void setDataCategory(DataSourceCategory dataCategory)
    {
    	m_DataCategory = dataCategory;
    }
    
    
	/**
	 * Returns the data source type of this view. Convenience method
	 * for retrieving both the data source name (e.g. p2ps logs, p2psmon user usage) 
	 * and the data source category (e.g. notification, time series) in one call.
	 * @return dataSourceType the data source type, or <code>null</code> if this
	 * 	View is not showing not data for one specific source type.
	 */
	public DataSourceType getDataSourceType()
	{
		DataSourceType dsType = null;
		
		if (m_DataType != null && m_DataCategory != null)
		{
			dsType = new DataSourceType(m_DataType, m_DataCategory);
		}
		
		return dsType;
	}
	
	
	/**
	 * Returns the automatic refresh frequency to use for the view.
     * @return the refresh frequency, in milliseconds, which has a default value
     * 			of 1 minute.
     */
    public int getAutoRefreshFrequency()
    {
    	return m_AutoRefreshFrequency;
    }


    /**
	 * Sets the automatic refresh frequency to use for the view.
     * @param autoRefreshFrequency the refresh frequency, in milliseconds.
     */
    public void setAutoRefreshFrequency(int autoRefreshFrequency)
    {
    	m_AutoRefreshFrequency = autoRefreshFrequency;
    }
    

	/**
	 * Creates a View based on the properties of this view, and appends the 
	 * specified filter to any filter which has been defined for this view.
	 * @param addFilter the filter to append to this view's filter (with an AND
	 * operator) e.g. description = ? AND DATE_FORMAT(time,'%Y-%m-%d %H:%i') = ?.
	 * @param addFilterArgs filter arguments to substitute into '?' placeholders
	 * in the supplied filter.
	 * @return new View object.
	 */
	public abstract View createCopyAndAppendFilter(
			String filterAttribute, String filterValue);


	/**
	 * Returns a String summarising the properties of this View.
	 * @return a String displaying the properties of the View.
	 */
    public String toString()
    {
	   StringBuilder strRep = new StringBuilder("{");
	   
	   strRep.append("Name=");
	   strRep.append(m_Name);
	   
	   strRep.append(",Data Type=");
	   strRep.append(getDataType());
	   
	   strRep.append(",Data Category=");
	   strRep.append(getDataCategory());
	   
	   strRep.append(",Refresh Frequency=");
	   strRep.append(m_AutoRefreshFrequency);
	   
	   strRep.append('}');
	   
	   return strRep.toString();
    }
}
