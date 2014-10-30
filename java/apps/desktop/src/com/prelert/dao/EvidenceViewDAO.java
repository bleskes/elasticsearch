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

import java.util.*;

import com.prelert.data.*;
import com.prelert.data.gxt.GridRowInfo;


/**
 * Interface defining the methods to be implemented by a Data Access Object
 * to obtain information for Evidence views from information stored in 
 * the Prelert database.
 * 
 * @author Pete Harverson
 */
public interface EvidenceViewDAO
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
	 * Returns the total number of rows in the specified Evidence View.
	 * @param view Evidence View for which to obtain the row count.
	 * @return the row count for the database/view which the specified Evidence
	 * 			View is mapped to.
	 */
	public int getTotalRowCount(EvidenceView view);
	
	
	/**
	 * Returns the first page of evidence data for a view with the specified
	 * time frame.
	 * @param dataType identifier for the type of evidence data.
	 * @param timeFrame time frame for which to obtain the first page of records.
	 * @return List of Evidence records comprising the first page of data.
	 */
	public List<EventRecord> getFirstPage(String dataType, TimeFrame timeFrame, 
			String filterAttribute, String filterValue);
	
	
	/**
	 * Returns the last page of evidence data for a view with the specified
	 * time frame.
	 * @param dataType identifier for the type of evidence data.
	 * @param timeFrame time frame for which to obtain the last page of records.
	 * @return List of Evidence records comprising the last page of data.
	 */
	public List<EventRecord> getLastPage(String dataType, TimeFrame timeFrame,
			String filterAttribute, String filterValue);
	
	
	/**
	 * Returns the next page of evidence data following on from the row with the
	 * specified time and id.
	 * @param dataType identifier for the type of evidence data.
	 * @param timeFrame time frame for which to obtain the next page of records.
	 * @param bottomRowTime the value of the time column for the bottom row of 
	 * evidence in the current page.
	 * @param bottomRowId the value of the id column for the bottom row of evidence 
	 * in the current page.
	 * @return List of Evidence records comprising the next page of data.
	 */
	public List<EventRecord> getNextPage(
			String dataType, TimeFrame timeFrame, Date bottomRowTime, 
			String bottomRowId, String filterAttribute, String filterValue);
	
	
	/**
	 * Returns the previous page of evidence data to the row with the specified 
	 * time and id.
	 * @param dataType identifier for the type of evidence data.
	 * @param timeFrame time frame for which to obtain the previous page of records.
	 * @param topRowTime the value of the time column for the top row of 
	 * evidence in the current page.
	 * @param topRowId the value of the id column for the top row of evidence 
	 * in the current page.
	 * @return List of Evidence records comprising the previous page of data.
	 */
	public List<EventRecord> getPreviousPage(
			String dataType, TimeFrame timeFrame, Date topRowTime, 
			String topRowId,String filterAttribute, String filterValue);
	
	
	/**
	 * Returns a page of evidence data, the top row of which matches the specified
	 * description and time.
	 * @param dataType identifier for the type of evidence data.
	 * @param timeFrame time frame for which to obtain evidence records.
	 * @param time value of the time column to match on for the first row of 
	 * evidence data that will be returned.
	 * @param description the value of the description column to match on for the
	 * top row of evidence data that will be returned.
	 * @return List of Evidence records, sorted descending on time.
	 */
	public List<EventRecord> getForDescription(
			String dataType, TimeFrame timeFrame, Date time, String description);
	
	
	/**
	 * Returns a page of evidence data, whose top row will match the supplied time.
	 * @param dataType identifier for the type of evidence data.
	 * @param timeFrame time frame for which to obtain evidence records.
	 * @param time value of the time column to match on for the first row of 
	 * evidence data that will be returned.
	 * @return List of Evidence records, sorted descending on time.
	 */
	public List<EventRecord> getAtTime(String dataType, TimeFrame timeFrame, Date time,
			String filterAttribute, String filterValue);
	
	
	/**
	 * Returns a page of real-time (SECOND time frame) evidence data, 
	 * the top row of which matches the specified id.
	 * @param dataType identifier for the type of evidence data.
	 * @param id evidence id for the top row of evidence data that will be returned.
	 * @return List of Evidence records, sorted descending on time.
	 */
	public List<EventRecord> getIdPage(String dataType, int id);
	
	
	/**
	 * Returns all attributes for the row of evidence with the specified id 
  	 * (instead of just the current display columns).
	 * @param id the value of the id column for the row to obtain information on.
	 * @return List of GridRowInfo objects for the row with the specified id.
	 */
	public List<GridRowInfo> getRowInfo(int id);
	
	
	/**
	 * Returns the date of the earliest evidence record in the Prelert database
	 * for the given time frame.
	 * @param dataType identifier for the type of evidence data.
	 * @param filterAttribute attribute name on which to filter on, or
	 * <code>null</code> if no WHERE clause should be applied.
	 * @param filterValue attribute value to use as the filter in a WHERE clause.
	 * @return date of earliest evidence record for the specified time frame.
	 */
	public Date getEarliestDate(String dataType, 
			String filterAttribute, String filterValue);
	
	
	/**
	 * Returns the date of the latest evidence record in the Prelert database
	 * for the given time frame.
	 * @param dataType identifier for the type of evidence data.
	 * @param filterAttribute attribute name on which to filter on, or
	 * <code>null</code> if no WHERE clause should be applied.
	 * @param filterValue attribute value to use as the filter in a WHERE clause.
	 * @return date of latest evidence record for the specified time frame.
	 */
	public Date getLatestDate(String dataType, 
			String filterAttribute, String filterValue);
	
}
