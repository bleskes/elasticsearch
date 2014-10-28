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

package com.prelert.client.gxt;

import com.extjs.gxt.ui.client.widget.menu.MenuItem;
import com.prelert.client.ClientUtil;
import com.prelert.data.DataSourceType;


/**
 * Extension of the Ext GWT MenuItem widget to hold a reference to the data type
 * of the view to open when the menu item is selected.
 * @author Pete Harverson
 */
public class ViewMenuItem extends MenuItem
{
	private DataSourceType	m_DataType;
	
	
	/**
	 * Constructs a menu item to open a view for the specified data type.
	 * @param dataType the data type of the view to open.
	 */
	public ViewMenuItem(DataSourceType dataType)
	{
		setDataType(dataType);
	}
	
	
	/**
	 * Returns the data type of the view to open this menu item is selected.
	 * @return the data type of the view to open.
	 */
	public DataSourceType getDataType()
    {
    	return m_DataType;
    }

	
	/**
	 * Sets the data type of the view to open this menu item is selected.
	 * @param viewTool the tool to run.
	 */
	public void setDataType(DataSourceType dataType)
    {
		m_DataType = dataType;
		setText(ClientUtil.CLIENT_CONSTANTS.showDataType(dataType.getName()));
    }
}
