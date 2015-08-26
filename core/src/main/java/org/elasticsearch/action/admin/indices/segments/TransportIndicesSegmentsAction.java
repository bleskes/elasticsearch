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

package org.elasticsearch.action.admin.indices.segments;

import com.google.common.collect.Lists;
import org.elasticsearch.action.ShardOperationFailedException;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.DefaultShardOperationFailedException;
import org.elasticsearch.action.support.broadcast.BroadcastShardOperationFailedException;
import org.elasticsearch.action.support.indices.BaseBroadcastByNodeRequest;
import org.elasticsearch.action.support.indices.BaseBroadcastByNodeResponse;
import org.elasticsearch.action.support.broadcast.node.TransportBroadcastByNodeAction;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.routing.GroupShardsIterator;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.cluster.routing.ShardsIterator;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.IndexService;
import org.elasticsearch.index.shard.IndexShard;
import org.elasticsearch.indices.IndicesService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

import java.io.IOException;
import java.util.List;

/**
 *
 */
public class TransportIndicesSegmentsAction extends TransportBroadcastByNodeAction<IndicesSegmentsRequest, IndicesSegmentResponse, TransportIndicesSegmentsAction.IndexShardSegmentRequest, TransportIndicesSegmentsAction.IndexShardSegmentResponse, ShardSegments> {

    private final IndicesService indicesService;

    @Inject
    public TransportIndicesSegmentsAction(Settings settings, ThreadPool threadPool, ClusterService clusterService, TransportService transportService,
                                          IndicesService indicesService, ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver) {
        super(settings, IndicesSegmentsAction.NAME, threadPool, clusterService, transportService, actionFilters, indexNameExpressionResolver,
                IndicesSegmentsRequest.class, TransportIndicesSegmentsAction.IndexShardSegmentRequest.class, ThreadPool.Names.MANAGEMENT);
        this.indicesService = indicesService;
    }

    /**
     * Segments goes across *all* active shards.
     */
    @Override
    protected ShardsIterator shards(ClusterState clusterState, IndicesSegmentsRequest request, String[] concreteIndices) {
        return clusterState.routingTable().allActiveShards(concreteIndices, true);
    }

    @Override
    protected ClusterBlockException checkGlobalBlock(ClusterState state, IndicesSegmentsRequest request) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_READ);
    }

    @Override
    protected ClusterBlockException checkRequestBlock(ClusterState state, IndicesSegmentsRequest countRequest, String[] concreteIndices) {
        return state.blocks().indicesBlockedException(ClusterBlockLevel.METADATA_READ, concreteIndices);
    }

    @Override
    protected IndicesSegmentResponse newResponse(IndicesSegmentsRequest request, int totalShards, int successfulShards, int failedShards, List<IndexShardSegmentResponse> indexShardSegmentResponses, List<ShardOperationFailedException> shardFailures) {
        List<ShardSegments> concatenation = Lists.newArrayList();
        for (IndexShardSegmentResponse response : indexShardSegmentResponses) {
            concatenation.addAll(response.getShardSegments());
        }
        return new IndicesSegmentResponse(concatenation.toArray(new ShardSegments[concatenation.size()]), totalShards, successfulShards, failedShards, shardFailures);
    }

    @Override
    protected IndexShardSegmentRequest newNodeRequest(String nodeId, IndicesSegmentsRequest request, List<ShardRouting> shards) {
        return new IndexShardSegmentRequest(nodeId, shards, request);
    }

    @Override
    protected IndexShardSegmentResponse newNodeResponse() {
        return new IndexShardSegmentResponse();
    }

    @Override
    protected IndexShardSegmentResponse newNodeResponse(String nodeId, int totalShards, int successfulShards, List<ShardSegments> results, List<BroadcastShardOperationFailedException> exceptions) {
        return new IndexShardSegmentResponse(nodeId, totalShards, successfulShards, results, exceptions);
    }

    @Override
    protected ShardSegments shardOperation(IndexShardSegmentRequest request, ShardRouting shardRouting) {
        IndexService indexService = indicesService.indexServiceSafe(shardRouting.getIndex());
        IndexShard indexShard = indexService.shardSafe(shardRouting.id());
        return new ShardSegments(indexShard.routingEntry(), indexShard.engine().segments(request.verbose));
    }

    static class IndexShardSegmentRequest extends BaseBroadcastByNodeRequest<IndicesSegmentsRequest> {
        boolean verbose;
        
        IndexShardSegmentRequest() {
            verbose = false;
        }

        IndexShardSegmentRequest(String nodeId, List<ShardRouting> shards, IndicesSegmentsRequest request) {
            super(nodeId, request, shards);
            verbose = request.verbose();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeBoolean(verbose);
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            verbose = in.readBoolean();
        }

        @Override
        protected IndicesSegmentsRequest newRequest() {
            return new IndicesSegmentsRequest();
        }
    }

    static class IndexShardSegmentResponse extends BaseBroadcastByNodeResponse {
        private List<ShardSegments> shardSegments;

        public IndexShardSegmentResponse() {
        }

        public IndexShardSegmentResponse(String nodeId, int totalShards, int successfulShards, List<ShardSegments> shardSegments, List<BroadcastShardOperationFailedException> exceptions) {
            super(nodeId, totalShards, successfulShards, exceptions);
            this.shardSegments = shardSegments;
        }

        public List<ShardSegments> getShardSegments() {
            return shardSegments;
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeVInt(shardSegments.size());
            for (int i = 0; i < shardSegments.size(); i++) {
                shardSegments.get(i).writeTo(out);
            }
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            int size = in.readVInt();
            shardSegments = Lists.newArrayListWithCapacity(size);
            for (int i = 0; i < size; i++) {
                shardSegments.add(ShardSegments.readShardSegments(in));
            }
        }
    }
}
