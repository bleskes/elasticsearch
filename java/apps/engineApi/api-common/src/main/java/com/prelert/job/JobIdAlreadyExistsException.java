/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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

package com.prelert.job;

import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.messages.Messages;


/**
 * Job ids (names) must be unique no 2 jobs can have the same id.
 */
public class JobIdAlreadyExistsException extends JobException
{
    private static final long serialVersionUID = 8656604180755905746L;

    private final String m_JobId;

    /**
     * Create a new JobIdAlreadyExistsException with the error code
     * and Id (job name)
     *
     * @param jobId The Job Id that could not be found
     */
    public JobIdAlreadyExistsException(String jobId)
    {
        super(Messages.getMessage(Messages.JOB_CONFIG_ID_ALREADY_TAKEN, jobId),
                ErrorCodes.JOB_ID_TAKEN);
        m_JobId = jobId;
    }

    public String getAlias()
    {
        return m_JobId;
    }
}
