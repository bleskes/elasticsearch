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

package org.elasticsearch.xpack.watcher.actions;

import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xpack.watcher.condition.CompareCondition;
import org.elasticsearch.xpack.watcher.execution.ExecutionState;
import org.elasticsearch.xpack.watcher.history.HistoryStore;
import org.elasticsearch.xpack.watcher.history.WatchRecord;
import org.elasticsearch.xpack.watcher.test.AbstractWatcherIntegrationTestCase;
import org.elasticsearch.xpack.watcher.transport.actions.put.PutWatchResponse;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertHitCount;
import static org.elasticsearch.xpack.watcher.actions.ActionBuilders.indexAction;
import static org.elasticsearch.xpack.watcher.client.WatchSourceBuilders.watchBuilder;
import static org.elasticsearch.xpack.watcher.input.InputBuilders.searchInput;
import static org.elasticsearch.xpack.watcher.test.WatcherTestUtils.templateRequest;
import static org.elasticsearch.xpack.watcher.transform.TransformBuilders.searchTransform;
import static org.elasticsearch.xpack.watcher.trigger.TriggerBuilders.schedule;
import static org.elasticsearch.xpack.watcher.trigger.schedule.Schedules.interval;
import static org.hamcrest.Matchers.is;

public class TimeThrottleIntegrationTests extends AbstractWatcherIntegrationTestCase {

    @Before
    public void indexTestDoc() {
        createIndex("actions", "events");
        ensureGreen("actions", "events");

        IndexResponse eventIndexResponse = client().prepareIndex("events", "event")
                .setSource("level", "error")
                .get();
        assertEquals(DocWriteResponse.Result.CREATED, eventIndexResponse.getResult());
        refresh();
    }

    @Override
    protected boolean enableSecurity() {
        return false;
    }

    @Override
    protected boolean timeWarped() {
        return true;
    }

    public void testTimeThrottle() throws Exception {
        String id = randomAlphaOfLength(20);
        PutWatchResponse putWatchResponse = watcherClient().preparePutWatch()
                .setId(id)
                .setSource(watchBuilder()
                        .trigger(schedule(interval("5s")))
                        .input(searchInput(templateRequest(new SearchSourceBuilder(), "events")))
                        .condition(new CompareCondition("ctx.payload.hits.total", CompareCondition.Op.GT, 0L))
                        .transform(searchTransform(templateRequest(new SearchSourceBuilder(), "events")))
                        .addAction("_id", indexAction("actions", "action"))
                        .defaultThrottlePeriod(TimeValue.timeValueSeconds(30)))
                .get();
        assertThat(putWatchResponse.isCreated(), is(true));

        timeWarp().clock().setTime(DateTime.now(DateTimeZone.UTC));
        timeWarp().scheduler().trigger(id);
        refresh();

        // the first fire should work
        long actionsCount = docCount("actions", "action", matchAllQuery());
        assertThat(actionsCount, is(1L));

        timeWarp().clock().fastForwardSeconds(5);
        timeWarp().scheduler().trigger(id);
        refresh();

        // the last fire should have been throttled, so number of actions shouldn't change
        actionsCount = docCount("actions", "action", matchAllQuery());
        assertThat(actionsCount, is(1L));

        timeWarp().clock().fastForwardSeconds(30);
        timeWarp().scheduler().trigger(id);
        refresh();

        // the last fire occurred passed the throttle period, so a new action should have been added
        actionsCount = docCount("actions", "action", matchAllQuery());
        assertThat(actionsCount, is(2L));

        SearchResponse response = client().prepareSearch(HistoryStore.INDEX_PREFIX_WITH_TEMPLATE + "*")
                .setSource(new SearchSourceBuilder().query(QueryBuilders.boolQuery()
                        .must(matchQuery(WatchRecord.STATE.getPreferredName(), ExecutionState.THROTTLED.id()))
                        .must(termQuery("watch_id", id))))
                .get();
        assertHitCount(response, 1L);
    }

    public void testTimeThrottleDefaults() throws Exception {
        String id = randomAlphaOfLength(30);
        PutWatchResponse putWatchResponse = watcherClient().preparePutWatch()
                .setId(id)
                .setSource(watchBuilder()
                        .trigger(schedule(interval("1s")))
                        .input(searchInput(templateRequest(new SearchSourceBuilder(), "events")))
                        .condition(new CompareCondition("ctx.payload.hits.total", CompareCondition.Op.GT, 0L))
                        .transform(searchTransform(templateRequest(new SearchSourceBuilder(), "events")))
                        .addAction("_id", indexAction("actions", "action")))
                .get();
        assertThat(putWatchResponse.isCreated(), is(true));

        timeWarp().clock().setTime(DateTime.now(DateTimeZone.UTC));

        timeWarp().scheduler().trigger(id);
        refresh();

        // the first trigger should work
        long actionsCount = docCount("actions", "action", matchAllQuery());
        assertThat(actionsCount, is(1L));

        timeWarp().clock().fastForwardSeconds(2);
        timeWarp().scheduler().trigger(id);
        refresh("actions");

        // the last fire should have been throttled, so number of actions shouldn't change
        actionsCount = docCount("actions", "action", matchAllQuery());
        assertThat(actionsCount, is(1L));

        timeWarp().clock().fastForwardSeconds(10);
        timeWarp().scheduler().trigger(id);
        refresh("actions");

        // the last fire occurred passed the throttle period, so a new action should have been added
        actionsCount = docCount("actions", "action", matchAllQuery());
        assertThat(actionsCount, is(2L));

        SearchResponse searchResponse = client().prepareSearch(HistoryStore.INDEX_PREFIX_WITH_TEMPLATE + "*")
                .setSource(new SearchSourceBuilder().query(QueryBuilders.boolQuery()
                        .must(matchQuery(WatchRecord.STATE.getPreferredName(), ExecutionState.THROTTLED.id()))
                        .must(termQuery("watch_id", id))))
                .get();
        assertHitCount(searchResponse, 1);
    }
}
