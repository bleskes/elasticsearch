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

package org.elasticsearch.shield.transport.ssl;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.http.HttpServerTransport;
import org.elasticsearch.node.internal.InternalNode;
import org.elasticsearch.shield.ssl.SSLService;
import org.elasticsearch.shield.ssl.SSLServiceProvider;
import org.elasticsearch.test.ShieldIntegrationTest;
import org.elasticsearch.test.ShieldSettingsSource;
import org.elasticsearch.test.rest.client.http.HttpRequestBuilder;
import org.elasticsearch.test.rest.client.http.HttpResponse;
import org.elasticsearch.transport.Transport;
import org.junit.Test;

import javax.net.ssl.SSLHandshakeException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder;
import static org.elasticsearch.shield.authc.support.UsernamePasswordToken.basicAuthHeaderValue;
import static org.elasticsearch.test.ElasticsearchIntegrationTest.ClusterScope;
import static org.elasticsearch.test.ElasticsearchIntegrationTest.Scope;
import static org.hamcrest.Matchers.containsString;

@ClusterScope(scope = Scope.SUITE)
public class SslClientAuthTests extends ShieldIntegrationTest {

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return settingsBuilder()
                .put(super.nodeSettings(nodeOrdinal))
                // invert the require auth settings
                .put("shield.transport.ssl", true)
                .put("shield.http.ssl", true)
                .put("shield.http.ssl.client.auth", true)
                .put("transport.profiles.default.shield.ssl.client.auth", false)
                .put(InternalNode.HTTP_ENABLED, true)
                .build();
    }

    @Override
    protected boolean sslTransportEnabled() {
        return true;
    }

    @Test(expected = SSLHandshakeException.class)
    public void testThatHttpFailsWithoutSslClientAuth() throws IOException {
        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
                SSLContexts.createDefault(),
                SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        CloseableHttpClient client = HttpClients.custom().setSSLSocketFactory(socketFactory).build();

        new HttpRequestBuilder(client)
                .httpTransport(internalCluster().getInstance(HttpServerTransport.class))
                .method("GET").path("/")
                .protocol("https")
                .execute();
    }

    @Test
    public void testThatHttpWorksWithSslClientAuth() throws IOException {
        Settings settings = settingsBuilder().put(ShieldSettingsSource.getSSLSettingsForStore("/org/elasticsearch/shield/transport/ssl/certs/simple/testclient.jks", "testclient")).build();
        SSLService sslService = new SSLServiceProvider(settings).get();

        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
                sslService.getSslContext(),
                SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        CloseableHttpClient client = HttpClients.custom().setSSLSocketFactory(socketFactory).build();

        HttpResponse response = new HttpRequestBuilder(client)
                .httpTransport(internalCluster().getInstance(HttpServerTransport.class))
                .method("GET").path("/")
                .protocol("https")
                .addHeader("Authorization", basicAuthHeaderValue(transportClientUsername(), transportClientPassword()))
                .execute();
        assertThat(response.getBody(), containsString("You Know, for Search"));
    }

    @Test
    public void testThatTransportWorksWithoutSslClientAuth() throws Exception {
        // specify an arbitrary keystore, that does not include the certs needed to connect to the transport protocol
        File store;
        try {
            store = new File(ShieldSettingsSource.class.getResource("/org/elasticsearch/shield/transport/ssl/certs/simple/testclient-client-profile.jks").toURI());
        } catch (URISyntaxException e) {
            throw new ElasticsearchException("exception while reading the store", e);
        }

        if (!store.exists()) {
            throw new ElasticsearchException("store path doesn't exist");
        }

        Settings settings = settingsBuilder()
                .put("shield.transport.ssl", true)
                .put("shield.ssl.keystore.path", store.getPath())
                .put("shield.ssl.keystore.password", "testclient-client-profile")
                .put("cluster.name", internalCluster().getClusterName())
                .put("shield.user", transportClientUsername() + ":" + new String(transportClientPassword().internalChars()))
                .build();
        try (TransportClient client = new TransportClient(settings)) {
            Transport transport = internalCluster().getDataNodeInstance(Transport.class);
            TransportAddress transportAddress = transport.boundAddress().publishAddress();
            client.addTransportAddress(transportAddress);

            assertGreenClusterState(client);
        }
    }
}
