
package org.elasticsearch.xpack.prelert.job.persistence;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.ShardOperationFailedException;
import org.elasticsearch.action.admin.indices.forcemerge.ForceMergeAction;
import org.elasticsearch.action.admin.indices.forcemerge.ForceMergeResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteAction;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.search.SearchAction;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.xpack.prelert.job.ModelSizeStats;
import org.elasticsearch.xpack.prelert.job.ModelSnapshot;
import org.elasticsearch.xpack.prelert.job.ModelState;
import org.elasticsearch.xpack.prelert.job.results.*;

import java.util.Objects;
import java.util.function.LongSupplier;

public class ElasticsearchBulkDeleter implements JobDataDeleter
{
    private static final Logger LOGGER = Loggers.getLogger(ElasticsearchBulkDeleter.class);

    private static final int SCROLL_SIZE = 1000;
    private static final String SCROLL_CONTEXT_DURATION = "5m";

    private final Client client;
    private final ElasticsearchJobId jobId;
    private final BulkRequestBuilder bulkRequestBuilder;
    private long deletedBucketCount;
    private long deletedRecordCount;
    private long deletedBucketInfluencerCount;
    private long deletedInfluencerCount;
    private long deletedModelSnapshotCount;
    private long deletedModelStateCount;
    private boolean quiet;

    public ElasticsearchBulkDeleter(Client client, ElasticsearchJobId jobId, boolean quiet)
    {
        this.client = Objects.requireNonNull(client);
        this.jobId = Objects.requireNonNull(jobId);
        bulkRequestBuilder = client.prepareBulk();
        deletedBucketCount = 0;
        deletedRecordCount = 0;
        deletedBucketInfluencerCount = 0;
        deletedInfluencerCount = 0;
        deletedModelSnapshotCount = 0;
        deletedModelStateCount = 0;
        this.quiet = quiet;
    }

    public ElasticsearchBulkDeleter(Client client, String jobId)
    {
        this(client, new ElasticsearchJobId(jobId), false);
    }

    @Override
    public void deleteBucket(Bucket bucket)
    {
        deleteRecords(bucket);
        deleteBucketInfluencers(bucket);
        bulkRequestBuilder.add(
                client.prepareDelete(jobId.getIndex(), Bucket.TYPE, bucket.getId()));
        ++deletedBucketCount;
    }

    @Override
    public void deleteRecords(Bucket bucket)
    {
        // Find the records using the time stamp rather than a parent-child
        // relationship.  The parent-child filter involves two queries behind
        // the scenes, and Elasticsearch documentation claims it's significantly
        // slower.  Here we rely on the record timestamps being identical to the
        // bucket timestamp.
        deleteTypeByBucket(bucket, AnomalyRecord.TYPE.getPreferredName(), () -> ++deletedRecordCount);
    }

    private void deleteTypeByBucket(Bucket bucket, String type, LongSupplier deleteCounter)
    {
        QueryBuilder query = QueryBuilders.termQuery(ElasticsearchMappings.ES_TIMESTAMP,
                bucket.getTimestamp().getTime());

        int done = 0;
        boolean finished = false;
        while (finished == false)
        {
            SearchResponse searchResponse = SearchAction.INSTANCE.newRequestBuilder(client)
                    .setIndices(jobId.getIndex())
                    .setTypes(type)
                    .setQuery(query)
                    .addSort(SortBuilders.fieldSort(ElasticsearchMappings.ES_DOC))
                    .setSize(SCROLL_SIZE)
                    .setFrom(done)
                    .execute().actionGet();

            for (SearchHit hit : searchResponse.getHits())
            {
                ++done;
                addDeleteRequest(hit);
                deleteCounter.getAsLong();
            }
            if (searchResponse.getHits().getTotalHits() == done)
            {
                finished = true;
            }
        }
    }

    private void addDeleteRequest(SearchHit hit)
    {
        DeleteRequestBuilder deleteRequest = DeleteAction.INSTANCE.newRequestBuilder(client)
                .setIndex(jobId.getIndex())
                .setType(hit.getType())
                .setId(hit.getId());
        SearchHitField parentField = hit.field(ElasticsearchMappings.PARENT);
        if (parentField != null)
        {
            deleteRequest.setParent(parentField.getValue().toString());
        }
        bulkRequestBuilder.add(deleteRequest);
    }

    public void deleteBucketInfluencers(Bucket bucket)
    {
        // Find the bucket influencers using the time stamp, relying on the
        // bucket influencer timestamps being identical to the bucket timestamp.
        deleteTypeByBucket(bucket, BucketInfluencer.TYPE, () -> ++deletedBucketInfluencerCount);
    }

    public void deleteInfluencers(Bucket bucket)
    {
        // Find the influencers using the time stamp, relying on the influencer
        // timestamps being identical to the bucket timestamp.
        deleteTypeByBucket(bucket, Influencer.TYPE, () -> ++deletedInfluencerCount);
    }

    public void deleteBucketByTime(Bucket bucket)
    {
        deleteTypeByBucket(bucket, Bucket.TYPE, () -> ++deletedBucketCount);
    }

