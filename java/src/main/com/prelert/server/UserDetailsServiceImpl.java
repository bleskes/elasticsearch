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

package com.prelert.server;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.prelert.dao.UserDAO;
import com.prelert.data.User;


/**
 * Implementation of the Spring Security <tt>UserDetailsService</tt> which retrieves 
 * the user details (username, password and authorities) from a store of Prelert
 * users via a {@link com.prelert.dao.UserDAO}.
 * 
 * @author Pete Harverson
 */
public class UserDetailsServiceImpl implements UserDetailsService
{
	static Logger logger = Logger.getLogger(UserDetailsServiceImpl.class);
	
	private UserDAO m_UserDAO;
	private String	m_RolePrefix = "ROLE_";
	

	@Override
	public UserDetails loadUserByUsername(String username)
	        throws UsernameNotFoundException, DataAccessException
	{
    	logger.debug("loadUserByUsername('" + username + "')");
    	
    	User user = m_UserDAO.getUser(username);
    	
		if (user == null)
		{
			logger.debug("loadUserByUsername(), no user with username '" + username + "'");

			throw new UsernameNotFoundException(
					"UserDetailsServiceImpl - no user found with username '" + username + "'");
		}

		// Make sure the user has at least one role.
		String role = user.getRoleName();
		if ( (role == null) || (role.isEmpty()) )
		{
			logger.debug("User '" + username + "' has no role and will be treated as 'not found'");

			throw new UsernameNotFoundException(
					"UserDetailsServiceImpl - user '" + username + "' has not been granted a role");
		}
				

		// Wrap the Prelert role name into a Spring Security GrantedAuthority.
		GrantedAuthorityImpl authority = new GrantedAuthorityImpl(getRolePrefix() + role);
		List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
		authorities.add(authority);


		// Create new Spring Security User object with:
		// new User(java.lang.String username, java.lang.String password, boolean enabled, 
		// boolean accountNonExpired, boolean credentialsNonExpired, boolean accountNonLocked, 
		// java.util.Collection<? extends GrantedAuthority> authorities) 
		org.springframework.security.core.userdetails.User userDetails = 
			new org.springframework.security.core.userdetails.User(username, 
					user.getPasswordHash(), true, true, true, true, authorities);
		
		return userDetails;
	}
	
	
	/**
	 * Allows a default role prefix to be specified. If this is set to a
	 * non-empty value, then it is automatically prepended to any roles read in
	 * from the store of Prelert users. This may for example be used to add the 
	 * <tt>ROLE_</tt> prefix expected to exist in role names (by default) by the 
	 * Spring Security authentication provider, in the case that the prefix is not
	 * already present in the database of users.
	 * 
	 * @param rolePrefix the new prefix
	 */
	public void setRolePrefix(String rolePrefix)
	{
		m_RolePrefix = rolePrefix;
	}


	/**
	 * Returns the role prefix.
	 * @return the role prefix, which is '<tt>ROLE_</tt>' by default.
	 */
	protected String getRolePrefix()
	{
		return m_RolePrefix;
	}
	
	
	/**
	 * Returns the data access object being used to load user data.
     * @return the data access object for user data.
     */
    public UserDAO getUserDAO()
    {
    	return m_UserDAO;
    }

    
	/**
	 * Sets the data access object to be used to load user data.
     * @param userDAO the the data access object for user data.
     */
    public void setUserDAO(UserDAO userDAO)
    {
    	m_UserDAO = userDAO;
    }

}
