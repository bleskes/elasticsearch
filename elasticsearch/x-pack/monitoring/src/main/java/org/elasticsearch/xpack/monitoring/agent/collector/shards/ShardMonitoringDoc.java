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

package org.elasticsearch.xpack.monitoring.agent.collector.shards;

import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.xpack.monitoring.agent.exporter.MonitoringDoc;

public class ShardMonitoringDoc extends MonitoringDoc {

    private ShardRouting shardRouting;
    private String clusterStateUUID;

    public ShardMonitoringDoc(String monitoringId, String monitoringVersion) {
        super(monitoringId, monitoringVersion);
    }

    public void setShardRouting(ShardRouting shardRouting) {
        this.shardRouting = shardRouting;
    }

    public void setClusterStateUUID(java.lang.String clusterStateUUID) {
        this.clusterStateUUID = clusterStateUUID;
    }

    public ShardRouting getShardRouting() {
        return shardRouting;
    }

    public String getClusterStateUUID() {
        return clusterStateUUID;
    }
}
