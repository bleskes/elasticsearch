
package org.elasticsearch.xpack.prelert.job.config.verification;

import org.elasticsearch.xpack.prelert.job.AnalysisConfig;
import org.elasticsearch.xpack.prelert.job.Detector;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;
import org.elasticsearch.xpack.prelert.utils.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class AnalysisConfigVerifier {

    private AnalysisConfigVerifier() {}

    /**
     * Checks the configuration is valid
     * <ol>
     * <li>Check that if non-null BucketSpan, BatchSpan, Latency and Period are &gt= 0</li>
     * <li>Check that if non-null Latency is &lt= MAX_LATENCY </li>
     * <li>Check there is at least one detector configured</li>
     * <li>Check all the detectors are configured correctly</li>
     * <li>Check that OVERLAPPING_BUCKETS is set appropriately</li>
     * <li>Check that MULTIPLE_BUCKETSPANS are set appropriately</li>
     * <li>If Per Partition normalization is configured at least one detector
     * must have a partition field and no influences can be used</li>
     * </ol>
     */
    public static boolean verify(AnalysisConfig config) {
        checkFieldIsNotNegativeIfSpecified("bucketSpan", config.getBucketSpan());
        checkFieldIsNotNegativeIfSpecified("batchSpan", config.getBatchSpan());
        checkFieldIsNotNegativeIfSpecified("latency", config.getLatency());
        checkFieldIsNotNegativeIfSpecified("period", config.getPeriod());
        DetectorVerifier.verifyFieldName(config.getSummaryCountFieldName());
        DetectorVerifier.verifyFieldName(config.getCategorizationFieldName());
        verifyDetectors(config);
        verifyCategorizationFilters(config);
        verifyMultipleBucketSpans(config);

        if (config.getUsePerPartitionNormalization()) {
            checkDetectorsHavePartitionFields(config.getDetectors());
            checkNoInfluencersAreSet(config);
        }

        return true;
    }

    private static void checkFieldIsNotNegativeIfSpecified(String fieldName, Long value) {
        if (value != null && value < 0) {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_FIELD_VALUE_TOO_LOW, fieldName, 0, value);
            throw ExceptionsHelper.invalidRequestException(msg, ErrorCodes.INVALID_VALUE);
        }
    }

    private static void verifyDetectors(AnalysisConfig config) {
        if (config.getDetectors().isEmpty()) {
            throw ExceptionsHelper.invalidRequestException(Messages.getMessage(Messages.JOB_CONFIG_NO_DETECTORS),
                    ErrorCodes.INCOMPLETE_CONFIGURATION);
        }

        boolean isSummarised = config.getSummaryCountFieldName() != null &&
                !config.getSummaryCountFieldName().isEmpty();
        for (Detector d : config.getDetectors())
        {
            DetectorVerifier.verify(d, isSummarised);
        }

        verifyOverlappingBucketsConfig(config);
    }

    private static void verifyOverlappingBucketsConfig(AnalysisConfig config)  {
        // If any detector function is rare/freq_rare, mustn't use overlapping buckets
        boolean mustNotUse = false;

        // If overlappingBuckets are not set, default to true if all the detectors
        // work with it
        boolean canUse = true;
        List<String> illegalFunctions = new ArrayList<>();
        for (Detector d : config.getDetectors())
        {
            if (Detector.NO_OVERLAPPING_BUCKETS_FUNCTIONS.contains(d.getFunction()))
            {
                illegalFunctions.add(d.getFunction());
                mustNotUse = true;
            }
            if (Detector.OVERLAPPING_BUCKETS_FUNCTIONS_NOT_NEEDED.contains(d.getFunction()))
            {
                canUse = false;
            }
        }
        setOverlappingBucketsConfig(config, mustNotUse, canUse, illegalFunctions, false);
    }

    private static void setOverlappingBucketsConfig(AnalysisConfig config, boolean mustNotUse, boolean canUse,
                                                    List<String> illegalFunctions, boolean defaultOn) {
        if (config.getOverlappingBuckets() == null) {
            if (defaultOn == true) {
                // Wasn't specified: turn on by default if detectors allow
                if (mustNotUse == false && canUse == true) {
                    config.setOverlappingBuckets(true);
                } else {
                    config.setOverlappingBuckets(false);
                }
            }
        } else {
            if (config.getOverlappingBuckets() == true && mustNotUse == true) {
                throw ExceptionsHelper.invalidRequestException(
                        Messages.getMessage(Messages.JOB_CONFIG_OVERLAPPING_BUCKETS_INCOMPATIBLE_FUNCTION, illegalFunctions.toString()),
                        ErrorCodes.INVALID_FUNCTION);
            }
        }
    }

    private static void verifyCategorizationFilters(AnalysisConfig config) {
        List<String> filters = config.getCategorizationFilters();
        if (filters == null || filters.isEmpty()) {
            return;
        }

        verifyCategorizationFieldNameSetIfFiltersAreSet(config);
        verifyCategorizationFiltersAreDistinct(filters);
        verifyCategorizationFiltersContainNoneEmpty(filters);
        verifyCategorizationFiltersAreValidRegex(filters);
    }

    private static void verifyCategorizationFieldNameSetIfFiltersAreSet(AnalysisConfig config) {
        if (config.getCategorizationFieldName() == null) {
            throw ExceptionsHelper.invalidRequestException(
                    Messages.getMessage(Messages.JOB_CONFIG_CATEGORIZATION_FILTERS_REQUIRE_CATEGORIZATION_FIELD_NAME),
                    ErrorCodes.CATEGORIZATION_FILTERS_REQUIRE_CATEGORIZATION_FIELD_NAME);
        }
    }

    private static void verifyCategorizationFiltersAreDistinct(List<String> filters) {
        if (filters.stream().distinct().count() != filters.size()) {
            throw ExceptionsHelper.invalidRequestException(
                    Messages.getMessage(Messages.JOB_CONFIG_CATEGORIZATION_FILTERS_CONTAINS_DUPLICATES),
                    ErrorCodes.CATEGORIZATION_FILTERS_CONTAIN_DUPLICATES);
        }
    }

    private static void verifyCategorizationFiltersContainNoneEmpty(List<String> filters) {
        if (filters.stream().anyMatch(f -> f.isEmpty())) {
            throw ExceptionsHelper.invalidRequestException(Messages.getMessage(Messages.JOB_CONFIG_CATEGORIZATION_FILTERS_CONTAINS_EMPTY),
                    ErrorCodes.INVALID_VALUE);
        }
    }

    private static void verifyCategorizationFiltersAreValidRegex(List<String> filters) {
        for (String filter : filters) {
            if (!isValidRegex(filter)) {
                throw ExceptionsHelper.invalidRequestException(
                        Messages.getMessage(Messages.JOB_CONFIG_CATEGORIZATION_FILTERS_CONTAINS_INVALID_REGEX, filter),
                        ErrorCodes.INVALID_VALUE);
            }
        }
    }

    private static void verifyMultipleBucketSpans(AnalysisConfig config)  {
        List<Long> multipleBucketSpans = config.getMultipleBucketSpans();
        if (multipleBucketSpans == null) {
            return;
        }

        Long bucketSpan = config.getBucketSpan();
        if (bucketSpan == null) {
            throw ExceptionsHelper.invalidRequestException(Messages.getMessage(Messages.JOB_CONFIG_MULTIPLE_BUCKETSPANS_REQUIRE_BUCKETSPAN),
                    ErrorCodes.INCOMPLETE_CONFIGURATION);
        }
        for (Long span : multipleBucketSpans) {
            if ((span % bucketSpan != 0L) || (span <= bucketSpan)) {
                throw ExceptionsHelper.invalidRequestException(
                        Messages.getMessage(Messages.JOB_CONFIG_MULTIPLE_BUCKETSPANS_MUST_BE_MULTIPLE, span, bucketSpan),
                        ErrorCodes.MULTIPLE_BUCKETSPANS_NOT_MULTIPLE);
            }
        }
    }

    private static boolean checkDetectorsHavePartitionFields(List<Detector> detectors) {
        for (Detector detector : detectors) {
            if (!Strings.isNullOrEmpty(detector.getPartitionFieldName())) {
                return true;
            }
        }

        throw ExceptionsHelper.invalidRequestException(
                Messages.getMessage(Messages.JOB_CONFIG_PER_PARTITION_NORMALIZATION_REQUIRES_PARTITION_FIELD),
                ErrorCodes.PER_PARTITION_NORMALIZATION_REQUIRES_PARTITION_FIELD);
    }

    private static boolean checkNoInfluencersAreSet(AnalysisConfig config) {
        if (!config.getInfluencers().isEmpty()) {
            throw ExceptionsHelper.invalidRequestException(
                    Messages.getMessage(Messages.JOB_CONFIG_PER_PARTITION_NORMALIZATION_CANNOT_USE_INFLUENCERS),
                    ErrorCodes.PER_PARTITION_NORMALIZATION_CANNOT_USE_INFLUENCERS);
        }

        return true;
    }

    private static boolean isValidRegex(String exp) {
        try {
            Pattern.compile(exp);
            return true;
        } catch (PatternSyntaxException e) {
            return false;
        }
    }
}
