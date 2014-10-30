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

import com.prelert.data.EventRecord;
import com.prelert.data.TimeFrame;

public interface ExceptionViewDAO
{
	
	/**
	 * Returns a list of all of the column names to be displayed in an Exception List.
	 * @param dataType identifier for the type of evidence data.
	 * @return List of column names for an Exception List.
	 */
	public List<String> getAllColumns(String dataType);
	
	
	/**
	 * Returns the first page of evidence data for a view with the specified
	 * time, noise level and time window.
	 * @param dataType identifier for the type of evidence data.
	 * @param time time defining the start and end time of the exception time
	 * 		window.
	 * @param noiseLevel noise filter, where a high value represents the most
	 * 		likely exceptions and a low value indicates all data items.
	 * @param timeWindow time window for the exception calculation 
	 * 		e.g. MINUTE, HOUR or DAY.
	 * @return List of Evidence records comprising the first page of data.
	 */
	public List<EventRecord> getFirstPage(
			String dataType, Date time, int noiseLevel, TimeFrame timeWindow);
	
	
	/**
	 * Returns the last page of evidence data for a view with the specified
	 * time, noise level and time window.
	 * @param dataType identifier for the type of evidence data.
	 * @param time time defining the start and end time of the exception time
	 * 		window.
	 * @param noiseLevel noise filter, where a high value represents the most
	 * 		likely exceptions and a low value indicates all data items.
	 * @param timeWindow time window for the exception calculation 
	 * 		e.g. MINUTE, HOUR or DAY.
	 * @return List of Evidence records comprising the last page of data.
	 */
	public List<EventRecord> getLastPage(
			String dataType, Date time, int noiseLevel, TimeFrame timeWindow);
	
	
	/**
	 * Returns the next page of evidence data following on from the row with the
	 * specified time and id.
	 * @param dataType identifier for the type of evidence data.
	 * @param bottomRowTime the value of the time column for the bottom row of 
	 * evidence in the current page.
	 * @param bottomRowId the value of the id column for the bottom row of evidence 
	 * in the current page.
	 * @param noiseLevel noise filter, where a high value represents the most
	 * 		likely exceptions and a low value indicates all data items.
	 * @param timeWindow time window for the exception calculation 
	 * 		e.g. MINUTE, HOUR or DAY.
	 * @return List of Evidence records comprising the next page of data.
	 */
	public List<EventRecord> getNextPage(
			String dataType, Date bottomRowTime, String bottomRowId,
			int noiseLevel, TimeFrame timeWindow);
	
	
	/**
	 * Returns the previous page of evidence data to the row with the specified 
	 * time and id.
	 * @param dataType identifier for the type of evidence data.
	 * @param topRowTime the value of the time column for the top row of 
	 * evidence in the current page.
	 * @param topRowId the value of the id column for the top row of evidence 
	 * in the current page.
	 * @param noiseLevel noise filter, where a high value represents the most
	 * 		likely exceptions and a low value indicates all data items.
	 * @param timeWindow time window for the exception calculation 
	 * 		e.g. MINUTE, HOUR or DAY.
	 * @return List of Evidence records comprising the previous page of data.
	 */
	public List<EventRecord> getPreviousPage(
			String dataType, Date topRowTime, String topRowId,
			int noiseLevel, TimeFrame timeWindow);
	
	
	/**
	 * Returns a page of evidence data, whose top row will match the supplied time.
	 * @param dataType identifier for the type of evidence data.
	 * @param time value of the time column to match on for the first row of 
	 * evidence data that will be returned.
	 * @param noiseLevel noise filter, where a high value represents the most
	 * 		likely exceptions and a low value indicates all data items.
	 * @param timeWindow time window for the exception calculation 
	 * 		e.g. MINUTE, HOUR or DAY.
	 * @return List of Evidence records, sorted descending on time.
	 */
	public List<EventRecord> getAtTime(
			String dataType, Date time, int noiseLevel, TimeFrame timeWindow);
	
	
	/**
	 * Returns the date of the earliest evidence record in the Prelert database
	 * for the given time, noise level and time window.
	 * @param dataType identifier for the type of evidence data.
	 * @param time time defining the start and end time of the exception time
	 * 		window.
	 * @param noiseLevel noise filter, where a high value represents the most
	 * 		likely exceptions and a low value indicates all data items.
	 * @param timeWindow time window for the exception calculation 
	 * 		e.g. MINUTE, HOUR or DAY.
	 * @return date of earliest evidence record.
	 */
	public Date getEarliestDate(
			String dataType, Date time, int noiseLevel, TimeFrame timeWindow);

	
	/**
	 * Returns the date of the latest evidence record in the Prelert database
	 * for the given time, noise level and time window.
	 * @param dataType identifier for the type of evidence data.
	 * @param time time defining the start and end time of the exception time
	 * 		window.
	 * @param noiseLevel noise filter, where a high value represents the most
	 * 		likely exceptions and a low value indicates all data items.
	 * @param timeWindow time window for the exception calculation 
	 * 		e.g. MINUTE, HOUR or DAY.
	 * @return date of latest evidence record.
	 */
	public Date getLatestDate(
			String dataType, Date time, int noiseLevel, TimeFrame timeWindow);
}
