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

import org.elasticsearch.action.admin.cluster.stats.ClusterStatsResponse;
import org.elasticsearch.marvel.agent.exporter.MarvelDoc;

public class ClusterStatsMarvelDoc extends MarvelDoc<ClusterStatsMarvelDoc.Payload> {

    private final Payload payload;

    public ClusterStatsMarvelDoc(String clusterUUID, String type, long timestamp, Payload payload) {
        super(clusterUUID, type, timestamp);
        this.payload = payload;
    }

    @Override
    public ClusterStatsMarvelDoc.Payload payload() {
        return payload;
    }

    public static ClusterStatsMarvelDoc createMarvelDoc(String clusterUUID, String type, long timestamp, ClusterStatsResponse clusterStats) {
        return new ClusterStatsMarvelDoc(clusterUUID, type, timestamp, new Payload(clusterStats));
    }

    public static class Payload {

        private final ClusterStatsResponse clusterStats;

        Payload(ClusterStatsResponse clusterStats) {
            this.clusterStats = clusterStats;
        }

        public ClusterStatsResponse getClusterStats() {
            return clusterStats;
        }
    }
}
