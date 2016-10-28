
package org.elasticsearch.xpack.prelert.job.persistence;

import org.elasticsearch.xpack.prelert.job.ModelSizeStats;
import org.elasticsearch.xpack.prelert.job.ModelSnapshot;
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
     */
    void persistBucket(Bucket bucket);

    /**
     * Persist the category definition
     * @param category The category to be persisted
     */
    void persistCategoryDefinition(CategoryDefinition category);

    /**
     * Persist the quantiles
     */
    void persistQuantiles(Quantiles quantiles);

    /**
     * Persist a model snapshot description
     */
    void persistModelSnapshot(ModelSnapshot modelSnapshot);

    /**
     * Persist the memory usage data
     */
    void persistModelSizeStats(ModelSizeStats modelSizeStats);

    /**
     * Persist model debug output
     */
    void persistModelDebugOutput(ModelDebugOutput modelDebugOutput);

    /**
     * Persist the influencer
     */
    void persistInfluencer(Influencer influencer);

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
