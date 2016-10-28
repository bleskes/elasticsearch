
package org.elasticsearch.xpack.prelert.job.config.verification;

import org.elasticsearch.xpack.prelert.job.AnalysisConfig;
import org.elasticsearch.xpack.prelert.job.DataDescription;
import org.elasticsearch.xpack.prelert.job.DataDescription.DataFormat;
import org.elasticsearch.xpack.prelert.job.JobConfiguration;
import org.elasticsearch.xpack.prelert.job.SchedulerConfig;
import org.elasticsearch.xpack.prelert.job.SchedulerConfig.DataSource;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.job.transform.TransformConfig;
import org.elasticsearch.xpack.prelert.job.transform.TransformConfigs;
import org.elasticsearch.xpack.prelert.job.transform.verification.TransformConfigsVerifier;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;
import org.elasticsearch.xpack.prelert.utils.Strings;

import java.util.Set;
import java.util.regex.Pattern;

public final class JobConfigurationVerifier {

    public static final long MIN_BACKGROUND_PERSIST_INTERVAL = 3600;

    public static final int MAX_JOB_ID_LENGTH = 64;

    /**
     * Valid jobId characters. Note that '.' is allowed but not documented.
     */
    private static final Pattern VALID_JOB_ID_CHAR_PATTERN = Pattern.compile("[a-z0-9_\\-\\.]+");

    private JobConfigurationVerifier()
    {
    }

    /**
     * Checks the job configuration settings and throws an exception
     * if any values are invalid
     *
     * <ol>
     * <li>Either an AnalysisConfig or Job reference must be set</li>
     * <li>Verify {@link AnalysisConfigVerifier#verify(AnalysisConfig) AnalysisConfig}</li>
     * <li>Verify {@link DataDescriptionVerifier#verify(DataDescription) DataDescription}</li>
     * <li>Verify {@link TransformConfigsVerifier#verify(List) Transforms}</li>
     * <li>Verify {@link ModelDebugConfigVerifier#verify(ModelDebugConfig) ModelDebugConfig}</li>
     * <li>Verify all the transform outputs are used</li>
     * <li>Check timeout is a +ve number</li>
     * <li>The job ID cannot contain any upper case characters, control
     * characters or any characters not matched by {@link #VALID_JOB_ID_CHAR_PATTERN}</li>
     * <li>The job is cannot be longer than {@link #MAX_JOB_ID_LENGTH }</li>
     * <li></li>
     * </ol>
     *
     * @param config The job configuration
     */
    public static boolean verify(JobConfiguration config) {
        if (config.getId() != null && config.getId().isEmpty() == false) {
            checkValidId(config.getId());
        }

        checkAnalysisConfigIsPresent(config);
        AnalysisConfigVerifier.verify(config.getAnalysisConfig());

        if (config.getSchedulerConfig() != null) {
            verifySchedulerConfig(config);
        }

        if (config.getDataDescription() != null) {
            DataDescriptionVerifier.verify(config.getDataDescription());
        }

        checkValidTransforms(config);

        checkValueNotLessThan(0, "timeout", config.getTimeout());
        checkValueNotLessThan(0, "renormalizationWindowDays", config.getRenormalizationWindowDays());
        checkValueNotLessThan(MIN_BACKGROUND_PERSIST_INTERVAL, "backgroundPersistInterval", config.getBackgroundPersistInterval());
        checkValueNotLessThan(0, "modelSnapshotRetentionDays", config.getModelSnapshotRetentionDays());
        checkValueNotLessThan(0, "resultsRetentionDays", config.getResultsRetentionDays());

        if (config.getModelDebugConfig() != null) {
            ModelDebugConfigVerifier.verify(config.getModelDebugConfig());
        }

        return true;
    }

    private static void checkAnalysisConfigIsPresent(JobConfiguration config) {
        if (config.getAnalysisConfig() == null)
        {
            throw ExceptionsHelper.invalidRequestException(Messages.getMessage(Messages.JOB_CONFIG_MISSING_ANALYSISCONFIG),
                    ErrorCodes.INCOMPLETE_CONFIGURATION);
        }
    }

