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
import com.prelert.job.JobDetails;
import com.prelert.job.JobException;
import com.prelert.job.SchedulerConfig;
import com.prelert.job.SchedulerConfig.DataSource;
import com.prelert.job.UnknownJobException;
import com.prelert.job.config.verification.JobConfigurationException;
import com.prelert.job.config.verification.SchedulerConfigVerifier;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.manager.JobManager;
import com.prelert.job.messages.Messages;
import com.prelert.rs.provider.JobConfigurationParseException;

class SchedulerConfigUpdater extends AbstractUpdater
{
    private SchedulerConfig m_NewSchedulerConfig;

    public SchedulerConfigUpdater(JobManager jobManager, String jobId)
    {
        super(jobManager, jobId);
    }

    @Override
    void prepareUpdate(JsonNode node) throws UnknownJobException, JobConfigurationException
    {
        checkJobIsScheduled();
        m_NewSchedulerConfig = parseSchedulerConfig(node);
        checkNotNull();
        m_NewSchedulerConfig.fillDefaults();
        checkDataSourceHasNotChanged();
        SchedulerConfigVerifier.verify(m_NewSchedulerConfig);
    }

    private void checkJobIsScheduled() throws JobConfigurationException
    {
        if (!jobManager().isScheduledJob(jobId()))
        {
            throw new JobConfigurationException(
                    Messages.getMessage(Messages.JOB_SCHEDULER_NO_SUCH_SCHEDULED_JOB, jobId()),
                    ErrorCodes.NO_SUCH_SCHEDULED_JOB);
        }
    }

    private SchedulerConfig parseSchedulerConfig(JsonNode node) throws JobConfigurationException
    {
        try
        {
            return JSON_MAPPER.convertValue(node, SchedulerConfig.class);
        }
        catch (IllegalArgumentException e)
        {
            throw new JobConfigurationParseException(
                    Messages.getMessage(Messages.JOB_CONFIG_UPDATE_SCHEDULE_CONFIG_PARSE_ERROR),
                    e, ErrorCodes.INVALID_VALUE);
        }
    }

    private void checkNotNull() throws JobConfigurationException
    {
        if (m_NewSchedulerConfig == null)
        {
            throw new JobConfigurationException(
                    Messages.getMessage(Messages.JOB_CONFIG_UPDATE_SCHEDULE_CONFIG_CANNOT_BE_NULL),
                    ErrorCodes.INVALID_VALUE);
        }
    }

    private void checkDataSourceHasNotChanged() throws UnknownJobException, JobConfigurationException
    {
        JobDetails job = jobManager().getJobOrThrowIfUnknown(jobId());
        SchedulerConfig currentSchedulerConfig = job.getSchedulerConfig();
        DataSource currentDataSource = currentSchedulerConfig.getDataSource();
        DataSource newDataSource = m_NewSchedulerConfig.getDataSource();
        if (!currentDataSource.equals(newDataSource))
        {
            throw new JobConfigurationException(Messages.getMessage(
                    Messages.JOB_CONFIG_UPDATE_SCHEDULE_CONFIG_DATA_SOURCE_INVALID,
                    currentDataSource, newDataSource), ErrorCodes.INVALID_VALUE);
        }
    }

    @Override
    void commit() throws JobException
    {
        jobManager().updateSchedulerConfig(jobId(), m_NewSchedulerConfig);
    }
}
