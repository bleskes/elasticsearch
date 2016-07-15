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

package org.elasticsearch.xpack.security.transport.ssl;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.io.Streams;
import org.elasticsearch.common.network.NetworkModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.http.HttpServerTransport;
import org.elasticsearch.xpack.security.ssl.ClientSSLService;
import org.elasticsearch.xpack.security.ssl.SSLConfiguration.Global;
import org.elasticsearch.xpack.security.transport.netty3.SecurityNetty3HttpServerTransport;
import org.elasticsearch.test.SecurityIntegTestCase;
import org.elasticsearch.transport.Transport;
import org.elasticsearch.xpack.XPackPlugin;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Locale;

import static org.elasticsearch.test.SecuritySettingsSource.getSSLSettingsForStore;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;

public class SslIntegrationTests extends SecurityIntegTestCase {
    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return Settings.builder().put(super.nodeSettings(nodeOrdinal))
                .put(NetworkModule.HTTP_ENABLED.getKey(), true)
                .put(SecurityNetty3HttpServerTransport.SSL_SETTING.getKey(), true).build();
    }

    @Override
    protected boolean sslTransportEnabled() {
        return true;
    }

    // no SSL exception as this is the exception is returned when connecting
    public void testThatUnconfiguredCiphersAreRejected() {
        try (TransportClient transportClient = TransportClient.builder().addPlugin(XPackPlugin.class).settings(Settings.builder()
                .put(transportClientSettings())
                .put("node.name", "programmatic_transport_client")
                .put("cluster.name", internalCluster().getClusterName())
                .putArray("xpack.security.ssl.ciphers", new String[]{"TLS_ECDH_anon_WITH_RC4_128_SHA", "SSL_RSA_WITH_3DES_EDE_CBC_SHA"})
                .build()).build()) {

            TransportAddress transportAddress = randomFrom(internalCluster().getInstance(Transport.class).boundAddress().boundAddresses());
            transportClient.addTransportAddress(transportAddress);

            transportClient.admin().cluster().prepareHealth().get();
            fail("Expected NoNodeAvailableException");
        } catch (NoNodeAvailableException e) {
            assertThat(e.getMessage(), containsString("None of the configured nodes are available: [{#transport#"));
        }
    }

    // no SSL exception as this is the exception is returned when connecting
    public void testThatTransportClientUsingSSLv3ProtocolIsRejected() {
        try(TransportClient transportClient = TransportClient.builder().addPlugin(XPackPlugin.class).settings(Settings.builder()
                .put(transportClientSettings())
                .put("node.name", "programmatic_transport_client")
                .put("cluster.name", internalCluster().getClusterName())
                .putArray("xpack.security.ssl.supported_protocols", new String[]{"SSLv3"})
                .build()).build()) {

            TransportAddress transportAddress = randomFrom(internalCluster().getInstance(Transport.class).boundAddress().boundAddresses());
            transportClient.addTransportAddress(transportAddress);

            transportClient.admin().cluster().prepareHealth().get();
            fail("Expected NoNodeAvailableException");
        } catch (NoNodeAvailableException e) {
            assertThat(e.getMessage(), containsString("None of the configured nodes are available: [{#transport#"));
        }
    }

    public void testThatConnectionToHTTPWorks() throws Exception {
        Settings settings = Settings.builder()
                .put(getSSLSettingsForStore("/org/elasticsearch/xpack/security/transport/ssl/certs/simple/testclient.jks", "testclient"))
                .build();
        ClientSSLService service = new ClientSSLService(settings, new Global(settings));

        CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(nodeClientUsername(),
                new String(nodeClientPassword().internalChars())));
        try (CloseableHttpClient client = HttpClients.custom().setSSLContext(service.sslContext())
                .setDefaultCredentialsProvider(provider).build();
            CloseableHttpResponse response = client.execute(new HttpGet(getNodeUrl()))) {
            assertThat(response.getStatusLine().getStatusCode(), is(200));
            String data = Streams.copyToString(new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8));
            assertThat(data, containsString("You Know, for Search"));
        }
    }

    public void testThatHttpUsingSSLv3IsRejected() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("SSL");
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init((KeyStore) null);

        sslContext.init(null, factory.getTrustManagers(), new SecureRandom());
        SSLConnectionSocketFactory sf = new SSLConnectionSocketFactory(sslContext, new String[]{ "SSLv3" }, null,
                NoopHostnameVerifier.INSTANCE);
        try (CloseableHttpClient client = HttpClients.custom().setSSLSocketFactory(sf).build()) {
            client.execute(new HttpGet(getNodeUrl()));
            fail("Expected a connection error due to SSLv3 not being supported by default");
        } catch (Exception e) {
            assertThat(e, is(instanceOf(SSLHandshakeException.class)));
        }
    }

    private String getNodeUrl() {
        TransportAddress transportAddress =
                randomFrom(internalCluster().getInstance(HttpServerTransport.class).boundAddress().boundAddresses());
        assertThat(transportAddress, is(instanceOf(InetSocketTransportAddress.class)));
        InetSocketTransportAddress inetSocketTransportAddress = (InetSocketTransportAddress) transportAddress;
        return String.format(Locale.ROOT, "https://%s:%s/", "localhost", inetSocketTransportAddress.address().getPort());
    }
}
