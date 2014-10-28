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

package demo.app.splash.service;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import demo.app.data.gxt.DataSourceTreeModel;

/**
 * Defines the methods for the interface to the Data Source Query service.
 * @author Pete Harverson
 */
public interface DataSourceQueryService extends RemoteService
{
	/**
	 * Returns a list of all the source types from which data has been retrieved
	 * by the Prelert engine e.g. p2ps logs, CPU data, p2psmon user usage data.
	 * <p>
	 * <b>N.B.</b> the returned list of DataSourceModel objects will contain no count data.
	 * @return the complete list of data source types, with no count data.
	 */
	public List<DataSourceTreeModel> getDataSourceTypes();
	
	
	/**
	 * Returns a list of all the source types from which data has been retrieved
	 * by the Prelert engine e.g. p2ps logs, CPU data, p2psmon user usage data.
	 * @return the complete list of data source types, complete with data point count.
	 */
	public List<DataSourceTreeModel> getDataSourceTypeCounts();
	
	
	/**
	 * Returns a list of all sources for a specified source type, ordered by 
	 * source name e.g. a list of p2ps servers supplying service usage information.
	 * <p>
	 * <b>N.B.</b> the returned list of DataSourceModel objects will contain no count data.
	 * @param dataSourceType the source type for which to return the list of sources.
	 * @return the complete list of sources for the given data source type, with no
	 * 			count data.
	 */
	public List<DataSourceTreeModel> getSources(DataSourceTreeModel dataSourceType);
	
	
	/**
	 * Returns a list of all sources for a specified source type, ordered by 
	 * source name e.g. a list of p2ps servers supplying service usage information.
	 * @param dataSourceType the source type for which to return the list of sources.
	 * @return the complete list of sources for the given data source type, complete 
	 * with data point count.
	 */
	public List<DataSourceTreeModel> getSourceCounts(DataSourceTreeModel dataSourceType);
	
	
	/**
	 * Returns a list of all sources for a specified source type, in descending 
	 * order of data point count e.g. a list of p2ps servers supplying service 
	 * usage information.
	 * @param dataSourceType the source type for which to return the list of sources.
	 * @return the complete list of sources for the given data source type, complete 
	 * with data point count.
	 */
	public List<DataSourceTreeModel> getSourcesOrderByCount(DataSourceTreeModel dataSourceType);
	
	
	/**
	 * Returns a list of 'child' data sources for the given 'parent' data source
	 * e.g. the list of p2ps servers supplying p2ps log file data.
	 * @param dataSource the source for which to obtain the 'child' data sources.
	 * 			If <code>null</code>, then the list of source types from which data
	 * 			has been retrieved will be returned.
	 * @return list of data sources for the given 'parent' data source.
	 */
	public List<DataSourceTreeModel> getDataSources(DataSourceTreeModel dataSource);
	
}
