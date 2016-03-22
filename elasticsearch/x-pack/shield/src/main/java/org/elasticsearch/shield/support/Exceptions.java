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

package org.elasticsearch.shield.support;

import org.elasticsearch.ElasticsearchSecurityException;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.shield.Security;

/**
 *
 */
public class Exceptions {

    private Exceptions() {
    }

    public static ElasticsearchSecurityException authenticationError(String msg, Throwable cause, Object... args) {
        ElasticsearchSecurityException e = new ElasticsearchSecurityException(msg, RestStatus.UNAUTHORIZED, cause, args);
        e.addHeader("WWW-Authenticate", "Basic realm=\"" + Security.NAME + "\"");
        return e;
    }

    public static ElasticsearchSecurityException authenticationError(String msg, Object... args) {
        ElasticsearchSecurityException e = new ElasticsearchSecurityException(msg, RestStatus.UNAUTHORIZED, args);
        e.addHeader("WWW-Authenticate", "Basic realm=\"" + Security.NAME + "\"");
        return e;
    }

    public static ElasticsearchSecurityException authorizationError(String msg, Object... args) {
        return new ElasticsearchSecurityException(msg, RestStatus.FORBIDDEN, args);
    }
}
