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

import com.fasterxml.jackson.databind.JsonNode;
import com.prelert.job.UnknownJobException;
import com.prelert.job.config.verification.JobConfigurationException;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.manager.JobManager;
import com.prelert.job.messages.Messages;

class ResultsRetentionDaysUpdater extends AbstractUpdater
{
    private Long m_NewRetentionDays;

    public ResultsRetentionDaysUpdater(JobManager jobManager, String jobId)
    {
        super(jobManager, jobId);
    }

    @Override
    void prepareUpdate(JsonNode node) throws UnknownJobException, JobConfigurationException
    {
        if (node.isIntegralNumber() || node.isNull())
        {
            m_NewRetentionDays = node.isIntegralNumber() ? node.asLong() : null;
            if (m_NewRetentionDays != null && m_NewRetentionDays < 0)
            {
                throwInvalidValue();
            }
        }
        else
        {
            throwInvalidValue();
        }
    }

    private void throwInvalidValue() throws JobConfigurationException
    {
        throw new JobConfigurationException(
                Messages.getMessage(Messages.JOB_CONFIG_UPDATE_RESULTS_RETENTION_DAYS_INVALID),
                ErrorCodes.INVALID_VALUE);
    }

    @Override
    void commit() throws UnknownJobException
    {
        jobManager().setResultsRetentionDays(jobId(), m_NewRetentionDays);
    }
}
