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

package org.elasticsearch.xpack.monitoring.collector.cluster;

import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.xpack.monitoring.exporter.MonitoringDoc;

/**
 * Monitoring document collected by {@link ClusterStateCollector} that contains the
 * current cluster state.
 */
public class ClusterStateMonitoringDoc extends MonitoringDoc {

    public static final String TYPE = "cluster_state";

    private final ClusterState clusterState;
    private final ClusterHealthStatus status;

    public ClusterStateMonitoringDoc(String monitoringId, String monitoringVersion,
                                     String clusterUUID, long timestamp, DiscoveryNode node,
                                     ClusterState clusterState, ClusterHealthStatus status) {
        super(monitoringId, monitoringVersion, TYPE, null, clusterUUID, timestamp, node);
        this.clusterState = clusterState;
        this.status = status;
    }

    public ClusterState getClusterState() {
        return clusterState;
    }

    public ClusterHealthStatus getStatus() {
        return status;
    }
}
