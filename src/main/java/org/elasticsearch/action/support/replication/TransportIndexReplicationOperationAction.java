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

package org.elasticsearch.action.support.replication;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.action.ActionWriteResponse;
import org.elasticsearch.action.ActionWriteResponse.ShardInfo.Failure;
import org.elasticsearch.action.ShardOperationFailedException;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.DefaultShardOperationFailedException;
import org.elasticsearch.action.support.TransportAction;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.routing.GroupShardsIterator;
import org.elasticsearch.cluster.routing.ShardIterator;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Internal transport action that executes on multiple shards, doesn't register any transport handler as it is always executed locally.
 * It relies on a shard sub-action that gets sent over the transport and executed on each of the shard.
 * The index provided with the request is expected to be a concrete index, properly resolved by the callers (parent actions).
 */
public abstract class TransportIndexReplicationOperationAction<Request extends IndexReplicationOperationRequest, Response extends ActionResponse, ShardRequest extends ShardReplicationOperationRequest, ShardResponse extends ActionWriteResponse>
        extends TransportAction<Request, Response> {

    protected final ClusterService clusterService;

    protected final TransportShardReplicationOperationAction<ShardRequest, ShardRequest, ShardResponse> shardAction;

    protected TransportIndexReplicationOperationAction(Settings settings, String actionName, ClusterService clusterService,
                                                       ThreadPool threadPool, TransportShardReplicationOperationAction<ShardRequest, ShardRequest, ShardResponse> shardAction, ActionFilters actionFilters) {
        super(settings, actionName, threadPool, actionFilters);
        this.clusterService = clusterService;
        this.shardAction = shardAction;
    }

    @Override
    protected void doExecute(final Request request, final ActionListener<Response> listener) {
        ClusterState clusterState = clusterService.state();
        ClusterBlockException blockException = checkGlobalBlock(clusterState, request);
        if (blockException != null) {
            throw blockException;
        }
        blockException = checkRequestBlock(clusterState, request);
        if (blockException != null) {
            throw blockException;
        }

        final GroupShardsIterator groups;
        try {
            groups = shards(request);
        } catch (Throwable e) {
            listener.onFailure(e);
            return;
        }
        final AtomicInteger indexCounter = new AtomicInteger();
        final AtomicInteger failureCounter = new AtomicInteger();
        final AtomicInteger completionCounter = new AtomicInteger(groups.size());
        final AtomicReferenceArray<ShardActionResult> shardsResponses = new AtomicReferenceArray<>(groups.size());

        for (final ShardIterator shardIt : groups) {
            ShardRequest shardRequest = newShardRequestInstance(request, shardIt.shardId().id());

            // TODO for now, we fork operations on shardIt of the index
            shardRequest.beforeLocalFork(); // optimize for local fork
            shardRequest.operationThreaded(true);

            // no need for threaded listener, we will fork when its done based on the index request
            shardRequest.listenerThreaded(false);
            shardAction.execute(shardRequest, new ActionListener<ShardResponse>() {
                @Override
                public void onResponse(ShardResponse result) {
                    shardsResponses.set(indexCounter.getAndIncrement(), new ShardActionResult(result));
                    returnIfNeeded();
                }

                @Override
                public void onFailure(Throwable e) {
                    failureCounter.getAndIncrement();
                    int index = indexCounter.getAndIncrement();
                    if (accumulateExceptions()) {
                        shardsResponses.set(index, new ShardActionResult(
                                new DefaultShardOperationFailedException(request.index(), shardIt.shardId().id(), e), shardIt));
                    }
                    returnIfNeeded();
                }

                private void returnIfNeeded() {
                    if (completionCounter.decrementAndGet() == 0) {
                        List<ShardResponse> responses = new ArrayList<>();
                        List<Failure> failureList = new ArrayList<>();

                        int total = groups.totalSize();
                        int pending = 0;
                        int successful = 0;
                        for (int i = 0; i < shardsResponses.length(); i++) {
                            ShardActionResult shardActionResult = shardsResponses.get(i);
                            if (shardActionResult == null) {
                                assert !accumulateExceptions();
                                continue;
                            }
                            if (shardActionResult.isFailure()) {
                                assert accumulateExceptions() && shardActionResult.shardFailure != null;
                                // Set the status here, since it is a failure on primary shard
                                // The failure doesn't include the node id, maybe add it to ShardOperationFailedException...
                                ShardOperationFailedException sf = shardActionResult.shardFailure;

                                ShardIterator shardIterator = shardActionResult.shardIterator;
                                for (ShardRouting shardRouting = shardIterator.nextOrNull(); shardRouting != null; shardRouting = shardIterator.nextOrNull()) {
                                    if (shardRouting.primary()) {
                                        failureList.add(new Failure(sf.index(), sf.shardId(), shardRouting.currentNodeId(), sf.reason(), sf.status(), true));
                                    } else {
                                        failureList.add(new Failure(sf.index(), sf.shardId(), shardRouting.currentNodeId(), "Failed to execute on replica shard: " + sf.reason(), sf.status(), false));
                                    }
                                }
                            } else {
                                pending += shardActionResult.shardResponse.getShardInfo().getPending();
                                successful += shardActionResult.shardResponse.getShardInfo().getSuccessful();
                                failureList.addAll(Arrays.asList(shardActionResult.shardResponse.getShardInfo().getFailures()));
                                responses.add(shardActionResult.shardResponse);
                            }
                        }
                        assert failureList.size() == 0 || numShardGroupFailures(failureList) == failureCounter.get();

                        final Failure[] failures;
                        if (failureList.isEmpty()) {
                            failures = ActionWriteResponse.EMPTY;
                        } else {
                            failures = failureList.toArray(new Failure[failureList.size()]);
                        }
                        listener.onResponse(newResponseInstance(request, responses, new ActionWriteResponse.ShardInfo(total, successful, pending, failures)));
                    }
                }

                private int numShardGroupFailures(List<Failure> failures) {
                    int numShardGroupFailures = 0;
                    for (Failure failure : failures) {
                        if (failure.primary()) {
                            numShardGroupFailures++;
                        }
                    }
                    return numShardGroupFailures;
                }
            });

        }
    }

    protected abstract Response newResponseInstance(Request request, List<ShardResponse> shardResponses, ActionWriteResponse.ShardInfo shardInfo);

    protected abstract GroupShardsIterator shards(Request request) throws ElasticsearchException;

    protected abstract ShardRequest newShardRequestInstance(Request request, int shardId);

    protected abstract boolean accumulateExceptions();

    protected ClusterBlockException checkGlobalBlock(ClusterState state, Request request) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.WRITE);
    }

    protected ClusterBlockException checkRequestBlock(ClusterState state, Request request) {
        return state.blocks().indexBlockedException(ClusterBlockLevel.WRITE, request.index());
    }

    private class ShardActionResult {

        private final ShardResponse shardResponse;
        private final ShardOperationFailedException shardFailure;
        private final ShardIterator shardIterator;

        private ShardActionResult(ShardResponse shardResponse) {
            assert shardResponse != null;
            this.shardResponse = shardResponse;
            this.shardFailure = null;
            this.shardIterator = null;
        }

        private ShardActionResult(ShardOperationFailedException shardOperationFailedException, ShardIterator shardIterator) {
            assert shardOperationFailedException != null;
            this.shardFailure = shardOperationFailedException;
            this.shardIterator = shardIterator;
            this.shardResponse = null;
        }

        boolean isFailure() {
            return shardFailure != null;
        }
    }
}
