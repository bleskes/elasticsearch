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
package org.elasticsearch.discovery.zen;

import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ProcessedClusterStateUpdateTask;
import org.elasticsearch.cluster.block.ClusterBlocks;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.routing.allocation.AllocationService;
import org.elasticsearch.cluster.routing.allocation.RoutingAllocation;
import org.elasticsearch.common.Priority;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.concurrent.ConcurrentCollections;
import org.elasticsearch.discovery.DiscoverySettings;
import org.elasticsearch.discovery.zen.membership.MembershipAction;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class NodeJoinController {

    protected final ClusterService clusterService;
    protected final ESLogger logger;
    protected final AllocationService allocationService;


    protected final BlockingQueue<Tuple<DiscoveryNode, MembershipAction.JoinCallback>> processJoinRequests = ConcurrentCollections.newBlockingQueue();

    protected NodeJoinController(ClusterService clusterService, AllocationService allocationService, ESLogger logger) {
        this.clusterService = clusterService;
        this.logger = logger;
        this.allocationService = allocationService;
    }

    public void handleJoinRequest(final DiscoveryNode node, final MembershipAction.JoinCallback callback) {
        processJoinRequests.add(new Tuple<>(node, callback));
    }

    public static class AccumulateJoinsAndElectAsMaster extends NodeJoinController {

        private final int requiredJoins;
        private final Callback listener;
        private final DiscoverySettings discoverySettings;
        private final CountDownLatch electedAsMaster = new CountDownLatch(1);
        final AtomicBoolean done = new AtomicBoolean(false);

        public interface Callback {
            void onElectedAsMaster(ClusterState state);

            void onFailure(Throwable t);
        }

        protected AccumulateJoinsAndElectAsMaster(int requiredJoins, ClusterService clusterService, ESLogger logger, AllocationService allocationService, DiscoverySettings discoverySettings, Callback listener) {
            super(clusterService, allocationService, logger);
            this.requiredJoins = requiredJoins;
            this.listener = listener;
            this.discoverySettings = discoverySettings;
            if (requiredJoins <= 0) {
                electAsMaster(new ArrayList<Tuple<DiscoveryNode, MembershipAction.JoinCallback>>());
            }
        }

        public boolean waitToBeElectedAsMaster(TimeValue timeValue) {
            try {
                if (electedAsMaster.await(timeValue.millis(), TimeUnit.MILLISECONDS)) {
                    assert done.get() : "elected as master but not done";
                    return true;
                }
            } catch (InterruptedException e) {

            }
            logger.debug("timed out waiting to be elected, marking as done and failing incoming joins");
            if (done.compareAndSet(false, true) == false) {
                // master election started
                return true;
            }
            // fail all the accumulated joins
            clusterService.submitStateUpdateTask("zen-disco-join(failing pending joins after timeout)", Priority.URGENT, new ProcessJoinsTask());
            return false;
        }

        // synchronize to try and be nice to join requests that come in while we become a master so we want reject them
        @Override
        public synchronized void handleJoinRequest(DiscoveryNode node, final MembershipAction.JoinCallback callback) {
            super.handleJoinRequest(node, callback);
            if (done.get()) {
                clusterService.submitStateUpdateTask("zen-disco-receive(join from node[" + node + "])", Priority.URGENT, new ProcessJoinsTask());
                return;
            }
            if (processJoinRequests.size() >= requiredJoins) {
                ArrayList<Tuple<DiscoveryNode, MembershipAction.JoinCallback>> seedJoins = new ArrayList<>();
                processJoinRequests.drainTo(seedJoins);
                assert seedJoins.size() >= requiredJoins;
                electAsMaster(seedJoins);
            }
        }

        private void electAsMaster(final ArrayList<Tuple<DiscoveryNode, MembershipAction.JoinCallback>> seedJoins) {
            // from now on pass things through directly.. we don't care about concurrent times - we got enough joins so we can become master
            done.set(true);
            final int joinedReceived = seedJoins.size();
            final String source = "zen-disco-join(elected_as_master, [" + joinedReceived + "] joins received)";
            clusterService.submitStateUpdateTask(source, Priority.IMMEDIATE, new ProcessJoinsTask(seedJoins) {
                @Override
                public ClusterState execute(ClusterState currentState) {
                    // Take into account the previous known nodes, if they happen not to be available
                    // then fault detection will remove these nodes.

                    if (currentState.nodes().masterNode() != null) {
                        // TODO can we tie break here? we don't have a remote master cluster state version to decide on
                        logger.trace("join thread elected local node as master, but there is already a master in place: {}", currentState.nodes().masterNode());
                        throw new NotMasterException("Node [" + clusterService.localNode() + "] not master for join request");
                    }

                    DiscoveryNodes.Builder builder = new DiscoveryNodes.Builder(currentState.nodes()).masterNodeId(currentState.nodes().localNode().id());
                    // update the fact that we are the master...
                    ClusterBlocks clusterBlocks = ClusterBlocks.builder().blocks(currentState.blocks()).removeGlobalBlock(discoverySettings.getNoMasterBlock()).build();
                    currentState = ClusterState.builder(currentState).nodes(builder).blocks(clusterBlocks).build();

                    // add the incoming join requests
                    currentState = super.execute(currentState);

                    // eagerly run reroute to remove dead nodes from routing table
                    RoutingAllocation.Result result = allocationService.reroute(currentState);
                    return ClusterState.builder(currentState).routingResult(result).build();
                }

                @Override
                public boolean runOnlyOnMaster() {
                    return false;
                }

                @Override
                public void onFailure(String source, Throwable t) {
                    super.onFailure(source, t);
                    listener.onFailure(t);
                }

                @Override
                public void clusterStateProcessed(String source, ClusterState oldState, ClusterState newState) {
                    super.clusterStateProcessed(source, oldState, newState);
                    listener.onElectedAsMaster(newState);
                    electedAsMaster.countDown();
                }
            });
        }
    }

    public static class ImmediateProcessing extends NodeJoinController {

        public ImmediateProcessing(ClusterService clusterService, AllocationService allocationService, ESLogger logger) {
            super(clusterService, allocationService, logger);
        }

        @Override
        public void handleJoinRequest(DiscoveryNode node, MembershipAction.JoinCallback callback) {
            super.handleJoinRequest(node, callback);
            clusterService.submitStateUpdateTask("zen-disco-receive(join from node[" + node + "])", Priority.URGENT, new ProcessJoinsTask());
        }
    }


    class ProcessJoinsTask extends ProcessedClusterStateUpdateTask {

        private final List<Tuple<DiscoveryNode, MembershipAction.JoinCallback>> joinRequestsToProcess;
        private boolean nodeAdded = false;


        ProcessJoinsTask() {
            joinRequestsToProcess = new ArrayList<>();
        }

        ProcessJoinsTask(List<Tuple<DiscoveryNode, MembershipAction.JoinCallback>> nodesToProcess) {
            this.joinRequestsToProcess = new ArrayList<>(nodesToProcess);
        }

        @Override
        public ClusterState execute(ClusterState currentState) {
            processJoinRequests.drainTo(joinRequestsToProcess);
            if (joinRequestsToProcess.isEmpty()) {
                return currentState;
            }

            DiscoveryNodes.Builder nodesBuilder = DiscoveryNodes.builder(currentState.nodes());
            for (Tuple<DiscoveryNode, MembershipAction.JoinCallback> task : joinRequestsToProcess) {
                DiscoveryNode node = task.v1();
                if (currentState.nodes().nodeExists(node.id())) {
                    logger.debug("received a join request for an existing node [{}]", node);
                } else {
                    nodeAdded = true;
                    nodesBuilder.put(node);
                    for (DiscoveryNode existingNode : currentState.nodes()) {
                        if (node.address().equals(existingNode.address())) {
                            nodesBuilder.remove(existingNode.id());
                            logger.warn("received join request from node [{}], but found existing node {} with same address, removing existing node", node, existingNode);
                        }
                    }
                }
            }


            // we must return a new cluster state instance to force publishing. This is important
            // for the joining node to finalize it's join and set us as a master
            final ClusterState.Builder newState = ClusterState.builder(currentState);
            if (nodeAdded) {
                newState.nodes(nodesBuilder);
            }

            return newState.build();
        }

        @Override
        public void onNoLongerMaster(String source) {
            // we are rejected, so drain all pending task (execute never run)
            processJoinRequests.drainTo(joinRequestsToProcess);
            Exception e = new NotMasterException("Node [" + clusterService.localNode() + "] not master [" + source + "]");
            innerOnFailure(e);
        }

        void innerOnFailure(Throwable t) {
            for (Tuple<DiscoveryNode, MembershipAction.JoinCallback> drainedTask : joinRequestsToProcess) {
                try {
                    drainedTask.v2().onFailure(t);
                } catch (Exception e) {
                    logger.error("error during task failure", e);
                }
            }
        }

        @Override
        public void onFailure(String source, Throwable t) {
            logger.error("unexpected failure during [{}]", t, source);
            innerOnFailure(t);
        }

        @Override
        public void clusterStateProcessed(String source, ClusterState oldState, ClusterState newState) {
            if (nodeAdded) {
                // we reroute not in the same cluster state update since in certain areas we rely on
                // the node to be in the cluster state (sampled from ClusterService#state) to be there, also
                // shard transitions need to better be handled in such cases
                routingService.reroute("post_node_add");
            }
            for (Tuple<DiscoveryNode, MembershipAction.JoinCallback> drainedTask : joinRequestsToProcess) {
                try {
                    drainedTask.v2().onSuccess();
                } catch (Exception e) {
                    logger.error("unexpected error during [{}]", e, source);
                }
            }
        }
    }
}
