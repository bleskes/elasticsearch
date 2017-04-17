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

package org.elasticsearch.indices.recovery;

import org.elasticsearch.ExceptionsHelper;
import org.elasticsearch.cluster.routing.RoutingNode;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.lease.Releasable;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.IndexService;
import org.elasticsearch.index.shard.IndexEventListener;
import org.elasticsearch.index.shard.IndexShard;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.indices.IndicesService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportChannel;
import org.elasticsearch.transport.TransportRequestHandler;
import org.elasticsearch.transport.TransportService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The source recovery accepts recovery requests from other peer shards and start the recovery process from this
 * source shard to the target shard.
 */
public class PeerRecoverySourceService extends AbstractComponent implements IndexEventListener {

    public static class Actions {
        public static final String START_LEGACY_RECOVERY =
            "internal:index/shard/recovery/start_recovery";
        public static final String START_FILE_RECOVERY =
            "internal:index/shard/recovery/start_file_recovery";
        public static final String START_OPS_RECOVERY =
            "internal:index/shard/recovery/start_ops_recovery";
        public static final String START_PRIMARY_HANDOFF =
            "internal:index/shard/recovery/start_primary_handoff";
    }

    private final TransportService transportService;
    private final IndicesService indicesService;
    private final RecoverySettings recoverySettings;

    private final ClusterService clusterService;

    private final OngoingRecoveries ongoingRecoveries = new OngoingRecoveries();

    @Inject
    public PeerRecoverySourceService(Settings settings, TransportService transportService,
                                     IndicesService indicesService,
                                     RecoverySettings recoverySettings,
                                     ClusterService clusterService) {
        super(settings);
        this.transportService = transportService;
        this.indicesService = indicesService;
        this.clusterService = clusterService;
        this.recoverySettings = recoverySettings;
        transportService.registerRequestHandler(Actions.START_FILE_RECOVERY,
            StartFileRecoveryRequest::new, ThreadPool.Names.GENERIC,
            new StartRecoveryTransportRequestHandler());
        transportService.registerRequestHandler(Actions.START_OPS_RECOVERY,
            StartOpsRecoveryRequest::new, ThreadPool.Names.GENERIC,
            new StartRecoveryTransportRequestHandler());
        transportService.registerRequestHandler(Actions.START_PRIMARY_HANDOFF,
            StartPrimaryHandoffRequest::new, ThreadPool.Names.GENERIC,
            new StartRecoveryTransportRequestHandler());
    }

    @Override
    public void beforeIndexShardClosed(ShardId shardId, @Nullable IndexShard indexShard,
                                       Settings indexSettings) {
        if (indexShard != null) {
            ongoingRecoveries.cancel(indexShard, "shard is closed");
        }
    }

    private RecoveryResponse recover(final StartRecoveryRequest request) throws IOException {
        final IndexService indexService =
            indicesService.indexServiceSafe(request.shardId().getIndex());
        final IndexShard shard = indexService.getShard(request.shardId().id());

        // starting recovery from that our (the source) shard state is marking the shard to be in
        // recovery mode as well, otherwise the index operations will not be routed to it properly
        RoutingNode node = clusterService.state().getRoutingNodes().node(request.targetNode().getId());
        if (node == null) {
            logger.debug("delaying recovery of {} as source node {} is unknown", request.shardId(), request.targetNode());
            throw new DelayRecoveryException("source node does not have the node [" + request.targetNode() + "] in its state yet..");
        }

        ShardRouting targetShardRouting = node.getByShardId(request.shardId());
        if (targetShardRouting == null) {
            logger.debug(
                "delaying recovery of {} as it is not listed as assigned to target node {}",
                request.shardId(), request.targetNode());
            throw new DelayRecoveryException(
                "source node does not have the shard listed in its state as allocated on the node");
        }
        if (!targetShardRouting.initializing()) {
            logger.debug("delaying recovery of {} as it is not listed as initializing on the " +
                    "target node {}. known shards state is [{}]",
                request.shardId(), request.targetNode(), targetShardRouting.state());
            throw new DelayRecoveryException("source node has the state of the target shard to " +
                "be [" + targetShardRouting.state() + "], expecting to be [initializing]");
        }

        if (targetShardRouting.primary()) {
            ShardRouting routingEntry = shard.routingEntry();
            if (routingEntry.relocating() == false ||
                routingEntry.relocatingNodeId().equals(targetShardRouting.currentNodeId()) == false) {
                assert false :
                    "primary relocation but shard is not yet marked as relocated: " + routingEntry +
                        " target: " + targetShardRouting;
                logger.warn("receive primary recovery request to [{}] but local shard [{}] is not "
                    + "relocated", targetShardRouting, routingEntry);
                throw new IllegalArgumentException(
                    "source shard is not marked as relocating to [" + request.targetNode() + "]");
            }
            if (request instanceof StartPrimaryHandoffRequest &&
                shard.isAllocationIDInSync(targetShardRouting.allocationId().getId()) == false) {
                assert false: "primary handoff requested but target aID is not in sync: "
                    + targetShardRouting;
                throw new IllegalArgumentException(
                    "target shard is not marked as in sync to [" + request.targetNode() +"]");
            }
        }

        RecoverySourceHandler handler =
            ongoingRecoveries.addNewRecovery(request, targetShardRouting, shard);
        logger.trace("[{}][{}] starting recovery to {}",
            request.shardId().getIndex().getName(), request.shardId().id(), request.targetNode());
        try {
            // nocommit: check for shard close
            return handler.recoverToTarget();
        } finally {
            ongoingRecoveries.remove(shard, handler);
        }
    }

