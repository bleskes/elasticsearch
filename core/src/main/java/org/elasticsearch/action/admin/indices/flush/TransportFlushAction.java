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

package org.elasticsearch.action.admin.indices.flush;

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
 * Flush Action.
 */
public class TransportFlushAction extends TransportNodeBroadcastAction<FlushRequest, FlushResponse, ShardFlushRequest, ShardFlushResponse, Boolean> {

    private final IndicesService indicesService;

    @Inject
    public TransportFlushAction(Settings settings, ThreadPool threadPool, ClusterService clusterService,
                                TransportService transportService, IndicesService indicesService,
                                ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver) {
        super(settings, FlushAction.NAME, threadPool, clusterService, transportService, actionFilters, indexNameExpressionResolver,
                FlushRequest.class, ShardFlushRequest.class, ThreadPool.Names.FLUSH);
        this.indicesService = indicesService;
    }

    @Override
    protected FlushResponse newResponse(FlushRequest request, int totalShards, int successfulShards, int failedShards, List<ShardFlushResponse> responses, List<DefaultShardOperationFailedException> shardFailures) {
        return new FlushResponse(totalShards, successfulShards, failedShards, shardFailures);
    }

    @Override
    protected ShardFlushRequest newNodeRequest(String nodeId, FlushRequest request, List<ShardRouting> shards) {
        return new ShardFlushRequest(request, shards, nodeId);
    }

    @Override
    protected ShardFlushResponse newNodeResponse() {
        return new ShardFlushResponse();
    }

    @Override
    protected ShardFlushResponse newNodeResponse(String nodeId, int totalShards, int successfulShards, List<Boolean> results, List<BroadcastShardOperationFailedException> exceptions) {
        return new ShardFlushResponse(nodeId, totalShards, successfulShards, exceptions);
    }

    @Override
    protected Boolean shardOperation(ShardFlushRequest request, ShardRouting shardRouting) {
        IndexShard indexShard = indicesService.indexServiceSafe(shardRouting.getIndex()).shardSafe(shardRouting.id());
        indexShard.flush(request.getRequest());
        return Boolean.TRUE;
    }

    /**
     * The refresh request works against *all* shards.
     */
    @Override
    protected GroupShardsIterator shards(ClusterState clusterState, FlushRequest request, String[] concreteIndices) {
        return clusterState.routingTable().allActiveShardsGrouped(concreteIndices, true, true);
    }

    @Override
    protected ClusterBlockException checkGlobalBlock(ClusterState state, FlushRequest request) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_WRITE);
    }

    @Override
    protected ClusterBlockException checkRequestBlock(ClusterState state, FlushRequest countRequest, String[] concreteIndices) {
        return state.blocks().indicesBlockedException(ClusterBlockLevel.METADATA_WRITE, concreteIndices);
    }
}
