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
import java.util.Collection;
import java.util.Map;

import org.elasticsearch.common.xcontent.XContentBuilder;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.AnalysisLimits;
import com.prelert.job.CategorizerState;
import com.prelert.job.DataCounts;
import com.prelert.job.DataDescription;
import com.prelert.job.Detector;
import com.prelert.job.JobDetails;
import com.prelert.job.ListDocument;
import com.prelert.job.ModelDebugConfig;
import com.prelert.job.ModelSizeStats;
import com.prelert.job.ModelSnapshot;
import com.prelert.job.ModelState;
import com.prelert.job.SchedulerConfig;
import com.prelert.job.audit.AuditActivity;
import com.prelert.job.audit.AuditMessage;
import com.prelert.job.detectionrules.DetectionRule;
import com.prelert.job.detectionrules.RuleCondition;
import com.prelert.job.quantiles.Quantiles;
import com.prelert.job.results.AnomalyCause;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.BucketInfluencer;
import com.prelert.job.results.BucketProcessingTime;
import com.prelert.job.results.CategoryDefinition;
import com.prelert.job.results.Influence;
import com.prelert.job.results.Influencer;
import com.prelert.job.results.ModelDebugOutput;
import com.prelert.job.results.PartitionNormalisedProb;
import com.prelert.job.transform.TransformConfig;
import com.prelert.job.usage.Usage;

/**
 * Static methods to create Elasticsearch mappings for the autodetect
 * persisted objects/documents
 *
 * ElasticSearch automatically recognises array types so they are
 * not explicitly mapped as such. For arrays of objects the type
 * must be set to <i>nested</i> so the arrays are searched properly
 * see https://www.elastic.co/guide/en/elasticsearch/guide/current/nested-objects.html
 *
 * It is expected that indexes to which these mappings are applied have their
 * default analyzer set to "keyword", which does not tokenise fields.  The
 * index-wide default analyzer cannot be set via these mappings, so needs to be
 * set in the index settings during index creation.  Then the _all field has its
 * analyzer set to "whitespace" by these mappings, so that _all gets tokenised
 * using whitespace.
 */
public class ElasticsearchMappings
{
    /**
     * String constants used in mappings
     */
    static final String INDEX = "index";
    static final String NO = "no";
    static final String ALL = "_all";
    static final String ENABLED = "enabled";
    static final String ANALYZER = "analyzer";
    static final String WHITESPACE = "whitespace";
    static final String INCLUDE_IN_ALL = "include_in_all";
    static final String NESTED = "nested";
    static final String COPY_TO = "copy_to";
    static final String PARENT = "_parent";
    static final String PROPERTIES = "properties";
    static final String TYPE = "type";
    static final String DYNAMIC = "dynamic";
    static final String FIELDS = "fields";
    static final String RAW = "raw";

    /**
     * Name of the field used to store the timestamp in Elasticsearch.
     * Note the field name is different to {@link com.prelert.job.results.Bucket#TIMESTAMP} used by the
     * API Bucket Resource, and is chosen for consistency with the default field name used by
     * Logstash and Kibana.
     */
    static final String ES_TIMESTAMP = "@timestamp";

    /**
     * Name of the Elasticsearch field by which documents are sorted by default
     */
    static final String ES_DOC = "_doc";

    /**
     * Elasticsearch data types
     */
    static final String BOOLEAN = "boolean";
    static final String DATE = "date";
    static final String DOUBLE = "double";
    static final String INTEGER = "integer";
    static final String KEYWORD = "keyword";
    static final String LONG = "long";
    static final String OBJECT = "object";
    static final String TEXT = "text";

    private ElasticsearchMappings()
    {
    }

