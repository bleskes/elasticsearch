/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2013     *
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

package com.prelert.proxy.dao;

import java.rmi.RemoteException;
import java.util.Date;
import java.util.List;

import com.prelert.data.CausalityData;
import com.prelert.data.Evidence;
import com.prelert.data.ProbableCause;

/**
 * Interface defining the remote methods that can be called via RMI.
 * This interface is a remote version of CausalityDAO and exactly matches 
 * that interface.
 * 
 * @author dkyle
 *
 */
public interface RemoteCausalityDAO extends java.rmi.Remote
{
	/**
	 * Returns the list of probable causes for the item of evidence with the 
	 * specified id. Note that attributes are only populated for time series
	 * type probable causes.
	 * @param evidenceId the id of the item of evidence for which to obtain
	 * 	the probable causes.
	 * @param timeSpanSecs time span, in seconds, that is used for calculating 
	 * 	metrics for probable causes that are features in time series e.g. peak 
	 * 	value in time window of interest.
	 * @param maxOneFeaturePerSeries Where there are multiple features for the
	 *  same time series in the activity, should only one of them be returned?
	 * @return list of probable causes. This list will be empty if the item of
	 * 	evidence has no probable causes.
	 * @see #getProbableCausesForExport(int, int, boolean)
	 */
	public List<ProbableCause> getProbableCauses(int evidenceId,
												int timeSpanSecs,
												boolean maxOneFeaturePerSeries) throws RemoteException;
	
	
	/**
	 * Returns the list of probable causes for full data export e.g. to CSV format.
	 * This method is similar to {@link #getProbableCauses(int, int, boolean)}, except that
	 * attributes are populated for notifications as well as time series, whilst the
	 * peak value and scaling factor properties are not populated.
	 * @param evidenceId the id of the item of evidence for which to obtain
	 * 	the probable causes.
	 * @param timeSpanSecs time span, in seconds, that is used for calculating 
	 * 	metrics for probable causes that are features in time series e.g. peak 
	 * 	value in time window of interest.
	 * @param maxOneFeaturePerSeries Where there are multiple features for the
	 *  same time series in the activity, should only one of them be returned?
	 * @return list of probable causes. This list will be empty if the item of
	 * 	evidence has no probable causes.
	 * @see #getProbableCauses(int, int, boolean)
	 */
	public List<ProbableCause> getProbableCausesForExport(int evidenceId,
														int timeSpanSecs,
														boolean maxOneFeaturePerSeries) throws RemoteException;


	/**
	 * Returns the list of probable causes for all evidence in the supplied
	 * list.  THIS ONLY WORKS FOR DATABASES IN THE "EXTERNAL" FORMAT.
	 * It was added as a performance optimisation for the CA OEM deal.  For
	 * internal data it is more appropriate to get activity data for one
	 * activity at a time, as it is displayed.  Peak values and time series
	 * attributes are not returned in order to improve performance.
	 * @param evidenceIds a list containing the ids of items of evidence for
	 *  which to obtain the probable causes.
	 * @param maxOneFeaturePerSeries Where there are multiple features for the
	 *  same time series in the activity, should only one of them be returned?
	 * @return list of probable causes. This list may be empty (if the items of
	 * 	evidence have no probable causes), or may contain probable causes for
	 *  many activities (in which case the different activities can be
	 *  distinguished by looking at the "top" evidence ids).
	 * @see #getProbableCauses(int, int, boolean)
	 */
	public List<ProbableCause> getProbableCausesInBulk(List<Integer> evidenceIds,
													boolean maxOneFeaturePerSeries) throws RemoteException;


