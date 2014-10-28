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
 * Class encapsulating a type of data source analysed by the Prelert engine.
 * Each data source has a name and a category of data, such as fault information,
 * performance data or usage data. 
 * @author Pete Harverson
 */

public class DataSourceType implements Serializable
{
	private static final long serialVersionUID = 8014094266994391192L;
	
	private String				m_Name;
	private DataSourceCategory	m_DataCategory; 	// Notification / Time series.
	private int 				m_Count = -1;
	
	
	/**
	 * Creates a new, blank data source type.
	 */
	public DataSourceType()
	{
		
	}
	
	
	/**
	 * Creates a data source type with the given name and category.
	 * @param name the name of the data source type.
	 * @param category the category of data collected by the data source.
	 */
	public DataSourceType(String name, DataSourceCategory category)
	{
		m_Name = name;
		m_DataCategory = category;
	}

	
	/**
	 * Returns the name of the data source type.
     * @return the name of the data source type.
     */
    public String getName()
    {
    	return m_Name;
    }

    
	/**
	 * Sets the name of the data source type.
     * @param name the name of the data source type.
     */
    public void setName(String name)
    {
    	m_Name = name;
    }

    
	/**
	 * Returns the category of the data source type.
     * @return the category of data for this source type.
     */
    public DataSourceCategory getDataCategory()
    {
    	return m_DataCategory;
    }

    
	/**
	 * Sets the category of data for this source type.
     * @param dataCategory the category of data for this source type.
     */
    public void setDataCategory(DataSourceCategory dataCategory)
    {
    	m_DataCategory = dataCategory;
    }
    
    
    /**
	 * If available, returns the total count of data obtained from this source
	 * type across all servers.
	 * @return the data point count for this source type, or -1 if the count has not
	 * 		been set for this object.
	 */
	public int getCount()
	{
		return m_Count;
	}


	/**
	 * Sets the count of data obtained from this source type.
	 * @param count the data point count for this source type.
	 */
	public void setCount(int count)
	{
		m_Count = count;
	}
    
    
    /**
	 * Tests this DataSourceType object for equality with another object.
	 * @return true if the comparison object is a DataSourceType object with
	 * 	identical property values to this DataSourceType.
     */
    @Override
    public boolean equals(Object obj)
    {
        if(this == obj)
        {
            return true;
        }
        if(!(obj instanceof DataSourceType))
        {
            return false;
        }

        DataSourceType other = (DataSourceType)obj;
    	
    	// Compare names.
    	if (m_Name != null)
    	{
    		if (m_Name.equals(other.getName()) == false)
    		{
    			return false;
    		}
    	}
    	else
    	{
    		if (other.getName() != null)
    		{
    			return false;
    		}
    	}
    	
    	// Compare categories.
    	if (m_DataCategory != null)
    	{
    		if (m_DataCategory.equals(other.getDataCategory()) == false)
    		{
    			return false;
    		}
    	}
    	else
    	{
    		if (other.getDataCategory() != null)
    		{
    			return false;
    		}
    	}
    	    	
    	return true;
    }
    
    
    /**
	 * Returns a hash code for this object. This is obtained by a call
	 * to hashCode() on the String representation of this
	 * DataSourceType (i.e. name and category).
	 * @return  a hash code value for this DataSourceType object.
     */
    @Override
    public int hashCode()
    {
	    String strVal = this.toString();
	    return strVal.hashCode();
    }
	
	
	/**
	 * Returns a String representation of this data source type.
	 * @return String representation of the data source.
	 */
	public String toString()
	{
		StringBuilder strRep = new StringBuilder();
		strRep.append("{name=");
		strRep.append(m_Name);
		strRep.append(", category=");
		strRep.append(m_DataCategory);
		if (m_Count >= 0)
		{
			strRep.append(", count=");
			strRep.append(m_Count);
		}
		strRep.append('}');
		
		return strRep.toString();
	}
}

