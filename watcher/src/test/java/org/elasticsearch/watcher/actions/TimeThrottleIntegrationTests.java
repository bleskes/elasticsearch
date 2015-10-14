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

package org.elasticsearch.watcher.actions;


import org.apache.lucene.util.LuceneTestCase.AwaitsFix;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.watcher.client.WatcherClient;
import org.elasticsearch.watcher.execution.ExecutionState;
import org.elasticsearch.watcher.history.HistoryStore;
import org.elasticsearch.watcher.history.WatchRecord;
import org.elasticsearch.watcher.test.AbstractWatcherIntegrationTestCase;
import org.elasticsearch.watcher.transport.actions.put.PutWatchResponse;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.concurrent.TimeUnit;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.watcher.actions.ActionBuilders.indexAction;
import static org.elasticsearch.watcher.client.WatchSourceBuilders.watchBuilder;
import static org.elasticsearch.watcher.condition.ConditionBuilders.scriptCondition;
import static org.elasticsearch.watcher.input.InputBuilders.searchInput;
import static org.elasticsearch.watcher.test.WatcherTestUtils.matchAllRequest;
import static org.elasticsearch.watcher.transform.TransformBuilders.searchTransform;
import static org.elasticsearch.watcher.trigger.TriggerBuilders.schedule;
import static org.elasticsearch.watcher.trigger.schedule.Schedules.interval;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

/**
 */
@AwaitsFix(bugUrl = "https://github.com/elastic/x-plugins/issues/724")
public class TimeThrottleIntegrationTests extends AbstractWatcherIntegrationTestCase {
    private IndexResponse indexTestDoc() {
        createIndex("actions", "events");
        ensureGreen("actions", "events");

        IndexResponse eventIndexResponse = client().prepareIndex("events", "event")
                .setSource("level", "error")
                .get();
        assertThat(eventIndexResponse.isCreated(), is(true));
        refresh();
        return eventIndexResponse;
    }

    @Override
    protected boolean enableShield() {
        return false;
    }

    public void testTimeThrottle() throws Exception {
        WatcherClient watcherClient = watcherClient();
        indexTestDoc();

        PutWatchResponse putWatchResponse = watcherClient.preparePutWatch()
                .setId("_name")
                .setSource(watchBuilder()
                        .trigger(schedule(interval("5s")))
                        .input(searchInput(matchAllRequest().indices("events")))
                        .condition(scriptCondition("ctx.payload.hits.total > 0"))
                        .transform(searchTransform(matchAllRequest().indices("events")))
                        .addAction("_id", indexAction("actions", "action"))
                        .defaultThrottlePeriod(TimeValue.timeValueSeconds(30)))
                .get();
        assertThat(putWatchResponse.isCreated(), is(true));

        if (timeWarped()) {
            timeWarp().clock().setTime(DateTime.now(DateTimeZone.UTC));

            timeWarp().scheduler().trigger("_name");
            refresh();

            // the first fire should work
            long actionsCount = docCount("actions", "action", matchAllQuery());
            assertThat(actionsCount, is(1L));

            timeWarp().clock().fastForwardSeconds(5);
            timeWarp().scheduler().trigger("_name");
            refresh();

            // the last fire should have been throttled, so number of actions shouldn't change
            actionsCount = docCount("actions", "action", matchAllQuery());
            assertThat(actionsCount, is(1L));

            timeWarp().clock().fastForwardSeconds(30);
            timeWarp().scheduler().trigger("_name");
            refresh();

            // the last fire occurred passed the throttle period, so a new action should have been added
            actionsCount = docCount("actions", "action", matchAllQuery());
            assertThat(actionsCount, is(2L));

            long throttledCount = docCount(HistoryStore.INDEX_PREFIX + "*", null,
                    matchQuery(WatchRecord.Field.STATE.getPreferredName(), ExecutionState.THROTTLED.id()));
            assertThat(throttledCount, is(1L));

        } else {
            Thread.sleep(TimeUnit.SECONDS.toMillis(5));
            // the first fire should work so we should have a single action in the actions index
            assertBusy(new Runnable() {
                @Override
                public void run() {
                    refresh();
                    long actionsCount = docCount("actions", "action", matchAllQuery());
                    assertThat(actionsCount, is(1L));
                }
            }, 5, TimeUnit.SECONDS);
            Thread.sleep(TimeUnit.SECONDS.toMillis(5));
            // we should still be within the throttling period... so the number of actions shouldn't change
            assertBusy(new Runnable() {
                @Override
                public void run() {
                    refresh();
                    long actionsCount = docCount("actions", "action", matchAllQuery());
                    assertThat(actionsCount, is(1L));

                    long throttledCount = docCount(HistoryStore.INDEX_PREFIX + "*", null,
                            matchQuery(WatchRecord.Field.STATE.getPreferredName(), ExecutionState.THROTTLED.id()));
                    assertThat(throttledCount, greaterThanOrEqualTo(1L));
                }
            }, 5, TimeUnit.SECONDS);
        }
    }

    public void testTimeThrottleDefaults() throws Exception {
        WatcherClient watcherClient = watcherClient();
        indexTestDoc();

        PutWatchResponse putWatchResponse = watcherClient.preparePutWatch()
                .setId("_name")
                .setSource(watchBuilder()
                        .trigger(schedule(interval("1s")))
                        .input(searchInput(matchAllRequest().indices("events")))
                        .condition(scriptCondition("ctx.payload.hits.total > 0"))
                        .transform(searchTransform(matchAllRequest().indices("events")))
                        .addAction("_id", indexAction("actions", "action")))
                .get();
        assertThat(putWatchResponse.isCreated(), is(true));

        if (timeWarped()) {
            timeWarp().clock().setTime(DateTime.now(DateTimeZone.UTC));

            timeWarp().scheduler().trigger("_name");
            refresh();

            // the first trigger should work
            long actionsCount = docCount("actions", "action", matchAllQuery());
            assertThat(actionsCount, is(1L));

            timeWarp().clock().fastForwardSeconds(2);
            timeWarp().scheduler().trigger("_name");
            refresh();

            // the last fire should have been throttled, so number of actions shouldn't change
            actionsCount = docCount("actions", "action", matchAllQuery());
            assertThat(actionsCount, is(1L));

            timeWarp().clock().fastForwardSeconds(10);
            timeWarp().scheduler().trigger("_name");
            refresh();

            // the last fire occurred passed the throttle period, so a new action should have been added
            actionsCount = docCount("actions", "action", matchAllQuery());
            assertThat(actionsCount, is(2L));

            long throttledCount = docCount(HistoryStore.INDEX_PREFIX + "*", null,
                    matchQuery(WatchRecord.Field.STATE.getPreferredName(), ExecutionState.THROTTLED.id()));
            assertThat(throttledCount, is(1L));
        }
    }

}
