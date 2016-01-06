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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortOrder;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.prelert.job.CategorizerState;
import com.prelert.job.JobDetails;
import com.prelert.job.JobStatus;
import com.prelert.job.ModelSizeStats;
import com.prelert.job.ModelState;
import com.prelert.job.UnknownJobException;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.persistence.BatchedResultsIterator;
import com.prelert.job.persistence.DataStoreException;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.persistence.QueryPage;
import com.prelert.job.quantiles.Quantiles;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.CategoryDefinition;
import com.prelert.job.results.Detector;
import com.prelert.job.results.Influencer;
import com.prelert.job.usage.Usage;

public class ElasticsearchJobProvider implements JobProvider
{
    private static final Logger LOGGER = Logger.getLogger(ElasticsearchJobProvider.class);


    /**
     * The index to store total usage/metering information
     */
    public static final String PRELERT_USAGE_INDEX = "prelert-usage";

    /**
     * Where to store the prelert info in Elasticsearch - must match what's
     * expected by kibana/engineAPI/app/directives/prelertLogUsage.js
     */
    private static final String PRELERT_INFO_INDEX = "prelert-int";
    private static final String PRELERT_INFO_TYPE = "info";
    private static final String PRELERT_INFO_ID = "infoStats";

    private static final String _PARENT = "_parent";

    private static final List<String> SECONDARY_SORT = new ArrayList<>();

    private static final int UPDATE_JOB_RETRY_COUNT = 3;
    private static final int RECORDS_TAKE_PARAM = 500;


    private final Node m_Node;
    private final Client m_Client;

    private final ObjectMapper m_ObjectMapper;

    ElasticsearchJobProvider(Node node, Client client)
    {
        m_Node = Objects.requireNonNull(node);
        m_Client = Objects.requireNonNull(client);

        m_ObjectMapper = new ObjectMapper();
        m_ObjectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        m_ObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        createUsageMeteringIndex();

        LOGGER.info("Connecting to Elasticsearch cluster '" + m_Client.settings().get("cluster.name")
                + "'");
    }

    public static ElasticsearchJobProvider create(String elasticSearchHost,
            String elasticSearchClusterName, String portRange, String numProcessors)
    {
        Node node = NodeBuilder.nodeBuilder()
                .settings(buildSettings(elasticSearchHost, portRange, numProcessors))
                .client(true)
                .clusterName(elasticSearchClusterName).node();
        return new ElasticsearchJobProvider(node, node.client());
    }

    /**
     * Elasticsearch settings that instruct the node not to accept HTTP, not to
     * attempt multicast discovery and to only look for another node to connect
     * to on the given host.
     */
    private static Settings buildSettings(String host, String portRange, String numProcessors)
    {
        // Multicast discovery is expected to be disabled on the Elasticsearch
        // data node, so disable it for this embedded node too and tell it to
        // expect the data node to be on the same machine
        Builder builder = Settings.builder()
                .put("http.enabled", "false")
                .put("discovery.zen.ping.unicast.hosts", host);

        if (portRange != null && portRange.isEmpty() == false)
        {
            LOGGER.info("Using TCP port range " + portRange + " to connect to Elasticsearch");
            builder.put("transport.tcp.port", portRange);
        }
        if (numProcessors != null && numProcessors.isEmpty() == false)
        {
            LOGGER.info("Telling Elasticsearch there are " + numProcessors
                    + " processors on this machine");
            builder.put("processors", numProcessors);
        }

        return builder.build();
    }

    /**
     * Close the Elasticsearch node
     */
    @Override
    public void close() throws IOException
    {
        m_Node.close();
    }

    public Client getClient()
    {
        return m_Client;
    }

    /**
     * If the {@value ElasticsearchJobProvider#PRELERT_USAGE_INDEX} index does
     * not exist create it here with the usage document mapping.
     */
    private void createUsageMeteringIndex()
    {
        try
        {
            LOGGER.trace("ES API CALL: index exists? " + PRELERT_USAGE_INDEX);
            boolean indexExists = m_Client.admin().indices()
                    .exists(new IndicesExistsRequest(PRELERT_USAGE_INDEX))
                    .get().isExists();

            if (indexExists == false)
            {
                LOGGER.info("Creating the internal '" + PRELERT_USAGE_INDEX + "' index");

                XContentBuilder usageMapping = ElasticsearchMappings.usageMapping();

                LOGGER.trace("ES API CALL: create index " + PRELERT_USAGE_INDEX);
                m_Client.admin().indices().prepareCreate(PRELERT_USAGE_INDEX)
                                .addMapping(Usage.TYPE, usageMapping)
                                .get();
                LOGGER.trace("ES API CALL: wait for yellow status " + PRELERT_USAGE_INDEX);
                m_Client.admin().cluster().prepareHealth(PRELERT_USAGE_INDEX).setWaitForYellowStatus().execute().actionGet();
            }
        }
        catch (InterruptedException | ExecutionException e)
        {
            LOGGER.warn("Error checking the usage metering index", e);
        }
        catch (IOException e)
        {
            LOGGER.warn("Error creating the usage metering index", e);
        }

    }

