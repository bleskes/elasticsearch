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
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.rest.action.RestBuilderListener;
import org.elasticsearch.xpack.security.SecurityContext;
import org.elasticsearch.xpack.security.action.user.ChangePasswordResponse;
import org.elasticsearch.xpack.security.client.SecurityClient;
import org.elasticsearch.xpack.security.rest.RestRequestFilter;
import org.elasticsearch.xpack.security.user.User;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestRequest.Method.PUT;

public class RestChangePasswordAction extends BaseRestHandler implements RestRequestFilter {

    private final SecurityContext securityContext;

    public RestChangePasswordAction(Settings settings, RestController controller, SecurityContext securityContext) {
        super(settings);
        this.securityContext = securityContext;
        controller.registerHandler(POST, "/_xpack/security/user/{username}/_password", this);
        controller.registerHandler(PUT, "/_xpack/security/user/{username}/_password", this);
        controller.registerHandler(POST, "/_xpack/security/user/_password", this);
        controller.registerHandler(PUT, "/_xpack/security/user/_password", this);
    }

    @Override
    public RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        final User user = securityContext.getUser();
        final String username;
        if (request.param("username") == null) {
            username = user.runAs() == null ? user.principal() : user.runAs().principal();
        } else {
            username = request.param("username");
        }

        final String refresh = request.param("refresh");
        return channel ->
                new SecurityClient(client)
                        .prepareChangePassword(username, request.content(), request.getXContentType())
                        .setRefreshPolicy(refresh)
                        .execute(new RestBuilderListener<ChangePasswordResponse>(channel) {
                            @Override
                            public RestResponse buildResponse(
                                    ChangePasswordResponse changePasswordResponse,
                                    XContentBuilder builder)
                                    throws Exception {
                                return new BytesRestResponse(RestStatus.OK, builder.startObject().endObject());
                            }
                        });
    }

    private static final Set<String> FILTERED_FIELDS = Collections.singleton("password");

    @Override
    public Set<String> getFilteredFields() {
        return FILTERED_FIELDS;
    }
}
