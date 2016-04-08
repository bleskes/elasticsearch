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

package org.elasticsearch.watcher;

import org.apache.http.HttpStatus;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.elasticsearch.action.admin.cluster.node.info.NodeInfo;
import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.common.network.NetworkModule;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.http.HttpServerTransport;
import org.elasticsearch.marvel.Marvel;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.shield.Security;
import org.elasticsearch.test.ESIntegTestCase;
import org.elasticsearch.test.ESIntegTestCase.ClusterScope;
import org.elasticsearch.test.rest.client.http.HttpRequestBuilder;
import org.elasticsearch.test.rest.client.http.HttpResponse;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.threadpool.ThreadPoolInfo;
import org.elasticsearch.watcher.execution.InternalWatchExecutor;
import org.elasticsearch.xpack.XPackPlugin;

import java.util.Collection;
import java.util.Collections;

import static org.elasticsearch.test.ESIntegTestCase.Scope.SUITE;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 *
 */
@ClusterScope(scope = SUITE, numClientNodes = 0, transportClientRatio = 0, randomDynamicTemplates = false, maxNumDataNodes = 3)
public class WatcherPluginDisableTests extends ESIntegTestCase {
    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put(XPackPlugin.featureEnabledSetting(Watcher.NAME), false)

                // disable shield because of query cache check and authentication/authorization
                .put(XPackPlugin.featureEnabledSetting(Security.NAME), false)
                .put(XPackPlugin.featureEnabledSetting(Marvel.NAME), false)

                .put(NetworkModule.HTTP_ENABLED.getKey(), true)
                .build();
    }

    @Override
    protected Collection<Class<? extends Plugin>> nodePlugins() {
        return Collections.<Class<? extends Plugin>>singleton(XPackPlugin.class);
    }

    @Override
    protected Collection<Class<? extends Plugin>> transportClientPlugins() {
        return Collections.<Class<? extends Plugin>>singleton(XPackPlugin.class);
    }

    @Override
    protected Settings transportClientSettings() {
        return Settings.builder()
                .put(super.transportClientSettings())
                .build();
    }

    public void testRestEndpoints() throws Exception {
        HttpServerTransport httpServerTransport = internalCluster().getDataNodeInstance(HttpServerTransport.class);
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpRequestBuilder request = new HttpRequestBuilder(httpClient).httpTransport(httpServerTransport)
                    .method("GET")
                    .path("/_watcher");
            HttpResponse response = request.execute();
            assertThat(response.getStatusCode(), is(HttpStatus.SC_BAD_REQUEST));
        }
    }

    public void testThreadPools() throws Exception {
        NodesInfoResponse nodesInfo = client().admin().cluster().prepareNodesInfo().setThreadPool(true).get();
        for (NodeInfo nodeInfo : nodesInfo) {
            ThreadPoolInfo threadPoolInfo = nodeInfo.getThreadPool();
            for (ThreadPool.Info info : threadPoolInfo) {
                assertThat(info.getName(), not(is(InternalWatchExecutor.THREAD_POOL_NAME)));
            }
        }
    }
}
