
package org.elasticsearch.xpack.prelert.job.persistence;

/**
 * Get a {@linkplain JobProvider}
 * This may create a new JobProvider or return an existing
 * one if it is thread safe and shareable.
 */
public interface JobProviderFactory
{
    /**
     * Get a {@linkplain JobProvider}
     */
    JobProvider jobProvider();
}
