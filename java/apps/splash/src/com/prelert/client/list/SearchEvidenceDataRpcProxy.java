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

package com.prelert.client.list;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.prelert.data.gxt.DatePagingLoadResult;
import com.prelert.data.gxt.EvidenceModel;
import com.prelert.data.gxt.EvidencePagingLoadConfig;
import com.prelert.service.AsyncServiceLocator;
import com.prelert.service.EvidenceQueryServiceAsync;


/**
 * Concrete sub-class of the EvidencePagingRpcProxy for running searches for
 * evidence data which have one or more attributes containing specified text.
 */
public class SearchEvidenceDataRpcProxy extends EvidencePagingRpcProxy
{
	private EvidenceQueryServiceAsync 	m_EvidenceQueryService = null;
	

	/**
	 * Creates a new data proxy for loading evidence data matching search criteria
	 * from the evidence query service.
	 */
	public SearchEvidenceDataRpcProxy()
	{
		m_EvidenceQueryService = AsyncServiceLocator.getInstance().getEvidenceQueryService();
	}
    
    
	/**
	 * Loads the first page of evidence data matching the search criteria in
	 * the specified load config.
	 * @param config load config specifying the text to be matched in the attributes.
	 * @param callback callback object to receive the DatePagingLoadResult from
	 *  	the remote procedure call.
	 */
    public void loadFirstPage(EvidencePagingLoadConfig loadConfig,
            AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback)
    {
    	m_EvidenceQueryService.searchFirstPage(loadConfig, callback);
    }
    
    
    /**
	 * Loads the last page of evidence data matching the search criteria in
	 * the specified load config.
	 * @param config load config specifying the text to be matched in the attributes.
	 * @param callback callback object to receive the DatePagingLoadResult from
	 *  	the remote procedure call.
	 */
    public void loadLastPage(EvidencePagingLoadConfig loadConfig,
            AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback)
    {
    	m_EvidenceQueryService.searchLastPage(loadConfig, callback);
    }
    
    
    /**
	 * Loads the next page of evidence data matching the search criteria in
	 * the specified load config following on from the row with the
	 * specified id.
	 * @param config load config specifying the text to be matched in the attributes.
	 * The date in this DatePagingLoadConfig must correspond to the value of the time column 
	 * for the bottom row of evidence in the current page.
	 * @param bottomRowId the value of the id column for the bottom row of evidence 
	 * in the current page.
	 * @param callback callback object to receive the DatePagingLoadResult from
	 *  	the remote procedure call.
	 */
    public void loadNextPage(EvidencePagingLoadConfig loadConfig, String bottomRowId, 
    		AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback)
    {
    	m_EvidenceQueryService.searchNextPage(loadConfig, bottomRowId, callback);
    }
    
    
    /**
	 * Loads the previous page of evidence data to the row with the specified id, 
	 * matching the search criteria in the specified load config.
	 * @param config load config specifying the text to be matched in the attributes.
	 * The date in this DatePagingLoadConfig must correspond to the value of the time 
	 * column  for the top row of evidence in the current page.
	 * @param topRowId the value of the id column for the top row of evidence 
	 * in the current page.
	 * @param callback callback object to receive the DatePagingLoadResult from
	 *  	the remote procedure call.
	 */
    public void loadPreviousPage(EvidencePagingLoadConfig loadConfig, String topRowId, 
    		AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback)
    {
    	m_EvidenceQueryService.searchPreviousPage(loadConfig, topRowId, callback);
    }
    
    
    /**
	 * Loads a page of evidence data, whose top row will match the date
	 * in the supplied config and where one or more attributes contain the text in the 
	 * specified load config.
	 * @param config load config specifying the text to be matched in the attributes.
	 * The date in this DatePagingLoadConfig will correspond to the value of the time  
	 * column for the first row of evidence data that will be returned.
	 * @param callback callback object to receive the DatePagingLoadResult from
	 *  	the remote procedure call.
	 */
    public void loadAtTime(EvidencePagingLoadConfig loadConfig, 
    		AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback)
    {
    	m_EvidenceQueryService.searchAtTime(loadConfig, callback);
    }
    
}
