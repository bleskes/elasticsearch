
package org.elasticsearch.xpack.prelert.job.persistence;

/**
 * Get a {@linkplain JobResultsPersister}
 * This may create a new JobResultsPersister or return an existing
 * one if it is thread safe and shareable.
 */
public interface JobResultsPeristerFactory
{
    /**
     * Get a {@linkplain JobResultsPersister}
     * @param jobId The to create the persister for.
     * @return
     */
    JobResultsPersister jobResultsPersister(String jobId);
}
