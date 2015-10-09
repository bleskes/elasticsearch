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
import org.elasticsearch.license.core.License;
import org.elasticsearch.marvel.agent.exporter.MarvelDoc;

public class ClusterInfoMarvelDoc extends MarvelDoc {

    private final String clusterName;
    private final String version;
    private final License license;
    private final ClusterStatsResponse clusterStats;

    ClusterInfoMarvelDoc(String index, String type, String id, String clusterUUID, long timestamp,
                         String clusterName, String version, License license, ClusterStatsResponse clusterStats) {
        super(index, type, id, clusterUUID, timestamp);
        this.clusterName = clusterName;
        this.version = version;
        this.license = license;
        this.clusterStats = clusterStats;
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getVersion() {
        return version;
    }

    public License getLicense() {
        return license;
    }

    public ClusterStatsResponse getClusterStats() {
        return clusterStats;
    }
}
