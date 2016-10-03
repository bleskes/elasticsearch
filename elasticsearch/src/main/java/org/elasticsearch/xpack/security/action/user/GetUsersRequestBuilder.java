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

package org.elasticsearch.xpack.security.action.user;

import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.client.ElasticsearchClient;

public class GetUsersRequestBuilder extends ActionRequestBuilder<GetUsersRequest, GetUsersResponse, GetUsersRequestBuilder> {

    public GetUsersRequestBuilder(ElasticsearchClient client) {
        this(client, GetUsersAction.INSTANCE);
    }

    public GetUsersRequestBuilder(ElasticsearchClient client, GetUsersAction action) {
        super(client, action, new GetUsersRequest());
    }

    public GetUsersRequestBuilder usernames(String... usernames) {
        request.usernames(usernames);
        return this;
    }
}
