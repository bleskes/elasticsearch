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

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.prelert.client.list.ModelDatePagingRpcProxy;
import com.prelert.data.gxt.DatePagingLoadResult;
import com.prelert.data.gxt.IncidentModel;
import com.prelert.data.gxt.ActivityPagingLoadConfig;
import com.prelert.data.gxt.ModelDatePagingLoadConfig;
import com.prelert.service.AsyncServiceLocator;
import com.prelert.service.IncidentQueryServiceAsync;


/**
 * Concrete sub-class of ModelDatePagingRpcProxy for paging through activity data.
 * The RpcProxy pages through activities who have an anomaly score greater than or
 * equal to the threshold in the ActivityPagingLoadConfig supplied to the calls.
 * @see ActivityPagingLoadConfig
 * @author Pete Harverson
 */
public class ActivityPagingRpcProxy extends ModelDatePagingRpcProxy<IncidentModel>
{
	private IncidentQueryServiceAsync	m_IncidentQueryService;
	
	
	/**
	 * Creates a new RPC proxy for paging through activity data.
	 */
	public ActivityPagingRpcProxy()
	{
		m_IncidentQueryService = AsyncServiceLocator.getInstance().getIncidentQueryService();
	}
	

	/**
	 * @param loadConfig ActivityPagingLoadConfig specifying an anomaly threshold of
	 * 	activities to return.
	 */
	@Override
    public void loadFirstPage(ModelDatePagingLoadConfig loadConfig,
            AsyncCallback<DatePagingLoadResult<IncidentModel>> callback)
    {
		m_IncidentQueryService.getFirstPage((ActivityPagingLoadConfig)loadConfig, callback);
    }
	

	/**
	 * @param loadConfig ActivityPagingLoadConfig specifying an anomaly threshold of
	 * 	activities to return.
	 */
	@Override
    public void loadLastPage(ModelDatePagingLoadConfig loadConfig,
            AsyncCallback<DatePagingLoadResult<IncidentModel>> callback)
    {
		m_IncidentQueryService.getLastPage((ActivityPagingLoadConfig)loadConfig, callback);
    }
	

	/**
	 * @param loadConfig ActivityPagingLoadConfig specifying an anomaly threshold of
	 * 	activities to return.
	 */
	@Override
    public void loadNextPage(ModelDatePagingLoadConfig loadConfig,
            AsyncCallback<DatePagingLoadResult<IncidentModel>> callback)
    {
		m_IncidentQueryService.getNextPage((ActivityPagingLoadConfig)loadConfig, callback);
    }
	

	/**
	 * @param loadConfig ActivityPagingLoadConfig specifying an anomaly threshold of
	 * 	activities to return.
	 */
	@Override
    public void loadPreviousPage(ModelDatePagingLoadConfig loadConfig,
            AsyncCallback<DatePagingLoadResult<IncidentModel>> callback)
    {
		m_IncidentQueryService.getPreviousPage((ActivityPagingLoadConfig)loadConfig, callback);
    }
	

	/**
	 * @param loadConfig ActivityPagingLoadConfig specifying an anomaly threshold of
	 * 	activities to return.
	 */
	@Override
    public void loadAtTime(ModelDatePagingLoadConfig loadConfig,
            AsyncCallback<DatePagingLoadResult<IncidentModel>> callback)
    {
		m_IncidentQueryService.getAtTime((ActivityPagingLoadConfig)loadConfig, callback);
    }
}
