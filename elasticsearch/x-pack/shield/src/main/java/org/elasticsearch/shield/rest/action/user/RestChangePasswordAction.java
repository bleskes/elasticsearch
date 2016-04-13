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
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.rest.action.support.RestBuilderListener;
import org.elasticsearch.shield.SecurityContext;
import org.elasticsearch.shield.user.User;
import org.elasticsearch.shield.action.user.ChangePasswordResponse;
import org.elasticsearch.shield.client.SecurityClient;

/**
 */
public class RestChangePasswordAction extends BaseRestHandler {

    private final SecurityContext securityContext;

    @Inject
    public RestChangePasswordAction(Settings settings, Client client, RestController controller, SecurityContext securityContext) {
        super(settings, client);
        this.securityContext = securityContext;
        controller.registerHandler(RestRequest.Method.POST, "/_shield/user/{username}/_password", this);
        controller.registerHandler(RestRequest.Method.PUT, "/_shield/user/{username}/_password", this);
        controller.registerHandler(RestRequest.Method.POST, "/_shield/user/_password", this);
        controller.registerHandler(RestRequest.Method.PUT, "/_shield/user/_password", this);
    }

    @Override
    protected void handleRequest(RestRequest request, RestChannel channel, Client client) throws Exception {
        final User user = securityContext.getUser();
        String username = request.param("username");
        if (username == null) {
            username = user.runAs() == null ? user.principal() : user.runAs().principal();;
        }

        new SecurityClient(client).prepareChangePassword(username, request.content())
                .refresh(request.paramAsBoolean("refresh", true))
                .execute(new RestBuilderListener<ChangePasswordResponse>(channel) {
                    @Override
                    public RestResponse buildResponse(ChangePasswordResponse changePasswordResponse, XContentBuilder builder) throws
                            Exception {
                        return new BytesRestResponse(RestStatus.OK, channel.newBuilder().startObject().endObject());
                    }
                });
    }
}
