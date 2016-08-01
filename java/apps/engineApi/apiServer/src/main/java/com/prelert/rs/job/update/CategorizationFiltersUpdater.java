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
 * are owned by Prelert Ltd. No part of this source code    *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/

package com.prelert.rs.job.update;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.prelert.job.AnalysisConfig;
import com.prelert.job.JobDetails;
import com.prelert.job.JobException;
import com.prelert.job.config.verification.AnalysisConfigVerifier;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobConfigurationException;
import com.prelert.job.manager.JobManager;
import com.prelert.job.messages.Messages;

class CategorizationFiltersUpdater extends AbstractUpdater
{
    private List<String> m_NewCategorizationFilters;

    public CategorizationFiltersUpdater(JobManager jobManager, JobDetails job, String updateKey)
    {
        super(jobManager, job, updateKey);
        m_NewCategorizationFilters = null;
    }

    @Override
    void prepareUpdate(JsonNode node) throws JobConfigurationException
    {
        if (node.isNull())
        {
            return;
        }
        if (!node.isArray())
        {
            throw new JobConfigurationException(
                    Messages.getMessage(Messages.JOB_CONFIG_UPDATE_CATEGORIZATION_FILTERS_INVALID,
                            node.toString()),
                    ErrorCodes.INVALID_VALUE);
        }
        parseStringArray(node);
        verifyNewCategorizationFilters();
    }

    private void parseStringArray(JsonNode arrayNode) throws JobConfigurationException
    {
        Iterator<JsonNode> iterator = arrayNode.elements();
        m_NewCategorizationFilters = iterator.hasNext() ? new ArrayList<>() : null;
        while (iterator.hasNext())
        {
            JsonNode elementNode = iterator.next();
            if (elementNode.isTextual())
            {
                m_NewCategorizationFilters.add(elementNode.asText());
            }
            else
            {
                throw new JobConfigurationException(
                        Messages.getMessage(Messages.JOB_CONFIG_UPDATE_CATEGORIZATION_FILTERS_INVALID,
                                arrayNode.toString()),
                        ErrorCodes.INVALID_VALUE);
            }
        }
    }

    private void verifyNewCategorizationFilters() throws JobConfigurationException
    {
        AnalysisConfig analysisConfig = job().getAnalysisConfig();
        analysisConfig.setCategorizationFilters(m_NewCategorizationFilters);
        AnalysisConfigVerifier.verify(analysisConfig);
    }

    @Override
    void commit() throws JobException
    {
        if (jobManager().updateCategorizationFilters(jobId(), m_NewCategorizationFilters) == false)
        {
            throw new JobConfigurationException(
                    Messages.getMessage(Messages.JOB_CONFIG_UPDATE_FAILED),
                    ErrorCodes.UNKNOWN_ERROR);
        }
    }
}
