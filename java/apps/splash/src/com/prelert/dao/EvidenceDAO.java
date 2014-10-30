/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2009     *
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

import com.prelert.data.TimeFrame;
import com.prelert.data.gxt.AttributeModel;
import com.prelert.data.gxt.EvidenceModel;


/**
 * Interface defining the methods to be implemented by a Data Access Object
 * to obtain information for Evidence views from information stored in 
 * the Prelert database.
 * 
 * @author Pete Harverson
 */
public interface EvidenceDAO
{
	/**
	 * Returns a list of all of the column names which are available for an
	 * Evidence view with the specified time frame.
	 * @param dataType identifier for the type of evidence data.
	 * @param time frame for which to obtain the columns.
	 * @return List of column names for the given Evidence View.
	 */
	public List<String> getAllColumns(String dataType, TimeFrame timeFrame);
	
	
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
	 * Returns the first page of evidence data for a view with the specified
	 * time frame.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param timeFrame time frame for which to obtain the first page of records.
	 * @param filterAttributes optional attribute names on which to filter the query.
	 * @param filterValues optional attribute values on which to filter the query.
	 * @return List of Evidence records comprising the first page of data.
	 */
	public List<EvidenceModel> getFirstPage(String dataType, String source, 
			TimeFrame timeFrame, List<String> filterAttributes, List<String> filterValues);
	
	
	/**
	 * Returns the last page of evidence data for a view with the specified
	 * time frame.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param timeFrame time frame for which to obtain the last page of records.
	 * @param filterAttributes optional attribute names on which to filter the query.
	 * @param filterValues optional attribute values on which to filter the query.
	 * @return List of Evidence records comprising the last page of data.
	 */
	public List<EvidenceModel> getLastPage(String dataType, String source, 
			TimeFrame timeFrame, List<String> filterAttributes, List<String> filterValues);
	
	
	/**
	 * Returns the next page of evidence data following on from the row with the
	 * specified time and id.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param timeFrame time frame for which to obtain the next page of records.
	 * @param bottomRowTime the value of the time column for the bottom row of 
	 * evidence in the current page.
	 * @param bottomRowId the value of the id column for the bottom row of evidence 
	 * in the current page.
	 * @param filterAttributes optional attribute names on which to filter the query.
	 * @param filterValues optional attribute values on which to filter the query.
	 * @return List of Evidence records comprising the next page of data.
	 */
	public List<EvidenceModel> getNextPage(
			String dataType, String source, TimeFrame timeFrame, Date bottomRowTime, 
			String bottomRowId, List<String> filterAttributes, List<String> filterValues);
	
	
	/**
	 * Returns the previous page of evidence data to the row with the specified 
	 * time and id.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param timeFrame time frame for which to obtain the previous page of records.
	 * @param topRowTime the value of the time column for the top row of 
	 * evidence in the current page.
	 * @param topRowId the value of the id column for the top row of evidence 
	 * in the current page.
	 * @param filterAttributes optional attribute names on which to filter the query.
	 * @param filterValues optional attribute values on which to filter the query.
	 * @return List of Evidence records comprising the previous page of data.
	 */
	public List<EvidenceModel> getPreviousPage(
			String dataType, String source, TimeFrame timeFrame, Date topRowTime, 
			String topRowId, List<String> filterAttributes, List<String> filterValues);
	
	
	/**
	 * Returns a page of evidence data, the top row of which matches the specified
	 * description and time.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param timeFrame time frame for which to obtain evidence records.
	 * @param time value of the time column to match on for the first row of 
	 * evidence data that will be returned.
	 * @param description the value of the description column to match on for the
	 * top row of evidence data that will be returned.
	 * @return List of Evidence records, sorted descending on time.
	 */
	public List<EvidenceModel> getForDescription(String dataType, String source, 
			TimeFrame timeFrame, Date time, String description);
	
	
	/**
	 * Returns a page of evidence data, whose top row will match the supplied time.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param timeFrame time frame for which to obtain evidence records.
	 * @param time value of the time column to match on for the first row of 
	 * evidence data that will be returned.
	 * @param filterAttributes optional attribute names on which to filter the query.
	 * @param filterValues optional attribute values on which to filter the query.
	 * @return List of Evidence records, sorted descending on time.
	 */
	public List<EvidenceModel> getAtTime(String dataType, String source, 
			TimeFrame timeFrame, Date time,
			List<String> filterAttributes, List<String> filterValues);
	
	
	/**
	 * Returns a page of real-time (SECOND time frame) evidence data, 
	 * the top row of which matches the specified id.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param id evidence id for the top row of evidence data that will be returned.
	 * @return List of Evidence records, sorted descending on time.
	 */
	public List<EvidenceModel> getIdPage(String dataType, String source, int id);
	
	
	/**
	 * Runs a search to return the first page of data where the specified text 
	 * is contained within one or more of the evidence attributes.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param containsText String to search for within the evidence attributes.
	 * @return List of Evidence records comprising the first page of data.
	 */
	public List<EvidenceModel> searchFirstPage(
			String dataType, String source, String containsText);
	
	
	/**
	 * Runs a search to return the last page of data where the specified text 
	 * is contained within one or more of the evidence attributes.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param containsText String to search for within the evidence attributes.
	 * @return List of Evidence records comprising the last page of data.
	 */
	public List<EvidenceModel> searchLastPage(
			String dataType, String source, String containsText);
	
	
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
	 * @return List of Evidence records comprising the next page of data.
	 */
	public List<EvidenceModel> searchNextPage(
			String dataType, String source, Date bottomRowTime, 
			int bottomRowId, String containsText);
	
	
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
	 * @return List of Evidence records comprising the previous page of data.
	 */
	public List<EvidenceModel> searchPreviousPage(
			String dataType, String source, Date topRowTime, 
			int topRowId, String containsText);
	
	
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
	 * @return List of Evidence records, sorted descending on time.
	 */
	public List<EvidenceModel> searchAtTime(String dataType, String source, 
			Date time, String containsText);
	
	
	/**
	 * Returns data for a single item of evidence with the specified id.
	 * @param id the unique identifier for the item of evidence.
	 * @return full data model for the evidence item with the specified id.
	 */
	public EvidenceModel getEvidenceSingle(int id);
	
	
	/**
	 * Returns all attributes for the item of evidence with the specified id 
  	 * (instead of just the current display columns).
	 * @param id the value of the id column for the evidence to obtain information on.
	 * @return List of AttributeModel objects for the row with the specified id.
	 */
	public List<AttributeModel> getEvidenceAttributes(int id);
	
	
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
	public EvidenceModel getEarliestEvidence(String dataType, String source,
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
	public EvidenceModel getLatestEvidence(String dataType, String source,
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
	public EvidenceModel searchEarliestEvidence(
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
	public EvidenceModel searchLatestEvidence(
			String dataType, String source, String containsText);
	
	
	/**
	 * Returns the page size being used for queries.
	 * @return the page size.
	 */
	public int getPageSize();
	
	
	/**
	 * Sets the page size to be used for queries.
	 * @param pageSize the page size.
	 */
	public void setPageSize(int pageSize);
}
