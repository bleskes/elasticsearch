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

import org.apache.lucene.util.LuceneTestCase;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.watcher.support.clock.SystemClock;
import org.joda.time.DateTime;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.test.junit.annotations.TestLogging;
import org.elasticsearch.watcher.WatcherException;
import org.elasticsearch.watcher.client.WatchSourceBuilder;
import org.elasticsearch.watcher.client.WatcherClient;
import org.elasticsearch.watcher.condition.ConditionBuilders;
import org.elasticsearch.watcher.history.HistoryStore;
import org.elasticsearch.watcher.support.WatcherUtils;
import org.elasticsearch.watcher.support.template.Template;
import org.elasticsearch.watcher.support.xcontent.XContentSource;
import org.elasticsearch.watcher.test.AbstractWatcherIntegrationTests;
import org.elasticsearch.watcher.transport.actions.delete.DeleteWatchResponse;
import org.elasticsearch.watcher.transport.actions.get.GetWatchResponse;
import org.elasticsearch.watcher.transport.actions.put.PutWatchResponse;
import org.elasticsearch.watcher.trigger.schedule.IntervalSchedule;
import org.elasticsearch.watcher.trigger.schedule.ScheduleTriggerException;
import org.elasticsearch.watcher.trigger.schedule.Schedules;
import org.elasticsearch.watcher.trigger.schedule.support.MonthTimes;
import org.elasticsearch.watcher.trigger.schedule.support.WeekTimes;
import org.elasticsearch.watcher.watch.WatchStore;
import org.junit.Test;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.elasticsearch.search.builder.SearchSourceBuilder.searchSource;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertAcked;
import static org.elasticsearch.test.hamcrest.ElasticsearchAssertions.assertHitCount;
import static org.elasticsearch.watcher.actions.ActionBuilders.indexAction;
import static org.elasticsearch.watcher.actions.ActionBuilders.loggingAction;
import static org.elasticsearch.watcher.client.WatchSourceBuilders.watchBuilder;
import static org.elasticsearch.watcher.condition.ConditionBuilders.alwaysCondition;
import static org.elasticsearch.watcher.condition.ConditionBuilders.scriptCondition;
import static org.elasticsearch.watcher.input.InputBuilders.searchInput;
import static org.elasticsearch.watcher.input.InputBuilders.simpleInput;
import static org.elasticsearch.watcher.test.WatcherTestUtils.newInputSearchRequest;
import static org.elasticsearch.watcher.trigger.TriggerBuilders.schedule;
import static org.elasticsearch.watcher.trigger.schedule.Schedules.*;
import static org.hamcrest.Matchers.*;

/**
 */
@TestLogging("watcher.trigger.schedule:TRACE")
public class BasicWatcherTests extends AbstractWatcherIntegrationTests {

    @Test
    public void testIndexWatch() throws Exception {
        WatcherClient watcherClient = watcherClient();
        createIndex("idx");
        // Have a sample document in the index, the watch is going to evaluate
        client().prepareIndex("idx", "type").setSource("field", "value").get();
        refresh();
        SearchRequest searchRequest = newInputSearchRequest("idx").source(searchSource().query(termQuery("field", "value")));
        watcherClient.preparePutWatch("_name")
                .setSource(watchBuilder()
                        .trigger(schedule(interval(5, IntervalSchedule.Interval.Unit.SECONDS)))
                        .input(searchInput(searchRequest))
                        .condition(scriptCondition("ctx.payload.hits.total == 1"))
                        .addAction("_logger", loggingAction("\n\n************\n" +
                                        "total hits: {{ctx.payload.hits.total}}\n" +
                                        "************\n")
                                        .setCategory("_category")))
                .get();

        if (timeWarped()) {
            timeWarp().scheduler().trigger("_name");
            refresh();
        }

        assertWatchWithMinimumPerformedActionsCount("_name", 1);

        GetWatchResponse getWatchResponse = watcherClient().prepareGetWatch().setId("_name").get();
        assertThat(getWatchResponse.isFound(), is(true));
        assertThat(getWatchResponse.getSource(), notNullValue());
    }

