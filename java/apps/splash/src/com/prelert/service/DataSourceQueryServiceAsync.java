/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2010     *
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

import com.extjs.gxt.ui.client.data.BaseListLoadResult;
import com.extjs.gxt.ui.client.data.BaseModelData;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.prelert.data.DataSourceType;
import com.prelert.data.gxt.DataSourceModel;
import com.prelert.data.gxt.DataSourceTreeModel;


/**
 * Defines the methods to be implemented by the asynchronous client interface
 * to the data source query service.
 * @author Pete Harverson
 */
public interface DataSourceQueryServiceAsync
{
	
	/**
	 * Returns a list of all the source types from which data has been retrieved
	 * by the Prelert engine e.g. p2ps logs, CPU data, p2psmon user usage data.
	 * @param includeCounts <code>true</code> to include data point count in the returned data,
	 * 			<code>false</code> otherwise.
	 * @param callback callback object to receive a response from the remote procedure call.
	 */
	public void getDataSourceTypeTreeModels(boolean includeCounts, 
			AsyncCallback<List<DataSourceTreeModel>> callback);
	
	
	/**
	 * Returns a list of 'child' data sources for the given 'parent' data source
	 * e.g. the list of p2ps servers supplying p2ps log file data.
	 * @param dataSource the source for which to obtain the 'child' data sources.
	 * 			If <code>null</code>, then an 'Analysed Data' root node will be
	 * 			returned. If called with 'Analysed Data' root node (i.e. where the
	 * 			data source category is null), then the the list of source types 
	 * 			from which data has been retrieved will be returned.
	 * @param includeCounts <code>true</code> to include data point count in the returned data,
	 * 			<code>false</code> otherwise.
	 * @param callback callback object to receive a response from the remote procedure call.
	 */
	public void getDataSourceTreeModels(DataSourceTreeModel dataSource, 
			boolean includeCounts, AsyncCallback<List<DataSourceTreeModel>> callback);
	
	
	/**
	 * Returns a list of all sources for a specified source type, in descending 
	 * order of data point count e.g. a list of p2ps servers supplying service 
	 * usage information.
	 * @param dataSourceType the source type for which to return the list of sources.
	 * @param callback callback object to receive a response from the remote procedure call.
	 */
	public void getDataSourceTreeModelsByCount(DataSourceTreeModel dataSourceType, 
			AsyncCallback<List<DataSourceTreeModel>> callback);
	
	
	/**
	 * Returns a list of all the source types from which data has been retrieved
	 * by the Prelert engine e.g. p2ps logs, CPU data, p2psmon user usage data.
	 * @param includeCounts <code>true</code> to include data point count in the returned data,
	 * 			<code>false</code> otherwise.
	 * @param callback callback object to receive a response from the remote procedure call.
	 */
	public void getDataSourceTypes(AsyncCallback<List<DataSourceModel>> callback);
	
	
	/**
	 * Returns a list of 'child' data sources for the given 'parent' data source
	 * e.g. the list of p2ps servers supplying p2ps log file data.
	 * @param dataSource the source for which to obtain the 'child' data sources.
	 * 			If <code>null</code>, then an 'Analysed Data' root node will be
	 * 			returned. If called with 'Analysed Data' root node (i.e. where the
	 * 			data source category is null), then the the list of source types 
	 * 			from which data has been retrieved will be returned.
	 * @param includeCounts <code>true</code> to include data point count in the returned data,
	 * 			<code>false</code> otherwise.
	 * @param callback callback object to receive a response from the remote procedure call.
	 */
	public void getDataSources(DataSourceType dataSourceType, 
			AsyncCallback<List<DataSourceModel>> callback);
	
	
	/**
	 * Returns the complete list of data sources for list based load results.
	 * @param callback callback object to receive a response from the remote procedure call.
	 */
	public void getAllDataSourcesListLoadResult(
			AsyncCallback<BaseListLoadResult<BaseModelData>> callback);
	
	
	/**
	 * Returns the data sources for the specified source type for list based load results.
	 * @param callback callback object to receive a response from the remote procedure call.
	 */
	public void getDataSourcesListLoadResult(DataSourceType dataSourceType,
			AsyncCallback<BaseListLoadResult<BaseModelData>> callback);
	
	
}
