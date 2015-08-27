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

package org.elasticsearch.marvel.agent.collector.node;

import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.marvel.agent.exporter.MarvelDoc;

public class NodeStatsMarvelDoc extends MarvelDoc {

    private final String nodeId;
    private final boolean nodeMaster;
    private final NodeStats nodeStats;

    private final boolean mlockall;
    private final Double diskThresholdWaterMarkHigh;
    private final boolean diskThresholdDeciderEnabled;

    public NodeStatsMarvelDoc(String clusterUUID, String type, long timestamp,
                              String nodeId, boolean nodeMaster, NodeStats nodeStats, boolean mlockall, Double diskThresholdWaterMarkHigh, boolean diskThresholdDeciderEnabled) {
        super(clusterUUID, type, timestamp);
        this.nodeId = nodeId;
        this.nodeMaster = nodeMaster;
        this.nodeStats = nodeStats;
        this.mlockall = mlockall;
        this.diskThresholdWaterMarkHigh = diskThresholdWaterMarkHigh;
        this.diskThresholdDeciderEnabled = diskThresholdDeciderEnabled;
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

    public Double getDiskThresholdWaterMarkHigh() {
        return diskThresholdWaterMarkHigh;
    }

    public boolean isDiskThresholdDeciderEnabled() {
        return diskThresholdDeciderEnabled;
    }
}