    @Test
    public void testIndexWatch_registerWatchBeforeTargetIndex() throws Exception {
        WatcherClient watcherClient = watcherClient();
        SearchRequest searchRequest = newInputSearchRequest("idx").source(searchSource().query(termQuery("field", "value")));
        watcherClient.preparePutWatch("_name")
                .setSource(watchBuilder()
                        .trigger(schedule(interval(5, IntervalSchedule.Interval.Unit.SECONDS)))
                        .input(searchInput(searchRequest))
                        .condition(scriptCondition("ctx.payload.hits.total == 1")))
                .get();

        if (timeWarped()) {
            timeWarp().scheduler().trigger("_name");
            refresh();
        }

        // The watch's condition won't meet because there is no data that matches with the query
        assertWatchWithNoActionNeeded("_name", 1);

        // Index sample doc after we register the watch and the watch's condition should meet
        client().prepareIndex("idx", "type").setSource("field", "value").get();
        refresh();

        if (timeWarped()) {
            timeWarp().clock().fastForwardSeconds(5);
            timeWarp().scheduler().trigger("_name");
            refresh();
        }

        assertWatchWithMinimumPerformedActionsCount("_name", 1);
    }

    @Test
    public void testDeleteWatch() throws Exception {
        WatcherClient watcherClient = watcherClient();
        SearchRequest searchRequest = newInputSearchRequest("idx").source(searchSource().query(matchAllQuery()));
        PutWatchResponse indexResponse = watcherClient.preparePutWatch("_name")
                .setSource(watchBuilder()
                        .trigger(schedule(interval(5, IntervalSchedule.Interval.Unit.SECONDS)))
                        .input(searchInput(searchRequest))
                        .condition(scriptCondition("ctx.payload.hits.total == 1")))
                .get();
        assertThat(indexResponse.isCreated(), is(true));

        if (!timeWarped()) {
            // Although there is no added benefit in this test for waiting for the watch to fire, however
            // we need to wait here because of a test timing issue. When we tear down a test we delete the watch and delete all
            // indices, but there may still be inflight fired watches, which may trigger the watch history to be created again, before
            // we finished the tear down phase.
            assertWatchWithNoActionNeeded("_name", 1);
        }

        DeleteWatchResponse deleteWatchResponse = watcherClient.prepareDeleteWatch("_name").get();
        assertThat(deleteWatchResponse, notNullValue());
        assertThat(deleteWatchResponse.isFound(), is(true));

        refresh();
        assertHitCount(client().prepareCount(WatchStore.INDEX).get(), 0l);

        // Deleting the same watch for the second time
        deleteWatchResponse = watcherClient.prepareDeleteWatch("_name").get();
        assertThat(deleteWatchResponse, notNullValue());
        assertThat(deleteWatchResponse.isFound(), is(false));
    }

    @Test
    public void testMalformedWatch() throws Exception {
        WatcherClient watcherClient = watcherClient();
        createIndex("idx");
        // Have a sample document in the index, the watch is going to evaluate
        client().prepareIndex("idx", "type").setSource("field", "value").get();
        XContentBuilder watchSource = jsonBuilder();

        watchSource.startObject();
        watchSource.field("unknown_field", "x");
        watchSource.startObject("schedule").field("cron", "0/5 * * * * ? *").endObject();

        watchSource.startObject("condition").startObject("script").field("script", "return true").field("request");
        WatcherUtils.writeSearchRequest(newInputSearchRequest(), watchSource, ToXContent.EMPTY_PARAMS);
        watchSource.endObject();

        watchSource.endObject();
        try {
            watcherClient.preparePutWatch("_name")
                    .setSource(watchSource.bytes())
                    .get();
            fail();
        } catch (WatcherException e) {
            // In watch store we fail parsing if an watch contains undefined fields.
        }
        try {
            client().prepareIndex(WatchStore.INDEX, WatchStore.DOC_TYPE, "_name")
                    .setSource(watchSource)
                    .get();
            fail();
        } catch (Exception e) {
            // The watch index template the mapping is defined as strict
        }
    }

