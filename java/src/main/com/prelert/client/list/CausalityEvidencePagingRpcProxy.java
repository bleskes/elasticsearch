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

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.prelert.data.gxt.CausalityEvidencePagingLoadConfig;
import com.prelert.data.gxt.DatePagingLoadResult;
import com.prelert.data.gxt.EvidenceModel;
import com.prelert.data.gxt.ModelDatePagingLoadConfig;
import com.prelert.service.AsyncServiceLocator;
import com.prelert.service.CausalityQueryServiceAsync;


/**
 * Concrete sub-class of the EvidencePagingRpcProxy for loading evidence data
 * from a probable cause incident. The RpcProxy pages through evidence matching
 * the data type and description of the notification defined in the 
 * CausalityEvidencePagingLoadConfig parameter passed to the methods.
 */
public class CausalityEvidencePagingRpcProxy extends ModelDatePagingRpcProxy<EvidenceModel>
{
	private CausalityQueryServiceAsync 	m_CausalityQueryService = null;

	
	/**
	 * Creates a new data proxy for loading evidence data from a probable cause
	 * incident from the causality query service.
	 */
	public CausalityEvidencePagingRpcProxy()
	{
		m_CausalityQueryService = AsyncServiceLocator.getInstance().getCausalityQueryService();
	}
	

	/**
	 * @param loadConfig CausalityEvidencePagingLoadConfig specifying properties 
	 * 	of data to load (evidence ID, single description).
	 */
    @Override
    public void loadFirstPage(ModelDatePagingLoadConfig loadConfig,
            AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback)
    {
    	m_CausalityQueryService.getFirstPage(
    			(CausalityEvidencePagingLoadConfig)loadConfig, callback);
    }


    /**
	 * @param loadConfig CausalityEvidencePagingLoadConfig specifying properties 
	 * 	of data to load (evidence ID, single description).
	 */
    @Override
    public void loadLastPage(ModelDatePagingLoadConfig loadConfig,
            AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback)
    {
    	m_CausalityQueryService.getLastPage(
    			(CausalityEvidencePagingLoadConfig)loadConfig, callback);
    }


    /**
	 * @param loadConfig CausalityEvidencePagingLoadConfig specifying properties 
	 * 	of data to load (evidence ID, single description).
	 */
    @Override
    public void loadNextPage(ModelDatePagingLoadConfig loadConfig,
            AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback)
    {
    	m_CausalityQueryService.getNextPage(
    			(CausalityEvidencePagingLoadConfig)loadConfig, callback);
    }


    /**
	 * @param loadConfig CausalityEvidencePagingLoadConfig specifying properties 
	 * 	of data to load (evidence ID, single description).
	 */
    @Override
    public void loadPreviousPage(ModelDatePagingLoadConfig loadConfig,
            AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback)
    {
    	m_CausalityQueryService.getPreviousPage(
    			(CausalityEvidencePagingLoadConfig)loadConfig, callback);
    }

    
    /**
	 * @param loadConfig CausalityEvidencePagingLoadConfig specifying properties 
	 * 	of data to load (evidence ID, single description).
	 */
    @Override
    public void loadAtTime(ModelDatePagingLoadConfig loadConfig,
            AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback)
    {
    	m_CausalityQueryService.getAtTime(
    			(CausalityEvidencePagingLoadConfig)loadConfig, callback);    
    }
}
