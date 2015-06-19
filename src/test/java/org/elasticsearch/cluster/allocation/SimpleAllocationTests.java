/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.cluster.allocation;

import com.carrotsearch.randomizedtesting.annotations.Repeat;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.routing.RoutingNode;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.elasticsearch.test.InternalTestCluster;
import org.elasticsearch.test.junit.annotations.TestLogging;
import org.junit.Test;

import java.io.IOException;

import static org.elasticsearch.cluster.metadata.IndexMetaData.SETTING_NUMBER_OF_REPLICAS;
import static org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;

@ElasticsearchIntegrationTest.ClusterScope(scope = ElasticsearchIntegrationTest.Scope.TEST)
public class SimpleAllocationTests extends ElasticsearchIntegrationTest {

    @Override
    protected int numberOfShards() {
        return 3;
    }

    @Override
    protected int numberOfReplicas() {
        return 1;
    }

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return ImmutableSettings.builder().put(super.nodeSettings(nodeOrdinal)).put("gateway.type", "local").build();
    }

    /**
     * Test for
     * https://groups.google.com/d/msg/elasticsearch/y-SY_HyoB-8/EZdfNt9VO44J
     */
    @Test
    public void testSaneAllocation() {
        assertAcked(prepareCreate("test", 3));
        ensureGreen();

        ClusterState state = client().admin().cluster().prepareState().execute().actionGet().getState();
        assertThat(state.routingNodes().unassigned().size(), equalTo(0));
        for (RoutingNode node : state.routingNodes()) {
            if (!node.isEmpty()) {
                assertThat(node.size(), equalTo(2));
            }
        }
        client().admin().indices().prepareUpdateSettings("test").setSettings(settingsBuilder().put(SETTING_NUMBER_OF_REPLICAS, 0)).execute().actionGet();
        ensureGreen();
        state = client().admin().cluster().prepareState().execute().actionGet().getState();

        assertThat(state.routingNodes().unassigned().size(), equalTo(0));
        for (RoutingNode node : state.routingNodes()) {
            if (!node.isEmpty()) {
                assertThat(node.size(), equalTo(1));
            }
        }

        // create another index
        assertAcked(prepareCreate("test2", 3));
        ensureGreen();

        client().admin().indices().prepareUpdateSettings("test").setSettings(settingsBuilder().put(SETTING_NUMBER_OF_REPLICAS, 1)).execute().actionGet();
        ensureGreen();
        state = client().admin().cluster().prepareState().execute().actionGet().getState();

        assertThat(state.routingNodes().unassigned().size(), equalTo(0));
        for (RoutingNode node : state.routingNodes()) {
            if (!node.isEmpty()) {
                assertThat(node.size(), equalTo(4));
            }
        }
    }

    @Repeat(iterations = 20)
    @TestLogging("gateway:TRACE")
    public void testPrimariesStayUnassigned() throws IOException {
        internalCluster().ensureAtLeastNumDataNodes(2);
        final String index = "test";
        prepareCreate(index).setSettings(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 1, IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 0).get();
        ensureGreen(index);
        ClusterState state = client().admin().cluster().prepareState().clear().setNodes(true).setRoutingTable(true).get().getState();
        String primaryNodeId = state.getRoutingTable().index(index).shard(0).primaryShard().currentNodeId();
        String primaryNode = state.nodes().get(primaryNodeId).name();
        int nodeCount = state.getNodes().size();
        logger.info("--> stopping [{}]", primaryNode);
        internalCluster().stopRandomNode(InternalTestCluster.nameFilter(primaryNode));
        final Client client = internalCluster().masterClient();
        logger.info("--> client [{}]", client.getClass());
        logger.info("--> checking [{}] is red (nodes [{}])", index, nodeCount - 1);
        assertThat(client.admin().cluster().prepareHealth(index).setWaitForNodes("" + (nodeCount - 1)).get().getStatus(), is(ClusterHealthStatus.RED));
        internalCluster().startNode();
        ensureGreen(index);
    }
}
