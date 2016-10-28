
package org.elasticsearch.xpack.prelert.job.exceptions;

import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;

/**
 * This type of exception represents an error where
 * an operation uses a <i>JobId</i> that does not exist.
 */
public class UnknownJobException extends JobException
{
    private static final long serialVersionUID = 8603362038035845948L;

    private final String jobId;

    /**
     * Create with the default message and error code
     * set to ErrorCode.MISSING_JOB_ERROR
     * @param jobId the jobId
     */
    public UnknownJobException(String jobId)
    {
        super(Messages.getMessage(Messages.JOB_UNKNOWN_ID, jobId), ErrorCodes.MISSING_JOB_ERROR);
        this.jobId = jobId;
    }

    /**
     * Create a new UnknownJobException with an error code
     *
     * @param jobId The Job Id that could not be found
     * @param message Details of error explaining the context
     * @param errorCode the error code
     */
    public UnknownJobException(String jobId, String message, ErrorCodes errorCode)
    {
        super(message, errorCode);
        this.jobId = jobId;
    }

    public UnknownJobException(String jobId, String message, ErrorCodes errorCode,
                               Throwable cause)
    {
        super(message, errorCode, cause);
        this.jobId = jobId;
    }

    /**
     * Get the unknown <i>JobId</i> that was the source of the error.
     */
    public String getJobId()
    {
        return jobId;
    }
}
