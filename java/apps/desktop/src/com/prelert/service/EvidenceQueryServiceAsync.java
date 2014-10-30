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

import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.prelert.data.*;
import com.prelert.data.gxt.GridRowInfo;


/**
 * Defines the methods to be implemented by the asynchronous client interface
 * to the Evidence query service.
 * @author Pete Harverson
 */
public interface EvidenceQueryServiceAsync
{
	public void getAllColumns(String dataType, TimeFrame timeFrame, 
			AsyncCallback<List<String>> callback);
	
	public void getColumnValues(String dataType, String columnName, 
			AsyncCallback<List<String>> callback);
	
	public void getFirstPage(EvidencePagingLoadConfig config,
			AsyncCallback<DatePagingLoadResult<EventRecord>> callback);
	
	public void getLastPage(EvidencePagingLoadConfig config,
			AsyncCallback<DatePagingLoadResult<EventRecord>> callback);
	
	public void getNextPage(
			EvidencePagingLoadConfig config, String bottomRowId, 
			AsyncCallback<DatePagingLoadResult<EventRecord>> callback);
	
	public void getPreviousPage(
			EvidencePagingLoadConfig config, String topRowId,
			AsyncCallback<DatePagingLoadResult<EventRecord>> callback);
	
	public void getForDescription(
			EvidencePagingLoadConfig config, String description,
			AsyncCallback<DatePagingLoadResult<EventRecord>> callback);
	
	public void getAtTime(EvidencePagingLoadConfig config,
			AsyncCallback<DatePagingLoadResult<EventRecord>> callback);
	
	public void getIdPage(
			EvidencePagingLoadConfig config, int id,
			AsyncCallback<DatePagingLoadResult<EventRecord>> callback);
	
	public void getRowInfo(int id, AsyncCallback<List<GridRowInfo>> callback);
	
	public void getEarliestDate(String dataType, AsyncCallback<Date> callback);
	
	public void getLatestDate(String dataType, AsyncCallback<Date> callback);
	
}
