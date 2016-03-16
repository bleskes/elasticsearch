/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/

package com.prelert.job.persistence.elasticsearch;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

import org.apache.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;

import com.prelert.job.JobDetails;
import com.prelert.job.ModelSizeStats;
import com.prelert.job.ModelSnapshot;
import com.prelert.job.persistence.JobRenormaliser;
import com.prelert.job.persistence.JobResultsPersister;
import com.prelert.job.quantiles.Quantiles;
import com.prelert.job.results.AnomalyCause;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.BucketInfluencer;
import com.prelert.job.results.CategoryDefinition;
import com.prelert.job.results.Influence;
import com.prelert.job.results.Influencer;
import com.prelert.job.results.ModelDebugOutput;
import com.prelert.job.results.ReservedFieldNames;

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
 * and are stored in documents of type {@link Quantiles.TYPE}
 * <br>
 * <h2>ModelSizeStats</h2>
 * This is stored in a flat structure
 * <br>
 * @see com.prelert.job.persistence.elasticsearch.ElasticsearchMappings
 */
public class ElasticsearchPersister implements JobResultsPersister, JobRenormaliser
{

    private static final Logger LOGGER = Logger.getLogger(ElasticsearchPersister.class);

    /**
     * We add the jobId in top level results to facilitate display in kibana
     */
    static final String JOB_ID_NAME = "jobId";

    private final Client m_Client;
    private final ElasticsearchJobId m_JobId;

