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

package org.elasticsearch.discovery.raft;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.ElasticsearchIllegalStateException;
import org.elasticsearch.Version;
import org.elasticsearch.cluster.*;
import org.elasticsearch.cluster.block.ClusterBlocks;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.node.DiscoveryNodeService;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.routing.allocation.AllocationService;
import org.elasticsearch.cluster.routing.allocation.RoutingAllocation;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Priority;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.component.Lifecycle;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.concurrent.AtomicArray;
import org.elasticsearch.common.util.concurrent.ConcurrentCollections;
import org.elasticsearch.common.util.concurrent.EsRejectedExecutionException;
import org.elasticsearch.discovery.Discovery;
import org.elasticsearch.discovery.DiscoveryService;
import org.elasticsearch.discovery.DiscoverySettings;
import org.elasticsearch.discovery.InitialStateDiscoveryListener;
import org.elasticsearch.discovery.zen.DiscoveryNodesProvider;
import org.elasticsearch.discovery.zen.fd.MasterFaultDetection;
import org.elasticsearch.node.service.NodeService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class RaftDiscovery extends AbstractLifecycleComponent<Discovery> implements Discovery, DiscoveryNodesProvider {

    public static final int LIMIT_PORTS_COUNT = 1;

    private final TransportService transportService;
    private final ClusterService clusterService;
    private final ClusterName clusterName;
    private final DiscoveryNodeService discoveryNodeService;
    private final DiscoverySettings discoverySettings;
    private final Version version;
    private final ThreadPool threadPool;
    private AllocationService allocationService;
    private final RequestVoteAction requestVoteAction;
    private final PublishClusterStateAction publishClusterState;
    private final MembershipAction membershipAction;
    private final RaftPing raftPing;
    private final Random random;
    private final MasterFaultDetection masterFD;
    private final NodesFaultDetection nodesFD;
    private NodeService nodeService;


    private final AtomicBoolean initialStateSent = new AtomicBoolean();

    private DiscoveryNode localNode;

    private final CopyOnWriteArrayList<InitialStateDiscoveryListener> initialStateListeners = new CopyOnWriteArrayList<>();

    private RaftState raftState = new RaftState();

    private final DiscoveryNode[] configuredTargetNodes;
    private final AtomicArray<DiscoveryNode> resolvedTargetNodes;
    private final BlockingQueue<Tuple<DiscoveryNode, MembershipAction.JoinCallback>> processJoinRequests = ConcurrentCollections.newBlockingQueue();


    @Inject
    public RaftDiscovery(Settings settings, ClusterName clusterName,
                         TransportService transportService, ClusterService clusterService,
                         DiscoveryNodeService discoveryNodeService, Version version,
                         DiscoverySettings discoverySettings, ThreadPool threadPool) {
        super(settings);
        this.clusterName = clusterName;
        this.clusterService = clusterService;
        this.transportService = transportService;
        this.discoveryNodeService = discoveryNodeService;
        this.discoverySettings = discoverySettings;
        this.version = version;
        this.threadPool = threadPool;
        this.random = createRandom(settings);

        String[] hostArr = componentSettings.getAsArray("hosts");
        // trim the hosts
        for (int i = 0; i < hostArr.length; i++) {
            hostArr[i] = hostArr[i].trim();
        }
        List<String> hosts = Lists.newArrayList(hostArr);
        logger.debug("using initial hosts {}", hosts);

        List<DiscoveryNode> configuredTargetNodes = Lists.newArrayList();
        int idCounter = 0;
        for (String host : hosts) {
            try {
                TransportAddress[] addresses = transportService.addressesFromString(host);
                // we only limit to 1 addresses, makes no sense to ping 100 ports
                for (int i = 0; (i < addresses.length && i < LIMIT_PORTS_COUNT); i++) {
                    configuredTargetNodes.add(new DiscoveryNode("#raft_" + (++idCounter) + "#", addresses[i], version.minimumCompatibilityVersion()));
                }
            } catch (Exception e) {
                throw new ElasticsearchIllegalArgumentException("Failed to resolve address for [" + host + "]", e);
            }
        }
        this.configuredTargetNodes = configuredTargetNodes.toArray(new DiscoveryNode[configuredTargetNodes.size()]);
        this.resolvedTargetNodes = new AtomicArray<>(configuredTargetNodes.size());

        this.requestVoteAction = new RequestVoteAction(settings, transportService, clusterName, this, raftState);
        this.publishClusterState = new PublishClusterStateAction(settings, transportService, this, new NewClusterStateListener(), discoverySettings, clusterName, raftState);
        this.raftPing = new RaftPing(settings, transportService, this, clusterName, raftState, clusterService);

        this.masterFD = new MasterFaultDetection(settings, threadPool, transportService, this, clusterName);
        this.masterFD.addListener(new MasterNodeFailureListener());

        this.nodesFD = new NodesFaultDetection(settings, threadPool, transportService, clusterName, raftState, this);
        this.nodesFD.addListener(new NodeFaultDetectionListener());

        this.membershipAction = new MembershipAction(settings, transportService, new MembershipListener(), this);
    }


    private void startAnElection(long expectedTerm) {
        boolean startElection = false;
        synchronized (raftState) {
            if (raftState.term() == expectedTerm) {
                raftState.term(raftState.term() + 1);
                raftState.role(RaftState.RaftRole.CANDIDATE);
                startElection = true;
            }
        }
        if (startElection) {
            ArrayList<DiscoveryNode> resolvedNodes = new ArrayList<>();
            for (int i = 0; i < resolvedTargetNodes.length(); i++) {
                if (resolvedTargetNodes.get(i) != null) {
                    resolvedNodes.add(resolvedTargetNodes.get(i));
                }
            }
            requestVoteAction.performElection(expectedTerm + 1,
                    resolvedNodes.toArray(new DiscoveryNode[resolvedNodes.size()]),
                    resolvedTargetNodes.length() / 2 + 1); // we still need majority, even if we didn't resolve all
        }
    }

    public void handleElectionVictory(final long electionsTerm, final List<DiscoveryNode> activeNodes) {
        synchronized (raftState) {
            if (raftState.term() != electionsTerm) {
                logger.trace("won the election for term [{}] but my term is [{}], ignoring", electionsTerm, raftState.term());
                return;
            }
            raftState.role(RaftState.RaftRole.MASTER);
        }

        clusterService.submitStateUpdateTask("elected_as_master (term [" + electionsTerm + "])", Priority.URGENT, new ProcessedClusterStateNonMasterUpdateTask() {
            @Override
            public ClusterState execute(ClusterState currentState) {
                if (raftState.term() != electionsTerm) {
                    logger.debug("term changed while processing election results - ignore election (election term: [{}], current [{}])", electionsTerm, raftState.term());
                    return currentState;
                }
                DiscoveryNodes.Builder nodesBuilder = new DiscoveryNodes.Builder()
                        .localNodeId(localNode.id())
                        .masterNodeId(localNode.id())
                                // put our local node
                        .put(localNode);

                // Todo: not sure we want to do this...
                for (DiscoveryNode node : activeNodes) {
                    if (!node.equals(localNode)) {
                        nodesBuilder.put(node);
                    }
                }

                // update the fact that we are the master...
                ClusterBlocks clusterBlocks = ClusterBlocks.builder().blocks(currentState.blocks()).removeGlobalBlock(discoverySettings.getNoMasterBlock()).build();
                currentState = ClusterState.builder(currentState).nodes(nodesBuilder.build()).blocks(clusterBlocks).build();

                masterFD.stop("elected_as_master");
                nodesFD.start();

                // eagerly run reroute to remove dead nodes from routing table
                RoutingAllocation.Result result = allocationService.reroute(currentState);
                return ClusterState.builder(currentState).routingResult(result).build();
            }

            @Override
            public void onFailure(String source, Throwable t) {
                logger.error("unexpected failure during [{}]", t, source);
            }

            @Override
            public void clusterStateProcessed(String source, ClusterState oldState, ClusterState newState) {
                sendInitialStateEventIfNeeded();
            }
        });
    }

    public void handleElectionLoss(final long electionTerm) {
        synchronized (raftState) {
            if (raftState.term() != electionTerm) {
                logger.trace("lost the election for term [{}] but my term is [{}], ignoring", electionTerm, raftState.term());
                return;
            }
            // check if we didn't become a follower in the mean time
            if (raftState.role() == RaftState.RaftRole.CANDIDATE) {
                // give things some time and try to join what ever is there..
                scheduleClusterJoin(0, false);
            }
        }
    }

    @Override
    public DiscoveryNodes nodes() {
        return clusterService.state().nodes();
    }

    @Override
    public NodeService nodeService() {
        return nodeService;
    }

    static class ProcessClusterState {
        final long term;
        final ClusterState clusterState;
        final NewClusterStateListener.NewStateProcessed newStateProcessed;
        volatile boolean processed;

        ProcessClusterState(long term, ClusterState clusterState, NewClusterStateListener.NewStateProcessed newStateProcessed) {
            this.term = term;
            this.clusterState = clusterState;
            this.newStateProcessed = newStateProcessed;
        }
    }

    private final BlockingQueue<ProcessClusterState> processNewClusterStates = ConcurrentCollections.newBlockingQueue();

    void handleNewClusterStateFromMaster(long termForNewState, final ClusterState newClusterState, final NewClusterStateListener.NewStateProcessed newStateProcessed) {
        final ClusterName incomingClusterName = newClusterState.getClusterName();
        /* The cluster name can still be null if the state comes from a node that is prev 1.1.1*/
        if (incomingClusterName != null && !incomingClusterName.equals(this.clusterName)) {
            logger.warn("received cluster state from [{}] which is also master but with a different cluster name [{}]", newClusterState.nodes().masterNode(), incomingClusterName);
            newStateProcessed.onNewClusterStateFailed(new ElasticsearchIllegalStateException("received state from a node that is not part of the cluster"));
            return;
        }

        if (newClusterState.nodes().localNode() == null) {
            logger.warn("received a cluster state from [{}] and not part of the cluster, should not happen", newClusterState.nodes().masterNode());
            newStateProcessed.onNewClusterStateFailed(new ElasticsearchIllegalStateException("received state from a node that is not part of the cluster"));
            return;
        }

        if (termForNewState < raftState.term()) {
            logger.debug("received cluster state from [{}] of a lower term [{}] (expected >= [{}])",
                    newClusterState.nodes().masterNode(), termForNewState, raftState.term());
            // TODO: signal other master to step down?
            newStateProcessed.onNewClusterStateProcessed();
            return;
        }
        final ProcessClusterState processClusterState = new ProcessClusterState(termForNewState, newClusterState, newStateProcessed);
        processNewClusterStates.add(processClusterState);


        assert newClusterState.nodes().masterNode() != null : "received a cluster state without a master";
        assert !newClusterState.blocks().hasGlobalBlock(discoverySettings.getNoMasterBlock()) : "received a cluster state with a master block";

        clusterService.submitStateUpdateTask("raft-receive(from master [" + newClusterState.nodes().masterNode() + "] term [" + termForNewState + "])", Priority.URGENT, new ProcessedClusterStateNonMasterUpdateTask() {

            // TODO: hack - replace
            long termUsed = -1;

            @Override
            public ClusterState execute(ClusterState currentState) {
                // we already processed it in a previous event
                if (processClusterState.processed) {
                    return currentState;
                }

                // TODO: once improvement that we can do is change the message structure to include version and masterNodeId
                // at the start, this will allow us to keep the "compressed bytes" around, and only parse the first page
                // to figure out if we need to use it or not, and only once we picked the latest one, parse the whole state


                // try and get the state with the highest version out of all the ones with the same master node id
                ProcessClusterState stateToProcess = processNewClusterStates.poll();
                if (stateToProcess == null) {
                    return currentState;
                }
                stateToProcess.processed = true;
                while (true) {
                    ProcessClusterState potentialState = processNewClusterStates.peek();
                    // nothing else in the queue, bail
                    if (potentialState == null) {
                        break;
                    }
                    // if its not from the same master, then bail
                    if (!Objects.equal(stateToProcess.clusterState.nodes().masterNodeId(), potentialState.clusterState.nodes().masterNodeId())) {
                        break;
                    }

                    // we are going to use it for sure, poll (remove) it
                    potentialState = processNewClusterStates.poll();
                    if (potentialState == null) {
                        // might happen if the queue is drained
                        break;
                    }

                    potentialState.processed = true;

                    if (potentialState.term > stateToProcess.term ||
                            (potentialState.term == stateToProcess.term && potentialState.clusterState.version() > stateToProcess.clusterState.version())
                            ) {
                        // we found a new one
                        stateToProcess = potentialState;
                    }
                }

                ClusterState updatedState = stateToProcess.clusterState;

                boolean termUpdated = false;
                synchronized (raftState) {
                    // double check the term didn't increase in the meantime
                    if (stateToProcess.term < raftState.term()) {
                        logger.trace("term was increased to [{}] during processing of cluster state from term [{}]. ignoring",
                                raftState.term(), stateToProcess.term);
                        return currentState;
                    } else if (stateToProcess.term > raftState.term()) {
                        // update term
                        raftState.term(stateToProcess.term);
                        termUpdated = true;
                    }
                    if (raftState.role() != RaftState.RaftRole.FOLLOWER) {
                        logger.trace("got a new state from master node not being a [{}], converting to a follower", raftState);
                        raftState.role(RaftState.RaftRole.FOLLOWER);
                    }
                }

                // if the new state has the same master node but an older version and the term didn't change, then no need to process it
                if (!termUpdated &&
                        updatedState.version() < currentState.version() &&
                        Objects.equal(updatedState.nodes().masterNodeId(), currentState.nodes().masterNodeId())) {
                    return currentState;
                }

                termUsed = stateToProcess.term;

                // check to see that we monitor the correct master of the cluster
                if (masterFD.masterNode() == null || !masterFD.masterNode().equals(newClusterState.nodes().masterNode())) {
                    masterFD.restart(newClusterState.nodes().masterNode(), "new cluster state received and we are monitoring the wrong master [" + masterFD.masterNode() + "]");
                }

                if (currentState.blocks().hasGlobalBlock(discoverySettings.getNoMasterBlock())) {
                    // its a fresh update from the master as we transition from a start of not having a master to having one
                    logger.debug("got first state from fresh master [{}]", updatedState.nodes().masterNodeId());
                    return updatedState;
                }


                // some optimizations to make sure we keep old objects where possible
                ClusterState.Builder builder = ClusterState.builder(updatedState);

                // if the routing table did not change, use the original one
                if (updatedState.routingTable().version() == currentState.routingTable().version()) {
                    builder.routingTable(currentState.routingTable());
                }
                // same for metadata
                if (updatedState.metaData().version() == currentState.metaData().version()) {
                    builder.metaData(currentState.metaData());
                } else {
                    // if its not the same version, only copy over new indices or ones that changed the version
                    MetaData.Builder metaDataBuilder = MetaData.builder(updatedState.metaData()).removeAllIndices();
                    for (IndexMetaData indexMetaData : updatedState.metaData()) {
                        IndexMetaData currentIndexMetaData = currentState.metaData().index(indexMetaData.index());
                        if (currentIndexMetaData == null || currentIndexMetaData.version() != indexMetaData.version()) {
                            metaDataBuilder.put(indexMetaData, false);
                        } else {
                            metaDataBuilder.put(currentIndexMetaData, false);
                        }
                    }
                    builder.metaData(metaDataBuilder);
                }

                return builder.build();
            }

            @Override
            public void onFailure(String source, Throwable t) {
                logger.error("unexpected failure during [{}]", t, source);
                newStateProcessed.onNewClusterStateFailed(t);
            }

            @Override
            public void clusterStateProcessed(String source, ClusterState oldState, ClusterState newState) {
                sendInitialStateEventIfNeeded();
                if (termUsed > 0) {
                    synchronized (raftState) {
                        raftState.lastClusterStateTerm(termUsed);
                        raftState.lastClusterStateVersion(newState.version());
                    }
                }
                newStateProcessed.onNewClusterStateProcessed();
            }
        });
    }

    public void handleHigherTermFromFollower(long term, DiscoveryNode node) {
        synchronized (raftState) {
            if (raftState.term() >= term || raftState.role() != RaftState.RaftRole.MASTER) {
                return;
            }

            logger.debug("stepping down as master, found a follower {}] with a higher term [{}] (ours is [{}])", node, term, raftState.term());
            raftState.term(term);
            raftState.role(RaftState.RaftRole.FOLLOWER);
        }
        clusterService.submitStateUpdateTask("follower_with_higher_term(" + node + ")", Priority.IMMEDIATE, new ProcessedClusterStateNonMasterUpdateTask() {
            @Override
            public ClusterState execute(ClusterState currentState) {
                if (!currentState.nodes().localNodeMaster()) {
                    // master got switched on us, no need to send anything
                    return currentState;
                }

                DiscoveryNodes discoveryNodes = DiscoveryNodes.builder(currentState.nodes()).masterNodeId(null).build();

                // flush any pending cluster states from old master, so it will not be set as master again
                ArrayList<ProcessClusterState> pendingNewClusterStates = new ArrayList<>();
                processNewClusterStates.drainTo(pendingNewClusterStates);
                logger.trace("removed [{}] pending cluster states", pendingNewClusterStates.size());

                return rejoin(ClusterState.builder(currentState).nodes(discoveryNodes).build(), "stepped down(follower with a higher temr)");
            }

            @Override
            public void onFailure(String source, Throwable t) {
                logger.error("unexpected failure during [{}]", t, source);

            }

            @Override
            public void clusterStateProcessed(String source, ClusterState oldState, ClusterState newState) {
            }

        });
    }

    private void handleMasterGone(final DiscoveryNode masterNode, final String reason) {
        if (lifecycleState() != Lifecycle.State.STARTED) {
            // not started, ignore a master failure
            return;
        }

        logger.info("master_left [{}], reason [{}]", masterNode, reason);

        clusterService.submitStateUpdateTask("raft-disco-master_failed (" + masterNode + ")", Priority.IMMEDIATE, new ProcessedClusterStateNonMasterUpdateTask() {
            @Override
            public ClusterState execute(ClusterState currentState) {
                if (!masterNode.id().equals(currentState.nodes().masterNodeId())) {
                    // master got switched on us, no need to send anything
                    return currentState;
                }

                DiscoveryNodes discoveryNodes = DiscoveryNodes.builder(currentState.nodes()).masterNodeId(null).build();

                // flush any pending cluster states from old master, so it will not be set as master again
                ArrayList<ProcessClusterState> pendingNewClusterStates = new ArrayList<>();
                processNewClusterStates.drainTo(pendingNewClusterStates);
                logger.trace("removed [{}] pending cluster states", pendingNewClusterStates.size());

                return rejoin(ClusterState.builder(currentState).nodes(discoveryNodes).build(), "master left (reason = " + reason + ")");
            }

            @Override
            public void onFailure(String source, Throwable t) {
                logger.error("unexpected failure during [{}]", t, source);
            }

            @Override
            public void clusterStateProcessed(String source, ClusterState oldState, ClusterState newState) {
                sendInitialStateEventIfNeeded();
            }

        });
    }

    private ClusterState rejoin(ClusterState clusterState, String reason) {
        logger.warn(reason + ", current nodes: {}", clusterState.nodes());
        nodesFD.stop();
        masterFD.stop(reason);

        ClusterBlocks clusterBlocks = ClusterBlocks.builder().blocks(clusterState.blocks())
                .addGlobalBlock(discoverySettings.getNoMasterBlock())
                .build();

        // clean the nodes, we are now not connected to anybody, since we try and reform the cluster
        DiscoveryNodes nodes = new DiscoveryNodes.Builder(clusterState.nodes()).masterNodeId(null).build();

        scheduleClusterJoin(0, false);

        return ClusterState.builder(clusterState)
                .blocks(clusterBlocks)
                .nodes(nodes)
                .build();
    }

    private void scheduleClusterJoin(final int attemptCount, boolean immediate) {
        final long term;
        synchronized (raftState) {
            term = raftState.term();
            // clear vote for this term
            raftState.votedFor(null);
        }
        // TODO: settings
        TimeValue waitTime = TimeValue.timeValueMillis(immediate ? 0 : 100 + random.nextInt(300));
        threadPool.schedule(waitTime, ThreadPool.Names.GENERIC,
                new Runnable() {
                    public void run() {
                        if (raftState.term() > term || raftState.votedFor() != null) {
                            // something have changed - relinquish control to the process
                            logger.trace("skipping cluster rejoin as term has changed or vote is not null. Expected term [{}], found [{}]",
                                    raftState.term(), term);
                            return;
                        }
                        RaftPing.PingResult result;
                        try {
                            result = doPing();
                        } catch (Exception e) {
                            // TODO:
                            logger.error("error while pinging before cluster join, scheduling a retry", e);
                            // do not increment attempt count - this should have no influence on the term
                            scheduleClusterJoin(attemptCount, false);
                            return;
                        }

                        if (result.masterAdvice() == null) {
                            logger.debug("pinging didn't discover any master candidate, starting an election");
                            startAnElection(term);
                            return;
                        }

                        if (result.masterAdvice().equals(localNode)) {
                            // TODO: what to do here?
                            logger.debug("pinging pointed local node as potential master, starting an election ");
                            startAnElection(term);
                            return;
                        }

                        logger.trace("joining advised master {}", result.masterAdvice());
                        try {
                            transportService.connectToNode(result.masterAdvice());
                            membershipAction.sendJoinRequestBlocking(result.masterAdvice(), localNode(), TimeValue.timeValueSeconds(60));
                            return;
                        } catch (Exception e) {
                            // TODO: differentiate between errors
                            logger.debug("error while joining master {}", e, result.masterAdvice());
                        }

                        // if we got here it's not good...
                        // TODO: configure
                        if (attemptCount > 3) {
                            startAnElection(term);
                            return;
                        }

                        scheduleClusterJoin(attemptCount + 1, false);

                    }
                });
    }

    private void handleNodeRemoval(final DiscoveryNode node, String reason) {
        if (lifecycleState() != Lifecycle.State.STARTED) {
            // not started, ignore a node failure
            return;
        }

        clusterService.submitStateUpdateTask("raft_removing_node(" + node + "), reason " + reason, Priority.IMMEDIATE, new ProcessedClusterStateUpdateTask() {
            @Override
            public ClusterState execute(ClusterState currentState) {
                DiscoveryNodes nodes = DiscoveryNodes.builder(currentState.nodes())
                        .remove(node.id()).build();
                currentState = ClusterState.builder(currentState).nodes(nodes).build();
                // check if we have enough master nodes, if not, we need to move into joining the cluster again

                int memberTargets = 0;
                for (int i = 0; i < resolvedTargetNodes.length(); i++) {
                    DiscoveryNode target = resolvedTargetNodes.get(i);
                    if (target != null && nodes.nodeExists(target.id())) {
                        memberTargets++;
                    }
                }

                if (memberTargets < resolvedTargetNodes.length() / 2 + 1) {
                    return rejoin(currentState, "not enough master nodes");
                }
                // eagerly run reroute to remove dead nodes from routing table
                RoutingAllocation.Result routingResult = allocationService.reroute(ClusterState.builder(currentState).build());
                return ClusterState.builder(currentState).routingResult(routingResult).build();
            }

            @Override
            public void onNoLongerMaster(String source) {
                // already logged
            }

            @Override
            public void onFailure(String source, Throwable t) {
                logger.error("unexpected failure during [{}]", t, source);
            }

            @Override
            public void clusterStateProcessed(String source, ClusterState oldState, ClusterState newState) {
                sendInitialStateEventIfNeeded();
            }
        });
    }


    private class NodeFaultDetectionListener extends NodesFaultDetection.Listener {

        @Override
        public void onNodeFailure(DiscoveryNode node, String reason) {
            handleNodeRemoval(node, reason);
        }

        @Override
        public void onPingReceived(final NodesFaultDetection.PingRequest pingRequest) {

        }
    }

    private class MasterNodeFailureListener implements MasterFaultDetection.Listener {

        @Override
        public void onMasterFailure(DiscoveryNode masterNode, String reason) {
            handleMasterGone(masterNode, reason);
        }

        @Override
        public void onDisconnectedFromMaster() {
            // got disconnected from the master, send a join request
            // TODO: think whether we want to copy this behavior
            scheduleClusterJoin(0, true);
        }
    }

    @Override
    protected void doStart() throws ElasticsearchException {
        Map<String, String> nodeAttributes = discoveryNodeService.buildAttributes();
        // note, we rely on the fact that its a new id each time we start, see FD and "kill -9" handling
        final String nodeId = DiscoveryService.generateNodeId(settings);
        localNode = new DiscoveryNode(settings.get("name"), nodeId, transportService.boundAddress().publishAddress(), nodeAttributes, version);
        nodesFD.updateNodes(new DiscoveryNodes.Builder().put(localNode).localNodeId(localNode.id()).build(), ClusterState.UNKNOWN_VERSION);

        scheduleClusterJoin(0, false);
    }

    @Override
    protected void doStop() throws ElasticsearchException {

    }

    @Override
    protected void doClose() throws ElasticsearchException {
        publishClusterState.close();
    }

    @Override
    public DiscoveryNode localNode() {
        return localNode;
    }

    @Override
    public void addListener(InitialStateDiscoveryListener listener) {
        this.initialStateListeners.add(listener);
    }

    @Override
    public void removeListener(InitialStateDiscoveryListener listener) {
        this.initialStateListeners.remove(listener);
    }

    @Override
    public String nodeDescription() {
        return clusterName.value() + "/" + localNode.id();
    }

    @Override
    public void setNodeService(@Nullable NodeService nodeService) {
        this.nodeService = nodeService;

    }

    @Override
    public void setAllocationService(AllocationService allocationService) {
        this.allocationService = allocationService;
    }

    @Override
    public void publish(ClusterState clusterState, AckListener ackListener) {
        nodesFD.updateNodes(clusterState.nodes(), clusterState.version());
        publishClusterState.publish(clusterState, ackListener);
    }

    protected static Random createRandom(Settings settings) {
        String seed = settings.get("discovery.raft.seed");
        if (seed != null) {
            return new Random(Long.parseLong(seed));
        }
        return new Random();
    }

    private void sendInitialStateEventIfNeeded() {
        if (initialStateSent.compareAndSet(false, true)) {
            for (InitialStateDiscoveryListener listener : initialStateListeners) {
                listener.initialStateProcessed();
            }
        }
    }

    private class NewClusterStateListener implements PublishClusterStateAction.NewClusterStateListener {

        @Override
        public void onNewClusterState(long term, ClusterState clusterState, NewStateProcessed newStateProcessed) {
            handleNewClusterStateFromMaster(term, clusterState, newStateProcessed);
        }
    }

    private class MembershipListener implements MembershipAction.MembershipListener {
        @Override
        public void onJoin(DiscoveryNode node, MembershipAction.JoinCallback callback) {
            handleJoinRequest(node, callback);
        }

        @Override
        public void onLeave(DiscoveryNode node) {
            if (node.equals(clusterService.state().nodes().masterNode())) {
                handleMasterGone(node, "node_leave");
            } else {
                handleNodeRemoval(node, "node_leave");
            }
        }
    }

    public void handleValidateJoin(long term) {
        synchronized (raftState) {
            if (raftState.term() < term) {
                return;
            }
            raftState.term(term);
            if (raftState.role() != RaftState.RaftRole.FOLLOWER) {
                logger.trace("switching to a follower role due to join validation (term [{}], role was [{}])", term, raftState.role());
                raftState.role(RaftState.RaftRole.FOLLOWER);
            }
        }
    }

    private void handleJoinRequest(final DiscoveryNode node, final MembershipAction.JoinCallback callback) {
        if (!clusterService.state().nodes().localNodeMaster()) {
            throw new ElasticsearchIllegalStateException("Node [" + localNode + "] not master for join request from [" + node + "]");
        }

        if (!transportService.addressSupported(node.address().getClass())) {
            // TODO, what should we do now? Maybe inform that node that its crap?
            logger.warn("received a wrong address type from [{}], ignoring...", node);
        } else {
            // try and connect to the node, if it fails, we can raise an exception back to the client...
            transportService.connectToNode(node);

            // validate the join request, will throw a failure if it fails, which will get back to the
            // node calling the join request
            // TODO: configure timeout
            membershipAction.sendValidateJoinRequestBlocking(raftState.term(), node, TimeValue.timeValueSeconds(30));

            // TODO: this is a bit hacky for keeping the resolved target hosts up to date, replace
            doPing();

            processJoinRequests.add(new Tuple<>(node, callback));
            clusterService.submitStateUpdateTask("join_from_node[" + node + "]", Priority.IMMEDIATE, new ProcessedClusterStateUpdateTask() {

                private final List<Tuple<DiscoveryNode, MembershipAction.JoinCallback>> drainedTasks = new ArrayList<>();

                @Override
                public ClusterState execute(ClusterState currentState) {
                    processJoinRequests.drainTo(drainedTasks);
                    if (drainedTasks.isEmpty()) {
                        return currentState;
                    }

                    boolean modified = false;
                    DiscoveryNodes.Builder nodesBuilder = DiscoveryNodes.builder(currentState.nodes());
                    for (Tuple<DiscoveryNode, MembershipAction.JoinCallback> task : drainedTasks) {
                        DiscoveryNode node = task.v1();
                        if (currentState.nodes().nodeExists(node.id())) {
                            logger.debug("received a join request for an existing node [{}]", node);
                        } else {
                            modified = true;
                            nodesBuilder.put(node);
                            for (DiscoveryNode existingNode : currentState.nodes()) {
                                if (node.address().equals(existingNode.address())) {
                                    nodesBuilder.remove(existingNode.id());
                                    logger.warn("received join request from node [{}], but found existing node {} with same address, removing existing node", node, existingNode);
                                }
                            }
                        }
                    }

                    ClusterState.Builder stateBuilder = ClusterState.builder(currentState);
                    if (modified) {
                        stateBuilder.nodes(nodesBuilder);
                    }
                    return stateBuilder.build();
                }

                @Override
                public void onNoLongerMaster(String source) {
                    Exception e = new EsRejectedExecutionException("no longer master. source: [" + source + "]");
                    innerOnFailure(e);
                }

                void innerOnFailure(Throwable t) {
                    for (Tuple<DiscoveryNode, MembershipAction.JoinCallback> drainedTask : drainedTasks) {
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
                    for (Tuple<DiscoveryNode, MembershipAction.JoinCallback> drainedTask : drainedTasks) {
                        try {
                            drainedTask.v2().onSuccess();
                        } catch (Exception e) {
                            logger.error("unexpected error during [{}]", e, source);
                        }
                    }
                }
            });
        }
    }

    private RaftPing.PingResult doPing() {
        RaftPing.PingResult result = raftPing.ping(raftState.term(), configuredTargetNodes);

        assert resolvedTargetNodes.length() == result.discoveredNodes().length;
        // resolve arbitrary nodes to their true id etc.
        for (int i = 0; i < result.discoveredNodes().length; i++) {
            if (result.discoveredNodes()[i] != null) {
                resolvedTargetNodes.set(i, result.discoveredNodes()[i]);
            }
        }
        return result;
    }


}
