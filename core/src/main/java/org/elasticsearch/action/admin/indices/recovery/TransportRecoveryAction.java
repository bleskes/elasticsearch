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

package org.elasticsearch.action.admin.indices.recovery;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.elasticsearch.action.ShardOperationFailedException;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.broadcast.BroadcastShardOperationFailedException;
import org.elasticsearch.action.support.broadcast.node.TransportBroadcastByNodeAction;
import org.elasticsearch.action.support.indices.BaseBroadcastByNodeResponse;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.cluster.routing.ShardsIterator;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.IndexService;
import org.elasticsearch.index.shard.IndexShard;
import org.elasticsearch.indices.IndicesService;
import org.elasticsearch.indices.recovery.RecoveryState;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Transport action for shard recovery operation. This transport action does not actually
 * perform shard recovery, it only reports on recoveries (both active and complete).
 */
public class TransportRecoveryAction extends TransportBroadcastByNodeAction<RecoveryRequest, RecoveryResponse, RecoveryState> {

    private final IndicesService indicesService;

    @Inject
    public TransportRecoveryAction(Settings settings, ThreadPool threadPool, ClusterService clusterService,
                                   TransportService transportService, IndicesService indicesService,
                                   ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver) {
        super(settings, RecoveryAction.NAME, threadPool, clusterService, transportService, actionFilters, indexNameExpressionResolver,
                RecoveryRequest.class, ThreadPool.Names.MANAGEMENT);
        this.indicesService = indicesService;
    }

    @Override
    protected RecoveryState readShardResult(StreamInput in) throws IOException {
        return RecoveryState.readRecoveryState(in);
    }


    @Override
    protected RecoveryResponse newResponse(RecoveryRequest request, int totalShards, int successfulShards, int failedShards, List<RecoveryState> responses, List<ShardOperationFailedException> shardFailures) {
        Map<String, List<RecoveryState>> shardResponses = Maps.newHashMap();
        for (RecoveryState recoveryState : responses) {
            if (recoveryState == null) {
                continue;
            }
            String indexName = recoveryState.getShardId().getIndex();
            if (!shardResponses.containsKey(indexName)) {
                shardResponses.put(indexName, Lists.<RecoveryState>newArrayList());
            }
            if (request.activeOnly()) {
                if (recoveryState.getStage() != RecoveryState.Stage.DONE) {
                    shardResponses.get(indexName).add(recoveryState);
                }
            } else {
                shardResponses.get(indexName).add(recoveryState);
            }
        }
        return new RecoveryResponse(totalShards, successfulShards, failedShards, request.detailed(), shardResponses, shardFailures);
    }

    @Override
    protected RecoveryRequest readRequestFrom(StreamInput in) throws IOException {
        final RecoveryRequest recoveryRequest = new RecoveryRequest();
        recoveryRequest.readFrom(in);
        return recoveryRequest;
    }

    @Override
    protected RecoveryState shardOperation(RecoveryRequest request, ShardRouting shardRouting) {
        IndexService indexService = indicesService.indexServiceSafe(shardRouting.shardId().getIndex());
        IndexShard indexShard = indexService.shardSafe(shardRouting.shardId().id());
        return indexShard.recoveryState();
    }

    @Override
    protected ShardsIterator shards(ClusterState state, RecoveryRequest request, String[] concreteIndices) {
        return state.routingTable().allAssignedShards(concreteIndices, true, true);
    }

    @Override
    protected ClusterBlockException checkGlobalBlock(ClusterState state, RecoveryRequest request) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.READ);
    }

    @Override
    protected ClusterBlockException checkRequestBlock(ClusterState state, RecoveryRequest request, String[] concreteIndices) {
        return state.blocks().indicesBlockedException(ClusterBlockLevel.READ, concreteIndices);
    }

    /**
     * Information regarding the recovery state of a shard.
     */
    public static class NodeRecoveryResponse extends BaseBroadcastByNodeResponse {

        List<RecoveryState> recoveryStates;

        public NodeRecoveryResponse() {
        }

        /**
         * Constructs shard recovery in formation for the given index and shard id.
         *
         * @param nodeId           Id of the node
         * @param totalShards      The total number of shards for which the operation was performed
         * @param successfulShards The number of shards for which the operation was successful
         * @param exceptions       The exceptions from the failed shards
         */
        public NodeRecoveryResponse(String nodeId, int totalShards, int successfulShards, List<BroadcastShardOperationFailedException> exceptions) {
            super(nodeId, totalShards, successfulShards, exceptions);
        }

        /**
         * Sets the recovery state information for the shard.
         *
         * @param recoveryStates Recovery states
         */
        public void setRecoveryStates(List<RecoveryState> recoveryStates) {
            this.recoveryStates = recoveryStates;
        }

        /**
         * Gets the recovery state information for the shard. Null if shard wasn't recovered / recovery didn't start yet.
         *
         * @return Recovery state
         */
        @Nullable
        public List<RecoveryState> recoveryStates() {
            return recoveryStates;
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeVInt(recoveryStates.size());
            for (int i = 0; i < recoveryStates.size(); i++) {
                recoveryStates.get(i).writeTo(out);
            }
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            int size = in.readVInt();
            recoveryStates = Lists.newArrayListWithCapacity(size);
            for (int i = 0; i < size; i++) {
                recoveryStates.add(RecoveryState.readRecoveryState(in));
            }
        }

        /**
         * Builds a new NodeRecoveryResponse from the give input stream.
         *
         * @param in Input stream
         * @return A new NodeRecoveryResponse
         * @throws IOException
         */
        public static NodeRecoveryResponse readShardRecoveryResponse(StreamInput in) throws IOException {
            NodeRecoveryResponse response = new NodeRecoveryResponse();
            response.readFrom(in);
            return response;
        }
    }
}