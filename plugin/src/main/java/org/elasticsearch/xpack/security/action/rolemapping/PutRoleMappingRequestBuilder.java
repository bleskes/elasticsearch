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

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.support.WriteRequestBuilder;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.xpack.security.authc.support.mapper.ExpressionRoleMapping;
import org.elasticsearch.xpack.security.authc.support.mapper.expressiondsl.RoleMapperExpression;

/**
 * Builder for requests to add/update a role-mapping to the native store
 *
 * @see org.elasticsearch.xpack.security.authc.support.mapper.NativeRoleMappingStore
 */
public class PutRoleMappingRequestBuilder extends ActionRequestBuilder<PutRoleMappingRequest,
        PutRoleMappingResponse, PutRoleMappingRequestBuilder> implements
        WriteRequestBuilder<PutRoleMappingRequestBuilder> {

    public PutRoleMappingRequestBuilder(ElasticsearchClient client, PutRoleMappingAction action) {
        super(client, action, new PutRoleMappingRequest());
    }

    /**
     * Populate the put role request from the source and the role's name
     */
    public PutRoleMappingRequestBuilder source(String name, BytesReference source,
                                               XContentType xContentType) throws IOException {
        ExpressionRoleMapping mapping = ExpressionRoleMapping.parse(name, source, xContentType);
        request.setName(name);
        request.setEnabled(mapping.isEnabled());
        request.setRoles(mapping.getRoles());
        request.setRules(mapping.getExpression());
        request.setMetadata(mapping.getMetadata());
        return this;
    }

    public PutRoleMappingRequestBuilder name(String name) {
        request.setName(name);
        return this;
    }

    public PutRoleMappingRequestBuilder roles(String... roles) {
        request.setRoles(Arrays.asList(roles));
        return this;
    }

    public PutRoleMappingRequestBuilder expression(RoleMapperExpression expression) {
        request.setRules(expression);
        return this;
    }

    public PutRoleMappingRequestBuilder enabled(boolean enabled) {
        request.setEnabled(enabled);
        return this;
    }

    public PutRoleMappingRequestBuilder metadata(Map<String, Object> metadata) {
        request.setMetadata(metadata);
        return this;
    }
}
