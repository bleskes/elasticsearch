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
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.exceptions.JobConfigurationException;
import com.prelert.job.manager.JobManager;
import com.prelert.job.messages.Messages;

abstract class AbstractLongUpdater extends AbstractUpdater
{
    private Long m_NewValue;
    private long m_MinVal;

    public AbstractLongUpdater(JobManager jobManager, JobDetails job, String updateKey, long minVal)
    {
        super(jobManager, job, updateKey);
        m_MinVal = minVal;
    }

    @Override
    void prepareUpdate(JsonNode node) throws JobConfigurationException
    {
        if (node.isIntegralNumber() || node.isNull())
        {
            m_NewValue = node.isIntegralNumber() ? node.asLong() : null;
            if (m_NewValue != null && m_NewValue < m_MinVal)
            {
                throwInvalidValue();
            }
        }
        else
        {
            throwInvalidValue();
        }
    }

    protected Long getNewValue()
    {
        return m_NewValue;
    }

    private void throwInvalidValue() throws JobConfigurationException
    {
        throw new JobConfigurationException(Messages.getMessage(getInvalidMessageKey()),
                ErrorCodes.INVALID_VALUE);
    }

    protected abstract String getInvalidMessageKey();
}
