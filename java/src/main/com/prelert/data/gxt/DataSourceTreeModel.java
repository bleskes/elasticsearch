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
import java.util.Map;

import com.extjs.gxt.ui.client.data.BaseTreeModel;

import com.prelert.data.DataSourceCategory;
import com.prelert.data.DataSourceType;


/**
 * Extension of BaseTreeModel for representing sources of Prelert data in a tree.
 * @author Pete Harverson
 */

@SuppressWarnings("serial")
public class DataSourceTreeModel extends BaseTreeModel implements Serializable
{
	/**
	 * Creates a new, blank DataSourceModel.
	 */
	public DataSourceTreeModel()
	{
		
	}
	
	
	/**
	 * Sets the data source type of this model object. Convenience method
	 * for setting both the data source name (e.g. p2ps logs, p2psmon user usage) 
	 * and the data source category (e.g. notification, time series) in one call.
	 * @param dataSourceType the data source type.
	 */
	public void setDataSourceType(DataSourceType dataSourceType)
	{
		// Jan 2010: Store each DataSourceType field separately as Strings as
		// otherwise errors may occur when this object is transported via GWT RPC.
		setDataSourceName(dataSourceType.getName());
		setDataSourceCategory(dataSourceType.getDataCategory());
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
	 * Sets the name of the data source e.g. p2ps logs or UDP error data.
	 * @param dataSourceName the data source name.
	 */
	public void setDataSourceName(String dataSourceName)
	{
		set("dataSourceName", dataSourceName);
	}
	
	
	/**
	 * Returns the name of the data source e.g. p2ps logs or UDP error data.
	 * @return the data source name.
	 */
	public String getDataSourceName()
	{
		return get("dataSourceName");
	}
	
	
	/**
	 * Sets the category of the data source e.g. notification or time series.
     * @param dataSourceCategory the category of data for this source type.
     */
	public void setDataSourceCategory(DataSourceCategory dataSourceCategory)
	{
		// Jan 2010: Store the category as a String as otherwise errors may occur
		// when this object is transported via GWT RPC.
		set("dataSourceCategory", dataSourceCategory.toString());
	}
	
	
	/**
	 * Returns the category of the data source.
     * @return the category of data for this source type, or <code>null</code>
     * if this tree model has no data category (e.g. the root node).
     */
    public DataSourceCategory getDataSourceCategory()
    {
    	DataSourceCategory dsCategory = null;
    	
    	String dataCategoryStr = get("dataSourceCategory");
    	
    	if (dataCategoryStr != null)
    	{
    		dsCategory = DataSourceCategory.getValue(dataCategoryStr);
    	}
    	
    	return dsCategory;
    }
	
	
	/**
	 * Sets the text to be used to identify this data source model.
	 * @param text text for the data source.
	 */
	public void setText(String text)
	{
		set("text", text);
	}
	
	
	/**
	 * Returns the text being used to identify this data source.
	 * @return text for the data source, such as could be used as the tree node label.
	 */
	public String getText()
	{
		return get("text");
	}
	
	
	/**
	 * Sets the name of the source for this data, such as the server name.
	 * @param source the name of the source. For data source type nodes, such
	 * as 'p2ps logs', a <code>null</code> should be supplied.
	 */
	public void setSource(String source)
	{
		set("source", source);
	}
	
	
	/**
	 * Returns the name of the source for this data, such as the server name.
	 * @return the name of the source or <code>null</code> for data source type
	 * or 'All sources' type nodes.
	 */
	public String getSource()
	{
		return get("source");
	}
    
    
    /**
     * Sets the count of data points collected for this data source.
     * @param count the count of data points.
     */
    public void setCount(int count)
    {
    	set("count", count);
    }
    
    
    /**
     * Returns the count of data points collected for this data source.
     * @return the count of data points, or -1 if the count has not been obtained.
     */
    public int getCount()
    {
    	int count = -1;
    	Integer countInt = (Integer)(get("count"));
    	if (countInt != null)
    	{
    		count = countInt.intValue();
    	}
    	return count;
    }
    
    
    /**
     * Returns whether this model is representing a data source type.
     * @return <code>true</code> for a source type, <code>false</code> otherwise.
     */
    public boolean isSourceType()
    {
    	String source = getSource();
    	return (source == null);
    }


	/**
	 * Returns a String representation of this data source model.
	 * @return String representation of the data source.
	 */
	public String toString()
	{
		return getProperties().toString();
	}


	/**
	 * Tests this DataSource object for equality with another object.
	 * @return true if the comparison object is a DataSourceTreeModel object with
	 * 	identical property values to this DataSourceTreeModel.
     */
    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
        {
            return true;
        }
        if(!(obj instanceof DataSourceTreeModel))
        {
            return false;
        }

        DataSourceTreeModel other = (DataSourceTreeModel)obj;
    	
    	Map<String, Object> thisProps = this.getProperties();
    	Map<String, Object> otherProps = other.getProperties();
        
        return thisProps.equals(otherProps);
    }

	
	
}