    @Override
    public void checkJobExists(String jobId) throws UnknownJobException
    {
        ElasticsearchJobId elasticJobId = new ElasticsearchJobId(jobId);
        try
        {
            LOGGER.trace("ES API CALL: get ID " + elasticJobId.getId() +
                    " type " + JobDetails.TYPE + " from index " + elasticJobId.getIndex());
            GetResponse response = m_Client.prepareGet(elasticJobId.getIndex(), JobDetails.TYPE, elasticJobId.getId())
                            .setFetchSource(false)
                            .setFields()
                            .get();

            if (response.isExists() == false)
            {
                String msg = "No job document with id " + elasticJobId.getId();
                LOGGER.warn(msg);
                throw new UnknownJobException(elasticJobId.getId());
            }
        }
        catch (IndexNotFoundException e)
        {
            // the job does not exist
            String msg = "Missing Index: no job with id " + elasticJobId.getId();
            LOGGER.warn(msg);
            throw new UnknownJobException(elasticJobId.getId());
        }
    }


    @Override
    public boolean jobIdIsUnique(String jobId)
    {
        return indexExists(new ElasticsearchJobId(jobId)) == false;
    }

    private boolean indexExists(ElasticsearchJobId jobId)
    {
        LOGGER.trace("ES API CALL: index exists? " + jobId.getIndex());
        IndicesExistsResponse res =
                m_Client.admin().indices().exists(new IndicesExistsRequest(jobId.getIndex())).actionGet();

        return res.isExists();
    }


    @Override
    public Optional<JobDetails> getJobDetails(String jobId)
    {
        ElasticsearchJobId elasticJobId = new ElasticsearchJobId(jobId);
        try
        {
            LOGGER.trace("ES API CALL: get ID " + elasticJobId.getId() +
                    " type " + JobDetails.TYPE + " from index " + elasticJobId.getIndex());
            GetResponse response = m_Client.prepareGet(elasticJobId.getIndex(), JobDetails.TYPE, elasticJobId.getId()).get();
            if (!response.isExists())
            {
                String msg = "No details for job with id " + elasticJobId.getId();
                LOGGER.warn(msg);
                return Optional.empty();
            }
            JobDetails details = m_ObjectMapper.convertValue(response.getSource(), JobDetails.class);

            // Pull out the modelSizeStats document, and add this to the JobDetails
            LOGGER.trace("ES API CALL: get ID " + ModelSizeStats.TYPE +
                    " type " + ModelSizeStats.TYPE + " from index " + elasticJobId.getIndex());
            GetResponse modelSizeStatsResponse = m_Client.prepareGet(
                    elasticJobId.getIndex(), ModelSizeStats.TYPE, ModelSizeStats.TYPE).get();
            if (!modelSizeStatsResponse.isExists())
            {
                String msg = "No model size stats for job with id " + elasticJobId.getId();
                LOGGER.warn(msg);
            }
            else
            {
                ModelSizeStats modelSizeStats = m_ObjectMapper.convertValue(
                    modelSizeStatsResponse.getSource(), ModelSizeStats.class);
                details.setModelSizeStats(modelSizeStats);
            }

            return Optional.of(details);
        }
        catch (IndexNotFoundException e)
        {
            // the job does not exist
            String msg = "Missing Index no job with id " + elasticJobId.getId();
            LOGGER.warn(msg);
            return Optional.empty();
        }
    }

