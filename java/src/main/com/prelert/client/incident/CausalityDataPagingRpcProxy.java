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

package com.prelert.client.incident;

import com.extjs.gxt.ui.client.data.PagingLoadResult;
import com.extjs.gxt.ui.client.data.RpcProxy;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.prelert.data.gxt.CausalityDataModel;
import com.prelert.data.gxt.CausalityDataPagingLoadConfig;
import com.prelert.service.AsyncServiceLocator;
import com.prelert.service.CausalityQueryServiceAsync;


/**
 * Extension of the RpcProxy class for loading causality data through remote
 * procedure calls to the causality query service. The load criteria are
 * specified though a {@link CausalityDataPagingLoadConfig} object passed to
 * the load() method.
 * @author Pete Harverson
 */
public class CausalityDataPagingRpcProxy extends RpcProxy<PagingLoadResult<CausalityDataModel>>
{
	private CausalityQueryServiceAsync 	m_CausalityQueryService = null;
	
	
	/**
	 * Creates a new data proxy for loading causality data relating to an item of
	 * evidence from the causality query service.
	 */
	public CausalityDataPagingRpcProxy()
	{
		m_CausalityQueryService = AsyncServiceLocator.getInstance().getCausalityQueryService();
	}
	
	
	@Override
    protected void load(Object loadConfig,
            AsyncCallback<PagingLoadResult<CausalityDataModel>> callback)
    {
		m_CausalityQueryService.getCausalityDataPage((CausalityDataPagingLoadConfig)loadConfig, callback);
    }

}
