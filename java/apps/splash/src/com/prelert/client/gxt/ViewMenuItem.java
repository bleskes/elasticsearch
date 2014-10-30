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

package com.prelert.client.gxt;

import com.extjs.gxt.ui.client.widget.menu.MenuItem;
import com.prelert.client.ClientUtil;
import com.prelert.data.CausalityViewTool;
import com.prelert.data.ListViewTool;
import com.prelert.data.TimeSeriesViewTool;
import com.prelert.data.Tool;
import com.prelert.data.ViewTool;


/**
 * Extension of the Ext GWT MenuItem widget to hold a reference to a tool to be
 * run when the menu item is selected.
 * @author Pete Harverson
 */
public class ViewMenuItem extends MenuItem
{
	private Tool	m_Tool;
	
	
	/**
	 * Creates a new, empty View MenuItem.
	 */
	public ViewMenuItem()
	{
		
	}
	
	
	/**
	 * Creates a new menu item to run the specified tool when selected.
	 * @param tool tool to run when the menu item is selected.
	 */
	public ViewMenuItem(Tool tool)
	{
		m_Tool = tool;
		if(m_Tool != null)
		{
			if (m_Tool.getClass() == CausalityViewTool.class)
			{
				setText(ClientUtil.CLIENT_CONSTANTS.showProbableCause());
			}
			else if (m_Tool.getClass() == ListViewTool.class ||
					m_Tool.getClass() == TimeSeriesViewTool.class)
			{
				ViewTool viewTool = (ViewTool)m_Tool;
				setText(ClientUtil.CLIENT_CONSTANTS.show() + " " + 
						viewTool.getViewToOpen());
			}
			else
			{
				setText(m_Tool.getName());
			}
		}
	}
	
	
	/**
	 * Returns the tool to be run when this menu item is selected.
	 * @return the tool to run.
	 */
	public Tool getTool()
    {
    	return m_Tool;
    }

	
	/**
	 * Sets the tool to be run when this menu item is selected.
	 * @param viewTool the tool to run.
	 */
	public void setTool(Tool viewTool)
    {
		m_Tool = viewTool;
    }


}
