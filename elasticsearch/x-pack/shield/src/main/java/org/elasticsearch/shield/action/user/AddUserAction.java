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

package org.elasticsearch.shield.action.user;

import org.elasticsearch.action.Action;
import org.elasticsearch.client.ElasticsearchClient;

/**
 * Action for adding a user to the shield administrative index
 */
public class AddUserAction extends Action<AddUserRequest, AddUserResponse, AddUserRequestBuilder> {

    public static final AddUserAction INSTANCE = new AddUserAction();
    public static final String NAME = "cluster:admin/shield/user/add";


    protected AddUserAction() {
        super(NAME);
    }

    @Override
    public AddUserRequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new AddUserRequestBuilder(client, this);
    }

    @Override
    public AddUserResponse newResponse() {
        return new AddUserResponse();
    }
}
