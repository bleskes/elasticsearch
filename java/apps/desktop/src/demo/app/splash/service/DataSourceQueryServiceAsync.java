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

import com.google.gwt.user.client.rpc.AsyncCallback;

import demo.app.data.gxt.DataSourceTreeModel;


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
	 * @param callback callback object to receive a response from the remote procedure call.
	 */
	public void getDataSourceTypes(AsyncCallback<List<DataSourceTreeModel>> callback);
	
	
	/**
	 * Returns a map of the source types against the total number of data points
	 * that have been collected for each type.
	 * @param callback callback object to receive a response from the remote procedure call.
	 */
	public void getDataSourceTypeCounts(AsyncCallback<List<DataSourceTreeModel>> callback);
	
	
	/**
	 * Returns a list of all sources for a specified source type, ordered by 
	 * source name e.g. a list of p2ps servers supplying service usage information.
	 * @param dataSourceType the source type for which to return the list of sources.
	 * @param callback callback object to receive a response from the remote procedure call.
	 */
	public void getSources(DataSourceTreeModel dataSourceType, AsyncCallback<List<DataSourceTreeModel>> callback);
	
	
	/**
	 * Returns a map of the sources for the specified source type against the
	 * total number of data points that have been collected for each source.
	 * @param dataSourceType the source type for which to return the sources.
	 * @param callback callback object to receive a response from the remote procedure call.
	 */
	public void getSourceCounts(
			DataSourceTreeModel dataSourceType, AsyncCallback<List<DataSourceTreeModel>> callback);
	
	
	/**
	 * Returns a map of all sources for a specified source type against the
	 * total number of data points that have been collected for each source, 
	 * ordered by number of data points collected e.g. a list of p2ps servers 
	 * supplying service usage information.
	 * @param dataSourceType the source type for which to return the list of sources.
	 * @param callback callback object to receive a response from the remote procedure call.
	 */
	public void getSourcesOrderByCount(
			DataSourceTreeModel dataSourceType, AsyncCallback<List<DataSourceTreeModel>> callback);
	
	
	/**
	 * Returns a list of 'child' data sources for the given 'parent' data source
	 * e.g. the list of p2ps servers supplying p2ps log file data.
	 * @param dataSource the source for which to obtain the 'child' data sources.
	 * 			If <code>null</code>, then the list of source types from which data
	 * 			has been retrieved will be returned.
	  @param callback callback object to receive a response from the remote procedure call.
	 */
	public void getDataSources(DataSourceTreeModel dataSource, 
			AsyncCallback<List<DataSourceTreeModel>> callback);
	

}
