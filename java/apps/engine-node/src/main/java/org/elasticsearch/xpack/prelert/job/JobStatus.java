
package org.elasticsearch.xpack.prelert.job;

import java.util.Arrays;

/**
 * Jobs whether running or complete are in one of these states.
 * When a job is created it is initialised in to the status closed
 * i.e. it is not running.
 */
public enum JobStatus
{
    RUNNING, CLOSING, CLOSED, FAILED, PAUSING, PAUSED;

    /**
     * @return {@code true} if status matches any of the given {@code candidates}
     */
    public boolean isAnyOf(JobStatus... candidates)
    {
        return Arrays.stream(candidates).anyMatch(candidate -> this == candidate);
    }
}
