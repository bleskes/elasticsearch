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
import org.elasticsearch.xpack.security.action.role.PutRoleRequest;
import org.elasticsearch.xpack.security.action.role.PutRoleRequestBuilder;
import org.elasticsearch.xpack.security.action.role.PutRoleResponse;

/**
 * Action for adding a role to the security index
 */
public class PutRoleMappingAction extends Action<PutRoleMappingRequest, PutRoleMappingResponse,
        PutRoleMappingRequestBuilder> {

    public static final PutRoleMappingAction INSTANCE = new PutRoleMappingAction();
    public static final String NAME = "cluster:admin/xpack/security/role_mapping/put";

    private PutRoleMappingAction() {
        super(NAME);
    }

    @Override
    public PutRoleMappingRequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new PutRoleMappingRequestBuilder(client, this);
    }

    @Override
    public PutRoleMappingResponse newResponse() {
        return new PutRoleMappingResponse();
    }
}