    @Test
    public void testModifyWatches() throws Exception {
        SearchRequest searchRequest = newInputSearchRequest("idx")
                .source(searchSource().query(matchAllQuery()));

        WatchSourceBuilder source = watchBuilder()
                .trigger(schedule(interval("5s")))
                .input(searchInput(searchRequest))
                .addAction("_id", indexAction("idx", "action"));

        watcherClient().preparePutWatch("_name")
                .setSource(source.condition(scriptCondition("ctx.payload.hits.total == 1")))
                .get();

        if (timeWarped()) {
            timeWarp().clock().fastForwardSeconds(5);
            timeWarp().scheduler().trigger("_name");
            refresh();
        }
        assertWatchWithMinimumPerformedActionsCount("_name", 0, false);

        watcherClient().preparePutWatch("_name")
                .setSource(source.condition(scriptCondition("ctx.payload.hits.total == 0")))
                .get();

        if (timeWarped()) {
            timeWarp().clock().fastForwardSeconds(5);
            timeWarp().scheduler().trigger("_name");
            refresh();
        }
        assertWatchWithMinimumPerformedActionsCount("_name", 1, false);

        watcherClient().preparePutWatch("_name")
                .setSource(source
                        .trigger(schedule(Schedules.cron("0/1 * * * * ? 2020")))
                        .condition(scriptCondition("ctx.payload.hits.total == 0")))
                .get();

        if (timeWarped()) {
            timeWarp().clock().fastForwardSeconds(5);
            timeWarp().scheduler().trigger("_name");
            refresh();
        } else {
            Thread.sleep(1000);
        }

        long count =  findNumberOfPerformedActions("_name");

        if (timeWarped()) {
            timeWarp().clock().fastForwardSeconds(5);
            timeWarp().scheduler().trigger("_name");
            refresh();
        } else {
            Thread.sleep(1000);
        }

        assertThat(count, equalTo(findNumberOfPerformedActions("_name")));
    }

    @Test
    @LuceneTestCase.Slow
    public void testModifyWatchWithSameUnit() throws Exception {
        if (timeWarped()) {
            logger.info("Skipping testModifyWatches_ because timewarp is enabled");
            return;
        }

        WatchSourceBuilder source = watchBuilder()
                .trigger(schedule(interval("1s")))
                .input(simpleInput("key", "value"))
                .defaultThrottlePeriod(TimeValue.timeValueSeconds(0))
                .addAction("_id", loggingAction("hello {{ctx.watcher_id}}!"));
        watcherClient().preparePutWatch("_name")
                .setSource(source)
                .get();

        Thread.sleep(5000);
        assertWatchWithMinimumPerformedActionsCount("_name", 5, false);

        source = watchBuilder()
                .trigger(schedule(interval("100s")))
                .defaultThrottlePeriod(TimeValue.timeValueSeconds(0))
                .input(simpleInput("key", "value"))
                .addAction("_id", loggingAction("hello {{ctx.watcher_id}}!"));
        watcherClient().preparePutWatch("_name")
                .setSource(source)
                .get();

        // Wait one second to be sure that the scheduler engine has executed any previous job instance of the watch
        Thread.sleep(1000);
        long before = historyRecordsCount("_name");
        Thread.sleep(5000);
        assertThat("Watch has been updated to 100s interval, so no new records should have been added.", historyRecordsCount("_name"), equalTo(before));
    }

    @Test
    public void testConditionSearchWithSource() throws Exception {
        String variable = randomFrom("ctx.execution_time", "ctx.trigger.scheduled_time", "ctx.trigger.triggered_time");
        SearchSourceBuilder searchSourceBuilder = searchSource().query(filteredQuery(
                matchQuery("level", "a"),
                rangeQuery("_timestamp")
                        .from("{{" + variable + "}}||-30s")
                        .to("{{" + variable + "}}")));

        testConditionSearch(newInputSearchRequest("events").source(searchSourceBuilder));
    }

