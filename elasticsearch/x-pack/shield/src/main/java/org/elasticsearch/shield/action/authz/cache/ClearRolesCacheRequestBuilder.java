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

package org.elasticsearch.shield.action.authz.cache;

import org.elasticsearch.action.support.nodes.NodesOperationRequestBuilder;
import org.elasticsearch.client.ElasticsearchClient;

/**
 * Request builder for the {@link ClearRolesCacheRequest}
 */
public class ClearRolesCacheRequestBuilder extends NodesOperationRequestBuilder<ClearRolesCacheRequest, ClearRolesCacheResponse, ClearRolesCacheRequestBuilder> {

    public ClearRolesCacheRequestBuilder(ElasticsearchClient client) {
        this(client, ClearRolesCacheAction.INSTANCE, new ClearRolesCacheRequest());
    }

    public ClearRolesCacheRequestBuilder(ElasticsearchClient client, ClearRolesCacheAction action, ClearRolesCacheRequest request) {
        super(client, action, request);
    }

    /**
     * Set the roles to be cleared
     * @param roles the names of the roles that should be cleared
     * @return the builder instance
     */
    public ClearRolesCacheRequestBuilder roles(String... roles) {
        request.roles(roles);
        return this;
    }
}