    /**
     * Create the Elasticsearch mapping for {@linkplain com.prelert.job.JobDetails}.
     * The '_all' field is disabled as the document isn't meant to be searched.
     *
     * @return
     * @throws IOException
     */
    public static XContentBuilder jobMapping()
    throws IOException
    {
        return jsonBuilder()
            .startObject()
                .startObject(JobDetails.TYPE)
                    .startObject(ALL)
                        .field(ENABLED, false)
                        // analyzer must be specified even though _all is disabled
                        // because all types in the same index must have the same
                        // analyzer for a given field
                        .field(ANALYZER, WHITESPACE)
                    .endObject()
                    .startObject(PROPERTIES)
                        .startObject(ElasticsearchPersister.JOB_ID_NAME)
                            .field(TYPE, KEYWORD)
                        .endObject()
                        // "description" is analyzed so that it has the same
                        // mapping as a user field of the same name - this means
                        // it doesn't have to be a reserved field name
                        .startObject(JobDetails.DESCRIPTION)
                            .field(TYPE, TEXT)
                        .endObject()
                        // "status" is analyzed so that it has the same mapping
                        // as a user field of the same name - this means it
                        // doesn't have to be a reserved field name
                        .startObject(JobDetails.STATUS)
                            .field(TYPE, TEXT)
                        .endObject()
                        .startObject(JobDetails.SCHEDULER_STATUS)
                            .field(TYPE, KEYWORD)
                        .endObject()
                        .startObject(ES_TIMESTAMP)
                            .field(TYPE, DATE)
                        .endObject()
                        .startObject(JobDetails.FINISHED_TIME)
                            .field(TYPE, DATE)
                        .endObject()
                        .startObject(JobDetails.LAST_DATA_TIME)
                            .field(TYPE, DATE)
                        .endObject()
                        .startObject(JobDetails.COUNTS)
                            .field(TYPE, OBJECT)
                            .startObject(PROPERTIES)
                                .startObject(DataCounts.BUCKET_COUNT)
                                    .field(TYPE, LONG)
                                .endObject()
                                .startObject(DataCounts.PROCESSED_RECORD_COUNT)
                                    .field(TYPE, LONG)
                                .endObject()
                                .startObject(DataCounts.PROCESSED_FIELD_COUNT)
                                    .field(TYPE, LONG)
                                .endObject()
                                .startObject(DataCounts.INPUT_BYTES)
                                    .field(TYPE, LONG)
                                .endObject()
                                .startObject(DataCounts.INPUT_RECORD_COUNT)
                                    .field(TYPE, LONG)
                                .endObject()
                                .startObject(DataCounts.INPUT_FIELD_COUNT)
                                    .field(TYPE, LONG)
                                .endObject()
                                .startObject(DataCounts.INVALID_DATE_COUNT)
                                    .field(TYPE, LONG)
                                .endObject()
                                .startObject(DataCounts.MISSING_FIELD_COUNT)
                                    .field(TYPE, LONG)
                                .endObject()
                                .startObject(DataCounts.OUT_OF_ORDER_TIME_COUNT)
                                    .field(TYPE, LONG)
                                .endObject()
                                .startObject(DataCounts.FAILED_TRANSFORM_COUNT)
                                    .field(TYPE, LONG)
                                .endObject()
                                .startObject(DataCounts.EXCLUDED_RECORD_COUNT)
                                    .field(TYPE, LONG)
                                .endObject()
                                .startObject(DataCounts.LATEST_RECORD_TIME)
                                    .field(TYPE, DATE)
                                .endObject()
                            .endObject()
                        .endObject()
                        .startObject(JobDetails.IGNORE_DOWNTIME)
                            .field(TYPE, KEYWORD)
                        .endObject()
                        .startObject(JobDetails.TIMEOUT)
                            .field(TYPE, LONG).field(INDEX, NO)
                        .endObject()
                        .startObject(JobDetails.RENORMALIZATION_WINDOW_DAYS)
                            .field(TYPE, LONG).field(INDEX, NO)
                        .endObject()
                        .startObject(JobDetails.BACKGROUND_PERSIST_INTERVAL)
                            .field(TYPE, LONG).field(INDEX, NO)
                        .endObject()
                        .startObject(JobDetails.MODEL_SNAPSHOT_RETENTION_DAYS)
                            .field(TYPE, LONG).field(INDEX, NO)
                        .endObject()
                        .startObject(JobDetails.RESULTS_RETENTION_DAYS)
                            .field(TYPE, LONG).field(INDEX, NO)
                        .endObject()
                        .startObject(JobDetails.ANALYSIS_CONFIG)
                            .field(TYPE, OBJECT)
                            .startObject(PROPERTIES)
                                .startObject(AnalysisConfig.BUCKET_SPAN)
                                    .field(TYPE, LONG).field(INDEX, NO)
                                .endObject()
                                .startObject(AnalysisConfig.BATCH_SPAN)
                                    .field(TYPE, LONG).field(INDEX, NO)
                                .endObject()
                                .startObject(AnalysisConfig.LATENCY)
                                    .field(TYPE, LONG).field(INDEX, NO)
                                .endObject()
                                .startObject(AnalysisConfig.PERIOD)
                                    .field(TYPE, LONG).field(INDEX, NO)
                                .endObject()
                                .startObject(AnalysisConfig.SUMMARY_COUNT_FIELD_NAME)
                                    .field(TYPE, KEYWORD)
                                .endObject()
                                .startObject(AnalysisConfig.CATEGORIZATION_FIELD_NAME)
                                    .field(TYPE, KEYWORD)
                                .endObject()
                                .startObject(AnalysisConfig.CATEGORIZATION_FILTERS)
                                    .field(TYPE, KEYWORD)
                                .endObject()
                                .startObject(AnalysisConfig.DETECTORS)
                                    .startObject(PROPERTIES)
                                        .startObject(Detector.DETECTOR_DESCRIPTION)
                                            .field(TYPE, KEYWORD)
                                        .endObject()
                                        .startObject(Detector.FUNCTION)
                                            .field(TYPE, KEYWORD)
                                        .endObject()
                                        .startObject(Detector.FIELD_NAME)
                                            .field(TYPE, KEYWORD)
                                        .endObject()
                                        .startObject(Detector.BY_FIELD_NAME)
                                            .field(TYPE, KEYWORD)
                                        .endObject()
                                        .startObject(Detector.OVER_FIELD_NAME)
                                            .field(TYPE, KEYWORD)
                                        .endObject()
                                        .startObject(Detector.PARTITION_FIELD_NAME)
                                            .field(TYPE, KEYWORD)
                                        .endObject()
                                        .startObject(Detector.USE_NULL)
                                            .field(TYPE, BOOLEAN)
                                        .endObject()
                                        .startObject(Detector.DETECTOR_RULES)
                                            .field(TYPE, OBJECT)
                                            .startObject(PROPERTIES)
                                                .startObject(DetectionRule.RULE_ACTION)
                                                    .field(TYPE, KEYWORD)
                                                    .endObject()
                                                .startObject(DetectionRule.TARGET_FIELD_NAME)
                                                    .field(TYPE, KEYWORD)
                                                .endObject()
                                                .startObject(DetectionRule.TARGET_FIELD_VALUE)
                                                    .field(TYPE, KEYWORD)
                                                .endObject()
                                                .startObject(DetectionRule.CONDITIONS_CONNECTIVE)
                                                    .field(TYPE, KEYWORD)
                                                .endObject()
                                                .startObject(DetectionRule.RULE_CONDITIONS)
                                                    .field(TYPE, OBJECT)
                                                    .startObject(PROPERTIES)
                                                        .startObject(RuleCondition.CONDITION_TYPE)
                                                            .field(TYPE, KEYWORD)
                                                        .endObject()
                                                        .startObject(RuleCondition.FIELD_NAME)
                                                            .field(TYPE, KEYWORD)
                                                        .endObject()
                                                        .startObject(RuleCondition.FIELD_VALUE)
                                                            .field(TYPE, KEYWORD)
                                                        .endObject()
                                                        .startObject(RuleCondition.VALUE_LIST)
                                                            .field(TYPE, KEYWORD)
                                                        .endObject()
                                                    .endObject()
                                                .endObject()
                                            .endObject()
                                        .endObject()
                                    .endObject()
                                .endObject()
                                .startObject(AnalysisConfig.OVERLAPPING_BUCKETS)
                                    .field(TYPE, BOOLEAN).field(INDEX, NO)
                                .endObject()
                                .startObject(AnalysisConfig.RESULT_FINALIZATION_WINDOW)
                                    .field(TYPE, LONG).field(INDEX, NO)
                                .endObject()
                                .startObject(AnalysisConfig.MULTIVARIATE_BY_FIELDS)
                                    .field(TYPE, BOOLEAN).field(INDEX, NO)
                                .endObject()
                            .endObject()
                        .endObject()
                        .startObject(JobDetails.ANALYSIS_LIMITS)
                            .field(TYPE, OBJECT)
                            .startObject(PROPERTIES)
                                .startObject(AnalysisLimits.MODEL_MEMORY_LIMIT)
                                    .field(TYPE, LONG).field(INDEX, NO)
                                .endObject()
                                .startObject(AnalysisLimits.CATEGORIZATION_EXAMPLES_LIMIT)
                                    .field(TYPE, LONG).field(INDEX, NO)
                                .endObject()
                            .endObject()
                        .endObject()
                        .startObject(JobDetails.DATA_DESCRIPTION)
                            .field(TYPE, OBJECT)
                            .startObject(PROPERTIES)
                                .startObject(DataDescription.FORMAT)
                                    .field(TYPE, KEYWORD)
                                .endObject()
                                .startObject(DataDescription.TIME_FIELD_NAME)
                                    .field(TYPE, KEYWORD)
                                .endObject()
                                .startObject(DataDescription.TIME_FORMAT)
                                    .field(TYPE, KEYWORD)
                                .endObject()
                                .startObject(DataDescription.FIELD_DELIMITER)
                                    .field(TYPE, KEYWORD)
                                .endObject()
                                .startObject(DataDescription.QUOTE_CHARACTER)
                                    .field(TYPE, KEYWORD)
                                .endObject()
                            .endObject()
                        .endObject()
                        .startObject(JobDetails.TRANSFORMS)
                            .field(TYPE, OBJECT)
                            .startObject(PROPERTIES)
                                .startObject(TransformConfig.TRANSFORM)
                                    .field(TYPE, KEYWORD)
                                .endObject()
                                .startObject(TransformConfig.ARGUMENTS)
                                    .field(TYPE, KEYWORD)
                                .endObject()
                                .startObject(TransformConfig.INPUTS)
                                    .field(TYPE, KEYWORD)
                                .endObject()
                                .startObject(TransformConfig.OUTPUTS)
                                    .field(TYPE, KEYWORD)
                                .endObject()
                            .endObject()
                        .endObject()
                        .startObject(JobDetails.SCHEDULER_CONFIG)
                            .field(TYPE, OBJECT)
                            .startObject(PROPERTIES)
                                .startObject(SchedulerConfig.DATA_SOURCE)
                                    .field(TYPE, KEYWORD)
                                .endObject()
                                .startObject(SchedulerConfig.DATA_SOURCE_COMPATIBILITY)
                                    .field(TYPE, KEYWORD)
                                .endObject()
                                .startObject(SchedulerConfig.QUERY_DELAY)
                                    .field(TYPE, LONG)
                                .endObject()
                                .startObject(SchedulerConfig.FREQUENCY)
                                    .field(TYPE, LONG)
                                .endObject()
                                .startObject(SchedulerConfig.FILE_PATH)
                                    .field(TYPE, KEYWORD)
                                .endObject()
                                .startObject(SchedulerConfig.TAIL_FILE)
                                    .field(TYPE, BOOLEAN)
                                .endObject()
                                .startObject(SchedulerConfig.BASE_URL)
                                    .field(TYPE, KEYWORD)
                                .endObject()
                                // "username" is analyzed so that it has the
                                // same mapping as a user field of the same
                                // name - this means it doesn't have to be a
                                // reserved field name
                                .startObject(SchedulerConfig.USERNAME)
                                    .field(TYPE, TEXT)
                                .endObject()
                                .startObject(SchedulerConfig.ENCRYPTED_PASSWORD)
                                    .field(TYPE, KEYWORD)
                                .endObject()
                                .startObject(SchedulerConfig.INDEXES)
                                    .field(TYPE, KEYWORD)
                                .endObject()
                                .startObject(SchedulerConfig.TYPES)
                                    .field(TYPE, KEYWORD)
                                .endObject()
                                .startObject(SchedulerConfig.RETRIEVE_WHOLE_SOURCE)
                                    .field(TYPE, BOOLEAN)
                                .endObject()
                                .startObject(SchedulerConfig.QUERY)
                                    .field(TYPE, OBJECT)
                                    .field(DYNAMIC, false)
                                .endObject()
                                .startObject(SchedulerConfig.AGGREGATIONS)
                                    .field(TYPE, OBJECT)
                                    .field(DYNAMIC, false)
                                .endObject()
                                .startObject(SchedulerConfig.AGGS)
                                    .field(TYPE, OBJECT)
                                    .field(DYNAMIC, false)
                                .endObject()
                                .startObject(SchedulerConfig.SCRIPT_FIELDS)
                                    .field(TYPE, OBJECT)
                                    .field(DYNAMIC, false)
                                .endObject()
                                .startObject(SchedulerConfig.SCROLL_SIZE)
                                    .field(TYPE, INTEGER)
                                .endObject()
                            .endObject()
                        .endObject()
                        .startObject(JobDetails.MODEL_DEBUG_CONFIG)
                            .field(TYPE, OBJECT)
                            .startObject(PROPERTIES)
                                .startObject(ModelDebugConfig.WRITE_TO)
                                    .field(TYPE, TEXT).field(INDEX, NO)
                                .endObject()
                                .startObject(ModelDebugConfig.BOUNDS_PERCENTILE)
                                    .field(TYPE, DOUBLE).field(INDEX, NO)
                                .endObject()
                                .startObject(ModelDebugConfig.TERMS)
                                    .field(TYPE, TEXT).field(INDEX, NO)
                                .endObject()
                            .endObject()
                        .endObject()
                        .startObject(JobDetails.CUSTOM_SETTINGS)
                            .field(TYPE, OBJECT)
                            .field(DYNAMIC, false)
                        .endObject()
                    .endObject()
                .endObject()
            .endObject();
    }