    @Override
    public QueryPage<JobDetails> getJobs(int skip, int take)
    {
        QueryBuilder fb = QueryBuilders.matchAllQuery();
        SortBuilder sb = new FieldSortBuilder(JobDetails.ID)
                                .unmappedType("string")
                                .order(SortOrder.ASC);

        LOGGER.trace("ES API CALL: search all of type " + JobDetails.TYPE +
                " from all indexes sort ascending " + JobDetails.ID +
                " skip " + skip + " take " + take);
        SearchResponse response = m_Client.prepareSearch("_all")
                .setTypes(JobDetails.TYPE)
                .setPostFilter(fb)
                .setFrom(skip).setSize(take)
                .addSort(sb)
                .get();

        List<JobDetails> jobs = new ArrayList<>();
        for (SearchHit hit : response.getHits().getHits())
        {
            JobDetails job;
            try
            {
                job = m_ObjectMapper.convertValue(hit.getSource(), JobDetails.class);
            }
            catch (IllegalArgumentException e)
            {
                LOGGER.error("Cannot parse job from JSON", e);
                continue;
            }
            ElasticsearchJobId elasticJobId = new ElasticsearchJobId(job.getId());

            // Pull out the modelSizeStats document, and add this to the JobDetails
            LOGGER.trace("ES API CALL: get ID " + ModelSizeStats.TYPE +
                    " type " + ModelSizeStats.TYPE + " from index " + elasticJobId.getIndex());
            GetResponse modelSizeStatsResponse = m_Client.prepareGet(
                    elasticJobId.getIndex(), ModelSizeStats.TYPE, ModelSizeStats.TYPE).get();

            if (!modelSizeStatsResponse.isExists())
            {
                String msg = "No memory usage details for job with id " + job.getId();
                LOGGER.warn(msg);
            }
            else
            {
                ModelSizeStats modelSizeStats = m_ObjectMapper.convertValue(
                    modelSizeStatsResponse.getSource(), ModelSizeStats.class);
                job.setModelSizeStats(modelSizeStats);
            }
            jobs.add(job);
        }

        QueryPage<JobDetails> page = new QueryPage<JobDetails>(jobs, response.getHits().getTotalHits());
        return page;
    }

    /**
     * Create the Elasticsearch index and the mappings
     * @throws
     */
    @Override
    public boolean createJob(JobDetails job)
    {
        try
        {
            XContentBuilder jobMapping = ElasticsearchMappings.jobMapping();
            XContentBuilder bucketMapping = ElasticsearchMappings.bucketMapping();
            XContentBuilder categorizerStateMapping = ElasticsearchMappings.categorizerStateMapping();
            XContentBuilder categoryDefinitionMapping = ElasticsearchMappings.categoryDefinitionMapping();
            XContentBuilder detectorMapping = ElasticsearchMappings.detectorMapping();
            XContentBuilder recordMapping = ElasticsearchMappings.recordMapping();
            XContentBuilder quantilesMapping = ElasticsearchMappings.quantilesMapping();
            XContentBuilder modelStateMapping = ElasticsearchMappings.modelStateMapping();
            XContentBuilder usageMapping = ElasticsearchMappings.usageMapping();
            XContentBuilder modelSizeStatsMapping = ElasticsearchMappings.modelSizeStatsMapping();
            XContentBuilder influencerMapping = ElasticsearchMappings.influencerMapping();

            ElasticsearchJobId elasticJobId = new ElasticsearchJobId(job.getId());

            LOGGER.trace("ES API CALL: create index " + job.getId());
            m_Client.admin().indices()
                    .prepareCreate(elasticJobId.getIndex())
                    .addMapping(JobDetails.TYPE, jobMapping)
                    .addMapping(Bucket.TYPE, bucketMapping)
                    .addMapping(CategorizerState.TYPE, categorizerStateMapping)
                    .addMapping(CategoryDefinition.TYPE, categoryDefinitionMapping)
                    .addMapping(Detector.TYPE, detectorMapping)
                    .addMapping(AnomalyRecord.TYPE, recordMapping)
                    .addMapping(Quantiles.TYPE, quantilesMapping)
                    .addMapping(ModelState.TYPE, modelStateMapping)
                    .addMapping(Usage.TYPE, usageMapping)
                    .addMapping(ModelSizeStats.TYPE, modelSizeStatsMapping)
                    .addMapping(Influencer.TYPE, influencerMapping)
                    .get();
            LOGGER.trace("ES API CALL: wait for yellow status " + elasticJobId.getId());
            m_Client.admin().cluster().prepareHealth(elasticJobId.getIndex())
                    .setWaitForYellowStatus().execute().actionGet();


            String json = m_ObjectMapper.writeValueAsString(job);

            LOGGER.trace("ES API CALL: index " + JobDetails.TYPE +
                    " to index " + elasticJobId.getIndex() + " with ID " + elasticJobId.getId());
            m_Client.prepareIndex(elasticJobId.getIndex(), JobDetails.TYPE, elasticJobId.getId())
                    .setSource(json)
                    .setRefresh(true)
                    .get();

            return true;
        }
        catch (ElasticsearchException e)
        {
            LOGGER.error("Error writing Elasticsearch mappings", e);
            throw e;
        }
        catch (IOException e)
        {
            LOGGER.error("Error writing Elasticsearch mappings", e);
        }

        return false;
    }

