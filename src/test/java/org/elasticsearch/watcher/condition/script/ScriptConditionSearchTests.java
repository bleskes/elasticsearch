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

package org.elasticsearch.watcher.condition.script;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.ShardSearchFailure;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.text.StringText;
import org.elasticsearch.env.Environment;
import org.elasticsearch.node.settings.NodeSettingsService;
import org.elasticsearch.script.ScriptEngineService;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.script.groovy.GroovyScriptEngineService;
import org.elasticsearch.search.SearchShardTarget;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogram;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.internal.InternalSearchHit;
import org.elasticsearch.search.internal.InternalSearchHits;
import org.elasticsearch.search.internal.InternalSearchResponse;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.watcher.ResourceWatcherService;
import org.elasticsearch.watcher.support.Script;
import org.elasticsearch.watcher.support.init.proxy.ScriptServiceProxy;
import org.elasticsearch.watcher.test.AbstractWatcherIntegrationTests;
import org.elasticsearch.watcher.watch.Payload;
import org.elasticsearch.watcher.execution.WatchExecutionContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.elasticsearch.watcher.test.WatcherTestUtils.mockExecutionContext;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

/**
 */
public class ScriptConditionSearchTests extends AbstractWatcherIntegrationTests {

    private ThreadPool tp = null;
    private ScriptServiceProxy scriptService;

    @Before
    public void init() throws Exception {
        tp = new ThreadPool(ThreadPool.Names.SAME);
        Settings settings = ImmutableSettings.settingsBuilder()
                .put(ScriptService.DISABLE_DYNAMIC_SCRIPTING_SETTING, "none")
                .build();
        GroovyScriptEngineService groovyScriptEngineService = new GroovyScriptEngineService(settings);
        Set<ScriptEngineService> engineServiceSet = new HashSet<>();
        engineServiceSet.add(groovyScriptEngineService);
        NodeSettingsService nodeSettingsService = new NodeSettingsService(settings);
        scriptService = ScriptServiceProxy.of(new ScriptService(settings, new Environment(), engineServiceSet, new ResourceWatcherService(settings, tp), nodeSettingsService));
    }

    @After
    public void cleanup() {
        tp.shutdownNow();
    }

    @Test
    public void testExecute_withAggs() throws Exception {

        client().admin().indices().prepareCreate("my-index")
                .addMapping("my-type", "_timestamp", "enabled=true")
                .get();

        client().prepareIndex("my-index", "my-type").setTimestamp("2005-01-01T00:00").setSource("{}").get();
        client().prepareIndex("my-index", "my-type").setTimestamp("2005-01-01T00:10").setSource("{}").get();
        client().prepareIndex("my-index", "my-type").setTimestamp("2005-01-01T00:20").setSource("{}").get();
        client().prepareIndex("my-index", "my-type").setTimestamp("2005-01-01T00:30").setSource("{}").get();
        refresh();

        SearchResponse response = client().prepareSearch("my-index")
                .addAggregation(AggregationBuilders.dateHistogram("rate").field("_timestamp").interval(DateHistogram.Interval.HOUR).order(Histogram.Order.COUNT_DESC))
                .get();

        ExecutableScriptCondition condition = new ExecutableScriptCondition(new ScriptCondition(new Script("ctx.payload.aggregations.rate.buckets[0]?.doc_count >= 5")), logger, scriptService);

        WatchExecutionContext ctx = mockExecutionContext("_name", new Payload.XContent(response));
        assertFalse(condition.execute(ctx).met());

        client().prepareIndex("my-index", "my-type").setTimestamp("2005-01-01T00:40").setSource("{}").get();
        refresh();

        response = client().prepareSearch("my-index")
                .addAggregation(AggregationBuilders.dateHistogram("rate").field("_timestamp").interval(DateHistogram.Interval.HOUR).order(Histogram.Order.COUNT_DESC))
                .get();

        ctx = mockExecutionContext("_name", new Payload.XContent(response));
        assertThat(condition.execute(ctx).met(), is(true));
    }

    @Test
    public void testExecute_accessHits() throws Exception {
        ExecutableScriptCondition condition = new ExecutableScriptCondition(new ScriptCondition(new Script("ctx.payload.hits?.hits[0]?._score == 1.0")), logger, scriptService);
        InternalSearchHit hit = new InternalSearchHit(0, "1", new StringText("type"), null);
        hit.score(1f);
        hit.shard(new SearchShardTarget("a", "a", 0));

        InternalSearchResponse internalSearchResponse = new InternalSearchResponse(new InternalSearchHits(new InternalSearchHit[]{hit}, 1l, 1f), null, null, null, false, null);
        SearchResponse response = new SearchResponse(internalSearchResponse, "", 3, 3, 500l, new ShardSearchFailure[0]);

        WatchExecutionContext ctx = mockExecutionContext("_watch_name", new Payload.XContent(response));
        assertThat(condition.execute(ctx).met(), is(true));
        hit.score(2f);
        when(ctx.payload()).thenReturn(new Payload.XContent(response));
        assertThat(condition.execute(ctx).met(), is(false));
    }

}
