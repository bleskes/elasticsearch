package org.elasticsearch.xpack.prelert.job.persistence;

import org.apache.logging.log4j.Logger;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.ModelSizeStats;
import org.elasticsearch.xpack.prelert.job.ModelSnapshot;
import org.elasticsearch.xpack.prelert.job.exceptions.JobException;
import org.elasticsearch.xpack.prelert.job.exceptions.UnknownJobException;
import org.elasticsearch.xpack.prelert.job.persistence.serialisation.StorageSerialisable;
import org.elasticsearch.xpack.prelert.job.quantiles.Quantiles;
import org.elasticsearch.xpack.prelert.job.results.*;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

/**
 * Saves result Buckets and Quantiles to Elasticsearch<br>
 *
 * <b>Buckets</b> are written with the following structure:
 * <h2>Bucket</h2>
 * The results of each job are stored in buckets, this is the top level
 * structure for the results. A bucket contains multiple anomaly records.
 * The anomaly score of the bucket may not match the summed score of all
 * the records as all the records may not have been outputted for the bucket.
 * <h2>Anomaly Record</h2>
 * In Elasticsearch records have a parent <-> child relationship with
 * buckets and should only exist is relation to a parent bucket. Each record
 * was generated by a detector which can be identified via the detectorIndex field.
 * <h2>Detector</h2>
 * The Job has a fixed number of detectors but there may not be output
 * for every detector in each bucket.
 * <br>
 * <b>Quantiles</b> may contain model quantiles used in normalisation
 * and are stored in documents of type {@link Quantiles#TYPE}
 * <br>
 * <h2>ModelSizeStats</h2>
 * This is stored in a flat structure
 * <br>
 * @see org.elasticsearch.xpack.prelert.job.persistence.ElasticsearchMappings
 */
public class ElasticsearchPersister implements JobResultsPersister, JobRenormaliser
{

    private static final Logger LOGGER = Loggers.getLogger(ElasticsearchPersister.class);

    /**
     * We add the jobId in top level results to facilitate display in kibana
     */
    static final String JOB_ID_NAME = "jobId";

    private final Client client;
    private final ElasticsearchJobId jobId;

    /**
     * Create with the Elasticsearch client. Data will be written to
     * the index <code>jobId</code>
     *
     * @param jobId The job Id/Elasticsearch index
     * @param client The Elasticsearch client
     */
    public ElasticsearchPersister(String jobId, Client client)
    {
        this.jobId = new ElasticsearchJobId(jobId);
        this.client = client;
    }

