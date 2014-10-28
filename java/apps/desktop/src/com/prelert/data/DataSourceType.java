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
	private String				m_Name;
	private DataSourceCategory	m_DataCategory; 	// Notification / Time series.
	
	
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
		strRep.append('}');
		
		return strRep.toString();
	}
}
