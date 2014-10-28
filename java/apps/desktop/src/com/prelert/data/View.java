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
import java.util.ArrayList;

/**
 * Abstract base class encapsulating configuration properties of a desktop view
 * which are common to all types of View, such as the view name and style ID.
 * <p>
 * Desktop views should extend this class, adding properties which
 * are specific to that particular type of view.
 * @author Pete Harverson
 */

@SuppressWarnings("serial")
public abstract class View implements Serializable
{
	private String		m_Name;
	private String		m_DataType;
	private String		m_StyleId;
	private int			m_AutoRefreshFrequency = 60000;	// Default of 1 minute.
	private boolean		m_DesktopShortcut = false;	

	
	/**
	 * Returns the name of the View.
	 * @return name of the View.
	 */
	public String getName()
    {
    	return m_Name;
    }


	/**
	 * Sets the name of the View.
	 * @param name the name of the View.
	 */
	public void setName(String name)
    {
    	m_Name = name;
    }
	
	
	/**
	 * Returns the data type, such as 'apache_logs' or 'system_udp', which is used
	 * to identify the particular type of data being displayed.
	 * @return the data type.
	 */
	public String getDataType()
    {
    	return m_DataType;
    }


	/**
	 * Sets the data type, such as 'apache_logs' or 'system_udp', which is used
	 * to identify the particular type of data being displayed.
	 * @param dataType the data type.
	 */
	public void setDataType(String dataType)
    {
    	m_DataType = dataType;
    }


	/**
	 * Returns the style ID for the view. This ID is used as a prefix when
	 * referring to CSS styles for use by visual elements in the view 
	 * e.g images for window and shortcut icons.
	 * @return the style ID.
	 */
	public String getStyleId()
    {
    	return m_StyleId;
    }


	/**
	 * Sets the style ID for the view. This ID is used as a prefix when
	 * referring to CSS styles for use by visual elements in the view 
	 * e.g images for window and shortcut icons.
	 * @param styleId the style ID.
	 */
	public void setStyleId(String styleId)
    {
    	m_StyleId = styleId;
    }
	
	
	/**
	 * Returns the automatic refresh frequency to use for the view.
     * @return the refresh frequency, in milliseconds, which has a default value
     * 			of 1 minute.
     */
    public int getAutoRefreshFrequency()
    {
    	return m_AutoRefreshFrequency;
    }


    /**
	 * Sets the automatic refresh frequency to use for the view.
     * @param autoRefreshFrequency the refresh frequency, in milliseconds.
     */
    public void setAutoRefreshFrequency(int autoRefreshFrequency)
    {
    	m_AutoRefreshFrequency = autoRefreshFrequency;
    }


	/**
	 * Returns whether the view should have a shortcut added to the desktop.
     * @return <code>true</code> if a shortcut should be added, 
     * 			<code>false</code> otherwise.
     */
    public boolean isDesktopShortcut()
    {
    	return m_DesktopShortcut;
    }


	/**
	 * Sets whether the view should have a shortcut added to the desktop.
     * @param desktopShortcut <code>true</code> if a shortcut should be added, 
     * 			<code>false</code> otherwise.
     */
    public void setDesktopShortcut(boolean desktopShortcut)
    {
    	m_DesktopShortcut = desktopShortcut;
    }


	/**
	 * Creates a View based on the properties of this view, and appends the 
	 * specified filter to any filter which has been defined for this view.
	 * @param addFilter the filter to append to this view's filter (with an AND
	 * operator) e.g. description = ? AND DATE_FORMAT(time,'%Y-%m-%d %H:%i') = ?.
	 * @param addFilterArgs filter arguments to substitute into '?' placeholders
	 * in the supplied filter.
	 * @return new View object.
	 */
	public abstract View createCopyAndAppendFilter(
			String filterAttribute, String filterValue);


	/**
	 * Returns a String summarising the properties of this View.
	 * @return a String displaying the properties of the View.
	 */
    public String toString()
    {
	   StringBuilder strRep = new StringBuilder("{");
	   
	   strRep.append("Name=");
	   strRep.append(m_Name);
	   
	   strRep.append(",Data Type=");
	   strRep.append(getDataType());
	   
	   strRep.append(",Style=");
	   strRep.append(m_StyleId);
	   
	   strRep.append(",Refresh Frequency=");
	   strRep.append(m_AutoRefreshFrequency);
	   
	   strRep.append(",Shortcut=");
	   strRep.append(m_DesktopShortcut);
	   
	   strRep.append('}');
	   
	   return strRep.toString();
    }
}
