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

import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.elasticsearch.test.junit.annotations.TestLogging;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.not;

@ElasticsearchIntegrationTest.ClusterScope(scope = ElasticsearchIntegrationTest.Scope.TEST, numDataNodes = 0)
public class CloseAfterRecoveryTest extends ElasticsearchIntegrationTest {

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        ImmutableSettings.Builder builder = ImmutableSettings.builder().put(super.nodeSettings(nodeOrdinal));
        builder.put("gateway.type", "local");
        return builder.build();
    }

    @TestLogging(value = "cluster.service:trace")
    public void test() throws ExecutionException, InterruptedException {
        List<String> nodes = internalCluster().startNodesAsync(3).get();
        prepareCreate("test").setSettings(
                IndexMetaData.SETTING_NUMBER_OF_SHARDS, 1, IndexMetaData.SETTING_NUMBER_OF_REPLICAS, 1,
                "index.routing.allocation.include._name", nodes.get(0)).get();
        ensureYellow();
        logger.info("--> assigning replica to node1");
        client().admin().indices().prepareUpdateSettings("test").setSettings("index.routing.allocation.include._name: " + nodes.get(0) + "," + nodes.get(1)).get();
        ensureGreen();
        logger.info("--> moving primary to node2");
        client().admin().indices().prepareUpdateSettings("test").setSettings("index.routing.allocation.include._name: " + nodes.get(1) + "," + nodes.get(2)).get();

        client().admin().cluster().prepareHealth().setWaitForRelocatingShards(0).get();

        logger.info("--> removing any limitation");
        client().admin().indices().prepareUpdateSettings("test").setSettings("index.routing.allocation.include._name: " + Strings.collectionToDelimitedString(nodes, ",")).get();

        logger.info("--> closing");
        client().admin().indices().prepareClose("test").get();

        logger.info("--> opening");
        client().admin().indices().prepareOpen("test").get();
        ensureGreen();

        ClusterState state = internalCluster().clusterService().state();
        assertThat(state.routingNodes().activePrimary(new ShardId("test", 0)).currentNodeId(),
                not(equalTo(state.nodes().resolveNode(nodes.get(0)).id())));
    }
}
