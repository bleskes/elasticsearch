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

package com.prelert.service;

import java.util.List;

import com.extjs.gxt.ui.client.data.PagingLoadConfig;
import com.extjs.gxt.ui.client.data.PagingLoadResult;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import com.prelert.data.gxt.UserModel;


/**
 * Defines the methods for the interface to the User Query service.
 * @author Pete Harverson
 */
@RemoteServiceRelativePath("services/userQueryService")
public interface UserQueryService extends RemoteService
{
	/** Status code indicating operation on user service succeeded. */
	public static final int STATUS_SUCCESS = 0;
	
	/** Status code indicating operation on user service failed, with cause unknown. */
	public static final int STATUS_FAILURE_UNKNOWN = 101;
	
	/** 
	 * Status code indicating operation on user service failed due to use of a
	 * message digest algorithm not supported by the default security providers.
	 */
	public static final int STATUS_DIGEST_ALGORITHM_UNSUPPORTED = 102;
	
	/** Status code indicating operation on user service failed due to an incorrect password. */
	public static final int STATUS_INCORRECT_PASSWORD = 103;
	
	
	/**
	 * Adds a Prelert user.
	 * @param user the data for the user to add.
	 * @return a status code, where zero indicates the user was added successfully,
	 * 		and non-zero indicates the operation failed.
	 */
	public int addUser(UserModel user); 
	
	
	/**
	 * Edits the details of an existing Prelert user.
	 * @param oldUsername old username of user being edited.
	 * @param newUserData the new data for the user.
	 * @return a status code, where zero indicates the user was edited successfully,
	 * 		and non-zero indicates the operation failed.
	 */
	public int editUser(String oldUsername, UserModel newUserData);
	
	
	/**
	 * Deletes a Prelert user.
	 * @param username username of the user to be deleted.
	 * @return a status code, where zero indicates the user was deleted successfully,
	 * 		and non-zero indicates the operation failed.
	 */
	public int deleteUser(String username);
	
	
	/**
	 * Returns details of the currently logged in user.
	 * @return UserModel for the logged in user, minus password.
	 */
	public UserModel getLoggedInUser();
	
	
	/**
	 * Returns a list of all the Prelert role names.
	 * @return list of role names.
	 */
	public List<String> getRoles();
	
	
	/**
	 * Returns a page of Prelert user data.
	 * @param config paging load configuration, specifying the offset and page size.
	 * @return a page of user data.
	 */
	public PagingLoadResult<UserModel> getUsers(PagingLoadConfig config);
	
	
	/**
	 * Returns a page of Prelert user data containing the specified user.
	 * @param config paging load configuration, specifying the page size.
	 * @param username username of the user that the returned page should include.
	 * @return a page of user data.
	 */
	public PagingLoadResult<UserModel> getUsers(PagingLoadConfig config, String username);
	
	
	/**
	 * Sets the password for an existing user.
	 * @param username username of the user whose password is being set.
	 * @param password plain text password.
	 * @return a status code, where zero indicates the password was changed successfully,
	 * 		and non-zero indicates the operation failed.
	 */
	public int setPassword(String username, String password);
	
	
	/**
	 * Sets the password for an existing user after first verifying that the 
	 * password supplied by the client matches with the current password stored
	 * for that user.
	 * @param username username username of the user whose password is being set.
	 * @param currentPassword current plain text password supplied by the client
	 * 	 	for verification.
	 * @param newPassword new, plain text, password
	 * @return a status code, where zero indicates the password was changed successfully,
	 * 		and non-zero indicates the operation failed.
	 */
	public int setPassword(String username, String currentPassword, String newPassword);
}
