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

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.Version;
import org.elasticsearch.action.IndicesRequest;
import org.elasticsearch.action.ShardOperationFailedException;
import org.elasticsearch.action.support.ActionFilter;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.DefaultShardOperationFailedException;
import org.elasticsearch.action.support.PlainActionFuture;
import org.elasticsearch.action.support.broadcast.BroadcastRequest;
import org.elasticsearch.action.support.broadcast.BroadcastResponse;
import org.elasticsearch.action.support.broadcast.BroadcastShardOperationFailedException;
import org.elasticsearch.action.support.indices.BaseBroadcastByNodeRequest;
import org.elasticsearch.action.support.indices.BaseBroadcastByNodeResponse;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlock;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.block.ClusterBlocks;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.routing.*;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.DummyTransportAddress;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.test.cluster.TestClusterService;
import org.elasticsearch.test.transport.CapturingTransport;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportChannel;
import org.elasticsearch.transport.TransportResponse;
import org.elasticsearch.transport.TransportResponseOptions;
import org.elasticsearch.transport.TransportService;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.object.HasToString.hasToString;

public class TransportBroadcastByNodeActionTests extends ESTestCase {
    private static final String TEST_INDEX = "test-index";
    private static final String TEST_CLUSTER = "test-cluster";
    private static ThreadPool THREAD_POOL;

    private TestClusterService clusterService;
    private CapturingTransport transport;
    private TransportService transportService;

    private TestTransportBroadcastByNodeAction action;

    public static class Request extends BroadcastRequest<Request> {
        public Request() {
        }

        public Request(String[] indices) {
            super(indices);
        }
    }

    public static class Response extends BroadcastResponse {
        public Response() {
        }

        public Response(int totalShards, int successfulShards, int failedShards, List<? extends ShardOperationFailedException> shardFailures) {
            super(totalShards, successfulShards, failedShards, shardFailures);
        }
    }

    public static class BroadcastByNodeRequest extends BaseBroadcastByNodeRequest<Request> {
        public BroadcastByNodeRequest() {
        }

        public BroadcastByNodeRequest(String nodeId, Request request, List<ShardRouting> shards) {
            super(nodeId, request, shards);
        }

        @Override
        protected Request newRequest() {
            return new Request();
        }
    }

    public static class BroadcastByNodeResponse extends BaseBroadcastByNodeResponse {
        public BroadcastByNodeResponse() {
        }

        public BroadcastByNodeResponse(String nodeId, int totalShards, int successfulShards, List<BroadcastShardOperationFailedException> exceptions) {
            super(nodeId, totalShards, successfulShards, exceptions);
        }
    }

    class TestTransportBroadcastByNodeAction extends TransportBroadcastByNodeAction<Request, Response, BroadcastByNodeRequest, BroadcastByNodeResponse, Boolean> {
        private final Map<ShardRouting, Object> shards = new HashMap<>();

        public TestTransportBroadcastByNodeAction(Settings settings, TransportService transportService, ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver, Class<Request> request, Class<BroadcastByNodeRequest> nodeBroadcastRequest, String executor) {
            super(settings, "indices:admin/test", THREAD_POOL, TransportBroadcastByNodeActionTests.this.clusterService, transportService, actionFilters, indexNameExpressionResolver, request, nodeBroadcastRequest, executor);
        }

        @Override
        protected Response newResponse(Request request, int totalShards, int successfulShards, int failedShards, List<BroadcastByNodeResponse> broadcastByNodeResponses, List<DefaultShardOperationFailedException> shardFailures) {
            return new Response(totalShards, successfulShards, failedShards, shardFailures);
        }

        @Override
        protected BroadcastByNodeRequest newNodeRequest(String nodeId, Request request, List<ShardRouting> shards) {
            return new BroadcastByNodeRequest(nodeId, request, shards);
        }

        @Override
        protected BroadcastByNodeResponse newNodeResponse() {
            return new BroadcastByNodeResponse();
        }