	/**
	 * Returns a list of causality data matching the supplied filter criteria for
	 * the activity containing the notification or time series feature with the specified id.
	 * @param evidenceId the id of an item of evidence from the activity of interest.
	 * @param returnAttributes list of the names of any additional attributes to be
	 * 	included in the output.
	 * @param primaryFilterNamesNull list of the names of attributes that must have 
	 * 	<code>null</code> values.
	 * @param primaryFilterNamesNotNull list of the names of attributes that must have 
	 * 	non-<code>null</code> values.
	 * @param primaryFilterValues list of attribute values corresponding to the
	 * 	non-<code>null</code> filter fields
	 * @param secondaryFilterName optional attribute name for a secondary filter.
	 * @param secondaryFilterValue optional attribute value for a secondary filter.
	 * @return list of causality data matching the specified criteria.
	 */
	public List<CausalityData> getCausalityData(int evidenceId, 
			List<String> returnAttributes, List<String> primaryFilterNamesNull,
			List<String> primaryFilterNamesNotNull, List<String> primaryFilterValues,
			String secondaryFilterName, String secondaryFilterValue) throws RemoteException;

	
	/**
	 * For the incident containing the specified item of evidence, the method returns
	 * a page of notifications with the same data type and description (if
	 * <code>singleDescription</code> is true), whose top row will match the
	 * supplied time.
	 * @param singleDescription <code>true</code> if only evidence with the same description 
	 * 	and data type as <code>evidenceId</code> should be returned, <code>false</code> otherwise.
	 * @param evidenceId the id of an item of evidence from an incident with
	 * 		which to match data type and description.
	 * @param time value of the time column to match on for the first row of 
	 * evidence data that will be returned.
	 * @param filterAttributes optional attribute names on which to filter the query.
	 * @param filterValues optional attribute values on which to filter the query.
	 * @param pageSize the page size - the maximum number of data items to return.
	 * @return List of Evidence records, sorted descending on time.
	 */
	public List<Evidence> getAtTime(boolean singleDescription, int evidenceId, Date time,
			List<String> filterAttributes, List<String> filterValues, int pageSize) throws RemoteException;

	
	/**
	 * For the incident containing the specified item of evidence, the method returns 
	 * the first page of notifications with the same data type and description
	 * (if <code>singleDescription</code> is true).
	 * @param singleDescription <code>true</code> if only evidence with the same description 
	 * 	and data type as <code>evidenceId</code> should be returned, <code>false</code> otherwise.
	 * @param evidenceId the id of an item of evidence from an incident with
	 * 		which to match data type and description.
	 * @param filterAttributes optional attribute names on which to filter the query.
	 * @param filterValues optional attribute values on which to filter the query.
	 * @param pageSize the page size - the maximum number of data items to return.
	 * @return List of Evidence records comprising the first page of data.
	 */
	public List<Evidence> getFirstPage(boolean singleDescription, int evidenceId, 
			List<String> filterAttributes, List<String> filterValues, int pageSize) throws RemoteException;

	
	/**
	 * For the incident containing the specified item of evidence, the method returns 
	 * the last page of notifications with the same data type and description
	 * (if <code>singleDescription</code> is true).
	 * @param singleDescription <code>true</code> if only evidence with the same description 
	 * 	and data type as <code>evidenceId</code> should be returned, <code>false</code> otherwise.
	 * @param evidenceId the id of an item of evidence from an incident with
	 * 		which to match data type and description.
	 * @param filterAttributes optional attribute names on which to filter the query.
	 * @param filterValues optional attribute values on which to filter the query.
	 * @param pageSize the page size - the maximum number of data items to return.
	 * @return List of Evidence records comprising the first page of data.
	 */
	public List<Evidence> getLastPage(boolean singleDescription, int evidenceId, 
			List<String> filterAttributes, List<String> filterValues, int pageSize) throws RemoteException;

	
	/**
	 * Returns the next page of evidence data following on from the row with the
	 * specified time and id. The method will return notifications from the
	 * incident containing the specified item of evidence, with the same data type
	 * and description (if <code>singleDescription</code> is true).
	 * @param singleDescription <code>true</code> if only evidence with the same description 
	 * 	and data type as <code>evidenceId</code> should be returned, <code>false</code> otherwise.
	 * @param bottomRowId the value of the id column for the bottom row of evidence 
	 * 		in the current page.
	 * @param bottomRowTime the value of the time column for the bottom row of 
	 * 		evidence in the current page.
	 * @param filterAttributes optional attribute names on which to filter the query.
	 * @param filterValues optional attribute values on which to filter the query.
	 * @param pageSize the page size - the maximum number of data items to return.
	 * @return List of Evidence records comprising the next page of data.
	 */
	public List<Evidence> getNextPage(boolean singleDescription,
			int bottomRowId, Date bottomRowTime, 
			List<String> filterAttributes, List<String> filterValues, int pageSize) throws RemoteException;

	
	/**
	 * Returns the previous page of evidence data following on from the row with the
	 * specified time and id. The method will return notifications from the
	 * incident containing the specified item of evidence, with the same data type
	 * and description (if <code>singleDescription</code> is true).
	 * @param singleDescription <code>true</code> if only evidence with the same description 
	 * 	and data type as <code>evidenceId</code> should be returned, <code>false</code> otherwise.
	 * @param topRowId the value of the id column for the top row of evidence 
	 * 		in the current page.
	 * @param topRowTime the value of the time column for the top row of 
	 * 		evidence in the current page.
	 * @param filterAttributes optional attribute names on which to filter the query.
	 * @param filterValues optional attribute values on which to filter the query.
	 * @param pageSize the page size - the maximum number of data items to return.
	 * @return List of Evidence records comprising the next page of data.
	 */
	public List<Evidence> getPreviousPage(boolean singleDescription,
			int topRowId, Date topRowTime, 
			List<String> filterAttributes, List<String> filterValues, int pageSize) throws RemoteException;

	
	/**
	 * For the incident containing the specified item of evidence, the method returns 
	 * the earliest notification with the same data type and description (if
	 * <code>singleDescription</code> is true).
	 * @param singleDescription <code>true</code> if only evidence with the same description 
	 * 	and data type as <code>evidenceId</code> should be considered, <code>false</code> otherwise.
	 * @param evidenceId the id of an item of evidence from an incident with
	 * 		which to match data type and description.
	 * @param filterAttributes optional attribute names on which to filter the query.
	 * @param filterValues optional attribute values on which to filter the query.
	 * @return earliest evidence record with same data type and description.
	 */
	public Evidence getEarliestEvidence(boolean singleDescription, int evidenceId,
			List<String> filterAttributes, List<String> filterValues) throws RemoteException;

	
	/**
	 * For the incident containing the specified item of evidence, the method returns 
	 * the latest notification with the same data type and description (if
	 * <code>singleDescription</code> is true).
	 * @param singleDescription <code>true</code> if only evidence with the same description 
	 * 	and data type as <code>evidenceId</code> should be considered, <code>false</code> otherwise.
	 * @param evidenceId the id of an item of evidence from an incident with
	 * 		which to match data type and description.
	 * @param filterAttributes optional attribute names on which to filter the query.
	 * @param filterValues optional attribute values on which to filter the query.
	 * @return latest evidence record with same data type and description.
	 */
	public Evidence getLatestEvidence(boolean singleDescription, int evidenceId,
			List<String> filterAttributes, List<String> filterValues) throws RemoteException;

}
