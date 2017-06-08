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
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.license.License;
import org.elasticsearch.xpack.XPackFeatureSet;
import org.elasticsearch.xpack.monitoring.exporter.MonitoringDoc;

import java.util.List;

/**
 * Monitoring document collected by {@link ClusterStatsCollector}.
 * <p>
 * It contains all information about the current cluster, mostly for enabling/disabling features on Kibana side according to the license
 * and also for the "phone home" feature.
 * <p>
 * In the future, the usage stats (and possibly the license) may be collected <em>less</em> frequently and therefore
 * split into a separate monitoring document, but keeping them here simplifies the code.
 */
public class ClusterStatsMonitoringDoc extends MonitoringDoc {

    public static final String TYPE = "cluster_stats";

    private final String clusterName;
    private final String version;
    private final License license;
    private final List<XPackFeatureSet.Usage> usage;
    private final ClusterStatsResponse clusterStats;
    private final ClusterState clusterState;
    private final ClusterHealthStatus status;

    public ClusterStatsMonitoringDoc(String monitoringId, String monitoringVersion,
                                     String clusterUUID, long timestamp, DiscoveryNode node,
                                     String clusterName, String version, License license,
                                     List<XPackFeatureSet.Usage> usage,
                                     ClusterStatsResponse clusterStats,
                                     ClusterState clusterState, ClusterHealthStatus status) {
        super(monitoringId, monitoringVersion, TYPE, null, clusterUUID, timestamp, node);
        this.clusterName = clusterName;
        this.version = version;
        this.license = license;
        this.usage = usage;
        this.clusterStats = clusterStats;
        this.clusterState = clusterState;
        this.status = status;
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

    public List<XPackFeatureSet.Usage> getUsage() {
        return usage;
    }

    public ClusterStatsResponse getClusterStats() {
        return clusterStats;
    }

    public ClusterState getClusterState() {
        return clusterState;
    }

    public ClusterHealthStatus getStatus() {
        return status;
    }

}
