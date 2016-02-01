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

package org.elasticsearch.shield.rest.action.role;

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
import org.elasticsearch.shield.action.role.GetRolesResponse;
import org.elasticsearch.shield.client.ShieldClient;

/**
 * Rest endpoint to retrieve a Role from the shield index
 */
public class RestGetRolesAction extends BaseRestHandler {

    @Inject
    public RestGetRolesAction(Settings settings, RestController controller, Client client) {
        super(settings, client);
        controller.registerHandler(RestRequest.Method.GET, "/_shield/role/", this);
        controller.registerHandler(RestRequest.Method.GET, "/_shield/role/{id}", this);
        controller.registerHandler(RestRequest.Method.GET, "/_shield/roles/", this);
        controller.registerHandler(RestRequest.Method.GET, "/_shield/roles/{id}", this);
    }

    @Override
    protected void handleRequest(RestRequest request, final RestChannel channel, Client client) throws Exception {
        String[] roles = Strings.splitStringByCommaToArray(request.param("id"));

        new ShieldClient(client).prepareGetRoles().roles(roles).execute(new RestBuilderListener<GetRolesResponse>(channel) {
            @Override
            public RestResponse buildResponse(GetRolesResponse getRolesResponse, XContentBuilder builder) throws Exception {
                builder.startObject();
                builder.field("found", getRolesResponse.isExists());
                builder.startArray("roles");
                for (ToXContent role : getRolesResponse.roles()) {
                    role.toXContent(builder, ToXContent.EMPTY_PARAMS);
                }
                builder.endArray();
                builder.endObject();
                return new BytesRestResponse(RestStatus.OK, builder);
            }
        });
    }
}
