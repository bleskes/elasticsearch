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

package org.elasticsearch.shield.transport.filter;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.network.NetworkModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.http.HttpServerTransport;
import org.elasticsearch.test.ESIntegTestCase.ClusterScope;
import org.elasticsearch.test.ESIntegTestCase.Scope;
import org.elasticsearch.test.ShieldIntegTestCase;
import org.elasticsearch.transport.Transport;
import org.junit.BeforeClass;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

// no client nodes, no transport clients, as they all get rejected on network connections
@ClusterScope(scope = Scope.SUITE, numDataNodes = 0, numClientNodes = 0, transportClientRatio = 0.0)
public class IpFilteringIntegrationTests extends ShieldIntegTestCase {
    private static int randomClientPort;

    @BeforeClass
    public static void getRandomPort() {
        randomClientPort = randomIntBetween(49000, 65500); // ephemeral port
    }

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        String randomClientPortRange = randomClientPort + "-" + (randomClientPort+100);
        return Settings.builder().put(super.nodeSettings(nodeOrdinal))
                .put(NetworkModule.HTTP_ENABLED.getKey(), true)
                .put("transport.profiles.client.port", randomClientPortRange)
                // make sure this is "localhost", no matter if ipv4 or ipv6, but be consistent
                .put("transport.profiles.client.bind_host", "localhost")
                .put("transport.profiles.client.xpack.security.filter.deny", "_all")
                .put(IPFilter.TRANSPORT_FILTER_DENY_SETTING.getKey(), "_all")
                .build();
    }

    public void testThatIpFilteringIsIntegratedIntoNettyPipelineViaHttp() throws Exception {
        TransportAddress transportAddress =
                randomFrom(internalCluster().getDataNodeInstance(HttpServerTransport.class).boundAddress().boundAddresses());
        assertThat(transportAddress, is(instanceOf(InetSocketTransportAddress.class)));
        InetSocketTransportAddress inetSocketTransportAddress = (InetSocketTransportAddress) transportAddress;

        try (Socket socket = new Socket()){
            trySocketConnection(socket, inetSocketTransportAddress.address());
            assertThat(socket.isClosed(), is(true));
        }
    }

    public void testThatIpFilteringIsNotAppliedForDefaultTransport() throws Exception {
        Client client = internalCluster().transportClient();
        assertGreenClusterState(client);
    }

    public void testThatIpFilteringIsAppliedForProfile() throws Exception {
        try (Socket socket = new Socket()){
            trySocketConnection(socket, new InetSocketAddress(InetAddress.getLoopbackAddress(), getProfilePort("client")));
            assertThat(socket.isClosed(), is(true));
        }
    }

    private void trySocketConnection(Socket socket, InetSocketAddress address) throws IOException {
        logger.info("connecting to {}", address);
        socket.connect(address, 500);

        assertThat(socket.isConnected(), is(true));
        try (OutputStream os = socket.getOutputStream()) {
            os.write("fooooo".getBytes(StandardCharsets.UTF_8));
            os.flush();
        }
    }

    private static int getProfilePort(String profile) {
        TransportAddress transportAddress =
                randomFrom(internalCluster().getInstance(Transport.class).profileBoundAddresses().get(profile).boundAddresses());
        assert transportAddress instanceof InetSocketTransportAddress;
        return ((InetSocketTransportAddress)transportAddress).address().getPort();
    }
}
