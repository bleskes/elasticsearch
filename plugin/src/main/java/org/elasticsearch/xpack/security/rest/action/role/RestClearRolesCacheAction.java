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

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestActions.NodesResponseRestListener;
import org.elasticsearch.xpack.security.action.role.ClearRolesCacheRequest;
import org.elasticsearch.xpack.security.client.SecurityClient;
import org.elasticsearch.xpack.security.rest.action.SecurityBaseRestHandler;

import java.io.IOException;

import static org.elasticsearch.rest.RestRequest.Method.POST;

public final class RestClearRolesCacheAction extends SecurityBaseRestHandler {

    public RestClearRolesCacheAction(Settings settings, RestController controller, XPackLicenseState licenseState) {
        super(settings, licenseState);
        controller.registerHandler(POST, "/_xpack/security/role/{name}/_clear_cache", this);

        // @deprecated: Remove in 6.0
        controller.registerAsDeprecatedHandler(POST, "/_shield/role/{name}/_clear_cache", this,
                                               "[POST /_shield/role/{name}/_clear_cache] is deprecated! Use " +
                                               "[POST /_xpack/security/role/{name}/_clear_cache] instead.",
                                               deprecationLogger);
    }

    @Override
    public RestChannelConsumer innerPrepareRequest(RestRequest request, NodeClient client) throws IOException {
        String[] roles = request.paramAsStringArrayOrEmptyIfAll("name");

        ClearRolesCacheRequest req = new ClearRolesCacheRequest().names(roles);

        return channel -> new SecurityClient(client).clearRolesCache(req, new NodesResponseRestListener<>(channel));
    }
}
