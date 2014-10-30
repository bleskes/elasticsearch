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
import java.util.List;

/**
 * View subclass for a Time Series View. It defines configuration properties 
 * such as the service ID, available usage metrics, and labels for the controls
 * on a time series widget.
 * @author Pete Harverson
 */
public class TimeSeriesView extends View implements Serializable
{
	private List<String>		m_AttributeNames;
	private List<String> 		m_Metrics;
	private List<Tool>			m_ContextMenuItems;
	
	// Labels for the GUI controls.
	private String		m_SourceFieldText;
	private String		m_AllSourcesText;
	private String		m_SelectSourceText;
	private String		m_UserFieldText;
	private String		m_AllAttrValuesText;
	private String		m_SelectUserText;
	
	
	/**
	 * Creates a new Usage View object to show TIME_SERIES data.
	 */
	public TimeSeriesView()
	{
		setDataCategory(DataSourceCategory.TIME_SERIES);
	}
	
	
	/**
	 * Returns a list of the usage metrics available for this view, such as 'total'
	 * or 'pending'.
	 * @return list of the usage metrics for this view.
	 */
	public List<String> getMetrics()
	{
		return m_Metrics;
	}


	/**
	 * Sets the list of the usage metrics available for this view, such as 'total'
	 * or 'pending'.
	 * @param list of the usage metrics for this view.
	 */
	public void setMetrics(List<String> metrics)
	{
		m_Metrics = metrics;
	}
	
	
	/**
	 * Adds a usage metric to the list of metrics available for this view.
	 * @param metric usage metrics to add for this view, such as 'total' or 'pending'.
	 */
	public void addMetric(String metric)
	{
		if (m_Metrics == null)
		{
			m_Metrics = new ArrayList<String>();
		}
		
		m_Metrics.add(metric);
	}
	
	
	/**
	 * Returns the list of attribute names associated with this usage view
	 * e.g. username and app_id for p2psmon_users.
     * @return the list of attribute names for this view.
     */
    public List<String> getAttributeNames()
    {
    	return m_AttributeNames;
    }


	/**
	 * Sets the list of attribute names associated with this usage view
	 * e.g. username and app_id for p2psmon_users.
     * @param attributeNames list of attribute names for this view.
     */
    public void setAttributeNames(List<String> attributeNames)
    {
    	m_AttributeNames = attributeNames;
    }
    
    
	/**
	 * Adds an attribute name to the list of attributes available for this view.
	 * @param attributeName attribute name to add for this view, 
	 * 			such as 'username' or 'app_id'.
	 */
	public void addAttributeName(String attributeName)
	{
		if (m_AttributeNames == null)
		{
			m_AttributeNames = new ArrayList<String>();
		}
		
		m_AttributeNames.add(attributeName);
	}


	/**
	 * Returns the list of right-click context menu items to display for a 
	 * selected usage record.
	 * @return list of right-click context menu items.
	 */
	public List<Tool> getContextMenuItems()
    {
    	return m_ContextMenuItems;
    }


	/**
	 * Sets the list of right-click context menu items to display for a 
	 * selected usage record.
	 * @param contextMenuItems list of right-click context menu items.
	 */
	public void setContextMenuItems(List<Tool> contextMenuItems)
    {
    	m_ContextMenuItems = contextMenuItems;
    }
	
	
	/**
	 * Returns the text to use as the label for the Source field.
     * @return the source field text label.
     */
    public String getSourceFieldText()
    {
    	return m_SourceFieldText;
    }


	/**
	 * Sets the text to use as the label for the Source field.
     * @param sourceFieldText the source field text label.
     */
    public void setSourceFieldText(String sourceFieldText)
    {
    	m_SourceFieldText = sourceFieldText;
    }


	/**
	 * Returns the text to use as the label for the 'All Sources'
	 * option for the Source field.
     * @return the text for the 'All Sources' option.
     */
    public String getAllSourcesText()
    {
    	return m_AllSourcesText;
    }


	/**
	 * Sets the text to use as the label for the 'All Sources'
	 * option for the Source field.
     * @param allSourcesText the text for the 'All Sources' option.
     */
    public void setAllSourcesText(String allSourcesText)
    {
    	m_AllSourcesText = allSourcesText;
    }


    /**
	 * Returns the text to use as the label for instructing the user to select
	 * a Source.
     * @return label instructing the user to select a value from the Source control.
     */
    public String getSelectSourceText()
    {
    	return m_SelectSourceText;
    }


