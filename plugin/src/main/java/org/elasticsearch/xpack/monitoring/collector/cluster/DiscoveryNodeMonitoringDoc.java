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
 * Monitoring document collected by {@link ClusterStateCollector} that contains information
 * about every node of the cluster.
 */
public class DiscoveryNodeMonitoringDoc extends MonitoringDoc {

    public static final String TYPE = "node";

    private final DiscoveryNode node;

    public DiscoveryNodeMonitoringDoc(String monitoringId, String monitoringVersion,
                                      String clusterUUID, long timestamp, DiscoveryNode node) {
        super(monitoringId, monitoringVersion, TYPE, node.getId(), clusterUUID, timestamp, node);
        this.node = node;
    }

    public DiscoveryNode getNode() {
        return node;
    }
}