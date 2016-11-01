/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.prelert.job.exceptions;


import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;

/**
 * Job ids (names) must be unique no 2 jobs can have the same id.
 */
public class JobIdAlreadyExistsException extends JobException
{

    private final String jobId;

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
        this.jobId = jobId;
    }

    public String getAlias()
    {
        return jobId;
    }
}
