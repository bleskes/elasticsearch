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

package org.elasticsearch.shield.ssl;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.env.Environment;
import org.elasticsearch.shield.ShieldSettingsException;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.elasticsearch.test.junit.annotations.Network;
import org.junit.Before;
import org.junit.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSessionContext;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.elasticsearch.common.settings.Settings.settingsBuilder;
import static org.hamcrest.Matchers.*;

public class ClientSSLServiceTests extends ElasticsearchTestCase {

    Environment env;
    Path testclientStore;

    @Before
    public void setup() throws Exception {
        testclientStore = getDataPath("/org/elasticsearch/shield/transport/ssl/certs/simple/testclient.jks");
        env = new Environment(settingsBuilder().put("path.home", createTempDir()).build());
    }

    @Test(expected = ElasticsearchSSLException.class)
    public void testThatInvalidProtocolThrowsException() throws Exception {
        new ClientSSLService(settingsBuilder()
                .put("shield.ssl.protocol", "non-existing")
                .put("shield.ssl.keystore.path", testclientStore)
                .put("shield.ssl.keystore.password", "testclient")
                .put("shield.ssl.truststore.path", testclientStore)
                .put("shield.ssl.truststore.password", "testclient")
                .build(), env).createSSLEngine();
    }

    @Test
    public void testThatCustomTruststoreCanBeSpecified() throws Exception {
        Path testnodeStore = getDataPath("/org/elasticsearch/shield/transport/ssl/certs/simple/testnode.jks");

        ClientSSLService sslService = new ClientSSLService(settingsBuilder()
                .put("shield.ssl.keystore.path", testclientStore)
                .put("shield.ssl.keystore.password", "testclient")
                .build(), env);

        Settings.Builder settingsBuilder = settingsBuilder()
                .put("truststore.path", testnodeStore)
                .put("truststore.password", "testnode");

        SSLEngine sslEngineWithTruststore = sslService.createSSLEngine(settingsBuilder.build());
        assertThat(sslEngineWithTruststore, is(not(nullValue())));

        SSLEngine sslEngine = sslService.createSSLEngine();
        assertThat(sslEngineWithTruststore, is(not(sameInstance(sslEngine))));
    }

    @Test
    public void testThatSslContextCachingWorks() throws Exception {
        ClientSSLService sslService = new ClientSSLService(settingsBuilder()
                .put("shield.ssl.keystore.path", testclientStore)
                .put("shield.ssl.keystore.password", "testclient")
                .build(), env);

        SSLContext sslContext = sslService.sslContext();
        SSLContext cachedSslContext = sslService.sslContext();

        assertThat(sslContext, is(sameInstance(cachedSslContext)));
    }

    @Test
    public void testThatKeyStoreAndKeyCanHaveDifferentPasswords() throws Exception {
        Path differentPasswordsStore = getDataPath("/org/elasticsearch/shield/transport/ssl/certs/simple/testnode-different-passwords.jks");
        new ClientSSLService(settingsBuilder()
                .put("shield.ssl.keystore.path", differentPasswordsStore)
                .put("shield.ssl.keystore.password", "testnode")
                .put("shield.ssl.keystore.key_password", "testnode1")
                .build(), env).createSSLEngine();
    }

    @Test(expected = ElasticsearchSSLException.class)
    public void testIncorrectKeyPasswordThrowsException() throws Exception {
        Path differentPasswordsStore = getDataPath("/org/elasticsearch/shield/transport/ssl/certs/simple/testnode-different-passwords.jks");
        new ClientSSLService(settingsBuilder()
                .put("shield.ssl.keystore.path", differentPasswordsStore)
                .put("shield.ssl.keystore.password", "testnode")
                .build(), env).createSSLEngine();
    }

    @Test
    public void testThatSSLv3IsNotEnabled() throws Exception {
        ClientSSLService sslService = new ClientSSLService(settingsBuilder()
                .put("shield.ssl.keystore.path", testclientStore)
                .put("shield.ssl.keystore.password", "testclient")
                .build(), env);
        SSLEngine engine = sslService.createSSLEngine();
        assertThat(Arrays.asList(engine.getEnabledProtocols()), not(hasItem("SSLv3")));
    }

    @Test
    public void testThatSSLSessionCacheHasDefaultLimits() throws Exception {
        ClientSSLService sslService = new ClientSSLService(settingsBuilder()
                .put("shield.ssl.keystore.path", testclientStore)
                .put("shield.ssl.keystore.password", "testclient")
                .build(), env);
        SSLSessionContext context = sslService.sslContext().getServerSessionContext();
        assertThat(context.getSessionCacheSize(), equalTo(1000));
        assertThat(context.getSessionTimeout(), equalTo((int) TimeValue.timeValueHours(24).seconds()));
    }

