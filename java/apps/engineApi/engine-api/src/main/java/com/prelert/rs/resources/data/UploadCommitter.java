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

package com.prelert.rs.resources.data;

import java.util.Objects;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.apache.log4j.Logger;

import com.prelert.job.JobInUseException;
import com.prelert.job.UnknownJobException;
import com.prelert.job.manager.JobManager;
import com.prelert.job.process.NativeProcessRunException;

public class UploadCommitter {

    private static final Logger LOGGER = Logger.getLogger(UploadCommitter.class);

    private final JobManager m_JobManager;

    public UploadCommitter(JobManager jobManager)
    {
        m_JobManager = Objects.requireNonNull(jobManager);
    }
    /**
     * Retires the job cleans up after this
     * @param jobId
     * @return
     * @throws UnknownJobException
     * @throws NativeProcessRunException
     * @throws JobInUseException
     */
    @Path("/{jobId}/close")
    @POST
    public void commitUpload(String jobId) throws UnknownJobException,
            NativeProcessRunException, JobInUseException
    {
        LOGGER.debug("Post to close data upload for job " + jobId);
        m_JobManager.finishJob(jobId);
        LOGGER.debug("Process finished successfully, Job Id = '" + jobId + "'");
    }
}
