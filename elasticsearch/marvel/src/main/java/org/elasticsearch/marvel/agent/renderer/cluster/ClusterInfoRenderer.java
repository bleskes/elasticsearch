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

package org.elasticsearch.marvel.agent.renderer.cluster;

import org.elasticsearch.action.admin.cluster.stats.ClusterStatsResponse;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.common.hash.MessageDigests;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import org.elasticsearch.license.core.License;
import org.elasticsearch.marvel.agent.collector.cluster.ClusterInfoMarvelDoc;
import org.elasticsearch.marvel.agent.renderer.AbstractRenderer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ClusterInfoRenderer extends AbstractRenderer<ClusterInfoMarvelDoc> {
    public ClusterInfoRenderer() {
        super(null, false);
    }

    @Override
    protected void doRender(ClusterInfoMarvelDoc marvelDoc, XContentBuilder builder, ToXContent.Params params) throws IOException {
        builder.field(Fields.CLUSTER_NAME, marvelDoc.getClusterName());
        builder.field(Fields.VERSION, marvelDoc.getVersion());

        License license = marvelDoc.getLicense();
        if (license != null) {
            builder.startObject(Fields.LICENSE);
            Map<String, String> extraParams = new MapBuilder<String, String>()
                    .put(License.REST_VIEW_MODE, "true")
                    .map();
            params = new ToXContent.DelegatingMapParams(extraParams, params);
            license.toInnerXContent(builder, params);
            builder.field(Fields.HKEY, hash(license, marvelDoc.clusterUUID()));
            builder.endObject();
        }

        builder.startObject(Fields.CLUSTER_STATS);
        ClusterStatsResponse clusterStats = marvelDoc.getClusterStats();
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
        static final XContentBuilderString CLUSTER_NAME = new XContentBuilderString("cluster_name");
        static final XContentBuilderString LICENSE = new XContentBuilderString("license");
        static final XContentBuilderString VERSION = new XContentBuilderString("version");
        static final XContentBuilderString CLUSTER_STATS = new XContentBuilderString("cluster_stats");

        static final XContentBuilderString HKEY = new XContentBuilderString("hkey");

        static final XContentBuilderString UID = new XContentBuilderString("uid");
        static final XContentBuilderString TYPE = new XContentBuilderString("type");
    }
}
