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

package com.prelert.dao;

import java.util.Date;
import java.util.List;

import com.prelert.data.ProbableCause;
import com.prelert.data.gxt.EvidenceModel;


/**
 * Interface defining the methods to be implemented by a Data Access Object
 * to obtain information for Causality views from information stored in 
 * the Prelert database.
 * 
 * @author Pete Harverson
 */
public interface CausalityDAO
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
	public List<ProbableCause> getProbableCauses(int evidenceId, int timeSpanSecs);
	
	
	/**
	 * For the incident containing the specified item of evidence, the method returns 
	 * the first page of notifications with the same data type and description.
	 * @param evidenceId the id of an item of evidence from an incident with
	 * 		which to match data type and description.
	 * @param filterAttributes optional attribute names on which to filter the query.
	 * @param filterValues optional attribute values on which to filter the query.
	 * @return List of Evidence records comprising the first page of data.
	 */
	public List<EvidenceModel> getFirstPage( 
			int evidenceId, List<String> filterAttributes, List<String> filterValues);
	
	
	/**
	 * For the incident containing the specified item of evidence, the method returns 
	 * the last page of notifications with the same data type and description.
	 * @param evidenceId the id of an item of evidence from an incident with
	 * 		which to match data type and description.
	 * @param filterAttributes optional attribute names on which to filter the query.
	 * @param filterValues optional attribute values on which to filter the query.
	 * @return List of Evidence records comprising the first page of data.
	 */
	public List<EvidenceModel> getLastPage( 
			int evidenceId, List<String> filterAttributes, List<String> filterValues);
	
	
	/**
	 * Returns the next page of evidence data following on from the row with the
	 * specified time and id. The method will return notifications from the
	 * incident containing the specified item of evidence, with the same data type
	 * and description.
	 * @param bottomRowId the value of the id column for the bottom row of evidence 
	 * 		in the current page.
	 * @param bottomRowTime the value of the time column for the bottom row of 
	 * 		evidence in the current page.
	 * @param filterAttributes optional attribute names on which to filter the query.
	 * @param filterValues optional attribute values on which to filter the query.
	 * @return List of Evidence records comprising the next page of data.
	 */
	public List<EvidenceModel> getNextPage(
			int bottomRowId, Date bottomRowTime, 
			List<String> filterAttributes, List<String> filterValues);
	
	
	/**
	 * Returns the previous page of evidence data following on from the row with the
	 * specified time and id. The method will return notifications from the
	 * incident containing the specified item of evidence, with the same data type
	 * and description.
	 * @param topRowId the value of the id column for the top row of evidence 
	 * 		in the current page.
	 * @param topRowTime the value of the time column for the top row of 
	 * 		evidence in the current page.
	 * @param filterAttributes optional attribute names on which to filter the query.
	 * @param filterValues optional attribute values on which to filter the query.
	 * @return List of Evidence records comprising the next page of data.
	 */
	public List<EvidenceModel> getPreviousPage(
			int topRowId, Date topRowTime, 
			List<String> filterAttributes, List<String> filterValues);
	
	
	/**
	 * For the incident containing the specified item of evidence, the method returns
	 * a page of notifications with the same data type and description, whose top 
	 * row will match the supplied time.
	 * @param evidenceId the id of an item of evidence from an incident with
	 * 		which to match data type and description.
	 * @param time value of the time column to match on for the first row of 
	 * evidence data that will be returned.
	 * @param filterAttributes optional attribute names on which to filter the query.
	 * @param filterValues optional attribute values on which to filter the query.
	 * @return List of Evidence records, sorted descending on time.
	 */
	public List<EvidenceModel> getAtTime(int evidenceId, Date time,
			List<String> filterAttributes, List<String> filterValues);
	
	
	/**
	 * For the incident containing the specified item of evidence, the method returns 
	 * the earliest notification with the same data type and description.
	 * @param evidenceId the id of an item of evidence from an incident with
	 * 		which to match data type and description.
	 * @param filterAttributes optional attribute names on which to filter the query.
	 * @param filterValues optional attribute values on which to filter the query.
	 * @return earliest evidence record with same data type and description.
	 */
	public EvidenceModel getEarliestEvidence(int evidenceId,
			List<String> filterAttributes, List<String> filterValues);
	
	
	/**
	 * For the incident containing the specified item of evidence, the method returns 
	 * the latest notification with the same data type and description.
	 * @param evidenceId the id of an item of evidence from an incident with
	 * 		which to match data type and description.
	 * @param filterAttributes optional attribute names on which to filter the query.
	 * @param filterValues optional attribute values on which to filter the query.
	 * @return latest evidence record with same data type and description.
	 */
	public EvidenceModel getLatestEvidence(int evidenceId,
			List<String> filterAttributes, List<String> filterValues);
}
