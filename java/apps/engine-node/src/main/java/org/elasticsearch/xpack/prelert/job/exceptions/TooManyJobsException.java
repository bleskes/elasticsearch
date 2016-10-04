
package org.elasticsearch.xpack.prelert.job.exceptions;

import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;

/**
 * This type of exception represents an error where an operation
 * would result in too many jobs running at the same time.
 */
public class TooManyJobsException extends JobException
{
    private static final long serialVersionUID = 8503362038035845948L;

    private final int limit;

    /**
     * Create a new TooManyJobsException with an error code
     *
     * @param limit The limit on the number of jobs
     * @param message Details of error explaining the context
     * @param errorCode
     */
    public TooManyJobsException(int limit, String message, ErrorCodes errorCode)
    {
        super(message, errorCode);
        this.limit = limit;
    }


    public TooManyJobsException(int limit, String message, ErrorCodes errorCode,
            Throwable cause)
    {
        super(message, errorCode, cause);
        this.limit = limit;
    }


    /**
     * Get the limit on the number of concurrently running jobs.
     * @return
     */
    public int getLimit()
    {
        return limit;
    }
}
