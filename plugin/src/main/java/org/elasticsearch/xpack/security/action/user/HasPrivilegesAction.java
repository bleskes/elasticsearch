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

import java.util.Collections;

import org.elasticsearch.action.Action;
import org.elasticsearch.client.ElasticsearchClient;

/**
 * This action is testing whether a user has the specified
 * {@link org.elasticsearch.xpack.security.authz.RoleDescriptor.IndicesPrivileges privileges}
 */
public class HasPrivilegesAction extends Action<HasPrivilegesRequest, HasPrivilegesResponse, HasPrivilegesRequestBuilder> {

    public static final HasPrivilegesAction INSTANCE = new HasPrivilegesAction();
    public static final String NAME = "cluster:admin/xpack/security/user/has_privileges";

    private HasPrivilegesAction() {
        super(NAME);
    }

    @Override
    public HasPrivilegesRequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new HasPrivilegesRequestBuilder(client);
    }

    @Override
    public HasPrivilegesResponse newResponse() {
        return new HasPrivilegesResponse();
    }
}
