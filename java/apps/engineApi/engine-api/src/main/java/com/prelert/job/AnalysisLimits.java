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

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.prelert.job.exceptions.JobConfigurationException;
import com.prelert.rs.data.ErrorCode;

/**
 * Analysis limits for autodetect
 *
 * If an option has not been set it shouldn't be used so the default value is picked up instead.
 */
@JsonInclude(Include.NON_NULL)
public class AnalysisLimits
{
    /**
     * Serialisation field names
     */
    public static final String MODEL_MEMORY_LIMIT = "modelMemoryLimit";
    public static final String CATEGORIZATION_EXAMPLES_LIMIT = "categorizationExamplesLimit";

    /** It is initialised to 0. A value of 0 indicates it was not set. */
    private long m_ModelMemoryLimit;

    /**
     * It is initialised to <code>null</code>.
     * A value of <code>null</code> indicates it was not set.
     * */
    private Long m_CategorizationExamplesLimit;

    public AnalysisLimits()
    {
        m_ModelMemoryLimit = 0;
        m_CategorizationExamplesLimit = null;
    }

    public AnalysisLimits(long modelMemoryLimit, Long categorizationExamplesLimit)
    {
        m_ModelMemoryLimit = modelMemoryLimit;
        m_CategorizationExamplesLimit = categorizationExamplesLimit;
    }

    /**
     * Maximum size of the model in MB before the anomaly detector
     * will drop new samples to prevent the model using any more
     * memory
     *
     * @return The set memory limit or 0 if not set
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
     * Gets the limit to the number of examples that are stored per category
     * @return the limit or <code>null</code> if not set
     */
    public Long getCategorizationExamplesLimit()
    {
        return m_CategorizationExamplesLimit;
    }

    public void setCategorizationExamplesLimit(Long value)
    {
        m_CategorizationExamplesLimit = value;
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
        return this.m_ModelMemoryLimit == that.m_ModelMemoryLimit
                && Objects.equals(this.m_CategorizationExamplesLimit,
                        that.m_CategorizationExamplesLimit);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(m_ModelMemoryLimit, m_CategorizationExamplesLimit);
    }

    /**
     * Verifies the analysis limits parameters were set to valid values
     *
     * @return true when all values are valid
     * @throws JobConfigurationException if some of the values are invalid
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
        if (m_CategorizationExamplesLimit != null && m_CategorizationExamplesLimit < 0)
        {
            throw new JobConfigurationException(
                    "Invalid Analysis limit categorizationExamplesLimit must be >= 0",
                    ErrorCode.INVALID_VALUE);
        }
        return true;
    }

}
