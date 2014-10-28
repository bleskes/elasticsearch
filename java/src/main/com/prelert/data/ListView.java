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
 * View subclass for a List View. It defines configuration properties such as the
 * list of display columns and sort settings.
 * @author Pete Harverson
 */
public class ListView extends View implements Serializable
{
	private static final long serialVersionUID = -3088916222548102787L;
	
	private List<String>				m_Columns;
	private List<SortInformation>		m_DefaultOrderBy;
	
	
	/**
	 * Creates a new List View object.
	 */
	public ListView()
	{

	}


	/**
	 * Returns the list of columns that are available for display in this list view.
     * @return the complete list of columns that can be displayed.
     */
    public List<String> getColumns()
    {
    	return m_Columns;
    }


	/**
	 * Sets the list of columns that are available for display in this list view.
     * @param columns the complete list of columns that can be displayed.
     */
    public void setColumns(List<String> columns)
    {
    	m_Columns = columns;
    }
    
    
	/**
	 * Returns the default sort to be used for this view if no other sort information
	 * has been supplied (e.g. by clicking on a column in a grid view).
	 * @return a list of the default SortInformation to use for the view.
	 */
	public List<SortInformation> getDefaultOrderBy()
    {
    	return m_DefaultOrderBy;
    }


	/**
	 * Sets the default sort to be used for this view if no other sort information
	 * has been supplied (e.g. by clicking on a column in a grid view).
	 * @param defaultSort a list of the default SortInformation to use for this view.
	 */
	public void setDefaultOrderBy(List<SortInformation> defaultSort)
    {
    	m_DefaultOrderBy = defaultSort;
    }


	/**
	 * Returns a String summarising the properties of this View.
	 * @return a String displaying the properties of the View.
	 */
    public String toString()
    {
	   StringBuilder strRep = new StringBuilder("{");
	   
	   strRep.append("ListView Name=");
	   strRep.append(getName());
	   
	   strRep.append(",Data Type=");
	   strRep.append(getDataType());
	   
	   strRep.append(",Data Category=");
	   strRep.append(getDataCategory());
	   
	   strRep.append(",DefaultOrderBy=");
	   strRep.append(m_DefaultOrderBy);
	   
	   strRep.append('}');
	   
	   return strRep.toString();
    }
}
