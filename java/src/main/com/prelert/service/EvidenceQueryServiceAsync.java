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

package com.prelert.service;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.prelert.data.gxt.AttributeModel;
import com.prelert.data.gxt.DatePagingLoadResult;
import com.prelert.data.gxt.EvidenceModel;
import com.prelert.data.gxt.EvidencePagingLoadConfig;


/**
 * Defines the methods to be implemented by the asynchronous client interface
 * to the Evidence query service.
 * @author Pete Harverson
 */
public interface EvidenceQueryServiceAsync
{
	/**
	 * Returns the first page of evidence data matching the specified load config.
	 * @param config load config specifying the range of data (e.g. data type, source)
	 * to obtain.
	 * @param callback callback object to receive the page of evidence from the remote procedure call.
	 */
	public void getFirstPage(EvidencePagingLoadConfig config, 
			AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
	
	
	/**
	 * Returns the last page of evidence data matching the specified load config.
	 * @param config load config specifying the range of data (e.g. data type, source)
	 * to obtain.
	 * @param callback callback object to receive the page of evidence from the remote procedure call.
	 */
	public void getLastPage(EvidencePagingLoadConfig config, 
			AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
	
	
	/**
	 * Returns the next page of evidence data following on from the row in the 
	 * supplied load config.
	 * @param config load config specifying the range of data to obtain. The date
	 * and row id must correspond to the bottom row of evidence in the current page.
	 * @param callback callback object to receive the page of evidence from the remote procedure call.
	 */
	public void getNextPage(EvidencePagingLoadConfig config,  
			AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
	
	
	/**
	 * Returns the previous page of evidence data to the row in the supplied load
	 * config.
	 * @param config load config specifying the range of data to obtain. The date
	 * and row id must correspond to the top row of evidence in the current page.
	 * @param callback callback object to receive the page of evidence from the remote procedure call.
	 */
	public void getPreviousPage(EvidencePagingLoadConfig config,
			AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
	
	
	/**
	 * Returns a page of evidence data, whose top row will match the date in
	 * the supplied config.
	 * @param config load config specifying the range of data to obtain. The date
	 * in this DatePagingLoadConfig will correspond to the value of the time column 
	 * for the first row of evidence data that will be returned.
	 * @param callback callback object to receive the page of evidence from the remote procedure call.
	 */
	public void getAtTime(EvidencePagingLoadConfig config, 
			AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
	
	
	/**
	 * Returns a page of evidence data, the top row of which matches the row id in
	 * the supplied load config.
	 * @param config load config specifying the range of data to obtain 
	 * 	e.g. row id.
	 * @param callback callback object to receive the page of evidence from the remote procedure call.
	 */
	public void getIdPage(EvidencePagingLoadConfig config, 
			AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
	
	
	/**
	 * Runs a search to return the first page of evidence data where one or more
	 * attributes contain the text in the specified load config.
	 * @param config load config specifying the text to search for in evidence attributes.
	 * @param callback callback object to receive the page of evidence from the remote procedure call.
	 */
	public void searchFirstPage(EvidencePagingLoadConfig config, 
			AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
	
	
	/**
	 * Runs a search to return the last page of evidence data where one or more
	 * attributes contain the text in the specified load config.
	 * @param config load config specifying the text to search for in evidence attributes.
	 * @param callback callback object to receive the page of evidence from the remote procedure call.
	 */
	public void searchLastPage(EvidencePagingLoadConfig config, 
			AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
	
	
	/**
	 * Runs a search to return the next page of evidence data, following on from the row
	 * with the specified id, where one or more attributes contain the text in the 
	 * specified load config.
	 * @param config load config specifying the text to search for in evidence attributes.
	 * @param callback callback object to receive the page of evidence from the remote procedure call.
	 */
	public void searchNextPage(EvidencePagingLoadConfig config,
			AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
	
	
	/**
	 * Runs a search to return the previous page of evidence data to the one
	 * with the specified id, where one or more attributes contain the text in the 
	 * specified load config.
	 * @param config load config specifying the text to search for in evidence attributes.
	 * @param callback callback object to receive the page of evidence from the remote procedure call.
	 */
	public void searchPreviousPage(EvidencePagingLoadConfig config,
			AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
	
	
	/**
	 * Runs a search to return a page of evidence data, whose top row will match the date
	 * in the supplied config and where one or more attributes contain the text in the 
	 * specified load config.
	 * @param config load config specifying the range of data to obtain. The date
	 * in this DatePagingLoadConfig will correspond to the value of the time column 
	 * for the first row of evidence data that will be returned.
	 * @param callback callback object to receive the page of evidence from the remote procedure call.
	 */
	public void searchAtTime(EvidencePagingLoadConfig config, 
			AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
	
	
	/**
	 * Returns all attributes for the item of evidence with the specified id 
  	 * (instead of just the current display columns).
	 * @param id the value of the id column for the evidence to obtain information on.
	 * @param callback callback object to receive the list of attributes from the remote procedure call.
	 */
	public void getEvidenceAttributes(int rowId, 
			AsyncCallback<List<AttributeModel>> callback);
	
	
	/**
	 * Returns the data model for the single item of evidence with the given id.
	 * @param id id of the item of evidence to return.
	 * @param callback callback object to receive the item of evidence from the remote procedure call.
	 */
	public void getEvidenceSingle(int id,
			AsyncCallback<EvidenceModel> callback);
	
	
	/**
	 * Returns a list of all of the columns in an Evidence View for the specified
	 * data type.
	 * @param dataType identifier for the type of evidence data.
	 * @param callback callback object to receive the list of columns from the remote procedure call.
	 */
	public void getAllColumns(String dataType,
			AsyncCallback<List<String>> callback);
	
	
	/**
	 * Returns a list of the names of the columns that support filtering.
	 * @param dataType identifier for the type of evidence data.
	 * @param getCompulsory <code>true</code> to return compulsory columns, 
	 * 		<code>false</code> otherwise.
	 * @param getOptional <code>true</code> to return optional columns, 
	 * 		<code>false</code> otherwise.
	 * @param callback callback object to receive the list of columns from the remote procedure call.
	 */
	public void getFilterableColumns(String dataType, 
			boolean getCompulsory, boolean getOptional, 
			AsyncCallback<List<String>> callback);
	
	
	/**
	 * Returns the list of values in the evidence table for the specified column.
	 * @param dataType identifier for the type of evidence data.
	 * @param columnName name of the column for which to return the values.
	 * @param maxRows maximum number of values to return.
	 * @param callback callback object to receive the list of values from the remote procedure call.
	 */
	public void getColumnValues(String dataType, String columnName, int maxRows, 
			AsyncCallback<List<String>> callback);
}
