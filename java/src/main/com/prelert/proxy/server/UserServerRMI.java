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

package com.prelert.proxy.server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

import com.prelert.dao.UserDAO;
import com.prelert.data.User;
import com.prelert.proxy.dao.RemoteUserDAO;


/**
 * Server-side implementation of the User data access object that is accessed
 * by clients via RMI.
 * @author Pete Harverson
 */
public class UserServerRMI extends UnicastRemoteObject implements RemoteUserDAO
{
	private static final long serialVersionUID = -8295804396746839769L;

	private String 		m_ServerName;
	private UserDAO		m_UserDAO;
	
	public UserServerRMI() throws RemoteException
    {
	    super();
    }


	@Override
	public int addUser(String requesterUsername, String username,
	        String firstName, String lastName, String rolename,
	        String passwordHash) throws RemoteException
	{
		return m_UserDAO.addUser(requesterUsername, username, firstName, 
				lastName, rolename, passwordHash);
	}
	
	
	@Override
	public int editUser(String requesterUsername, String oldUsername,
	        String newUsername, String firstName, String lastName,
	        String rolename) throws RemoteException
	{
		return m_UserDAO.editUser(requesterUsername, oldUsername, newUsername, 
				firstName, lastName, rolename);
	}


	@Override
	public int deleteUser(String requesterUsername, String username)
	        throws RemoteException
	{
		return m_UserDAO.deleteUser(requesterUsername, username);
	}


	@Override
	public List<String> getRoles() throws RemoteException
	{
		return m_UserDAO.getRoles();
	}
	
	
	@Override
	public List<User> getUsers() throws RemoteException
	{
		return m_UserDAO.getUsers();
	}


	@Override
	public User getUser(String username) throws RemoteException
	{
		return m_UserDAO.getUser(username);
	}


	@Override
	public int setPassword(String requesterUsername, String username,
	        String passwordHash) throws RemoteException
	{
		return m_UserDAO.setPassword(requesterUsername, username, passwordHash);
	}
	
	
	public String getServerName()
	{
		return m_ServerName;
	}
	
	
	public void setServerName(String serverName)
	{
		m_ServerName = serverName;
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

}