    /**
     * Create the Elasticsearch mapping for {@linkplain com.prelert.job.results.Bucket}.
     * The '_all' field is disabled as the document isn't meant to be searched.
     *
     * @return
     * @throws IOException
     */
    public static XContentBuilder bucketMapping()
    throws IOException
    {
        return jsonBuilder()
            .startObject()
                .startObject(Bucket.TYPE)
                    .startObject(ALL)
                        .field(ENABLED, false)
                        // analyzer must be specified even though _all is disabled
                        // because all types in the same index must have the same
                        // analyzer for a given field
                        .field(ANALYZER, WHITESPACE)
                    .endObject()
                    .startObject(PROPERTIES)
                        .startObject(ElasticsearchPersister.JOB_ID_NAME)
                            .field(TYPE, KEYWORD)
                        .endObject()
                        .startObject(ES_TIMESTAMP)
                            .field(TYPE, DATE)
                        .endObject()
                        .startObject(Bucket.ANOMALY_SCORE)
                            .field(TYPE, DOUBLE)
                        .endObject()
                        .startObject(Bucket.INITIAL_ANOMALY_SCORE)
                            .field(TYPE, DOUBLE)
                        .endObject()
                        .startObject(Bucket.MAX_NORMALIZED_PROBABILITY)
                            .field(TYPE, DOUBLE)
                        .endObject()
                        .startObject(Bucket.IS_INTERIM)
                            .field(TYPE, BOOLEAN)
                        .endObject()
                        .startObject(Bucket.RECORD_COUNT)
                            .field(TYPE, LONG)
                        .endObject()
                        .startObject(Bucket.EVENT_COUNT)
                            .field(TYPE, LONG)
                        .endObject()
                        .startObject(Bucket.BUCKET_SPAN)
                            .field(TYPE, LONG)
                        .endObject()
                        .startObject(Bucket.PROCESSING_TIME_MS)
                            .field(TYPE, LONG)
                        .endObject()
                        .startObject(Bucket.BUCKET_INFLUENCERS)
                            .field(TYPE, NESTED)
                            .startObject(PROPERTIES)
                                .startObject(BucketInfluencer.INFLUENCER_FIELD_NAME)
                                    .field(TYPE, KEYWORD)
                                .endObject()
                                .startObject(BucketInfluencer.INITIAL_ANOMALY_SCORE)
                                    .field(TYPE, DOUBLE)
                                .endObject()
                                .startObject(BucketInfluencer.ANOMALY_SCORE)
                                    .field(TYPE, DOUBLE)
                                .endObject()
                                .startObject(BucketInfluencer.RAW_ANOMALY_SCORE)
                                    .field(TYPE, DOUBLE)
                                .endObject()
                                .startObject(BucketInfluencer.PROBABILITY)
                                    .field(TYPE, DOUBLE)
                                .endObject()
                            .endObject()
                        .endObject()
                        .startObject(Bucket.PARTITION_SCORES)
                            .field(TYPE, NESTED)
                            .startObject(PROPERTIES)
                                .startObject(AnomalyRecord.PARTITION_FIELD_NAME)
                                    .field(TYPE, KEYWORD)
                                .endObject()
                                .startObject(AnomalyRecord.PARTITION_FIELD_VALUE)
                                    .field(TYPE, KEYWORD)
                                .endObject()
                                .startObject(AnomalyRecord.ANOMALY_SCORE)
                                    .field(TYPE, DOUBLE)
                                .endObject()
                                .startObject(AnomalyRecord.PROBABILITY)
                                .field(TYPE, DOUBLE)
                            .endObject()
                            .endObject()
                        .endObject()
                    .endObject()
                .endObject()
            .endObject();
    }

