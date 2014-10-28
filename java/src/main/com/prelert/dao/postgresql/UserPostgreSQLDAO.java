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

package com.prelert.dao.postgresql;

import java.text.MessageFormat;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcDaoSupport;

import com.prelert.dao.UserDAO;
import com.prelert.dao.spring.UserRowMapper;
import com.prelert.data.User;


/**
 * Implementation for a PostgreSQL database of the UserDAO interface which 
 * uses calls to functions to query, add, edit and delete Prelert user data.
 * @author Pete Harverson
 */
public class UserPostgreSQLDAO extends SimpleJdbcDaoSupport implements UserDAO
{
	static Logger logger = Logger.getLogger(UserPostgreSQLDAO.class);
	

    @Override
    public int addUser(String requesterUsername, String username,
            String firstName, String lastName, String rolename,
            String passwordHash)
    {
		// Query calls a function of the form:
		// add_user(requesterName, username, firstName, lastName, role, pwdHash)
    	String query = "select * from add_user(?, ?, ?, ?, ?, ?)";
		
		String debugQuery = "select * from add_user({0}, {1}, {2}, {3}, {4}, {5})";
		debugQuery = MessageFormat.format(debugQuery, requesterUsername, 
				username, firstName, lastName, rolename, passwordHash);
		logger.debug("addUser() call: " + debugQuery);
		
		return getSimpleJdbcTemplate().queryForInt(query, requesterUsername, 
				username, firstName, lastName, rolename, passwordHash);
    }


    @Override
    public int editUser(String requesterUsername, String oldUsername,
            String newUsername, String firstName, String lastName,
            String rolename)
    {
    	// Query calls a function of the form:
		// edit_user(requesterName, oldUsername, newUsername, firstName, lastName, rolename) 
    	String query = "select * from edit_user(?, ?, ?, ?, ?, ?)";
		
		String debugQuery = "select * from edit_user({0}, {1}, {2}, {3}, {4}, {5})";
		debugQuery = MessageFormat.format(debugQuery, requesterUsername, 
				oldUsername, newUsername, firstName, lastName, rolename);
		logger.debug("editUser() call: " + debugQuery);
		
		return getSimpleJdbcTemplate().queryForInt(query, requesterUsername, 
				oldUsername, newUsername, firstName, lastName, rolename);
    }
    
    
    @Override
    public int deleteUser(String requesterUsername, String username)
    {
    	String query = "select * from delete_user(?, ?)";
		
		String debugQuery = "select * from delete_user({0}, {1})";
		debugQuery = MessageFormat.format(debugQuery, requesterUsername, username);
		logger.debug("deleteUser() call: " + debugQuery);
		
		return getSimpleJdbcTemplate().queryForInt(query, requesterUsername, username);
    }


	@Override
    public List<String> getRoles()
    {
		String query = "select * from role_list_all()";
		
		logger.debug("getRoles() query: " + query);
		
		return getSimpleJdbcTemplate().query(query, new SingleColumnRowMapper<String>());
    }
	
	
	@Override
	public List<User> getUsers()
	{
		String query = "select * from user_list_all()";
		
		logger.debug("getUsers() query: " + query);

		return getSimpleJdbcTemplate().query(query, new UserRowMapper());
	}


    @Override
    public User getUser(String username)
    {
		String query = "select * from user_list_single(?);";
		
		String debugQuery = "select * from user_list_single({0});";
		debugQuery = MessageFormat.format(debugQuery, username);
		logger.debug("getUser() query: " + debugQuery);
		
		User user = null;
		try
		{
			user = getSimpleJdbcTemplate().queryForObject(
				query, new UserRowMapper(), username);
		}
		catch (EmptyResultDataAccessException e)
		{
			logger.debug("getUser() - no user found for username: " + username);
		}
		
		return user;
    }
    
    
    @Override
    public int setPassword(String requesterUsername, String username,
            String passwordHash)
    {
    	String query = "select * from change_user_pwd(?, ?, ?)";
    	
    	String debugQuery = "select * from change_user_pwd({0}, {1})";
		debugQuery = MessageFormat.format(debugQuery, requesterUsername, username);
		logger.debug("setPassword() call: " + debugQuery);
		
		return getSimpleJdbcTemplate().queryForInt(query, 
				requesterUsername, username, passwordHash);
    }

}
