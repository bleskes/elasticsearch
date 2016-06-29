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

package org.elasticsearch.xpack.monitoring.agent.collector.cluster;

import org.elasticsearch.xpack.monitoring.agent.exporter.MonitoringDoc;

public class ClusterStateNodeMonitoringDoc extends MonitoringDoc {

    private String stateUUID;
    private String nodeId;

    public ClusterStateNodeMonitoringDoc(String monitoringId, String monitoringVersion) {
        super(monitoringId, monitoringVersion);
    }

    public String getStateUUID() {
        return stateUUID;
    }

    public void setStateUUID(String stateUUID) {
        this.stateUUID = stateUUID;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
}

