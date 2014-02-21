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
 * on +44 (0)20 7953 7243 or email to legal@prelert.com.    *
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

import com.google.gwt.user.client.rpc.RemoteService;
import com.prelert.data.gxt.CausalityEvidencePagingLoadConfig;
import com.prelert.data.gxt.DatePagingLoadResult;
import com.prelert.data.gxt.EvidenceModel;
import com.prelert.data.gxt.ProbableCauseModel;
import com.prelert.data.gxt.ProbableCauseModelCollection;


/**
 * Defines the methods for the interface to the Causality Query service.
 * @author Pete Harverson
 */
public interface CausalityQueryService extends RemoteService
{

	/**
	 * Returns the list of probable causes for the item of evidence with the specified id.
	 * @param evidenceId the id of the item of evidence for which to obtain
	 * 	the probable causes.
	 * @param timeSpanSecs time span, in seconds, that is used for calculating 
	 * 	metrics for probable causes that are features in time series e.g. peak 
	 * 	value in time window of interest.
	 * @return list of probable causes. This list will be empty if the item of
	 * 	evidence has no probable causes.
	 */
	public List<ProbableCauseModel> getProbableCauses(int evidenceId, int timeSpanSecs);
	
	
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
	 * @return list of ProbableCauseModelCollection objects. This list will be empty 
	 * 	if the item of evidence has no probable causes.
	 */
	public List<ProbableCauseModelCollection> getAggregatedProbableCauses(
			int evidenceId, int timeSpanSecs);
	
	
	/**
	 * Returns a list of the columns to display in a page of evidence data from
	 * a probable cause with the specified data type.
	 * @param dataType identifier for the type of evidence data.
	 * @return list of all of the columns for an evidence grid.
	 */
	public List<String> getEvidenceColumns(String dataType);
	
	
	/**
	 * Returns the first page of evidence data from a probable cause incident 
	 * matching the specified load config (i.e. evidence id).
	 * @param config load config specifying the data to obtain.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EvidenceModel> getFirstPage(
			CausalityEvidencePagingLoadConfig config);
	
	
	/**
	 * Returns the last page of evidence data from a probable cause incident 
	 * matching the specified load config (i.e. evidence id).
	 * @param config load config specifying the data to obtain.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EvidenceModel> getLastPage(
			CausalityEvidencePagingLoadConfig config);
	
	
	/**
	 * Returns the next page of evidence data following on from the row specified
	 * in the supplied load config (i.e. id and date of bottom row of evidence in
	 * the current page). The method will return notifications from the
	 * incident containing the specified item of evidence, with the same data type
	 * and description.
	 * @param config load config specifying the data to obtain.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EvidenceModel> getNextPage(
			CausalityEvidencePagingLoadConfig config);
	
	
	/**
	 * Returns the previous page of evidence data to the row specified in the
	 * supplied load config (i.e. id and date of top row of evidence in
	 * the current page). The method will return notifications from the
	 * incident containing the specified item of evidence, with the same data type
	 * and description.
	 * @param config load config specifying the data to obtain.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EvidenceModel> getPreviousPage(
			CausalityEvidencePagingLoadConfig config);
	
	
	/**
	 * Returns a page of evidence data from a probable cause incident, 
	 * whose top row will match the date in the supplied config.
	 * @param config load config specifying the range of data to obtain. The date
	 * in this DatePagingLoadConfig will correspond to the value of the time column 
	 * for the first row of evidence data that will be returned.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EvidenceModel> getAtTime(
			CausalityEvidencePagingLoadConfig config);

}