    /**
     * Create the Elasticsearch mapping for {@linkplain com.prelert.job.results.BucketInfluencer}.
     *
     * @return
     * @throws IOException
     */
    public static XContentBuilder bucketInfluencerMapping()
    throws IOException
    {
        return jsonBuilder()
            .startObject()
                .startObject(BucketInfluencer.TYPE)
                    .startObject(ALL)
                        .field(ENABLED, false)
                        // analyzer must be specified even though _all is disabled
                        // because all types in the same index must have the same
                        // analyzer for a given field
                        .field(ANALYZER, WHITESPACE)
                    .endObject()
                    .startObject(PROPERTIES)
                        .startObject(ElasticsearchPersister.JOB_ID_NAME)
                            .field(TYPE, KEYWORD)
                        .endObject()
                        .startObject(ES_TIMESTAMP)
                            .field(TYPE, DATE)
                        .endObject()
                        .startObject(Bucket.IS_INTERIM)
                            .field(TYPE, BOOLEAN)
                        .endObject()
                        .startObject(BucketInfluencer.INFLUENCER_FIELD_NAME)
                            .field(TYPE, KEYWORD)
                        .endObject()
                        .startObject(BucketInfluencer.INITIAL_ANOMALY_SCORE)
                            .field(TYPE, DOUBLE)
                        .endObject()
                        .startObject(BucketInfluencer.ANOMALY_SCORE)
                            .field(TYPE, DOUBLE)
                        .endObject()
                        .startObject(BucketInfluencer.RAW_ANOMALY_SCORE)
                            .field(TYPE, DOUBLE)
                        .endObject()
                        .startObject(BucketInfluencer.PROBABILITY)
                            .field(TYPE, DOUBLE)
                        .endObject()
                    .endObject()
                .endObject()
            .endObject();
    }

    /**
     * Partition normalized scores. There is one per bucket
     * so the timestamp is sufficient to uniquely identify
     * the document per bucket per job
     *
     * Partition field values and scores are nested objects.
     */
    public static XContentBuilder bucketPartitionMaxNormalizedScores()
    throws IOException
    {
        return jsonBuilder()
            .startObject()
                .startObject(PartitionNormalisedProb.TYPE)
                    .startObject(ALL)
                        .field(ENABLED, false)
                        // analyzer must be specified even though _all is disabled
                        // because all types in the same index must have the same
                        // analyzer for a given field
                        .field(ANALYZER, WHITESPACE)
                    .endObject()
                    .startObject(PROPERTIES)
                        .startObject(ElasticsearchPersister.JOB_ID_NAME)
                            .field(TYPE, KEYWORD)
                        .endObject()
                        .startObject(ES_TIMESTAMP)
                            .field(TYPE, DATE)
                        .endObject()
                        .startObject(PartitionNormalisedProb.PARTITION_NORMALIZED_PROBS)
                            .field(TYPE, NESTED)
                            .startObject(PROPERTIES)
                                .startObject(AnomalyRecord.PARTITION_FIELD_VALUE)
                                    .field(TYPE, KEYWORD)
                                .endObject()
                                .startObject(Bucket.MAX_NORMALIZED_PROBABILITY)
                                    .field(TYPE, DOUBLE)
                                .endObject()
                            .endObject()
                        .endObject()
                    .endObject()
                .endObject()
            .endObject();
    }

    public static XContentBuilder categorizerStateMapping() throws IOException
    {
        return jsonBuilder()
                .startObject()
                    .startObject(CategorizerState.TYPE)
                        .field(ENABLED, false)
                        .startObject(ALL)
                            .field(ENABLED, false)
                            // analyzer must be specified even though _all is disabled
                            // because all types in the same index must have the same
                            // analyzer for a given field
                            .field(ANALYZER, WHITESPACE)
                        .endObject()
                    .endObject()
                .endObject();
    }

