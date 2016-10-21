/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */
package org.elasticsearch.xpack.prelert.job.persistence;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
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
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.IndexNotFoundException;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.ConstantScoreQueryBuilder;
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
import org.elasticsearch.xpack.prelert.job.CategorizerState;
import org.elasticsearch.xpack.prelert.job.JobDetails;
import org.elasticsearch.xpack.prelert.job.JsonViews;
import org.elasticsearch.xpack.prelert.job.ModelSizeStats;
import org.elasticsearch.xpack.prelert.job.ModelSnapshot;
import org.elasticsearch.xpack.prelert.job.SchedulerState;
import org.elasticsearch.xpack.prelert.job.audit.AuditActivity;
import org.elasticsearch.xpack.prelert.job.audit.AuditMessage;
import org.elasticsearch.xpack.prelert.job.audit.Auditor;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.exceptions.NoSuchModelSnapshotException;
import org.elasticsearch.xpack.prelert.job.exceptions.UnknownJobException;
import org.elasticsearch.xpack.prelert.job.persistence.BucketsQueryBuilder.BucketsQuery;
import org.elasticsearch.xpack.prelert.job.persistence.InfluencersQueryBuilder.InfluencersQuery;
import org.elasticsearch.xpack.prelert.job.quantiles.Quantiles;
import org.elasticsearch.xpack.prelert.job.results.AnomalyRecord;
import org.elasticsearch.xpack.prelert.job.results.Bucket;
import org.elasticsearch.xpack.prelert.job.results.BucketInfluencer;
import org.elasticsearch.xpack.prelert.job.results.CategoryDefinition;
import org.elasticsearch.xpack.prelert.job.results.Influencer;
import org.elasticsearch.xpack.prelert.job.results.ModelDebugOutput;
import org.elasticsearch.xpack.prelert.job.results.PartitionNormalisedProb;
import org.elasticsearch.xpack.prelert.job.results.ReservedFieldNames;
import org.elasticsearch.xpack.prelert.job.usage.Usage;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class ElasticsearchJobProvider implements JobProvider
{
    private static final Logger LOGGER = Loggers.getLogger(ElasticsearchJobProvider.class);

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

    private static final List<String> SECONDARY_SORT = Arrays.asList(
            AnomalyRecord.ANOMALY_SCORE.getPreferredName(),
            AnomalyRecord.OVER_FIELD_VALUE.getPreferredName(),
            AnomalyRecord.PARTITION_FIELD_VALUE.getPreferredName(),
            AnomalyRecord.BY_FIELD_VALUE.getPreferredName(),
            AnomalyRecord.FIELD_NAME.getPreferredName(),
            AnomalyRecord.FUNCTION.getPreferredName()
            );

    private static final int UPDATE_JOB_RETRY_COUNT = 3;
    private static final int RECORDS_TAKE_PARAM = 500;
    private static final long CLUSTER_INIT_TIMEOUT_MS = 30000;


    private final Node node;
    private final Client client;
    private final int numberOfReplicas;

    private final ObjectMapper objectMapper;

    public ElasticsearchJobProvider(Node node, Client client, int numberOfReplicas) {
        this.node = node;
        this.client = Objects.requireNonNull(client);
        this.numberOfReplicas = numberOfReplicas;

        objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        // When we serialise objects with multiple views we want to choose the datastore view
        objectMapper.setConfig(objectMapper.getSerializationConfig().withView(JsonViews.DatastoreView.class));
    }

    public void initialize() {
        LOGGER.info("Connecting to Elasticsearch cluster '" + client.settings().get("cluster.name")
                + "'");

        // This call was added because if we try to connect to Elasticsearch
        // while it's doing the recovery operations it does at startup then we
        // can get weird effects like indexes being reported as not existing
        // when they do.  See EL16-182 in Jira.
        LOGGER.trace("ES API CALL: wait for yellow status on whole cluster");
        ClusterHealthResponse response = client.admin().cluster()
                .prepareHealth()
                .setWaitForYellowStatus()
                .execute().actionGet();

        // The wait call above can time out.
        // Throw an error if in cluster health is red
        if (response.getStatus() == ClusterHealthStatus.RED) {
            String msg = "Waited for the Elasticsearch status to be YELLOW but is RED after wait timeout";
            LOGGER.error(msg);
            throw new IllegalStateException(msg);
        }

        LOGGER.info("Elasticsearch cluster '" + client.settings().get("cluster.name")
                + "' now ready to use");


        createUsageMeteringIndex();
    }

    /*
     * True if the Job index is healthy
     *
     * (non-Javadoc)
     * @see org.elasticsearch.xpack.prelert.job.persistence.JobProvider#isConnected(java.lang.String)
     */
    @Override
    public boolean isConnected(String jobId)
    {
        ElasticsearchJobId elasticJobId = new ElasticsearchJobId(jobId);
        try
        {
            client.admin().cluster()
            .prepareHealth(elasticJobId.getIndex())
            .get(TimeValue.timeValueSeconds(2));
            return true;
        }
        catch (Exception e) // nodenotavailable, estimeout
        {
            LOGGER.info(e);
            return false;
        }
    }

    /**
     * Close the Elasticsearch node or client
     */
    public void shutdown()
    {
        client.close();
        LOGGER.info("Elasticsearch client shut down");
        if (node != null)
        {
            try
            {
                node.close();
                LOGGER.info("Elasticsearch node shut down");
            }
            catch (IOException e)
            {
                LOGGER.error("Failed to shut down elasticsearch node", e);
            }
        }
    }

    /**
     * If the {@value ElasticsearchJobProvider#PRELERT_USAGE_INDEX} index does
     * not exist then create it here with the usage document mapping.
     */
    private void createUsageMeteringIndex() {
        try {
            LOGGER.trace("ES API CALL: index exists? " + PRELERT_USAGE_INDEX);
            boolean indexExists = client.admin().indices()
                    .exists(new IndicesExistsRequest(PRELERT_USAGE_INDEX))
                    .get().isExists();

            if (indexExists == false) {
                LOGGER.info("Creating the internal '" + PRELERT_USAGE_INDEX + "' index");

                XContentBuilder usageMapping = ElasticsearchMappings.usageMapping();

                LOGGER.trace("ES API CALL: create index " + PRELERT_USAGE_INDEX);
                client.admin().indices().prepareCreate(PRELERT_USAGE_INDEX)
                .setSettings(prelertIndexSettings())
                .addMapping(Usage.TYPE, usageMapping)
                .get();
                LOGGER.trace("ES API CALL: wait for yellow status " + PRELERT_USAGE_INDEX);
                client.admin().cluster().prepareHealth(PRELERT_USAGE_INDEX).setWaitForYellowStatus().execute().actionGet();
            }
        } catch (InterruptedException | ExecutionException | IOException e) {
            LOGGER.warn("Error checking the usage metering index", e);
        } catch (IndexAlreadyExistsException e) {
            LOGGER.debug("Usage metering index already exists", e);
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
            boolean indexExists = client.admin().indices()
                    .exists(new IndicesExistsRequest(PRELERT_INFO_INDEX))
                    .get().isExists();

            if (indexExists == false)
            {
                LOGGER.info("Creating the internal '" + PRELERT_INFO_INDEX + "' index");

                LOGGER.trace("ES API CALL: create index " + PRELERT_INFO_INDEX);
                client.admin().indices().prepareCreate(PRELERT_INFO_INDEX)
                .setSettings(prelertIndexSettings())
                .addMapping(AuditActivity.TYPE, ElasticsearchMappings.auditActivityMapping())
                .addMapping(AuditMessage.TYPE, ElasticsearchMappings.auditMessageMapping())
                .get();
                LOGGER.trace("ES API CALL: wait for yellow status " + PRELERT_INFO_INDEX);
                client.admin().cluster().prepareHealth(PRELERT_INFO_INDEX).setWaitForYellowStatus().execute().actionGet();
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
            GetResponse response = client.prepareGet(elasticJobId.getIndex(), JobDetails.TYPE, elasticJobId.getId())
                    .setFetchSource(false)
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
                client.admin().indices().exists(new IndicesExistsRequest(jobId.getIndex())).actionGet();

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
        return Settings.builder()
                // Our indexes are small and one shard puts the
                // least possible burden on Elasticsearch
                .put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 1)
                .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, numberOfReplicas)
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
    public BatchedDocumentsIterator<JobDetails> newBatchedJobsIterator()
    {
        return new ElasticsearchBatchedJobsIterator(client, ElasticsearchJobId.INDEX_PREFIX + "*",
                objectMapper);
    }

    /**
     * Create the Elasticsearch index and the mappings
     * @throws
     */
    @Override
    public void createJob(JobDetails job, ActionListener<Boolean> listener) {
        Collection<String> termFields = (job.getAnalysisConfig() != null) ? job.getAnalysisConfig().termFields() : null;
        Collection<String> influencers = (job.getAnalysisConfig() != null) ? job.getAnalysisConfig().getInfluencers() : null;
        try {
            XContentBuilder bucketMapping = ElasticsearchMappings.bucketMapping();
            XContentBuilder bucketInfluencerMapping = ElasticsearchMappings.bucketInfluencerMapping();
            XContentBuilder categorizerStateMapping = ElasticsearchMappings.categorizerStateMapping();
            XContentBuilder categoryDefinitionMapping = ElasticsearchMappings.categoryDefinitionMapping();
            XContentBuilder recordMapping = ElasticsearchMappings.recordMapping(termFields);
            XContentBuilder quantilesMapping = ElasticsearchMappings.quantilesMapping();
            XContentBuilder modelStateMapping = ElasticsearchMappings.modelStateMapping();
            XContentBuilder modelSnapshotMapping = ElasticsearchMappings.modelSnapshotMapping();
            XContentBuilder modelSizeStatsMapping = ElasticsearchMappings.modelSizeStatsMapping();
            XContentBuilder influencerMapping = ElasticsearchMappings.influencerMapping(influencers);
            XContentBuilder modelDebugMapping = ElasticsearchMappings.modelDebugOutputMapping(termFields);
            XContentBuilder processingTimeMapping = ElasticsearchMappings.processingTimeMapping();
            XContentBuilder partitionScoreMapping = ElasticsearchMappings.bucketPartitionMaxNormalizedScores();

            ElasticsearchJobId elasticJobId = new ElasticsearchJobId(job.getId());
            LOGGER.trace("ES API CALL: create index " + job.getId());
            CreateIndexRequest createIndexRequest = new CreateIndexRequest(elasticJobId.getIndex());
            createIndexRequest.settings(prelertIndexSettings());
            createIndexRequest.mapping(Bucket.TYPE.getPreferredName(), bucketMapping);
            createIndexRequest.mapping(BucketInfluencer.TYPE.getPreferredName(), bucketInfluencerMapping);
            createIndexRequest.mapping(CategorizerState.TYPE, categorizerStateMapping);
            createIndexRequest.mapping(CategoryDefinition.TYPE.getPreferredName(), categoryDefinitionMapping);
            createIndexRequest.mapping(AnomalyRecord.TYPE.getPreferredName(), recordMapping);
            createIndexRequest.mapping(Quantiles.TYPE.getPreferredName(), quantilesMapping);
            createIndexRequest.mapping(ModelSnapshot.TYPE.getPreferredName(), modelSnapshotMapping);
            createIndexRequest.mapping(ModelSizeStats.TYPE.getPreferredName(), modelSizeStatsMapping);
            createIndexRequest.mapping(Influencer.TYPE.getPreferredName(), influencerMapping);
            createIndexRequest.mapping(ModelDebugOutput.TYPE.getPreferredName(), modelDebugMapping);
            createIndexRequest.mapping(ReservedFieldNames.BUCKET_PROCESSING_TIME_TYPE, processingTimeMapping);
            createIndexRequest.mapping(PartitionNormalisedProb.TYPE, partitionScoreMapping);

            client.admin().indices().create(createIndexRequest, new ActionListener<CreateIndexResponse>() {
                @Override
                public void onResponse(CreateIndexResponse createIndexResponse) {
                    listener.onResponse(true);
                }

                @Override
                public void onFailure(Exception e) {
                    listener.onFailure(e);
                }
            });
        } catch (Exception e) {
            listener.onFailure(e);
        }
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
            DeleteIndexResponse response = client.admin()
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
    public QueryPage<Bucket> buckets(String jobId, BucketsQuery query)
            throws ResourceNotFoundException
    {
        QueryBuilder fb = new ResultsFilterBuilder()
                .timeRange(ElasticsearchMappings.ES_TIMESTAMP, query.getEpochStart(), query.getEpochEnd())
                .score(Bucket.ANOMALY_SCORE.getPreferredName(), query.getAnomalyScoreFilter())
                .score(Bucket.MAX_NORMALIZED_PROBABILITY.getPreferredName(), query.getNormalizedProbability())
                .interim(Bucket.IS_INTERIM.getPreferredName(), query.isIncludeInterim())
                .build();

        SortBuilder sortBuilder = new FieldSortBuilder(esSortField(query.getSortField()))
                .order(query.isSortDescending() ? SortOrder.DESC : SortOrder.ASC);
        ElasticsearchJobId elasticJobId = new ElasticsearchJobId(jobId);
        QueryPage<Bucket> buckets = buckets(elasticJobId, query.isIncludeInterim(), query.getSkip(), query.getTake(), fb, sortBuilder);

        if (Strings.isNullOrEmpty(query.getPartitionValue()))
        {
            for (Bucket b : buckets.hits())
            {
                if (query.isExpand()  && b.getRecordCount() > 0)
                {
                    expandBucket(jobId, query.isIncludeInterim(), b);
                }
            }
        }
        else
        {
            List<ScoreTimestamp> scores =
                    partitionScores(elasticJobId,
                            query.getEpochStart(), query.getEpochEnd(),
                            query.getPartitionValue());

            mergePartitionScoresIntoBucket(scores, buckets.hits());

            for (Bucket b : buckets.hits())
            {
                if (query.isExpand() && b.getRecordCount() > 0)
                {
                    this.expandBucketForPartitionValue(jobId,
                            query.isIncludeInterim(),
                            b, query.getPartitionValue());
                }

                b.setAnomalyScore(
                        b.partitionAnomalyScore(query.getPartitionValue()));
            }

        }

        return buckets;
    }

    void mergePartitionScoresIntoBucket(List<ScoreTimestamp> scores,
            List<Bucket> buckets)
    {
        Iterator<ScoreTimestamp> itr = scores.iterator();
        ScoreTimestamp score = itr.hasNext() ? itr.next() : null;
        for (Bucket b : buckets)
        {
            if (score ==  null)
            {
                b.setMaxNormalizedProbability(0.0);
            }
            else
            {
                if (score.timestamp.equals(b.getTimestamp()))
                {
                    b.setMaxNormalizedProbability(score.score);
                    score = itr.hasNext() ? itr.next() : null;
                }
                else
                {
                    b.setMaxNormalizedProbability(0.0);
                }
            }
        }
    }

    private QueryPage<Bucket> buckets(ElasticsearchJobId jobId, boolean includeInterim,
            int skip, int take, QueryBuilder fb, SortBuilder sb) throws ResourceNotFoundException
    {
        SearchResponse searchResponse;
        try {
            LOGGER.trace("ES API CALL: search all of type " + Bucket.TYPE +
                    " from index " + jobId.getIndex() + " sort ascending " + ElasticsearchMappings.ES_TIMESTAMP +
                    " with filter after sort skip " + skip + " take " + take);
            searchResponse = client.prepareSearch(jobId.getIndex())
                    .setTypes(Bucket.TYPE.getPreferredName())
                    .addSort(sb)
                    .setPostFilter(fb)
                    .setFrom(skip).setSize(take)
                    .get();
        } catch (IndexNotFoundException e) {
            throw ExceptionsHelper.missingException(jobId.getId());
        }

        List<Bucket> results = new ArrayList<>();


        for (SearchHit hit : searchResponse.getHits().getHits())
        {
            // Remove the Kibana/Logstash '@timestamp' entry as stored in Elasticsearch,
            // and replace using the API 'timestamp' key.
            Object timestamp = hit.getSource().remove(ElasticsearchMappings.ES_TIMESTAMP);
            hit.getSource().put(Bucket.TIMESTAMP.getPreferredName(), timestamp);

            Bucket bucket = objectMapper.convertValue(hit.getSource(), Bucket.class);
            bucket.setId(hit.getId());

            if (includeInterim || bucket.isInterim() == false)
            {
                results.add(bucket);
            }
        }

        return new QueryPage<>(results, searchResponse.getHits().getTotalHits());
    }


    @Override
    public Optional<Bucket> bucket(String jobId, BucketQueryBuilder.BucketQuery query) throws ResourceNotFoundException {
        ElasticsearchJobId elasticJobId = new ElasticsearchJobId(jobId);
        SearchHits hits;
        try {
            LOGGER.trace("ES API CALL: get Bucket with timestamp " + query.getTimestamp() +
                    " from index " + elasticJobId.getIndex());
            QueryBuilder qb = QueryBuilders.matchQuery(ElasticsearchMappings.ES_TIMESTAMP,
                    query.getTimestamp());

            SearchResponse searchResponse = client.prepareSearch(elasticJobId.getIndex())
                    .setTypes(Bucket.TYPE.getPreferredName())
                    .setQuery(qb)
                    .addSort(SortBuilders.fieldSort(ElasticsearchMappings.ES_DOC))
                    .get();
            hits = searchResponse.getHits();
        } catch (IndexNotFoundException e) {
            throw ExceptionsHelper.missingException(jobId);
        }

        Optional<Bucket> doc = Optional.<Bucket>empty();
        if (hits.getTotalHits() == 1L) {
            SearchHit hit = hits.getAt(0);
            // Remove the Kibana/Logstash '@timestamp' entry as stored in Elasticsearch,
            // and replace using the API 'timestamp' key.
            Object ts = hit.getSource().remove(ElasticsearchMappings.ES_TIMESTAMP);
            hit.getSource().put(Bucket.TIMESTAMP.getPreferredName(), ts);

            Bucket bucket = objectMapper.convertValue(hit.getSource(), Bucket.class);
            bucket.setId(hit.getId());

            // don't return interim buckets if not requested
            if (bucket.isInterim() && query.isIncludeInterim() == false) {
                return doc;
            }

            if (Strings.isNullOrEmpty(query.getPartitionValue())) {
                if (query.isExpand() && bucket.getRecordCount() > 0) {
                    expandBucket(jobId, query.isIncludeInterim(), bucket);
                }
            } else {
                List<ScoreTimestamp> scores =
                        partitionScores(elasticJobId,
                                query.getTimestamp(), query.getTimestamp() +1,
                                query.getPartitionValue());


                bucket.setMaxNormalizedProbability(scores.isEmpty() == false ?
                        scores.get(0).score : 0.0d);
                if (query.isExpand() && bucket.getRecordCount() > 0) {
                    this.expandBucketForPartitionValue(jobId, query.isIncludeInterim(),
                            bucket, query.getPartitionValue());
                }

                bucket.setAnomalyScore(
                        bucket.partitionAnomalyScore(query.getPartitionValue()));
            }

            doc = Optional.of(bucket);
        }

        return doc;
    }

    final class ScoreTimestamp
    {
        double score;
        Date timestamp;

        public ScoreTimestamp(Date timestamp, double score)
        {
            this.score = score;
            this.timestamp = timestamp;
        }
    }

    private List<ScoreTimestamp> partitionScores(ElasticsearchJobId jobId, Object epochStart,
            Object epochEnd, String partitionFieldValue)
                    throws ResourceNotFoundException
    {
        QueryBuilder qb = new ResultsFilterBuilder()
                .timeRange(ElasticsearchMappings.ES_TIMESTAMP, epochStart, epochEnd)
                .build();

        FieldSortBuilder sb = new FieldSortBuilder(ElasticsearchMappings.ES_TIMESTAMP)
                .order(SortOrder.ASC);

        SearchRequestBuilder searchBuilder = client
                .prepareSearch(jobId.getIndex())
                .setPostFilter(qb)
                .addSort(sb)
                .setTypes(PartitionNormalisedProb.TYPE);

        SearchResponse searchResponse;
        try {
            searchResponse = searchBuilder.get();
        } catch (IndexNotFoundException e) {
            throw ExceptionsHelper.missingException(jobId.getId());
        }

        List<ScoreTimestamp> results = new ArrayList<>();

        // expect 1 document per bucket
        if (searchResponse.getHits().totalHits() > 0)
        {
            Map<String, Object> m  = searchResponse.getHits().getAt(0).getSource();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> probs = (List<Map<String, Object>>)
            m.get(PartitionNormalisedProb.PARTITION_NORMALIZED_PROBS);
            for (Map<String, Object> prob : probs)
            {
                if (partitionFieldValue.equals(prob.get(AnomalyRecord.PARTITION_FIELD_VALUE)))
                {
                    Date ts = objectMapper.convertValue(m.get(ElasticsearchMappings.ES_TIMESTAMP), Date.class);
                    results.add(new ScoreTimestamp(ts,
                            (Double) prob.get(Bucket.MAX_NORMALIZED_PROBABILITY)));
                }
            }
        }

        return results;
    }

    public int expandBucketForPartitionValue(String jobId, boolean includeInterim, Bucket bucket,
            String partitionFieldValue) throws ResourceNotFoundException
    {
        int skip = 0;

        QueryPage<AnomalyRecord> page = bucketRecords(
                jobId, bucket, skip, RECORDS_TAKE_PARAM, includeInterim,
                AnomalyRecord.PROBABILITY.getPreferredName(), false, partitionFieldValue);
        bucket.setRecords(page.hits());

        while (page.hitCount() > skip + RECORDS_TAKE_PARAM)
        {
            skip += RECORDS_TAKE_PARAM;
            page = bucketRecords(
                    jobId, bucket, skip, RECORDS_TAKE_PARAM, includeInterim,
                    AnomalyRecord.PROBABILITY.getPreferredName(), false, partitionFieldValue);
            bucket.getRecords().addAll(page.hits());
        }

        return bucket.getRecords().size();
    }


    @Override
    public BatchedDocumentsIterator<Bucket> newBatchedBucketsIterator(String jobId)
    {
        return new ElasticsearchBatchedBucketsIterator(client, jobId, objectMapper);
    }

    @Override
    public int expandBucket(String jobId, boolean includeInterim, Bucket bucket) throws ResourceNotFoundException {
        int skip = 0;

        QueryPage<AnomalyRecord> page = bucketRecords(
                jobId, bucket, skip, RECORDS_TAKE_PARAM, includeInterim,
                AnomalyRecord.PROBABILITY.getPreferredName(), false, null);
        bucket.setRecords(page.hits());

        while (page.hitCount() > skip + RECORDS_TAKE_PARAM)
        {
            skip += RECORDS_TAKE_PARAM;
            page = bucketRecords(
                    jobId, bucket, skip, RECORDS_TAKE_PARAM, includeInterim,
                    AnomalyRecord.PROBABILITY.getPreferredName(), false, null);
            bucket.getRecords().addAll(page.hits());
        }

        return bucket.getRecords().size();
    }

    QueryPage<AnomalyRecord> bucketRecords(String jobId,
            Bucket bucket, int skip, int take, boolean includeInterim,
            String sortField, boolean descending, String partitionFieldValue)
                    throws ResourceNotFoundException
    {
        // Find the records using the time stamp rather than a parent-child
        // relationship.  The parent-child filter involves two queries behind
        // the scenes, and Elasticsearch documentation claims it's significantly
        // slower.  Here we rely on the record timestamps being identical to the
        // bucket timestamp.
        QueryBuilder recordFilter = QueryBuilders.termQuery(ElasticsearchMappings.ES_TIMESTAMP,
                bucket.getTimestamp().getTime());

        recordFilter = new ResultsFilterBuilder(recordFilter)
                .interim(AnomalyRecord.IS_INTERIM.getPreferredName(), includeInterim)
                .term(AnomalyRecord.PARTITION_FIELD_VALUE.getPreferredName(), partitionFieldValue)
                .build();

        FieldSortBuilder sb = null;
        if (sortField != null)
        {
            sb = new FieldSortBuilder(esSortField(sortField))
                    .missing("_last")
                    .order(descending ? SortOrder.DESC : SortOrder.ASC);
        }

        return records(new ElasticsearchJobId(jobId), skip, take, recordFilter, sb, SECONDARY_SORT,
                descending);
    }

    @Override
    public QueryPage<CategoryDefinition> categoryDefinitions(String jobId, int skip, int take) {
        ElasticsearchJobId elasticJobId = new ElasticsearchJobId(jobId);
        LOGGER.trace("ES API CALL: search all of type " + CategoryDefinition.TYPE +
                " from index " + elasticJobId.getIndex() + " sort ascending " + CategoryDefinition.CATEGORY_ID +
                " skip " + skip + " take " + take);
        SearchRequestBuilder searchBuilder = client.prepareSearch(elasticJobId.getIndex())
                .setTypes(CategoryDefinition.TYPE.getPreferredName())
                .setFrom(skip).setSize(take)
                .addSort(new FieldSortBuilder(CategoryDefinition.CATEGORY_ID.getPreferredName()).order(SortOrder.ASC));

        SearchResponse searchResponse;
        try {
            searchResponse = searchBuilder.get();
        } catch (IndexNotFoundException e) {
            throw ExceptionsHelper.missingException(jobId);
        }

        List<CategoryDefinition> results = Arrays.stream(searchResponse.getHits().getHits())
                .map(hit -> objectMapper.convertValue(hit.getSource(), CategoryDefinition.class))
                .collect(Collectors.toList());

        return new QueryPage<>(results, searchResponse.getHits().getTotalHits());
    }


    @Override
    public Optional<CategoryDefinition> categoryDefinition(String jobId, String categoryId) {
        ElasticsearchJobId elasticJobId = new ElasticsearchJobId(jobId);
        GetResponse response;

        try {
            LOGGER.trace("ES API CALL: get ID " + categoryId + " type " + CategoryDefinition.TYPE +
                    " from index " + elasticJobId.getIndex());
            response = client.prepareGet(elasticJobId.getIndex(), CategoryDefinition.TYPE.getPreferredName(), categoryId).get();
        } catch (IndexNotFoundException e) {
            throw ExceptionsHelper.missingException(jobId);
        }

        return response.isExists() ? Optional.of(objectMapper.convertValue(response.getSource(),
                CategoryDefinition.class)) : Optional.<CategoryDefinition> empty();
    }

    @Override
    public QueryPage<AnomalyRecord> records(String jobId, RecordsQueryBuilder.RecordsQuery query) {
        QueryBuilder fb = new ResultsFilterBuilder()
                .timeRange(ElasticsearchMappings.ES_TIMESTAMP, query.getEpochStart(), query.getEpochEnd())
                .score(AnomalyRecord.ANOMALY_SCORE.getPreferredName(), query.getAnomalyScoreThreshold())
                .score(AnomalyRecord.NORMALIZED_PROBABILITY.getPreferredName(), query.getNormalizedProbabilityThreshold())
                .interim(AnomalyRecord.IS_INTERIM.getPreferredName(), query.isIncludeInterim())
                .term(AnomalyRecord.PARTITION_FIELD_VALUE.getPreferredName(), query.getPartitionFieldValue()).build();

        return records(new ElasticsearchJobId(jobId), query.getSkip(), query.getTake(), fb, query.getSortField(), query.isSortDescending());
    }


    private QueryPage<AnomalyRecord> records(ElasticsearchJobId jobId,
            int skip, int take, QueryBuilder recordFilter,
            String sortField, boolean descending)
                    throws ResourceNotFoundException
    {
        FieldSortBuilder sb = null;
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
            QueryBuilder recordFilter, FieldSortBuilder sb, List<String> secondarySort,
            boolean descending) throws ResourceNotFoundException {
        SearchRequestBuilder searchBuilder = client.prepareSearch(jobId.getIndex())
                .setTypes(AnomalyRecord.TYPE.getPreferredName())
                .setPostFilter(recordFilter)
                .setFrom(skip).setSize(take)
                .addSort(sb == null ? SortBuilders.fieldSort(ElasticsearchMappings.ES_DOC) : sb)
                .addDocValueField(ElasticsearchMappings.PARENT)   // include the parent id
                .setFetchSource(true);  // the field option turns off source so request it explicitly

        for (String sortField : secondarySort)
        {
            searchBuilder.addSort(esSortField(sortField), descending ? SortOrder.DESC : SortOrder.ASC);
        }

        SearchResponse searchResponse;
        try {
            LOGGER.trace("ES API CALL: search all of type " + AnomalyRecord.TYPE +
                    " from index " + jobId.getIndex() + ((sb != null) ? " with sort" : "") +
                    (secondarySort.isEmpty() ? "" : " with secondary sort") +
                    " with filter after sort skip " + skip + " take " + take);
            searchResponse = searchBuilder.get();
        } catch (IndexNotFoundException e) {
            throw ExceptionsHelper.missingException(jobId.getId());
        }

        List<AnomalyRecord> results = new ArrayList<>();
        for (SearchHit hit : searchResponse.getHits().getHits())
        {
            Map<String, Object> m  = hit.getSource();

            // replace logstash timestamp name with timestamp
            m.put(AnomalyRecord.TIMESTAMP.getPreferredName(), m.remove(ElasticsearchMappings.ES_TIMESTAMP));

            AnomalyRecord record = objectMapper.convertValue(
                    m, AnomalyRecord.class);

            // set the ID and parent ID
            record.setId(hit.getId());
            record.setParent(hit.field(ElasticsearchMappings.PARENT).getValue().toString());

            results.add(record);
        }

        return new QueryPage<>(results, searchResponse.getHits().getTotalHits());
    }

    @Override
    public QueryPage<Influencer> influencers(String jobId, InfluencersQuery query) throws ResourceNotFoundException
    {

        QueryBuilder fb = new ResultsFilterBuilder()
                .timeRange(ElasticsearchMappings.ES_TIMESTAMP, query.getEpochStart(), query.getEpochEnd())
                .score(Bucket.ANOMALY_SCORE.getPreferredName(), query.getAnomalyScoreFilter())
                .interim(Bucket.IS_INTERIM.getPreferredName(), query.isIncludeInterim())
                .build();

        return influencers(new ElasticsearchJobId(jobId), query.getSkip(), query.getTake(), fb, query.getSortField(),
                query.isSortDescending());
    }

    private QueryPage<Influencer> influencers(ElasticsearchJobId jobId, int skip, int take, QueryBuilder filterBuilder, String sortField,
            boolean sortDescending) throws ResourceNotFoundException {
        LOGGER.trace("ES API CALL: search all of type " + Influencer.TYPE + " from index " + jobId.getIndex()
        + ((sortField != null)
                ? " with sort " + (sortDescending ? "descending" : "ascending") + " on field " + esSortField(sortField) : "")
        + " with filter after sort skip " + skip + " take " + take);

        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(jobId.getIndex())
                .setTypes(Influencer.TYPE.getPreferredName())
                .setPostFilter(filterBuilder)
                .setFrom(skip).setSize(take);

        FieldSortBuilder sb = sortField == null ? SortBuilders.fieldSort(ElasticsearchMappings.ES_DOC)
                : new FieldSortBuilder(esSortField(sortField)).order(sortDescending ? SortOrder.DESC : SortOrder.ASC);
        searchRequestBuilder.addSort(sb);

        SearchResponse response = null;
        try
        {
            response = searchRequestBuilder.get();
        }
        catch (IndexNotFoundException e)
        {
            throw new ResourceNotFoundException("job " + jobId.getId() + " not found");
        }

        List<Influencer> influencers = new ArrayList<>();
        for (SearchHit hit : response.getHits().getHits())
        {
            Map<String, Object> m = hit.getSource();

            // replace logstash timestamp name with timestamp
            m.put(Influencer.TIMESTAMP.getPreferredName(), m.remove(ElasticsearchMappings.ES_TIMESTAMP));

            Influencer influencer = objectMapper.convertValue(m, Influencer.class);
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
    public BatchedDocumentsIterator<Influencer> newBatchedInfluencersIterator(String jobId)
    {
        return new ElasticsearchBatchedInfluencersIterator(client, jobId, objectMapper);
    }

    @Override
    public BatchedDocumentsIterator<ModelSnapshot> newBatchedModelSnapshotIterator(String jobId)
    {
        return new ElasticsearchBatchedModelSnapshotIterator(client, jobId, objectMapper);
    }

    @Override
    public BatchedDocumentsIterator<ModelDebugOutput> newBatchedModelDebugOutputIterator(String jobId)
    {
        return new ElasticsearchBatchedModelDebugOutputIterator(client, jobId, objectMapper);
    }

    @Override
    public BatchedDocumentsIterator<ModelSizeStats> newBatchedModelSizeStatsIterator(String jobId)
    {
        return new ElasticsearchBatchedModelSizeStatsIterator(client, jobId, objectMapper);
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
        client.prepareIndex(PRELERT_INFO_INDEX, PRELERT_INFO_TYPE, PRELERT_INFO_ID)
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
            GetResponse response = client.prepareGet(
                    elasticJobId.getIndex(), Quantiles.TYPE.getPreferredName(), Quantiles.QUANTILES_ID).get();
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
    public QueryPage<ModelSnapshot> modelSnapshots(String jobId, int skip, int take)
            throws UnknownJobException
    {
        return modelSnapshots(jobId, skip, take, null, null, null, true, null, null);
    }

    @Override
    public QueryPage<ModelSnapshot> modelSnapshots(String jobId, int skip, int take,
            String startEpochMs, String endEpochMs, String sortField, boolean sortDescending,
            String snapshotId, String description) throws UnknownJobException
    {
        boolean haveId = snapshotId != null && !snapshotId.isEmpty();
        boolean haveDescription = description != null && !description.isEmpty();
        ResultsFilterBuilder fb;
        if (haveId || haveDescription)
        {
            BoolQueryBuilder query = QueryBuilders.boolQuery();
            if (haveId)
            {
                query.must(QueryBuilders.termQuery(ModelSnapshot.SNAPSHOT_ID.getPreferredName(), snapshotId));
            }
            if (haveDescription)
            {
                query.must(QueryBuilders.termQuery(ModelSnapshot.DESCRIPTION.getPreferredName(), description));
            }

            fb = new ResultsFilterBuilder(query);
        }
        else
        {
            fb = new ResultsFilterBuilder();
        }

        return modelSnapshots(new ElasticsearchJobId(jobId), skip, take,
                (sortField == null || sortField.isEmpty()) ? ModelSnapshot.RESTORE_PRIORITY.getPreferredName() : sortField,
                        sortDescending, fb.timeRange(
                                ElasticsearchMappings.ES_TIMESTAMP, startEpochMs, endEpochMs).build());
    }

    private QueryPage<ModelSnapshot> modelSnapshots(ElasticsearchJobId jobId, int skip, int take,
            String sortField, boolean sortDescending, QueryBuilder fb) throws UnknownJobException
    {
        FieldSortBuilder sb = new FieldSortBuilder(esSortField(sortField))
                .order(sortDescending ? SortOrder.DESC : SortOrder.ASC);

        // Wrap in a constant_score because we always want to
        // run it as a filter
        fb = new ConstantScoreQueryBuilder(fb);

        SearchResponse searchResponse;
        try
        {
            LOGGER.trace("ES API CALL: search all of type " + ModelSnapshot.TYPE +
                    " from index " + jobId.getIndex() + " sort ascending " + esSortField(sortField) +
                    " with filter after sort skip " + skip + " take " + take);
            searchResponse = client.prepareSearch(jobId.getIndex())
                    .setTypes(ModelSnapshot.TYPE.getPreferredName())
                    .addSort(sb)
                    .setQuery(fb)
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
            hit.getSource().put(ModelSnapshot.TIMESTAMP.getPreferredName(), timestamp);

            Object o = hit.getSource().get(ModelSizeStats.TYPE);
            if (o instanceof Map)
            {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>)o;
                Object ts = map.remove(ElasticsearchMappings.ES_TIMESTAMP);
                map.put(ModelSizeStats.TIMESTAMP_FIELD.getPreferredName(), ts);
            }

            ModelSnapshot modelSnapshot = objectMapper.convertValue(hit.getSource(), ModelSnapshot.class);
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
        ElasticsearchPersister persister = new ElasticsearchPersister(jobId, client);
        persister.persistModelSnapshot(modelSnapshot);

        if (restoreModelSizeStats)
        {
            if (modelSnapshot.getModelSizeStats() != null)
            {
                persister.persistModelSizeStats(modelSnapshot.getModelSizeStats());
            }
            if (modelSnapshot.getQuantiles() != null)
            {
                persister.persistQuantiles(modelSnapshot.getQuantiles());
            }
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
                null, null, null, true, snapshotId, null).hits();
        if (deleteCandidates == null || deleteCandidates.isEmpty())
        {
            throw new NoSuchModelSnapshotException(jobId);
        }

        ModelSnapshot modelSnapshot = deleteCandidates.get(0);

        ElasticsearchBulkDeleter deleter = new ElasticsearchBulkDeleter(client, jobId);
        deleter.deleteModelSnapshot(modelSnapshot);
        deleter.commit();

        return modelSnapshot;
    }

    private Quantiles createQuantiles(String jobId, GetResponse response)
    {
        Quantiles quantiles = objectMapper.convertValue(response.getSource(), Quantiles.class);
        if (quantiles.getQuantileState() == null)
        {
            LOGGER.error("Inconsistency - no " + Quantiles.QUANTILE_STATE
                    + " field in quantiles for job " + jobId);
        }
        return quantiles;
    }

    @Override
    public Optional<ModelSizeStats> modelSizeStats(String jobId)
    {
        ElasticsearchJobId elasticJobId = new ElasticsearchJobId(jobId);

        try
        {
            LOGGER.trace("ES API CALL: get ID " + ModelSizeStats.TYPE +
                    " type " + ModelSizeStats.TYPE + " from index " + elasticJobId.getIndex());

            GetResponse modelSizeStatsResponse = client.prepareGet(
                    elasticJobId.getIndex(), ModelSizeStats.TYPE.getPreferredName(), ModelSizeStats.TYPE.getPreferredName()).get();

            if (!modelSizeStatsResponse.isExists())
            {
                String msg = "No memory usage details for job with id " + elasticJobId.getId();
                LOGGER.warn(msg);
                return Optional.empty();
            }
            else
            {
                // Remove the Kibana/Logstash '@timestamp' entry as stored in Elasticsearch,
                // and replace using the API 'timestamp' key.
                Object timestamp = modelSizeStatsResponse.getSource().remove(ElasticsearchMappings.ES_TIMESTAMP);
                modelSizeStatsResponse.getSource().put(ModelSizeStats.TIMESTAMP_FIELD.getPreferredName(), timestamp);

                ModelSizeStats modelSizeStats = objectMapper.convertValue(
                        modelSizeStatsResponse.getSource(), ModelSizeStats.class);
                return Optional.of(modelSizeStats);
            }
        }
        catch (IndexNotFoundException e)
        {
            LOGGER.warn("Missing index " + elasticJobId.getIndex(), e);
            return Optional.empty();
        }
    }

    @Override
    public void refreshIndex(String jobId)
    {
        String indexName = new ElasticsearchJobId(jobId).getIndex();
        // Flush should empty the translog into Lucene
        LOGGER.trace("ES API CALL: flush index " + indexName);
        client.admin().indices().flush(new FlushRequest(indexName)).actionGet();
        // Refresh should wait for Lucene to make the data searchable
        LOGGER.trace("ES API CALL: refresh index " + indexName);
        client.admin().indices().refresh(new RefreshRequest(indexName)).actionGet();
    }

    @Override
    public boolean updateSchedulerState(String jobId, SchedulerState schedulerState)
            throws UnknownJobException
    {
        LOGGER.trace("ES API CALL: update scheduler state for job " + jobId);

        ElasticsearchJobId esJobId = new ElasticsearchJobId(jobId);

        try
        {
            client.prepareIndex(esJobId.getIndex(), SchedulerState.TYPE, SchedulerState.TYPE)
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
            response = client.prepareGet(esJobId.getIndex(), SchedulerState.TYPE, SchedulerState.TYPE).get();
        }
        catch (IndexNotFoundException e)
        {
            LOGGER.warn("No schedulerState could be retrieved for job: " + jobId, e);
        }

        if (response != null && response.isExists())
        {
            SchedulerState schedulerState = objectMapper.convertValue(response.getSource(),
                    SchedulerState.class);
            result = Optional.of(schedulerState);
        }
        return result;
    }

    @Override
    public Auditor audit(String jobId)
    {
        return new ElasticsearchAuditor(client, PRELERT_INFO_INDEX, jobId);
    }

    private String esSortField(String sortField)
    {
        // Beware: There's an assumption here that Bucket.TIMESTAMP,
        // AnomalyRecord.TIMESTAMP, Influencer.TIMESTAMP and
        // ModelSnapshot.TIMESTAMP are all the same
        return sortField.equals(Bucket.TIMESTAMP.getPreferredName()) ? ElasticsearchMappings.ES_TIMESTAMP : sortField;
    }
}
