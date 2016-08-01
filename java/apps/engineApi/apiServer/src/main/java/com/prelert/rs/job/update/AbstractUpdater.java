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

package com.prelert.rs.job.update;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prelert.job.JobDetails;
import com.prelert.job.JobException;
import com.prelert.job.JobStatus;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobConfigurationException;
import com.prelert.job.manager.JobManager;
import com.prelert.job.messages.Messages;

abstract class AbstractUpdater
{
    protected static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private final JobManager m_JobManager;
    private final JobDetails m_Job;
    private final String m_UpdateKey;

    AbstractUpdater(JobManager jobManager, JobDetails job, String updateKey)
    {
        m_JobManager = Objects.requireNonNull(jobManager);
        m_Job = Objects.requireNonNull(job);
        m_UpdateKey = Objects.requireNonNull(updateKey);
    }

    protected JobManager jobManager()
    {
        return m_JobManager;
    }

    protected JobDetails job()
    {
        return m_Job;
    }

    protected String jobId()
    {
        return m_Job.getId();
    }

    protected String updateKey()
    {
        return m_UpdateKey;
    }

    protected final Map<String, Object> convertToMap(JsonNode node,
            Supplier<String> errorMessageSupplier) throws JobConfigurationException
    {
        try
        {
            return JSON_MAPPER.convertValue(node, new TypeReference<Map<String, Object>>() {});
        }
        catch (IllegalArgumentException e)
        {
            throw new JobConfigurationException(errorMessageSupplier.get(),
                    ErrorCodes.INVALID_VALUE, e);
        }
    }

    protected void checkJobIsClosed(JobDetails job) throws JobConfigurationException
    {
        JobStatus jobStatus = job.getStatus();
        if (jobStatus != JobStatus.CLOSED)
        {
            throw new JobConfigurationException(
                    Messages.getMessage(Messages.JOB_CONFIG_UPDATE_JOB_IS_NOT_CLOSED, m_UpdateKey, jobStatus),
                    ErrorCodes.JOB_NOT_CLOSED);
        }
    }

    abstract void prepareUpdate(JsonNode node) throws JobConfigurationException;
    abstract void commit() throws JobException;
}
