/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */
package org.elasticsearch.xpack.prelert.utils;

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
