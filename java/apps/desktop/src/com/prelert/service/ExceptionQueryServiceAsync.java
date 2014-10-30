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

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.prelert.data.DatePagingLoadResult;
import com.prelert.data.EventRecord;
import com.prelert.data.ExceptionPagingLoadConfig;


/**
 * Defines the methods to be implemented by the asynchronous client interface
 * to the Evidence query service.
 * @author Pete Harverson
 */
public interface ExceptionQueryServiceAsync
{
	/**
	 * Returns the first page of evidence data matching the specified load config.
	 * @param config load config specifying the range of data (i.e. noise level
	 * 		and time window) to obtain.
	 * @param callback object to be notified when the asynchronous call completes.
	 */
	public void getFirstPage(ExceptionPagingLoadConfig config, 
			AsyncCallback<DatePagingLoadResult<EventRecord>> callback);
	
	
	/**
	 * Returns the last page of evidence data matching the specified load config.
	 * @param config load config specifying the range of data (i.e. noise level
	 * 		and time window) to obtain.
	 * @param callback object to be notified when the asynchronous call completes.
	 */
	public void getLastPage(ExceptionPagingLoadConfig config, 
			AsyncCallback<DatePagingLoadResult<EventRecord>> callback);
	
	
	/**
	 * Returns the next page of evidence data following on from the row with the
	 * specified id.
	 * @param config load config specifying the range of data to obtain. The date
	 * in this DatePagingLoadConfig must correspond to the value of the time column 
	 * for the bottom row of evidence in the current page.
	 * @param bottomRowId the value of the id column for the bottom row of evidence 
	 * in the current page.
	 * @param callback object to be notified when the asynchronous call completes.
	 */
	public void getNextPage(
			ExceptionPagingLoadConfig config, String bottomRowId, 
			AsyncCallback<DatePagingLoadResult<EventRecord>> callback);
	
	
	/**
	 * Returns the previous page of evidence data to the row with the specified id.
	 * @param config load config specifying the range of data to obtain. The date
	 * in this DatePagingLoadConfig must correspond to the value of the time column 
	 * for the top row of evidence in the current page.
	 * @param topRowId the value of the id column for the top row of evidence 
	 * in the current page.
	 * @param callback object to be notified when the asynchronous call completes.
	 */
	public void getPreviousPage(
			ExceptionPagingLoadConfig config, String topRowId, 
			AsyncCallback<DatePagingLoadResult<EventRecord>> callback);
	
	
	/**
	 * Returns a page of evidence data, whose top row will match the date in
	 * the supplied config.
	 * @param config load config specifying the range of data to obtain. The date
	 * in this DatePagingLoadConfig will correspond to the value of the time column 
	 * for the first row of evidence data that will be returned.
	 * @param callback object to be notified when the asynchronous call completes.
	 */
	public void getAtTime(ExceptionPagingLoadConfig config, 
			AsyncCallback<DatePagingLoadResult<EventRecord>> callback);
	
	
	/**
	 * Returns a list of all of the columns in an Exception List.
	 * @param dataType identifier for the type of evidence data.
	 * @return list of all of the columns for an Exception List.
	 */
	public void getAllColumns(String dataType, AsyncCallback<List<String>> callback);
}
