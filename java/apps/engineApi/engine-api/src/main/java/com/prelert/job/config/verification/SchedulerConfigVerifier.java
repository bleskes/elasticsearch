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
     *   <li>Check that base URL, indexes, types, query, aggregations, aggs, start time and end time are not specified</li>
     *   </ol>
     * </li>
     * <li>If data source is ELASTICSEARCH
     *   <ol>
     *   <li>Check that the base URL is valid</li>
     *   <li>Check that at least one index has been specified</li>
     *   <li>Check that at least one type has been specified</li>
     *   <li>Check that the query is not null or empty</li>
     *   <li>Check that at least one of aggregations and aggs is null</li>
     *   <li>Check that end time is greater than start time if they're both specified</li>
     *   <li>Check that path and tail are not specified</li>
     *   </ol>
     * </li>
     * </ul>
     */
    public static boolean verify(SchedulerConfig config) throws JobConfigurationException
    {
        checkFieldIsNotNegative(SchedulerConfig.QUERY_DELAY, config.getQueryDelay());
        checkFieldIsNotNegative(SchedulerConfig.FREQUENCY, config.getFrequency());

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
        checkFieldIsNotNullOrEmpty(SchedulerConfig.FILE_PATH, config.getFilePath());
        checkFieldIsNull(dataSource, SchedulerConfig.BASE_URL, config.getBaseUrl());
        checkFieldIsNull(dataSource, SchedulerConfig.USERNAME, config.getUsername());
        checkFieldIsNull(dataSource, SchedulerConfig.PASSWORD, config.getPassword());
        checkFieldIsNull(dataSource, SchedulerConfig.ENCRYPTED_PASSWORD, config.getEncryptedPassword());
        checkFieldIsNull(dataSource, SchedulerConfig.INDEXES, config.getIndexes());
        checkFieldIsNull(dataSource, SchedulerConfig.TYPES, config.getTypes());
        checkFieldIsNull(dataSource, SchedulerConfig.RETRIEVE_WHOLE_SOURCE, config.getRetrieveWholeSource());
        checkFieldIsNull(dataSource, SchedulerConfig.AGGREGATIONS, config.getAggregations());
        checkFieldIsNull(dataSource, SchedulerConfig.QUERY, config.getQuery());
        checkFieldIsNull(dataSource, SchedulerConfig.SCRIPT_FIELDS, config.getScriptFields());
        checkFieldIsNull(dataSource, SchedulerConfig.SCROLL_SIZE, config.getScrollSize());
    }

    private static void verifyElasticsearchSchedulerConfig(SchedulerConfig config,
            DataSource dataSource) throws JobConfigurationException
    {
        checkUrl(SchedulerConfig.BASE_URL, config.getBaseUrl());
        checkUserPass(config.getUsername(), config.getPassword(), config.getEncryptedPassword());
        checkFieldIsNotNullOrEmpty(SchedulerConfig.INDEXES, config.getIndexes());
        checkFieldIsNotNullOrEmpty(SchedulerConfig.TYPES, config.getTypes());
        if (config.getAggregations() != null)
        {
            // Not allowed both aggs and aggregations
            checkFieldIsNull(dataSource, SchedulerConfig.AGGS, config.getAggs());
        }
        if (Boolean.TRUE.equals(config.getRetrieveWholeSource()))
        {
            // Not allowed script_fields when retrieveWholeSource is true
            checkFieldIsNull(dataSource, SchedulerConfig.SCRIPT_FIELDS, config.getScriptFields());
        }
        checkFieldIsNotNegative(SchedulerConfig.SCROLL_SIZE, config.getScrollSize());
        checkFieldIsNull(dataSource, SchedulerConfig.FILE_PATH, config.getFilePath());
        checkFieldIsNull(dataSource, SchedulerConfig.TAIL_FILE, config.getTailFile());
    }

    private static void checkUserPass(String username, String password, String encryptedPassword)
            throws JobConfigurationException
    {
        if (username == null && password == null && encryptedPassword == null)
        {
            // It's acceptable to have no credentials
            return;
        }

        if (username == null)
        {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_INCOMPLETE_CREDENTIALS);
            throw new JobConfigurationException(msg, ErrorCodes.SCHEDULER_INCOMPLETE_CREDENTIALS);
        }

        if (password == null && encryptedPassword == null)
        {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_INCOMPLETE_CREDENTIALS);
            throw new JobConfigurationException(msg, ErrorCodes.SCHEDULER_INCOMPLETE_CREDENTIALS);
        }

        if (password != null && encryptedPassword != null)
        {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_MULTIPLE_PASSWORDS);
            throw new JobConfigurationException(msg, ErrorCodes.SCHEDULER_MULTIPLE_PASSWORDS);
        }
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
            throwInvalidOptionValue(fieldName, value);
        }
    }

    private static void throwInvalidOptionValue(String fieldName, Object value)
            throws JobConfigurationException
    {
        String msg = Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_INVALID_OPTION_VALUE,
                fieldName, value);
        throw new JobConfigurationException(msg, ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE);
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

        throwInvalidOptionValue(fieldName, value);
    }

    private static void checkFieldIsNotNegative(String fieldName, Number value)
            throws JobConfigurationException
    {
        if (value != null && value.longValue() < 0)
        {
            throwInvalidOptionValue(fieldName, value);
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
            throwInvalidOptionValue(fieldName, value);
        }
    }
}
