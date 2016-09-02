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

import org.elasticsearch.action.admin.cluster.stats.ClusterStatsResponse;
import org.elasticsearch.license.License;
import org.elasticsearch.xpack.XPackFeatureSet;
import org.elasticsearch.xpack.monitoring.exporter.MonitoringDoc;

import java.util.List;

public class ClusterInfoMonitoringDoc extends MonitoringDoc {

    private String clusterName;
    private String version;
    private License license;
    private List<XPackFeatureSet.Usage> usage;
    private ClusterStatsResponse clusterStats;

    public ClusterInfoMonitoringDoc(String monitoringId, String monitoringVersion) {
        super(monitoringId, monitoringVersion);
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

    public List<XPackFeatureSet.Usage> getUsage() {
        return usage;
    }

    public void setUsage(List<XPackFeatureSet.Usage> usage) {
        this.usage = usage;
    }

    public ClusterStatsResponse getClusterStats() {
        return clusterStats;
    }

    public void setClusterStats(ClusterStatsResponse clusterStats) {
        this.clusterStats = clusterStats;
    }
}
