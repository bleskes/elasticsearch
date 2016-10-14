
package org.elasticsearch.xpack.prelert.job.persistence;

import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.xpack.prelert.job.ModelSizeStats;
import org.elasticsearch.xpack.prelert.job.ModelSnapshot;
import org.elasticsearch.xpack.prelert.job.exceptions.UnknownJobException;
import org.elasticsearch.xpack.prelert.job.persistence.InfluencersQueryBuilder.InfluencersQuery;
import org.elasticsearch.xpack.prelert.job.results.*;

import java.util.Optional;

public interface JobResultsProvider
{
    /**
     * Search for buckets with the parameters in the {@link BucketsQueryBuilder}
     * @param jobId
     * @param query
     * @return QueryPage of Buckets
     * @throws ResourceNotFoundException If the job id is no recognised
     */
    QueryPage<Bucket> buckets(String jobId, BucketsQueryBuilder.BucketsQuery query)
            throws ResourceNotFoundException;

    /**
     * Get the bucket at time <code>timestampMillis</code> from the job.
     *
     * @param jobId
     * @param query The bucket query
     * @return Optional Bucket
     * @throws UnknownJobException If the job id is no recognised
     */
    Optional<Bucket> bucket(String jobId, BucketQueryBuilder.BucketQuery query)
            throws ResourceNotFoundException;

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
     */
    QueryPage<CategoryDefinition> categoryDefinitions(String jobId, int skip, int take);

    /**
     * Get the specific CategoryDefinition for the given job and category id.
     *
     * @param jobId
     * @param categoryId Unique id
     * @return Optional CategoryDefinition
     */
    Optional<CategoryDefinition> categoryDefinition(String jobId, String categoryId);

    /**
     * Search for anomaly records with the parameters in the
     * {@link org.elasticsearch.xpack.prelert.job.persistence.RecordsQueryBuilder.RecordsQuery}
     * @param jobId
     * @param query
     * @return QueryPage of AnomalyRecords
     */
    QueryPage<AnomalyRecord> records(String jobId, RecordsQueryBuilder.RecordsQuery query);

    /**
     * Return a page of influencers for the given job and within the given date
     * range
     *
     * @param jobId
     *            The job ID for which influencers are requested
     * @param skip
     *            Skip the first N Buckets. This parameter is for paging if not
     *            required set to 0.
     * @param take
     *            Maximum number of influencers to insert in the page
     * @param startEpochMs
     *            The start influencer timestamp. An influencer with this
     *            timestamp will be included in the results. If 0 all buckets up
     *            to <code>endEpochMs</code> are returned
     * @param endEpochMs
     *            The end bucket timestamp buckets up to but NOT including this
     *            timestamp are returned. If 0 all buckets from
     *            <code>startEpochMs</code> are returned
     * @param sortField
     *            The field to sort influencers by. If <code>null</code> no sort
     *            is applied
     * @param sortDescending
     *            Sort in descending order
     * @param anomalyScoreFilter
     *            Return only influencers with an anomalyScore >= this value
     * @param includeInterim
     *            Include interim results
     * @return QueryPage of Influencer
     * @throws ResourceNotFoundException
     */
    QueryPage<Influencer> influencers(String jobId, InfluencersQuery query)
            throws ResourceNotFoundException;

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
