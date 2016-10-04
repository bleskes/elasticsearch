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

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.ResourceNotFoundException;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.xpack.prelert.job.errorcodes.ErrorCodes;

public class ExceptionsHelper {

    public static ElasticsearchStatusException invalidRequestException(String msg, ErrorCodes errorCode) {
        ElasticsearchStatusException e =  new ElasticsearchStatusException(msg, RestStatus.BAD_REQUEST);
        e.addHeader("errorCode", errorCode.getValueString());
        return e;
    }

    public static ResourceNotFoundException missingException(String msg, ErrorCodes errorCode) {
        ResourceNotFoundException e =  new ResourceNotFoundException(msg, RestStatus.BAD_REQUEST);
        e.addHeader("errorCode", errorCode.getValueString());
        return e;
    }

}
