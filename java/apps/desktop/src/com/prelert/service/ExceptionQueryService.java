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

package com.prelert.service;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import com.prelert.data.DatePagingLoadResult;
import com.prelert.data.EventRecord;
import com.prelert.data.ExceptionPagingLoadConfig;
import com.prelert.data.TimeFrame;


/**
 * Defines the methods for the interface to the Exception View query service
 * for retrieving evidence items which are 'exceptions'.
 * @author Pete Harverson
 */
public interface ExceptionQueryService extends RemoteService
{
	/**
	 * Returns the first page of evidence data matching the specified load config.
	 * @param config load config specifying the range of data (i.e. noise level
	 * 		and time window) to obtain.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EventRecord> getFirstPage(ExceptionPagingLoadConfig config);
	
	
	/**
	 * Returns the last page of evidence data matching the specified load config.
	 * @param config load config specifying the range of data (i.e. noise level
	 * 		and time window) to obtain.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EventRecord> getLastPage(ExceptionPagingLoadConfig config);
	
	
	/**
	 * Returns the next page of evidence data following on from the row with the
	 * specified id.
	 * @param config load config specifying the range of data to obtain. The date
	 * in this DatePagingLoadConfig must correspond to the value of the time column 
	 * for the bottom row of evidence in the current page.
	 * @param bottomRowId the value of the id column for the bottom row of evidence 
	 * in the current page.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EventRecord> getNextPage(
			ExceptionPagingLoadConfig config, String bottomRowId);
	
	
	/**
	 * Returns the previous page of evidence data to the row with the specified id.
	 * @param config load config specifying the range of data to obtain. The date
	 * in this DatePagingLoadConfig must correspond to the value of the time column 
	 * for the top row of evidence in the current page.
	 * @param topRowId the value of the id column for the top row of evidence 
	 * in the current page.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EventRecord> getPreviousPage(
			ExceptionPagingLoadConfig config, String topRowId);
	
	
	/**
	 * Returns a page of evidence data, whose top row will match the date in
	 * the supplied config.
	 * @param config load config specifying the range of data to obtain. The date
	 * in this DatePagingLoadConfig will correspond to the value of the time column 
	 * for the first row of evidence data that will be returned.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EventRecord> getAtTime(ExceptionPagingLoadConfig config);
	
	
	/**
	 * Returns a list of all of the columns in an Exception List.
	 * @param dataType identifier for the type of evidence data.
	 * @return list of all of the columns for an Exception List.
	 */
	public List<String> getAllColumns(String dataType);
}