    public static XContentBuilder categoryDefinitionMapping() throws IOException
    {
        return jsonBuilder()
                .startObject()
                    .startObject(CategoryDefinition.TYPE)
                        .startObject(ALL)
                            .field(ENABLED, false)
                            // analyzer must be specified even though _all is disabled
                            // because all types in the same index must have the same
                            // analyzer for a given field
                            .field(ANALYZER, WHITESPACE)
                        .endObject()
                        .startObject(PROPERTIES)
                            .startObject(CategoryDefinition.CATEGORY_ID)
                                .field(TYPE, LONG)
                            .endObject()
                            .startObject(ElasticsearchPersister.JOB_ID_NAME)
                                .field(TYPE, KEYWORD)
                            .endObject()
                            .startObject(CategoryDefinition.TERMS)
                                .field(TYPE, TEXT).field(INDEX, NO)
                            .endObject()
                            .startObject(CategoryDefinition.REGEX)
                                .field(TYPE, TEXT).field(INDEX, NO)
                            .endObject()
                            .startObject(CategoryDefinition.MAX_MATCHING_LENGTH)
                                .field(TYPE, LONG)
                            .endObject()
                            .startObject(CategoryDefinition.EXAMPLES)
                                .field(TYPE, TEXT).field(INDEX, NO)
                            .endObject()
                        .endObject()
                    .endObject()
                .endObject();
    }

    /**
     * Records have a _parent mapping to a {@linkplain com.prelert.job.results.Bucket}.
     *
     * @param termFieldNames Optionally, other field names to include in the
     * mappings.  Pass <code>null</code> if not required.
     * @return
     * @throws IOException
     */
    public static XContentBuilder recordMapping(Collection<String> termFieldNames)
    throws IOException
    {
        XContentBuilder builder = jsonBuilder()
            .startObject()
                .startObject(AnomalyRecord.TYPE)
                    .startObject(PARENT)
                        .field(TYPE, Bucket.TYPE)
                    .endObject()
                    .startObject(ALL)
                        .field(ANALYZER, WHITESPACE)
                    .endObject()
                    .startObject(PROPERTIES)
                        .startObject(ElasticsearchPersister.JOB_ID_NAME)
                            .field(TYPE, KEYWORD).field(INCLUDE_IN_ALL, false)
                        .endObject()
                        .startObject(ES_TIMESTAMP)
                            .field(TYPE, DATE).field(INCLUDE_IN_ALL, false)
                        .endObject()
                        .startObject(AnomalyRecord.DETECTOR_INDEX)
                            .field(TYPE, INTEGER).field(INCLUDE_IN_ALL, false)
                        .endObject()
                        .startObject(AnomalyRecord.ACTUAL)
                            .field(TYPE, DOUBLE).field(INCLUDE_IN_ALL, false)
                        .endObject()
                        .startObject(AnomalyRecord.TYPICAL)
                            .field(TYPE, DOUBLE).field(INCLUDE_IN_ALL, false)
                        .endObject()
                        .startObject(AnomalyRecord.PROBABILITY)
                            .field(TYPE, DOUBLE).field(INCLUDE_IN_ALL, false)
                        .endObject()
                        .startObject(AnomalyRecord.FUNCTION)
                            .field(TYPE, KEYWORD).field(INCLUDE_IN_ALL, false)
                        .endObject()
                        .startObject(AnomalyRecord.FUNCTION_DESCRIPTION)
                            .field(TYPE, KEYWORD).field(INCLUDE_IN_ALL, false)
                        .endObject()
                        .startObject(AnomalyRecord.BY_FIELD_NAME)
                            .field(TYPE, KEYWORD).field(INCLUDE_IN_ALL, false)
                        .endObject()
                        .startObject(AnomalyRecord.BY_FIELD_VALUE)
                            .field(TYPE, KEYWORD)
                        .endObject()
                        .startObject(AnomalyRecord.FIELD_NAME)
                            .field(TYPE, KEYWORD).field(INCLUDE_IN_ALL, false)
                        .endObject()
                        .startObject(AnomalyRecord.PARTITION_FIELD_NAME)
                            .field(TYPE, KEYWORD).field(INCLUDE_IN_ALL, false)
                        .endObject()
                        .startObject(AnomalyRecord.PARTITION_FIELD_VALUE)
                            .field(TYPE, KEYWORD)
                        .endObject()
                        .startObject(AnomalyRecord.OVER_FIELD_NAME)
                            .field(TYPE, KEYWORD).field(INCLUDE_IN_ALL, false)
                        .endObject()
                        .startObject(AnomalyRecord.OVER_FIELD_VALUE)
                            .field(TYPE, KEYWORD)
                        .endObject()
                        .startObject(AnomalyRecord.CAUSES)
                            .field(TYPE, NESTED)
                            .startObject(PROPERTIES)
                                .startObject(AnomalyCause.ACTUAL)
                                    .field(TYPE, DOUBLE).field(INCLUDE_IN_ALL, false)
                                .endObject()
                                .startObject(AnomalyCause.TYPICAL)
                                    .field(TYPE, DOUBLE).field(INCLUDE_IN_ALL, false)
                                .endObject()
                                .startObject(AnomalyCause.PROBABILITY)
                                    .field(TYPE, DOUBLE).field(INCLUDE_IN_ALL, false)
                                .endObject()
                                .startObject(AnomalyCause.FUNCTION)
                                    .field(TYPE, KEYWORD).field(INCLUDE_IN_ALL, false)
                                .endObject()
                                .startObject(AnomalyCause.FUNCTION_DESCRIPTION)
                                    .field(TYPE, KEYWORD).field(INCLUDE_IN_ALL, false)
                                .endObject()
                                .startObject(AnomalyCause.BY_FIELD_NAME)
                                    .field(TYPE, KEYWORD).field(INCLUDE_IN_ALL, false)
                                .endObject()
                                .startObject(AnomalyCause.BY_FIELD_VALUE)
                                    .field(TYPE, KEYWORD)
                                .endObject()
                                .startObject(AnomalyCause.CORRELATED_BY_FIELD_VALUE)
                                    .field(TYPE, KEYWORD)
                                .endObject()
                                .startObject(AnomalyCause.FIELD_NAME)
                                    .field(TYPE, KEYWORD).field(INCLUDE_IN_ALL, false)
                                .endObject()
                                .startObject(AnomalyCause.PARTITION_FIELD_NAME)
                                    .field(TYPE, KEYWORD).field(INCLUDE_IN_ALL, false)
                                .endObject()
                                .startObject(AnomalyCause.PARTITION_FIELD_VALUE)
                                    .field(TYPE, KEYWORD)
                                .endObject()
                                .startObject(AnomalyCause.OVER_FIELD_NAME)
                                    .field(TYPE, KEYWORD).field(INCLUDE_IN_ALL, false)
                                .endObject()
                                .startObject(AnomalyCause.OVER_FIELD_VALUE)
                                    .field(TYPE, KEYWORD)
                                .endObject()
                            .endObject()
                        .endObject()
                        .startObject(AnomalyRecord.ANOMALY_SCORE)
                            .field(TYPE, DOUBLE).field(INCLUDE_IN_ALL, false)
                        .endObject()
                        .startObject(AnomalyRecord.NORMALIZED_PROBABILITY)
                            .field(TYPE, DOUBLE).field(INCLUDE_IN_ALL, false)
                        .endObject()
                        .startObject(AnomalyRecord.INITIAL_NORMALIZED_PROBABILITY)
                            .field(TYPE, DOUBLE).field(INCLUDE_IN_ALL, false)
                        .endObject()
                        .startObject(AnomalyRecord.IS_INTERIM)
                            .field(TYPE, BOOLEAN).field(INCLUDE_IN_ALL, false)
                        .endObject()
                        .startObject(AnomalyRecord.INFLUENCERS)
                            /* Array of influences */
                            .field(TYPE, NESTED)
                            .startObject(PROPERTIES)
                                .startObject(Influence.INFLUENCER_FIELD_NAME)
                                    .field(TYPE, KEYWORD).field(INCLUDE_IN_ALL, false)
                                .endObject()
                                .startObject(Influence.INFLUENCER_FIELD_VALUES)
                                    .field(TYPE, KEYWORD)
                                .endObject()
                            .endObject()
                        .endObject();

        if (termFieldNames != null)
        {
            ElasticsearchDotNotationReverser reverser = new ElasticsearchDotNotationReverser();
            for (String fieldName : termFieldNames)
            {
                reverser.add(fieldName, "");
            }
            for (Map.Entry<String, Object> entry : reverser.getMappingsMap().entrySet())
            {
                builder.field(entry.getKey(), entry.getValue());
            }
        }

        return builder
                    .endObject()
                .endObject()
            .endObject();
    }

