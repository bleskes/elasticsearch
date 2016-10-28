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