    @Override
    public boolean updateJob(String jobId, Map<String, Object> updates) throws UnknownJobException
    {
        checkJobExists(jobId);
        ElasticsearchJobId elasticJobId = new ElasticsearchJobId(jobId);

        int retryCount = UPDATE_JOB_RETRY_COUNT;
        while (--retryCount >= 0)
        {
            try
            {
                LOGGER.trace("ES API CALL: update ID " + elasticJobId.getId() + " type " + JobDetails.TYPE +
                        " in index " + elasticJobId.getIndex() + " using map of new values");
                m_Client.prepareUpdate(elasticJobId.getIndex(), JobDetails.TYPE, elasticJobId.getId())
                                    .setDoc(updates)
                                    .get();

                break;
            }
            catch (VersionConflictEngineException e)
            {
                LOGGER.warn("Conflict updating job document " + elasticJobId.getId());
            }
        }

        if (retryCount <= 0)
        {
            LOGGER.warn("Unable to update conflicted job document " + elasticJobId.getId() +
                    ". Updates = " + updates);
            return false;
        }
        return true;
    }

    @Override
    public boolean setJobStatus(String jobId, JobStatus status) throws UnknownJobException
    {
        Map<String, Object> update = new HashMap<>();
        update.put(JobDetails.STATUS, status);
        return updateJob(jobId, update);

    }

    @Override
    public boolean setJobFinishedTimeAndStatus(String jobId, Date time, JobStatus status)
            throws UnknownJobException
    {
        Map<String, Object> update = new HashMap<>();
        update.put(JobDetails.FINISHED_TIME, time);
        update.put(JobDetails.STATUS, status);
        return updateJob(jobId, update);
    }


    @Override
    public boolean deleteJob(String jobId) throws UnknownJobException, DataStoreException
    {
        ElasticsearchJobId elasticJobId = new ElasticsearchJobId(jobId);
        if (indexExists(elasticJobId) == false)
        {
            throw new UnknownJobException(elasticJobId.getId());
        }
        LOGGER.trace("ES API CALL: delete index " + elasticJobId.getIndex());

        try
        {
            DeleteIndexResponse response = m_Client.admin()
                    .indices().delete(new DeleteIndexRequest(elasticJobId.getIndex())).get();
            return response.isAcknowledged();
        }
        catch (InterruptedException|ExecutionException e)
        {
            String msg = "Error deleting index '" + elasticJobId.getIndex() + "'";
            ErrorCodes errorCode = ErrorCodes.DATA_STORE_ERROR;
            if (e.getCause() instanceof IndexNotFoundException)
            {
                msg = "Cannot delete job - no index with id '" + elasticJobId.getIndex() + " in the database";
                errorCode = ErrorCodes.MISSING_JOB_ERROR;
            }
            LOGGER.warn(msg);
            throw new UnknownJobException(jobId, msg, errorCode);
        }
    }

    @Override
    public QueryPage<Bucket> buckets(String jobId,
            boolean expand, boolean includeInterim, int skip, int take,
            double anomalyScoreThreshold, double normalizedProbabilityThreshold)
    throws UnknownJobException
    {
        return buckets(jobId, expand, includeInterim, skip, take, 0, 0, anomalyScoreThreshold,
                normalizedProbabilityThreshold);
    }

    @Override
    public QueryPage<Bucket> buckets(String jobId, boolean expand,
            boolean includeInterim, int skip, int take, long startEpochMs, long endEpochMs,
            double anomalyScoreThreshold, double normalizedProbabilityThreshold)
    throws UnknownJobException
    {
        QueryBuilder fb = new ResultsFilterBuilder()
                .timeRange(ElasticsearchMappings.ES_TIMESTAMP, startEpochMs, endEpochMs)
                .score(Bucket.ANOMALY_SCORE, anomalyScoreThreshold)
                .score(Bucket.MAX_NORMALIZED_PROBABILITY, normalizedProbabilityThreshold)
                .interim(Bucket.IS_INTERIM, includeInterim)
                .build();
        return buckets(new ElasticsearchJobId(jobId), expand, includeInterim, skip, take, fb);
    }

