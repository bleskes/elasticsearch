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

import com.extjs.gxt.ui.client.data.BasePagingLoader;
import com.extjs.gxt.ui.client.data.LoadEvent;
import com.extjs.gxt.ui.client.data.PagingLoadConfig;
import com.extjs.gxt.ui.client.data.PagingLoadResult;

import com.prelert.client.ApplicationResponseHandler;
import com.prelert.data.gxt.UserModel;


/**
 * Loader for a page of user data, adding support for loading a page containing
 * a specified user.
 * @author Pete Harverson
 */
public class UserPagingLoader extends BasePagingLoader<PagingLoadResult<UserModel>>
{
	private UserPagingRpcProxy	m_UserPagingProxy;
	
	
	/**
	 * Creates a new loader for a page of user data, using the specified object
	 * to retrieve data from the user query service via GWT RPC.
	 * @param proxy UserPagingRpcProxy to retrieve data from the user query service.
	 */
    public UserPagingLoader(UserPagingRpcProxy proxy)
    {
	    super(proxy);
	    m_UserPagingProxy = proxy;
    }
    

    @Override
    protected void loadData(final Object config)
    {
    	
    	ApplicationResponseHandler<PagingLoadResult<UserModel>> callback = 
			new ApplicationResponseHandler<PagingLoadResult<UserModel>>()
		{
			public void uponFailure(Throwable caught)
			{
				onLoadFailure(config, caught);
			}


			public void uponSuccess(PagingLoadResult<UserModel> result)
			{
				onLoadSuccess(config, result);
			}
		};

		proxy.load(reader, config, callback);
    }


	/**
     * Loads a page of user data containing the specified user.
     * @param username username of the user that the returned page should include.
     */
	public void loadPageWithUser(String username)
	{
		Object config = (reuseConfig && lastConfig != null) ? lastConfig : newLoadConfig();
		config = prepareLoadConfig(config);
		
		final PagingLoadConfig loadConfig = (PagingLoadConfig)config;
		
		if (fireEvent(BeforeLoad, new LoadEvent(this, loadConfig)))
		{
			lastConfig = config;
			
			ApplicationResponseHandler<PagingLoadResult<UserModel>> callback = 
				new ApplicationResponseHandler<PagingLoadResult<UserModel>>()
			{
				@Override
                public void uponFailure(Throwable caught)
				{
					onLoadFailure(loadConfig, caught);
				}


				@Override
                public void uponSuccess(PagingLoadResult<UserModel> result)
				{
					onLoadSuccess(loadConfig, result);
				}
			};

			m_UserPagingProxy.loadWithUser(loadConfig, username, callback);
		}
	}	
	
}
