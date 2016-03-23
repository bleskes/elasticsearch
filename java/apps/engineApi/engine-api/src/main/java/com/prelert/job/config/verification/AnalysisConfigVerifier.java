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

import java.util.ArrayList;
import java.util.List;

import com.prelert.job.AnalysisConfig;
import com.prelert.job.Detector;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.messages.Messages;

public final class AnalysisConfigVerifier
{
    private AnalysisConfigVerifier()
    {
    }

    /**
     * Checks the configuration is valid
     * <ol>
     * <li>Check that if non-null BucketSpan, BatchSpan, Latency and Period are &gt= 0</li>
     * <li>Check that if non-null Latency is &lt= MAX_LATENCY </li>
     * <li>Check there is at least one detector configured</li>
     * <li>Check all the detectors are configured correctly</li>
     * <li>Check that OVERLAPPING_BUCKETS is set appropriately</li>
     * </ol>
     */
    public static boolean verify(AnalysisConfig config) throws JobConfigurationException
    {
        checkFieldIsNotNegativeIfSpecified("bucketSpan", config.getBucketSpan());
        checkFieldIsNotNegativeIfSpecified("batchSpan", config.getBatchSpan());
        checkFieldIsNotNegativeIfSpecified("latency", config.getLatency());
        checkFieldIsNotNegativeIfSpecified("period", config.getPeriod());
        DetectorVerifier.verifyFieldName(config.getSummaryCountFieldName());
        DetectorVerifier.verifyFieldName(config.getCategorizationFieldName());
        verifyDetectors(config);

        return true;
    }

    private static void checkFieldIsNotNegativeIfSpecified(String fieldName, Long value)
            throws JobConfigurationException
    {
        if (value != null && value < 0)
        {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_FIELD_VALUE_TOO_LOW,
                                                fieldName, 0, value);
            throw new JobConfigurationException(msg, ErrorCodes.INVALID_VALUE);
        }
    }

    private static void verifyDetectors(AnalysisConfig config) throws JobConfigurationException
    {
        if (config.getDetectors().isEmpty())
        {
            throw new JobConfigurationException(
                    Messages.getMessage(Messages.JOB_CONFIG_NO_DETECTORS),
                    ErrorCodes.INCOMPLETE_CONFIGURATION);
        }

        // If any detector function is rare/freq_rare, mustn't use overlapping buckets
        boolean mustNotUseOverlappingBuckets = false;
        // If overlappingBuckets are not set, default to true if all the detectors
        // work with it

        @SuppressWarnings("unused")
        boolean canUseOverlappingBuckets = true;
        List<String> illegalFunctions = new ArrayList<>();

        boolean isSummarised = config.getSummaryCountFieldName() != null &&
                !config.getSummaryCountFieldName().isEmpty();
        for (Detector d : config.getDetectors())
        {
            DetectorVerifier.verify(d, isSummarised);

            if (Detector.NO_OVERLAPPING_BUCKETS_FUNCTIONS.contains(d.getFunction()))
            {
                illegalFunctions.add(d.getFunction());
                mustNotUseOverlappingBuckets = true;
            }
            if (Detector.OVERLAPPING_BUCKETS_FUNCTIONS_NOT_NEEDED.contains(d.getFunction()))
            {
                canUseOverlappingBuckets = false;
            }
        }

        if (config.getOverlappingBuckets() == null)
        {
            /* Uncomment this when overlappingBuckets are turned on by default
             *
            // Wasn't specified: turn on by default if detectors allow
            if (mustNotUseOverlappingBuckets == false &&
                    canUseOverlappingBuckets == true)
            {
                config.setOverlappingBuckets(true);
            }
            else
            {
                config.setOverlappingBuckets(false);
            }
            */
        }
        else
        {
            if (config.getOverlappingBuckets() && mustNotUseOverlappingBuckets == true)
            {
                throw new JobConfigurationException(
                        Messages.getMessage(Messages.JOB_CONFIG_OVERLAPPING_BUCKETS_INCOMPATIBLE_FUNCTION,
                                            illegalFunctions.toString()),
                        ErrorCodes.INVALID_FUNCTION);
            }
        }
    }

}
