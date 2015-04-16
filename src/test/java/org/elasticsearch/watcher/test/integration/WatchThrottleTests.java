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

import com.carrotsearch.randomizedtesting.annotations.Repeat;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.joda.time.DateTimeZone;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.watcher.client.WatcherClient;
import org.elasticsearch.watcher.history.HistoryStore;
import org.elasticsearch.watcher.history.WatchRecord;
import org.elasticsearch.watcher.test.AbstractWatcherIntegrationTests;
import org.elasticsearch.watcher.transport.actions.ack.AckWatchResponse;
import org.elasticsearch.watcher.transport.actions.get.GetWatchRequest;
import org.elasticsearch.watcher.transport.actions.get.GetWatchResponse;
import org.elasticsearch.watcher.transport.actions.put.PutWatchResponse;
import org.elasticsearch.watcher.watch.Watch;
import org.elasticsearch.watcher.watch.WatchStore;
import org.hamcrest.Matchers;
import org.junit.Test;

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
import static org.elasticsearch.watcher.trigger.schedule.Schedules.cron;
import static org.elasticsearch.watcher.trigger.schedule.Schedules.interval;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.IsEqual.equalTo;

/**
 */
public class WatchThrottleTests extends AbstractWatcherIntegrationTests {

    @Test
    public void testAckThrottle() throws Exception {
        WatcherClient watcherClient = watcherClient();
        IndexResponse eventIndexResponse = indexTestDoc();

        PutWatchResponse putWatchResponse = watcherClient.preparePutWatch()
                .setId("_name")
                .setSource(watchBuilder()
                        .trigger(schedule(cron("0/5 * * * * ? *")))
                        .input(searchInput(matchAllRequest().indices("events")))
                        .condition(scriptCondition("ctx.payload.hits.total > 0"))
                        .transform(searchTransform(matchAllRequest().indices("events")))
                        .addAction("_id", indexAction("actions", "action")))
                .get();

        assertThat(putWatchResponse.isCreated(), is(true));

        if (timeWarped()) {
            timeWarp().scheduler().trigger("_name", 4, TimeValue.timeValueSeconds(5));
        } else {
            Thread.sleep(20000);
        }
        AckWatchResponse ackResponse = watcherClient.prepareAckWatch("_name").get();
        assertThat(ackResponse.getStatus().ackStatus().state(), is(Watch.Status.AckStatus.State.ACKED));

        refresh();
        long countAfterAck = docCount("actions", "action", matchAllQuery());
        assertThat(countAfterAck, greaterThanOrEqualTo((long) 1));

        if (timeWarped()) {
            timeWarp().scheduler().trigger("_name", 4, TimeValue.timeValueSeconds(5));
        } else {
            Thread.sleep(20000);
        }
        refresh();

        // There shouldn't be more actions in the index after we ack the watch, even though the watch was triggered
        long countAfterPostAckFires = docCount("actions", "action", matchAllQuery());
        assertThat(countAfterPostAckFires, equalTo(countAfterAck));

        //Now delete the event and the ack state should change to AWAITS_EXECUTION
        DeleteResponse response = client().prepareDelete("events", "event", eventIndexResponse.getId()).get();
        assertThat(response.isFound(), is(true));
        refresh();

        if (timeWarped()) {
            timeWarp().scheduler().trigger("_name", 4, TimeValue.timeValueSeconds(5));
        } else {
            Thread.sleep(20000);
        }

        GetWatchResponse getWatchResponse = watcherClient.prepareGetWatch("_name").get();
        assertThat(getWatchResponse.isFound(), is(true));

        Watch parsedWatch = watchParser().parse(getWatchResponse.getId(), true,
                getWatchResponse.getSource());
        assertThat(parsedWatch.status().ackStatus().state(), is(Watch.Status.AckStatus.State.AWAITS_EXECUTION));

        long throttledCount = docCount(HistoryStore.INDEX_PREFIX + "*", null,
                matchQuery(WatchRecord.Parser.STATE_FIELD.getPreferredName(), WatchRecord.State.THROTTLED.id()));
        assertThat(throttledCount, greaterThan(0L));
    }

    public IndexResponse indexTestDoc() {
        createIndex("actions", "events");
        ensureGreen("actions", "events");

        IndexResponse eventIndexResponse = client().prepareIndex("events", "event")
                .setSource("level", "error")
                .get();
        assertThat(eventIndexResponse.isCreated(), is(true));
        refresh();
        return eventIndexResponse;
    }


