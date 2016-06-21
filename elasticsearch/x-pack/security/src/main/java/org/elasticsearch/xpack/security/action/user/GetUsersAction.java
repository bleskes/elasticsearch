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

import org.elasticsearch.action.Action;
import org.elasticsearch.client.ElasticsearchClient;

/**
 * Action for retrieving a user from the security index
 */
public class GetUsersAction extends Action<GetUsersRequest, GetUsersResponse, GetUsersRequestBuilder> {

    public static final GetUsersAction INSTANCE = new GetUsersAction();
    public static final String NAME = "cluster:admin/xpack/security/user/get";

    protected GetUsersAction() {
        super(NAME);
    }

    @Override
    public GetUsersRequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new GetUsersRequestBuilder(client, this);
    }

    @Override
    public GetUsersResponse newResponse() {
        return new GetUsersResponse();
    }
}
