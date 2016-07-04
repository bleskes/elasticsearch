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
package com.prelert.job.config.verification;

import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.base.Strings;
import com.prelert.job.AnalysisConfig;
import com.prelert.job.DataDescription;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.JobConfiguration;
import com.prelert.job.ModelDebugConfig;
import com.prelert.job.SchedulerConfig;
import com.prelert.job.SchedulerConfig.DataSource;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.messages.Messages;
import com.prelert.job.transform.TransformConfig;
import com.prelert.job.transform.TransformConfigs;
import com.prelert.job.transform.TransformConfigurationException;
import com.prelert.job.transform.verification.TransformConfigsVerifier;

public final class JobConfigurationVerifier
{
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
     * <li>Verify {@link AnalysisConfigVerifier#verify() AnalysisConfig}</li>
     * <li>Verify {@link AnalysisLimitsVerifier#verify() AnalysisLimits}</li>
     * <li>Verify {@link SchedulerConfigVerifier#verify() SchedulerConfig}</li>
     * <li>Verify {@link DataDescriptionVerifier#verify() DataDescription}</li>
     * <li>Verify {@link TransformConfigsVerifier#verify() Transforms}</li>
     * <li>Verify all the transform outputs are used</li>
     * <li>Check timeout is a +ve number</li>
     * <li>The job ID cannot contain any upper case characters, control
     * characters or any characters in {@link #PROHIBITED_JOB_ID_CHARACTERS_SET}</li>
     * <li>The job is cannot be longer than {@link MAX_JOB_ID_LENGTH }</li>
     * <li>Verify {@link ModelDebugConfig#verify() ModelDebugConfig}</li>
     * <li></li>
     * </ol>
     *
     * @param config The job configuration
     */
    public static boolean verify(JobConfiguration config)
    throws JobConfigurationException
    {
        if (config.getId() != null && config.getId().isEmpty() == false)
        {
            checkValidId(config.getId());
        }

        checkAnalysisConfigIsPresent(config);
        AnalysisConfigVerifier.verify(config.getAnalysisConfig());

        if (config.getAnalysisLimits() != null)
        {
            AnalysisLimitsVerifier.verify(config.getAnalysisLimits());
        }

        if (config.getSchedulerConfig() != null)
        {
            verifySchedulerConfig(config);
        }

        if (config.getDataDescription() != null)
        {
            DataDescriptionVerifier.verify(config.getDataDescription());
        }

        checkValidTransforms(config);

        checkValueNotLessThan(0, "timeout", config.getTimeout());
        checkValueNotLessThan(0, "renormalizationWindowDays", config.getRenormalizationWindowDays());
        checkValueNotLessThan(MIN_BACKGROUND_PERSIST_INTERVAL, "backgroundPersistInterval", config.getBackgroundPersistInterval());
        checkValueNotLessThan(0, "modelSnapshotRetentionDays", config.getModelSnapshotRetentionDays());
        checkValueNotLessThan(0, "resultsRetentionDays", config.getResultsRetentionDays());

        if (config.getModelDebugConfig() != null)
        {
            ModelDebugConfigVerifier.verify(config.getModelDebugConfig());
        }

        return true;
    }

    private static void checkAnalysisConfigIsPresent(JobConfiguration config)
            throws JobConfigurationException
    {
        if (config.getAnalysisConfig() == null)
        {
            throw new JobConfigurationException(
                    Messages.getMessage(Messages.JOB_CONFIG_MISSING_ANALYSISCONFIG),
                    ErrorCodes.INCOMPLETE_CONFIGURATION);
        }
    }

    private static void verifySchedulerConfig(JobConfiguration config) throws JobConfigurationException
    {
        SchedulerConfig schedulerConfig = config.getSchedulerConfig();
        SchedulerConfigVerifier.verify(schedulerConfig);

        AnalysisConfig analysisConfig = config.getAnalysisConfig();
        if (analysisConfig.getBucketSpan() == null)
        {
            throw new JobConfigurationException(
                    Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_REQUIRES_BUCKET_SPAN),
                    ErrorCodes.SCHEDULER_REQUIRES_BUCKET_SPAN);
        }
        if (schedulerConfig.getDataSource() == DataSource.ELASTICSEARCH)
        {
            if (!isNullOrZero(analysisConfig.getLatency()))
            {
                throw new JobConfigurationException(
                        Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_ELASTICSEARCH_DOES_NOT_SUPPORT_LATENCY),
                        ErrorCodes.SCHEDULER_ELASTICSEARCH_DOES_NOT_SUPPORT_LATENCY);
            }
            if (schedulerConfig.getAggregationsOrAggs() != null
                    && !SchedulerConfig.DOC_COUNT.equals(analysisConfig.getSummaryCountFieldName()))
            {
                throw new JobConfigurationException(
                        Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_AGGREGATIONS_REQUIRES_SUMMARY_COUNT_FIELD,
                            DataSource.ELASTICSEARCH.toString(), SchedulerConfig.DOC_COUNT),
                        ErrorCodes.SCHEDULER_AGGREGATIONS_REQUIRES_SUMMARY_COUNT_FIELD);
            }
            if (config.getDataDescription() == null
                    || config.getDataDescription().getFormat() != DataFormat.ELASTICSEARCH)
            {
                throw new JobConfigurationException(
                        Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_ELASTICSEARCH_REQUIRES_DATAFORMAT_ELASTICSEARCH),
                        ErrorCodes.SCHEDULER_ELASTICSEARCH_REQUIRES_DATAFORMAT_ELASTICSEARCH);
            }
        }
    }

    private static boolean isNullOrZero(Long value)
    {
        return value == null || value.longValue() == 0;
    }

    private static void checkValidTransforms(JobConfiguration config)
            throws JobConfigurationException
    {
        if (config.getTransforms() != null)
        {
            try
            {
                TransformConfigsVerifier.verify(config.getTransforms());
            }
            catch (TransformConfigurationException e)
            {
                throw new JobConfigurationException(e.getMessage(), e.getErrorCode());
            }
            checkTransformOutputIsUsed(config);
        }

        checkAtLeastOneTransformIfDataFormatIsSingleLine(config);
    }

    /**
     * Transform outputs should be used in either the date field,
     * as an analysis field or input to another transform
     * @return
     * @throws JobConfigurationException
     */
    private static boolean checkTransformOutputIsUsed(JobConfiguration config)
            throws JobConfigurationException
    {
        Set<String> usedFields = new TransformConfigs(config.getTransforms()).inputFieldNames();
        usedFields.addAll(config.getAnalysisConfig().analysisFields());
        String summaryCountFieldName = config.getAnalysisConfig().getSummaryCountFieldName();
        boolean isSummarised = !Strings.isNullOrEmpty(summaryCountFieldName);
        if (isSummarised)
        {
            usedFields.remove(summaryCountFieldName);
        }

        String timeField = config.getDataDescription() == null ? DataDescription.DEFAULT_TIME_FIELD
                : config.getDataDescription().getTimeField();
        usedFields.add(timeField);

        for (TransformConfig tc : config.getTransforms())
        {
            // if the type has no default outputs it doesn't need an output
            boolean usesAnOutput = tc.type().defaultOutputNames().isEmpty()
                    || tc.getOutputs().stream().anyMatch(outputName -> usedFields.contains(outputName));

            if (isSummarised && tc.getOutputs().contains(summaryCountFieldName))
            {
                String msg = Messages.getMessage(
                        Messages.JOB_CONFIG_TRANSFORM_DUPLICATED_OUTPUT_NAME,
                        tc.type().prettyName());
                throw new JobConfigurationException(msg, ErrorCodes.DUPLICATED_TRANSFORM_OUTPUT_NAME);
            }

            if (!usesAnOutput)
            {
                String msg = Messages.getMessage(Messages.JOB_CONFIG_TRANSFORM_OUTPUTS_UNUSED,
                        tc.type().prettyName());
                throw new JobConfigurationException(msg, ErrorCodes.TRANSFORM_OUTPUTS_UNUSED);
            }
        }

        return false;
    }

    private static void checkAtLeastOneTransformIfDataFormatIsSingleLine(JobConfiguration config)
    throws JobConfigurationException
    {
        if (isSingleLineFormat(config) && hasNoTransforms(config))
        {
            String msg = Messages.getMessage(
                            Messages.JOB_CONFIG_DATAFORMAT_REQUIRES_TRANSFORM,
                            DataFormat.SINGLE_LINE);

            throw new JobConfigurationException(msg,
                    ErrorCodes.DATA_FORMAT_IS_SINGLE_LINE_BUT_NO_TRANSFORMS);
        }
    }

    private static boolean isSingleLineFormat(JobConfiguration config)
    {
        return config.getDataDescription() != null
                && config.getDataDescription().getFormat() == DataFormat.SINGLE_LINE;
    }

    private static boolean hasNoTransforms(JobConfiguration config)
    {
        return config.getTransforms() == null || config.getTransforms().isEmpty();
    }

    private static void checkValidId(String jobId) throws JobConfigurationException
    {
        checkValidIdLength(jobId);
        checkIdContainsValidCharacters(jobId);
    }

    private static void checkValidIdLength(String jobId) throws JobConfigurationException
    {
        if (jobId.length() > MAX_JOB_ID_LENGTH)
        {
            throw new JobConfigurationException(
                    Messages.getMessage(Messages.JOB_CONFIG_ID_TOO_LONG, MAX_JOB_ID_LENGTH),
                            ErrorCodes.JOB_ID_TOO_LONG);
        }
    }

    private static void checkIdContainsValidCharacters(String jobId)
            throws JobConfigurationException
    {
        if (!VALID_JOB_ID_CHAR_PATTERN.matcher(jobId).matches())
        {
            throw new JobConfigurationException(
                    Messages.getMessage(Messages.JOB_CONFIG_INVALID_JOBID_CHARS),
                    ErrorCodes.PROHIBITIED_CHARACTER_IN_JOB_ID);
        }
    }

    private static void checkValueNotLessThan(long minVal, String name, Long value)
            throws JobConfigurationException
    {
        if (value != null && value < minVal)
        {
            throw new JobConfigurationException(
                    Messages.getMessage(Messages.JOB_CONFIG_FIELD_VALUE_TOO_LOW, name, minVal, value),
                    ErrorCodes.INVALID_VALUE);
        }
    }

}