    /**
     * Create the Elasticsearch mapping for {@linkplain Quantiles}.
     * The '_all' field is disabled as the document isn't meant to be searched.
     *
     * The quantile state string is not searchable (index = 'no') as it could be
     * very large.
     *
     * @return
     * @throws IOException
     */
    public static XContentBuilder quantilesMapping()
    throws IOException
    {
        return jsonBuilder()
            .startObject()
                .startObject(Quantiles.TYPE)
                    .startObject(ALL)
                        .field(ENABLED, false)
                        // analyzer must be specified even though _all is disabled
                        // because all types in the same index must have the same
                        // analyzer for a given field
                        .field(ANALYZER, WHITESPACE)
                    .endObject()
                    .startObject(PROPERTIES)
                        .startObject(ES_TIMESTAMP)
                            .field(TYPE, DATE)
                        .endObject()
                        .startObject(Quantiles.QUANTILE_STATE)
                            .field(TYPE, TEXT).field(INDEX, NO)
                        .endObject()
                    .endObject()
                .endObject()
            .endObject();
    }

    /**
     * Create the Elasticsearch mapping for {@linkplain ModelState}.
     * The model state could potentially be huge (over a gigabyte in size)
     * so all analysis by Elasticsearch is disabled.  The only way to
     * retrieve the model state is by knowing the ID of a particular
     * document or by searching for all documents of this type.
     *
     * @return
     * @throws IOException
     */
    public static XContentBuilder modelStateMapping()
    throws IOException
    {
        return jsonBuilder()
            .startObject()
                .startObject(ModelState.TYPE)
                    .field(ENABLED, false)
                    .startObject(ALL)
                        .field(ENABLED, false)
                        // analyzer must be specified even though _all is disabled
                        // because all types in the same index must have the same
                        // analyzer for a given field
                        .field(ANALYZER, WHITESPACE)
                    .endObject()
                .endObject()
            .endObject();
    }

    /**
     * Create the Elasticsearch mapping for {@linkplain ModelState}.
     * The model state could potentially be huge (over a gigabyte in size)
     * so all analysis by Elasticsearch is disabled.  The only way to
     * retrieve the model state is by knowing the ID of a particular
     * document or by searching for all documents of this type.
     *
     * @return
     * @throws IOException
     */
    public static XContentBuilder modelSnapshotMapping()
    throws IOException
    {
        return jsonBuilder()
            .startObject()
                .startObject(ModelSnapshot.TYPE)
                    .startObject(ALL)
                        .field(ENABLED, false)
                        // analyzer must be specified even though _all is disabled
                        // because all types in the same index must have the same
                        // analyzer for a given field
                        .field(ANALYZER, WHITESPACE)
                    .endObject()
                    .startObject(PROPERTIES)
                        .startObject(ElasticsearchPersister.JOB_ID_NAME)
                            .field(TYPE, KEYWORD)
                        .endObject()
                        .startObject(ES_TIMESTAMP)
                            .field(TYPE, DATE)
                        .endObject()
                        // "description" is analyzed so that it has the same
                        // mapping as a user field of the same name - this means
                        // it doesn't have to be a reserved field name
                        .startObject(ModelSnapshot.DESCRIPTION)
                            .field(TYPE, TEXT)
                        .endObject()
                        .startObject(ModelSnapshot.RESTORE_PRIORITY)
                            .field(TYPE, LONG)
                        .endObject()
                        .startObject(ModelSnapshot.SNAPSHOT_ID)
                            .field(TYPE, KEYWORD)
                        .endObject()
                        .startObject(ModelSnapshot.SNAPSHOT_DOC_COUNT)
                            .field(TYPE, INTEGER)
                        .endObject()
                        .startObject(ModelSizeStats.TYPE)
                            .startObject(PROPERTIES)
                                .startObject(ElasticsearchPersister.JOB_ID_NAME)
                                    .field(TYPE, KEYWORD)
                                 .endObject()
                                .startObject(ModelSizeStats.MODEL_BYTES)
                                    .field(TYPE, LONG)
                                .endObject()
                                .startObject(ModelSizeStats.TOTAL_BY_FIELD_COUNT)
                                    .field(TYPE, LONG)
                                .endObject()
                                .startObject(ModelSizeStats.TOTAL_OVER_FIELD_COUNT)
                                    .field(TYPE, LONG)
                                .endObject()
                                .startObject(ModelSizeStats.TOTAL_PARTITION_FIELD_COUNT)
                                    .field(TYPE, LONG)
                                .endObject()
                                .startObject(ModelSizeStats.BUCKET_ALLOCATION_FAILURES_COUNT)
                                    .field(TYPE, LONG)
                                .endObject()
                                .startObject(ModelSizeStats.MEMORY_STATUS)
                                    .field(TYPE, KEYWORD)
                                .endObject()
                                .startObject(ES_TIMESTAMP)
                                    .field(TYPE, DATE)
                                .endObject()
                                .startObject(ModelSizeStats.LOG_TIME)
                                    .field(TYPE, DATE)
                                .endObject()
                            .endObject()
                        .endObject()
                        .startObject(Quantiles.TYPE)
                            .startObject(PROPERTIES)
                                .startObject(ES_TIMESTAMP)
                                    .field(TYPE, DATE)
                                .endObject()
                                .startObject(Quantiles.QUANTILE_STATE)
                                    .field(TYPE, TEXT).field(INDEX, NO)
                                .endObject()
                            .endObject()
                        .endObject()
                        .startObject(ModelSnapshot.LATEST_RECORD_TIME)
                            .field(TYPE, DATE)
                        .endObject()
                        .startObject(ModelSnapshot.LATEST_RESULT_TIME)
                        .field(TYPE, DATE)
                    .endObject()
                    .endObject()
                .endObject()
            .endObject();
    }

