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

import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.client.ElasticsearchClient;

/**
 * Builder for a request to retrieve role-mappings from X-Pack security
 *
 * @see org.elasticsearch.xpack.security.authc.support.mapper.NativeRoleMappingStore
 */
public class GetRoleMappingsRequestBuilder extends ActionRequestBuilder<GetRoleMappingsRequest,
        GetRoleMappingsResponse, GetRoleMappingsRequestBuilder> {

    public GetRoleMappingsRequestBuilder(ElasticsearchClient client, GetRoleMappingsAction action) {
        super(client, action, new GetRoleMappingsRequest());
    }

    public GetRoleMappingsRequestBuilder names(String... names) {
        request.setNames(names);
        return this;
    }
}
