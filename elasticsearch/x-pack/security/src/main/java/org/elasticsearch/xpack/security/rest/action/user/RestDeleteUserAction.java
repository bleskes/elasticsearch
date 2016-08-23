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
import org.elasticsearch.xpack.security.action.user.DeleteUserResponse;
import org.elasticsearch.xpack.security.client.SecurityClient;

import static org.elasticsearch.rest.RestRequest.Method.DELETE;

/**
 * Rest action to delete a user from the security index
 */
public class RestDeleteUserAction extends BaseRestHandler {

    @Inject
    public RestDeleteUserAction(Settings settings, RestController controller) {
        super(settings);
        controller.registerHandler(DELETE, "/_xpack/security/user/{username}", this);

        // @deprecated: Remove in 6.0
        controller.registerAsDeprecatedHandler(DELETE, "/_shield/user/{username}", this,
                                               "[DELETE /_shield/user/{username}] is deprecated! Use " +
                                               "[DELETE /_xpack/security/user/{username}] instead.",
                                               deprecationLogger);
    }

    @Override
    public void handleRequest(RestRequest request, final RestChannel channel, NodeClient client) throws Exception {
        new SecurityClient(client).prepareDeleteUser(request.param("username"))
                .setRefreshPolicy(request.param("refresh"))
                .execute(new RestBuilderListener<DeleteUserResponse>(channel) {
                    @Override
                    public RestResponse buildResponse(DeleteUserResponse response, XContentBuilder builder) throws Exception {
                        return new BytesRestResponse(response.found() ? RestStatus.OK : RestStatus.NOT_FOUND,
                                builder.startObject()
                                        .field("found", response.found())
                                        .endObject());
                    }
                });
    }
}
