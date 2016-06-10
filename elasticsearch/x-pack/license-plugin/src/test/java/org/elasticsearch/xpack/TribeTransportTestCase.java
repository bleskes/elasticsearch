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

package org.elasticsearch.xpack;

import org.elasticsearch.action.Action;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.common.Priority;
import org.elasticsearch.common.UUIDs;
import org.elasticsearch.common.network.NetworkModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.discovery.zen.ping.unicast.UnicastZenPing;
import org.elasticsearch.marvel.Monitoring;
import org.elasticsearch.node.Node;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.shield.Security;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.test.ESIntegTestCase.ClusterScope;
import org.elasticsearch.test.ESIntegTestCase.Scope;
import org.elasticsearch.test.InternalTestCluster;
import org.elasticsearch.test.NodeConfigurationSource;
import org.elasticsearch.test.TestCluster;
import org.elasticsearch.xpack.graph.Graph;
import org.elasticsearch.xpack.watcher.Watcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

@ClusterScope(scope = Scope.TEST, transportClientRatio = 0, numClientNodes = 1, numDataNodes = 0)
public abstract class TribeTransportTestCase extends ESIntegTestCase {

    private static final Collection<String> ALL_FEATURES = Arrays.asList(Security.NAME, Monitoring.NAME,
            Watcher.NAME, Graph.NAME);

    protected List<String> enabledFeatures() {
        return Collections.emptyList();
    }

    @Override
    protected final Settings nodeSettings(int nodeOrdinal) {
        final Settings.Builder builder = Settings.builder()
                .put(NetworkModule.HTTP_ENABLED.getKey(), false)
                .put(Node.NODE_LOCAL_SETTING.getKey(), true);
        List<String> enabledFeatures = enabledFeatures();
        for (String feature : ALL_FEATURES) {
            builder.put(XPackPlugin.featureEnabledSetting(feature), enabledFeatures.contains(feature));
        }
        return builder.build();
    }

    @Override
    protected final Collection<Class<? extends Plugin>> nodePlugins() {
        return Collections.<Class<? extends Plugin>>singletonList(XPackPlugin.class);
    }

    @Override
    protected final Collection<Class<? extends Plugin>> transportClientPlugins() {
        return nodePlugins();
    }

    public void testTribeSetup() throws Exception {
        NodeConfigurationSource nodeConfigurationSource = new NodeConfigurationSource() {
            @Override
            public Settings nodeSettings(int nodeOrdinal) {
                return TribeTransportTestCase.this.nodeSettings(nodeOrdinal);
            }

            @Override
            public Collection<Class<? extends Plugin>> nodePlugins() {
                return TribeTransportTestCase.this.nodePlugins();
            }

            @Override
            public Settings transportClientSettings() {
                return TribeTransportTestCase.this.transportClientSettings();
            }

            @Override
            public Collection<Class<? extends Plugin>> transportClientPlugins() {
                return TribeTransportTestCase.this.transportClientPlugins();
            }
        };
        final InternalTestCluster cluster2 = new InternalTestCluster(InternalTestCluster.configuredNodeMode(),
                randomLong(), createTempDir(), true, 2, 2,
                UUIDs.randomBase64UUID(random()), nodeConfigurationSource, 1, false, "tribe_node2",
                getMockPlugins(), getClientWrapper());

        cluster2.beforeTest(random(), 0.0);
        cluster2.ensureAtLeastNumDataNodes(2);

        logger.info("create 2 indices, test1 on t1, and test2 on t2");
        assertAcked(internalCluster().client().admin().indices().prepareCreate("test1").get());
        assertAcked(cluster2.client().admin().indices().prepareCreate("test2").get());
        ensureYellow(internalCluster());
        ensureYellow(cluster2);
        Map<String,String> asMap = internalCluster().getDefaultSettings().getAsMap();
        Settings.Builder tribe1Defaults = Settings.builder();
        Settings.Builder tribe2Defaults = Settings.builder();
        for (Map.Entry<String, String> entry : asMap.entrySet()) {
            if (entry.getKey().startsWith("path.")) {
                continue;
            }
            tribe1Defaults.put("tribe.t1." + entry.getKey(), entry.getValue());
            tribe2Defaults.put("tribe.t2." + entry.getKey(), entry.getValue());
        }
        // give each tribe it's unicast hosts to connect to
        tribe1Defaults.putArray("tribe.t1." + UnicastZenPing.DISCOVERY_ZEN_PING_UNICAST_HOSTS_SETTING.getKey(),
                getUnicastHosts(internalCluster().client()));
        tribe1Defaults.putArray("tribe.t2." + UnicastZenPing.DISCOVERY_ZEN_PING_UNICAST_HOSTS_SETTING.getKey(),
                getUnicastHosts(cluster2.client()));

        Settings merged = Settings.builder()
                .put("tribe.t1.cluster.name", internalCluster().getClusterName())
                .put("tribe.t2.cluster.name", cluster2.getClusterName())
                .put("tribe.blocks.write", false)
                .put(tribe1Defaults.build())
                .put(tribe2Defaults.build())
                .put(internalCluster().getDefaultSettings())
                .put("node.name", "tribe_node") // make sure we can identify threads from this node
                .put(Node.NODE_LOCAL_SETTING.getKey(), true)
                .build();

        final Node tribeNode = new Node(merged).start();
        Client tribeClient = tribeNode.client();

        logger.info("wait till tribe has the same nodes as the 2 clusters");
        assertBusy(() -> {
            DiscoveryNodes tribeNodes = tribeNode.client().admin().cluster().prepareState().get().getState().getNodes();
            assertThat(countDataNodesForTribe("t1", tribeNodes),
                    equalTo(internalCluster().client().admin().cluster().prepareState().get().getState()
                            .getNodes().getDataNodes().size()));
            assertThat(countDataNodesForTribe("t2", tribeNodes),
                    equalTo(cluster2.client().admin().cluster().prepareState().get().getState()
                            .getNodes().getDataNodes().size()));
        });
        logger.info(" --> verify transport actions for tribe node");
        verifyActionOnTribeNode(tribeClient);
        logger.info(" --> verify transport actions for data node");
        verifyActionOnDataNode((randomBoolean() ? internalCluster() : cluster2).dataNodeClient());
        logger.info(" --> verify transport actions for master node");
        verifyActionOnMasterNode((randomBoolean() ? internalCluster() : cluster2).masterClient());
        logger.info(" --> verify transport actions for client node");
        verifyActionOnClientNode((randomBoolean() ? internalCluster() : cluster2).coordOnlyNodeClient());
        try {
            cluster2.wipe(Collections.<String>emptySet());
        } finally {
            cluster2.afterTest();
        }
        tribeNode.close();
        cluster2.close();
    }

