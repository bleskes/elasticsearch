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

/**
 * Class encapsulating the properties of a Prelert user.
 * 
 * @author Pete Harverson
 */
public class User implements Serializable
{
	private static final long serialVersionUID = 5236866503618621613L;
	
	private String m_Username;
	private String m_FirstName;
	private String m_LastName;
	private String m_RoleName;
	private String m_PasswordHash;


	/**
	 * Returns the username.
	 * @return the username.
	 */
	public String getUsername()
	{
		return m_Username;
	}


	/**
	 * Sets the username.
	 * @param username the username.
	 */
	public void setUsername(String username)
	{
		m_Username = username;
	}


	/**
	 * Returns the first name of the user.
	 * @return the user's first name.
	 */
	public String getFirstName()
	{
		return m_FirstName;
	}


	/**
	 * Sets the first name of the user.
	 * @param firstName the user's first name.
	 */
	public void setFirstName(String firstName)
	{
		m_FirstName = firstName;
	}


	/**
	 * Sets the last name of the user.
	 * @return the user's last name.
	 */
	public String getLastName()
	{
		return m_LastName;
	}


	/**
	 * Sets the last name of the user.
	 * @param lastName the user's last name.
	 */
	public void setLastName(String lastName)
	{
		m_LastName = lastName;
	}


	/**
	 * Returns the role name of the user.
	 * @return the name of the user's role.
	 */
	public String getRoleName()
	{
		return m_RoleName;
	}


	/**
	 * Sets the role name of the user.
	 * @param roleName the name of the user's role.
	 */
	public void setRoleName(String roleName)
	{
		m_RoleName = roleName;
	}
	
	
	/**
	 * Returns the encrypted password hash for the user.
	 * @return the password hash.
	 */
	public String getPasswordHash()
	{
		return m_PasswordHash;
	}
	
	
	/**
	 * Sets the encrypted password hash for the user.
	 * @param passwordHash the password hash.
	 */
	public void setPasswordHash(String passwordHash)
	{
		m_PasswordHash = passwordHash;
	}
	

    @Override
    public String toString()
    {
    	StringBuilder strRep = new StringBuilder('{');
		strRep.append("username=");
		strRep.append(m_Username);
		strRep.append(", first_name=");
		strRep.append(m_FirstName);
		strRep.append(", last_name=");
		strRep.append(m_LastName);
		strRep.append(", rolename=");
		strRep.append(m_RoleName);
		
		return strRep.toString();
    }
}