    class StartRecoveryTransportRequestHandler implements
        TransportRequestHandler<StartRecoveryRequest> {
        @Override
        public void messageReceived(final StartRecoveryRequest request,
                                    final TransportChannel channel) throws Exception {
            RecoveryResponse response = recover(request);
            channel.sendResponse(response);
        }
    }

    private final class OngoingRecoveries {
        private final Map<IndexShard, ShardRecoveryContext> ongoingRecoveries = new HashMap<>();

        synchronized RecoverySourceHandler addNewRecovery(StartRecoveryRequest request,
                                                              ShardRouting targetRouting,
                                                              IndexShard shard) {
            final ShardRecoveryContext shardContext =
                ongoingRecoveries.computeIfAbsent(shard, s -> new ShardRecoveryContext());
            RecoverySourceHandler handler =
                shardContext.addNewRecovery(request, targetRouting, shard);
            shard.recoveryStats().incCurrentAsSource();
            return handler;
        }

        synchronized void remove(IndexShard shard, RecoverySourceHandler handler) {
            final ShardRecoveryContext shardRecoveryContext = ongoingRecoveries.get(shard);
            assert shardRecoveryContext != null : "Shard was not registered [" + shard + "]";
            boolean remove = shardRecoveryContext.recoveryHandlers.remove(handler);
            assert remove : "Handler was not registered [" + handler + "]";
            if (remove) {
                shard.recoveryStats().decCurrentAsSource();
            }
            if (shardRecoveryContext.recoveryHandlers.isEmpty()) {
                ongoingRecoveries.remove(shard);
                assert shardRecoveryContext.onNewRecoveryException == null;
            }
        }

        synchronized void cancel(IndexShard shard, String reason) {
            final ShardRecoveryContext shardRecoveryContext = ongoingRecoveries.get(shard);
            if (shardRecoveryContext != null) {
                final List<Exception> failures = new ArrayList<>();
                for (RecoverySourceHandler handlers : shardRecoveryContext.recoveryHandlers) {
                    try {
                        handlers.cancel(reason);
                    } catch (Exception ex) {
                        failures.add(ex);
                    } finally {
                        shard.recoveryStats().decCurrentAsSource();
                    }
                }
                ExceptionsHelper.maybeThrowRuntimeAndSuppress(failures);
            }
        }

        private final class ShardRecoveryContext {
            final Set<RecoverySourceHandler> recoveryHandlers = new HashSet<>();

            @Nullable
            private DelayRecoveryException onNewRecoveryException;

            /**
             * Adds recovery source handler if recoveries are not delayed from starting
             * (see also {@link #delayNewRecoveries}.
             * Throws {@link DelayRecoveryException} if new recoveries are delayed from starting.
             */
            synchronized RecoverySourceHandler addNewRecovery(StartRecoveryRequest request,
                                                                  ShardRouting targetRouting,
                                                                  IndexShard shard) {
                if (onNewRecoveryException != null) {
                    throw onNewRecoveryException;
                }
                RecoverySourceHandler handler =
                    createRecoverySourceHandler(request, targetRouting, shard);
                recoveryHandlers.add(handler);
                return handler;
            }

            /**
             * Makes new recoveries throw a {@link DelayRecoveryException} with the provided message.
             *
             * Throws {@link IllegalStateException} if new recoveries are already being delayed.
             */
            synchronized Releasable delayNewRecoveries(String exceptionMessage) throws IllegalStateException {
                if (onNewRecoveryException != null) {
                    throw new IllegalStateException("already delaying recoveries");
                }
                onNewRecoveryException = new DelayRecoveryException(exceptionMessage);
                return this::unblockNewRecoveries;
            }


            private synchronized void unblockNewRecoveries() {
                onNewRecoveryException = null;
            }

            private RecoverySourceHandler createRecoverySourceHandler(
                StartRecoveryRequest request,
                ShardRouting targetRouting, IndexShard shard) {
                RecoverySourceHandler handler;
                if (request instanceof StartOpsRecoveryRequest) {
                    final RemoteOpsRecoveryTarget recoveryTarget =
                        new RemoteOpsRecoveryTarget(request.recoveryId(), request.shardId(),
                            targetRouting.allocationId().getId(), transportService,
                            request.targetNode(), recoverySettings);
                    handler = new OpsRecoverySourceHandler(
                        shard, recoveryTarget,(StartOpsRecoveryRequest) request,
                        recoverySettings.getChunkSize().bytesAsInt(),settings);
                } else if (request instanceof StartFileRecoveryRequest) {
                    final StartFileRecoveryRequest fullRequest = (StartFileRecoveryRequest) request;
                    final RemoteFileRecoveryTarget recoveryTarget =
                        new RemoteFileRecoveryTarget(request.recoveryId(), request.shardId(),
                            targetRouting.allocationId().getId(), transportService,
                            request.targetNode(),
                            recoverySettings,
                            throttleTime -> shard.recoveryStats().addThrottleTime(throttleTime));
                    Supplier<Long> currentClusterStateVersionSupplier = () -> clusterService.state().getVersion();
                    handler = new FileRecoverySourceHandler(shard, recoveryTarget, fullRequest,
                        recoverySettings.getChunkSize().bytesAsInt(), settings);
                } else if (request instanceof StartPrimaryHandoffRequest) {
                    RemotePrimaryHandoffRecoveryTarget recoveryTarget =
                        new RemotePrimaryHandoffRecoveryTarget(request.recoveryId(),
                            request.shardId(), transportService, request.targetNode(),
                            recoverySettings);
                    handler = new PrimaryHandoffRecoverySourceHandler(shard, recoveryTarget,
                        (StartPrimaryHandoffRequest) request,
                        () -> clusterService.state().version(), this::delayNewRecoveries,
                        settings);

                } else {
                    throw new IllegalArgumentException(
                        "unknown recovery type " + request.getClass().getName());
                }
                return handler;
            }
        }
    }
}