    @Override
    public void deleteInfluencer(Influencer influencer)
    {
        String id = influencer.getId();
        if (id == null)
        {
            LOGGER.error("Cannot delete specific influencer without an ID",
                    // This means we get a stack trace to show where the request came from
                    new NullPointerException());
            return;
        }
        bulkRequestBuilder.add(
                client.prepareDelete(jobId.getIndex(), Influencer.TYPE, id));
        ++deletedInfluencerCount;
    }

    @Override
    public void deleteModelSnapshot(ModelSnapshot modelSnapshot)
    {
        String snapshotId = modelSnapshot.getSnapshotId();
        int docCount = modelSnapshot.getSnapshotDocCount();

        // Deduce the document IDs of the state documents from the information
        // in the snapshot document - we cannot query the state itself as it's
        // too big and has no mappings
        for (int i = 0; i < docCount; ++i)
        {
            String stateId = snapshotId + '_' + i;
            bulkRequestBuilder.add(
                    client.prepareDelete(jobId.getIndex(), ModelState.TYPE, stateId));
            ++deletedModelStateCount;
        }

        bulkRequestBuilder.add(
                client.prepareDelete(jobId.getIndex(), ModelSnapshot.TYPE, snapshotId));
        ++deletedModelSnapshotCount;
    }

    @Override
    public void deleteModelDebugOutput(ModelDebugOutput modelDebugOutput)
    {
        String id = modelDebugOutput.getId();
        bulkRequestBuilder.add(
                client.prepareDelete(jobId.getIndex(), ModelDebugOutput.TYPE, id));
    }

    @Override
    public void deleteModelSizeStats(ModelSizeStats modelSizeStats)
    {
        String id = modelSizeStats.getModelSizeStatsId();
        bulkRequestBuilder.add(
                client.prepareDelete(jobId.getIndex(), ModelSizeStats.TYPE, id));
    }

    public void deleteInterimResults()
    {
        QueryBuilder qb = QueryBuilders.termQuery(Bucket.IS_INTERIM, true);

        SearchResponse searchResponse = client.prepareSearch(jobId.getIndex())
                .setTypes(Bucket.TYPE, AnomalyRecord.TYPE.getPreferredName(), Influencer.TYPE, BucketInfluencer.TYPE)
                .setQuery(qb)
                .addSort(SortBuilders.fieldSort(ElasticsearchMappings.ES_DOC))
                .setScroll(SCROLL_CONTEXT_DURATION)
                .setSize(SCROLL_SIZE)
                .get();

        String scrollId = searchResponse.getScrollId();
        long totalHits = searchResponse.getHits().totalHits();
        long totalDeletedCount = 0;
        while (totalDeletedCount < totalHits)
        {
            for (SearchHit hit : searchResponse.getHits())
            {
                LOGGER.trace("Search hit for bucket: " + hit.toString() + ", " + hit.getId());
                String type = hit.getType();
                if (type.equals(Bucket.TYPE))
                {
                    ++deletedBucketCount;
                }
                else if (type.equals(AnomalyRecord.TYPE))
                {
                    ++deletedRecordCount;
                }
                else if (type.equals(BucketInfluencer.TYPE))
                {
                    ++deletedBucketInfluencerCount;
                }
                else if (type.equals(Influencer.TYPE))
                {
                    ++deletedInfluencerCount;
                }
                ++totalDeletedCount;
                addDeleteRequest(hit);
            }

            searchResponse = client.prepareSearchScroll(scrollId).setScroll(SCROLL_CONTEXT_DURATION).get();
        }
    }

    @Override
    public void commitAndFreeDiskSpace()
    {
        commit(true);
    }

    @Override
    public void commit()
    {
        commit(false);
    }

    /**
     * Commits the deletions and if {@code forceMerge} is {@code true}, it
     * forces a merge which removes the data from disk.
     */
    private void commit(boolean forceMerge)
    {
        if (bulkRequestBuilder.numberOfActions() == 0)
        {
            return;
        }

        if (!quiet)
        {
            LOGGER.debug("Requesting deletion of "
                    + deletedBucketCount + " buckets, "
                    + deletedRecordCount + " records, "
                    + deletedBucketInfluencerCount + " bucket influencers, "
                    + deletedInfluencerCount + " influencers, "
                    + deletedModelSnapshotCount + " model snapshots, "
                    + " and "
                    + deletedModelStateCount + " model state documents");
        }

        try
        {
            executeBulkRequest();
            if (forceMerge)
            {
                forceMerge();
            }
        }
        catch (RuntimeException e)
        {
            LOGGER.error("Failed to perform bulk delete", e);
        }
    }

    private void executeBulkRequest()
    {
        BulkResponse bulkResponse = bulkRequestBuilder.execute().actionGet();
        if (bulkResponse.hasFailures())
        {
            LOGGER.error(bulkResponse.buildFailureMessage());
        }
    }

    private void forceMerge()
    {
        // We need to do a force-merge request to ACTUALLY delete the results from disk
        ForceMergeResponse forceMergeResponse = ForceMergeAction.INSTANCE.newRequestBuilder(client)
                .setIndices(jobId.getIndex())
                .setOnlyExpungeDeletes(true)
                .get();
        for (ShardOperationFailedException shardFailure: forceMergeResponse.getShardFailures())
        {
            LOGGER.error("Shard failed during force-merge: " + shardFailure.reason());
        }
    }
}
