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

import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.xpack.monitoring.exporter.MonitoringDoc;

/**
 * Monitoring document collected by {@link ClusterStateCollector} that contains the id of
 * every node of the cluster.
 */
public class ClusterStateNodeMonitoringDoc extends MonitoringDoc {

    public static final String TYPE = "node";

    private final String stateUUID;
    private final String nodeId;

    public ClusterStateNodeMonitoringDoc(String monitoringId, String monitoringVersion,
                                         String clusterUUID, long timestamp, DiscoveryNode node,
                                         String stateUUID, String nodeId) {
        super(monitoringId, monitoringVersion, TYPE, null, clusterUUID, timestamp, node);
        this.stateUUID = stateUUID;
        this.nodeId = nodeId;
    }

    public String getStateUUID() {
        return stateUUID;
    }

    public String getNodeId() {
        return nodeId;
    }
}