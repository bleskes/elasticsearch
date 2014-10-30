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
import java.util.List;


/**
 * View subclass for a Causality View. It defines configuration properties such
 * as the right-click context menu items to display for a selected item of evidence.
 * @author Pete Harverson
 */
public class CausalityView extends View implements Serializable
{
	private boolean			m_DisplayAsEpisodes;
	private List<Tool>		m_ContextMenuItems;
	
	
	/**
	 * Returns whether the view should display causality information in the form
	 * of episodes.
     * @return <code>true</code> to display as episodes, <code>false</code> otherwise.
     */
    public boolean isDisplayAsEpisodes()
    {
    	return m_DisplayAsEpisodes;
    }


	/**
	 * Sets whether the view should display causality information in the form
	 * of episodes.
     * @param displayAsEpisodes <code>true</code> to display as episodes, 
     * <code>false</code> otherwise.
     */
    public void setDisplayAsEpisodes(boolean displayAsEpisodes)
    {
    	m_DisplayAsEpisodes = displayAsEpisodes;
    }

	
	/**
	 * Returns the list of right-click context menu items to display for a 
	 * selected item of evidence in a Causality View.
	 * @return list of right-click context menu items.
	 */
	public List<Tool> getContextMenuItems()
    {
    	return m_ContextMenuItems;
    }


	/**
	 * Sets the list of right-click context menu items to display for a 
	 * selected item of evidence in a Causality View.
	 * @param contextMenuItems list of right-click context menu items.
	 */
	public void setContextMenuItems(List<Tool> contextMenuItems)
    {
    	m_ContextMenuItems = contextMenuItems;
    }
	
	
	/**
	 * Creates a new Causality View based on the properties of this view.
	 * @param filterAttribute unused filter attribute parameter.
	 * @param filterValue unused filter value parameter.
	 * @return a new Causality View which has the same properties as this view.
	 */
	public View createCopyAndAppendFilter(String filterAttribute,
	        String filterValue)
	{
		// Copy each of the properties of the view.
		CausalityView newView = new CausalityView();
		newView.setName(new String(getName()));
		newView.setDisplayAsEpisodes(m_DisplayAsEpisodes);
		newView.setContextMenuItems(m_ContextMenuItems);
		
		return newView;
	}
	
	
	/**
	 * Returns a String summarising the properties of this View.
	 * @return a String displaying the properties of the View.
	 */
    public String toString()
    {
	   StringBuilder strRep = new StringBuilder("{");
	   
	   strRep.append("CausalityView Name=");
	   strRep.append(getName());
	   
	   strRep.append(",Episodes=");
	   strRep.append(m_DisplayAsEpisodes);
	   
	   strRep.append(",Context Menu=");
	   strRep.append(m_ContextMenuItems);
	   
	   strRep.append('}');
	   
	   return strRep.toString();
    }

}
