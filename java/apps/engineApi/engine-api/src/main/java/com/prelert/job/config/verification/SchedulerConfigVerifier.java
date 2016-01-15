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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;

import com.prelert.job.SchedulerConfig;
import com.prelert.job.SchedulerConfig.DataSource;
import com.prelert.job.errorcodes.ErrorCodes;
import com.prelert.job.messages.Messages;

public final class SchedulerConfigVerifier
{
    private SchedulerConfigVerifier()
    {
    }

    /**
     * Checks the configuration is valid
     * <ul>
     * <li>If data source is FILE
     *   <ol>
     *   <li>Check that path is not null or empty</li>
     *   <li>Check that base URL, indexes, types, query, start time and end time are not specified</li>
     *   </ol>
     * </li>
     * <li>If data source is FILE
     *   <ol>
     *   <li>Check that the base URL is valid</li>
     *   <li>Check that at least one index has been specified</li>
     *   <li>Check that at least one type has been specified</li>
     *   <li>Check that the query is not null or empty</li>
     *   <li>Check that end time is greater than start time if they're both specified</li>
     *   <li>Check that path and tail are not specified</li>
     *   </ol>
     * </li>
     * </ul>
     */
    public static boolean verify(SchedulerConfig config) throws JobConfigurationException
    {
        DataSource dataSource = config.getDataSource();

        switch (dataSource)
        {
            case FILE:
                verifyFileSchedulerConfig(config, dataSource);
                break;
            case ELASTICSEARCH:
                verifyElasticsearchSchedulerConfig(config, dataSource);
                break;
            default:
                throw new IllegalStateException();
        }

        return true;
    }

    private static void verifyFileSchedulerConfig(SchedulerConfig config, DataSource dataSource)
            throws JobConfigurationException
    {
        checkFieldIsNotNullOrEmpty("path", config.getPath());
        checkFieldIsNull(dataSource, "baseUrl", config.getBaseUrl());
        checkFieldIsNull(dataSource, "indexes", config.getIndexes());
        checkFieldIsNull(dataSource, "types", config.getTypes());
        checkFieldIsNull(dataSource, "query", config.getQuery());
        checkFieldIsNull(dataSource, "startTime", config.getStartTime());
        checkFieldIsNull(dataSource, "endTime", config.getEndTime());
    }

    private static void verifyElasticsearchSchedulerConfig(SchedulerConfig config,
            DataSource dataSource) throws JobConfigurationException
    {
        checkUrl("baseUrl", config.getBaseUrl());
        checkFieldIsNotNullOrEmpty("indexes", config.getIndexes());
        checkFieldIsNotNullOrEmpty("types", config.getTypes());
        checkTimesInOrder("startTime", config.getStartTime(), config.getEndTime());
        checkFieldIsNull(dataSource, "path", config.getPath());
        checkFieldIsNull(dataSource, "tail", config.getTail());
    }

    private static void checkFieldIsNull(DataSource dataSource, String fieldName, Object value)
            throws JobConfigurationException
    {
        if (value != null)
        {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_FIELD_NOT_SUPPORTED,
                                                fieldName, dataSource.toString());
            throw new JobConfigurationException(msg, ErrorCodes.SCHEDULER_FIELD_NOT_SUPPORTED_FOR_DATASOURCE);
        }
    }

    private static void checkFieldIsNotNullOrEmpty(String fieldName, String value)
            throws JobConfigurationException
    {
        if (value == null || value.isEmpty())
        {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_INVALID_OPTION_VALUE,
                                             fieldName, "" + value);
            throw new JobConfigurationException(msg, ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE);
        }
    }

    private static void checkFieldIsNotNullOrEmpty(String fieldName, List<String> value)
            throws JobConfigurationException
    {
        if (value != null)
        {
            for (String item : value)
            {
                if (item != null && !item.isEmpty())
                {
                    return;
                }
            }
        }

        String msg = Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_INVALID_OPTION_VALUE,
                                         fieldName, "" + value);
        throw new JobConfigurationException(msg, ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE);
    }

    private static void checkTimesInOrder(String startTimeFieldName, Date startTime, Date endTime)
            throws JobConfigurationException
    {
        if (startTime != null && endTime != null && startTime.getTime() >= endTime.getTime())
        {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_INVALID_OPTION_VALUE,
                                             startTimeFieldName, "" + startTime);
            throw new JobConfigurationException(msg, ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE);
        }
    }

    private static void checkUrl(String fieldName, String value)
            throws JobConfigurationException
    {
        try
        {
            new URL(value);
        }
        catch (MalformedURLException e)
        {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_INVALID_OPTION_VALUE,
                                                fieldName, "" + value);
            throw new JobConfigurationException(msg, ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE);
        }
    }
}