        @Override
        protected BroadcastByNodeResponse newNodeResponse(String nodeId, int totalShards, int successfulShards, List<Boolean> booleans, List<BroadcastShardOperationFailedException> exceptions) {
            return new BroadcastByNodeResponse(nodeId, totalShards, successfulShards, exceptions);
        }

        @Override
        protected Boolean shardOperation(BroadcastByNodeRequest request, ShardRouting shardRouting) {
            if (rarely()) {
                shards.put(shardRouting, Boolean.TRUE);
                return Boolean.TRUE;
            } else {
                ElasticsearchException e = new ElasticsearchException("operation failed");
                shards.put(shardRouting, e);
                throw e;
            }
        }

        @Override
        protected ShardsIterator shards(ClusterState clusterState, Request request, String[] concreteIndices) {
            return clusterState.routingTable().allActiveShards(new String[]{TEST_INDEX}, true);
        }

        @Override
        protected ClusterBlockException checkGlobalBlock(ClusterState state, Request request) {
            return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_WRITE);
        }

        @Override
        protected ClusterBlockException checkRequestBlock(ClusterState state, Request request, String[] concreteIndices) {
            return state.blocks().indicesBlockedException(ClusterBlockLevel.METADATA_WRITE, concreteIndices);
        }

        public Map<ShardRouting, Object> getResults() {
            return shards;
        }
    }

    class MyResolver extends IndexNameExpressionResolver {
        public MyResolver() {
            super(Settings.EMPTY);
        }

        @Override
        public String[] concreteIndices(ClusterState state, IndicesRequest request) {
            return request.indices();
        }
    }

    @BeforeClass
    public static void startThreadPool() {
        THREAD_POOL = new ThreadPool(TransportBroadcastByNodeActionTests.class.getSimpleName());
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        transport = new CapturingTransport();
        clusterService = new TestClusterService(THREAD_POOL);
        transportService = new TransportService(transport, THREAD_POOL);
        transportService.start();
        setClusterState(clusterService, TEST_INDEX);
        action = new TestTransportBroadcastByNodeAction(
                Settings.EMPTY,
                transportService,
                new ActionFilters(new HashSet<ActionFilter>()),
                new MyResolver(),
                Request.class,
                BroadcastByNodeRequest.class,
                ThreadPool.Names.SAME
        );
    }

    void setClusterState(TestClusterService clusterService, String index) {
        int numberOfNodes = randomIntBetween(3, 5);
        DiscoveryNodes.Builder discoBuilder = DiscoveryNodes.builder();
        IndexRoutingTable.Builder indexRoutingTable = IndexRoutingTable.builder(index);

        int shardIndex = -1;
        for (int i = 0; i < numberOfNodes; i++) {
            final DiscoveryNode node = newNode(i);
            discoBuilder = discoBuilder.put(node);
            int numberOfShards = randomIntBetween(0, 10);
            for (int j = 0; j < numberOfShards; j++) {
                final ShardId shardId = new ShardId(index, ++shardIndex);
                ShardRouting shard = TestShardRouting.newShardRouting(index, shardId.getId(), node.id(), true, ShardRoutingState.STARTED, 1);
                IndexShardRoutingTable.Builder indexShard = new IndexShardRoutingTable.Builder(shardId);
                indexShard.addShard(shard);
                indexRoutingTable.addIndexShard(indexShard.build());
            }
        }
        discoBuilder.localNodeId(newNode(0).id());
        discoBuilder.masterNodeId(newNode(numberOfNodes - 1).id());
        ClusterState.Builder stateBuilder = ClusterState.builder(new ClusterName(TEST_CLUSTER));
        stateBuilder.nodes(discoBuilder);
        stateBuilder.routingTable(RoutingTable.builder().add(indexRoutingTable.build()).build());
        ClusterState clusterState = stateBuilder.build();
        clusterService.setState(clusterState);
    }

    static DiscoveryNode newNode(int nodeId) {
        return new DiscoveryNode("node_" + nodeId, DummyTransportAddress.INSTANCE, Version.CURRENT);
    }

    @AfterClass
    public static void destroyThreadPool() {
        ThreadPool.terminate(THREAD_POOL, 30, TimeUnit.SECONDS);
        // since static must set to null to be eligible for collection
        THREAD_POOL = null;
    }

    public void testGlobalBlock() {
        Request request = new Request(new String[]{TEST_INDEX});
        PlainActionFuture<Response> listener = new PlainActionFuture<>();

        ClusterBlocks.Builder block = ClusterBlocks.builder()
                .addGlobalBlock(new ClusterBlock(1, "", false, true, RestStatus.SERVICE_UNAVAILABLE, ClusterBlockLevel.ALL));
        clusterService.setState(ClusterState.builder(clusterService.state()).blocks(block));
        try {
            action.new AsyncAction(request, listener).start();
            fail("expected ClusterBlockException");
        } catch (ClusterBlockException expected) {

        }
    }

    public void testRequestBlock() {
        Request request = new Request(new String[]{TEST_INDEX});
        PlainActionFuture<Response> listener = new PlainActionFuture<>();

        ClusterBlocks.Builder block = ClusterBlocks.builder()
                .addIndexBlock(TEST_INDEX, new ClusterBlock(1, "test-block", false, true, RestStatus.SERVICE_UNAVAILABLE, ClusterBlockLevel.ALL));
        clusterService.setState(ClusterState.builder(clusterService.state()).blocks(block));
        try {
            action.new AsyncAction(request, listener).start();
            fail("expected ClusterBlockException");
        } catch (ClusterBlockException expected) {

        }
    }

    public void testOneRequestIsSentToEachNodeHoldingAShard() {
        Request request = new Request(new String[]{TEST_INDEX});
        PlainActionFuture<Response> listener = new PlainActionFuture<>();

        action.new AsyncAction(request, listener).start();
        Map<String, List<CapturingTransport.CapturedRequest>> capturedRequests = transport.capturedRequestsByTargetNode();

        ShardsIterator shardIt = clusterService.state().routingTable().allActiveShards(new String[]{TEST_INDEX}, true);
        Set<String> set = new HashSet<>();
        for (ShardRouting shard : shardIt.asUnordered()) {
            set.add(shard.currentNodeId());
        }

        // check a request was sent to the right number of nodes
        assertEquals(set.size(), capturedRequests.size());

        // check requests were sent to the right nodes
        assertEquals(set, capturedRequests.keySet());
        for (Map.Entry<String, List<CapturingTransport.CapturedRequest>> entry : capturedRequests.entrySet()) {
            // check one request was sent to each node
            assertEquals(1, entry.getValue().size());
        }
    }

    public void testOperationExecution() throws Exception {
        ShardsIterator shardIt = clusterService.state().routingTable().allActiveShards(new String[]{TEST_INDEX}, true);
        Set<ShardRouting> shards = new HashSet<>();
        String nodeId = shardIt.asUnordered().iterator().next().currentNodeId();
        for (ShardRouting shard : shardIt.asUnordered()) {
            if (nodeId.equals(shard.currentNodeId())) {
                shards.add(shard);
            }
        }
        final TransportBroadcastByNodeAction.BroadcastByNodeTransportRequestHandler handler =
                action.new BroadcastByNodeTransportRequestHandler();

        TestTransportChannel channel = new TestTransportChannel();
        handler.messageReceived(new BroadcastByNodeRequest(nodeId, new Request(), new ArrayList<>(shards)), channel);

        // check the operation was executed only on the expected shards
        assertEquals(shards, action.getResults().keySet());

        TransportResponse response = channel.getCapturedResponse();
        assertTrue(response instanceof BroadcastByNodeResponse);
        BroadcastByNodeResponse broadcastByNodeResponse = (BroadcastByNodeResponse)response;

        // check the operation was executed on the correct node
        assertEquals("node id", nodeId, broadcastByNodeResponse.getNodeId());

        int successfulShards = 0;
        int failedShards = 0;
        for (Object result : action.getResults().values()) {
            if (!(result instanceof ElasticsearchException)) {
                successfulShards++;
            } else {
                failedShards++;
            }
        }

        // check the operation results
        assertEquals("successful shards", successfulShards, broadcastByNodeResponse.getSuccessfulShards());
        assertEquals("total shards", action.getResults().size(), broadcastByNodeResponse.getTotalShards());
        assertEquals("failed shards", failedShards, broadcastByNodeResponse.getExceptions().size());
        for (BroadcastShardOperationFailedException exception : broadcastByNodeResponse.getExceptions()) {
            assertThat(exception.getMessage(), is("operation indices:admin/test failed"));
            assertThat(exception, hasToString(containsString("operation failed")));
        }
    }

    public void testResultAggregation() throws ExecutionException, InterruptedException {
        Request request = new Request(new String[]{TEST_INDEX});
        PlainActionFuture<Response> listener = new PlainActionFuture<>();

        action.new AsyncAction(request, listener).start();
        Map<String, List<CapturingTransport.CapturedRequest>> capturedRequests = transport.capturedRequestsByTargetNode();
        transport.clear();

        ShardsIterator shardIt = clusterService.state().getRoutingTable().allActiveShards(new String[]{TEST_INDEX}, true);
        Map<String, List<ShardRouting>> map = new HashMap<>();
        for (ShardRouting shard : shardIt.asUnordered()) {
            if (!map.containsKey(shard.currentNodeId())) {
                map.put(shard.currentNodeId(), new ArrayList<ShardRouting>());
            }
            map.get(shard.currentNodeId()).add(shard);
        }

        int totalShards = 0;
        int totalSuccessfulShards = 0;
        int totalFailedShards = 0;
        for (Map.Entry<String, List<CapturingTransport.CapturedRequest>> entry : capturedRequests.entrySet()) {
            List<BroadcastShardOperationFailedException> exceptions = new ArrayList<>();
            long requestId = entry.getValue().get(0).requestId;
            if (rarely()) {
                // simulate node failure
                totalShards += map.get(entry.getKey()).size();
                totalFailedShards += map.get(entry.getKey()).size();
                transport.handleResponse(requestId, new Exception());
            } else {
                List<ShardRouting> shards = map.get(entry.getKey());
                int successfulShards = 0;
                for (ShardRouting shard : shards) {
                    totalShards++;
                    if (rarely()) {
                        // simulate operation failure
                        totalFailedShards++;
                        exceptions.add(new BroadcastShardOperationFailedException(shard.shardId(), "operation indices:admin/test failed"));
                    } else {
                        successfulShards++;
                    }
                }
                totalSuccessfulShards += successfulShards;
                BroadcastByNodeResponse broadcastByNodeResponse = new BroadcastByNodeResponse(entry.getKey(), shards.size(), successfulShards, exceptions);
                transport.handleResponse(requestId, broadcastByNodeResponse);
            }
        }

        Response response = listener.get();
        assertEquals("total shards", totalShards, response.getTotalShards());
        assertEquals("successful shards", totalSuccessfulShards, response.getSuccessfulShards());
        assertEquals("failed shards", totalFailedShards, response.getFailedShards());
        assertEquals("accumulated exceptions", totalFailedShards, response.getShardFailures().length);
    }

    public class TestTransportChannel implements TransportChannel {
        private TransportResponse capturedResponse;

        public TransportResponse getCapturedResponse() {
            return capturedResponse;
        }

        @Override
        public String action() {
            return null;
        }

        @Override
        public String getProfileName() {
            return "";
        }

        @Override
        public void sendResponse(TransportResponse response) throws IOException {
            capturedResponse = response;
        }

        @Override
        public void sendResponse(TransportResponse response, TransportResponseOptions options) throws IOException {
        }

        @Override
        public void sendResponse(Throwable error) throws IOException {
        }
    }
}