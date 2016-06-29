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


package org.elasticsearch.xpack.monitoring.agent.resolver.cluster;

import org.elasticsearch.action.admin.cluster.stats.ClusterStatsResponse;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.common.hash.MessageDigests;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.license.core.License;
import org.elasticsearch.xpack.monitoring.agent.collector.cluster.ClusterInfoMonitoringDoc;
import org.elasticsearch.xpack.monitoring.agent.resolver.MonitoringIndexNameResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ClusterInfoResolver extends MonitoringIndexNameResolver.Data<ClusterInfoMonitoringDoc> {

    public static final String TYPE = "cluster_info";

    @Override
    public String type(ClusterInfoMonitoringDoc document) {
        return TYPE;
    }

    @Override
    public String id(ClusterInfoMonitoringDoc document) {
        return document.getClusterUUID();
    }

    @Override
    protected void buildXContent(ClusterInfoMonitoringDoc document, XContentBuilder builder, ToXContent.Params params) throws IOException {
        builder.field(Fields.CLUSTER_NAME, document.getClusterName());
        builder.field(Fields.VERSION, document.getVersion());

        License license = document.getLicense();
        if (license != null) {
            builder.startObject(Fields.LICENSE);
            Map<String, String> extraParams = new MapBuilder<String, String>()
                    .put(License.REST_VIEW_MODE, "true")
                    .map();
            params = new ToXContent.DelegatingMapParams(extraParams, params);
            license.toInnerXContent(builder, params);
            builder.field(Fields.HKEY, hash(license, document.getClusterUUID()));
            builder.endObject();
        }

        builder.startObject(Fields.CLUSTER_STATS);
        ClusterStatsResponse clusterStats = document.getClusterStats();
        if (clusterStats != null) {
            clusterStats.toXContent(builder, params);
        }
        builder.endObject();
    }

    public static String hash(License license, String clusterName) {
        return hash(license.status().label(), license.uid(), license.type(), String.valueOf(license.expiryDate()), clusterName);
    }

    public static String hash(String licenseStatus, String licenseUid, String licenseType, String licenseExpiryDate, String clusterUUID) {
        String toHash = licenseStatus + licenseUid + licenseType + licenseExpiryDate + clusterUUID;
        return MessageDigests.toHexString(MessageDigests.sha256().digest(toHash.getBytes(StandardCharsets.UTF_8)));
    }

    static final class Fields {
        static final String CLUSTER_NAME = "cluster_name";
        static final String LICENSE = "license";
        static final String VERSION = "version";
        static final String CLUSTER_STATS = "cluster_stats";

        static final String HKEY = "hkey";

        static final String UID = "uid";
        static final String TYPE = "type";
    }
}
