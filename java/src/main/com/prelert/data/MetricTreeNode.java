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
import java.util.List;

/**
 * Data class for carrying information about Metric Tree nodes. 
 */
public class MetricTreeNode implements Serializable, Comparable<MetricTreeNode>
{
	private static final long serialVersionUID = 3211483925662034416L;
	
	private String m_Name;
	private String m_Value;
	private String m_Prefix;
	
	private String m_PartialPath;
	
	private Integer	m_OpaqueNum;
	private String 	m_OpaqueStr;
	
	private String m_Type;
	private DataSourceCategory m_Category;
	private String m_Source;
	
	private String m_Metric;

	/**
	 * This will only be populated if the metric tree node corresponds to a
	 * single time series.
	 */
	private Integer m_TimeSeriesId;

	/**
	 * This will only be populated if the metric tree node corresponds to a
	 * single time series.
	 */
	private String m_ExternalKey;

	private List<Attribute> m_Attributes;
	
	private boolean	m_HasAnyWildcard;
	private boolean	m_IsWildcard;
	private boolean m_IsLeaf;
	
	
	/**
	 * Creates a new object holding data on a node in a metric path tree.
	 */
	public MetricTreeNode()
	{

	}


	/**
     * Returns the name of the property represented by this node in the metric tree.
     * @return the name of the property for this level in the metric path tree.
     */
	public String getName()
	{
		return m_Name;
	}


	/**
     * Sets the name of the property represented by this node in the metric tree.
     * @param name the name of the property for this level in the metric path tree.
     */
	public void setName(String name)
	{
		m_Name = name;
	}


	/**
     * Returns the value represented by this node in the metric tree.
     * @return the value for this level in the metric path tree.
     */
	public String getValue()
	{
		return m_Value;
	}


	/**
     * Sets the value represented by this node in the metric tree.
     * @param value the value for this level in the metric path tree.
     */
	public void setValue(String value)
	{
		m_Value = value;
	}


	/**
     * Returns the prefix to use as the separator in the path for the next level
     * in the tree.
     * @return the prefix for the next level.
     */
	public String getPrefix()
	{
		return m_Prefix;
	}


	/**
     * Sets the prefix to use as the separator in the path for the next level
     * in the tree.
     * @param prefix the prefix for the next level.
     */
	public void setPrefix(String prefix)
	{
		m_Prefix = prefix;
	}


	/**
	 * Returns the partial metric path to this node in the tree.
	 * @return the partial path.
	 */
	public String getPartialPath()
	{
		return m_PartialPath;
	}


	/**
	 * Sets the partial metric path to this node in the tree.
	 * @param partialPath the partial path.
	 */
	public void setPartialPath(String partialPath)
	{
		m_PartialPath = partialPath;
	}


	/**
	 * Returns the opaque integer ID representing this node, used by some external
	 * plugins to obtain metric path data.
	 * @return opaque numeric ID.
	 */
	public Integer getOpaqueNum()
	{
		return m_OpaqueNum;
	}


	/**
	 * Sets the opaque integer ID representing this node, used by some external
	 * plugins to obtain metric path data.
	 * @param opaqueNum numeric ID.
	 */
	public void setOpaqueNum(int opaqueNum)
	{
		m_OpaqueNum = opaqueNum;
	}


	/**
	 * Returns the value of the textual field holding the GUID, used by some external
	 * plugins to obtain metric path data.
	 * @return opaque textual GUID.
	 */
	public String getOpaqueStr()
	{
		return m_OpaqueStr;
	}


	/**
	 * Sets the value of the textual field holding the GUID, used by some external
	 * plugins to obtain metric path data.
	 * @param opaqueStr opaque textual GUID.
	 */
	public void setOpaqueStr(String opaqueStr)
	{
		m_OpaqueStr = opaqueStr;
	}


	/**
	 * Returns the data source type of this metric path tree node.
	 * @return the data type.
	 */
	public String getType()
	{
		return m_Type;
	}


