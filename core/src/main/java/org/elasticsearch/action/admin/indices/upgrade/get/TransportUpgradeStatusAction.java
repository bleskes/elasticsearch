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

package org.elasticsearch.action.admin.indices.upgrade.get;

import com.google.common.collect.Lists;
import org.elasticsearch.Version;
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
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.IndexService;
import org.elasticsearch.index.engine.Segment;
import org.elasticsearch.index.shard.IndexShard;
import org.elasticsearch.indices.IndicesService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class TransportUpgradeStatusAction extends TransportBroadcastByNodeAction<UpgradeStatusRequest, UpgradeStatusResponse, TransportUpgradeStatusAction.NodeUpgradeStatusRequest, TransportUpgradeStatusAction.NodeUpgradeStatusResponse, ShardUpgradeStatus> {

    private final IndicesService indicesService;

    @Inject
    public TransportUpgradeStatusAction(Settings settings, ThreadPool threadPool, ClusterService clusterService, TransportService transportService,
                                        IndicesService indicesService, ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver) {
        super(settings, UpgradeStatusAction.NAME, threadPool, clusterService, transportService, actionFilters, indexNameExpressionResolver,
                UpgradeStatusRequest.class, NodeUpgradeStatusRequest.class, ThreadPool.Names.MANAGEMENT);
        this.indicesService = indicesService;
    }

    /**
     * Getting upgrade stats from *all* active shards.
     */
    @Override
    protected GroupShardsIterator shards(ClusterState clusterState, UpgradeStatusRequest request, String[] concreteIndices) {
        return clusterState.routingTable().allActiveShardsGrouped(concreteIndices, true);
    }

    @Override
    protected ClusterBlockException checkGlobalBlock(ClusterState state, UpgradeStatusRequest request) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_READ);
    }

    @Override
    protected ClusterBlockException checkRequestBlock(ClusterState state, UpgradeStatusRequest countRequest, String[] concreteIndices) {
        return state.blocks().indicesBlockedException(ClusterBlockLevel.METADATA_READ, concreteIndices);
    }

    @Override
    protected UpgradeStatusResponse newResponse(UpgradeStatusRequest request, int totalShards, int successfulShards, int failedShards, List<NodeUpgradeStatusResponse> responses, List<DefaultShardOperationFailedException> shardFailures) {
        List<ShardUpgradeStatus> concatenation = Lists.newArrayList();
        for (NodeUpgradeStatusResponse response : responses) {
            concatenation.addAll(response.getShards());
        }
        return new UpgradeStatusResponse(concatenation.toArray(new ShardUpgradeStatus[concatenation.size()]), totalShards, successfulShards, failedShards, shardFailures);
    }

    @Override
    protected NodeUpgradeStatusRequest newNodeRequest(String nodeId, UpgradeStatusRequest request, List<ShardRouting> shards) {
        return new NodeUpgradeStatusRequest(request, shards, nodeId);
    }

    @Override
    protected NodeUpgradeStatusResponse newNodeResponse() {
        return new NodeUpgradeStatusResponse();
    }

    @Override
    protected NodeUpgradeStatusResponse newNodeResponse(String nodeId, int totalShards, int successfulShards, List<ShardUpgradeStatus> shards, List<BroadcastShardOperationFailedException> exceptions) {
        return new NodeUpgradeStatusResponse(nodeId, totalShards, successfulShards, shards, exceptions);
    }

    @Override
    protected ShardUpgradeStatus shardOperation(NodeUpgradeStatusRequest request, ShardRouting shardRouting) {
        IndexService indexService = indicesService.indexServiceSafe(shardRouting.shardId().getIndex());
        IndexShard indexShard = indexService.shardSafe(shardRouting.shardId().id());
        List<Segment> segments = indexShard.engine().segments(false);
        long total_bytes = 0;
        long to_upgrade_bytes = 0;
        long to_upgrade_bytes_ancient = 0;
        for (Segment seg : segments) {
            total_bytes += seg.sizeInBytes;
            if (seg.version.major != Version.CURRENT.luceneVersion.major) {
                to_upgrade_bytes_ancient += seg.sizeInBytes;
                to_upgrade_bytes += seg.sizeInBytes;
            } else if (seg.version.minor != Version.CURRENT.luceneVersion.minor) {
                // TODO: this comparison is bogus! it would cause us to upgrade even with the same format
                // instead, we should check if the codec has changed
                to_upgrade_bytes += seg.sizeInBytes;
            }
        }

        return new ShardUpgradeStatus(indexShard.routingEntry(), total_bytes, to_upgrade_bytes, to_upgrade_bytes_ancient);
    }

    static class NodeUpgradeStatusRequest extends BaseBroadcastByNodeRequest<UpgradeStatusRequest> {
        NodeUpgradeStatusRequest() {
        }

        NodeUpgradeStatusRequest(UpgradeStatusRequest request, List<ShardRouting> shards, String nodeId) {
            super(nodeId, request, shards);
        }

        @Override
        protected UpgradeStatusRequest newRequest() {
            return new UpgradeStatusRequest();
        }
    }

    public class NodeUpgradeStatusResponse extends BaseBroadcastByNodeResponse {
        private List<ShardUpgradeStatus> shards;

        public NodeUpgradeStatusResponse() {
        }

        public NodeUpgradeStatusResponse(String nodeId, int totalShards, int successfulShards, List<ShardUpgradeStatus> shards, List<BroadcastShardOperationFailedException> exceptions) {
            super(nodeId, totalShards, successfulShards, exceptions);
            this.shards = shards;
        }

        public List<ShardUpgradeStatus> getShards() {
            return shards;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            if (in.readBoolean()) {
                int size = in.readVInt();
                shards = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    shards.add(ShardUpgradeStatus.readShardUpgradeStatus(in));
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
