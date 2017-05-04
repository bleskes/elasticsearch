/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2017] Elasticsearch Incorporated. All Rights Reserved.
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

package org.elasticsearch.xpack.security.rest.action.rolemapping;

import java.io.IOException;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.rest.action.RestBuilderListener;
import org.elasticsearch.xpack.security.action.rolemapping.GetRoleMappingsResponse;
import org.elasticsearch.xpack.security.authc.support.mapper.ExpressionRoleMapping;
import org.elasticsearch.xpack.security.client.SecurityClient;
import org.elasticsearch.xpack.security.rest.action.SecurityBaseRestHandler;

import static org.elasticsearch.rest.RestRequest.Method.GET;

/**
 * Rest endpoint to retrieve a role-mapping from the {@link org.elasticsearch.xpack.security.authc.support.mapper.NativeRoleMappingStore}
 */
public class RestGetRoleMappingsAction extends SecurityBaseRestHandler {
    public RestGetRoleMappingsAction(Settings settings, RestController controller, XPackLicenseState licenseState) {
        super(settings, licenseState);
        controller.registerHandler(GET, "/_xpack/security/role_mapping/", this);
        controller.registerHandler(GET, "/_xpack/security/role_mapping/{name}", this);
    }

    @Override
    public RestChannelConsumer innerPrepareRequest(RestRequest request, NodeClient client) throws IOException {
        final String[] names = request.paramAsStringArrayOrEmptyIfAll("name");
        return channel -> new SecurityClient(client).prepareGetRoleMappings(names)
                .execute(new RestBuilderListener<GetRoleMappingsResponse>(channel) {
                    @Override
                    public RestResponse buildResponse(GetRoleMappingsResponse response, XContentBuilder builder) throws Exception {
                        builder.startObject();
                        for (ExpressionRoleMapping mapping : response.mappings()) {
                            builder.field(mapping.getName(), mapping);
                        }
                        builder.endObject();

                        // if the request specified mapping names, but nothing was found then return a 404 result
                        if (names.length != 0 && response.mappings().length == 0) {
                            return new BytesRestResponse(RestStatus.NOT_FOUND, builder);
                        } else {
                            return new BytesRestResponse(RestStatus.OK, builder);
                        }
                    }
                });
    }
}