	/**
	 * Sets the data source type of this metric path tree node.
	 * @param type the data type.
	 */
	public void setType(String type)
	{
		m_Type = type;
	}


	/**
	 * Returns the category of the data represented by this metric path tree node
	 * i.e. notification or time series data.
	 * @return the data category, such as notification or time series.
	 */
	public DataSourceCategory getCategory()
	{
		return m_Category;
	}


	/**
	 * Sets the category of the data represented by this metric path tree node
	 * i.e. notification or time series data.
	 * @param category the data category, such as notification or time series.
	 */
	public void setCategory(DataSourceCategory category)
	{
		m_Category = category;
	}


	/**
	 * Sets the category of the data represented by this metric path tree node
	 * i.e. notification or time series data.
	 * @param category the data category, as a String representation of one of the
	 * 		enum values of DataSourceCategory.
	 */
	public void setCategory(String category)
	{
		m_Category = DataSourceCategory.getValue(category);
	}


	/**
	 * Returns the source (server) of this metric path tree node.
	 * @return the source, or <code>null</code> if it represents data from 
	 * 	multiple sources.
	 */
	public String getSource()
	{
		return m_Source;
	}


	/**
	 * Sets the source (server) of this metric path tree node.
	 * @param source the source, or <code>null</code> if it represents data from 
	 * 	multiple sources.
	 */
	public void setSource(String source)
	{
		m_Source = source;
	}


	/**
	 * For time series type tree nodes, returns the name of the metric if known.
	 * @return the time series metric, if known, at this level in the tree.
	 */
	public String getMetric()
	{
		return m_Metric;
	}


	/**
	 * For time series type tree nodes, sets the name of the metric.
	 * @param metric the time series metric.
	 */
	public void setMetric(String metric)
	{
		m_Metric = metric;
	}


	/**
	 * For time series type tree nodes that correspond to a single time series,
	 * returns the time series ID if known.
	 * @return the time series ID, if uniquely determined, at this level in the
	 *         tree.
	 */
	public Integer getTimeSeriesId()
	{
		return m_TimeSeriesId;
	}


	/**
	 * For time series type tree nodes, sets the time series ID.  This must only
	 * be set if the level of the tree corresponds to a single time series.
	 * @param timeSeriesId the time series ID.
	 */
	public void setTimeSeriesId(Integer timeSeriesId)
	{
		m_TimeSeriesId = timeSeriesId;
	}


	/**
	 * For time series type tree nodes that correspond to a single time series,
	 * returns the time series external key if known.
	 * @return the time series external key, if uniquely determined, at this
	 *         level in the tree.
	 */
	public String getExternalKey()
	{
		return m_ExternalKey;
	}


	/**
	 * For time series type tree nodes, sets the time series external key.  This
	 * must only be set if the level of the tree corresponds to a single time
	 * series.
	 * @param externalKey the external key.
	 */
	public void setExternalKey(String externalKey)
	{
		m_ExternalKey = externalKey;
	}


	/**
	 * Returns the list of all attributes up to this level in the metric path.
	 * @return list of attributes up this level in the tree.
	 */
	public List<Attribute> getAttributes()
	{
		return m_Attributes;
	}


	/**
	 * Sets the list of all attributes up to this level in the metric path.
	 * @param attributes list of attributes up this level in the tree.
	 */
	public void setAttributes(List<Attribute> attributes)
	{
		m_Attributes = attributes;
	}


	/**
	 * Returns whether this is a leaf node.
	 * @return <code>true</code> if it is a leaf node, <code>false</code> for
	 * 	 a branch.
	 */
	public boolean isLeaf()
	{
		return m_IsLeaf;
	}


