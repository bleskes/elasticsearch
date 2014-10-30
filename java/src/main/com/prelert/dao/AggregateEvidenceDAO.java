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

import com.prelert.data.TimeFrame;
import com.prelert.data.Evidence;


/**
 * Interface defining the methods to be implemented by a Data Access Object
 * to obtain information for aggregate Evidence views, such as for History Views
 * showing numbers of notifications by the day, hour or minute.
 * 
 * @author Pete Harverson
 */
public interface AggregateEvidenceDAO
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
	 * Returns the first page of evidence data for a view with the specified
	 * time frame.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param timeFrame time frame for which to obtain the first page of records.
	 * @param description evidence description for which to obtain data,
	 * 	or <code>null</code> to return data across all descriptions.
	 * @return List of Evidence records comprising the first page of data.
	 */
	public List<Evidence> getFirstPage(String dataType, String source, 
			TimeFrame timeFrame, String description);
	
	
	/**
	 * Returns the last page of evidence data for a view with the specified
	 * time frame.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param timeFrame time frame for which to obtain the last page of records.
	 * @param description evidence description for which to obtain data,
	 * 	or <code>null</code> to return data across all descriptions.
	 * @return List of Evidence records comprising the last page of data.
	 */
	public List<Evidence> getLastPage(String dataType, String source, 
			TimeFrame timeFrame, String description);
	
	
	/**
	 * Returns the next page of evidence data following on from the row with the
	 * specified time and id.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param timeFrame time frame for which to obtain the next page of records.
	 * @param bottomRowTime the value of the time column for the bottom row of 
	 * evidence in the current page.
	 * @param bottomRowDesc the value of the description column for the bottom row of 
	 * data in the current page.
	 * @param isFiltered <code>true</code> if only evidence data matching the description
	 * of the bottom row should be returned, or <code>false</code> to return data across
	 * all descriptions.
	 * @return List of Evidence records comprising the next page of data.
	 */
	public List<Evidence> getNextPage(
			String dataType, String source, TimeFrame timeFrame, Date bottomRowTime, 
			String bottomRowDesc, boolean isFiltered);
	
	
	/**
	 * Returns the previous page of evidence data to the row with the specified 
	 * time and id.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param timeFrame time frame for which to obtain the previous page of records.
	 * @param topRowTime the value of the time column for the top row of 
	 * evidence in the current page.
	 * @param topRowDesc the value of the description column for the top row of 
	 * data in the current page.
	 * @param isFiltered <code>true</code> if only evidence data matching the description
	 * of the bottom row should be returned, or <code>false</code> to return data across
	 * all descriptions.
	 * @return List of Evidence records comprising the previous page of data.
	 */
	public List<Evidence> getPreviousPage(
			String dataType, String source, TimeFrame timeFrame, Date topRowTime, 
			String topRowDesc, boolean isFiltered);
	
	
	/**
	 * Returns a page of evidence data, whose top row will match the supplied time.
	 * @param dataType identifier for the type of evidence data.
	 * @param source name of the source (server) for which to obtain evidence data
	 * 	or <code>null</code> to return data from all sources.
	 * @param timeFrame time frame for which to obtain evidence records.
	 * @param time value of the time column to match on for the first row of 
	 * evidence data that will be returned.
	 * @param description evidence description for which to obtain data,
	 * 	or <code>null</code> to return data across all descriptions.
	 * @return List of Evidence records, sorted descending on time.
	 */
	public List<Evidence> getAtTime(String dataType, String source, 
			TimeFrame timeFrame, Date time, String description);
	
	
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
