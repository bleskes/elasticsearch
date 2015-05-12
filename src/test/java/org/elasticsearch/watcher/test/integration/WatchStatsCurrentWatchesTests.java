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
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import org.elasticsearch.watcher.WatcherState;
import org.elasticsearch.watcher.actions.ActionBuilders;
import org.elasticsearch.watcher.condition.ConditionBuilders;
import org.elasticsearch.watcher.execution.ExecutionPhase;
import org.elasticsearch.watcher.input.InputBuilders;
import org.elasticsearch.watcher.test.AbstractWatcherIntegrationTests;
import org.elasticsearch.watcher.transport.actions.stats.WatcherStatsResponse;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.elasticsearch.test.ElasticsearchIntegrationTest.Scope.TEST;
import static org.elasticsearch.watcher.client.WatchSourceBuilders.watchBuilder;
import static org.elasticsearch.watcher.trigger.TriggerBuilders.schedule;
import static org.elasticsearch.watcher.trigger.schedule.Schedules.interval;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 */
@ElasticsearchIntegrationTest.ClusterScope(scope = TEST, numClientNodes = 0, transportClientRatio = 0, randomDynamicTemplates = false, numDataNodes = 1)
public class WatchStatsCurrentWatchesTests extends AbstractWatcherIntegrationTests {

    @Override
    protected boolean timeWarped() {
        return false;
    }

    @Test
    @LuceneTestCase.Slow
    public void testCurrentWatches() throws Exception {
        watcherClient().preparePutWatch("_id").setSource(watchBuilder()
                .trigger(schedule(interval("1s")))
                .input(InputBuilders.simpleInput("key", "value"))
                .condition(ConditionBuilders.scriptCondition("sleep 10000; return true"))
                .addAction("_action", ActionBuilders.loggingAction("hello {{ctx.watch_id}}!"))
        ).get();

        assertBusy(new Runnable() {
            @Override
            public void run() {
                WatcherStatsResponse response = watcherClient().prepareWatcherStats().setIncludeCurrentWatches(true).get();
                assertThat(response.getWatcherState(), equalTo(WatcherState.STARTED));
                assertThat(response.getWatchesCount(), equalTo(1l));
                assertThat(response.getSnapshots(), notNullValue());
                assertThat(response.getSnapshots().size(), equalTo(1));
                assertThat(response.getSnapshots().get(0).watchId(), equalTo("_id"));
                assertThat(response.getSnapshots().get(0).executionPhase(), equalTo(ExecutionPhase.CONDITION));
            }
        });
    }

}
