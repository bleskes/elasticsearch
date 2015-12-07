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

package org.elasticsearch.marvel.agent.collector.cluster;

import org.elasticsearch.marvel.agent.exporter.MarvelDoc;

public class ClusterStateNodeMarvelDoc extends MarvelDoc {

    private final String stateUUID;
    private final String nodeId;

    public ClusterStateNodeMarvelDoc(String clusterUUID, String type, long timestamp, String stateUUID, String nodeId) {
        super(clusterUUID, type, timestamp);
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

