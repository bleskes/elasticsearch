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

import org.elasticsearch.Version;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.license.XPackLicenseState;
import org.elasticsearch.xpack.monitoring.MonitoredSystem;
import org.elasticsearch.xpack.monitoring.MonitoringSettings;
import org.elasticsearch.xpack.monitoring.collector.AbstractCollectorTestCase;
import org.elasticsearch.xpack.monitoring.exporter.MonitoringDoc;

import java.util.Collection;

import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertHitCount;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;

public class ClusterStateCollectorTests extends AbstractCollectorTestCase {

    public void testClusterStateCollectorNoIndices() throws Exception {
        // waits for pending tasks before collecing cluster state.
        // prevents the collector to read an older cluster state than the one used in assert later
        ensureGreen();
        assertMonitoringDocs(newClusterStateCollector().doCollect(), 0);
    }

    public void testClusterStateCollectorOneIndex() throws Exception {
        int nbShards = randomIntBetween(1, 5);
        assertAcked(prepareCreate("test").setSettings(Settings.builder()
                .put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, nbShards)
                .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 0)
                .build()));

        int nbDocs = randomIntBetween(1, 20);
        for (int i = 0; i < nbDocs; i++) {
            client().prepareIndex("test", "test").setSource("num", i).get();
        }

        flush();
        refresh();

        assertHitCount(client().prepareSearch().setSize(0).get(), nbDocs);
        assertMonitoringDocs(newClusterStateCollector().doCollect(), nbShards);
    }

    public void testClusterStateCollectorMultipleIndices() throws Exception {
        int nbIndices = randomIntBetween(1, 5);
        int[] docsPerIndex = new int[nbIndices];
        int[] shardsPerIndex = new int[nbIndices];
        int nbShards = 0;

        for (int i = 0; i < nbIndices; i++) {
            shardsPerIndex[i] = randomIntBetween(1, 5);
            nbShards += shardsPerIndex[i];
            assertAcked(prepareCreate("test-" + i).setSettings(Settings.builder()
                    .put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, shardsPerIndex[i])
                    .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 0)
                    .build()));

            docsPerIndex[i] = randomIntBetween(1, 20);
            for (int j = 0; j < docsPerIndex[i]; j++) {
                client().prepareIndex("test-" + i, "test").setSource("num", i).get();
            }
        }

        refresh();

        for (int i = 0; i < nbIndices; i++) {
            assertHitCount(client().prepareSearch("test-" + i).setSize(0).get(), docsPerIndex[i]);
        }

        Collection<MonitoringDoc> results = newClusterStateCollector().doCollect();
        assertMonitoringDocs(results, nbShards);

        MonitoringDoc monitoringDoc = results.iterator().next();
        assertNotNull(monitoringDoc);
        assertThat(monitoringDoc, instanceOf(ClusterStateMonitoringDoc.class));

        ClusterStateMonitoringDoc clusterStateMonitoringDoc = (ClusterStateMonitoringDoc) monitoringDoc;

        assertThat(clusterStateMonitoringDoc.getMonitoringId(), equalTo(MonitoredSystem.ES.getSystem()));
        assertThat(clusterStateMonitoringDoc.getMonitoringVersion(), equalTo(Version.CURRENT.toString()));
        assertThat(clusterStateMonitoringDoc.getClusterUUID(),
                equalTo(client().admin().cluster().prepareState().setMetaData(true).get().getState().metaData().clusterUUID()));
        assertThat(clusterStateMonitoringDoc.getTimestamp(), greaterThan(0L));
        assertThat(clusterStateMonitoringDoc.getSourceNode(), notNullValue());
        assertNotNull(clusterStateMonitoringDoc.getClusterState());

        ClusterState clusterState = clusterStateMonitoringDoc.getClusterState();
        for (int i = 0; i < nbIndices; i++) {
            assertThat(clusterState.getRoutingTable().allShards("test-" + i), hasSize(shardsPerIndex[i]));
        }
    }

    private ClusterStateCollector newClusterStateCollector() {
        // This collector runs on master node only
        return newClusterStateCollector(internalCluster().getMasterName());
    }

    private ClusterStateCollector newClusterStateCollector(String nodeId) {
        assertNotNull(nodeId);
        return new ClusterStateCollector(internalCluster().getInstance(Settings.class, nodeId),
                internalCluster().getInstance(ClusterService.class, nodeId),
                internalCluster().getInstance(MonitoringSettings.class, nodeId),
                internalCluster().getInstance(XPackLicenseState.class, nodeId),
                securedClient(nodeId));
    }

    private void assertMonitoringDocs(Collection<MonitoringDoc> results, final int nbShards) {
        assertThat("expecting 1 document for cluster state", results, hasSize(1));

        final ClusterState clusterState = securedClient().admin().cluster().prepareState().get().getState();
        final String clusterUUID = clusterState.getMetaData().clusterUUID();

        for (MonitoringDoc doc : results) {
            assertThat(doc.getMonitoringId(), equalTo(MonitoredSystem.ES.getSystem()));
            assertThat(doc.getMonitoringVersion(), equalTo(Version.CURRENT.toString()));
            assertThat(doc.getClusterUUID(), equalTo(clusterUUID));
            assertThat(doc.getTimestamp(), greaterThan(0L));
            assertThat(doc.getSourceNode(), notNullValue());
            assertThat(doc, instanceOf(ClusterStateMonitoringDoc.class));

            if (doc instanceof ClusterStateMonitoringDoc) {
                ClusterStateMonitoringDoc clusterStateMonitoringDoc = (ClusterStateMonitoringDoc) doc;
                assertThat(clusterStateMonitoringDoc.getClusterState().getRoutingTable().allShards(), hasSize(nbShards));
                assertThat(clusterStateMonitoringDoc.getClusterState().getNodes().getSize(), equalTo(internalCluster().size()));

            } else {
                fail("unknown monitoring document type " + doc);
            }
        }
    }
}
