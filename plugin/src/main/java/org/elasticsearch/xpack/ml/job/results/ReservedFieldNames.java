/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.ml.job.results;

import org.elasticsearch.xpack.ml.job.process.autodetect.state.DataCounts;
import org.elasticsearch.xpack.ml.job.config.Job;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.ModelSizeStats;
import org.elasticsearch.xpack.ml.job.process.autodetect.state.ModelSnapshot;
import org.elasticsearch.xpack.ml.job.persistence.ElasticsearchMappings;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;


/**
 * Defines the field names that we use for our results.
 * Fields from the raw data with these names are not added to any result.  Even
 * different types of results will not have raw data fields with reserved names
 * added to them, as it could create confusion if in some results a given field
 * contains raw data and in others it contains some aspect of our output.
 */
public final class ReservedFieldNames {
    private static final Pattern DOT_PATTERN = Pattern.compile("\\.");

    /**
     * This array should be updated to contain all the field names that appear
     * in any documents we store in our results index.  (The reason it's any
     * documents we store and not just results documents is that Elasticsearch
     * 2.x requires mappings for given fields be consistent across all types
     * in a given index.)
     */
    private static final String[] RESERVED_FIELD_NAME_ARRAY = {
            ElasticsearchMappings.ALL_FIELD_VALUES,

            Job.ID.getPreferredName(),

            AnomalyCause.PROBABILITY.getPreferredName(),
            AnomalyCause.OVER_FIELD_NAME.getPreferredName(),
            AnomalyCause.OVER_FIELD_VALUE.getPreferredName(),
            AnomalyCause.BY_FIELD_NAME.getPreferredName(),
            AnomalyCause.BY_FIELD_VALUE.getPreferredName(),
            AnomalyCause.CORRELATED_BY_FIELD_VALUE.getPreferredName(),
            AnomalyCause.PARTITION_FIELD_NAME.getPreferredName(),
            AnomalyCause.PARTITION_FIELD_VALUE.getPreferredName(),
            AnomalyCause.FUNCTION.getPreferredName(),
            AnomalyCause.FUNCTION_DESCRIPTION.getPreferredName(),
            AnomalyCause.TYPICAL.getPreferredName(),
            AnomalyCause.ACTUAL.getPreferredName(),
            AnomalyCause.INFLUENCERS.getPreferredName(),
            AnomalyCause.FIELD_NAME.getPreferredName(),

            AnomalyRecord.DETECTOR_INDEX.getPreferredName(),
            AnomalyRecord.PROBABILITY.getPreferredName(),
            AnomalyRecord.BY_FIELD_NAME.getPreferredName(),
            AnomalyRecord.BY_FIELD_VALUE.getPreferredName(),
            AnomalyRecord.CORRELATED_BY_FIELD_VALUE.getPreferredName(),
            AnomalyRecord.PARTITION_FIELD_NAME.getPreferredName(),
            AnomalyRecord.PARTITION_FIELD_VALUE.getPreferredName(),
            AnomalyRecord.FUNCTION.getPreferredName(),
            AnomalyRecord.FUNCTION_DESCRIPTION.getPreferredName(),
            AnomalyRecord.TYPICAL.getPreferredName(),
            AnomalyRecord.ACTUAL.getPreferredName(),
            AnomalyRecord.INFLUENCERS.getPreferredName(),
            AnomalyRecord.FIELD_NAME.getPreferredName(),
            AnomalyRecord.OVER_FIELD_NAME.getPreferredName(),
            AnomalyRecord.OVER_FIELD_VALUE.getPreferredName(),
            AnomalyRecord.CAUSES.getPreferredName(),
            AnomalyRecord.RECORD_SCORE.getPreferredName(),
            AnomalyRecord.INITIAL_RECORD_SCORE.getPreferredName(),
            AnomalyRecord.BUCKET_SPAN.getPreferredName(),
            AnomalyRecord.SEQUENCE_NUM.getPreferredName(),

            Bucket.ANOMALY_SCORE.getPreferredName(),
            Bucket.BUCKET_INFLUENCERS.getPreferredName(),
            Bucket.BUCKET_SPAN.getPreferredName(),
            Bucket.RECORD_COUNT.getPreferredName(),
            Bucket.EVENT_COUNT.getPreferredName(),
            Bucket.INITIAL_ANOMALY_SCORE.getPreferredName(),
            Bucket.PROCESSING_TIME_MS.getPreferredName(),
            Bucket.PARTITION_SCORES.getPreferredName(),

            BucketInfluencer.INITIAL_ANOMALY_SCORE.getPreferredName(), BucketInfluencer.ANOMALY_SCORE.getPreferredName(),
            BucketInfluencer.RAW_ANOMALY_SCORE.getPreferredName(), BucketInfluencer.PROBABILITY.getPreferredName(),

            CategoryDefinition.CATEGORY_ID.getPreferredName(),
            CategoryDefinition.TERMS.getPreferredName(),
            CategoryDefinition.REGEX.getPreferredName(),
            CategoryDefinition.MAX_MATCHING_LENGTH.getPreferredName(),
            CategoryDefinition.EXAMPLES.getPreferredName(),

            DataCounts.PROCESSED_RECORD_COUNT.getPreferredName(),
            DataCounts.PROCESSED_FIELD_COUNT.getPreferredName(),
            DataCounts.INPUT_BYTES.getPreferredName(),
            DataCounts.INPUT_RECORD_COUNT.getPreferredName(),
            DataCounts.INPUT_FIELD_COUNT.getPreferredName(),
            DataCounts.INVALID_DATE_COUNT.getPreferredName(),
            DataCounts.MISSING_FIELD_COUNT.getPreferredName(),
            DataCounts.OUT_OF_ORDER_TIME_COUNT.getPreferredName(),
            DataCounts.EMPTY_BUCKET_COUNT.getPreferredName(),
            DataCounts.SPARSE_BUCKET_COUNT.getPreferredName(),
            DataCounts.BUCKET_COUNT.getPreferredName(),
            DataCounts.LATEST_RECORD_TIME.getPreferredName(),
            DataCounts.EARLIEST_RECORD_TIME.getPreferredName(),
            DataCounts.LAST_DATA_TIME.getPreferredName(),
            DataCounts.LATEST_EMPTY_BUCKET_TIME.getPreferredName(),
            DataCounts.LATEST_SPARSE_BUCKET_TIME.getPreferredName(),

            Influence.INFLUENCER_FIELD_NAME.getPreferredName(),
            Influence.INFLUENCER_FIELD_VALUES.getPreferredName(),

            Influencer.PROBABILITY.getPreferredName(),
            Influencer.INFLUENCER_FIELD_NAME.getPreferredName(),
            Influencer.INFLUENCER_FIELD_VALUE.getPreferredName(),
            Influencer.INITIAL_INFLUENCER_SCORE.getPreferredName(),
            Influencer.INFLUENCER_SCORE.getPreferredName(),
            Influencer.BUCKET_SPAN.getPreferredName(),
            Influencer.SEQUENCE_NUM.getPreferredName(),

            ModelPlot.PARTITION_FIELD_NAME.getPreferredName(), ModelPlot.PARTITION_FIELD_VALUE.getPreferredName(),
            ModelPlot.OVER_FIELD_NAME.getPreferredName(), ModelPlot.OVER_FIELD_VALUE.getPreferredName(),
            ModelPlot.BY_FIELD_NAME.getPreferredName(), ModelPlot.BY_FIELD_VALUE.getPreferredName(),
            ModelPlot.MODEL_FEATURE.getPreferredName(), ModelPlot.MODEL_LOWER.getPreferredName(),
            ModelPlot.MODEL_UPPER.getPreferredName(), ModelPlot.MODEL_MEDIAN.getPreferredName(),
            ModelPlot.ACTUAL.getPreferredName(),

            ModelSizeStats.MODEL_BYTES_FIELD.getPreferredName(),
            ModelSizeStats.TOTAL_BY_FIELD_COUNT_FIELD.getPreferredName(),
            ModelSizeStats.TOTAL_OVER_FIELD_COUNT_FIELD.getPreferredName(),
            ModelSizeStats.TOTAL_PARTITION_FIELD_COUNT_FIELD.getPreferredName(),
            ModelSizeStats.BUCKET_ALLOCATION_FAILURES_COUNT_FIELD.getPreferredName(),
            ModelSizeStats.MEMORY_STATUS_FIELD.getPreferredName(),
            ModelSizeStats.LOG_TIME_FIELD.getPreferredName(),

            ModelSnapshot.DESCRIPTION.getPreferredName(),
            ModelSnapshot.SNAPSHOT_ID.getPreferredName(),
            ModelSnapshot.SNAPSHOT_DOC_COUNT.getPreferredName(),
            ModelSnapshot.LATEST_RECORD_TIME.getPreferredName(),
            ModelSnapshot.LATEST_RESULT_TIME.getPreferredName(),
            ModelSnapshot.RETAIN.getPreferredName(),

            PerPartitionMaxProbabilities.PER_PARTITION_MAX_PROBABILITIES.getPreferredName(),
            PerPartitionMaxProbabilities.MAX_RECORD_SCORE.getPreferredName(),

            Result.RESULT_TYPE.getPreferredName(),
            Result.TIMESTAMP.getPreferredName(),
            Result.IS_INTERIM.getPreferredName()
    };

    /**
     * Test if fieldName is one of the reserved names or if it contains dots then
     * that the segment before the first dot is not a reserved name. A fieldName
     * containing dots represents nested fields in which case we only care about
     * the top level.
     *
     * @param fieldName Document field name. This may contain dots '.'
     * @return True if fieldName is not a reserved name or the top level segment
     * is not a reserved name.
     */
    public static boolean isValidFieldName(String fieldName) {
        String[] segments = DOT_PATTERN.split(fieldName);
        return !RESERVED_FIELD_NAMES.contains(segments[0]);
    }

    /**
     * A set of all reserved field names in our results.  Fields from the raw
     * data with these names are not added to any result.
     */
    public static final Set<String> RESERVED_FIELD_NAMES = new HashSet<>(Arrays.asList(RESERVED_FIELD_NAME_ARRAY));

    private ReservedFieldNames() {
    }
}
