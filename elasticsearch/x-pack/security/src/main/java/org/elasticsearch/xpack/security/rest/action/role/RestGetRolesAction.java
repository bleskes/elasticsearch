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

package org.elasticsearch.xpack.security.rest.action.role;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.Strings;
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
import org.elasticsearch.xpack.security.action.role.GetRolesResponse;
import org.elasticsearch.xpack.security.client.SecurityClient;
import org.elasticsearch.xpack.security.authz.RoleDescriptor;

/**
 * Rest endpoint to retrieve a Role from the security index
 */
public class RestGetRolesAction extends BaseRestHandler {

    @Inject
    public RestGetRolesAction(Settings settings, RestController controller, Client client) {
        super(settings, client);
        controller.registerHandler(RestRequest.Method.GET, "/_xpack/security/role/", this);
        controller.registerHandler(RestRequest.Method.GET, "/_xpack/security/role/{name}", this);
    }

    @Override
    protected void handleRequest(RestRequest request, RestChannel channel, Client client) throws Exception {
        final String[] roles = request.paramAsStringArray("name", Strings.EMPTY_ARRAY);
        new SecurityClient(client).prepareGetRoles(roles).execute(new RestBuilderListener<GetRolesResponse>(channel) {
            @Override
            public RestResponse buildResponse(GetRolesResponse response, XContentBuilder builder) throws Exception {
                builder.startObject();
                for (RoleDescriptor role : response.roles()) {
                    builder.field(role.getName(), role);
                }
                builder.endObject();

                // if the user asked for specific roles, but none of them were found
                // we'll return an empty result and 404 status code
                if (roles.length != 0 && response.roles().length == 0) {
                    return new BytesRestResponse(RestStatus.NOT_FOUND, builder);
                }

                // either the user asked for all roles, or at least one of the roles
                // the user asked for was found
                return new BytesRestResponse(RestStatus.OK, builder);
            }
        });
    }
}
