/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2017] Elasticsearch Incorporated. All Rights Reserved.
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

package org.elasticsearch.xpack.security.action.token;

import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.client.ElasticsearchClient;

/**
 * Request builder that is used to populate a {@link InvalidateTokenRequest}
 */
public final class InvalidateTokenRequestBuilder
        extends ActionRequestBuilder<InvalidateTokenRequest, InvalidateTokenResponse, InvalidateTokenRequestBuilder> {

    public InvalidateTokenRequestBuilder(ElasticsearchClient client) {
        super(client, InvalidateTokenAction.INSTANCE, new InvalidateTokenRequest());
    }

    /**
     * The string representation of the token that is being invalidated. This is the value returned
     * from a create token request.
     */
    public InvalidateTokenRequestBuilder setTokenString(String token) {
        request.setTokenString(token);
        return this;
    }
}
