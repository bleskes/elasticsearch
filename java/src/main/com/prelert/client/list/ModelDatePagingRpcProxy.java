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

package com.prelert.client.list;

import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.data.RpcProxy;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.prelert.data.gxt.DatePagingLoadResult;
import com.prelert.data.gxt.ModelDatePagingLoadConfig;


/**
 * Abstract data proxy base class for loading pages of GXT ModelData objects
 * via GWT remote procedure calls (GWT RPC). Concrete implementations should be 
 * provided for loading different GXT ModelData subclasses through a date range
 * via a toolbar with date/time paging controls.
 * @author Pete Harverson
 *
 * @param <M> the type of the model data being paged.
 */
public abstract class ModelDatePagingRpcProxy<M extends ModelData>
	extends RpcProxy<DatePagingLoadResult<M>>
{

    @Override
    protected void load(Object loadConfig,
            AsyncCallback<DatePagingLoadResult<M>> callback)
    {
    	ModelDatePagingLoadConfig config = (ModelDatePagingLoadConfig)loadConfig;
    	
    	if (config.getTime() == null)
    	{
    		loadFirstPage(config, callback);
    	}
    	else
    	{
    		loadAtTime(config, callback);
    	}
    }
    

	/**
	 * Loads the first page of data matching the specified load config.
	 * @param config load config specifying the data to obtain.
	 * @param callback callback object to receive the DatePagingLoadResult from
	 *  	the remote procedure call.
	 */
    public abstract void loadFirstPage(ModelDatePagingLoadConfig loadConfig,
    		AsyncCallback<DatePagingLoadResult<M>> callback);
    
    
    /**
	 * Loads the last page of data matching the specified load config.
	 * @param config load config specifying the data to obtain.
	 * @param callback callback object to receive the DatePagingLoadResult from
	 *  	the remote procedure call.
	 */
    public abstract void loadLastPage(ModelDatePagingLoadConfig loadConfig,
            AsyncCallback<DatePagingLoadResult<M>> callback);
    
    
    /**
     * Loads the next page of data following the item specified by the time
     * and row id in the supplied load config.
     * @param loadConfig specifying the data to obtain.
     * @param callback callback object to receive the DatePagingLoadResult from
	 *  	the remote procedure call.
     */
    public abstract void loadNextPage(ModelDatePagingLoadConfig loadConfig,
    		AsyncCallback<DatePagingLoadResult<M>> callback);
    
    
    /**
     * Loads the previous page of data to the item specified by the time and
     * row id in the supplied load config.
     * @param loadConfig specifying the data to obtain.
     * @param callback callback object to receive the DatePagingLoadResult from
	 *  	the remote procedure call.
     */
    public abstract void loadPreviousPage(ModelDatePagingLoadConfig loadConfig, 
    		AsyncCallback<DatePagingLoadResult<M>> callback);
    
    
    /**
     * Loads a page of data, whose top row will be closest in time, up to or before,
     * the time in the supplied load config.
	 * @param loadConfig load config specifying the data to obtain. The first row 
	 * 	of data returned by the call will be the closest in time, up to or before,
     * 	the time in the supplied config.
	 * @param callback callback object to receive the DatePagingLoadResult from
	 *  	the remote procedure call.
     */
    public abstract void loadAtTime(ModelDatePagingLoadConfig loadConfig, 
    		AsyncCallback<DatePagingLoadResult<M>> callback);
}