    @Override
    public void persistBucket(Bucket bucket)
    {
        if (bucket.getRecords() == null)
        {
            return;
        }

        try
        {
            XContentBuilder content = serialiseWithJobId(bucket);

            LOGGER.trace("ES API CALL: index type " + Bucket.TYPE +
                    " to index " + jobId.getIndex() + " at epoch " + bucket.getEpoch());
            IndexResponse response = client.prepareIndex(jobId.getIndex(), Bucket.TYPE.getPreferredName())
                    .setSource(content)
                    .execute().actionGet();

            bucket.setId(response.getId());

            persistBucketInfluencersStandalone(bucket.getId(), bucket.getBucketInfluencers(),
                    bucket.getTimestamp(), bucket.isInterim());

            if (bucket.getInfluencers() != null && bucket.getInfluencers().isEmpty() == false)
            {
                BulkRequestBuilder addInfluencersRequest = client.prepareBulk();

                for (Influencer influencer : bucket.getInfluencers())
                {
                    influencer.setTimestamp(bucket.getTimestamp());
                    influencer.setInterim(bucket.isInterim());
                    content = serialiseWithJobId(influencer);
                    LOGGER.trace("ES BULK ACTION: index type " + Influencer.TYPE +
                            " to index " + jobId.getIndex() + " with auto-generated ID");
                    addInfluencersRequest.add(
                            client.prepareIndex(jobId.getIndex(), Influencer.TYPE.getPreferredName())
                            .setSource(content));
                }

                LOGGER.trace("ES API CALL: bulk request with " + addInfluencersRequest.numberOfActions() + " actions");
                BulkResponse addInfluencersResponse = addInfluencersRequest.execute().actionGet();
                if (addInfluencersResponse.hasFailures())
                {
                    LOGGER.error("Bulk index of Influencers has errors: "
                            + addInfluencersResponse.buildFailureMessage());
                }
            }

            if (bucket.getRecords().isEmpty() == false)
            {
                BulkRequestBuilder addRecordsRequest = client.prepareBulk();
                for (AnomalyRecord record : bucket.getRecords())
                {
                    record.setTimestamp(bucket.getTimestamp());
                    content = serialiseWithJobId(record);

                    LOGGER.trace("ES BULK ACTION: index type " + AnomalyRecord.TYPE +
                            " to index " + jobId.getIndex() + " with auto-generated ID, for bucket "
                            + bucket.getId());
                    addRecordsRequest.add(client.prepareIndex(jobId.getIndex(), AnomalyRecord.TYPE.getPreferredName())
                            .setSource(content)
                            .setParent(bucket.getId()));
                }

                LOGGER.trace("ES API CALL: bulk request with " + addRecordsRequest.numberOfActions() + " actions");
                BulkResponse addRecordsResponse = addRecordsRequest.execute().actionGet();
                if (addRecordsResponse.hasFailures())
                {
                    LOGGER.error("Bulk index of AnomalyRecord has errors: "
                            + addRecordsResponse.buildFailureMessage());
                }
            }

            persistPerPartitionMaxProbabilities(bucket);
        }
        catch (IOException e)
        {
            LOGGER.error("Error writing bucket state", e);
        }
    }

    private void persistPerPartitionMaxProbabilities(Bucket bucket)
    {
        if (bucket.getPerPartitionMaxProbability().isEmpty())
        {
            return;
        }

        try
        {
            XContentBuilder builder = jsonBuilder();
            ElasticsearchStorageSerialiser serialiser =
                    new ElasticsearchStorageSerialiser(builder);

            serialiser.startObject()
            .addTimestamp(bucket.getTimestamp())
            .add(JOB_ID_NAME, jobId.getId());
            serialiser.startList(PartitionNormalisedProb.PARTITION_NORMALIZED_PROBS);
            for (Entry<String, Double> entry : bucket.getPerPartitionMaxProbability().entrySet())
            {
                serialiser.startObject()
                .add(AnomalyRecord.PARTITION_FIELD_VALUE.getPreferredName(), entry.getKey())
                .add(Bucket.MAX_NORMALIZED_PROBABILITY.getPreferredName(), entry.getValue())
                .endObject();
            }
            serialiser.endList().endObject();

            LOGGER.trace("ES API CALL: index type " + PartitionNormalisedProb.TYPE +
                    " to index " + jobId.getIndex() + " at epoch " + bucket.getEpoch());
            client.prepareIndex(jobId.getIndex(), PartitionNormalisedProb.TYPE)
            .setSource(builder)
            .setId(bucket.getId())
            .execute().actionGet();
        }
        catch (IOException e)
        {
            LOGGER.error("Error updating bucket per partition max normalized scores", e);
            return;
        }
    }

    @Override
    public void persistCategoryDefinition(CategoryDefinition category)
    {
        Persistable persistable = new Persistable(category, () -> CategoryDefinition.TYPE.getPreferredName(),
                () -> String.valueOf(category.getCategoryId()),
                () -> serialiseCategoryDefinition(category));
        persistable.persist();

        // Don't commit as we expect masses of these updates and they're not
        // read again by this process
    }

    /**
     * The quantiles objects are written with a fixed ID, so that the
     * latest quantiles will overwrite the previous ones.  For each ES index,
     * which corresponds to a job, there can only be one quantiles document.
     * @param quantiles If <code>null</code> then returns straight away.
     */
    @Override
    public void persistQuantiles(Quantiles quantiles)
    {
        Persistable persistable = new Persistable(quantiles, () -> Quantiles.TYPE,
                () -> Quantiles.QUANTILES_ID, () -> serialiseWithJobId(quantiles));
        if (persistable.persist())
        {
            // Refresh the index when persisting quantiles so that previously
            // persisted results will be available for searching.  Do this using the
            // indices API rather than the index API (used to write the quantiles
            // above), because this will refresh all shards rather than just the
            // shard that the quantiles document itself was written to.
            commitWrites();
        }
    }

