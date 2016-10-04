
package org.elasticsearch.xpack.prelert.job.exceptions;

import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;

/**
 * Represents the invalid configuration of a job.
 */
public class JobConfigurationException extends JobException
{
    private static final long serialVersionUID = -563428978300447381L;

    /**
     * Create a new JobConfigurationException.
     *
     * @param message Details of error explaining the context
     * @param errorCode See {@linkplain org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes}
     */
    public JobConfigurationException(String message, ErrorCodes errorCode)
    {
        super(message, errorCode);
    }

    public JobConfigurationException(String message, ErrorCodes errorCode, Throwable cause)
    {
        super(message, errorCode, cause);
    }
}