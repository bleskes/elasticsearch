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

package org.elasticsearch.xpack.watcher.actions.webhook;

import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.QueueDispatcher;
import com.squareup.okhttp.mockwebserver.RecordedRequest;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.Callback;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.xpack.ssl.SSLService;
import org.elasticsearch.xpack.watcher.actions.ActionBuilders;
import org.elasticsearch.xpack.watcher.condition.always.AlwaysCondition;
import org.elasticsearch.xpack.watcher.history.WatchRecord;
import org.elasticsearch.xpack.common.http.HttpRequestTemplate;
import org.elasticsearch.xpack.common.http.Scheme;
import org.elasticsearch.xpack.common.http.auth.basic.BasicAuth;
import org.elasticsearch.xpack.common.text.TextTemplate;
import org.elasticsearch.xpack.watcher.support.xcontent.XContentSource;
import org.elasticsearch.xpack.watcher.test.AbstractWatcherIntegrationTestCase;
import org.junit.After;
import org.junit.Before;

import java.net.BindException;
import java.nio.file.Path;

import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertNoFailures;
import static org.elasticsearch.xpack.watcher.client.WatchSourceBuilders.watchBuilder;
import static org.elasticsearch.xpack.watcher.input.InputBuilders.simpleInput;
import static org.elasticsearch.xpack.watcher.test.WatcherTestUtils.xContentSource;
import static org.elasticsearch.xpack.watcher.trigger.TriggerBuilders.schedule;
import static org.elasticsearch.xpack.watcher.trigger.schedule.Schedules.interval;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class WebhookHttpsIntegrationTests extends AbstractWatcherIntegrationTestCase {
    private int webPort;
    private MockWebServer webServer;

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        Path resource = getDataPath("/org/elasticsearch/xpack/security/keystore/testnode.jks");
        return Settings.builder()
                .put(super.nodeSettings(nodeOrdinal))
                .put("xpack.http.ssl.keystore.path", resource.toString())
                .put("xpack.http.ssl.keystore.password", "testnode")
                .build();
    }

    @Before
    public void startWebservice() throws Exception {
        for (webPort = 9200; webPort < 9300; webPort++) {
            try {
                webServer = new MockWebServer();
                webServer.setProtocolNegotiationEnabled(false);
                QueueDispatcher dispatcher = new QueueDispatcher();
                dispatcher.setFailFast(true);
                webServer.setDispatcher(dispatcher);
                webServer.start(webPort);
                SSLService sslService = getInstanceFromMaster(SSLService.class);
                Settings settings = getInstanceFromMaster(Settings.class);
                webServer.useHttps(sslService.sslSocketFactory(settings.getByPrefix("xpack.http.ssl.")), false);
                return;
            } catch (BindException be) {
                logger.warn("port [{}] was already in use trying next port", webPort);
            }
        }
        throw new ElasticsearchException("unable to find open port between 9200 and 9300");
    }

    @After
    public void stopWebservice() throws Exception {
        webServer.shutdown();
    }

    public void testHttps() throws Exception {
        webServer.enqueue(new MockResponse().setResponseCode(200).setBody("body"));
        HttpRequestTemplate.Builder builder = HttpRequestTemplate.builder("localhost", webPort)
                .scheme(Scheme.HTTPS)
                .path(new TextTemplate("/test/_id"))
                .body(new TextTemplate("{key=value}"));

        watcherClient().preparePutWatch("_id")
                .setSource(watchBuilder()
                        .trigger(schedule(interval("5s")))
                        .input(simpleInput("key", "value"))
                        .condition(AlwaysCondition.INSTANCE)
                        .addAction("_id", ActionBuilders.webhookAction(builder)))
                .get();

        if (timeWarped()) {
            timeWarp().scheduler().trigger("_id");
            refresh();
        }

        assertWatchWithMinimumPerformedActionsCount("_id", 1, false);
        RecordedRequest recordedRequest = webServer.takeRequest();
        assertThat(recordedRequest.getPath(), equalTo("/test/_id"));
        assertThat(recordedRequest.getBody().readUtf8Line(), equalTo("{key=value}"));

        SearchResponse response = searchWatchRecords(new Callback<SearchRequestBuilder>() {
            @Override
            public void handle(SearchRequestBuilder builder) {
                builder.setQuery(QueryBuilders.termQuery(WatchRecord.Field.STATE.getPreferredName(), "executed"));
            }
        });
        assertNoFailures(response);
        XContentSource source = xContentSource(response.getHits().getAt(0).sourceRef());
        String body = source.getValue("result.actions.0.webhook.response.body");
        assertThat(body, notNullValue());
        assertThat(body, is("body"));

        Number status = source.getValue("result.actions.0.webhook.response.status");
        assertThat(status, notNullValue());
        assertThat(status.intValue(), is(200));
    }

    public void testHttpsAndBasicAuth() throws Exception {
        webServer.enqueue(new MockResponse().setResponseCode(200).setBody("body"));
        HttpRequestTemplate.Builder builder = HttpRequestTemplate.builder("localhost", webPort)
                .scheme(Scheme.HTTPS)
                .auth(new BasicAuth("_username", "_password".toCharArray()))
                .path(new TextTemplate("/test/_id"))
                .body(new TextTemplate("{key=value}"));

        watcherClient().preparePutWatch("_id")
                .setSource(watchBuilder()
                        .trigger(schedule(interval("5s")))
                        .input(simpleInput("key", "value"))
                        .condition(AlwaysCondition.INSTANCE)
                        .addAction("_id", ActionBuilders.webhookAction(builder)))
                .get();

        if (timeWarped()) {
            timeWarp().scheduler().trigger("_id");
            refresh();
        }

        assertWatchWithMinimumPerformedActionsCount("_id", 1, false);
        RecordedRequest recordedRequest = webServer.takeRequest();
        assertThat(recordedRequest.getPath(), equalTo("/test/_id"));
        assertThat(recordedRequest.getBody().readUtf8Line(), equalTo("{key=value}"));
        assertThat(recordedRequest.getHeader("Authorization"), equalTo("Basic X3VzZXJuYW1lOl9wYXNzd29yZA=="));
    }
}
