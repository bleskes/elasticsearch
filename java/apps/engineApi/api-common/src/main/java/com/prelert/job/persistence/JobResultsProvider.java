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

package com.prelert.job.persistence;

import java.util.Optional;

import com.prelert.app.Shutdownable;
import com.prelert.job.ModelSizeStats;
import com.prelert.job.ModelSnapshot;
import com.prelert.job.UnknownJobException;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.CategoryDefinition;
import com.prelert.job.results.Influencer;
import com.prelert.job.results.ModelDebugOutput;

public interface JobResultsProvider extends Shutdownable
{
    /**
     * Search for buckets with the parameters in the {@link BucketsQueryBuilder}
     * @param jobId
     * @param query
     * @return QueryPage of Buckets
     * @throws UnknownJobException If the job id is no recognised
     */
    QueryPage<Bucket> buckets(String jobId, BucketsQueryBuilder.BucketsQuery query)
            throws UnknownJobException;

    /**
     * Get the bucket at time <code>timestampMillis</code> from the job.
     *
     * @param jobId
     * @param timestampMillis Bucket timestamp as epoch milliseconds
     * @param expand Include anomaly records
     * @param includeInterim Include interim results
     * @return Optional Bucket
     * @throws UnknownJobException If the job id is no recognised
     */
    Optional<Bucket> bucket(String jobId, long timestampMillis, boolean expand,
            boolean includeInterim) throws UnknownJobException;

    /**
     * Returns a {@link BatchedDocumentsIterator} that allows querying
     * and iterating over a large number of buckets of the given job
     *
     * @param jobId the id of the job for which buckets are requested
     * @return a bucket {@link BatchedDocumentsIterator}
     */
    BatchedDocumentsIterator<Bucket> newBatchedBucketsIterator(String jobId);

    /**
     * Expand a bucket to include the associated records.
     *
     * @param jobId
     * @param includeInterim Include interim results
     * @param bucket The bucket to be expanded
     * @return The number of records added to the bucket
     * @throws UnknownJobException If the job id is no recognised
     */
    int expandBucket(String jobId, boolean includeInterim, Bucket bucket) throws UnknownJobException;

    /**
     * Get a page of {@linkplain CategoryDefinition}s for the given <code>jobId</code>.
     *
     * @param jobId
     * @param skip Skip the first N categories. This parameter is for paging
     * @param take Take only this number of categories
     * @return QueryPage of CategoryDefinition
     * @throws UnknownJobException If the job id is no recognised
     */
    QueryPage<CategoryDefinition> categoryDefinitions(String jobId, int skip, int take)
            throws UnknownJobException;

    /**
     * Get the specific CategoryDefinition for the given job and category id.
     *
     * @param jobId
     * @param categoryId Unique id
     * @return Optional CategoryDefinition
     * @throws UnknownJobException
     */
    Optional<CategoryDefinition> categoryDefinition(String jobId, String categoryId)
            throws UnknownJobException;

    /**
     * Search for anomaly records with the parameters in the
     * {@link RecordsQueryBuilder.com.prelert.job.persistence.RecordsQueryBuilder.RecordsQuery}
     * @param jobId
     * @param query
     * @return QueryPage of AnomalyRecords
     * @throws UnknownJobException If the job id is no recognised
     */
    QueryPage<AnomalyRecord> records(String jobId, RecordsQueryBuilder.RecordsQuery query)
        throws UnknownJobException;

    /**
     * Return a page of influencers for the given job.
     *
     * @param jobId
     * @param skip Skip the first N records. This parameter is for paging
     * if not required set to 0.
     * @param take Take only this number of records
     * @param includeInterim Include interim results
     * @return QueryPage of Influencer
     * @throws UnknownJobException
     */
    QueryPage<Influencer> influencers(String jobId, int skip, int take, boolean includeInterim)
    throws UnknownJobException;

    /**
     * Return a page of influencers for the given job and within the given date range
     *
     * @param jobId The job ID for which influencers are requested
     * @param skip Skip the first N Buckets. This parameter is for paging
     * if not required set to 0.
     * @param take Maximum number of influencers to insert in the page
     * @param startEpochMs The start influencer timestamp. An influencer with this timestamp will be
     * included in the results. If 0 all buckets up to <code>endEpochMs</code>
     * are returned
     * @param endEpochMs The end bucket timestamp buckets up to but NOT including this
     * timestamp are returned. If 0 all buckets from <code>startEpochMs</code>
     * are returned
     * @param sortField The field to sort influencers by. If <code>null</code> no sort is applied
     * @param sortDescending Sort in descending order
     * @param anomalyScoreThreshold Return only influencers with an anomalyScore >= this value
     * @param includeInterim Include interim results
     * @return QueryPage of Influencer
     * @throws UnknownJobException
     */
    QueryPage<Influencer> influencers(String jobId, int skip, int take, long startEpochMs,
            long endEpochMs, String sortField, boolean sortDescending, double anomalyScoreFilter,
            boolean includeInterim)
            throws UnknownJobException;

    /**
     * Get the influencer for the given job for id
     *
     * @param jobId
     * @param influencerId The unique influencer Id
     * @return Optional Influencer
     */
    Optional<Influencer> influencer(String jobId, String influencerId);

    /**
     * Returns a {@link BatchedDocumentsIterator} that allows querying
     * and iterating over a large number of influencers of the given job
     *
     * @param jobId the id of the job for which influencers are requested
     * @return an influencer {@link BatchedDocumentsIterator}
     */
    BatchedDocumentsIterator<Influencer> newBatchedInfluencersIterator(String jobId);

    /**
     * Returns a {@link BatchedDocumentsIterator} that allows querying
     * and iterating over a number of model snapshots of the given job
     *
     * @param jobId the id of the job for which model snapshots are requested
     * @return a model snapshot {@link BatchedDocumentsIterator}
     */
    BatchedDocumentsIterator<ModelSnapshot> newBatchedModelSnapshotIterator(String jobId);

    /**
     * Returns a {@link BatchedDocumentsIterator} that allows querying
     * and iterating over a number of ModelDebugOutputs of the given job
     *
     * @param jobId the id of the job for which model snapshots are requested
     * @return a model snapshot {@link BatchedDocumentsIterator}
     */
    BatchedDocumentsIterator<ModelDebugOutput> newBatchedModelDebugOutputIterator(String jobId);

    /**
     * Returns a {@link BatchedDocumentsIterator} that allows querying
     * and iterating over a number of ModelSizeStats of the given job
     *
     * @param jobId the id of the job for which model snapshots are requested
     * @return a model snapshot {@link BatchedDocumentsIterator}
     */
    BatchedDocumentsIterator<ModelSizeStats> newBatchedModelSizeStatsIterator(String jobId);
}