    /**
     * Write a model snapshot description to Elasticsearch.  Note that this is
     * only the description - the actual model state is persisted separately.
     * @param modelSnapshot If <code>null</code> then returns straight away.
     */
    @Override
    public void persistModelSnapshot(ModelSnapshot modelSnapshot)
    {
        Persistable persistable = new Persistable(modelSnapshot, () -> ModelSnapshot.TYPE,
                () -> modelSnapshot.getSnapshotId(), () -> serialiseWithJobId(modelSnapshot));
        persistable.persist();
    }

    /**
     * Persist the memory usage data
     * @param modelSizeStats If <code>null</code> then returns straight away.
     */
    @Override
    public void persistModelSizeStats(ModelSizeStats modelSizeStats)
    {
        LOGGER.trace("Persisting model size stats, for size " + modelSizeStats.getModelBytes());
        Persistable persistable = new Persistable(modelSizeStats, () -> ModelSizeStats.TYPE,
                () -> modelSizeStats.getModelSizeStatsId(),
                () -> serialiseWithJobId(modelSizeStats));
        persistable.persist();

        modelSizeStats.setModelSizeStatsId(null);

        persistable = new Persistable(modelSizeStats, () -> ModelSizeStats.TYPE,
                () -> modelSizeStats.getModelSizeStatsId(),
                () -> serialiseWithJobId(modelSizeStats));
        persistable.persist();

        // Don't commit as we expect masses of these updates and they're only
        // for information at the API level
    }

    /**
     * Persist model debug output
     * @param modelDebugOutput If <code>null</code> then returns straight away.
     */
    @Override
    public void persistModelDebugOutput(ModelDebugOutput modelDebugOutput)
    {
        Persistable persistable = new Persistable(modelDebugOutput, () -> ModelDebugOutput.TYPE,
                () -> null, () -> serialiseWithJobId(modelDebugOutput));
        persistable.persist();

        // Don't commit as we expect masses of these updates and they're not
        // read again by this process
    }

    @Override
    public void persistInfluencer(Influencer influencer)
    {
        Persistable persistable = new Persistable(influencer, () -> Influencer.TYPE.getPreferredName(),
                () -> influencer.getId(), () -> serialiseWithJobId(influencer));
        persistable.persist();

        // Don't commit as we expect masses of these updates and they're not
        // read again by this process
    }

    @Override
    public void incrementBucketCount(long count) throws JobException, UnknownJobException {
        LOGGER.trace("ES API CALL: update ID " + jobId.getId() + " type " + JobDetails.TYPE +
                " in index " + jobId.getIndex() +
                " by running Groovy script update-bucket-count with params count=" + count);

        ElasticsearchScripts.updateViaScript(client,
                jobId.getIndex(),
                JobDetails.TYPE,  jobId.getId(),
                ElasticsearchScripts.newUpdateBucketCount(count));
    }

    @Override
    public void updateAverageBucketProcessingTime(long timeMs) throws JobException, UnknownJobException
    {
        LOGGER.trace("ES API CALL: update ID " + jobId.getId() + " type " + ReservedFieldNames.BUCKET_PROCESSING_TIME_TYPE +
                " in index " + jobId.getIndex() + " update last bucket processing time");


        Map<String, Object> upsertMap = new HashMap<>();
        upsertMap.put(ReservedFieldNames.AVERAGE_PROCESSING_TIME_MS, new Long(timeMs));

        try
        {
            ElasticsearchScripts.upsertViaScript(client,
                    jobId.getIndex(),
                    ReservedFieldNames.BUCKET_PROCESSING_TIME_TYPE, ReservedFieldNames.AVERAGE_PROCESSING_TIME_MS,
                    ElasticsearchScripts.updateProcessingTime(timeMs),
                    upsertMap);
        }
        catch (VersionConflictEngineException e)
        {
            LOGGER.error("Failed to update the bucket processing time document " +
                    ReservedFieldNames.AVERAGE_PROCESSING_TIME_MS + " in index " + jobId.getIndex(), e);
        }
    }

