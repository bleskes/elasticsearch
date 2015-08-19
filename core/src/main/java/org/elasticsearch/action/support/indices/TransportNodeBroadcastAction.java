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

package org.elasticsearch.action.support.indices;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.FailedNodeException;
import org.elasticsearch.action.NoShardAvailableActionException;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.DefaultShardOperationFailedException;
import org.elasticsearch.action.support.HandledTransportAction;
import org.elasticsearch.action.support.TransportActions;
import org.elasticsearch.action.support.broadcast.BroadcastShardOperationFailedException;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.routing.GroupShardsIterator;
import org.elasticsearch.cluster.routing.ShardIterator;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

public abstract class TransportNodeBroadcastAction<Request extends IndicesLevelRequest,
        Response extends IndicesLevelResponse,
        NodeBroadcastRequest extends BaseNodeBroadcastRequest<Request>,
        NodeBroadcastResponse extends BaseNodeBroadcastResponse,
        ShardOperationResult> extends HandledTransportAction<Request, Response> {

    private final ClusterService clusterService;
    private final TransportService transportService;

    final String transportNodeBroadcastAction;

    public TransportNodeBroadcastAction(
            Settings settings,
            String actionName,
            ThreadPool threadPool,
            ClusterService clusterService,
            TransportService transportService,
            ActionFilters actionFilters,
            IndexNameExpressionResolver indexNameExpressionResolver,
            Class<Request> request,
            Class<NodeBroadcastRequest> nodeBroadcastRequest,
            String executor) {
        super(settings, actionName, threadPool, transportService, actionFilters, indexNameExpressionResolver, request);

        this.clusterService = clusterService;
        this.transportService = transportService;

        transportNodeBroadcastAction = actionName + "[i]";

        transportService.registerRequestHandler(transportNodeBroadcastAction, nodeBroadcastRequest, executor, new NodeBroadcastTransportHandler());
    }

    private final Response newResponse(Request request, AtomicReferenceArray responses, List<NoShardAvailableActionException> unavailableShardExceptions) {
        int totalShards = 0;
        int successfulShards = 0;
        List<NodeBroadcastResponse> nodeBroadcastResponses = Lists.newArrayList();
        List<DefaultShardOperationFailedException> exceptions = Lists.newArrayList();
        for (int i = 0; i < responses.length(); i++) {
            NodeBroadcastResponse response = (NodeBroadcastResponse) responses.get(i);
            nodeBroadcastResponses.add(response);
            totalShards += response.getTotalShards();
            successfulShards += response.getSuccessfulShards();
            for (BroadcastShardOperationFailedException t : response.getExceptions()) {
                if (!TransportActions.isShardNotAvailableException(t)) {
                    exceptions.add(new DefaultShardOperationFailedException(t.getIndex(), t.getShardId().getId(), t));
                }
            }
        }
        totalShards += unavailableShardExceptions.size();
        int failedShards = exceptions.size();
        return newResponse(request, totalShards, successfulShards, failedShards, nodeBroadcastResponses, exceptions);
    }

    protected abstract Response newResponse(Request request, int totalShards, int successfulShards, int failedShards, List<NodeBroadcastResponse> responses, List<DefaultShardOperationFailedException> shardFailures);

    protected abstract NodeBroadcastRequest newNodeRequest(String nodeId, Request request, List<ShardRouting> shards);

    protected abstract NodeBroadcastResponse newNodeResponse();

    protected abstract NodeBroadcastResponse newNodeResponse(String nodeId, int totalShards, int successfulShards, List<ShardOperationResult> results, List<BroadcastShardOperationFailedException> exceptions);

    protected abstract ShardOperationResult shardOperation(NodeBroadcastRequest request, ShardRouting shardRouting);

    protected abstract GroupShardsIterator shards(ClusterState clusterState, Request request, String[] concreteIndices);

    protected abstract ClusterBlockException checkGlobalBlock(ClusterState state, Request request);

    protected abstract ClusterBlockException checkRequestBlock(ClusterState state, Request request, String[] concreteIndices);

    @Override
    protected void doExecute(Request request, ActionListener<Response> listener) {
        new AsyncAction(request, listener).start();
    }

    protected class AsyncAction {
        private final Request request;
        private final ActionListener<Response> listener;
        private final ClusterState clusterState;
        private final DiscoveryNodes nodes;
        private final Map<String, List<ShardRouting>> nodeIds;
        private final AtomicReferenceArray<Object> responses;
        private final AtomicInteger counter = new AtomicInteger();
        private List<NoShardAvailableActionException> unavailableShardExceptions = Lists.newArrayList();

        protected AsyncAction(Request request, ActionListener<Response> listener) {
            this.request = request;
            this.listener = listener;

            clusterState = clusterService.state();
            nodes = clusterState.nodes();

            ClusterBlockException globalBlockException = checkGlobalBlock(clusterState, request);
            if (globalBlockException != null) {
                throw globalBlockException;
            }

            String[] concreteIndices = indexNameExpressionResolver.concreteIndices(clusterState, request);
            ClusterBlockException requestBlockException = checkRequestBlock(clusterState, request, concreteIndices);
            if (requestBlockException != null) {
                throw requestBlockException;
            }

            logger.trace("resolving shards based on cluster state version [{}]", clusterState.version());
            GroupShardsIterator shardIts = shards(clusterState, request, concreteIndices);
            nodeIds = Maps.newHashMap();

            for (ShardIterator shardIt : shardIts) {
                ShardRouting shard = shardIt.nextOrNull();
                if (shard != null) {
                    String nodeId = shard.currentNodeId();
                    if (!nodeIds.containsKey(nodeId)) {
                        nodeIds.put(nodeId, Lists.<ShardRouting>newArrayList());
                    }
                    nodeIds.get(nodeId).add(shard);
                } else {
                    unavailableShardExceptions.add(
                            new NoShardAvailableActionException(
                                    shardIt.shardId(),
                                    "[" + shardIt.shardId().getIndex() + "][" + shardIt.shardId().getId() + "] no shards available while executing " + actionName
                            )
                    );
                }
            }

            responses = new AtomicReferenceArray<>(nodeIds.size());
        }

        public void start() {
            if (nodeIds.size() == 0) {
                try {
                    listener.onResponse(newResponse(request, new AtomicReferenceArray(0), unavailableShardExceptions));
                } catch (Throwable e) {
                    listener.onFailure(e);
                }
            } else {
                int nodeIndex = -1;
                for (Map.Entry<String, List<ShardRouting>> entry : nodeIds.entrySet()) {
                    nodeIndex++;
                    DiscoveryNode node = nodes.get(entry.getKey());
                    performOperation(node, entry.getValue(), nodeIndex);
                }
            }
        }

        private void performOperation(final DiscoveryNode node, List<ShardRouting> shards, final int nodeIndex) {
            try {
                NodeBroadcastRequest nodeRequest = newNodeRequest(node.getId(), request, shards);
                transportService.sendRequest(node, transportNodeBroadcastAction, nodeRequest, new BaseTransportResponseHandler<NodeBroadcastResponse>() {
                    @Override
                    public NodeBroadcastResponse newInstance() {
                        return newNodeResponse();
                    }

                    @Override
                    public void handleResponse(NodeBroadcastResponse response) {
                        onOperation(node, nodeIndex, response);
                    }

                    @Override
                    public void handleException(TransportException exp) {
                        onFailure(node, nodeIndex, exp);
                    }

                    @Override
                    public String executor() {
                        return ThreadPool.Names.SAME;
                    }
                });
            } catch (Throwable e) {
                onFailure(node, nodeIndex, e);
            }
        }

        protected void onOperation(DiscoveryNode node, int nodeIndex, NodeBroadcastResponse response) {
            logger.trace("received response from node [{}]", node.id());
            responses.set(nodeIndex, response);
            if (counter.incrementAndGet() == responses.length()) {
                onCompletion();
            }
        }

        protected void onFailure(DiscoveryNode node, int nodeIndex, Throwable t) {
            String nodeId = node.id();
            if (logger.isDebugEnabled() && !(t instanceof NodeShouldNotConnectException)) {
                logger.debug("failed to execute on node [{}]: [{}]", nodeId, t);
            }

            responses.set(nodeIndex, new FailedNodeException(nodeId, "Failed node [" + nodeId + "]", t));

            if (counter.incrementAndGet() == responses.length()) {
                onCompletion();
            }
        }

        protected void onCompletion() {
            try {
                Response response = newResponse(request, responses, unavailableShardExceptions);
                listener.onResponse(response);
            } catch (Throwable t) {
                logger.debug("failed to combine responses from nodes", t);
                listener.onFailure(t);
            }
        }
    }

    class NodeBroadcastTransportHandler implements TransportRequestHandler<NodeBroadcastRequest> {
        @Override
        public void messageReceived(final NodeBroadcastRequest request, TransportChannel channel) throws Exception {
            List<ShardRouting> shards = request.getShards();
            final int totalShards = shards.size();
            logger.trace("executing operation [{}] on [{}] shards on node [{}]", actionName, totalShards, request.getNodeId());
            final Object[] shardResults = new Object[totalShards];

            int shardIndex = -1;
            for (final ShardRouting shardRouting : shards) {
                shardIndex++;
                onShardOperation(request, shardResults, shardIndex, shardRouting);
            }

            List<BroadcastShardOperationFailedException> accumulatedExceptions = Lists.newArrayList();
            List<ShardOperationResult> results = Lists.newArrayList();
            for (int i = 0; i < totalShards; i++) {
                if (shardResults[i] instanceof BroadcastShardOperationFailedException) {
                    accumulatedExceptions.add((BroadcastShardOperationFailedException) shardResults[i]);
                } else {
                    results.add((ShardOperationResult) shardResults[i]);
                }
            }

            channel.sendResponse(newNodeResponse(request.getNodeId(), totalShards, totalShards - accumulatedExceptions.size(), results, accumulatedExceptions));
        }

        private void onShardOperation(final NodeBroadcastRequest request, final Object[] shardResults, final int shardIndex, final ShardRouting shardRouting) {
            try {
                logger.trace("executing operation [{}] for shard [{}] on node [{}]", actionName, shardRouting.shortSummary(), request.getNodeId());
                ShardOperationResult result = shardOperation(request, shardRouting);
                shardResults[shardIndex] = result;
                logger.trace("completed operation [{}] for shard [{}] on node [{}]", actionName, shardRouting.shortSummary(), request.getNodeId());
            } catch (Throwable t) {
                BroadcastShardOperationFailedException e = new BroadcastShardOperationFailedException(shardRouting.shardId(), "operation " + actionName + " failed", t);
                e.setIndex(shardRouting.getIndex());
                e.setShard(shardRouting.shardId());
                shardResults[shardIndex] = e;
                logger.trace("failed to execute operation [{}] for shard [{}] on node [{}]: [{}]", actionName, shardRouting.shortSummary(), request.getNodeId(), e);
            }
        }
    }
}
