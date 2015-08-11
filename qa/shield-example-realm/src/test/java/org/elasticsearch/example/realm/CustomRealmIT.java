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

package org.elasticsearch.example.realm;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.client.support.Headers;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.shield.ShieldPlugin;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.test.rest.client.http.HttpResponse;
import org.junit.Test;

import static org.hamcrest.Matchers.*;

/**
 * Integration test to test authentication with the custom realm
 */
public class CustomRealmIT extends ESIntegTestCase {

    @Override
    protected Settings externalClusterClientSettings() {
        return Settings.builder()
                .put("plugin.types", ShieldPlugin.class.getName())
                .put(Headers.PREFIX + "." + CustomRealm.USER_HEADER, CustomRealm.KNOWN_USER)
                .put(Headers.PREFIX + "." + CustomRealm.PW_HEADER, CustomRealm.KNOWN_PW)
                .build();
    }

    @Test
    public void testHttpConnectionWithNoAuthentication() throws Exception {
        HttpResponse response = httpClient().path("/").execute();
        assertThat(response.getStatusCode(), is(401));
        String value = response.getHeaders().get("WWW-Authenticate");
        assertThat(value, is("custom-challenge"));
    }

    @Test
    public void testHttpAuthentication() throws Exception {
        HttpResponse response = httpClient().path("/")
                .addHeader(CustomRealm.USER_HEADER, CustomRealm.KNOWN_USER)
                .addHeader(CustomRealm.PW_HEADER, CustomRealm.KNOWN_PW)
                .execute();
        assertThat(response.getStatusCode(), is(200));
    }

    @Test
    public void testTransportClient() throws Exception {
        NodesInfoResponse nodeInfos = client().admin().cluster().prepareNodesInfo().get();
        NodeInfo[] nodes = nodeInfos.getNodes();
        assertTrue(nodes.length > 0);
        TransportAddress publishAddress = randomFrom(nodes).getTransport().address().publishAddress();
        String clusterName = nodeInfos.getClusterNameAsString();

        Settings settings = Settings.builder()
                .put("path.home", createTempDir())
                .put("plugin.types", ShieldPlugin.class.getName())
                .put("cluster.name", clusterName)
                .put(Headers.PREFIX + "." + CustomRealm.USER_HEADER, CustomRealm.KNOWN_USER)
                .put(Headers.PREFIX + "." + CustomRealm.PW_HEADER, CustomRealm.KNOWN_PW)
                .build();
        try (TransportClient client = TransportClient.builder().settings(settings).build()) {
            client.addTransportAddress(publishAddress);
            ClusterHealthResponse response = client.admin().cluster().prepareHealth().execute().actionGet();
            assertThat(response.isTimedOut(), is(false));
        }
    }

    @Test
    public void testTransportClientWrongAuthentication() throws Exception {
        NodesInfoResponse nodeInfos = client().admin().cluster().prepareNodesInfo().get();
        NodeInfo[] nodes = nodeInfos.getNodes();
        assertTrue(nodes.length > 0);
        TransportAddress publishAddress = randomFrom(nodes).getTransport().address().publishAddress();
        String clusterName = nodeInfos.getClusterNameAsString();

        Settings settings = Settings.builder()
                .put("path.home", createTempDir())
                .put("plugin.types", ShieldPlugin.class.getName())
                .put("cluster.name", clusterName)
                .put(Headers.PREFIX + "." + CustomRealm.USER_HEADER, CustomRealm.KNOWN_USER + randomAsciiOfLength(1))
                .put(Headers.PREFIX + "." + CustomRealm.PW_HEADER, CustomRealm.KNOWN_PW)
                .build();
        try (TransportClient client = TransportClient.builder().settings(settings).build()) {
            client.addTransportAddress(publishAddress);
            client.admin().cluster().prepareHealth().execute().actionGet();
            fail("authentication failure should have resulted in a NoNodesAvailableException");
        } catch (NoNodeAvailableException e) {
            // expected
        }
    }
}
