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

import org.elasticsearch.ElasticsearchIllegalStateException;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.util.concurrent.ConcurrentCollections;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public class RequestVoteAction extends AbstractComponent {

    public static final String ACTION_NAME = "internal:discovery/raft/requestVote";

    public static final String SETTING_RAFT_ELECTION_TIMEOUT = "discovery.raft.election.timeout";

    private final TransportService transportService;
    private final ClusterName clusterName;
    private final RaftDiscovery raftDiscovery;
    private final RaftState raftState;
    private final TimeValue electionTimeout;

    public RequestVoteAction(Settings settings, TransportService transportService, ClusterName clusterName, RaftDiscovery raftDiscovery, RaftState raftState) {
        super(settings);
        this.transportService = transportService;
        this.clusterName = clusterName;
        this.raftDiscovery = raftDiscovery;
        this.raftState = raftState;
        this.electionTimeout = settings.getAsTime(SETTING_RAFT_ELECTION_TIMEOUT, TimeValue.timeValueSeconds(3));
        transportService.registerHandler(ACTION_NAME, new VoteRequestHandler());
    }

    public void close() {
        transportService.removeHandler(ACTION_NAME);
    }

    public void performElection(long forTerm, DiscoveryNode[] fromNodes, int neededVotes) {
        boolean selfVoteSucceeded = false;
        long lastClusterStateTerm = -1;
        long lastClusterStateVersion = -1;
        synchronized (raftState) {
            if (raftState.votedFor() == null || raftState.term() < forTerm) {
                raftState.term(forTerm);
                raftState.votedFor(raftDiscovery.localNode());
                lastClusterStateTerm = raftState.lastClusterStateTerm();
                lastClusterStateVersion = raftState.lastClusterStateVersion();
                selfVoteSucceeded = true;
            }
        }
        if (!selfVoteSucceeded) {
            logger.debug("failing election: election started for term [{}] but we have already voted or term has been changed", forTerm);
            raftDiscovery.handleElectionLoss(forTerm);
        }
        logger.debug("starting election for term [{}]", forTerm);
        Election election = new Election(forTerm, fromNodes.length, neededVotes);
        for (DiscoveryNode node : fromNodes) {
            try {
                // TODO: disconnect/ lookup in already connected nodes etc.
                transportService.connectToNode(node);
                transportService.sendRequest(node, ACTION_NAME,
                        new VoteRequest(forTerm, lastClusterStateTerm, lastClusterStateVersion, raftDiscovery.localNode(), clusterName),
                        TransportRequestOptions.options().withTimeout(electionTimeout), election);
            } catch (Exception e) {
                election.handleSendingException(e);
            }
        }
    }

    private VoteResponse maybeVote(VoteRequest request) {
        DiscoveryNode votedFor = null;
        synchronized (raftState) {
            if (request.forTerm < raftState.term()) {
                logger.trace("received request for term [{}] from {}, but our term is newer: [{}]",
                        request.forTerm, request.candidateNode, raftState.term());
            } else if (raftState.lastClusterStateTerm() > request.lastClusterStateTerm ||
                    (raftState.lastClusterStateTerm() == request.lastClusterStateTerm &&
                            raftState.lastClusterStateVersion() > request.lastClusterStateVersion)) {
                logger.trace("received request for term [{}] from {}, but candidate CS is too old. got term [{}]/version [{}]. our term [{}]/version [{}]",
                        request.forTerm, request.candidateNode,
                        request.lastClusterStateTerm, request.lastClusterStateVersion,
                        raftState.lastClusterStateTerm(), raftState.lastClusterStateVersion()
                );

                // we do have to update the term (its equal or higher)
                raftState.term(request.forTerm);

            } else {
                if (raftState.votedFor() == null) {
                    logger.debug("election term [{}] - voting for {}. got CS term [{}]/version [{}]. our CS term [{}]/version [{}].",
                            request.forTerm, request.candidateNode,
                            request.lastClusterStateTerm, request.lastClusterStateVersion,
                            raftState.lastClusterStateTerm(), raftState.lastClusterStateVersion());

                    raftState.votedFor(request.candidateNode);
                } else {
                    logger.trace("received request for term [{}] from {}, responding with previous vote for {}",
                            request.forTerm, request.candidateNode, raftState.votedFor());
                }
                votedFor = raftState.votedFor();
            }
        }

        return new VoteResponse(request.forTerm, raftDiscovery.localNode(), votedFor);
    }

    private class Election extends BaseTransportResponseHandler<VoteResponse> {
        private final long term;
        private final long nodeCount;
        private final long votesNeeded;

        private final AtomicInteger votesWon = new AtomicInteger();
        private final AtomicInteger requestsCompleted = new AtomicInteger();

        // TODO: think this very carefully - we need this to make sure we have the right node ids and
        // during start up
        private final BlockingQueue<DiscoveryNode> activeNodes = ConcurrentCollections.newBlockingQueue();

        public Election(long term, long nodeCount, long votesNeeded) {
            this.term = term;
            this.nodeCount = nodeCount;
            this.votesNeeded = votesNeeded;
        }

        private void finishElection() {
            if (votesWon.get() >= votesNeeded) {
                logger.debug("won election for term [{}] with [{}] votes ([{}] needed)", term, votesWon.get(), votesNeeded);
                List<DiscoveryNode> activeNodesList = new ArrayList<>();
                activeNodes.drainTo(activeNodesList);
                raftDiscovery.handleElectionVictory(term, activeNodesList);
            } else {
                logger.debug("lost election for term [{}] with [{}] votes ([{}] needed)", term, votesWon.get(), votesNeeded);
                raftDiscovery.handleElectionLoss(term);
            }
        }

        @Override
        public VoteResponse newInstance() {
            return new VoteResponse();
        }

        @Override
        public void handleResponse(VoteResponse response) {
            assert response.term == term;

            activeNodes.add(response.voter);

            if (raftDiscovery.localNode().equals(response.votedFor)) {
                long currentVotes = votesWon.incrementAndGet();
                logger.debug("received a vote from {} for term [{}]. current votes: [{}]. needed votes [{}]",
                        response.voter, term, currentVotes, votesNeeded);
            } else {
                logger.trace("failed to receive the vote of {} for term [{}]. it voted for [{}]",
                        response.voter, term, response.votedFor);
            }

            if (requestsCompleted.incrementAndGet() == nodeCount) {
                finishElection();
            }
        }

        @Override
        public void handleException(TransportException exp) {
            logger.debug("failed to get a response for vote request", exp);
            if (requestsCompleted.incrementAndGet() == nodeCount) {
                finishElection();
            }
        }

        public void handleSendingException(Exception exp) {
            logger.debug("failed to send vote request", exp);
            if (requestsCompleted.incrementAndGet() == nodeCount) {
                finishElection();
            }
        }

        @Override
        public String executor() {
            return ThreadPool.Names.SAME;
        }
    }

    private static class Vote {
        public final long term;
        public final DiscoveryNode votedFor;

        private Vote(long term, DiscoveryNode votedFor) {
            this.term = term;
            this.votedFor = votedFor;
        }
    }

    private class VoteRequestHandler extends BaseTransportRequestHandler<VoteRequest> {

        @Override
        public VoteRequest newInstance() {
            return new VoteRequest();
        }

        @Override
        public void messageReceived(VoteRequest request, final TransportChannel channel) throws Exception {
            if (!request.clusterName.equals(clusterName)) {
                throw new ElasticsearchIllegalStateException("vote requested with cluster name [" + request.clusterName + "], but I'm part of cluster [" + clusterName + "]");
            }
            channel.sendResponse(maybeVote(request));
        }

        @Override
        public String executor() {
            return ThreadPool.Names.SAME;
        }
    }


    public static class VoteRequest extends TransportRequest {

        private long forTerm;
        private DiscoveryNode candidateNode;
        private ClusterName clusterName;

        private long lastClusterStateTerm;
        private long lastClusterStateVersion;

        // TODO: add cluster state version

        public VoteRequest(long forTerm, long lastClusterStateTerm, long lastClusterStateVersion, DiscoveryNode candidateNode, ClusterName clusterName) {
            this.forTerm = forTerm;
            this.candidateNode = candidateNode;
            this.clusterName = clusterName;
            this.lastClusterStateTerm = lastClusterStateTerm;
            this.lastClusterStateVersion = lastClusterStateVersion;
        }

        private VoteRequest() {

        }

        public static VoteRequest readVoteRequest(StreamInput in) throws IOException {
            VoteRequest voteRequest = new VoteRequest();
            voteRequest.readFrom(in);
            return voteRequest;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            forTerm = in.readLong();
            lastClusterStateTerm = in.readLong();
            lastClusterStateVersion = in.readLong();
            candidateNode = DiscoveryNode.readNode(in);
            clusterName = ClusterName.readClusterName(in);
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeLong(forTerm);
            out.writeLong(lastClusterStateTerm);
            out.writeLong(lastClusterStateVersion);
            candidateNode.writeTo(out);
            clusterName.writeTo(out);
        }
    }

    public static class VoteResponse extends TransportResponse {
        private long term;
        private DiscoveryNode voter;
        private DiscoveryNode votedFor;

        /**
         * @param term     the term of the vote
         * @param voter    the sending node
         * @param votedFor the vote of the sending node for the relevant term, or null
         *                 if the sending nodes' term is higher then requested (implying no vote)
         */
        public VoteResponse(long term, DiscoveryNode voter, @Nullable DiscoveryNode votedFor) {
            this.term = term;
            this.voter = voter;
            this.votedFor = votedFor;
        }

        private VoteResponse() {

        }

        public static VoteResponse readVoteResponse(StreamInput in) throws IOException {
            VoteResponse voteResponse = new VoteResponse();
            voteResponse.readFrom(in);
            return voteResponse;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            term = in.readLong();
            voter = DiscoveryNode.readNode(in);
            if (in.readBoolean()) {
                votedFor = DiscoveryNode.readNode(in);
            } else {
                votedFor = null;
            }
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeLong(term);
            voter.writeTo(out);
            if (votedFor == null) {
                out.writeBoolean(false);
            } else {
                out.writeBoolean(true);
                votedFor.writeTo(out);
            }
        }
    }
}
