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

package demo.app.data;

import java.io.Serializable;

import com.extjs.gxt.ui.client.data.BaseTreeModel;

/**
 * Extension of BaseTreeModel for representing sources of Prelert data in a tree.
 * @author Pete Harverson
 *
 */
public class DataSourceModel extends BaseTreeModel implements Serializable
{
	/**
	 * Creates a new, blank DataSourceModel.
	 */
	public DataSourceModel()
	{
		
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
	 * Returns the category of the data source.
     * @return the category of data for this source type.
     */
    public int getDataCategory()
    {
    	int dataCategory = -1;
    	Integer dataCategoryInt = (Integer)(get("dataCategory"));
    	if (dataCategoryInt != null)
    	{
    		dataCategory = dataCategoryInt.intValue();
    	}
    	return dataCategory;
    }

    
	/**
	 * Sets the category of data for this source type.
     * @param dataCategory the category of data for this source type.
     */
    public void setDataCategory(int dataCategory)
    {
    	set("dataCategory", dataCategory);
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
		StringBuilder strRep = new StringBuilder('{');
		strRep.append("text=");
		strRep.append(getText());
		strRep.append(", category=");
		strRep.append(getDataCategory());
		strRep.append(", count=");
		strRep.append(getCount());
		strRep.append('}');
		
		return strRep.toString();
	}

}
