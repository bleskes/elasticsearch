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

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.shield.ShieldPlugin;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AuthenticationException extends ElasticsearchException.WithRestHeadersException {

    public static final Map<String, List<String>> HEADERS = Collections.singletonMap("WWW-Authenticate", Collections.singletonList("Basic realm=\"" + ShieldPlugin.NAME + "\""));

    public AuthenticationException(String msg) {
        this(msg, null);
    }

    public AuthenticationException(String msg, Throwable cause) {
        super(msg, cause, HEADERS);
    }

    @Override
    public RestStatus status() {
        return RestStatus.UNAUTHORIZED;
    }
}
