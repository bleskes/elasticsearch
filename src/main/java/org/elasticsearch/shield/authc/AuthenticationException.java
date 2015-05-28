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

package org.elasticsearch.shield.authc;

import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.shield.ShieldException;
import org.elasticsearch.shield.ShieldPlugin;

/**
 *
 */
public class AuthenticationException extends ShieldException {

    public static final Tuple<String, String[]> BASIC_AUTH_HEADER = Tuple.tuple("WWW-Authenticate", new String[]{"Basic realm=\"" + ShieldPlugin.NAME + "\""});

    public AuthenticationException(String msg) {
        super(msg, BASIC_AUTH_HEADER);
    }

    public AuthenticationException(String msg, Throwable cause) {
        super(msg, cause, BASIC_AUTH_HEADER);
    }

    @Override
    public RestStatus status() {
        return RestStatus.UNAUTHORIZED;
    }
}
