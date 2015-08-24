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
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.DefaultShardOperationFailedException;
import org.elasticsearch.action.support.broadcast.BroadcastShardOperationFailedException;
import org.elasticsearch.action.support.indices.BaseNodeBroadcastRequest;
import org.elasticsearch.action.support.indices.TransportNodeBroadcastAction;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.routing.GroupShardsIterator;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.IndexService;
import org.elasticsearch.index.shard.IndexShard;
import org.elasticsearch.indices.IndicesService;
import org.elasticsearch.indices.recovery.RecoveryState;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

import java.util.List;
import java.util.Map;

/**
 * Transport action for shard recovery operation. This transport action does not actually
 * perform shard recovery, it only reports on recoveries (both active and complete).
 */
public class TransportRecoveryAction extends TransportNodeBroadcastAction<RecoveryRequest, RecoveryResponse, TransportRecoveryAction.ShardRecoveryRequest, RecoveryNodeResponse, RecoveryState> {

    private final IndicesService indicesService;

    @Inject
    public TransportRecoveryAction(Settings settings, ThreadPool threadPool, ClusterService clusterService,
                                   TransportService transportService, IndicesService indicesService,
                                   ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver) {
        super(settings, RecoveryAction.NAME, threadPool, clusterService, transportService, actionFilters, indexNameExpressionResolver,
                RecoveryRequest.class, ShardRecoveryRequest.class, ThreadPool.Names.MANAGEMENT);
        this.indicesService = indicesService;
    }

    @Override
    protected RecoveryResponse newResponse(RecoveryRequest request, int totalShards, int successfulShards, int failedShards, List<RecoveryNodeResponse> responses, List<DefaultShardOperationFailedException> shardFailures) {
        Map<String, List<RecoveryState>> shardResponses = Maps.newHashMap();
        for (RecoveryNodeResponse response : responses) {
            for (RecoveryState recoveryState : response.recoveryStates) {
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
        }
        return new RecoveryResponse(totalShards, successfulShards, failedShards, request.detailed(), shardResponses, shardFailures);
    }

    @Override
    protected ShardRecoveryRequest newNodeRequest(String nodeId, RecoveryRequest request, List<ShardRouting> shards) {
        return new ShardRecoveryRequest(request, shards, nodeId);
    }

    @Override
    protected RecoveryNodeResponse newNodeResponse() {
        return new RecoveryNodeResponse();
    }

    @Override
    protected RecoveryNodeResponse newNodeResponse(String nodeId, int totalShards, int successfulShards, List<RecoveryState> results, List<BroadcastShardOperationFailedException> exceptions) {
        RecoveryNodeResponse response = new RecoveryNodeResponse(nodeId, totalShards, successfulShards, exceptions);
        response.setRecoveryStates(results);
        return response;
    }

    @Override
    protected RecoveryState shardOperation(ShardRecoveryRequest request, ShardRouting shardRouting) {
        IndexService indexService = indicesService.indexServiceSafe(shardRouting.shardId().getIndex());
        IndexShard indexShard = indexService.shardSafe(shardRouting.shardId().id());
        return indexShard.recoveryState();
    }

    @Override
    protected GroupShardsIterator shards(ClusterState state, RecoveryRequest request, String[] concreteIndices) {
        return state.routingTable().allAssignedShardsGrouped(concreteIndices, true, true);
    }

    @Override
    protected ClusterBlockException checkGlobalBlock(ClusterState state, RecoveryRequest request) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.READ);
    }

    @Override
    protected ClusterBlockException checkRequestBlock(ClusterState state, RecoveryRequest request, String[] concreteIndices) {
        return state.blocks().indicesBlockedException(ClusterBlockLevel.READ, concreteIndices);
    }

    static class ShardRecoveryRequest extends BaseNodeBroadcastRequest<RecoveryRequest> {

        ShardRecoveryRequest() {
        }

        ShardRecoveryRequest(RecoveryRequest request, List<ShardRouting> shards, String nodeId) {
            super(nodeId, request, shards);
        }

        @Override
        protected RecoveryRequest newRequest() {
            return new RecoveryRequest();
        }
    }
}