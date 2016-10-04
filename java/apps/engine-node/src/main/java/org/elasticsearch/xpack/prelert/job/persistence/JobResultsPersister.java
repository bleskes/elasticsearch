
package org.elasticsearch.xpack.prelert.job.persistence;

import org.elasticsearch.xpack.prelert.job.ModelSizeStats;
import org.elasticsearch.xpack.prelert.job.ModelSnapshot;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;
import org.elasticsearch.xpack.prelert.job.exceptions.UnknownJobException;
import org.elasticsearch.xpack.prelert.job.quantiles.Quantiles;
import org.elasticsearch.xpack.prelert.job.results.Bucket;
import org.elasticsearch.xpack.prelert.job.results.CategoryDefinition;
import org.elasticsearch.xpack.prelert.job.results.Influencer;
import org.elasticsearch.xpack.prelert.job.results.ModelDebugOutput;

/**
 * Interface for classes that persist {@linkplain Bucket Buckets} and
 * {@linkplain Quantiles Quantiles}
 */
public interface JobResultsPersister
{
    /**
     * Persist the result bucket
     * @param bucket
     */
    void persistBucket(Bucket bucket);

    /**
     * Persist the category definition
     * @param category The category to be persisted
     */
    void persistCategoryDefinition(CategoryDefinition category);

    /**
     * Persist the quantiles
     * @param quantiles
     */
    void persistQuantiles(Quantiles quantiles);

    /**
     * Persist a model snapshot description
     * @param modelSnapshot
     */
    void persistModelSnapshot(ModelSnapshot modelSnapshot);

    /**
     * Persist the memory usage data
     * @param modelSizeStats
     */
    void persistModelSizeStats(ModelSizeStats modelSizeStats);

    /**
     * Persist model debug output
     * @param modelDebugOutput
     */
    void persistModelDebugOutput(ModelDebugOutput modelDebugOutput);

    /**
     * Persist the influencer
     * @param influencer
     */
    void persistInfluencer(Influencer influencer);

    /**
     * Increment the jobs bucket result count by <code>count</code>
     * @param count
     * @throws JobException If there was an error updating
     */
    void incrementBucketCount(long count) throws JobException;

    /**
     * Update the last bucket's processing time
     * @param timeMs
     * @throws JobException If there was an error updating
     */
    void updateAverageBucketProcessingTime(long timeMs) throws JobException;

    /**
     * Delete any existing interim results
     */
    void deleteInterimResults();

    /**
     * Once all the job data has been written this function will be
     * called to commit the data if the implementing persister requires
     * it.
     *
     * @return True if successful
     */
    boolean commitWrites();
}
