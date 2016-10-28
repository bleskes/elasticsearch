
package org.elasticsearch.xpack.prelert.job.exceptions;

import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;

/**
 * This type of exception represents an error where an operation
 * would result in too many jobs running at the same time.
 */
public class LicenseViolationException extends JobException
{
    private static final long serialVersionUID = 7225703980253532853L;

    /**
     * Create a new LicenseViolationException with an error code
     *
     * @param message Details of error explaining the context
     * @param errorCode the error code
     */
    public LicenseViolationException(String message, ErrorCodes errorCode)
    {
        super(message, errorCode);
    }
}
