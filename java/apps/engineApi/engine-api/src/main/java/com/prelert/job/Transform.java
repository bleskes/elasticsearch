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
	// Serialisation strings
	public static final String TYPE = "transform";
	public static final String TRANSFORM = "transform";
	public static final String INPUTS = "inputs";
	public static final String OUTPUTS = "outputs";


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

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((m_Inputs == null) ? 0 : m_Inputs.hashCode());
		result = prime * result + ((m_Name == null) ? 0 : m_Name.hashCode());
		result = prime * result
				+ ((m_Outputs == null) ? 0 : m_Outputs.hashCode());
		result = prime * result + ((m_Type == null) ? 0 : m_Type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		if (obj == null)
		{
			return false;
		}

		if (getClass() != obj.getClass())
		{
			return false;
		}

		Transform other = (Transform) obj;
		if (m_Inputs == null)
		{
			if (other.m_Inputs != null)
			{
				return false;
			}
		}
		else if (!m_Inputs.equals(other.m_Inputs))
		{
			return false;
		}

		if (m_Name == null)
		{
			if (other.m_Name != null)
			{
				return false;
			}
		}
		else if (!m_Name.equals(other.m_Name))
		{
			return false;
		}

		if (m_Outputs == null)
		{
			if (other.m_Outputs != null)
			{
				return false;
			}
		}
		else if (!m_Outputs.equals(other.m_Outputs))
		{
			return false;
		}

		if (m_Type != other.m_Type)
		{
			return false;
		}

		return true;
	}
}
