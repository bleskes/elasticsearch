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

package org.elasticsearch.watcher.transport.action.stats;

import org.apache.lucene.util.LuceneTestCase.AwaitsFix;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.test.ESIntegTestCase.ClusterScope;
import org.elasticsearch.test.junit.annotations.TestLogging;
import org.elasticsearch.watcher.WatcherBuild;
import org.elasticsearch.watcher.WatcherState;
import org.elasticsearch.watcher.client.WatcherClient;
import org.elasticsearch.watcher.condition.ConditionBuilders;
import org.elasticsearch.watcher.test.AbstractWatcherIntegrationTestCase;
import org.elasticsearch.watcher.test.WatcherTestUtils;
import org.elasticsearch.watcher.transport.actions.delete.DeleteWatchResponse;
import org.elasticsearch.watcher.transport.actions.stats.WatcherStatsRequest;
import org.elasticsearch.watcher.transport.actions.stats.WatcherStatsResponse;

import java.util.concurrent.TimeUnit;

import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.search.builder.SearchSourceBuilder.searchSource;
import static org.elasticsearch.test.ESIntegTestCase.Scope.TEST;
import static org.elasticsearch.watcher.client.WatchSourceBuilders.watchBuilder;
import static org.elasticsearch.watcher.input.InputBuilders.searchInput;
import static org.elasticsearch.watcher.trigger.TriggerBuilders.schedule;
import static org.elasticsearch.watcher.trigger.schedule.Schedules.cron;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;


/**
 */
@ClusterScope(scope = TEST, numClientNodes = 0, transportClientRatio = 0, randomDynamicTemplates = false)
@TestLogging("watcher:TRACE")
@AwaitsFix(bugUrl = "https://github.com/elastic/x-plugins/issues/724")
public class WatcherStatsTests extends AbstractWatcherIntegrationTestCase {
    public void testStartedStats() throws Exception {
        WatcherStatsRequest watcherStatsRequest = watcherClient().prepareWatcherStats().request();
        WatcherStatsResponse response = watcherClient().watcherStats(watcherStatsRequest).actionGet();

        assertThat(response.getWatcherState(), is(WatcherState.STARTED));
        assertThat(response.getThreadPoolQueueSize(), is(0L));
        assertThat(response.getWatchesCount(), is(0L));
        assertThat(response.getThreadPoolMaxSize(), is(timeWarped() ? 1L : 0L));
        assertThat(response.getBuild(), is(WatcherBuild.CURRENT));
    }

    public void testWatchCountStats() throws Exception {
        WatcherClient watcherClient = watcherClient();

        WatcherStatsRequest watcherStatsRequest = watcherClient.prepareWatcherStats().request();
        WatcherStatsResponse response = watcherClient.watcherStats(watcherStatsRequest).actionGet();

        assertThat(response.getWatcherState(), equalTo(WatcherState.STARTED));

        SearchRequest searchRequest = WatcherTestUtils.newInputSearchRequest("idx").source(searchSource().query(termQuery("field", "value")));
        watcherClient().preparePutWatch("_name")
                .setSource(watchBuilder()
                        .trigger(schedule(cron("* * * * * ? *")))
                        .input(searchInput(searchRequest))
                        .condition(ConditionBuilders.scriptCondition("ctx.payload.hits.total == 1"))
                )
                .get();

        if (timeWarped()) {
            timeWarp().scheduler().trigger("_name", 30, TimeValue.timeValueSeconds(1));
        } else {
            //Wait a little until we should have queued an action
            Thread.sleep(TimeUnit.SECONDS.toMillis(5));
        }

        response = watcherClient().watcherStats(watcherStatsRequest).actionGet();

        assertThat(response.getWatcherState(), is(WatcherState.STARTED));
        assertThat(response.getWatchesCount(), is(1L));
        assertThat(response.getThreadPoolMaxSize(), greaterThan(0L));

        DeleteWatchResponse deleteWatchResponse = watcherClient.prepareDeleteWatch("_name").get();
        assertThat(deleteWatchResponse.isFound(), is(true));
    }
}
