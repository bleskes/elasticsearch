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

package com.prelert.client.admin;

import com.extjs.gxt.ui.client.data.PagingLoadConfig;
import com.extjs.gxt.ui.client.data.PagingLoadResult;
import com.extjs.gxt.ui.client.data.RpcProxy;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.prelert.data.gxt.UserModel;
import com.prelert.service.UserQueryServiceAsync;


/**
 * Data proxy for retrieving pages of user data from the user query service via
 * GWT RPC calls.
 * @author Pete Harverson
 */
public class UserPagingRpcProxy extends RpcProxy<PagingLoadResult<UserModel>>
{
	private UserQueryServiceAsync	m_QueryService;
	
	
	/**
	 * Creates a new UserPagingRpcProxy data proxy.
	 */
	public UserPagingRpcProxy()
	{
		m_QueryService = AdminServiceLocator.getInstance().getUserQueryService();
	}

	
	@Override
    protected void load(Object loadConfig,
            AsyncCallback<PagingLoadResult<UserModel>> callback)
    {
		m_QueryService.getUsers((PagingLoadConfig)loadConfig, callback);
    }
	
	
	/**
	 * Loads a page of user data such that the returned page will contain
 	 * the specified user.
	 * @param loadConfig the PagingLoadConfig, specifying the offset and page size.
	 * @param username username of the user that should be returned in the page data.
	 * @param callback the data callback.
	 */
	public void loadWithUser(PagingLoadConfig loadConfig, String username, 
			AsyncCallback<PagingLoadResult<UserModel>> callback)
	{
		m_QueryService.getUsers(loadConfig, username, callback);
	}

}
