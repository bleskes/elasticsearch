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
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.settings.SecureString;

/**
 * Request builder used to populate a {@link CreateTokenRequest}
 */
public final class CreateTokenRequestBuilder
        extends ActionRequestBuilder<CreateTokenRequest, CreateTokenResponse, CreateTokenRequestBuilder> {

    public CreateTokenRequestBuilder(ElasticsearchClient client) {
        super(client, CreateTokenAction.INSTANCE, new CreateTokenRequest());
    }

    /**
     * Specifies the grant type for this request. Currently only <code>password</code> is supported
     */
    public CreateTokenRequestBuilder setGrantType(String grantType) {
        request.setGrantType(grantType);
        return this;
    }

    /**
     * Set the username to be used for authentication with a password grant
     */
    public CreateTokenRequestBuilder setUsername(String username) {
        request.setUsername(username);
        return this;
    }

    /**
     * Set the password credentials associated with the user. These credentials will be used for
     * authentication and the resulting token will be for this user
     */
    public CreateTokenRequestBuilder setPassword(SecureString password) {
        request.setPassword(password);
        return this;
    }

    /**
     * Set the scope of the access token. A <code>null</code> scope implies the default scope. If
     * the requested scope differs from the scope of the token, the token's scope will be returned
     * in the response
     */
    public CreateTokenRequestBuilder setScope(@Nullable String scope) {
        request.setScope(scope);
        return this;
    }
}
