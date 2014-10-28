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
 * Extension of the generic ViewTool class for launching a List View.
 * It adds properties which are specific to List Views, such as the name of the
 * attribute on which the List View should be filtered.
 * @author Pete Harverson
 */
public class ListViewTool extends ViewTool implements Serializable
{
	private String m_FilterAttribute;
	private String m_FilterArg;
	
	
	/**
	 * Returns the attribute name on which the new view is to be filtered 
	 * e.g. 'description'.
     * @return the attribute name (corresponding to the database table column name)
     * on which the view is filtered on, or <code>null</code> if the view should
     * be unfiltered.
     */
	public String getFilterAttribute()
	{
		return m_FilterAttribute;
	}


	/**
	 * Sets the attribute name on which the new view is to be filtered 
	 * e.g. 'description'.
     * @param filterAttribute the attribute name (corresponding to the database 
     * table column name) on which the new view is to be filtered. Passing in 
     * <code>null</code> will result in an unfiltered view.
     */
	public void setFilterAttribute(String queryFilter)
	{
		m_FilterAttribute = queryFilter;
	}


	/**
	 * Returns the name of the attribute on which the tool is run whose value 
	 * should be supplied as the filter argument for the new view.
	 * @return the name of the attribute whose value should be passed as the
	 * new List View filter value.
	 */
	public String getFilterArg()
	{
		return m_FilterArg;
	}


	/**
	 * Sets the name of the attribute on which the tool is run whose value 
	 * should be supplied as the filter argument for the new view.
	 * @param filterArg name of the attribute whose value should be passed as the
	 * new List View filter value.
	 */
	public void setFilterArg(String filterArg)
	{
		m_FilterArg = filterArg;
	}
	
	
	/**
	 * Returns a String summarising the properties of this tool.
	 * @return a String displaying the properties of the tool.
	 */
    public String toString()
    {
		StringBuilder strRep = new StringBuilder("{");
		   
		strRep.append("Name=");
		strRep.append(getName());

		strRep.append(",Open View=");
		strRep.append(getViewToOpen());

		if (m_FilterAttribute != null)
		{
			strRep.append(",Filter Attribute=");
			strRep.append(m_FilterAttribute);
	
			strRep.append(",Filter Arg=");
			strRep.append(m_FilterArg);
		}

		strRep.append('}');

		return strRep.toString();
    }
}
