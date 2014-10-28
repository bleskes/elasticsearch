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
import java.util.ArrayList;
import java.util.List;

import com.prelert.data.Evidence;

/**
 * An extension of the ListView class, defining a view for a list of evidence data.
 * It adds in a time frame property, defining the granularity of the data displayed
 * in the list such as day, hour, minute or second, and a list of attribute names
 * on which the list may be filtered.
 * @author Pete Harverson
 */
public class EvidenceView extends ListView implements Serializable
{
	private static final long serialVersionUID = -5322655085871206055L;
	
	private TimeFrame				m_TimeFrame;
	private List<String>			m_FilterableAttributes;


	/**
	 * Creates a new Evidence View object, with a default SECOND TimeFrame
	 * and a NOTIFICATION data category.
	 */
	public EvidenceView()
	{
		m_TimeFrame = TimeFrame.SECOND;
		setDataCategory(DataSourceCategory.NOTIFICATION);
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
     * Returns the name of the column in the database table mapped to this view
     * which holds the 'time' property.
     * @return the name of the time column.
     */
    public String getTimeColumnName()
    {
    	return Evidence.getTimeColumnName(m_TimeFrame);
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
    public void setFilterableAttributes(List<String> filterableAttributes)
    {
    	m_FilterableAttributes = filterableAttributes;
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
	   
	   strRep.append(",Data Category=");
	   strRep.append(getDataCategory());

	   strRep.append(",TimeFrame=");
	   strRep.append(getTimeFrame());

	   if (getFilterableAttributes() != null && getFilterableAttributes().size() > 0)
	   {
		   strRep.append(",Filterable attributes=");
		   strRep.append(getFilterableAttributes());
	   }
	   
	   if (getDefaultOrderBy() != null)
	   {
		   strRep.append(",DefaultOrderBy=");
		   strRep.append(getDefaultOrderBy());
	   }

	   strRep.append('}');

	   return strRep.toString();
    }

}
