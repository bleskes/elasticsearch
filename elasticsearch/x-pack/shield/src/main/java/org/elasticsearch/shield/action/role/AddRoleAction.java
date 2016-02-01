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

import org.elasticsearch.action.Action;
import org.elasticsearch.client.ElasticsearchClient;

/**
 * Action for adding a role to the shield administrative index
 */
public class AddRoleAction extends Action<AddRoleRequest, AddRoleResponse, AddRoleRequestBuilder> {

    public static final AddRoleAction INSTANCE = new AddRoleAction();
    public static final String NAME = "cluster:admin/shield/role/add";


    protected AddRoleAction() {
        super(NAME);
    }

    @Override
    public AddRoleRequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new AddRoleRequestBuilder(client, this);
    }

    @Override
    public AddRoleResponse newResponse() {
        return new AddRoleResponse();
    }
}
