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

import java.io.IOException;
import java.io.StringWriter;

import com.fasterxml.jackson.databind.JsonNode;
import com.prelert.job.ModelDebugConfig;
import com.prelert.job.UnknownJobException;
import com.prelert.job.config.verification.JobConfigurationException;
import com.prelert.job.config.verification.ModelDebugConfigVerifier;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.manager.JobManager;
import com.prelert.job.messages.Messages;
import com.prelert.job.process.writer.ModelDebugConfigWriter;
import com.prelert.rs.provider.JobConfigurationParseException;

class ModelDebugConfigUpdater extends AbstractUpdater
{
    private final StringWriter m_ConfigWriter;

    public ModelDebugConfigUpdater(JobManager jobManager, String jobId, StringWriter configWriter)
    {
        super(jobManager, jobId);
        m_ConfigWriter = configWriter;
    }

    @Override
    void update(JsonNode node) throws UnknownJobException, JobConfigurationException
    {
        ModelDebugConfig modelDebugConfig = null;
        try
        {
            modelDebugConfig = JSON_MAPPER.convertValue(node, ModelDebugConfig.class);
            if (modelDebugConfig != null)
            {
                ModelDebugConfigVerifier.verify(modelDebugConfig);
            }
            jobManager().setModelDebugConfig(jobId(), modelDebugConfig);
        }
        catch (IllegalArgumentException e)
        {
            throw new JobConfigurationParseException(
                    Messages.getMessage(Messages.JOB_CONFIG_UPDATE_MODEL_DEBUG_CONFIG_PARSE_ERROR),
                    e.getCause(), ErrorCodes.INVALID_VALUE);
        }

        write(modelDebugConfig);
    }

    private void write(ModelDebugConfig modelDebugConfig) throws JobConfigurationException
    {
        m_ConfigWriter.write("[modelDebugConfig]\n");
        if (modelDebugConfig == null)
        {
            modelDebugConfig = new ModelDebugConfig(-1.0, null);
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
