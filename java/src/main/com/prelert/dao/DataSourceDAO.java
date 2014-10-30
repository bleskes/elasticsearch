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

package com.prelert.dao;

import java.util.Date;
import java.util.List;

import com.prelert.data.DataSource;
import com.prelert.data.DataSourceType;
import com.prelert.data.MetricTreeNode;


/**
 * Interface defining the methods to be implemented by a Data Access Object
 * to obtain information on Prelert data sources.
 * 
 * @author Pete Harverson
 */
public interface DataSourceDAO
{
	/**
	 * Returns a list of all the source types from which data has been retrieved
	 * by the Prelert engine e.g. p2ps logs, CPU data, p2psmon user usage data.
	 * @return the complete list of data source types.
	 */
	public List<DataSourceType> getDataSourceTypes();
	
	/**
	 * Returns a list of all sources for a specified source type, ordered by 
	 * source name e.g. a list of p2ps servers supplying service usage information.
	 * @param dataSourceType the source type for which to return the list of sources.
	 * @return the complete list of sources for the given data source type.
	 */
	public List<DataSource> getDataSources(DataSourceType dataSourceType);
	
	
	/**
	 * Returns the complete list of sources from which data has been retrieved
	 * by the Prelert engine.
	 * @return the full list of data sources.
	 */
	public List<DataSource>	getAllDataSources();
	
	
	/**
	 * Returns the list of <code>MetricTreeNode</code> in the next level of the 
	 * metric path tree for the specified path.
	 * 
	 * If all 5 parameters are <code>null</code> then the top level node will 
	 * be returned. For levels under the top node, all 5 parameters should be 
	 * non-<code>null</code>.
	 *  
	 * @param type the data type, or <code>null</code> if loading the top level of the
     * 	metric path tree.
	 * @param previousPath the previous path to the level being loaded, or <code>null</code> 
     * 	if loading the top level of the metric path tree.
	 * @param currentValue the partial path value at the current level to load, 
	 * 	or <code>null</code> if loading the top level of the metric path tree.
	 * @param opaqueNum opaque integer ID representing the level being loaded, used 
	 * 	by some external plugins to obtain metric path data.
	 * @param opaqueStr opaque textual GUID, or <code>null</code> if loading the top level 
     * 	of the metric path tree.
	 * @return list of nodes in the next level in the metric path tree.
	 */
	public List<MetricTreeNode> getDataSourceTreeNextLevel(String type, String previousPath,
													String currentValue, Integer opaqueNum,
													String opaqueStr);
	
	/**
	 * Returns a list of <code>MetricTreeNode</code> for the specified 
	 * paths parent node. Returns the equivalent of calling 
	 * <code>getDataSourceTreeNextLevel()</code> for the nodes parent's
	 * parent.
	 * 
	 * If previousPath is <code>null</code> then the top level items will
	 * be returned (equivalent to calling getDataSourceTreeNextLevel(null, 
	 * null, null, null, null).
	 * 
	 * If previousPath exactly equals type then the top level items are
	 * returned (equivalent to calling getDataSourceTreeNextLevel(null, 
	 * null, null, null, null).
	 *  
	 * @param type the data type, or <code>null</code> if loading the top level of the
     * 	metric path tree.
	 * @param previousPath the previous path to the level being loaded.
	 * @param opaqueNum opaque integer ID representing the level being loaded, used 
	 * 	by some external plugins to obtain metric path data.
	 * @param opaqueStr opaque textual GUID, or <code>null</code> if loading the top level 
     * 	of the metric path tree.
	 * @return list of nodes in the previous level in the metric path tree.
	 */
	public List<MetricTreeNode> getDataSourceTreePreviousLevel(String type, String previousPath,
															Integer opaqueNum, String opaqueStr);	
	
	
	/**
	 * Return all the child tree nodes of the path denoted
	 * by the <code>previousPath</code> argument.
	 * 
	 * If previousPath is <code>null</code> then the top level items will
	 * be returned (equivalent to calling getDataSourceTreeNextLevel(null, 
	 * null, null, null, null).
	 * 
	 * If previousPath exactly equals type then the top level items are
	 * returned (equivalent to calling getDataSourceTreeNextLevel(null, 
	 * null, null, null, null).
	 *   
	 * @param type the data type, or <code>null</code> if loading the top level of the
     * 	metric path tree.
	 * @param previousPath the level being loaded.
	 * @param opaqueNum opaque integer ID representing the level being loaded, used 
	 * 	by some external plugins to obtain metric path data.
	 * @param opaqueStr opaque textual GUID, or <code>null</code> if loading the top level 
     * 	of the metric path tree.
	 * @return list of nodes in the current level in the metric path tree.
	 */
	public List<MetricTreeNode> getDataSourceTreeCurrentLevel(
			String type, String previousPath, Integer opaqueNum, String opaqueStr);	
	
	
	// Returns the expiry time of the product license.
	public Date getEndTime();


	/**
	 * Return the customer ID for this installation.  A null customer ID
	 * indicates that no usage data is to be gathered.
	 * @return the customer ID for this installation (which may be null)
	 */
	public String getCustomerId();

}
