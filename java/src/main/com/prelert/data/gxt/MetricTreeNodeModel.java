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

import java.util.List;

import com.extjs.gxt.ui.client.data.BaseModelData;
import com.prelert.data.Attribute;
import com.prelert.data.DataSourceCategory;

import static com.prelert.data.PropertyNames.*;


/**
 * Extension of the GXT BaseModelData class for nodes in the metric path tree.
 * @author Pete Harverson
 */
public class MetricTreeNodeModel extends BaseModelData
{
    private static final long serialVersionUID = 5979061559730688226L;
    
    @SuppressWarnings("unused")
	private DataSourceCategory		m_Category;		// DO NOT DELETE - custom field serializer.

    
    /**
     * Returns the name of the property represented by this node in the metric tree.
     * @return the name of the property for this level in the metric path tree.
     */
    public String getName()
    {
    	return get(NAME);
    }
    
    
    /**
     * Sets the name of the property represented by this node in the metric tree.
     * @param name the name of the property for this level in the metric path tree.
     */
    public void setName(String name)
    {
    	set(NAME, name);
    }
        
    
    /**
     * Returns the value represented by this node in the metric tree.
     * @return the value for this level in the metric path tree.
     */
    public String getValue()
    {
    	return get(VALUE);
    }
    
    
    /**
     * Sets the value represented by this node in the metric tree.
     * @param value the value for this level in the metric path tree.
     */
    public void setValue(String value)
    {
    	set(VALUE, value);
    }
    
    
    /**
     * Returns the prefix to use as the separator in the path for the next level
     * in the tree.
     * @return the prefix for the next level.
     */
    public String getPrefix()
	{
		return get("prefix");
	}


    /**
     * Sets the prefix to use as the separator in the path for the next level
     * in the tree.
     * @param prefix the prefix for the next level.
     */
	public void setPrefix(String prefix)
	{
		set("prefix", prefix);
	}


	/**
	 * Returns the partial metric path to this node in the tree.
	 * @return the partial path.
	 */
	public String getPartialPath()
	{
		return get("partialPath");
	}


	/**
	 * Sets the partial metric path to this node in the tree.
	 * @param partialPath the partial path.
	 */
	public void setPartialPath(String partialPath)
	{
		set("partialPath", partialPath);
	}
	
	
	/**
	 * Returns the full metric path of this node in the tree.
	 * @return a concatenation of the partial path, prefix and value.
	 */
	public String getFullPath()
	{
		StringBuilder path = new StringBuilder();
		path.append(getPartialPath()).append(getPrefix()).append(getValue());
		return path.toString();
	}


	/**
	 * Returns the opaque integer ID representing this node, used by some external
	 * plugins to obtain metric path data.
	 * @return opaque numeric ID, or 0 if no id has been set.
	 */
	public int getOpaqueNum()
	{
		return get(OPAQUE_NUM, new Integer(0));
	}


	/**
	 * Sets the opaque integer ID representing this node, used by some external
	 * plugins to obtain metric path data.
	 * @param opaqueNum numeric ID.
	 */
	public void setOpaqueNum(int opaqueNum)
	{
		set(OPAQUE_NUM, opaqueNum);
	}


	/**
	 * Returns the value of the textual field holding the GUID, used by some external
	 * plugins to obtain metric path data.
	 * @return opaque textual GUID.
	 */
	public String getOpaqueStr()
	{
		return get(OPAQUE_STR);
	}


	/**
	 * Sets the value of the textual field holding the GUID, used by some external
	 * plugins to obtain metric path data.
	 * @param opaqueStr opaque textual GUID.
	 */
	public void setOpaqueStr(String opaqueStr)
	{
		set(OPAQUE_STR, opaqueStr);
	}


	/**
	 * Returns the data source type of this metric path tree node.
	 * @return the data type.
	 */
	public String getType()
	{
		return get(TYPE);
	}


	/**
	 * Sets the data source type of this metric path tree node.
	 * @param type the data type.
	 */
	public void setType(String type)
	{
		set(TYPE, type);
	}


	/**
	 * Returns the category of the data represented by this metric path tree node
	 * i.e. notification or time series data.
	 * @return the data category, such as notification or time series.
	 */
	public DataSourceCategory getCategory()
	{
		return get(CATEGORY);
	}


	/**
	 * Sets the category of the data represented by this metric path tree node
	 * i.e. notification or time series data.
	 * @param category the data category, such as notification or time series.
	 */
	public void setCategory(DataSourceCategory category)
	{
		set(CATEGORY, category);
	}


