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

package org.elasticsearch.shield.action.admin.role;

import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.shield.authz.RoleDescriptor;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Builder for requests to add a role to the administrative index
 */
public class AddRoleRequestBuilder extends ActionRequestBuilder<AddRoleRequest, AddRoleResponse, AddRoleRequestBuilder> {

    public AddRoleRequestBuilder(ElasticsearchClient client) {
        this(client, AddRoleAction.INSTANCE);
    }

    public AddRoleRequestBuilder(ElasticsearchClient client, AddRoleAction action) {
        super(client, action, new AddRoleRequest());
    }

    public AddRoleRequestBuilder name(String name) {
        request.name(name);
        return this;
    }

    public AddRoleRequestBuilder cluster(String... cluster) {
        request.cluster(Arrays.asList(cluster));
        return this;
    }

    public AddRoleRequestBuilder runAs(String... runAsUsers) {
        request.runAs(Arrays.asList(runAsUsers));
        return this;
    }

    public AddRoleRequestBuilder addIndices(String[] indices, String[] privileges, @Nullable String[] fields, @Nullable BytesReference query) {
        request.addIndex(indices, privileges, fields, query);
        return this;
    }
}
