
package org.elasticsearch.xpack.prelert.job.exceptions;

import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;

/**
 * This exception is thrown if an operation is attempted on a job
 * that can't be executed as the job is already being used.
 */
public class JobInUseException extends JobException
{
    private static final long serialVersionUID = -2759814168552580059L;

    private final String host;

    /**
     * Create a new JobInUseException.
     *
     * @param message Details of error explaining the context
     * @param errorCode
     * @see ErrorCodes
     */
    public JobInUseException(String message, ErrorCodes errorCode)
    {
        this(message, errorCode, null, null);
    }

    /**
     *
     * @param message Details of error explaining the context
     * @param errorCode
     * @param hostname The host the job is running on
     */
    public JobInUseException(String message, ErrorCodes errorCode, String hostname)
    {
        this(message, errorCode, hostname, null);
    }

    public JobInUseException(String message, ErrorCodes errorCode, Throwable cause)
    {
        this(message, errorCode, null, cause);
    }

    public JobInUseException(String message, ErrorCodes errorCode, String hostname, Throwable cause)
    {
        super(message, errorCode, cause);
        host = hostname;
    }

    public String getHost()
    {
        return host;
    }
}

