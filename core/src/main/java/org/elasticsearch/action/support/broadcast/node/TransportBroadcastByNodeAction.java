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

package org.elasticsearch.action.support.broadcast.node;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.elasticsearch.action.*;
import org.elasticsearch.action.support.*;
import org.elasticsearch.action.support.broadcast.BroadcastRequest;
import org.elasticsearch.action.support.broadcast.BroadcastResponse;
import org.elasticsearch.action.support.broadcast.BroadcastShardOperationFailedException;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.cluster.routing.ShardsIterator;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Streamable;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Abstraction for transporting aggregated shard-level operations in a single request (NodeRequest) per-node
 * and executing the shard-level operations serially on the receiving node. Each shard-level operation can produce a
 * result (ShardOperationResult), these per-node shard-level results are aggregated into a single result
 * (BroadcastByNodeResponse) to the coordinating node. These per-node results are aggregated into a single result (Result)
 * to the client.
 *
 * @param <Request>              the underlying client request
 * @param <Response>             the response to the client request
 * @param <ShardOperationResult> per-shard operation results
 */
public abstract class TransportBroadcastByNodeAction<Request extends BroadcastRequest,
        Response extends BroadcastResponse,
        ShardOperationResult extends Streamable> extends HandledTransportAction<Request, Response> {

    private final ClusterService clusterService;
    private final TransportService transportService;

    final String transportNodeBroadcastAction;

    public TransportBroadcastByNodeAction(
            Settings settings,
            String actionName,
            ThreadPool threadPool,
            ClusterService clusterService,
            TransportService transportService,
            ActionFilters actionFilters,
            IndexNameExpressionResolver indexNameExpressionResolver,
            Class<Request> request,
            String executor) {
        super(settings, actionName, threadPool, transportService, actionFilters, indexNameExpressionResolver, request);

        this.clusterService = clusterService;
        this.transportService = transportService;

        transportNodeBroadcastAction = actionName + "[n]";

        transportService.registerRequestHandler(transportNodeBroadcastAction, new Callable<NodeRequest>() {
            @Override
            public NodeRequest call() throws Exception {
                return new NodeRequest();
            }
        }, executor, new BroadcastByNodeTransportRequestHandler());
    }

    private final Response newResponse(Request request, AtomicReferenceArray responses, List<NoShardAvailableActionException> unavailableShardExceptions, Map<String, List<ShardRouting>> nodes) {
        int totalShards = 0;
        int successfulShards = 0;
        List<ShardOperationResult> broadcastByNodeResponses = Lists.newArrayList();
        List<ShardOperationFailedException> exceptions = Lists.newArrayList();
        for (int i = 0; i < responses.length(); i++) {
            if (responses.get(i) instanceof FailedNodeException) {
                FailedNodeException exception = (FailedNodeException) responses.get(i);
                totalShards += nodes.get(exception.nodeId()).size();
                for (ShardRouting shard : nodes.get(exception.nodeId())) {
                    exceptions.add(new DefaultShardOperationFailedException(shard.getIndex(), shard.getId(), exception));
                }
            } else {
                NodeResponse response = (NodeResponse) responses.get(i);
                broadcastByNodeResponses.addAll(response.results);
                totalShards += response.getTotalShards();
                successfulShards += response.getSuccessfulShards();
                for (BroadcastShardOperationFailedException throwable : response.getExceptions()) {
                    if (!TransportActions.isShardNotAvailableException(throwable)) {
                        exceptions.add(new DefaultShardOperationFailedException(throwable.getIndex(), throwable.getShardId().getId(), throwable));
                    }
                }
            }
        }
        totalShards += unavailableShardExceptions.size();
        int failedShards = exceptions.size();
        return newResponse(request, totalShards, successfulShards, failedShards, broadcastByNodeResponses, exceptions);
    }

    protected abstract ShardOperationResult readShardResult(StreamInput in) throws IOException;


    /**
     * Creates a new response to the underlying request.
     *
     * @param request          the underlying request
     * @param totalShards      the total number of shards considered for execution of the operation
     * @param successfulShards the total number of shards for which execution of the operation was successful
     * @param failedShards     the total number of shards for which execution of the operation failed
     * @param results          the per-node aggregated shard-level results
     * @param shardFailures    the exceptions corresponding to shard operationa failures
     * @return the response
     */
    protected abstract Response newResponse(Request request, int totalShards, int successfulShards, int failedShards, List<ShardOperationResult> results, List<ShardOperationFailedException> shardFailures);


    protected abstract Request readRequestFrom(StreamInput in) throws IOException;

    /**
     * Executes the shard-level operation. This method is called once per shard serially on the receiving node.
     *
     * @param request      the node-level request
     * @param shardRouting the shard on which to execute the operation
     * @return the result of the shard-level operation for the shard
     */
    protected abstract ShardOperationResult shardOperation(Request request, ShardRouting shardRouting);

    /**
     * Determines the shards on which this operation will be executed on. The operation is executed once per shard
     * iterator and unlike TransportBroadcastAction does not fall back to the next shard in the iterator on failure.
     *
     * @param clusterState    the cluster state
     * @param request         the underlying request
     * @param concreteIndices the concrete indices on which to execute the operation
     * @return the shards on which to execute the operation
     */
    protected abstract ShardsIterator shards(ClusterState clusterState, Request request, String[] concreteIndices);

    /**
     * Executes a global block check before polling the cluster state.
     *
     * @param state   the cluster state
     * @param request the underlying request
     * @return a non-null exception if the operation is blocked
     */
    protected abstract ClusterBlockException checkGlobalBlock(ClusterState state, Request request);

    /**
     * Executes a global request-level check before polling the cluster state.
     *
     * @param state           the cluster state
     * @param request         the underlying request
     * @param concreteIndices the concrete indices on which to execute the operation
     * @return a non-null exception if the operation if blocked
     */
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
            ShardsIterator shardIt = shards(clusterState, request, concreteIndices);
            nodeIds = Maps.newHashMap();

            for (ShardRouting shard : shardIt.asUnordered()) {
                if (shard.assignedToNode()) {
                    String nodeId = shard.currentNodeId();
                    if (!nodeIds.containsKey(nodeId)) {
                        nodeIds.put(nodeId, Lists.<ShardRouting>newArrayList());
                    }
                    nodeIds.get(nodeId).add(shard);
                } else {
                    unavailableShardExceptions.add(
                            new NoShardAvailableActionException(
                                    shard.shardId(),
                                    "[" + shard.shardId().getIndex() + "][" + shard.shardId().getId() + "] no shards available while executing " + actionName
                            )
                    );
                }
            }

            responses = new AtomicReferenceArray<>(nodeIds.size());
        }

        public void start() {
            if (nodeIds.size() == 0) {
                try {
                    listener.onResponse(newResponse(request, new AtomicReferenceArray(0), unavailableShardExceptions, nodeIds));
                } catch (Throwable e) {
                    listener.onFailure(e);
                }
            } else {
                int nodeIndex = -1;
                for (Map.Entry<String, List<ShardRouting>> entry : nodeIds.entrySet()) {
                    nodeIndex++;
                    DiscoveryNode node = nodes.get(entry.getKey());
                    sendNodeRequest(node, entry.getValue(), nodeIndex);
                }
            }
        }

        private void sendNodeRequest(final DiscoveryNode node, List<ShardRouting> shards, final int nodeIndex) {
            try {
                NodeRequest nodeRequest = new NodeRequest(node.getId(), request, shards);
                transportService.sendRequest(node, transportNodeBroadcastAction, nodeRequest, new BaseTransportResponseHandler<NodeResponse>() {
                    @Override
                    public NodeResponse newInstance() {
                        return new NodeResponse();
                    }

                    @Override
                    public void handleResponse(NodeResponse response) {
                        onNodeResponse(node, nodeIndex, response);
                    }

                    @Override
                    public void handleException(TransportException exp) {
                        onNodeFailure(node, nodeIndex, exp);
                    }

                    @Override
                    public String executor() {
                        return ThreadPool.Names.SAME;
                    }
                });
            } catch (Throwable e) {
                onNodeFailure(node, nodeIndex, e);
            }
        }

        protected void onNodeResponse(DiscoveryNode node, int nodeIndex, NodeResponse response) {
            logger.trace("received response from node [{}]", node.id());
            responses.set(nodeIndex, response);
            if (counter.incrementAndGet() == responses.length()) {
                onCompletion();
            }
        }

        protected void onNodeFailure(DiscoveryNode node, int nodeIndex, Throwable t) {
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
            Response response = null;
            try {
                response = newResponse(request, responses, unavailableShardExceptions, nodeIds);
            } catch (Throwable t) {
                logger.debug("failed to combine responses from nodes", t);
                listener.onFailure(t);
            }
            if (response != null) {
                try {
                    listener.onResponse(response);
                } catch (Throwable t) {
                    listener.onFailure(t);
                }
            }
        }
    }

    class BroadcastByNodeTransportRequestHandler implements TransportRequestHandler<NodeRequest> {


        @Override
        public void messageReceived(final NodeRequest request, TransportChannel channel) throws Exception {
            List<ShardRouting> shards = request.getShards();
            final int totalShards = shards.size();
            logger.trace("[{}] executing operation on [{}] shards", actionName, totalShards);
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

            channel.sendResponse(new NodeResponse(request.getNodeId(), totalShards, results, accumulatedExceptions));
        }

        private void onShardOperation(final NodeRequest request, final Object[] shardResults, final int shardIndex, final ShardRouting shardRouting) {
            try {
                logger.trace("[{}]  executing operation for shard [{}]", actionName, shardRouting.shortSummary());
                ShardOperationResult result = shardOperation(request.indicesLevelRequest, shardRouting);
                shardResults[shardIndex] = result;
                logger.trace("[{}]  completed operation for shard [{}]", actionName, shardRouting.shortSummary());
            } catch (Throwable t) {
                BroadcastShardOperationFailedException e = new BroadcastShardOperationFailedException(shardRouting.shardId(), "operation " + actionName + " failed", t);
                e.setIndex(shardRouting.getIndex());
                e.setShard(shardRouting.shardId());
                shardResults[shardIndex] = e;
                logger.debug("[{}] failed to execute operation for shard [{}]", e, actionName, shardRouting.shortSummary());
            }
        }
    }

    class NodeRequest extends TransportRequest implements IndicesRequest {
        private String nodeId;

        private List<ShardRouting> shards;

        protected Request indicesLevelRequest;

        protected NodeRequest() {
        }

        public NodeRequest(String nodeId, Request request, List<ShardRouting> shards) {
            super(request);
            this.indicesLevelRequest = request;
            this.shards = shards;
            this.nodeId = nodeId;
        }

        public List<ShardRouting> getShards() {
            return shards;
        }

        public String getNodeId() {
            return nodeId;
        }

        public String[] indices() {
            return indicesLevelRequest.indices();
        }

        public IndicesOptions indicesOptions() {
            return indicesLevelRequest.indicesOptions();
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            indicesLevelRequest = readRequestFrom(in);
            int size = in.readVInt();
            shards = Lists.newArrayListWithCapacity(size);
            for (int i = 0; i < size; i++) {
                shards.add(ShardRouting.readShardRoutingEntry(in));
            }
            nodeId = in.readString();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            indicesLevelRequest.writeTo(out);
            int size = shards.size();
            out.writeVInt(size);
            for (int i = 0; i < size; i++) {
                shards.get(i).writeTo(out);
            }
            out.writeString(nodeId);
        }

        public Request getIndicesLevelRequest() {
            return indicesLevelRequest;
        }
    }

    class NodeResponse extends TransportResponse {
        protected String nodeId;
        protected int totalShards;
        protected List<BroadcastShardOperationFailedException> exceptions;
        protected List<ShardOperationResult> results;

        public NodeResponse() {
        }

        public NodeResponse(String nodeId,
                            int totalShards,
                            List<ShardOperationResult> results,
                            List<BroadcastShardOperationFailedException> exceptions) {
            this.nodeId = nodeId;
            this.totalShards = totalShards;
            this.results = results;
            this.exceptions = exceptions;
        }

        public String getNodeId() {
            return nodeId;
        }

        public int getTotalShards() {
            return totalShards;
        }

        public int getSuccessfulShards() {
            return results.size();
        }

        public List<BroadcastShardOperationFailedException> getExceptions() {
            return exceptions;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            nodeId = in.readString();
            totalShards = in.readVInt();
            int resultsSize = in.readVInt();
            results = new ArrayList<>(resultsSize);
            for (; resultsSize > 0; resultsSize--) {
                final ShardOperationResult result = in.readBoolean() ? readShardResult(in) : null;
                results.add(result);
            }
            if (in.readBoolean()) {
                int failureShards = in.readVInt();
                exceptions = new ArrayList<>(failureShards);
                for (int i = 0; i < failureShards; i++) {
                    exceptions.add(new BroadcastShardOperationFailedException(in));
                }
            } else {
                exceptions = null;
            }
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeString(nodeId);
            out.writeVInt(totalShards);
            out.writeVInt(results.size());
            for (ShardOperationResult result : results) {
                out.writeOptionalStreamable(result);
            }
            out.writeBoolean(exceptions != null);
            if (exceptions != null) {
                int failureShards = exceptions.size();
                out.writeVInt(failureShards);
                for (int i = 0; i < failureShards; i++) {
                    exceptions.get(i).writeTo(out);
                }
            }
        }
    }


    public final static class EmptyResult implements Streamable {

        @Override
        public void readFrom(StreamInput in) throws IOException {

        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {

        }

        public static EmptyResult readEmptyResultFrom(StreamInput in) {
            return new EmptyResult();
        }
    }


}
