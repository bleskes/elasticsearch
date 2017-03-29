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

package org.elasticsearch.xpack.monitoring.collector.indices;

import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.xpack.monitoring.exporter.MonitoringDoc;

/**
 * Monitoring document collected by {@link IndicesStatsCollector}
 */
public class IndicesStatsMonitoringDoc extends MonitoringDoc {

    public static final String TYPE = "indices_stats";

    private final IndicesStatsResponse indicesStats;

    public IndicesStatsMonitoringDoc(String monitoringId, String monitoringVersion,
                                     String clusterUUID, long timestamp, DiscoveryNode node,
                                     IndicesStatsResponse indicesStats) {
        super(monitoringId, monitoringVersion, TYPE, null, clusterUUID, timestamp, node);
        this.indicesStats = indicesStats;
    }

    public IndicesStatsResponse getIndicesStats() {
        return indicesStats;
    }
}
