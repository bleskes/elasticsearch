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

class RenormalizationWindowUpdater extends AbstractUpdater
{
    public RenormalizationWindowUpdater(JobManager jobManager, String jobId)
    {
        super(jobManager, jobId);
    }

    @Override
    void update(JsonNode node) throws UnknownJobException, JobConfigurationException
    {
        if (node.isIntegralNumber() || node.isNull())
        {
            Long renormalizationWindow = null;
            if (node.isIntegralNumber())
            {
                renormalizationWindow = node.asLong();
            }
            jobManager().setRenormalizationWindow(jobId(), renormalizationWindow);
            return;
        }
        throw new JobConfigurationException(
                Messages.getMessage(Messages.JOB_CONFIG_UPDATE_RENORMALIZATION_WINDOW_INVALID),
                ErrorCodes.INVALID_VALUE);
    }
}
