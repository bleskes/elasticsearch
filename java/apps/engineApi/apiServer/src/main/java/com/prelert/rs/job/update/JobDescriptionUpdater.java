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
import com.prelert.job.JobDetails;
import com.prelert.job.UnknownJobException;
import com.prelert.job.config.verification.JobConfigurationException;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.manager.JobManager;
import com.prelert.job.messages.Messages;

class JobDescriptionUpdater extends AbstractUpdater
{
    private String m_NewDescription;

    public JobDescriptionUpdater(JobManager jobManager, JobDetails job, String updateKey)
    {
        super(jobManager, job, updateKey);
    }

    @Override
    void prepareUpdate(JsonNode node) throws JobConfigurationException
    {
        if (node.isTextual() == false)
        {
            throw new JobConfigurationException(
                    Messages.getMessage(Messages.JOB_CONFIG_UPDATE_DESCRIPTION_INVALID),
                    ErrorCodes.INVALID_VALUE);
        }
        m_NewDescription = node.asText();
    }

    @Override
    void commit() throws UnknownJobException
    {
        jobManager().setDescription(jobId(), m_NewDescription);
    }
}
