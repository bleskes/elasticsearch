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

import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.marvel.agent.collector.AbstractCollectorTestCase;
import org.elasticsearch.marvel.agent.exporter.MarvelDoc;
import org.elasticsearch.marvel.agent.settings.MarvelSettings;
import org.elasticsearch.marvel.license.MarvelLicensee;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertHitCount;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;

public class ClusterStateCollectorTests extends AbstractCollectorTestCase {

    public void testClusterStateCollectorNoIndices() throws Exception {
        assertMarvelDocs(newClusterStateCollector().doCollect(), 0);
    }

    public void testClusterStateCollectorOneIndex() throws Exception {
        int nbShards = randomIntBetween(1, 5);
        assertAcked(prepareCreate("test").setSettings(Settings.settingsBuilder()
                .put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, nbShards)
                .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 0)
                .build()));

        int nbDocs = randomIntBetween(1, 20);
        for (int i = 0; i < nbDocs; i++) {
            client().prepareIndex("test", "test").setSource("num", i).get();
        }

        securedFlush();
        securedRefresh();

        assertHitCount(client().prepareSearch().setSize(0).get(), nbDocs);
        assertMarvelDocs(newClusterStateCollector().doCollect(), nbShards);
    }

    public void testClusterStateCollectorMultipleIndices() throws Exception {
        int nbIndices = randomIntBetween(1, 5);
        int[] docsPerIndex = new int[nbIndices];
        int[] shardsPerIndex = new int[nbIndices];
        int nbShards = 0;

        for (int i = 0; i < nbIndices; i++) {
            shardsPerIndex[i] = randomIntBetween(1, 5);
            nbShards += shardsPerIndex[i];
            assertAcked(prepareCreate("test-" + i).setSettings(Settings.settingsBuilder()
                    .put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, shardsPerIndex[i])
                    .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 0)
                    .build()));

            docsPerIndex[i] = randomIntBetween(1, 20);
            for (int j = 0; j < docsPerIndex[i]; j++) {
                client().prepareIndex("test-" + i, "test").setSource("num", i).get();
            }
        }

        securedFlush();
        securedRefresh();

        for (int i = 0; i < nbIndices; i++) {
            assertHitCount(client().prepareSearch("test-" + i).setSize(0).get(), docsPerIndex[i]);
        }

        Collection<MarvelDoc> results = newClusterStateCollector().doCollect();
        assertMarvelDocs(results, nbShards);

        MarvelDoc marvelDoc = results.iterator().next();
        assertNotNull(marvelDoc);
        assertThat(marvelDoc, instanceOf(ClusterStateMarvelDoc.class));

        ClusterStateMarvelDoc clusterStateMarvelDoc = (ClusterStateMarvelDoc) marvelDoc;
        assertThat(clusterStateMarvelDoc.clusterUUID(), equalTo(client().admin().cluster().prepareState().setMetaData(true).get().getState().metaData().clusterUUID()));
        assertThat(clusterStateMarvelDoc.timestamp(), greaterThan(0L));
        assertThat(clusterStateMarvelDoc.type(), equalTo(ClusterStateCollector.TYPE));
        assertNotNull(clusterStateMarvelDoc.getClusterState());

        ClusterState clusterState = clusterStateMarvelDoc.getClusterState();
        for (int i = 0; i < nbIndices; i++) {
            assertThat(clusterState.getRoutingTable().allShards("test-" + i), hasSize(shardsPerIndex[i]));
        }
    }

    public void testClusterStateCollectorWithLicensing() {
        try {
            String[] nodes = internalCluster().getNodeNames();
            for (String node : nodes) {
                logger.debug("--> creating a new instance of the collector");
                ClusterStateCollector collector = newClusterStateCollector(node);
                assertNotNull(collector);

                logger.debug("--> enabling license and checks that the collector can collect data if node is master");
                enableLicense();
                if (node.equals(internalCluster().getMasterName())) {
                    assertCanCollect(collector);
                } else {
                    assertCannotCollect(collector);
                }

                logger.debug("--> starting graceful period and checks that the collector can still collect data if node is master");
                beginGracefulPeriod();
                if (node.equals(internalCluster().getMasterName())) {
                    assertCanCollect(collector);
                } else {
                    assertCannotCollect(collector);
                }

                logger.debug("--> ending graceful period and checks that the collector cannot collect data");
                endGracefulPeriod();
                assertCannotCollect(collector);

                logger.debug("--> disabling license and checks that the collector cannot collect data");
                disableLicense();
                assertCannotCollect(collector);
            }
        } finally {
            // Ensure license is enabled before finishing the test
            enableLicense();
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
                internalCluster().getInstance(MarvelSettings.class, nodeId),
                internalCluster().getInstance(MarvelLicensee.class, nodeId),
                securedClient(nodeId));
    }

    private void assertMarvelDocs(Collection<MarvelDoc> results, final int nbShards) {
        assertThat("expecting 1 document for the cluster state and 2 documents per node", results, hasSize(1 + internalCluster().size() * 2));

        final ClusterState clusterState = securedClient().admin().cluster().prepareState().get().getState();
        final String clusterUUID = clusterState.getMetaData().clusterUUID();
        final String stateUUID = clusterState.stateUUID();

        List<ClusterStateNodeMarvelDoc> clusterStateNodes = new ArrayList<>();
        List<DiscoveryNodeMarvelDoc> discoveryNodes = new ArrayList<>();

        for (MarvelDoc marvelDoc : results) {
            assertThat(marvelDoc.clusterUUID(), equalTo(clusterUUID));
            assertThat(marvelDoc.timestamp(), greaterThan(0L));
            assertThat(marvelDoc, anyOf(instanceOf(ClusterStateMarvelDoc.class), instanceOf(ClusterStateNodeMarvelDoc.class), instanceOf(DiscoveryNodeMarvelDoc.class)));

            switch (marvelDoc.type()) {
                case ClusterStateCollector.TYPE:
                    ClusterStateMarvelDoc clusterStateMarvelDoc = (ClusterStateMarvelDoc) marvelDoc;
                    assertThat(clusterStateMarvelDoc.getClusterState().getRoutingTable().allShards(), hasSize(nbShards));
                    break;

                case ClusterStateCollector.NODES_TYPE:
                    ClusterStateNodeMarvelDoc clusterStateNodeMarvelDoc = (ClusterStateNodeMarvelDoc) marvelDoc;
                    assertThat(clusterStateNodeMarvelDoc.getStateUUID(), equalTo(stateUUID));
                    assertThat(clusterStateNodeMarvelDoc.getNodeId(), not(isEmptyOrNullString()));
                    clusterStateNodes.add(clusterStateNodeMarvelDoc);
                    break;

                case ClusterStateCollector.NODE_TYPE:
                    DiscoveryNodeMarvelDoc discoveryNodeMarvelDoc = (DiscoveryNodeMarvelDoc) marvelDoc;
                    assertThat(discoveryNodeMarvelDoc.index(), equalTo(MarvelSettings.MARVEL_DATA_INDEX_NAME));
                    assertThat(discoveryNodeMarvelDoc.id(),  not(isEmptyOrNullString()));
                    assertNotNull(discoveryNodeMarvelDoc.getNode());
                    discoveryNodes.add(discoveryNodeMarvelDoc);
                    break;
                default:
                    fail("unknown marvel document type " + marvelDoc.type());
            }
        }

        assertThat(clusterStateNodes, hasSize(internalCluster().size()));
        assertThat(discoveryNodes, hasSize(internalCluster().size()));

        for (final String nodeName : internalCluster().getNodeNames()) {
            final String nodeId = internalCluster().clusterService(nodeName).localNode().getId();

            boolean found = false;
            for (ClusterStateNodeMarvelDoc doc : clusterStateNodes) {
                if (nodeId.equals(doc.getNodeId())) {
                    found = true;
                    break;
                }
            }
            assertTrue("Could not find node id [" + nodeName + "]", found);

            found = false;
            for (DiscoveryNodeMarvelDoc doc : discoveryNodes) {
                if (nodeName.equals(doc.getNode().getName())) {
                    found = true;
                    break;
                }
            }
            assertTrue("Could not find node name [" + nodeName + "]", found);
        }
    }
}
