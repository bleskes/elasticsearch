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
 * Abstract class representing a tool for running an action against data in
 * a Prelert View. 
 * <p>
 * Classes for tools such as opening Views should extend this class and add in
 * properties specific for the action to be run by the tool.
 * @author Pete Harverson
 */
public abstract class Tool implements Serializable
{
	private String m_Name;
	
	/**
	 * Returns the name of the tool. This can be used as a label for the tool, for
	 * example as a label for a menu item used for running the tool.
	 * @return the name of the tool.
	 */
	public String getName()
	{
		return m_Name;
	}


	/**
	 * Sets the name of the tool. This can be used as a label for the tool, for
	 * example as a label for a menu item used for running the tool.
	 * @param name the name of the tool.
	 */
	public void setName(String name)
	{
		m_Name = name;
	}
}
