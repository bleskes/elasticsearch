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

package org.elasticsearch.xpack.security.rest.action.user;

import org.elasticsearch.client.node.NodeClient;
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
import org.elasticsearch.rest.action.RestBuilderListener;
import org.elasticsearch.xpack.security.action.user.PutUserRequestBuilder;
import org.elasticsearch.xpack.security.action.user.PutUserResponse;
import org.elasticsearch.xpack.security.client.SecurityClient;

import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestRequest.Method.PUT;

/**
 * Rest endpoint to add a User to the security index
 */
public class RestPutUserAction extends BaseRestHandler {

    @Inject
    public RestPutUserAction(Settings settings, RestController controller) {
        super(settings);
        controller.registerHandler(POST, "/_xpack/security/user/{username}", this);
        controller.registerHandler(PUT, "/_xpack/security/user/{username}", this);

        // @deprecated: Remove in 6.0
        controller.registerAsDeprecatedHandler(POST, "/_shield/user/{username}", this,
                                               "[POST /_shield/user/{username}] is deprecated! Use " +
                                               "[POST /_xpack/security/user/{username}] instead.",
                                               deprecationLogger);
        controller.registerAsDeprecatedHandler(PUT, "/_shield/user/{username}", this,
                                               "[PUT /_shield/user/{username}] is deprecated! Use " +
                                               "[PUT /_xpack/security/user/{username}] instead.",
                                               deprecationLogger);
    }

    @Override
    public void handleRequest(RestRequest request, final RestChannel channel, NodeClient client) throws Exception {
        PutUserRequestBuilder requestBuilder = new SecurityClient(client).preparePutUser(request.param("username"), request.content());
        requestBuilder.setRefreshPolicy(request.param("refresh"));
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
