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
import org.junit.Ignore;
import org.junit.Test;

import javax.net.ssl.SSLEngine;
import java.io.File;

import static org.elasticsearch.common.settings.ImmutableSettings.settingsBuilder;
import static org.hamcrest.Matchers.*;

public class SSLServiceTests extends ElasticsearchTestCase {

    File testnodeStore;

    @Before
    public void setup() throws Exception {
        testnodeStore = new File(getClass().getResource("/org/elasticsearch/shield/transport/ssl/certs/simple/testnode.jks").toURI());
    }

    @Test(expected = ElasticsearchSSLException.class)
    public void testThatInvalidProtocolThrowsException() throws Exception {
        new SSLService(settingsBuilder()
                            .put("shield.ssl.protocol", "non-existing")
                            .put("shield.ssl.keystore.path", testnodeStore.getPath())
                            .put("shield.ssl.keystore.password", "testnode")
                            .put("shield.ssl.truststore.path", testnodeStore.getPath())
                            .put("shield.ssl.truststore.password", "testnode")
                        .build());
    }

    @Test @Ignore //TODO it appears that setting a specific protocol doesn't make a difference
    public void testSpecificProtocol() {
        SSLService ssl = new SSLService(settingsBuilder()
                .put("shield.ssl.protocol", "TLSv1.2")
                .put("shield.ssl.keystore.path", testnodeStore.getPath())
                .put("shield.ssl.keystore.password", "testnode")
                .put("shield.ssl.truststore.path", testnodeStore.getPath())
                .put("shield.ssl.truststore.password", "testnode")
                .build());
        assertThat(ssl.createSSLEngine().getSSLParameters().getProtocols(), arrayContaining("TLSv1.2"));
    }

    @Test
    public void testThatCustomTruststoreCanBeSpecified() throws Exception {
        File testClientStore = new File(getClass().getResource("/org/elasticsearch/shield/transport/ssl/certs/simple/testclient.jks").toURI());

        SSLService sslService = new SSLService(settingsBuilder()
                .put("shield.ssl.keystore.path", testnodeStore.getPath())
                .put("shield.ssl.keystore.password", "testnode")
                .build());

        ImmutableSettings.Builder settingsBuilder = settingsBuilder()
                .put("truststore.path", testClientStore.getPath())
                .put("truststore.password", "testclient");

        SSLEngine sslEngineWithTruststore = sslService.createSSLEngineWithTruststore(settingsBuilder.build());
        assertThat(sslEngineWithTruststore, is(not(nullValue())));
    }
}
