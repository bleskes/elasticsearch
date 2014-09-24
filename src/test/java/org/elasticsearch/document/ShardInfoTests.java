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

package org.elasticsearch.document;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.recovery.RecoveryResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.deletebyquery.IndexDeleteByQueryResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.replication.ReplicationType;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.junit.Test;

import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;
import static org.hamcrest.Matchers.*;

/**
 */
@ElasticsearchIntegrationTest.ClusterScope(scope = ElasticsearchIntegrationTest.Scope.TEST, numDataNodes = 0, numClientNodes = 0)
public class ShardInfoTests extends ElasticsearchIntegrationTest {

    @Test
    public void testIndexAndDelete() throws Exception {
        int numReplicas = scaledRandomIntBetween(0, 10);
        int numCopies = numReplicas + 1;
        logger.info("Number of shard copies {}", numCopies);

        int minNumberOfNodes = numCopies == 1 ? 1 : (numCopies / 2) + 1;
        logger.info("Initially starting {} nodes", minNumberOfNodes);
        internalCluster().startNodesAsync(minNumberOfNodes).get();
        assertAcked(prepareCreate("idx").setSettings(
                ImmutableSettings.builder()
                        .put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 1)
                        .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, numReplicas))
                .get());
        ensureActiveShardCopies(0, minNumberOfNodes);

        IndexResponse indexResponse = client().prepareIndex("idx", "type").setSource("{}").get();
        assertThat(indexResponse.getShardInfo().getTotal(), equalTo(numCopies));
        assertThat(indexResponse.getShardInfo().getSuccessful(), equalTo(minNumberOfNodes));
        DeleteResponse deleteResponse = client().prepareDelete("idx", "type", indexResponse.getId()).get();
        assertThat(deleteResponse.getShardInfo().getTotal(), equalTo(numCopies));
        assertThat(deleteResponse.getShardInfo().getSuccessful(), equalTo(minNumberOfNodes));
        assertThat(deleteResponse.getShardInfo().getPending(), equalTo(0));

        for (int i = minNumberOfNodes + 1; i < numCopies; i++) {
            logger.info("Checking replication information with {} active copies", i);
            internalCluster().startNode();
            ensureActiveShardCopies(0, i);

            indexResponse = client().prepareIndex("idx", "type").setSource("{}").get();
            assertThat(indexResponse.getShardInfo().getTotal(), equalTo(numCopies));
            assertThat(indexResponse.getShardInfo().getSuccessful(), equalTo(i));
            assertThat(deleteResponse.getShardInfo().getPending(), equalTo(0));

            deleteResponse = client().prepareDelete("idx", "type", indexResponse.getId()).get();
            assertThat(deleteResponse.getShardInfo().getTotal(), equalTo(numCopies));
            assertThat(deleteResponse.getShardInfo().getSuccessful(), equalTo(i));
            assertThat(deleteResponse.getShardInfo().getPending(), equalTo(0));
        }
    }

    @Test
    public void testUpdate() throws Exception {
        int numReplicas = scaledRandomIntBetween(0, 10);
        int numCopies = numReplicas + 1;
        logger.info("Number of shard copies {}", numCopies);

        int minNumberOfNodes = numCopies == 1 ? 1 : (numCopies / 2) + 1;
        logger.info("Initially starting {} nodes", minNumberOfNodes);
        internalCluster().startNodesAsync(minNumberOfNodes).get();
        assertAcked(prepareCreate("idx").setSettings(
                ImmutableSettings.builder()
                        .put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 1)
                        .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, numReplicas))
                .get());
        ensureActiveShardCopies(0, minNumberOfNodes);

        UpdateResponse updateResponse = client().prepareUpdate("idx", "type", "1").setDoc("{}").setDocAsUpsert(true).get();
        assertThat(updateResponse.getShardInfo().getTotal(), equalTo(numCopies));
        assertThat(updateResponse.getShardInfo().getSuccessful(), equalTo(minNumberOfNodes));
        assertThat(updateResponse.getShardInfo().getPending(), equalTo(0));

        for (int i = minNumberOfNodes + 1; i < numCopies; i++) {
            logger.info("Checking replication information with {} active replicas", i);
            internalCluster().startNode();
            ensureActiveShardCopies(0, i);

            updateResponse = client().prepareUpdate("idx", "type", "1").setDoc("{}").get();
            assertThat(updateResponse.getShardInfo().getTotal(), equalTo(numCopies));
            assertThat(updateResponse.getShardInfo().getSuccessful(), equalTo(i));
            assertThat(updateResponse.getShardInfo().getPending(), equalTo(0));
        }
    }

    @Test
    public void testBulk_withIndexAndDeleteItems() throws Exception {
        int numReplicas = scaledRandomIntBetween(0, 10);
        int numCopies = numReplicas + 1;
        logger.info("Number of shard copies {}", numCopies);

        int minNumberOfNodes = numCopies == 1 ? 1 : (numCopies / 2) + 1;
        logger.info("Initially starting {} nodes", minNumberOfNodes);
        internalCluster().startNodesAsync(minNumberOfNodes).get();
        assertAcked(prepareCreate("idx").setSettings(
                ImmutableSettings.builder()
                        .put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 1)
                        .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, numReplicas))
                .get());
        ensureActiveShardCopies(0, minNumberOfNodes);

        BulkRequestBuilder bulkRequestBuilder = client().prepareBulk();
        for (int i = 0; i < 10; i++) {
            bulkRequestBuilder.add(client().prepareIndex("idx", "type").setSource("{}"));
        }

        BulkResponse bulkResponse = bulkRequestBuilder.get();
        bulkRequestBuilder = client().prepareBulk();
        for (BulkItemResponse item : bulkResponse) {
            assertThat(item.isFailed(), equalTo(false));
            assertThat(item.getResponse().getShardInfo().getTotal(), equalTo(numCopies));
            assertThat(item.getResponse().getShardInfo().getSuccessful(), equalTo(minNumberOfNodes));
            assertThat(item.getResponse().getShardInfo().getPending(), equalTo(0));

            bulkRequestBuilder.add(client().prepareDelete("idx", "type", item.getId()));
        }

        bulkResponse = bulkRequestBuilder.get();
        for (BulkItemResponse item : bulkResponse) {
            assertThat(item.isFailed(), equalTo(false));
            assertThat(item.getResponse().getShardInfo().getTotal(), equalTo(numCopies));
            assertThat(item.getResponse().getShardInfo().getSuccessful(), equalTo(minNumberOfNodes));
            assertThat(item.getResponse().getShardInfo().getPending(), equalTo(0));
        }

        for (int i = minNumberOfNodes + 1; i < numCopies; i++) {
            logger.info("Checking replication information with {} active replicas", i);
            internalCluster().startNode();
            ensureActiveShardCopies(0, i);

            bulkRequestBuilder = client().prepareBulk();
            for (int j = 0; j < 10; j++) {
                bulkRequestBuilder.add(client().prepareIndex("idx", "type").setSource("{}"));
            }

            bulkResponse = bulkRequestBuilder.get();
            bulkRequestBuilder = client().prepareBulk();
            for (BulkItemResponse item : bulkResponse) {
                assertThat(item.isFailed(), equalTo(false));
                assertThat(item.getResponse().getShardInfo().getTotal(), equalTo(numCopies));
                assertThat(item.getResponse().getShardInfo().getSuccessful(), equalTo(i));
                assertThat(item.getResponse().getShardInfo().getPending(), equalTo(0));

                bulkRequestBuilder.add(client().prepareDelete("idx", "type", item.getId()));
            }

            bulkResponse = bulkRequestBuilder.get();
            for (BulkItemResponse item : bulkResponse) {
                assertThat(item.isFailed(), equalTo(false));
                assertThat(item.getResponse().getShardInfo().getTotal(), equalTo(numCopies));
                assertThat(item.getResponse().getShardInfo().getSuccessful(), equalTo(i));
                assertThat(item.getResponse().getShardInfo().getPending(), equalTo(0));
            }
        }
    }

    @Test
    public void testBulk_withUpdateItems() throws Exception {
        int numReplicas = scaledRandomIntBetween(0, 10);
        int numCopies = numReplicas + 1;
        logger.info("Number of shard copies {}", numCopies);

        int minNumberOfNodes = numCopies == 1 ? 1 : (numCopies / 2) + 1;
        logger.info("Initially starting {} nodes", minNumberOfNodes);
        internalCluster().startNodesAsync(minNumberOfNodes).get();
        assertAcked(prepareCreate("idx").setSettings(
                ImmutableSettings.builder()
                        .put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 1)
                        .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, numReplicas))
                .get());
        ensureActiveShardCopies(0, minNumberOfNodes);

        BulkRequestBuilder bulkRequestBuilder = client().prepareBulk();
        for (int i = 0; i < 10; i++) {
            bulkRequestBuilder.add(client().prepareUpdate("idx", "type", Integer.toString(i)).setDoc("{}").setDocAsUpsert(true));
        }

        BulkResponse bulkResponse = bulkRequestBuilder.get();
        for (BulkItemResponse item : bulkResponse) {
            assertThat(item.isFailed(), equalTo(false));
            assertThat(item.getResponse().getShardInfo().getTotal(), equalTo(numCopies));
            assertThat(item.getResponse().getShardInfo().getSuccessful(), equalTo(minNumberOfNodes));
            assertThat(item.getResponse().getShardInfo().getPending(), equalTo(0));
        }

        for (int i = minNumberOfNodes + 1; i < numCopies; i++) {
            logger.info("Checking replication information with {} active replicas", i);
            internalCluster().startNode();
            ensureActiveShardCopies(0, i);

            bulkRequestBuilder = client().prepareBulk();
            for (int j = 0; j < 10; j++) {
                bulkRequestBuilder.add(client().prepareUpdate("idx", "type", Integer.toString(i)).setDoc("{}"));
            }

            bulkResponse = bulkRequestBuilder.get();
            for (BulkItemResponse item : bulkResponse) {
                assertThat(item.isFailed(), equalTo(false));
                assertThat(item.getResponse().getShardInfo().getTotal(), equalTo(numCopies));
                assertThat(item.getResponse().getShardInfo().getSuccessful(), equalTo(i));
                assertThat(item.getResponse().getShardInfo().getPending(), equalTo(0));
            }
        }
    }

    @Test
    public void testDeleteWithRoutingRequiredButNotSpecified() throws Exception {
        int numReplicas = scaledRandomIntBetween(0, 10);
        int numCopies = numReplicas + 1;
        logger.info("Number of shard copies {}", numCopies);

        int minNumberOfNodes = numCopies == 1 ? 1 : (numCopies / 2) + 1;
        logger.info("Initially starting {} nodes", minNumberOfNodes);
        internalCluster().startNodesAsync(minNumberOfNodes).get();
        assertAcked(prepareCreate("idx").setSettings(
                ImmutableSettings.builder()
                        .put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 2)
                        .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, numReplicas))
                .addMapping("type", "_routing", "required=true")
                .get());
        ensureActiveShardCopies(0, minNumberOfNodes);
        ensureActiveShardCopies(1, minNumberOfNodes);

        DeleteResponse deleteResponse = client().prepareDelete("idx", "type", "1").get();
        assertThat(deleteResponse.getShardInfo().getTotal(), equalTo(numCopies * 2));
        assertThat(deleteResponse.getShardInfo().getSuccessful(), equalTo(minNumberOfNodes * 2));
        assertThat(deleteResponse.getShardInfo().getPending(), equalTo(0));

        for (int i = minNumberOfNodes + 1; i < numCopies; i++) {
            logger.info("Checking replication information with {} active replicas", i);
            internalCluster().startNode();
            ensureActiveShardCopies(0, i);
            ensureActiveShardCopies(1, i);

            deleteResponse = client().prepareDelete("idx", "type", "1").get();
            assertThat(deleteResponse.getShardInfo().getTotal(), equalTo(numCopies * 2));
            assertThat(deleteResponse.getShardInfo().getSuccessful(), equalTo(i * 2));
            assertThat(deleteResponse.getShardInfo().getPending(), equalTo(0));
        }
    }

    @Test
    public void testDeleteByQuery() throws Exception {
        int numReplicas = scaledRandomIntBetween(0, 10);
        int numCopies = numReplicas + 1;
        logger.info("Number of shard copies {}", numCopies);

        int minNumberOfNodes = numCopies == 1 ? 1 : (numCopies / 2) + 1;
        logger.info("Initially starting {} nodes", minNumberOfNodes);
        internalCluster().startNodesAsync(minNumberOfNodes).get();
        assertAcked(prepareCreate("idx").setSettings(
                ImmutableSettings.builder()
                        .put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 2)
                        .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, numReplicas))
                .get());
        ensureActiveShardCopies(0, minNumberOfNodes);
        ensureActiveShardCopies(1, minNumberOfNodes);

        IndexDeleteByQueryResponse indexDeleteByQueryResponse = client().prepareDeleteByQuery("idx")
                .setQuery(QueryBuilders.matchAllQuery())
                .get().getIndex("idx");

        assertThat(indexDeleteByQueryResponse.getShardInfo().getTotal(), equalTo(numCopies * 2));
        assertThat(indexDeleteByQueryResponse.getShardInfo().getSuccessful(), equalTo(minNumberOfNodes * 2));
        assertThat(indexDeleteByQueryResponse.getShardInfo().getPending(), equalTo(0));

        for (int i = minNumberOfNodes + 1; i < numCopies; i++) {
            logger.info("Checking replication information with {} active replicas", i);
            internalCluster().startNode();
            ensureActiveShardCopies(0, i);
            ensureActiveShardCopies(1, i);

            indexDeleteByQueryResponse = client().prepareDeleteByQuery("idx")
                    .setQuery(QueryBuilders.matchAllQuery())
                    .get().getIndex("idx");
            assertThat(indexDeleteByQueryResponse.getShardInfo().getTotal(), equalTo(numCopies * 2));
            assertThat(indexDeleteByQueryResponse.getShardInfo().getSuccessful(), equalTo(i * 2));
            assertThat(indexDeleteByQueryResponse.getShardInfo().getPending(), equalTo(0));
        }
    }

    @Test
    public void testIndexWithAsyncReplication() throws Exception {
        int numReplicas = scaledRandomIntBetween(0, 10);
        int numCopies = numReplicas + 1;
        logger.info("Number of shard copies {}", numCopies);

        int minNumberOfNodes = numCopies == 1 ? 1 : (numCopies / 2) + 1;
        logger.info("Initially starting {} nodes", minNumberOfNodes);
        internalCluster().startNodesAsync(minNumberOfNodes).get();
        assertAcked(prepareCreate("idx").setSettings(
                ImmutableSettings.builder()
                        .put(IndexMetaData.SETTING_NUMBER_OF_SHARDS, 1)
                        .put(IndexMetaData.SETTING_NUMBER_OF_REPLICAS, numReplicas))
                .get());
        ensureActiveShardCopies(0, minNumberOfNodes);

        IndexResponse indexResponse = client().prepareIndex("idx", "type")
                .setReplicationType(ReplicationType.ASYNC)
                .setSource("{}")
                .get();
        assertThat(indexResponse.getShardInfo().getTotal(), equalTo(numCopies));
        assertThat(indexResponse.getShardInfo().getSuccessful(), equalTo(1));
        assertThat(indexResponse.getShardInfo().getPending(), equalTo(minNumberOfNodes - 1));

        for (int i = minNumberOfNodes + 1; i < numCopies; i++) {
            logger.info("Checking replication information with {} active replicas", i);
            internalCluster().startNode();
            ensureActiveShardCopies(0, i);

            indexResponse = client().prepareIndex("idx", "type")
                    .setReplicationType(ReplicationType.ASYNC)
                    .setSource("{}")
                    .get();
            assertThat(indexResponse.getShardInfo().getTotal(), equalTo(numCopies));
            assertThat(indexResponse.getShardInfo().getSuccessful(), equalTo(1));
            assertThat(indexResponse.getShardInfo().getPending(), equalTo(i - 1));
        }
    }

    private void ensureActiveShardCopies(final int shardId, final int copyCount) throws Exception {
        assertBusy(new Runnable() {
            @Override
            public void run() {
                ClusterState state = client().admin().cluster().prepareState().get().getState();
                assertThat(state.routingTable().index("idx"), not(nullValue()));
                assertThat(state.routingTable().index("idx").shard(shardId), not(nullValue()));
                assertThat(state.routingTable().index("idx").shard(shardId).activeShards().size(), equalTo(copyCount));

                ClusterHealthResponse healthResponse = client().admin().cluster().prepareHealth("idx")
                        .setWaitForRelocatingShards(0)
                        .get();
                assertThat(healthResponse.isTimedOut(), equalTo(false));

                RecoveryResponse recoveryResponse = client().admin().indices().prepareRecoveries("idx")
                        .setActiveOnly(true)
                        .get();
                assertThat(recoveryResponse.shardResponses().get("idx").size(), equalTo(0));
            }
        });
    }

}
