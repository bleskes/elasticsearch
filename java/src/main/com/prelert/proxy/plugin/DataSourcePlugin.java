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

package com.prelert.proxy.plugin;

import java.util.List;

import com.prelert.data.DataSource;
import com.prelert.data.DataSourceType;
import com.prelert.data.MetricTreeNode;

/**
 * Interface defining the methods available for DataSource plugins.
 */
public interface DataSourcePlugin 
{
	/**
	 * Get the count of data items for this data type (as displayed on the GUI's
	 * "Analysed Data" screen), or -1 if this information is not available.
	 * @param type The data type to get the item count for.
	 * @return The count of data items for this data type.  If this information
	 *         is not available returns -1.
	 */
	public int getDataTypeItemCount(DataSourceType type);


	/**
	 * Returns the number of points collected from a particular source for
	 * a particular data type (as displayed in GUI data sources in the
	 * explorer view) or -1 if unknown.
	 * 
	 * @param source The datatype and source.
	 * @return The count of data items for this source or -1 if the count is not
	 * 		available.
	 */
	public int getDataSourceItemCount(DataSource source);
	
	
	/**
	 * Query the plugin for available sources.
	 * @param type A data type to restrict the sources retrieved.
	 * @return The sources known by this plugin that have provided data of the
	 *         specified type.
	 */
	public List<DataSource> getDataSources(DataSourceType type);
	
	
	/**
	 * Returns the <code>MetricTreeNode</code> for the specified path.
	 * 
	 * If all 5 parameters are <code>null</code> then the top level 
	 * node will be returned. For levels under the top node all 
	 * 5 parameters should be non-<code>null</code>.
	 * 
	 * @param datatype - may be <code>null</code> 
	 * @param previousPath - may be <code>null</code>
	 * @param currentValue - may be <code>null</code>
	 * @param opaqueNum - may be <code>null</code>
	 * @param opaqueStr - may be <code>null</code>
	 * @return
	 */
	public List<MetricTreeNode> getDataSourceTreeNextLevel(String datatype,
										String previousPath,
										String currentValue, int opaqueNum,
										String opaqueStr);
	
	
	/**
	 * Returns the <code>MetricTreeNode</code> for the specified 
	 * paths parent node. Returns the equivalent of calling 
	 * <code>getDataSourceTreeNextLevel()</code> for the nodes parent's
	 * parent.
	 * 
	 * If datatype or previousPath are <code>null</code> then nothing will be
	 * returned.
	 * 
	 * @param datatype 
	 * @param previousPath
	 * @param opaqueNum
	 * @param opaqueStr
	 * @return
	 */
	public List<MetricTreeNode> getDataSourceTreePreviousLevel(String datatype,
										String previousPath,
										int opaqueNum,String opaqueStr);
	
	
	/**
	 * Returns the <code>MetricTreeNode</code> for the child nodes of 
	 * the specified path. 
	 * 
	 * If datatype or previousPath are <code>null</code> then nothing will be
	 * returned.
	 * 
	 * @param datatype
	 * @param previousPath
	 * @param opaqueNum
	 * @param opaqueStr
	 * @return
	 */
	public List<MetricTreeNode> getDataSourceTreeCurrentLevel(String datatype,
												String previousPath,
												int opaqueNum,String opaqueStr);

}
