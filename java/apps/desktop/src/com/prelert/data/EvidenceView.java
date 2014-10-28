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
import java.util.ArrayList;
import java.util.List;

/**
 * An extension of the ListView class, defining a view for a list of evidence data.
 * It adds in a time frame property, defining the granularity of the data displayed
 * in the list such as day, hour, minute or second.
 * @author Pete Harverson
 */

@SuppressWarnings("serial")
public class EvidenceView extends ListView implements Serializable
{
	private TimeFrame				m_TimeFrame;
	private List<String>			m_FilterableAttributes;


	/**
	 * Creates a new Evidence View object, with a default SECOND TimeFrame.
	 */
	public EvidenceView()
	{
		m_TimeFrame = TimeFrame.SECOND;
	}


	/**
	 * Returns the time frame of the view e.g. SECOND, MINUTE, HOUR.
     * @return the time frame of the view.
     */
    public TimeFrame getTimeFrame()
    {
    	return m_TimeFrame;
    }


	/**
	 * Sets the time frame of the view e.g. SECOND, MINUTE, HOUR.
     * @param timeFrame the time frame of the view.
     * @throws IllegalArgumentException if there is no TimeFrame enum
     * with the specified name.
     */
    public void setTimeFrame(String timeFrame) throws IllegalArgumentException
    {
    	m_TimeFrame = Enum.valueOf(TimeFrame.class, timeFrame);
    }


	/**
	 * Sets the time frame of the view e.g. SECOND, MINUTE, HOUR.
     * @param timeFrame the time frame of the view.
     */
    public void setTimeFrame(TimeFrame timeFrame)
    {
    	m_TimeFrame = timeFrame;
    }

	
	/**
	 * Returns the list of attributes on which the view can be filtered,
	 * such as severity, description or source.
	 * @return  the list of filterable attribute names, or <code>null</code>
	 * if this view cannot be filtered.
	 */
    public List<String> getFilterableAttributes()
    {
    	return m_FilterableAttributes;
    }


	/**
	 * Sets the list of attributes on which the view can be filtered,
	 * such as severity, description or source.
	 * @param filterableAttribute the list of filterable attribute names, 
	 * or <code>null</code> if this view cannot be filtered.
     */
    public void setFilterableAttributes(List<String> filterableAttribute)
    {
    	m_FilterableAttributes = filterableAttribute;
    }
    
    
	/**
	 * Adds an attribute to the list of filterable attributes.
	 * @param attributeName	name of attribute to add.
	 */
	public void addFilterableAttribute(String attributeName)
	{
		if (m_FilterableAttributes == null)
		{
			m_FilterableAttributes = new ArrayList<String>();
		}
		
		m_FilterableAttributes.add(attributeName);
	}


	/**
     * Returns the name of the column in the database table mapped to this view
     * which holds the 'time' property.
     * @return the name of the time column.
     */
    public String getTimeColumnName()
    {
    	return EventRecord.getTimeColumnName(m_TimeFrame);
    }


	/**
	 * Creates a new Evidence View based on the properties of this view, and appends
	 * the specified filter to any filter which has been defined for this view.
	 * @param filterAttribute the filter attribute for the new view.
	 * @param filterValue the filter value for the new view.
	 * @return new EvidenceView object.
	 */
	public EvidenceView createCopyAndAppendFilter(String filterAttribute, String filterValue)
	{
		// Copy each of the properties of the view.
		EvidenceView newView = new EvidenceView();
		newView.setName(new String(getName()));
		newView.setDataType(getDataType());
		newView.setStyleId(new String(getStyleId()));
		newView.setTimeFrame(m_TimeFrame);
		newView.setDatabaseView(new String(getDatabaseView()));
		newView.setFilterableAttributes(getFilterableAttributes());
		newView.setContextMenuItems(getContextMenuItems());
		if (getDoubleClickTool() != null)
		{
			newView.setDoubleClickTool(new String(getDoubleClickTool()));
		}
		newView.setDefaultOrderBy(getDefaultOrderBy());

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

	   strRep.append("EvidenceView Name=");
	   strRep.append(getName());
	   
	   strRep.append(",Data Type=");
	   strRep.append(getDataType());

	   strRep.append(",TimeFrame=");
	   strRep.append(getTimeFrame());

	   strRep.append(",Style=");
	   strRep.append(getStyleId());
	   
	   strRep.append(",Shortcut=");
	   strRep.append(isDesktopShortcut());

	   strRep.append(",Database View=");
	   strRep.append(getDatabaseView());
	   
	   if (getFilterableAttributes() != null && getFilterableAttributes().size() > 0)
	   {
		   strRep.append(",Filterable attributes=");
		   strRep.append(getFilterableAttributes());
	   }

	   if (getFilterAttribute() != null)
	   {
		   strRep.append(",Filter Attribute=");
		   strRep.append(getFilterAttribute());
	
		   strRep.append(",Filter Value=");
		   strRep.append(getFilterValue());
	   }

	   if (getDefaultOrderBy() != null)
	   {
		   strRep.append(",DefaultOrderBy=");
		   strRep.append(getDefaultOrderBy());
	   }

	   strRep.append(",Context Menu=");
	   strRep.append(getContextMenuItems());

	   strRep.append(",Double Click=");
	   strRep.append(getDoubleClickTool());

	   strRep.append('}');

	   return strRep.toString();
    }



}
