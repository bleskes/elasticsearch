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

import java.util.Arrays;

import com.fasterxml.jackson.databind.JsonNode;
import com.prelert.job.IgnoreDowntime;
import com.prelert.job.UnknownJobException;
import com.prelert.job.config.verification.JobConfigurationException;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.manager.JobManager;
import com.prelert.job.messages.Messages;
import com.prelert.rs.provider.JobConfigurationParseException;

class IgnoreDowntimeUpdater extends AbstractUpdater
{
    private IgnoreDowntime m_NewIgnoreDowntime;

    public IgnoreDowntimeUpdater(JobManager jobManager, String jobId, String updateKey)
    {
        super(jobManager, jobId, updateKey);
    }

    @Override
    void prepareUpdate(JsonNode node) throws JobConfigurationException
    {
        try
        {
            m_NewIgnoreDowntime = JSON_MAPPER.convertValue(node, IgnoreDowntime.class);
        }
        catch (IllegalArgumentException e)
        {
            throw new JobConfigurationParseException(
                    Messages.getMessage(Messages.JOB_CONFIG_UPDATE_IGNORE_DOWNTIME_PARSE_ERROR,
                            Arrays.toString(IgnoreDowntime.values()), node.toString()),
                    e.getCause(), ErrorCodes.INVALID_VALUE);
        }
    }

    @Override
    void commit() throws UnknownJobException
    {
        jobManager().updateIgnoreDowntime(jobId(), m_NewIgnoreDowntime);
    }
}
