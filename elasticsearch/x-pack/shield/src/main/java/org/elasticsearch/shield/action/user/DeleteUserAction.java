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
 * Action for deleting a native user.
 */
public class DeleteUserAction extends Action<DeleteUserRequest, DeleteUserResponse, DeleteUserRequestBuilder> {

    public static final DeleteUserAction INSTANCE = new DeleteUserAction();
    public static final String NAME = "cluster:admin/xpack/security/user/delete";

    protected DeleteUserAction() {
        super(NAME);
    }

    @Override
    public DeleteUserRequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new DeleteUserRequestBuilder(client, this);
    }

    @Override
    public DeleteUserResponse newResponse() {
        return new DeleteUserResponse();
    }
}
