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

package org.elasticsearch.shield.rest.action;

import org.elasticsearch.ElasticsearchSecurityException;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.shield.User;
import org.elasticsearch.shield.authc.AuthenticationService;

import static org.elasticsearch.rest.RestRequest.Method.GET;

public class RestAuthenticateAction extends BaseRestHandler {

    private final AuthenticationService authenticationService;
    @Inject
    public RestAuthenticateAction(Settings settings, RestController controller, Client client, AuthenticationService authenticationService) {
        super(settings, client);
        this.authenticationService = authenticationService;
        controller.registerHandler(GET, "/_shield/authenticate", this);
    }

    @Override
    protected void handleRequest(RestRequest request, RestChannel channel, Client client) throws Exception {
        // we should be authenticated at this point, but we call the authc service to retrieve the user from the context
        User user = authenticationService.authenticate(request);
        assert user != null;
        if (user.isSystem()) {
            throw new ElasticsearchSecurityException("the authenticate API cannot be used for the internal system user");
        }
        XContentBuilder builder = channel.newBuilder();
        user.toXContent(builder, ToXContent.EMPTY_PARAMS);
        channel.sendResponse(new BytesRestResponse(RestStatus.OK, builder));
    }
}
