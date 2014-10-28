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

import com.prelert.data.gxt.CausalityEvidencePagingLoadConfig;


/**
 * Loader for a grid of evidence data from an incident, used for paging through
 * the notifications and time series features which have been causally related
 * across metrics or resources. It adds a flag to indicate whether only evidence 
 * with the same description and data type should be loaded.
 * @author Pete Harverson
 */
public class CausalityEvidencePagingLoader extends EvidencePagingLoader
{
	private boolean	m_IsSingleDescription;


	/**
	 * Creates a paging loader for a grid of evidence from aggregated causality data.
	 * @param proxy proxy retrieving the evidence data from the server via GWT RPC.
	 */
	public CausalityEvidencePagingLoader(CausalityEvidencePagingRpcProxy proxy)
	{
		super(proxy);
	}
	
	
	/**
	 * Returns whether only evidence with the same description and data type as
	 * that of the specified item of evidence should be loaded.
	 * @return <code>true</code> to limit loading to a single description,
	 * 	<code>false</code> otherwise.
	 * @see #getEvidenceId()
	 */
	public boolean isSingleDescription()
	{
		return m_IsSingleDescription;
	}
	
	
	/**
	 * Sets whether only evidence with the same description and data type as
	 * that of the specified item of evidence should be loaded.
	 * @param singleDescription <code>true</code> to limit loading to a single 
	 * 	description, <code>false</code> otherwise.
	 * @see #setEvidenceId(int)
	 */
	public void setSingleDescription(boolean singleDescription)
	{
		m_IsSingleDescription = singleDescription;
	}

	
	@Override
    protected Object newLoadConfig()
	{
		return new CausalityEvidencePagingLoadConfig();
	}


	@Override
    protected Object prepareLoadConfig(Object config)
	{
		super.prepareLoadConfig(config);
		
		CausalityEvidencePagingLoadConfig pagingLoadConfig = 
			(CausalityEvidencePagingLoadConfig)config;
		pagingLoadConfig.setSingleDescription(m_IsSingleDescription);

		return pagingLoadConfig;
	}
}
