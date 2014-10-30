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

import java.util.Date;

import com.prelert.data.TimeFrame;
import com.prelert.data.gxt.CausalityEvidencePagingLoadConfig;


/**
 * Paging loader for a grid of notification data from a probable cause incident,
 * adding support for setting the id of a item of evidence from the incident.
 * @author Pete Harverson
 */
public class CausalityEvidencePagingLoader extends EvidencePagingLoader
{
	private int 	m_EvidenceId;


	/**
	 * Creates a paging loader for a grid of notification data from a probable cause.
	 * @param proxy proxy retrieving the evidence data from the server via GWT RPC.
	 */
	public CausalityEvidencePagingLoader(EvidencePagingRpcProxy proxy)
	{
		super(proxy);
	}


	/**
	 * Returns the id of an item of evidence from the probable cause incident.
	 * The loader will page notification data from the incident with the same
	 * data type and description as the supplied evidence.
	 * @return the id of an item of evidence from the probable cause.
	 */
	public int getEvidenceId()
	{
		return m_EvidenceId;
	}


	/**
	 * Sets the id of an item of evidence from the probable cause incident.
	 * The loader will page notification data from the incident with the same
	 * data type and description as the supplied evidence.
	 * @param evidenceId the id of an item of evidence from the probable cause.
	 */
	public void setEvidenceId(int evidenceId)
	{
		m_EvidenceId = evidenceId;
	}
	

    @Override
    public void loadNextPage(Date bottomRowDate, String bottomRowId,
            TimeFrame timeFrame)
    {
    	setEvidenceId(Integer.parseInt(bottomRowId));
    	
	    super.loadNextPage(bottomRowDate, bottomRowId, timeFrame);
    }


	@Override
    public void loadPreviousPage(Date topRowDate, String topRowId,
            TimeFrame timeFrame)
    {
	    setEvidenceId(Integer.parseInt(topRowId));
	    
	    super.loadPreviousPage(topRowDate, topRowId, timeFrame);
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
		pagingLoadConfig.setEvidenceId(m_EvidenceId);

		return pagingLoadConfig;
	}


}
