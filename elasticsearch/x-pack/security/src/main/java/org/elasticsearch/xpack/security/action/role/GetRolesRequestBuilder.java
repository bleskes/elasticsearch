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

package org.elasticsearch.xpack.security.action.role;

import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.client.ElasticsearchClient;

/**
 * Builder for requests to retrieve a role from the security index
 */
public class GetRolesRequestBuilder extends ActionRequestBuilder<GetRolesRequest, GetRolesResponse, GetRolesRequestBuilder> {

    public GetRolesRequestBuilder(ElasticsearchClient client) {
        this(client, GetRolesAction.INSTANCE);
    }

    public GetRolesRequestBuilder(ElasticsearchClient client, GetRolesAction action) {
        super(client, action, new GetRolesRequest());
    }

    public GetRolesRequestBuilder names(String... names) {
        request.names(names);
        return this;
    }
}
