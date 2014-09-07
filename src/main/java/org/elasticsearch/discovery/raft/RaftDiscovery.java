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
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.ProcessedClusterStateNonMasterUpdateTask;
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
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.concurrent.ConcurrentCollections;
import org.elasticsearch.discovery.Discovery;
import org.elasticsearch.discovery.DiscoveryService;
import org.elasticsearch.discovery.DiscoverySettings;
import org.elasticsearch.discovery.InitialStateDiscoveryListener;
import org.elasticsearch.node.service.NodeService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class RaftDiscovery extends AbstractLifecycleComponent<Discovery> implements Discovery {

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
    private final RaftPing raftPing;
    private final Random random;

    private final AtomicBoolean initialStateSent = new AtomicBoolean();

    private DiscoveryNode localNode;

    private final CopyOnWriteArrayList<InitialStateDiscoveryListener> initialStateListeners = new CopyOnWriteArrayList<>();

    // TODO: make configurable
    private final TimeValue masterLossDelayTime = TimeValue.timeValueMillis(300);
    private final TimeValue reElectionDelayTime = TimeValue.timeValueMillis(300);
    private final TimeValue initialElectionDelay = TimeValue.timeValueSeconds(3);

    private RaftState raftState = new RaftState();

    private DiscoveryNode[] configuredTargetNodes;

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

        this.requestVoteAction = new RequestVoteAction(settings, transportService, clusterName, this, raftState);
        this.publishClusterState = new PublishClusterStateAction(settings, transportService, this, new NewClusterStateListener(), discoverySettings, clusterName, raftState);
        this.raftPing = new RaftPing(settings, transportService, this, clusterName, raftState, clusterService);


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
                threadPool.schedule(TimeValue.timeValueMillis(random.nextInt((int) reElectionDelayTime.millis())), ThreadPool.Names.GENERIC, new Runnable() {
                    @Override
                    public void run() {
                        // check again if we didn't become a follower in the mean time
                        boolean startElection = false;
                        synchronized (raftState) {
                            if (raftState.term() == electionTerm && raftState.role() == RaftState.RaftRole.CANDIDATE) {
                                raftState.term(raftState.term() + 1);
                                startElection = true;
                            }
                        }
                        if (startElection) {
                            requestVoteAction.performElection(electionTerm + 1, configuredTargetNodes);
                        }
                    }
                });
            }
        }
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

    void handleNewClusterStateFromMaster(long termForNewState, ClusterState newClusterState, final NewClusterStateListener.NewStateProcessed newStateProcessed) {
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
            logger.debug("received cluster state from [{}] of a lower term (expected >= [{}])",
                    newClusterState.nodes().masterNode(), raftState.term());
            // TODO: signal other master to step down?
            newStateProcessed.onNewClusterStateProcessed();
            return;
        }
        final ProcessClusterState processClusterState = new ProcessClusterState(termForNewState, newClusterState, newStateProcessed);
        processNewClusterStates.add(processClusterState);


        assert newClusterState.nodes().masterNode() != null : "received a cluster state without a master";
        assert !newClusterState.blocks().hasGlobalBlock(discoverySettings.getNoMasterBlock()) : "received a cluster state with a master block";

        clusterService.submitStateUpdateTask("raft-receive(from master [" + newClusterState.nodes().masterNode() + "] term [" + termForNewState + "])", Priority.URGENT, new ProcessedClusterStateNonMasterUpdateTask() {
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

                if (raftState.term() < stateToProcess.term) {
                    return currentState;
                }

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


                // check to see that we monitor the correct master of the cluster
                // TODO: renable
//                        if (masterFD.masterNode() == null || !masterFD.masterNode().equals(latestDiscoNodes.masterNode())) {
//                            masterFD.restart(latestDiscoNodes.masterNode(), "new cluster state received and we are monitoring the wrong master [" + masterFD.masterNode() + "]");
//                        }

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
                newStateProcessed.onNewClusterStateProcessed();
            }
        });
    }


    @Override
    protected void doStart() throws ElasticsearchException {
        Map<String, String> nodeAttributes = discoveryNodeService.buildAttributes();
        // note, we rely on the fact that its a new id each time we start, see FD and "kill -9" handling
        final String nodeId = DiscoveryService.generateNodeId(settings);
        localNode = new DiscoveryNode(settings.get("name"), nodeId, transportService.boundAddress().publishAddress(), nodeAttributes, version);
        threadPool.schedule(initialElectionDelay, ThreadPool.Names.GENERIC, new Runnable() {
            @Override
            public void run() {
                boolean startElection = false;
                synchronized (raftState) {
                    // TODO: not sure if the term should be 0 - what happens if we voted for someone who never became master?
                    if (raftState.term() == 0) {
                        raftState.term(1);
                        raftState.role(RaftState.RaftRole.CANDIDATE);
                        startElection = true;
                    }
                }
                if (startElection) {
                    requestVoteAction.performElection(1, configuredTargetNodes);
                }
            }
        });
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

    }

    @Override
    public void setAllocationService(AllocationService allocationService) {
        this.allocationService = allocationService;
    }

    @Override
    public void publish(ClusterState clusterState, AckListener ackListener) {
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

}
