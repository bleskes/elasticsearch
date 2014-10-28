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

package com.prelert.data.gxt;

import com.extjs.gxt.ui.client.data.BaseModelData;


/**
 * Extension of the GXT BaseModelData class for user data.
 * @author Pete Harverson
 */
@SuppressWarnings("serial")
public class UserModel extends BaseModelData
{
	/** The name of the administrator role. */
	public static final String ROLE_NAME_ADMINISTRATOR = "administrator";
	
	/**
	 * Returns the username.
	 * @return the username.
	 */
	public String getUsername()
	{
		return get("username");
	}


	/**
	 * Sets the username.
	 * @param username the username.
	 */
	public void setUsername(String username)
	{
		set("username", username);
	}


	/**
	 * Returns the first name of the user.
	 * @return the user's first name.
	 */
	public String getFirstName()
	{
		return get("firstName");
	}


	/**
	 * Sets the first name of the user.
	 * @param firstName the user's first name.
	 */
	public void setFirstName(String firstName)
	{
		set("firstName", firstName);
	}


	/**
	 * Sets the last name of the user.
	 * @return the user's last name.
	 */
	public String getLastName()
	{
		return get("lastName");
	}


	/**
	 * Sets the last name of the user.
	 * @param lastName the user's last name.
	 */
	public void setLastName(String lastName)
	{
		set("lastName", lastName);
	}


	/**
	 * Returns the rolename of the user.
	 * @return the name of the user's role.
	 */
	public String getRoleName()
	{
		return get("roleName");
	}


	/**
	 * Sets the rolename of the user.
	 * @param roleName the name of the user's role.
	 */
	public void setRoleName(String roleName)
	{
		set("roleName", roleName);
	}
	
	
	/**
	 * Returns the password for the user.
	 * @return the plain text password. 
	 */
	public String getPassword()
	{
		return get("password");
	}
	
	
	/**
	 * Sets the password for the user.
	 * @param password the plain text password.
	 */
	public void setPassword(String password)
	{
		set("password", password);
	}
	
	
	/**
	 * Returns a summary of the user data.
	 * @return String representation of this user.
	 */
	public String toString()
	{
		return getProperties().toString();
	}
}
