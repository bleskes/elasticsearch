/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
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
 ************************************************************/

package com.prelert.job;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Transforms
{
	private List<Transform> m_Transforms;

	public List<Transform> getTransforms()
	{
		return m_Transforms;
	}

	public void setTransforms(List<Transform> transforms)
	{
		m_Transforms = transforms;
	}

	/**
	 * Set of all the field names configured as inputs to the transforms
	 * @return
	 */
	public Set<String> inputFieldNames()
	{
		Set<String> fields = new HashSet<>();
		for (Transform t : m_Transforms)
		{
			fields.addAll(t.getInputs());
		}

		return fields;
	}

	public Set<String> outputFieldNames()
	{
		Set<String> fields = new HashSet<>();
		for (Transform t : m_Transforms)
		{
			fields.addAll(t.getOutputs());
		}

		return fields;
	}

	public boolean verify() throws JobConfigurationException
	{
		for (Transform tr : m_Transforms)
		{
			tr.verify();
		}

		return true;
	}
}
