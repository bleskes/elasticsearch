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

import com.extjs.gxt.ui.client.data.PagingLoadResult;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.prelert.data.Attribute;
import com.prelert.data.CausalityView;
import com.prelert.data.gxt.CausalityDataModel;
import com.prelert.data.gxt.CausalityDataPagingLoadConfig;
import com.prelert.data.gxt.CausalityEvidencePagingLoadConfig;
import com.prelert.data.gxt.DatePagingLoadResult;
import com.prelert.data.gxt.EvidenceModel;
import com.prelert.data.gxt.ProbableCauseModel;
import com.prelert.data.gxt.ProbableCauseModelCollection;


/**
 * Defines the methods to be implemented by the asynchronous client interface
 * to the Causality query service.
 * @author Pete Harverson
 */
public interface CausalityQueryServiceAsync
{
	
	/**
	 * Returns the list of probable causes for the item of evidence with the specified id.
	 * @param evidenceId the id of the item of evidence for which to obtain
	 * 	the probable causes.
	 * @param timeSpanSecs time span, in seconds, that is used for calculating 
	 * 	metrics for probable causes that are features in time series e.g. peak 
	 * 	value in time window of interest.
	 * @param callback callback object to receive a response from the remote procedure call.
	 */
	public void getProbableCauses(int evidenceId, int timeSpanSecs, 
			AsyncCallback<List<ProbableCauseModel>> callback);
	

	/**
	 * Returns a list of probable causes which have been aggregated across shared data
	 * source type, time, and description values. For example:<br>
	 *  - system_udp, Mon Mar 15 2010, 'feature in packets_sent metric'<br>
	 *  which would contain a list probable cause objects for a range of servers.
	 * @param evidenceId the id of the item of evidence for which to obtain
	 * 	the aggregated probable causes.
	 * @param timeSpanSecs time span, in seconds, that is used for calculating 
	 * 	metrics for probable causes that are features in time series e.g. peak 
	 * 	value in time window of interest.
	 * @param callback callback object to receive a response from the remote procedure call.
	 */
	public void getAggregatedProbableCauses(int evidenceId, int timeSpanSecs, 
			AsyncCallback<List<ProbableCauseModelCollection>> callback);
	
	
	/**
	 * Returns configuration data for a causality view for the notification or
	 * time series feature with the specified id.
	 * @param evidenceId the id of an item of evidence from the incident of interest.
	 * @param timeSpanSecs time span, in seconds, of the window for which to return
	 * 		the peak value.
	 * @param callback callback object to receive the configuration object.
	 */
	public void getViewConfiguration(int evidenceId, int timeSpanSecs,
			AsyncCallback<CausalityView> callback);
	
	
	/**
	 * Returns a page of causality data from a probable cause incident according
	 * to the specified load config (i.e. evidence id, filters, sort, offset, limit).
	 * @param config load config specifying the data to obtain.
	 * @param callback callback object to receive the page of causality data.
	 */
	public void getCausalityDataPage(CausalityDataPagingLoadConfig config,
			AsyncCallback<PagingLoadResult<CausalityDataModel>> callback);
	
	
	/**
	 * Returns a list of the different values for the attribute with the given name
	 * across all the notifications and time series features from a probable cause
	 * incident containing the specified item of evidence.
	 * @param  evidenceId the id of an item of evidence from the incident of interest.
	 * @param attributeName name of the attribute for which to return the different values.
	 * @param callback callback object to receive the list of values.
	 */
	public void getCausalityDataColumnValues(int evidenceId, 
			String attributeName, AsyncCallback<List<String>> callback);
	
	
	/**
	 * Returns the ID of the latest item of evidence for causality data matching the
	 * given properties from the incident containing the specified item of evidence.
	 * @param evidenceId the id of an item of evidence from the incident of interest.
	 * @param dataSourceName name of the data type of the evidence to return.
	 * @param description description of the evidence to return.
	 * @param source name of the source (server) of the evidence to return.
	 * @param attributes optional list of additional attributes defining evidence to return.
	 * @param callback callback object to receive the evidence ID.
	 */
	public void getLatestEvidenceId(int evidenceId, String dataSourceName,
    		String description, String source, List<Attribute> attributes,
			AsyncCallback<Integer> callback);
	
	
	/**
	 * Returns a list of the columns to display in a page of evidence data from
	 * a probable cause with the specified data type.
	 * @param dataType identifier for the type of evidence data.
	 * @param callback callback object to receive a response from the remote procedure call.
	 */
	public void getEvidenceColumns(String dataType, 
			AsyncCallback<List<String>> callback);
	
	
	/**
	 * Returns the first page of evidence data from a probable cause incident 
	 * matching the specified load config (i.e. evidence id).
	 * @param config load config specifying the data to obtain.
	 * @param callback callback object to receive a response from the remote procedure call.
	 */
	public  void getFirstPage(CausalityEvidencePagingLoadConfig config, 
			AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
	
	
	/**
	 * Returns the last page of evidence data from a probable cause incident 
	 * matching the specified load config (i.e. evidence id).
	 * @param config load config specifying the data to obtain.
	 * @param callback callback object to receive a response from the remote procedure call.
	 */
	public void getLastPage(CausalityEvidencePagingLoadConfig config, 
			AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
	
	
	/**
	 * Returns the next page of evidence data following on from the row specified
	 * in the supplied load config (i.e. id and date of bottom row of evidence in
	 * the current page). The method will return notifications from the
	 * incident containing the specified item of evidence, with the same data type
	 * and description.
	 * @param config load config specifying the data to obtain.
	 * @param callback callback object to receive a response from the remote procedure call.
	 */
	public void getNextPage(CausalityEvidencePagingLoadConfig config, 
			AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);

	
	/**
	 * Returns the previous page of evidence data to the row specified in the
	 * supplied load config (i.e. id and date of top row of evidence in
	 * the current page). The method will return notifications from the
	 * incident containing the specified item of evidence, with the same data type
	 * and description.
	 * @param config load config specifying the data to obtain.
	 * @param callback callback object to receive a response from the remote procedure call.
	 */
	public void getPreviousPage(CausalityEvidencePagingLoadConfig config,
			AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
	
	
	/**
	 * Returns a page of evidence data from a probable cause incident, 
	 * whose top row will match the date in the supplied config.
	 * @param config load config specifying the range of data to obtain. The date
	 * in this DatePagingLoadConfig will correspond to the value of the time column 
	 * for the first row of evidence data that will be returned.
	 * @param callback callback object to receive the page of evidence data.
	 */
	public void getAtTime(CausalityEvidencePagingLoadConfig config, 
			AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
	
	
	/**
     * Returns the configured page size for the analysis view chart selection grid 
     * i.e. the number of rows to return in each load operation.
     * @param callback callback object to receive the page size.
     */
	public void getSelectionGridPageSize(AsyncCallback<Integer> callback);

}
