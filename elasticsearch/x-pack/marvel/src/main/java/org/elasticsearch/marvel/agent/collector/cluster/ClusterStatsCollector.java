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

import org.elasticsearch.ElasticsearchSecurityException;
import org.elasticsearch.Version;
import org.elasticsearch.action.admin.cluster.stats.ClusterStatsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.plugin.core.LicenseUtils;
import org.elasticsearch.license.plugin.core.LicensesManagerService;
import org.elasticsearch.marvel.MarvelSettings;
import org.elasticsearch.marvel.agent.collector.AbstractCollector;
import org.elasticsearch.marvel.agent.exporter.MonitoringDoc;
import org.elasticsearch.marvel.license.MarvelLicensee;
import org.elasticsearch.shield.InternalClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Collector for cluster stats.
 * <p>
 * This collector runs on the master node. It collect data about the current
 * license and also retrieves the cluster stats.
 * <p>
 * the license and cluster stats are indexed in the data index in a "cluster_info"
 * document; the cluster stats are also indexed in the timestamped index in a
 * "cluster_stats" document.
 */
public class ClusterStatsCollector extends AbstractCollector<ClusterStatsCollector> {

    public static final String NAME = "cluster-stats-collector";

    private final ClusterName clusterName;
    private final LicensesManagerService licensesManagerService;
    private final Client client;

    @Inject
    public ClusterStatsCollector(Settings settings, ClusterService clusterService,
                                 MarvelSettings marvelSettings, MarvelLicensee marvelLicensee, InternalClient client,
                                 LicensesManagerService licensesManagerService, ClusterName clusterName) {
        super(settings, NAME, clusterService, marvelSettings, marvelLicensee);
        this.client = client;
        this.clusterName = clusterName;
        this.licensesManagerService = licensesManagerService;
    }

    @Override
    protected boolean shouldCollect() {
        // This collector can always collect data on the master node
        return isLocalNodeMaster();
    }

    @Override
    protected Collection<MonitoringDoc> doCollect() throws Exception {
        List<MonitoringDoc> results = new ArrayList<>(1);

        // Retrieves cluster stats
        ClusterStatsResponse clusterStats = null;
        try {
            clusterStats = client.admin().cluster().prepareClusterStats().get(marvelSettings.clusterStatsTimeout());
        } catch (ElasticsearchSecurityException e) {
            if (LicenseUtils.isLicenseExpiredException(e)) {
                logger.trace("collector [{}] - unable to collect data because of expired license", e, name());
            } else {
                throw e;
            }
        }

        long timestamp = System.currentTimeMillis();
        String clusterUUID = clusterUUID();
        DiscoveryNode sourceNode = localNode();

        // Adds a cluster info document
        ClusterInfoMonitoringDoc clusterInfoDoc = new ClusterInfoMonitoringDoc(monitoringId(), monitoringVersion());
        clusterInfoDoc.setClusterUUID(clusterUUID);
        clusterInfoDoc.setTimestamp(timestamp);
        clusterInfoDoc.setSourceNode(sourceNode);
        clusterInfoDoc.setClusterName(clusterName.value());
        clusterInfoDoc.setVersion(Version.CURRENT.toString());
        clusterInfoDoc.setLicense(licensesManagerService.getLicense());
        clusterInfoDoc.setClusterStats(clusterStats);
        results.add(clusterInfoDoc);

        // Adds a cluster stats document
        if (super.shouldCollect()) {
            ClusterStatsMonitoringDoc clusterStatsDoc = new ClusterStatsMonitoringDoc(monitoringId(), monitoringVersion());
            clusterStatsDoc.setClusterUUID(clusterUUID);
            clusterStatsDoc.setTimestamp(timestamp);
            clusterStatsDoc.setSourceNode(sourceNode);
            clusterStatsDoc.setClusterStats(clusterStats);
            results.add(clusterStatsDoc);
        }

        return Collections.unmodifiableCollection(results);
    }
}
