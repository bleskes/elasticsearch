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

package com.prelert.client.introscope;

import com.extjs.gxt.ui.client.data.PagingLoadResult;
import com.extjs.gxt.ui.client.data.RpcProxy;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.prelert.data.gxt.AttributeModel;
import com.prelert.service.introscope.IntroscopeConfigServiceAsync;


/**
 * Data proxy for loading pages of Introscope agents from the IntroscopeConfigService
 * via GWT RPC calls.
 * @author Pete Harverson
 */
public class AgentPagingRpcProxy extends RpcProxy<PagingLoadResult<AttributeModel>>
{
	private IntroscopeConfigServiceAsync	m_QueryService;

	
	/**
	 * Creates a new RPC proxy for paging through the available Introscope agents.
	 */
	public AgentPagingRpcProxy()
	{
		m_QueryService = IntroscopeServiceLocator.getInstance().getConfigService();
	}
	
	
	@Override
    protected void load(Object loadConfig,
            AsyncCallback<PagingLoadResult<AttributeModel>> callback)
    {
	    m_QueryService.listAgents((AgentPagingLoadConfig)loadConfig, callback);
    }

}
