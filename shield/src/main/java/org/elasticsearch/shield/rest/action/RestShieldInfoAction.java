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

import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.internal.Nullable;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.*;
import org.elasticsearch.shield.ShieldBuild;
import org.elasticsearch.shield.ShieldPlugin;
import org.elasticsearch.shield.ShieldVersion;
import org.elasticsearch.shield.license.LicenseService;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestRequest.Method.HEAD;

public class RestShieldInfoAction extends BaseRestHandler {

    private final ClusterName clusterName;
    private final LicenseService licenseService;
    private final boolean shieldEnabled;

    @Inject
    public RestShieldInfoAction(Settings settings, RestController controller, Client client, ClusterName clusterName, @Nullable LicenseService licenseService) {
        super(settings, controller, client);
        this.clusterName = clusterName;
        this.licenseService = licenseService;
        this.shieldEnabled = ShieldPlugin.shieldEnabled(settings);
        controller.registerHandler(GET, "/_shield", this);
        controller.registerHandler(HEAD, "/_shield", this);
    }

    @Override
    protected void handleRequest(RestRequest request, RestChannel channel, Client client) throws Exception {
        if (request.method() == RestRequest.Method.HEAD) {
            channel.sendResponse(new BytesRestResponse(RestStatus.OK));
            return;
        }

        XContentBuilder builder = channel.newBuilder();

        // Default to pretty printing, but allow ?pretty=false to disable
        if (!request.hasParam("pretty")) {
            builder.prettyPrint().lfAtEnd();
        }

        builder.startObject();

        builder.field("status", resolveStatus());
        if (settings.get("name") != null) {
            builder.field("name", settings.get("name"));
        }
        builder.field("cluster_name", clusterName.value());
        builder.startObject("version")
                .field("number", ShieldVersion.CURRENT.number())
                .field("build_hash", ShieldBuild.CURRENT.hash())
                .field("build_timestamp", ShieldBuild.CURRENT.timestamp())
                .field("build_snapshot", ShieldVersion.CURRENT.snapshot)
                .endObject();
        builder.field("tagline", "You know, for security");
        builder.endObject();

        channel.sendResponse(new BytesRestResponse(RestStatus.OK, builder));
    }

    private Status resolveStatus() {
        if (shieldEnabled) {
            if (licenseService.enabled()) {
                return Status.ENABLED;
            }
            return Status.UNLICENSED;
        }
        return Status.DISABLED;
    }

    private static enum Status {
        ENABLED("enabled"), DISABLED("disabled"), UNLICENSED("unlicensed");

        private final String status;

        Status(String status) {
            this.status = status;
        }

        @Override
        public String toString() {
            return status;
        }
    }
}
