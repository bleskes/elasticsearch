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

import org.apache.http.message.BasicHeader;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.network.NetworkModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.env.Environment;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.xpack.XPackPlugin;
import org.elasticsearch.xpack.XPackTransportClient;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
                .put(NetworkModule.TRANSPORT_TYPE_KEY, randomFrom("security3", "security4"))
                .build();
    }

    @Override
    protected Collection<Class<? extends Plugin>> transportClientPlugins() {
        return Collections.<Class<? extends Plugin>>singleton(XPackPlugin.class);
    }

    public void testHttpConnectionWithNoAuthentication() throws Exception {
        try {
            getRestClient().performRequest("GET", "/");
            fail("request should have failed");
        } catch(ResponseException e) {
            Response response = e.getResponse();
            assertThat(response.getStatusLine().getStatusCode(), is(401));
            String value = response.getHeader("WWW-Authenticate");
            assertThat(value, is("custom-challenge"));
        }
    }

    public void testHttpAuthentication() throws Exception {
        Response response = getRestClient().performRequest("GET", "/",
                new BasicHeader(CustomRealm.USER_HEADER, CustomRealm.KNOWN_USER),
                new BasicHeader(CustomRealm.PW_HEADER, CustomRealm.KNOWN_PW));
        assertThat(response.getStatusLine().getStatusCode(), is(200));
    }

    public void testTransportClient() throws Exception {
        NodesInfoResponse nodeInfos = client().admin().cluster().prepareNodesInfo().get();
        List<NodeInfo> nodes = nodeInfos.getNodes();
        assertTrue(nodes.isEmpty() == false);
        TransportAddress publishAddress = randomFrom(nodes).getTransport().address().publishAddress();
        String clusterName = nodeInfos.getClusterName().value();

        Settings settings = Settings.builder()
                .put("cluster.name", clusterName)
                .put(Environment.PATH_HOME_SETTING.getKey(), createTempDir().toAbsolutePath().toString())
                .put(ThreadContext.PREFIX + "." + CustomRealm.USER_HEADER, CustomRealm.KNOWN_USER)
                .put(ThreadContext.PREFIX + "." + CustomRealm.PW_HEADER, CustomRealm.KNOWN_PW)
                .build();
        try (TransportClient client = new XPackTransportClient(settings)) {
            client.addTransportAddress(publishAddress);
            ClusterHealthResponse response = client.admin().cluster().prepareHealth().execute().actionGet();
            assertThat(response.isTimedOut(), is(false));
        }
    }

    public void testTransportClientWrongAuthentication() throws Exception {
        NodesInfoResponse nodeInfos = client().admin().cluster().prepareNodesInfo().get();
        List<NodeInfo> nodes = nodeInfos.getNodes();
        assertTrue(nodes.isEmpty() == false);
        TransportAddress publishAddress = randomFrom(nodes).getTransport().address().publishAddress();
        String clusterName = nodeInfos.getClusterName().value();

        Settings settings = Settings.builder()
                .put("cluster.name", clusterName)
                .put(Environment.PATH_HOME_SETTING.getKey(), createTempDir().toAbsolutePath().toString())
                .put(ThreadContext.PREFIX + "." + CustomRealm.USER_HEADER, CustomRealm.KNOWN_USER + randomAsciiOfLength(1))
                .put(ThreadContext.PREFIX + "." + CustomRealm.PW_HEADER, CustomRealm.KNOWN_PW)
                .build();
        try (TransportClient client = new XPackTransportClient(settings)) {
            client.addTransportAddress(publishAddress);
            client.admin().cluster().prepareHealth().execute().actionGet();
            fail("authentication failure should have resulted in a NoNodesAvailableException");
        } catch (NoNodeAvailableException e) {
            // expected
        }
    }

    public void testSettingsFiltering() throws Exception {
        NodesInfoResponse nodeInfos = client().admin().cluster().prepareNodesInfo().clear().setSettings(true).get();
        for(NodeInfo info : nodeInfos.getNodes()) {
            Settings settings = info.getSettings();
            assertNotNull(settings);
            assertNull(settings.get("xpack.security.authc.realms.custom.filtered_setting"));
            assertEquals(CustomRealm.TYPE, settings.get("xpack.security.authc.realms.custom.type"));
        }
    }
}
