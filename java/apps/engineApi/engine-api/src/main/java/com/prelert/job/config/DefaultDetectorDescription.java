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

package com.prelert.job.config;

import com.prelert.job.Detector;
import com.prelert.utils.PrelertStrings;
import com.prelert.utils.Strings;

public final class DefaultDetectorDescription
{
    private static final String BY_TOKEN = " by ";
    private static final String OVER_TOKEN = " over ";

    private static final String USE_NULL_OPTION = " usenull=";
    private static final String PARTITION_FIELD_OPTION = " partitionfield=";
    private static final String EXCLUDE_FREQUENT_OPTION = " excludefrequent=";

    private DefaultDetectorDescription()
    {
        // do nothing
    }

    /**
     * Returns the default description for the given {@code detector}
     * @param detector the {@code Detector} for which a default description is requested
     * @return the default description
     */
    public static String of(Detector detector)
    {
        StringBuilder sb = new StringBuilder();
        appendOn(detector, sb);
        return sb.toString();
    }

    /**
     * Appends to the given {@code StringBuilder} the default description
     * for the given {@code detector}
     * @param detector the {@code Detector} for which a default description is requested
     * @param sb the {@code StringBuilder} to append to
     */
    public static void appendOn(Detector detector, StringBuilder sb)
    {
        if (isNotNullOrEmpty(detector.getFunction()))
        {
            sb.append(detector.getFunction());
            if (isNotNullOrEmpty(detector.getFieldName()))
            {
                sb.append('(').append(quoteField(detector.getFieldName()))
                        .append(')');
            }
        }
        else if (isNotNullOrEmpty(detector.getFieldName()))
        {
            sb.append(quoteField(detector.getFieldName()));
        }

        if (isNotNullOrEmpty(detector.getByFieldName()))
        {
            sb.append(BY_TOKEN).append(quoteField(detector.getByFieldName()));
        }

        if (isNotNullOrEmpty(detector.getOverFieldName()))
        {
            sb.append(OVER_TOKEN).append(quoteField(detector.getOverFieldName()));
        }

        if (detector.isUseNull() != null)
        {
            sb.append(USE_NULL_OPTION).append(detector.isUseNull());
        }

        if (isNotNullOrEmpty(detector.getPartitionFieldName()))
        {
            sb.append(PARTITION_FIELD_OPTION)
                    .append(quoteField(detector.getPartitionFieldName()));
        }

        if (isNotNullOrEmpty(detector.getExcludeFrequent()))
        {
            sb.append(EXCLUDE_FREQUENT_OPTION)
                    .append(detector.getExcludeFrequent());
        }
    }

    private static String quoteField(String field)
    {
        return PrelertStrings.doubleQuoteIfNotAlphaNumeric(field);
    }

    private static boolean isNotNullOrEmpty(String arg)
    {
        return !Strings.isNullOrEmpty(arg);
    }
}
