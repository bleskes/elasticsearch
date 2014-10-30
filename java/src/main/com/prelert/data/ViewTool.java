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
 * Class encapsulating the configuration properties of a generic tool for
 * launching a Prelert Desktop View. 
 * <p>
 * Subclasses for opening specific types of View, such as List Views or Usage
 * Views, add extra properties specific to that particular type of View.
 * @author Pete Harverson
 */

public class ViewTool extends Tool implements Serializable
{
	private static final long serialVersionUID = 9071919984953497675L;
	
	private String m_ViewToOpen;


	/**
	 * Returns the name of the view to open when running this tool.
	 * @return the name of the view to open.
	 */
	public String getViewToOpen()
	{
		return m_ViewToOpen;
	}


	/**
	 * Sets the name of the view to open when running this tool.
	 * @param viewToOpen the name of the view to open.
	 */
	public void setViewToOpen(String viewToOpen)
	{
		m_ViewToOpen = viewToOpen;
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
		strRep.append(m_ViewToOpen);

		strRep.append('}');

		return strRep.toString();
    }
}
