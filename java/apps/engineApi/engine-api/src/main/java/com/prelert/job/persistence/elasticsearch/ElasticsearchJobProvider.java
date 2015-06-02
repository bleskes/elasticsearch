/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2015     *
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

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import org.elasticsearch.index.get.GetField;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.RangeFilterBuilder;
import org.elasticsearch.index.query.TermFilterBuilder;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.node.Node;
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
import com.prelert.job.exceptions.JobIdAlreadyExistsException;
import com.prelert.job.exceptions.UnknownJobException;
import com.prelert.job.persistence.JobProvider;
import com.prelert.job.quantiles.Quantiles;
import com.prelert.job.usage.Usage;
import com.prelert.rs.data.AnomalyRecord;
import com.prelert.rs.data.Bucket;
import com.prelert.rs.data.CategoryDefinition;
import com.prelert.rs.data.Detector;
import com.prelert.rs.data.ErrorCode;
import com.prelert.rs.data.Pagination;
import com.prelert.rs.data.SingleDocument;

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


    private final Node m_Node;
    private final Client m_Client;

    private final ObjectMapper m_ObjectMapper;

    public static ElasticsearchJobProvider create(String elasticSearchClusterName, String portRange)
    {
        Node node = nodeBuilder()
                .settings(buildSettings(portRange))
                .client(true)
                .clusterName(elasticSearchClusterName).node();
        return new ElasticsearchJobProvider(node, node.client());
    }

    public ElasticsearchJobProvider(Node node, Client client)
    {
        m_Node = Objects.requireNonNull(node);
        m_Client = Objects.requireNonNull(client);

        m_ObjectMapper = new ObjectMapper();
        m_ObjectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        m_ObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        createUsageMeteringIndex();

        LOGGER.info("Connecting to Elasticsearch cluster '" + m_Node.settings().get("cluster.name")
                + "'");
    }

    /**
     * Elasticsearch settings that instruct the node not to accept HTTP, not to
     * attempt multicast discovery and to only look for another node to connect
     * to on the local machine.
     */
    private static Settings buildSettings(String portRange)
    {
        // Multicast discovery is expected to be disabled on the Elasticsearch
        // data node, so disable it for this embedded node too and tell it to
        // expect the data node to be on the same machine
        Builder builder = ImmutableSettings.settingsBuilder()
                .put("http.enabled", "false")
                .put("discovery.zen.ping.multicast.enabled", "false")
                .put("discovery.zen.ping.unicast.hosts", "localhost");

        if (portRange != null && portRange.isEmpty() == false)
        {
            LOGGER.info("Using TCP port range " + portRange + " to connect to Elasticsearch");
            builder.put("transport.tcp.port", portRange);
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
            boolean indexExists = m_Client.admin().indices()
                    .exists(new IndicesExistsRequest(PRELERT_USAGE_INDEX))
                    .get().isExists();

            if (indexExists == false)
            {
                LOGGER.info("Creating the internal '" + PRELERT_USAGE_INDEX + "' index");

                XContentBuilder usageMapping = ElasticsearchMappings.usageMapping();

                m_Client.admin().indices().prepareCreate(PRELERT_USAGE_INDEX)
                                .addMapping(Usage.TYPE, usageMapping)
                                .get();
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
    public boolean jobExists(String jobId) throws UnknownJobException
    {
        try
        {
            GetResponse response = m_Client.prepareGet(jobId, JobDetails.TYPE, jobId)
                            .setFetchSource(false)
                            .setFields()
                            .get();

            if (response.isExists() == false)
            {
                String msg = "No job document with id " + jobId;
                LOGGER.warn(msg);
                throw new UnknownJobException(jobId, msg,
                        ErrorCode.MISSING_JOB_ERROR);
            }
        }
        catch (IndexMissingException e)
        {
            // the job does not exist
            String msg = "Missing Index no job with id " + jobId;
            LOGGER.warn(msg);
            throw new UnknownJobException(jobId, "No known job with id '" + jobId + "'",
                    ErrorCode.MISSING_JOB_ERROR);
        }

        return true;
    }


    @Override
    public boolean jobIdIsUnique(String jobId) throws JobIdAlreadyExistsException
    {
        if (indexExists(jobId))
        {
            throw new JobIdAlreadyExistsException(jobId);
        }

        return true;
    }

    private boolean indexExists(String jobId)
    {
        IndicesExistsResponse res =
                m_Client.admin().indices().exists(new IndicesExistsRequest(jobId)).actionGet();

        return res.isExists();
    }


    @Override
    public JobDetails getJobDetails(String jobId) throws UnknownJobException
    {
        try
        {
            GetResponse response = m_Client.prepareGet(jobId, JobDetails.TYPE, jobId).get();
            if (!response.isExists())
            {
                String msg = "No details for job with id " + jobId;
                LOGGER.warn(msg);
                throw new UnknownJobException(jobId, msg,
                        ErrorCode.MISSING_JOB_ERROR);
            }
            JobDetails details = m_ObjectMapper.convertValue(response.getSource(), JobDetails.class);

            // Pull out the modelSizeStats document, and add this to the JobDetails
            GetResponse modelSizeStatsResponse = m_Client.prepareGet(
                jobId, ModelSizeStats.TYPE, ModelSizeStats.TYPE).get();
            if (!modelSizeStatsResponse.isExists())
            {
                String msg = "No model size stats for job with id "
                    + jobId + " " + ModelSizeStats.TYPE;
                LOGGER.warn(msg);
            }
            else
            {
                ModelSizeStats modelSizeStats = m_ObjectMapper.convertValue(
                    modelSizeStatsResponse.getSource(), ModelSizeStats.class);
                details.setModelSizeStats(modelSizeStats);
            }
            return details;
        }
        catch (IndexMissingException e)
        {
            // the job does not exist
            String msg = "Missing Index no job with id " + jobId;
            LOGGER.warn(msg);
            throw new UnknownJobException(jobId, "No known job with id '" + jobId + "'",
                    ErrorCode.MISSING_JOB_ERROR);
        }
    }

    @Override
    public Pagination<JobDetails> getJobs(int skip, int take)
    {
        FilterBuilder fb = FilterBuilders.matchAllFilter();
        SortBuilder sb = new FieldSortBuilder(JobDetails.ID)
                                .unmappedType("string")
                                .order(SortOrder.ASC);

        SearchResponse response = m_Client.prepareSearch("_all")
                .setTypes(JobDetails.TYPE)
                .setPostFilter(fb)
                .setFrom(skip).setSize(take)
                .addSort(sb)
                .get();

        List<JobDetails> jobs = new ArrayList<>();
        for (SearchHit hit : response.getHits().getHits())
        {
            JobDetails job = m_ObjectMapper.convertValue(hit.getSource(), JobDetails.class);

            // Pull out the modelSizeStats document, and add this to the JobDetails
            GetResponse modelSizeStatsResponse = m_Client.prepareGet(
                job.getId(), ModelSizeStats.TYPE, ModelSizeStats.TYPE).get();

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

        Pagination<JobDetails> page = new Pagination<>();
        page.setDocuments(jobs);
        page.setHitCount(response.getHits().getTotalHits());
        page.setSkip(skip);
        page.setTake(take);

        return page;
    }

    /**
     * Create the Elasticsearch index and the mappings
     * @throws
     */
    @Override
    public boolean createJob(JobDetails job)
    throws JobIdAlreadyExistsException
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

            m_Client.admin().indices()
                    .prepareCreate(job.getId())
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
                    .get();
            m_Client.admin().cluster().prepareHealth(job.getId()).setWaitForYellowStatus().execute().actionGet();


            String json = m_ObjectMapper.writeValueAsString(job);

            m_Client.prepareIndex(job.getId(), JobDetails.TYPE, job.getId())
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

    /**
     * Returns null if the field cannot be found or converted to
     * type V
     */
    @SuppressWarnings("unchecked")
    @Override
    public <V> V getField(String jobId, String fieldName)
    {
        try
        {
            GetResponse response = m_Client
                    .prepareGet(jobId, JobDetails.TYPE, jobId)
                    .setFields(fieldName)
                    .get();
            try
            {
                GetField f = response.getField(fieldName);
                return (f != null) ? (V)f.getValue() : null;
            }
            catch (ClassCastException e)
            {
                return null;
            }
        }
        catch (IndexMissingException e)
        {
            // the job does not exist
            String msg = "Missing Index no job with id " + jobId;
            LOGGER.error(msg);
        }

        return null;
    }


    @Override
    public boolean updateJob(String jobId, Map<String, Object> updates)
    throws UnknownJobException
    {
        if (jobExists(jobId))
        {
            int retryCount = 3;
            while (--retryCount >= 0)
            {
                try
                {
                    m_Client.prepareUpdate(jobId, JobDetails.TYPE, jobId)
                                        .setDoc(updates)
                                        .get();

                    break;
                }
                catch (VersionConflictEngineException e)
                {
                    LOGGER.warn("Conflict updating job document");
                }
            }

            if (retryCount <= 0)
            {
                LOGGER.warn("Unable to update conflicted job document " + jobId +
                        ". Updates = " + updates);
                return false;
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean setJobStatus(String jobId, JobStatus status)
    throws UnknownJobException
    {
        Map<String, Object> update = new HashMap<>();
        update.put(JobDetails.STATUS, status);
        return this.updateJob(jobId, update);

    }

    @Override
    public boolean setJobFinishedTimeandStatus(String jobId, Date time,
            JobStatus status)
    throws UnknownJobException
    {
        Map<String, Object> update = new HashMap<>();
        update.put(JobDetails.FINISHED_TIME, time);
        update.put(JobDetails.STATUS, status);
        return this.updateJob(jobId, update);
    }


    @Override
    public boolean deleteJob(String jobId) throws UnknownJobException
    {
        try
        {
            if (indexExists(jobId))
            {
                DeleteIndexResponse response = m_Client.admin()
                        .indices().delete(new DeleteIndexRequest(jobId)).get();

                return response.isAcknowledged();
            }
            else
            {
                throw new UnknownJobException(jobId);
            }
        }
        catch (InterruptedException|ExecutionException e)
        {
            if (e.getCause() instanceof IndexMissingException)
            {
                String msg = String.format("Cannot delete job - no index with id '%s' in the database", jobId);
                LOGGER.warn(msg);
                throw new UnknownJobException(jobId, msg,
                        ErrorCode.MISSING_JOB_ERROR);
            }
            else
            {
                String msg = "Error deleting index " + jobId;
                LOGGER.error(msg);
                throw new UnknownJobException(jobId, msg,
                        ErrorCode.DATA_STORE_ERROR, e.getCause());
            }
        }
    }

    /* Results */
    @Override
    public Pagination<Bucket> buckets(String jobId,
            boolean expand, boolean includeInterim, int skip, int take,
            double anomalyScoreThreshold, double normalizedProbabilityThreshold)
    throws UnknownJobException
    {
        return buckets(jobId, expand, includeInterim, skip, take, 0, 0, anomalyScoreThreshold,
                normalizedProbabilityThreshold);
    }

    @Override
    public Pagination<Bucket> buckets(String jobId, boolean expand,
            boolean includeInterim, int skip, int take, long startEpochMs, long endEpochMs,
            double anomalyScoreThreshold, double normalizedProbabilityThreshold)
    throws UnknownJobException
    {
        FilterBuilder fb = null;

        if (startEpochMs > 0 || endEpochMs > 0)
        {
            // HACK for the timestamps being @timestamp in the database
            RangeFilterBuilder timeRange = FilterBuilders.rangeFilter(ElasticsearchMappings.ES_TIMESTAMP);

            if (startEpochMs > 0)
            {
                timeRange.gte(startEpochMs);
            }
            if (endEpochMs > 0)
            {
                timeRange.lt(endEpochMs);
            }

            fb = timeRange;
        }

        if (anomalyScoreThreshold > 0.0)
        {
            RangeFilterBuilder scoreFilter = FilterBuilders.rangeFilter(Bucket.ANOMALY_SCORE);
            scoreFilter.gte(anomalyScoreThreshold);

            fb = (fb == null) ? scoreFilter : FilterBuilders.andFilter(scoreFilter, fb);
        }

        if (normalizedProbabilityThreshold > 0.0)
        {
            RangeFilterBuilder scoreFilter = FilterBuilders.rangeFilter(Bucket.MAX_NORMALIZED_PROBABILITY);
            scoreFilter.gte(normalizedProbabilityThreshold);

            fb = (fb == null) ? scoreFilter : FilterBuilders.andFilter(scoreFilter, fb);
        }

        fb = andWithInterimFilter(includeInterim, Bucket.IS_INTERIM, fb);

        if (fb == null)
        {
            fb = FilterBuilders.matchAllFilter();
        }

        return buckets(jobId, expand, includeInterim, skip, take, fb);
    }

    private Pagination<Bucket> buckets(String jobId,
            boolean expand, boolean includeInterim, int skip, int take,
            FilterBuilder fb)
    throws UnknownJobException
    {
        SortBuilder sb = new FieldSortBuilder(Bucket.ID)
                    .unmappedType("string")
                    .order(SortOrder.ASC);

        SearchResponse searchResponse;
        try
        {
            searchResponse = m_Client.prepareSearch(jobId)
                                        .setTypes(Bucket.TYPE)
                                        .addSort(sb)
                                        .setPostFilter(fb)
                                        .setFrom(skip).setSize(take)
                                        .get();
        }
        catch (IndexMissingException e)
        {
            throw new UnknownJobException(jobId);
        }

        List<Bucket> results = new ArrayList<>();


        for (SearchHit hit : searchResponse.getHits().getHits())
        {
            // Remove the Kibana/Logstash '@timestamp' entry as stored in Elasticsearch,
            // and replace using the API 'timestamp' key.
            Object timestamp = hit.getSource().remove(ElasticsearchMappings.ES_TIMESTAMP);
            hit.getSource().put(Bucket.TIMESTAMP, timestamp);

            Bucket bucket = m_ObjectMapper.convertValue(hit.getSource(), Bucket.class);

            if (expand)
            {
                int rskip = 0;
                int rtake = 500;
                Pagination<AnomalyRecord> page = this.bucketRecords(
                        jobId, hit.getId(), rskip, rtake, includeInterim,
                        AnomalyRecord.PROBABILITY, false);

                if (page.getHitCount() > 0)
                {
                    bucket.setRecords(page.getDocuments());
                }

                while (page.getHitCount() > rskip + rtake)
                {
                    rskip += rtake;
                    page = this.bucketRecords(
                            jobId, hit.getId(), rskip, rtake, includeInterim,
                            AnomalyRecord.PROBABILITY, false);
                    bucket.getRecords().addAll(page.getDocuments());
                }
            }

            results.add(bucket);
        }


        Pagination<Bucket> page = new Pagination<>();
        page.setDocuments(results);
        page.setHitCount(searchResponse.getHits().getTotalHits());
        page.setSkip(skip);
        page.setTake(take);

        return page;
    }

    @Override
    public SingleDocument<Bucket> bucket(String jobId,
            String bucketId, boolean expand, boolean includeInterim)
    throws UnknownJobException
    {
        GetResponse response;

        try
        {
            response = m_Client.prepareGet(jobId, Bucket.TYPE, bucketId).get();
        }
        catch (IndexMissingException e)
        {
            throw new UnknownJobException(jobId);
        }

        SingleDocument<Bucket> doc = new SingleDocument<>();
        doc.setType(Bucket.TYPE);
        doc.setDocumentId(bucketId);
        if (response.isExists())
        {
            // Remove the Kibana/Logstash '@timestamp' entry as stored in Elasticsearch,
            // and replace using the API 'timestamp' key.
            Object timestamp = response.getSource().remove(ElasticsearchMappings.ES_TIMESTAMP);
            response.getSource().put(Bucket.TIMESTAMP, timestamp);

            Bucket bucket = m_ObjectMapper.convertValue(response.getSource(), Bucket.class);
            if (includeInterim ||
                bucket.isInterim() == null ||
                bucket.isInterim() == false)
            {
                if (expand)
                {
                    int rskip = 0;
                    int rtake = 500;
                    Pagination<AnomalyRecord> page = this.bucketRecords(
                            jobId, bucketId, rskip, rtake, includeInterim,
                            AnomalyRecord.PROBABILITY, false);
                    bucket.setRecords(page.getDocuments());

                    while (page.getHitCount() > rskip + rtake)
                    {
                        rskip += rtake;
                        page = this.bucketRecords(
                                jobId, bucketId, rskip, rtake, includeInterim,
                                AnomalyRecord.PROBABILITY, false);
                        bucket.getRecords().addAll(page.getDocuments());
                    }
                }

                doc.setDocument(bucket);
            }
        }

        return doc;
    }


    @Override
    public Pagination<AnomalyRecord> bucketRecords(String jobId,
            String bucketId, int skip, int take, boolean includeInterim, String sortField, boolean descending)
    throws UnknownJobException
    {
        FilterBuilder recordFilter = FilterBuilders.hasParentFilter(Bucket.TYPE,
                                FilterBuilders.termFilter(Bucket.ID, bucketId));

        recordFilter = andWithInterimFilter(includeInterim, AnomalyRecord.IS_INTERIM, recordFilter);

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

        return records(jobId, skip, take, recordFilter, sb, secondarySort,
                descending);
    }


    @Override
    public Pagination<CategoryDefinition> categoryDefinitions(String jobId, int skip, int take)
            throws UnknownJobException
    {
        SearchRequestBuilder searchBuilder = m_Client.prepareSearch(jobId)
                .setTypes(CategoryDefinition.TYPE)
                .setFrom(skip).setSize(take)
                .addSort(new FieldSortBuilder(CategoryDefinition.CATEGORY_ID).order(SortOrder.ASC));

        SearchResponse searchResponse;
        try
        {
            searchResponse = searchBuilder.get();
        }
        catch (IndexMissingException e)
        {
            throw new UnknownJobException(jobId);
        }

        List<CategoryDefinition> results = Arrays.stream(searchResponse.getHits().getHits())
                .map(hit -> m_ObjectMapper.convertValue(hit.getSource(), CategoryDefinition.class))
                .collect(Collectors.toList());

        Pagination<CategoryDefinition> page = new Pagination<>();
        page.setDocuments(results);
        page.setHitCount(searchResponse.getHits().getTotalHits());
        page.setSkip(skip);
        page.setTake(take);

        return page;
    }


    @Override
    public SingleDocument<CategoryDefinition> categoryDefinition(String jobId, String categoryId)
            throws UnknownJobException
    {
        GetResponse response;

        try
        {
            response = m_Client.prepareGet(jobId, CategoryDefinition.TYPE, categoryId).get();
        }
        catch (IndexMissingException e)
        {
            throw new UnknownJobException(jobId);
        }

        SingleDocument<CategoryDefinition> doc = new SingleDocument<>();
        doc.setType(CategoryDefinition.TYPE);
        doc.setDocumentId(categoryId);
        if (response.isExists())
        {
            doc.setDocument(m_ObjectMapper.convertValue(response.getSource(), CategoryDefinition.class));
        }
        return doc;
    }

    @Override
    public Pagination<AnomalyRecord> records(String jobId,
            int skip, int take, boolean includeInterim, String sortField, boolean descending,
            double anomalyScoreThreshold, double normalizedProbabilityThreshold)
    throws UnknownJobException
    {
        return records(jobId, skip, take, 0, 0, includeInterim, sortField, descending,
                anomalyScoreThreshold, normalizedProbabilityThreshold);
    }

    @Override
    public Pagination<AnomalyRecord> records(String jobId,
            int skip, int take, long startEpochMs, long endEpochMs,
            boolean includeInterim, String sortField, boolean descending,
            double anomalyScoreThreshold, double normalizedProbabilityThreshold)
    throws UnknownJobException
    {
        FilterBuilder fb = null;

        if (startEpochMs > 0 || endEpochMs > 0)
        {
            // HACK for the timestamps being @timestamp in the database
            RangeFilterBuilder rangeFilter = FilterBuilders.rangeFilter(ElasticsearchMappings.ES_TIMESTAMP);

            if (startEpochMs > 0)
            {
                rangeFilter.gte(startEpochMs);
            }
            if (endEpochMs > 0)
            {
                rangeFilter.lt(endEpochMs);
            }

            fb = FilterBuilders.hasParentFilter(Bucket.TYPE, rangeFilter);
        }

        if (anomalyScoreThreshold > 0.0)
        {
            RangeFilterBuilder scoreFilter = FilterBuilders.rangeFilter(AnomalyRecord.ANOMALY_SCORE);
            scoreFilter.gte(anomalyScoreThreshold);

            fb = (fb == null) ? scoreFilter : FilterBuilders.andFilter(scoreFilter, fb);
        }

        if (normalizedProbabilityThreshold > 0.0)
        {
            RangeFilterBuilder scoreFilter = FilterBuilders.rangeFilter(AnomalyRecord.NORMALIZED_PROBABILITY);
            scoreFilter.gte(normalizedProbabilityThreshold);

            fb = (fb == null) ? scoreFilter : FilterBuilders.andFilter(scoreFilter, fb);
        }

        fb = andWithInterimFilter(includeInterim, AnomalyRecord.IS_INTERIM, fb);

        if (fb == null)
        {
            fb = FilterBuilders.matchAllFilter();
        }

        return records(jobId, skip, take, fb, sortField, descending);
    }

    private Pagination<AnomalyRecord> records(String jobId,
            int skip, int take, FilterBuilder recordFilter,
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
    private Pagination<AnomalyRecord> records(String jobId, int skip, int take,
            FilterBuilder recordFilter, SortBuilder sb, List<String> secondarySort,
            boolean descending)
    throws UnknownJobException
    {
        SearchRequestBuilder searchBuilder = m_Client.prepareSearch(jobId)
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
            searchResponse = searchBuilder.get();
        }
        catch (IndexMissingException e)
        {
            throw new UnknownJobException(jobId);
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

        Pagination<AnomalyRecord> page = new Pagination<>();
        page.setDocuments(results);
        page.setHitCount(searchResponse.getHits().getTotalHits());
        page.setSkip(skip);
        page.setTake(take);

        return page;
    }


    /**
     * Always returns true
     */
    @Override
    public boolean savePrelertInfo(String infoDoc)
    {
        m_Client.prepareIndex(PRELERT_INFO_INDEX, PRELERT_INFO_TYPE, PRELERT_INFO_ID)
                        .setSource(infoDoc)
                        .execute().actionGet();

        return true;
    }


    @Override
    public Quantiles getQuantiles(String jobId)
    throws UnknownJobException
    {
        try
        {
            GetResponse response = m_Client.prepareGet(
                    jobId, Quantiles.TYPE, Quantiles.QUANTILES_ID).get();
            if (!response.isExists())
            {
                LOGGER.info("There are currently no quantiles for job " + jobId);
                return new Quantiles();
            }
            return checkQuantilesVersion(jobId, response) ? createQuantiles(jobId, response)
                    : new Quantiles();
        }
        catch (IndexMissingException e)
        {
            LOGGER.error("Unknown job '" + jobId + "'. Cannot read persisted state");
            throw new UnknownJobException(jobId,
                    "Cannot read persisted quantiles", ErrorCode.MISSING_JOB_ERROR);
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


    private FilterBuilder andWithInterimFilter(boolean includeInterim,
            String fieldName, FilterBuilder fb)
    {
        if (includeInterim)
        {
            // Including interim results does not stop final results being
            // shown, so including interim results means no filtering on the
            // isInterim field
            return fb;
        }

        // Implemented as "NOT isInterim == true" so that not present and null
        // are equivalent to false.  This improves backwards compatibility.
        // Also, note how for a boolean field, unlike numeric term filters, the
        // term value is supplied as a string.
        TermFilterBuilder interimFilter = FilterBuilders.termFilter(fieldName,
                Boolean.TRUE.toString());
        FilterBuilder notInterimFilter = FilterBuilders.notFilter(interimFilter);
        if (fb == null)
        {
            return notInterimFilter;
        }
        return FilterBuilders.andFilter(notInterimFilter, fb);
    }
}