    /**
     * Refreshes the Elasticsearch index.
     * Blocks until results are searchable.
     * @return
     */
    @Override
    public boolean commitWrites()
    {
        String indexName = jobId.getIndex();
        // Flush should empty the translog into Lucene
        LOGGER.trace("ES API CALL: flush index " + indexName);
        client.admin().indices().flush(new FlushRequest(indexName)).actionGet();
        // Refresh should wait for Lucene to make the data searchable
        LOGGER.trace("ES API CALL: refresh index " + indexName);
        client.admin().indices().refresh(new RefreshRequest(indexName)).actionGet();
        return true;
    }

    @Override
    public void updateBucket(Bucket bucket)
    {
        try
        {
            LOGGER.trace("ES API CALL: index type " + Bucket.TYPE +
                    " to index " + jobId.getIndex() + " with ID " + bucket.getId());
            client.prepareIndex(jobId.getIndex(), Bucket.TYPE.getPreferredName(), bucket.getId())
            .setSource(serialiseWithJobId(bucket)).execute().actionGet();
        }
        catch (IOException e)
        {
            LOGGER.error("Error updating bucket state", e);
            return;
        }

        // If the update to the bucket was successful, also update the
        // standalone copies of the nested bucket influencers
        try
        {
            persistBucketInfluencersStandalone(bucket.getId(), bucket.getBucketInfluencers(),
                    bucket.getTimestamp(), bucket.isInterim());
        }
        catch (IOException e)
        {
            LOGGER.error("Error updating standalone bucket influencer state", e);
            return;
        }

        persistPerPartitionMaxProbabilities(bucket);
    }

    private void persistBucketInfluencersStandalone(String bucketId, List<BucketInfluencer> bucketInfluencers,
            Date bucketTime, boolean isInterim) throws IOException
    {
        if (bucketInfluencers != null && bucketInfluencers.isEmpty() == false)
        {
            BulkRequestBuilder addBucketInfluencersRequest = client.prepareBulk();

            for (BucketInfluencer bucketInfluencer : bucketInfluencers)
            {
                XContentBuilder content = serialiseBucketInfluencerStandalone(bucketInfluencer,
                        bucketTime, isInterim);
                // Need consistent IDs to ensure overwriting on renormalisation
                String id = bucketId + bucketInfluencer.getInfluencerFieldName();
                LOGGER.trace("ES BULK ACTION: index type " + BucketInfluencer.TYPE +
                        " to index " + jobId.getIndex() + " with ID " + id);
                addBucketInfluencersRequest.add(
                        client.prepareIndex(jobId.getIndex(), BucketInfluencer.TYPE.getPreferredName(), id)
                        .setSource(content));
            }

            LOGGER.trace("ES API CALL: bulk request with " + addBucketInfluencersRequest.numberOfActions() + " actions");
            BulkResponse addBucketInfluencersResponse = addBucketInfluencersRequest.execute().actionGet();
            if (addBucketInfluencersResponse.hasFailures())
            {
                LOGGER.error("Bulk index of Bucket Influencers has errors: "
                        + addBucketInfluencersResponse.buildFailureMessage());
            }
        }
    }