    /**
     * Create with the Elasticsearch client. Data will be written to
     * the index <code>jobId</code>
     *
     * @param jobId The job Id/Elasticsearch index
     * @param client The Elasticsearch client
     */
    public ElasticsearchPersister(String jobId, Client client)
    {
        m_JobId = new ElasticsearchJobId(jobId);
        m_Client = client;
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
            XContentBuilder content = serialiseBucket(bucket);

            LOGGER.trace("ES API CALL: index type " + Bucket.TYPE +
                    " to index " + m_JobId.getIndex() + " at epoch " + bucket.getEpoch());
            IndexResponse response = m_Client.prepareIndex(m_JobId.getIndex(), Bucket.TYPE)
                    .setSource(content)
                    .execute().actionGet();
            bucket.setId(response.getId());

            persistBucketInfluencersStandalone(bucket.getBucketInfluencers(),
                    bucket.getTimestamp(), bucket.isInterim());

            if (bucket.getInfluencers() != null && bucket.getInfluencers().isEmpty() == false)
            {
                BulkRequestBuilder addInfluencersRequest = m_Client.prepareBulk();

                for (Influencer influencer : bucket.getInfluencers())
                {
                    influencer.setTimestamp(bucket.getTimestamp());
                    content = serialiseInfluencer(influencer, bucket.isInterim());
                    LOGGER.trace("ES BULK ACTION: index type " + Influencer.TYPE +
                            " to index " + m_JobId.getIndex() + " ID = " + influencer.getId());
                    addInfluencersRequest.add(
                            m_Client.prepareIndex(m_JobId.getIndex(), Influencer.TYPE, influencer.getId())
                            .setSource(content));
                }

                LOGGER.trace("ES API CALL: bulk request with " + addInfluencersRequest.numberOfActions() + " actions");
                BulkResponse addInfluencersResponse = addInfluencersRequest.execute().actionGet();
                if (addInfluencersResponse.hasFailures())
                {
                    LOGGER.error("Bulk index of Influencers has errors");
                    for (BulkItemResponse item : addInfluencersResponse.getItems())
                    {
                        LOGGER.error(item.getFailureMessage());
                    }
                }
            }

            if (bucket.getRecords().isEmpty() == false)
            {
                BulkRequestBuilder addRecordsRequest = m_Client.prepareBulk();
                for (AnomalyRecord record : bucket.getRecords())
                {
                    content = serialiseRecord(record, bucket.getTimestamp());

                    LOGGER.trace("ES BULK ACTION: index type " + AnomalyRecord.TYPE +
                            " to index " + m_JobId.getIndex() + " with auto-generated ID, for bucket "
                            + bucket.getId());
                    addRecordsRequest.add(m_Client.prepareIndex(m_JobId.getIndex(), AnomalyRecord.TYPE)
                            .setSource(content)
                            .setParent(bucket.getId()));
                }

                LOGGER.trace("ES API CALL: bulk request with " + addRecordsRequest.numberOfActions() + " actions");
                BulkResponse addRecordsResponse = addRecordsRequest.execute().actionGet();
                if (addRecordsResponse.hasFailures())
                {
                    LOGGER.error("Bulk index of AnomalyRecord has errors");
                    for (BulkItemResponse item : addRecordsResponse.getItems())
                    {
                        LOGGER.error(item.getFailureMessage());
                    }
                }
            }
        }
        catch (IOException e)
        {
            LOGGER.error("Error writing bucket state", e);
        }
    }

    @Override
    public void persistCategoryDefinition(CategoryDefinition category)
    {
        Persistable persistable = new Persistable(category, () -> CategoryDefinition.TYPE,
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
                () -> quantiles.getId(), () -> serialiseQuantiles(quantiles));
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
                () -> modelSnapshot.getSnapshotId(), () -> serialiseModelSnapshot(modelSnapshot));
        persistable.persist();
    }

    /**
     * Persist the memory usage data
     * @param modelSizeStats If <code>null</code> then returns straight away.
     */
    @Override
    public void persistModelSizeStats(ModelSizeStats modelSizeStats)
    {
        Persistable persistable = new Persistable(modelSizeStats, () -> ModelSizeStats.TYPE,
                () -> modelSizeStats.getId(), () -> serialiseModelSizeStats(modelSizeStats));
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
                () -> null, () -> serialiseModelDebugOutput(modelDebugOutput));
        persistable.persist();

        // Don't commit as we expect masses of these updates and they're not
        // read again by this process
    }

    @Override
    public void persistInfluencer(Influencer influencer)
    {
        Persistable persistable = new Persistable(influencer, () -> Influencer.TYPE,
                () -> influencer.getId(), () -> serialiseInfluencer(influencer, false));
        persistable.persist();

        // Don't commit as we expect masses of these updates and they're not
        // read again by this process
    }

    @Override
    public void incrementBucketCount(long count)
    {
        LOGGER.trace("ES API CALL: update ID " + m_JobId.getId() + " type " + JobDetails.TYPE +
                " in index " + m_JobId.getIndex() +
                " by running Groovy script update-bucket-count with params count=" + count);

        m_Client.prepareUpdate(m_JobId.getIndex(), JobDetails.TYPE, m_JobId.getId())
                        .setScript(ElasticsearchScripts.newUpdateBucketCount(count))
                        .setRetryOnConflict(3).get();
    }

    /**
     * Refreshes the Elasticsearch index.
     * Blocks until results are searchable.
     * @return
     */
    @Override
    public boolean commitWrites()
    {
        String indexName = m_JobId.getIndex();
        // Flush should empty the translog into Lucene
        LOGGER.trace("ES API CALL: flush index " + indexName);
        m_Client.admin().indices().flush(new FlushRequest(indexName)).actionGet();
        // Refresh should wait for Lucene to make the data searchable
        LOGGER.trace("ES API CALL: refresh index " + indexName);
        m_Client.admin().indices().refresh(new RefreshRequest(indexName)).actionGet();
        return true;
    }

    @Override
    public void updateBucket(Bucket bucket)
    {
        try
        {
            LOGGER.trace("ES API CALL: index type " + Bucket.TYPE +
                    " to index " + m_JobId.getIndex() + " with ID " + bucket.getId());
            m_Client.prepareIndex(m_JobId.getIndex(), Bucket.TYPE, bucket.getId())
                    .setSource(serialiseBucket(bucket)).execute().actionGet();
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
            persistBucketInfluencersStandalone(bucket.getBucketInfluencers(),
                    bucket.getTimestamp(), bucket.isInterim());
        }
        catch (IOException e)
        {
            LOGGER.error("Error updating standalone bucket influencer state", e);
            return;
        }
    }

    private void persistBucketInfluencersStandalone(List<BucketInfluencer> bucketInfluencers,
            Date bucketTime, boolean isInterim) throws IOException
    {
        if (bucketInfluencers != null && bucketInfluencers.isEmpty() == false)
        {
            BulkRequestBuilder addBucketInfluencersRequest = m_Client.prepareBulk();

            for (BucketInfluencer bucketInfluencer : bucketInfluencers)
            {
                XContentBuilder content = serialiseBucketInfluencerStandalone(bucketInfluencer,
                        bucketTime, isInterim);
                String id = bucketInfluencer.getId(bucketTime);
                LOGGER.trace("ES BULK ACTION: index type " + BucketInfluencer.TYPE +
                        " to index " + m_JobId.getIndex() + " ID = " + id);
                addBucketInfluencersRequest.add(
                        m_Client.prepareIndex(m_JobId.getIndex(), BucketInfluencer.TYPE, id)
                        .setSource(content));
            }

            LOGGER.trace("ES API CALL: bulk request with " + addBucketInfluencersRequest.numberOfActions() + " actions");
            BulkResponse addBucketInfluencersResponse = addBucketInfluencersRequest.execute().actionGet();
            if (addBucketInfluencersResponse.hasFailures())
            {
                LOGGER.error("Bulk index of Bucket Influencers has errors");
                for (BulkItemResponse item : addBucketInfluencersResponse.getItems())
                {
                    LOGGER.error(item.getFailureMessage());
                }
            }
        }
    }

    @Override
    public void updateRecords(String bucketId, List<AnomalyRecord> records)
    {
        try
        {
            // Now bulk update the records within the bucket
            BulkRequestBuilder bulkRequest = m_Client.prepareBulk();
            boolean addedAny = false;
            for (AnomalyRecord record : records)
            {
                String recordId = record.getId();

                LOGGER.trace("ES BULK ACTION: update ID " + recordId + " type " + AnomalyRecord.TYPE +
                        " in index " + m_JobId.getIndex() + " using map of new values, for bucket " +
                        bucketId);

                bulkRequest.add(
                        m_Client.prepareIndex(m_JobId.getIndex(), AnomalyRecord.TYPE, recordId)
                                .setSource(serialiseRecord(record, record.getTimestamp()))
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
                    LOGGER.error("BulkResponse has errors");
                    for (BulkItemResponse item : bulkResponse.getItems())
                    {
                        LOGGER.error(item.getFailureMessage());
                    }
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
        ElasticsearchBulkDeleter deleter = new ElasticsearchBulkDeleter(m_Client, m_JobId, true);
        deleter.deleteInterimResults();
        deleter.commit();
    }

    private interface Serialiser
    {
        XContentBuilder serialise() throws IOException;
    }

    private class Persistable
    {
        private final Object m_Object;
        private final Supplier<String> m_TypeSupplier;
        private final Supplier<String> m_IdSupplier;
        private final Serialiser m_Serialiser;

        Persistable(Object object, Supplier<String> typeSupplier, Supplier<String> idSupplier,
                Serialiser serialiser)
        {
            m_Object = object;
            m_TypeSupplier = typeSupplier;
            m_IdSupplier = idSupplier;
            m_Serialiser = serialiser;
        }

        boolean persist()
        {
            String type = m_TypeSupplier.get();
            String id = m_IdSupplier.get();

            if (m_Object == null)
            {
                LOGGER.warn("No " + type + " to persist for job " + m_JobId.getId());
                return false;
            }

            logCall(type, id);

            try
            {
                m_Client.prepareIndex(m_JobId.getIndex(), type, m_IdSupplier.get())
                        .setSource(m_Serialiser.serialise())
                        .execute().actionGet();
                return true;
            }
            catch (IOException e)
            {
                LOGGER.error("Error writing " + m_TypeSupplier.get(), e);
                return false;
            }
        }

        private void logCall(String type, String id)
        {
            String msg = "ES API CALL: index type " + type + " to index " + m_JobId.getIndex();
            if (id != null)
            {
                msg += " with ID " + m_IdSupplier.get();
            }
            LOGGER.trace(msg);
        }
    }

    /**
     * Return the bucket as serialisable content
     * @param bucket
     * @return
     * @throws IOException
     */
    private XContentBuilder serialiseBucket(Bucket bucket)
    throws IOException
    {
        XContentBuilder builder = jsonBuilder().startObject()
                .field(JOB_ID_NAME, m_JobId.getId())
                .field(ElasticsearchMappings.ES_TIMESTAMP, bucket.getTimestamp())
                .field(Bucket.ANOMALY_SCORE, bucket.getAnomalyScore())
                .field(Bucket.INITIAL_ANOMALY_SCORE, bucket.getInitialAnomalyScore())
                .field(Bucket.MAX_NORMALIZED_PROBABILITY, bucket.getMaxNormalizedProbability())
                .field(Bucket.RECORD_COUNT, bucket.getRecordCount())
                .field(Bucket.EVENT_COUNT, bucket.getEventCount());

        if (bucket.isInterim())
        {
            builder.field(Bucket.IS_INTERIM, bucket.isInterim());
        }

        if (bucket.getBucketInfluencers() != null)
        {
            builder.startArray(Bucket.BUCKET_INFLUENCERS);
            for (BucketInfluencer bucketInfluencer : bucket.getBucketInfluencers())
            {
                serialiseBucketInfluencerNested(bucketInfluencer, builder);
            }
            builder.endArray();
        }

        builder.endObject();

        return builder;
    }

    private XContentBuilder serialiseCategoryDefinition(CategoryDefinition category)
            throws IOException
    {
        List<String> examples = category.getExamples();
        return jsonBuilder().startObject()
                .field(JOB_ID_NAME, m_JobId.getId())
                .field(CategoryDefinition.CATEGORY_ID, category.getCategoryId())
                .field(CategoryDefinition.TERMS, category.getTerms())
                .field(CategoryDefinition.REGEX, category.getRegex())
                .array(CategoryDefinition.EXAMPLES, examples.toArray(new Object[examples.size()]))
                .endObject();
    }

    /**
     * Return the quantiles as serialisable content
     * @param quantiles
     * @return
     * @throws IOException
     */
    private XContentBuilder serialiseQuantiles(Quantiles quantiles)
    throws IOException
    {
        return jsonBuilder().startObject()
                .field(Quantiles.ID, quantiles.getId())
                .field(Quantiles.VERSION, quantiles.getVersion())
                .field(Quantiles.QUANTILE_STATE, quantiles.getState())
                .endObject();
    }

    /**
     * Return the model snapshot description as serialisable content
     * @param modelSnapshot
     * @return
     * @throws IOException
     */
    private XContentBuilder serialiseModelSnapshot(ModelSnapshot modelSnapshot)
    throws IOException
    {
        XContentBuilder builder = jsonBuilder().startObject()
                .field(JOB_ID_NAME, m_JobId.getId())
                .field(ElasticsearchMappings.ES_TIMESTAMP, modelSnapshot.getTimestamp())
                .field(ModelSnapshot.DESCRIPTION, modelSnapshot.getDescription())
                .field(ModelSnapshot.RESTORE_PRIORITY, modelSnapshot.getRestorePriority())
                .field(ModelSnapshot.SNAPSHOT_ID, modelSnapshot.getSnapshotId())
                .field(ModelSnapshot.SNAPSHOT_DOC_COUNT, modelSnapshot.getSnapshotDocCount());

        if (modelSnapshot.getModelSizeStats() != null)
        {
            builder.startObject(ModelSizeStats.TYPE);
            serialiseModelSizeStatsContent(modelSnapshot.getModelSizeStats(), builder);
            builder.endObject();
        }
        if (modelSnapshot.getLatestRecordTimeStamp() != null)
        {
            builder.field(ModelSnapshot.LATEST_RECORD_TIME, modelSnapshot.getLatestRecordTimeStamp());
        }

        return builder.endObject();
    }

    /**
     * Add the modelSizeStats serialisable content to an existing JSON builder
     * @param modelSizeStats
     * @param builder
     * @return
     * @throws IOException
     */
    private XContentBuilder serialiseModelSizeStatsContent(ModelSizeStats modelSizeStats, XContentBuilder builder)
    throws IOException
    {
        return builder
                .field(ModelSizeStats.MODEL_BYTES, modelSizeStats.getModelBytes())
                .field(ModelSizeStats.TOTAL_BY_FIELD_COUNT, modelSizeStats.getTotalByFieldCount())
                .field(ModelSizeStats.TOTAL_OVER_FIELD_COUNT, modelSizeStats.getTotalOverFieldCount())
                .field(ModelSizeStats.TOTAL_PARTITION_FIELD_COUNT, modelSizeStats.getTotalPartitionFieldCount())
                .field(ModelSizeStats.BUCKET_ALLOCATION_FAILURES_COUNT, modelSizeStats.getBucketAllocationFailuresCount())
                .field(ModelSizeStats.MEMORY_STATUS, modelSizeStats.getMemoryStatus());
    }

    /**
     * Return the modelSizeStats as serialisable content
     * @param modelSizeStats
     * @return
     * @throws IOException
     */
    private XContentBuilder serialiseModelSizeStats(ModelSizeStats modelSizeStats)
    throws IOException
    {
        XContentBuilder builder = jsonBuilder().startObject();
        serialiseModelSizeStatsContent(modelSizeStats, builder);
        return builder.endObject();
    }

    /**
     * Return the modelDebugOutput as serialisable content
     * @param modelDebugOutput
     * @return
     * @throws IOException
     */
    private XContentBuilder serialiseModelDebugOutput(ModelDebugOutput modelDebugOutput)
    throws IOException
    {
        XContentBuilder builder = jsonBuilder().startObject()
                .field(JOB_ID_NAME, m_JobId.getId())
                .field(ElasticsearchMappings.ES_TIMESTAMP, modelDebugOutput.getTimestamp())
                .field(ModelDebugOutput.DEBUG_FEATURE, modelDebugOutput.getDebugFeature())
                .field(ModelDebugOutput.DEBUG_LOWER, modelDebugOutput.getDebugLower())
                .field(ModelDebugOutput.DEBUG_UPPER, modelDebugOutput.getDebugUpper())
                .field(ModelDebugOutput.DEBUG_MEAN, modelDebugOutput.getDebugMean())
                .field(ModelDebugOutput.ACTUAL, modelDebugOutput.getActual());

        if (modelDebugOutput.getByFieldName() != null)
        {
            builder.field(ModelDebugOutput.BY_FIELD_NAME, modelDebugOutput.getByFieldName());
            if (modelDebugOutput.getByFieldValue() != null &&
                !ReservedFieldNames.RESERVED_FIELD_NAMES.contains(modelDebugOutput.getByFieldName()))
            {
                builder.field(modelDebugOutput.getByFieldName(), modelDebugOutput.getByFieldValue());
            }
        }
        if (modelDebugOutput.getByFieldValue() != null)
        {
            builder.field(ModelDebugOutput.BY_FIELD_VALUE, modelDebugOutput.getByFieldValue());
        }
        if (modelDebugOutput.getOverFieldName() != null)
        {
            builder.field(ModelDebugOutput.OVER_FIELD_NAME, modelDebugOutput.getOverFieldName());
            if (modelDebugOutput.getOverFieldValue() != null &&
                !ReservedFieldNames.RESERVED_FIELD_NAMES.contains(modelDebugOutput.getOverFieldName()))
            {
                builder.field(modelDebugOutput.getOverFieldName(), modelDebugOutput.getOverFieldValue());
            }
        }
        if (modelDebugOutput.getOverFieldValue() != null)
        {
            builder.field(ModelDebugOutput.OVER_FIELD_VALUE, modelDebugOutput.getOverFieldValue());
        }
        if (modelDebugOutput.getPartitionFieldName() != null)
        {
            builder.field(ModelDebugOutput.PARTITION_FIELD_NAME, modelDebugOutput.getPartitionFieldName());
            if (modelDebugOutput.getPartitionFieldValue() != null &&
                !ReservedFieldNames.RESERVED_FIELD_NAMES.contains(modelDebugOutput.getPartitionFieldName()))
            {
                builder.field(modelDebugOutput.getPartitionFieldName(), modelDebugOutput.getPartitionFieldValue());
            }
        }
        if (modelDebugOutput.getPartitionFieldValue() != null)
        {
            builder.field(ModelDebugOutput.PARTITION_FIELD_VALUE, modelDebugOutput.getPartitionFieldValue());
        }

        builder.endObject();

        return builder;
    }

    /**
     * Return the anomaly record as serialisable content
     *
     * @param record Record to serialise
     * @param bucketTime The timestamp of the anomaly record parent bucket
     * @return
     * @throws IOException
     */
    private XContentBuilder serialiseRecord(AnomalyRecord record, Date bucketTime)
    throws IOException
    {
        XContentBuilder builder = jsonBuilder().startObject()
                .field(JOB_ID_NAME, m_JobId.getId())
                .field(AnomalyRecord.DETECTOR_INDEX, record.getDetectorIndex())
                .field(AnomalyRecord.PROBABILITY, record.getProbability())
                .field(AnomalyRecord.ANOMALY_SCORE, record.getAnomalyScore())
                .field(AnomalyRecord.NORMALIZED_PROBABILITY, record.getNormalizedProbability())
                .field(AnomalyRecord.INITIAL_NORMALIZED_PROBABILITY, record.getInitialNormalizedProbability())
                .field(ElasticsearchMappings.ES_TIMESTAMP, bucketTime);

        List<String> topLevelExcludes = new ArrayList<>();

        if (record.getByFieldName() != null)
        {
            builder.field(AnomalyRecord.BY_FIELD_NAME, record.getByFieldName());
            if (record.getByFieldValue() != null &&
                !ReservedFieldNames.RESERVED_FIELD_NAMES.contains(record.getByFieldName()))
            {
                builder.field(record.getByFieldName(), record.getByFieldValue());
                topLevelExcludes.add(record.getByFieldName());
            }
        }
        if (record.getByFieldValue() != null)
        {
            builder.field(AnomalyRecord.BY_FIELD_VALUE, record.getByFieldValue());
        }
        if (record.getTypical() != null)
        {
            builder.field(AnomalyRecord.TYPICAL, record.getTypical());
        }
        if (record.getActual() != null)
        {
            builder.field(AnomalyRecord.ACTUAL, record.getActual());
        }
        if (record.isInterim())
        {
            builder.field(AnomalyRecord.IS_INTERIM, record.isInterim());
        }
        if (record.getFieldName() != null)
        {
            builder.field(AnomalyRecord.FIELD_NAME, record.getFieldName());
        }
        if (record.getFunction() != null)
        {
            builder.field(AnomalyRecord.FUNCTION, record.getFunction());
        }
        if (record.getFunctionDescription() != null)
        {
            builder.field(AnomalyRecord.FUNCTION_DESCRIPTION, record.getFunctionDescription());
        }
        if (record.getPartitionFieldName() != null)
        {
            builder.field(AnomalyRecord.PARTITION_FIELD_NAME, record.getPartitionFieldName());
            if (record.getPartitionFieldValue() != null &&
                !ReservedFieldNames.RESERVED_FIELD_NAMES.contains(record.getPartitionFieldName()))
            {
                builder.field(record.getPartitionFieldName(), record.getPartitionFieldValue());
                topLevelExcludes.add(record.getPartitionFieldName());
            }
        }
        if (record.getPartitionFieldValue() != null)
        {
            builder.field(AnomalyRecord.PARTITION_FIELD_VALUE, record.getPartitionFieldValue());
        }
        if (record.getOverFieldName() != null)
        {
            builder.field(AnomalyRecord.OVER_FIELD_NAME, record.getOverFieldName());
            if (record.getOverFieldValue() != null &&
                !ReservedFieldNames.RESERVED_FIELD_NAMES.contains(record.getOverFieldName()))
            {
                builder.field(record.getOverFieldName(), record.getOverFieldValue());
                topLevelExcludes.add(record.getOverFieldName());
            }
        }
        if (record.getOverFieldValue() != null)
        {
            builder.field(AnomalyRecord.OVER_FIELD_VALUE, record.getOverFieldValue());
        }
        if (record.getCauses() != null)
        {
            builder.startArray(AnomalyRecord.CAUSES);
            for (AnomalyCause cause : record.getCauses())
            {
                serialiseCause(cause, builder);
            }
            builder.endArray();
        }
        if (record.getInfluencers() != null && record.getInfluencers().isEmpty() == false)
        {
            // First add the influencers array

            builder.startArray(AnomalyRecord.INFLUENCERS);
            for (Influence influence: record.getInfluencers())
            {
                serialiseInfluence(influence, builder);
            }
            builder.endArray();

            // Then, where possible without creating duplicates, add top level
            // raw data fields
            for (Influence influence: record.getInfluencers())
            {
                if (influence.getInfluencerFieldName() != null &&
                    !influence.getInfluencerFieldValues().isEmpty() &&
                    !topLevelExcludes.contains(influence.getInfluencerFieldName()) &&
                    !ReservedFieldNames.RESERVED_FIELD_NAMES.contains(influence.getInfluencerFieldName()))
                {
                    builder.field(influence.getInfluencerFieldName(), influence.getInfluencerFieldValues().get(0));
                }
            }
        }

        builder.endObject();

        return builder;
    }

    /**
     * Augment the anomaly record serialisable content with a cause
     *
     * @param cause Cause to serialise
     * @param builder JSON builder to be augmented
     * @throws IOException
     */
    private void serialiseCause(AnomalyCause cause, XContentBuilder builder)
    throws IOException
    {
        builder.startObject()
                .field(AnomalyCause.PROBABILITY, cause.getProbability())
                .field(AnomalyCause.ACTUAL, cause.getActual())
                .field(AnomalyCause.TYPICAL, cause.getTypical());

        if (cause.getByFieldName() != null)
        {
            builder.field(AnomalyCause.BY_FIELD_NAME, cause.getByFieldName());
            if (cause.getByFieldValue() != null &&
                !ReservedFieldNames.RESERVED_FIELD_NAMES.contains(cause.getByFieldName()))
            {
                builder.field(cause.getByFieldName(), cause.getByFieldValue());
            }
        }
        if (cause.getByFieldValue() != null)
        {
            builder.field(AnomalyCause.BY_FIELD_VALUE, cause.getByFieldValue());
        }
        if (cause.getFieldName() != null)
        {
            builder.field(AnomalyCause.FIELD_NAME, cause.getFieldName());
        }
        if (cause.getFunction() != null)
        {
            builder.field(AnomalyCause.FUNCTION, cause.getFunction());
        }
        if (cause.getFunctionDescription() != null)
        {
            builder.field(AnomalyCause.FUNCTION_DESCRIPTION, cause.getFunctionDescription());
        }
        if (cause.getPartitionFieldName() != null)
        {
            builder.field(AnomalyCause.PARTITION_FIELD_NAME, cause.getPartitionFieldName());
            if (cause.getPartitionFieldValue() != null &&
                !ReservedFieldNames.RESERVED_FIELD_NAMES.contains(cause.getPartitionFieldName()))
            {
                builder.field(cause.getPartitionFieldName(), cause.getPartitionFieldValue());
            }
        }
        if (cause.getPartitionFieldValue() != null)
        {
            builder.field(AnomalyCause.PARTITION_FIELD_VALUE, cause.getPartitionFieldValue());
        }
        if (cause.getOverFieldName() != null)
        {
            builder.field(AnomalyCause.OVER_FIELD_NAME, cause.getOverFieldName());
            if (cause.getOverFieldValue() != null &&
                !ReservedFieldNames.RESERVED_FIELD_NAMES.contains(cause.getOverFieldName()))
            {
                builder.field(cause.getOverFieldName(), cause.getOverFieldValue());
            }
        }
        if (cause.getOverFieldValue() != null)
        {
            builder.field(AnomalyCause.OVER_FIELD_VALUE, cause.getOverFieldValue());
        }

        builder.endObject();
    }


    /**
     * Add the influence object to the content builder.
     *
     * @param influence Influence to serialise
     * @param builder JSON builder to be augmented
     * @throws IOException
     */
    private void serialiseInfluence(Influence influence, XContentBuilder builder)
    throws IOException
    {
        builder.startObject().field(Influence.INFLUENCER_FIELD_NAME, influence.getInfluencerFieldName());

        builder.startArray(Influence.INFLUENCER_FIELD_VALUES);
        for (String value : influence.getInfluencerFieldValues())
        {
            builder.value(value);
        }
        builder.endArray();

        builder.endObject();
    }

    private XContentBuilder serialiseBucketInfluencerStandalone(BucketInfluencer bucketInfluencer,
            Date bucketTime, boolean isInterim) throws IOException
    {
        XContentBuilder builder = jsonBuilder().startObject()
                .field(JOB_ID_NAME, m_JobId.getId())
                .field(ElasticsearchMappings.ES_TIMESTAMP, bucketTime)
                .field(BucketInfluencer.PROBABILITY, bucketInfluencer.getProbability())
                .field(BucketInfluencer.INFLUENCER_FIELD_NAME, bucketInfluencer.getInfluencerFieldName())
                .field(BucketInfluencer.INITIAL_ANOMALY_SCORE, bucketInfluencer.getInitialAnomalyScore())
                .field(BucketInfluencer.ANOMALY_SCORE, bucketInfluencer.getAnomalyScore())
                .field(BucketInfluencer.RAW_ANOMALY_SCORE, bucketInfluencer.getRawAnomalyScore());

        if (isInterim)
        {
            builder.field(Bucket.IS_INTERIM, true);
        }

        builder.endObject();

        return builder;
    }

    private void serialiseBucketInfluencerNested(BucketInfluencer bucketInfluencer,
            XContentBuilder bucketBuilder) throws IOException
    {
        bucketBuilder.startObject()
                .field(BucketInfluencer.PROBABILITY, bucketInfluencer.getProbability())
                .field(BucketInfluencer.INFLUENCER_FIELD_NAME, bucketInfluencer.getInfluencerFieldName())
                .field(BucketInfluencer.INITIAL_ANOMALY_SCORE, bucketInfluencer.getInitialAnomalyScore())
                .field(BucketInfluencer.ANOMALY_SCORE, bucketInfluencer.getAnomalyScore())
                .field(BucketInfluencer.RAW_ANOMALY_SCORE, bucketInfluencer.getRawAnomalyScore())
                .endObject();
    }

    /**
     * Return the bucket as serialisable content
     * @param bucket
     * @return
     * @throws IOException
     */
    private XContentBuilder serialiseInfluencer(Influencer influencer, boolean isInterim)
    throws IOException
    {
        XContentBuilder builder = jsonBuilder().startObject()
                .field(JOB_ID_NAME, m_JobId.getId())
                .field(ElasticsearchMappings.ES_TIMESTAMP, influencer.getTimestamp())
                .field(Influencer.PROBABILITY, influencer.getProbability())
                .field(Influencer.INFLUENCER_FIELD_NAME, influencer.getInfluencerFieldName())
                .field(Influencer.INFLUENCER_FIELD_VALUE, influencer.getInfluencerFieldValue())
                .field(Influencer.INITIAL_ANOMALY_SCORE, influencer.getInitialAnomalyScore())
                .field(Influencer.ANOMALY_SCORE, influencer.getAnomalyScore());

        if (isInterim)
        {
            builder.field(Bucket.IS_INTERIM, true);
        }

        if (!ReservedFieldNames.RESERVED_FIELD_NAMES.contains(influencer.getInfluencerFieldName()))
        {
            builder.field(influencer.getInfluencerFieldName(), influencer.getInfluencerFieldValue());
        }

        builder.endObject();

        return builder;
    }
}