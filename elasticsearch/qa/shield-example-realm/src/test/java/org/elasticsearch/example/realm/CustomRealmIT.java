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
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.test.rest.client.http.HttpResponse;
import org.elasticsearch.xpack.XPackPlugin;

import java.util.Collection;
import java.util.Collections;

import static org.hamcrest.Matchers.is;

/**
 * Integration test to test authentication with the custom realm
 */
public class CustomRealmIT extends ESIntegTestCase {
    @Override
    protected Settings externalClusterClientSettings() {
        return Settings.builder()
                .put(ThreadContext.PREFIX + "." + CustomRealm.USER_HEADER, CustomRealm.KNOWN_USER)
                .put(ThreadContext.PREFIX + "." + CustomRealm.PW_HEADER, CustomRealm.KNOWN_PW)
                .build();
    }

    @Override
    protected Collection<Class<? extends Plugin>> transportClientPlugins() {
        return Collections.<Class<? extends Plugin>>singleton(XPackPlugin.class);
    }

    public void testHttpConnectionWithNoAuthentication() throws Exception {
        HttpResponse response = httpClient().path("/").execute();
        assertThat(response.getStatusCode(), is(401));
        String value = response.getHeaders().get("WWW-Authenticate");
        assertThat(value, is("custom-challenge"));
    }

    public void testHttpAuthentication() throws Exception {
        HttpResponse response = httpClient().path("/")
                .addHeader(CustomRealm.USER_HEADER, CustomRealm.KNOWN_USER)
                .addHeader(CustomRealm.PW_HEADER, CustomRealm.KNOWN_PW)
                .execute();
        assertThat(response.getStatusCode(), is(200));
    }

    public void testTransportClient() throws Exception {
        NodesInfoResponse nodeInfos = client().admin().cluster().prepareNodesInfo().get();
        NodeInfo[] nodes = nodeInfos.getNodes();
        assertTrue(nodes.length > 0);
        TransportAddress publishAddress = randomFrom(nodes).getTransport().address().publishAddress();
        String clusterName = nodeInfos.getClusterNameAsString();

        Settings settings = Settings.builder()
                .put("cluster.name", clusterName)
                .put(ThreadContext.PREFIX + "." + CustomRealm.USER_HEADER, CustomRealm.KNOWN_USER)
                .put(ThreadContext.PREFIX + "." + CustomRealm.PW_HEADER, CustomRealm.KNOWN_PW)
                .build();
        try (TransportClient client = TransportClient.builder().settings(settings).addPlugin(XPackPlugin.class).build()) {
            client.addTransportAddress(publishAddress);
            ClusterHealthResponse response = client.admin().cluster().prepareHealth().execute().actionGet();
            assertThat(response.isTimedOut(), is(false));
        }
    }

    public void testTransportClientWrongAuthentication() throws Exception {
        NodesInfoResponse nodeInfos = client().admin().cluster().prepareNodesInfo().get();
        NodeInfo[] nodes = nodeInfos.getNodes();
        assertTrue(nodes.length > 0);
        TransportAddress publishAddress = randomFrom(nodes).getTransport().address().publishAddress();
        String clusterName = nodeInfos.getClusterNameAsString();

        Settings settings = Settings.builder()
                .put("cluster.name", clusterName)
                .put(ThreadContext.PREFIX + "." + CustomRealm.USER_HEADER, CustomRealm.KNOWN_USER + randomAsciiOfLength(1))
                .put(ThreadContext.PREFIX + "." + CustomRealm.PW_HEADER, CustomRealm.KNOWN_PW)
                .build();
        try (TransportClient client = TransportClient.builder().addPlugin(XPackPlugin.class).settings(settings).build()) {
            client.addTransportAddress(publishAddress);
            client.admin().cluster().prepareHealth().execute().actionGet();
            fail("authentication failure should have resulted in a NoNodesAvailableException");
        } catch (NoNodeAvailableException e) {
            // expected
        }
    }
}
