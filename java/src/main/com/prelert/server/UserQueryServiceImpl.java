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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import com.extjs.gxt.ui.client.data.BasePagingLoadResult;
import com.extjs.gxt.ui.client.data.PagingLoadConfig;
import com.extjs.gxt.ui.client.data.PagingLoadResult;
import com.extjs.gxt.ui.client.util.DefaultComparator;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import com.prelert.dao.UserDAO;
import com.prelert.data.User;
import com.prelert.data.gxt.UserModel;
import com.prelert.service.UserQueryService;


/**
 * Server-side implementation of the service for retrieving data on Prelert users.
 * @author Pete Harverson
 */

@SuppressWarnings("serial")
public class UserQueryServiceImpl extends RemoteServiceServlet implements UserQueryService
{
	
	static Logger logger = Logger.getLogger(UserQueryServiceImpl.class);
	
	private UserDAO m_UserDAO;
	private String	m_DigestAlgorithm = "SHA-1";
	

	@Override
    public int addUser(UserModel user)
    {
    	int statusCode = STATUS_SUCCESS;
    	
    	try
    	{
    		String requesterUsername = getLoggedInUsername();
    		String passwordHash = getEncryptedPassword(user.getPassword());
    	
    		statusCode = m_UserDAO.addUser(requesterUsername, 
    				user.getUsername(), user.getFirstName(), user.getLastName(), 
    				user.getRoleName(), passwordHash);
    	}
    	catch (NoSuchAlgorithmException nsae)
    	{
    		logger.error("addUser(): unsupported digest algorithm (" + m_DigestAlgorithm + 
    				"): ", nsae);
    		statusCode = STATUS_DIGEST_ALGORITHM_UNSUPPORTED;
    	}
    	catch (Exception e)
    	{
    		logger.error("addUser(): error adding user", e);
    		statusCode = STATUS_FAILURE_UNKNOWN;
    	}
    	
    	if (statusCode != STATUS_SUCCESS)
    	{
    		logger.error("addUser(): returning failure status code (" + statusCode + 
    				") for user: " + user);
    	}
    	
    	return statusCode;
    }
    

    @Override
    public int editUser(String oldUsername, UserModel newUserData)
    {
    	int statusCode = STATUS_SUCCESS;
    	
    	try
    	{
    		String requesterUsername = getLoggedInUsername();
    		statusCode = m_UserDAO.editUser(requesterUsername, oldUsername, 
    				newUserData.getUsername(), newUserData.getFirstName(), 
    				newUserData.getLastName(), newUserData.getRoleName());
    	}
    	catch (Exception e)
    	{
    		logger.error("editUser(): error editing user", e);
    		statusCode = STATUS_FAILURE_UNKNOWN;
    	}
    	
    	if (statusCode != STATUS_SUCCESS)
    	{
    		logger.error("editUser(): returning failure status code (" + statusCode + 
    				") for user: " + newUserData);
    	}
    	
    	return statusCode;
    }
    

    @Override
    public int deleteUser(String username)
    {
    	int statusCode = STATUS_SUCCESS;
    		
    	try
    	{
    		String requesterUsername = getLoggedInUsername();
    		statusCode = m_UserDAO.deleteUser(requesterUsername, username);
    	}
    	catch (Exception e)
    	{
    		logger.error("deleteUser(): error deleting user", e);
    		statusCode = STATUS_FAILURE_UNKNOWN;
    	}
    	
    	if (statusCode != STATUS_SUCCESS)
    	{
    		logger.error("deleteUser(): returning failure status code (" + statusCode + 
    				") for user: " + username);
    	}
    	
    	return statusCode;
    }
    
    
    @Override
    public UserModel getLoggedInUser()
    {
    	String username = getLoggedInUsername();
    	
    	User user = m_UserDAO.getUser(username);
    	UserModel userModel = createUserModel(user);
    	
    	return userModel;
    }
    
    
    /**
     * Returns the username of currently logged in user.
     * @return the username of the currently authenticated user.
     */
    protected String getLoggedInUsername()
    {
    	// Get the name of the currently authenticated user from the Spring Security context.	
		Object principal = 
			SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		
		String username = null;
		if (principal instanceof UserDetails)
		{
			username = ((UserDetails) principal).getUsername();
		}
		else
		{
			username = principal.toString();
		}
		
		return username;
    }


	@Override
    public List<String> getRoles()
    {
	    return m_UserDAO.getRoles();
    }
	
	
    @Override
    public PagingLoadResult<UserModel> getUsers(PagingLoadConfig loadConfig)
    {  	
    	return getUsers(loadConfig, null);
    }
    

