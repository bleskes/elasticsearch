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
 * on +44 (0)20 7953 7243 or email to legal@prelert.com.    *
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

package com.prelert.proxy.dao;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import com.prelert.data.User;


/**
 * Interface defining the remote methods that can be called via RMI.
 * This interface is a remote version of UserDAO and exactly matches 
 * that interface.
 * 
 * @author Pete Harverson
 */
public interface RemoteUserDAO extends Remote
{
	/**
	 * Adds a Prelert user. 
	 * @param requesterUsername username of the user who is adding the new user.
	 * @param username username of the new user.
	 * @param firstName first name of the new user.
	 * @param lastName last name of the new user.
	 * @param rolename name of the role of the new user.
	 * @param passwordHash password, pre-encrypted with SHA-1 hash in lower case hex.
	 * @return a status code, where zero indicates the user was added successfully,
	 * 		and non-zero indicates the operation failed.
	 */
	public int addUser(String requesterUsername, String username, String firstName, 
			String lastName, String rolename, String passwordHash) throws RemoteException;
	

	/**
	 * Edits the details of an existing Prelert user.
	 * @param requesterUsername username of the user who is performing the edit.
	 * @param oldUsername old username of user being edited.
	 * @param newUsername new username of user being edited (may be unchanged).
	 * @param firstName first name of the user being edited.
	 * @param lastName last name of the user being edited.
	 * @param rolename name of the role of the user being edited.
	 * @return a status code, where zero indicates the user was added successfully,
	 * 		and non-zero indicates the operation failed.
	 */
	public int editUser(String requesterUsername, String oldUsername, String newUsername,
			String firstName, String lastName, String rolename) throws RemoteException;
	
	
	/**
	 * Deletes a Prelert user.
	 * @param requesterUsername username of the user who is performing the edit.
	 * @param username username of the user to be deleted.
	 * @return a status code, where zero indicates the user was deleted successfully,
	 * 		and non-zero indicates the operation failed.
	 */
	public int deleteUser(String requesterUsername, String username) throws RemoteException;
	
	
	/**
	 * Returns a list of all the Prelert role names.
	 * @return list of role names.
	 */
	public List<String> getRoles() throws RemoteException;
	
	
	/**
	 * Returns a list of all the Prelert users.
	 * @return a list of UserModel data objects.
	 */
	public List<User> getUsers() throws RemoteException;
	
	
	/**
	 * Returns the user with the specified username.
	 * @param username username of user to return.
	 * @return User with specified username, or <code>null</code> if no user exists
	 * 	with the specified username.
	 */
	public User getUser(String username) throws RemoteException;
	
	
	/**
	 * Sets the password for an existing user.
	 * @param requesterUsername username of the user who is performing the edit.
	 * @param username username of the user whose password is being changed.
	 * @param passwordHash password, pre-encrypted with SHA-1 hash in lower case hex.
	 * @return a status code, where zero indicates the password was set successfully,
	 * 		and non-zero indicates the operation failed.
	 */
	public int setPassword(String requesterUsername, String username, String passwordHash) 
		throws RemoteException;
}
