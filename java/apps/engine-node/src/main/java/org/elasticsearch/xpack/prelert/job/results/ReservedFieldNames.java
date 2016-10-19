
package org.elasticsearch.xpack.prelert.job.results;

import org.elasticsearch.xpack.prelert.job.*;
import org.elasticsearch.xpack.prelert.job.detectionrules.DetectionRule;
import org.elasticsearch.xpack.prelert.job.detectionrules.RuleCondition;
import org.elasticsearch.xpack.prelert.job.quantiles.Quantiles;
import org.elasticsearch.xpack.prelert.job.transform.TransformConfig;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


/**
 * Defines the field names that we use for our results.
 * Fields from the raw data with these names are not added to any result.  Even
 * different types of results will not have raw data fields with reserved names
 * added to them, as it could create confusion if in some results a given field
 * contains raw data and in others it contains some aspect of our output.
 */
public final class ReservedFieldNames
{
    /**
     * jobId isn't in this package, so redefine.
     */
    private static final String JOB_ID_NAME = "jobId";

    /**
     * @timestamp isn't in this package, so redefine.
     */
    private static final String ES_TIMESTAMP = "@timestamp";

    /**
     * This array should be updated to contain all the field names that appear
     * in any documents we store in our results index.  (The reason it's any
     * documents we store and not just results documents is that Elasticsearch
     * 2.x requires mappings for given fields be consistent across all types
     * in a given index.)
     */
    private static final String[] RESERVED_FIELD_NAME_ARRAY = {

            AnalysisConfig.BUCKET_SPAN.getPreferredName(),
            AnalysisConfig.BATCH_SPAN.getPreferredName(),
            AnalysisConfig.LATENCY.getPreferredName(),
            AnalysisConfig.PERIOD.getPreferredName(),
            AnalysisConfig.SUMMARY_COUNT_FIELD_NAME.getPreferredName(),
            AnalysisConfig.CATEGORIZATION_FIELD_NAME.getPreferredName(),
            AnalysisConfig.CATEGORIZATION_FILTERS.getPreferredName(),
            AnalysisConfig.DETECTORS.getPreferredName(),
            AnalysisConfig.INFLUENCERS.getPreferredName(),
            AnalysisConfig.OVERLAPPING_BUCKETS.getPreferredName(),
            AnalysisConfig.RESULT_FINALIZATION_WINDOW.getPreferredName(),
            AnalysisConfig.MULTIVARIATE_BY_FIELDS.getPreferredName(),

        AnalysisLimits.MODEL_MEMORY_LIMIT.getPreferredName(),
        AnalysisLimits.CATEGORIZATION_EXAMPLES_LIMIT.getPreferredName(),

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

        AnomalyRecord.DETECTOR_INDEX,
        AnomalyRecord.PROBABILITY,
        AnomalyRecord.BY_FIELD_NAME,
        AnomalyRecord.BY_FIELD_VALUE,
        AnomalyRecord.CORRELATED_BY_FIELD_VALUE,
        AnomalyRecord.PARTITION_FIELD_NAME,
        AnomalyRecord.PARTITION_FIELD_VALUE,
        AnomalyRecord.FUNCTION,
        AnomalyRecord.FUNCTION_DESCRIPTION,
        AnomalyRecord.TYPICAL,
        AnomalyRecord.ACTUAL,
        AnomalyRecord.IS_INTERIM,
        AnomalyRecord.INFLUENCERS,
        AnomalyRecord.FIELD_NAME,
        AnomalyRecord.OVER_FIELD_NAME,
        AnomalyRecord.OVER_FIELD_VALUE,
        AnomalyRecord.CAUSES,
        AnomalyRecord.ANOMALY_SCORE,
        AnomalyRecord.NORMALIZED_PROBABILITY,
        AnomalyRecord.INITIAL_NORMALIZED_PROBABILITY,
        AnomalyRecord.BUCKET_SPAN,

        Bucket.ANOMALY_SCORE,
        Bucket.BUCKET_SPAN,
        Bucket.MAX_NORMALIZED_PROBABILITY,
        Bucket.IS_INTERIM,
        Bucket.RECORD_COUNT,
        Bucket.EVENT_COUNT,
        Bucket.RECORDS,
        Bucket.BUCKET_INFLUENCERS,
        Bucket.INFLUENCERS,
        Bucket.INITIAL_ANOMALY_SCORE,
        Bucket.PROCESSING_TIME_MS,
        Bucket.PARTITION_SCORES,

        BucketInfluencer.BUCKET_TIME,
        BucketInfluencer.INFLUENCER_FIELD_NAME,
        BucketInfluencer.INITIAL_ANOMALY_SCORE,
        BucketInfluencer.ANOMALY_SCORE,
        BucketInfluencer.RAW_ANOMALY_SCORE,
        BucketInfluencer.PROBABILITY,

        BucketProcessingTime.AVERAGE_PROCESSING_TIME_MS,

        PartitionNormalisedProb.PARTITION_NORMALIZED_PROBS,
        PartitionNormalisedProb.TYPE,

        CategoryDefinition.CATEGORY_ID,
        CategoryDefinition.TERMS,
        CategoryDefinition.REGEX,
        CategoryDefinition.MAX_MATCHING_LENGTH,
        CategoryDefinition.EXAMPLES,

            DataCounts.BUCKET_COUNT.getPreferredName(),
            DataCounts.PROCESSED_RECORD_COUNT.getPreferredName(),
            DataCounts.PROCESSED_FIELD_COUNT.getPreferredName(),
            DataCounts.INPUT_BYTES.getPreferredName(),
            DataCounts.INPUT_RECORD_COUNT.getPreferredName(),
            DataCounts.INPUT_FIELD_COUNT.getPreferredName(),
            DataCounts.INVALID_DATE_COUNT.getPreferredName(),
            DataCounts.MISSING_FIELD_COUNT.getPreferredName(),
            DataCounts.OUT_OF_ORDER_TIME_COUNT.getPreferredName(),
            DataCounts.FAILED_TRANSFORM_COUNT.getPreferredName(),
            DataCounts.EXCLUDED_RECORD_COUNT.getPreferredName(),
            DataCounts.LATEST_RECORD_TIME.getPreferredName(),

        DataDescription.FORMAT_FIELD.getPreferredName(),
        DataDescription.TIME_FIELD_NAME_FIELD.getPreferredName(),
        DataDescription.TIME_FORMAT_FIELD.getPreferredName(),
        DataDescription.FIELD_DELIMITER_FIELD.getPreferredName(),
        DataDescription.QUOTE_CHARACTER_FIELD.getPreferredName(),

        DetectionRule.RULE_ACTION_FIELD.getPreferredName(),
        DetectionRule.TARGET_FIELD_NAME_FIELD.getPreferredName(),
        DetectionRule.TARGET_FIELD_VALUE_FIELD.getPreferredName(),
        DetectionRule.CONDITIONS_CONNECTIVE_FIELD.getPreferredName(),
        DetectionRule.RULE_CONDITIONS_FIELD.getPreferredName(),

        Detector.DETECTOR_DESCRIPTION_FIELD.getPreferredName(),
        Detector.FUNCTION_FIELD.getPreferredName(),
        Detector.FIELD_NAME_FIELD.getPreferredName(),
        Detector.BY_FIELD_NAME_FIELD.getPreferredName(),
        Detector.OVER_FIELD_NAME_FIELD.getPreferredName(),
        Detector.PARTITION_FIELD_NAME_FIELD.getPreferredName(),
        Detector.USE_NULL_FIELD.getPreferredName(),
        Detector.DETECTOR_RULES_FIELD.getPreferredName(),

        RuleCondition.CONDITION_TYPE_FIELD.getPreferredName(),
        RuleCondition.FIELD_NAME_FIELD.getPreferredName(),
        RuleCondition.FIELD_VALUE_FIELD.getPreferredName(),
        RuleCondition.VALUE_LIST_FIELD.getPreferredName(),

        Influence.INFLUENCER_FIELD_NAME.getPreferredName(),
        Influence.INFLUENCER_FIELD_VALUES.getPreferredName(),

        Influencer.PROBABILITY,
        Influencer.INFLUENCER_FIELD_NAME,
        Influencer.INFLUENCER_FIELD_VALUE,
        Influencer.INITIAL_ANOMALY_SCORE,
        Influencer.ANOMALY_SCORE,

        // JobDetails.DESCRIPTION is not reserved because it is an analyzed string
        // JobDetails.STATUS is not reserved because it is an analyzed string
        JobDetails.DATA_DESCRIPTION,
        JobDetails.SCHEDULER_STATUS,
        JobDetails.SCHEDULER_CONFIG,
        JobDetails.FINISHED_TIME,
        JobDetails.LAST_DATA_TIME,
        JobDetails.COUNTS,
        JobDetails.TIMEOUT,
        JobDetails.RENORMALIZATION_WINDOW_DAYS,
        JobDetails.BACKGROUND_PERSIST_INTERVAL,
        JobDetails.MODEL_SNAPSHOT_RETENTION_DAYS,
        JobDetails.RESULTS_RETENTION_DAYS,
        JobDetails.ANALYSIS_CONFIG,
        JobDetails.ANALYSIS_LIMITS,
        JobDetails.TRANSFORMS,
        JobDetails.MODEL_DEBUG_CONFIG,
        JobDetails.IGNORE_DOWNTIME,
        JobDetails.CUSTOM_SETTINGS,

        ModelDebugConfig.WRITE_TO_FIELD.getPreferredName(),
        ModelDebugConfig.BOUNDS_PERCENTILE_FIELD.getPreferredName(),
        ModelDebugConfig.TERMS_FIELD.getPreferredName(),

        ModelDebugOutput.PARTITION_FIELD_NAME,
        ModelDebugOutput.PARTITION_FIELD_VALUE,
        ModelDebugOutput.OVER_FIELD_NAME,
        ModelDebugOutput.OVER_FIELD_VALUE,
        ModelDebugOutput.BY_FIELD_NAME,
        ModelDebugOutput.BY_FIELD_VALUE,
        ModelDebugOutput.DEBUG_FEATURE,
        ModelDebugOutput.DEBUG_LOWER,
        ModelDebugOutput.DEBUG_UPPER,
        ModelDebugOutput.DEBUG_MEDIAN,
        ModelDebugOutput.ACTUAL,

        ModelSizeStats.MODEL_BYTES_FIELD.getPreferredName(),
        ModelSizeStats.TOTAL_BY_FIELD_COUNT_FIELD.getPreferredName(),
        ModelSizeStats.TOTAL_OVER_FIELD_COUNT_FIELD.getPreferredName(),
        ModelSizeStats.TOTAL_PARTITION_FIELD_COUNT_FIELD.getPreferredName(),
        ModelSizeStats.BUCKET_ALLOCATION_FAILURES_COUNT_FIELD.getPreferredName(),
        ModelSizeStats.MEMORY_STATUS_FIELD.getPreferredName(),
        ModelSizeStats.LOG_TIME_FIELD.getPreferredName(),

        // ModelSnapshot.DESCRIPTION is not reserved because it is an analyzed string
        ModelSnapshot.RESTORE_PRIORITY,
        ModelSnapshot.SNAPSHOT_ID,
        ModelSnapshot.SNAPSHOT_DOC_COUNT,
        ModelSizeStats.TYPE,
        ModelSnapshot.LATEST_RECORD_TIME,
        ModelSnapshot.LATEST_RESULT_TIME,

        Quantiles.QUANTILE_STATE,

        SchedulerConfig.DATA_SOURCE.getPreferredName(),
        SchedulerConfig.QUERY_DELAY.getPreferredName(),
        SchedulerConfig.FREQUENCY.getPreferredName(),
        SchedulerConfig.FILE_PATH.getPreferredName(),
        SchedulerConfig.TAIL_FILE.getPreferredName(),
        SchedulerConfig.BASE_URL.getPreferredName(),
        // SchedulerConfig.USERNAME, is not reserved because it is an analyzed string
        SchedulerConfig.ENCRYPTED_PASSWORD.getPreferredName(),
        SchedulerConfig.INDEXES.getPreferredName(),
        SchedulerConfig.TYPES.getPreferredName(),
        SchedulerConfig.QUERY.getPreferredName(),
        SchedulerConfig.RETRIEVE_WHOLE_SOURCE.getPreferredName(),
        SchedulerConfig.AGGREGATIONS.getPreferredName(),
        SchedulerConfig.AGGS.getPreferredName(),
        SchedulerConfig.SCRIPT_FIELDS.getPreferredName(),
        SchedulerConfig.SCROLL_SIZE.getPreferredName(),

            TransformConfig.TRANSFORM.getPreferredName(),
            TransformConfig.ARGUMENTS.getPreferredName(),
            TransformConfig.INPUTS.getPreferredName(),
            TransformConfig.OUTPUTS.getPreferredName(),

        JOB_ID_NAME,
        ES_TIMESTAMP
    };

    /**
     * A set of all reserved field names in our results.  Fields from the raw
     * data with these names are not added to any result.
     */
    public static final Set<String> RESERVED_FIELD_NAMES = new HashSet<>(Arrays.asList(RESERVED_FIELD_NAME_ARRAY));

    private ReservedFieldNames()
    {
    }
}