    private QueryPage<Bucket> buckets(ElasticsearchJobId jobId, boolean expand, boolean includeInterim,
            int skip, int take, QueryBuilder fb) throws UnknownJobException
    {
        SortBuilder sb = new FieldSortBuilder(Bucket.ID)
                    .unmappedType("string")
                    .order(SortOrder.ASC);

        SearchResponse searchResponse;
        try
        {
            LOGGER.trace("ES API CALL: search all of type " + Bucket.TYPE +
                    " from index " + jobId.getIndex() + " sort ascending " + Bucket.ID +
                    " with filter after sort skip " + skip + " take " + take);
            searchResponse = m_Client.prepareSearch(jobId.getIndex())
                                        .setTypes(Bucket.TYPE)
                                        .addSort(sb)
                                        .setPostFilter(fb)
                                        .setFrom(skip).setSize(take)
                                        .get();
        }
        catch (IndexNotFoundException e)
        {
            throw new UnknownJobException(jobId.getId());
        }

        List<Bucket> results = new ArrayList<>();


        for (SearchHit hit : searchResponse.getHits().getHits())
        {
            // Remove the Kibana/Logstash '@timestamp' entry as stored in Elasticsearch,
            // and replace using the API 'timestamp' key.
            Object timestamp = hit.getSource().remove(ElasticsearchMappings.ES_TIMESTAMP);
            hit.getSource().put(Bucket.TIMESTAMP, timestamp);

            Bucket bucket = m_ObjectMapper.convertValue(hit.getSource(), Bucket.class);

            if (expand && bucket.getRecordCount() > 0)
            {
                expandBucket(jobId.getId(), includeInterim, bucket);
            }

            results.add(bucket);
        }

        return new QueryPage<>(results, searchResponse.getHits().getTotalHits());
    }


    @Override
    public Optional<Bucket> bucket(String jobId,
            String bucketId, boolean expand, boolean includeInterim)
    throws UnknownJobException
    {
        ElasticsearchJobId elasticJobId = new ElasticsearchJobId(jobId);
        GetResponse response;

        try
        {
            LOGGER.trace("ES API CALL: get ID " + bucketId + " type " + Bucket.TYPE +
                    " from index " + elasticJobId.getIndex());
            response = m_Client.prepareGet(elasticJobId.getIndex(), Bucket.TYPE, bucketId).get();
        }
        catch (IndexNotFoundException e)
        {
            throw new UnknownJobException(elasticJobId.getId());
        }

        Optional<Bucket> doc = Optional.<Bucket>empty();
        if (response.isExists())
        {
            // Remove the Kibana/Logstash '@timestamp' entry as stored in Elasticsearch,
            // and replace using the API 'timestamp' key.
            Object timestamp = response.getSource().remove(ElasticsearchMappings.ES_TIMESTAMP);
            response.getSource().put(Bucket.TIMESTAMP, timestamp);

            Bucket bucket = m_ObjectMapper.convertValue(response.getSource(), Bucket.class);
            if (includeInterim || bucket.isInterim() == false)
            {
                if (expand && bucket.getRecordCount() > 0)
                {
                    expandBucket(jobId, includeInterim, bucket);
                }

                doc = Optional.of(bucket);
            }
        }

        return doc;
    }

    @Override
    public BatchedResultsIterator<Bucket> newBatchedBucketsIterator(String jobId)
    {
        return new ElasticsearchBatchedBucketsIterator(m_Client, jobId, m_ObjectMapper);
    }

    @Override
    public int expandBucket(String jobId, boolean includeInterim, Bucket bucket)
    throws UnknownJobException
    {
        int skip = 0;

        QueryPage<AnomalyRecord> page = bucketRecords(
                jobId, bucket, skip, RECORDS_TAKE_PARAM, includeInterim,
                AnomalyRecord.PROBABILITY, false);
        bucket.setRecords(page.queryResults());

        while (page.hitCount() > skip + RECORDS_TAKE_PARAM)
        {
            skip += RECORDS_TAKE_PARAM;
            page = bucketRecords(
                    jobId, bucket, skip, RECORDS_TAKE_PARAM, includeInterim,
                    AnomalyRecord.PROBABILITY, false);
            bucket.getRecords().addAll(page.queryResults());
        }

        return bucket.getRecords().size();
    }


