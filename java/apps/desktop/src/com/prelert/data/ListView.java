/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2009     *
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
 * database view to query, filter and sort settings, and the right-click context menu.
 * @author Pete Harverson
 */

@SuppressWarnings("serial")
public class ListView extends View implements Serializable
{
	private String						m_DatabaseView;
	private List<String>				m_Columns;
	private String						m_FilterAttribute;
	private String						m_FilterValue;
	private List<SortInformation>		m_DefaultOrderBy;
	private List<Tool>					m_ContextMenuItems;
	private String						m_DoubleClickTool;
	
	
	/**
	 * Creates a new List View object.
	 */
	public ListView()
	{

	}


	/**
	 * Returns the name of the database view or table from which this List View 
	 * retrieves its rows of data.
	 * @return the name of the database view to be queried for data.
	 */
	public String getDatabaseView()
    {
    	return m_DatabaseView;
    }


	/**
	 * Sets the name of the database view or table from which this List View 
	 * retrieves its rows of data.
	 * @param databaseView the name of the database view to be queried for data.
	 */
	public void setDatabaseView(String databaseView)
    {
    	m_DatabaseView = databaseView;
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
	 * Returns the attribute name on which the view is filtered e.g. 'description'.
     * @return the attribute name (corresponding to the database table column name)
     * on which this view is filtered on, or <code>null</code> if the view is
     * not filtered.
     */
    public String getFilterAttribute()
    {
    	return m_FilterAttribute;
    }


	/**
	 * Sets the attribute name on which the view is filtered e.g. 'description'.
     * @param filterAttribute the attribute name (corresponding to the database 
     * table column name) on which this view is filtered on. Passing in <code>null</code>
     * will result in an unfiltered view.
     */
    public void setFilterAttribute(String filterAttribute)
    {
    	m_FilterAttribute = filterAttribute;
    }


	/**
	 * Returns the value of the attribute on which the view is filtered 
	 * e.g. a value of 'service has shutdown' might be returned for a 'description'
	 * attribute.
     * @return the value of the filter attribute.
     */
    public String getFilterValue()
    {
    	return m_FilterValue;
    }


	/**
	 * Sets the value of the attribute on which the view is filtered 
	 * e.g. a value of 'service has shutdown' might be set for a 'description'
	 * attribute.
     * @param filterValue the value of the filter.
     */
    public void setFilterValue(String filterValue)
    {
    	m_FilterValue = filterValue;
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
	 * Returns the list of right-click context menu items to display for a 
	 * selected list view row.
	 * @return list of right-click context menu items.
	 */
	public List<Tool> getContextMenuItems()
    {
    	return m_ContextMenuItems;
    }


	/**
	 * Sets the list of right-click context menu items to display for a 
	 * selected list view row.
	 * @param contextMenuItems list of right-click context menu items.
	 */
	public void setContextMenuItems(List<Tool> contextMenuItems)
    {
    	m_ContextMenuItems = contextMenuItems;
    }
	
	
	/**
	 * Returns the name of the tool that should be run if a user double-clicks
	 * on an item in the view.
     * @return the name of the the double-click tool to run, or <code>null</code>
     * 		if no double-click tool has been configured.
     */
    public String getDoubleClickTool()
    {
    	return m_DoubleClickTool;
    }


	/**
	 * Sets the name of the tool that should be run if a user double-clicks
	 * on an item in the view.
     * @param doubleClickTool the name of the the double-click tool to run, 
     * or <code>null</code> if no tool should run on double-click.
     */
    public void setDoubleClickTool(String doubleClickTool)
    {
    	m_DoubleClickTool = doubleClickTool;
    }
	
	
	/**
	 * Creates a new List View based on the properties of this view, and appends
	 * the specified filter to any filter which has been defined for this view.
	 * @param addFilter the filter to append to this view's filter (with an AND
	 * operator) e.g. description = ? AND DATE_FORMAT(time,'%Y-%m-%d %H:%i') = ?.
	 * @param addFilterArgs filter arguments to substitute into '?' placeholders
	 * in the supplied filter.
	 * @return new ListView object.
	 */
	public ListView createCopyAndAppendFilter(String filterAttribute, String filterValue)
	{
		// Copy each of the properties of the view.
		ListView newView = new ListView();
		newView.setName(new String(getName()));
		newView.setDataType(getDataType());
		newView.setStyleId(new String(getStyleId()));
		newView.setDatabaseView(new String(m_DatabaseView));
		newView.setContextMenuItems(m_ContextMenuItems);
		if (getDoubleClickTool() != null)
		{
			newView.setDoubleClickTool(new String(getDoubleClickTool()));
		}
		newView.setDefaultOrderBy(m_DefaultOrderBy);
		
		// Set the filter for the new View.
		newView.setFilterAttribute(filterAttribute);
		newView.setFilterValue(filterValue);
		
		return newView;
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
	   
	   strRep.append(",Style=");
	   strRep.append(getStyleId());
	   
	   strRep.append(",Database View=");
	   strRep.append(m_DatabaseView);
	   
	   strRep.append(",Filter Attribute=");
	   if (m_FilterAttribute != null)
	   {
		   strRep.append(m_FilterAttribute);
		   
		   strRep.append(",Filter Value=");
		   strRep.append(m_FilterValue);
	   }
	   
	   strRep.append(",DefaultOrderBy=");
	   strRep.append(m_DefaultOrderBy);
	   
	   strRep.append(",Context Menu=");
	   strRep.append(m_ContextMenuItems);
	   
	   strRep.append(",Double Click=");
	   strRep.append(m_DoubleClickTool);
	   
	   strRep.append('}');
	   
	   return strRep.toString();
    }
}
