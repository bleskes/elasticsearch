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

import com.prelert.data.Evidence;
import com.prelert.data.Attribute;
import com.prelert.data.MetricPath;



/**
 * Interface defining the methods to be implemented by a Data Access Object
 * to obtain information for Evidence views.
 * 
 * @author Pete Harverson
 */
public interface EvidenceDAO
{
	/**
	 * Returns a list of all of the column names which are available for an
	 * Evidence view with the specified data type.
	 * @param dataType identifier for the type of evidence data.
	 * @return List of column names for the given data type.
	 */
	public List<String> getAllColumns(String dataType);
	
	
	/**
	 * Returns a list of the names of the attributes that support filtering.
	 * @param dataType identifier for the type of evidence data.
	 * @param getCompulsory <code>true</code> to return compulsory columns, 
	 * 		<code>false</code> otherwise.
	 * @param getOptional <code>true</code> to return optional columns, 
	 * 		<code>false</code> otherwise.
	 * @return a list of the filterable attributes.
	 */
	public List<String> getFilterableColumns(String dataType, 
			boolean getCompulsory, boolean getOptional);
	
	
	/**
	 * Returns the list of values in the evidence table for the specified column.
	 * @param dataType identifier for the type of evidence data.
	 * @param columnName name of the column for which to return the values.
	 * @param maxRows maximum number of values to return.
	 * @return list of all the distinct values in the evidence table for the
	 * 			given column.
	 */
	public List<String> getColumnValues(String dataType, String columnName, int maxRows);
	
	
	/**
	 * Returns the first page of evidence data with the specified properties.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param filterAttributes optional attribute names on which to filter the query.
	 * @param filterValues optional attribute values on which to filter the query.
	 * @param pageSize the page size - the maximum number of data items to return.
	 * @return List of Evidence records comprising the first page of data.
	 */
	public List<Evidence> getFirstPage(String dataType, String source, 
			List<String> filterAttributes, List<String> filterValues, int pageSize);
	
