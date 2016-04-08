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

import org.elasticsearch.ElasticsearchSecurityException;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.transport.TransportMessage;

import static org.elasticsearch.shield.support.Exceptions.authenticationError;

/**
 * The default implementation of a {@link AuthenticationFailureHandler}. This handler will return an exception with a
 * RestStatus of 401 and the WWW-Authenticate header with a Basic challenge.
 */
public class DefaultAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public ElasticsearchSecurityException failedAuthentication(RestRequest request, AuthenticationToken token,
                                                               ThreadContext context) {
        return authenticationError("unable to authenticate user [{}] for REST request [{}]", token.principal(), request.uri());
    }

    @Override
    public ElasticsearchSecurityException failedAuthentication(TransportMessage message, AuthenticationToken token, String action,
                                                               ThreadContext context) {
        return authenticationError("unable to authenticate user [{}] for action [{}]", token.principal(), action);
    }

    @Override
    public ElasticsearchSecurityException exceptionProcessingRequest(RestRequest request, Exception e, ThreadContext context) {
        if (e instanceof ElasticsearchSecurityException) {
            assert ((ElasticsearchSecurityException) e).status() == RestStatus.UNAUTHORIZED;
            assert ((ElasticsearchSecurityException) e).getHeader("WWW-Authenticate").size() == 1;
            return (ElasticsearchSecurityException) e;
        }
        return authenticationError("error attempting to authenticate request", e);
    }

    @Override
    public ElasticsearchSecurityException exceptionProcessingRequest(TransportMessage message, String action, Exception e,
                                                                     ThreadContext context) {
        if (e instanceof ElasticsearchSecurityException) {
            assert ((ElasticsearchSecurityException) e).status() == RestStatus.UNAUTHORIZED;
            assert ((ElasticsearchSecurityException) e).getHeader("WWW-Authenticate").size() == 1;
            return (ElasticsearchSecurityException) e;
        }
        return authenticationError("error attempting to authenticate request", e);
    }

    @Override
    public ElasticsearchSecurityException missingToken(RestRequest request, ThreadContext context) {
        return authenticationError("missing authentication token for REST request [{}]", request.uri());
    }

    @Override
    public ElasticsearchSecurityException missingToken(TransportMessage message, String action, ThreadContext context) {
        return authenticationError("missing authentication token for action [{}]", action);
    }

    @Override
    public ElasticsearchSecurityException authenticationRequired(String action, ThreadContext context) {
        return authenticationError("action [{}] requires authentication", action);
    }
}