    @Override
    public QueryPage<AnomalyRecord> bucketRecords(String jobId,
            Bucket bucket, int skip, int take, boolean includeInterim, String sortField, boolean descending)
    throws UnknownJobException
    {
        // Find the records using the time stamp rather than a parent-child
        // relationship.  The parent-child filter involves two queries behind
        // the scenes, and Elasticsearch documentation claims it's significantly
        // slower.  Here we rely on the record timestamps being identical to the
        // bucket timestamp.
        QueryBuilder recordFilter = QueryBuilders.termQuery(ElasticsearchMappings.ES_TIMESTAMP,
                bucket.getTimestamp().getTime());

        recordFilter = new ResultsFilterBuilder(recordFilter).interim(
                AnomalyRecord.IS_INTERIM, includeInterim).build();

        SortBuilder sb = null;
        if (sortField != null)
        {
            sb = new FieldSortBuilder(sortField)
                        .missing("_last")
                        .order(descending ? SortOrder.DESC : SortOrder.ASC);
        }

        List<String> secondarySort = Arrays.asList(new String[] {
            AnomalyRecord.ANOMALY_SCORE,
            AnomalyRecord.OVER_FIELD_VALUE,
            AnomalyRecord.PARTITION_FIELD_VALUE,
            AnomalyRecord.BY_FIELD_VALUE,
            AnomalyRecord.FIELD_NAME,
            AnomalyRecord.FUNCTION}
        );

        return records(new ElasticsearchJobId(jobId), skip, take, recordFilter, sb, secondarySort,
                descending);
    }


    @Override
    public QueryPage<CategoryDefinition> categoryDefinitions(String jobId, int skip, int take)
            throws UnknownJobException
    {
        ElasticsearchJobId elasticJobId = new ElasticsearchJobId(jobId);
        LOGGER.trace("ES API CALL: search all of type " + CategoryDefinition.TYPE +
                " from index " + elasticJobId.getIndex() + " sort ascending " + CategoryDefinition.CATEGORY_ID +
                " skip " + skip + " take " + take);
        SearchRequestBuilder searchBuilder = m_Client.prepareSearch(elasticJobId.getIndex())
                .setTypes(CategoryDefinition.TYPE)
                .setFrom(skip).setSize(take)
                .addSort(new FieldSortBuilder(CategoryDefinition.CATEGORY_ID).order(SortOrder.ASC));

        SearchResponse searchResponse;
        try
        {
            searchResponse = searchBuilder.get();
        }
        catch (IndexNotFoundException e)
        {
            throw new UnknownJobException(jobId);
        }

        List<CategoryDefinition> results = Arrays.stream(searchResponse.getHits().getHits())
                .map(hit -> m_ObjectMapper.convertValue(hit.getSource(), CategoryDefinition.class))
                .collect(Collectors.toList());

        return new QueryPage<>(results, searchResponse.getHits().getTotalHits());
    }


    @Override
    public Optional<CategoryDefinition> categoryDefinition(String jobId, String categoryId)
            throws UnknownJobException
    {
        ElasticsearchJobId elasticJobId = new ElasticsearchJobId(jobId);
        GetResponse response;

        try
        {
            LOGGER.trace("ES API CALL: get ID " + categoryId + " type " + CategoryDefinition.TYPE +
                    " from index " + elasticJobId.getIndex());
            response = m_Client.prepareGet(elasticJobId.getIndex(), CategoryDefinition.TYPE, categoryId).get();
        }
        catch (IndexNotFoundException e)
        {
            throw new UnknownJobException(jobId);
        }

        return response.isExists() ? Optional.of(m_ObjectMapper.convertValue(response.getSource(),
                CategoryDefinition.class)) : Optional.<CategoryDefinition> empty();
    }

    @Override
    public QueryPage<AnomalyRecord> records(String jobId,
            int skip, int take, boolean includeInterim, String sortField, boolean descending,
            double anomalyScoreThreshold, double normalizedProbabilityThreshold)
    throws UnknownJobException
    {
        return records(jobId, skip, take, 0, 0, includeInterim, sortField, descending,
                anomalyScoreThreshold, normalizedProbabilityThreshold);
    }

    @Override
    public QueryPage<AnomalyRecord> records(String jobId,
            int skip, int take, long startEpochMs, long endEpochMs,
            boolean includeInterim, String sortField, boolean descending,
            double anomalyScoreThreshold, double normalizedProbabilityThreshold)
    throws UnknownJobException
    {
        QueryBuilder fb = new ResultsFilterBuilder()
                .timeRange(ElasticsearchMappings.ES_TIMESTAMP, startEpochMs, endEpochMs)
                .score(AnomalyRecord.ANOMALY_SCORE, anomalyScoreThreshold)
                .score(AnomalyRecord.NORMALIZED_PROBABILITY, normalizedProbabilityThreshold)
                .interim(AnomalyRecord.IS_INTERIM, includeInterim)
                .build();
        return records(new ElasticsearchJobId(jobId), skip, take, fb, sortField, descending);
    }