    /**
     * Create the Elasticsearch mapping for {@linkplain ModelSizeStats}.
     * @return
     * @throws IOException
     */
    public static XContentBuilder modelSizeStatsMapping()
    throws IOException
    {
        return jsonBuilder()
            .startObject()
                .startObject(ModelSizeStats.TYPE)
                    .startObject(ALL)
                        .field(ENABLED,  false)
                        // analyzer must be specified even though _all is disabled
                        // because all types in the same index must have the same
                        // analyzer for a given field
                        .field(ANALYZER, WHITESPACE)
                    .endObject()
                    .startObject(PROPERTIES)
                        .startObject(ModelSizeStats.MODEL_BYTES)
                            .field(TYPE, LONG)
                        .endObject()
                        .startObject(ModelSizeStats.TOTAL_BY_FIELD_COUNT)
                            .field(TYPE, LONG)
                        .endObject()
                        .startObject(ModelSizeStats.TOTAL_OVER_FIELD_COUNT)
                            .field(TYPE, LONG)
                        .endObject()
                        .startObject(ModelSizeStats.TOTAL_PARTITION_FIELD_COUNT)
                            .field(TYPE, LONG)
                        .endObject()
                        .startObject(ModelSizeStats.BUCKET_ALLOCATION_FAILURES_COUNT)
                            .field(TYPE, LONG)
                        .endObject()
                        .startObject(ModelSizeStats.MEMORY_STATUS)
                            .field(TYPE, KEYWORD)
                        .endObject()
                        .startObject(ES_TIMESTAMP)
                            .field(TYPE, DATE)
                        .endObject()
                        .startObject(ModelSizeStats.LOG_TIME)
                            .field(TYPE, DATE)
                        .endObject()
                    .endObject()
                .endObject()
            .endObject();
    }


    /**
     * The Elasticsearch mappings for the usage documents
     *
     * @return
     * @throws IOException
     */
    public static XContentBuilder usageMapping()
    throws IOException
    {
        return jsonBuilder()
            .startObject()
                .startObject(Usage.TYPE)
                    .startObject(ALL)
                        .field(ENABLED, false)
                        // analyzer must be specified even though _all is disabled
                        // because all types in the same index must have the same
                        // analyzer for a given field
                        .field(ANALYZER, WHITESPACE)
                    .endObject()
                    .startObject(PROPERTIES)
                        .startObject(ES_TIMESTAMP)
                            .field(TYPE, DATE)
                        .endObject()
                        .startObject(Usage.INPUT_BYTES)
                            .field(TYPE, LONG)
                        .endObject()
                        .startObject(Usage.INPUT_FIELD_COUNT)
                            .field(TYPE, LONG)
                        .endObject()
                        .startObject(Usage.INPUT_RECORD_COUNT)
                            .field(TYPE, LONG)
                        .endObject()
                    .endObject()
                .endObject()
            .endObject();
    }

    /**
     * Mapping for the saved data records
     *
     * @return
     * @throws IOException
     */
    public static XContentBuilder inputDataMapping()
    throws IOException
    {
        return jsonBuilder()
            .startObject()
                .startObject(ElasticsearchJobDataPersister.TYPE)
                    .startObject(ALL)
                        .field(ENABLED, false)
                        // analyzer must be specified even though _all is disabled
                        // because all types in the same index must have the same
                        // analyzer for a given field
                        .field(ANALYZER, WHITESPACE)
                    .endObject()
                    .startObject(PROPERTIES)
                        .startObject(ES_TIMESTAMP)
                            .field(TYPE, DATE)
                        .endObject()
                        .startObject(ElasticsearchJobDataPersister.FIELDS)
                            .field(TYPE, KEYWORD)
                            .field(COPY_TO, "field")
                        .endObject()
                        .startObject(ElasticsearchJobDataPersister.BY_FIELDS)
                            .field(TYPE, KEYWORD)
                            .field(COPY_TO, "byField")
                        .endObject()
                        .startObject(ElasticsearchJobDataPersister.OVER_FIELDS)
                            .field(TYPE, KEYWORD)
                            .field(COPY_TO, "overField")
                        .endObject()
                        .startObject(ElasticsearchJobDataPersister.PARTITION_FIELDS)
                            .field(TYPE, KEYWORD)
                            .field(COPY_TO, "partitionField")
                        .endObject()
                    .endObject()
                .endObject()
            .endObject();
    }

    /**
     * Mapping for model debug output
     *
     * @param termFieldNames Optionally, other field names to include in the
     * mappings.  Pass <code>null</code> if not required.
     * @return
     * @throws IOException
     */
    public static XContentBuilder modelDebugOutputMapping(Collection<String> termFieldNames)
    throws IOException
    {
        XContentBuilder builder = jsonBuilder()
            .startObject()
                .startObject(ModelDebugOutput.TYPE)
                    .startObject(ALL)
                        .field(ANALYZER, WHITESPACE)
                    .endObject()
                    .startObject(PROPERTIES)
                        .startObject(ElasticsearchPersister.JOB_ID_NAME)
                            .field(TYPE, KEYWORD).field(INCLUDE_IN_ALL, false)
                        .endObject()
                        .startObject(ES_TIMESTAMP)
                            .field(TYPE, DATE).field(INCLUDE_IN_ALL, false)
                        .endObject()
                        .startObject(ModelDebugOutput.PARTITION_FIELD_VALUE)
                            .field(TYPE, KEYWORD)
                        .endObject()
                        .startObject(ModelDebugOutput.OVER_FIELD_VALUE)
                            .field(TYPE, KEYWORD)
                        .endObject()
                        .startObject(ModelDebugOutput.BY_FIELD_VALUE)
                            .field(TYPE, KEYWORD)
                        .endObject()
                        .startObject(ModelDebugOutput.DEBUG_FEATURE)
                            .field(TYPE, KEYWORD).field(INCLUDE_IN_ALL, false)
                        .endObject()
                        .startObject(ModelDebugOutput.DEBUG_LOWER)
                            .field(TYPE, DOUBLE).field(INCLUDE_IN_ALL, false)
                        .endObject()
                        .startObject(ModelDebugOutput.DEBUG_UPPER)
                            .field(TYPE, DOUBLE).field(INCLUDE_IN_ALL, false)
                        .endObject()
                        .startObject(ModelDebugOutput.DEBUG_MEDIAN)
                            .field(TYPE, DOUBLE).field(INCLUDE_IN_ALL, false)
                        .endObject()
                        .startObject(ModelDebugOutput.ACTUAL)
                            .field(TYPE, DOUBLE).field(INCLUDE_IN_ALL, false)
                        .endObject();

        if (termFieldNames != null)
        {
            ElasticsearchDotNotationReverser reverser = new ElasticsearchDotNotationReverser();
            for (String fieldName : termFieldNames)
            {
                reverser.add(fieldName, "");
            }
            for (Map.Entry<String, Object> entry : reverser.getMappingsMap().entrySet())
            {
                builder.field(entry.getKey(), entry.getValue());
            }
        }

        return builder
                    .endObject()
                .endObject()
            .endObject();
    }

