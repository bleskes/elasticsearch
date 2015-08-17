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

package org.elasticsearch.action.admin.indices.refresh;

import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.DefaultShardOperationFailedException;
import org.elasticsearch.action.support.broadcast.BroadcastShardOperationFailedException;
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
import org.elasticsearch.index.shard.IndexShard;
import org.elasticsearch.indices.IndicesService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

import java.util.List;

/**
 * Refresh action.
 */
public class TransportRefreshAction extends TransportNodeBroadcastAction<RefreshRequest, RefreshResponse, ShardRefreshRequest, ShardRefreshResponse, Boolean> {

    private final IndicesService indicesService;

    @Inject
    public TransportRefreshAction(Settings settings, ThreadPool threadPool, ClusterService clusterService,
                                  TransportService transportService, IndicesService indicesService,
                                  ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver) {
        super(settings, RefreshAction.NAME, threadPool, clusterService, transportService, actionFilters, indexNameExpressionResolver,
                RefreshRequest.class, ShardRefreshRequest.class, ThreadPool.Names.REFRESH);
        this.indicesService = indicesService;
    }

    @Override
    protected RefreshResponse newResponse(RefreshRequest request, int totalShards, int successfulShards, int failedShards, List<ShardRefreshResponse> shardRefreshResponses, List<DefaultShardOperationFailedException> shardFailures) {
        return new RefreshResponse(totalShards, successfulShards, failedShards, shardFailures);
    }

    @Override
    protected ShardRefreshRequest newNodeRequest(String nodeId, RefreshRequest request, List<ShardRouting> shards) {
        return new ShardRefreshRequest(nodeId, shards, request);
    }

    @Override
    protected ShardRefreshResponse newNodeResponse() {
        return new ShardRefreshResponse();
    }

    @Override
    protected ShardRefreshResponse newNodeResponse(String nodeId, int totalShards, int successfulShards, List<Boolean> results, List<BroadcastShardOperationFailedException> exceptions) {
        return new ShardRefreshResponse(nodeId, totalShards, successfulShards, exceptions);
    }

    @Override
    protected Boolean shardOperation(ShardRefreshRequest request, ShardRouting shardRouting) {
        IndexShard indexShard = indicesService.indexServiceSafe(shardRouting.getIndex()).shardSafe(shardRouting.shardId().id());
        indexShard.refresh("api");
        logger.trace("{} refresh request executed", indexShard.shardId());
        return Boolean.TRUE;
    }

    /**
     * The refresh request works against *all* shards.
     */
    @Override
    protected GroupShardsIterator shards(ClusterState clusterState, RefreshRequest request, String[] concreteIndices) {
        return clusterState.routingTable().allAssignedShardsGrouped(concreteIndices, true, true);
    }

    @Override
    protected ClusterBlockException checkGlobalBlock(ClusterState state, RefreshRequest request) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_WRITE);
    }

    @Override
    protected ClusterBlockException checkRequestBlock(ClusterState state, RefreshRequest countRequest, String[] concreteIndices) {
        return state.blocks().indicesBlockedException(ClusterBlockLevel.METADATA_WRITE, concreteIndices);
    }
}
