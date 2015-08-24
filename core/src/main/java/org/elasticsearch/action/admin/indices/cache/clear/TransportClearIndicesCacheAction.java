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

package org.elasticsearch.action.admin.indices.cache.clear;

import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.DefaultShardOperationFailedException;
import org.elasticsearch.action.support.broadcast.BroadcastShardOperationFailedException;
import org.elasticsearch.action.support.indices.TransportBroadcastByNodeAction;
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
import org.elasticsearch.indices.cache.request.IndicesRequestCache;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

import java.util.List;

/**
 * Indices clear cache action.
 */
public class TransportClearIndicesCacheAction extends TransportBroadcastByNodeAction<ClearIndicesCacheRequest, ClearIndicesCacheResponse, ShardClearIndicesCacheRequest, ShardClearIndicesCacheResponse, Boolean> {

    private final IndicesService indicesService;
    private final IndicesRequestCache indicesRequestCache;

    @Inject
    public TransportClearIndicesCacheAction(Settings settings, ThreadPool threadPool, ClusterService clusterService,
                                            TransportService transportService, IndicesService indicesService,
                                            IndicesRequestCache indicesQueryCache, ActionFilters actionFilters,
                                            IndexNameExpressionResolver indexNameExpressionResolver) {
        super(settings, ClearIndicesCacheAction.NAME, threadPool, clusterService, transportService, actionFilters, indexNameExpressionResolver,
                ClearIndicesCacheRequest.class, ShardClearIndicesCacheRequest.class, ThreadPool.Names.MANAGEMENT);
        this.indicesService = indicesService;
        this.indicesRequestCache = indicesQueryCache;
    }

    @Override
    protected ClearIndicesCacheResponse newResponse(ClearIndicesCacheRequest request, int totalShards, int successfulShards, int failedShards, List<ShardClearIndicesCacheResponse> responses, List<DefaultShardOperationFailedException> shardFailures) {
        return new ClearIndicesCacheResponse(totalShards, successfulShards, failedShards, shardFailures);
    }

    @Override
    protected ShardClearIndicesCacheRequest newNodeRequest(String nodeId, ClearIndicesCacheRequest request, List<ShardRouting> shards) {
        return new ShardClearIndicesCacheRequest(request, shards, nodeId);
    }

    @Override
    protected ShardClearIndicesCacheResponse newNodeResponse() {
        return new ShardClearIndicesCacheResponse();
    }

    @Override
    protected ShardClearIndicesCacheResponse newNodeResponse(String nodeId, int totalShards, int successfulShards, List<Boolean> results, List<BroadcastShardOperationFailedException> exceptions) {
        return new ShardClearIndicesCacheResponse(nodeId, totalShards, successfulShards, exceptions);
    }

    @Override
    protected Boolean shardOperation(ShardClearIndicesCacheRequest request, ShardRouting shardRouting) {
        IndexService service = indicesService.indexService(shardRouting.getIndex());
        if (service != null) {
            IndexShard shard = service.shard(shardRouting.id());
            boolean clearedAtLeastOne = false;
            if (request.queryCache()) {
                clearedAtLeastOne = true;
                service.cache().query().clear("api");
            }
            if (request.fieldDataCache()) {
                clearedAtLeastOne = true;
                if (request.fields() == null || request.fields().length == 0) {
                    service.fieldData().clear();
                } else {
                    for (String field : request.fields()) {
                        service.fieldData().clearField(field);
                    }
                }
            }
            if (request.requestCache()) {
                clearedAtLeastOne = true;
                indicesRequestCache.clear(shard);
            }
            if (request.recycler()) {
                logger.debug("Clear CacheRecycler on index [{}]", service.index());
                clearedAtLeastOne = true;
                // cacheRecycler.clear();
            }
            if (!clearedAtLeastOne) {
                if (request.fields() != null && request.fields().length > 0) {
                    // only clear caches relating to the specified fields
                    for (String field : request.fields()) {
                        service.fieldData().clearField(field);
                    }
                } else {
                    service.cache().clear("api");
                    service.fieldData().clear();
                    indicesRequestCache.clear(shard);
                }
            }
        }
        return Boolean.TRUE;
    }

    /**
     * The refresh request works against *all* shards.
     */
    @Override
    protected GroupShardsIterator shards(ClusterState clusterState, ClearIndicesCacheRequest request, String[] concreteIndices) {
        return clusterState.routingTable().allActiveShardsGrouped(concreteIndices, true);
    }

    @Override
    protected ClusterBlockException checkGlobalBlock(ClusterState state, ClearIndicesCacheRequest request) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_WRITE);
    }

    @Override
    protected ClusterBlockException checkRequestBlock(ClusterState state, ClearIndicesCacheRequest request, String[] concreteIndices) {
        return state.blocks().indicesBlockedException(ClusterBlockLevel.METADATA_WRITE, concreteIndices);
    }

}