    /**
     * Influence results mapping
     * @param influencerFieldNames Optionally, other field names to include in the
     * mappings.  Pass <code>null</code> if not required.
     * @return
     * @throws IOException
     */
    public static XContentBuilder influencerMapping(Collection<String> influencerFieldNames)
    throws IOException
    {
        XContentBuilder builder = jsonBuilder()
            .startObject()
                .startObject(Influencer.TYPE)
                    .startObject(ALL)
                        .field(ANALYZER, WHITESPACE)
                    .endObject()
                    .startObject(PROPERTIES)
                        .startObject(ElasticsearchPersister.JOB_ID_NAME)
                            .field(TYPE, KEYWORD).field(INCLUDE_IN_ALL, false)
                        .endObject()
                        .startObject(ES_TIMESTAMP)
                            .field(TYPE, DATE).field(INCLUDE_IN_ALL, false)
                        .endObject()
                        .startObject(Influencer.PROBABILITY)
                            .field(TYPE, DOUBLE).field(INCLUDE_IN_ALL, false)
                        .endObject()
                        .startObject(Influencer.INITIAL_ANOMALY_SCORE)
                            .field(TYPE, DOUBLE).field(INCLUDE_IN_ALL, false)
                        .endObject()
                        .startObject(Influencer.ANOMALY_SCORE)
                            .field(TYPE, DOUBLE).field(INCLUDE_IN_ALL, false)
                        .endObject()
                        .startObject(Influencer.INFLUENCER_FIELD_NAME)
                            .field(TYPE, KEYWORD).field(INCLUDE_IN_ALL, false)
                        .endObject()
                        .startObject(Influencer.INFLUENCER_FIELD_VALUE)
                            .field(TYPE, KEYWORD)
                        .endObject()
                        .startObject(Bucket.IS_INTERIM)
                            .field(TYPE, BOOLEAN).field(INCLUDE_IN_ALL, false)
                        .endObject();

        if (influencerFieldNames != null)
        {
            ElasticsearchDotNotationReverser reverser = new ElasticsearchDotNotationReverser();
            for (String fieldName : influencerFieldNames)
            {
                reverser.add(fieldName, "");
            }
            for (Map.Entry<String, Object> entry : reverser.getMappingsMap().entrySet())
            {
                builder.field(entry.getKey(), entry.getValue());
            }
        }

        return builder
                    .endObject()
                .endObject()
            .endObject();
    }

    public static XContentBuilder auditMessageMapping() throws IOException
    {
        return jsonBuilder()
                .startObject()
                    .startObject(AuditMessage.TYPE)
                        .startObject(PROPERTIES)
                            .startObject(ES_TIMESTAMP)
                                .field(TYPE, DATE)
                            .endObject()
                            .startObject(AuditMessage.JOB_ID)
                                .field(TYPE, KEYWORD)
                            .endObject()
                            .startObject(AuditMessage.LEVEL)
                                .field(TYPE, KEYWORD)
                            .endObject()
                            .startObject(AuditMessage.MESSAGE)
                                .field(TYPE, TEXT)
                                .startObject(FIELDS)
                                    .startObject(RAW)
                                        .field(TYPE, KEYWORD)
                                    .endObject()
                                .endObject()
                            .endObject()
                        .endObject()
                    .endObject()
                .endObject();
    }

    public static XContentBuilder auditActivityMapping() throws IOException
    {
        return jsonBuilder()
                .startObject()
                    .startObject(AuditActivity.TYPE)
                        .startObject(PROPERTIES)
                            .startObject(ES_TIMESTAMP)
                                .field(TYPE, DATE)
                            .endObject()
                            .startObject(AuditActivity.RUNNING_DETECTORS)
                                .field(TYPE, INTEGER)
                            .endObject()
                            .startObject(AuditActivity.RUNNING_JOBS)
                                .field(TYPE, INTEGER)
                            .endObject()
                            .startObject(AuditActivity.TOTAL_DETECTORS)
                                .field(TYPE, INTEGER)
                            .endObject()
                            .startObject(AuditActivity.TOTAL_JOBS)
                                .field(TYPE, INTEGER)
                            .endObject()
                        .endObject()
                    .endObject()
                .endObject();
    }

    public static XContentBuilder listDocumentMapping() throws IOException
    {
        return jsonBuilder()
                .startObject()
                    .startObject(ListDocument.TYPE)
                        .startObject(PROPERTIES)
                            .startObject(ListDocument.ID)
                                .field(TYPE, KEYWORD)
                            .endObject()
                            .startObject(ListDocument.ITEMS)
                                .field(TYPE, TEXT)
                            .endObject()
                        .endObject()
                    .endObject()
                .endObject();
    }

    public static XContentBuilder processingTimeMapping() throws IOException
    {
        return jsonBuilder()
                .startObject()
                    .startObject(BucketProcessingTime.TYPE)
                        .startObject(ALL)
                            .field(ENABLED, false)
                            // analyzer must be specified even though _all is disabled
                            // because all types in the same index must have the same
                            // analyzer for a given field
                            .field(ANALYZER, WHITESPACE)
                        .endObject()
                        .startObject(PROPERTIES)
                            .startObject(BucketProcessingTime.AVERAGE_PROCESSING_TIME_MS)
                                .field(TYPE, DOUBLE)
                            .endObject()
                        .endObject()
                    .endObject()
                .endObject();
    }
}
