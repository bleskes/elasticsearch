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

    private String clusterName;
    private String version;
    private License license;
    private ClusterStatsResponse clusterStats;

    public ClusterInfoMarvelDoc(String index, String type, String id) {
        super(index, type, id);
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public License getLicense() {
        return license;
    }

    public void setLicense(License license) {
        this.license = license;
    }

    public ClusterStatsResponse getClusterStats() {
        return clusterStats;
    }

    public void setClusterStats(ClusterStatsResponse clusterStats) {
        this.clusterStats = clusterStats;
    }
}
