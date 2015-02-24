/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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

import java.util.Objects;

import com.prelert.rs.data.ErrorCode;

/**
 * Analysis limits for autodetect (max model memory size).
 *
 * If an option has not been set it's value will be 0 in which case it
 * shouldn't be used so the default value is picked up instead.
 */
public class AnalysisLimits
{
	/**
	 * Serialisation field names
	 */
	public static final String MODEL_MEMORY_LIMIT = "modelMemoryLimit";

	private long m_ModelMemoryLimit;

	/**
	 * Initialise values to 0.
	 * If the values are 0 they haven't been set
	 */
	public AnalysisLimits()
	{
		m_ModelMemoryLimit = 0;
	}

	public AnalysisLimits(long modelMemoryLimit)
	{
		m_ModelMemoryLimit = modelMemoryLimit;
	}

	/**
	 * Maximum size of the model in MB before the anomaly detector
     * will drop new samples to prevent the model using any more
     * memory
	 */
	public long getModelMemoryLimit()
	{
		return m_ModelMemoryLimit;
	}

	public void setModelMemoryLimit(long value)
	{
		m_ModelMemoryLimit = value;
	}

	/**
	 * Overridden equality test
	 */
	@Override
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}

		if (other instanceof AnalysisLimits == false)
		{
			return false;
		}

		AnalysisLimits that = (AnalysisLimits)other;
		return (this.m_ModelMemoryLimit == that.m_ModelMemoryLimit);
	}

    @Override
    public int hashCode()
    {
        return Objects.hash(m_ModelMemoryLimit);
    }

	/**
	 * Empty implementation of verify.
	 *
	 * A value of 0 means use the default in autodetect.
	 *
	 * @return true
	 * @throws JobConfigurationException
	 */
	public boolean verify()
	throws JobConfigurationException
	{
        if (m_ModelMemoryLimit < 0)
        {
            throw new JobConfigurationException(
                    "Invalid Analysis limit modelMemoryLimit must be >= 0",
                    ErrorCode.INVALID_VALUE);
        }
		return true;
	}

}
