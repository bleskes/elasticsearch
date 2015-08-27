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
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentBuilderString;
import org.elasticsearch.marvel.agent.collector.cluster.ClusterStatsMarvelDoc;
import org.elasticsearch.marvel.agent.renderer.AbstractRenderer;

import java.io.IOException;

public class ClusterStatsRenderer extends AbstractRenderer<ClusterStatsMarvelDoc> {

    public static final String[] FILTERS = {
            "cluster_stats.nodes.count.total",
            "cluster_stats.indices.count",
            "cluster_stats.indices.shards.total",
            "cluster_stats.indices.shards.index.replication.min",
            "cluster_stats.indices.docs.count",
            "cluster_stats.indices.store.size_in_bytes",
            "cluster_stats.nodes.fs.total_in_bytes",
            "cluster_stats.nodes.fs.free_in_bytes",
            "cluster_stats.nodes.fs.available_in_bytes",
            "cluster_stats.nodes.jvm.max_uptime_in_millis",
            "cluster_stats.nodes.versions",
    };

    public ClusterStatsRenderer() {
        super(FILTERS, true);
    }

    @Override
    protected void doRender(ClusterStatsMarvelDoc marvelDoc, XContentBuilder builder, ToXContent.Params params) throws IOException {
        builder.startObject(Fields.CLUSTER_STATS);

        ClusterStatsResponse clusterStats = marvelDoc.getClusterStats();
        if (clusterStats != null) {
            clusterStats.toXContent(builder, params);
        }

        builder.endObject();
    }

    static final class Fields {
        static final XContentBuilderString CLUSTER_STATS = new XContentBuilderString("cluster_stats");
    }
}
