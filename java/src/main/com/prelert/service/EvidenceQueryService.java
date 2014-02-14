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
 * on +44 (0)20 7953 7243 or email to legal@prelert.com.    *
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

import com.prelert.data.gxt.DatePagingLoadResult;
import com.prelert.data.gxt.EvidenceModel;
import com.prelert.data.gxt.AttributeModel;
import com.prelert.data.gxt.EvidencePagingLoadConfig;


/**
 * Defines the methods for the interface to the Evidence View query service.
 * @author Pete Harverson
 */
public interface EvidenceQueryService extends RemoteService
{
	/**
	 * Returns the first page of evidence data matching the specified load config.
	 * @param config load config specifying the range of data (i.e. time frame)
	 * to obtain.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EvidenceModel> getFirstPage(EvidencePagingLoadConfig config);
	
	
	/**
	 * Returns the last page of evidence data matching the specified load config.
	 * @param config load config specifying the range of data (i.e. time frame)
	 * to obtain.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EvidenceModel> getLastPage(EvidencePagingLoadConfig config);
	
	
	/**
	 * Returns the next page of evidence data following on from the row in the 
	 * supplied load config.
	 * @param config load config specifying the range of data to obtain. The date
	 * and row id must correspond to the bottom row of evidence in the current page.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EvidenceModel> getNextPage(
			EvidencePagingLoadConfig config);
	
	
	/**
	 * Returns the previous page of evidence data to the row in the supplied load
	 * config.
	 * @param config load config specifying the range of data to obtain. The date
	 * and row id must correspond to the top row of evidence in the current page.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EvidenceModel> getPreviousPage(
			EvidencePagingLoadConfig config);
	
	
	/**
	 * Returns a page of evidence data, whose top row will match the date in
	 * the supplied config.
	 * @param config load config specifying the range of data to obtain. The date
	 * in this DatePagingLoadConfig will correspond to the value of the time column 
	 * for the first row of evidence data that will be returned.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EvidenceModel> getAtTime(EvidencePagingLoadConfig config);
	
	
	/**
	 * Returns a page of evidence data, the top row of which matches the row id in
	 * the supplied load config.
	 * @param config load config specifying the range of data to obtain 
	 * 	e.g. row id.
	 * @return load result containing the requested evidence data. 
	 */
	public DatePagingLoadResult<EvidenceModel> getIdPage(
			EvidencePagingLoadConfig config);
	
	
	/**
	 * Runs a search to return the first page of evidence data where one or more
	 * attributes contain the text in the specified load config.
	 * @param config load config specifying the text to search for in evidence attributes.
	 * @return load result containing the first page of matching evidence data.
	 */
	public DatePagingLoadResult<EvidenceModel> searchFirstPage(EvidencePagingLoadConfig config);
	
	
	/**
	 * Runs a search to return the last page of evidence data where one or more
	 * attributes contain the text in the specified load config.
	 * @param config load config specifying the text to search for in evidence attributes.
	 * @return load result containing the last page of matching evidence data.
	 */
	public DatePagingLoadResult<EvidenceModel> searchLastPage(EvidencePagingLoadConfig config);
	
	
	/**
	 * Runs a search to return the next page of evidence data, following on from the row
	 * with the specified id, where one or more attributes contain the text in the 
	 * specified load config.
	 * @param config load config specifying the text to search for in evidence attributes.
	 * @return load result containing the next page of matching evidence data.
	 */
	public DatePagingLoadResult<EvidenceModel> searchNextPage(
			EvidencePagingLoadConfig config);
	
	
	/**
	 * Runs a search to return the previous page of evidence data to the one
	 * with the specified id, where one or more attributes contain the text in the 
	 * specified load config.
	 * @param config load config specifying the text to search for in evidence attributes.
	 * @return load result containing the previous page of matching evidence data.
	 */
	public DatePagingLoadResult<EvidenceModel> searchPreviousPage(
			EvidencePagingLoadConfig config);
	
	
	/**
	 * Runs a search to return a page of evidence data, whose top row will match the date
	 * in the supplied config and where one or more attributes contain the text in the 
	 * specified load config.
	 * @param config load config specifying the range of data to obtain. The date
	 * in this DatePagingLoadConfig will correspond to the value of the time column 
	 * for the first row of evidence data that will be returned.
	 * @return load result containing the requested evidence data.
	 */
	public DatePagingLoadResult<EvidenceModel> searchAtTime(EvidencePagingLoadConfig config);
	
	
	/**
	 * Returns all attributes for the item of evidence with the specified id 
  	 * (instead of just the current display columns).
	 * @param id the value of the id column for the evidence to obtain information on.
	 * @return List of AttributeModel objects for the row with the specified id.
	 * 		Note that values for time fields are transported as a String representation
	 * 		of the number of milliseconds since January 1, 1970, 00:00:00 GMT.
	 */
	public List<AttributeModel> getEvidenceAttributes(int id);
	
	
	/**
	 * Returns the data model for the single item of evidence with the given id.
	 * @param id id of the item of evidence to return.
	 * @return the full data model for the item with the specified id,
	 * 	 or <code>null</code> if no evidence exists with this id.
	 */
	public EvidenceModel getEvidenceSingle(int id);
	
	
	/**
	 * Returns a list of all of the columns in an Evidence View for the specified
	 * data type.
	 * @param dataType identifier for the type of evidence data.
	 * @return list of all of the columns for an Evidence View.
	 */
	public List<String> getAllColumns(String dataType);
	
	
	/**
	 * Returns a list of the names of the columns that support filtering.
	 * @param dataType identifier for the type of evidence data.
	 * @param getCompulsory <code>true</code> to return compulsory columns, 
	 * 		<code>false</code> otherwise.
	 * @param getOptional <code>true</code> to return optional columns, 
	 * 		<code>false</code> otherwise.
	 * @return a list of the filterable columns.
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
	public List<String> getColumnValues(
			String dataType, String columnName, int maxRows);
	
}
