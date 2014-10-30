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

package com.prelert.data.gxt;

import java.io.Serializable;

import com.prelert.data.TimeFrame;


/**
 * Extension of EvidencePagingLoadConfig for paging of evidence relating to a
 * specific causality incident, adding an evidence id property.
 * @author Pete Harverson
 */
public class CausalityEvidencePagingLoadConfig 
	extends EvidencePagingLoadConfig implements Serializable
{

	// DO NOT DELETE - custom field serializer.
	// The TimeFrame field of the parent DatePagingLoadConfig data transfer object
	// is not referenced directly in any of the service methods in CausalityQueryService
	// so needs to be referenced here so that a serializer is created for it.
	// methods 
	@SuppressWarnings("unused")
    private TimeFrame m_TimeFrameSerializer;
	
	
	/**
	 * Returns the id of the item of evidence from the incident for which evidence
	 * data is being paged.
	 * @return the <code>id</code> of the evidence, or -1 if no value has been set.
	 */
	public int getEvidenceId()
	{
		int evidenceId = get("evidenceId", new Integer(-1));
    	return evidenceId;
	}
	
	
	/**
	 * Sets the id of the item of evidence from the incident for which evidence
	 * data is to be paged.
	 * @param evidenceId the evidence id.
	 */
	public void setEvidenceId(int evidenceId)
	{
		set("evidenceId", evidenceId);
	}
	
}