    private QueryPage<AnomalyRecord> records(ElasticsearchJobId jobId,
            int skip, int take, QueryBuilder recordFilter,
            String sortField, boolean descending)
    throws UnknownJobException
    {
        SortBuilder sb = null;
        if (sortField != null)
        {
            sb = new FieldSortBuilder(sortField)
                        .missing("_last")
                        .order(descending ? SortOrder.DESC : SortOrder.ASC);
        }

        return records(jobId, skip, take, recordFilter, sb, SECONDARY_SORT, descending);
    }


    /**
     * The returned records have the parent bucket id set.
     */
    private QueryPage<AnomalyRecord> records(ElasticsearchJobId jobId, int skip, int take,
            QueryBuilder recordFilter, SortBuilder sb, List<String> secondarySort,
            boolean descending)
    throws UnknownJobException
    {
        SearchRequestBuilder searchBuilder = m_Client.prepareSearch(jobId.getIndex())
                .setTypes(AnomalyRecord.TYPE)
                .setPostFilter(recordFilter)
                .setFrom(skip).setSize(take)
                .addField(_PARENT)   // include the parent id
                .setFetchSource(true);  // the field option turns off source so request it explicitly


        if (sb != null)
        {
            searchBuilder.addSort(sb);
        }

        for (String sortField : secondarySort)
        {
            searchBuilder.addSort(sortField, descending ? SortOrder.DESC : SortOrder.ASC);
        }


        SearchResponse searchResponse;
        try
        {
            LOGGER.trace("ES API CALL: search all of type " + AnomalyRecord.TYPE +
                    " from index " + jobId.getIndex() + ((sb != null) ? " with sort" : "") +
                    (secondarySort.isEmpty() ? "" : " with secondary sort") +
                    " with filter after sort skip " + skip + " take " + take);
            searchResponse = searchBuilder.get();
        }
        catch (IndexNotFoundException e)
        {
            throw new UnknownJobException(jobId.getId());
        }

        List<AnomalyRecord> results = new ArrayList<>();
        for (SearchHit hit : searchResponse.getHits().getHits())
        {
            Map<String, Object> m  = hit.getSource();

            // replace logstash timestamp name with timestamp
            m.put(Bucket.TIMESTAMP, m.remove(ElasticsearchMappings.ES_TIMESTAMP));

            AnomalyRecord record = m_ObjectMapper.convertValue(
                    m, AnomalyRecord.class);

            // set the ID and parent ID
            record.setId(hit.getId());
            record.setParent(hit.field(_PARENT).getValue().toString());

            results.add(record);
        }

        return new QueryPage<>(results, searchResponse.getHits().getTotalHits());
    }


    @Override
    public QueryPage<Influencer> influencers(String jobId, int skip, int take,
                                            boolean includeInterim)
    throws UnknownJobException
    {
        QueryBuilder builder = new ResultsFilterBuilder()
                                .interim(Bucket.IS_INTERIM, includeInterim)
                                .build();

        return influencers(new ElasticsearchJobId(jobId), skip, take, builder,
                Influencer.ANOMALY_SCORE, true);
    }

    @Override
    public QueryPage<Influencer> influencers(String jobId, int skip, int take, long startEpochMs,
            long endEpochMs, String sortField, boolean sortDescending, double anomalyScoreFilter,
            boolean includeInterim)
    throws UnknownJobException
    {
        QueryBuilder fb = new ResultsFilterBuilder()
                .timeRange(Influencer.TIMESTAMP, startEpochMs, endEpochMs)
                .score(Influencer.ANOMALY_SCORE, anomalyScoreFilter)
                .interim(Bucket.IS_INTERIM, includeInterim)
                .build();

        return influencers(new ElasticsearchJobId(jobId), skip, take, fb, sortField,
                            sortDescending);
    }