    @Override
    public void updateRecords(String bucketId, List<AnomalyRecord> records)
    {
        try
        {
            // Now bulk update the records within the bucket
            BulkRequestBuilder bulkRequest = client.prepareBulk();
            boolean addedAny = false;
            for (AnomalyRecord record : records)
            {
                String recordId = record.getId();

                LOGGER.trace("ES BULK ACTION: update ID " + recordId + " type " + AnomalyRecord.TYPE +
                        " in index " + jobId.getIndex() + " using map of new values, for bucket " +
                        bucketId);

                bulkRequest.add(
                        client.prepareIndex(jobId.getIndex(), AnomalyRecord.TYPE.getPreferredName(), recordId)
                        .setSource(serialiseWithJobId(record))
                        // Need to specify the parent ID when updating a child
                        .setParent(bucketId));

                addedAny = true;
            }

            if (addedAny)
            {
                LOGGER.trace("ES API CALL: bulk request with " +
                        bulkRequest.numberOfActions() + " actions");
                BulkResponse bulkResponse = bulkRequest.execute().actionGet();
                if (bulkResponse.hasFailures())
                {
                    LOGGER.error("BulkResponse has errors: " + bulkResponse.buildFailureMessage());
                }
            }
        }
        catch (IOException | ElasticsearchException e)
        {
            LOGGER.error("Error updating anomaly records", e);
        }
    }

    @Override
    public void updateInfluencer(Influencer influencer)
    {
        persistInfluencer(influencer);
    }

    @Override
    public void deleteInterimResults()
    {
        ElasticsearchBulkDeleter deleter = new ElasticsearchBulkDeleter(client, jobId, true);
        deleter.deleteInterimResults();
        deleter.commit();
    }

    private interface Serialiser
    {
        XContentBuilder serialise() throws IOException;
    }

    private class Persistable
    {
        private final Object object;
        private final Supplier<String> typeSupplier;
        private final Supplier<String> idSupplier;
        private final Serialiser serialiser;

        Persistable(Object object, Supplier<String> typeSupplier, Supplier<String> idSupplier,
                Serialiser serialiser)
        {
            this.object = object;
            this.typeSupplier = typeSupplier;
            this.idSupplier = idSupplier;
            this.serialiser = serialiser;
        }

        boolean persist()
        {
            String type = typeSupplier.get();
            String id = idSupplier.get();

            if (object == null)
            {
                LOGGER.warn("No " + type + " to persist for job " + jobId.getId());
                return false;
            }

            logCall(type, id);

            try
            {
                client.prepareIndex(jobId.getIndex(), type, idSupplier.get())
                .setSource(serialiser.serialise())
                .execute().actionGet();
                return true;
            }
            catch (IOException e)
            {
                LOGGER.error("Error writing " + typeSupplier.get(), e);
                return false;
            }
        }

        private void logCall(String type, String id)
        {
            String msg = "ES API CALL: index type " + type + " to index " + jobId.getIndex();
            if (id != null)
            {
                msg += " with ID " + idSupplier.get();
            }
            else
            {
                msg += " with auto-generated ID";
            }
            LOGGER.trace(msg);
        }
    }

    private XContentBuilder serialiseWithJobId(StorageSerialisable obj) throws IOException
    {
        XContentBuilder builder = jsonBuilder();
        new ElasticsearchStorageSerialiser(builder)
        .startObject()
        .add(JOB_ID_NAME, jobId.getId())
        .serialise(obj)
        .endObject();
        return builder;
    }

    private XContentBuilder serialiseCategoryDefinition(CategoryDefinition categoryDefinition)
            throws IOException
    {
        XContentBuilder builder = jsonBuilder();
        new ElasticsearchStorageSerialiser(builder)
        .startObject()
        .add(JOB_ID_NAME, jobId.getId())
        .addTimestamp(new Date())
        .serialise(categoryDefinition)
        .endObject();
        return builder;
    }

    private XContentBuilder serialiseBucketInfluencerStandalone(BucketInfluencer bucketInfluencer,
            Date bucketTime, boolean isInterim) throws IOException
    {
        XContentBuilder builder = jsonBuilder();
        new ElasticsearchStorageSerialiser(builder)
        .startObject()
        .add(JOB_ID_NAME, jobId.getId())
        .addTimestamp(bucketTime)
        .serialise(bucketInfluencer);

        if (isInterim)
        {
            builder.field(Bucket.IS_INTERIM.getPreferredName(), true);
        }

        return builder.endObject();
    }
}
