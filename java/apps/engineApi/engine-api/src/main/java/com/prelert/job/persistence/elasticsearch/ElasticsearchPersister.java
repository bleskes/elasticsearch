/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2014     *
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
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.script.ScriptService;

import com.prelert.job.JobDetails;
import com.prelert.job.ModelSizeStats;
import com.prelert.job.persistence.JobResultsPersister;
import com.prelert.job.quantiles.Quantiles;
import com.prelert.rs.data.AnomalyCause;
import com.prelert.rs.data.AnomalyRecord;
import com.prelert.rs.data.Bucket;
import com.prelert.rs.data.CategoryDefinition;
import com.prelert.rs.data.Detector;

/**
 * Saves result Buckets and Quantiles to Elasticsearch<br/>
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
 * was generated by a detector which comes from its detector name field.
 * <h2>Detector</h2>
 * The Job has a fixed number of detectors but there may not be output
 * for every detector in each bucket. The detector has a name/key every
 * record has a detector name field so you can search for records by
 * detector<br/>
 * <br/>
 * <b>Quantiles</b> may contain model quantiles used in normalisation
 * and are stored in documents of type {@link Quantiles.TYPE}
 * <br/>
 * <h2>ModelSizeStats</h2>
 * This is stored in a flat structure
 * <br/>
 * @see com.prelert.job.persistence.elasticsearch.ElasticsearchMappings
 */
public class ElasticsearchPersister implements JobResultsPersister
{
    private static final Logger LOGGER = Logger.getLogger(ElasticsearchPersister.class);

    private Client m_Client;
    private String m_JobId;

    private Set<String> m_DetectorNames;

    /**
     * Create with the Elasticsearch client. Data will be written to
     * the index <code>jobId</code>
     *
     * @param jobId The job Id/Elasticsearch index
     * @param client The Elasticsearch client
     */
    public ElasticsearchPersister(String jobId, Client client)
    {
        m_JobId = jobId;
        m_Client = client;
        m_DetectorNames = new HashSet<String>();
    }

