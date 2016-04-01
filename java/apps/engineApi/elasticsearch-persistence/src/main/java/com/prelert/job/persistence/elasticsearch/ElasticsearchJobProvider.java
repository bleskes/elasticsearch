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
import java.util.Arrays;
import java.util.Collection;
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
import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexAlreadyExistsException;
import org.elasticsearch.node.Node;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.prelert.job.CategorizerState;
import com.prelert.job.JobDetails;
import com.prelert.job.JobStatus;
import com.prelert.job.JsonViews;
import com.prelert.job.ModelSizeStats;
import com.prelert.job.ModelSnapshot;
import com.prelert.job.ModelState;
import com.prelert.job.NoSuchModelSnapshotException;
import com.prelert.job.SchedulerState;
import com.prelert.job.UnknownJobException;
import com.prelert.job.audit.AuditMessage;
import com.prelert.job.audit.Auditor;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.persistence.BatchedResultsIterator;
import com.prelert.job.persistence.DataStoreException;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.persistence.QueryPage;
import com.prelert.job.quantiles.Quantiles;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.BucketInfluencer;
import com.prelert.job.results.CategoryDefinition;
import com.prelert.job.results.Influencer;
import com.prelert.job.results.ModelDebugOutput;
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

    private static final String SETTING_TRANSLOG_DURABILITY = "index.translog.durability";
    private static final String ASYNC = "async";
    private static final String SETTING_MAPPER_DYNAMIC = "index.mapper.dynamic";
    private static final String SETTING_DEFAULT_ANALYZER_TYPE = "index.analysis.analyzer.default.type";
    private static final String KEYWORD = "keyword";

    private static final List<String> SECONDARY_SORT = new ArrayList<>();

    private static final int UPDATE_JOB_RETRY_COUNT = 3;
    private static final int RECORDS_TAKE_PARAM = 500;


    private final Node m_Node;
    private final Client m_Client;

    private final ObjectMapper m_ObjectMapper;

    public ElasticsearchJobProvider(Node node, Client client)
    {
        m_Node = node;
        m_Client = Objects.requireNonNull(client);

        m_ObjectMapper = new ObjectMapper();
        m_ObjectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        m_ObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // When we serialise objects with multiple views we want to choose the datastore view
        m_ObjectMapper.setConfig(m_ObjectMapper.getSerializationConfig().withView(JsonViews.DatastoreView.class));

        LOGGER.info("Connecting to Elasticsearch cluster '" + m_Client.settings().get("cluster.name")
                + "'");

        // This call was added because if we try to connect to Elasticsearch
        // while it's doing the recovery operations it does at startup then we
        // can get weird effects like indexes being reported as not existing
        // when they do.  See EL16-182 in Jira.
        LOGGER.trace("ES API CALL: wait for yellow status on whole cluster");
        m_Client.admin().cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet();

        LOGGER.info("Elasticsearch cluster '" + m_Client.settings().get("cluster.name")
                + "' now ready to use");

        createUsageMeteringIndex();
    }

    /**
     * Close the Elasticsearch node or client
     */
    @Override
    public void shutdown()
    {
        m_Client.close();
        LOGGER.info("Elasticsearch client shut down");
        if (m_Node != null)
        {
            m_Node.close();
            LOGGER.info("Elasticsearch node shut down");
        }
    }

    /**
     * If the {@value ElasticsearchJobProvider#PRELERT_USAGE_INDEX} index does
     * not exist then create it here with the usage document mapping.
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
                                .setSettings(prelertIndexSettings())
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
        catch (IndexAlreadyExistsException | IOException e)
        {
            LOGGER.warn("Error creating the usage metering index", e);
        }
    }

    /**
     * If the {@value ElasticsearchJobProvider#PRELERT_INFO_INDEX} index does
     * not exist then create it here.
     */
    private void createInfoIndex()
    {
        try
        {
            LOGGER.trace("ES API CALL: index exists? " + PRELERT_INFO_INDEX);
            boolean indexExists = m_Client.admin().indices()
                    .exists(new IndicesExistsRequest(PRELERT_INFO_INDEX))
                    .get().isExists();

            if (indexExists == false)
            {
                LOGGER.info("Creating the internal '" + PRELERT_INFO_INDEX + "' index");

                LOGGER.trace("ES API CALL: create index " + PRELERT_INFO_INDEX);
                m_Client.admin().indices().prepareCreate(PRELERT_INFO_INDEX)
                                .setSettings(prelertIndexSettings())
                                .addMapping(AuditMessage.TYPE, ElasticsearchMappings.auditMessageMapping())
                                .get();
                LOGGER.trace("ES API CALL: wait for yellow status " + PRELERT_INFO_INDEX);
                m_Client.admin().cluster().prepareHealth(PRELERT_INFO_INDEX).setWaitForYellowStatus().execute().actionGet();
            }
        }
        catch (InterruptedException | ExecutionException | IOException e)
        {
            LOGGER.warn("Error checking the info index", e);
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

    /**
     * Build the Elasticsearch index settings that we want to apply to Prelert
     * indexes.  It's better to do this in code rather than in elasticsearch.yml
     * because then the settings can be applied regardless of whether we're
     * using our own Elasticsearch to store results or a customer's pre-existing
     * Elasticsearch.
     * @return An Elasticsearch builder initialised with the desired settings
     * for Prelert indexes.
     */
    private Settings.Builder prelertIndexSettings()
    {
        return Settings.settingsBuilder()
                // Our indexes are small and one shard, no replicas puts the
                // least possible burden on Elasticsearch
                .put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 1)
                .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 0)
                // Sacrifice durability for performance: in the event of power
                // failure we can lose the last 5 seconds of changes, but it's
                // much faster
                .put(SETTING_TRANSLOG_DURABILITY, ASYNC)
                // We need to allow fields not mentioned in the mappings to
                // pick up default mappings and be used in queries
                .put(SETTING_MAPPER_DYNAMIC, true)
                // By default "analyzed" fields won't be tokenised
                .put(SETTING_DEFAULT_ANALYZER_TYPE, KEYWORD);
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
                // Remove the Kibana/Logstash '@timestamp' entry as stored in Elasticsearch,
                // and replace using the API 'timestamp' key.
                Object timestamp = modelSizeStatsResponse.getSource().remove(ModelSizeStats.ES_TIMESTAMP);
                modelSizeStatsResponse.getSource().put(ModelSizeStats.TIMESTAMP, timestamp);

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
        SortBuilder sb = new FieldSortBuilder(ElasticsearchPersister.JOB_ID_NAME)
                                .unmappedType("string")
                                .order(SortOrder.ASC);

        LOGGER.trace("ES API CALL: search all of type " + JobDetails.TYPE +
                " from all indexes sort ascending " + ElasticsearchPersister.JOB_ID_NAME +
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
                // Remove the Kibana/Logstash '@timestamp' entry as stored in Elasticsearch,
                // and replace using the API 'timestamp' key.
                Object timestamp = modelSizeStatsResponse.getSource().remove(ModelSizeStats.ES_TIMESTAMP);
                modelSizeStatsResponse.getSource().put(ModelSizeStats.TIMESTAMP, timestamp);

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
        Collection<String> termFields = (job.getAnalysisConfig() != null) ? job.getAnalysisConfig().termFields() : null;
        Collection<String> influencers = (job.getAnalysisConfig() != null) ? job.getAnalysisConfig().getInfluencers() : null;
        try
        {
            XContentBuilder jobMapping = ElasticsearchMappings.jobMapping();
            XContentBuilder bucketMapping = ElasticsearchMappings.bucketMapping();
            XContentBuilder bucketInfluencerMapping = ElasticsearchMappings.bucketInfluencerMapping();
            XContentBuilder categorizerStateMapping = ElasticsearchMappings.categorizerStateMapping();
            XContentBuilder categoryDefinitionMapping = ElasticsearchMappings.categoryDefinitionMapping();
            XContentBuilder recordMapping = ElasticsearchMappings.recordMapping(termFields);
            XContentBuilder quantilesMapping = ElasticsearchMappings.quantilesMapping();
            XContentBuilder modelStateMapping = ElasticsearchMappings.modelStateMapping();
            XContentBuilder modelSnapshotMapping = ElasticsearchMappings.modelSnapshotMapping();
            XContentBuilder usageMapping = ElasticsearchMappings.usageMapping();
            XContentBuilder modelSizeStatsMapping = ElasticsearchMappings.modelSizeStatsMapping();
            XContentBuilder influencerMapping = ElasticsearchMappings.influencerMapping(influencers);
            XContentBuilder modelDebugMapping = ElasticsearchMappings.modelDebugOutputMapping(termFields);

            ElasticsearchJobId elasticJobId = new ElasticsearchJobId(job.getId());

            LOGGER.trace("ES API CALL: create index " + job.getId());
            m_Client.admin().indices()
                    .prepareCreate(elasticJobId.getIndex())
                    .setSettings(prelertIndexSettings())
                    .addMapping(JobDetails.TYPE, jobMapping)
                    .addMapping(Bucket.TYPE, bucketMapping)
                    .addMapping(BucketInfluencer.TYPE, bucketInfluencerMapping)
                    .addMapping(CategorizerState.TYPE, categorizerStateMapping)
                    .addMapping(CategoryDefinition.TYPE, categoryDefinitionMapping)
                    .addMapping(AnomalyRecord.TYPE, recordMapping)
                    .addMapping(Quantiles.TYPE, quantilesMapping)
                    .addMapping(ModelState.TYPE, modelStateMapping)
                    .addMapping(ModelSnapshot.TYPE, modelSnapshotMapping)
                    .addMapping(Usage.TYPE, usageMapping)
                    .addMapping(ModelSizeStats.TYPE, modelSizeStatsMapping)
                    .addMapping(Influencer.TYPE, influencerMapping)
                    .addMapping(ModelDebugOutput.TYPE, modelDebugMapping)
                    .get();
            LOGGER.trace("ES API CALL: wait for yellow status " + elasticJobId.getId());
            m_Client.admin().cluster().prepareHealth(elasticJobId.getIndex())
                    .setWaitForYellowStatus().execute().actionGet();

            if (job.getModelSizeStats() != null)
            {
                LOGGER.warn("Initial job model size stats non-null on job creation - removed them");
                job.setModelSizeStats(null);
            }
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

        LOGGER.trace("ES API CALL: update ID " + elasticJobId.getId() + " type " + JobDetails.TYPE +
                " in index " + elasticJobId.getIndex() + " using map of new values");
        try
        {
            m_Client.prepareUpdate(elasticJobId.getIndex(), JobDetails.TYPE, elasticJobId.getId())
                                .setDoc(updates)
                                .setRetryOnConflict(UPDATE_JOB_RETRY_COUNT)
                                .get();
        }
        catch (VersionConflictEngineException e)
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
        catch (InterruptedException | ExecutionException e)
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
        SortBuilder sb = new FieldSortBuilder(ElasticsearchMappings.ES_TIMESTAMP)
                    .order(SortOrder.ASC);

        SearchResponse searchResponse;
        try
        {
            LOGGER.trace("ES API CALL: search all of type " + Bucket.TYPE +
                    " from index " + jobId.getIndex() + " sort ascending " + ElasticsearchMappings.ES_TIMESTAMP +
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
            bucket.setId(hit.getId());

            if (expand && bucket.getRecordCount() > 0)
            {
                expandBucket(jobId.getId(), includeInterim, bucket);
            }

            results.add(bucket);
        }

        return new QueryPage<>(results, searchResponse.getHits().getTotalHits());
    }


    @Override
    public Optional<Bucket> bucket(String jobId, long timestampMillis, boolean expand,
            boolean includeInterim) throws UnknownJobException
    {
        ElasticsearchJobId elasticJobId = new ElasticsearchJobId(jobId);
        SearchHits hits;

        try
        {
            LOGGER.trace("ES API CALL: get Bucket with timestamp " + timestampMillis +
                    " from index " + elasticJobId.getIndex());
            QueryBuilder qb = QueryBuilders.matchQuery(ElasticsearchMappings.ES_TIMESTAMP,
                    new Date(timestampMillis));

            SearchResponse searchResponse = m_Client.prepareSearch(elasticJobId.getIndex())
                    .setTypes(Bucket.TYPE)
                    .setQuery(qb)
                    .addSort(SortBuilders.fieldSort(ElasticsearchMappings.ES_DOC))
                    .get();
            hits = searchResponse.getHits();
        }
        catch (IndexNotFoundException e)
        {
            throw new UnknownJobException(elasticJobId.getId());
        }

        Optional<Bucket> doc = Optional.<Bucket>empty();
        if (hits.getTotalHits() == 1L)
        {
            SearchHit hit = hits.getAt(0);
            // Remove the Kibana/Logstash '@timestamp' entry as stored in Elasticsearch,
            // and replace using the API 'timestamp' key.
            Object ts = hit.getSource().remove(ElasticsearchMappings.ES_TIMESTAMP);
            hit.getSource().put(Bucket.TIMESTAMP, ts);

            Bucket bucket = m_ObjectMapper.convertValue(hit.getSource(), Bucket.class);
            bucket.setId(hit.getId());
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
            sb = new FieldSortBuilder(esSortField(sortField))
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
            sb = new FieldSortBuilder(esSortField(sortField))
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
                .addSort(sb == null ? SortBuilders.fieldSort(ElasticsearchMappings.ES_DOC) : sb)
                .addField(ElasticsearchMappings.PARENT)   // include the parent id
                .setFetchSource(true);  // the field option turns off source so request it explicitly


        if (sb != null)
        {
            searchBuilder.addSort(sb);
        }

        for (String sortField : secondarySort)
        {
            searchBuilder.addSort(esSortField(sortField), descending ? SortOrder.DESC : SortOrder.ASC);
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
            record.setParent(hit.field(ElasticsearchMappings.PARENT).getValue().toString());

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

        SortBuilder sb = sortField == null ? SortBuilders.fieldSort(ElasticsearchMappings.ES_DOC)
                : new FieldSortBuilder(esSortField(sortField))
                        .order(sortDescending ? SortOrder.DESC : SortOrder.ASC);
        searchRequestBuilder.addSort(sb);

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
            influencer.setId(hit.getId());

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

    @Override
    public BatchedResultsIterator<ModelSnapshot> newBatchedModelSnapshotIterator(String jobId)
    {
        return new ElasticsearchBatchedModelSnapshotIterator(m_Client, jobId, m_ObjectMapper);
    }

    /**
     * Always returns true
     */
    @Override
    public boolean savePrelertInfo(String infoDoc)
    {
        createInfoIndex();

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
            return createQuantiles(jobId, response);
        }
        catch (IndexNotFoundException e)
        {
            LOGGER.error("Missing index when getting quantiles", e);
            throw new UnknownJobException(jobId);
        }
    }

    @Override
    public QueryPage<ModelSnapshot> modelSnapshots(String jobId,
            int skip, int take)
    throws UnknownJobException
    {
        return modelSnapshots(jobId, skip, take, 0, 0, null, null, null);
    }

    @Override
    public QueryPage<ModelSnapshot> modelSnapshots(String jobId,
            int skip, int take, long startEpochMs, long endEpochMs,
            String sortField, String snapshotId, String description)
    throws UnknownJobException
    {
        boolean haveId = (snapshotId != null && !snapshotId.isEmpty());
        boolean haveDescription = (description != null && !description.isEmpty());
        ResultsFilterBuilder fb;
        if (haveId || haveDescription)
        {
            QueryBuilder query;
            if (haveId && haveDescription)
            {
                query = QueryBuilders.boolQuery()
                        .must(QueryBuilders.termQuery(ModelSnapshot.SNAPSHOT_ID, snapshotId))
                        .must(QueryBuilders.termQuery(ModelSnapshot.DESCRIPTION, description));
            }
            else if (haveId)
            {
                query = QueryBuilders.termQuery(ModelSnapshot.SNAPSHOT_ID, snapshotId);
            }
            else
            {
                query = QueryBuilders.termQuery(ModelSnapshot.DESCRIPTION, description);
            }

            fb = new ResultsFilterBuilder(query);
        }
        else
        {
            fb = new ResultsFilterBuilder();
        }

        return modelSnapshots(new ElasticsearchJobId(jobId), skip, take,
                (sortField == null || sortField.isEmpty()) ? ModelSnapshot.RESTORE_PRIORITY : sortField,
                fb.timeRange(ElasticsearchMappings.ES_TIMESTAMP, startEpochMs, endEpochMs).build());
    }

    private QueryPage<ModelSnapshot> modelSnapshots(ElasticsearchJobId jobId,
            int skip, int take, String sortField, QueryBuilder fb) throws UnknownJobException
    {
        SortBuilder sb = new FieldSortBuilder(esSortField(sortField)).order(SortOrder.DESC);

        SearchResponse searchResponse;
        try
        {
            LOGGER.trace("ES API CALL: search all of type " + ModelSnapshot.TYPE +
                    " from index " + jobId.getIndex() + " sort ascending " + sortField +
                    " with filter after sort skip " + skip + " take " + take);
            searchResponse = m_Client.prepareSearch(jobId.getIndex())
                                        .setTypes(ModelSnapshot.TYPE)
                                        .addSort(sb)
                                        .setPostFilter(fb)
                                        .setFrom(skip).setSize(take)
                                        .get();
        }
        catch (IndexNotFoundException e)
        {
            throw new UnknownJobException(jobId.getId());
        }

        List<ModelSnapshot> results = new ArrayList<>();

        for (SearchHit hit : searchResponse.getHits().getHits())
        {
            // Remove the Kibana/Logstash '@timestamp' entry as stored in Elasticsearch,
            // and replace using the API 'timestamp' key.
            Object timestamp = hit.getSource().remove(ElasticsearchMappings.ES_TIMESTAMP);
            hit.getSource().put(ModelSnapshot.TIMESTAMP, timestamp);

            Object o = hit.getSource().get(ModelSizeStats.TYPE);
            if (o instanceof Map)
            {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>)o;
                Object ts = map.remove(ModelSizeStats.ES_TIMESTAMP);
                map.put(ModelSizeStats.TIMESTAMP, ts);
            }

            ModelSnapshot modelSnapshot = m_ObjectMapper.convertValue(hit.getSource(), ModelSnapshot.class);
            results.add(modelSnapshot);
        }

        return new QueryPage<>(results, searchResponse.getHits().getTotalHits());
    }

    @Override
    public void updateModelSnapshot(String jobId, ModelSnapshot modelSnapshot,
            boolean restoreModelSizeStats) throws UnknownJobException
    {
        // For Elasticsearch the update can be done in exactly the same way as
        // the original persist
        ElasticsearchPersister persister = new ElasticsearchPersister(jobId, m_Client);
        persister.persistModelSnapshot(modelSnapshot);

        if (restoreModelSizeStats && modelSnapshot.getModelSizeStats() != null)
        {
            persister.persistModelSizeStats(modelSnapshot.getModelSizeStats());
        }

        // Commit so that when the REST API call that triggered the update
        // returns the updated document is searchable
        persister.commitWrites();
    }

    @Override
    public ModelSnapshot deleteModelSnapshot(String jobId, String snapshotId)
            throws UnknownJobException, NoSuchModelSnapshotException
    {
        List<ModelSnapshot> deleteCandidates = modelSnapshots(jobId, 0, 1,
                    0, 0, null, snapshotId, null).queryResults();
        if (deleteCandidates == null || deleteCandidates.isEmpty())
        {
            throw new NoSuchModelSnapshotException(jobId);
        }

        ModelSnapshot modelSnapshot = deleteCandidates.get(0);

        ElasticsearchBulkDeleter deleter = new ElasticsearchBulkDeleter(m_Client, jobId);
        deleter.deleteModelSnapshot(modelSnapshot);
        deleter.commit();

        return modelSnapshot;
    }

    private Quantiles createQuantiles(String jobId, GetResponse response)
    {
        Quantiles quantiles = m_ObjectMapper.convertValue(response.getSource(), Quantiles.class);
        if (quantiles.getQuantileState() == null)
        {
            LOGGER.error("Inconsistency - no " + Quantiles.QUANTILE_STATE
                    + " field in quantiles for job " + jobId);
        }
        return quantiles;
    }

    @Override
    public void refreshIndex(String jobId)
    {
        String indexName = new ElasticsearchJobId(jobId).getIndex();
        // Flush should empty the translog into Lucene
        LOGGER.trace("ES API CALL: flush index " + indexName);
        m_Client.admin().indices().flush(new FlushRequest(indexName)).actionGet();
        // Refresh should wait for Lucene to make the data searchable
        LOGGER.trace("ES API CALL: refresh index " + indexName);
        m_Client.admin().indices().refresh(new RefreshRequest(indexName)).actionGet();
    }

    @Override
    public boolean updateDetectorDescription(String jobId, int detectorIndex, String newDescription)
            throws UnknownJobException
    {
        LOGGER.trace("ES API CALL: update detector description for job " + jobId + ", detector at index "
                + detectorIndex + " by running Groovy script update-detector-description with params newDescription="
                + newDescription);

        ElasticsearchJobId esJobId = new ElasticsearchJobId(jobId);

        try
        {
            m_Client.prepareUpdate(esJobId.getIndex(), JobDetails.TYPE, esJobId.getId())
                            .setScript(ElasticsearchScripts.newUpdateDetectorDescription(
                                    detectorIndex, newDescription))
                            .setRetryOnConflict(3).get();
        }
        catch (IndexNotFoundException e)
        {
            throw new UnknownJobException(jobId);
        }

        return true;
    }

    @Override
    public boolean updateSchedulerState(String jobId, SchedulerState schedulerState)
            throws UnknownJobException
    {
        LOGGER.trace("ES API CALL: update scheduler state for job " + jobId);

        ElasticsearchJobId esJobId = new ElasticsearchJobId(jobId);

        try
        {
            m_Client.prepareIndex(esJobId.getIndex(), SchedulerState.TYPE, SchedulerState.TYPE)
                    .setSource(jsonBuilder()
                            .startObject()
                                .field(SchedulerState.START_TIME_MILLIS, schedulerState.getStartTimeMillis())
                                .field(SchedulerState.END_TIME_MILLIS, schedulerState.getEndTimeMillis())
                            .endObject())
                    .execute().actionGet();
        }
        catch (IndexNotFoundException e)
        {
            throw new UnknownJobException(jobId);
        }
        catch (IOException e)
        {
            LOGGER.error("Error while updating schedulerState", e);
        }

        return true;
    }

    @Override
    public Optional<SchedulerState> getSchedulerState(String jobId)
    {
        Optional<SchedulerState> result = Optional.empty();
        ElasticsearchJobId esJobId = new ElasticsearchJobId(jobId);
        GetResponse response = null;
        try
        {
            response = m_Client.prepareGet(esJobId.getIndex(), SchedulerState.TYPE, SchedulerState.TYPE).get();
        }
        catch (IndexNotFoundException e)
        {
            LOGGER.warn("No schedulerState could be retrieved for job: " + jobId, e);
        }

        if (response != null && response.isExists())
        {
            SchedulerState schedulerState = m_ObjectMapper.convertValue(response.getSource(),
                    SchedulerState.class);
            result = Optional.of(schedulerState);
        }
        return result;
    }

    @Override
    public Auditor audit(String jobId)
    {
        return new ElasticsearchAuditor(m_Client, PRELERT_INFO_INDEX, jobId);
    }

    private String esSortField(String sortField)
    {
        return sortField.equals(Bucket.TIMESTAMP) ? ElasticsearchMappings.ES_TIMESTAMP : sortField;
    }
}
