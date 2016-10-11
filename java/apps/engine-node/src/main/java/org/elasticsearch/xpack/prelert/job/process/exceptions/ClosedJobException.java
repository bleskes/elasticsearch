
package org.elasticsearch.xpack.prelert.job.process.exceptions;


import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;

/**
 * Exception thrown when a job is expected to be running
 * but is closed
 */
public class ClosedJobException extends JobException {
    private static final long serialVersionUID = 5491387807381215423L;

    public ClosedJobException(String message) {
        super(message, ErrorCodes.JOB_NOT_RUNNING);
    }
}
