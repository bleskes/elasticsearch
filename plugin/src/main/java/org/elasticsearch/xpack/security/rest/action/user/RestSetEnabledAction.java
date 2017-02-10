/*
 * ELASTICSEARCH CONFIDENTIAL
 *  __________________
 *
 * [2014] Elasticsearch Incorporated. All Rights Reserved.
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
import org.elasticsearch.xpack.security.action.user.SetEnabledResponse;
import org.elasticsearch.xpack.security.client.SecurityClient;

import java.io.IOException;

import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestRequest.Method.PUT;

/**
 * REST handler for enabling and disabling users. The username is required and we use the path to determine if the user is being
 * enabled or disabled.
 */
public class RestSetEnabledAction extends BaseRestHandler {
    public RestSetEnabledAction(Settings settings, RestController controller) {
        super(settings);
        controller.registerHandler(POST, "/_xpack/security/user/{username}/_enable", this);
        controller.registerHandler(PUT, "/_xpack/security/user/{username}/_enable", this);
        controller.registerHandler(POST, "/_xpack/security/user/{username}/_disable", this);
        controller.registerHandler(PUT, "/_xpack/security/user/{username}/_disable", this);
    }

    @Override
    public RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        final boolean enabled = request.path().endsWith("_enable");
        assert enabled || request.path().endsWith("_disable");
        final String username = request.param("username");
        return channel -> new SecurityClient(client).prepareSetEnabled(username, enabled)
                .execute(new RestBuilderListener<SetEnabledResponse>(channel) {
                    @Override
                    public RestResponse buildResponse(SetEnabledResponse setEnabledResponse, XContentBuilder builder) throws Exception {
                        return new BytesRestResponse(RestStatus.OK, builder.startObject().endObject());
                    }
                });
    }
}
