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
import org.elasticsearch.shield.action.user.PutUserRequestBuilder;
import org.elasticsearch.shield.action.user.PutUserResponse;
import org.elasticsearch.shield.client.SecurityClient;

/**
 * Rest endpoint to add a User to the shield index
 */
public class RestPutUserAction extends BaseRestHandler {

    @Inject
    public RestPutUserAction(Settings settings, RestController controller, Client client) {
        super(settings, client);
        controller.registerHandler(RestRequest.Method.POST, "/_shield/user/{username}", this);
        controller.registerHandler(RestRequest.Method.PUT, "/_shield/user/{username}", this);
    }

    @Override
    protected void handleRequest(RestRequest request, final RestChannel channel, Client client) throws Exception {
        PutUserRequestBuilder requestBuilder = new SecurityClient(client).preparePutUser(request.param("username"), request.content());
        if (request.hasParam("refresh")) {
            requestBuilder.refresh(request.paramAsBoolean("refresh", true));
        }
        requestBuilder.execute(new RestBuilderListener<PutUserResponse>(channel) {
            @Override
            public RestResponse buildResponse(PutUserResponse putUserResponse, XContentBuilder builder) throws Exception {
                return new BytesRestResponse(RestStatus.OK,
                        builder.startObject()
                                .field("user", putUserResponse)
                                .endObject());
            }
        });
    }
}