    /**
     * Verify transport action behaviour on client node
     */
    protected abstract void verifyActionOnClientNode(Client client) throws Exception;

    /**
     * Verify transport action behaviour on master node
     */
    protected abstract void verifyActionOnMasterNode(Client masterClient) throws Exception;

    /**
     * Verify transport action behaviour on data node
     */
    protected abstract void verifyActionOnDataNode(Client dataNodeClient) throws Exception;

    /**
     * Verify transport action behaviour on tribe node
     */
    protected abstract void verifyActionOnTribeNode(Client tribeClient);

    protected void failAction(Client client, Action action) {
        try {
            client.execute(action, action.newRequestBuilder(client).request());
            fail("expected [" + action.name() + "] to fail");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("failed to find action"));
        }
    }

    private void ensureYellow(TestCluster testCluster) {
        ClusterHealthResponse actionGet = testCluster.client().admin().cluster()
                        .health(Requests.clusterHealthRequest().waitForYellowStatus()
                                .waitForEvents(Priority.LANGUID).waitForRelocatingShards(0)).actionGet();
        if (actionGet.isTimedOut()) {
            logger.info("ensureGreen timed out, cluster state:\n{}\n{}", testCluster.client().admin().cluster()
                    .prepareState().get().getState().prettyPrint(),
                    testCluster.client().admin().cluster().preparePendingClusterTasks().get().prettyPrint());
            assertThat("timed out waiting for yellow state", actionGet.isTimedOut(), equalTo(false));
        }
        assertThat(actionGet.getStatus(), anyOf(equalTo(ClusterHealthStatus.YELLOW), equalTo(ClusterHealthStatus.GREEN)));
    }

    private int countDataNodesForTribe(String tribeName, DiscoveryNodes nodes) {
        int count = 0;
        for (DiscoveryNode node : nodes) {
            if (!node.isDataNode()) {
                continue;
            }
            if (tribeName.equals(node.getAttributes().get("tribe.name"))) {
                count++;
            }
        }
        return count;
    }

    private static String[] getUnicastHosts(Client client) {
        ArrayList<String> unicastHosts = new ArrayList<>();
        NodesInfoResponse nodeInfos = client.admin().cluster().prepareNodesInfo().clear().setTransport(true).get();
        for (NodeInfo info : nodeInfos.getNodes()) {
            TransportAddress address = info.getTransport().getAddress().publishAddress();
            unicastHosts.add(address.getAddress() + ":" + address.getPort());
        }
        return unicastHosts.toArray(new String[unicastHosts.size()]);
    }

}
