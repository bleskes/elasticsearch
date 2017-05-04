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

package org.elasticsearch.xpack.security.action.rolemapping;

import org.elasticsearch.action.Action;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.xpack.security.action.role.DeleteRoleRequest;
import org.elasticsearch.xpack.security.action.role.DeleteRoleRequestBuilder;
import org.elasticsearch.xpack.security.action.role.DeleteRoleResponse;

/**
 * Action for deleting a role-mapping from the
 * {@link org.elasticsearch.xpack.security.authc.support.mapper.NativeRoleMappingStore}
 */
public class DeleteRoleMappingAction extends Action<DeleteRoleMappingRequest,
        DeleteRoleMappingResponse, DeleteRoleMappingRequestBuilder> {

    public static final DeleteRoleMappingAction INSTANCE = new DeleteRoleMappingAction();
    public static final String NAME = "cluster:admin/xpack/security/role_mapping/delete";

    private DeleteRoleMappingAction() {
        super(NAME);
    }

    @Override
    public DeleteRoleMappingRequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new DeleteRoleMappingRequestBuilder(client, this);
    }

    @Override
    public DeleteRoleMappingResponse newResponse() {
        return new DeleteRoleMappingResponse();
    }
}
