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

import java.util.List;

public class Transform
{
	private List<String> m_Inputs;
	private String m_Name;
	private List<String> m_Outputs;
	private TransformType m_Type;

	public Transform()
	{
	}

	public List<String> getInputs()
	{
		return m_Inputs;
	}

	public void setInputs(List<String> fields)
	{
		m_Inputs = fields;
	}

	public String getTransform()
	{
		return m_Name;
	}

	public void setTransform(String type)
	{
		m_Name = type;
	}

	public List<String> getOutputs()
	{
		return m_Outputs;
	}

	public void setOutputs(List<String> outputs)
	{
		m_Outputs = outputs;
	}

	/**
	 * This field shouldn't be serialised as its created dynamically
	 * Type may be null when the class is constructed.
	 * @return
	 */
	public TransformType type() throws TransformConfigurationException
	{
		if (m_Type == null)
		{
			m_Type = TransformType.fromString(m_Name);
		}

		return m_Type;
	}

	public boolean verify() throws TransformConfigurationException
	{
		return type().verify(this);
	}
}