    @Test @Repeat(iterations = 10)
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
                        .throttlePeriod(TimeValue.timeValueSeconds(10)))
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

            timeWarp().clock().fastForwardSeconds(10);
            timeWarp().scheduler().trigger("_name");
            refresh();

            // the last fire occurred passed the throttle period, so a new action should have been added
            actionsCount = docCount("actions", "action", matchAllQuery());
            assertThat(actionsCount, is(2L));

            long throttledCount = docCount(HistoryStore.INDEX_PREFIX + "*", null,
                    matchQuery(WatchRecord.Parser.STATE_FIELD.getPreferredName(), WatchRecord.State.THROTTLED.id()));
            assertThat(throttledCount, is(1L));

        } else {

            Thread.sleep(TimeUnit.SECONDS.toMillis(2));
            refresh();

            // the first fire should work so we should have a single action in the actions index
            long actionsCount = docCount("actions", "action", matchAllQuery());
            assertThat(actionsCount, is(1L));

            Thread.sleep(TimeUnit.SECONDS.toMillis(5));

            // we should still be within the throttling period... so the number of actions shouldn't change
            actionsCount = docCount("actions", "action", matchAllQuery());
            assertThat(actionsCount, is(1L));

            long throttledCount = docCount(HistoryStore.INDEX_PREFIX + "*", null,
                    matchQuery(WatchRecord.Parser.STATE_FIELD.getPreferredName(), WatchRecord.State.THROTTLED.id()));
            assertThat(throttledCount, greaterThanOrEqualTo(1L));
        }
    }

    @Test
    @Repeat(iterations = 2)
    public void test_ack_with_restart() throws Exception {
        WatcherClient watcherClient = watcherClient();
        indexTestDoc();
        PutWatchResponse putWatchResponse = watcherClient.preparePutWatch()
                .setId("_name")
                .setSource(watchBuilder()
                        .trigger(schedule(cron("0/5 * * * * ? *")))
                        .input(searchInput(matchAllRequest().indices("events")))
                        .condition(scriptCondition("ctx.payload.hits.total > 0"))
                        .transform(searchTransform(matchAllRequest().indices("events")))
                        .addAction("_id", indexAction("actions", "action")))
                .get();

        assertThat(putWatchResponse.isCreated(), is(true));

        if (timeWarped()) {
            timeWarp().scheduler().trigger("_name", 4, TimeValue.timeValueSeconds(5));
        } else {
            Thread.sleep(20000);
        }

        if (randomBoolean()) {
            stopWatcher();
            ensureWatcherStopped();
            startWatcher();
            ensureWatcherStarted();
        }

        AckWatchResponse ackResponse = watcherClient.prepareAckWatch("_name").get();
        assertThat(ackResponse.getStatus().ackStatus().state(), is(Watch.Status.AckStatus.State.ACKED));

        refresh();
        long countAfterAck = docCount("actions", "action", matchAllQuery());
        assertThat(countAfterAck, greaterThanOrEqualTo((long) 1));

        if (randomBoolean()) {
            stopWatcher();
            ensureWatcherStopped();
            startWatcher();
            ensureWatcherStarted();
        }

        GetWatchResponse watchResponse = watcherClient.getWatch(new GetWatchRequest("_name")).actionGet();
        Watch watch = watchParser().parse("_name", true, watchResponse.getSource());
        assertThat(watch.status().ackStatus().state(), Matchers.equalTo(Watch.Status.AckStatus.State.ACKED));

        refresh();
        GetResponse getResponse = client().get(new GetRequest(WatchStore.INDEX, WatchStore.DOC_TYPE, "_name")).actionGet();
        Watch indexedWatch = watchParser().parse("_name", true, getResponse.getSourceAsBytesRef());
        assertThat(watch.status().ackStatus().state(), Matchers.equalTo(indexedWatch.status().ackStatus().state()));


        if (timeWarped()) {
            timeWarp().scheduler().trigger("_name", 4, TimeValue.timeValueSeconds(5));
        } else {
            Thread.sleep(20000);
        }
        refresh();

        // There shouldn't be more actions in the index after we ack the watch, even though the watch was triggered
        long countAfterPostAckFires = docCount("actions", "action", matchAllQuery());
        assertThat(countAfterPostAckFires, equalTo(countAfterAck));
    }
}
