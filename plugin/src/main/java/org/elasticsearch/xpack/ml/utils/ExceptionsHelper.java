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
package org.elasticsearch.xpack.ml.utils;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.ResourceAlreadyExistsException;
import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.action.search.ShardSearchFailure;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchShardTarget;
import org.elasticsearch.xpack.ml.job.messages.Messages;

public class ExceptionsHelper {

    public static ResourceNotFoundException missingJobException(String jobId) {
        return new ResourceNotFoundException(Messages.getMessage(Messages.JOB_UNKNOWN_ID, jobId));
    }

    public static ResourceAlreadyExistsException jobAlreadyExists(String jobId) {
        throw new ResourceAlreadyExistsException(Messages.getMessage(Messages.JOB_CONFIG_ID_ALREADY_TAKEN, jobId));
    }

    public static ResourceNotFoundException missingDatafeedException(String datafeedId) {
        throw new ResourceNotFoundException(Messages.getMessage(Messages.DATAFEED_NOT_FOUND, datafeedId));
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

    /**
     * Creates an error message that explains there are shard failures, displays info
     * for the first failure (shard/reason) and kindly asks to see more info in the logs
     */
    public static String shardFailuresToErrorMsg(String jobId, ShardSearchFailure[] shardFailures) {
        if (shardFailures == null || shardFailures.length == 0) {
            throw new IllegalStateException("Invalid call with null or empty shardFailures");
        }
        SearchShardTarget shardTarget = shardFailures[0].shard();
        return "[" + jobId + "] Search request returned shard failures; first failure: shard ["
                + (shardTarget == null ? "_na" : shardTarget) + "], reason ["
                + shardFailures[0].reason() + "]; see logs for more info";
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
