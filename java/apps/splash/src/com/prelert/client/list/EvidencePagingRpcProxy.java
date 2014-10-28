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

import com.extjs.gxt.ui.client.data.RpcProxy;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.prelert.data.gxt.DatePagingLoadResult;
import com.prelert.data.gxt.EvidenceModel;
import com.prelert.data.gxt.EvidencePagingLoadConfig;


/**
 * Abstract data proxy base class for loading pages of evidence data via GWT 
 * remote procedure calls. Concrete implementations should be provided for loading
 * evidence data for lists which have controls for paging back and forth.
 */
public abstract class EvidencePagingRpcProxy extends RpcProxy<DatePagingLoadResult<EvidenceModel>>
{
	
	/**
	 * Loads the first page of data using the specified load configuration. 
	 */
    protected void load(Object loadConfig,
            AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback)
    {
    	loadFirstPage((EvidencePagingLoadConfig)loadConfig, callback);   
    }
	
	
	/**
	 * Loads the first page of evidence data matching the specified load config.
	 * @param config load config specifying the range of data (i.e. time frame)
	 * 		to obtain.
	 * @param callback callback object to receive the DatePagingLoadResult from
	 *  	the remote procedure call.
	 */
    public abstract void loadFirstPage(EvidencePagingLoadConfig loadConfig,
            AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
    
    
    /**
	 * Loads the last page of evidence data matching the specified load config.
	 * @param config load config specifying the range of data (i.e. time frame)
	 * 		to obtain.
	 * @param callback callback object to receive the DatePagingLoadResult from
	 *  	the remote procedure call.
	 */
    public abstract void loadLastPage(EvidencePagingLoadConfig loadConfig,
            AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
    
    
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
    public abstract void loadNextPage(EvidencePagingLoadConfig loadConfig, String bottomRowId, 
    		AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
    
    
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
    public abstract void loadPreviousPage(EvidencePagingLoadConfig loadConfig, String topRowId, 
    		AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
    
    
    /**
	 * Loads a page of evidence data, whose top row will match the date in
	 * the supplied config.
	 * @param config load config specifying the range of data to obtain. The date
	 * in this DatePagingLoadConfig will correspond to the value of the time column 
	 * for the first row of evidence data that will be returned.
	 * @param callback callback object to receive the DatePagingLoadResult from
	 *  	the remote procedure call.
	 */
    public abstract void loadAtTime(EvidencePagingLoadConfig loadConfig, 
    		AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback);
}
