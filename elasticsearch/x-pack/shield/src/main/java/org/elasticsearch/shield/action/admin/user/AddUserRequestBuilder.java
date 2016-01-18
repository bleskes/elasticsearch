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

package org.elasticsearch.shield.action.admin.user;

import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.shield.authc.support.Hasher;
import org.elasticsearch.shield.authc.support.SecuredString;

public class AddUserRequestBuilder extends ActionRequestBuilder<AddUserRequest, AddUserResponse, AddUserRequestBuilder> {

    private final Hasher hasher = Hasher.BCRYPT;

    public AddUserRequestBuilder(ElasticsearchClient client) {
        this(client, AddUserAction.INSTANCE);
    }

    public AddUserRequestBuilder(ElasticsearchClient client, AddUserAction action) {
        super(client, action, new AddUserRequest());
    }

    public AddUserRequestBuilder username(String username) {
        request.username(username);
        return this;
    }

    public AddUserRequestBuilder roles(String... roles) {
        request.roles(roles);
        return this;
    }

    public AddUserRequestBuilder password(String password) {
        request.passwordHash(hasher.hash(new SecuredString(password.toCharArray())));
        return this;
    }
}
