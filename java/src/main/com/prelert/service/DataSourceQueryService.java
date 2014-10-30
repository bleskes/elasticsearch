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

import com.extjs.gxt.ui.client.data.BaseListLoadResult;
import com.extjs.gxt.ui.client.data.BaseModelData;
import com.google.gwt.user.client.rpc.RemoteService;

import com.prelert.data.DataSourceType;
import com.prelert.data.gxt.DataSourceModel;
import com.prelert.data.gxt.DataSourceTreeModel;
import com.prelert.data.gxt.MetricPathListLoadConfig;
import com.prelert.data.gxt.MetricTreeNodeModel;


/**
 * Defines the methods for the interface to the Data Source Query service.
 * @author Pete Harverson
 */
public interface DataSourceQueryService extends RemoteService
{
	/**
	 * Returns a list of all the source types from which data has been retrieved
	 * by the Prelert engine e.g. p2ps logs, CPU data, p2psmon user usage data.
	 * @param includeCounts <code>true</code> to include data point count in the returned data,
	 * 			<code>false</code> otherwise.
	 * @return the complete list of data source types, as tree model objects.
	 */
	public List<DataSourceTreeModel> getDataSourceTypeTreeModels(boolean includeCounts);
	
	
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
	 * @return list of data sources for the given 'parent' data source as tree model objects.
	 */
	public List<DataSourceTreeModel> getDataSourceTreeModels(
			DataSourceTreeModel dataSource, boolean includeCounts);
	
	
	/**
	 * Returns a list of all sources for a specified source type, in descending 
	 * order of data point count e.g. a list of p2ps servers supplying service 
	 * usage information.
	 * @param dataSourceType the source type for which to return the list of sources.
	 * @return the list of sources for the given data source type, complete 
	 * with data point count, as tree model objects.
	 */
	public List<DataSourceTreeModel> getDataSourceTreeModelsByCount(
			DataSourceTreeModel dataSourceType);
	
	
	/**
	 * Returns a list of all the source types from which data has been retrieved
	 * by the Prelert engine e.g. p2ps logs, CPU data, p2psmon user usage data.
	 * @param includeCounts <code>true</code> to include data point count in the returned data,
	 * 			<code>false</code> otherwise.
	 * @return the complete list of data source types.
	 */
	public List<DataSourceModel> getDataSourceTypes();
	
	
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
	 * @return list of data sources for the given 'parent' data source.
	 */
	public List<DataSourceModel> getDataSources(DataSourceType dataSourceType);
	
	
	/**
	 * Returns the complete list of data sources for list based load results.
	 * @return load result containing the requested data.
	 */
	public BaseListLoadResult<BaseModelData> getAllDataSourcesListLoadResult();
	
	
	/**
	 * Returns the data sources for the specified source type for list based load results.
	 * @return load result containing the requested data.
	 */
	public BaseListLoadResult<BaseModelData> getDataSourcesListLoadResult(DataSourceType dataSourceType);
	
	
	/**
	 * Returns the next level in the metric path tree to the one defined in the
	 * supplied load configuration.
	 * @param config MetricPathListLoadConfig defining the currently selected
	 * 	path in the tree.
	 * @return list load result containing the nodes in the next level in the metric path tree.
	 */
	public BaseListLoadResult<MetricTreeNodeModel> getNextLevel(
			MetricPathListLoadConfig config);
	
	
	/**
	 * Returns the previous level in the metric path tree to the one defined in the
	 * supplied load configuration.
	 * @param config MetricPathListLoadConfig defining the currently selected
	 * 	path in the tree.
	 * @return list load result containing the nodes in the previous level in 
	 * 	the metric path tree.
	 */
	public BaseListLoadResult<MetricTreeNodeModel> getPreviousLevel(
			MetricPathListLoadConfig config);
	
	
	/**
	 * Returns all the nodes in the current level of the metric path tree, as defined
	 * by the supplied load configuration.
	 * @param config MetricPathListLoadConfig defining the currently selected
	 * 	path in the tree.
	 * @return list load result containing all the nodes in this level of 
	 * 	the metric path tree.
	 */
	public BaseListLoadResult<MetricTreeNodeModel> getCurrentLevel(
			MetricPathListLoadConfig config);
	
}