	/**
	 * Returns the last page of evidence data with the specified properties.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param filterAttributes optional attribute names on which to filter the query.
	 * @param filterValues optional attribute values on which to filter the query.
	 * @param pageSize the page size - the maximum number of data items to return.
	 * @return List of Evidence records comprising the last page of data.
	 */
	public List<Evidence> getLastPage(String dataType, String source, 
			List<String> filterAttributes, List<String> filterValues, int pageSize);
	
	
	/**
	 * Returns the next page of evidence data following on from the row with the
	 * specified time and id.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param bottomRowTime the value of the time column for the bottom row of 
	 * evidence in the current page.
	 * @param bottomRowId the value of the id column for the bottom row of evidence 
	 * in the current page.
	 * @param filterAttributes optional attribute names on which to filter the query.
	 * @param filterValues optional attribute values on which to filter the query.
	 * @param pageSize the page size - the maximum number of data items to return.
	 * @return List of Evidence records comprising the next page of data.
	 */
	public List<Evidence> getNextPage(
			String dataType, String source, Date bottomRowTime, int bottomRowId, 
			List<String> filterAttributes, List<String> filterValues, int pageSize);

	
	/**
	 * Returns the previous page of evidence data to the row with the specified 
	 * time and id.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param topRowTime the value of the time column for the top row of 
	 * evidence in the current page.
	 * @param topRowId the value of the id column for the top row of evidence 
	 * in the current page.
	 * @param filterAttributes optional attribute names on which to filter the query.
	 * @param filterValues optional attribute values on which to filter the query.
	 * @param pageSize the page size - the maximum number of data items to return.
	 * @return List of Evidence records comprising the previous page of data.
	 */
	public List<Evidence> getPreviousPage(
			String dataType, String source, Date topRowTime, int topRowId, 
			List<String> filterAttributes, List<String> filterValues, int pageSize);
	
	
	/**
	 * Returns a page of evidence data, whose top row will match the supplied time.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param time value of the time column to match on for the first row of 
	 * evidence data that will be returned.
	 * @param filterAttributes optional attribute names on which to filter the query.
	 * @param filterValues optional attribute values on which to filter the query.
	 * @param pageSize the page size - the maximum number of data items to return.
	 * @return List of Evidence records, sorted descending on time.
	 */
	public List<Evidence> getAtTime(String dataType, String source, Date time, 
			List<String> filterAttributes, List<String> filterValues, int pageSize);

	
	/**
	 * Returns a page of evidence data, the top row of which matches the specified id.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param id evidence id for the top row of evidence data that will be returned.
	 * @param pageSize the page size - the maximum number of data items to return.
	 * @return List of Evidence records, sorted descending on time.
	 */
	public List<Evidence> getIdPage(String dataType, String source, int id, int pageSize);
	
	
	/**
	 * Runs a search to return the first page of data where the specified text 
	 * is contained within one or more of the evidence attributes.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param containsText String to search for within the evidence attributes.
	 * @param pageSize the page size - the maximum number of data items to return.
	 * @return List of Evidence records comprising the first page of data.
	 */
	public List<Evidence> searchFirstPage(
			String dataType, String source, String containsText, int pageSize);
	
	
	/**
	 * Runs a search to return the last page of data where the specified text 
	 * is contained within one or more of the evidence attributes.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param containsText String to search for within the evidence attributes.
	 * @param pageSize the page size - the maximum number of data items to return.
	 * @return List of Evidence records comprising the last page of data.
	 */
	public List<Evidence> searchLastPage(
			String dataType, String source, String containsText, int pageSize);
	
	
	/**
	 * Runs a search to return the next page of data following on from the row 
	 * with the specified time and id, where the specified text is contained
	 * within one or more of the evidence attributes.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param bottomRowTime the value of the time column for the bottom row of 
	 * evidence in the current page.
	 * @param bottomRowId the value of the id column for the bottom row of evidence 
	 * in the current page.
	 * @param containsText String to search for within the evidence attributes.
	 * @param pageSize the page size - the maximum number of data items to return.
	 * @return List of Evidence records comprising the next page of data.
	 */
	public List<Evidence> searchNextPage(
			String dataType, String source, Date bottomRowTime, 
			int bottomRowId, String containsText, int pageSize);
	
	
	/**
	 * Runs a search to return the previous page of data to the row with the specified 
	 * time and id, where the specified text is contained within one or more of the 
	 * evidence attributes.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param topRowTime the value of the time column for the top row of 
	 * evidence in the current page.
	 * @param topRowId the value of the id column for the top row of evidence 
	 * in the current page.
	 * @param containsText String to search for within the evidence attributes.
	 * @param pageSize the page size - the maximum number of data items to return.
	 * @return List of Evidence records comprising the previous page of data.
	 */
	public List<Evidence> searchPreviousPage(
			String dataType, String source, Date topRowTime, 
			int topRowId, String containsText, int pageSize);
	
	
	/**
	 * Runs a search to return a page of evidence data, whose top row will match 
	 * the supplied time and where the specified text is contained within one or 
	 * more of the evidence attributes.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param time value of the time column to match on for the first row of 
	 * evidence data that will be returned.
	 * @param containsText String to search for within the evidence attributes.
	 * @param pageSize the page size - the maximum number of data items to return.
	 * @return List of Evidence records, sorted descending on time.
	 */
	public List<Evidence> searchAtTime(String dataType, String source, 
			Date time, String containsText, int pageSize);
	
	
	/**
	 * Returns data for a single item of evidence with the specified id.
	 * @param id the unique identifier for the item of evidence.
	 * @return full data model for the item with the specified id,
	 * 	 or <code>null</code> if no evidence exists with this id.
	 */
	public Evidence getEvidenceSingle(int id);
	
	
	/**
	 * Returns all attributes for the item of evidence with the specified id 
  	 * (instead of just the current display columns).
	 * @param id the value of the id column for the evidence to obtain information on.
	 * @return List of Attribute objects for the row with the specified id.
	 */
	public List<Attribute> getEvidenceAttributes(int id);
	
	
	/**
	 * Returns the earliest evidence record in the Prelert database for the given
	 * properties.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param filterAttributes optional attribute names on which to filter the query.
	 * @param filterValues optional attribute values on which to filter the query.
	 * @return earliest evidence record for the specified properties.
	 */
	public Evidence getEarliestEvidence(String dataType, String source,
			List<String> filterAttributes, List<String> filterValues);
	
	
	/**
	 * Returns the latest evidence record in the Prelert database for the given
	 * properties.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param filterAttributes optional attribute names on which to filter the query.
	 * @param filterValues optional attribute values on which to filter the query.
	 * @return latest evidence record for the specified properties.
	 */
	public Evidence getLatestEvidence(String dataType, String source,
			List<String> filterAttributes, List<String> filterValues);
	
	
	/**
	 * Runs a search to return the earliest evidence record where the 
	 * specified text is contained within one or more of the evidence attributes.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param containsText String to search for within the evidence attributes.
	 * @return earliest evidence record containing specified text.
	 */
	public Evidence searchEarliestEvidence(
			String dataType, String source, String containsText);

	
	/**
	 * Runs a search to return the latest evidence record where the 
	 * specified text is contained within one or more of the evidence attributes.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param containsText String to search for within the evidence attributes.
	 * @return latest evidence record containing specified text.
	 */
	public Evidence searchLatestEvidence(
			String dataType, String source, String containsText);
	
	
	/**
	 * For the Time Series which can be uniquely defined by <code>datatype</code>,
	 * <code>metric</code>, <code>source</code> and <code>attributes</code> this 
	 * function returns any evidence features which occurred between the time period 
	 * <code>startTime</code> and <code>endTime</code>.
	 * 
	 * @param datatype 
	 * @param metric
	 * @param source
	 * @param attributes Filter by attributes
	 * @param startTime
	 * @param endTime
	 * @return List of <code>Evidence</code> features, if any, for the time series 
	 * defined by datatype, metric, source in the time period.
	 * The only the <code>Id</code> and <code>Time</code> fields will be populated
	 * in the returned <code>Evidence</code> objects. 
	 */
	public List<Evidence> getEvidenceInTimeSeries(String datatype, String metric, 
												String source, List<Attribute> attributes,
												Date startTime, Date endTime);	
	
	/**
	 * For the External Time Series which can be uniquely defined by <code>datatype</code>,
	 * <code>metric</code>, <code>source</code> and <code>attributes</code> this 
	 * function returns any evidence features which occurred between the time period 
	 * <code>startTime</code> and <code>endTime</code>.
	 * 
	 * @param datatype 
	 * @param metric
	 * @param source
	 * @param attributes Filter by attributes
	 * @param startTime
	 * @param endTime
	 * @return List of <code>Evidence</code> features, if any, for the time series 
	 * defined by datatype, metric, source in the time period.
	 * The only the <code>Id</code> and <code>Time</code> fields will be populated
	 * in the returned <code>Evidence</code> objects. 
	 */
	public List<Evidence> getEvidenceInExternalTimeSeries(String datatype, String metric, 
												String source, List<Attribute> attributes,
												Date startTime, Date endTime);	
	
	
	/**
	 * If evidenceId corresponds to a time series feature this function
	 * will return the metric path for that time series.
	 * If the metric path cannot be found <code>null</code> is
	 * returned. 
	 * 
	 * @param evidenceId - The unique evidence id.
	 * @return MetricPath or <code>null</code>
	 */
	public MetricPath getMetricPathFromEvidenceId(int evidenceId);

}
