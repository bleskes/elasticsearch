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

import com.extjs.gxt.ui.client.data.LoadEvent;
import com.prelert.client.ApplicationResponseHandler;
import com.prelert.data.gxt.DatePagingLoadResult;
import com.prelert.data.gxt.EvidenceModel;
import com.prelert.data.gxt.EvidencePagingLoadConfig;


/**
 * Extension of the standard evidence paging loader, which adds support for loading at
 * a specific evidence ID.
 * @author Pete Harverson
 */
public class EvidenceViewPagingLoader extends EvidencePagingLoader
{
	private GetEvidenceDataRpcProxy m_Proxy;
	
	public EvidenceViewPagingLoader(GetEvidenceDataRpcProxy proxy)
    {
		super(proxy);
		m_Proxy = proxy;
    }

	
    /**
     * Loads evidence data starting at the specified id.
	 * @param evidenceId id for the top row of evidence data to be loaded. 
     */
	public void loadAtId(int evidenceId)
	{
		setRowId(evidenceId);
		
		Object config = (reuseConfig && lastConfig != null) ? lastConfig : newLoadConfig();
		config = prepareLoadConfig(config);
		
		final Object loadConfig = config;
		
		if (fireEvent(BeforeLoad, new LoadEvent(this, loadConfig)))
		{
			lastConfig = config;
			
			ApplicationResponseHandler<DatePagingLoadResult<EvidenceModel>> callback = 
				new ApplicationResponseHandler<DatePagingLoadResult<EvidenceModel>>()
			{
				public void uponFailure(Throwable caught)
				{
					onLoadFailure(loadConfig, caught);
				}


				public void uponSuccess(DatePagingLoadResult<EvidenceModel> result)
				{
					onLoadSuccess(loadConfig, result);
				}
			};

			m_Proxy.loadAtId((EvidencePagingLoadConfig)loadConfig, callback);
		}
	}
	
	
}