    @Test
    public void testThatSettingSSLSessionCacheLimitsWorks() throws Exception {
        ClientSSLService sslService = new ClientSSLService(settingsBuilder()
                .put("shield.ssl.keystore.path", testclientStore)
                .put("shield.ssl.keystore.password", "testclient")
                .put("shield.ssl.session.cache_size", "300")
                .put("shield.ssl.session.cache_timeout", "600s")
                .build(), env);
        SSLSessionContext context = sslService.sslContext().getServerSessionContext();
        assertThat(context.getSessionCacheSize(), equalTo(300));
        assertThat(context.getSessionTimeout(), equalTo(600));
    }

    @Test
    public void testThatCreateClientSSLEngineWithoutAnySettingsWorks() throws Exception {
        ClientSSLService sslService = new ClientSSLService(Settings.EMPTY, env);
        SSLEngine sslEngine = sslService.createSSLEngine();
        assertThat(sslEngine, notNullValue());
    }

    @Test
    public void testThatCreateSSLEngineWithOnlyTruststoreWorks() throws Exception {
        ClientSSLService sslService = new ClientSSLService(settingsBuilder()
                .put("shield.ssl.truststore.path", testclientStore)
                .put("shield.ssl.truststore.password", "testclient")
                .build(), env);
        SSLEngine sslEngine = sslService.createSSLEngine();
        assertThat(sslEngine, notNullValue());
    }

    @Test
    public void testThatCreateSSLEngineWithOnlyKeystoreWorks() throws Exception {
        ClientSSLService sslService = new ClientSSLService(settingsBuilder()
                .put("shield.ssl.keystore.path", testclientStore)
                .put("shield.ssl.keystore.password", "testclient")
                .build(), env);
        SSLEngine sslEngine = sslService.createSSLEngine();
        assertThat(sslEngine, notNullValue());
    }

    @Test
    @Network
    public void testThatSSLContextWithoutSettingsWorks() throws Exception {
        ClientSSLService sslService = new ClientSSLService(Settings.EMPTY, env);
        SSLContext sslContext = sslService.sslContext();
        try (CloseableHttpClient client = HttpClients.custom().setSslcontext(sslContext).build()) {
            // Execute a GET on a site known to have a valid certificate signed by a trusted public CA
            // This will result in a SSLHandshakeException if the SSLContext does not trust the CA, but the default
            // truststore trusts all common public CAs so the handshake will succeed
            client.execute(new HttpGet("https://www.elastic.co/"));
        }
    }

    @Test
    @Network
    public void testThatSSLContextWithKeystoreDoesNotTrustAllPublicCAs() throws Exception {
        ClientSSLService sslService = new ClientSSLService(settingsBuilder()
                .put("shield.ssl.keystore.path", testclientStore)
                .put("shield.ssl.keystore.password", "testclient")
                .build(), env);
        SSLContext sslContext = sslService.sslContext();
        try (CloseableHttpClient client = HttpClients.custom().setSslcontext(sslContext).build()) {
            // Execute a GET on a site known to have a valid certificate signed by a trusted public CA
            // This will result in a SSLHandshakeException because the truststore is the testnodestore, which doesn't
            // trust any public CAs
            client.execute(new HttpGet("https://www.elastic.co/"));
            fail("A SSLHandshakeException should have been thrown here");
        } catch (Exception e) {
            assertThat(e, instanceOf(SSLHandshakeException.class));
        }
    }

    @Test(expected = ShieldSettingsException.class)
    public void testThatTruststorePasswordIsRequired() throws Exception {
        ClientSSLService sslService = new ClientSSLService(settingsBuilder()
                .put("shield.ssl.truststore.path", testclientStore)
                .build(), env);
        sslService.sslContext();
    }

    @Test(expected = ShieldSettingsException.class)
    public void testThatKeystorePasswordIsRequired() throws Exception {
        ClientSSLService sslService = new ClientSSLService(settingsBuilder()
                .put("shield.ssl.keystore.path", testclientStore)
                .build(), env);
        sslService.sslContext();
    }

    @Test
    public void validCiphersAndInvalidCiphersWork() throws Exception {
        List<String> ciphers = new ArrayList<>(Arrays.asList(AbstractSSLService.DEFAULT_CIPHERS));
        ciphers.add("foo");
        ciphers.add("bar");
        ClientSSLService sslService = new ClientSSLService(settingsBuilder()
                .putArray("shield.ssl.ciphers", ciphers.toArray(new String[ciphers.size()]))
                .build(), env);
        SSLEngine engine = sslService.createSSLEngine();
        assertThat(engine, is(notNullValue()));
        String[] enabledCiphers = engine.getEnabledCipherSuites();
        assertThat(Arrays.asList(enabledCiphers), not(contains("foo", "bar")));
    }

    @Test(expected = ShieldSettingsException.class)
    public void invalidCiphersOnlyThrowsException() throws Exception {
        ClientSSLService sslService = new ClientSSLService(settingsBuilder()
                .putArray("shield.ssl.ciphers", new String[] { "foo", "bar" })
                .build(), env);
        sslService.createSSLEngine();
    }
}
