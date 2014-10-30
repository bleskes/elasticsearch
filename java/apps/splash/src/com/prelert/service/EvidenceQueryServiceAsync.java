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

package com.prelert.service;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.prelert.data.TimeFrame;
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
	public  void getFirstPage(EvidencePagingLoadConfig config, 
			AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
	
	public void getLastPage(EvidencePagingLoadConfig config, 
			AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
	
	public void getNextPage(
			EvidencePagingLoadConfig config, String bottomRowId, 
			AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
	
	public void getPreviousPage(
			EvidencePagingLoadConfig config, String topRowId, 
			AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
	
	public void getForDescription(
			EvidencePagingLoadConfig config, String description, 
			AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
	
	public void getAtTime(EvidencePagingLoadConfig config, 
			AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
	
	public void getIdPage(
			EvidencePagingLoadConfig config, int id,
			AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
	
	public void searchFirstPage(EvidencePagingLoadConfig config, 
			AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
	
	public void searchLastPage(EvidencePagingLoadConfig config, 
			AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
	
	public void searchNextPage(
			EvidencePagingLoadConfig config, String bottomRowId, 
			AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
	
	public void searchPreviousPage(
			EvidencePagingLoadConfig config, String topRowId, 
			AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
	
	public void searchAtTime(EvidencePagingLoadConfig config, 
			AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
	
	public void getEvidenceAttributes(int rowId, 
			AsyncCallback<List<AttributeModel>> callback);
	
	public void getEvidenceSingle(int id,
			AsyncCallback<EvidenceModel> callback);
	
	public void getAllColumns(String dataType, TimeFrame timeFrame,
			AsyncCallback<List<String>> callback);
	
	public void getFilterableColumns(String dataType, 
			boolean getCompulsory, boolean getOptional, 
			AsyncCallback<List<String>> callback);
	
	public void getColumnValues(String dataType, String columnName, int maxRows, 
			AsyncCallback<List<String>> callback);
}
