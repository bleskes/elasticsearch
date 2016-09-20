
package org.elasticsearch.xpack.prelert.job;

import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.errorcodes.HasErrorCode;

/**
 * General job exception class with a specific error code and message.
 */
public class JobException extends Exception implements HasErrorCode
{
    private static final long serialVersionUID = -5289885963015348819L;

    private final ErrorCodes errorCode;

    public JobException(String message, ErrorCodes errorCode)
    {
        super(message);
        this.errorCode = errorCode;
    }

    public JobException(String message, ErrorCodes errorCode, Throwable cause)
    {
        super(message, cause);
        this.errorCode = errorCode;
    }

    @Override
    public ErrorCodes getErrorCode()
    {
        return errorCode;
    }
}
