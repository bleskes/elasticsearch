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

import org.elasticsearch.action.Action;
import org.elasticsearch.client.ElasticsearchClient;

/**
 * Action for deleting a role from the security index
 */
public class DeleteRoleAction extends Action<DeleteRoleRequest, DeleteRoleResponse, DeleteRoleRequestBuilder> {

    public static final DeleteRoleAction INSTANCE = new DeleteRoleAction();
    public static final String NAME = "cluster:admin/xpack/security/role/delete";


    protected DeleteRoleAction() {
        super(NAME);
    }

    @Override
    public DeleteRoleRequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new DeleteRoleRequestBuilder(client, this);
    }

    @Override
    public DeleteRoleResponse newResponse() {
        return new DeleteRoleResponse();
    }
}
