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

import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.shield.Security;
import org.elasticsearch.shield.transport.netty.ShieldNettyTransport;
import org.elasticsearch.test.ShieldIntegTestCase;
import org.elasticsearch.transport.Transport;
import org.elasticsearch.xpack.XPackPlugin;

import static org.elasticsearch.test.ShieldSettingsSource.DEFAULT_PASSWORD;
import static org.elasticsearch.test.ShieldSettingsSource.DEFAULT_USER_NAME;
import static org.hamcrest.Matchers.containsString;

public class AutoSSLIntegTests extends ShieldIntegTestCase {

    @Override
    public boolean sslTransportEnabled() {
        return true;
    }

    @Override
    public boolean autoSSLEnabled() {
        return true;
    }

    public void testTransportClient() {
        String clusterName = internalCluster().getClusterName();
        TransportAddress transportAddress = randomFrom(internalCluster().getInstance(Transport.class).boundAddress().boundAddresses());
        try (TransportClient transportClient = TransportClient.builder().addPlugin(XPackPlugin.class)
                .settings(Settings.builder()
                        .put("cluster.name", clusterName)
                        .put(Security.USER_SETTING.getKey(), DEFAULT_USER_NAME + ":" + DEFAULT_PASSWORD))
                .build()) {
            transportClient.addTransportAddress(transportAddress);
            assertGreenClusterState(transportClient);
        }

        // now try with SSL disabled and it should fail
        try (TransportClient transportClient = TransportClient.builder().addPlugin(XPackPlugin.class)
                .settings(Settings.builder()
                        .put("cluster.name", clusterName)
                        .put(ShieldNettyTransport.SSL_SETTING.getKey(), false)
                        .put(Security.USER_SETTING.getKey(), DEFAULT_USER_NAME + ":" + DEFAULT_PASSWORD))
                .build()) {
            transportClient.addTransportAddress(transportAddress);
            assertGreenClusterState(transportClient);
            fail("should not have been able to connect");
        } catch (NoNodeAvailableException e) {
            assertThat(e.getMessage(), containsString("None of the configured nodes are available"));
        }
    }
}
