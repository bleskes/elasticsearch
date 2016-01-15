package com.prelert.job.manager;

import com.prelert.job.JobException;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.messages.Messages;

public class CannotStartSchedulerWhileItIsStoppingException extends JobException
{
    private static final long serialVersionUID = -2359537142811349135L;

    public CannotStartSchedulerWhileItIsStoppingException(String jobId)
    {
        super(Messages.getMessage(Messages.JOB_SCHEDULER_CANNOT_START_WHILE_STOPPING, jobId),
                ErrorCodes.BUCKET_RESET_NOT_SUPPORTED);
    }
}
