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

package com.prelert.dao.proxy;

import java.rmi.AccessException;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.List;

import org.apache.log4j.Logger;

import com.prelert.dao.UserDAO;
import com.prelert.data.User;
import com.prelert.proxy.dao.RemoteObjectFactoryDAO;
import com.prelert.proxy.dao.RemoteUserDAO;


/**
 * Implementation for RMI (Remote Method Invocation) of the UserDAO 
 * interface. The class makes calls through RMI to a remote server which 
 * returns user data.
 * 
 * @author Pete Harverson
 */
public class UserProxyDAO extends RemoteProxyDAO implements UserDAO
{
	static Logger s_Logger = Logger.getLogger(UserProxyDAO.class);

	private RemoteUserDAO 	m_RemoteDAO;


	/**
	 * @throws ProxyDataAccessException if an error occurs adding the user
	 * remotely through the Proxy.
	 */
	@Override
	public int addUser(String requesterUsername, String username,
	        String firstName, String lastName, String rolename,
	        String passwordHash)
	{
		boolean alreadyReset = false;
		while (true)
		{
			try
        	{
				return getRemoteDAO().addUser(requesterUsername, username, firstName,
	        			lastName, rolename, passwordHash);
        	}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteUserDAO";
	        		s_Logger.error("addUser(): " + errMsg, ce);
	        		throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
        	catch (Exception e)
        	{
        		String errMsg = "Error adding user '" + username + "' through RemoteUserDAO";
	        	s_Logger.error("addUser(): " + errMsg, e);
	        	throw new ProxyDataAccessException(errMsg, e);
        	}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs editing the user
	 * remotely through the Proxy.
	 */
	@Override
	public int editUser(String requesterUsername, String oldUsername,
	        String newUsername, String firstName, String lastName,
	        String rolename)
	{
		s_Logger.debug("editUser RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
        	{
	        	return getRemoteDAO().editUser(requesterUsername, oldUsername,
	        			newUsername, firstName, lastName, rolename);
        	}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteUserDAO";
	        		s_Logger.error("editUser(): " + errMsg, ce);
	        		throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
        	catch (Exception e)
        	{
        		String errMsg = "Error editing user '" + oldUsername + "' through RemoteUserDAO";
	        	s_Logger.error("editUser(): " + errMsg, e);
	        	throw new ProxyDataAccessException(errMsg, e);
        	}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs deleting the user
	 * remotely through the Proxy.
	 */
	@Override
	public int deleteUser(String requesterUsername, String username)
	{
		s_Logger.debug("deleteUser RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
        	{
	        	return getRemoteDAO().deleteUser(requesterUsername, username);
        	}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteUserDAO";
	        		s_Logger.error("deleteUser(): " + errMsg, ce);
	        		throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
        	catch (Exception e)
        	{
        		String errMsg = "Error deleting user '" + username + "' through RemoteUserDAO";
	        	s_Logger.error("deleteUser(): " + errMsg, e);
	        	throw new ProxyDataAccessException(errMsg, e);
        	}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs obtaining the list of
	 * 		roles remotely through the Proxy.
	 */
	@Override
	public List<String> getRoles()
	{
		s_Logger.debug("getRoles RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
        	{
	        	return getRemoteDAO().getRoles();
        	}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteUserDAO";
	        		s_Logger.error("getRoles(): " + errMsg, ce);
	        		throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
        	catch (Exception e)
        	{
        		String errMsg = "Error getting list of roles through RemoteUserDAO";
	        	s_Logger.error("getRoles(): " + errMsg, e);
	        	throw new ProxyDataAccessException(errMsg, e);
        	}
		}
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs obtaining the list of
	 * 		users remotely through the Proxy.
	 */
	@Override
	public List<User> getUsers()
	{
		s_Logger.debug("getUsers RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
        	{
	        	return getRemoteDAO().getUsers();
        	}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteUserDAO";
	        		s_Logger.error("getUsers(): " + errMsg, ce);
	        		throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
        	catch (Exception e)
        	{
        		String errMsg = "Error getting list of users through RemoteUserDAO";
	        	s_Logger.error("getUsers(): " + errMsg, e);
	        	throw new ProxyDataAccessException(errMsg, e);
        	}
        }
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs obtaining the details
	 * 		of the user remotely through the Proxy.
	 */
	@Override
	public User getUser(String username)
	{
		s_Logger.debug("getUser RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
        	{
	        	return getRemoteDAO().getUser(username);
        	}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteUserDAO";
	        		s_Logger.error("getUser(): " + errMsg, ce);
	        		throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
        	catch (Exception e)
        	{
        		String errMsg = "Error getting details of user '" + username + "' through RemoteUserDAO";
	        	s_Logger.error("getUser(): " + errMsg, e);
	        	throw new ProxyDataAccessException(errMsg, e);
        	}
        }
	}


	/**
	 * @throws ProxyDataAccessException if an error occurs setting the password
	 * 		of the user remotely through the Proxy.
	 */
	@Override
	public int setPassword(String requesterUsername, String username,
	        String passwordHash)
	{
		s_Logger.debug("setPassword RMI call.");

		boolean alreadyReset = false;
		while (true)
		{
			try
        	{
	        	return getRemoteDAO().setPassword(requesterUsername,
	        			username, passwordHash);
        	}
			catch (ConnectException ce)
			{
				resetRemoteDAO();

				if (alreadyReset)
				{
					String errMsg = "Error connecting to the RemoteUserDAO";
	        		s_Logger.error("setPassword(): " + errMsg, ce);
	        		throw new ProxyDataAccessException(errMsg, ce);
				}

				alreadyReset = true;
			}
        	catch (Exception e)
        	{
        		String errMsg = "Error setting the password for user '" + username +
        			"' through RemoteUserDAO";
	        	s_Logger.error("getUser(): " + errMsg, e);
	        	throw new ProxyDataAccessException(errMsg, e);
        	}
        }
	}


	/**
	 * Returns a valid remote object or throws and exception.
	 * 
	 * This uses RMI to connect to a <code>RemoteObjectFactoryDAO</code> and
	 * queries it for the remote data object.
	 * 
	 * @return A valid remote object.
	 * @throws AccessException  if the object registry is local and it denies the 
	 * 		caller access to perform this operation 
	 * @throws RemoteException  if remote communication with the registry failed.
	 * @throws NotBoundException if the RemoteObjectFactory is not currently bound.
	 */
	private RemoteUserDAO getRemoteDAO() throws AccessException, RemoteException, NotBoundException
	{
		if (m_RemoteDAO != null)
		{
			return m_RemoteDAO;
		}

		RemoteObjectFactoryDAO factory = getRemoteFactory();

		m_RemoteDAO = factory.getUserDAO(getOriginatorName());
		return m_RemoteDAO;
	}


	/**
	 * Sets the m_RemoteDAO member to null. 
	 * This will force a new connection to any calls to <code>getRemoteDAO</code>
	 * to try to make a new connection.
	 */
	private void resetRemoteDAO()
	{
		m_RemoteDAO = null;
	}

}
