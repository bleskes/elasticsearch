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
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.rest.action.RestBuilderListener;
import org.elasticsearch.xpack.security.action.rolemapping.PutRoleMappingRequestBuilder;
import org.elasticsearch.xpack.security.action.rolemapping.PutRoleMappingResponse;
import org.elasticsearch.xpack.security.client.SecurityClient;
import org.elasticsearch.xpack.security.rest.action.SecurityBaseRestHandler;

import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestRequest.Method.PUT;

/**
 * Rest endpoint to add a role-mapping to the native store
 *
 * @see org.elasticsearch.xpack.security.authc.support.mapper.NativeRoleMappingStore
 */
public class RestPutRoleMappingAction extends SecurityBaseRestHandler {
    public RestPutRoleMappingAction(Settings settings, RestController controller, XPackLicenseState licenseState) {
        super(settings, licenseState);
        controller.registerHandler(POST, "/_xpack/security/role_mapping/{name}", this);
        controller.registerHandler(PUT, "/_xpack/security/role_mapping/{name}", this);
    }

    @Override
    public RestChannelConsumer innerPrepareRequest(RestRequest request, NodeClient client) throws IOException {
        final String name = request.param("name");
        PutRoleMappingRequestBuilder requestBuilder = new SecurityClient(client)
                .preparePutRoleMapping(name, request.content(), request.getXContentType())
                .setRefreshPolicy(request.param("refresh"));
        return channel -> requestBuilder.execute(
                new RestBuilderListener<PutRoleMappingResponse>(channel) {
                    @Override
                    public RestResponse buildResponse(PutRoleMappingResponse response, XContentBuilder builder) throws Exception {
                        return new BytesRestResponse(RestStatus.OK, builder.startObject().field("role_mapping", response).endObject());
                    }
                });
    }
}