    @Test
    public void testConditionSearchWithIndexedTemplate() throws Exception {
        String variable = randomFrom("ctx.execution_time", "ctx.trigger.scheduled_time", "ctx.trigger.triggered_time");
        SearchSourceBuilder searchSourceBuilder = searchSource().query(filteredQuery(
                matchQuery("level", "a"),
                rangeQuery("_timestamp")
                        .from("{{" + variable + "}}||-30s")
                        .to("{{" + variable + "}}")));

        client().preparePutIndexedScript()
                .setScriptLang("mustache")
                .setId("my-template")
                .setSource(jsonBuilder().startObject().field("template").value(searchSourceBuilder).endObject())
                .get();
        refresh();

        BytesReference templateSource = jsonBuilder()
                .value(Template.indexed("my-template").build())
                .bytes();
        SearchRequest searchRequest = newInputSearchRequest("events");
        // TODO (2.0 upgrade): move back to BytesReference instead of coverting to a string
        searchRequest.templateSource(templateSource.toUtf8());
        testConditionSearch(searchRequest);
    }

    @Test
    public void testInputFiltering() throws Exception {
        WatcherClient watcherClient = watcherClient();
        createIndex("idx");
        // Have a sample document in the index, the watch is going to evaluate
        client().prepareIndex("idx", "type").setSource("field", "value").get();
        refresh();
        SearchRequest searchRequest = newInputSearchRequest("idx").source(searchSource().query(termQuery("field", "value")));
        watcherClient.preparePutWatch("_name1")
                .setSource(watchBuilder()
                        .trigger(schedule(interval(5, IntervalSchedule.Interval.Unit.SECONDS)))
                        .input(searchInput(searchRequest).extractKeys("hits.total"))
                        .condition(scriptCondition("ctx.payload.hits.total == 1")))
                .get();
        // in this watcher the condition will fail, because max_score isn't extracted, only total:
        watcherClient.preparePutWatch("_name2")
                .setSource(watchBuilder()
                        .trigger(schedule(interval(5, IntervalSchedule.Interval.Unit.SECONDS)))
                        .input(searchInput(searchRequest).extractKeys("hits.total"))
                        .condition(scriptCondition("ctx.payload.hits.max_score >= 0")))
                .get();

        if (timeWarped()) {
            timeWarp().scheduler().trigger("_name1");
            timeWarp().scheduler().trigger("_name2");
            refresh();
        } else {
            Thread.sleep(5000);
        }

        assertWatchWithMinimumPerformedActionsCount("_name1", 1);
        assertWatchWithNoActionNeeded("_name2", 1);

        // Check that the input result payload has been filtered
        refresh();
        SearchResponse searchResponse = client().prepareSearch(HistoryStore.INDEX_PREFIX + "*")
                .setIndicesOptions(IndicesOptions.lenientExpandOpen())
                .setQuery(matchQuery("watch_id", "_name1"))
                .setSize(1)
                .get();
        assertHitCount(searchResponse, 1);
        XContentSource source = new XContentSource(searchResponse.getHits().getAt(0).getSourceRef());
        assertThat(source.getValue("result.input.payload.hits.total"), equalTo((Object) 1));
    }

