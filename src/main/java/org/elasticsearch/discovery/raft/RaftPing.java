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
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.component.AbstractComponent;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.concurrent.AtomicArray;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

public class RaftPing extends AbstractComponent {

    public static final String ACTION_NAME = "internal:discovery/raft/ping";

    private final TransportService transportService;
    private final RaftDiscovery raftDiscovery;
    private final ClusterName clusterName;
    private final RaftState raftState;
    private final ClusterService clusterService;

    public RaftPing(Settings settings, TransportService transportService, RaftDiscovery raftDiscovery,
                    ClusterName clusterName, RaftState raftState, ClusterService clusterService) {
        super(settings);
        this.transportService = transportService;
        this.raftDiscovery = raftDiscovery;
        this.clusterName = clusterName;
        this.raftState = raftState;
        this.clusterService = clusterService;
        transportService.registerHandler(ACTION_NAME, new PingRequestHandler());
    }

    public void close() {
        transportService.removeHandler(ACTION_NAME);
    }

    public PingResult ping(long term, final DiscoveryNode[] targets) {
        final AtomicArray<PingResponse> responses = new AtomicArray<>(targets.length);
        final CountDownLatch latch = new CountDownLatch(targets.length);
        for (int i = 0; i < targets.length; i++) {
            // TODO: configurable timeout
            final int id = i;
            transportService.sendRequest(targets[id], ACTION_NAME, new PingRequest(clusterName, raftDiscovery.localNode()),
                    new TransportRequestOptions().withTimeout(300), new BaseTransportResponseHandler<PingResponse>() {

                        @Override
                        public PingResponse newInstance() {
                            return new PingResponse();
                        }

                        @Override
                        public void handleResponse(PingResponse response) {
                            responses.set(id, response);
                            latch.countDown();
                        }

                        @Override
                        public void handleException(TransportException exp) {
                            latch.countDown();
                            if (exp instanceof ConnectTransportException) {
                                // ok, not connected...
                                logger.trace("failed to connect to {}", exp, targets[id]);
                            } else if (exp.getCause() instanceof ElasticsearchIllegalStateException) {
                                logger.warn("ping to [{}] failed", exp, targets[id]);
                            } else {
                                logger.warn("failed to send ping to [{}]", exp, targets[id]);
                            }
                        }

                        @Override
                        public String executor() {
                            return null;
                        }
                    });
        }
        try {
            latch.wait();
        } catch (InterruptedException e) {
            // meh..
        }
        return new PingResult(term, responses.toArray(new PingResponse[targets.length]));
    }


    public static class PingResult {
        private final DiscoveryNode[] discoveredNodes;
        private final DiscoveryNode masterAdvice;
        private final long term;

        public PingResult(long term, PingResponse[] pingResponses) {
            this.term = term;
            ArrayList<DiscoveryNode> nodes = new ArrayList<>();
            DiscoveryNode discoveredMaster = null;
            DiscoveryNode discoveredVotedFor = null;
            long bestTermSeen = term; // ignore everything bellow this
            for (PingResponse ping : pingResponses) {
                if (ping == null) {
                    continue;
                }
                nodes.add(ping.node);
                if (ping.term > bestTermSeen) {
                    bestTermSeen = ping.term;
                    discoveredMaster = ping.master;
                    discoveredVotedFor = ping.votedFor;
                } else if (ping.term == bestTermSeen) {
                    // TODO: make this smarter and choose better candidates...
                    if (ping.master != null) {
                        discoveredMaster = ping.master;
                    }
                    if (ping.votedFor != null) {
                        discoveredVotedFor = ping.votedFor;
                    }
                }
            }
            discoveredNodes = nodes.toArray(new DiscoveryNode[nodes.size()]);
            masterAdvice = discoveredMaster != null ? discoveredMaster : discoveredVotedFor;
        }

        public DiscoveryNode[] discoveredNodes() {
            return discoveredNodes;
        }

        public DiscoveryNode masterAdvice() {
            return masterAdvice;
        }

        public long term() {
            return term;
        }
    }

    private PingResponse handleIncomingPing(PingRequest request) {
        if (!clusterName.equals(request.clusterName)) {
            throw new ElasticsearchIllegalStateException("received ping from another cluster. got [" + request.clusterName.value()
                    + "]. expected [" + clusterName.value() + "]");
        }
        DiscoveryNode masterNode = clusterService.state().nodes().getMasterNode();
        synchronized (raftState) {
            return new PingResponse(raftState.term(), raftDiscovery.localNode(), masterNode, raftState.votedFor());
        }
    }


    private class PingRequestHandler extends BaseTransportRequestHandler<PingRequest> {

        @Override
        public PingRequest newInstance() {
            return new PingRequest();
        }

        @Override
        public void messageReceived(PingRequest request, TransportChannel channel) throws Exception {
            channel.sendResponse(handleIncomingPing(request));
        }

        @Override
        public String executor() {
            return ThreadPool.Names.SAME;
        }
    }


    private static class PingRequest extends TransportRequest {
        private long term;
        private ClusterName clusterName;
        private DiscoveryNode sourceNode;

        public PingRequest(ClusterName clusterName, DiscoveryNode sourceNode) {
            this.clusterName = clusterName;
            this.sourceNode = sourceNode;
        }

        public PingRequest() {
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            clusterName = ClusterName.readClusterName(in);
            sourceNode = DiscoveryNode.readNode(in);
            term = in.readLong();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            clusterName.writeTo(out);
            sourceNode.writeTo(out);
            out.writeLong(term);
        }
    }

    private static class PingResponse extends TransportResponse {
        private long term;
        private DiscoveryNode node;

        @Nullable
        private DiscoveryNode master;

        @Nullable
        private DiscoveryNode votedFor;

        private PingResponse(long term, DiscoveryNode node, @Nullable DiscoveryNode master, @Nullable DiscoveryNode votedFor) {
            this.term = term;
            this.node = node;
            this.master = master;
            this.votedFor = votedFor;
        }

        public PingResponse() {
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            term = in.readLong();
            node = DiscoveryNode.readNode(in);
            if (in.readBoolean()) {
                master = DiscoveryNode.readNode(in);
            } else {
                master = null;
            }
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
            node.writeTo(out);
            if (master != null) {
                out.writeBoolean(true);
                master.writeTo(out);
            } else {
                out.writeBoolean(false);
            }
            if (votedFor != null) {
                out.writeBoolean(true);
                votedFor.writeTo(out);
            } else {
                out.writeBoolean(false);
            }
        }
    }

}
