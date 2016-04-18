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

package org.elasticsearch.marvel.agent.resolver.cluster;

import org.elasticsearch.action.admin.cluster.stats.ClusterStatsResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.marvel.MonitoredSystem;
import org.elasticsearch.marvel.agent.collector.cluster.ClusterStatsMonitoringDoc;
import org.elasticsearch.marvel.agent.resolver.MonitoringIndexNameResolver;

import java.io.IOException;

public class ClusterStatsResolver extends MonitoringIndexNameResolver.Timestamped<ClusterStatsMonitoringDoc> {

    public static final String TYPE = "cluster_stats";

    static final String[] FILTERS = {
            "cluster_uuid",
            "timestamp",
            "source_node",
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
            "cluster_stats.nodes.jvm.mem.heap_max_in_bytes",
            "cluster_stats.nodes.jvm.mem.heap_used_in_bytes",
            "cluster_stats.nodes.versions",
    };

    public ClusterStatsResolver(MonitoredSystem id, int version, Settings settings) {
        super(id, version, settings);
    }

    @Override
    public String type(ClusterStatsMonitoringDoc document) {
        return TYPE;
    }

    @Override
    public String[] filters() {
        return FILTERS;
    }

    @Override
    protected void buildXContent(ClusterStatsMonitoringDoc document, XContentBuilder builder, ToXContent.Params params) throws IOException {
        builder.startObject(Fields.CLUSTER_STATS);
        ClusterStatsResponse clusterStats = document.getClusterStats();
        if (clusterStats != null) {
            clusterStats.toXContent(builder, params);
        }
        builder.endObject();
    }

    static final class Fields {
        static final String CLUSTER_STATS = TYPE;
    }
}