    /**
	 * Sets the text to use as the label for instructing the user to select
	 * a Source.
     * @param selectSourceText label instructing the user to select a value 
     * from the Source control.
     */
    public void setSelectSourceText(String selectSourceText)
    {
    	m_SelectSourceText = selectSourceText;
    }

    
    /**
     * Returns whether the usage view has attributes.
     * @return true if the view has attributes (e.g. User and Service Usage),
     * 			false otherwise (e.g. IPC Usage).
     */
    public boolean hasAttributes()
    {
    	return (m_AttributeNames != null && m_AttributeNames.size() > 0);
    }
    

    /**
	 * Returns the text to use as the label for the User field.
     * @return the user field text label.
     */
    public String getUserFieldText()
    {
    	return m_UserFieldText;
    }


    /**
	 * Sets the text to use as the label for the User field.
     * @param userFieldText the user field text label.
     */
    public void setUserFieldText(String userFieldText)
    {
    	m_UserFieldText = userFieldText;
    }


    /**
	 * Returns the text to use as the label for the 'All'
	 * option for the attribute value field.
     * @return the text for the 'All' option.
     */
    public String getAllAttributeValuesText()
    {
    	return m_AllAttrValuesText;
    }


    /**
	 * Sets the text to use as the label for the 'All'
	 * option for the attribute value field.
     * @param allValuesText the text for the 'All' option.
     */
    public void setAllAttributeValuesText(String allValuesText)
    {
    	m_AllAttrValuesText = allValuesText;
    }


    /**
	 * Returns the text to use as the label for instructing the user to select
	 * a user.
     * @return label instructing the user to select a value from the User control.
     */
    public String getSelectUserText()
    {
    	return m_SelectUserText;
    }


    /**
	 * Sets the text to use as the label for instructing the user to select
	 * a User.
     * @param selectUserText label instructing the user to select a value 
     * from the User control.
     */
    public void setSelectUserText(String selectUserText)
    {
    	m_SelectUserText = selectUserText;
    }
    
    
	/**
	 * Creates a new Usage View based on the properties of this view, and appends 
	 * the  specified filter to any filter which has been defined for this view.
	 * @param filterAttribute
	 * @param filterValue
	 * @return new View object.
	 */
	public TimeSeriesView createCopyAndAppendFilter(String filterAttribute, 
			String filterValue)
	
	{
		TimeSeriesView newView = new TimeSeriesView();
		
		newView.setName(new String(getName()));
		newView.setAutoRefreshFrequency(getAutoRefreshFrequency());
		newView.setDataType(new String(getDataType()));
		newView.setDataCategory(getDataCategory());
		newView.setAttributeNames(m_AttributeNames);
		newView.setMetrics(m_Metrics);
		newView.setContextMenuItems(m_ContextMenuItems);
		newView.setSourceFieldText(new String(m_SourceFieldText));
		newView.setAllSourcesText(new String(m_AllSourcesText));
		newView.setSelectSourceText(new String(m_SelectSourceText));
		
		if (hasAttributes() == true)
		{
			newView.setUserFieldText(new String(m_UserFieldText));
			newView.setAllAttributeValuesText(new String(m_AllAttrValuesText));
			newView.setSelectUserText(new String(m_SelectUserText));
		}
		
		return newView;
	}
    
	
	/**
	 * Returns a String summarising the properties of this View.
	 * @return a String displaying the properties of the View.
	 */
    public String toString()
    {
	   StringBuilder strRep = new StringBuilder("{");
	   
	   strRep.append("TimeSeriesView Name=");
	   strRep.append(getName());
	   
	   strRep.append(",Data Type=");
	   strRep.append(getDataType());
	   
	   strRep.append(",Refresh Frequency=");
	   strRep.append(getAutoRefreshFrequency());
	   
	   strRep.append(",Metrics=");
	   if (m_Metrics != null)
	   {
		   strRep.append(m_Metrics);
	   }
	   
	   if (m_AttributeNames != null)
	   {
		   strRep.append(",Attributes=");
		   strRep.append(m_AttributeNames);
	   }
	   
	   strRep.append(",Context Menu=");
	   strRep.append(m_ContextMenuItems);
	   
	   strRep.append('}');
	   
	   return strRep.toString();
    }


}
