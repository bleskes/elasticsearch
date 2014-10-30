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

import java.util.Collections;
import java.util.List;

import com.prelert.data.DataSource;
import com.prelert.data.DataSourceType;
import com.prelert.data.MetricTreeNode;

/**
 * Convenience class which contains a skeleton implementation of the
 * <code>DataSourcePlugin</code> interface.  Provides implementations
 * for every function in the <code>DataSourcePlugin</code> interface so
 * a <code>Plugin</code> which doesn't handle data source data
 * can delegate functionality to this class.
 *
 * All <code>Plugin</code>s that can serve data to the GUI must implement
 * the <code>getDataSources</code> method; only a plugin that provides
 * data to be stored internally within Prelert may use this class without
 * overriding any methods.
 */
public class DataSourcePluginSkeletonImpl implements DataSourcePlugin
{
	/**
	 * Get the count of data items for this data type (as displayed on the GUI's
	 * "Analysed Data" screen), or -1 if this information is not available.
	 * @param type The data type to get the item count for.
	 * @return The count of data items for this data type.  If this information
	 *         is not available returns -1.
	 */
	@Override
	public int getDataTypeItemCount(DataSourceType type)
	{
		return -1;
	}


	@Override
	public int getDataSourceItemCount(DataSource source) 
	{
		return source.getCount();
	}

	/**
	 * Query the plugin for available sources.
	 * @param type A data type to restrict the sources retrieved.
	 * @return The sources known by this plugin that have provided data of the
	 *         specified type.
	 */
	@Override
	public List<DataSource> getDataSources(DataSourceType type)
	{
		return Collections.emptyList();
	}


	@Override
	public List<MetricTreeNode> getDataSourceTreeNextLevel(String datatype,
								String previousPath, 
								String currentValue, int opaqueNum,
									String opaqueStr) 
	{
		return Collections.emptyList();
	}


	@Override
	public List<MetricTreeNode> getDataSourceTreePreviousLevel(String datatype,
											String previousPath, 
											int opaqueNum, String opaqueStr)
	{
		return Collections.emptyList();
	}
	
	
	@Override
	public List<MetricTreeNode> getDataSourceTreeCurrentLevel(String datatype,
											String previousPath, 
											int opaqueNum, String opaqueStr)
	{
		return Collections.emptyList();
	}

}
