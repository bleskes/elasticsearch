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
     * </ol>
     */
    public static boolean verify(AnalysisConfig config) throws JobConfigurationException
    {
        checkFieldIsNotNegativeIfSpecified("BucketSpan", config.getBucketSpan());
        checkFieldIsNotNegativeIfSpecified("BatchSpan", config.getBatchSpan());
        checkFieldIsNotNegativeIfSpecified("Latency", config.getLatency());
        checkFieldIsNotNegativeIfSpecified("Period", config.getPeriod());
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
            String msg = Messages.getMessage(Messages.JOB_CONFIG_NEGATIVE_FIELD_VALUE,
                                                fieldName, value);
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

        boolean isSummarised = config.getSummaryCountFieldName() != null && !config.getSummaryCountFieldName().isEmpty();
        for (Detector d : config.getDetectors())
        {
            DetectorVerifier.verify(d, isSummarised);
        }
    }

}
