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
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.prelert.data.gxt.UserModel;


/**
 * Defines the methods to be implemented by the asynchronous client interface
 * to the user query service.
 * @author Pete Harverson
 */
public interface UserQueryServiceAsync
{
	/**
	 * Adds a Prelert user.
	 * @param user the data for the user to add.
	 * @param callback callback object to receive the status code response from
	 * 	 the remote procedure call.
	 */
	public void addUser(UserModel user, AsyncCallback<Integer> callback); 
	
	
	/**
	 * Edits the details of an existing Prelert user.
	 * @param oldUsername old username of user being edited.
	 * @param newUserData the new data for the user.
	 * @param callback callback object to receive the status code response from
	 * 	 the remote procedure call.
	 */
	public void editUser(String oldUsername, UserModel newUserData, 
			AsyncCallback<Integer> callback);
	
	
	/**
	 * Deletes a Prelert user.
	 * @param username username of the user to be deleted.
	 * @param callback callback object to receive the status code response from
	 * 	 the remote procedure call.
	 */
	public void deleteUser(String username, AsyncCallback<Integer> callback);
	
	
	/**
	 * Returns details of the currently logged in user.
	 * @param callback callback object to receive the UserModel response from
	 * 	 the remote procedure call.
	 */
	public void getLoggedInUser(AsyncCallback<UserModel> callback);
	
	
	/**
	 * Returns a list of all the Prelert role names.
	 * @param callback callback object to receive a response from the remote procedure call.
	 */
	public void getRoles(AsyncCallback<List<String>> callback);
	
	
	/**
	 * Returns a page of Prelert user data.
	 * @param config paging load configuration, specifying the offset and page size.
	 * @param callback callback object to receive a response from the remote procedure call.
	 */
	public void getUsers(PagingLoadConfig config, 
			AsyncCallback<PagingLoadResult<UserModel>> callback);
	
	
	/**
	 * Returns a page of Prelert user data containing the specified user.
	 * @param config paging load configuration, specifying the page size.
	 * @param username username of the user that the returned page should include.
	 * @param callback callback object to receive a response from the remote procedure call.
	 */
	public void getUsers(PagingLoadConfig config, String username,
			AsyncCallback<PagingLoadResult<UserModel>> callback);
	
	
	/**
	 * Sets the password for an existing user.
	 * @param username username of the user whose password is being set.	
	 * @param password plain text password.
	 * @param callback callback object to receive the status code response from
	 * 	 the remote procedure call.
	 */
	public void setPassword(String username, String password, 
			AsyncCallback<Integer> callback);
	
	
	/**
	 * Sets the password for an existing user after first verifying that the 
	 * password supplied by the client matches with the current password stored
	 * for that user.
	 * @param username username username of the user whose password is being set.
	 * @param currentPassword current plain text password supplied by the client
	 * 	 	for verification.
	 * @param newPassword new, plain text, password
	 * @param callback callback object to receive the status code response from
	 * 	 the remote procedure call.
	 */
	public void setPassword(String username, String currentPassword, String newPassword,
			AsyncCallback<Integer> callback);
}