	/**
	 * Sets the flag which indicates if this is a leaf node.
	 * @param isLeaf <code>true</code> if it is a leaf node, <code>false</code> for
	 * 	 a branch.
	 */
	public void setIsLeaf(boolean isLeaf)
	{
		m_IsLeaf = isLeaf;
	}
	
	
	/**
	 * Returns whether or not this node represents a wildcard in the metric 
	 * path e.g. 'all sources' or 'all users'.
	 * @return <code>true</code> if it is a wildcard, <code>false</code> if not.
	 */
	public boolean isWildcard()
	{
		return m_IsWildcard;
	}
	
	
	/**
	 * Sets the flag which indicates if this is a wildcard node e.g. 'all sources' or 'all users'.
	 * @param isWildcard <code>true</code> if it is a wildcard, <code>false</code> if not.
	 */
	public void setIsWildcard(boolean isWildcard)
	{
		m_IsWildcard = isWildcard;
	}
	
	
	/**
	 * Returns whether this node has any wildcard at any point along its metric 
	 * path e.g. 'all sources' or 'all users'.
	 * @return <code>true</code> if it has a wildcard, <code>false</code> if not.
	 */
	public boolean hasAnyWildcard()
	{
		return m_HasAnyWildcard;
	}
	
	
	/**
	 * Sets the flag which indicates if this node has any wildcard at any point
	 * along its metric path e.g. 'all sources' or 'all users'.
	 * @param hasAnyWildcard <code>true</code> if it has a wildcard, <code>false</code> if not.
	 */
	public void setHasAnyWildcard(boolean hasAnyWildcard)
	{
		m_HasAnyWildcard = hasAnyWildcard;
	}

	
	/**
	 * Returns a summary of the properties of this Metric tree node.
	 * @return String representation of this MetricTreeNode object.
	 */
    @Override
    public String toString()
    {
    	StringBuilder strRep = new StringBuilder();
    	
    	strRep.append("{name=").append(m_Name);
    	strRep.append(", value=").append(m_Value);
    	strRep.append(", nextLevelPrefix=").append(m_Prefix);
    	strRep.append(", partialPath=").append(m_PartialPath);
    	strRep.append(", opaqueNum=").append(m_OpaqueNum);
    	strRep.append(", opaqueStr=").append(m_OpaqueStr);
    	strRep.append(", type=").append(m_Type);
    	strRep.append(", source=").append(m_Source);
    	strRep.append(", category=").append(m_Category);
    	strRep.append(", metric=").append(m_Metric);
    	if (m_Attributes != null)
		{
			strRep.append(", attributes=");
			strRep.append(m_Attributes);
		}
    	strRep.append(", isLeaf=").append(m_IsLeaf);
    	strRep.append(", isWildcard=").append(m_IsWildcard);
    	strRep.append(", hasAnyWildcard=").append(m_HasAnyWildcard);
    	strRep.append(", timeSeriesId=").append(m_TimeSeriesId);
    	strRep.append(", externalKey=").append(m_ExternalKey);
    	
    	strRep.append('}');
		
		return strRep.toString();
    }


    /**
     * Compares this MetricTreeNode with another. Nodes are ordered alphabetically 
     * using the default locale, but leaf nodes will always appear first, followed by
     * any wildcard folder nodes (e.g. All sources, All instance).
     * @return a value less than 0 if this node should appear first, a value greater 
     * 	than 0 if the other node should appear first, or 0 if these nodes have 
     * 	identical leaf, wildcard and value properties.
     */
	@Override
	public int compareTo(MetricTreeNode other) 
	{
		// If one is a leaf and the other isn't the leaf comes first.
		if (other.isLeaf() != this.isLeaf())
		{
			if (other.isLeaf())
			{
				return 1;
			}
			else 
			{
				return -1;
			}
		}
		
		if (other.isWildcard() != this.isWildcard())
		{
			if (other.isWildcard())
			{
				return 1;
			}
			else
			{
				return -1;
			}
		}
		
		// Order lexicographically (case sensitive).
		return this.getValue().compareTo(other.getValue());
	}

}
