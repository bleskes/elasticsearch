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

package com.prelert.data.gxt;

import java.io.Serializable;

import com.prelert.data.TimeFrame;


/**
 * Extension of EvidencePagingLoadConfig for paging of evidence relating to a specific
 * causality incident, adding a flag to indicate whether only evidence with the same 
 * description and data type as the specified id should be loaded.
 * @author Pete Harverson
 */
public class CausalityEvidencePagingLoadConfig 
	extends EvidencePagingLoadConfig implements Serializable
{
    private static final long serialVersionUID = 3845357297856680938L;
    
	// DO NOT DELETE - custom field serializer.
	// The TimeFrame field of the parent DatePagingLoadConfig data transfer object
	// is not referenced directly in any of the service methods in CausalityQueryService
	// so needs to be referenced here so that a serializer is created for it.
	// methods 
	@SuppressWarnings("unused")
    private TimeFrame m_TimeFrameSerializer;
	
	
	/**
	 * Returns whether only evidence with the same description and data type as
	 * that of the specified item of evidence should be loaded.
	 * @return <code>true</code> to limit loading to a single description,
	 * 	<code>false</code> otherwise.
	 * @see #getEvidenceId()
	 */
	public boolean isSingleDescription()
	{
		return get("singleDescription", false);
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
		set("singleDescription", singleDescription);
	}
}
