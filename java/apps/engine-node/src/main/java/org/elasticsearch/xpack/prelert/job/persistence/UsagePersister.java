
package org.elasticsearch.xpack.prelert.job.persistence;

import org.elasticsearch.xpack.prelert.job.exceptions.JobException;

/**
 * Interface for classes that persist usage information
 */
public interface UsagePersister
{
    /**
     * Persist the usage info.
     *
     * @throws JobException IF there was an error persisting
     */
    public void persistUsage(String jobId, long bytesRead, long fieldsRead, long recordsRead) throws JobException;
}
