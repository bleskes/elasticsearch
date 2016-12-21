/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.prelert.utils;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.ResourceAlreadyExistsException;
import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.xpack.prelert.job.messages.Messages;

// NORELEASE: add cause exceptions!
public class ExceptionsHelper {

    public static ResourceNotFoundException missingJobException(String jobId) {
        return new ResourceNotFoundException(Messages.getMessage(Messages.JOB_UNKNOWN_ID, jobId));
    }

    public static ResourceAlreadyExistsException jobAlreadyExists(String jobId) {
        throw new ResourceAlreadyExistsException(Messages.getMessage(Messages.JOB_CONFIG_ID_ALREADY_TAKEN, jobId));
    }

    public static ResourceNotFoundException missingSchedulerException(String schedulerId) {
        throw new ResourceNotFoundException(Messages.getMessage(Messages.SCHEDULER_NOT_FOUND, schedulerId));
    }

    public static ElasticsearchException serverError(String msg) {
        return new ElasticsearchException(msg);
    }

    public static ElasticsearchException serverError(String msg, Throwable cause) {
        return new ElasticsearchException(msg, cause);
    }

    public static ElasticsearchStatusException conflictStatusException(String msg) {
        return new ElasticsearchStatusException(msg, RestStatus.CONFLICT);
    }

    public static ElasticsearchParseException parseException(ParseField parseField, Throwable cause) {
        throw new ElasticsearchParseException("Failed to parse [" + parseField.getPreferredName() + "]", cause);
    }

    /**
     * A more REST-friendly Object.requireNonNull()
     */
    public static <T> T requireNonNull(T obj, String paramName) {
        if (obj == null) {
            throw new IllegalArgumentException("[" + paramName + "] must not be null.");
        }
        return obj;
    }
}
