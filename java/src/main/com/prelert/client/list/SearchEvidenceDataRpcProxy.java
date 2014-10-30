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

import com.prelert.data.gxt.DatePagingLoadResult;
import com.prelert.data.gxt.EvidenceModel;
import com.prelert.data.gxt.EvidencePagingLoadConfig;
import com.prelert.data.gxt.ModelDatePagingLoadConfig;
import com.prelert.service.AsyncServiceLocator;
import com.prelert.service.EvidenceQueryServiceAsync;


/**
 * Concrete sub-class of the ModelDatePagingRpcProxy for running searches for
 * evidence data which have one or more attributes containing specified text.
 */
public class SearchEvidenceDataRpcProxy extends ModelDatePagingRpcProxy<EvidenceModel>
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
	 * @param loadConfig EvidencePagingLoadConfig specifying properties of data
	 * 	to load (data type, source, containsText).
	 */
    @Override
    public void loadFirstPage(ModelDatePagingLoadConfig loadConfig,
            AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback)
    {
    	m_EvidenceQueryService.searchFirstPage((EvidencePagingLoadConfig)loadConfig, callback);
    }


    /**
	 * @param loadConfig EvidencePagingLoadConfig specifying properties of data
	 * 	to load (data type, source, containsText).
	 */
    @Override
    public void loadLastPage(ModelDatePagingLoadConfig loadConfig,
            AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback)
    {
    	m_EvidenceQueryService.searchLastPage((EvidencePagingLoadConfig)loadConfig, callback);
    }


    /**
	 * @param loadConfig EvidencePagingLoadConfig specifying properties of data
	 * 	to load (data type, source, containsText).
	 */
    @Override
    public void loadNextPage(ModelDatePagingLoadConfig loadConfig,
            AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback)
    {
	    m_EvidenceQueryService.searchNextPage((EvidencePagingLoadConfig)loadConfig, callback);
    }


    /**
	 * @param loadConfig EvidencePagingLoadConfig specifying properties of data
	 * 	to load (data type, source, containsText).
	 */
    @Override
    public void loadPreviousPage(ModelDatePagingLoadConfig loadConfig,
            AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback)
    {
    	m_EvidenceQueryService.searchPreviousPage((EvidencePagingLoadConfig)loadConfig, callback);
    }


    /**
	 * @param loadConfig EvidencePagingLoadConfig specifying properties of data
	 * 	to load (data type, source, containsText).
	 */
    @Override
    public void loadAtTime(ModelDatePagingLoadConfig loadConfig,
            AsyncCallback<DatePagingLoadResult<EvidenceModel>> callback)
    {
    	m_EvidenceQueryService.searchAtTime((EvidencePagingLoadConfig)loadConfig, callback);
    }
}