	/**
	 * Returns the source (server) of this metric path tree node.
	 * @return the source, or <code>null</code> if it represents data from 
	 * 	multiple sources.
	 */
	public String getSource()
	{
		return get(SOURCE);
	}


	/**
	 * Sets the source (server) of this metric path tree node.
	 * @param source the source, or <code>null</code> if it represents data from 
	 * 	multiple sources.
	 */
	public void setSource(String source)
	{
		set(SOURCE, source);
	}


	/**
	 * For time series type tree nodes, returns the name of the metric if known.
	 * @return the time series metric, if known, at this level in the tree.
	 */
	public String getMetric()
	{
		return get(METRIC);
	}


	/**
	 * For time series type tree nodes, sets the name of the metric.
	 * @param metric the time series metric.
	 */
	public void setMetric(String metric)
	{
		set(METRIC, metric);
	}


	/**
	 * Returns the list of all attributes up to this level in the metric path.
	 * @return list of attributes up this level in the tree.
	 */
	public List<Attribute> getAttributes()
	{
		return get(ATTRIBUTES);
	}


	/**
	 * Sets the list of all attributes up to this level in the metric path.
	 * @param attributes list of attributes up this level in the tree.
	 */
	public void setAttributes(List<Attribute> attributes)
	{
		set(ATTRIBUTES, attributes);
	}
	
	
	/**
	 * For time series type tree nodes that correspond to a single time series,
	 * returns the time series ID if known.
	 * @return the time series ID, if uniquely determined, at this level in the
	 *         tree, or <code>0</code> if this node does not correspond to a single
	 *         time series or the time series ID is unknown.
	 */
	public int getTimeSeriesId()
	{
		return get(TIME_SERIES_ID, new Integer(0));
	}


	/**
	 * For time series type tree nodes, sets the time series ID.  This must only
	 * be set if the level of the tree corresponds to a single time series.
	 * @param timeSeriesId the time series ID.
	 */
	public void setTimeSeriesId(int timeSeriesId)
	{
		set(TIME_SERIES_ID, timeSeriesId);
	}


	/**
	 * Returns whether this is a leaf node.
	 * @return <code>true</code> if it is a leaf node, <code>false</code> for
	 * 	 a branch.
	 */
	public boolean isLeaf()
	{
		return get("isLeaf", new Boolean(false));
	}


	/**
	 * Sets the flag which indicates if this is a leaf node.
	 * @param isLeaf <code>true</code> if it is a leaf node, <code>false</code> for
	 * 	 a branch.
	 */
	public void setIsLeaf(boolean isLeaf)
	{
		set("isLeaf", isLeaf);
	}
	
	
	/**
	 * Returns whether or not this node represents a wildcard in the metric 
	 * path e.g. 'all sources' or 'all users'.
	 * @return <code>true</code> if it is a wildcard, <code>false</code> if not.
	 */
	public boolean isWildcard()
	{
		return get("isWildcard", new Boolean(false));
	}
	
	
	/**
	 * Sets the flag which indicates if this is a wildcard node e.g. 'all sources' or 'all users'.
	 * @param isWildcard <code>true</code> if it is a wildcard, <code>false</code> if not.
	 */
	public void setIsWildcard(boolean isWildcard)
	{
		set("isWildcard", isWildcard);
	}
	
	
	/**
	 * Returns whether this node has any wildcard at any point along its metric 
	 * path e.g. 'all sources' or 'all users'.
	 * @return <code>true</code> if it has a wildcard, <code>false</code> if not.
	 */
	public boolean hasAnyWildcard()
	{
		return get("hasAnyWildcard", new Boolean(false));
	}
	
	
	/**
	 * Sets the flag which indicates if this node has any wildcard at any point
	 * along its metric path e.g. 'all sources' or 'all users'.
	 * @param hasAnyWildcard <code>true</code> if it has a wildcard, <code>false</code> if not.
	 */
	public void setHasAnyWildcard(boolean hasAnyWildcard)
	{
		set("hasAnyWildcard", hasAnyWildcard);
	}
    
    
    /**
	 * Returns a summary of this metric path tree node.
	 * @return String representation of the metric path tree node, showing all fields
	 * 	and values.
	 */
    @Override
	public String toString()
	{
		StringBuilder strRep = new StringBuilder();
		strRep.append(getProperties());
		
		return strRep.toString();
	}
}
