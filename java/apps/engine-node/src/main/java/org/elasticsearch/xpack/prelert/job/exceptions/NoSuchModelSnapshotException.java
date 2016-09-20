package org.elasticsearch.xpack.prelert.job.exceptions;


import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;

/**
 * This type of exception represents an error where
 * an operation uses a <i>SnapshotId</i> that does not exist.
 */
public class NoSuchModelSnapshotException extends JobException
{
    private static final long serialVersionUID = -2359537142813149135L;

    public NoSuchModelSnapshotException(String jobId)
    {
        super(Messages.getMessage(Messages.REST_NO_SUCH_MODEL_SNAPSHOT, jobId),
                ErrorCodes.NO_SUCH_MODEL_SNAPSHOT);
    }
}
