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

import org.elasticsearch.action.admin.indices.recovery.RecoveryResponse;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.xpack.monitoring.exporter.MonitoringDoc;

/**
 * Monitoring document collected by {@link IndexRecoveryCollector}
 */
public class IndexRecoveryMonitoringDoc extends MonitoringDoc {

    public static final String TYPE = "index_recovery";

    private final RecoveryResponse recoveryResponse;

    public IndexRecoveryMonitoringDoc(String monitoringId, String monitoringVersion,
                                      String clusterUUID, long timestamp, DiscoveryNode node,
                                      RecoveryResponse recoveryResponse) {
        super(monitoringId, monitoringVersion, TYPE, null, clusterUUID, timestamp, node);
        this.recoveryResponse = recoveryResponse;
    }

    public RecoveryResponse getRecoveryResponse() {
        return recoveryResponse;
    }
}