    private static void verifySchedulerConfig(JobConfiguration config) {
        // NORELEASE move all scheduler validation to SchedulerConfig.Builder
        SchedulerConfig schedulerConfig = config.getSchedulerConfig();
        AnalysisConfig analysisConfig = config.getAnalysisConfig();
        if (analysisConfig.getBucketSpan() == null) {
            throw ExceptionsHelper.invalidRequestException(Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_REQUIRES_BUCKET_SPAN),
                    ErrorCodes.SCHEDULER_REQUIRES_BUCKET_SPAN);
        }
        if (schedulerConfig.getDataSource() == DataSource.ELASTICSEARCH) {
            if (!isNullOrZero(analysisConfig.getLatency())) {
                throw ExceptionsHelper.invalidRequestException(
                        Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_ELASTICSEARCH_DOES_NOT_SUPPORT_LATENCY),
                        ErrorCodes.SCHEDULER_ELASTICSEARCH_DOES_NOT_SUPPORT_LATENCY);
            }
            if (schedulerConfig.getAggregationsOrAggs() != null
                    && !SchedulerConfig.DOC_COUNT.equals(analysisConfig.getSummaryCountFieldName())) {
                throw ExceptionsHelper.invalidRequestException(
                        Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_AGGREGATIONS_REQUIRES_SUMMARY_COUNT_FIELD,
                            DataSource.ELASTICSEARCH.toString(), SchedulerConfig.DOC_COUNT),
                        ErrorCodes.SCHEDULER_AGGREGATIONS_REQUIRES_SUMMARY_COUNT_FIELD);
            }
            if (config.getDataDescription() == null || config.getDataDescription().getFormat() != DataFormat.ELASTICSEARCH) {
                throw ExceptionsHelper.invalidRequestException(
                        Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_ELASTICSEARCH_REQUIRES_DATAFORMAT_ELASTICSEARCH),
                        ErrorCodes.SCHEDULER_ELASTICSEARCH_REQUIRES_DATAFORMAT_ELASTICSEARCH);
            }
        }
    }

    private static boolean isNullOrZero(Long value) {
        return value == null || value.longValue() == 0;
    }

    private static void checkValidTransforms(JobConfiguration config) {
        if (config.getTransforms() != null)
        {
            TransformConfigsVerifier.verify(config.getTransforms());
            checkTransformOutputIsUsed(config);
        }

        checkAtLeastOneTransformIfDataFormatIsSingleLine(config);
    }

    /**
     * Transform outputs should be used in either the date field,
     * as an analysis field or input to another transform
     */
    private static boolean checkTransformOutputIsUsed(JobConfiguration config) {
        Set<String> usedFields = new TransformConfigs(config.getTransforms()).inputFieldNames();
        usedFields.addAll(config.getAnalysisConfig().analysisFields());
        String summaryCountFieldName = config.getAnalysisConfig().getSummaryCountFieldName();
        boolean isSummarised = !Strings.isNullOrEmpty(summaryCountFieldName);
        if (isSummarised) {
            usedFields.remove(summaryCountFieldName);
        }

        String timeField = config.getDataDescription() == null ? DataDescription.DEFAULT_TIME_FIELD
                : config.getDataDescription().getTimeField();
        usedFields.add(timeField);

        for (TransformConfig tc : config.getTransforms()) {
            // if the type has no default outputs it doesn't need an output
            boolean usesAnOutput = tc.type().defaultOutputNames().isEmpty()
                    || tc.getOutputs().stream().anyMatch(outputName -> usedFields.contains(outputName));

            if (isSummarised && tc.getOutputs().contains(summaryCountFieldName)) {
                String msg = Messages.getMessage(Messages.JOB_CONFIG_TRANSFORM_DUPLICATED_OUTPUT_NAME, tc.type().prettyName());
                throw ExceptionsHelper.invalidRequestException(msg, ErrorCodes.DUPLICATED_TRANSFORM_OUTPUT_NAME);
            }

            if (!usesAnOutput) {
                String msg = Messages.getMessage(Messages.JOB_CONFIG_TRANSFORM_OUTPUTS_UNUSED,
                        tc.type().prettyName());
                throw ExceptionsHelper.invalidRequestException(msg, ErrorCodes.TRANSFORM_OUTPUTS_UNUSED);
            }
        }

        return false;
    }

    private static void checkAtLeastOneTransformIfDataFormatIsSingleLine(JobConfiguration config) {
        if (isSingleLineFormat(config) && hasNoTransforms(config)) {
            String msg = Messages.getMessage(
                            Messages.JOB_CONFIG_DATAFORMAT_REQUIRES_TRANSFORM,
                            DataFormat.SINGLE_LINE);

            throw ExceptionsHelper.invalidRequestException(msg, ErrorCodes.DATA_FORMAT_IS_SINGLE_LINE_BUT_NO_TRANSFORMS);
        }
    }

    private static boolean isSingleLineFormat(JobConfiguration config) {
        return config.getDataDescription() != null && config.getDataDescription().getFormat() == DataFormat.SINGLE_LINE;
    }

    private static boolean hasNoTransforms(JobConfiguration config) {
        return config.getTransforms() == null || config.getTransforms().isEmpty();
    }

    private static void checkValidId(String jobId) {
        checkValidIdLength(jobId);
        checkIdContainsValidCharacters(jobId);
    }

    private static void checkValidIdLength(String jobId) {
        if (jobId.length() > MAX_JOB_ID_LENGTH) {
            throw ExceptionsHelper.invalidRequestException(Messages.getMessage(Messages.JOB_CONFIG_ID_TOO_LONG, MAX_JOB_ID_LENGTH),
                    ErrorCodes.JOB_ID_TOO_LONG);
        }
    }

    private static void checkIdContainsValidCharacters(String jobId) {
        if (!VALID_JOB_ID_CHAR_PATTERN.matcher(jobId).matches()) {
            throw ExceptionsHelper.invalidRequestException(Messages.getMessage(Messages.JOB_CONFIG_INVALID_JOBID_CHARS),
                    ErrorCodes.PROHIBITIED_CHARACTER_IN_JOB_ID);
        }
    }

    private static void checkValueNotLessThan(long minVal, String name, Long value) {
        if (value != null && value < minVal) {
            throw ExceptionsHelper.invalidRequestException(
                    Messages.getMessage(Messages.JOB_CONFIG_FIELD_VALUE_TOO_LOW, name, minVal, value), ErrorCodes.INVALID_VALUE);
        }
    }
}
