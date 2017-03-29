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

package org.elasticsearch.xpack.monitoring.collector.node;

import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.xpack.monitoring.exporter.MonitoringDoc;

/**
 * Monitoring document collected by {@link NodeStatsCollector}
 */
public class NodeStatsMonitoringDoc extends MonitoringDoc {

    public static final String TYPE = "node_stats";

    private final String nodeId;
    private final boolean nodeMaster;
    private final NodeStats nodeStats;
    private final boolean mlockall;

    public NodeStatsMonitoringDoc(String monitoringId, String monitoringVersion,
                                  String clusterUUID, DiscoveryNode node,
                                  boolean isMaster, NodeStats nodeStats, boolean mlockall) {
        super(monitoringId, monitoringVersion, TYPE, null, clusterUUID,
                nodeStats.getTimestamp(), node);
        this.nodeId = node.getId();
        this.nodeMaster = isMaster;
        this.nodeStats = nodeStats;
        this.mlockall = mlockall;
    }

    public String getNodeId() {
        return nodeId;
    }

    public boolean isNodeMaster() {
        return nodeMaster;
    }

    public NodeStats getNodeStats() {
        return nodeStats;
    }

    public boolean isMlockall() {
        return mlockall;
    }
}