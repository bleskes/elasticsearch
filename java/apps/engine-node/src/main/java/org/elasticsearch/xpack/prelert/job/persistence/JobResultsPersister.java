/****************************************************************************
 *                                                                          *
 * Copyright 2015-2016 Prelert Ltd                                          *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 *    http://www.apache.org/licenses/LICENSE-2.0                            *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 *                                                                          *
 ***************************************************************************/

package org.elasticsearch.xpack.prelert.job.persistence;

import org.elasticsearch.xpack.prelert.job.JobException;
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
