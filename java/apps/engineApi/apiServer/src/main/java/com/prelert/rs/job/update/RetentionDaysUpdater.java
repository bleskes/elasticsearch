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
import com.prelert.job.UnknownJobException;
import com.prelert.job.config.verification.JobConfigurationException;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.manager.JobManager;

abstract class RetentionDaysUpdater extends AbstractUpdater
{
    private Long m_NewRetentionDays;
    private String m_InvalidValidMessage;

    public RetentionDaysUpdater(JobManager jobManager, String jobId,
            String invalidValidMessage)
    {
        super(jobManager, jobId);
        m_InvalidValidMessage = invalidValidMessage;
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

    protected Long newRetentionDays()
    {
        return m_NewRetentionDays;
    }

    private void throwInvalidValue() throws JobConfigurationException
    {
        throw new JobConfigurationException(
                m_InvalidValidMessage, ErrorCodes.INVALID_VALUE);
    }
}
