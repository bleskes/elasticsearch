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

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;

import org.elasticsearch.common.xcontent.XContentBuilder;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.AnalysisLimits;
import com.prelert.job.CategorizerState;
import com.prelert.job.DataCounts;
import com.prelert.job.DataDescription;
import com.prelert.job.Detector;
import com.prelert.job.JobDetails;
import com.prelert.job.ModelDebugConfig;
import com.prelert.job.ModelSizeStats;
import com.prelert.job.ModelState;
import com.prelert.job.quantiles.Quantiles;
import com.prelert.job.results.AnomalyCause;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.CategoryDefinition;
import com.prelert.job.results.Influence;
import com.prelert.job.results.Influencer;
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
 */
public class ElasticsearchMappings
{
    /**
     * String constants used in mappings
     */
    public static final String NOT_ANALYZED = "not_analyzed";
    public static final String INDEX = "index";
    public static final String NO = "no";

    /**
     * Name of the field used to store the timestamp in Elasticsearch.
     * Note the field name is different to {@link com.prelert.job.results.Bucket#TIMESTAMP} used by the
     * API Bucket Resource, and is chosen for consistency with the default field name used by
     * Logstash and Kibana.
     */
    public static final String ES_TIMESTAMP = "@timestamp";

    private static final String DATE = "date";
    private static final String LONG = "long";
    private static final String OBJECT = "object";
    private static final String PROPERTIES = "properties";
    private static final String STRING = "string";
    private static final String DOUBLE = "double";
    private static final String BOOLEAN = "boolean";
    private static final String TYPE = "type";

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
                    .startObject(PROPERTIES)
                        .startObject(JobDetails.ID)
                            .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                        .endObject()
                        .startObject(JobDetails.DESCRIPTION)
                            .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                        .endObject()
                        .startObject(JobDetails.STATUS)
                            .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                        .endObject()
                        .startObject(JobDetails.CREATE_TIME)
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
                        .startObject(ModelSizeStats.TYPE)
                            .field(TYPE, OBJECT)
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
                                    .field(TYPE, STRING)
                                .endObject()
                            .endObject()
                        .endObject()
                        .startObject(JobDetails.TIMEOUT)
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
                                    .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                                .endObject()
                                .startObject(AnalysisConfig.CATEGORIZATION_FIELD_NAME)
                                    .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                                .endObject()
                                .startObject(AnalysisConfig.DETECTORS)
                                    .startObject(PROPERTIES)
                                        .startObject(Detector.FUNCTION)
                                            .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                                        .endObject()
                                        .startObject(Detector.FIELD_NAME)
                                            .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                                        .endObject()
                                        .startObject(Detector.BY_FIELD_NAME)
                                            .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                                        .endObject()
                                        .startObject(Detector.OVER_FIELD_NAME)
                                            .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                                        .endObject()
                                        .startObject(Detector.PARTITION_FIELD_NAME)
                                            .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                                        .endObject()
                                        .startObject(Detector.USE_NULL)
                                            .field(TYPE, BOOLEAN)
                                        .endObject()
                                    .endObject()
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
                                    .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                                .endObject()
                                .startObject(DataDescription.TIME_FIELD_NAME)
                                    .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                                .endObject()
                                .startObject(DataDescription.TIME_FORMAT)
                                    .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                                .endObject()
                                .startObject(DataDescription.FIELD_DELIMITER)
                                    .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                                .endObject()
                                .startObject(DataDescription.QUOTE_CHARACTER)
                                    .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                                .endObject()
                            .endObject()
                        .endObject()
                        .startObject(JobDetails.TRANSFORMS)
                            .field(TYPE, OBJECT)
                            .startObject(PROPERTIES)
                                .startObject(TransformConfig.TRANSFORM)
                                    .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                                .endObject()
                                .startObject(TransformConfig.ARGUMENTS)
                                    .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                                .endObject()
                                .startObject(TransformConfig.INPUTS)
                                    .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                                .endObject()
                                .startObject(TransformConfig.OUTPUTS)
                                    .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                                .endObject()
                            .endObject()
                        .endObject()
                        .startObject(JobDetails.MODEL_DEBUG_CONFIG)
                            .field(TYPE, OBJECT)
                            .startObject(PROPERTIES)
                                .startObject(ModelDebugConfig.BOUNDS_PERCENTILE)
                                    .field(TYPE, DOUBLE).field(INDEX, NO)
                                .endObject()
                                .startObject(ModelDebugConfig.TERMS)
                                    .field(TYPE, STRING).field(INDEX, NO)
                                .endObject()
                            .endObject()
                        .endObject()
                    .endObject()
                    .startObject("_all")
                        .field("enabled", "false")
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
                    .startObject("_all")
                        .field("enabled", false)
                    .endObject()
                    .startObject(PROPERTIES)
                        .startObject(Bucket.ID)
                            .field(TYPE, STRING)
                        .endObject()
                        .startObject(ES_TIMESTAMP)
                            .field(TYPE, DATE)
                        .endObject()
                        .startObject(Bucket.RAW_ANOMALY_SCORE)
                            .field(TYPE, DOUBLE)
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
                    .endObject()
                .endObject()
            .endObject();
    }

    public static XContentBuilder categorizerStateMapping() throws IOException
    {
        return jsonBuilder()
                .startObject()
                    .startObject(CategorizerState.TYPE)
                        .field("enabled", false)
                        .startObject("_all")
                            .field("enabled", false)
                        .endObject()
                    .endObject()
                .endObject();
    }

    public static XContentBuilder categoryDefinitionMapping() throws IOException
    {
        return jsonBuilder()
                .startObject()
                    .startObject(CategoryDefinition.TYPE)
                        .startObject("_all")
                            .field("enabled", false)
                        .endObject()
                        .startObject(PROPERTIES)
                            .startObject(CategoryDefinition.CATEGORY_ID)
                                .field(TYPE, LONG)
                            .endObject()
                            .startObject(CategoryDefinition.TERMS)
                                .field(TYPE, STRING).field(INDEX, NO)
                            .endObject()
                            .startObject(CategoryDefinition.REGEX)
                                .field(TYPE, STRING).field(INDEX, NO)
                            .endObject()
                            .startObject(CategoryDefinition.EXAMPLES)
                                .field(TYPE, STRING).field(INDEX, NO)
                            .endObject()
                        .endObject()
                    .endObject()
                .endObject();
    }

    /**
     * Create the Elasticsearch mapping for {@linkplain com.prelert.job.results.Detector}.
     * The '_all' field is disabled as the document isn't meant to be searched.
     *
     * @return
     * @throws IOException
     */
    public static XContentBuilder detectorMapping()
    throws IOException
    {
        return jsonBuilder()
            .startObject()
                .startObject(com.prelert.job.results.Detector.TYPE)
                    .startObject("_all")
                        .field("enabled", false)
                    .endObject()
                    .startObject(PROPERTIES)
                        .startObject(com.prelert.job.results.Detector.NAME)
                            .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                        .endObject()
                    .endObject()
                .endObject()
            .endObject();
    }


    /**
     * Create the Elasticsearch mapping for {@linkplain com.prelert.job.results.Detector}.
     * The '_all' field is disabled as the document isn't meant to be searched.
     * Records have a _parent mapping to a {@linkplain com.prelert.job.results.Bucket}.
     *
     * @return
     * @throws IOException
     */
    public static XContentBuilder recordMapping()
    throws IOException
    {
        return jsonBuilder()
            .startObject()
                .startObject(AnomalyRecord.TYPE)
                    .startObject("_all")
                        .field("enabled", false)
                    .endObject()
                    .startObject("_parent")
                        .field(TYPE, Bucket.TYPE)
                    .endObject()
                    .startObject(PROPERTIES)
                        .startObject(ES_TIMESTAMP)
                            .field(TYPE, DATE)
                        .endObject()
                        .startObject(AnomalyRecord.ACTUAL)
                            .field(TYPE, DOUBLE)
                        .endObject()
                        .startObject(AnomalyRecord.TYPICAL)
                            .field(TYPE, DOUBLE)
                        .endObject()
                        .startObject(AnomalyRecord.PROBABILITY)
                            .field(TYPE, DOUBLE)
                        .endObject()
                        .startObject(AnomalyRecord.FUNCTION)
                            .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                        .endObject()
                        .startObject(AnomalyRecord.FUNCTION_DESCRIPTION)
                            .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                        .endObject()
                        .startObject(AnomalyRecord.BY_FIELD_NAME)
                            .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                        .endObject()
                        .startObject(AnomalyRecord.BY_FIELD_VALUE)
                            .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                        .endObject()
                        .startObject(AnomalyRecord.FIELD_NAME)
                            .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                        .endObject()
                        .startObject(AnomalyRecord.PARTITION_FIELD_NAME)
                            .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                        .endObject()
                        .startObject(AnomalyRecord.PARTITION_FIELD_VALUE)
                            .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                        .endObject()
                        .startObject(AnomalyRecord.OVER_FIELD_NAME)
                            .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                        .endObject()
                        .startObject(AnomalyRecord.OVER_FIELD_VALUE)
                            .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                        .endObject()
                        .startObject(AnomalyRecord.CAUSES)
                            .startObject(PROPERTIES)
                                .startObject(AnomalyCause.ACTUAL)
                                    .field(TYPE, DOUBLE)
                                .endObject()
                                .startObject(AnomalyCause.TYPICAL)
                                    .field(TYPE, DOUBLE)
                                .endObject()
                                .startObject(AnomalyCause.PROBABILITY)
                                    .field(TYPE, DOUBLE)
                                .endObject()
                                .startObject(AnomalyCause.FUNCTION)
                                    .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                                .endObject()
                                .startObject(AnomalyCause.FUNCTION_DESCRIPTION)
                                    .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                                .endObject()
                                .startObject(AnomalyCause.BY_FIELD_NAME)
                                    .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                                .endObject()
                                .startObject(AnomalyCause.BY_FIELD_VALUE)
                                    .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                                .endObject()
                                .startObject(AnomalyCause.FIELD_NAME)
                                    .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                                .endObject()
                                .startObject(AnomalyCause.PARTITION_FIELD_NAME)
                                    .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                                .endObject()
                                .startObject(AnomalyCause.PARTITION_FIELD_VALUE)
                                    .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                                .endObject()
                                .startObject(AnomalyCause.OVER_FIELD_NAME)
                                    .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                                .endObject()
                                .startObject(AnomalyCause.OVER_FIELD_VALUE)
                                    .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                                .endObject()
                            .endObject()
                        .endObject()
                        .startObject(AnomalyRecord.ANOMALY_SCORE)
                            .field(TYPE, DOUBLE)
                        .endObject()
                        .startObject(AnomalyRecord.NORMALIZED_PROBABILITY)
                            .field(TYPE, DOUBLE)
                        .endObject()
                        .startObject(AnomalyRecord.INITIAL_NORMALIZED_PROBABILITY)
                            .field(TYPE, "double")
                        .endObject()
                        .startObject(AnomalyRecord.IS_INTERIM)
                            .field(TYPE, BOOLEAN)
                        .endObject()
                        .startObject(AnomalyRecord.INFLUENCES)
                            /* Array of influences */
                            .field(TYPE, "nested")
                            .startObject(PROPERTIES)
                                .startObject(Influence.INFLUENCE_FIELD_NAME)
                                    .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                                .endObject()
                                .startObject(Influence.INFLUENCE_FIELD_VALUES)
                                    .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                                .endObject()
                            .endObject()
                        .endObject()
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
                    .startObject("_all")
                        .field("enabled", false)
                    .endObject()
                    .startObject(PROPERTIES)
                        .startObject(Quantiles.ID)
                            .field(TYPE, STRING).field(INDEX, NO)
                        .endObject()
                        .startObject(Quantiles.VERSION)
                            .field(TYPE, STRING).field(INDEX, NO)
                        .endObject()
                        .startObject(Quantiles.QUANTILE_STATE)
                            .field(TYPE, STRING).field(INDEX, NO)
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
                    .field("enabled", false)
                    .startObject("_all")
                        .field("enabled", false)
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
                    .startObject("_all")
                        .field("enabled",  false)
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
                            .field(TYPE, STRING)
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
                    .startObject("_all")
                        .field("enabled", false)
                    .endObject()
                    .startObject(PROPERTIES)
                        .startObject(Usage.TIMESTAMP)
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
                    .startObject("_all")
                        .field("enabled", false)
                    .endObject()
                    .startObject(PROPERTIES)
                        .startObject("epoch")
                            .field(TYPE, DATE)
                        .endObject()
                        .startObject(ElasticsearchJobDataPersister.FIELDS)
                            .field(TYPE, STRING)
                            .field("index_name", "field")
                            .field(INDEX, NOT_ANALYZED)
                        .endObject()
                        .startObject(ElasticsearchJobDataPersister.BY_FIELDS)
                            .field(TYPE, STRING)
                            .field("index_name", "byField")
                            .field(INDEX, NOT_ANALYZED)
                        .endObject()
                        .startObject(ElasticsearchJobDataPersister.OVER_FIELDS)
                            .field(TYPE, STRING)
                            .field("index_name", "overField")
                            .field(INDEX, NOT_ANALYZED)
                        .endObject()
                        .startObject(ElasticsearchJobDataPersister.PARTITION_FIELDS)
                            .field(TYPE, STRING)
                            .field("index_name", "partitionField")
                            .field(INDEX, NOT_ANALYZED)
                        .endObject()
                    .endObject()
                .endObject()
            .endObject();
    }

    /**
     * Influence results mapping
     * @return
     * @throws IOException
     */
    public static XContentBuilder influencerMapping()
    throws IOException
    {
        return jsonBuilder()
            .startObject()
                .startObject(Influencer.TYPE)
                    .startObject("_all")
                        .field("enabled", false)
                    .endObject()
                    .startObject(PROPERTIES)
                        .startObject(Influencer.TIMESTAMP)
                            .field(TYPE, DATE)
                        .endObject()
                        .startObject(Influencer.PROBABILITY)
                            .field(TYPE, DOUBLE)
                        .endObject()
                        .startObject(Influencer.INITIAL_ANOMALY_SCORE)
                            .field(TYPE, DOUBLE)
                        .endObject()
                        .startObject(Influencer.ANOMALY_SCORE)
                            .field(TYPE, DOUBLE)
                        .endObject()
                        .startObject(Influencer.INFLUENCER_FIELD_NAME)
                            .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                        .endObject()
                        .startObject(Influencer.INFLUENCER_FIELD_NAME)
                            .field(TYPE, STRING).field(INDEX, NOT_ANALYZED)
                        .endObject()
                    .endObject()
                .endObject()
            .endObject();
    }

}
