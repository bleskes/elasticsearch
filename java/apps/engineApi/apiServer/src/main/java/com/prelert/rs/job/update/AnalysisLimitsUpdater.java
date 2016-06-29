/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
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

package com.prelert.rs.job.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.prelert.job.AnalysisLimits;
import com.prelert.job.JobDetails;
import com.prelert.job.JobException;
import com.prelert.job.config.verification.AnalysisLimitsVerifier;
import com.prelert.job.config.verification.JobConfigurationException;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.manager.JobManager;
import com.prelert.job.messages.Messages;
import com.prelert.rs.provider.JobConfigurationParseException;

class AnalysisLimitsUpdater extends AbstractUpdater
{
    private AnalysisLimits m_NewLimits;

    public AnalysisLimitsUpdater(JobManager jobManager, JobDetails job, String updateKey)
    {
        super(jobManager, job, updateKey);
    }

    @Override
    void prepareUpdate(JsonNode node) throws JobConfigurationException
    {
        JobDetails job = job();
        checkJobIsClosed(job);
        m_NewLimits = parseAnalysisLimits(node);
        checkNotNull();
        AnalysisLimitsVerifier.verify(m_NewLimits);
        checkModelMemoryLimitIsNotDecreased(job);
    }

    private AnalysisLimits parseAnalysisLimits(JsonNode node) throws JobConfigurationException
    {
        try
        {
            return JSON_MAPPER.convertValue(node, AnalysisLimits.class);
        }
        catch (IllegalArgumentException e)
        {
            throw new JobConfigurationParseException(
                    Messages.getMessage(Messages.JOB_CONFIG_UPDATE_ANALYSIS_LIMITS_PARSE_ERROR),
                    e, ErrorCodes.INVALID_VALUE);
        }
    }

    private void checkNotNull() throws JobConfigurationException
    {
        if (m_NewLimits == null)
        {
            throw new JobConfigurationException(
                    Messages.getMessage(Messages.JOB_CONFIG_UPDATE_ANALYSIS_LIMITS_CANNOT_BE_NULL),
                    ErrorCodes.INVALID_VALUE);
        }
    }

    private void checkModelMemoryLimitIsNotDecreased(JobDetails job)
            throws JobConfigurationException
    {
        AnalysisLimits analysisLimits = job.getAnalysisLimits();
        if (analysisLimits == null)
        {
            return;
        }
        long oldMemoryLimit = analysisLimits.getModelMemoryLimit();
        long newMemoryLimit = m_NewLimits.getModelMemoryLimit();
        if (newMemoryLimit < oldMemoryLimit)
        {
            throw new JobConfigurationException(
                    Messages.getMessage(Messages.JOB_CONFIG_UPDATE_ANALYSIS_LIMITS_MODEL_MEMORY_LIMIT_CANNOT_BE_DECREASED,
                            oldMemoryLimit, newMemoryLimit),
                    ErrorCodes.INVALID_VALUE);
        }
    }

    @Override
    void commit() throws JobException
    {
        jobManager().setAnalysisLimits(jobId(), m_NewLimits);
    }

}
