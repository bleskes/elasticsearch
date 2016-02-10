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

package org.elasticsearch.shield.transport.netty;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.test.ShieldIntegTestCase;
import org.elasticsearch.transport.Transport;
import org.elasticsearch.xpack.XPackPlugin;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.elasticsearch.common.settings.Settings.settingsBuilder;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;

public class SslHostnameVerificationTests extends ShieldIntegTestCase {
    @Override
    protected boolean sslTransportEnabled() {
        return true;
    }

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        Settings.Builder settingsBuilder = settingsBuilder().put(super.nodeSettings(nodeOrdinal));
        Path keystore;
        try {
            /*
             * This keystore uses a cert without any subject alternative names and a CN of "Elasticsearch Test Node No SAN"
             * that will not resolve to a DNS name and will always cause hostname verification failures
             */
            keystore = getDataPath("/org/elasticsearch/shield/transport/ssl/certs/simple/testnode-no-subjaltname.jks");
            assert keystore != null;
            assertThat(Files.exists(keystore), is(true));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return settingsBuilder.put("shield.ssl.keystore.path", keystore.toAbsolutePath()) // settings for client truststore
                .put("shield.ssl.keystore.password", "testnode-no-subjaltname")
                .put("shield.ssl.truststore.path", keystore.toAbsolutePath()) // settings for client truststore
                .put("shield.ssl.truststore.password", "testnode-no-subjaltname")
                // disable hostname verification as this test uses non-localhost addresses
                .put(ShieldNettyTransport.HOSTNAME_VERIFICATION_SETTING, false)
                .build();
    }

    @Override
    protected Settings transportClientSettings() {
        Path keystore = getDataPath("/org/elasticsearch/shield/transport/ssl/certs/simple/testnode-no-subjaltname.jks");
        assert keystore != null;
        return Settings.builder().put(super.transportClientSettings())
                .put(ShieldNettyTransport.HOSTNAME_VERIFICATION_SETTING, false)
                .put("shield.ssl.truststore.path", keystore.toAbsolutePath()) // settings for client truststore
                .put("shield.ssl.truststore.password", "testnode-no-subjaltname")
                .build();
    }

    public void testThatHostnameMismatchDeniesTransportClientConnection() throws Exception {
        Transport transport = internalCluster().getDataNodeInstance(Transport.class);
        TransportAddress transportAddress = transport.boundAddress().publishAddress();
        assertThat(transportAddress, instanceOf(InetSocketTransportAddress.class));
        InetSocketAddress inetSocketAddress = ((InetSocketTransportAddress) transportAddress).address();

        Settings settings = settingsBuilder().put(transportClientSettings())
                .put(ShieldNettyTransport.HOSTNAME_VERIFICATION_SETTING, true)
                .build();

        try (TransportClient client = TransportClient.builder().addPlugin(XPackPlugin.class).settings(settings).build()) {
            client.addTransportAddress(new InetSocketTransportAddress(inetSocketAddress.getAddress(), inetSocketAddress.getPort()));
            client.admin().cluster().prepareHealth().get();
            fail("Expected a NoNodeAvailableException due to hostname verification failures");
        } catch (NoNodeAvailableException e) {
            assertThat(e.getMessage(), containsString("None of the configured nodes are available: [{#transport#"));
        }
    }

    public void testTransportClientConnectionIgnoringHostnameVerification() throws Exception {
        Client client = internalCluster().transportClient();
        assertGreenClusterState(client);
    }
}
