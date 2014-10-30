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

public class DataSourceModel extends BaseTreeModel implements Serializable
{
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
	
	
	public String getText()
	{
		return get("text");
	}
	
	
	public void setSource(String source)
	{
		set("source", source);
	}
	
	
	public String getSource()
	{
		return get("source");
	}
	
    
	/**
	 * Returns the category of the data source type.
     * @return the category of data for this source type.
     */
    public int getDataCategory()
    {
    	return ((Integer)(get("dataCategory"))).intValue();
    }

    
	/**
	 * Sets the category of data for this source type.
     * @param dataCategory the category of data for this source type.
     */
    public void setDataCategory(int dataCategory)
    {
    	set("dataCategory", dataCategory);
    }
    
    
    public void setCount(int count)
    {
    	set("count", count);
    }
    
    
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
