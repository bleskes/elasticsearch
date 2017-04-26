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

import org.elasticsearch.action.Action;
import org.elasticsearch.client.ElasticsearchClient;

/**
 * Action for creating a new token
 */
public final class CreateTokenAction extends Action<CreateTokenRequest, CreateTokenResponse, CreateTokenRequestBuilder> {

    public static final String NAME = "cluster:admin/xpack/security/token/create";
    public static final CreateTokenAction INSTANCE = new CreateTokenAction();

    private CreateTokenAction() {
        super(NAME);
    }

    @Override
    public CreateTokenRequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new CreateTokenRequestBuilder(client);
    }

    @Override
    public CreateTokenResponse newResponse() {
        return new CreateTokenResponse();
    }
}
