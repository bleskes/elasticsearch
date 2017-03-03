/*
 * ELASTICSEARCH CONFIDENTIAL
 *  __________________
 *
 * [2017] Elasticsearch Incorporated. All Rights Reserved.
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

package org.elasticsearch.xpack.security.action.user;

import java.io.IOException;

import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.xpack.security.authz.RoleDescriptor;
import org.elasticsearch.xpack.security.authz.RoleDescriptor.IndicesPrivileges;

/**
 * Request builder for checking a user's privileges
 */
public class HasPrivilegesRequestBuilder
        extends ActionRequestBuilder<HasPrivilegesRequest, HasPrivilegesResponse, HasPrivilegesRequestBuilder> {

    public HasPrivilegesRequestBuilder(ElasticsearchClient client) {
        super(client, HasPrivilegesAction.INSTANCE, new HasPrivilegesRequest());
    }

    /**
     * Set the username of the user that should enabled or disabled. Must not be {@code null}
     */
    public HasPrivilegesRequestBuilder username(String username) {
        request.username(username);
        return this;
    }

    /**
     * Set whether the user should be enabled or not
     */
    public HasPrivilegesRequestBuilder source(String username, BytesReference source, XContentType xContentType) throws IOException {
        final RoleDescriptor role = RoleDescriptor.parsePrivilegesCheck(username + "/has_privileges", source, xContentType);
        request.username(username);
        request.indexPrivileges(role.getIndicesPrivileges());
        request.clusterPrivileges(role.getClusterPrivileges());
        return this;
    }
}
