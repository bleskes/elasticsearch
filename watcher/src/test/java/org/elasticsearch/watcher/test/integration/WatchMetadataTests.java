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

import org.apache.lucene.util.LuceneTestCase.AwaitsFix;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.test.junit.annotations.TestLogging;
import org.joda.time.DateTime;
import org.elasticsearch.watcher.actions.logging.LoggingAction;
import org.elasticsearch.watcher.actions.logging.LoggingLevel;
import org.elasticsearch.watcher.condition.always.AlwaysCondition;
import org.elasticsearch.watcher.execution.ActionExecutionMode;
import org.elasticsearch.watcher.history.HistoryStore;
import org.elasticsearch.watcher.support.text.TextTemplate;
import org.elasticsearch.watcher.support.xcontent.ObjectPath;
import org.elasticsearch.watcher.test.AbstractWatcherIntegrationTestCase;
import org.elasticsearch.watcher.test.WatcherTestUtils;
import org.elasticsearch.watcher.transport.actions.execute.ExecuteWatchResponse;
import org.elasticsearch.watcher.trigger.TriggerEvent;
import org.elasticsearch.watcher.trigger.schedule.ScheduleTriggerEvent;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.joda.time.DateTimeZone.UTC;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.search.builder.SearchSourceBuilder.searchSource;
import static org.elasticsearch.watcher.actions.ActionBuilders.loggingAction;
import static org.elasticsearch.watcher.client.WatchSourceBuilders.watchBuilder;
import static org.elasticsearch.watcher.condition.ConditionBuilders.scriptCondition;
import static org.elasticsearch.watcher.input.InputBuilders.searchInput;
import static org.elasticsearch.watcher.trigger.TriggerBuilders.schedule;
import static org.elasticsearch.watcher.trigger.schedule.Schedules.cron;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

/**
 *
 */
@AwaitsFix(bugUrl = "https://github.com/elastic/x-plugins/issues/724")
public class WatchMetadataTests extends AbstractWatcherIntegrationTestCase {

    @Test
    public void testWatchMetadata() throws Exception {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("foo", "bar");
        List<String> metaList = new ArrayList<>();
        metaList.add("this");
        metaList.add("is");
        metaList.add("a");
        metaList.add("test");

        metadata.put("baz", metaList);
        watcherClient().preparePutWatch("_name")
                .setSource(watchBuilder()
                        .trigger(schedule(cron("0/5 * * * * ? *")))
                        .input(searchInput(WatcherTestUtils.newInputSearchRequest("my-index").source(searchSource().query(matchAllQuery()))))
                        .condition(scriptCondition("ctx.payload.hits.total == 1"))
                        .metadata(metadata))
                        .get();

        if (timeWarped()) {
            timeWarp().scheduler().trigger("_name");
        } else {
            // Wait for a no action entry to be added. (the condition search request will not match, because there are no docs in my-index)
            assertWatchWithNoActionNeeded("_name", 1);
        }

        refresh();
        SearchResponse searchResponse = client().prepareSearch(HistoryStore.INDEX_PREFIX + "*")
                .setQuery(termQuery("metadata.foo", "bar"))
                .get();
        assertThat(searchResponse.getHits().getTotalHits(), greaterThan(0L));
    }

    @Test
    public void testWatchMetadataAvailableAtExecution() throws Exception {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("foo", "bar");
        metadata.put("logtext", "This is a test");

        LoggingAction.Builder loggingAction = loggingAction(TextTemplate.inline("{{ctx.metadata.logtext}}"))
                .setLevel(LoggingLevel.DEBUG)
                .setCategory("test");

        watcherClient().preparePutWatch("_name")
                .setSource(watchBuilder()
                        .trigger(schedule(cron("0 0 0 1 1 ? 2050")))
                        .input(searchInput(WatcherTestUtils.newInputSearchRequest("my-index").source(searchSource().query(matchAllQuery()))))
                        .condition(new AlwaysCondition())
                        .addAction("testLogger", loggingAction)
                        .defaultThrottlePeriod(TimeValue.timeValueSeconds(0))
                        .metadata(metadata))
                .get();

        TriggerEvent triggerEvent = new ScheduleTriggerEvent(new DateTime(UTC), new DateTime(UTC));
        ExecuteWatchResponse executeWatchResponse = watcherClient().prepareExecuteWatch("_name").setTriggerEvent(triggerEvent).setActionMode("_all", ActionExecutionMode.SIMULATE).get();
        Map<String, Object> result = executeWatchResponse.getRecordSource().getAsMap();
        logger.info("result=\n{}", result);

        assertThat(ObjectPath.<String>eval("metadata.foo", result), equalTo("bar"));
        assertThat(ObjectPath.<String>eval("result.actions.0.id", result), equalTo("testLogger"));
        assertThat(ObjectPath.<String>eval("result.actions.0.logging.logged_text", result), equalTo("This is a test"));
    }
}
