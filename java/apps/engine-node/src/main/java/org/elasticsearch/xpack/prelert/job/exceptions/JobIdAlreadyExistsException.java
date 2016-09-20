package org.elasticsearch.xpack.prelert.job.exceptions;


import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;

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
