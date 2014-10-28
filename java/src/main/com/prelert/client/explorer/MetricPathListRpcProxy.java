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

package com.prelert.client.explorer;

import com.extjs.gxt.ui.client.data.BaseListLoadResult;
import com.extjs.gxt.ui.client.data.RpcProxy;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.prelert.data.gxt.MetricPathListLoadConfig;
import com.prelert.data.gxt.MetricTreeNodeModel;
import com.prelert.service.AsyncServiceLocator;
import com.prelert.service.DataSourceQueryServiceAsync;


/**
 * Extension of the GXT <code>RpcProxy</code> class for loading a level of data
 * from the metric path tree through remote procedure calls to the data source 
 * query service. The load criteria are specified though a 
 * {@link MetricPathListLoadConfig} object passed to the load methods.
 * @author Pete Harverson
 */
public class MetricPathListRpcProxy extends RpcProxy<BaseListLoadResult<MetricTreeNodeModel>>
{
	private DataSourceQueryServiceAsync 	m_DataSourceQueryService;

	public MetricPathListRpcProxy()
	{
		m_DataSourceQueryService = AsyncServiceLocator.getInstance().getDataSourceQueryService();
	}
	
	
	@Override
    protected void load(Object loadConfig,
            AsyncCallback<BaseListLoadResult<MetricTreeNodeModel>> callback)
    {
		loadNextLevel((MetricPathListLoadConfig)loadConfig, callback);
    }
	
	
	/**
	 * Loads the next level in the metric path tree to the one defined in the
	 * supplied load configuration.
	 * @param loadConfig <code>MetricPathListLoadConfig</code defining the currently selected
	 * 	path in the tree.
	 * @param callback callback object to receive the nodes in the next level in the metric path tree.
	 */
	public void loadNextLevel(MetricPathListLoadConfig loadConfig, 
			AsyncCallback<BaseListLoadResult<MetricTreeNodeModel>> callback)
	{
		m_DataSourceQueryService.getNextLevel(loadConfig, callback);
	}
	
	
	/**
	 * Loads the previous level in the metric path tree to the one defined in the
	 * supplied load configuration.
	 * @param loadConfig <code>MetricPathListLoadConfig</code> defining the currently selected
	 * 	path in the tree.
	 * @param callback callback object to receive the nodes in the previous level in the 
	 * 	metric path tree.
	 */
	public void loadPreviousLevel(MetricPathListLoadConfig loadConfig, 
			AsyncCallback<BaseListLoadResult<MetricTreeNodeModel>> callback)
	{
		m_DataSourceQueryService.getPreviousLevel(loadConfig, callback);
	}
	
	
	/**
	 * Loads the current level of the metric path tree, as defined by the supplied 
	 * load configuration.
	 * @param loadConfig <code>MetricPathListLoadConfig</code> defining the currently selected
	 * 	path in the tree.
	 * @param callback callback object to receive the nodes in the current level
	 * 	of the metric path tree.
	 */
	public void loadCurrentLevel(MetricPathListLoadConfig loadConfig, 
			AsyncCallback<BaseListLoadResult<MetricTreeNodeModel>> callback)
	{
		m_DataSourceQueryService.getCurrentLevel(loadConfig, callback);
	}

}
