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

package org.elasticsearch.shield.rest.action.user;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.rest.action.support.RestBuilderListener;
import org.elasticsearch.shield.User;
import org.elasticsearch.shield.action.user.GetUsersResponse;
import org.elasticsearch.shield.client.SecurityClient;

/**
 * Rest action to retrieve a user from the shield index
 */
public class RestGetUsersAction extends BaseRestHandler {

    @Inject
    public RestGetUsersAction(Settings settings, RestController controller, Client client) {
        super(settings, client);
        controller.registerHandler(RestRequest.Method.GET, "/_shield/user/", this);
        controller.registerHandler(RestRequest.Method.GET, "/_shield/user/{username}", this);
    }

    @Override
    protected void handleRequest(RestRequest request, final RestChannel channel, Client client) throws Exception {
        String[] usernames = request.paramAsStringArray("username", Strings.EMPTY_ARRAY);

        new SecurityClient(client).prepareGetUsers().usernames(usernames).execute(new RestBuilderListener<GetUsersResponse>(channel) {
            @Override
            public RestResponse buildResponse(GetUsersResponse getUsersResponse, XContentBuilder builder) throws Exception {
                builder.startObject();
                builder.field("found", getUsersResponse.hasUsers());
                builder.startArray("users");
                for (User user : getUsersResponse.users()) {
                    user.toXContent(builder, ToXContent.EMPTY_PARAMS);
                }
                builder.endArray();
                builder.endObject();
                return new BytesRestResponse(RestStatus.OK, builder);
            }
        });
    }
}
