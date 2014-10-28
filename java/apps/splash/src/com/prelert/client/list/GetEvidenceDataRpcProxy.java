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
 * Concrete sub-class of the EvidencePagingRpcProxy for loading data for an evidence view
 * with controls for paging back and forth through the data. The class adds 
 * extra functionality for retrieving pages of evidence whose top row will match a 
 * specified id or description.
 */
public class GetEvidenceDataRpcProxy extends EvidencePagingRpcProxy
{
	private EvidenceQueryServiceAsync 	m_EvidenceQueryService = null;
	
	
	/**
	 * Creates a new data proxy for loading data for evidence views from the 
	 * evidence query service.
	 */
	public GetEvidenceDataRpcProxy()
	{
		m_EvidenceQueryService = AsyncServiceLocator.getInstance().getEvidenceQueryService();
	}
    
    
    /**
	 * Loads the first page of evidence data matching the specified load config.
	 * @param config load config specifying the range of data (i.e. time frame)
	 * 		to obtain.
	 * @param callback callback object to receive the DatePagingLoadResult from
	 *  	the remote procedure call.
	 */
    public void loadFirstPage(EvidencePagingLoadConfig loadConfig,
            AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback)
    {
    	m_EvidenceQueryService.getFirstPage(loadConfig, callback);
    }
    
    
    /**
	 * Loads the last page of evidence data matching the specified load config.
	 * @param config load config specifying the range of data (i.e. time frame)
	 * 		to obtain.
	 * @param callback callback object to receive the DatePagingLoadResult from
	 *  	the remote procedure call.
	 */
    public void loadLastPage(EvidencePagingLoadConfig loadConfig,
            AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback)
    {
    	m_EvidenceQueryService.getLastPage(loadConfig, callback);
    }
    
    
    /**
	 * Loads the next page of evidence data following on from the row with the
	 * specified id.
	 * @param config load config specifying the range of data to obtain. The date
	 * in this DatePagingLoadConfig must correspond to the value of the time column 
	 * for the bottom row of evidence in the current page.
	 * @param bottomRowId the value of the id column for the bottom row of evidence 
	 * in the current page.
	 * @param callback callback object to receive the DatePagingLoadResult from
	 *  	the remote procedure call.
	 */
    public void loadNextPage(EvidencePagingLoadConfig loadConfig, String bottomRowId, 
    		AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback)
    {
    	m_EvidenceQueryService.getNextPage(loadConfig, bottomRowId, callback);
    }
    
    
    /**
	 * Loads the previous page of evidence data to the row with the specified id.
	 * @param config load config specifying the range of data to obtain. The date
	 * in this DatePagingLoadConfig must correspond to the value of the time column 
	 * for the top row of evidence in the current page.
	 * @param topRowId the value of the id column for the top row of evidence 
	 * in the current page.
	 * @param callback callback object to receive the DatePagingLoadResult from
	 *  	the remote procedure call.
	 */
    public void loadPreviousPage(EvidencePagingLoadConfig loadConfig, String topRowId, 
    		AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback)
    {
    	m_EvidenceQueryService.getPreviousPage(loadConfig, topRowId, callback);
    }
    
    
    /**
	 * Loads a page of evidence data, whose top row will match the date in
	 * the supplied config.
	 * @param config load config specifying the range of data to obtain. The date
	 * in this DatePagingLoadConfig will correspond to the value of the time column 
	 * for the first row of evidence data that will be returned.
	 * @param callback callback object to receive the DatePagingLoadResult from
	 *  	the remote procedure call.
	 */
    public void loadAtTime(EvidencePagingLoadConfig loadConfig, 
    		AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback)
    {
    	m_EvidenceQueryService.getAtTime(loadConfig, callback);
    }
    
    
    /**
	 * Loads a page of evidence data, the top row of which matches the specified id.
	 * @param config load config specifying the range of data to obtain 
	 * 	e.g. the time frame.
	 * @param id evidence id for the top row of evidence data that will be returned.
	 * @param callback callback object to receive the DatePagingLoadResult from
	 *  	the remote procedure call.
	 */
    public void loadAtId(EvidencePagingLoadConfig loadConfig, int id,
    		AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback)
    {
    	m_EvidenceQueryService.getIdPage(loadConfig, id, callback);
    }
    
    
    /**
	 * Loads a page of evidence data, the top row of which matches the specified
	 * description, and whose time corresponds to the date in the supplied config.
	 * @param config load config specifying the range of data to obtain. The date
	 * in this DatePagingLoadConfig will correspond to the value of the time column 
	 * for the first row of evidence data that will be returned.
	 * @param description the value of the description column to match on for the
	 * top row of evidence data that will be returned.
	 * @param callback callback object to receive the DatePagingLoadResult from
	 *  	the remote procedure call.
	 */
    public void loadForDescription(EvidencePagingLoadConfig loadConfig, String description,
    		AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback)
    {
    	m_EvidenceQueryService.getForDescription(loadConfig, description, callback);
    }


}
