
package org.elasticsearch.xpack.prelert.job.config.verification;

import org.elasticsearch.xpack.prelert.job.SchedulerConfig;
import org.elasticsearch.xpack.prelert.job.SchedulerConfig.DataSource;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;
import org.elasticsearch.xpack.prelert.utils.ExceptionsHelper;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

//NORELEASE: validation should be part of SchedulerConfig.Builder
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
    public static boolean verify(SchedulerConfig.Builder config) {
        checkFieldIsNotNegative(SchedulerConfig.QUERY_DELAY.getPreferredName(), config.getQueryDelay());
        checkFieldIsNotNegative(SchedulerConfig.FREQUENCY.getPreferredName(), config.getFrequency());

        DataSource dataSource = config.getDataSource();
        switch (dataSource) {
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

    private static void verifyFileSchedulerConfig(SchedulerConfig.Builder config, DataSource dataSource) {
        checkFieldIsNotNullOrEmpty(SchedulerConfig.FILE_PATH.getPreferredName(), config.getFilePath());
        checkFieldIsNull(dataSource, SchedulerConfig.BASE_URL.getPreferredName(), config.getBaseUrl());
        checkFieldIsNull(dataSource, SchedulerConfig.USERNAME.getPreferredName(), config.getUsername());
        checkFieldIsNull(dataSource, SchedulerConfig.PASSWORD.getPreferredName(), config.getPassword());
        checkFieldIsNull(dataSource, SchedulerConfig.ENCRYPTED_PASSWORD.getPreferredName(), config.getEncryptedPassword());
        checkFieldIsNull(dataSource, SchedulerConfig.INDEXES.getPreferredName(), config.getIndexes());
        checkFieldIsNull(dataSource, SchedulerConfig.TYPES.getPreferredName(), config.getTypes());
        checkFieldIsNull(dataSource, SchedulerConfig.RETRIEVE_WHOLE_SOURCE.getPreferredName(), config.getRetrieveWholeSource());
        checkFieldIsNull(dataSource, SchedulerConfig.AGGREGATIONS.getPreferredName(), config.getAggregations());
        checkFieldIsNull(dataSource, SchedulerConfig.QUERY.getPreferredName(), config.getQuery());
        checkFieldIsNull(dataSource, SchedulerConfig.SCRIPT_FIELDS.getPreferredName(), config.getScriptFields());
        checkFieldIsNull(dataSource, SchedulerConfig.SCROLL_SIZE.getPreferredName(), config.getScrollSize());
    }

    private static void verifyElasticsearchSchedulerConfig(SchedulerConfig.Builder config, DataSource dataSource) {
        checkUrl(SchedulerConfig.BASE_URL.getPreferredName(), config.getBaseUrl());
        checkUserPass(config.getUsername(), config.getPassword(), config.getEncryptedPassword());
        checkFieldIsNotNullOrEmpty(SchedulerConfig.INDEXES.getPreferredName(), config.getIndexes());
        checkFieldIsNotNullOrEmpty(SchedulerConfig.TYPES.getPreferredName(), config.getTypes());
        checkNoMultipleAggregations(config);
        if (Boolean.TRUE.equals(config.getRetrieveWholeSource())) {
            // Not allowed script_fields when retrieveWholeSource is true
            checkFieldIsNull(dataSource, SchedulerConfig.SCRIPT_FIELDS.getPreferredName(), config.getScriptFields());
        }
        checkFieldIsNotNegative(SchedulerConfig.SCROLL_SIZE.getPreferredName(), config.getScrollSize());
        checkFieldIsNull(dataSource, SchedulerConfig.FILE_PATH.getPreferredName(), config.getFilePath());
        checkFieldIsNull(dataSource, SchedulerConfig.TAIL_FILE.getPreferredName(), config.getTailFile());
    }

    private static void checkUserPass(String username, String password, String encryptedPassword) {
        boolean isNoPasswordSet = password == null && encryptedPassword == null;
        boolean isMultiplePasswordSet = password != null && encryptedPassword != null;

        if (username == null && isNoPasswordSet) {
            // It's acceptable to have no credentials
            return;
        }

        if (username == null || isNoPasswordSet) {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_INCOMPLETE_CREDENTIALS);
            throw ExceptionsHelper.invalidRequestException(msg, ErrorCodes.SCHEDULER_INCOMPLETE_CREDENTIALS);
        }

        if (isMultiplePasswordSet) {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_MULTIPLE_PASSWORDS);
            throw ExceptionsHelper.invalidRequestException(msg, ErrorCodes.SCHEDULER_MULTIPLE_PASSWORDS);
        }
    }

    private static void checkNoMultipleAggregations(SchedulerConfig.Builder config) {
        if (config.getAggregations() != null && config.getAggs() != null) {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_MULTIPLE_AGGREGATIONS);
            throw ExceptionsHelper.invalidRequestException(msg, ErrorCodes.SCHEDULER_MULTIPLE_AGGREGATIONS);
        }
    }

    private static void checkFieldIsNull(DataSource dataSource, String fieldName, Object value) {
        if (value != null) {
            String msg = Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_FIELD_NOT_SUPPORTED, fieldName, dataSource.toString());
            throw ExceptionsHelper.invalidRequestException(msg, ErrorCodes.SCHEDULER_FIELD_NOT_SUPPORTED_FOR_DATASOURCE);
        }
    }

    private static void checkFieldIsNotNullOrEmpty(String fieldName, String value) {
        if (value == null || value.isEmpty()) {
            throwInvalidOptionValue(fieldName, value);
        }
    }

    private static void throwInvalidOptionValue(String fieldName, Object value) {
        String msg = Messages.getMessage(Messages.JOB_CONFIG_SCHEDULER_INVALID_OPTION_VALUE, fieldName, value);
        throw ExceptionsHelper.invalidRequestException(msg, ErrorCodes.SCHEDULER_INVALID_OPTION_VALUE);
    }

    private static void checkFieldIsNotNullOrEmpty(String fieldName, List<String> value) {
        if (value != null) {
            for (String item : value)            {
                if (item != null && !item.isEmpty()) {
                    return;
                }
            }
        }

        throwInvalidOptionValue(fieldName, value);
    }

    private static void checkFieldIsNotNegative(String fieldName, Number value) {
        if (value != null && value.longValue() < 0) {
            throwInvalidOptionValue(fieldName, value);
        }
    }

    private static void checkUrl(String fieldName, String value) {
        try {
            new URL(value);
        } catch (MalformedURLException e) {
            throwInvalidOptionValue(fieldName, value);
        }
    }
}