    /**
     * This implementation tracks detector names and only writes detectors
     * to the database when it sees one it hasn't seen before. If the
     * same instance is used to write all buckets in the job then the
     * detectors will only be written once else they will be written multiple
     * times and Elasticsearch will overwrite them creating new versions of
     * the document but the end result is the same.
     */
    @Override
    public void persistBucket(Bucket bucket)
    {
        if (bucket.getDetectors() == null)
        {
            return;
        }

        try
        {
            XContentBuilder content = serialiseBucket(bucket);

            IndexResponse response = m_Client.prepareIndex(m_JobId, Bucket.TYPE, bucket.getId())
                    .setSource(content)
                    .execute().actionGet();

            if (response.isCreated() == false)
            {
                LOGGER.warn(String.format("Bucket %s document has been overwritten",
                        bucket.getId()));
            }


            for (Detector detector : bucket.getDetectors())
            {
                if (m_DetectorNames.contains(detector.getName()) == false)
                {
                    m_DetectorNames.add(detector.getName());
                    // Write the detector
                    content = serialiseDetector(detector);
                    response = m_Client.prepareIndex(m_JobId, Detector.TYPE, detector.getName())
                        .setSource(content)
                        .get();
                }

                if (response.isCreated() == false)
                {
                    LOGGER.warn(String.format("Detector %s document has been overwritten",
                            detector.getName()));
                }

                BulkRequestBuilder bulkRequest = m_Client.prepareBulk();
                int count = 1;
                for (AnomalyRecord record : detector.getRecords())
                {
                    content = serialiseRecord(record, bucket.getTimestamp());

                    String recordId = record.generateNewId(bucket.getId(), detector.getName(), count);
                    bulkRequest.add(m_Client.prepareIndex(m_JobId, AnomalyRecord.TYPE, recordId)
                            .setSource(content)
                            .setParent(bucket.getId()));
                    ++count;
                }

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
        catch (IOException e)
        {
            LOGGER.error("Error writing bucket state", e);
        }
    }


    @Override
    public void persistCategoryDefinition(CategoryDefinition category)
    {
        XContentBuilder content = null;
        try
        {
            content = serialiseCategoryDefinition(category);
        }
        catch (IOException e)
        {
            LOGGER.error("Error writing category definition", e);
            return;
        }
        String categoryId = String.valueOf(category.getCategoryId());
        m_Client.prepareIndex(m_JobId, CategoryDefinition.TYPE, categoryId)
                .setSource(content)
                .execute().actionGet();
    }

    /**
     * The quantiles objects are written with one of two keys, such that
     * the latest quantiles will overwrite the previous ones.  For each ES index,
     * which corresponds to a job, there can only be 2 quantiles documents,
     * one for system change quantiles and the other for unusual behaviour
     * quantiles.
     * @param quantiles If <code>null</code> then returns straight away.
     * @throws IOException
     */
    @Override
    public void persistQuantiles(Quantiles quantiles)
    {
        if (quantiles == null)
        {
            LOGGER.warn("No quantiles to persist for job " + m_JobId);
            return;
        }

        try
        {
            XContentBuilder content = serialiseQuantiles(quantiles);

            m_Client.prepareIndex(m_JobId, Quantiles.TYPE, quantiles.getId())
                    .setSource(content)
                    .execute().actionGet();

            // Don't warn on overwrite as we expect this
        }
        catch (IOException e)
        {
            LOGGER.error("Error writing quantiles", e);
            return;
        }

        // Refresh the index when persisting quantiles so that previously
        // persisted results will be available for searching.  Do this using the
        // indices API rather than the index API (used to write the quantiles
        // above), because this will refresh all shards rather than just the
        // shard that the quantiles document itself was written to.
        commitWrites();
    }


    /**
     * Persist the memory usage data
     * @param modelSizeStats If <code>null</code> then returns straight away.
     * @throws IOException
     */
    @Override
    public void persistModelSizeStats(ModelSizeStats modelSizeStats)
    {
        if (modelSizeStats == null)
        {
            LOGGER.warn("No modelSizeStats to persist for job " + m_JobId);
            return;
        }

        try
        {
            XContentBuilder content = serialiseModelSizeStats(modelSizeStats);

            m_Client.prepareIndex(m_JobId, ModelSizeStats.TYPE, modelSizeStats.getId())
                    .setSource(content)
                    .execute().actionGet();

            // Don't warn on overwrite as we expect this
        }
        catch (IOException e)
        {
            LOGGER.error("Error writing modelSizeStats", e);
            return;
        }

        // Don't commit as we expect masses of these updates and they're only
        // for information at the API level
    }


    @Override
    public void incrementBucketCount(long count)
    {
        m_Client.prepareUpdate(m_JobId, JobDetails.TYPE, m_JobId)
                        .setScript("update-bucket-count", ScriptService.ScriptType.FILE)
                        .addScriptParam("count", count)
                        .setRetryOnConflict(3).get();
    }


    /**
     * Refreshes the elastic search index
     * @return
     */
    @Override
    public boolean commitWrites()
    {
        // refresh the index so the buckets are immediately searchable
        m_Client.admin().indices().refresh(new RefreshRequest(m_JobId)).actionGet();
        return true;
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
                .field(Bucket.ID, bucket.getId())
                .field(ElasticsearchMappings.ES_TIMESTAMP, bucket.getTimestamp())
                .field(Bucket.RAW_ANOMALY_SCORE, bucket.getRawAnomalyScore())
                .field(Bucket.ANOMALY_SCORE, bucket.getAnomalyScore())
                .field(Bucket.MAX_NORMALIZED_PROBABILITY, bucket.getMaxNormalizedProbability())
                .field(Bucket.RECORD_COUNT, bucket.getRecordCount())
                .field(Bucket.EVENT_COUNT, bucket.getEventCount());

        if (bucket.isInterim() != null)
        {
            builder.field(Bucket.IS_INTERIM, bucket.isInterim());
        }

        builder.endObject();

        return builder;
    }

    private XContentBuilder serialiseCategoryDefinition(CategoryDefinition category)
            throws IOException
    {
        List<String> examples = category.getExamples();
        return jsonBuilder().startObject()
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
                .field(Quantiles.QUANTILE_STATE, quantiles.getState())
                .endObject();
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
        return jsonBuilder().startObject()
                .field(ModelSizeStats.MODEL_BYTES, modelSizeStats.getModelBytes())
                .field(ModelSizeStats.TOTAL_BY_FIELD_COUNT, modelSizeStats.getTotalByFieldCount())
                .field(ModelSizeStats.TOTAL_OVER_FIELD_COUNT, modelSizeStats.getTotalOverFieldCount())
                .field(ModelSizeStats.TOTAL_PARTITION_FIELD_COUNT, modelSizeStats.getTotalPartitionFieldCount())
                .field(ModelSizeStats.BUCKET_ALLOCATION_FAILURES_COUNT, modelSizeStats.getBucketAllocationFailuresCount())
                .field(ModelSizeStats.MEMORY_STATUS, modelSizeStats.getMemoryStatus())
                .endObject();
    }

    /**
     * Return the detector as serialisable content
     * @param detector
     * @return
     * @throws IOException
     */
    private XContentBuilder serialiseDetector(Detector detector)
    throws IOException
    {
        return jsonBuilder().startObject()
                .field(Detector.NAME, detector.getName())
                .endObject();
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
                .field(AnomalyRecord.PROBABILITY, record.getProbability())
                .field(AnomalyRecord.ANOMALY_SCORE, record.getAnomalyScore())
                .field(AnomalyRecord.NORMALIZED_PROBABILITY, record.getNormalizedProbability())
                .field(ElasticsearchMappings.ES_TIMESTAMP, bucketTime);

        if (record.getByFieldName() != null)
        {
            builder.field(AnomalyRecord.BY_FIELD_NAME, record.getByFieldName());
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
        if (record.isInterim() != null)
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
        if (record.getPartitionFieldName() != null)
        {
            builder.field(AnomalyRecord.PARTITION_FIELD_NAME, record.getPartitionFieldName());
        }
        if (record.getPartitionFieldValue() != null)
        {
            builder.field(AnomalyRecord.PARTITION_FIELD_VALUE, record.getPartitionFieldValue());
        }
        if (record.getOverFieldName() != null)
        {
            builder.field(AnomalyRecord.OVER_FIELD_NAME, record.getOverFieldName());
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
        if (cause.getPartitionFieldName() != null)
        {
            builder.field(AnomalyCause.PARTITION_FIELD_NAME, cause.getPartitionFieldName());
        }
        if (cause.getPartitionFieldValue() != null)
        {
            builder.field(AnomalyCause.PARTITION_FIELD_VALUE, cause.getPartitionFieldValue());
        }
        if (cause.getOverFieldName() != null)
        {
            builder.field(AnomalyCause.OVER_FIELD_NAME, cause.getOverFieldName());
        }
        if (cause.getOverFieldValue() != null)
        {
            builder.field(AnomalyCause.OVER_FIELD_VALUE, cause.getOverFieldValue());
        }

        builder.endObject();
    }

}
