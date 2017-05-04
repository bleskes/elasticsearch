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

/**
 * Action to retrieve one or more role-mappings from X-Pack security

 * @see org.elasticsearch.xpack.security.authc.support.mapper.NativeRoleMappingStore
 */
public class GetRoleMappingsAction extends Action<GetRoleMappingsRequest, GetRoleMappingsResponse, GetRoleMappingsRequestBuilder> {

    public static final GetRoleMappingsAction INSTANCE = new GetRoleMappingsAction();
    public static final String NAME = "cluster:admin/xpack/security/role_mapping/get";

    private GetRoleMappingsAction() {
        super(NAME);
    }

    @Override
    public GetRoleMappingsRequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new GetRoleMappingsRequestBuilder(client, this);
    }

    @Override
    public GetRoleMappingsResponse newResponse() {
        return new GetRoleMappingsResponse();
    }
}