    @Test
    public void testPutWatchWithNegativeSchedule() throws Exception {
        try {
            watcherClient().preparePutWatch("_name")
                    .setSource(watchBuilder()
                            .trigger(schedule(interval(-5, IntervalSchedule.Interval.Unit.SECONDS)))
                            .input(simpleInput("key", "value"))
                            .condition(alwaysCondition())
                            .addAction("_logger", loggingAction("executed!")))
                    .get();
            fail("put watch should have failed");
        } catch (ScheduleTriggerException e) {
            assertThat(e.getMessage(), equalTo("interval can't be lower than 1000 ms, but [-5s] was specified"));
        }

        try {
            watcherClient().preparePutWatch("_name")
                    .setSource(watchBuilder()
                            .trigger(schedule(hourly().minutes(-10).build()))
                            .input(simpleInput("key", "value"))
                            .condition(alwaysCondition())
                            .addAction("_logger", loggingAction("executed!")))
                    .get();
            fail("put watch should have failed");
        } catch (ScheduleTriggerException e) {
            assertThat(e.getMessage(), equalTo("invalid hourly minute [-10]. minute must be between 0 and 59 incl."));
        }

        try {
            watcherClient().preparePutWatch("_name")
                    .setSource(watchBuilder()
                            .trigger(schedule(daily().atRoundHour(-10).build()))
                            .input(simpleInput("key", "value"))
                            .condition(alwaysCondition())
                            .addAction("_logger", loggingAction("executed!")))
                    .get();
            fail("put watch should have failed");
        } catch (ScheduleTriggerException e) {
            assertThat(e.getMessage(), equalTo("invalid time [0-10:00]. invalid time hour value [-10]. time hours must be between 0 and 23 incl."));
        }

        try {
            watcherClient().preparePutWatch("_name")
                    .setSource(watchBuilder()
                            .trigger(schedule(weekly().time(WeekTimes.builder().atRoundHour(-10).build()).build()))
                                    .input(simpleInput("key", "value"))
                                    .condition(alwaysCondition())
                                    .addAction("_logger", loggingAction("executed!")))
                            .get();
            fail("put watch should have failed");
        } catch (ScheduleTriggerException e) {
            assertThat(e.getMessage(), equalTo("invalid time [0-10:00]. invalid time hour value [-10]. time hours must be between 0 and 23 incl."));
        }

        try {
            watcherClient().preparePutWatch("_name")
                    .setSource(watchBuilder()
                            .trigger(schedule(monthly().time(MonthTimes.builder().atRoundHour(-10).build()).build()))
                            .input(simpleInput("key", "value"))
                            .condition(alwaysCondition())
                            .addAction("_logger", loggingAction("executed!")))
                    .get();
            fail("put watch should have failed");
        } catch (ScheduleTriggerException e) {
            assertThat(e.getMessage(), equalTo("invalid time [0-10:00]. invalid time hour value [-10]. time hours must be between 0 and 23 incl."));
        }
    }

    private void testConditionSearch(SearchRequest request) throws Exception {
        if (timeWarped()) {
            // reset, so we don't miss event docs when we filter over the _timestamp field.
            timeWarp().clock().setTime(SystemClock.INSTANCE.now());
        }

        String watchName = "_name";
        assertAcked(prepareCreate("events").addMapping("event", "_timestamp", "enabled=true", "level", "type=string"));

        watcherClient().preparePutWatch(watchName)
                .setSource(watchBuilder()
                        .trigger(schedule(interval("5s")))
                        .input(searchInput(request))
                        .condition(ConditionBuilders.scriptCondition("return ctx.payload.hits.total >= 3")))
                .get();

        logger.info("created watch [{}] at [{}]", watchName, SystemClock.INSTANCE.now());

        client().prepareIndex("events", "event")
                .setCreate(true)
                .setSource("level", "a")
                .get();
        client().prepareIndex("events", "event")
                .setCreate(true)
                .setSource("level", "a")
                .get();

        refresh();

        if (timeWarped()) {
            timeWarp().clock().fastForwardSeconds(5);
            timeWarp().scheduler().trigger(watchName);
            refresh();
        } else {
            Thread.sleep(5000);
        }
        assertWatchWithNoActionNeeded(watchName, 1);

        client().prepareIndex("events", "event")
                .setCreate(true)
                .setSource("level", "b")
                .get();
        refresh();
        if (timeWarped()) {
            timeWarp().clock().fastForwardSeconds(5);
            timeWarp().scheduler().trigger(watchName);
            refresh();
        } else {
            Thread.sleep(5000);
        }
        assertWatchWithNoActionNeeded(watchName, 2);

        client().prepareIndex("events", "event")
                .setCreate(true)
                .setSource("level", "a")
                .get();
        refresh();
        if (timeWarped()) {
            timeWarp().clock().fastForwardSeconds(5);
            timeWarp().scheduler().trigger(watchName);
            refresh();
        } else {
            Thread.sleep(5000);
        }
        assertWatchWithMinimumPerformedActionsCount(watchName, 1);
    }
}
