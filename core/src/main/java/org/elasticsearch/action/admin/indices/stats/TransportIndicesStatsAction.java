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

package org.elasticsearch.action.admin.indices.stats;

import com.google.common.collect.Lists;
import org.elasticsearch.action.ShardOperationFailedException;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.broadcast.BroadcastShardOperationFailedException;
import org.elasticsearch.action.support.broadcast.node.TransportBroadcastByNodeAction;
import org.elasticsearch.action.support.indices.BaseBroadcastByNodeRequest;
import org.elasticsearch.action.support.indices.BaseBroadcastByNodeResponse;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.cluster.routing.ShardsIterator;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.IndexService;
import org.elasticsearch.index.shard.IndexShard;
import org.elasticsearch.index.shard.ShardNotFoundException;
import org.elasticsearch.indices.IndicesService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class TransportIndicesStatsAction extends TransportBroadcastByNodeAction<IndicesStatsRequest, IndicesStatsResponse, TransportIndicesStatsAction.IndexShardStatsRequest, TransportIndicesStatsAction.IndexShardStatsResponse, ShardStats> {

    private final IndicesService indicesService;

    @Inject
    public TransportIndicesStatsAction(Settings settings, ThreadPool threadPool, ClusterService clusterService,
                                       TransportService transportService, IndicesService indicesService,
                                       ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver) {
        super(settings, IndicesStatsAction.NAME, threadPool, clusterService, transportService, actionFilters, indexNameExpressionResolver,
                IndicesStatsRequest.class, IndexShardStatsRequest.class, ThreadPool.Names.MANAGEMENT);
        this.indicesService = indicesService;
    }

    /**
     * Status goes across *all* shards.
     */
    @Override
    protected ShardsIterator shards(ClusterState clusterState, IndicesStatsRequest request, String[] concreteIndices) {
        return clusterState.routingTable().allAssignedShards(concreteIndices, true);
    }

    @Override
    protected ClusterBlockException checkGlobalBlock(ClusterState state, IndicesStatsRequest request) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_READ);
    }

    @Override
    protected ClusterBlockException checkRequestBlock(ClusterState state, IndicesStatsRequest request, String[] concreteIndices) {
        return state.blocks().indicesBlockedException(ClusterBlockLevel.METADATA_READ, concreteIndices);
    }

    @Override
    protected IndicesStatsResponse newResponse(IndicesStatsRequest request, int totalShards, int successfulShards, int failedShards, List<IndexShardStatsResponse> responses, List<ShardOperationFailedException> shardFailures) {
        List<ShardStats> concatenation = Lists.newArrayList();
        for (IndexShardStatsResponse response : responses) {
            concatenation.addAll(response.getShards());
        }
        return new IndicesStatsResponse(concatenation.toArray(new ShardStats[concatenation.size()]), totalShards, successfulShards, failedShards, shardFailures);
    }

    @Override
    protected IndexShardStatsRequest newNodeRequest(String nodeId, IndicesStatsRequest request, List<ShardRouting> shards) {
        return new IndexShardStatsRequest(request, shards, nodeId);
    }

    @Override
    protected IndexShardStatsResponse newNodeResponse() {
        return new IndexShardStatsResponse();
    }

    @Override
    protected IndexShardStatsResponse newNodeResponse(String nodeId, int totalShards, int successfulShards, List<ShardStats> shards, List<BroadcastShardOperationFailedException> exceptions) {
        return new IndexShardStatsResponse(nodeId, totalShards, successfulShards, shards, exceptions);
    }

    @Override
    protected ShardStats shardOperation(IndexShardStatsRequest request, ShardRouting shardRouting) {
        IndexService indexService = indicesService.indexServiceSafe(shardRouting.shardId().getIndex());
        IndexShard indexShard = indexService.shardSafe(shardRouting.shardId().id());
        // if we don't have the routing entry yet, we need it stats wise, we treat it as if the shard is not ready yet
        if (indexShard.routingEntry() == null) {
            throw new ShardNotFoundException(indexShard.shardId());
        }

        CommonStatsFlags flags = new CommonStatsFlags().clear();

        if (request.getIndicesLevelRequest().docs()) {
            flags.set(CommonStatsFlags.Flag.Docs);
        }
        if (request.getIndicesLevelRequest().store()) {
            flags.set(CommonStatsFlags.Flag.Store);
        }
        if (request.getIndicesLevelRequest().indexing()) {
            flags.set(CommonStatsFlags.Flag.Indexing);
            flags.types(request.getIndicesLevelRequest().types());
        }
        if (request.getIndicesLevelRequest().get()) {
            flags.set(CommonStatsFlags.Flag.Get);
        }
        if (request.getIndicesLevelRequest().search()) {
            flags.set(CommonStatsFlags.Flag.Search);
            flags.groups(request.getIndicesLevelRequest().groups());
        }
        if (request.getIndicesLevelRequest().merge()) {
            flags.set(CommonStatsFlags.Flag.Merge);
        }
        if (request.getIndicesLevelRequest().refresh()) {
            flags.set(CommonStatsFlags.Flag.Refresh);
        }
        if (request.getIndicesLevelRequest().flush()) {
            flags.set(CommonStatsFlags.Flag.Flush);
        }
        if (request.getIndicesLevelRequest().warmer()) {
            flags.set(CommonStatsFlags.Flag.Warmer);
        }
        if (request.getIndicesLevelRequest().queryCache()) {
            flags.set(CommonStatsFlags.Flag.QueryCache);
        }
        if (request.getIndicesLevelRequest().fieldData()) {
            flags.set(CommonStatsFlags.Flag.FieldData);
            flags.fieldDataFields(request.getIndicesLevelRequest().fieldDataFields());
        }
        if (request.getIndicesLevelRequest().percolate()) {
            flags.set(CommonStatsFlags.Flag.Percolate);
        }
        if (request.getIndicesLevelRequest().segments()) {
            flags.set(CommonStatsFlags.Flag.Segments);
        }
        if (request.getIndicesLevelRequest().completion()) {
            flags.set(CommonStatsFlags.Flag.Completion);
            flags.completionDataFields(request.getIndicesLevelRequest().completionFields());
        }
        if (request.getIndicesLevelRequest().translog()) {
            flags.set(CommonStatsFlags.Flag.Translog);
        }
        if (request.getIndicesLevelRequest().suggest()) {
            flags.set(CommonStatsFlags.Flag.Suggest);
        }
        if (request.getIndicesLevelRequest().requestCache()) {
            flags.set(CommonStatsFlags.Flag.RequestCache);
        }
        if (request.getIndicesLevelRequest().recovery()) {
            flags.set(CommonStatsFlags.Flag.Recovery);
        }

        return new ShardStats(indexShard, indexShard.routingEntry(), flags);
    }

    static class IndexShardStatsRequest extends BaseBroadcastByNodeRequest<IndicesStatsRequest> {
        IndexShardStatsRequest() {
        }

        IndexShardStatsRequest(IndicesStatsRequest request, List<ShardRouting> shards, String nodeId) {
            super(nodeId, request, shards);
        }
        
        @Override
        protected IndicesStatsRequest newRequest() {
            return new IndicesStatsRequest();
        }
    }

    public class IndexShardStatsResponse extends BaseBroadcastByNodeResponse {
        List<ShardStats> shards;

        public IndexShardStatsResponse() {
        }

        public IndexShardStatsResponse(String nodeId, int totalShards, int successfulShards, List<ShardStats> shards, List<BroadcastShardOperationFailedException> exceptions) {
            super(nodeId, totalShards, successfulShards, exceptions);
            this.shards = shards;
        }

        public List<ShardStats> getShards() {
            return shards;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            if (in.readBoolean()) {
                int size = in.readVInt();
                shards = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    shards.add(ShardStats.readShardStats(in));
                }
            } else {
                shards = null;
            }
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeBoolean(shards != null);
            if (shards != null) {
                int size = shards.size();
                out.writeVInt(size);
                for (int i = 0; i < size; i++) {
                    shards.get(i).writeTo(out);
                }
            }
        }
    }
}
