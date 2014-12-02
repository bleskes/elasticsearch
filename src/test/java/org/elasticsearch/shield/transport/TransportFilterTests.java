/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */

package org.elasticsearch.shield.transport;

import com.google.common.collect.ImmutableSet;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.inject.multibindings.MapBinder;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.*;
import org.junit.Test;
import org.mockito.InOrder;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.elasticsearch.test.ElasticsearchIntegrationTest.ClusterScope;
import static org.elasticsearch.test.ElasticsearchIntegrationTest.Scope.SUITE;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 *
 */
@ClusterScope(scope = SUITE, numDataNodes = 0)
public class TransportFilterTests extends ElasticsearchIntegrationTest {

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return ImmutableSettings.settingsBuilder()
                .put("plugins.load_classpath_plugins", false)
                .put("plugin.types", InternalPlugin.class.getName())
                .put(TransportModule.TRANSPORT_SERVICE_TYPE_KEY, SecuredTransportService.class.getName())
                .build();
    }

    @Test
    public void test() throws Exception {
        String source = internalCluster().startNode();
        DiscoveryNode sourceNode = internalCluster().getInstance(ClusterService.class, source).localNode();
        TransportService sourceService = internalCluster().getInstance(TransportService.class, source);

        String target = internalCluster().startNode();
        DiscoveryNode targetNode = internalCluster().getInstance(ClusterService.class, target).localNode();
        TransportService targetService = internalCluster().getInstance(TransportService.class, target);

        CountDownLatch latch = new CountDownLatch(2);
        targetService.registerHandler("_action", new RequestHandler(new Response("trgt_to_src"), latch));
        sourceService.sendRequest(targetNode, "_action", new Request("src_to_trgt"), new ResponseHandler(new Response("trgt_to_src"), latch));
        await(latch);

        latch = new CountDownLatch(2);
        sourceService.registerHandler("_action", new RequestHandler(new Response("src_to_trgt"), latch));
        targetService.sendRequest(sourceNode, "_action", new Request("trgt_to_src"), new ResponseHandler(new Response("src_to_trgt"), latch));
        await(latch);

        ServerTransportFilters sourceFilters = internalCluster().getInstance(ServerTransportFilters.class, source);
        ServerTransportFilter sourceServerFilter = sourceFilters.getTransportFilterForProfile("default");
        ClientTransportFilter sourceClientFilter = internalCluster().getInstance(ClientTransportFilter.class, source);
        ServerTransportFilters targetFilters = internalCluster().getInstance(ServerTransportFilters.class, target);
        ServerTransportFilter targetServerFilter = targetFilters.getTransportFilterForProfile("default");
        ClientTransportFilter targetClientFilter = internalCluster().getInstance(ClientTransportFilter.class, target);
        InOrder inOrder = inOrder(sourceServerFilter, sourceClientFilter, targetServerFilter, targetClientFilter);
        inOrder.verify(sourceClientFilter).outbound("_action", new Request("src_to_trgt"));
        inOrder.verify(targetServerFilter).inbound("_action", new Request("src_to_trgt"));
        inOrder.verify(targetClientFilter).outbound("_action", new Request("trgt_to_src"));
        inOrder.verify(sourceServerFilter).inbound("_action", new Request("trgt_to_src"));
    }

    public static class InternalPlugin extends AbstractPlugin {

        @Override
        public String name() {
            return "test-transport-filter";
        }

        @Override
        public String description() {
            return "";
        }

        @Override
        public Collection<Class<? extends Module>> modules() {
            return ImmutableSet.<Class<? extends Module>>of(TestTransportFilterModule.class);
        }
    }

    public static class TestTransportFilterModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(ClientTransportFilter.class).toInstance(mock(ClientTransportFilter.class));

            // bind all to our dummy impls
            MapBinder<String, ServerTransportFilter> mapBinder = MapBinder.newMapBinder(binder(), String.class, ServerTransportFilter.class);
            mapBinder.addBinding(ServerTransportFilters.SERVER_TRANSPORT_FILTER_TRANSPORT_CLIENT).toInstance(mock(ServerTransportFilter.class));
            mapBinder.addBinding(ServerTransportFilters.SERVER_TRANSPORT_FILTER_AUTHENTICATE_ONLY).toInstance(mock(ServerTransportFilter.class));
            mapBinder.addBinding(ServerTransportFilters.SERVER_TRANSPORT_FILTER_AUTHENTICATE_REJECT_INTERNAL_ACTIONS).toInstance(mock(ServerTransportFilter.class));
            bind(ServerTransportFilters.class).asEagerSingleton();
        }
    }

    static class Request extends TransportRequest {

        private String msg;

        Request() {
        }

        Request(String msg) {
            this.msg = msg;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            msg = in.readString();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeString(msg);
        }

        @Override
        public String toString() {
            return msg;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Request request = (Request) o;

            if (!msg.equals(request.msg)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return msg.hashCode();
        }
    }

    static class Response extends TransportResponse {

        private String msg;

        Response() {
        }

        Response(String msg) {
            this.msg = msg;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            msg = in.readString();
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeString(msg);
        }

        @Override
        public String toString() {
            return msg;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Response response = (Response) o;

            if (!msg.equals(response.msg)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return msg.hashCode();
        }
    }

    static class RequestHandler implements TransportRequestHandler<Request> {

        private final Response response;
        private final CountDownLatch latch;

        RequestHandler(Response response, CountDownLatch latch) {
            this.response = response;
            this.latch = latch;
        }

        @Override
        public Request newInstance() {
            return new Request();
        }

        @Override
        public void messageReceived(Request request, TransportChannel channel) throws Exception {
            channel.sendResponse(response);
            latch.countDown();
        }

        @Override
        public String executor() {
            return ThreadPool.Names.SAME;
        }

        @Override
        public boolean isForceExecution() {
            return false;
        }
    }

    class ResponseHandler implements TransportResponseHandler<Response> {

        private final Response response;
        private final CountDownLatch latch;

        ResponseHandler(Response response, CountDownLatch latch) {
            this.response = response;
            this.latch = latch;
        }

        @Override
        public Response newInstance() {
            return new Response();
        }

        @Override
        public void handleResponse(Response response) {
            assertThat(response, equalTo(this.response));
            latch.countDown();
        }

        @Override
        public void handleException(TransportException exp) {
            logger.error("execution of request failed", exp);
            fail("execution of request failed");
        }

        @Override
        public String executor() {
            return ThreadPool.Names.SAME;
        }
    }

    static void await(CountDownLatch latch) throws Exception {
        if (!latch.await(5, TimeUnit.SECONDS)) {
            fail("waiting too long for request");
        }
    }
}