    private QueryPage<Influencer> influencers(ElasticsearchJobId jobId, int skip, int take,
            QueryBuilder filterBuilder, String sortField, boolean sortDescending)
            throws UnknownJobException
    {
        LOGGER.trace("ES API CALL: search all of type " + Influencer.TYPE + " from index " + jobId.getIndex()
                + ((sortField != null) ? " with sort "
                + (sortDescending ? "descending" : "ascending") + " on field " + sortField : "")
                + " with filter after sort skip " + skip + " take " + take);

        SearchRequestBuilder searchRequestBuilder = m_Client.prepareSearch(jobId.getIndex())
                .setTypes(Influencer.TYPE)
                .setPostFilter(filterBuilder)
                .setFrom(skip).setSize(take);

        if (sortField != null)
        {
            SortBuilder sb = new FieldSortBuilder(sortField).order(
                    sortDescending ? SortOrder.DESC : SortOrder.ASC);
            searchRequestBuilder.addSort(sb);
        }
        SearchResponse response = null;
        try
        {
            response = searchRequestBuilder.get();
        }
        catch (IndexNotFoundException e)
        {
            throw new UnknownJobException(jobId.getId());
        }

        List<Influencer> influencers = new ArrayList<>();
        for (SearchHit hit : response.getHits().getHits())
        {
            Map<String, Object> m = hit.getSource();

            // replace logstash timestamp name with timestamp
            m.put(Bucket.TIMESTAMP, m.remove(ElasticsearchMappings.ES_TIMESTAMP));

            Influencer influencer = m_ObjectMapper.convertValue(m, Influencer.class);

            influencers.add(influencer);
        }

        return new QueryPage<>(influencers, response.getHits().getTotalHits());
    }

    @Override
    public Optional<Influencer> influencer(String jobId, String influencerId)
    {
        throw new IllegalStateException();
    }

    @Override
    public BatchedResultsIterator<Influencer> newBatchedInfluencersIterator(String jobId)
    {
        return new ElasticsearchBatchedInfluencersIterator(m_Client, jobId, m_ObjectMapper);
    }

    /**
     * Always returns true
     */
    @Override
    public boolean savePrelertInfo(String infoDoc)
    {
        LOGGER.trace("ES API CALL: index type " + PRELERT_INFO_TYPE +
                " in index " + PRELERT_INFO_INDEX + " with ID " + PRELERT_INFO_ID);

        m_Client.prepareIndex(PRELERT_INFO_INDEX, PRELERT_INFO_TYPE, PRELERT_INFO_ID)
                        .setSource(infoDoc)
                        .execute().actionGet();

        return true;
    }


    @Override
    public Quantiles getQuantiles(String jobId)
    throws UnknownJobException
    {
        ElasticsearchJobId elasticJobId = new ElasticsearchJobId(jobId);
        try
        {
            LOGGER.trace("ES API CALL: get ID " + Quantiles.QUANTILES_ID +
                    " type " + Quantiles.TYPE + " from index " + elasticJobId.getIndex());
            GetResponse response = m_Client.prepareGet(
                    elasticJobId.getIndex(), Quantiles.TYPE, Quantiles.QUANTILES_ID).get();
            if (!response.isExists())
            {
                LOGGER.info("There are currently no quantiles for job " + jobId);
                return new Quantiles();
            }
            return checkQuantilesVersion(jobId, response) ? createQuantiles(jobId, response)
                    : new Quantiles();
        }
        catch (IndexNotFoundException e)
        {
            LOGGER.error("Missing index when getting quantiles", e);
            throw new UnknownJobException(jobId);
        }
    }

    private boolean checkQuantilesVersion(String jobId, GetResponse response)
    {
        Object version = response.getSource().get(Quantiles.VERSION);
        if (!Quantiles.CURRENT_VERSION.equals(version.toString()))
        {
            LOGGER.warn(
                    "Cannot restore quantiles: version of quantiles for job " + jobId + " is older "
                            + "than the current one. New quantiles will be used instead.");
            return false;
        }
        return true;
    }

    private static Quantiles createQuantiles(String jobId, GetResponse response)
    {
        Quantiles quantiles = new Quantiles();
        Object state = response.getSource().get(Quantiles.QUANTILE_STATE);
        if (state == null)
        {
            LOGGER.error("Inconsistency - no " + Quantiles.QUANTILE_STATE
                    + " field in quantiles for job " + jobId);
        }
        else
        {
            quantiles.setState(state.toString());
        }
        return quantiles;
    }

    @Override
    public void refreshIndex(String jobId)
    {
        String indexName = new ElasticsearchJobId(jobId).getIndex();
        m_Client.admin().indices().refresh(new RefreshRequest(indexName)).actionGet();
    }
}

