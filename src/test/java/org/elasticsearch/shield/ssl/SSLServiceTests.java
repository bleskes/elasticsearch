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

import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.junit.Before;
import org.junit.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder;
import static org.hamcrest.Matchers.*;

public class SSLServiceTests extends ElasticsearchTestCase {

    Path testnodeStore;

    @Before
    public void setup() throws Exception {
        testnodeStore = Paths.get(getClass().getResource("/org/elasticsearch/shield/transport/ssl/certs/simple/testnode.jks").toURI());
    }

    @Test(expected = ElasticsearchSSLException.class)
    public void testThatInvalidProtocolThrowsException() throws Exception {
        new SSLService(settingsBuilder()
                            .put("shield.ssl.protocol", "non-existing")
                            .put("shield.ssl.keystore.path", testnodeStore)
                            .put("shield.ssl.keystore.password", "testnode")
                            .put("shield.ssl.truststore.path", testnodeStore)
                            .put("shield.ssl.truststore.password", "testnode")
                        .build()).createSSLEngine();
    }

    @Test
    public void testThatCustomTruststoreCanBeSpecified() throws Exception {
        Path testClientStore = Paths.get(getClass().getResource("/org/elasticsearch/shield/transport/ssl/certs/simple/testclient.jks").toURI());

        SSLService sslService = new SSLService(settingsBuilder()
                .put("shield.ssl.keystore.path", testnodeStore)
                .put("shield.ssl.keystore.password", "testnode")
                .build());

        ImmutableSettings.Builder settingsBuilder = settingsBuilder()
                .put("truststore.path", testClientStore)
                .put("truststore.password", "testclient");

        SSLEngine sslEngineWithTruststore = sslService.createSSLEngine(settingsBuilder.build());
        assertThat(sslEngineWithTruststore, is(not(nullValue())));

        SSLEngine sslEngine = sslService.createSSLEngine();
        assertThat(sslEngineWithTruststore, is(not(sameInstance(sslEngine))));
    }

    @Test
    public void testThatSslContextCachingWorks() throws Exception {
        SSLService sslService = new SSLService(settingsBuilder()
            .put("shield.ssl.keystore.path", testnodeStore)
            .put("shield.ssl.keystore.password", "testnode")
            .build());

        SSLContext sslContext = sslService.getSslContext();
        SSLContext cachedSslContext = sslService.getSslContext();

        assertThat(sslContext, is(sameInstance(cachedSslContext)));
    }

    @Test
    public void testThatKeyStoreAndKeyCanHaveDifferentPasswords() throws Exception {
        Path differentPasswordsStore = Paths.get(getClass().getResource("/org/elasticsearch/shield/transport/ssl/certs/simple/testnode-different-passwords.jks").toURI());
        new SSLService(settingsBuilder()
                .put("shield.ssl.keystore.path", differentPasswordsStore)
                .put("shield.ssl.keystore.password", "testnode")
                .put("shield.ssl.keystore.key_password", "testnode1")
                .build()).createSSLEngine();
    }

    @Test(expected = ElasticsearchSSLException.class)
    public void testIncorrectKeyPasswordThrowsException() throws Exception {
        Path differentPasswordsStore = Paths.get(getClass().getResource("/org/elasticsearch/shield/transport/ssl/certs/simple/testnode-different-passwords.jks").toURI());
        new SSLService(settingsBuilder()
                .put("shield.ssl.keystore.path", differentPasswordsStore)
                .put("shield.ssl.keystore.password", "testnode")
                .build()).createSSLEngine();
    }

    @Test
    public void testThatSSLv3IsNotEnabled() throws Exception {
        SSLService sslService = new SSLService(settingsBuilder()
                .put("shield.ssl.keystore.path", testnodeStore)
                .put("shield.ssl.keystore.password", "testnode")
                .build());
        SSLEngine engine = sslService.createSSLEngine();
        assertThat(Arrays.asList(engine.getEnabledProtocols()), not(hasItem("SSLv3")));
    }
}
