
package org.elasticsearch.xpack.prelert.job.process.exceptions;

import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;

/**
 * Exception thrown when there is an error running
 * a native process (autodetect).
 */
public class NativeProcessRunException extends JobException {
    private static final long serialVersionUID = 5722287151589093943L;

    /**
     * Create exception with error code ErrorCode.NATIVE_PROCESS_ERROR
     *
     * @param message
     */
    public NativeProcessRunException(String message) {
        super(message, ErrorCodes.NATIVE_PROCESS_ERROR);
    }

    public NativeProcessRunException(String message, ErrorCodes errorCode) {
        super(message, errorCode);
    }

    public NativeProcessRunException(String message, ErrorCodes errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}