    @Override
    public PagingLoadResult<UserModel> getUsers(PagingLoadConfig loadConfig,
            String username)
    {
    	List<User> users = m_UserDAO.getUsers();
    	
    	List<UserModel> fullList = new ArrayList<UserModel>();
		if (users != null)
		{
			for (User user : users)
			{
				fullList.add(createUserModel(user));
			}
		}
    	
    	// Sort the full list of users if necessary.
		if (loadConfig.getSortInfo().getSortField() != null)
		{
			final String sortField = loadConfig.getSortInfo().getSortField();
			if (sortField != null)
			{
				Collections.sort(fullList, loadConfig.getSortInfo().getSortDir().comparator(
						new Comparator<UserModel>()
						{
							public int compare(UserModel o1, UserModel o2)
							{
								Object v1 = o1.get(sortField);
								Object v2 = o2.get(sortField);

						        return DefaultComparator.INSTANCE.compare(v1, v2);
					        }
				        }));
			}

		}
		
		// Return sublist containing specified user if one is set.
		int offset = loadConfig.getOffset();
		int pageSize = loadConfig.getLimit();
		if (username != null)
		{
			// Find the offset for the page containing the specified user.
			for (int i = 0; i < fullList.size(); i++)
			{
				if (fullList.get(i).getUsername().equals(username))
				{
					offset = i;
					
					if (pageSize > 0)
					{
						offset = offset - (offset % pageSize);
					}
					break;
				}
			}
		}
		
		List<UserModel> sublist = new ArrayList<UserModel>();
		int limit = fullList.size();
		if (pageSize > 0)
		{
			limit = Math.min(offset + pageSize, limit);
		}
		for (int i = offset; i < limit; i++)
		{
			sublist.add(fullList.get(i));
		}
    	
	    return new BasePagingLoadResult<UserModel>(sublist,  offset, fullList.size());
    }


	@Override
    public int setPassword(String username, String password)
    {
    	int statusCode = STATUS_SUCCESS;
    	
    	try
    	{
    		String requesterUsername = getLoggedInUsername();
    		String passwordHash = getEncryptedPassword(password);
    		statusCode = m_UserDAO.setPassword(requesterUsername, username, passwordHash);
    	}
    	catch (NoSuchAlgorithmException nsae)
    	{
    		logger.error("setPassword(): unsupported digest algorithm (" + m_DigestAlgorithm + 
    				"): ", nsae);
    		statusCode = STATUS_DIGEST_ALGORITHM_UNSUPPORTED;
    	}
    	catch (Exception e)
    	{
    		logger.error("setPassword(): error changing password for user " + username, e);
    		statusCode = STATUS_FAILURE_UNKNOWN;
    	}
    	
    	if (statusCode != STATUS_SUCCESS)
    	{
    		logger.error("setPassword(): returning failure status code (" + statusCode + 
    				") for user: " + username);
    	}
    	
    	return statusCode;
    }
	
	
    @Override
    public int setPassword(String username, String currentPassword,
            String newPassword)
    {
    	int statusCode = STATUS_SUCCESS;
    	
	    // Verify that the current password.
    	User user = m_UserDAO.getUser(username);
    	try
    	{
    		String currentPasswordHash = getEncryptedPassword(currentPassword);
    		
    		if (currentPasswordHash.equals(user.getPasswordHash()))
    		{
	    		statusCode = setPassword(username, newPassword);
    		}
    		else
    		{
    			statusCode = STATUS_INCORRECT_PASSWORD;
    		}
    	}
    	catch (NoSuchAlgorithmException nsae)
    	{
    		logger.error("setPassword(): unsupported digest algorithm (" + m_DigestAlgorithm + 
    				"): ", nsae);
    		statusCode = STATUS_DIGEST_ALGORITHM_UNSUPPORTED;
    	}
    	
	    return statusCode;
    }


	/**
	 * Returns the data access object being used to query, add, edit and delete
	 * user data.
     * @return the data access object for user data.
     */
    public UserDAO getUserDAO()
    {
    	return m_UserDAO;
    }

    
	/**
	 * Sets the data access object to be used to query, add, edit and delete
	 * user data.
     * @param userDAO the the data access object for user data.
     */
    public void setUserDAO(UserDAO userDAO)
    {
    	m_UserDAO = userDAO;
    }
    
    
    /**
     * Returns the name of the message digest algorithm to be used when encrypting
     * a plain text password before storing in the Prelert database.
     * @return the name of the message digest algorithm.
     */
    public String getDigestAlgorithm()
    {
    	return m_DigestAlgorithm;
    }
    
    
    /**
     * Sets the name of the message digest algorithm to be used when encrypting
     * a plain text password before storing in the Prelert database.
     * @param algorithm the name of the message digest algorithm.
     */
    public void setDigestAlgorithm(String algorithm)
    {
    	m_DigestAlgorithm = algorithm;
    }

    
    /**
	 * Converts a User to a GXT UserModel object.
	 * @return UserModel object.
	 */
	protected UserModel createUserModel(User user)
	{
		UserModel model = new UserModel();
		model.setUsername(user.getUsername());
		model.setFirstName(user.getFirstName());
		model.setLastName(user.getLastName());
		model.setRoleName(user.getRoleName());
		
		return model;
	}
	
	
	/**
	 * Encrypts a plain text password using the message digest algorithm that has
	 * been set for this object.
	 * @param password plain text password to encrypt.
	 * @return encrypted password, in lower case hex.
	 * @throws NoSuchAlgorithmException if the message digest algorithm set for 
	 * 		this object is not supported by any of the default security providers.
	 */
	protected String getEncryptedPassword(String password) 
		throws NoSuchAlgorithmException
	{
		// Compute the digest for the plain text password.
		MessageDigest sha = MessageDigest.getInstance(m_DigestAlgorithm);
		byte[] hash = sha.digest(password.getBytes());
		
		// Convert to lower case hex String.
		StringBuffer sb = new StringBuffer(hash.length * 2);
		for (int i = 0; i < hash.length; i++)
		{
			int v = hash[i] & 0xff;
			if (v < 16)
			{
				sb.append('0');
			}
			sb.append(Integer.toHexString(v));
		}
		return sb.toString().toLowerCase();
	}

}
