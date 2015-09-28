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
package com.prelert.job.config.verification;

import java.util.Set;

import com.prelert.job.DataDescription;
import com.prelert.job.DataDescription.DataFormat;
import com.prelert.job.JobConfiguration;
import com.prelert.job.ModelDebugConfig;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.messages.Messages;
import com.prelert.job.transform.TransformConfig;
import com.prelert.job.transform.TransformConfigs;
import com.prelert.job.transform.TransformConfigurationException;
import com.prelert.job.transform.verification.TransformConfigsVerifier;

public final class JobConfigurationVerifier
{
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
        checkEitherAnalysisConfigOrJobReferenceIdPresent(config);

        if (config.getAnalysisConfig() != null)
        {
            AnalysisConfigVerifier.verify(config.getAnalysisConfig());
        }

        if (config.getAnalysisLimits() != null)
        {
            AnalysisLimitsVerifier.verify(config.getAnalysisLimits());
        }
        if (config.getDataDescription() != null)
        {
            DataDescriptionVerifier.verify(config.getDataDescription());
        }

        checkValidTransforms(config);

        if (config.getTimeout() != null && config.getTimeout() < 0)
        {
            throw new JobConfigurationException(
                    Messages.getMessage(Messages.JOB_CONFIG_NEGATIVE_FIELD_VALUE,
                            "timeout", config.getTimeout()),
                    ErrorCodes.INVALID_VALUE);
        }

        if (config.getId() != null && config.getId().isEmpty() == false)
        {
            checkValidId(config.getId());
        }

        if (config.getModelDebugConfig() != null)
        {
            ModelDebugConfigVerifier.verify(config.getModelDebugConfig());
        }

        return true;
    }

    private static void checkEitherAnalysisConfigOrJobReferenceIdPresent(JobConfiguration config)
            throws JobConfigurationException
    {
        if (config.getAnalysisConfig() == null && config.getReferenceJobId() == null)
        {
            throw new JobConfigurationException(
                    Messages.getMessage(Messages.JOB_CONFIG_MISSING_ANALYSISCONFIG),
                    ErrorCodes.INCOMPLETE_CONFIGURATION);
        }
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
        boolean isSummarised = config.getAnalysisConfig().getSummaryCountFieldName() != null &&
                                config.getAnalysisConfig().getSummaryCountFieldName().isEmpty() == false;
        if (isSummarised)
        {
            usedFields.remove(config.getAnalysisConfig().getSummaryCountFieldName());
        }

        String timeField = DataDescription.DEFAULT_TIME_FIELD;
        if (config.getDataDescription() != null)
        {
            timeField = config.getDataDescription().getTimeField();
        }
        usedFields.add(timeField);

        for (TransformConfig tc : config.getTransforms())
        {
            // if the type has no default outputs it doesn't need an output
            boolean usesAnOutput = tc.type().defaultOutputNames().isEmpty();
            for (String outputName : tc.getOutputs())
            {
                if (usedFields.contains(outputName))
                {
                    usesAnOutput = true;
                    break;
                }
            }

            if (isSummarised && tc.getOutputs().contains(
                    config.getAnalysisConfig().getSummaryCountFieldName()))
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
        if (jobId.length() > JobConfiguration.MAX_JOB_ID_LENGTH)
        {
            throw new JobConfigurationException(
                    Messages.getMessage(Messages.JOB_CONFIG_ID_TOO_LONG,
                            JobConfiguration.MAX_JOB_ID_LENGTH),
                            ErrorCodes.JOB_ID_TOO_LONG);
        }
    }

    private static void checkIdContainsValidCharacters(String jobId)
            throws JobConfigurationException
    {
        for (char c : jobId.toCharArray())
        {
            if (JobConfiguration.PROHIBITED_JOB_ID_CHARACTERS_SET.contains(c))
            {
                throw new JobConfigurationException(
                        Messages.getMessage(Messages.JOB_CONFIG_INVALID_JOBID_CHARS,
                                c, JobConfiguration.PROHIBITED_JOB_ID_CHARACTERS),
                        ErrorCodes.PROHIBITIED_CHARACTER_IN_JOB_ID);
            }
            if (Character.isUpperCase(c))
            {
                throw new JobConfigurationException(
                        Messages.getMessage(Messages.JOB_CONFIG_ID_CONTAINS_UPPERCASE_CHARS),
                        ErrorCodes.PROHIBITIED_CHARACTER_IN_JOB_ID);
            }
            if (Character.isISOControl(c))
            {
                throw new JobConfigurationException(
                        Messages.getMessage(Messages.JOB_CONFIG_ID_CONTAINS_CONTROL_CHARS),
                        ErrorCodes.PROHIBITIED_CHARACTER_IN_JOB_ID);
            }
        }
    }
}
