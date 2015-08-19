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

import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.marvel.agent.exporter.MarvelDoc;

public class ClusterStateMarvelDoc extends MarvelDoc<ClusterStateMarvelDoc.Payload> {

    private final Payload payload;

    public ClusterStateMarvelDoc(String clusterUUID, String type, long timestamp, Payload payload) {
        super(clusterUUID, type, timestamp);
        this.payload = payload;
    }

    @Override
    public ClusterStateMarvelDoc.Payload payload() {
        return payload;
    }

    public static ClusterStateMarvelDoc createMarvelDoc(String clusterUUID, String type, long timestamp, ClusterState clusterState, ClusterHealthStatus status) {
        return new ClusterStateMarvelDoc(clusterUUID, type, timestamp, new Payload(clusterState, status));
    }

    public static class Payload {

        private final ClusterState clusterState;
        private final ClusterHealthStatus status;

        Payload(ClusterState clusterState, ClusterHealthStatus status) {
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
}
