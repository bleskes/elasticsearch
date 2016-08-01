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

import java.io.IOException;
import java.io.StringWriter;

import com.fasterxml.jackson.databind.JsonNode;
import com.prelert.job.JobDetails;
import com.prelert.job.ModelDebugConfig;
import com.prelert.job.UnknownJobException;
import com.prelert.job.config.verification.ModelDebugConfigVerifier;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobConfigurationException;
import com.prelert.job.manager.JobManager;
import com.prelert.job.messages.Messages;
import com.prelert.job.process.writer.ModelDebugConfigWriter;
import com.prelert.rs.provider.JobConfigurationParseException;

class ModelDebugConfigUpdater extends AbstractUpdater
{
    private final StringWriter m_ConfigWriter;
    private ModelDebugConfig m_NewConfig;

    public ModelDebugConfigUpdater(JobManager jobManager, JobDetails job, String updateKey,
            StringWriter configWriter)
    {
        super(jobManager, job, updateKey);
        m_ConfigWriter = configWriter;
    }

    @Override
    void prepareUpdate(JsonNode node) throws JobConfigurationException
    {
        try
        {
            m_NewConfig = JSON_MAPPER.convertValue(node, ModelDebugConfig.class);
            if (m_NewConfig != null)
            {
                ModelDebugConfigVerifier.verify(m_NewConfig);
            }
        }
        catch (IllegalArgumentException e)
        {
            throw new JobConfigurationParseException(
                    Messages.getMessage(Messages.JOB_CONFIG_UPDATE_MODEL_DEBUG_CONFIG_PARSE_ERROR),
                    e.getCause(), ErrorCodes.INVALID_VALUE);
        }
    }

    @Override
    void commit() throws JobConfigurationException, UnknownJobException
    {
        jobManager().setModelDebugConfig(jobId(), m_NewConfig);
        write(m_NewConfig);
    }

    private void write(ModelDebugConfig modelDebugConfig) throws JobConfigurationException
    {
        m_ConfigWriter.write("[modelDebugConfig]\n");
        if (modelDebugConfig == null)
        {
            modelDebugConfig = new ModelDebugConfig(null, -1.0, null);
        }
        try
        {
            new ModelDebugConfigWriter(modelDebugConfig, m_ConfigWriter).write();
        }
        catch (IOException e)
        {
            throw new JobConfigurationException("Failed to write", null, e);
        }
    }
}
