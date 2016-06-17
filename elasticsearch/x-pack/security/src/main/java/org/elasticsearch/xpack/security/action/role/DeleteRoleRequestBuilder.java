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
 * A builder for requests to delete a role from the security index
 */
public class DeleteRoleRequestBuilder extends ActionRequestBuilder<DeleteRoleRequest, DeleteRoleResponse, DeleteRoleRequestBuilder> {

    public DeleteRoleRequestBuilder(ElasticsearchClient client) {
        this(client, DeleteRoleAction.INSTANCE);
    }

    public DeleteRoleRequestBuilder(ElasticsearchClient client, DeleteRoleAction action) {
        super(client, action, new DeleteRoleRequest());
    }

    public DeleteRoleRequestBuilder name(String name) {
        request.name(name);
        return this;
    }

    public DeleteRoleRequestBuilder refresh(boolean refresh) {
        request.refresh(refresh);
        return this;
    }
}
