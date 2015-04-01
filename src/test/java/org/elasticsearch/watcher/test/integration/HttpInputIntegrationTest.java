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

package org.elasticsearch.watcher.test.integration;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.internal.InternalNode;
import org.elasticsearch.watcher.client.WatchSourceBuilder;
import org.elasticsearch.watcher.client.WatcherClient;
import org.elasticsearch.watcher.history.HistoryStore;
import org.elasticsearch.watcher.input.InputBuilders;
import org.elasticsearch.watcher.support.http.TemplatedHttpRequest;
import org.elasticsearch.watcher.support.http.auth.BasicAuth;
import org.elasticsearch.watcher.support.init.proxy.ScriptServiceProxy;
import org.elasticsearch.watcher.support.template.ScriptTemplate;
import org.elasticsearch.watcher.test.AbstractWatcherIntegrationTests;
import org.elasticsearch.watcher.trigger.TriggerBuilders;
import org.elasticsearch.watcher.trigger.schedule.IntervalSchedule;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertHitCount;
import static org.elasticsearch.watcher.actions.ActionBuilders.indexAction;
import static org.elasticsearch.watcher.client.WatchSourceBuilder.watchSourceBuilder;
import static org.elasticsearch.watcher.condition.ConditionBuilders.scriptCondition;
import static org.elasticsearch.watcher.trigger.TriggerBuilders.schedule;
import static org.elasticsearch.watcher.trigger.schedule.Schedules.interval;
import static org.hamcrest.Matchers.equalTo;

/**
 */
public class HttpInputIntegrationTest extends AbstractWatcherIntegrationTests {

    @Override
    protected Settings nodeSettings(int nodeOrdinal) {
        return ImmutableSettings.builder()
                .put(InternalNode.HTTP_ENABLED, true)
                .put(super.nodeSettings(nodeOrdinal))
                .build();
    }

    @Override
    protected boolean enableShield() {
        return false;
    }

    @Test
    public void testHttpInput() throws Exception {
        ScriptServiceProxy sc = scriptService();
        createIndex("index");
        client().prepareIndex("index", "type", "id").setSource("{}").setRefresh(true).get();

        InetSocketAddress address = internalTestCluster().httpAddresses()[0];
        TemplatedHttpRequest.SourceBuilder requestBuilder = new TemplatedHttpRequest.SourceBuilder()
                .setHost(address.getHostName())
                .setPort(address.getPort())
                .setPath(new ScriptTemplate(sc, "/index/_search"))
                .setBody(new ScriptTemplate(sc, jsonBuilder().startObject().field("size", 1).endObject().string()));
        if (shieldEnabled()) {
            requestBuilder.setAuth(new BasicAuth("test", "changeme"));
        }
        WatchSourceBuilder source = watchSourceBuilder()
                .trigger(TriggerBuilders.schedule(interval("5s")))
                .input(InputBuilders.httpInput(requestBuilder))
                .condition(scriptCondition("ctx.payload.hits.total == 1"))
                .addAction(indexAction("idx", "action"));
        watcherClient().preparePutWatch("_name")
                .source(source)
                .get();

        if (timeWarped()) {
            timeWarp().scheduler().trigger("_name");
            refresh();
        }
        assertWatchWithMinimumPerformedActionsCount("_name", 1, false);
    }

    @Test
    public void testInputFiltering() throws Exception {
        WatcherClient watcherClient = watcherClient();
        createIndex("idx");
        // Have a sample document in the index, the watch is going to evaluate
        client().prepareIndex("idx", "type").setSource("field", "value").get();
        refresh();

        ScriptServiceProxy sc = scriptService();
        InetSocketAddress address = internalTestCluster().httpAddresses()[0];
        String body = jsonBuilder().prettyPrint().startObject()
                    .field("query").value(termQuery("field", "value"))
                .endObject().string();
        TemplatedHttpRequest.SourceBuilder requestBuilder = new TemplatedHttpRequest.SourceBuilder()
                .setHost(address.getHostName())
                .setPort(address.getPort())
                .setPath(new ScriptTemplate(sc, "/idx/_search"))
                .setBody(new ScriptTemplate(sc, body));
        if (shieldEnabled()) {
            requestBuilder.setAuth(new BasicAuth("test", "changeme"));
        }

        watcherClient.preparePutWatch("_name1")
                .source(watchSourceBuilder()
                        .trigger(schedule(interval(5, IntervalSchedule.Interval.Unit.SECONDS)))
                        .input(InputBuilders.httpInput(requestBuilder).addExtractKey("hits.total"))
                        .condition(scriptCondition("ctx.payload.hits.total == 1")))
                .get();

        // in this watcher the condition will fail, because max_score isn't extracted, only total:
        watcherClient.preparePutWatch("_name2")
                .source(watchSourceBuilder()
                        .trigger(schedule(interval(5, IntervalSchedule.Interval.Unit.SECONDS)))
                        .input(InputBuilders.httpInput(requestBuilder).addExtractKey("hits.total"))
                        .condition(scriptCondition("ctx.payload.hits.max_score >= 0")))
                .get();

        if (timeWarped()) {
            timeWarp().scheduler().trigger("_name1");
            timeWarp().scheduler().trigger("_name2");
            refresh();
        }

        assertWatchWithMinimumPerformedActionsCount("_name1", 1, false);
        assertWatchWithNoActionNeeded("_name2", 1);

        // Check that the input result payload has been filtered
        refresh();
        SearchResponse searchResponse = client().prepareSearch(HistoryStore.INDEX_PREFIX + "*")
                .setIndicesOptions(IndicesOptions.lenientExpandOpen())
                .setQuery(matchQuery("watch_name", "_name1"))
                .setSize(1)
                .get();
        assertHitCount(searchResponse, 1);
        Map payload = (Map) ((Map)((Map)((Map) searchResponse.getHits().getAt(0).sourceAsMap().get("watch_execution")).get("input_result")).get("http")).get("payload");
        assertThat(payload.size(), equalTo(1));
        assertThat(((Map) payload.get("hits")).size(), equalTo(1));
        assertThat((Integer) ((Map) payload.get("hits")).get("total"), equalTo(1));
    }

}
