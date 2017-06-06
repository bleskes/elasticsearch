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

import java.util.Collection;

import org.elasticsearch.Version;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.LicenseService;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.xpack.monitoring.MonitoredSystem;
import org.elasticsearch.xpack.monitoring.MonitoringSettings;
import org.elasticsearch.xpack.monitoring.collector.AbstractCollectorTestCase;
import org.elasticsearch.xpack.monitoring.exporter.MonitoringDoc;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

public class ClusterStatsCollectorTests extends AbstractCollectorTestCase {

    public void testClusterStatsCollector() throws Exception {
        Collection<MonitoringDoc> results = newClusterStatsCollector().doCollect();
        assertThat(results, hasSize(1));

        // validate the document
        ClusterStatsMonitoringDoc clusterInfoMonitoringDoc =
                (ClusterStatsMonitoringDoc)results.stream().filter(o -> o instanceof ClusterStatsMonitoringDoc).findFirst().get();

        assertThat(clusterInfoMonitoringDoc.getMonitoringId(), equalTo(MonitoredSystem.ES.getSystem()));
        assertThat(clusterInfoMonitoringDoc.getMonitoringVersion(), equalTo(Version.CURRENT.toString()));
        assertThat(clusterInfoMonitoringDoc.getClusterUUID(),
                equalTo(client().admin().cluster().prepareState().setMetaData(true).get().getState().metaData().clusterUUID()));
        assertThat(clusterInfoMonitoringDoc.getTimestamp(), greaterThan(0L));
        assertThat(clusterInfoMonitoringDoc.getSourceNode(), notNullValue());

        assertThat(clusterInfoMonitoringDoc.getClusterName(),
                equalTo(client().admin().cluster().prepareState().setMetaData(true).get().getClusterName().value()));
        assertThat(clusterInfoMonitoringDoc.getVersion(),
                equalTo(client().admin().cluster().prepareNodesInfo().get().getNodes().get(0).getVersion().toString()));

        assertThat(clusterInfoMonitoringDoc.getLicense(), notNullValue());

        assertNotNull(clusterInfoMonitoringDoc.getClusterStats());
        assertThat(clusterInfoMonitoringDoc.getClusterStats().getNodesStats().getCounts().getTotal(),
                equalTo(internalCluster().getNodeNames().length));
    }

    private ClusterStatsCollector newClusterStatsCollector() {
        // This collector runs on master node only
        return newClusterStatsCollector(internalCluster().getMasterName());
    }

    private ClusterStatsCollector newClusterStatsCollector(String nodeId) {
        assertNotNull(nodeId);
        return new ClusterStatsCollector(internalCluster().getInstance(Settings.class, nodeId),
                internalCluster().getInstance(ClusterService.class, nodeId),
                internalCluster().getInstance(MonitoringSettings.class, nodeId),
                internalCluster().getInstance(XPackLicenseState.class, nodeId),
                securedClient(nodeId),
                internalCluster().getInstance(LicenseService.class, nodeId));
    }
}
