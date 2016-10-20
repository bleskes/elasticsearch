
package org.elasticsearch.xpack.prelert.job.persistence;

/**
 * Create a {@linkplain JobResultsPersister}
 */
public interface JobResultsPeristerFactory {
    /**
     * Get a {@linkplain JobResultsPersister}
     * @param jobId The job to create the persister for.
     * @return The results persister
     */
    JobResultsPersister jobResultsPersister(String jobId);
}
