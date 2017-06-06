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

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.xpack.monitoring.MonitoringSettings;
import org.elasticsearch.xpack.monitoring.collector.Collector;
import org.elasticsearch.xpack.monitoring.exporter.MonitoringDoc;
import org.elasticsearch.xpack.security.InternalClient;

import java.util.Collection;
import java.util.Collections;

/**
 * Collector for cluster state.
 * <p>
 * This collector runs on the master node only and collects {@link ClusterStateMonitoringDoc}
 * document at a given frequency.
 */
public class ClusterStateCollector extends Collector {

    private final Client client;

    public ClusterStateCollector(Settings settings, ClusterService clusterService,
                                 MonitoringSettings monitoringSettings,
                                 XPackLicenseState licenseState, InternalClient client) {
        super(settings, "cluster-state", clusterService, monitoringSettings, licenseState);
        this.client = client;
    }

    @Override
    protected boolean shouldCollect() {
        return super.shouldCollect() && isLocalNodeMaster();
    }

    @Override
    protected Collection<MonitoringDoc> doCollect() throws Exception {
        ClusterState clusterState = clusterService.state();
        String clusterUUID = clusterState.metaData().clusterUUID();
        long timestamp = System.currentTimeMillis();

        ClusterHealthResponse clusterHealth = client.admin().cluster().prepareHealth().get(monitoringSettings.clusterStateTimeout());

        // Adds a cluster_state document with associated status
        return Collections.singleton(
                new ClusterStateMonitoringDoc(monitoringId(), monitoringVersion(), clusterUUID,
                                              timestamp, localNode(), clusterState, clusterHealth.getStatus()));
    }
}
