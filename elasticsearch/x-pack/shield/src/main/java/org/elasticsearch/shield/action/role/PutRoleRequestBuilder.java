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

package org.elasticsearch.shield.action.role;

import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.shield.authz.RoleDescriptor;

/**
 * Builder for requests to add a role to the administrative index
 */
public class PutRoleRequestBuilder extends ActionRequestBuilder<PutRoleRequest, PutRoleResponse, PutRoleRequestBuilder> {

    public PutRoleRequestBuilder(ElasticsearchClient client) {
        this(client, PutRoleAction.INSTANCE);
    }

    public PutRoleRequestBuilder(ElasticsearchClient client, PutRoleAction action) {
        super(client, action, new PutRoleRequest());
    }

    public PutRoleRequestBuilder source(String name, BytesReference source) throws Exception {
        RoleDescriptor descriptor = RoleDescriptor.parse(name, source);
        assert name.equals(descriptor.getName());
        request.name(name);
        request.cluster(descriptor.getClusterPrivileges());
        request.addIndex(descriptor.getIndicesPrivileges());
        request.runAs(descriptor.getRunAs());
        return this;
    }

    public PutRoleRequestBuilder name(String name) {
        request.name(name);
        return this;
    }

    public PutRoleRequestBuilder cluster(String... cluster) {
        request.cluster(cluster);
        return this;
    }

    public PutRoleRequestBuilder runAs(String... runAsUsers) {
        request.runAs(runAsUsers);
        return this;
    }

    public PutRoleRequestBuilder addIndices(String[] indices, String[] privileges,
            @Nullable String[] fields, @Nullable BytesReference query) {
        request.addIndex(indices, privileges, fields, query);
        return this;
    }

    public PutRoleRequestBuilder refresh(boolean refresh) {
        request.refresh(refresh);
        return this;
    }
}
