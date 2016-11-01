/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
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
import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;
import org.elasticsearch.xpack.prelert.job.messages.Messages;

// NORELEASE: add cause exceptions!
public class ExceptionsHelper {

    public static ElasticsearchStatusException invalidRequestException(String msg, ErrorCodes errorCode) {
        ElasticsearchStatusException e =  new ElasticsearchStatusException(msg, RestStatus.BAD_REQUEST);
        e.addHeader("errorCode", errorCode.getValueString());
        return e;
    }

    public static ElasticsearchStatusException invalidRequestException(String msg, ErrorCodes errorCode, Throwable cause) {
        ElasticsearchStatusException e =  new ElasticsearchStatusException(msg, RestStatus.BAD_REQUEST, cause);
        e.addHeader("errorCode", errorCode.getValueString());
        return e;
    }

    public static ResourceNotFoundException missingException(String jobId) {
        String msg = Messages.getMessage(Messages.JOB_UNKNOWN_ID, jobId);
        ResourceNotFoundException e =  new ResourceNotFoundException(msg, RestStatus.BAD_REQUEST);
        e.addHeader("errorCode", ErrorCodes.MISSING_JOB_ERROR.getValueString());
        return e;
    }

    public static ElasticsearchStatusException jobAlreadyExists(String jobId) {
        String msg = Messages.getMessage(Messages.JOB_CONFIG_ID_ALREADY_TAKEN, jobId);
        ElasticsearchStatusException e = new ElasticsearchStatusException(msg, RestStatus.BAD_REQUEST);
        e.addHeader("errorCode", ErrorCodes.JOB_ID_TAKEN.getValueString());
        return e;
    }

    public static ElasticsearchParseException parseException(String msg, ErrorCodes errorCode) {
        ElasticsearchParseException e = new ElasticsearchParseException(msg);
        e.addHeader("errorCode", errorCode.getValueString());
        return e;
    }

    public static ElasticsearchParseException parseException(String msg, ErrorCodes errorCode, Exception cause) {
        ElasticsearchParseException e = new ElasticsearchParseException(msg, cause);
        e.addHeader("errorCode", errorCode.getValueString());
        return e;
    }

    public static ElasticsearchException serverError(String msg, ErrorCodes errorCode) {
        ElasticsearchException e = new ElasticsearchException(msg);
        e.addHeader("errorCode", errorCode.getValueString());
        return e;
    }

    public static ElasticsearchException serverError(String msg, Throwable cause, ErrorCodes errorCode) {
        ElasticsearchException e = new ElasticsearchException(msg, cause);
        e.addHeader("errorCode", errorCode.getValueString());
        return e;
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
